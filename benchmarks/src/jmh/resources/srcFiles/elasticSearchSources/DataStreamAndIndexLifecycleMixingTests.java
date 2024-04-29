/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ilm;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.rollover.RolloverConditions;
import org.elasticsearch.action.admin.indices.template.put.TransportPutComposableIndexTemplateAction;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.datastreams.CreateDataStreamAction;
import org.elasticsearch.action.datastreams.GetDataStreamAction;
import org.elasticsearch.action.datastreams.GetDataStreamAction.Response.ManagedBy;
import org.elasticsearch.action.datastreams.lifecycle.ExplainDataStreamLifecycleAction;
import org.elasticsearch.action.datastreams.lifecycle.ExplainIndexDataStreamLifecycle;
import org.elasticsearch.action.datastreams.lifecycle.PutDataStreamLifecycleAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.elasticsearch.cluster.metadata.DataStream;
import org.elasticsearch.cluster.metadata.DataStreamLifecycle;
import org.elasticsearch.cluster.metadata.Template;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.datastreams.DataStreamsPlugin;
import org.elasticsearch.datastreams.lifecycle.DataStreamLifecycleService;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.mapper.DateFieldMapper;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.LocalStateCompositeXPackPlugin;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.ilm.ExplainLifecycleRequest;
import org.elasticsearch.xpack.core.ilm.ExplainLifecycleResponse;
import org.elasticsearch.xpack.core.ilm.IndexLifecycleExplainResponse;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.LifecycleSettings;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.PhaseCompleteStep;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.WaitForRolloverReadyStep;
import org.elasticsearch.xpack.core.ilm.action.ExplainLifecycleAction;
import org.elasticsearch.xpack.core.ilm.action.ILMActions;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleRequest;
import org.junit.Before;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.elasticsearch.cluster.metadata.DataStreamTestHelper.backingIndexEqualTo;
import static org.elasticsearch.cluster.metadata.MetadataIndexTemplateService.DEFAULT_TIMESTAMP_FIELD;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

public class DataStreamAndIndexLifecycleMixingTests extends ESIntegTestCase {

    private String policy;
    private String dataStreamName;
    private String indexTemplateName;

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return List.of(LocalStateCompositeXPackPlugin.class, IndexLifecycle.class, DataStreamsPlugin.class);
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        Settings.Builder settings = Settings.builder().put(super.nodeSettings(nodeOrdinal, otherSettings));
        settings.put(DataStreamLifecycleService.DATA_STREAM_LIFECYCLE_POLL_INTERVAL, "1s");
        settings.put(DataStreamLifecycle.CLUSTER_LIFECYCLE_DEFAULT_ROLLOVER_SETTING.getKey(), "min_docs=1,max_docs=1");
        settings.put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), false);
        settings.put(XPackSettings.SECURITY_ENABLED.getKey(), false);
        settings.put(XPackSettings.WATCHER_ENABLED.getKey(), false);
        settings.put(XPackSettings.GRAPH_ENABLED.getKey(), false);
        settings.put(LifecycleSettings.LIFECYCLE_POLL_INTERVAL, "1s");
        settings.put(LifecycleSettings.LIFECYCLE_HISTORY_INDEX_ENABLED, false);
        return settings.build();
    }

    @Before
    public void refreshAbstractions() {
        policy = "policy-" + randomAlphaOfLength(5);
        dataStreamName = "datastream-" + randomAlphaOfLengthBetween(10, 15).toLowerCase(Locale.ROOT);
        indexTemplateName = "indextemplate-" + randomAlphaOfLengthBetween(10, 15).toLowerCase(Locale.ROOT);
    }

    public void testIndexTemplateSwapsILMForDataStreamLifecycle() throws Exception {
        RolloverAction rolloverIlmAction = new RolloverAction(RolloverConditions.newBuilder().addMaxIndexDocsCondition(2L).build());
        Phase hotPhase = new Phase("hot", TimeValue.ZERO, Map.of(rolloverIlmAction.getWriteableName(), rolloverIlmAction));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("hot", hotPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            null
        );
        CreateDataStreamAction.Request createDataStreamRequest = new CreateDataStreamAction.Request(dataStreamName);
        client().execute(CreateDataStreamAction.INSTANCE, createDataStreamRequest).get();

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getName(), equalTo(dataStreamName));
            List<Index> backingIndices = getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices();
            assertThat(backingIndices.size(), equalTo(2));
            String backingIndex = backingIndices.get(0).getName();
            assertThat(backingIndex, backingIndexEqualTo(dataStreamName, 1));
            String writeIndex = backingIndices.get(1).getName();
            assertThat(writeIndex, backingIndexEqualTo(dataStreamName, 2));
        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(firstGenerationIndex, secondGenerationIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });



        DataStreamLifecycle customLifecycle = customEnabledLifecycle();
        putComposableIndexTemplate(indexTemplateName, null, List.of(dataStreamName + "*"), Settings.EMPTY, null, customLifecycle);

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(3));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String writeIndex = backingIndices.get(2);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(false));

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(1));
            ExplainIndexDataStreamLifecycle writeIndexDataStreamLifecycleExplain = dataStreamLifecycleExplainResponse.getIndices().get(0);
            assertThat(writeIndexDataStreamLifecycleExplain.isManagedByLifecycle(), is(false));
        });

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, new DataStreamLifecycle())
        ).actionGet();

        indexDocs(dataStreamName, 1);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(4));

        });

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, customLifecycle.getDataStreamRetention())
        ).actionGet();

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String thirdGenerationIndex = backingIndices.get(2);
            String writeIndex = backingIndices.get(3);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                thirdGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(false));

            IndexLifecycleExplainResponse writeIndexExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(writeIndexExplain.managedByILM(), is(false));

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { thirdGenerationIndex, writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(2));
            for (ExplainIndexDataStreamLifecycle index : dataStreamLifecycleExplainResponse.getIndices()) {
                assertThat(index.isManagedByLifecycle(), is(true));
                assertThat(index.getLifecycle(), equalTo(customLifecycle));
            }
        });
    }

    public void testUpdateIndexTemplateFromILMtoBothILMAndDataStreamLifecycle() throws Exception {
        RolloverAction rolloverIlmAction = new RolloverAction(RolloverConditions.newBuilder().addMaxIndexDocsCondition(2L).build());
        Phase hotPhase = new Phase("hot", TimeValue.ZERO, Map.of(rolloverIlmAction.getWriteableName(), rolloverIlmAction));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("hot", hotPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            null
        );
        CreateDataStreamAction.Request createDataStreamRequest = new CreateDataStreamAction.Request(dataStreamName);
        client().execute(CreateDataStreamAction.INSTANCE, createDataStreamRequest).get();

        indexDocs(dataStreamName, 2);
        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getName(), equalTo(dataStreamName));
            List<Index> backingIndices = getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices();
            assertThat(backingIndices.size(), equalTo(2));
        });
        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(firstGenerationIndex, secondGenerationIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });



        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            new DataStreamLifecycle()
        );

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(3));

        });
        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String thirdGenerationIndex = backingIndices.get(2);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                thirdGenerationIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(true));
            assertThat(thirdGenerationExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });
        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, TimeValue.timeValueDays(90))
        );

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(IndexSettings.PREFER_ILM, false).put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            new DataStreamLifecycle()
        );

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(4));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String thirdGenerationIndex = backingIndices.get(2);
            String writeIndex = backingIndices.get(3);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                thirdGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(true));
            assertThat(thirdGenerationExplain.getPhase(), is("hot"));
            assertThat(thirdGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(1));
            ExplainIndexDataStreamLifecycle dataStreamLifecycleExplain = dataStreamLifecycleExplainResponse.getIndices().get(0);
            assertThat(dataStreamLifecycleExplain.isManagedByLifecycle(), is(true));
            assertThat(dataStreamLifecycleExplain.getIndex(), is(writeIndex));
        });
    }

    public void testUpdateIndexTemplateToDataStreamLifecyclePreference() throws Exception {
        RolloverAction rolloverIlmAction = new RolloverAction(RolloverConditions.newBuilder().addMaxIndexDocsCondition(2L).build());
        Phase hotPhase = new Phase("hot", TimeValue.ZERO, Map.of(rolloverIlmAction.getWriteableName(), rolloverIlmAction));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("hot", hotPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            null
        );
        CreateDataStreamAction.Request createDataStreamRequest = new CreateDataStreamAction.Request(dataStreamName);
        client().execute(CreateDataStreamAction.INSTANCE, createDataStreamRequest).get();

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getName(), equalTo(dataStreamName));
            List<Index> backingIndices = getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices();
            assertThat(backingIndices.size(), equalTo(2));
        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(firstGenerationIndex, secondGenerationIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });


        DataStreamLifecycle customLifecycle = customEnabledLifecycle();
        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(IndexSettings.PREFER_ILM, false).put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            customLifecycle
        );

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(3));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String writeIndex = backingIndices.get(2);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(1));
            ExplainIndexDataStreamLifecycle dataStreamLifecycleExplain = dataStreamLifecycleExplainResponse.getIndices().get(0);
            assertThat(dataStreamLifecycleExplain.isManagedByLifecycle(), is(false));
        });

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, new DataStreamLifecycle())
        ).actionGet();

        indexDocs(dataStreamName, 1);

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, customLifecycle.getDataStreamRetention())
        ).actionGet();

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(4));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String thirdGenerationIndex = backingIndices.get(2);
            String writeIndex = backingIndices.get(3);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                thirdGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(false));

            IndexLifecycleExplainResponse writeIndexExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(writeIndexExplain.managedByILM(), is(false));

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { thirdGenerationIndex, writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(2));
            for (ExplainIndexDataStreamLifecycle index : dataStreamLifecycleExplainResponse.getIndices()) {
                assertThat(index.isManagedByLifecycle(), is(true));
                assertThat(index.getLifecycle(), equalTo(customLifecycle));
            }
        });

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, TimeValue.timeValueDays(90), false)
        ).actionGet();

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String thirdGenerationIndex = backingIndices.get(2);
            String writeIndex = backingIndices.get(3);
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(
                firstGenerationIndex,
                secondGenerationIndex,
                thirdGenerationIndex,
                writeIndex
            );
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse firstGenerationExplain = explainResponse.getIndexResponses().get(firstGenerationIndex);
            assertThat(firstGenerationExplain.managedByILM(), is(true));
            assertThat(firstGenerationExplain.getPhase(), is("hot"));
            assertThat(firstGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse secondGenerationExplain = explainResponse.getIndexResponses().get(secondGenerationIndex);
            assertThat(secondGenerationExplain.managedByILM(), is(true));
            assertThat(secondGenerationExplain.getPhase(), is("hot"));
            assertThat(secondGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(true));
            assertThat(thirdGenerationExplain.getPhase(), is("hot"));
            assertThat(thirdGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse writeIndexExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(writeIndexExplain.managedByILM(), is(true));
            assertThat(writeIndexExplain.getPhase(), is("hot"));
            assertThat(writeIndexExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });
    }

    public void testUpdateIndexTemplateToMigrateFromDataStreamLifecycleToIlm() throws Exception {
        putComposableIndexTemplate(indexTemplateName, null, List.of(dataStreamName + "*"), null, null, new DataStreamLifecycle());

        indexDocs(dataStreamName, 1);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(2));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String writeIndex = backingIndices.get(1);

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { firstGenerationIndex, writeIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(2));
            for (ExplainIndexDataStreamLifecycle index : dataStreamLifecycleExplainResponse.getIndices()) {
                assertThat(index.isManagedByLifecycle(), is(true));
            }
        });

        RolloverAction rolloverIlmAction = new RolloverAction(RolloverConditions.newBuilder().addMaxIndexDocsCondition(2L).build());
        Phase hotPhase = new Phase("hot", TimeValue.ZERO, Map.of(rolloverIlmAction.getWriteableName(), rolloverIlmAction));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("hot", hotPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());



        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            null
        );

        indexDocs(dataStreamName, 1);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(3));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String writeIndex = backingIndices.get(2);

            ExplainDataStreamLifecycleAction.Response dataStreamLifecycleExplainResponse = client().execute(
                ExplainDataStreamLifecycleAction.INSTANCE,
                new ExplainDataStreamLifecycleAction.Request(new String[] { firstGenerationIndex, secondGenerationIndex })
            ).actionGet();
            assertThat(dataStreamLifecycleExplainResponse.getIndices().size(), is(2));
            for (ExplainIndexDataStreamLifecycle index : dataStreamLifecycleExplainResponse.getIndices()) {
                assertThat(index.isManagedByLifecycle(), is(true));
            }

            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(writeIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse writeIndexExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(writeIndexExplain.managedByILM(), is(true));
            assertThat(writeIndexExplain.getPhase(), is("hot"));
            assertThat(writeIndexExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });

        indexDocs(dataStreamName, 2);
        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(4));

        });

        assertBusy(() -> {
            List<String> backingIndices = getBackingIndices(dataStreamName);
            String thirdGenerationIndex = backingIndices.get(2);
            String writeIndex = backingIndices.get(3);

            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(thirdGenerationIndex, writeIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse thirdGenerationExplain = explainResponse.getIndexResponses().get(thirdGenerationIndex);
            assertThat(thirdGenerationExplain.managedByILM(), is(true));
            assertThat(thirdGenerationExplain.getPhase(), is("hot"));
            assertThat(thirdGenerationExplain.getStep(), is(PhaseCompleteStep.NAME));

            IndexLifecycleExplainResponse writeIndexExplain = explainResponse.getIndexResponses().get(writeIndex);
            assertThat(writeIndexExplain.managedByILM(), is(true));
            assertThat(writeIndexExplain.getPhase(), is("hot"));
            assertThat(writeIndexExplain.getStep(), is(WaitForRolloverReadyStep.NAME));
        });
    }

    public void testGetDataStreamResponse() throws Exception {
        RolloverAction rolloverIlmAction = new RolloverAction(RolloverConditions.newBuilder().addMaxIndexDocsCondition(2L).build());
        Phase hotPhase = new Phase("hot", TimeValue.ZERO, Map.of(rolloverIlmAction.getWriteableName(), rolloverIlmAction));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("hot", hotPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).build(),
            null,
            null
        );
        CreateDataStreamAction.Request createDataStreamRequest = new CreateDataStreamAction.Request(dataStreamName);
        client().execute(CreateDataStreamAction.INSTANCE, createDataStreamRequest).get();

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            assertThat(getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().size(), is(2));
        });

        putComposableIndexTemplate(
            indexTemplateName,
            null,
            List.of(dataStreamName + "*"),
            Settings.builder().put(LifecycleSettings.LIFECYCLE_NAME, policy).put(IndexSettings.PREFER_ILM, false).build(),
            null,
            null
        );

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, TimeValue.timeValueDays(90))
        ).actionGet();

        indexDocs(dataStreamName, 2);

        assertBusy(() -> {
            GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
            GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
                .actionGet();
            assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
            GetDataStreamAction.Response.DataStreamInfo dataStreamInfo = getDataStreamResponse.getDataStreams().get(0);
            List<Index> indices = dataStreamInfo.getDataStream().getIndices();
            assertThat(indices.size(), is(3));

            assertThat(dataStreamInfo.templatePreferIlmValue(), is(false));
            assertThat(dataStreamInfo.getIlmPolicy(), is(policy));

            List<String> backingIndices = getBackingIndices(dataStreamName);
            String firstGenerationIndex = backingIndices.get(0);
            String secondGenerationIndex = backingIndices.get(1);
            String writeIndex = backingIndices.get(2);
            assertThat(
                indices.stream().map(i -> i.getName()).toList(),
                containsInAnyOrder(firstGenerationIndex, secondGenerationIndex, writeIndex)
            );

            Function<String, Optional<Index>> backingIndexSupplier = indexName -> indices.stream()
                .filter(index -> index.getName().equals(indexName))
                .findFirst();

            Optional<Index> firstGenSettings = backingIndexSupplier.apply(firstGenerationIndex);
            assertThat(firstGenSettings.isPresent(), is(true));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(firstGenSettings.get()).preferIlm(), is(true));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(firstGenSettings.get()).ilmPolicyName(), is(policy));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(firstGenSettings.get()).managedBy(), is(ManagedBy.ILM));
            Optional<Index> secondGenSettings = backingIndexSupplier.apply(secondGenerationIndex);
            assertThat(secondGenSettings.isPresent(), is(true));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(secondGenSettings.get()).preferIlm(), is(true));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(secondGenSettings.get()).ilmPolicyName(), is(policy));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(secondGenSettings.get()).managedBy(), is(ManagedBy.ILM));
            Optional<Index> writeIndexSettings = backingIndexSupplier.apply(writeIndex);
            assertThat(writeIndexSettings.isPresent(), is(true));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(writeIndexSettings.get()).preferIlm(), is(false));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(writeIndexSettings.get()).ilmPolicyName(), is(policy));
            assertThat(dataStreamInfo.getIndexSettingsValues().get(writeIndexSettings.get()).managedBy(), is(ManagedBy.LIFECYCLE));

            assertThat(dataStreamInfo.getNextGenerationManagedBy(), is(ManagedBy.LIFECYCLE));
        });

        putComposableIndexTemplate(indexTemplateName, null, List.of(dataStreamName + "*"), Settings.builder().build(), null, null);
        GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
        GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
            .actionGet();
        assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
        GetDataStreamAction.Response.DataStreamInfo dataStreamInfo = getDataStreamResponse.getDataStreams().get(0);
        assertThat(dataStreamInfo.getNextGenerationManagedBy(), is(ManagedBy.LIFECYCLE));

        client().execute(
            PutDataStreamLifecycleAction.INSTANCE,
            new PutDataStreamLifecycleAction.Request(new String[] { dataStreamName }, TimeValue.timeValueDays(90), false)
        ).actionGet();

        getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
        getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest).actionGet();
        assertThat(getDataStreamResponse.getDataStreams().size(), equalTo(1));
        dataStreamInfo = getDataStreamResponse.getDataStreams().get(0);
        assertThat(dataStreamInfo.getNextGenerationManagedBy(), is(ManagedBy.UNMANAGED));
    }

    static void indexDocs(String dataStream, int numDocs) {
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < numDocs; i++) {
            String value = DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER.formatMillis(System.currentTimeMillis());
            bulkRequest.add(
                new IndexRequest(dataStream).opType(DocWriteRequest.OpType.CREATE)
                    .source(String.format(Locale.ROOT, "{\"%s\":\"%s\"}", DEFAULT_TIMESTAMP_FIELD, value), XContentType.JSON)
            );
        }
        BulkResponse bulkResponse = client().bulk(bulkRequest).actionGet();
        assertThat(bulkResponse.getItems().length, equalTo(numDocs));
        String backingIndexPrefix = DataStream.BACKING_INDEX_PREFIX + dataStream;
        for (BulkItemResponse itemResponse : bulkResponse) {
            assertThat(itemResponse.getFailureMessage(), nullValue());
            assertThat(itemResponse.status(), equalTo(RestStatus.CREATED));
            assertThat(itemResponse.getIndex(), startsWith(backingIndexPrefix));
        }
        indicesAdmin().refresh(new RefreshRequest(dataStream)).actionGet();
    }

    static void putComposableIndexTemplate(
        String name,
        @Nullable String mappings,
        List<String> patterns,
        @Nullable Settings settings,
        @Nullable Map<String, Object> metadata,
        @Nullable DataStreamLifecycle lifecycle
    ) throws IOException {
        TransportPutComposableIndexTemplateAction.Request request = new TransportPutComposableIndexTemplateAction.Request(name);
        request.indexTemplate(
            ComposableIndexTemplate.builder()
                .indexPatterns(patterns)
                .template(new Template(settings, mappings == null ? null : CompressedXContent.fromJSON(mappings), null, lifecycle))
                .metadata(metadata)
                .dataStreamTemplate(new ComposableIndexTemplate.DataStreamTemplate())
                .build()
        );
        client().execute(TransportPutComposableIndexTemplateAction.TYPE, request).actionGet();
    }

    private static DataStreamLifecycle customEnabledLifecycle() {
        return DataStreamLifecycle.newBuilder().dataRetention(TimeValue.timeValueMillis(randomMillisUpToYear9999())).build();
    }

    private List<String> getBackingIndices(String dataStreamName) {
        GetDataStreamAction.Request getDataStreamRequest = new GetDataStreamAction.Request(new String[] { dataStreamName });
        GetDataStreamAction.Response getDataStreamResponse = client().execute(GetDataStreamAction.INSTANCE, getDataStreamRequest)
            .actionGet();
        return getDataStreamResponse.getDataStreams().get(0).getDataStream().getIndices().stream().map(Index::getName).toList();
    }
}
