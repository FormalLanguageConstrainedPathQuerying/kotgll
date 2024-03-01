/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.coordination;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.RequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.TestShardRoutingRoleStrategies;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodeUtils;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.disruption.BlockClusterStateProcessing;
import org.elasticsearch.transport.TransportSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptySet;
import static org.elasticsearch.action.DocWriteResponse.Result.CREATED;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0, numClientNodes = 0)
public class RareClusterStateIT extends ESIntegTestCase {

    @Override
    protected int numberOfShards() {
        return 1;
    }

    @Override
    protected int numberOfReplicas() {
        return 0;
    }

    public void testAssignmentWithJustAddedNodes() {
        internalCluster().startNode(Settings.builder().put(TransportSettings.CONNECT_TIMEOUT.getKey(), "1s"));
        final String index = "index";
        prepareCreate(index).setSettings(indexSettings(1, 0)).get();
        ensureGreen(index);

        indicesAdmin().prepareClose(index).get();

        final String masterName = internalCluster().getMasterName();
        final ClusterService clusterService = internalCluster().clusterService(masterName);
        final AllocationService allocationService = internalCluster().getInstance(AllocationService.class, masterName);
        clusterService.submitUnbatchedStateUpdateTask("test-inject-node-and-reroute", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                ClusterState.Builder builder = ClusterState.builder(currentState);
                builder.nodes(
                    DiscoveryNodes.builder(currentState.nodes()).add(DiscoveryNodeUtils.builder("_non_existent").roles(emptySet()).build())
                );

                final IndexMetadata indexMetadata = IndexMetadata.builder(currentState.metadata().index(index))
                    .state(IndexMetadata.State.OPEN)
                    .build();

                builder.metadata(Metadata.builder(currentState.metadata()).put(indexMetadata, true));
                builder.blocks(ClusterBlocks.builder().blocks(currentState.blocks()).removeIndexBlocks(index));
                ClusterState updatedState = builder.build();

                RoutingTable.Builder routingTable = RoutingTable.builder(
                    TestShardRoutingRoleStrategies.DEFAULT_ROLE_ONLY,
                    updatedState.routingTable()
                );
                routingTable.addAsRecovery(updatedState.metadata().index(index));
                updatedState = ClusterState.builder(updatedState).routingTable(routingTable.build()).build();

                return allocationService.reroute(updatedState, "reroute", ActionListener.noop());
            }

            @Override
            public void onFailure(Exception e) {}
        });
        ensureGreen(index);
        clusterService.submitUnbatchedStateUpdateTask("test-remove-injected-node", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                ClusterState.Builder builder = ClusterState.builder(currentState);
                builder.nodes(DiscoveryNodes.builder(currentState.nodes()).remove("_non_existent"));

                currentState = builder.build();
                return allocationService.disassociateDeadNodes(currentState, true, "reroute");
            }

            @Override
            public void onFailure(Exception e) {}
        });
    }

    private <Req extends ActionRequest, Res extends ActionResponse> ActionFuture<Res> executeAndCancelCommittedPublication(
        RequestBuilder<Req, Res> req
    ) throws Exception {
        final Coordinator masterCoordinator = internalCluster().getCurrentMasterNodeInstance(Coordinator.class);

        ensureNoPendingMasterTasks().actionGet(TimeValue.timeValueSeconds(30));
        ActionFuture<Res> future = req.execute();

        assertBusy(() -> assertTrue(masterCoordinator.cancelCommittedPublication()));
        var task = ensureNoPendingMasterTasks();
        while (task.isDone() == false) {
            masterCoordinator.cancelCommittedPublication();
            Thread.onSpinWait();
        }
        task.actionGet(TimeValue.timeValueSeconds(30));

        return future;
    }

    private PlainActionFuture<Void> ensureNoPendingMasterTasks() {
        var future = new PlainActionFuture<Void>();
        internalCluster().getCurrentMasterNodeInstance(ClusterService.class)
            .submitUnbatchedStateUpdateTask(
                "ensureNoPendingMasterTasks",
                new ClusterStateUpdateTask(Priority.LANGUID, TimeValue.timeValueSeconds(30)) {

                    @Override
                    public ClusterState execute(ClusterState currentState) {
                        return currentState;
                    }

                    @Override
                    public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                        future.onResponse(null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        future.onFailure(e);
                    }
                }
            );
        return future;
    }

    public void testDeleteCreateInOneBulk() throws Exception {
        internalCluster().startMasterOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        assertFalse(clusterAdmin().prepareHealth().setWaitForNodes("2").get().isTimedOut());
        prepareCreate("test").setSettings(Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)).get();
        ensureGreen("test");

        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(dataNode, random());
        internalCluster().setDisruptionScheme(disruption);
        logger.info("--> indexing a doc");
        indexDoc("test", "1");
        refresh();
        disruption.startDisrupting();
        logger.info("--> delete index");
        executeAndCancelCommittedPublication(indicesAdmin().prepareDelete("test").setTimeout("0s")).get(30, TimeUnit.SECONDS);
        logger.info("--> and recreate it");
        executeAndCancelCommittedPublication(
            prepareCreate("test").setSettings(
                Settings.builder()
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                    .put(IndexMetadata.SETTING_WAIT_FOR_ACTIVE_SHARDS.getKey(), "0")
            ).setTimeout("0s")
        ).get(30, TimeUnit.SECONDS);

        logger.info("--> letting cluster proceed");

        disruption.stopDisrupting();
        ensureGreen(TimeValue.timeValueMinutes(30), "test");
        assertBusy(() -> {
            long masterClusterStateVersion = internalCluster().clusterService(internalCluster().getMasterName()).state().version();
            long dataClusterStateVersion = internalCluster().clusterService(dataNode).state().version();
            assertThat(masterClusterStateVersion, equalTo(dataClusterStateVersion));
        });
        assertHitCount(prepareSearch("test"), 0);
    }

    public void testDelayedMappingPropagationOnPrimary() throws Exception {

        final List<String> nodeNames = internalCluster().startNodes(2);
        assertFalse(clusterAdmin().prepareHealth().setWaitForNodes("2").get().isTimedOut());

        final String master = internalCluster().getMasterName();
        assertThat(nodeNames, hasItem(master));
        String otherNode = null;
        for (String node : nodeNames) {
            if (node.equals(master) == false) {
                otherNode = node;
                break;
            }
        }
        assertNotNull(otherNode);

        assertAcked(prepareCreate("index").setSettings(indexSettings(1, 0).put("index.routing.allocation.exclude._name", master)).get());
        ensureGreen();

        ClusterState state = clusterAdmin().prepareState().get().getState();
        assertEquals(master, state.nodes().getMasterNode().getName());
        List<ShardRouting> shards = state.routingTable().allShards("index");
        assertThat(shards, hasSize(1));
        for (ShardRouting shard : shards) {
            if (shard.primary()) {
                assertFalse(state.nodes().getMasterNodeId().equals(shard.currentNodeId()));
            } else {
                fail(); 
            }
        }

        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(otherNode, random());
        internalCluster().setDisruptionScheme(disruption);
        disruption.startDisrupting();

        ActionFuture<AcknowledgedResponse> putMappingResponse = executeAndCancelCommittedPublication(
            indicesAdmin().preparePutMapping("index").setSource("field", "type=long")
        );

        assertBusy(() -> {
            MappingMetadata typeMappings = indicesAdmin().prepareGetMappings("index").get().getMappings().get("index");
            assertNotNull(typeMappings);
            Object properties;
            try {
                properties = typeMappings.getSourceAsMap().get("properties");
            } catch (ElasticsearchParseException e) {
                throw new AssertionError(e);
            }
            assertNotNull(properties);
            @SuppressWarnings("unchecked")
            Object fieldMapping = ((Map<String, Object>) properties).get("field");
            assertNotNull(fieldMapping);
        });

        ActionFuture<DocWriteResponse> docIndexResponse = prepareIndex("index").setId("1").setSource("field", 42).execute();

        Thread.sleep(100);
        assertFalse(putMappingResponse.isDone());
        assertFalse(docIndexResponse.isDone());

        disruption.stopDisrupting();
        assertTrue(putMappingResponse.get(30, TimeUnit.SECONDS).isAcknowledged());
        assertThat(docIndexResponse.get(10, TimeUnit.SECONDS), instanceOf(IndexResponse.class));
        assertEquals(1, docIndexResponse.get(10, TimeUnit.SECONDS).getShardInfo().getTotal());
    }

    public void testDelayedMappingPropagationOnReplica() throws Exception {
        final List<String> nodeNames = internalCluster().startNodes(2);
        assertFalse(clusterAdmin().prepareHealth().setWaitForNodes("2").get().isTimedOut());

        final String master = internalCluster().getMasterName();
        assertThat(nodeNames, hasItem(master));
        String otherNode = null;
        for (String node : nodeNames) {
            if (node.equals(master) == false) {
                otherNode = node;
                break;
            }
        }
        assertNotNull(otherNode);

        prepareCreate("index").setSettings(indexSettings(1, 1).put("index.routing.allocation.include._name", master)).get();
        updateIndexSettings(Settings.builder().put("index.routing.allocation.include._name", ""), "index");
        ensureGreen();

        ClusterState state = clusterAdmin().prepareState().get().getState();
        assertEquals(master, state.nodes().getMasterNode().getName());
        List<ShardRouting> shards = state.routingTable().allShards("index");
        assertThat(shards, hasSize(2));
        for (ShardRouting shard : shards) {
            if (shard.primary()) {
                assertEquals(state.nodes().getMasterNodeId(), shard.currentNodeId());
            } else {
                assertTrue(shard.active());
            }
        }

        BlockClusterStateProcessing disruption = new BlockClusterStateProcessing(otherNode, random());
        internalCluster().setDisruptionScheme(disruption);
        disruption.startDisrupting();
        final ActionFuture<AcknowledgedResponse> putMappingResponse = executeAndCancelCommittedPublication(
            indicesAdmin().preparePutMapping("index").setSource("field", "type=long")
        );

        final Index index = resolveIndex("index");
        assertBusy(() -> {
            final IndicesService indicesService = internalCluster().getInstance(IndicesService.class, master);
            final IndexService indexService = indicesService.indexServiceSafe(index);
            assertNotNull(indexService);
            final MapperService mapperService = indexService.mapperService();
            DocumentMapper mapper = mapperService.documentMapper();
            assertNotNull(mapper);
            assertNotNull(mapper.mappers().getMapper("field"));
        });

        assertBusy(() -> {
            long minVersion = Long.MAX_VALUE;
            long maxVersion = Long.MIN_VALUE;
            for (final var coordinator : internalCluster().getInstances(Coordinator.class)) {
                final var clusterStateVersion = coordinator.getApplierState().version();
                minVersion = Math.min(minVersion, clusterStateVersion);
                maxVersion = Math.max(maxVersion, clusterStateVersion);
            }
            assertEquals(minVersion, maxVersion);
        });

        final ActionFuture<DocWriteResponse> docIndexResponse = prepareIndex("index").setId("1").setSource("field", 42).execute();

        assertBusy(() -> assertTrue(client().prepareGet("index", "1").get().isExists()));

        ActionFuture<DocWriteResponse> dynamicMappingsFut = executeAndCancelCommittedPublication(
            prepareIndex("index").setId("2").setSource("field2", 42)
        );

        assertBusy(() -> {
            final IndicesService indicesService = internalCluster().getInstance(IndicesService.class, master);
            final IndexService indexService = indicesService.indexServiceSafe(index);
            assertNotNull(indexService);
            final MapperService mapperService = indexService.mapperService();
            DocumentMapper mapper = mapperService.documentMapper();
            assertNotNull(mapper);
            assertNotNull(mapper.mappers().getMapper("field2"));
        });

        assertBusy(() -> assertTrue(client().prepareGet("index", "2").get().isExists()));

        Thread.sleep(100);
        assertFalse(putMappingResponse.isDone());
        assertFalse(docIndexResponse.isDone());

        disruption.stopDisrupting();
        assertTrue(putMappingResponse.get(30, TimeUnit.SECONDS).isAcknowledged());
        assertThat(docIndexResponse.get(10, TimeUnit.SECONDS), instanceOf(IndexResponse.class));
        assertEquals(2, docIndexResponse.get(10, TimeUnit.SECONDS).getShardInfo().getTotal()); 

        assertThat(dynamicMappingsFut.get(30, TimeUnit.SECONDS).getResult(), equalTo(CREATED));
    }

}
