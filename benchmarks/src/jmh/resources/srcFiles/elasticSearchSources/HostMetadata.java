/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling.action;

import org.elasticsearch.core.UpdateForV9;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

final class HostMetadata implements ToXContentObject {
    static final int DEFAULT_PROFILING_NUM_CORES = 4;
    final String hostID;
    final InstanceType instanceType;
    final String hostArchitecture; 
    final int profilingNumCores; 

    HostMetadata(String hostID, InstanceType instanceType, String hostArchitecture, Integer profilingNumCores) {
        this.hostID = hostID;
        this.instanceType = instanceType;
        this.hostArchitecture = hostArchitecture;
        this.profilingNumCores = profilingNumCores != null ? profilingNumCores : DEFAULT_PROFILING_NUM_CORES;
    }

    @UpdateForV9 
    public static HostMetadata fromSource(Map<String, Object> source) {
        if (source != null) {
            String hostID = (String) source.get("host.id");
            String hostArchitecture = (String) source.get("host.arch");
            if (hostArchitecture == null) {
                hostArchitecture = (String) source.get("profiling.host.machine");
            }
            Integer profilingNumCores = (Integer) source.get("profiling.agent.config.present_cpu_cores");
            return new HostMetadata(hostID, InstanceType.fromHostSource(source), hostArchitecture, profilingNumCores);
        }
        return new HostMetadata("", new InstanceType("", "", ""), "", null);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        instanceType.toXContent(builder, params);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HostMetadata that = (HostMetadata) o;
        return Objects.equals(hostID, that.hostID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostID);
    }
}
