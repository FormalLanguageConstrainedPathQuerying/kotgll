/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.upgrades;

import org.elasticsearch.Build;
import org.elasticsearch.TransportVersion;
import org.elasticsearch.client.Request;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.test.rest.ObjectPath;
import org.elasticsearch.test.rest.RestTestLegacyFeatures;

import java.util.Map;

import static org.elasticsearch.cluster.ClusterState.INFERRED_TRANSPORT_VERSION;
import static org.elasticsearch.cluster.ClusterState.VERSION_INTRODUCING_TRANSPORT_VERSIONS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;

public class TransportVersionClusterStateUpgradeIT extends AbstractUpgradeTestCase {

    public void testReadsInferredTransportVersions() throws Exception {
        assertTrue(waitUntil(() -> {
            try {
                for (int i = getClusterHosts().size(); i > 0; i--) {
                    if (runTransportVersionsTest() == false) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }));
    }

    private boolean runTransportVersionsTest() throws Exception {
        final var clusterState = ObjectPath.createFromResponse(
            client().performRequest(new Request("GET", "/_cluster/state" + randomFrom("", "/nodes") + randomFrom("", "?local")))
        );
        final var description = clusterState.toString();

        final var nodeIds = clusterState.evaluateMapKeys("nodes");
        final Map<String, String> versionsByNodeId = Maps.newHashMapWithExpectedSize(nodeIds.size());
        for (final var nodeId : nodeIds) {
            versionsByNodeId.put(nodeId, clusterState.evaluate("nodes." + nodeId + ".version"));
        }

        final var hasTransportVersions = clusterState.evaluate("transport_versions") != null;
        final var hasNodesVersions = clusterState.evaluate("nodes_versions") != null;
        assertFalse(description, hasNodesVersions && hasTransportVersions);

        switch (CLUSTER_TYPE) {
            case OLD -> {
                if (clusterHasFeature(RestTestLegacyFeatures.TRANSPORT_VERSION_SUPPORTED) == false) {
                    assertFalse(description, hasTransportVersions);
                    assertFalse(description, hasNodesVersions);
                } else if (clusterHasFeature(RestTestLegacyFeatures.STATE_REPLACED_TRANSPORT_VERSION_WITH_NODES_VERSION) == false) {
                    assertTrue(description, hasTransportVersions);
                    assertFalse(description, hasNodesVersions);
                } else {
                    assertFalse(description, hasTransportVersions);
                    assertTrue(description, hasNodesVersions);
                }
            }
            case MIXED -> {
                if (clusterHasFeature(RestTestLegacyFeatures.TRANSPORT_VERSION_SUPPORTED) == false) {
                    assertFalse(description, hasTransportVersions);
                } else if (clusterHasFeature(RestTestLegacyFeatures.STATE_REPLACED_TRANSPORT_VERSION_WITH_NODES_VERSION) == false) {
                    assertTrue(description, hasNodesVersions || hasTransportVersions);
                } else {
                    assertFalse(description, hasTransportVersions);
                    assertTrue(description, hasNodesVersions);
                }
            }
            case UPGRADED -> {
                assertFalse(description, hasTransportVersions);
                assertTrue(description, hasNodesVersions);
                assertThat(description, versionsByNodeId.values(), everyItem(equalTo(Build.current().version())));
            }
        }

        if (hasTransportVersions) {
            assertFalse(description, clusterHasFeature(RestTestLegacyFeatures.STATE_REPLACED_TRANSPORT_VERSION_WITH_NODES_VERSION));
            assertTrue(description, clusterHasFeature(RestTestLegacyFeatures.TRANSPORT_VERSION_SUPPORTED));
            assertNotEquals(description, ClusterType.UPGRADED, CLUSTER_TYPE);

            assertEquals(description, nodeIds.size(), clusterState.evaluateArraySize("transport_versions"));
            for (int i = 0; i < nodeIds.size(); i++) {
                final var path = "transport_versions." + i;
                final String nodeId = clusterState.evaluate(path + ".node_id");
                final var nodeDescription = nodeId + "/" + description;
                final var transportVersion = TransportVersion.fromString(clusterState.evaluate(path + ".transport_version"));
                final var nodeVersion = versionsByNodeId.get(nodeId);
                assertNotNull(nodeDescription, nodeVersion);
                if (nodeVersion.equals(Build.current().version())) {
                    assertEquals(nodeDescription, TransportVersion.current(), transportVersion);
                } else {
                    assertThat(nodeDescription, transportVersion, greaterThanOrEqualTo(INFERRED_TRANSPORT_VERSION));
                }
            }
        } else if (hasNodesVersions) {
            assertFalse(
                description,
                clusterHasFeature(RestTestLegacyFeatures.STATE_REPLACED_TRANSPORT_VERSION_WITH_NODES_VERSION) == false
                    && CLUSTER_TYPE == ClusterType.OLD
            );

            assertEquals(description, nodeIds.size(), clusterState.evaluateArraySize("nodes_versions"));
            for (int i = 0; i < nodeIds.size(); i++) {
                final var path = "nodes_versions." + i;
                final String nodeId = clusterState.evaluate(path + ".node_id");
                final var nodeDescription = nodeId + "/" + description;
                final var transportVersion = TransportVersion.fromString(clusterState.evaluate(path + ".transport_version"));
                final var nodeVersion = versionsByNodeId.get(nodeId);
                assertNotNull(nodeDescription, nodeVersion);
                if (nodeVersion.equals(Build.current().version())) {
                    assertThat(
                        nodeDescription,
                        transportVersion,
                        clusterHasFeature(RestTestLegacyFeatures.TRANSPORT_VERSION_SUPPORTED)
                            ? equalTo(TransportVersion.current())
                            : oneOf(TransportVersion.current(), INFERRED_TRANSPORT_VERSION)
                    );
                    if (CLUSTER_TYPE == ClusterType.UPGRADED && transportVersion.equals(INFERRED_TRANSPORT_VERSION)) {
                        logger.info("{} - not fixed up yet, retrying", nodeDescription);
                        return false;
                    }
                } else {
                    var version = parseLegacyVersion(nodeVersion);
                    var transportVersionIntroduced = version.map(v -> v.after(VERSION_INTRODUCING_TRANSPORT_VERSIONS)).orElse(true);
                    if (transportVersionIntroduced) {
                        assertThat(nodeDescription, transportVersion, greaterThan(INFERRED_TRANSPORT_VERSION));
                    } else {
                        assertEquals(nodeDescription, TransportVersion.fromId(version.get().id()), transportVersion);
                    }
                }
            }
        }

        return true;
    }
}
