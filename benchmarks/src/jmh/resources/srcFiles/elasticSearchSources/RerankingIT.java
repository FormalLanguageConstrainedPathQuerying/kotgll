/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 *
 * this file was contributed to by a generative AI
 */

package org.elasticsearch.xpack.inference;

import org.apache.lucene.tests.util.LuceneTestCase;

import java.io.IOException;

@LuceneTestCase.AwaitsFix(bugUrl = "https:
public class RerankingIT extends InferenceBaseRestTest {

    public void testPutCohereRerankEndpoint() throws IOException {
        String endpoint = putCohereRerankEndpoint();
        postCohereRerankEndpoint(
            endpoint,
            "what is elasticsearch for?",
            new String[] { "for search", "for security", "for logs", "for email", "for rubber bands", "for kiwis" }
        );
    }

    private String putCohereRerankEndpoint() throws IOException {
        String endpointID = randomAlphaOfLength(10).toLowerCase();
        putRequest("/_inference/rerank/" + endpointID, """
            {
              "service": "cohere",
              "service_settings": {
                "model_id": "rerank-english-v2.0",
                "api_key": ""
              }
            }
            """);
        return endpointID;
    }

    public void testPutCohereRerankEndpointWithDocuments() throws IOException {
        String endpoint = putCohereRerankEndpointWithDocuments();
        postCohereRerankEndpoint(
            endpoint,
            "what is elasticsearch for?",
            new String[] { "for search", "for security", "for logs", "for email", "for rubber bands", "for kiwis" }
        );
    }

    private String putCohereRerankEndpointWithDocuments() throws IOException {
        String endpointID = randomAlphaOfLength(10).toLowerCase();
        putRequest("/_inference/rerank/" + endpointID, """
            {
              "service": "cohere",
              "service_settings": {
                "model_id": "rerank-english-v2.0",
                "api_key": ""
              },
              "task_settings": {
                "return_documents": true
              }
            }
            """);
        return endpointID;
    }

    public void testPutCohereRerankEndpointWithTop2() throws IOException {
        String endpoint = putCohereRerankEndpointWithTop2();
        postCohereRerankEndpoint(
            endpoint,
            "what is elasticsearch for?",
            new String[] { "for search", "for security", "for logs", "for email", "for rubber bands", "for kiwis" }
        );
    }

    private String putCohereRerankEndpointWithTop2() throws IOException {
        String endpointID = randomAlphaOfLength(10).toLowerCase();
        putRequest("/_inference/rerank/" + endpointID, """
            {
              "service": "cohere",
              "service_settings": {
                "model_id": "rerank-english-v2.0",
                "api_key": "8TNPBvpBO7oN97009HQHzQbBhNrxmREbcJrZCwkK"
              },
              "task_settings": {
                "top_n": 2
              }
            }
            """);
        return endpointID;
    }

    public void postCohereRerankEndpoint(String endpoint, String query, String[] input) throws IOException {
        StringBuilder body = new StringBuilder();

        body.append("{");

        body.append("\"query\":\"").append(query).append("\",");

        body.append("\"input\":[");

        for (int i = 0; i < input.length; i++) {
            body.append("\"").append(input[i]).append("\"");
            if (i < input.length - 1) {
                body.append(",");
            }
        }

        body.append("]}");
        postRequest("/_inference/rerank/" + endpoint, body.toString());
    }

}
