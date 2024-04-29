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
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexResponse;
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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ClusterServiceUtils;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.transport.MockTransportService;
import org.elasticsearch.transport.TransportSettings;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptySet;
import static org.elasticsearch.action.DocWriteResponse.Result.CREATED;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

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

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return CollectionUtils.appendToCopy(super.nodePlugins(), MockTransportService.TestPlugin.class);
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

    public void testDeleteCreateInOneBulk() throws Exception {
        final var master = internalCluster().startMasterOnlyNode();
        final var masterClusterService = internalCluster().clusterService(master);

        final var dataNode = internalCluster().startDataOnlyNode();
        final var dataNodeClusterService = internalCluster().clusterService(dataNode);

        assertFalse(clusterAdmin().prepareHealth().setWaitForNodes("2").get().isTimedOut());
        prepareCreate("test").setSettings(Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)).get();
        ensureGreen("test");

        final var originalIndexUuid = masterClusterService.state().metadata().index("test").getIndexUUID();
        final var uuidChangedListener = ClusterServiceUtils.addTemporaryStateListener(
            dataNodeClusterService,
            clusterState -> originalIndexUuid.equals(clusterState.metadata().index("test").getIndexUUID()) == false
                && clusterState.routingTable().index("test").allShardsActive()
        );

        logger.info("--> indexing a doc");
        indexDoc("test", "1");
        refresh();

        final var dataNodeTransportService = MockTransportService.getInstance(dataNode);
        dataNodeTransportService.addRequestHandlingBehavior(
            PublicationTransportHandler.PUBLISH_STATE_ACTION_NAME,
            (handler, request, channel, task) -> channel.sendResponse(new IllegalStateException("cluster state updates blocked"))
        );

        logger.info("--> delete index");
        assertFalse(indicesAdmin().prepareDelete("test").setTimeout(TimeValue.ZERO).get().isAcknowledged());
        logger.info("--> and recreate it");
        assertFalse(
            prepareCreate("test").setSettings(
                Settings.builder()
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                    .put(IndexMetadata.SETTING_WAIT_FOR_ACTIVE_SHARDS.getKey(), "0")
            ).setTimeout(TimeValue.ZERO).get().isAcknowledged()
        );

        logger.info("--> letting cluster proceed");
        dataNodeTransportService.clearAllRules();
        publishTrivialClusterStateUpdate();

        safeAwait(uuidChangedListener);
        ensureGreen("test");
        final var finalClusterStateVersion = masterClusterService.state().version();
        assertBusy(() -> assertThat(dataNodeClusterService.state().version(), greaterThanOrEqualTo(finalClusterStateVersion)));
        assertHitCount(prepareSearch("test"), 0);
    }

    private static void publishTrivialClusterStateUpdate() {
        internalCluster().getCurrentMasterNodeInstance(ClusterService.class)
            .submitUnbatchedStateUpdateTask("trivial cluster state update", new ClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) {
                    return ClusterState.builder(currentState).build();
                }

                @Override
                public void onFailure(Exception e) {
                    fail(e);
                }
            });
    }

    public void testDelayedMappingPropagationOnPrimary() throws Exception {

        final var master = internalCluster().startMasterOnlyNode();
        final var primaryNode = internalCluster().startDataOnlyNode();

        assertAcked(prepareCreate("index").setSettings(indexSettings(1, 0)).get());
        ensureGreen();

        final var primaryNodeTransportService = MockTransportService.getInstance(primaryNode);
        primaryNodeTransportService.addRequestHandlingBehavior(
            PublicationTransportHandler.PUBLISH_STATE_ACTION_NAME,
            (handler, request, channel, task) -> channel.sendResponse(new IllegalStateException("cluster state updates blocked"))
        );

        final ActionFuture<DocWriteResponse> docIndexResponseFuture;
        try {
            assertFalse(indicesAdmin().preparePutMapping("index").setSource("field", "type=long").get().isAcknowledged());

            {
                MappingMetadata typeMappings = internalCluster().clusterService(master).state().metadata().index("index").mapping();
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
            }

            docIndexResponseFuture = prepareIndex("index").setId("1").setSource("field", 42).execute();

            Thread.sleep(100);
            assertFalse(docIndexResponseFuture.isDone());

        } finally {
            primaryNodeTransportService.clearAllRules();
            publishTrivialClusterStateUpdate();
        }
        assertEquals(1, asInstanceOf(IndexResponse.class, docIndexResponseFuture.get(10, TimeUnit.SECONDS)).getShardInfo().getTotal());
    }

    public void testDelayedMappingPropagationOnReplica() throws Exception {

        final var master = internalCluster().startMasterOnlyNode();
        final var masterClusterService = internalCluster().clusterService(master);

        final var primaryNode = internalCluster().startDataOnlyNode();
        assertAcked(prepareCreate("index").setSettings(indexSettings(1, 0)));
        ensureGreen();

        updateIndexSettings(Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1));
        final var replicaNode = internalCluster().startDataOnlyNode();
        ensureGreen();

        final var state = masterClusterService.state();
        assertEquals(master, state.nodes().getMasterNode().getName());
        List<ShardRouting> shards = state.routingTable().allShards("index");
        assertThat(shards, hasSize(2));
        for (ShardRouting shard : shards) {
            assertTrue(shard.active());
            assertEquals(shard.primary() ? primaryNode : replicaNode, state.nodes().get(shard.currentNodeId()).getName());
        }

        final var primaryIndexService = internalCluster().getInstance(IndicesService.class, primaryNode)
            .indexServiceSafe(state.metadata().index("index").getIndex());

        final var replicaNodeTransportService = MockTransportService.getInstance(replicaNode);
        replicaNodeTransportService.addRequestHandlingBehavior(
            PublicationTransportHandler.PUBLISH_STATE_ACTION_NAME,
            (handler, request, channel, task) -> channel.sendResponse(new IllegalStateException("cluster state updates blocked"))
        );

        final ActionFuture<DocWriteResponse> docIndexResponseFuture, dynamicMappingsFuture;
        try {
            assertFalse(indicesAdmin().preparePutMapping("index").setSource("field", "type=long").get().isAcknowledged());

            {
                DocumentMapper mapper = primaryIndexService.mapperService().documentMapper();
                assertNotNull(mapper);
                assertNotNull(mapper.mappers().getMapper("field"));
            }

            docIndexResponseFuture = prepareIndex("index").setId("1").setSource("field", 42).execute();

            assertBusy(() -> assertTrue(client().prepareGet("index", "1").get().isExists()));

            dynamicMappingsFuture = prepareIndex("index").setId("2").setSource("field2", 42).execute();

            assertBusy(() -> {
                DocumentMapper mapper = primaryIndexService.mapperService().documentMapper();
                assertNotNull(mapper);
                assertNotNull(mapper.mappers().getMapper("field2"));
            });

            assertBusy(() -> assertTrue(client().prepareGet("index", "2").get().isExists()));

            Thread.sleep(100);
            assertFalse(docIndexResponseFuture.isDone());
            assertFalse(dynamicMappingsFuture.isDone());
        } finally {
            replicaNodeTransportService.clearAllRules();
            publishTrivialClusterStateUpdate();
        }

        assertEquals(2, asInstanceOf(IndexResponse.class, docIndexResponseFuture.get(10, TimeUnit.SECONDS)).getShardInfo().getTotal());
        assertThat(dynamicMappingsFuture.get(30, TimeUnit.SECONDS).getResult(), equalTo(CREATED));
    }
}
