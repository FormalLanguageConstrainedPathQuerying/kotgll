/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gateway;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.cluster.routing.UnassignedInfo.AllocationStatus;
import org.elasticsearch.cluster.routing.allocation.AllocateUnassignedDecision;
import org.elasticsearch.cluster.routing.allocation.NodeAllocationResult;
import org.elasticsearch.cluster.routing.allocation.NodeAllocationResult.ShardStoreInfo;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.store.StoreFileMetadata;
import org.elasticsearch.indices.store.TransportNodesListShardStoreMetadata;
import org.elasticsearch.indices.store.TransportNodesListShardStoreMetadata.NodeStoreFilesMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.elasticsearch.cluster.routing.UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING;

public abstract class ReplicaShardAllocator extends BaseGatewayShardAllocator {
    /**
     * Process existing recoveries of replicas and see if we need to cancel them if we find a better
     * match. Today, a better match is one that can perform a no-op recovery while the previous recovery
     * has to copy segment files.
     */
    public void processExistingRecoveries(RoutingAllocation allocation, Predicate<ShardRouting> isRelevantShardPredicate) {
        RoutingNodes routingNodes = allocation.routingNodes();
        List<Runnable> shardCancellationActions = new ArrayList<>();
        for (RoutingNode routingNode : routingNodes) {
            for (ShardRouting shard : routingNode) {
                if (shard.primary()) {
                    continue;
                }
                if (shard.initializing() == false) {
                    continue;
                }
                if (shard.relocatingNodeId() != null) {
                    continue;
                }
                if (isRelevantShardPredicate.test(shard) == false) {
                    continue;
                }

                if (shard.unassignedInfo() != null && shard.unassignedInfo().getReason() == UnassignedInfo.Reason.INDEX_CREATED) {
                    continue;
                }

                AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> shardStores = fetchData(shard, allocation);
                if (shardStores.hasData() == false) {
                    logger.trace("{}: fetching new stores for initializing shard", shard);
                    continue; 
                }

                ShardRouting primaryShard = allocation.routingNodes().activePrimary(shard.shardId());
                assert primaryShard != null : "the replica shard can be allocated on at least one node, so there must be an active primary";
                assert primaryShard.currentNodeId() != null;
                final DiscoveryNode primaryNode = allocation.nodes().get(primaryShard.currentNodeId());
                final TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore = findStore(primaryNode, shardStores);
                if (primaryStore == null) {
                    logger.trace("{}: no primary shard store found or allocated, letting actual allocation figure it out", shard);
                    continue;
                }

                MatchingNodes matchingNodes = findMatchingNodes(shard, allocation, true, primaryNode, primaryStore, shardStores, false);
                if (matchingNodes.nodeWithHighestMatch() != null) {
                    DiscoveryNode currentNode = allocation.nodes().get(shard.currentNodeId());
                    DiscoveryNode nodeWithHighestMatch = matchingNodes.nodeWithHighestMatch();
                    if (currentNode.equals(nodeWithHighestMatch) == false
                        && matchingNodes.canPerformNoopRecovery(nodeWithHighestMatch)
                        && canPerformOperationBasedRecovery(primaryStore, shardStores, currentNode) == false) {
                        logger.debug(
                            "cancelling allocation of replica on [{}], can perform a noop recovery on node [{}]",
                            currentNode,
                            nodeWithHighestMatch
                        );
                        final Set<String> failedNodeIds = shard.unassignedInfo() == null
                            ? Collections.emptySet()
                            : shard.unassignedInfo().getFailedNodeIds();
                        UnassignedInfo unassignedInfo = new UnassignedInfo(
                            UnassignedInfo.Reason.REALLOCATED_REPLICA,
                            "existing allocation of replica to ["
                                + currentNode
                                + "] cancelled, can perform a noop recovery on ["
                                + nodeWithHighestMatch
                                + "]",
                            null,
                            0,
                            allocation.getCurrentNanoTime(),
                            System.currentTimeMillis(),
                            false,
                            UnassignedInfo.AllocationStatus.NO_ATTEMPT,
                            failedNodeIds,
                            null
                        );
                        shardCancellationActions.add(() -> routingNodes.failShard(logger, shard, unassignedInfo, allocation.changes()));
                    }
                }
            }
        }
        for (Runnable action : shardCancellationActions) {
            action.run();
        }
    }

    /**
     * Is the allocator responsible for allocating the given {@link ShardRouting}?
     */
    private static boolean isResponsibleFor(final ShardRouting shard) {
        return shard.primary() == false 
            && shard.unassigned() 
            && shard.unassignedInfo().getReason() != UnassignedInfo.Reason.INDEX_CREATED;
    }

    @Override
    public AllocateUnassignedDecision makeAllocationDecision(
        final ShardRouting unassignedShard,
        final RoutingAllocation allocation,
        final Logger logger
    ) {
        if (isResponsibleFor(unassignedShard) == false) {
            return AllocateUnassignedDecision.NOT_TAKEN;
        }

        final RoutingNodes routingNodes = allocation.routingNodes();
        final boolean explain = allocation.debugDecision();
        PerNodeAllocationResult result = canBeAllocatedToAtLeastOneNode(unassignedShard, allocation);
        Decision allocateDecision = result.decision();
        if (allocateDecision.type() != Decision.Type.YES && (explain == false || hasInitiatedFetching(unassignedShard) == false)) {
            logger.trace("{}: ignoring allocation, can't be allocated on any node", unassignedShard);
            return AllocateUnassignedDecision.no(UnassignedInfo.AllocationStatus.fromDecision(allocateDecision.type()), result.nodes());
        }

        AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> shardStores = fetchData(unassignedShard, allocation);
        if (shardStores.hasData() == false) {
            logger.trace("{}: ignoring allocation, still fetching shard stores", unassignedShard);
            allocation.setHasPendingAsyncFetch();
            List<NodeAllocationResult> nodeDecisions = null;
            if (explain) {
                nodeDecisions = buildDecisionsForAllNodes(unassignedShard, allocation);
            }
            return AllocateUnassignedDecision.no(AllocationStatus.FETCHING_SHARD_DATA, nodeDecisions);
        }

        ShardRouting primaryShard = routingNodes.activePrimary(unassignedShard.shardId());
        if (primaryShard == null) {
            assert explain
                : "primary should only be null here if we are in explain mode, so we didn't "
                    + "exit early when canBeAllocatedToAtLeastOneNode didn't return a YES decision";
            return AllocateUnassignedDecision.no(UnassignedInfo.AllocationStatus.fromDecision(allocateDecision.type()), result.nodes());
        }
        assert primaryShard.currentNodeId() != null;
        final DiscoveryNode primaryNode = allocation.nodes().get(primaryShard.currentNodeId());
        final TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore = findStore(primaryNode, shardStores);
        if (primaryStore == null) {
            logger.trace("{}: no primary shard store found or allocated, letting actual allocation figure it out", unassignedShard);
            return AllocateUnassignedDecision.NOT_TAKEN;
        }

        MatchingNodes matchingNodes = findMatchingNodes(
            unassignedShard,
            allocation,
            false,
            primaryNode,
            primaryStore,
            shardStores,
            explain
        );
        assert explain == false || matchingNodes.nodeDecisions != null : "in explain mode, we must have individual node decisions";

        List<NodeAllocationResult> nodeDecisions = augmentExplanationsWithStoreInfo(result.nodes(), matchingNodes.nodeDecisions);
        if (allocateDecision.type() != Decision.Type.YES) {
            return AllocateUnassignedDecision.no(UnassignedInfo.AllocationStatus.fromDecision(allocateDecision.type()), nodeDecisions);
        } else if (matchingNodes.nodeWithHighestMatch() != null) {
            RoutingNode nodeWithHighestMatch = allocation.routingNodes().node(matchingNodes.nodeWithHighestMatch().getId());
            Decision decision = allocation.deciders()
                .canAllocateReplicaWhenThereIsRetentionLease(unassignedShard, nodeWithHighestMatch, allocation);
            if (decision.type() == Decision.Type.THROTTLE) {
                logger.debug(
                    "[{}][{}]: throttling allocation [{}] to [{}] in order to reuse its unallocated persistent store",
                    unassignedShard.index(),
                    unassignedShard.id(),
                    unassignedShard,
                    nodeWithHighestMatch.node()
                );
                return AllocateUnassignedDecision.throttle(nodeDecisions);
            } else {
                logger.debug(
                    "[{}][{}]: allocating [{}] to [{}] in order to reuse its unallocated persistent store",
                    unassignedShard.index(),
                    unassignedShard.id(),
                    unassignedShard,
                    nodeWithHighestMatch.node()
                );
                return AllocateUnassignedDecision.yes(nodeWithHighestMatch.node(), null, nodeDecisions, true);
            }
        } else if (matchingNodes.hasAnyData() == false && unassignedShard.unassignedInfo().isDelayed()) {
            return delayedDecision(unassignedShard, allocation, logger, nodeDecisions);
        }

        return AllocateUnassignedDecision.NOT_TAKEN;
    }

    /**
     * Return a delayed decision, filling in the right amount of remaining time if decisions are debugged/explained.
     */
    public static AllocateUnassignedDecision delayedDecision(
        ShardRouting unassignedShard,
        RoutingAllocation allocation,
        Logger logger,
        List<NodeAllocationResult> nodeDecisions
    ) {
        boolean explain = allocation.debugDecision();
        logger.debug("{}: allocation of [{}] is delayed", unassignedShard.shardId(), unassignedShard);
        long remainingDelayMillis = 0L;
        long totalDelayMillis = 0L;
        if (explain) {
            UnassignedInfo unassignedInfo = unassignedShard.unassignedInfo();
            Metadata metadata = allocation.metadata();
            IndexMetadata indexMetadata = metadata.index(unassignedShard.index());
            totalDelayMillis = INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.get(indexMetadata.getSettings()).getMillis();
            long remainingDelayNanos = unassignedInfo.getRemainingDelay(
                System.nanoTime(),
                indexMetadata.getSettings(),
                metadata.nodeShutdowns()
            );
            remainingDelayMillis = TimeValue.timeValueNanos(remainingDelayNanos).millis();
        }
        return AllocateUnassignedDecision.delayed(remainingDelayMillis, totalDelayMillis, nodeDecisions);
    }

    /**
     * Determines if the shard can be allocated on at least one node based on the allocation deciders.
     *
     * Returns the best allocation decision for allocating the shard on any node (i.e. YES if at least one
     * node decided YES, THROTTLE if at least one node decided THROTTLE, and NO if none of the nodes decided
     * YES or THROTTLE).  If in explain mode, also returns the node-level explanations as the second element
     * in the returned tuple.
     */
    public static PerNodeAllocationResult canBeAllocatedToAtLeastOneNode(ShardRouting shard, RoutingAllocation allocation) {
        Decision madeDecision = Decision.NO;
        final boolean explain = allocation.debugDecision();
        List<NodeAllocationResult> nodeDecisions = explain ? new ArrayList<>() : null;
        for (DiscoveryNode discoveryNode : allocation.nodes().getDataNodes().values()) {
            RoutingNode node = allocation.routingNodes().node(discoveryNode.getId());
            if (node == null) {
                continue;
            }
            Decision decision = allocation.deciders().canAllocateReplicaWhenThereIsRetentionLease(shard, node, allocation);
            if (decision.type() == Decision.Type.YES && madeDecision.type() != Decision.Type.YES) {
                if (explain) {
                    madeDecision = decision;
                } else {
                    return new PerNodeAllocationResult(decision, null);
                }
            } else if (madeDecision.type() == Decision.Type.NO && decision.type() == Decision.Type.THROTTLE) {
                madeDecision = decision;
            }
            if (explain) {
                nodeDecisions.add(new NodeAllocationResult(node.node(), null, decision));
            }
        }
        return new PerNodeAllocationResult(madeDecision, nodeDecisions);
    }

    public record PerNodeAllocationResult(Decision decision, List<NodeAllocationResult> nodes) {}

    /**
     * Takes the store info for nodes that have a shard store and adds them to the node decisions,
     * leaving the node explanations untouched for those nodes that do not have any store information.
     */
    public static List<NodeAllocationResult> augmentExplanationsWithStoreInfo(
        List<NodeAllocationResult> nodeDecisions,
        Map<String, NodeAllocationResult> withShardStores
    ) {
        if (nodeDecisions == null || withShardStores == null) {
            return null;
        }
        List<NodeAllocationResult> augmented = new ArrayList<>(nodeDecisions.size());
        for (NodeAllocationResult nodeAllocationResult : nodeDecisions) {
            augmented.add(withShardStores.getOrDefault(nodeAllocationResult.getNode().getId(), nodeAllocationResult));
        }
        return augmented;
    }

    /**
     * Finds the store for the assigned shard in the fetched data, returns null if none is found.
     */
    private static TransportNodesListShardStoreMetadata.StoreFilesMetadata findStore(
        DiscoveryNode node,
        AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> data
    ) {
        NodeStoreFilesMetadata nodeFilesStore = data.getData().get(node);
        if (nodeFilesStore == null) {
            return null;
        }
        return nodeFilesStore.storeFilesMetadata();
    }

    private MatchingNodes findMatchingNodes(
        ShardRouting shard,
        RoutingAllocation allocation,
        boolean noMatchFailedNodes,
        DiscoveryNode primaryNode,
        TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore,
        AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> data,
        boolean explain
    ) {
        Map<DiscoveryNode, MatchingNode> matchingNodes = new HashMap<>();
        Map<String, NodeAllocationResult> nodeDecisions = explain ? new HashMap<>() : null;
        for (Map.Entry<DiscoveryNode, NodeStoreFilesMetadata> nodeStoreEntry : data.getData().entrySet()) {
            DiscoveryNode discoNode = nodeStoreEntry.getKey();
            if (noMatchFailedNodes
                && shard.unassignedInfo() != null
                && shard.unassignedInfo().getFailedNodeIds().contains(discoNode.getId())) {
                continue;
            }
            TransportNodesListShardStoreMetadata.StoreFilesMetadata storeFilesMetadata = nodeStoreEntry.getValue().storeFilesMetadata();
            if (storeFilesMetadata.isEmpty()) {
                continue;
            }

            RoutingNode node = allocation.routingNodes().node(discoNode.getId());
            if (node == null) {
                continue;
            }

            final long retainingSeqNoForReplica = primaryStore.getPeerRecoveryRetentionLeaseRetainingSeqNo(discoNode);
            final Decision decision;
            if (retainingSeqNoForReplica == -1) {
                decision = allocation.deciders().canAllocate(shard, node, allocation);
            } else {
                decision = allocation.deciders().canAllocateReplicaWhenThereIsRetentionLease(shard, node, allocation);
            }

            MatchingNode matchingNode = null;
            if (explain) {
                matchingNode = computeMatchingNode(primaryNode, primaryStore, discoNode, storeFilesMetadata);
                ShardStoreInfo shardStoreInfo = new ShardStoreInfo(matchingNode.matchingBytes);
                nodeDecisions.put(node.nodeId(), new NodeAllocationResult(discoNode, shardStoreInfo, decision));
            }

            if (decision.type() == Decision.Type.NO) {
                continue;
            }

            if (matchingNode == null) {
                matchingNode = computeMatchingNode(primaryNode, primaryStore, discoNode, storeFilesMetadata);
            }
            matchingNodes.put(discoNode, matchingNode);
            if (logger.isTraceEnabled()) {
                if (matchingNode.isNoopRecovery) {
                    logger.trace("{}: node [{}] can perform a noop recovery", shard, discoNode.getName());
                } else if (matchingNode.retainingSeqNo >= 0) {
                    logger.trace(
                        "{}: node [{}] can perform operation-based recovery with retaining sequence number [{}]",
                        shard,
                        discoNode.getName(),
                        matchingNode.retainingSeqNo
                    );
                } else {
                    logger.trace(
                        "{}: node [{}] has [{}/{}] bytes of re-usable data",
                        shard,
                        discoNode.getName(),
                        ByteSizeValue.ofBytes(matchingNode.matchingBytes),
                        matchingNode.matchingBytes
                    );
                }
            }
        }

        return MatchingNodes.create(matchingNodes, nodeDecisions);
    }

    private static long computeMatchingBytes(
        TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore,
        TransportNodesListShardStoreMetadata.StoreFilesMetadata storeFilesMetadata
    ) {
        long sizeMatched = 0;
        for (StoreFileMetadata storeFileMetadata : storeFilesMetadata) {
            String metadataFileName = storeFileMetadata.name();
            if (primaryStore.fileExists(metadataFileName) && primaryStore.file(metadataFileName).isSame(storeFileMetadata)) {
                sizeMatched += storeFileMetadata.length();
            }
        }
        return sizeMatched;
    }

    private static boolean hasMatchingSyncId(
        TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore,
        TransportNodesListShardStoreMetadata.StoreFilesMetadata replicaStore
    ) {
        String primarySyncId = primaryStore.syncId();
        return primarySyncId != null && primarySyncId.equals(replicaStore.syncId());
    }

    private static MatchingNode computeMatchingNode(
        DiscoveryNode primaryNode,
        TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore,
        DiscoveryNode replicaNode,
        TransportNodesListShardStoreMetadata.StoreFilesMetadata replicaStore
    ) {
        final long retainingSeqNoForPrimary = primaryStore.getPeerRecoveryRetentionLeaseRetainingSeqNo(primaryNode);
        final long retainingSeqNoForReplica = primaryStore.getPeerRecoveryRetentionLeaseRetainingSeqNo(replicaNode);
        final boolean isNoopRecovery = (retainingSeqNoForReplica >= retainingSeqNoForPrimary && retainingSeqNoForPrimary >= 0)
            || hasMatchingSyncId(primaryStore, replicaStore);
        final long matchingBytes = computeMatchingBytes(primaryStore, replicaStore);
        return new MatchingNode(matchingBytes, retainingSeqNoForReplica, isNoopRecovery);
    }

    private static boolean canPerformOperationBasedRecovery(
        TransportNodesListShardStoreMetadata.StoreFilesMetadata primaryStore,
        AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> shardStores,
        DiscoveryNode targetNode
    ) {
        final NodeStoreFilesMetadata targetNodeStore = shardStores.getData().get(targetNode);
        if (targetNodeStore == null || targetNodeStore.storeFilesMetadata().isEmpty()) {
            return false;
        }
        if (hasMatchingSyncId(primaryStore, targetNodeStore.storeFilesMetadata())) {
            return true;
        }
        return primaryStore.getPeerRecoveryRetentionLeaseRetainingSeqNo(targetNode) >= 0;
    }

    protected abstract AsyncShardFetch.FetchResult<NodeStoreFilesMetadata> fetchData(ShardRouting shard, RoutingAllocation allocation);

    /**
     * Returns a boolean indicating whether fetching shard data has been triggered at any point for the given shard.
     */
    protected abstract boolean hasInitiatedFetching(ShardRouting shard);

    private record MatchingNode(long matchingBytes, long retainingSeqNo, boolean isNoopRecovery) {

        static final Comparator<MatchingNode> COMPARATOR = Comparator.<MatchingNode, Boolean>comparing(m -> m.isNoopRecovery)
            .thenComparing(m -> m.retainingSeqNo)
            .thenComparing(m -> m.matchingBytes);

        boolean anyMatch() {
            return isNoopRecovery || retainingSeqNo >= 0 || matchingBytes > 0;
        }
    }

    private record MatchingNodes(
        Map<DiscoveryNode, MatchingNode> matchingNodes,
        @Nullable Map<String, NodeAllocationResult> nodeDecisions,
        @Nullable DiscoveryNode nodeWithHighestMatch
    ) {

        boolean canPerformNoopRecovery(DiscoveryNode node) {
            return matchingNodes.get(node).isNoopRecovery;
        }

        /**
         * Did we manage to find any data, regardless how well they matched or not.
         */
        public boolean hasAnyData() {
            return matchingNodes.isEmpty() == false;
        }

        private static MatchingNodes create(
            Map<DiscoveryNode, MatchingNode> matchingNodes,
            @Nullable Map<String, NodeAllocationResult> nodeDecisions
        ) {
            return new MatchingNodes(matchingNodes, nodeDecisions, getNodeWithHighestMatch(matchingNodes));
        }

        /**
         * Returns the node with the highest "non zero byte" match compared to the primary.
         */
        @Nullable
        private static DiscoveryNode getNodeWithHighestMatch(Map<DiscoveryNode, MatchingNode> matchingNodes) {
            return matchingNodes.entrySet()
                .stream()
                .filter(e -> e.getValue().anyMatch())
                .max(Map.Entry.comparingByValue(MatchingNode.COMPARATOR))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
    }
}
