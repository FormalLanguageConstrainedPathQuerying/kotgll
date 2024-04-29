/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ilm;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.ObjectPath;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.elasticsearch.xpack.ccr.ESCCRRestTestCase;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.UnfollowAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.core.ilm.ShrinkIndexNameSupplier.SHRUNKEN_INDEX_PREFIX;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CCRIndexLifecycleIT extends ESCCRRestTestCase {

    private static final Logger LOGGER = LogManager.getLogger(CCRIndexLifecycleIT.class);

    public void testBasicCCRAndILMIntegration() throws Exception {
        String indexName = "logs-1";

        String policyName = "basic-test";
        if ("leader".equals(targetCluster)) {
            putILMPolicy(policyName, "50GB", null, TimeValue.timeValueHours(7 * 24));
            Settings indexSettings = indexSettings(1, 0).put("index.lifecycle.name", policyName)
                .put("index.lifecycle.rollover_alias", "logs")
                .build();
            createIndex(indexName, indexSettings, "", "\"logs\": { }");
            ensureGreen(indexName);
        } else if ("follow".equals(targetCluster)) {
            putILMPolicy(policyName, "50GB", null, TimeValue.timeValueHours(7 * 24));
            followIndex(indexName, indexName);
            ensureGreen(indexName);

            assertBusy(() -> assertOK(client().performRequest(new Request("HEAD", "/" + indexName + "/_alias/logs"))));

            try (RestClient leaderClient = buildLeaderClient()) {
                index(leaderClient, indexName, "1");
                assertDocumentExists(leaderClient, indexName, "1");

                assertBusy(() -> {
                    assertDocumentExists(client(), indexName, "1");
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), equalTo("true"));

                    assertILMPolicy(leaderClient, indexName, policyName, "hot");
                    assertILMPolicy(client(), indexName, policyName, "hot");
                });

                updateIndexSettings(leaderClient, indexName, Settings.builder().put("index.lifecycle.indexing_complete", true).build());

                assertBusy(() -> {
                    assertThat(getIndexSetting(leaderClient, indexName, "index.lifecycle.indexing_complete"), equalTo("true"));
                    assertThat(getIndexSetting(client(), indexName, "index.lifecycle.indexing_complete"), equalTo("true"));

                    assertILMPolicy(leaderClient, indexName, policyName, "warm");
                    assertILMPolicy(client(), indexName, policyName, "warm");

                    assertThat(getIndexSetting(leaderClient, indexName, "index.blocks.write"), equalTo("true"));
                    assertThat(getIndexSetting(client(), indexName, "index.blocks.write"), equalTo("true"));
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), nullValue());
                });
            }
        } else {
            fail("unexpected target cluster [" + targetCluster + "]");
        }
    }

    public void testCCRUnfollowDuringSnapshot() throws Exception {
        String indexName = "unfollow-test-index";
        if ("leader".equals(targetCluster)) {
            createIndex(adminClient(), indexName, indexSettings(2, 0).build());
            ensureGreen(indexName);
        } else if ("follow".equals(targetCluster)) {
            createNewSingletonPolicy("unfollow-only", "hot", UnfollowAction.INSTANCE, TimeValue.ZERO);
            followIndex(indexName, indexName);
            ensureGreen(indexName);

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

            try (RestClient leaderClient = buildLeaderClient()) {
                index(leaderClient, indexName, "1");
                assertDocumentExists(leaderClient, indexName, "1");

                updateIndexSettings(leaderClient, indexName, Settings.builder().put("index.lifecycle.indexing_complete", true).build());

                String snapName = "snapshot-" + randomAlphaOfLength(10).toLowerCase(Locale.ROOT);
                request = new Request("PUT", "/_snapshot/repo/" + snapName);
                request.addParameter("wait_for_completion", "false");
                request.setJsonEntity("{\"indices\": \"" + indexName + "\"}");
                assertOK(client().performRequest(request));

                logger.info("--> starting unfollow");
                updatePolicy(indexName, "unfollow-only");

                assertBusy(() -> {
                    assertThat(getIndexSetting(leaderClient, indexName, "index.lifecycle.indexing_complete"), equalTo("true"));
                    assertThat(getIndexSetting(client(), indexName, "index.lifecycle.indexing_complete"), equalTo("true"));
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), nullValue());
                    assertDocumentExists(client(), indexName, "1");
                    assertILMPolicy(client(), indexName, "unfollow-only", "hot", "complete", "complete");
                }, 2, TimeUnit.MINUTES);

                assertThat(getSnapshotState(snapName), equalTo("SUCCESS"));
                assertOK(client().performRequest(new Request("DELETE", "/_snapshot/repo/" + snapName)));
            }
        } else {
            fail("unexpected target cluster [" + targetCluster + "]");
        }
    }

    public void testCcrAndIlmWithRollover() throws Exception {
        String alias = "mymetrics";
        String indexName = "mymetrics-000001";
        String nextIndexName = "mymetrics-000002";
        String policyName = "rollover-test";

        if ("leader".equals(targetCluster)) {
            putILMPolicy(policyName, null, 1, null);
            Request templateRequest = new Request("PUT", "/_index_template/my_template");
            Settings indexSettings = indexSettings(1, 0).put("index.lifecycle.name", policyName)
                .put("index.lifecycle.rollover_alias", alias)
                .build();
            templateRequest.setJsonEntity(
                "{\"index_patterns\":  [\"mymetrics-*\"], \"template\":{\"settings\":  " + Strings.toString(indexSettings) + "}}"
            );
            assertOK(client().performRequest(templateRequest));
        } else if ("follow".equals(targetCluster)) {
            putILMPolicy(policyName, null, 1, null);

            Request createAutoFollowRequest = new Request("PUT", "/_ccr/auto_follow/my_auto_follow_pattern");
            createAutoFollowRequest.setJsonEntity("""
                {
                  "leader_index_patterns": [ "mymetrics-*" ],
                  "remote_cluster": "leader_cluster",
                  "read_poll_timeout": "1000ms"
                }""");
            assertOK(client().performRequest(createAutoFollowRequest));

            try (RestClient leaderClient = buildLeaderClient()) {
                Request createIndexRequest = new Request("PUT", "/" + indexName);
                createIndexRequest.setJsonEntity(Strings.format("""
                    {
                      "mappings": {
                        "properties": {
                          "field": {
                            "type": "keyword"
                          }
                        }
                      },
                      "aliases": {
                        "%s": {
                          "is_write_index": true
                        }
                      }
                    }""", alias));
                assertOK(leaderClient.performRequest(createIndexRequest));
                Request checkIndexRequest = new Request("GET", "/_cluster/health/" + indexName);
                checkIndexRequest.addParameter("wait_for_status", "green");
                checkIndexRequest.addParameter("timeout", "70s");
                checkIndexRequest.addParameter("level", "shards");
                assertOK(leaderClient.performRequest(checkIndexRequest));

                assertBusy(() -> assertTrue(indexExists(indexName)));

                assertBusy(() -> assertOK(client().performRequest(new Request("HEAD", "/" + indexName + "/_alias/" + alias))));

                index(leaderClient, indexName, "1");
                assertDocumentExists(leaderClient, indexName, "1");

                ensureGreen(indexName);
                assertBusy(() -> {
                    assertDocumentExists(client(), indexName, "1");
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), equalTo("true"));
                });

                assertBusy(() -> {
                    assertOK(leaderClient.performRequest(new Request("HEAD", "/" + nextIndexName)));
                    assertThat(getIndexSetting(leaderClient, indexName, "index.lifecycle.indexing_complete"), equalTo("true"));

                });

                assertBusy(() -> {
                    assertOK(leaderClient.performRequest(new Request("HEAD", "/" + nextIndexName)));
                    assertThat(getIndexSetting(leaderClient, indexName, "index.blocks.write"), equalTo("true"));
                    assertThat(getIndexSetting(leaderClient, indexName, "index.lifecycle.indexing_complete"), equalTo("true"));
                });

                assertBusy(() -> {
                    assertThat(getIndexSetting(client(), indexName, "index.lifecycle.indexing_complete"), equalTo("true"));
                });

                assertBusy(() -> {
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), nullValue());
                    indexExists(nextIndexName);
                    assertOK(client().performRequest(new Request("HEAD", "/" + nextIndexName + "/_alias/" + alias)));
                });

                assertBusy(() -> {
                    assertILMPolicy(client(), indexName, policyName, "warm");
                });

                leaderClient.performRequest(new Request("DELETE", "/_index_template/my_template"));
            }
        } else {
            fail("unexpected target cluster [" + targetCluster + "]");
        }
    }

    public void testAliasReplicatedOnShrink() throws Exception {
        final String indexName = "shrink-alias-test";
        final String policyName = "shrink-test-policy";
        final int numberOfAliases = randomIntBetween(0, 4);

        if ("leader".equals(targetCluster)) {
            Settings indexSettings = indexSettings(3, 0).put("index.lifecycle.name", policyName).build();
            final StringBuilder aliases = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < numberOfAliases; i++) {
                if (first == false) {
                    aliases.append(",");
                }
                final Boolean isWriteIndex = randomFrom(new Boolean[] { null, false, true });
                if (isWriteIndex == null) {
                    aliases.append("\"alias_").append(i).append("\":{}");
                } else {
                    aliases.append("\"alias_").append(i).append("\":{\"is_write_index\":").append(isWriteIndex).append("}");
                }
                first = false;
            }
            createIndex(indexName, indexSettings, "", aliases.toString());
            ensureGreen(indexName);
        } else if ("follow".equals(targetCluster)) {
            putShrinkOnlyPolicy(client(), policyName);

            followIndex(indexName, indexName);
            assertBusy(() -> assertTrue(indexExists(indexName)));
            assertBusy(() -> assertILMPolicy(client(), indexName, policyName, "warm", "unfollow", "wait-for-indexing-complete"));

            try (RestClient leaderClient = buildLeaderClient()) {
                updateIndexSettings(leaderClient, indexName, Settings.builder().put("index.lifecycle.indexing_complete", true).build());
            }

            assertBusy(() -> assertThat(getIndexSetting(client(), indexName, "index.lifecycle.indexing_complete"), equalTo("true")));

            assertBusy(() -> assertThat(getShrinkIndexName(client(), indexName), notNullValue()), 30, TimeUnit.SECONDS);
            String shrunkenIndexName = getShrinkIndexName(client(), indexName);

            assertBusy(() -> assertTrue(indexExists(shrunkenIndexName)));

            assertBusy(() -> {
                for (int i = 0; i < numberOfAliases; i++) {
                    assertOK(client().performRequest(new Request("HEAD", "/" + shrunkenIndexName + "/_alias/alias_" + i)));
                }
            });
            assertBusy(() -> assertOK(client().performRequest(new Request("HEAD", "/" + shrunkenIndexName + "/_alias/" + indexName))));

            assertBusy(() -> assertILMPolicy(client(), shrunkenIndexName, policyName, null, "complete", "complete"));
        }
    }

    public void testUnfollowInjectedBeforeShrink() throws Exception {
        final String indexName = "shrink-test";
        final String policyName = "shrink-test-policy";

        if ("leader".equals(targetCluster)) {
            Settings indexSettings = indexSettings(3, 0).put("index.lifecycle.name", policyName).build();
            createIndex(indexName, indexSettings, "", "");
            ensureGreen(indexName);
        } else if ("follow".equals(targetCluster)) {
            putShrinkOnlyPolicy(client(), policyName);

            followIndex(indexName, indexName);
            assertBusy(() -> assertTrue(indexExists(indexName)));
            assertBusy(() -> assertILMPolicy(client(), indexName, policyName, "warm", "unfollow", "wait-for-indexing-complete"));

            try (RestClient leaderClient = buildLeaderClient()) {
                updateIndexSettings(leaderClient, indexName, Settings.builder().put("index.lifecycle.indexing_complete", true).build());
            }

            assertBusy(() -> assertThat(getIndexSetting(client(), indexName, "index.lifecycle.indexing_complete"), equalTo("true")));


            assertBusy(() -> assertThat(getShrinkIndexName(client(), indexName), notNullValue()), 1, TimeUnit.MINUTES);
            String shrunkenIndexName = getShrinkIndexName(client(), indexName);

            assertBusy(() -> assertTrue(indexExists(shrunkenIndexName)));

            assertBusy(() -> assertILMPolicy(client(), shrunkenIndexName, policyName, null, "complete", "complete"));
        }
    }

    public void testCannotShrinkLeaderIndex() throws Exception {
        String indexName = "shrink-leader-test";
        String policyName = "shrink-leader-test-policy";
        if ("leader".equals(targetCluster)) {
            putShrinkOnlyPolicy(client(), policyName);
            createIndex(indexName, indexSettings(2, 0).build(), "", "");
            ensureGreen(indexName);
        } else if ("follow".equals(targetCluster)) {

            try (RestClient leaderClient = buildLeaderClient()) {
                putUnfollowOnlyPolicy(client(), policyName);
                followIndex(indexName, indexName);
                ensureGreen(indexName);

                Request changePolicyRequest = new Request("PUT", "/" + indexName + "/_settings");
                final StringEntity changePolicyEntity = new StringEntity(
                    "{ \"index.lifecycle.name\": \"" + policyName + "\" }",
                    ContentType.APPLICATION_JSON
                );
                changePolicyRequest.setEntity(changePolicyEntity);
                assertOK(leaderClient.performRequest(changePolicyRequest));

                assertBusy(() -> {
                    assertThat(getIndexSetting(client(), indexName, "index.xpack.ccr.following_index"), equalTo("true"));

                    assertILMPolicy(leaderClient, indexName, policyName, "warm", "shrink", "wait-for-shard-history-leases");
                    assertILMPolicy(client(), indexName, policyName, "hot", "unfollow", "wait-for-indexing-complete");
                });

                for (int i = 0; i < 50; i++) {
                    index(leaderClient, indexName, Integer.toString(i));
                }
                assertBusy(() -> {
                    for (int i = 0; i < 50; i++) {
                        assertDocumentExists(client(), indexName, Integer.toString(i));
                    }
                });

                assertILMPolicy(leaderClient, indexName, policyName, "warm", "shrink", "wait-for-shard-history-leases");
                assertILMPolicy(client(), indexName, policyName, "hot", "unfollow", "wait-for-indexing-complete");

                updateIndexSettings(leaderClient, indexName, Settings.builder().put("index.lifecycle.indexing_complete", true).build());

                assertBusy(() -> assertThat(getShrinkIndexName(leaderClient, indexName), notNullValue()), 30, TimeUnit.SECONDS);
                String shrunkenIndexName = getShrinkIndexName(leaderClient, indexName);
                assertBusy(() -> {
                    Response shrunkenIndexExistsResponse = leaderClient.performRequest(new Request("HEAD", "/" + shrunkenIndexName));
                    assertEquals(RestStatus.OK.getStatus(), shrunkenIndexExistsResponse.getStatusLine().getStatusCode());

                    assertILMPolicy(leaderClient, shrunkenIndexName, policyName, null, "complete", "complete");
                    assertILMPolicy(client(), indexName, policyName, "hot", "complete", "complete");
                });
            }
        } else {
            fail("unexpected target cluster [" + targetCluster + "]");
        }
    }

    public void testILMUnfollowFailsToRemoveRetentionLeases() throws Exception {
        final String leaderIndex = "leader";
        final String followerIndex = "follower";
        final String policyName = "unfollow_only_policy";

        if ("leader".equals(targetCluster)) {
            Settings indexSettings = indexSettings(1, 0).put("index.lifecycle.name", policyName) 
                .build();
            createIndex(leaderIndex, indexSettings, "", "");
            ensureGreen(leaderIndex);
        } else if ("follow".equals(targetCluster)) {
            try (RestClient leaderClient = buildLeaderClient()) {
                String leaderRemoteClusterSeed = System.getProperty("tests.leader_remote_cluster_seed");
                configureRemoteClusters("other_remote", leaderRemoteClusterSeed);
                assertBusy(() -> {
                    Map<?, ?> localConnection = (Map<?, ?>) toMap(client().performRequest(new Request("GET", "/_remote/info"))).get(
                        "other_remote"
                    );
                    assertThat(localConnection, notNullValue());
                    assertThat(localConnection.get("connected"), is(true));
                });
                putUnfollowOnlyPolicy(client(), policyName);
                followIndex("other_remote", leaderIndex, followerIndex);
                ensureGreen(followerIndex);
                client().performRequest(new Request("POST", "/_ilm/stop"));

                updateIndexSettings(leaderClient, leaderIndex, Settings.builder().put("index.lifecycle.indexing_complete", true).build());
                assertBusy(
                    () -> { assertThat(getIndexSetting(client(), followerIndex, "index.lifecycle.indexing_complete"), is("true")); }
                );

                configureRemoteClusters("other_remote", null);
                assertBusy(() -> {
                    Map<?, ?> localConnection = (Map<?, ?>) toMap(client().performRequest(new Request("GET", "/_remote/info"))).get(
                        "other_remote"
                    );
                    assertThat(localConnection, nullValue());
                });
                configureRemoteClusters("other_remote", "localhost:9999");
                assertBusy(() -> {
                    Map<?, ?> localConnection = (Map<?, ?>) toMap(client().performRequest(new Request("GET", "/_remote/info"))).get(
                        "other_remote"
                    );
                    assertThat(localConnection, notNullValue());
                    assertThat(localConnection.get("connected"), is(false));

                    Request statsRequest = new Request("GET", "/" + followerIndex + "/_ccr/stats");
                    Map<?, ?> response = toMap(client().performRequest(statsRequest));
                    logger.info("follow shards response={}", response);
                    String expectedIndex = ObjectPath.eval("indices.0.index", response);
                    assertThat(expectedIndex, equalTo(followerIndex));
                    Object fatalError = ObjectPath.eval("indices.0.shards.0.read_exceptions.0", response);
                    assertThat(fatalError, notNullValue());
                });

                client().performRequest(new Request("POST", "/_ilm/start"));
                assertBusy(() -> { assertILMPolicy(client(), followerIndex, policyName, "hot", "complete", "complete"); });

                assertBusy(() -> { assertThat(getIndexSetting(client(), followerIndex, "index.xpack.ccr.following_index"), nullValue()); });
            }
        }
    }

    private void configureRemoteClusters(String name, String leaderRemoteClusterSeed) throws IOException {
        logger.info("Configuring leader remote cluster [{}]", leaderRemoteClusterSeed);
        Request request = new Request("PUT", "/_cluster/settings");
        request.setJsonEntity(
            "{\"persistent\": {\"cluster.remote."
                + name
                + ".seeds\": "
                + (leaderRemoteClusterSeed != null ? Strings.format("\"%s\"", leaderRemoteClusterSeed) : null)
                + "}}"
        );
        assertThat(client().performRequest(request).getStatusLine().getStatusCode(), equalTo(200));
    }

    private static void putILMPolicy(String name, String maxSize, Integer maxDocs, TimeValue maxAge) throws IOException {
        final Request request = new Request("PUT", "_ilm/policy/" + name);
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.startObject("policy");
            {
                builder.startObject("phases");
                {
                    builder.startObject("hot");
                    {
                        builder.startObject("actions");
                        {
                            builder.startObject("rollover");
                            if (maxSize != null) {
                                builder.field("max_size", maxSize);
                            }
                            if (maxAge != null) {
                                builder.field("max_age", maxAge);
                            }
                            if (maxDocs != null) {
                                builder.field("max_docs", maxDocs);
                            }
                            builder.endObject();
                        }
                        if (randomBoolean()) {
                            builder.startObject("unfollow");
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                    builder.startObject("warm");
                    {
                        builder.startObject("actions");
                        {
                            if (randomBoolean()) {
                                builder.startObject("unfollow");
                                builder.endObject();
                            }
                            builder.startObject("readonly");
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                    builder.startObject("delete");
                    {
                        builder.field("min_age", "7d");
                        builder.startObject("actions");
                        {
                            builder.startObject("delete");
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        request.setJsonEntity(Strings.toString(builder));
        assertOK(client().performRequest(request));
    }

    private void putShrinkOnlyPolicy(RestClient client, String policyName) throws IOException {
        final XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.startObject("policy");
            {
                builder.startObject("phases");
                {
                    builder.startObject("warm");
                    {
                        builder.startObject("actions");
                        {
                            builder.startObject("shrink");
                            {
                                builder.field("number_of_shards", 1);
                            }
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                    builder.endObject();

                    if (randomBoolean()) {
                        builder.startObject("cold");
                        {
                            builder.startObject("actions");
                            {
                                builder.startObject("unfollow");
                                builder.endObject();
                            }
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();

        final Request request = new Request("PUT", "_ilm/policy/" + policyName);
        request.setJsonEntity(Strings.toString(builder));
        assertOK(client.performRequest(request));
    }

    private void putUnfollowOnlyPolicy(RestClient client, String policyName) throws Exception {
        final XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.startObject("policy");
            {
                builder.startObject("phases");
                {
                    builder.startObject("hot");
                    {
                        builder.startObject("actions");
                        {
                            builder.startObject("unfollow");
                            builder.endObject();
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();

        final Request request = new Request("PUT", "_ilm/policy/" + policyName);
        request.setJsonEntity(Strings.toString(builder));
        assertOK(client.performRequest(request));
    }

    private static void assertILMPolicy(RestClient client, String index, String policy, String expectedPhase) throws IOException {
        assertILMPolicy(client, index, policy, expectedPhase, null, null);
    }

    private static void assertILMPolicy(
        RestClient client,
        String index,
        String policy,
        String expectedPhase,
        String expectedAction,
        String expectedStep
    ) throws IOException {
        final Request request = new Request("GET", "/" + index + "/_ilm/explain");
        Map<String, Object> response = toMap(client.performRequest(request));
        LOGGER.info("response={}", response);
        Map<?, ?> explanation = (Map<?, ?>) ((Map<?, ?>) response.get("indices")).get(index);
        assertThat(explanation.get("managed"), is(true));
        assertThat(explanation.get("policy"), equalTo(policy));
        if (expectedPhase != null) {
            assertThat(explanation.get("phase"), equalTo(expectedPhase));
        }
        if (expectedAction != null) {
            assertThat(explanation.get("action"), equalTo(expectedAction));
        }
        if (expectedStep != null) {
            assertThat(explanation.get("step"), equalTo(expectedStep));
        }
    }

    private static void updateIndexSettings(RestClient client, String index, Settings settings) throws IOException {
        final Request request = new Request("PUT", "/" + index + "/_settings");
        request.setJsonEntity(Strings.toString(settings));
        assertOK(client.performRequest(request));
    }

    private static Object getIndexSetting(RestClient client, String index, String setting) throws IOException {
        Request request = new Request("GET", "/" + index + "/_settings");
        request.addParameter("flat_settings", "true");
        Map<String, Object> response = toMap(client.performRequest(request));
        return Optional.ofNullable((Map<?, ?>) response.get(index))
            .map(m -> (Map<?, ?>) m.get("settings"))
            .map(m -> m.get(setting))
            .orElse(null);
    }

    private void assertDocumentExists(RestClient client, String index, String id) throws IOException {
        Request request = new Request("GET", "/" + index + "/_doc/" + id);
        Response response;
        try {
            response = client.performRequest(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                if (response.getEntity() != null) {
                    logger.error(EntityUtils.toString(response.getEntity()));
                } else {
                    logger.error("response body was null");
                }
                fail("HTTP response code expected to be [200] but was [" + response.getStatusLine().getStatusCode() + "]");
            }
        } catch (ResponseException ex) {
            if (ex.getResponse().getEntity() != null) {
                logger.error(EntityUtils.toString(ex.getResponse().getEntity()), ex);
            } else {
                logger.error("response body was null");
            }
            fail("HTTP response code expected to be [200] but was [" + ex.getResponse().getStatusLine().getStatusCode() + "]");
        }
    }

    private void createNewSingletonPolicy(String policyName, String phaseName, LifecycleAction action, TimeValue after) throws IOException {
        Phase phase = new Phase(phaseName, after, singletonMap(action.getWriteableName(), action));
        LifecyclePolicy lifecyclePolicy = new LifecyclePolicy(policyName, singletonMap(phase.getName(), phase));
        XContentBuilder builder = jsonBuilder();
        lifecyclePolicy.toXContent(builder, null);
        final StringEntity entity = new StringEntity("{ \"policy\":" + Strings.toString(builder) + "}", ContentType.APPLICATION_JSON);
        Request request = new Request("PUT", "_ilm/policy/" + policyName);
        request.setEntity(entity);
        client().performRequest(request);
    }

    public static void updatePolicy(String indexName, String policy) throws IOException {

        Request changePolicyRequest = new Request("PUT", "/" + indexName + "/_settings");
        final StringEntity changePolicyEntity = new StringEntity(
            "{ \"index.lifecycle.name\": \"" + policy + "\" }",
            ContentType.APPLICATION_JSON
        );
        changePolicyRequest.setEntity(changePolicyEntity);
        assertOK(client().performRequest(changePolicyRequest));
    }

    @SuppressWarnings("unchecked")
    private String getSnapshotState(String snapshot) throws IOException {
        Response response = client().performRequest(new Request("GET", "/_snapshot/repo/" + snapshot));
        Map<String, Object> responseMap;
        try (InputStream is = response.getEntity().getContent()) {
            responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
        }

        Map<String, Object> snapResponse = ((List<Map<String, Object>>) responseMap.get("snapshots")).get(0);
        assertThat(snapResponse.get("snapshot"), equalTo(snapshot));
        return (String) snapResponse.get("state");
    }

    @SuppressWarnings("unchecked")
    private static String getShrinkIndexName(RestClient client, String originalIndex) throws InterruptedException, IOException {
        String[] shrunkenIndexName = new String[1];
        waitUntil(() -> {
            try {
                Request explainRequest = new Request(
                    "GET",
                    SHRUNKEN_INDEX_PREFIX + "*" + originalIndex + "," + originalIndex + "/_ilm/explain"
                );
                explainRequest.addParameter("only_errors", Boolean.toString(false));
                explainRequest.addParameter("only_managed", Boolean.toString(false));
                Response response = client.performRequest(explainRequest);
                Map<String, Object> responseMap;
                try (InputStream is = response.getEntity().getContent()) {
                    responseMap = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                }

                Map<String, Map<String, Object>> indexResponse = ((Map<String, Map<String, Object>>) responseMap.get("indices"));
                Map<String, Object> explainIndexResponse = indexResponse.get(originalIndex);
                if (explainIndexResponse == null) {
                    for (Map.Entry<String, Map<String, Object>> indexToExplainMap : indexResponse.entrySet()) {
                        String indexName = indexToExplainMap.getKey();
                        if (indexName.startsWith(SHRUNKEN_INDEX_PREFIX) && indexName.contains(originalIndex)) {
                            explainIndexResponse = indexToExplainMap.getValue();
                            break;
                        }
                    }
                }

                LOGGER.info("--> index {}, explain {}", originalIndex, explainIndexResponse);
                if (explainIndexResponse == null) {
                    return false;
                }
                shrunkenIndexName[0] = (String) explainIndexResponse.get("shrink_index_name");
                return shrunkenIndexName[0] != null;
            } catch (IOException e) {
                return false;
            }
        }, 30, TimeUnit.SECONDS);
        assert shrunkenIndexName[0] != null
            : "lifecycle execution state must contain the target shrink index name for index [" + originalIndex + "]";
        return shrunkenIndexName[0];
    }
}
