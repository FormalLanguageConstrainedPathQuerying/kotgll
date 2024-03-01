/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.integration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.core.Strings;
import org.junit.Before;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class TransformScheduleNowIT extends TransformRestTestCase {

    private static final String TEST_USER_NAME = "transform_user";
    private static final String TEST_ADMIN_USER_NAME_1 = "transform_admin_1";
    private static final String BASIC_AUTH_VALUE_TRANSFORM_ADMIN_1 = basicAuthHeaderValue(
        TEST_ADMIN_USER_NAME_1,
        TEST_PASSWORD_SECURE_STRING
    );
    private static final String DATA_ACCESS_ROLE = "test_data_access";

    private static boolean indicesCreated = false;

    @Override
    protected boolean preserveIndicesUponCompletion() {
        return true;
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
        setupUser(TEST_USER_NAME, Arrays.asList("transform_user", DATA_ACCESS_ROLE));
        setupUser(TEST_ADMIN_USER_NAME_1, Arrays.asList("transform_admin", DATA_ACCESS_ROLE));

        if (indicesCreated) {
            return;
        }

        createReviewsIndex();
        indicesCreated = true;
    }

    public void testScheduleNow() throws Exception {
        String sourceIndex = REVIEWS_INDEX_NAME;
        String transformId = "old_transform";
        String destIndex = transformId + "_idx";
        setupDataAccessRole(DATA_ACCESS_ROLE, sourceIndex, destIndex);

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
                }
              },
              "sync": {
                "time": {
                  "field": "timestamp",
                  "delay": "1s"
                }
              },
              "frequency": "1h"
            }""", destIndex, sourceIndex);
        createTransformRequest.setJsonEntity(config);
        Map<String, Object> createTransformResponse = entityAsMap(client().performRequest(createTransformRequest));
        assertThat(createTransformResponse.get("acknowledged"), equalTo(Boolean.TRUE));

        scheduleNowTransform(transformId);

        startAndWaitForContinuousTransform(transformId, destIndex, null, null, 1L);

        String newUser = "user_666";
        verifyNumberOfSourceDocs(sourceIndex, newUser, 0);
        verifyDestDoc(destIndex, newUser, 0, null);

        indexSourceDoc(sourceIndex, newUser, 7);

        Thread.sleep(5_000);

        verifyNumberOfSourceDocs(sourceIndex, newUser, 1);
        verifyDestDoc(destIndex, newUser, 0, null);

        scheduleNowTransform(transformId);
        waitForTransformCheckpoint(transformId, 2L);

        verifyNumberOfSourceDocs(sourceIndex, newUser, 1);
        verifyDestDoc(destIndex, newUser, 1, 7.0);

        indexSourceDoc(sourceIndex, newUser, 9);

        Thread.sleep(5_000);

        verifyNumberOfSourceDocs(sourceIndex, newUser, 2);
        verifyDestDoc(destIndex, newUser, 1, 7.0);

        ResponseException e = expectThrows(ResponseException.class, () -> scheduleNowTransform("_all"));
        assertThat(e.getMessage(), containsString("_schedule_now API does not support _all wildcard"));

        scheduleNowTransform(transformId);
        waitForTransformCheckpoint(transformId, 3L);

        verifyNumberOfSourceDocs(sourceIndex, newUser, 2);
        verifyDestDoc(destIndex, newUser, 1, 8.0);  

        stopTransform(transformId, false);
        scheduleNowTransform(transformId);
    }

    private void indexSourceDoc(String sourceIndex, String user, int stars) throws IOException {
        String doc = Strings.format("""
            {"user_id":"%s","stars":%s,"timestamp":%s}
            """, user, stars, Instant.now().toEpochMilli());

        Request indexRequest = new Request("POST", sourceIndex + "/_doc");
        indexRequest.addParameter("refresh", "true");
        indexRequest.setJsonEntity(doc);

        Map<String, Object> indexResponse = entityAsMap(client().performRequest(indexRequest));
        assertThat(indexResponse.get("result"), equalTo("created"));
    }

    private void verifyNumberOfSourceDocs(String sourceIndex, String user, int expectedDocCount) throws IOException {
        Map<String, Object> searchResult = getAsMap(sourceIndex + "/_search?q=user_id:" + user);
        assertEquals(expectedDocCount, XContentMapValues.extractValue("hits.total.value", searchResult));
    }

    private void verifyDestDoc(String destIndex, String user, int expectedDocCount, Double expectedAvgRating) throws IOException {
        Map<String, Object> searchResult = getAsMap(destIndex + "/_search?q=reviewer:" + user);
        assertEquals(expectedDocCount, XContentMapValues.extractValue("hits.total.value", searchResult));
        if (expectedAvgRating != null) {
            assertEquals(List.of(expectedAvgRating), XContentMapValues.extractValue("hits.hits._source.avg_rating", searchResult));
        }
    }
}
