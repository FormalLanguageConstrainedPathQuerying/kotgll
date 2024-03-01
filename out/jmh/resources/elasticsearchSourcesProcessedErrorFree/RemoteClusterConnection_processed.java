/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.transport;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionListenerResponseHandler;
import org.elasticsearch.action.admin.cluster.remote.RemoteClusterNodesAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.support.ContextPreservingActionListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.transport.RemoteClusterPortSettings.REMOTE_CLUSTER_PROFILE;

/**
 * Represents a connection to a single remote cluster. In contrast to a local cluster a remote cluster is not joined such that the
 * current node is part of the cluster and it won't receive cluster state updates from the remote cluster. Remote clusters are also not
 * fully connected with the current node. From a connection perspective a local cluster forms a bi-directional star network while in the
 * remote case we only connect to a subset of the nodes in the cluster in an uni-directional fashion.
 *
 * This class also handles the discovery of nodes from the remote cluster. The initial list of seed nodes is only used to discover all nodes
 * in the remote cluster and connects to all eligible nodes, for details see {@link RemoteClusterService#REMOTE_NODE_ATTRIBUTE}.
 *
 * In the case of a disconnection, this class will issue a re-connect task to establish at most
 * {@link SniffConnectionStrategy#REMOTE_CONNECTIONS_PER_CLUSTER} until either all eligible nodes are exhausted or the maximum number of
 * connections per cluster has been reached.
 */
final class RemoteClusterConnection implements Closeable {

    private final TransportService transportService;
    private final RemoteConnectionManager remoteConnectionManager;
    private final RemoteConnectionStrategy connectionStrategy;
    private final String clusterAlias;
    private final ThreadPool threadPool;
    private volatile boolean skipUnavailable;
    private final TimeValue initialConnectionTimeout;

    /**
     * Creates a new {@link RemoteClusterConnection}
     * @param settings the nodes settings object
     * @param clusterAlias the configured alias of the cluster to connect to
     * @param transportService the local nodes transport service
     * @param credentialsManager object to lookup remote cluster credentials by cluster alias. If a cluster is protected by a credential,
     *                           i.e. it has a credential configured via secure setting.
     *                           This means the remote cluster uses the advances RCS model (as opposed to the basic model).
     */
    RemoteClusterConnection(
        Settings settings,
        String clusterAlias,
        TransportService transportService,
        RemoteClusterCredentialsManager credentialsManager
    ) {
        this.transportService = transportService;
        this.clusterAlias = clusterAlias;
        ConnectionProfile profile = RemoteConnectionStrategy.buildConnectionProfile(
            clusterAlias,
            settings,
            credentialsManager.hasCredentials(clusterAlias)
        );
        this.remoteConnectionManager = new RemoteConnectionManager(
            clusterAlias,
            credentialsManager,
            createConnectionManager(profile, transportService)
        );
        this.connectionStrategy = RemoteConnectionStrategy.buildStrategy(clusterAlias, transportService, remoteConnectionManager, settings);
        this.remoteConnectionManager.addListener(transportService);
        this.skipUnavailable = RemoteClusterService.REMOTE_CLUSTER_SKIP_UNAVAILABLE.getConcreteSettingForNamespace(clusterAlias)
            .get(settings);
        this.threadPool = transportService.threadPool;
        initialConnectionTimeout = RemoteClusterService.REMOTE_INITIAL_CONNECTION_TIMEOUT_SETTING.get(settings);
    }

    /**
     * Updates the skipUnavailable flag that can be dynamically set for each remote cluster
     */
    void setSkipUnavailable(boolean skipUnavailable) {
        this.skipUnavailable = skipUnavailable;
    }

    /**
     * Returns whether this cluster is configured to be skipped when unavailable
     */
    boolean isSkipUnavailable() {
        return skipUnavailable;
    }

    /**
     * Ensures that this cluster is connected. If the cluster is connected this operation
     * will invoke the listener immediately.
     */
    void ensureConnected(ActionListener<Void> listener) {
        if (remoteConnectionManager.size() == 0) {
            connectionStrategy.connect(listener);
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Collects all nodes on the connected cluster and returns / passes a nodeID to {@link DiscoveryNode} lookup function
     * that returns <code>null</code> if the node ID is not found.
     *
     * The requests to get cluster state on the connected cluster are made in the system context because logically
     * they are equivalent to checking a single detail in the local cluster state and should not require that the
     * user who made the request that is using this method in its implementation is authorized to view the entire
     * cluster state.
     */
    void collectNodes(ActionListener<Function<String, DiscoveryNode>> listener) {
        Runnable runnable = () -> {
            final ThreadContext threadContext = threadPool.getThreadContext();
            final ContextPreservingActionListener<Function<String, DiscoveryNode>> contextPreservingActionListener =
                new ContextPreservingActionListener<>(threadContext.newRestorableContext(false), listener);
            try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
                threadContext.markAsSystemContext();
                Transport.Connection connection = remoteConnectionManager.getAnyRemoteConnection();

                if (REMOTE_CLUSTER_PROFILE.equals(remoteConnectionManager.getConnectionProfile().getTransportProfile())) {
                    transportService.sendRequest(
                        connection,
                        RemoteClusterNodesAction.TYPE.name(),
                        RemoteClusterNodesAction.Request.ALL_NODES,
                        TransportRequestOptions.EMPTY,
                        new ActionListenerResponseHandler<>(contextPreservingActionListener.map(response -> {
                            final Map<String, DiscoveryNode> nodeLookup = response.getNodes()
                                .stream()
                                .collect(Collectors.toUnmodifiableMap(DiscoveryNode::getId, Function.identity()));
                            return nodeLookup::get;
                        }), RemoteClusterNodesAction.Response::new, TransportResponseHandler.TRANSPORT_WORKER)
                    );
                } else {
                    final ClusterStateRequest request = new ClusterStateRequest();
                    request.clear();
                    request.nodes(true);
                    request.local(true); 

                    transportService.sendRequest(
                        connection,
                        ClusterStateAction.NAME,
                        request,
                        TransportRequestOptions.EMPTY,
                        new ActionListenerResponseHandler<>(
                            contextPreservingActionListener.map(response -> response.getState().nodes()::get),
                            ClusterStateResponse::new,
                            TransportResponseHandler.TRANSPORT_WORKER
                        )
                    );
                }
            }
        };

        try {
            ensureConnected(listener.delegateFailureAndWrap((l, x) -> runnable.run()));
        } catch (Exception ex) {
            listener.onFailure(ex);
        }
    }

    /**
     * Returns a connection to the remote cluster, preferably a direct connection to the provided {@link DiscoveryNode}.
     * If such node is not connected, the returned connection will be a proxy connection that redirects to it.
     */
    Transport.Connection getConnection(DiscoveryNode remoteClusterNode) {
        return remoteConnectionManager.getConnection(remoteClusterNode);
    }

    Transport.Connection getConnection() {
        return remoteConnectionManager.getAnyRemoteConnection();
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(connectionStrategy, remoteConnectionManager);
    }

    public boolean isClosed() {
        return connectionStrategy.isClosed();
    }

    boolean assertNoRunningConnections() {
        return connectionStrategy.assertNoRunningConnections();
    }

    boolean isNodeConnected(final DiscoveryNode node) {
        return remoteConnectionManager.nodeConnected(node);
    }

    /**
     * Get the information about remote nodes to be rendered on {@code _remote/info} requests.
     */
    public RemoteConnectionInfo getConnectionInfo() {
        return new RemoteConnectionInfo(
            clusterAlias,
            connectionStrategy.getModeInfo(),
            initialConnectionTimeout,
            skipUnavailable,
            REMOTE_CLUSTER_PROFILE.equals(remoteConnectionManager.getConnectionProfile().getTransportProfile())
        );
    }

    int getNumNodesConnected() {
        return remoteConnectionManager.size();
    }

    private static ConnectionManager createConnectionManager(ConnectionProfile connectionProfile, TransportService transportService) {
        return new ClusterConnectionManager(connectionProfile, transportService.transport, transportService.threadPool.getThreadContext());
    }

    ConnectionManager getConnectionManager() {
        return remoteConnectionManager;
    }

    boolean shouldRebuildConnection(Settings newSettings) {
        return connectionStrategy.shouldRebuildConnection(newSettings);
    }
}
