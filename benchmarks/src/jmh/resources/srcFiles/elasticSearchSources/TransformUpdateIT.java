/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.integration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.core.Strings;
import org.elasticsearch.threadpool.TestThreadPool;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class TransformUpdateIT extends TransformRestTestCase {

    private static final String TEST_USER_NAME = "transform_user";
    private static final String BASIC_AUTH_VALUE_TRANSFORM_USER = basicAuthHeaderValue(TEST_USER_NAME, TEST_PASSWORD_SECURE_STRING);
    private static final String TEST_ADMIN_USER_NAME_1 = "transform_admin_1";
    private static final String BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1 = basicAuthHeaderValue(
        TEST_ADMIN_USER_NAME_1,
        TEST_PASSWORD_SECURE_STRING
    );
    private static final String TEST_ADMIN_USER_NAME_2 = "transform_admin_2";
    private static final String BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2 = basicAuthHeaderValue(
        TEST_ADMIN_USER_NAME_2,
        TEST_PASSWORD_SECURE_STRING
    );
    private static final String TEST_ADMIN_USER_NAME_NO_DATA = "transform_admin_no_data";
    private static final String BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA = basicAuthHeaderValue(
        TEST_ADMIN_USER_NAME_NO_DATA,
        TEST_PASSWORD_SECURE_STRING
    );
    private static final String DATA_ACCESS_ROLE = "test_data_access";
    private static final String DATA_ACCESS_ROLE_2 = "test_data_access_2";

    private TestThreadPool threadPool;

    @Override
    protected boolean preserveIndicesUponCompletion() {
        return false;
    }

    @Override
    protected boolean enableWarningsCheck() {
        return false;
    }

    @Override
    protected RestClient buildClient(Settings settings, HttpHost[] hosts) throws IOException {
        RestClientBuilder builder = RestClient.builder(hosts);
        configureClient(builder, settings);
        builder.setStrictDeprecationMode(false);
        return builder.build();
    }

    @Before
    public void createIndexes() throws IOException {
        setupDataAccessRole(DATA_ACCESS_ROLE, REVIEWS_INDEX_NAME);
        setupDataAccessRole(DATA_ACCESS_ROLE_2, REVIEWS_INDEX_NAME);

        setupUser(TEST_USER_NAME, List.of("transform_user", DATA_ACCESS_ROLE));
        setupUser(TEST_ADMIN_USER_NAME_1, List.of("transform_admin", DATA_ACCESS_ROLE));
        setupUser(TEST_ADMIN_USER_NAME_2, List.of("transform_admin", DATA_ACCESS_ROLE_2));
        setupUser(TEST_ADMIN_USER_NAME_NO_DATA, List.of("transform_admin"));
        createReviewsIndex();

        threadPool = new TestThreadPool(getTestName());
    }

    @After
    public void shutdownThreadPool() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    public void testUpdateDeprecatedSettings() throws Exception {
        String transformId = "old_transform";
        String transformDest = transformId + "_idx";
        setupDataAccessRole(DATA_ACCESS_ROLE, REVIEWS_INDEX_NAME, transformDest);

        final Request createTransformRequest = createRequestWithAuth(
            "PUT",
            getTransformEndpoint() + transformId,
            BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1
        );
        String config = Strings.format("""
            {
              "dest": {
                "index": "%s"
              },
              "source": {
                "index": "%s"
              },
              "pivot": {
                "group_by": {
                  "reviewer": {
                    "terms": {
                      "field": "user_id"
                    }
                  }
                },
                "aggregations": {
                  "avg_rating": {
                    "avg": {
                      "field": "stars"
                    }
                  }
                },
                "max_page_search_size": 555
              }
            }""", transformDest, REVIEWS_INDEX_NAME);

        createTransformRequest.setJsonEntity(config);
        Map<String, Object> createTransformResponse = entityAsMap(client().performRequest(createTransformRequest));
        assertThat(createTransformResponse.get("acknowledged"), equalTo(Boolean.TRUE));

        Map<String, Object> transform = getTransformConfig(transformId, BASIC_AUTH_VALUE_TRANSFORM_USER);
        assertThat(XContentMapValues.extractValue("pivot.max_page_search_size", transform), equalTo(555));

        final Request updateRequest = createRequestWithAuth(
            "POST",
            getTransformEndpoint() + transformId + "/_update",
            BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1
        );
        updateRequest.setJsonEntity("{}");

        Map<String, Object> updateResponse = entityAsMap(client().performRequest(updateRequest));

        assertNull(XContentMapValues.extractValue("pivot.max_page_search_size", updateResponse));
        assertThat(XContentMapValues.extractValue("settings.max_page_search_size", updateResponse), equalTo(555));

        transform = getTransformConfig(transformId, BASIC_AUTH_VALUE_TRANSFORM_USER);

        assertNull(XContentMapValues.extractValue("pivot.max_page_search_size", transform));
        assertThat(XContentMapValues.extractValue("settings.max_page_search_size", transform), equalTo(555));
    }

    public void testUpdateTransferRights() throws Exception {
        updateTransferRightsTester(false);
    }

    public void testUpdateTransferRightsSecondaryAuthHeaders() throws Exception {
        updateTransferRightsTester(true);
    }

    public void testUpdateThatChangesSettingsButNotHeaders() throws Exception {
        String transformId = "test_update_that_changes_settings";
        String destIndex = transformId + "-dest";

        createPivotReviewsTransform(transformId, destIndex, null, null, null);

        Request updateTransformRequest = createRequestWithAuth("POST", getTransformEndpoint() + transformId + "/_update", null);
        updateTransformRequest.setJsonEntity("""
            { "settings": { "max_page_search_size": 123 } }""");

        Map<String, Object> updatedConfig = entityAsMap(client().performRequest(updateTransformRequest));

        assertThat(updatedConfig.get("settings"), is(equalTo(Map.of("max_page_search_size", 123))));
    }

    public void testConcurrentUpdates() throws Exception {
        String transformId = "test_concurrent_updates";
        String destIndex = transformId + "-dest";

        createPivotReviewsTransform(transformId, destIndex, null, null, null);

        int minMaxPageSearchSize = 10;
        int maxMaxPageSearchSize = 20;
        List<Callable<Response>> concurrentUpdates = new ArrayList<>(10);
        for (int maxPageSearchSize = minMaxPageSearchSize; maxPageSearchSize < maxMaxPageSearchSize; ++maxPageSearchSize) {
            Request updateTransformRequest = createRequestWithAuth("POST", getTransformEndpoint() + transformId + "/_update", null);
            updateTransformRequest.setJsonEntity(Strings.format("""
                { "settings": { "max_page_search_size": %s } }""", maxPageSearchSize));

            concurrentUpdates.add(() -> client().performRequest(updateTransformRequest));
        }

        List<Future<Response>> futures = threadPool.generic().invokeAll(concurrentUpdates);
        for (Future<Response> future : futures) {
            try {  
                future.get();
            } catch (ExecutionException e) {  
                assertThat(e.getCause(), instanceOf(ResponseException.class));
                ResponseException re = (ResponseException) e.getCause();
                assertThat(re.getResponse().getStatusLine().getStatusCode(), is(equalTo(409)));
                assertThat(
                    re.getMessage(),
                    containsString("Cannot update transform id [" + transformId + "] due to a concurrent update conflict. Please retry.")
                );
            }
        }

        Map<String, Object> finalConfig = getTransformConfig(transformId, null);
        assertThat(
            (int) XContentMapValues.extractValue(finalConfig, "settings", "max_page_search_size"),
            is(both(greaterThanOrEqualTo(minMaxPageSearchSize)).and(lessThan(maxMaxPageSearchSize)))
        );
    }

    private void updateTransferRightsTester(boolean useSecondaryAuthHeaders) throws Exception {
        String transformId = "transform1";
        String transformIdCloned = "transform2";
        String transformDest = transformId + "_idx";
        setupDataAccessRole(DATA_ACCESS_ROLE, REVIEWS_INDEX_NAME, transformDest);
        setupDataAccessRole(DATA_ACCESS_ROLE_2, REVIEWS_INDEX_NAME, transformDest);

        final Request createTransformRequest = useSecondaryAuthHeaders
            ? createRequestWithSecondaryAuth(
                "PUT",
                getTransformEndpoint() + transformId,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2
            )
            : createRequestWithAuth("PUT", getTransformEndpoint() + transformId, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2);

        final Request createTransformRequest_2 = useSecondaryAuthHeaders
            ? createRequestWithSecondaryAuth(
                "PUT",
                getTransformEndpoint() + transformIdCloned,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2
            )
            : createRequestWithAuth("PUT", getTransformEndpoint() + transformIdCloned, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2);

        String config = Strings.format("""
            {
              "dest": {
                "index": "%s"
              },
              "source": {
                "index": "%s"
              },
              "pivot": {
                "group_by": {
                  "reviewer": {
                    "terms": {
                      "field": "user_id"
                    }
                  }
                },
                "aggregations": {
                  "avg_rating": {
                    "avg": {
                      "field": "stars"
                    }
                  }
                }
              }
            }""", transformDest, REVIEWS_INDEX_NAME);

        createTransformRequest.setJsonEntity(config);
        Map<String, Object> createTransformResponse = entityAsMap(client().performRequest(createTransformRequest));
        assertThat(createTransformResponse.get("acknowledged"), equalTo(Boolean.TRUE));

        Map<String, Object> transformConfig = getTransformConfig(transformId, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2);
        assertThat(transformConfig.get("authorization"), equalTo(Map.of("roles", List.of("transform_admin", DATA_ACCESS_ROLE_2))));

        createTransformRequest_2.setJsonEntity(config);
        createTransformResponse = entityAsMap(client().performRequest(createTransformRequest_2));
        assertThat(createTransformResponse.get("acknowledged"), equalTo(Boolean.TRUE));

        deleteUser(TEST_ADMIN_USER_NAME_2);
        deleteDataAccessRole(DATA_ACCESS_ROLE_2);

        try {
            getTransformConfig(transformId, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_2);
            fail("request should have failed");
        } catch (ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(401));
        }

        transformConfig = getTransformConfig(transformId, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1);

        try {
            if (useSecondaryAuthHeaders) {
                startAndWaitForTransform(
                    transformId,
                    transformDest,
                    BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA,
                    BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1,
                    new String[0]
                );
            } else {
                startAndWaitForTransform(transformId, transformDest, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1);
            }
            fail("request should have failed");
        } catch (ResponseException e) {
            assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(500));
        }
        assertBusy(() -> {
            Map<?, ?> transformStatsAsMap = getTransformStateAndStats(transformId);
            assertThat(XContentMapValues.extractValue("stats.documents_indexed", transformStatsAsMap), equalTo(0));
        }, 3, TimeUnit.SECONDS);

        final Request updateRequest = useSecondaryAuthHeaders
            ? createRequestWithSecondaryAuth(
                "POST",
                getTransformEndpoint() + transformIdCloned + "/_update",
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1
            )
            : createRequestWithAuth("POST", getTransformEndpoint() + transformIdCloned + "/_update", BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1);
        updateRequest.setJsonEntity("{}");
        assertOK(client().performRequest(updateRequest));

        getTransformConfig(transformIdCloned, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1);

        if (useSecondaryAuthHeaders) {
            startAndWaitForTransform(
                transformIdCloned,
                transformDest,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_NO_DATA,
                BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1,
                new String[0]
            );
        } else {
            startAndWaitForTransform(transformIdCloned, transformDest, BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1);
        }
        assertBusy(() -> {
            Map<?, ?> transformStatsAsMap = getTransformStateAndStats(transformIdCloned);
            assertThat(XContentMapValues.extractValue("stats.documents_indexed", transformStatsAsMap), equalTo(27));
        }, 15, TimeUnit.SECONDS);
    }

    private void deleteUser(String user) throws IOException {
        Request request = new Request("DELETE", "/_security/user/" + user);
        client().performRequest(request);
    }

    protected void deleteDataAccessRole(String role) throws IOException {
        Request request = new Request("DELETE", "/_security/role/" + role);
        client().performRequest(request);
    }
}
