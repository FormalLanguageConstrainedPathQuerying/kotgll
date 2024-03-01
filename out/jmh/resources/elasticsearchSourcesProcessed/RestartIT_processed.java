/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ccr;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.io.IOException;

public class RestartIT extends ESCCRRestTestCase {

    public void testRestart() throws Exception {
        final int numberOfDocuments = 128;
        final String testsTargetCluster = System.getProperty("tests.target_cluster");
        switch (testsTargetCluster) {
            case "leader" -> {
                createIndexAndIndexDocuments("leader", numberOfDocuments, client());
            }
            case "follow" -> {
                followIndex("leader", "follow-leader");
                verifyFollower("follow-leader", numberOfDocuments, client());

                final Request putPatternRequest = new Request("PUT", "/_ccr/auto_follow/leader_cluster_pattern");
                putPatternRequest.setJsonEntity("""
                    {
                      "leader_index_patterns": [
                        "leader-*"
                      ],
                      "remote_cluster": "leader_cluster",
                      "follow_index_pattern": "follow-{{leader_index}}"
                    }""");
                assertOK(client().performRequest(putPatternRequest));
                try (RestClient leaderClient = buildLeaderClient()) {
                    createIndexAndIndexDocuments("leader-1", numberOfDocuments, leaderClient);
                    verifyFollower("follow-leader-1", numberOfDocuments, client());
                }
            }
            case "follow-restart" -> {
                try (RestClient leaderClient = buildLeaderClient()) {
                    createIndexAndIndexDocuments("leader-2", numberOfDocuments, leaderClient);
                    for (final String index : new String[] { "leader", "leader-1", "leader-2" }) {
                        indexDocuments(index, numberOfDocuments, numberOfDocuments, leaderClient);
                    }
                    for (final String index : new String[] { "follow-leader", "follow-leader-1", "follow-leader-2" }) {
                        logger.info("verifying {} using {}", index, client().getNodes());
                        verifyFollower(index, 2 * numberOfDocuments, client());
                    }
                    createIndexAndIndexDocuments("leader-3", 2 * numberOfDocuments, leaderClient);
                    verifyFollower("follow-leader-3", 2 * numberOfDocuments, client());
                }
            }
            default -> {
                throw new IllegalArgumentException("unexpected value [" + testsTargetCluster + "] for tests.target_cluster");
            }
        }
    }

    private void createIndexAndIndexDocuments(final String index, final int numberOfDocuments, final RestClient client) throws IOException {
        final Request createIndexRequest = new Request("PUT", "/" + index);
        createIndexRequest.setJsonEntity("{\"settings\":" + Strings.toString(Settings.EMPTY) + "}");
        assertOK(client.performRequest(createIndexRequest));
        indexDocuments(index, numberOfDocuments, 0, client);
    }

    private void indexDocuments(final String index, final int numberOfDocuments, final int initial, final RestClient client)
        throws IOException {
        for (int i = 0, j = initial; i < numberOfDocuments; i++, j++) {
            index(client, index, Integer.toString(j), "field", j);
        }
        assertOK(client.performRequest(new Request("POST", "/" + index + "/_refresh")));
    }

    private void verifyFollower(final String index, final int numberOfDocuments, final RestClient client) throws Exception {
        assertBusy(() -> {
            ensureYellow(index, client);
            verifyDocuments(index, numberOfDocuments, "*:*", client);
        });
    }

    @Override
    protected Settings restClientSettings() {
        String token = basicAuthHeaderValue("admin", new SecureString("admin-password".toCharArray()));
        return Settings.builder().put(ThreadContext.PREFIX + ".Authorization", token).build();
    }

}
