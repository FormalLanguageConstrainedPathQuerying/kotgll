/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ilm;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.ccr.Ccr;
import org.elasticsearch.xpack.core.LocalStateCompositeXPackPlugin;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.ilm.ExplainLifecycleRequest;
import org.elasticsearch.xpack.core.ilm.ExplainLifecycleResponse;
import org.elasticsearch.xpack.core.ilm.IndexLifecycleExplainResponse;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.LifecycleSettings;
import org.elasticsearch.xpack.core.ilm.MigrateAction;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.PhaseCompleteStep;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.ShrunkShardsAllocatedStep;
import org.elasticsearch.xpack.core.ilm.Step;
import org.elasticsearch.xpack.core.ilm.action.ExplainLifecycleAction;
import org.elasticsearch.xpack.core.ilm.action.ILMActions;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleRequest;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.elasticsearch.cluster.metadata.IndexMetadata.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetadata.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0)
public class ClusterStateWaitThresholdBreachTests extends ESIntegTestCase {

    private String policy;
    private String managedIndex;

    @Before
    public void refreshDataStreamAndPolicy() {
        policy = "policy-" + randomAlphaOfLength(5);
        managedIndex = "index-" + randomAlphaOfLengthBetween(10, 15).toLowerCase(Locale.ROOT);
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(LocalStateCompositeXPackPlugin.class, IndexLifecycle.class, Ccr.class);
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        Settings.Builder settings = Settings.builder().put(super.nodeSettings(nodeOrdinal, otherSettings));
        settings.put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), false);
        settings.put(XPackSettings.SECURITY_ENABLED.getKey(), false);
        settings.put(XPackSettings.WATCHER_ENABLED.getKey(), false);
        settings.put(XPackSettings.GRAPH_ENABLED.getKey(), false);
        settings.put(LifecycleSettings.LIFECYCLE_POLL_INTERVAL, "1s");
        settings.put(LifecycleSettings.LIFECYCLE_HISTORY_INDEX_ENABLED, false);
        return settings.build();
    }

    public void testWaitInShrunkShardsAllocatedExceedsThreshold() throws Exception {
        List<String> masterOnlyNodes = internalCluster().startMasterOnlyNodes(1, Settings.EMPTY);
        internalCluster().startDataOnlyNode();

        int numShards = 2;
        Phase warmPhase = new Phase(
            "warm",
            TimeValue.ZERO,
            Map.of(MigrateAction.NAME, MigrateAction.DISABLED, ShrinkAction.NAME, new ShrinkAction(1, null, false))
        );
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, Map.of("warm", warmPhase));
        PutLifecycleRequest putLifecycleRequest = new PutLifecycleRequest(lifecyclePolicy);
        assertAcked(client().execute(ILMActions.PUT, putLifecycleRequest).get());

        Settings settings = Settings.builder()
            .put(indexSettings())
            .put(SETTING_NUMBER_OF_SHARDS, numShards)
            .put(SETTING_NUMBER_OF_REPLICAS, 42)
            .put(LifecycleSettings.LIFECYCLE_NAME, policy)
            .put(LifecycleSettings.LIFECYCLE_STEP_WAIT_TIME_THRESHOLD, "1h")
            .build();
        CreateIndexResponse res = indicesAdmin().prepareCreate(managedIndex).setSettings(settings).get();
        assertTrue(res.isAcknowledged());

        String[] firstAttemptShrinkIndexName = new String[1];
        assertBusy(() -> {
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(managedIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse indexLifecycleExplainResponse = explainResponse.getIndexResponses().get(managedIndex);
            firstAttemptShrinkIndexName[0] = indexLifecycleExplainResponse.getShrinkIndexName();
            assertThat(firstAttemptShrinkIndexName[0], is(notNullValue()));
        }, 30, TimeUnit.SECONDS);

        assertBusy(() -> {
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(managedIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse indexLifecycleExplainResponse = explainResponse.getIndexResponses().get(managedIndex);
            assertThat(indexLifecycleExplainResponse.getStep(), is(ShrunkShardsAllocatedStep.NAME));
        }, 30, TimeUnit.SECONDS);

        IndexMetadata managedIndexMetadata = clusterService().state().metadata().index(managedIndex);
        Step.StepKey currentStepKey = new Step.StepKey("warm", ShrinkAction.NAME, ShrunkShardsAllocatedStep.NAME);

        String masterNode = masterOnlyNodes.get(0);
        IndexLifecycleService indexLifecycleService = internalCluster().getInstance(IndexLifecycleService.class, masterNode);
        ClusterService clusterService = internalCluster().getInstance(ClusterService.class, masterNode);
        LongSupplier nowWayBackInThePastSupplier = () -> 1234L;
        clusterService.submitUnbatchedStateUpdateTask("testing-move-to-step-to-manipulate-step-time", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return new MoveToNextStepUpdateTask(
                    managedIndexMetadata.getIndex(),
                    policy,
                    currentStepKey,
                    currentStepKey,
                    nowWayBackInThePastSupplier,
                    indexLifecycleService.getPolicyRegistry(),
                    state -> {}
                ).execute(currentState);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });

        String[] secondCycleShrinkIndexName = new String[1];
        assertBusy(() -> {
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(managedIndex);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();

            IndexLifecycleExplainResponse indexLifecycleExplainResponse = explainResponse.getIndexResponses().get(managedIndex);
            secondCycleShrinkIndexName[0] = indexLifecycleExplainResponse.getShrinkIndexName();
            assertThat(secondCycleShrinkIndexName[0], notNullValue());
            assertThat(secondCycleShrinkIndexName[0], not(equalTo(firstAttemptShrinkIndexName[0])));
        }, 30, TimeUnit.SECONDS);

        assertBusy(() -> assertFalse(indexExists(firstAttemptShrinkIndexName[0])));

        assertBusy(() -> assertTrue(indexExists(secondCycleShrinkIndexName[0])), 30, TimeUnit.SECONDS);

        setReplicaCount(0, secondCycleShrinkIndexName[0]);
        assertBusy(() -> {
            ExplainLifecycleRequest explainRequest = new ExplainLifecycleRequest().indices(secondCycleShrinkIndexName[0]);
            ExplainLifecycleResponse explainResponse = client().execute(ExplainLifecycleAction.INSTANCE, explainRequest).get();
            IndexLifecycleExplainResponse indexLifecycleExplainResponse = explainResponse.getIndexResponses()
                .get(secondCycleShrinkIndexName[0]);
            assertThat(indexLifecycleExplainResponse.getPhase(), equalTo("warm"));
            assertThat(indexLifecycleExplainResponse.getStep(), equalTo(PhaseCompleteStep.NAME));
        }, 30, TimeUnit.SECONDS);
    }
}
