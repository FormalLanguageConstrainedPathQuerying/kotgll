/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling;

import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class InstanceType implements ToXContentObject {
    final String provider;
    final String region;
    final String name;

    InstanceType(String provider, String region, String name) {
        this.provider = provider != null ? provider : "";
        this.region = region != null ? region : "";
        this.name = name != null ? name : "";
    }

    /**
     * Creates a {@link InstanceType} from a {@link Map} of source data provided from profiling-hosts.
     *
     * @param source the source data
     * @return the {@link InstanceType}
     */
    public static InstanceType fromHostSource(Map<String, Object> source) {
        String region = (String) source.get("ec2.placement.region");
        if (region != null) {
            String instanceType = (String) source.get("ec2.instance_type");
            return new InstanceType("aws", region, instanceType);
        }

        String zone = (String) source.get("gce.instance.zone");
        if (zone != null) {
            region = zone.substring(zone.lastIndexOf('/') + 1);
            String[] tokens = region.split("-", 3);
            if (tokens.length > 2) {
                region = tokens[0] + "-" + tokens[1];
            }

            return new InstanceType("gcp", region, null);
        }

        region = (String) source.get("azure.compute.location");
        if (region != null) {
            String instanceType = (String) source.get("azure.compute.vmsize");
            return new InstanceType("azure", region, instanceType);
        }

        String provider = null;
        region = null;
        List<String> tags = listOf(source.get("profiling.host.tags"));
        for (String tag : tags) {
            String[] kv = tag.toLowerCase(Locale.ROOT).split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("cloud_provider".equals(kv[0])) {
                provider = kv[1];
            }
            if ("cloud_region".equals(kv[0])) {
                region = kv[1];
            }
        }

        return new InstanceType(provider, region, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> listOf(Object o) {
        if (o instanceof List) {
            return (List<T>) o;
        } else if (o != null) {
            return List.of((T) o);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("provider", this.provider);
        builder.field("region", this.region);
        builder.field("instance_type", this.name);
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
        InstanceType that = (InstanceType) o;
        return provider.equals(that.provider) && region.equals(that.region) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, region, name);
    }

    @Override
    public String toString() {
        return "provider '" + name + "' in region '" + region + "'";
    }
}
