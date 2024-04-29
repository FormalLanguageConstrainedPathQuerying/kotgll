/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.remotecluster;

import com.carrotsearch.randomizedtesting.annotations.TestCaseOrdering;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Strings;
import org.elasticsearch.test.AnnotationTestOrdering;
import org.elasticsearch.test.AnnotationTestOrdering.Order;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.elasticsearch.test.cluster.MutableSettingsProvider;
import org.elasticsearch.test.rest.ObjectPath;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;

@TestCaseOrdering(AnnotationTestOrdering.class)
public class RemoteClusterSecurityTransformMigrationIT extends AbstractRemoteClusterSecurityTestCase {

    private static final String TRANSFORM_USER = REMOTE_TRANSFORM_USER;
    private static final String TRANSFORM_USER_ROLE = "transform_user_role";
    private static final MutableSettingsProvider keystoreSettings = new MutableSettingsProvider();

    static {
        fulfillingCluster = ElasticsearchCluster.local()
            .name("fulfilling-cluster")
            .apply(commonClusterConfig)
            .module("transform")
            .module("reindex")
            .setting("remote_cluster_server.enabled", "true")
            .setting("remote_cluster.port", "0")
            .setting("xpack.security.remote_cluster_server.ssl.enabled", "true")
            .setting("xpack.security.remote_cluster_server.ssl.key", "remote-cluster.key")
            .setting("xpack.security.remote_cluster_server.ssl.certificate", "remote-cluster.crt")
            .keystore("xpack.security.remote_cluster_server.ssl.secure_key_passphrase", "remote-cluster-password")
            .build();

        queryCluster = ElasticsearchCluster.local()
            .name("query-cluster")
            .apply(commonClusterConfig)
            .module("transform")
            .module("reindex")
            .setting("xpack.security.remote_cluster_client.ssl.enabled", "true")
            .setting("xpack.security.remote_cluster_client.ssl.certificate_authorities", "remote-cluster-ca.crt")
            .keystore(keystoreSettings)
            .build();
    }

    @ClassRule
    public static TestRule clusterRule = RuleChain.outerRule(fulfillingCluster).around(queryCluster);

    @Override
    protected boolean preserveClusterUponCompletion() {
        return true;
    }

    @Order(10)
    public void testInitialSetup() throws IOException {
        final Request createIndexRequest = new Request("PUT", "shared-transform-index");
        createIndexRequest.setJsonEntity("""
            {
              "mappings": {
                "properties": {
                  "date": { "type": "date" },
                  "user": { "type": "keyword" },
                  "stars": { "type": "integer" }
                }
              }
            }""");
        assertOK(performRequestAgainstFulfillingCluster(createIndexRequest));

        final Request putUserRequest = new Request("PUT", "/_security/user/" + TRANSFORM_USER);
        putUserRequest.setJsonEntity(Strings.format("""
            {"password":"%s","roles":["transform_admin", "%s"]}
            """, PASS, TRANSFORM_USER_ROLE));
        assertOK(performRequestWithAdminUser(putUserRequest));
    }

    @Order(20)
    public void testRcs1Setup() throws Exception {
        final Request putRoleRequest = new Request("POST", "/_security/role/" + TRANSFORM_USER_ROLE);
        putRoleRequest.setJsonEntity("""
            {
              "indices": [
                {
                  "names": [ "*" ],
                  "privileges": [ "read", "read_cross_cluster", "view_index_metadata" ]
                }
              ]
            }""");
        performRequestAgainstFulfillingCluster(putRoleRequest);

        putRoleRequest.setJsonEntity("""
            {
              "indices": [
                {
                  "names": [ "*" ],
                  "privileges": [ "create_index", "index", "read" ]
                }
              ]
            }""");
        performRequestWithAdminUser(putRoleRequest);

        configureRemoteCluster("my_remote_cluster", fulfillingCluster, true, randomBoolean(), randomBoolean());

        final var putTransformRequest = new Request("PUT", "/_transform/simple-remote-transform");
        putTransformRequest.setJsonEntity("""
            {
              "source": { "index": "my_remote_cluster:shared-transform-index" },
              "dest": { "index": "simple-remote-transform" },
              "pivot": {
                "group_by": { "user": {"terms": {"field": "user"}}},
                "aggs": {"avg_stars": {"avg": {"field": "stars"}}}
              },
              "frequency": "1s",
              "sync": {
                "time": {
                  "field": "date",
                  "delay": "1s"
                }
              }
            }
            """);
        assertOK(performRequestWithTransformUser(putTransformRequest));
        startTransform();

        indexSourceDocuments(new UserStars("a", 1), new UserStars("a", 4), new UserStars("b", 2), new UserStars("b", 7));

        assertTransformedData(new UserStars("a", 2.5), new UserStars("b", 4.5));
    }

    @Order(30)
    public void testQueryClusterCredentialsChangeForRcs2() throws IOException {
        final Request putRoleRequest = new Request("POST", "/_security/role/" + TRANSFORM_USER_ROLE);
        putRoleRequest.setJsonEntity("""
            {
              "indices": [
                {
                  "names": [ "*" ],
                  "privileges": [ "create_index", "index", "read" ]
                }
              ],
              "remote_indices": [
                {
                  "clusters": [ "*" ],
                  "names": [ "*" ],
                  "privileges": [ "read", "read_cross_cluster", "view_index_metadata" ]
                }
              ]
            }""");
        performRequestWithAdminUser(putRoleRequest);

        stopTransform();

        removeRemoteCluster();

        indexSourceDocuments(new UserStars("a", 4), new UserStars("b", 3));

        final Map<String, Object> crossClusterAccessApiKey = createCrossClusterAccessApiKey("""
            {
              "search": [
                {
                   "names": ["shared-transform-index"]
                }
              ]
            }""");
        configureRemoteClusterCredentials("my_remote_cluster", (String) crossClusterAccessApiKey.get("encoded"), keystoreSettings);
    }

    @Order(40)
    public void testRcs2Setup() throws Exception {
        configureRemoteCluster("my_remote_cluster");

        indexSourceDocuments(new UserStars("c", 2), new UserStars("c", 6));

        startTransform();
        assertTransformedData(new UserStars("a", 3), new UserStars("b", 4), new UserStars("c", 4));

        indexSourceDocuments(new UserStars("a", 11), new UserStars("c", 1));
        assertTransformedData(new UserStars("a", 5), new UserStars("b", 4), new UserStars("c", 3));
    }

    @Order(50)
    public void testQueryClusterCredentialsChangeAgainForRcs1() throws IOException {
        stopTransform();

        removeRemoteCluster();

        if (randomBoolean()) {
            final Request putRoleRequest = new Request("POST", "/_security/role/" + TRANSFORM_USER_ROLE);
            putRoleRequest.setJsonEntity("""
                {
                  "cluster": [ "manage_ccr", "monitor" ],
                  "indices": [
                    {
                      "names": [ "*" ],
                      "privileges": [ "monitor", "read", "write", "manage_follow_index" ]
                    }
                  ]
                }""");
            performRequestWithAdminUser(putRoleRequest);
        }

        indexSourceDocuments(new UserStars("a", 0));

        removeRemoteClusterCredentials("my_remote_cluster", keystoreSettings);
    }

    @Order(60)
    public void testRcs1SetupAgain() throws Exception {
        configureRemoteCluster("my_remote_cluster", fulfillingCluster, true, randomBoolean(), randomBoolean());
        startTransform();
        assertTransformedData(new UserStars("a", 4), new UserStars("b", 4), new UserStars("c", 3));

        indexSourceDocuments(new UserStars("c", 1), new UserStars("d", 1));
        assertTransformedData(new UserStars("a", 4), new UserStars("b", 4), new UserStars("c", 2.5), new UserStars("d", 1));
    }

    private record UserStars(String user, double stars) {}

    private void assertTransformedData(UserStars... userStarsArray) throws Exception {
        assertBusy(() -> {
            final ObjectPath searchObjPath = assertOKAndCreateObjectPath(
                performRequestWithTransformUser(
                    new Request("GET", "/simple-remote-transform/_search?sort=user&rest_total_hits_as_int=true")
                )
            );
            assertThat(searchObjPath.evaluate("hits.total"), equalTo(userStarsArray.length));
            for (int i = 0; i < userStarsArray.length; i++) {
                final UserStars userStars = userStarsArray[i];
                assertThat(searchObjPath.evaluate("hits.hits." + i + "._source.user"), equalTo(userStars.user));
                assertThat(searchObjPath.evaluate("hits.hits." + i + "._source.avg_stars"), equalTo(userStars.stars));
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void indexSourceDocuments(UserStars... userStarsArray) throws IOException {
        final Request bulkRequest = new Request("POST", "/_bulk?refresh=true");
        final String payload = Arrays.stream(userStarsArray).map(entry -> Strings.format("""
            {"index": {"_index": "shared-transform-index"}}
            {"user": "%s", "stars": %s, "date": "%s"}
            """, entry.user, (int) entry.stars, Instant.now())).collect(Collectors.joining());
        bulkRequest.setJsonEntity(payload);
        assertOK(performRequestAgainstFulfillingCluster(bulkRequest));
    }

    private void removeRemoteCluster() throws IOException {
        updateClusterSettings(
            Settings.builder()
                .putNull("cluster.remote.my_remote_cluster.mode")
                .putNull("cluster.remote.my_remote_cluster.skip_unavailable")
                .putNull("cluster.remote.my_remote_cluster.proxy_address")
                .putNull("cluster.remote.my_remote_cluster.seeds")
                .build()
        );
    }

    private Response performRequestWithAdminUser(final Request request) throws IOException {
        return performRequestWithAdminUser(client(), request);
    }

    private Response performRequestWithTransformUser(final Request request) throws IOException {
        request.setOptions(RequestOptions.DEFAULT.toBuilder().addHeader("Authorization", basicAuthHeaderValue(TRANSFORM_USER, PASS)));
        return client().performRequest(request);
    }

    private void startTransform() throws IOException {
        assertOK(performRequestWithTransformUser(new Request("POST", "/_transform/simple-remote-transform/_start")));
    }

    private void stopTransform() throws IOException {
        assertOK(
            performRequestWithTransformUser(new Request("POST", "/_transform/simple-remote-transform/_stop?wait_for_completion=true"))
        );
    }
}
