/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.indices.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.IndexShardState;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportException;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.TransportRequestHandler;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportService;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.core.Strings.format;

public final class IndicesStore implements ClusterStateListener, Closeable {

    private static final Logger logger = LogManager.getLogger(IndicesStore.class);

    public static final Setting<TimeValue> INDICES_STORE_DELETE_SHARD_TIMEOUT = Setting.positiveTimeSetting(
        "indices.store.delete.shard.timeout",
        new TimeValue(30, TimeUnit.SECONDS),
        Property.NodeScope
    );
    public static final String ACTION_SHARD_EXISTS = "internal:index/shard/exists";
    private static final EnumSet<IndexShardState> ACTIVE_STATES = EnumSet.of(IndexShardState.STARTED);
    private final Settings settings;
    private final IndicesService indicesService;
    private final ClusterService clusterService;
    private final TransportService transportService;
    private final ThreadPool threadPool;

    private final Set<ShardId> folderNotFoundCache = new HashSet<>();

    private final TimeValue deleteShardTimeout;

    @Inject
    public IndicesStore(
        Settings settings,
        IndicesService indicesService,
        ClusterService clusterService,
        TransportService transportService,
        ThreadPool threadPool
    ) {
        this.settings = settings;
        this.indicesService = indicesService;
        this.clusterService = clusterService;
        this.transportService = transportService;
        this.threadPool = threadPool;
        transportService.registerRequestHandler(
            ACTION_SHARD_EXISTS,
            EsExecutors.DIRECT_EXECUTOR_SERVICE,
            ShardActiveRequest::new,
            new ShardActiveRequestHandler()
        );
        this.deleteShardTimeout = INDICES_STORE_DELETE_SHARD_TIMEOUT.get(settings);
        if (DiscoveryNode.canContainData(settings)) {
            clusterService.addListener(this);
        }
    }

    @Override
    public void close() {
        if (DiscoveryNode.canContainData(settings)) {
            clusterService.removeListener(this);
        }
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (event.routingTableChanged() == false) {
            return;
        }

        if (event.state().blocks().disableStatePersistence()) {
            return;
        }

        RoutingTable routingTable = event.state().routingTable();

        folderNotFoundCache.removeIf(shardId -> routingTable.hasIndex(shardId.getIndex()) == false);
        final String localNodeId = event.state().nodes().getLocalNodeId();
        RoutingNode localRoutingNode = event.state().getRoutingNodes().node(localNodeId);
        if (localRoutingNode != null) {
            for (ShardRouting routing : localRoutingNode) {
                folderNotFoundCache.remove(routing.shardId());
            }
        }

        for (IndexRoutingTable indexRoutingTable : routingTable) {
            for (int i = 0; i < indexRoutingTable.size(); i++) {
                IndexShardRoutingTable indexShardRoutingTable = indexRoutingTable.shard(i);
                ShardId shardId = indexShardRoutingTable.shardId();
                if (folderNotFoundCache.contains(shardId) == false && shardCanBeDeleted(localNodeId, indexShardRoutingTable)) {
                    IndexService indexService = indicesService.indexService(indexRoutingTable.getIndex());
                    final IndexSettings indexSettings;
                    if (indexService == null) {
                        IndexMetadata indexMetadata = event.state().getMetadata().getIndexSafe(indexRoutingTable.getIndex());
                        indexSettings = new IndexSettings(indexMetadata, settings);
                    } else {
                        indexSettings = indexService.getIndexSettings();
                    }
                    IndicesService.ShardDeletionCheckResult shardDeletionCheckResult = indicesService.canDeleteShardContent(
                        shardId,
                        indexSettings
                    );
                    switch (shardDeletionCheckResult) {
                        case FOLDER_FOUND_CAN_DELETE:
                            deleteShardIfExistElseWhere(event.state(), indexShardRoutingTable);
                            break;
                        case NO_FOLDER_FOUND:
                            folderNotFoundCache.add(shardId);
                            break;
                        case STILL_ALLOCATED:
                            break;
                        default:
                            assert false : "unknown shard deletion check result: " + shardDeletionCheckResult;
                    }
                }
            }
        }
    }

    static boolean shardCanBeDeleted(String localNodeId, IndexShardRoutingTable indexShardRoutingTable) {
        assert indexShardRoutingTable.size() > 0;

        if (indexShardRoutingTable.size() == 0) {
            return false;
        }

        for (int copy = 0; copy < indexShardRoutingTable.size(); copy++) {
            ShardRouting shardRouting = indexShardRoutingTable.shard(copy);
            if (shardRouting.started() == false) {
                return false;
            }

            if (localNodeId.equals(shardRouting.currentNodeId())) {
                return false;
            }
        }

        return true;
    }

    private void deleteShardIfExistElseWhere(ClusterState state, IndexShardRoutingTable indexShardRoutingTable) {
        List<Tuple<DiscoveryNode, ShardActiveRequest>> requests = new ArrayList<>(indexShardRoutingTable.size());
        String indexUUID = indexShardRoutingTable.shardId().getIndex().getUUID();
        ClusterName clusterName = state.getClusterName();
        for (int copy = 0; copy < indexShardRoutingTable.size(); copy++) {
            ShardRouting shardRouting = indexShardRoutingTable.shard(copy);
            assert shardRouting.started() : "expected started shard but was " + shardRouting;
            DiscoveryNode currentNode = state.nodes().get(shardRouting.currentNodeId());
            requests.add(
                new Tuple<>(currentNode, new ShardActiveRequest(clusterName, indexUUID, shardRouting.shardId(), deleteShardTimeout))
            );
        }

        ShardActiveResponseHandler responseHandler = new ShardActiveResponseHandler(
            indexShardRoutingTable.shardId(),
            state.getVersion(),
            requests.size()
        );
        for (Tuple<DiscoveryNode, ShardActiveRequest> request : requests) {
            logger.trace("{} sending shard active check to {}", request.v2().shardId, request.v1());
            transportService.sendRequest(request.v1(), ACTION_SHARD_EXISTS, request.v2(), responseHandler);
        }
    }

    private class ShardActiveResponseHandler implements TransportResponseHandler<ShardActiveResponse> {

        private final ShardId shardId;
        private final int expectedActiveCopies;
        private final long clusterStateVersion;
        private final AtomicInteger awaitingResponses;
        private final AtomicInteger activeCopies;

        ShardActiveResponseHandler(ShardId shardId, long clusterStateVersion, int expectedActiveCopies) {
            this.shardId = shardId;
            this.expectedActiveCopies = expectedActiveCopies;
            this.clusterStateVersion = clusterStateVersion;
            this.awaitingResponses = new AtomicInteger(expectedActiveCopies);
            this.activeCopies = new AtomicInteger();
        }

        @Override
        public ShardActiveResponse read(StreamInput in) throws IOException {
            return new ShardActiveResponse(in);
        }

        @Override
        public Executor executor() {
            return TransportResponseHandler.TRANSPORT_WORKER;
        }

        @Override
        public void handleResponse(ShardActiveResponse response) {
            logger.trace("{} is {}active on node {}", shardId, response.shardActive ? "" : "not ", response.node);
            if (response.shardActive) {
                activeCopies.incrementAndGet();
            }

            if (awaitingResponses.decrementAndGet() == 0) {
                allNodesResponded();
            }
        }

        @Override
        public void handleException(TransportException exp) {
            logger.debug(() -> format("shards active request failed for %s", shardId), exp);
            if (awaitingResponses.decrementAndGet() == 0) {
                allNodesResponded();
            }
        }

        private void allNodesResponded() {
            if (activeCopies.get() != expectedActiveCopies) {
                logger.trace(
                    "not deleting shard {}, expected {} active copies, but only {} found active copies",
                    shardId,
                    expectedActiveCopies,
                    activeCopies.get()
                );
                return;
            }

            ClusterState latestClusterState = clusterService.state();
            if (clusterStateVersion != latestClusterState.getVersion()) {
                logger.trace(
                    "not deleting shard {}, the latest cluster state version[{}] is not equal to cluster state "
                        + "before shard active api call [{}]",
                    shardId,
                    latestClusterState.getVersion(),
                    clusterStateVersion
                );
                return;
            }

            clusterService.getClusterApplierService()
                .runOnApplierThread("indices_store ([" + shardId + "] active fully on other nodes)", Priority.HIGH, currentState -> {
                    if (clusterStateVersion != currentState.getVersion()) {
                        logger.trace(
                            "not deleting shard {}, the update task state version[{}] is not equal to cluster state before "
                                + "shard active api call [{}]",
                            shardId,
                            currentState.getVersion(),
                            clusterStateVersion
                        );
                        return;
                    }
                    try {
                        indicesService.deleteShardStore("no longer used", shardId, currentState);
                    } catch (Exception ex) {
                        logger.debug(() -> format("%s failed to delete unallocated shard, ignoring", shardId), ex);
                    }
                }, new ActionListener<>() {
                    @Override
                    public void onResponse(Void unused) {}

                    @Override
                    public void onFailure(Exception e) {
                        logger.error(() -> format("%s unexpected error during deletion of unallocated shard", shardId), e);
                    }
                });
        }

    }

    private class ShardActiveRequestHandler implements TransportRequestHandler<ShardActiveRequest> {

        @Override
        public void messageReceived(final ShardActiveRequest request, final TransportChannel channel, Task task) {
            IndexShard indexShard = getShard(request);

            if (indexShard == null) {
                channel.sendResponse(new ShardActiveResponse(false, clusterService.localNode()));
            } else {
                ClusterStateObserver observer = new ClusterStateObserver(
                    clusterService,
                    request.timeout,
                    logger,
                    threadPool.getThreadContext()
                );
                boolean shardActive = shardActive(indexShard);
                if (shardActive) {
                    channel.sendResponse(new ShardActiveResponse(true, clusterService.localNode()));
                } else {
                    observer.waitForNextChange(new ClusterStateObserver.Listener() {
                        @Override
                        public void onNewClusterState(ClusterState state) {
                            sendResult(shardActive(getShard(request)));
                        }

                        @Override
                        public void onClusterServiceClose() {
                            sendResult(false);
                        }

                        @Override
                        public void onTimeout(TimeValue timeout) {
                            sendResult(shardActive(getShard(request)));
                        }

                        public void sendResult(boolean shardActive) {
                            try {
                                channel.sendResponse(new ShardActiveResponse(shardActive, clusterService.localNode()));
                            } catch (EsRejectedExecutionException e) {
                                logger.error(
                                    () -> format(
                                        "failed send response for shard active while trying to "
                                            + "delete shard %s - shard will probably not be removed",
                                        request.shardId
                                    ),
                                    e
                                );
                            }
                        }
                    }, newState -> {
                        IndexShard currentShard = getShard(request);
                        return currentShard == null || shardActive(currentShard);
                    });
                }
            }
        }

        private static boolean shardActive(IndexShard indexShard) {
            if (indexShard != null) {
                return ACTIVE_STATES.contains(indexShard.state());
            }
            return false;
        }

        private IndexShard getShard(ShardActiveRequest request) {
            ClusterName thisClusterName = clusterService.getClusterName();
            if (thisClusterName.equals(request.clusterName) == false) {
                logger.trace(
                    "shard exists request meant for cluster[{}], but this is cluster[{}], ignoring request",
                    request.clusterName,
                    thisClusterName
                );
                return null;
            }
            ShardId shardId = request.shardId;
            IndexService indexService = indicesService.indexService(shardId.getIndex());
            if (indexService != null && indexService.indexUUID().equals(request.indexUUID)) {
                return indexService.getShardOrNull(shardId.id());
            }
            return null;
        }

    }

    private static class ShardActiveRequest extends TransportRequest {
        private final TimeValue timeout;
        private final ClusterName clusterName;
        private final String indexUUID;
        private final ShardId shardId;

        ShardActiveRequest(StreamInput in) throws IOException {
            super(in);
            clusterName = new ClusterName(in);
            indexUUID = in.readString();
            shardId = new ShardId(in);
            timeout = new TimeValue(in.readLong(), TimeUnit.MILLISECONDS);
        }

        ShardActiveRequest(ClusterName clusterName, String indexUUID, ShardId shardId, TimeValue timeout) {
            this.shardId = shardId;
            this.indexUUID = indexUUID;
            this.clusterName = clusterName;
            this.timeout = timeout;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            clusterName.writeTo(out);
            out.writeString(indexUUID);
            shardId.writeTo(out);
            out.writeLong(timeout.millis());
        }
    }

    private static class ShardActiveResponse extends TransportResponse {

        private final boolean shardActive;
        private final DiscoveryNode node;

        ShardActiveResponse(boolean shardActive, DiscoveryNode node) {
            this.shardActive = shardActive;
            this.node = node;
        }

        ShardActiveResponse(StreamInput in) throws IOException {
            shardActive = in.readBoolean();
            node = new DiscoveryNode(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeBoolean(shardActive);
            node.writeTo(out);
        }
    }
}
