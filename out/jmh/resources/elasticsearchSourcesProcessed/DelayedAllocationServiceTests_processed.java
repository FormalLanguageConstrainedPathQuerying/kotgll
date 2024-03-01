/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.routing;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.ESAllocationTestCase;
import org.elasticsearch.cluster.TestShardRoutingRoleStrategies;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.test.NodeRoles;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singleton;
import static org.elasticsearch.cluster.routing.DelayedAllocationService.CLUSTER_UPDATE_TASK_SOURCE;
import static org.elasticsearch.cluster.routing.ShardRoutingState.STARTED;
import static org.elasticsearch.core.TimeValue.timeValueMillis;
import static org.elasticsearch.core.TimeValue.timeValueSeconds;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DelayedAllocationServiceTests extends ESAllocationTestCase {

    private TestDelayAllocationService delayedAllocationService;
    private MockAllocationService allocationService;
    private ClusterService clusterService;
    private ThreadPool threadPool;

    @Before
    public void createDelayedAllocationService() {
        threadPool = new TestThreadPool(getTestName());
        clusterService = mock(ClusterService.class);
        allocationService = createAllocationService(Settings.EMPTY, new DelayedShardsMockGatewayAllocator());
        when(clusterService.getSettings()).thenReturn(NodeRoles.masterOnlyNode());
        delayedAllocationService = new TestDelayAllocationService(threadPool, clusterService, allocationService);
        verify(clusterService).addListener(delayedAllocationService);
        verify(clusterService).getSettings();
    }

    @After
    public void shutdownThreadPool() {
        terminate(threadPool);
    }

    public void testNoDelayedUnassigned() {
        Metadata metadata = Metadata.builder()
            .put(
                IndexMetadata.builder("test")
                    .settings(settings(IndexVersion.current()).put(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(), "0"))
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .build();
        ClusterState clusterState = ClusterState.builder(ClusterName.DEFAULT)
            .metadata(metadata)
            .routingTable(RoutingTable.builder(TestShardRoutingRoleStrategies.DEFAULT_ROLE_ONLY).addAsNew(metadata.index("test")).build())
            .build();
        clusterState = ClusterState.builder(clusterState)
            .nodes(DiscoveryNodes.builder().add(newNode("node1")).add(newNode("node2")).localNodeId("node1").masterNodeId("node1"))
            .build();
        clusterState = allocationService.reroute(clusterState, "reroute", ActionListener.noop());
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        assertThat(clusterState.getRoutingNodes().unassigned().size() > 0, equalTo(false));
        ClusterState prevState = clusterState;
        DiscoveryNodes.Builder nodes = DiscoveryNodes.builder(clusterState.nodes()).remove("node2");
        boolean nodeAvailableForAllocation = randomBoolean();
        if (nodeAvailableForAllocation) {
            nodes.add(newNode("node3"));
        }
        clusterState = ClusterState.builder(clusterState).nodes(nodes).build();
        clusterState = allocationService.disassociateDeadNodes(clusterState, true, "reroute");
        ClusterState newState = clusterState;
        List<ShardRouting> unassignedShards = RoutingNodesHelper.shardsWithState(newState.getRoutingNodes(), ShardRoutingState.UNASSIGNED);
        if (nodeAvailableForAllocation) {
            assertThat(unassignedShards.size(), equalTo(0));
        } else {
            assertThat(unassignedShards.size(), equalTo(1));
            assertThat(unassignedShards.get(0).unassignedInfo().isDelayed(), equalTo(false));
        }

        delayedAllocationService.clusterChanged(new ClusterChangedEvent("test", newState, prevState));
        verifyNoMoreInteractions(clusterService);
        assertNull(delayedAllocationService.delayedRerouteTask.get());
    }

    public void testDelayedUnassignedScheduleReroute() throws Exception {
        TimeValue delaySetting = timeValueMillis(100);
        Metadata metadata = Metadata.builder()
            .put(
                IndexMetadata.builder("test")
                    .settings(
                        settings(IndexVersion.current()).put(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(), delaySetting)
                    )
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .build();
        ClusterState clusterState = ClusterState.builder(ClusterName.DEFAULT)
            .metadata(metadata)
            .routingTable(RoutingTable.builder(TestShardRoutingRoleStrategies.DEFAULT_ROLE_ONLY).addAsNew(metadata.index("test")).build())
            .build();
        clusterState = ClusterState.builder(clusterState)
            .nodes(
                DiscoveryNodes.builder()
                    .add(newNode("node0", singleton(DiscoveryNodeRole.MASTER_ROLE)))
                    .localNodeId("node0")
                    .masterNodeId("node0")
                    .add(newNode("node1"))
                    .add(newNode("node2"))
            )
            .build();
        final long baseTimestampNanos = System.nanoTime();
        allocationService.setNanoTimeOverride(baseTimestampNanos);
        clusterState = allocationService.reroute(clusterState, "reroute", ActionListener.noop());
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        assertFalse("no shards should be unassigned", clusterState.getRoutingNodes().unassigned().size() > 0);
        String nodeId = null;
        final List<ShardRouting> allShards = clusterState.getRoutingTable().allShards("test");
        for (ShardRouting shardRouting : allShards) {
            if (shardRouting.primary() == false) {
                nodeId = shardRouting.currentNodeId();
                break;
            }
        }
        assertNotNull(nodeId);

        clusterState = ClusterState.builder(clusterState).nodes(DiscoveryNodes.builder(clusterState.nodes()).remove(nodeId)).build();
        clusterState = allocationService.disassociateDeadNodes(clusterState, true, "reroute");
        ClusterState stateWithDelayedShard = clusterState;
        assertEquals(1, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithDelayedShard));
        ShardRouting delayedShard = stateWithDelayedShard.getRoutingNodes().unassigned().iterator().next();
        assertEquals(baseTimestampNanos, delayedShard.unassignedInfo().getUnassignedTimeInNanos());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ClusterStateUpdateTask> clusterStateUpdateTask = new AtomicReference<>();
        doAnswer(invocationOnMock -> {
            clusterStateUpdateTask.set((ClusterStateUpdateTask) invocationOnMock.getArguments()[1]);
            latch.countDown();
            return null;
        }).when(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), any(ClusterStateUpdateTask.class));
        assertNull(delayedAllocationService.delayedRerouteTask.get());
        long delayUntilClusterChangeEvent = TimeValue.timeValueNanos(randomInt((int) delaySetting.nanos() - 1)).nanos();
        long clusterChangeEventTimestampNanos = baseTimestampNanos + delayUntilClusterChangeEvent;
        delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
        delayedAllocationService.clusterChanged(new ClusterChangedEvent("fake node left", stateWithDelayedShard, clusterState));

        DelayedAllocationService.DelayedRerouteTask delayedRerouteTask = delayedAllocationService.delayedRerouteTask.get();
        assertNotNull(delayedRerouteTask);
        assertFalse(delayedRerouteTask.cancelScheduling.get());
        assertThat(delayedRerouteTask.baseTimestampNanos, equalTo(clusterChangeEventTimestampNanos));
        assertThat(
            delayedRerouteTask.nextDelay.nanos(),
            equalTo(delaySetting.nanos() - (clusterChangeEventTimestampNanos - baseTimestampNanos))
        );

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), eq(clusterStateUpdateTask.get()));

        long nanoTimeForReroute = clusterChangeEventTimestampNanos + delaySetting.nanos() + timeValueMillis(randomInt(200)).nanos();
        allocationService.setNanoTimeOverride(nanoTimeForReroute);
        ClusterState stateWithRemovedDelay = clusterStateUpdateTask.get().execute(stateWithDelayedShard);
        assertEquals(0, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithRemovedDelay));
        assertNull(delayedAllocationService.delayedRerouteTask.get());

        delayedAllocationService.setNanoTimeOverride(nanoTimeForReroute + timeValueMillis(randomInt(200)).nanos());
        delayedAllocationService.clusterChanged(
            new ClusterChangedEvent(CLUSTER_UPDATE_TASK_SOURCE, stateWithRemovedDelay, stateWithDelayedShard)
        );
        assertNull(delayedAllocationService.delayedRerouteTask.get());
        verifyNoMoreInteractions(clusterService);
    }

    /**
     * This tests that a new delayed reroute is scheduled right after a delayed reroute was run
     */
    public void testDelayedUnassignedScheduleRerouteAfterDelayedReroute() throws Exception {
        TimeValue shortDelaySetting = timeValueMillis(100);
        TimeValue longDelaySetting = TimeValue.timeValueSeconds(1);
        Metadata metadata = Metadata.builder()
            .put(
                IndexMetadata.builder("short_delay")
                    .settings(
                        settings(IndexVersion.current()).put(
                            UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(),
                            shortDelaySetting
                        )
                    )
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .put(
                IndexMetadata.builder("long_delay")
                    .settings(
                        settings(IndexVersion.current()).put(
                            UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(),
                            longDelaySetting
                        )
                    )
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .build();
        ClusterState clusterState = ClusterState.builder(ClusterName.DEFAULT)
            .metadata(metadata)
            .routingTable(
                RoutingTable.builder(TestShardRoutingRoleStrategies.DEFAULT_ROLE_ONLY)
                    .addAsNew(metadata.index("short_delay"))
                    .addAsNew(metadata.index("long_delay"))
                    .build()
            )
            .nodes(
                DiscoveryNodes.builder()
                    .add(newNode("node0", singleton(DiscoveryNodeRole.MASTER_ROLE)))
                    .localNodeId("node0")
                    .masterNodeId("node0")
                    .add(newNode("node1"))
                    .add(newNode("node2"))
                    .add(newNode("node3"))
                    .add(newNode("node4"))
            )
            .build();
        clusterState = allocationService.reroute(clusterState, "reroute", ActionListener.noop());
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        assertThat(
            "all shards should be started",
            RoutingNodesHelper.shardsWithState(clusterState.getRoutingNodes(), STARTED).size(),
            equalTo(4)
        );

        ShardRouting shortDelayReplica = null;
        for (ShardRouting shardRouting : clusterState.getRoutingTable().allShards("short_delay")) {
            if (shardRouting.primary() == false) {
                shortDelayReplica = shardRouting;
                break;
            }
        }
        assertNotNull(shortDelayReplica);

        ShardRouting longDelayReplica = null;
        for (ShardRouting shardRouting : clusterState.getRoutingTable().allShards("long_delay")) {
            if (shardRouting.primary() == false) {
                longDelayReplica = shardRouting;
                break;
            }
        }
        assertNotNull(longDelayReplica);

        final long baseTimestampNanos = System.nanoTime();

        ClusterState clusterStateBeforeNodeLeft = clusterState;
        clusterState = ClusterState.builder(clusterState)
            .nodes(
                DiscoveryNodes.builder(clusterState.nodes())
                    .remove(shortDelayReplica.currentNodeId())
                    .remove(longDelayReplica.currentNodeId())
            )
            .build();
        allocationService.setNanoTimeOverride(baseTimestampNanos);
        clusterState = allocationService.disassociateDeadNodes(clusterState, true, "reroute");
        final ClusterState stateWithDelayedShards = clusterState;
        assertEquals(2, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithDelayedShards));
        RoutingNodes.UnassignedShards.UnassignedIterator iter = stateWithDelayedShards.getRoutingNodes().unassigned().iterator();
        assertEquals(baseTimestampNanos, iter.next().unassignedInfo().getUnassignedTimeInNanos());
        assertEquals(baseTimestampNanos, iter.next().unassignedInfo().getUnassignedTimeInNanos());

        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<ClusterStateUpdateTask> clusterStateUpdateTask1 = new AtomicReference<>();
        doAnswer(invocationOnMock -> {
            clusterStateUpdateTask1.set((ClusterStateUpdateTask) invocationOnMock.getArguments()[1]);
            latch1.countDown();
            return null;
        }).when(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), any(ClusterStateUpdateTask.class));
        assertNull(delayedAllocationService.delayedRerouteTask.get());
        long delayUntilClusterChangeEvent = TimeValue.timeValueNanos(randomInt((int) shortDelaySetting.nanos() - 1)).nanos();
        long clusterChangeEventTimestampNanos = baseTimestampNanos + delayUntilClusterChangeEvent;
        delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
        delayedAllocationService.clusterChanged(
            new ClusterChangedEvent("fake node left", stateWithDelayedShards, clusterStateBeforeNodeLeft)
        );

        DelayedAllocationService.DelayedRerouteTask firstDelayedRerouteTask = delayedAllocationService.delayedRerouteTask.get();
        assertNotNull(firstDelayedRerouteTask);
        assertFalse(firstDelayedRerouteTask.cancelScheduling.get());
        assertThat(firstDelayedRerouteTask.baseTimestampNanos, equalTo(clusterChangeEventTimestampNanos));
        assertThat(
            firstDelayedRerouteTask.nextDelay.nanos(),
            equalTo(UnassignedInfo.findNextDelayedAllocation(clusterChangeEventTimestampNanos, stateWithDelayedShards))
        );
        assertThat(
            firstDelayedRerouteTask.nextDelay.nanos(),
            equalTo(shortDelaySetting.nanos() - (clusterChangeEventTimestampNanos - baseTimestampNanos))
        );

        assertTrue(latch1.await(30, TimeUnit.SECONDS));
        verify(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), eq(clusterStateUpdateTask1.get()));

        long nanoTimeForReroute = clusterChangeEventTimestampNanos + shortDelaySetting.nanos() + timeValueMillis(randomInt(50)).nanos();
        allocationService.setNanoTimeOverride(nanoTimeForReroute);
        ClusterState stateWithOnlyOneDelayedShard = clusterStateUpdateTask1.get().execute(stateWithDelayedShards);
        assertEquals(1, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithOnlyOneDelayedShard));
        assertNull(delayedAllocationService.delayedRerouteTask.get());

        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<ClusterStateUpdateTask> clusterStateUpdateTask2 = new AtomicReference<>();
        doAnswer(invocationOnMock -> {
            clusterStateUpdateTask2.set((ClusterStateUpdateTask) invocationOnMock.getArguments()[1]);
            latch2.countDown();
            return null;
        }).when(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), any(ClusterStateUpdateTask.class));
        delayUntilClusterChangeEvent = timeValueMillis(randomInt(50)).nanos();
        clusterChangeEventTimestampNanos = nanoTimeForReroute + delayUntilClusterChangeEvent;
        delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
        delayedAllocationService.clusterChanged(
            new ClusterChangedEvent(CLUSTER_UPDATE_TASK_SOURCE, stateWithOnlyOneDelayedShard, stateWithDelayedShards)
        );

        DelayedAllocationService.DelayedRerouteTask secondDelayedRerouteTask = delayedAllocationService.delayedRerouteTask.get();
        assertNotNull(secondDelayedRerouteTask);
        assertFalse(secondDelayedRerouteTask.cancelScheduling.get());
        assertThat(secondDelayedRerouteTask.baseTimestampNanos, equalTo(clusterChangeEventTimestampNanos));
        assertThat(
            secondDelayedRerouteTask.nextDelay.nanos(),
            equalTo(UnassignedInfo.findNextDelayedAllocation(clusterChangeEventTimestampNanos, stateWithOnlyOneDelayedShard))
        );
        assertThat(
            secondDelayedRerouteTask.nextDelay.nanos(),
            equalTo(longDelaySetting.nanos() - (clusterChangeEventTimestampNanos - baseTimestampNanos))
        );

        assertTrue(latch2.await(30, TimeUnit.SECONDS));
        verify(clusterService).submitUnbatchedStateUpdateTask(eq(CLUSTER_UPDATE_TASK_SOURCE), eq(clusterStateUpdateTask2.get()));

        nanoTimeForReroute = clusterChangeEventTimestampNanos + longDelaySetting.nanos() + timeValueMillis(randomInt(50)).nanos();
        allocationService.setNanoTimeOverride(nanoTimeForReroute);
        ClusterState stateWithNoDelayedShards = clusterStateUpdateTask2.get().execute(stateWithOnlyOneDelayedShard);
        assertEquals(0, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithNoDelayedShards));
        assertNull(delayedAllocationService.delayedRerouteTask.get());

        delayedAllocationService.setNanoTimeOverride(nanoTimeForReroute + timeValueMillis(randomInt(50)).nanos());
        delayedAllocationService.clusterChanged(
            new ClusterChangedEvent(CLUSTER_UPDATE_TASK_SOURCE, stateWithNoDelayedShards, stateWithOnlyOneDelayedShard)
        );
        assertNull(delayedAllocationService.delayedRerouteTask.get());
        verifyNoMoreInteractions(clusterService);
    }

    public void testDelayedUnassignedScheduleRerouteRescheduledOnShorterDelay() {
        TimeValue delaySetting = timeValueSeconds(30);
        TimeValue shorterDelaySetting = timeValueMillis(100);
        Metadata metadata = Metadata.builder()
            .put(
                IndexMetadata.builder("foo")
                    .settings(
                        settings(IndexVersion.current()).put(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(), delaySetting)
                    )
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .put(
                IndexMetadata.builder("bar")
                    .settings(
                        settings(IndexVersion.current()).put(
                            UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(),
                            shorterDelaySetting
                        )
                    )
                    .numberOfShards(1)
                    .numberOfReplicas(1)
            )
            .build();
        ClusterState clusterState = ClusterState.builder(ClusterName.DEFAULT)
            .metadata(metadata)
            .routingTable(
                RoutingTable.builder(TestShardRoutingRoleStrategies.DEFAULT_ROLE_ONLY)
                    .addAsNew(metadata.index("foo"))
                    .addAsNew(metadata.index("bar"))
                    .build()
            )
            .build();
        clusterState = ClusterState.builder(clusterState)
            .nodes(
                DiscoveryNodes.builder()
                    .add(newNode("node0", singleton(DiscoveryNodeRole.MASTER_ROLE)))
                    .localNodeId("node0")
                    .masterNodeId("node0")
                    .add(newNode("node1"))
                    .add(newNode("node2"))
                    .add(newNode("node3"))
                    .add(newNode("node4"))
            )
            .build();
        final long nodeLeftTimestampNanos = System.nanoTime();
        allocationService.setNanoTimeOverride(nodeLeftTimestampNanos);
        clusterState = allocationService.reroute(clusterState, "reroute", ActionListener.noop());
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        clusterState = startInitializingShardsAndReroute(allocationService, clusterState);
        assertFalse("no shards should be unassigned", clusterState.getRoutingNodes().unassigned().size() > 0);
        String nodeIdOfFooReplica = null;
        for (ShardRouting shardRouting : clusterState.getRoutingTable().allShards("foo")) {
            if (shardRouting.primary() == false) {
                nodeIdOfFooReplica = shardRouting.currentNodeId();
                break;
            }
        }
        assertNotNull(nodeIdOfFooReplica);

        clusterState = ClusterState.builder(clusterState)
            .nodes(DiscoveryNodes.builder(clusterState.nodes()).remove(nodeIdOfFooReplica))
            .build();
        clusterState = allocationService.disassociateDeadNodes(clusterState, true, "fake node left");
        ClusterState stateWithDelayedShard = clusterState;
        assertEquals(1, UnassignedInfo.getNumberOfDelayedUnassigned(stateWithDelayedShard));
        ShardRouting delayedShard = stateWithDelayedShard.getRoutingNodes().unassigned().iterator().next();
        assertEquals(nodeLeftTimestampNanos, delayedShard.unassignedInfo().getUnassignedTimeInNanos());

        assertNull(delayedAllocationService.delayedRerouteTask.get());
        long delayUntilClusterChangeEvent = TimeValue.timeValueNanos(randomInt((int) shorterDelaySetting.nanos() - 1)).nanos();
        long clusterChangeEventTimestampNanos = nodeLeftTimestampNanos + delayUntilClusterChangeEvent;
        delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
        delayedAllocationService.clusterChanged(new ClusterChangedEvent("fake node left", stateWithDelayedShard, clusterState));

        DelayedAllocationService.DelayedRerouteTask delayedRerouteTask = delayedAllocationService.delayedRerouteTask.get();
        assertNotNull(delayedRerouteTask);
        assertFalse(delayedRerouteTask.cancelScheduling.get());
        assertThat(delayedRerouteTask.baseTimestampNanos, equalTo(clusterChangeEventTimestampNanos));
        assertThat(
            delayedRerouteTask.nextDelay.nanos(),
            equalTo(delaySetting.nanos() - (clusterChangeEventTimestampNanos - nodeLeftTimestampNanos))
        );

        if (randomBoolean()) {
            ClusterState stateWithShorterDelay = ClusterState.builder(stateWithDelayedShard)
                .metadata(
                    Metadata.builder(stateWithDelayedShard.metadata())
                        .updateSettings(
                            Settings.builder()
                                .put(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(), shorterDelaySetting)
                                .build(),
                            "foo"
                        )
                )
                .build();
            delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
            delayedAllocationService.clusterChanged(
                new ClusterChangedEvent("apply shorter delay", stateWithShorterDelay, stateWithDelayedShard)
            );
        } else {
            String nodeIdOfBarReplica = null;
            for (ShardRouting shardRouting : stateWithDelayedShard.getRoutingTable().allShards("bar")) {
                if (shardRouting.primary() == false) {
                    nodeIdOfBarReplica = shardRouting.currentNodeId();
                    break;
                }
            }
            assertNotNull(nodeIdOfBarReplica);

            clusterState = ClusterState.builder(stateWithDelayedShard)
                .nodes(DiscoveryNodes.builder(stateWithDelayedShard.nodes()).remove(nodeIdOfBarReplica))
                .build();
            ClusterState stateWithShorterDelay = allocationService.disassociateDeadNodes(clusterState, true, "fake node left");
            delayedAllocationService.setNanoTimeOverride(clusterChangeEventTimestampNanos);
            delayedAllocationService.clusterChanged(
                new ClusterChangedEvent("fake node left", stateWithShorterDelay, stateWithDelayedShard)
            );
        }

        DelayedAllocationService.DelayedRerouteTask shorterDelayedRerouteTask = delayedAllocationService.delayedRerouteTask.get();
        assertNotNull(shorterDelayedRerouteTask);
        assertNotEquals(shorterDelayedRerouteTask, delayedRerouteTask);
        assertTrue(delayedRerouteTask.cancelScheduling.get()); 
        assertFalse(shorterDelayedRerouteTask.cancelScheduling.get());
        assertThat(delayedRerouteTask.baseTimestampNanos, equalTo(clusterChangeEventTimestampNanos));
        assertThat(
            shorterDelayedRerouteTask.nextDelay.nanos(),
            equalTo(shorterDelaySetting.nanos() - (clusterChangeEventTimestampNanos - nodeLeftTimestampNanos))
        );
    }

    private static class TestDelayAllocationService extends DelayedAllocationService {
        private volatile long nanoTimeOverride = -1L;

        private TestDelayAllocationService(ThreadPool threadPool, ClusterService clusterService, AllocationService allocationService) {
            super(threadPool, clusterService, allocationService);
        }

        @Override
        protected void assertClusterOrMasterStateThread() {
        }

        public void setNanoTimeOverride(long nanoTime) {
            this.nanoTimeOverride = nanoTime;
        }

        @Override
        protected long currentNanoTime() {
            return nanoTimeOverride == -1L ? super.currentNanoTime() : nanoTimeOverride;
        }
    }
}
