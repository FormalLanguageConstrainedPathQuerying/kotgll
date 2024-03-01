/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.remotecluster;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.core.Strings;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.elasticsearch.xcontent.ObjectPath;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public abstract class AbstractRemoteClusterSecurityWithMultipleRemotesRestIT extends AbstractRemoteClusterSecurityTestCase {

    protected static ElasticsearchCluster otherFulfillingCluster;
    protected static RestClient otherFulfillingClusterClient;

    @BeforeClass
    public static void initOtherFulfillingClusterClient() {
        if (otherFulfillingClusterClient != null) {
            return;
        }
        otherFulfillingClusterClient = buildRestClient(otherFulfillingCluster);
    }

    @AfterClass
    public static void closeOtherFulfillingClusterClient() throws IOException {
        try {
            IOUtils.close(otherFulfillingClusterClient);
        } finally {
            otherFulfillingClusterClient = null;
        }
    }

    public void testCrossClusterSearch() throws Exception {
        configureRemoteCluster();
        configureRolesOnClusters();

        {
            final Request bulkRequest = new Request("POST", "/_bulk?refresh=true");
            bulkRequest.setJsonEntity(Strings.format("""
                { "index": { "_index": "cluster1_index1" } }
                { "name": "doc1" }
                { "index": { "_index": "cluster1_index2" } }
                { "name": "doc2" }
                """));
            assertOK(performRequestAgainstFulfillingCluster(bulkRequest));
        }

        {
            final Request bulkRequest = new Request("POST", "/_bulk?refresh=true");
            bulkRequest.setJsonEntity(Strings.format("""
                { "index": { "_index": "cluster2_index1" } }
                { "name": "doc1" }
                { "index": { "_index": "cluster2_index2" } }
                { "name": "doc2" }
                """));
            assertOK(performRequestAgainstOtherFulfillingCluster(bulkRequest));
        }

        {
            final var indexDocRequest = new Request("POST", "/local_index/_doc?refresh=true");
            indexDocRequest.setJsonEntity("{\"name\": \"doc1\"}");
            assertOK(client().performRequest(indexDocRequest));

            searchAndAssertIndicesFound(
                String.format(
                    Locale.ROOT,
                    "/local_index,%s:%s/_search?ccs_minimize_roundtrips=%s",
                    randomFrom("my_remote_*", "*"),
                    randomFrom("*_index1", "*"),
                    randomBoolean()
                ),
                "cluster1_index1",
                "cluster2_index1",
                "local_index"
            );

            searchAndAssertIndicesFound(
                String.format(
                    Locale.ROOT,
                    "/%s:%s/_search?ccs_minimize_roundtrips=%s",
                    randomFrom("my_remote_*", "*"),
                    randomFrom("*_index1", "*"),
                    randomBoolean()
                ),
                "cluster1_index1",
                "cluster2_index1"
            );

            searchAndAssertIndicesFound(
                String.format(
                    Locale.ROOT,
                    "/my_remote_cluster:%s,my_remote_cluster_2:%s/_search?ccs_minimize_roundtrips=%s",
                    randomFrom("cluster1_index1", "*_index1", "*"),
                    randomFrom("cluster2_index1", "*_index1", "*"),
                    randomBoolean()
                ),
                "cluster1_index1",
                "cluster2_index1"
            );

            final boolean searchFirstCluster = randomBoolean();
            final String index1 = searchFirstCluster ? "cluster1_index1" : "cluster2_index1";
            searchAndAssertIndicesFound(
                String.format(
                    Locale.ROOT,
                    "/%s:%s/_search?ccs_minimize_roundtrips=%s",
                    searchFirstCluster ? "my_remote_cluster" : "my_remote_cluster_2",
                    randomFrom(index1, "*_index1", "*"),
                    randomBoolean()
                ),
                index1
            );

            final boolean skipUnavailableOnOtherCluster = isSkipUnavailable("my_remote_cluster_2");

            final String missingIndex = "missingIndex";
            final boolean missingIndexOnFirstCluster = randomBoolean();
            final boolean missingIndexOnSecondCluster = false == missingIndexOnFirstCluster || randomBoolean();
            final String searchPath1 = String.format(
                Locale.ROOT,
                "/my_remote_cluster:%s,my_remote_cluster_2:%s/_search?ccs_minimize_roundtrips=%s",
                missingIndexOnFirstCluster ? missingIndex : randomFrom("cluster1_index1", "*_index1", "*"),
                missingIndexOnSecondCluster ? missingIndex : randomFrom("cluster2_index1", "*_index1", "*"),
                randomBoolean()
            );
            if (skipUnavailableOnOtherCluster && false == missingIndexOnFirstCluster) {
                searchAndAssertIndicesFound(searchPath1, "cluster1_index1");
            } else {
                searchAndExpect403(searchPath1);
            }

            final String index2 = randomFrom("cluster1_index1", "cluster2_index1");
            final String searchPath2 = String.format(
                Locale.ROOT,
                "/my_remote_cluster*:%s/_search?ccs_minimize_roundtrips=%s",
                index2,
                randomBoolean()
            );
            if (skipUnavailableOnOtherCluster && index2.equals("cluster1_index1")) {
                searchAndAssertIndicesFound(searchPath2, index2);
            } else {
                searchAndExpect403(searchPath2);
            }

            searchAndExpect403(String.format(Locale.ROOT, "/*:%s/_search?ccs_minimize_roundtrips=%s", "missingIndex", randomBoolean()));
        }
    }

    private static boolean isSkipUnavailable(String clusterAlias) throws IOException {
        final Request remoteInfoRequest = new Request("GET", "/_remote/info");
        final Response remoteInfoResponse = adminClient().performRequest(remoteInfoRequest);
        assertOK(remoteInfoResponse);
        final Map<String, Object> remoteInfoMap = responseAsMap(remoteInfoResponse);
        assertThat(remoteInfoMap, hasKey(clusterAlias));
        assertThat(ObjectPath.eval(clusterAlias + ".connected", remoteInfoMap), is(true));
        return ObjectPath.eval(clusterAlias + ".skip_unavailable", remoteInfoMap);
    }

    private static void searchAndExpect403(String searchPath) {
        final ResponseException exception = expectThrows(
            ResponseException.class,
            () -> performRequestWithRemoteSearchUser(new Request("GET", searchPath))
        );
        assertThat(exception.getResponse().getStatusLine().getStatusCode(), equalTo(403));
    }

    protected abstract void configureRolesOnClusters() throws IOException;

    static void searchAndAssertIndicesFound(String searchPath, String... expectedIndices) throws IOException {
        final Response response = performRequestWithRemoteSearchUser(new Request("GET", searchPath));
        assertOK(response);
        final SearchResponse searchResponse;
        try (var parser = responseAsParser(response)) {
            searchResponse = SearchResponse.fromXContent(parser);
        }
        try {
            final List<String> actualIndices = Arrays.stream(searchResponse.getHits().getHits())
                .map(SearchHit::getIndex)
                .collect(Collectors.toList());
            assertThat(actualIndices, containsInAnyOrder(expectedIndices));
        } finally {
            searchResponse.decRef();
        }
    }

    static Response performRequestWithRemoteSearchUser(final Request request) throws IOException {
        request.setOptions(RequestOptions.DEFAULT.toBuilder().addHeader("Authorization", basicAuthHeaderValue(REMOTE_SEARCH_USER, PASS)));
        return client().performRequest(request);
    }

    static Response performRequestAgainstOtherFulfillingCluster(Request putRoleRequest) throws IOException {
        return performRequestWithAdminUser(otherFulfillingClusterClient, putRoleRequest);
    }
}
