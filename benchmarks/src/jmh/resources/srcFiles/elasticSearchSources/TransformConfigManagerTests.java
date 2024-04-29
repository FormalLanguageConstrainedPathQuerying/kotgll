/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.persistence;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.cluster.routing.TestShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.TestIndexNameExpressionResolver;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.core.ClientHelper;
import org.elasticsearch.xpack.core.action.util.PageParams;
import org.elasticsearch.xpack.core.transform.TransformMessages;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpointTests;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfigTests;
import org.elasticsearch.xpack.core.transform.transforms.TransformStoredDoc;
import org.elasticsearch.xpack.core.transform.transforms.TransformStoredDocTests;
import org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants;
import org.elasticsearch.xpack.transform.TransformSingleNodeTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.elasticsearch.core.Tuple.tuple;
import static org.elasticsearch.xpack.transform.persistence.TransformConfigManager.TO_XCONTENT_PARAMS;
import static org.elasticsearch.xpack.transform.persistence.TransformInternalIndex.mappings;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformConfigManagerTests extends TransformSingleNodeTestCase {

    private IndexBasedTransformConfigManager transformConfigManager;
    private ClusterService clusterService;

    @Before
    public void createComponents() {
        clusterService = mock(ClusterService.class);
        transformConfigManager = new IndexBasedTransformConfigManager(
            clusterService,
            TestIndexNameExpressionResolver.newInstance(),
            client(),
            xContentRegistry()
        );
    }

    public void testGetMissingTransform() throws InterruptedException {
        assertAsync(
            listener -> transformConfigManager.getTransformConfiguration("not_there", listener),
            (TransformConfig) null,
            null,
            e -> {
                assertEquals(ResourceNotFoundException.class, e.getClass());
                assertEquals(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
            }
        );

        assertAsync(
            listener -> transformConfigManager.putTransformConfiguration(TransformConfigTests.randomTransformConfig(), listener),
            true,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.getTransformConfiguration("not_there", listener),
            (TransformConfig) null,
            null,
            e -> {
                assertEquals(ResourceNotFoundException.class, e.getClass());
                assertEquals(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
            }
        );
    }

    public void testDeleteMissingTransform() throws InterruptedException {
        assertAsync(listener -> transformConfigManager.deleteTransform("not_there", listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
        });

        assertAsync(
            listener -> transformConfigManager.putTransformConfiguration(TransformConfigTests.randomTransformConfig(), listener),
            true,
            null,
            null
        );

        assertAsync(listener -> transformConfigManager.deleteTransform("not_there", listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
        });
    }

    public void testCreateReadDeleteTransform() throws InterruptedException {
        TransformConfig transformConfig = TransformConfigTests.randomTransformConfig();

        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig, listener), true, null, null);

        assertAsync(
            listener -> transformConfigManager.getTransformConfiguration(transformConfig.getId(), listener),
            transformConfig,
            null,
            null
        );

        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig, listener), (Boolean) null, null, e -> {
            assertEquals(ResourceAlreadyExistsException.class, e.getClass());
            assertEquals(
                TransformMessages.getMessage(TransformMessages.REST_PUT_TRANSFORM_EXISTS, transformConfig.getId()),
                e.getMessage()
            );
        });

        assertAsync(listener -> transformConfigManager.deleteTransform(transformConfig.getId(), listener), true, null, null);

        assertAsync(listener -> transformConfigManager.deleteTransform(transformConfig.getId(), listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, transformConfig.getId()), e.getMessage());
        });

        assertAsync(
            listener -> transformConfigManager.getTransformConfiguration(transformConfig.getId(), listener),
            (TransformConfig) null,
            null,
            e -> {
                assertEquals(ResourceNotFoundException.class, e.getClass());
                assertEquals(
                    TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, transformConfig.getId()),
                    e.getMessage()
                );
            }
        );
    }

    public void testCreateReadDeleteCheckPoint() throws InterruptedException {
        TransformCheckpoint checkpoint = TransformCheckpointTests.randomTransformCheckpoint();

        assertAsync(listener -> transformConfigManager.putTransformCheckpoint(checkpoint, listener), true, null, null);

        assertAsync(
            listener -> transformConfigManager.getTransformCheckpoint(checkpoint.getTransformId(), checkpoint.getCheckpoint(), listener),
            checkpoint,
            null,
            null
        );

        assertAsync(listener -> transformConfigManager.deleteTransform(checkpoint.getTransformId(), listener), true, null, null);

        assertAsync(listener -> transformConfigManager.deleteTransform(checkpoint.getTransformId(), listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(
                TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, checkpoint.getTransformId()),
                e.getMessage()
            );
        });

        assertAsync(
            listener -> transformConfigManager.getTransformCheckpoint(checkpoint.getTransformId(), checkpoint.getCheckpoint(), listener),
            TransformCheckpoint.EMPTY,
            null,
            null
        );
    }

    public void testExpandIds() throws Exception {
        TransformConfig transformConfig1 = TransformConfigTests.randomTransformConfig("transform1_expand");
        TransformConfig transformConfig2 = TransformConfigTests.randomTransformConfig("transform2_expand");
        TransformConfig transformConfig3 = TransformConfigTests.randomTransformConfig("transform3_expand");

        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig1, listener), true, null, null);
        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig2, listener), true, null, null);
        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig3, listener), true, null, null);

        assertAsync(
            listener -> transformConfigManager.expandTransformIds(
                transformConfig1.getId(),
                PageParams.defaultParams(),
                null,
                true,
                listener
            ),
            tuple(1L, tuple(singletonList("transform1_expand"), singletonList(transformConfig1))),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds(
                "transform1_expand,transform2_expand",
                PageParams.defaultParams(),
                null,
                true,
                listener
            ),
            tuple(2L, tuple(Arrays.asList("transform1_expand", "transform2_expand"), Arrays.asList(transformConfig1, transformConfig2))),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds(
                "transform1*,transform2_expand,transform3_expand",
                PageParams.defaultParams(),
                null,
                true,
                listener
            ),
            tuple(
                3L,
                tuple(
                    Arrays.asList("transform1_expand", "transform2_expand", "transform3_expand"),
                    Arrays.asList(transformConfig1, transformConfig2, transformConfig3)
                )
            ),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds("_all", PageParams.defaultParams(), null, true, listener),
            tuple(
                3L,
                tuple(
                    Arrays.asList("transform1_expand", "transform2_expand", "transform3_expand"),
                    Arrays.asList(transformConfig1, transformConfig2, transformConfig3)
                )
            ),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds("_all", new PageParams(0, 1), null, true, listener),
            tuple(3L, tuple(singletonList("transform1_expand"), singletonList(transformConfig1))),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds("_all", new PageParams(1, 2), null, true, listener),
            tuple(3L, tuple(Arrays.asList("transform2_expand", "transform3_expand"), Arrays.asList(transformConfig2, transformConfig3))),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds("unknown,unknown2", new PageParams(1, 2), null, true, listener),
            (Tuple<Long, Tuple<List<String>, List<TransformConfig>>>) null,
            null,
            e -> {
                assertThat(e, instanceOf(ResourceNotFoundException.class));
                assertThat(
                    e.getMessage(),
                    equalTo(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "unknown,unknown2"))
                );
            }
        );

        assertAsync(
            listener -> transformConfigManager.expandTransformIds("unknown*", new PageParams(1, 2), null, false, listener),
            (Tuple<Long, Tuple<List<String>, List<TransformConfig>>>) null,
            null,
            e -> {
                assertThat(e, instanceOf(ResourceNotFoundException.class));
                assertThat(e.getMessage(), equalTo(TransformMessages.getMessage(TransformMessages.REST_UNKNOWN_TRANSFORM, "unknown*")));
            }
        );

        String oldIndex = TransformInternalIndexConstants.INDEX_PATTERN + "001";
        String docId = TransformConfig.documentId(transformConfig2.getId());
        TransformConfig transformConfig = TransformConfigTests.randomTransformConfig(transformConfig2.getId());
        indicesAdmin().create(new CreateIndexRequest(oldIndex).mapping(mappings()).origin(ClientHelper.TRANSFORM_ORIGIN)).actionGet();

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformConfig.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source)
                .id(docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client().index(request).actionGet();
        }

        assertAsync(
            listener -> transformConfigManager.expandTransformIds(
                "transform1_expand,transform2_expand",
                PageParams.defaultParams(),
                null,
                true,
                listener
            ),
            tuple(2L, tuple(Arrays.asList("transform1_expand", "transform2_expand"), Arrays.asList(transformConfig1, transformConfig2))),
            null,
            null
        );

    }

    public void testGetAllTransformIdsAndGetAllOutdatedTransformIds() throws Exception {
        long numberOfTransformsToGenerate = 100L;
        Set<String> transformIds = new HashSet<>();

        for (long i = 0; i < numberOfTransformsToGenerate; ++i) {
            String id = "transform_" + i;
            transformIds.add(id);
            TransformConfig transformConfig = TransformConfigTests.randomTransformConfig(id);
            assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig, listener), true, null, null);
        }
        assertAsync(listener -> transformConfigManager.getAllTransformIds(null, listener), transformIds, null, null);

        assertAsync(
            listener -> transformConfigManager.expandAllTransformIds(false, 10, null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate), transformIds),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.getAllOutdatedTransformIds(null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate), Collections.<String>emptySet()),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandAllTransformIds(true, 10, null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate), Collections.<String>emptySet()),
            null,
            null
        );

        String oldIndex = TransformInternalIndexConstants.INDEX_PATTERN + "001";
        String transformId = "transform_42";
        String docId = TransformConfig.documentId(transformId);
        TransformConfig transformConfig = TransformConfigTests.randomTransformConfig(transformId);
        indicesAdmin().create(new CreateIndexRequest(oldIndex).mapping(mappings()).origin(ClientHelper.TRANSFORM_ORIGIN)).actionGet();

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformConfig.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source)
                .id(docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client().index(request).actionGet();
        }

        assertAsync(listener -> transformConfigManager.getAllTransformIds(null, listener), transformIds, null, null);
        assertAsync(
            listener -> transformConfigManager.getAllOutdatedTransformIds(null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate), Collections.<String>emptySet()),
            null,
            null
        );

        final String oldTransformId = "transform_oldindex";
        docId = TransformConfig.documentId(oldTransformId);
        transformConfig = TransformConfigTests.randomTransformConfig(oldTransformId);

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformConfig.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source)
                .id(docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client().index(request).actionGet();
        }

        assertAsync(
            listener -> transformConfigManager.putTransformCheckpoint(
                TransformCheckpointTests.randomTransformCheckpoint(oldTransformId),
                listener
            ),
            true,
            null,
            null
        );

        transformIds.add(oldTransformId);
        assertAsync(listener -> transformConfigManager.getAllTransformIds(null, listener), transformIds, null, null);
        assertAsync(
            listener -> transformConfigManager.getAllOutdatedTransformIds(null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate + 1), Collections.singleton(oldTransformId)),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.expandAllTransformIds(true, 10, null, listener),
            tuple(Long.valueOf(numberOfTransformsToGenerate + 1), Collections.singleton(oldTransformId)),
            null,
            null
        );
    }

    public void testStoredDoc() throws InterruptedException {
        String transformId = "transform_test_stored_doc_create_read_update";

        TransformStoredDoc storedDocs = TransformStoredDocTests.randomTransformStoredDoc(transformId);
        SeqNoPrimaryTermAndIndex firstIndex = new SeqNoPrimaryTermAndIndex(0, 1, TransformInternalIndexConstants.LATEST_INDEX_NAME);

        assertAsync(listener -> transformConfigManager.putOrUpdateTransformStoredDoc(storedDocs, null, listener), firstIndex, null, null);
        assertAsync(
            listener -> transformConfigManager.getTransformStoredDoc(transformId, false, listener),
            tuple(storedDocs, firstIndex),
            null,
            null
        );

        SeqNoPrimaryTermAndIndex secondIndex = new SeqNoPrimaryTermAndIndex(1, 1, TransformInternalIndexConstants.LATEST_INDEX_NAME);
        TransformStoredDoc updated = TransformStoredDocTests.randomTransformStoredDoc(transformId);
        assertAsync(
            listener -> transformConfigManager.putOrUpdateTransformStoredDoc(updated, firstIndex, listener),
            secondIndex,
            null,
            null
        );
        assertAsync(
            listener -> transformConfigManager.getTransformStoredDoc(transformId, false, listener),
            tuple(updated, secondIndex),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.putOrUpdateTransformStoredDoc(updated, firstIndex, listener),
            (SeqNoPrimaryTermAndIndex) null,
            r -> fail("did not fail with version conflict."),
            e -> assertThat(
                e.getMessage(),
                equalTo("Failed to persist transform statistics for transform [transform_test_stored_doc_create_read_update]")
            )
        );
    }

    public void testGetStoredDocMultiple() throws InterruptedException {
        int numStats = randomIntBetween(10, 15);
        List<TransformStoredDoc> expectedDocs = new ArrayList<>();
        for (int i = 0; i < numStats; i++) {
            SeqNoPrimaryTermAndIndex initialSeqNo = new SeqNoPrimaryTermAndIndex(i, 1, TransformInternalIndexConstants.LATEST_INDEX_NAME);
            TransformStoredDoc stat = TransformStoredDocTests.randomTransformStoredDoc(randomAlphaOfLength(6) + i);
            expectedDocs.add(stat);
            assertAsync(listener -> transformConfigManager.putOrUpdateTransformStoredDoc(stat, null, listener), initialSeqNo, null, null);
        }

        if (expectedDocs.size() > 1) {
            expectedDocs.remove(expectedDocs.size() - 1);
        }
        List<String> ids = expectedDocs.stream().map(TransformStoredDoc::getId).collect(Collectors.toList());

        expectedDocs.sort(Comparator.comparing(TransformStoredDoc::getId));
        assertAsync(listener -> transformConfigManager.getTransformStoredDocs(ids, null, listener), expectedDocs, null, null);
    }

    public void testDeleteOldTransformConfigurations() throws Exception {
        String oldIndex = TransformInternalIndexConstants.INDEX_PATTERN + "001";
        String transformId = "transform_test_delete_old_configurations";
        String docId = TransformConfig.documentId(transformId);
        TransformConfig transformConfig = TransformConfigTests.randomTransformConfig("transform_test_delete_old_configurations");
        indicesAdmin().create(new CreateIndexRequest(oldIndex).mapping(mappings()).origin(ClientHelper.TRANSFORM_ORIGIN)).actionGet();

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformConfig.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source)
                .id(docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client().index(request).actionGet();
        }

        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig, listener), true, null, null);

        assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(true));
        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );

        assertAsync(listener -> transformConfigManager.deleteOldTransformConfigurations(transformId, listener), true, null, null);

        indicesAdmin().refresh(new RefreshRequest(TransformInternalIndexConstants.INDEX_NAME_PATTERN)).actionGet();
        assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(false));
        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );
    }

    public void testDeleteOldTransformStoredDocuments() throws Exception {
        String oldIndex = TransformInternalIndexConstants.INDEX_PATTERN + "001";
        String transformId = "transform_test_delete_old_stored_documents";
        String docId = TransformStoredDoc.documentId(transformId);
        TransformStoredDoc transformStoredDoc = TransformStoredDocTests.randomTransformStoredDoc(transformId);
        indicesAdmin().create(new CreateIndexRequest(oldIndex).mapping(mappings()).origin(ClientHelper.TRANSFORM_ORIGIN)).actionGet();

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformStoredDoc.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source).id(docId);
            client().index(request).actionGet();
        }

        assertAsync(
            listener -> transformConfigManager.putOrUpdateTransformStoredDoc(
                transformStoredDoc,
                new SeqNoPrimaryTermAndIndex(3, 1, oldIndex),
                listener
            ),
            new SeqNoPrimaryTermAndIndex(0, 1, TransformInternalIndexConstants.LATEST_INDEX_NAME),
            null,
            null
        );

        indicesAdmin().refresh(new RefreshRequest(TransformInternalIndexConstants.INDEX_NAME_PATTERN)).actionGet();

        assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(true));
        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );

        assertAsync(listener -> transformConfigManager.deleteOldTransformStoredDocuments(transformId, listener), 1L, null, null);

        indicesAdmin().refresh(new RefreshRequest(TransformInternalIndexConstants.INDEX_NAME_PATTERN)).actionGet();
        assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(false));
        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );
    }

    public void testDeleteOldCheckpoints() throws InterruptedException {
        String transformId = randomAlphaOfLengthBetween(1, 10);
        long timestamp = System.currentTimeMillis() - randomLongBetween(20000, 40000);

        TransformStoredDoc storedDocs = TransformStoredDocTests.randomTransformStoredDoc(transformId);
        SeqNoPrimaryTermAndIndex firstIndex = new SeqNoPrimaryTermAndIndex(0, 1, TransformInternalIndexConstants.LATEST_INDEX_NAME);
        assertAsync(listener -> transformConfigManager.putOrUpdateTransformStoredDoc(storedDocs, null, listener), firstIndex, null, null);

        TransformConfig transformConfig = TransformConfigTests.randomTransformConfig(transformId);
        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfig, listener), true, null, null);

        for (int i = 1; i <= 100; i++) {
            TransformCheckpoint checkpoint = new TransformCheckpoint(
                transformId,
                timestamp + i * 200,
                i,
                emptyMap(),
                timestamp - 100 + i * 200
            );
            assertAsync(listener -> transformConfigManager.putTransformCheckpoint(checkpoint, listener), true, null, null);
        }

        int randomCheckpoint = randomIntBetween(1, 100);
        TransformCheckpoint checkpointExpected = new TransformCheckpoint(
            transformId,
            timestamp + randomCheckpoint * 200,
            randomCheckpoint,
            emptyMap(),
            timestamp - 100 + randomCheckpoint * 200
        );

        assertAsync(
            listener -> transformConfigManager.getTransformCheckpoint(transformId, randomCheckpoint, listener),
            checkpointExpected,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.deleteOldCheckpoints(transformId, 11L, timestamp + 1 + 20L * 200, listener),
            10L,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.deleteOldCheckpoints(transformId, 30L, timestamp + 1 + 20L * 200, listener),
            10L,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.deleteOldCheckpoints(transformId, 30L, timestamp + 1 + 20L * 200, listener),
            0L,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.deleteOldCheckpoints(transformId, 101L, timestamp + 1 + 100L * 200, listener),
            80L,
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.getTransformStoredDoc(transformId, false, listener),
            tuple(storedDocs, firstIndex),
            null,
            null
        );

        assertAsync(
            listener -> transformConfigManager.getTransformConfiguration(transformConfig.getId(), listener),
            transformConfig,
            null,
            null
        );
    }

    public void testDeleteOldIndices() throws Exception {
        String oldIndex = (randomBoolean()
            ? TransformInternalIndexConstants.INDEX_PATTERN
            : TransformInternalIndexConstants.INDEX_PATTERN_DEPRECATED) + "001";
        String transformId = "transform_test_delete_old_indices";
        String docId = TransformConfig.documentId(transformId);
        TransformConfig transformConfigOld = TransformConfigTests.randomTransformConfig(transformId);
        TransformConfig transformConfigNew = TransformConfigTests.randomTransformConfig(transformId);

        indicesAdmin().create(new CreateIndexRequest(oldIndex).mapping(mappings()).origin(ClientHelper.TRANSFORM_ORIGIN)).actionGet();

        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = transformConfigOld.toXContent(builder, new ToXContent.MapParams(TO_XCONTENT_PARAMS));
            IndexRequest request = new IndexRequest(oldIndex).source(source)
                .id(docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client().index(request).actionGet();
        }

        assertAsync(listener -> transformConfigManager.putTransformConfiguration(transformConfigNew, listener), true, null, null);

        assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(true));
        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );

        assertAsync(listener -> transformConfigManager.getTransformConfiguration(transformId, listener), transformConfigNew, null, null);

        when(clusterService.state()).thenReturn(
            createClusterStateWithTransformIndex(oldIndex, TransformInternalIndexConstants.LATEST_INDEX_NAME)
        );

        assertAsync(listener -> transformConfigManager.deleteOldIndices(listener), true, null, null);

        assertAsync(listener -> transformConfigManager.getTransformConfiguration(transformId, listener), transformConfigNew, null, null);

        expectThrows(
            IndexNotFoundException.class,
            () -> assertThat(client().get(new GetRequest(oldIndex).id(docId)).actionGet().isExists(), is(false))
        );

        assertThat(
            client().get(new GetRequest(TransformInternalIndexConstants.LATEST_INDEX_NAME).id(docId)).actionGet().isExists(),
            is(true)
        );
    }

    private static ClusterState createClusterStateWithTransformIndex(String... indexes) throws IOException {
        Map<String, IndexMetadata> indexMapBuilder = new HashMap<>();
        Metadata.Builder metaBuilder = Metadata.builder();
        ClusterState.Builder csBuilder = ClusterState.builder(ClusterName.DEFAULT);
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();

        for (String index : indexes) {
            IndexMetadata.Builder builder = new IndexMetadata.Builder(index).settings(
                Settings.builder()
                    .put(TransformInternalIndex.settings(Settings.EMPTY))
                    .put(IndexMetadata.SETTING_INDEX_VERSION_CREATED.getKey(), IndexVersion.current())
                    .build()
            ).numberOfReplicas(0).numberOfShards(1).putMapping(Strings.toString(TransformInternalIndex.mappings()));
            final var indexMetadata = builder.build();
            indexMapBuilder.put(index, indexMetadata);

            routingTableBuilder.add(
                IndexRoutingTable.builder(indexMetadata.getIndex())
                    .addShard(
                        TestShardRouting.newShardRouting(
                            new ShardId(indexMetadata.getIndex(), 0),
                            "node_a",
                            null,
                            true,
                            ShardRoutingState.STARTED
                        )
                    )
                    .build()
            );

        }
        csBuilder.routingTable(routingTableBuilder.build());
        metaBuilder.indices(indexMapBuilder);
        csBuilder.metadata(metaBuilder.build());

        return csBuilder.build();
    }
}
