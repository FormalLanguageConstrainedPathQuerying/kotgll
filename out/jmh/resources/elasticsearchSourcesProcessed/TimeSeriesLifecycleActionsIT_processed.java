/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ilm;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.cluster.metadata.DataStream;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.CheckedRunnable;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.engine.EngineConfig;
import org.elasticsearch.rest.action.admin.indices.RestPutIndexTemplateAction;
import org.elasticsearch.snapshots.SnapshotState;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.DeleteAction;
import org.elasticsearch.xpack.core.ilm.ErrorStep;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.FreezeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.LifecycleSettings;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.PhaseCompleteStep;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.SearchableSnapshotAction;
import org.elasticsearch.xpack.core.ilm.SetPriorityAction;
import org.elasticsearch.xpack.core.ilm.Step;
import org.elasticsearch.xpack.core.ilm.WaitForActiveShardsStep;
import org.elasticsearch.xpack.core.ilm.WaitForRolloverReadyStep;
import org.elasticsearch.xpack.core.ilm.WaitForSnapshotAction;
import org.hamcrest.Matchers;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createFullPolicy;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createIndexWithSettings;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createNewSingletonPolicy;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createSnapshotRepo;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.explainIndex;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getNumberOfPrimarySegments;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getOnlyIndexSettings;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getSnapshotState;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getStepKeyForIndex;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.index;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.indexDocument;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.updatePolicy;
import static org.elasticsearch.xpack.core.ilm.ShrinkIndexNameSupplier.SHRUNKEN_INDEX_PREFIX;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class TimeSeriesLifecycleActionsIT extends ESRestTestCase {
    private static final Logger logger = LogManager.getLogger(TimeSeriesLifecycleActionsIT.class);
    private static final String FAILED_STEP_RETRY_COUNT_FIELD = "failed_step_retry_count";

    private String index;
    private String policy;
    private String alias;

    @Before
    public void refreshIndex() {
        index = "index-" + randomAlphaOfLength(10).toLowerCase(Locale.ROOT);
        policy = "policy-" + randomAlphaOfLength(5);
        alias = "alias-" + randomAlphaOfLength(5);
    }

    public void testFullPolicy() throws Exception {
        String originalIndex = index + "-000001";
        String shrunkenOriginalIndex = SHRUNKEN_INDEX_PREFIX + originalIndex;
        String secondIndex = index + "-000002";
        createIndexWithSettings(
            client(),
            originalIndex,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 2)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put("index.routing.allocation.include._name", "javaRestTest-0")
                .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias)
        );

        createFullPolicy(client(), policy, TimeValue.ZERO);
        updatePolicy(client(), originalIndex, policy);
        index(client(), originalIndex, "_id", "foo", "bar");

        /*
         * These asserts are in the order that they should be satisfied in, in
         * order to maximize the time for all operations to complete.
         * An "out of order" assert here may result in this test occasionally
         * timing out and failing inappropriately.
         */
        assertBusy(() -> assertTrue(indexExists(secondIndex)));
        assertBusy(() -> assertFalse(indexExists(originalIndex)), 60, TimeUnit.SECONDS);
        assertBusy(() -> assertFalse(indexExists(shrunkenOriginalIndex)));
    }

    public void testRetryFailedDeleteAction() throws Exception {
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE);
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(IndexMetadata.SETTING_READ_ONLY, true)
                .put("index.lifecycle.name", policy)
        );

        assertBusy(
            () -> assertThat((Integer) explainIndex(client(), index).get(FAILED_STEP_RETRY_COUNT_FIELD), greaterThanOrEqualTo(1)),
            30,
            TimeUnit.SECONDS
        );
        assertTrue(indexExists(index));

        Request request = new Request("PUT", index + "/_settings");
        request.setJsonEntity("{\"index.blocks.read_only\":false}");
        assertOK(client().performRequest(request));

        assertBusy(() -> assertFalse(indexExists(index)));
    }

    public void testUpdatePolicyToNotContainFailedStep() throws Exception {
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE);
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(IndexMetadata.SETTING_READ_ONLY, true)
                .put("index.lifecycle.name", policy)
        );

        assertBusy(
            () -> assertThat((Integer) explainIndex(client(), index).get(FAILED_STEP_RETRY_COUNT_FIELD), greaterThanOrEqualTo(1)),
            30,
            TimeUnit.SECONDS
        );
        assertTrue(indexExists(index));

        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));

        Request request = new Request("PUT", index + "/_settings");
        request.setJsonEntity("{\"index.blocks.read_only\":false}");
        assertOK(client().performRequest(request));

        assertBusy(() -> assertFalse(indexExists(index)));
    }

    public void testFreezeNoop() throws Exception {
        createNewSingletonPolicy(client(), policy, "cold", FreezeAction.INSTANCE);

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put("index.lifecycle.name", policy)
        );

        assertBusy(
            () -> assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep("cold").getKey())),
            30,
            TimeUnit.SECONDS
        );
        assertFalse(getOnlyIndexSettings(client(), index).containsKey("index.frozen"));
    }

    public void testAllocateOnlyAllocation() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 2).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        String allocateNodeName = "javaRestTest-0,javaRestTest-1,javaRestTest-2,javaRestTest-3";
        AllocateAction allocateAction = new AllocateAction(null, null, singletonMap("_name", allocateNodeName), null, null);
        String endPhase = randomFrom("warm", "cold");
        createNewSingletonPolicy(client(), policy, endPhase, allocateAction);
        updatePolicy(client(), index, policy);
        assertBusy(
            () -> assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep(endPhase).getKey())),
            30,
            TimeUnit.SECONDS
        );
        ensureGreen(index);
    }

    public void testAllocateActionOnlyReplicas() throws Exception {
        int numShards = randomFrom(1, 5);
        int numReplicas = randomFrom(0, 1);
        int finalNumReplicas = (numReplicas + 1) % 2;
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, numShards)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, numReplicas)
        );
        AllocateAction allocateAction = new AllocateAction(finalNumReplicas, null, null, null, null);
        String endPhase = randomFrom("warm", "cold");
        createNewSingletonPolicy(client(), policy, endPhase, allocateAction);
        updatePolicy(client(), index, policy);
        assertBusy(() -> {
            Map<String, Object> settings = getOnlyIndexSettings(client(), index);
            assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep(endPhase).getKey()));
            assertThat(settings.get(IndexMetadata.INDEX_NUMBER_OF_REPLICAS_SETTING.getKey()), equalTo(String.valueOf(finalNumReplicas)));
        });
    }

    public void testWaitForSnapshot() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        String slmPolicy = randomAlphaOfLengthBetween(4, 10);
        String snapshotRepo = randomAlphaOfLengthBetween(4, 10);
        createSnapshotRepo(client(), snapshotRepo, randomBoolean());
        createSlmPolicy(slmPolicy, snapshotRepo);

        final String phaseName = "delete";
        createNewSingletonPolicy(client(), policy, phaseName, new WaitForSnapshotAction(slmPolicy));
        deleteSlmPolicy(slmPolicy); 
        updatePolicy(client(), index, policy);
        waitForPhaseTime(phaseName);
        assertBusy(() -> {
            Map<String, Object> indexILMState = explainIndex(client(), index);
            assertThat(indexILMState.get("action"), is("wait_for_snapshot"));
            assertThat(indexILMState.get("failed_step"), is("wait-for-snapshot"));
        }, slmPolicy);
        createSlmPolicy(slmPolicy, snapshotRepo); 
        assertBusy(() -> {
            Map<String, Object> indexILMState = explainIndex(client(), index);
            assertThat(indexILMState.get("failed_step"), nullValue());
            assertThat(indexILMState.get("action"), is("wait_for_snapshot"));
            assertThat(indexILMState.get("step"), is("wait-for-snapshot"));
        }, slmPolicy);
        waitForPhaseTime(phaseName); 

        Request request = new Request("PUT", "/_slm/policy/" + slmPolicy + "/_execute");
        assertOK(client().performRequest(request));
        assertBusy(() -> {
            Step.StepKey stepKey = getStepKeyForIndex(client(), index);
            logger.info("step key for index {} is {}", index, stepKey);
            assertThat(stepKey.action(), equalTo("complete"));
        }, slmPolicy);
    }

    /*
     * This test more rapidly creates a policy and then executes a snapshot, in an attempt to reproduce a timing bug where the snapshot
     * time gets set to a time earlier than the policy's action's time.
     */
    public void testWaitForSnapshotFast() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        String slmPolicy = randomAlphaOfLengthBetween(4, 10);
        String snapshotRepo = randomAlphaOfLengthBetween(4, 10);
        createSnapshotRepo(client(), snapshotRepo, randomBoolean());
        createSlmPolicy(slmPolicy, snapshotRepo);

        final String phaseName = "delete";
        createNewSingletonPolicy(client(), policy, phaseName, new WaitForSnapshotAction(slmPolicy));
        updatePolicy(client(), index, policy);
        waitForPhaseTime(phaseName);

        Request request = new Request("PUT", "/_slm/policy/" + slmPolicy + "/_execute");
        assertOK(client().performRequest(request));
        assertBusy(() -> {
            Step.StepKey stepKey = getStepKeyForIndex(client(), index);
            logger.info("step key for index {} is {}", index, stepKey);
            assertThat(stepKey.action(), equalTo("complete"));
        }, slmPolicy);
    }

    public void testWaitForSnapshotSlmExecutedBefore() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        String slmPolicy = randomAlphaOfLengthBetween(4, 10);
        String snapshotRepo = randomAlphaOfLengthBetween(4, 10);
        createSnapshotRepo(client(), snapshotRepo, randomBoolean());
        createSlmPolicy(slmPolicy, snapshotRepo);

        final String phaseName = "delete";
        createNewSingletonPolicy(client(), policy, phaseName, new WaitForSnapshotAction(slmPolicy));

        Request request = new Request("PUT", "/_slm/policy/" + slmPolicy + "/_execute");
        assertOK(client().performRequest(request));

        assertBusy(() -> {
            Response response = client().performRequest(new Request("GET", "/_slm/policy/" + slmPolicy));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                assertEquals(1, ((Map<?, ?>) ((Map<?, ?>) responseMap.get(slmPolicy)).get("stats")).get("snapshots_taken"));
            }
        }, slmPolicy);

        updatePolicy(client(), index, policy);
        waitForPhaseTime(phaseName);

        assertBusy(() -> {
            Map<String, Object> indexILMState = explainIndex(client(), index);
            assertThat(indexILMState.get("failed_step"), nullValue());
            assertThat(indexILMState.get("action"), is("wait_for_snapshot"));
            assertThat(indexILMState.get("step"), is("wait-for-snapshot"));
        }, slmPolicy);

        request = new Request("PUT", "/_slm/policy/" + slmPolicy + "/_execute");
        assertOK(client().performRequest(request));

        assertBusy(() -> {
            Response response = client().performRequest(new Request("GET", "/_slm/policy/" + slmPolicy));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                assertEquals(2, ((Map<?, ?>) ((Map<?, ?>) responseMap.get(slmPolicy)).get("stats")).get("snapshots_taken"));
            }
        }, slmPolicy);

        assertBusy(() -> {
            Step.StepKey stepKey = getStepKeyForIndex(client(), index);
            logger.info("stepKey for index {} is {}", index, stepKey);
            assertThat(stepKey.action(), equalTo("complete"));
        }, slmPolicy);
        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index).action(), equalTo("complete")), slmPolicy);
    }

    /*
     * This method waits until phase_time gets set in the state store for the given phase name. Otherwise we can wind up starting a snapshot
     * before the ILM policy is ready.
     */
    @SuppressWarnings("unchecked")
    private void waitForPhaseTime(String phaseName) throws Exception {
        assertBusy(() -> {
            Request request = new Request("GET", "/_cluster/state/metadata/" + index);
            Map<String, Object> response = entityAsMap(client().performRequest(request));
            Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
            Map<String, Object> indices = (Map<String, Object>) metadata.get("indices");
            Map<String, Object> indexMap = (Map<String, Object>) indices.get(index);
            Map<String, Object> ilm = (Map<String, Object>) indexMap.get("ilm");
            assertNotNull(ilm);
            Object phase = ilm.get("phase");
            assertEquals(phaseName, phase);
            Object phase_time = ilm.get("phase_time");
            assertNotNull(phase_time);
            logger.info("found phase time for {} phase: {}", phaseName, phase_time);
        });
    }

    public void testDelete() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE);
        updatePolicy(client(), index, policy);
        assertBusy(() -> assertFalse(indexExists(index)));
    }

    public void testDeleteOnlyShouldNotMakeIndexReadonly() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE, TimeValue.timeValueHours(1));
        updatePolicy(client(), index, policy);
        assertBusy(() -> {
            assertThat(getStepKeyForIndex(client(), index).action(), equalTo("complete"));
            Map<String, Object> settings = getOnlyIndexSettings(client(), index);
            assertThat(settings.get(IndexMetadata.INDEX_BLOCKS_WRITE_SETTING.getKey()), not("true"));
        });
        indexDocument(client(), index);
    }

    public void testDeleteDuringSnapshot() throws Exception {
        Request request = new Request("PUT", "/_snapshot/repo");
        request.setJsonEntity(
            Strings.toString(
                JsonXContent.contentBuilder()
                    .startObject()
                    .field("type", "fs")
                    .startObject("settings")
                    .field("compress", randomBoolean())
                    .field("location", System.getProperty("tests.path.repo"))
                    .field("max_snapshot_bytes_per_sec", "256b")
                    .endObject()
                    .endObject()
            )
        );
        assertOK(client().performRequest(request));
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE, TimeValue.timeValueMillis(0));
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        indexDocument(client(), index);
        String snapName = "snapshot-" + randomAlphaOfLength(6).toLowerCase(Locale.ROOT);
        request = new Request("PUT", "/_snapshot/repo/" + snapName);
        request.addParameter("wait_for_completion", "false");
        request.setJsonEntity("{\"indices\": \"" + index + "\"}");
        assertOK(client().performRequest(request));
        updatePolicy(client(), index, policy);
        assertBusy(() -> assertFalse(indexExists(index)), 2, TimeUnit.MINUTES);
        assertThat(getSnapshotState(client(), snapName), equalTo("SUCCESS"));
        assertOK(client().performRequest(new Request("DELETE", "/_snapshot/repo/" + snapName)));
    }

    public void checkForceMergeAction(String codec) throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
        );
        for (int i = 0; i < randomIntBetween(2, 10); i++) {
            Request request = new Request("PUT", index + "/_doc/" + i);
            request.addParameter("refresh", "true");
            request.setEntity(new StringEntity("{\"a\": \"test\"}", ContentType.APPLICATION_JSON));
            client().performRequest(request);
        }

        assertThat(getNumberOfPrimarySegments(client(), index), greaterThanOrEqualTo(1));
        createNewSingletonPolicy(client(), policy, "warm", new ForceMergeAction(1, codec));
        updatePolicy(client(), index, policy);

        assertBusy(() -> {
            assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep("warm").getKey()));
            Map<String, Object> settings = getOnlyIndexSettings(client(), index);
            assertThat(settings.get(EngineConfig.INDEX_CODEC_SETTING.getKey()), equalTo(codec));
            assertThat(settings.containsKey(IndexMetadata.INDEX_BLOCKS_WRITE_SETTING.getKey()), equalTo(false));
        }, 30, TimeUnit.SECONDS);

        indexDocument(client(), index);
    }

    public void testForceMergeAction() throws Exception {
        checkForceMergeAction(null);
    }

    public void testForceMergeActionWithCompressionCodec() throws Exception {
        checkForceMergeAction("best_compression");
    }

    public void testSetPriority() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(IndexMetadata.INDEX_PRIORITY_SETTING.getKey(), 100)
        );
        int priority = randomIntBetween(0, 99);
        createNewSingletonPolicy(client(), policy, "warm", new SetPriorityAction(priority));
        updatePolicy(client(), index, policy);
        assertBusy(() -> {
            Map<String, Object> settings = getOnlyIndexSettings(client(), index);
            assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep("warm").getKey()));
            assertThat(settings.get(IndexMetadata.INDEX_PRIORITY_SETTING.getKey()), equalTo(String.valueOf(priority)));
        });
    }

    public void testSetNullPriority() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(IndexMetadata.INDEX_PRIORITY_SETTING.getKey(), 100)
        );
        createNewSingletonPolicy(client(), policy, "warm", new SetPriorityAction((Integer) null));
        updatePolicy(client(), index, policy);
        assertBusy(() -> {
            Map<String, Object> settings = getOnlyIndexSettings(client(), index);
            assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep("warm").getKey()));
            assertNull(settings.get(IndexMetadata.INDEX_PRIORITY_SETTING.getKey()));
        });
    }

    @SuppressWarnings("unchecked")
    public void testNonexistentPolicy() throws Exception {
        String indexPrefix = randomAlphaOfLengthBetween(5, 15).toLowerCase(Locale.ROOT);
        final StringEntity template = new StringEntity(Strings.format("""
            {
              "index_patterns": "%s*",
              "settings": {
                "index": {
                  "lifecycle": {
                    "name": "does_not_exist",
                    "rollover_alias": "test_alias"
                  }
                }
              }
            }""", indexPrefix), ContentType.APPLICATION_JSON);
        Request templateRequest = new Request("PUT", "_template/test");
        templateRequest.setEntity(template);
        templateRequest.setOptions(expectWarnings(RestPutIndexTemplateAction.DEPRECATION_WARNING));
        client().performRequest(templateRequest);

        policy = randomAlphaOfLengthBetween(5, 20);
        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));

        index = indexPrefix + "-000001";
        final StringEntity putIndex = new StringEntity("""
            {
              "aliases": {
                "test_alias": {
                  "is_write_index": true
                }
              }
            }""", ContentType.APPLICATION_JSON);
        Request putIndexRequest = new Request("PUT", index);
        putIndexRequest.setEntity(putIndex);
        client().performRequest(putIndexRequest);
        indexDocument(client(), index);

        assertBusy(() -> {
            Request explainRequest = new Request("GET", index + "/_ilm/explain");
            Response response = client().performRequest(explainRequest);
            Map<String, Object> responseMap;
            try (InputStream is = response.getEntity().getContent()) {
                responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            }
            logger.info(responseMap);
            Map<String, Object> indexStatus = (Map<String, Object>) ((Map<String, Object>) responseMap.get("indices")).get(index);
            assertNull(indexStatus.get("phase"));
            assertNull(indexStatus.get("action"));
            assertEquals(ErrorStep.NAME, indexStatus.get("step"));
            Map<String, String> stepInfo = (Map<String, String>) indexStatus.get("step_info");
            assertNotNull(stepInfo);
            assertEquals("policy [does_not_exist] does not exist", stepInfo.get("reason"));
            assertEquals("illegal_argument_exception", stepInfo.get("type"));
        });
    }

    public void testInvalidPolicyNames() {
        ResponseException ex;

        policy = randomAlphaOfLengthBetween(0, 10) + "," + randomAlphaOfLengthBetween(0, 10);
        ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE)
        );
        assertThat(ex.getMessage(), containsString("invalid policy name"));

        policy = randomAlphaOfLengthBetween(0, 10) + "%20" + randomAlphaOfLengthBetween(0, 10);
        ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE)
        );
        assertThat(ex.getMessage(), containsString("invalid policy name"));

        policy = "_" + randomAlphaOfLengthBetween(1, 20);
        ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE)
        );
        assertThat(ex.getMessage(), containsString("invalid policy name"));

        policy = randomAlphaOfLengthBetween(256, 1000);
        ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE)
        );
        assertThat(ex.getMessage(), containsString("invalid policy name"));
    }

    public void testDeletePolicyInUse() throws IOException {
        String managedIndex1 = randomAlphaOfLength(7).toLowerCase(Locale.ROOT);
        String managedIndex2 = randomAlphaOfLength(8).toLowerCase(Locale.ROOT);
        String unmanagedIndex = randomAlphaOfLength(9).toLowerCase(Locale.ROOT);
        String managedByOtherPolicyIndex = randomAlphaOfLength(10).toLowerCase(Locale.ROOT);

        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE, TimeValue.timeValueHours(12));
        String originalPolicy = policy;
        String otherPolicy = randomValueOtherThan(policy, () -> randomAlphaOfLength(5));
        policy = otherPolicy;
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE, TimeValue.timeValueHours(13));

        createIndexWithSettings(
            client(),
            managedIndex1,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, randomIntBetween(1, 10))
                .put(LifecycleSettings.LIFECYCLE_NAME, originalPolicy)
        );
        createIndexWithSettings(
            client(),
            managedIndex2,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, randomIntBetween(1, 10))
                .put(LifecycleSettings.LIFECYCLE_NAME, originalPolicy)
        );
        createIndexWithSettings(
            client(),
            unmanagedIndex,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, randomIntBetween(1, 10))
        );
        createIndexWithSettings(
            client(),
            managedByOtherPolicyIndex,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, randomIntBetween(1, 10))
                .put(LifecycleSettings.LIFECYCLE_NAME, otherPolicy)
        );

        Request deleteRequest = new Request("DELETE", "_ilm/policy/" + originalPolicy);
        ResponseException ex = expectThrows(ResponseException.class, () -> client().performRequest(deleteRequest));
        assertThat(
            ex.getMessage(),
            Matchers.allOf(
                containsString("Cannot delete policy [" + originalPolicy + "]. It is in use by one or more indices: ["),
                containsString(managedIndex1),
                containsString(managedIndex2),
                not(containsString(unmanagedIndex)),
                not(containsString(managedByOtherPolicyIndex))
            )
        );
    }

    public void testRemoveAndReaddPolicy() throws Exception {
        String originalIndex = index + "-000001";
        String secondIndex = index + "-000002";
        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));
        createIndexWithSettings(
            client(),
            originalIndex,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy)
                .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias)
        );

        index(client(), originalIndex, "_id", "foo", "bar");

        assertBusy(() -> assertTrue(indexExists(secondIndex)));

        Request removeRequest = new Request("POST", "/" + originalIndex + "/_ilm/remove");
        removeRequest.setJsonEntity("");
        client().performRequest(removeRequest);

        Request addPolicyRequest = new Request("PUT", "/" + originalIndex + "/_settings");
        addPolicyRequest.setJsonEntity(Strings.format("""
            {
              "settings": {
                "index.lifecycle.name": "%s",
                "index.lifecycle.rollover_alias": "%s"
              }
            }""", policy, alias));
        client().performRequest(addPolicyRequest);
        assertBusy(() -> assertTrue((boolean) explainIndex(client(), originalIndex).getOrDefault("managed", false)));

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), originalIndex), equalTo(PhaseCompleteStep.finalStep("hot").getKey())));
    }

    public void testCanStopILMWithPolicyUsingNonexistentPolicy() throws Exception {
        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, randomAlphaOfLengthBetween(5, 15))
        );

        Request stopILMRequest = new Request("POST", "_ilm/stop");
        assertOK(client().performRequest(stopILMRequest));

        Request statusRequest = new Request("GET", "_ilm/status");
        assertBusy(() -> {
            Response statusResponse = client().performRequest(statusRequest);
            assertOK(statusResponse);
            Map<String, Object> statusResponseMap = entityAsMap(statusResponse);
            String status = (String) statusResponseMap.get("operation_mode");
            assertEquals("STOPPED", status);
        });

        Request startILMRequest = new Request("POST", "_ilm/start");
        assertOK(client().performRequest(startILMRequest));
    }

    public void testWaitForActiveShardsStep() throws Exception {
        String originalIndex = index + "-000001";
        String secondIndex = index + "-000002";
        createIndexWithSettings(
            client(),
            originalIndex,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias),
            true
        );

        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));
        updatePolicy(client(), originalIndex, policy);
        Request createIndexTemplate = new Request("PUT", "_template/rolling_indexes");
        createIndexTemplate.setJsonEntity(Strings.format("""
            {"index_patterns": ["%s-*"],
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 142,
                "index.write.wait_for_active_shards": "all"
              }
            }""", index));
        createIndexTemplate.setOptions(expectWarnings(RestPutIndexTemplateAction.DEPRECATION_WARNING));
        client().performRequest(createIndexTemplate);

        index(client(), originalIndex, "_id", "foo", "bar");
        assertBusy(() -> assertTrue(indexExists(secondIndex)));

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), originalIndex).name(), equalTo(WaitForActiveShardsStep.NAME)));

        updateIndexSettings(secondIndex, Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0));

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), originalIndex), equalTo(PhaseCompleteStep.finalStep("hot").getKey())));
    }

    public void testHistoryIsWrittenWithSuccess() throws Exception {
        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));
        Request createIndexTemplate = new Request("PUT", "_template/rolling_indexes");
        createIndexTemplate.setJsonEntity(Strings.format("""
            {
              "index_patterns": [ "%s-*" ],
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "index.lifecycle.name": "%s",
                "index.lifecycle.rollover_alias": "%s"
              }
            }""", index, policy, alias));
        createIndexTemplate.setOptions(expectWarnings(RestPutIndexTemplateAction.DEPRECATION_WARNING));
        client().performRequest(createIndexTemplate);

        createIndexWithSettings(client(), index + "-1", alias, Settings.builder(), true);

        index(client(), index + "-1", "1", "foo", "bar");
        Request refreshIndex = new Request("POST", "/" + index + "-1/_refresh");
        client().performRequest(refreshIndex);

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index + "-1"), equalTo(PhaseCompleteStep.finalStep("hot").getKey())));

        assertBusy(() -> assertHistoryIsPresent(policy, index + "-1", true, "check-rollover-ready"), 30, TimeUnit.SECONDS);
        assertBusy(() -> assertHistoryIsPresent(policy, index + "-1", true, "attempt-rollover"), 30, TimeUnit.SECONDS);
        assertBusy(() -> assertHistoryIsPresent(policy, index + "-1", true, "set-indexing-complete"), 30, TimeUnit.SECONDS);
        assertBusy(() -> assertHistoryIsPresent(policy, index + "-1", true, "complete"), 30, TimeUnit.SECONDS);

        assertBusy(() -> assertHistoryIsPresent(policy, index + "-000002", true, "check-rollover-ready"), 30, TimeUnit.SECONDS);
    }

    public void testHistoryIsWrittenWithFailure() throws Exception {
        createIndexWithSettings(client(), index + "-1", alias, Settings.builder(), false);
        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));
        updatePolicy(client(), index + "-1", policy);

        index(client(), index + "-1", "1", "foo", "bar");
        Request refreshIndex = new Request("POST", "/" + index + "-1/_refresh");
        client().performRequest(refreshIndex);

        assertBusy(
            () -> assertThat((Integer) explainIndex(client(), index + "-1").get("failed_step_retry_count"), greaterThanOrEqualTo(1))
        );

        assertBusy(() -> assertHistoryIsPresent(policy, index + "-1", false, "ERROR"), 30, TimeUnit.SECONDS);
    }

    public void testHistoryIsWrittenWithDeletion() throws Exception {
        createIndexWithSettings(client(), index, alias, Settings.builder(), false);
        createNewSingletonPolicy(client(), policy, "delete", DeleteAction.WITH_SNAPSHOT_DELETE);
        updatePolicy(client(), index, policy);

        assertBusy(() -> assertFalse(indexExists(index)));

        assertBusy(() -> {
            assertHistoryIsPresent(policy, index, true, "delete", "delete", "wait-for-shard-history-leases");
            assertHistoryIsPresent(policy, index, true, "delete", "delete", "complete");
        }, 30, TimeUnit.SECONDS);
    }

    public void testRetryableInitializationStep() throws Exception {
        String index = "retryinit-20xx-01-10";
        Request stopReq = new Request("POST", "/_ilm/stop");
        Request startReq = new Request("POST", "/_ilm/start");

        createNewSingletonPolicy(client(), policy, "hot", new SetPriorityAction(1));

        assertOK(client().performRequest(stopReq));

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy)
                .put(IndexSettings.LIFECYCLE_PARSE_ORIGINATION_DATE, false)
        );

        updateIndexSettings(index, Settings.builder().put(IndexSettings.LIFECYCLE_PARSE_ORIGINATION_DATE, true));

        assertOK(client().performRequest(startReq));

        assertBusy(
            () -> assertThat((Integer) explainIndex(client(), index).get(FAILED_STEP_RETRY_COUNT_FIELD), greaterThanOrEqualTo(1)),
            30,
            TimeUnit.SECONDS
        );

        updateIndexSettings(index, Settings.builder().put(IndexSettings.LIFECYCLE_PARSE_ORIGINATION_DATE, false));

        assertBusy(() -> {
            Map<String, Object> explainResp = explainIndex(client(), index);
            String phase = (String) explainResp.get("phase");
            assertThat(phase, equalTo("hot"));
        });
    }

    public void testRefreshablePhaseJson() throws Exception {
        String index = "refresh-index";

        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 100L, null, null, null, null, null, null));
        Request createIndexTemplate = new Request("PUT", "_template/rolling_indexes");
        createIndexTemplate.setJsonEntity(Strings.format("""
            {
              "index_patterns": ["%s-*"],
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "index.lifecycle.name": "%s",
                "index.lifecycle.rollover_alias": "%s"
              }
            }""", index, policy, alias));
        createIndexTemplate.setOptions(expectWarnings(RestPutIndexTemplateAction.DEPRECATION_WARNING));
        client().performRequest(createIndexTemplate);

        createIndexWithSettings(
            client(),
            index + "-1",
            alias,
            Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0),
            true
        );

        index(client(), index + "-1", "1", "foo", "bar");

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index + "-1").name(), equalTo(WaitForRolloverReadyStep.NAME)));

        createNewSingletonPolicy(client(), policy, "hot", new RolloverAction(null, null, null, 1L, null, null, null, null, null, null));

        assertBusy(() -> assertThat(indexExists(index + "-000002"), is(true)));
        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index + "-1").name(), equalTo(PhaseCompleteStep.NAME)));
    }

    public void testHaltAtEndOfPhase() throws Exception {
        String index = "halt-index";

        createNewSingletonPolicy(client(), policy, "hot", new SetPriorityAction(100));

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy),
            randomBoolean()
        );

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index), equalTo(PhaseCompleteStep.finalStep("hot").getKey())));

        {
            Map<String, LifecycleAction> hotActions = new HashMap<>();
            hotActions.put(SetPriorityAction.NAME, new SetPriorityAction(100));
            Map<String, Phase> phases = new HashMap<>();
            phases.put("hot", new Phase("hot", TimeValue.ZERO, hotActions));
            phases.put("delete", new Phase("delete", TimeValue.ZERO, singletonMap(DeleteAction.NAME, DeleteAction.WITH_SNAPSHOT_DELETE)));
            LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, phases);
            XContentBuilder builder = jsonBuilder();
            lifecyclePolicy.toXContent(builder, null);
            final StringEntity entity = new StringEntity("{ \"policy\":" + Strings.toString(builder) + "}", ContentType.APPLICATION_JSON);
            Request request = new Request("PUT", "_ilm/policy/" + policy);
            request.setEntity(entity);
            assertOK(client().performRequest(request));
        }

        assertBusy(() -> assertFalse("expected " + index + " to be deleted by ILM", indexExists(index)));
    }

    @SuppressWarnings("unchecked")
    public void testDeleteActionDoesntDeleteSearchableSnapshot() throws Exception {
        String snapshotRepo = randomAlphaOfLengthBetween(4, 10);
        createSnapshotRepo(client(), snapshotRepo, randomBoolean());

        Map<String, LifecycleAction> coldActions = Map.of(SearchableSnapshotAction.NAME, new SearchableSnapshotAction(snapshotRepo));
        Map<String, Phase> phases = new HashMap<>();
        phases.put("cold", new Phase("cold", TimeValue.ZERO, coldActions));
        phases.put(
            "delete",
            new Phase("delete", TimeValue.timeValueMillis(10000), singletonMap(DeleteAction.NAME, DeleteAction.NO_SNAPSHOT_DELETE))
        );
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policy, phases);
        XContentBuilder builder = jsonBuilder();
        lifecyclePolicy.toXContent(builder, null);
        final StringEntity entity = new StringEntity("{ \"policy\":" + Strings.toString(builder) + "}", ContentType.APPLICATION_JSON);
        Request createPolicyRequest = new Request("PUT", "_ilm/policy/" + policy);
        createPolicyRequest.setEntity(entity);
        assertOK(client().performRequest(createPolicyRequest));

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy),
            randomBoolean()
        );

        String[] snapshotName = new String[1];
        String restoredIndexName = SearchableSnapshotAction.FULL_RESTORED_INDEX_PREFIX + this.index;
        assertTrue(waitUntil(() -> {
            try {
                Map<String, Object> explainIndex = explainIndex(client(), index);
                if (explainIndex == null) {
                    explainIndex = explainIndex(client(), restoredIndexName);
                }
                snapshotName[0] = (String) explainIndex.get("snapshot_name");
                return snapshotName[0] != null;
            } catch (IOException e) {
                return false;
            }
        }, 30, TimeUnit.SECONDS));

        assertBusy(() -> {
            assertFalse(indexExists(index));
            assertFalse(indexExists(restoredIndexName));
        }, 90, TimeUnit.SECONDS);

        assertTrue("the snapshot we generate in the cold phase should not be deleted by the delete phase", waitUntil(() -> {
            try {
                Request getSnapshotsRequest = new Request("GET", "_snapshot/" + snapshotRepo + "/" + snapshotName[0]);
                Response getSnapshotsResponse = client().performRequest(getSnapshotsRequest);
                Map<String, Object> snapshotsResponseMap;
                try (InputStream is = getSnapshotsResponse.getEntity().getContent()) {
                    snapshotsResponseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                }
                if (snapshotsResponseMap.get("snapshots") != null) {
                    ArrayList<Object> snapshots = (ArrayList<Object>) snapshotsResponseMap.get("snapshots");
                    for (Object snapshot : snapshots) {
                        Map<String, Object> snapshotInfoMap = (Map<String, Object>) snapshot;
                        if (snapshotInfoMap.get("snapshot").equals(snapshotName[0]) &&
                            SnapshotState.valueOf((String) snapshotInfoMap.get("state")).completed()) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (IOException e) {
                return false;
            }
        }, 30, TimeUnit.SECONDS));
    }

    public void testSearchableSnapshotRequiresSnapshotRepoToExist() throws IOException {
        String repo = randomAlphaOfLengthBetween(4, 10);
        final String phaseName = "cold";
        ResponseException ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, phaseName, new SearchableSnapshotAction(repo))
        );
        assertThat(ex.getMessage(), containsString("no such repository"));
        assertThat(
            ex.getMessage(),
            containsString(
                "the snapshot repository referenced by the [searchable_snapshot] action "
                    + "in the [cold] phase must exist before it can be referenced by an ILM policy"
            )
        );
    }

    public void testWaitForSnapshotRequiresSLMPolicyToExist() throws IOException {
        String slmPolicy = randomAlphaOfLengthBetween(4, 10);
        final String phaseName = "delete";
        ResponseException ex = expectThrows(
            ResponseException.class,
            () -> createNewSingletonPolicy(client(), policy, phaseName, new WaitForSnapshotAction(slmPolicy))
        );
        assertThat(ex.getMessage(), containsString("no such snapshot lifecycle policy"));
        assertThat(
            ex.getMessage(),
            containsString(
                "the snapshot lifecycle policy referenced by the [wait_for_snapshot] action "
                    + "in the [delete] phase must exist before it can be referenced by an ILM policy"
            )
        );
    }

    private void assertHistoryIsPresent(String policyName, String indexName, boolean success, String stepName) throws IOException {
        assertHistoryIsPresent(policyName, indexName, success, null, null, stepName);
    }

    @SuppressWarnings("unchecked")
    private void assertHistoryIsPresent(
        String policyName,
        String indexName,
        boolean success,
        @Nullable String phase,
        @Nullable String action,
        String stepName
    ) throws IOException {
        logger.info(
            "--> checking for history item [{}], [{}], success: [{}], phase: [{}], action: [{}], step: [{}]",
            policyName,
            indexName,
            success,
            phase,
            action,
            stepName
        );
        final Request historySearchRequest = new Request("GET", "ilm-history*/_search?expand_wildcards=all");
        Object[] args = new Object[] {
            policyName,
            success,
            indexName,
            stepName,
            phase == null ? "" : Strings.format(",{\"term\": {\"state.phase\": \"%s\"}}", phase),
            action == null ? "" : ",{\"term\": {\"state.action\": \"" + action + "\"}}" };
        historySearchRequest.setJsonEntity(Strings.format("""
            {
              "query": {
                "bool": {
                  "must": [
                    {
                      "term": {
                        "policy": "%s"
                      }
                    },
                    {
                      "term": {
                        "success": %s
                      }
                    },
                    {
                      "term": {
                        "index": "%s"
                      }
                    },
                    {
                      "term": {
                        "state.step": "%s"
                      }
                    }
                    %s
                    %s
                  ]
                }
              }
            }""", args));
        Response historyResponse;
        try {
            historyResponse = client().performRequest(historySearchRequest);
            Map<String, Object> historyResponseMap;
            try (InputStream is = historyResponse.getEntity().getContent()) {
                historyResponseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            }
            logger.info("--> history response: {}", historyResponseMap);
            int hits = (int) ((Map<String, Object>) ((Map<String, Object>) historyResponseMap.get("hits")).get("total")).get("value");

            if (hits == 0) {
                final Request allResults = new Request("GET", "ilm-history*/_search");
                allResults.setJsonEntity(Strings.format("""
                    {
                      "query": {
                        "bool": {
                          "must": [
                            {
                              "term": {
                                "policy": "%s"
                              }
                            },
                            {
                              "term": {
                                "index": "%s"
                              }
                            }
                          ]
                        }
                      }
                    }""", policyName, indexName));
                final Response allResultsResp = client().performRequest(historySearchRequest);
                Map<String, Object> allResultsMap;
                try (InputStream is = allResultsResp.getEntity().getContent()) {
                    allResultsMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                }
                logger.info("--> expected at least 1 hit, got 0. All history for index [{}]: {}", indexName, allResultsMap);
            }
            assertThat(hits, greaterThanOrEqualTo(1));
        } catch (ResponseException e) {
            logger.error(e);
            fail("failed to perform search:" + e.getMessage());
        }

        String historyIndexName = DataStream.getDefaultBackingIndexName("ilm-history-7", 1);
        Response explainHistoryIndex = client().performRequest(new Request("GET", historyIndexName + "/_lifecycle/explain"));
        Map<String, Object> responseMap;
        try (InputStream is = explainHistoryIndex.getEntity().getContent()) {
            responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> indexResponse = ((Map<String, Map<String, Object>>) responseMap.get("indices"));
        Map<String, Object> historyIndexDSLExplain = indexResponse.get(historyIndexName);
        assertThat(historyIndexDSLExplain, is(notNullValue()));
        assertThat(historyIndexDSLExplain.get("managed_by_lifecycle"), is(true));
        assertThat(historyIndexDSLExplain.get("index_creation_date_millis"), is(notNullValue()));
    }

    private void createSlmPolicy(String smlPolicy, String repo) throws IOException {
        Request request;
        request = new Request("PUT", "/_slm/policy/" + smlPolicy);
        request.setJsonEntity(
            Strings.toString(
                JsonXContent.contentBuilder()
                    .startObject()
                    .field("schedule", "59 59 23 31 12 ? 2099")
                    .field("repository", repo)
                    .field("name", "snap" + randomAlphaOfLengthBetween(5, 10).toLowerCase(Locale.ROOT))
                    .startObject("config")
                    .field("include_global_state", false)
                    .endObject()
                    .endObject()
            )
        );

        assertOK(client().performRequest(request));
    }

    private void deleteSlmPolicy(String smlPolicy) throws IOException {
        assertOK(client().performRequest(new Request("DELETE", "/_slm/policy/" + smlPolicy)));
    }

    private void assertBusy(CheckedRunnable<Exception> runnable, String slmPolicy) throws Exception {
        assertBusy(() -> {
            try {
                runnable.run();
            } catch (AssertionError e) {
                Map<String, Object> slm;
                try (InputStream is = client().performRequest(new Request("GET", "/_slm/policy/" + slmPolicy)).getEntity().getContent()) {
                    slm = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, false);
                } catch (Exception ignored) {
                    slm = new HashMap<>();
                }
                throw new AssertionError("Index:" + explainIndex(client(), index) + "\nSLM:" + slm, e);
            }
        });
    }
}
