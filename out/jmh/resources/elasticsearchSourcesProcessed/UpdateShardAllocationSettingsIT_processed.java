/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.cluster.routing.allocation.decider;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESIntegTestCase;

import java.util.Set;

import static org.elasticsearch.cluster.routing.allocation.decider.SameShardAllocationDecider.CLUSTER_ROUTING_ALLOCATION_SAME_HOST_SETTING;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

/**
 * An integration test for testing updating shard allocation/routing settings and
 * ensuring the updated settings take effect as expected.
 */
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0)
public class UpdateShardAllocationSettingsIT extends ESIntegTestCase {

    /**
     * Tests that updating the {@link EnableAllocationDecider} related settings works as expected.
     */
    public void testEnableRebalance() {
        final String firstNode = internalCluster().startNode();
        updateClusterSettings(
            Settings.builder()
                .put(EnableAllocationDecider.CLUSTER_ROUTING_REBALANCE_ENABLE_SETTING.getKey(), EnableAllocationDecider.Rebalance.NONE)
        );
        final int numShards = 2;
        assertAcked(prepareCreate("test").setSettings(indexSettings(numShards, 0)));
        assertAcked(prepareCreate("test_1").setSettings(indexSettings(numShards, 0)));
        ensureGreen();
        assertAllShardsOnNodes("test", firstNode);
        assertAllShardsOnNodes("test_1", firstNode);

        final String secondNode = internalCluster().startNode();
        updateIndexSettings(
            Settings.builder()
                .put(EnableAllocationDecider.INDEX_ROUTING_REBALANCE_ENABLE_SETTING.getKey(), EnableAllocationDecider.Rebalance.NONE),
            "test"
        );
        clusterAdmin().prepareReroute().get();
        ensureGreen();
        assertAllShardsOnNodes("test", firstNode);
        assertAllShardsOnNodes("test_1", firstNode);

        updateIndexSettings(
            Settings.builder()
                .put(
                    EnableAllocationDecider.INDEX_ROUTING_REBALANCE_ENABLE_SETTING.getKey(),
                    randomBoolean() ? EnableAllocationDecider.Rebalance.PRIMARIES : EnableAllocationDecider.Rebalance.ALL
                ),
            "test"
        );
        logger.info("--> balance index [test]");
        clusterAdmin().prepareReroute().get();
        ensureGreen("test");
        Set<String> test = assertAllShardsOnNodes("test", firstNode, secondNode);
        assertThat("index: [test] expected to be rebalanced on both nodes", test.size(), equalTo(2));

        updateClusterSettings(
            Settings.builder()
                .put(
                    EnableAllocationDecider.CLUSTER_ROUTING_REBALANCE_ENABLE_SETTING.getKey(),
                    randomBoolean() ? EnableAllocationDecider.Rebalance.PRIMARIES : EnableAllocationDecider.Rebalance.ALL
                )
        );
        logger.info("--> balance index [test_1]");
        clusterAdmin().prepareReroute().get();
        ensureGreen("test_1");
        Set<String> test_1 = assertAllShardsOnNodes("test_1", firstNode, secondNode);
        assertThat("index: [test_1] expected to be rebalanced on both nodes", test_1.size(), equalTo(2));

        test = assertAllShardsOnNodes("test", firstNode, secondNode);
        assertThat("index: [test] expected to be rebalanced on both nodes", test.size(), equalTo(2));
    }

    /**
     * Tests that updating the {@link SameShardAllocationDecider#CLUSTER_ROUTING_ALLOCATION_SAME_HOST_SETTING} setting works as expected.
     */
    public void testUpdateSameHostSetting() {
        internalCluster().startNodes(2);
        updateClusterSettings(Settings.builder().put(CLUSTER_ROUTING_ALLOCATION_SAME_HOST_SETTING.getKey(), true));
        final String indexName = "idx";
        createIndex(indexName, 1, 1);
        ClusterState clusterState = clusterAdmin().prepareState().get().getState();
        assertFalse(
            "replica should be unassigned",
            clusterState.getRoutingTable().index(indexName).shardsWithState(ShardRoutingState.UNASSIGNED).isEmpty()
        );
        updateClusterSettings(Settings.builder().put(CLUSTER_ROUTING_ALLOCATION_SAME_HOST_SETTING.getKey(), false));

        clusterState = clusterAdmin().prepareState().get().getState();
        assertTrue(
            "all shards should be assigned",
            clusterState.getRoutingTable().index(indexName).shardsWithState(ShardRoutingState.UNASSIGNED).isEmpty()
        );
    }
}
