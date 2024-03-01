/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.test.cluster.local;

import org.elasticsearch.test.cluster.SettingsProvider;
import org.elasticsearch.test.cluster.local.LocalClusterSpec.LocalNodeSpec;
import org.elasticsearch.test.cluster.local.distribution.DistributionType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultSettingsProvider implements SettingsProvider {
    @Override
    public Map<String, String> get(LocalNodeSpec nodeSpec) {
        Map<String, String> settings = new HashMap<>();

        settings.put("node.attr.testattr", "test");
        settings.put("node.portsfile", "true");
        settings.put("http.port", "0");
        settings.put("transport.port", "0");
        settings.put("network.host", "_local_");

        if (nodeSpec.getDistributionType() == DistributionType.INTEG_TEST) {
            settings.put("xpack.security.enabled", "false");
        } else {
            if (nodeSpec.getVersion().onOrAfter("7.16.0")) {
                settings.put("cluster.deprecation_indexing.enabled", "false");
            }
        }

        settings.put("cluster.routing.allocation.disk.watermark.low", "1b");
        settings.put("cluster.routing.allocation.disk.watermark.high", "1b");
        settings.put("cluster.routing.allocation.disk.watermark.flood_stage", "1b");

        if (nodeSpec.getVersion().onOrAfter("7.9.0")) {
            settings.put("script.disable_max_compilations_rate", "true");
        } else {
            settings.put("script.max_compilations_rate", "2048/1m");
        }

        settings.put("indices.breaker.total.use_real_memory", "false");

        settings.put("discovery.initial_state_timeout", "0s");

        if (nodeSpec.getVersion().getMajor() >= 8) {
            settings.put("cluster.service.slow_task_logging_threshold", "5s");
            settings.put("cluster.service.slow_master_task_logging_threshold", "5s");
        }

        settings.put("action.destructive_requires_name", "false");

        String masterEligibleNodes = nodeSpec.getCluster()
            .getNodes()
            .stream()
            .filter(LocalNodeSpec::isMasterEligible)
            .map(LocalNodeSpec::getName)
            .collect(Collectors.joining(","));

        if (masterEligibleNodes.isEmpty()) {
            throw new IllegalStateException(
                "Cannot start cluster '" + nodeSpec.getCluster().getName() + "' as it configured with no master-eligible nodes."
            );
        }

        settings.put("cluster.initial_master_nodes", "[" + masterEligibleNodes + "]");
        settings.put("discovery.seed_providers", "file");
        settings.put("discovery.seed_hosts", "[]");

        return settings;
    }
}
