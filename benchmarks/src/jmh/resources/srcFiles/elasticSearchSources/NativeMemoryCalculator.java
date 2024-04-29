/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.utils;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.xpack.core.ml.MachineLearningField;
import org.elasticsearch.xpack.core.ml.dataframe.DataFrameAnalyticsConfig;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.ml.MachineLearning;

import java.util.OptionalLong;

import static org.elasticsearch.xpack.core.ml.MachineLearningField.USE_AUTO_MACHINE_MEMORY_PERCENT;
import static org.elasticsearch.xpack.ml.MachineLearning.MACHINE_MEMORY_NODE_ATTR;
import static org.elasticsearch.xpack.ml.MachineLearning.MAX_JVM_SIZE_NODE_ATTR;
import static org.elasticsearch.xpack.ml.MachineLearning.MAX_LAZY_ML_NODES;
import static org.elasticsearch.xpack.ml.MachineLearning.MAX_MACHINE_MEMORY_PERCENT;
import static org.elasticsearch.xpack.ml.MachineLearning.MAX_ML_NODE_SIZE;

public final class NativeMemoryCalculator {

    public static final long STATIC_JVM_UPPER_THRESHOLD = ByteSizeValue.ofGb(31).getBytes();
    public static final long MINIMUM_AUTOMATIC_NODE_SIZE = ByteSizeValue.ofMb(512).getBytes();
    private static final long OS_OVERHEAD = ByteSizeValue.ofMb(200).getBytes();
    public static final long JVM_SIZE_KNOT_POINT = ByteSizeValue.ofGb(16).getBytes();
    private static final long BYTES_IN_4MB = ByteSizeValue.ofMb(4).getBytes();
    private static final long MINIMUM_AUTOMATIC_JVM_SIZE = dynamicallyCalculateJvmSizeFromNodeSize(MINIMUM_AUTOMATIC_NODE_SIZE);

    private NativeMemoryCalculator() {}

    public static OptionalLong allowedBytesForMl(DiscoveryNode node, Settings settings) {
        if (node.getRoles().contains(DiscoveryNodeRole.ML_ROLE) == false) {
            return OptionalLong.empty();
        }
        return allowedBytesForMl(
            node.getAttributes().get(MACHINE_MEMORY_NODE_ATTR),
            node.getAttributes().get(MAX_JVM_SIZE_NODE_ATTR),
            MAX_MACHINE_MEMORY_PERCENT.get(settings),
            USE_AUTO_MACHINE_MEMORY_PERCENT.get(settings)
        );
    }

    public static OptionalLong allowedBytesForMl(DiscoveryNode node, ClusterSettings settings) {
        if (node.getRoles().contains(DiscoveryNodeRole.ML_ROLE) == false) {
            return OptionalLong.empty();
        }
        return allowedBytesForMl(
            node.getAttributes().get(MACHINE_MEMORY_NODE_ATTR),
            node.getAttributes().get(MAX_JVM_SIZE_NODE_ATTR),
            settings.get(MAX_MACHINE_MEMORY_PERCENT),
            settings.get(USE_AUTO_MACHINE_MEMORY_PERCENT)
        );
    }

    public static OptionalLong allowedBytesForMl(DiscoveryNode node, int maxMemoryPercent, boolean useAutoPercent) {
        if (node.getRoles().contains(DiscoveryNodeRole.ML_ROLE) == false) {
            return OptionalLong.empty();
        }
        return allowedBytesForMl(
            node.getAttributes().get(MACHINE_MEMORY_NODE_ATTR),
            node.getAttributes().get(MAX_JVM_SIZE_NODE_ATTR),
            maxMemoryPercent,
            useAutoPercent
        );
    }

    private static OptionalLong allowedBytesForMl(String nodeBytes, String jvmBytes, int maxMemoryPercent, boolean useAuto) {
        assert nodeBytes != null
            : "This private method should only be called for ML nodes, and all ML nodes should have the ml.machine_memory node attribute";
        if (nodeBytes == null) {
            return OptionalLong.empty();
        }
        final long machineMemory;
        try {
            machineMemory = Long.parseLong(nodeBytes);
        } catch (NumberFormatException e) {
            assert e == null : "ml.machine_memory should parse because we set it internally: invalid value was " + nodeBytes;
            return OptionalLong.empty();
        }
        assert jvmBytes != null
            : "This private method should only be called for ML nodes, and all ML nodes should have the ml.max_jvm_size node attribute";
        if (jvmBytes == null) {
            return OptionalLong.empty();
        }
        long jvmMemory;
        try {
            jvmMemory = Long.parseLong(jvmBytes);
        } catch (NumberFormatException e) {
            assert e == null : "ml.max_jvm_size should parse because we set it internally: invalid value was " + jvmBytes;
            return OptionalLong.empty();
        }
        return OptionalLong.of(allowedBytesForMl(machineMemory, jvmMemory, maxMemoryPercent, useAuto));
    }

    public static long calculateApproxNecessaryNodeSize(
        long mlNativeMemoryRequirement,
        Long jvmSize,
        int maxMemoryPercent,
        boolean useAuto
    ) {
        if (mlNativeMemoryRequirement == 0) {
            return 0;
        }
        if (useAuto) {
            jvmSize = jvmSize == null ? dynamicallyCalculateJvmSizeFromMlNativeMemorySize(mlNativeMemoryRequirement) : jvmSize;
            return Math.max(mlNativeMemoryRequirement + jvmSize + OS_OVERHEAD, MINIMUM_AUTOMATIC_NODE_SIZE);
        }
        return (long) Math.ceil((100.0 / maxMemoryPercent) * mlNativeMemoryRequirement);
    }

    static long allowedBytesForMl(long machineMemory, long jvmSize, int maxMemoryPercent, boolean useAuto) {
        if (machineMemory <= 0) {
            return 0L;
        }
        if (useAuto) {
            if (machineMemory - jvmSize <= OS_OVERHEAD) {
                return machineMemory / 100;
            }
            return Math.min(machineMemory - jvmSize - OS_OVERHEAD, machineMemory * 9 / 10);
        }
        return machineMemory * maxMemoryPercent / 100;
    }

    public static long allowedBytesForMl(long machineMemory, int maxMemoryPercent, boolean useAuto) {
        return allowedBytesForMl(
            machineMemory,
            useAuto ? dynamicallyCalculateJvmSizeFromNodeSize(machineMemory) : Math.min(machineMemory / 2, STATIC_JVM_UPPER_THRESHOLD),
            maxMemoryPercent,
            useAuto
        );
    }

    public static long dynamicallyCalculateJvmSizeFromNodeSize(long nodeSize) {
        if (nodeSize <= JVM_SIZE_KNOT_POINT) {
            return ((long) (nodeSize * 0.4) / BYTES_IN_4MB) * BYTES_IN_4MB;
        }
        return Math.min(
            ((long) (JVM_SIZE_KNOT_POINT * 0.4 + (nodeSize - JVM_SIZE_KNOT_POINT) * 0.1) / BYTES_IN_4MB) * BYTES_IN_4MB,
            STATIC_JVM_UPPER_THRESHOLD
        );
    }

    public static long dynamicallyCalculateJvmSizeFromMlNativeMemorySize(long mlNativeMemorySize) {
        long nativeAndOverhead = mlNativeMemorySize + OS_OVERHEAD;
        long higherAnswer;
        if (nativeAndOverhead <= (JVM_SIZE_KNOT_POINT - dynamicallyCalculateJvmSizeFromNodeSize(JVM_SIZE_KNOT_POINT))) {
            higherAnswer = (nativeAndOverhead * 2 / 3 / BYTES_IN_4MB) * BYTES_IN_4MB;
        } else {
            double nativeAndOverheadAbove16GB = nativeAndOverhead - JVM_SIZE_KNOT_POINT * 0.6;
            higherAnswer = ((long) (JVM_SIZE_KNOT_POINT * 0.4 + nativeAndOverheadAbove16GB / 0.9 * 0.1) / BYTES_IN_4MB) * BYTES_IN_4MB;
        }
        if (higherAnswer > BYTES_IN_4MB) {
            long lowerAnswer = higherAnswer - BYTES_IN_4MB;
            long nodeSizeImpliedByLowerAnswer = nativeAndOverhead + lowerAnswer;
            if (dynamicallyCalculateJvmSizeFromNodeSize(nodeSizeImpliedByLowerAnswer) == lowerAnswer) {
                return Math.max(MINIMUM_AUTOMATIC_JVM_SIZE, Math.min(lowerAnswer, STATIC_JVM_UPPER_THRESHOLD));
            }
        }
        return Math.max(MINIMUM_AUTOMATIC_JVM_SIZE, Math.min(higherAnswer, STATIC_JVM_UPPER_THRESHOLD));
    }

    /**
     * Calculates the highest model memory limit that a job could be
     * given and still stand a chance of being assigned in the cluster.
     * The calculation takes into account the possibility of autoscaling,
     * i.e. if lazy nodes are available then the maximum possible node
     * size is considered as well as the sizes of nodes in the current
     * cluster.
     */
    public static ByteSizeValue calculateMaxModelMemoryLimitToFit(ClusterSettings clusterSettings, DiscoveryNodes nodes) {

        long maxMlMemory = 0;

        for (DiscoveryNode node : nodes) {
            OptionalLong limit = allowedBytesForMl(node, clusterSettings);
            if (limit.isEmpty()) {
                continue;
            }
            maxMlMemory = Math.max(maxMlMemory, limit.getAsLong());
        }

        long maxMlNodeSize = clusterSettings.get(MAX_ML_NODE_SIZE).getBytes();
        int maxLazyNodes = clusterSettings.get(MAX_LAZY_ML_NODES);
        if (maxMlNodeSize > 0 && maxLazyNodes > 0) {
            maxMlMemory = Math.max(
                maxMlMemory,
                allowedBytesForMl(
                    maxMlNodeSize,
                    clusterSettings.get(MAX_MACHINE_MEMORY_PERCENT),
                    clusterSettings.get(USE_AUTO_MACHINE_MEMORY_PERCENT)
                )
            );
        }

        if (maxMlMemory == 0L) {
            return null;
        }

        maxMlMemory -= Math.max(Job.PROCESS_MEMORY_OVERHEAD.getBytes(), DataFrameAnalyticsConfig.PROCESS_MEMORY_OVERHEAD.getBytes());
        maxMlMemory -= MachineLearning.NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes();
        return ByteSizeValue.ofMb(ByteSizeUnit.BYTES.toMB(Math.max(0L, maxMlMemory)));
    }

    public static ByteSizeValue calculateTotalMlMemory(ClusterSettings clusterSettings, DiscoveryNodes nodes) {

        long totalMlMemory = 0;

        for (DiscoveryNode node : nodes) {
            OptionalLong limit = allowedBytesForMl(node, clusterSettings);
            if (limit.isEmpty()) {
                continue;
            }
            totalMlMemory += limit.getAsLong();
        }

        return ByteSizeValue.ofMb(ByteSizeUnit.BYTES.toMB(totalMlMemory));
    }

    /**
     * Get the maximum value of model memory limit that a user may set in a job config.
     * If the xpack.ml.max_model_memory_limit setting is set then the value comes from that.
     * Otherwise, if xpack.ml.use_auto_machine_memory_percent is set then the maximum model
     * memory limit is considered to be the largest model memory limit that could fit into
     * the cluster (on the assumption that configured lazy nodes will be added and other
     * jobs stopped to make space).
     * @return The maximum model memory limit calculated from the current cluster settings,
     *         or {@link ByteSizeValue#ZERO} if there is no limit.
     */
    public static ByteSizeValue getMaxModelMemoryLimit(ClusterService clusterService) {
        ClusterSettings clusterSettings = clusterService.getClusterSettings();
        ByteSizeValue maxModelMemoryLimit = clusterSettings.get(MachineLearningField.MAX_MODEL_MEMORY_LIMIT);
        if (maxModelMemoryLimit != null && maxModelMemoryLimit.getBytes() > 0) {
            return maxModelMemoryLimit;
        }
        Boolean autoMemory = clusterSettings.get(MachineLearningField.USE_AUTO_MACHINE_MEMORY_PERCENT);
        if (autoMemory) {
            DiscoveryNodes nodes = clusterService.state().getNodes();
            ByteSizeValue modelMemoryLimitToFit = calculateMaxModelMemoryLimitToFit(clusterSettings, nodes);
            if (modelMemoryLimitToFit != null) {
                return modelMemoryLimitToFit;
            }
        }
        return ByteSizeValue.ZERO;
    }
}
