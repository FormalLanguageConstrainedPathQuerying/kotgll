/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster;

import org.elasticsearch.cluster.metadata.IndexGraveyard;
import org.elasticsearch.cluster.metadata.IndexGraveyard.IndexGraveyardDiff;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.index.Index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An event received by the local node, signaling that the cluster state has changed.
 */
public class ClusterChangedEvent {

    private final String source;

    private final ClusterState previousState;

    private final ClusterState state;

    private final DiscoveryNodes.Delta nodesDelta;

    public ClusterChangedEvent(String source, ClusterState state, ClusterState previousState) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(previousState, "previousState must not be null");
        this.source = source;
        this.state = state;
        this.previousState = previousState;
        this.nodesDelta = state.nodes().delta(previousState.nodes());
    }

    /**
     * The source that caused this cluster event to be raised.
     */
    public String source() {
        return this.source;
    }

    /**
     * The new cluster state that caused this change event.
     */
    public ClusterState state() {
        return this.state;
    }

    /**
     * The previous cluster state for this change event.
     */
    public ClusterState previousState() {
        return this.previousState;
    }

    /**
     * Returns <code>true</code> if the routing tables (for all indices) have
     * changed between the previous cluster state and the current cluster state.
     * Note that this is an object reference equality test, not an equals test.
     */
    public boolean routingTableChanged() {
        return state.routingTable() != previousState.routingTable();
    }

    /**
     * Returns <code>true</code> iff the routing table has changed for the given index.
     * Note that this is an object reference equality test, not an equals test.
     */
    public boolean indexRoutingTableChanged(String index) {
        Objects.requireNonNull(index, "index must not be null");
        if (state.routingTable().hasIndex(index) == false && previousState.routingTable().hasIndex(index) == false) {
            return false;
        }
        if (state.routingTable().hasIndex(index) && previousState.routingTable().hasIndex(index)) {
            return state.routingTable().index(index) != previousState.routingTable().index(index);
        }
        return true;
    }

    /**
     * Returns the indices deleted in this event
     */
    public List<Index> indicesDeleted() {
        if (previousState.blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)) {
            return indicesDeletedFromTombstones();
        } else {
            return indicesDeletedFromClusterState();
        }
    }

    /**
     * Returns <code>true</code> iff the metadata for the cluster has changed between
     * the previous cluster state and the new cluster state. Note that this is an object
     * reference equality test, not an equals test.
     */
    public boolean metadataChanged() {
        return state.metadata() != previousState.metadata();
    }

    /**
     * Returns a set of custom meta data types when any custom metadata for the cluster has changed
     * between the previous cluster state and the new cluster state. custom meta data types are
     * returned iff they have been added, updated or removed between the previous and the current state
     */
    public Set<String> changedCustomMetadataSet() {
        Set<String> result = new HashSet<>();
        Map<String, Metadata.Custom> currentCustoms = state.metadata().customs();
        Map<String, Metadata.Custom> previousCustoms = previousState.metadata().customs();
        if (currentCustoms.equals(previousCustoms) == false) {
            for (Map.Entry<String, Metadata.Custom> currentCustomMetadata : currentCustoms.entrySet()) {
                if (previousCustoms.containsKey(currentCustomMetadata.getKey()) == false
                    || currentCustomMetadata.getValue().equals(previousCustoms.get(currentCustomMetadata.getKey())) == false) {
                    result.add(currentCustomMetadata.getKey());
                }
            }
            for (Map.Entry<String, Metadata.Custom> previousCustomMetadata : previousCustoms.entrySet()) {
                if (currentCustoms.containsKey(previousCustomMetadata.getKey()) == false) {
                    result.add(previousCustomMetadata.getKey());
                }
            }
        }
        return result;
    }

    /**
     * Returns <code>true</code> iff the {@link IndexMetadata} for a given index
     * has changed between the previous cluster state and the new cluster state.
     * Note that this is an object reference equality test, not an equals test.
     */
    public static boolean indexMetadataChanged(IndexMetadata metadata1, IndexMetadata metadata2) {
        assert metadata1 != null && metadata2 != null;
        return metadata1 != metadata2;
    }

    /**
     * Returns <code>true</code> iff the cluster level blocks have changed between cluster states.
     * Note that this is an object reference equality test, not an equals test.
     */
    public boolean blocksChanged() {
        return state.blocks() != previousState.blocks();
    }

    /**
     * Returns <code>true</code> iff the local node is the master node of the cluster.
     */
    public boolean localNodeMaster() {
        return state.nodes().isLocalNodeElectedMaster();
    }

    /**
     * Returns the {@link org.elasticsearch.cluster.node.DiscoveryNodes.Delta} between
     * the previous cluster state and the new cluster state.
     */
    public DiscoveryNodes.Delta nodesDelta() {
        return this.nodesDelta;
    }

    /**
     * Returns <code>true</code> iff nodes have been removed from the cluster since the last cluster state.
     */
    public boolean nodesRemoved() {
        return nodesDelta.removed();
    }

    /**
     * Returns <code>true</code> iff nodes have been added from the cluster since the last cluster state.
     */
    public boolean nodesAdded() {
        return nodesDelta.added();
    }

    /**
     * Returns <code>true</code> iff nodes have been changed (added or removed) from the cluster since the last cluster state.
     */
    public boolean nodesChanged() {
        return nodesRemoved() || nodesAdded();
    }

    /**
     * Determines whether or not the current cluster state represents an entirely
     * new cluster, either when a node joins a cluster for the first time or when
     * the node receives a cluster state update from a brand new cluster (different
     * UUID from the previous cluster), which will happen when a master node is
     * elected that has never been part of the cluster before.
     */
    public boolean isNewCluster() {
        final String prevClusterUUID = previousState.metadata().clusterUUID();
        final String currClusterUUID = state.metadata().clusterUUID();
        return prevClusterUUID.equals(currClusterUUID) == false;
    }

    private List<Index> indicesDeletedFromClusterState() {
        if (metadataChanged() == false || isNewCluster()) {
            return Collections.emptyList();
        }
        Set<Index> deleted = null;
        final Metadata previousMetadata = previousState.metadata();
        final Metadata currentMetadata = state.metadata();

        if (currentMetadata.indices() != previousMetadata.indices()) {
            for (IndexMetadata index : previousMetadata.indices().values()) {
                IndexMetadata current = currentMetadata.index(index.getIndex());
                if (current == null) {
                    if (deleted == null) {
                        deleted = new HashSet<>();
                    }
                    deleted.add(index.getIndex());
                }
            }
        }

        final IndexGraveyard currentGraveyard = currentMetadata.indexGraveyard();
        final IndexGraveyard previousGraveyard = previousMetadata.indexGraveyard();

        if (currentGraveyard != previousGraveyard) {
            final IndexGraveyardDiff indexGraveyardDiff = (IndexGraveyardDiff) currentGraveyard.diff(previousGraveyard);

            final List<IndexGraveyard.Tombstone> added = indexGraveyardDiff.getAdded();

            if (added.isEmpty() == false) {
                if (deleted == null) {
                    deleted = new HashSet<>();
                }
                for (IndexGraveyard.Tombstone tombstone : added) {
                    deleted.add(tombstone.getIndex());
                }
            }
        }

        return deleted == null ? Collections.<Index>emptyList() : new ArrayList<>(deleted);
    }

    private List<Index> indicesDeletedFromTombstones() {
        List<IndexGraveyard.Tombstone> tombstones = state.metadata().indexGraveyard().getTombstones();
        return tombstones.stream().map(IndexGraveyard.Tombstone::getIndex).toList();
    }

}
