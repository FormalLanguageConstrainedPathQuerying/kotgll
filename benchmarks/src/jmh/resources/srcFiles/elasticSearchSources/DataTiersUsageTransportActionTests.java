/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.datatiers;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.allocation.DataTier;
import org.elasticsearch.search.aggregations.metrics.TDigestState;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.elasticsearch.xpack.core.datatiers.DataTierUsageFixtures.indexMetadata;
import static org.elasticsearch.xpack.core.datatiers.DataTierUsageFixtures.newNode;
import static org.elasticsearch.xpack.core.datatiers.DataTierUsageFixtures.routeTestShardToNodes;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class DataTiersUsageTransportActionTests extends ESTestCase {

    private long byteSize;
    private long docCount;

    @Before
    public void setup() {
        byteSize = randomLongBetween(1024L, 1024L * 1024L * 1024L * 30L); 
        docCount = randomLongBetween(100L, 100000000L); 
    }

    public void testTierIndices() {
        DiscoveryNode dataNode = newNode(0, DiscoveryNodeRole.DATA_ROLE);
        DiscoveryNodes.Builder discoBuilder = DiscoveryNodes.builder();
        discoBuilder.add(dataNode);

        IndexMetadata hotIndex1 = indexMetadata("hot-1", 1, 0, DataTier.DATA_HOT);
        IndexMetadata hotIndex2 = indexMetadata("hot-2", 1, 0, DataTier.DATA_HOT);
        IndexMetadata warmIndex1 = indexMetadata("warm-1", 1, 0, DataTier.DATA_WARM);
        IndexMetadata coldIndex1 = indexMetadata("cold-1", 1, 0, DataTier.DATA_COLD);
        IndexMetadata coldIndex2 = indexMetadata("cold-2", 1, 0, DataTier.DATA_COLD, DataTier.DATA_WARM); 
        IndexMetadata nonTiered = indexMetadata("non-tier", 1, 0); 
        IndexMetadata hotIndex3 = indexMetadata("hot-3", 1, 0, DataTier.DATA_HOT);

        Metadata.Builder metadataBuilder = Metadata.builder()
            .put(hotIndex1, false)
            .put(hotIndex2, false)
            .put(warmIndex1, false)
            .put(coldIndex1, false)
            .put(coldIndex2, false)
            .put(nonTiered, false)
            .put(hotIndex3, false)
            .generateClusterUuidIfNeeded();
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();
        routingTableBuilder.add(getIndexRoutingTable(hotIndex1, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(hotIndex2, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(hotIndex2, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(warmIndex1, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(coldIndex1, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(coldIndex2, dataNode));
        routingTableBuilder.add(getIndexRoutingTable(nonTiered, dataNode));
        ClusterState clusterState = ClusterState.builder(new ClusterName("test"))
            .nodes(discoBuilder)
            .metadata(metadataBuilder)
            .routingTable(routingTableBuilder.build())
            .build();
        Map<String, Set<String>> result = DataTiersUsageTransportAction.getIndicesGroupedByTier(
            clusterState,
            List.of(new NodeDataTiersUsage(dataNode, Map.of(DataTier.DATA_WARM, createStats(5, 5, 0, 10))))
        );
        assertThat(result.keySet(), equalTo(Set.of(DataTier.DATA_HOT, DataTier.DATA_WARM, DataTier.DATA_COLD)));
        assertThat(result.get(DataTier.DATA_HOT), equalTo(Set.of(hotIndex1.getIndex().getName(), hotIndex2.getIndex().getName())));
        assertThat(result.get(DataTier.DATA_WARM), equalTo(Set.of(warmIndex1.getIndex().getName())));
        assertThat(result.get(DataTier.DATA_COLD), equalTo(Set.of(coldIndex1.getIndex().getName(), coldIndex2.getIndex().getName())));
    }

    public void testCalculateMAD() {
        assertThat(DataTiersUsageTransportAction.computeMedianAbsoluteDeviation(TDigestState.create(10)), equalTo(0L));

        TDigestState sketch = TDigestState.create(randomDoubleBetween(1, 1000, false));
        sketch.add(1);
        sketch.add(1);
        sketch.add(2);
        sketch.add(2);
        sketch.add(4);
        sketch.add(6);
        sketch.add(9);
        assertThat(DataTiersUsageTransportAction.computeMedianAbsoluteDeviation(sketch), equalTo(1L));
    }

    public void testCalculateStatsNoTiers() {
        DiscoveryNode leader = newNode(0, DiscoveryNodeRole.MASTER_ROLE);
        DiscoveryNode dataNode1 = newNode(1, DiscoveryNodeRole.DATA_ROLE);

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(dataNode1, Map.of())
        );
        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of()
        );

        assertThat(tierSpecificStats.size(), is(0));
    }

    public void testCalculateStatsTieredNodesOnly() {
        DiscoveryNode leader = newNode(0, DiscoveryNodeRole.MASTER_ROLE);
        DiscoveryNode dataNode1 = newNode(1, DiscoveryNodeRole.DATA_ROLE);
        DiscoveryNode hotNode1 = newNode(2, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode warmNode1 = newNode(3, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode coldNode1 = newNode(4, DiscoveryNodeRole.DATA_COLD_NODE_ROLE);
        DiscoveryNode frozenNode1 = newNode(5, DiscoveryNodeRole.DATA_FROZEN_NODE_ROLE);

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(dataNode1, Map.of()),
            new NodeDataTiersUsage(hotNode1, Map.of()),
            new NodeDataTiersUsage(warmNode1, Map.of()),
            new NodeDataTiersUsage(coldNode1, Map.of()),
            new NodeDataTiersUsage(frozenNode1, Map.of())
        );

        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of()
        );

        assertThat(tierSpecificStats.size(), is(4));

        DataTiersFeatureSetUsage.TierSpecificStats hotStats = tierSpecificStats.get(DataTier.DATA_HOT);
        assertThat(hotStats, is(notNullValue()));
        assertThat(hotStats.nodeCount, is(1));
        assertThat(hotStats.indexCount, is(0));
        assertThat(hotStats.totalShardCount, is(0));
        assertThat(hotStats.docCount, is(0L));
        assertThat(hotStats.totalByteCount, is(0L));
        assertThat(hotStats.primaryShardCount, is(0));
        assertThat(hotStats.primaryByteCount, is(0L));
        assertThat(hotStats.primaryByteCountMedian, is(0L)); 
        assertThat(hotStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats warmStats = tierSpecificStats.get(DataTier.DATA_WARM);
        assertThat(warmStats, is(notNullValue()));
        assertThat(warmStats.nodeCount, is(1));
        assertThat(warmStats.indexCount, is(0));
        assertThat(warmStats.totalShardCount, is(0));
        assertThat(warmStats.docCount, is(0L));
        assertThat(warmStats.totalByteCount, is(0L));
        assertThat(warmStats.primaryShardCount, is(0));
        assertThat(warmStats.primaryByteCount, is(0L));
        assertThat(warmStats.primaryByteCountMedian, is(0L)); 
        assertThat(warmStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats coldStats = tierSpecificStats.get(DataTier.DATA_COLD);
        assertThat(coldStats, is(notNullValue()));
        assertThat(coldStats.nodeCount, is(1));
        assertThat(coldStats.indexCount, is(0));
        assertThat(coldStats.totalShardCount, is(0));
        assertThat(coldStats.docCount, is(0L));
        assertThat(coldStats.totalByteCount, is(0L));
        assertThat(coldStats.primaryShardCount, is(0));
        assertThat(coldStats.primaryByteCount, is(0L));
        assertThat(coldStats.primaryByteCountMedian, is(0L)); 
        assertThat(coldStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats frozenStats = tierSpecificStats.get(DataTier.DATA_FROZEN);
        assertThat(frozenStats, is(notNullValue()));
        assertThat(frozenStats.nodeCount, is(1));
        assertThat(frozenStats.indexCount, is(0));
        assertThat(frozenStats.totalShardCount, is(0));
        assertThat(frozenStats.docCount, is(0L));
        assertThat(frozenStats.totalByteCount, is(0L));
        assertThat(frozenStats.primaryShardCount, is(0));
        assertThat(frozenStats.primaryByteCount, is(0L));
        assertThat(frozenStats.primaryByteCountMedian, is(0L)); 
        assertThat(frozenStats.primaryShardBytesMAD, is(0L)); 
    }

    public void testCalculateStatsTieredIndicesOnly() {
        int nodeId = 0;
        DiscoveryNode leader = newNode(nodeId++, DiscoveryNodeRole.MASTER_ROLE);
        DiscoveryNode dataNode1 = newNode(nodeId++, DiscoveryNodeRole.DATA_ROLE);
        DiscoveryNode dataNode2 = newNode(nodeId++, DiscoveryNodeRole.DATA_ROLE);
        DiscoveryNode dataNode3 = newNode(nodeId, DiscoveryNodeRole.DATA_ROLE);

        String hotIndex = "hot_index_1";
        String warmIndex1 = "warm_index_1";
        String warmIndex2 = "warm_index_2";
        String coldIndex1 = "cold_index_1";
        String coldIndex2 = "cold_index_2";
        String coldIndex3 = "cold_index_3";

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(
                dataNode1,
                Map.of(
                    DataTier.DATA_HOT,
                    createStats(1, 2, docCount, byteSize),
                    DataTier.DATA_WARM,
                    createStats(0, 2, docCount, byteSize),
                    DataTier.DATA_COLD,
                    createStats(1, 1, docCount, byteSize)
                )
            ),
            new NodeDataTiersUsage(
                dataNode2,
                Map.of(
                    DataTier.DATA_HOT,
                    createStats(1, 2, docCount, byteSize),
                    DataTier.DATA_WARM,
                    createStats(1, 1, docCount, byteSize),
                    DataTier.DATA_COLD,
                    createStats(1, 1, docCount, byteSize)
                )
            ),
            new NodeDataTiersUsage(
                dataNode3,
                Map.of(
                    DataTier.DATA_HOT,
                    createStats(1, 2, docCount, byteSize),
                    DataTier.DATA_WARM,
                    createStats(1, 1, docCount, byteSize),
                    DataTier.DATA_COLD,
                    createStats(1, 1, docCount, byteSize)
                )
            )
        );
        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of(
                DataTier.DATA_HOT,
                Set.of(hotIndex),
                DataTier.DATA_WARM,
                Set.of(warmIndex1, warmIndex2),
                DataTier.DATA_COLD,
                Set.of(coldIndex1, coldIndex2, coldIndex3)
            )
        );

        assertThat(tierSpecificStats.size(), is(3));

        DataTiersFeatureSetUsage.TierSpecificStats hotStats = tierSpecificStats.get(DataTier.DATA_HOT);
        assertThat(hotStats, is(notNullValue()));
        assertThat(hotStats.nodeCount, is(0));
        assertThat(hotStats.indexCount, is(1));
        assertThat(hotStats.totalShardCount, is(6));
        assertThat(hotStats.docCount, is(6 * docCount));
        assertThat(hotStats.totalByteCount, is(6 * byteSize));
        assertThat(hotStats.primaryShardCount, is(3));
        assertThat(hotStats.primaryByteCount, is(3 * byteSize));
        assertThat(hotStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(hotStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats warmStats = tierSpecificStats.get(DataTier.DATA_WARM);
        assertThat(warmStats, is(notNullValue()));
        assertThat(warmStats.nodeCount, is(0));
        assertThat(warmStats.indexCount, is(2));
        assertThat(warmStats.totalShardCount, is(4));
        assertThat(warmStats.docCount, is(4 * docCount));
        assertThat(warmStats.totalByteCount, is(4 * byteSize));
        assertThat(warmStats.primaryShardCount, is(2));
        assertThat(warmStats.primaryByteCount, is(2 * byteSize));
        assertThat(warmStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(warmStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats coldStats = tierSpecificStats.get(DataTier.DATA_COLD);
        assertThat(coldStats, is(notNullValue()));
        assertThat(coldStats.nodeCount, is(0));
        assertThat(coldStats.indexCount, is(3));
        assertThat(coldStats.totalShardCount, is(3));
        assertThat(coldStats.docCount, is(3 * docCount));
        assertThat(coldStats.totalByteCount, is(3 * byteSize));
        assertThat(coldStats.primaryShardCount, is(3));
        assertThat(coldStats.primaryByteCount, is(3 * byteSize));
        assertThat(coldStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(coldStats.primaryShardBytesMAD, is(0L)); 
    }

    public void testCalculateStatsReasonableCase() {
        int nodeId = 0;
        DiscoveryNode leader = newNode(nodeId++, DiscoveryNodeRole.MASTER_ROLE);
        DiscoveryNode hotNode1 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode hotNode2 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode hotNode3 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode warmNode1 = newNode(nodeId++, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode warmNode2 = newNode(nodeId++, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode warmNode3 = newNode(nodeId++, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode warmNode4 = newNode(nodeId++, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode warmNode5 = newNode(nodeId++, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode coldNode1 = newNode(nodeId, DiscoveryNodeRole.DATA_COLD_NODE_ROLE);

        String hotIndex1 = "hot_index_1";
        String warmIndex1 = "warm_index_1";
        String warmIndex2 = "warm_index_2";
        String coldIndex1 = "cold_index_1";
        String coldIndex2 = "cold_index_2";
        String coldIndex3 = "cold_index_3";

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(hotNode1, Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize))),
            new NodeDataTiersUsage(hotNode2, Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize))),
            new NodeDataTiersUsage(hotNode3, Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize))),
            new NodeDataTiersUsage(warmNode1, Map.of(DataTier.DATA_WARM, createStats(1, 1, docCount, byteSize))),
            new NodeDataTiersUsage(warmNode2, Map.of(DataTier.DATA_WARM, createStats(0, 1, docCount, byteSize))),
            new NodeDataTiersUsage(warmNode3, Map.of(DataTier.DATA_WARM, createStats(1, 1, docCount, byteSize))),
            new NodeDataTiersUsage(warmNode4, Map.of(DataTier.DATA_WARM, createStats(0, 1, docCount, byteSize))),
            new NodeDataTiersUsage(warmNode5, Map.of()),
            new NodeDataTiersUsage(coldNode1, Map.of(DataTier.DATA_COLD, createStats(3, 3, docCount, byteSize)))

        );
        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of(
                DataTier.DATA_HOT,
                Set.of(hotIndex1),
                DataTier.DATA_WARM,
                Set.of(warmIndex1, warmIndex2),
                DataTier.DATA_COLD,
                Set.of(coldIndex1, coldIndex2, coldIndex3)
            )
        );

        assertThat(tierSpecificStats.size(), is(3));

        DataTiersFeatureSetUsage.TierSpecificStats hotStats = tierSpecificStats.get(DataTier.DATA_HOT);
        assertThat(hotStats, is(notNullValue()));
        assertThat(hotStats.nodeCount, is(3));
        assertThat(hotStats.indexCount, is(1));
        assertThat(hotStats.totalShardCount, is(6));
        assertThat(hotStats.docCount, is(6 * docCount));
        assertThat(hotStats.totalByteCount, is(6 * byteSize));
        assertThat(hotStats.primaryShardCount, is(3));
        assertThat(hotStats.primaryByteCount, is(3 * byteSize));
        assertThat(hotStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(hotStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats warmStats = tierSpecificStats.get(DataTier.DATA_WARM);
        assertThat(warmStats, is(notNullValue()));
        assertThat(warmStats.nodeCount, is(5));
        assertThat(warmStats.indexCount, is(2));
        assertThat(warmStats.totalShardCount, is(4));
        assertThat(warmStats.docCount, is(4 * docCount));
        assertThat(warmStats.totalByteCount, is(4 * byteSize));
        assertThat(warmStats.primaryShardCount, is(2));
        assertThat(warmStats.primaryByteCount, is(2 * byteSize));
        assertThat(warmStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(warmStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats coldStats = tierSpecificStats.get(DataTier.DATA_COLD);
        assertThat(coldStats, is(notNullValue()));
        assertThat(coldStats.nodeCount, is(1));
        assertThat(coldStats.indexCount, is(3));
        assertThat(coldStats.totalShardCount, is(3));
        assertThat(coldStats.docCount, is(3 * docCount));
        assertThat(coldStats.totalByteCount, is(3 * byteSize));
        assertThat(coldStats.primaryShardCount, is(3));
        assertThat(coldStats.primaryByteCount, is(3 * byteSize));
        assertThat(coldStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(coldStats.primaryShardBytesMAD, is(0L)); 
    }

    public void testCalculateStatsMixedTiers() {
        int nodeId = 0;
        DiscoveryNode leader = newNode(nodeId++, DiscoveryNodeRole.MASTER_ROLE);

        DiscoveryNode mixedNode1 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode mixedNode2 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);
        DiscoveryNode mixedNode3 = newNode(nodeId, DiscoveryNodeRole.DATA_HOT_NODE_ROLE, DiscoveryNodeRole.DATA_WARM_NODE_ROLE);

        String hotIndex1 = "hot_index_1";
        String warmIndex1 = "warm_index_1";
        String warmIndex2 = "warm_index_2";

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(
                mixedNode1,
                Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize), DataTier.DATA_WARM, createStats(1, 2, docCount, byteSize))
            ),
            new NodeDataTiersUsage(
                mixedNode2,
                Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize), DataTier.DATA_WARM, createStats(0, 1, docCount, byteSize))
            ),
            new NodeDataTiersUsage(
                mixedNode3,
                Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize), DataTier.DATA_WARM, createStats(1, 1, docCount, byteSize))
            )
        );

        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of(DataTier.DATA_HOT, Set.of(hotIndex1), DataTier.DATA_WARM, Set.of(warmIndex1, warmIndex2))
        );

        assertThat(tierSpecificStats.size(), is(2));

        DataTiersFeatureSetUsage.TierSpecificStats hotStats = tierSpecificStats.get(DataTier.DATA_HOT);
        assertThat(hotStats, is(notNullValue()));
        assertThat(hotStats.nodeCount, is(3));
        assertThat(hotStats.indexCount, is(1));
        assertThat(hotStats.totalShardCount, is(6));
        assertThat(hotStats.docCount, is(6 * docCount));
        assertThat(hotStats.totalByteCount, is(6 * byteSize));
        assertThat(hotStats.primaryShardCount, is(3));
        assertThat(hotStats.primaryByteCount, is(3 * byteSize));
        assertThat(hotStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(hotStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats warmStats = tierSpecificStats.get(DataTier.DATA_WARM);
        assertThat(warmStats, is(notNullValue()));
        assertThat(warmStats.nodeCount, is(3));
        assertThat(warmStats.indexCount, is(2));
        assertThat(warmStats.totalShardCount, is(4));
        assertThat(warmStats.docCount, is(4 * docCount));
        assertThat(warmStats.totalByteCount, is(4 * byteSize));
        assertThat(warmStats.primaryShardCount, is(2));
        assertThat(warmStats.primaryByteCount, is(2 * byteSize));
        assertThat(warmStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(warmStats.primaryShardBytesMAD, is(0L)); 
    }

    public void testCalculateStatsStuckInWrongTier() {
        int nodeId = 0;
        DiscoveryNode leader = newNode(nodeId++, DiscoveryNodeRole.MASTER_ROLE);
        DiscoveryNode hotNode1 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode hotNode2 = newNode(nodeId++, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);
        DiscoveryNode hotNode3 = newNode(nodeId, DiscoveryNodeRole.DATA_HOT_NODE_ROLE);

        String hotIndex1 = "hot_index_1";
        String warmIndex1 = "warm_index_1";

        List<NodeDataTiersUsage> nodeDataTiersUsages = List.of(
            new NodeDataTiersUsage(leader, Map.of()),
            new NodeDataTiersUsage(
                hotNode1,
                Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize), DataTier.DATA_WARM, createStats(1, 1, docCount, byteSize))
            ),
            new NodeDataTiersUsage(
                hotNode2,
                Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize), DataTier.DATA_WARM, createStats(0, 1, docCount, byteSize))
            ),
            new NodeDataTiersUsage(hotNode3, Map.of(DataTier.DATA_HOT, createStats(1, 2, docCount, byteSize)))
        );

        Map<String, DataTiersFeatureSetUsage.TierSpecificStats> tierSpecificStats = DataTiersUsageTransportAction.aggregateStats(
            nodeDataTiersUsages,
            Map.of(DataTier.DATA_HOT, Set.of(hotIndex1), DataTier.DATA_WARM, Set.of(warmIndex1))
        );

        assertThat(tierSpecificStats.size(), is(2));

        DataTiersFeatureSetUsage.TierSpecificStats hotStats = tierSpecificStats.get(DataTier.DATA_HOT);
        assertThat(hotStats, is(notNullValue()));
        assertThat(hotStats.nodeCount, is(3));
        assertThat(hotStats.indexCount, is(1));
        assertThat(hotStats.totalShardCount, is(6));
        assertThat(hotStats.docCount, is(6 * docCount));
        assertThat(hotStats.totalByteCount, is(6 * byteSize));
        assertThat(hotStats.primaryShardCount, is(3));
        assertThat(hotStats.primaryByteCount, is(3 * byteSize));
        assertThat(hotStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(hotStats.primaryShardBytesMAD, is(0L)); 

        DataTiersFeatureSetUsage.TierSpecificStats warmStats = tierSpecificStats.get(DataTier.DATA_WARM);
        assertThat(warmStats, is(notNullValue()));
        assertThat(warmStats.nodeCount, is(0));
        assertThat(warmStats.indexCount, is(1));
        assertThat(warmStats.totalShardCount, is(2));
        assertThat(warmStats.docCount, is(2 * docCount));
        assertThat(warmStats.totalByteCount, is(2 * byteSize));
        assertThat(warmStats.primaryShardCount, is(1));
        assertThat(warmStats.primaryByteCount, is(byteSize));
        assertThat(warmStats.primaryByteCountMedian, is(byteSize)); 
        assertThat(warmStats.primaryShardBytesMAD, is(0L)); 
    }

    private NodeDataTiersUsage.UsageStats createStats(int primaryShardCount, int totalNumberOfShards, long docCount, long byteSize) {
        return new NodeDataTiersUsage.UsageStats(
            primaryShardCount > 0 ? IntStream.range(0, primaryShardCount).mapToObj(i -> byteSize).toList() : List.of(),
            totalNumberOfShards,
            totalNumberOfShards * docCount,
            totalNumberOfShards * byteSize
        );
    }

    private IndexRoutingTable.Builder getIndexRoutingTable(IndexMetadata indexMetadata, DiscoveryNode node) {
        IndexRoutingTable.Builder indexRoutingTableBuilder = IndexRoutingTable.builder(indexMetadata.getIndex());
        routeTestShardToNodes(indexMetadata, 0, indexRoutingTableBuilder, node);
        return indexRoutingTableBuilder;
    }
}
