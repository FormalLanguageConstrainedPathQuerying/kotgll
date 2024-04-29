/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.discovery;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.coordination.NoMasterBlockService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Strings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.disruption.BlockMasterServiceOnMaster;
import org.elasticsearch.test.disruption.IntermittentLongGCDisruption;
import org.elasticsearch.test.disruption.NetworkDisruption;
import org.elasticsearch.test.disruption.NetworkDisruption.TwoPartitions;
import org.elasticsearch.test.disruption.ServiceDisruptionScheme;
import org.elasticsearch.test.disruption.SingleNodeDisruption;
import org.elasticsearch.xcontent.XContentType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Tests relating to the loss of the master.
 */
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0)
public class MasterDisruptionIT extends AbstractDisruptionTestCase {

    /**
     * Test that cluster recovers from a long GC on master that causes other nodes to elect a new one
     */
    public void testMasterNodeGCs() throws Exception {
        List<String> nodes = startCluster(3);
        assumeFalse("jdk20 removed thread suspend/resume", Runtime.version().feature() >= 20);

        String oldMasterNode = internalCluster().getMasterName();
        SingleNodeDisruption masterNodeDisruption = new IntermittentLongGCDisruption(random(), oldMasterNode, 100, 200, 30000, 60000);
        internalCluster().setDisruptionScheme(masterNodeDisruption);
        masterNodeDisruption.startDisrupting();

        Set<String> oldNonMasterNodesSet = new HashSet<>(nodes);
        oldNonMasterNodesSet.remove(oldMasterNode);

        List<String> oldNonMasterNodes = new ArrayList<>(oldNonMasterNodesSet);

        logger.info("waiting for nodes to de-elect master [{}]", oldMasterNode);
        for (String node : oldNonMasterNodesSet) {
            assertDifferentMaster(node, oldMasterNode);
        }

        logger.info("waiting for nodes to elect a new master");
        ensureStableCluster(2, oldNonMasterNodes.get(0));

        masterNodeDisruption.stopDisrupting();
        final TimeValue waitTime = new TimeValue(DISRUPTION_HEALING_OVERHEAD.millis() + masterNodeDisruption.expectedTimeToHeal().millis());
        ensureStableCluster(3, waitTime, false, oldNonMasterNodes.get(0));

        String newMaster = internalCluster().getMasterName();
        assertThat(newMaster, not(equalTo(oldMasterNode)));
        assertMaster(newMaster, nodes);
    }

    /**
     * This test isolates the master from rest of the cluster, waits for a new master to be elected, restores the partition
     * and verifies that all node agree on the new cluster state
     */
    public void testIsolateMasterAndVerifyClusterStateConsensus() throws Exception {
        final List<String> nodes = startCluster(3);

        assertAcked(prepareCreate("test").setSettings(indexSettings(1 + randomInt(2), randomInt(2))));

        ensureGreen();
        String isolatedNode = internalCluster().getMasterName();
        TwoPartitions partitions = isolateNode(isolatedNode);
        NetworkDisruption networkDisruption = addRandomDisruptionType(partitions);
        networkDisruption.startDisrupting();

        String nonIsolatedNode = partitions.getMajoritySide().iterator().next();

        ensureStableCluster(2, nonIsolatedNode);

        assertNoMaster(isolatedNode, TimeValue.timeValueSeconds(40));

        networkDisruption.stopDisrupting();

        for (String node : nodes) {
            ensureStableCluster(
                3,
                new TimeValue(DISRUPTION_HEALING_OVERHEAD.millis() + networkDisruption.expectedTimeToHeal().millis()),
                true,
                node
            );
        }

        logger.info("issue a reroute");
        assertAcked(clusterAdmin().prepareReroute());
        ensureGreen("test");

        assertBusy(() -> {
            ClusterState state = null;
            for (String node : nodes) {
                ClusterState nodeState = getNodeClusterState(node);
                if (state == null) {
                    state = nodeState;
                    continue;
                }
                try {
                    assertEquals("unequal versions", state.version(), nodeState.version());
                    assertEquals("unequal node count", state.nodes().getSize(), nodeState.nodes().getSize());
                    assertEquals("different masters ", state.nodes().getMasterNodeId(), nodeState.nodes().getMasterNodeId());
                    assertEquals("different meta data version", state.metadata().version(), nodeState.metadata().version());
                    assertEquals("different routing", state.routingTable().toString(), nodeState.routingTable().toString());
                } catch (AssertionError t) {
                    fail(Strings.format("""
                        failed comparing cluster state: %s
                        --- cluster state of node [%s]: ---
                        %s
                        --- cluster state [%s]: ---
                        %s""", t.getMessage(), nodes.get(0), state, node, nodeState));
                }

            }
        });
    }

    /**
     * Verify that the proper block is applied when nodes lose their master
     */
    public void testVerifyApiBlocksDuringPartition() throws Exception {
        internalCluster().startNodes(3, Settings.builder().putNull(NoMasterBlockService.NO_MASTER_BLOCK_SETTING.getKey()).build());

        assertAcked(prepareCreate("test").setSettings(indexSettings(1, 2)));

        ensureGreen("test");

        TwoPartitions partitions = TwoPartitions.random(random(), internalCluster().getNodeNames());
        NetworkDisruption networkDisruption = addRandomDisruptionType(partitions);

        assertEquals(1, partitions.getMinoritySide().size());
        final String isolatedNode = partitions.getMinoritySide().iterator().next();
        assertEquals(2, partitions.getMajoritySide().size());
        final String nonIsolatedNode = partitions.getMajoritySide().iterator().next();

        networkDisruption.startDisrupting();

        logger.info("waiting for isolated node [{}] to have no master", isolatedNode);
        assertNoMaster(isolatedNode, NoMasterBlockService.NO_MASTER_BLOCK_WRITES, TimeValue.timeValueSeconds(30));

        logger.info("wait until elected master has been removed and a new 2 node cluster was from (via [{}])", isolatedNode);
        ensureStableCluster(2, nonIsolatedNode);

        for (String node : partitions.getMajoritySide()) {
            ClusterState nodeState = getNodeClusterState(node);
            boolean success = true;
            if (nodeState.nodes().getMasterNode() == null) {
                success = false;
            }
            if (nodeState.blocks().global().isEmpty() == false) {
                success = false;
            }
            if (success == false) {
                fail(Strings.format("""
                    node [%s] has no master or has blocks, despite of being on the right side of the partition. State dump:
                    %s""", node, nodeState));
            }
        }

        networkDisruption.stopDisrupting();

        ensureStableCluster(3, new TimeValue(DISRUPTION_HEALING_OVERHEAD.millis() + networkDisruption.expectedTimeToHeal().millis()));

        logger.info("Verify no master block with {} set to {}", NoMasterBlockService.NO_MASTER_BLOCK_SETTING.getKey(), "all");
        updateClusterSettings(Settings.builder().put(NoMasterBlockService.NO_MASTER_BLOCK_SETTING.getKey(), "all"));

        networkDisruption.startDisrupting();

        logger.info("waiting for isolated node [{}] to have no master", isolatedNode);
        assertNoMaster(isolatedNode, NoMasterBlockService.NO_MASTER_BLOCK_ALL, TimeValue.timeValueSeconds(30));

        ensureStableCluster(2, nonIsolatedNode);

    }

    public void testMappingTimeout() throws Exception {
        startCluster(3);
        createIndex("test", indexSettings(1, 1).put("index.routing.allocation.exclude._name", internalCluster().getMasterName()).build());

        index("test", "1", "{ \"f\": 1 }");

        ensureGreen();

        updateClusterSettings(Settings.builder().put("indices.mapping.dynamic_timeout", "1ms"));

        ServiceDisruptionScheme disruption = new BlockMasterServiceOnMaster(random());
        setDisruptionScheme(disruption);

        disruption.startDisrupting();

        BulkRequestBuilder bulk = client().prepareBulk();
        bulk.add(prepareIndex("test").setId("2").setSource("{ \"f\": 1 }", XContentType.JSON));
        bulk.add(prepareIndex("test").setId("3").setSource("{ \"g\": 1 }", XContentType.JSON));
        bulk.add(prepareIndex("test").setId("4").setSource("{ \"f\": 1 }", XContentType.JSON));
        BulkResponse bulkResponse = bulk.get();
        assertTrue(bulkResponse.hasFailures());

        disruption.stopDisrupting();

        assertBusy(() -> {
            IndicesStatsResponse stats = indicesAdmin().prepareStats("test").clear().get();
            for (ShardStats shardStats : stats.getShards()) {
                assertThat(
                    shardStats.getShardRouting().toString(),
                    shardStats.getSeqNoStats().getGlobalCheckpoint(),
                    equalTo(shardStats.getSeqNoStats().getLocalCheckpoint())
                );
            }
        });

    }
}
