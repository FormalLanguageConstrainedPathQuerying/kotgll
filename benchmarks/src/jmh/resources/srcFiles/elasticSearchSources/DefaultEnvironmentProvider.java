/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.test.cluster.local;

import org.elasticsearch.test.cluster.EnvironmentProvider;
import org.elasticsearch.test.cluster.local.LocalClusterSpec.LocalNodeSpec;
import org.elasticsearch.test.cluster.local.distribution.DistributionType;
import org.elasticsearch.test.cluster.util.Version;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DefaultEnvironmentProvider implements EnvironmentProvider {
    private static final String HOSTNAME_OVERRIDE = "LinuxDarwinHostname";
    private static final String COMPUTERNAME_OVERRIDE = "WindowsComputername";

    @Override
    public Map<String, String> get(LocalNodeSpec nodeSpec) {
        Map<String, String> environment = new HashMap<>();

        if (nodeSpec.getDistributionType() == DistributionType.INTEG_TEST || nodeSpec.getVersion().equals(Version.CURRENT)) {
            environment.put("ES_JAVA_HOME", System.getProperty("java.home"));
        }

        environment.put("HOSTNAME", HOSTNAME_OVERRIDE);
        environment.put("COMPUTERNAME", COMPUTERNAME_OVERRIDE);

        environment.put("TZ", TimeZone.getDefault().getID());

        return environment;
    }
}
