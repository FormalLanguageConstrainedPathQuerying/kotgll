/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.routing.allocation.DataTier;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Booleans;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.admin.indices.RestPutIndexTemplateAction;
import org.elasticsearch.test.XContentTestUtils;
import org.elasticsearch.test.XContentTestUtils.JsonMapView;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.elasticsearch.xpack.cluster.action.MigrateToDataTiersResponse;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.AllocationRoutedStep;
import org.elasticsearch.xpack.core.ilm.DeleteAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecycleSettings;
import org.elasticsearch.xpack.core.ilm.OperationMode;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.SetPriorityAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.junit.AfterClass;
import org.junit.Before;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createIndexWithSettings;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createNewSingletonPolicy;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.createPolicy;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getOnlyIndexSettings;
import static org.elasticsearch.xpack.TimeSeriesRestDriver.getStepKeyForIndex;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MigrateToDataTiersIT extends ESRestTestCase {
    private String index;
    private String policy;
    private String alias;

    @Before
    public void refreshIndexAndStartILM() throws IOException {
        index = "index-" + randomAlphaOfLength(10).toLowerCase(Locale.ROOT);
        policy = "policy-" + randomAlphaOfLength(5);
        alias = "alias-" + randomAlphaOfLength(5);
        assertOK(client().performRequest(new Request("POST", "_ilm/start")));
    }

    @AfterClass
    public static void restartIlm() throws IOException {
        assertOK(client().performRequest(new Request("POST", "_ilm/start")));
    }

    public void testAPIFailsIfILMIsNotStopped() throws IOException {
        Request migrateRequest = new Request("POST", "_ilm/migrate_to_data_tiers");
        ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest(migrateRequest));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), is(RestStatus.INTERNAL_SERVER_ERROR.getStatus()));
        assertThat(e.getMessage(), containsString("stop ILM before migrating to data tiers, current state is [RUNNING]"));
    }

    @SuppressWarnings("unchecked")
    public void testMigrateToDataTiersAction() throws Exception {
        String templateName = randomAlphaOfLengthBetween(10, 15).toLowerCase(Locale.ROOT);
        createLegacyTemplate(templateName);

        Map<String, LifecycleAction> hotActions = new HashMap<>();
        hotActions.put(SetPriorityAction.NAME, new SetPriorityAction(100));
        Map<String, LifecycleAction> warmActions = new HashMap<>();
        warmActions.put(SetPriorityAction.NAME, new SetPriorityAction(50));
        warmActions.put(ForceMergeAction.NAME, new ForceMergeAction(1, null));
        warmActions.put(AllocateAction.NAME, new AllocateAction(null, null, singletonMap("data", "warm"), null, null));
        warmActions.put(ShrinkAction.NAME, new ShrinkAction(1, null));
        Map<String, LifecycleAction> coldActions = new HashMap<>();
        coldActions.put(SetPriorityAction.NAME, new SetPriorityAction(0));
        coldActions.put(AllocateAction.NAME, new AllocateAction(0, null, null, null, singletonMap("data", "cold")));

        createPolicy(
            client(),
            policy,
            new Phase("hot", TimeValue.ZERO, hotActions),
            new Phase("warm", TimeValue.ZERO, warmActions),
            new Phase("cold", TimeValue.timeValueDays(100), coldActions),
            null,
            new Phase("delete", TimeValue.ZERO, singletonMap(DeleteAction.NAME, DeleteAction.WITH_SNAPSHOT_DELETE))
        );

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy)
                .putNull(DataTier.TIER_PREFERENCE)
                .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias)
        );

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index).phase(), equalTo("warm")), 30, TimeUnit.SECONDS);
        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index).name(), equalTo(AllocationRoutedStep.NAME)), 30, TimeUnit.SECONDS);

        String rolloverOnlyPolicyName = "rollover-policy";
        createNewSingletonPolicy(
            client(),
            rolloverOnlyPolicyName,
            "hot",
            new RolloverAction(null, null, null, 1L, null, null, null, null, null, null)
        );

        String rolloverIndexPrefix = "rolloverpolicytest_index";
        for (int i = 1; i <= 2; i++) {
            String rolloverIndex = rolloverIndexPrefix + "-00000" + i;
            createIndexWithSettings(
                client(),
                rolloverIndex,
                alias + i,
                Settings.builder()
                    .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                    .putNull(DataTier.TIER_PREFERENCE) 
                    .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias + i)
            );

            updateIndexSettings(rolloverIndex, Settings.builder().putNull(DataTier.TIER_PREFERENCE));
        }

        client().performRequest(new Request("POST", "_ilm/stop"));
        assertBusy(() -> {
            Response response = client().performRequest(new Request("GET", "_ilm/status"));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(OperationMode.STOPPED.toString()));
        });

        String indexWithDataWarmRouting = "indexwithdatawarmrouting";
        Settings.Builder settings = Settings.builder()
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
            .put(IndexMetadata.INDEX_ROUTING_REQUIRE_GROUP_SETTING.getKey() + "data", "warm");
        createIndex(indexWithDataWarmRouting, settings.build());

        updateIndexSettings(indexWithDataWarmRouting, Settings.builder().putNull(DataTier.TIER_PREFERENCE));

        Request migrateRequest = new Request("POST", "_ilm/migrate_to_data_tiers");
        migrateRequest.setJsonEntity(Strings.format("""
            {"legacy_template_to_delete": "%s", "node_attribute": "data"}
            """, templateName));
        Response migrateDeploymentResponse = client().performRequest(migrateRequest);
        assertOK(migrateDeploymentResponse);

        ensureGreen("indexwithdatawarmrouting");

        Map<String, Object> migrateResponseAsMap = responseAsMap(migrateDeploymentResponse);
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_ILM_POLICIES.getPreferredName()),
            contains(policy)
        );
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_INDICES.getPreferredName()),
            containsInAnyOrder(index, indexWithDataWarmRouting, rolloverIndexPrefix + "-000001", rolloverIndexPrefix + "-000002")
        );
        assertThat(migrateResponseAsMap.get(MigrateToDataTiersResponse.REMOVED_LEGACY_TEMPLATE.getPreferredName()), is(templateName));

        assertThat(migrateResponseAsMap.containsKey(MigrateToDataTiersResponse.MIGRATED_LEGACY_TEMPLATES.getPreferredName()), is(false));
        assertThat(
            migrateResponseAsMap.containsKey(MigrateToDataTiersResponse.MIGRATED_COMPOSABLE_TEMPLATES.getPreferredName()),
            is(false)
        );
        assertThat(migrateResponseAsMap.containsKey(MigrateToDataTiersResponse.MIGRATED_COMPONENT_TEMPLATES.getPreferredName()), is(false));

        Request getTemplateRequest = new Request("HEAD", "_template/" + templateName);
        assertThat(client().performRequest(getTemplateRequest).getStatusLine().getStatusCode(), is(RestStatus.NOT_FOUND.getStatus()));

        Map<String, Object> indexSettings = getOnlyIndexSettings(client(), indexWithDataWarmRouting);
        assertThat(indexSettings.get(DataTier.TIER_PREFERENCE), is("data_warm,data_hot"));

        Request getPolicy = new Request("GET", "/_ilm/policy/" + policy);
        Map<String, Object> policyAsMap = (Map<String, Object>) responseAsMap(client().performRequest(getPolicy)).get(policy);
        Map<String, Object> warmActionsMap = getActionsForPhase(policyAsMap, "warm");
        assertThat(warmActionsMap.size(), is(3));
        assertThat(warmActionsMap.get(AllocateAction.NAME), nullValue());
        Map<String, Object> coldActionsMap = getActionsForPhase(policyAsMap, "cold");
        assertThat(coldActionsMap.size(), is(2));
        assertThat(coldActionsMap.get(AllocateAction.NAME), notNullValue());
        Map<String, Object> coldAllocateActionMap = (Map<String, Object>) coldActionsMap.get(AllocateAction.NAME);
        assertThat((Map<String, Object>) coldAllocateActionMap.get("include"), is(anEmptyMap()));
        assertThat((Map<String, Object>) coldAllocateActionMap.get("require"), is(anEmptyMap()));
        assertThat((Map<String, Object>) coldAllocateActionMap.get("exclude"), is(anEmptyMap()));

        Request getClusterMetadataRequest = new Request("GET", "/_cluster/state/metadata/" + index);
        Response clusterMetadataResponse = client().performRequest(getClusterMetadataRequest);

        String cachedPhaseDefinition = getCachedPhaseDefAsMap(clusterMetadataResponse, index);
        assertThat(
            "the cached phase definition should reflect the migrated warm phase which must NOT contain an allocate action anymore",
            cachedPhaseDefinition,
            not(containsString(AllocateAction.NAME))
        );
        assertThat(cachedPhaseDefinition, containsString(ShrinkAction.NAME));
        assertThat(cachedPhaseDefinition, containsString(SetPriorityAction.NAME));
        assertThat(cachedPhaseDefinition, containsString(ForceMergeAction.NAME));

        Request getSettingsRequest = new Request("GET", "_cluster/settings?include_defaults");
        Response getSettingsResponse = client().performRequest(getSettingsRequest);
        JsonMapView json = XContentTestUtils.createJsonMapView(getSettingsResponse.getEntity().getContent());
        assertThat(json.get("persistent.cluster.routing.allocation.enforce_default_tier_preference"), nullValue());
        assertTrue(Booleans.parseBoolean(json.get("defaults.cluster.routing.allocation.enforce_default_tier_preference")));
    }

    @SuppressWarnings("unchecked")
    public void testIndexTemplatesMigration() throws Exception {
        String legacyTemplateToMigrate = "legacy_to_migrate";
        {
            Request legacyTemplateToMigrateReq = new Request("PUT", "/_template/" + legacyTemplateToMigrate);
            Settings indexSettings = Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.routing.allocation.require.data", "hot")
                .build();
            legacyTemplateToMigrateReq.setJsonEntity(
                "{\"index_patterns\":  [\"legacynotreallyimportant-*\"], \"settings\":  " + Strings.toString(indexSettings) + "}"
            );
            legacyTemplateToMigrateReq.setOptions(
                expectWarnings("Legacy index templates are deprecated in favor of composable templates" + ".")
            );
            assertOK(client().performRequest(legacyTemplateToMigrateReq));
        }

        String legacyTemplate = "legacy_template";
        {
            Request legacyTemplateRequest = new Request("PUT", "/_template/" + legacyTemplate);
            Settings indexSettings = Settings.builder().put("index.number_of_shards", 1).build();
            legacyTemplateRequest.setJsonEntity(
                "{\"index_patterns\":  [\"legacynotreallyimportant-*\"], \"settings\":  " + Strings.toString(indexSettings) + "}"
            );
            legacyTemplateRequest.setOptions(expectWarnings("Legacy index templates are deprecated in favor of composable templates."));
            assertOK(client().performRequest(legacyTemplateRequest));
        }

        String composableTemplateToMigrate = "to_migrate_composable_template";
        {
            Request toMigrateComposableTemplateReq = new Request("PUT", "/_index_template/" + composableTemplateToMigrate);
            Settings indexSettings = Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.routing.allocation.require.data", "hot")
                .build();
            toMigrateComposableTemplateReq.setJsonEntity(
                "{\"index_patterns\":  [\"0notreallyimportant-*\"], \"template\":{\"settings\":  " + Strings.toString(indexSettings) + "}}"
            );
            assertOK(client().performRequest(toMigrateComposableTemplateReq));
        }

        String composableTemplate = "no_need_to_migrate_composable_template";
        {
            Request composableTemplateRequest = new Request("PUT", "/_index_template/" + composableTemplate);
            Settings indexSettings = indexSettings(1, 0).build();
            composableTemplateRequest.setJsonEntity(
                "{\"index_patterns\":  [\"1notreallyimportant-*\"], \"template\":{\"settings\":  " + Strings.toString(indexSettings) + "}}"
            );
            assertOK(client().performRequest(composableTemplateRequest));
        }

        String componentTemplateToMigrate = "to_migrate_component_template";
        {
            Request componentTemplateRequest = new Request("PUT", "/_component_template/" + componentTemplateToMigrate);
            Settings indexSettings = Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.routing.allocation.require.data", "hot")
                .build();
            componentTemplateRequest.setJsonEntity("{\"template\":{\"settings\":  " + Strings.toString(indexSettings) + "}}");
            assertOK(client().performRequest(componentTemplateRequest));
        }

        String componentTemplate = "no_need_to_migrate_component_template";
        {
            Request componentTemplateRequest = new Request("PUT", "/_component_template/" + componentTemplate);
            Settings indexSettings = Settings.builder().put("index.number_of_shards", 1).build();
            componentTemplateRequest.setJsonEntity("{\"template\":{\"settings\":  " + Strings.toString(indexSettings) + "}}");
            assertOK(client().performRequest(componentTemplateRequest));
        }

        boolean dryRun = randomBoolean();
        if (dryRun == false) {
            client().performRequest(new Request("POST", "_ilm/stop"));
            assertBusy(() -> {
                Response response = client().performRequest(new Request("GET", "_ilm/status"));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(OperationMode.STOPPED.toString()));
            });
        }

        Request migrateRequest = new Request("POST", "_ilm/migrate_to_data_tiers");
        migrateRequest.addParameter("dry_run", String.valueOf(dryRun));
        migrateRequest.setJsonEntity("""
            { "node_attribute": "data"}
            """);
        Response migrateDeploymentResponse = client().performRequest(migrateRequest);
        assertOK(migrateDeploymentResponse);

        Map<String, Object> migrateResponseAsMap = responseAsMap(migrateDeploymentResponse);
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_LEGACY_TEMPLATES.getPreferredName()),
            is(List.of(legacyTemplateToMigrate))
        );
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_COMPOSABLE_TEMPLATES.getPreferredName()),
            is(List.of(composableTemplateToMigrate))
        );
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_COMPONENT_TEMPLATES.getPreferredName()),
            is(List.of(componentTemplateToMigrate))
        );
        assertThat(migrateResponseAsMap.get(MigrateToDataTiersResponse.DRY_RUN.getPreferredName()), is(dryRun));
    }

    @SuppressWarnings("unchecked")
    public void testMigrationDryRun() throws Exception {
        String templateName = randomAlphaOfLengthBetween(10, 15).toLowerCase(Locale.ROOT);
        createLegacyTemplate(templateName);

        Map<String, LifecycleAction> hotActions = new HashMap<>();
        hotActions.put(SetPriorityAction.NAME, new SetPriorityAction(100));
        Map<String, LifecycleAction> warmActions = new HashMap<>();
        warmActions.put(SetPriorityAction.NAME, new SetPriorityAction(50));
        warmActions.put(ForceMergeAction.NAME, new ForceMergeAction(1, null));
        warmActions.put(AllocateAction.NAME, new AllocateAction(null, null, singletonMap("data", "warm"), null, null));
        warmActions.put(ShrinkAction.NAME, new ShrinkAction(1, null));
        Map<String, LifecycleAction> coldActions = new HashMap<>();
        coldActions.put(SetPriorityAction.NAME, new SetPriorityAction(0));
        coldActions.put(AllocateAction.NAME, new AllocateAction(0, null, null, null, singletonMap("data", "cold")));

        createPolicy(
            client(),
            policy,
            new Phase("hot", TimeValue.ZERO, hotActions),
            new Phase("warm", TimeValue.ZERO, warmActions),
            new Phase("cold", TimeValue.timeValueDays(100), coldActions),
            null,
            new Phase("delete", TimeValue.ZERO, singletonMap(DeleteAction.NAME, DeleteAction.WITH_SNAPSHOT_DELETE))
        );

        createIndexWithSettings(
            client(),
            index,
            alias,
            Settings.builder()
                .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(LifecycleSettings.LIFECYCLE_NAME, policy)
                .putNull(DataTier.TIER_PREFERENCE) 
                .put(RolloverAction.LIFECYCLE_ROLLOVER_ALIAS, alias)
        );

        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index).phase(), equalTo("warm")), 30, TimeUnit.SECONDS);
        assertBusy(() -> assertThat(getStepKeyForIndex(client(), index).name(), equalTo(AllocationRoutedStep.NAME)), 30, TimeUnit.SECONDS);

        String indexWithDataWarmRouting = "indexwithdatawarmrouting";
        Settings.Builder settings = Settings.builder()
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
            .put(IndexMetadata.INDEX_ROUTING_REQUIRE_GROUP_SETTING.getKey() + "data", "warm");
        createIndex(indexWithDataWarmRouting, settings.build());

        updateIndexSettings(indexWithDataWarmRouting, Settings.builder().putNull(DataTier.TIER_PREFERENCE));

        Request migrateRequest = new Request("POST", "_ilm/migrate_to_data_tiers");
        migrateRequest.addParameter("dry_run", "true");
        migrateRequest.setJsonEntity("{\"legacy_template_to_delete\": \"" + templateName + "\", \"node_attribute\": \"data\"}");
        Response migrateDeploymentResponse = client().performRequest(migrateRequest);
        assertOK(migrateDeploymentResponse);

        Map<String, Object> migrateResponseAsMap = responseAsMap(migrateDeploymentResponse);
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_ILM_POLICIES.getPreferredName()),
            containsInAnyOrder(policy)
        );
        assertThat(
            (List<String>) migrateResponseAsMap.get(MigrateToDataTiersResponse.MIGRATED_INDICES.getPreferredName()),
            containsInAnyOrder(index, indexWithDataWarmRouting)
        );
        assertThat(migrateResponseAsMap.get(MigrateToDataTiersResponse.REMOVED_LEGACY_TEMPLATE.getPreferredName()), is(templateName));

        Request getTemplateRequest = new Request("HEAD", "_template/" + templateName);
        assertThat(client().performRequest(getTemplateRequest).getStatusLine().getStatusCode(), is(RestStatus.OK.getStatus()));

        Map<String, Object> indexSettings = getOnlyIndexSettings(client(), indexWithDataWarmRouting);
        assertThat(indexSettings.get(DataTier.TIER_PREFERENCE), nullValue());

        Request getPolicy = new Request("GET", "/_ilm/policy/" + policy);
        Map<String, Object> policyAsMap = (Map<String, Object>) responseAsMap(client().performRequest(getPolicy)).get(policy);
        Map<String, Object> warmActionsMap = getActionsForPhase(policyAsMap, "warm");
        assertThat(warmActionsMap.size(), is(4));
        assertThat(warmActionsMap.get(AllocateAction.NAME), notNullValue());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getActionsForPhase(Map<String, Object> policyAsMap, String phase) {
        Map<String, Object> phases = (Map<String, Object>) ((Map<String, Object>) policyAsMap.get("policy")).get("phases");
        return (Map<String, Object>) ((Map<String, Object>) phases.get(phase)).get("actions");
    }

    @SuppressWarnings("unchecked")
    private String getCachedPhaseDefAsMap(Response clusterMetadataResponse, String indexName) throws IOException {
        Map<String, Object> clusterMetadataMap = responseAsMap(clusterMetadataResponse);
        Map<String, Object> metadata = (Map<String, Object>) clusterMetadataMap.get("metadata");
        Map<String, Object> indices = (Map<String, Object>) metadata.get("indices");
        Map<String, Object> indexMetadata = (Map<String, Object>) indices.get(indexName);
        Map<String, Object> ilmMetadata = (Map<String, Object>) indexMetadata.get("ilm");
        return (String) ilmMetadata.get("phase_definition");
    }

    private void createLegacyTemplate(String templateName) throws IOException {
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
        Request templateRequest = new Request("PUT", "_template/" + templateName);
        templateRequest.setEntity(template);
        templateRequest.setOptions(expectWarnings(RestPutIndexTemplateAction.DEPRECATION_WARNING));
        client().performRequest(templateRequest);
    }
}
