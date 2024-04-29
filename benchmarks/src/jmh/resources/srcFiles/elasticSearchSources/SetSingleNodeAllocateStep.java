/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ilm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateObserver;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDeciders;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.cluster.routing.allocation.decider.FilterAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.NodeReplacementAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.NodeShutdownAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.NodeVersionAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.ShardsLimitAllocationDecider;
import org.elasticsearch.common.Randomness;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.xpack.cluster.routing.allocation.DataTierAllocationDecider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Allocates all shards in a single index to one node.
 * For example, as preparation for shrinking that index.
 */
public class SetSingleNodeAllocateStep extends AsyncActionStep {
    private static final Logger logger = LogManager.getLogger(SetSingleNodeAllocateStep.class);
    public static final String NAME = "set-single-node-allocation";

    public SetSingleNodeAllocateStep(StepKey key, StepKey nextStepKey, Client client) {
        super(key, nextStepKey, client);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    @Override
    public void performAction(
        IndexMetadata indexMetadata,
        ClusterState clusterState,
        ClusterStateObserver observer,
        ActionListener<Void> listener
    ) {
        AllocationDeciders allocationDeciders = new AllocationDeciders(
            List.of(
                new FilterAllocationDecider(
                    clusterState.getMetadata().settings(),
                    new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS)
                ),
                DataTierAllocationDecider.INSTANCE,
                new NodeVersionAllocationDecider(),
                new NodeShutdownAllocationDecider(),
                new NodeReplacementAllocationDecider()
            )
        );
        RoutingAllocation allocation = new RoutingAllocation(allocationDeciders, clusterState, null, null, System.nanoTime());
        List<String> validNodeIds = new ArrayList<>();
        String indexName = indexMetadata.getIndex().getName();
        final Map<ShardId, List<ShardRouting>> routingsByShardId = clusterState.getRoutingTable()
            .allShards(indexName)
            .stream()
            .collect(Collectors.groupingBy(ShardRouting::shardId));

        if (routingsByShardId.isEmpty() == false) {
            for (RoutingNode node : allocation.routingNodes()) {
                boolean canAllocateOneCopyOfEachShard = routingsByShardId.values()
                    .stream() 
                    .allMatch(
                        shardRoutings -> shardRoutings.stream() 
                            .map(shardRouting -> allocationDeciders.canAllocate(shardRouting, node, allocation).type())
                            .anyMatch(Decision.Type.YES::equals)
                    );
                if (canAllocateOneCopyOfEachShard) {
                    validNodeIds.add(node.node().getId());
                }
            }
            Randomness.shuffle(validNodeIds);
            Optional<String> nodeId = validNodeIds.stream().findAny();

            if (nodeId.isPresent()) {
                Settings settings = Settings.builder()
                    .put(IndexMetadata.INDEX_ROUTING_REQUIRE_GROUP_SETTING.getKey() + "_id", nodeId.get())
                    .putNull(ShardsLimitAllocationDecider.INDEX_TOTAL_SHARDS_PER_NODE_SETTING.getKey())
                    .build();
                UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest(indexName).masterNodeTimeout(TimeValue.MAX_VALUE)
                    .settings(settings);
                getClient().admin()
                    .indices()
                    .updateSettings(updateSettingsRequest, listener.delegateFailureAndWrap((l, response) -> l.onResponse(null)));
            } else {
                logger.debug("could not find any nodes to allocate index [{}] onto prior to shrink", indexName);
                listener.onFailure(
                    new NoNodeAvailableException("could not find any nodes to allocate index [" + indexName + "] onto" + " prior to shrink")
                );
            }
        } else {
            listener.onFailure(new IndexNotFoundException(indexMetadata.getIndex()));
        }
    }

}
