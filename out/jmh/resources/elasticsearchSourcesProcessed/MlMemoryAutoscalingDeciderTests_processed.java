/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.autoscaling;

import org.elasticsearch.cluster.ClusterInfo;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.node.DiscoveryNodeUtils;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.decider.AwarenessAllocationDecider;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.snapshots.SnapshotShardSizeInfo;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.autoscaling.capacity.AutoscalingCapacity;
import org.elasticsearch.xpack.autoscaling.capacity.AutoscalingDeciderContext;
import org.elasticsearch.xpack.core.ml.MachineLearningField;
import org.elasticsearch.xpack.core.ml.MlConfigVersion;
import org.elasticsearch.xpack.core.ml.MlTasks;
import org.elasticsearch.xpack.core.ml.action.OpenJobAction;
import org.elasticsearch.xpack.core.ml.action.StartDataFrameAnalyticsAction;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.StartTrainedModelDeploymentAction;
import org.elasticsearch.xpack.core.ml.dataframe.DataFrameAnalyticsState;
import org.elasticsearch.xpack.core.ml.dataframe.DataFrameAnalyticsTaskState;
import org.elasticsearch.xpack.core.ml.inference.assignment.Priority;
import org.elasticsearch.xpack.core.ml.inference.assignment.TrainedModelAssignment;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.config.JobState;
import org.elasticsearch.xpack.core.ml.job.config.JobTaskState;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.job.NodeLoad;
import org.elasticsearch.xpack.ml.job.NodeLoadDetector;
import org.elasticsearch.xpack.ml.job.task.OpenJobPersistentTasksExecutorTests;
import org.elasticsearch.xpack.ml.process.MlMemoryTracker;
import org.elasticsearch.xpack.ml.utils.NativeMemoryCalculator;
import org.junit.Before;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.LongSupplier;

import static java.lang.Math.min;
import static org.elasticsearch.test.hamcrest.OptionalMatchers.isEmpty;
import static org.elasticsearch.test.hamcrest.OptionalMatchers.isPresent;
import static org.elasticsearch.xpack.ml.MachineLearning.MACHINE_MEMORY_NODE_ATTR;
import static org.elasticsearch.xpack.ml.MachineLearning.MAX_JVM_SIZE_NODE_ATTR;
import static org.elasticsearch.xpack.ml.MachineLearning.NATIVE_EXECUTABLE_CODE_OVERHEAD;
import static org.elasticsearch.xpack.ml.job.JobNodeSelector.AWAITING_LAZY_ASSIGNMENT;
import static org.elasticsearch.xpack.ml.utils.NativeMemoryCalculator.JVM_SIZE_KNOT_POINT;
import static org.elasticsearch.xpack.ml.utils.NativeMemoryCalculator.STATIC_JVM_UPPER_THRESHOLD;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MlMemoryAutoscalingDeciderTests extends ESTestCase {

    private static final long[] NODE_TIERS_NO_MONITORING = new long[] {
        ByteSizeValue.ofGb(1).getBytes(),
        ByteSizeValue.ofGb(2).getBytes(),
        ByteSizeValue.ofGb(4).getBytes(),
        ByteSizeValue.ofGb(8).getBytes(),
        ByteSizeValue.ofGb(16).getBytes(),
        ByteSizeValue.ofGb(32).getBytes(),
        ByteSizeValue.ofGb(64).getBytes(),
        ByteSizeValue.ofGb(15).getBytes(),
        ByteSizeValue.ofGb(30).getBytes(),
        ByteSizeValue.ofGb(60).getBytes() };

    private static final long MONITORING_ALLOWANCE_BYTES = ByteSizeValue.ofMb(360).getBytes();

    private static final long[] NODE_TIERS_WITH_MONITORING = Arrays.stream(NODE_TIERS_NO_MONITORING)
        .map(m -> m - MONITORING_ALLOWANCE_BYTES)
        .toArray();

    private static final long BYTES_IN_4MB = ByteSizeValue.ofMb(4).getBytes();

    public static long mlOnlyNodeJvmBytes(long systemMemoryBytes) {
        long unroundedBytes = (systemMemoryBytes <= JVM_SIZE_KNOT_POINT)
            ? (long) (systemMemoryBytes * 0.4)
            : (long) min(JVM_SIZE_KNOT_POINT * 0.4 + (systemMemoryBytes - JVM_SIZE_KNOT_POINT) * 0.1, STATIC_JVM_UPPER_THRESHOLD);
        return (unroundedBytes / BYTES_IN_4MB) * BYTES_IN_4MB;
    }

    public static final List<Tuple<Long, Long>> AUTO_NODE_TIERS_NO_MONITORING = Arrays.stream(NODE_TIERS_NO_MONITORING)
        .mapToObj(m -> Tuple.tuple(m, mlOnlyNodeJvmBytes(m)))
        .toList();

    public static final List<Tuple<Long, Long>> AUTO_NODE_TIERS_WITH_MONITORING = Arrays.stream(NODE_TIERS_WITH_MONITORING)
        .mapToObj(m -> Tuple.tuple(m, mlOnlyNodeJvmBytes(m)))
        .toList();

    private static final long TEST_NODE_SIZE = ByteSizeValue.ofGb(20).getBytes();
    private static final long ML_MEMORY_FOR_TEST_NODE_SIZE = NativeMemoryCalculator.allowedBytesForMl(TEST_NODE_SIZE, 0, true);
    private static final long TEST_JVM_SIZE = mlOnlyNodeJvmBytes(TEST_NODE_SIZE);
    private static final int TEST_ALLOCATED_PROCESSORS = 2;
    private static final long TEST_JOB_SIZE = ByteSizeValue.ofMb(200).getBytes();
    private static final long PER_NODE_OVERHEAD = MachineLearning.NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes();

    private NodeLoadDetector nodeLoadDetector;
    private NodeRealAvailabilityZoneMapper nodeRealAvailabilityZoneMapper;
    private ClusterService clusterService;
    private Settings settings;
    private LongSupplier timeSupplier;
    private MlMemoryTracker mlMemoryTracker;

    @Before
    public void setup() {
        mlMemoryTracker = mock(MlMemoryTracker.class);
        when(mlMemoryTracker.isRecentlyRefreshed()).thenReturn(true);
        when(mlMemoryTracker.asyncRefresh()).thenReturn(true);
        when(mlMemoryTracker.getAnomalyDetectorJobMemoryRequirement(any())).thenReturn(TEST_JOB_SIZE);
        when(mlMemoryTracker.getDataFrameAnalyticsJobMemoryRequirement(any())).thenReturn(TEST_JOB_SIZE);
        when(mlMemoryTracker.getTrainedModelAssignmentMemoryRequirement(any())).thenReturn(TEST_JOB_SIZE);
        when(mlMemoryTracker.getJobMemoryRequirement(any(), any())).thenReturn(TEST_JOB_SIZE);
        nodeLoadDetector = mock(NodeLoadDetector.class);
        when(nodeLoadDetector.getMlMemoryTracker()).thenReturn(mlMemoryTracker);
        when(nodeLoadDetector.detectNodeLoad(any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(
            NodeLoad.builder("any").setUseMemory(true).incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes()).build()
        );
        nodeRealAvailabilityZoneMapper = mock(NodeRealAvailabilityZoneMapper.class);
        clusterService = mock(ClusterService.class);
        settings = Settings.EMPTY;
        timeSupplier = System::currentTimeMillis;
        ClusterSettings cSettings = new ClusterSettings(
            settings,
            Set.of(
                MachineLearning.MAX_MACHINE_MEMORY_PERCENT,
                MachineLearning.MAX_OPEN_JOBS_PER_NODE,
                MachineLearningField.USE_AUTO_MACHINE_MEMORY_PERCENT,
                MachineLearning.MAX_ML_NODE_SIZE,
                AwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_AWARENESS_ATTRIBUTE_SETTING
            )
        );
        when(clusterService.getClusterSettings()).thenReturn(cSettings);
    }

    public void testScalingEdgeCase() {
        when(mlMemoryTracker.getAnomalyDetectorJobMemoryRequirement(any())).thenReturn(
            ByteSizeValue.ofMb(128).getBytes() + Job.PROCESS_MEMORY_OVERHEAD.getBytes()
        );
        when(mlMemoryTracker.getJobMemoryRequirement(any(), any())).thenReturn(
            ByteSizeValue.ofMb(128).getBytes() + Job.PROCESS_MEMORY_OVERHEAD.getBytes()
        );
        List<String> jobTasks = List.of("waiting_job");
        long mlMemoryFor1GbNode = autoBytesForMl(AUTO_NODE_TIERS_NO_MONITORING.get(0).v1(), AUTO_NODE_TIERS_NO_MONITORING.get(0).v2());
        List<NodeLoad> nodesForScaleup = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(mlMemoryFor1GbNode)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(
                    ByteSizeValue.ofMb(200).getBytes() + ByteSizeValue.ofMb(10).getBytes() + ByteSizeValue.ofMb(9).getBytes()
                        + Job.PROCESS_MEMORY_OVERHEAD.getBytes() * 3
                )
                .incNumAssignedAnomalyDetectorJobs()
                .incNumAssignedAnomalyDetectorJobs()
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        MlMemoryAutoscalingCapacity scaleUpResult = decider.checkForScaleUp(
            0,
            0,
            nodesForScaleup,
            jobTasks,
            List.of(),
            List.of(),
            List.of(),
            null,
            new NativeMemoryCapacity(
                mlMemoryFor1GbNode - PER_NODE_OVERHEAD,
                mlMemoryFor1GbNode - PER_NODE_OVERHEAD,
                AUTO_NODE_TIERS_NO_MONITORING.get(0).v2()
            )
        ).orElseThrow();

        assertThat(
            scaleUpResult.tierSize().getBytes(),
            allOf(greaterThan(ByteSizeValue.ofGb(1).getBytes()), lessThan(ByteSizeValue.ofGb(2).getBytes()))
        );

        long mlMemoryFor2GbNode = autoBytesForMl(AUTO_NODE_TIERS_NO_MONITORING.get(1).v1(), AUTO_NODE_TIERS_NO_MONITORING.get(1).v2());
        List<NodeLoad> nodeForScaleDown = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(mlMemoryFor2GbNode)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(
                    ByteSizeValue.ofMb(200).getBytes() + ByteSizeValue.ofMb(10).getBytes() + ByteSizeValue.ofMb(9).getBytes()
                        + ByteSizeValue.ofMb(128).getBytes() + Job.PROCESS_MEMORY_OVERHEAD.getBytes() * 4
                )
                .incNumAssignedAnomalyDetectorJobs()
                .incNumAssignedAnomalyDetectorJobs()
                .incNumAssignedAnomalyDetectorJobs()
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        MlMemoryAutoscalingCapacity result = decider.checkForScaleDown(
            nodeForScaleDown,
            ByteSizeValue.ofMb(200).getBytes() + Job.PROCESS_MEMORY_OVERHEAD.getBytes(),
            new NativeMemoryCapacity(mlMemoryFor2GbNode, mlMemoryFor2GbNode, AUTO_NODE_TIERS_NO_MONITORING.get(1).v2())
        ).orElseThrow();
        assertThat(
            result.tierSize().getBytes(),
            allOf(greaterThan(ByteSizeValue.ofGb(1).getBytes()), lessThan(ByteSizeValue.ofGb(2).getBytes()))
        );
    }

    public void testScaleStability() {
        for (int i = 0; i < 10; i++) {
            final long[] nodeTiers;
            final List<Tuple<Long, Long>> autoNodeTiers;
            if ((i % 2) == 0) {
                nodeTiers = NODE_TIERS_NO_MONITORING;
                autoNodeTiers = AUTO_NODE_TIERS_NO_MONITORING;
            } else {
                nodeTiers = NODE_TIERS_WITH_MONITORING;
                autoNodeTiers = AUTO_NODE_TIERS_WITH_MONITORING;
            }
            for (int tier = 0; tier < autoNodeTiers.size() - 1; tier++) {
                final Tuple<Long, Long> lowerTier = autoNodeTiers.get(tier);
                final long lowerTierNodeSize = lowerTier.v1();
                final long lowerTierJvmSize = lowerTier.v2();
                final long lowerTierMemoryForMl = autoBytesForMl(lowerTierNodeSize, lowerTierJvmSize);
                final Tuple<Long, Long> higherTier = autoNodeTiers.get(tier + 1);
                NodeLoad.Builder forScaleUp = new NodeLoad.Builder("any").setMaxMemory(lowerTierMemoryForMl)
                    .setMaxJobs(Integer.MAX_VALUE)
                    .setUseMemory(true);
                final long higherTierMemoryForMl = autoBytesForMl(higherTier.v1(), higherTier.v2());
                NodeLoad.Builder forScaleDown = new NodeLoad.Builder("any").setMaxMemory(higherTierMemoryForMl)
                    .setMaxJobs(Integer.MAX_VALUE)
                    .setUseMemory(true);
                long maxJobSize = 0;
                while (forScaleUp.getFreeMemory() > Job.PROCESS_MEMORY_OVERHEAD.getBytes()) {
                    long jobSize = randomLongBetween(Job.PROCESS_MEMORY_OVERHEAD.getBytes(), forScaleUp.getFreeMemory());
                    maxJobSize = Math.max(jobSize, maxJobSize);
                    forScaleUp.incNumAssignedAnomalyDetectorJobs().incAssignedAnomalyDetectorMemory(jobSize);
                    forScaleDown.incNumAssignedAnomalyDetectorJobs().incAssignedAnomalyDetectorMemory(jobSize);
                }
                NodeLoad nodeLoadForScaleUp = forScaleUp.build();
                List<String> waitingJobs = new ArrayList<>();
                while (forScaleDown.getFreeMemory() > Job.PROCESS_MEMORY_OVERHEAD.getBytes()) {
                    long jobSize = randomLongBetween(Job.PROCESS_MEMORY_OVERHEAD.getBytes(), forScaleDown.getFreeMemory());
                    maxJobSize = Math.max(jobSize, maxJobSize);
                    forScaleDown.incNumAssignedAnomalyDetectorJobs().incAssignedAnomalyDetectorMemory(jobSize);
                    String waitingJob = randomAlphaOfLength(10);
                    when(mlMemoryTracker.getAnomalyDetectorJobMemoryRequirement(eq(waitingJob))).thenReturn(jobSize);
                    when(mlMemoryTracker.getJobMemoryRequirement(eq(MlTasks.JOB_TASK_NAME), eq(waitingJob))).thenReturn(jobSize);
                    waitingJobs.add(waitingJob);
                }
                MlMemoryAutoscalingDecider decider = buildDecider();
                decider.setUseAuto(true);

                MlMemoryAutoscalingCapacity scaleUpResult = decider.checkForScaleUp(
                    0,
                    0,
                    List.of(nodeLoadForScaleUp),
                    waitingJobs,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    new NativeMemoryCapacity(lowerTierMemoryForMl, lowerTierMemoryForMl, lowerTierJvmSize)
                ).orElseThrow();

                long scaledUpTierSizeRequested = scaleUpResult.tierSize().getBytes();
                assertThat(scaledUpTierSizeRequested, greaterThan(lowerTierNodeSize));
                assertThat(scaleUpResult.nodeSize().getBytes(), greaterThanOrEqualTo(lowerTierNodeSize));
                int nextTier = Arrays.binarySearch(nodeTiers, scaledUpTierSizeRequested);
                if (nextTier < 0) {
                    nextTier = -nextTier - 1;
                }
                if (nextTier >= nodeTiers.length) {
                    break;
                }
                long scaledUpSize = nodeTiers[nextTier];
                assertThat(scaledUpSize, greaterThanOrEqualTo(scaledUpTierSizeRequested));
                long scaledUpJvmSize = autoNodeTiers.get(nextTier).v2();
                long scaledUpBytesForMl = autoBytesForMl(scaledUpSize, scaledUpJvmSize);
                NodeLoad nodeLoadForScaleDown = forScaleDown.build();
                Optional<MlMemoryAutoscalingCapacity> result = decider.checkForScaleDown(
                    List.of(nodeLoadForScaleDown),
                    maxJobSize,
                    new NativeMemoryCapacity(scaledUpBytesForMl, scaledUpBytesForMl, scaledUpJvmSize)
                );
                if (result.isPresent()) {
                    long tierSizeRequired = result.get().tierSize().getBytes();
                    int afterScaleDownTier = Arrays.binarySearch(nodeTiers, tierSizeRequired);
                    if (afterScaleDownTier < 0) {
                        afterScaleDownTier = -afterScaleDownTier - 1;
                    }
                    assertThat(afterScaleDownTier, equalTo(nextTier));
                }
            }
        }
    }

    public void testScaleUp_withNoJobsWaitingNoMlNodes() {
        MlMemoryAutoscalingDecider decider = buildDecider();

        assertThat(
            decider.checkForScaleUp(
                0,
                0,
                List.of(), 
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                NativeMemoryCapacity.ZERO 
            ),
            equalTo(Optional.empty())
        );
    }

    public void testScaleUp_withWaitingJobsAndAutoMemoryAndNoRoomInNodes() {
        ByteSizeValue anomalyDetectorJobSize = ByteSizeValue.ofGb(randomIntBetween(2, 4));
        ByteSizeValue analyticsJobSize = ByteSizeValue.ofGb(randomIntBetween(2, 4));
        when(mlMemoryTracker.getAnomalyDetectorJobMemoryRequirement(any())).thenReturn(anomalyDetectorJobSize.getBytes());
        when(mlMemoryTracker.getJobMemoryRequirement(eq(MlTasks.JOB_TASK_NAME), any())).thenReturn(anomalyDetectorJobSize.getBytes());
        when(mlMemoryTracker.getDataFrameAnalyticsJobMemoryRequirement(any())).thenReturn(analyticsJobSize.getBytes());
        when(mlMemoryTracker.getJobMemoryRequirement(eq(MlTasks.DATA_FRAME_ANALYTICS_TASK_NAME), any())).thenReturn(
            analyticsJobSize.getBytes()
        );
        List<String> jobTasks = List.of("waiting_job", "waiting_job_2");
        List<String> analytics = List.of("analytics_waiting");
        List<NodeLoad> fullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(anomalyDetectorJobSize.getBytes())
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(anomalyDetectorJobSize.getBytes(), anomalyDetectorJobSize.getBytes());
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                0,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(
                result.nodeSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(
                result.tierSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            assertThat(
                allowedBytesForMlNode,
                greaterThanOrEqualTo(Math.max(anomalyDetectorJobSize.getBytes(), analyticsJobSize.getBytes()) + PER_NODE_OVERHEAD)
            );
            assertThat(
                allowedBytesForMlTier,
                greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() * 3 + analyticsJobSize.getBytes() + PER_NODE_OVERHEAD)
            );
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(
                result.nodeSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(
                result.tierSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
            assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() * 3 + PER_NODE_OVERHEAD));
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                1,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(
                result.nodeSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(
                result.tierSize().getBytes(),
                randomIntBetween(5, 90), 
                true
            );
            assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
            assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() * 2 + PER_NODE_OVERHEAD));
        }
    }

    public void testScaleUp_withWaitingSnapshotUpgradesAndAutoMemoryAndNoRoomInNodes() {
        ByteSizeValue anomalyDetectorJobSize = ByteSizeValue.ofGb(randomIntBetween(2, 8));
        ByteSizeValue analyticsJobSize = ByteSizeValue.ofGb(randomIntBetween(2, 8));
        when(mlMemoryTracker.getAnomalyDetectorJobMemoryRequirement(any())).thenReturn(anomalyDetectorJobSize.getBytes());
        when(mlMemoryTracker.getJobMemoryRequirement(eq(MlTasks.JOB_TASK_NAME), any())).thenReturn(anomalyDetectorJobSize.getBytes());
        when(mlMemoryTracker.getDataFrameAnalyticsJobMemoryRequirement(any())).thenReturn(analyticsJobSize.getBytes());
        when(mlMemoryTracker.getJobMemoryRequirement(eq(MlTasks.DATA_FRAME_ANALYTICS_TASK_NAME), any())).thenReturn(
            analyticsJobSize.getBytes()
        );
        List<String> snapshotUpgradeTasks = List.of("waiting_upgrade", "waiting_upgrade_2");
        List<NodeLoad> fullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes())
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(ByteSizeValue.ofGb(1).getBytes(), ByteSizeValue.ofGb(1).getBytes());
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                0,
                fullyLoadedNode,
                List.of(),
                snapshotUpgradeTasks,
                List.of(),
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(result.nodeSize().getBytes(), 30, true);
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(result.tierSize().getBytes(), 30, true);
            assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
            assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() * 2 + PER_NODE_OVERHEAD));
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                1,
                fullyLoadedNode,
                List.of(),
                snapshotUpgradeTasks,
                List.of(),
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(result.nodeSize().getBytes(), 30, true);
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(result.tierSize().getBytes(), 30, true);
            assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
            assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() * 2 + PER_NODE_OVERHEAD));
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                1,
                1,
                fullyLoadedNode,
                List.of(),
                snapshotUpgradeTasks,
                List.of(),
                List.of(),
                null,
                NativeMemoryCapacity.ZERO
            );
            assertFalse(decision.isEmpty());
            MlMemoryAutoscalingCapacity result = decision.get();
            long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(result.nodeSize().getBytes(), 30, true);
            long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(result.tierSize().getBytes(), 30, true);
            assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
            assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(anomalyDetectorJobSize.getBytes() + PER_NODE_OVERHEAD));
        }
    }

    public void testScaleUp_withWaitingJobsAndRoomInNodes() {
        List<String> jobTasks = List.of("waiting_job", "waiting_job_2");
        List<String> analytics = List.of("analytics_waiting");
        when(nodeRealAvailabilityZoneMapper.getNumMlAvailabilityZones()).thenReturn(OptionalInt.of(2));
        List<NodeLoad> nodesWithRoom = List.of(
            NodeLoad.builder("partially_filled")
                .setMaxMemory(2 * TEST_JOB_SIZE + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .setMaxJobs(10)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(TEST_JOB_SIZE)
                .incNumAssignedAnomalyDetectorJobs()
                .build(),
            NodeLoad.builder("not_filled").setMaxMemory(TEST_JOB_SIZE + PER_NODE_OVERHEAD).setMaxJobs(10).setUseMemory(true).build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(3 * TEST_JOB_SIZE, TEST_JOB_SIZE);
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        {
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                0,
                nodesWithRoom,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertTrue(decision.isPresent());
            assertThat(decision.get().nodeSize().getBytes(), equalTo(4 * (TEST_JOB_SIZE + PER_NODE_OVERHEAD)));
            assertThat(decision.get().tierSize().getBytes(), equalTo(4 * (4 * TEST_JOB_SIZE + 2 * PER_NODE_OVERHEAD)));
        }
        {
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                1,
                nodesWithRoom,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isPresent());
        }
        {
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                1,
                0,
                nodesWithRoom,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isPresent());
        }
    }

    public void testScaleUp_withWaitingJobsAndNoRoomInNodes() {
        List<String> jobTasks = List.of("waiting_job", "waiting_job_2");
        List<String> analytics = List.of("analytics_waiting");
        List<NodeLoad> fullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes())
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(ByteSizeValue.ofGb(1).getBytes(), ByteSizeValue.ofGb(1).getBytes());
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                0,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(decision.get().nodeSize().getBytes(), equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)));
            assertThat(
                decision.get().tierSize().getBytes(),
                equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + 3 * TEST_JOB_SIZE + PER_NODE_OVERHEAD))
            );
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(decision.get().nodeSize().getBytes(), equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)));
            assertThat(
                decision.get().tierSize().getBytes(),
                equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + 2 * TEST_JOB_SIZE + PER_NODE_OVERHEAD))
            );
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                1,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(decision.get().nodeSize().getBytes(), equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)));
            assertThat(
                decision.get().tierSize().getBytes(),
                equalTo(4 * (ByteSizeValue.ofGb(1).getBytes() + TEST_JOB_SIZE + PER_NODE_OVERHEAD))
            );
        }
    }

    public void testScaleUp_withWaitingJobsAndSomeRoomInNodes() {
        List<String> jobTasks = List.of("waiting_job");
        List<String> analytics = List.of("analytics_waiting");
        List<NodeLoad> nearlyFullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(2 * TEST_JOB_SIZE - ByteSizeValue.ofMb(1).getBytes() + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(TEST_JOB_SIZE)
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(
            2 * TEST_JOB_SIZE - ByteSizeValue.ofMb(1).getBytes(),
            2 * TEST_JOB_SIZE - ByteSizeValue.ofMb(1).getBytes()
        );
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                0,
                nearlyFullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(
                decision.get().nodeSize().getBytes(),
                equalTo(4 * (2 * TEST_JOB_SIZE - ByteSizeValue.ofMb(1).getBytes() + PER_NODE_OVERHEAD))
            );
            assertThat(decision.get().tierSize().getBytes(), equalTo(4 * (3 * TEST_JOB_SIZE + PER_NODE_OVERHEAD)));
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                0,
                1,
                nearlyFullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(
                decision.get().nodeSize().getBytes(),
                equalTo(4 * (2 * TEST_JOB_SIZE - ByteSizeValue.ofMb(1).getBytes() + PER_NODE_OVERHEAD))
            );
            assertThat(decision.get().tierSize().getBytes(), equalTo(4 * (2 * TEST_JOB_SIZE + PER_NODE_OVERHEAD)));
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                1,
                1,
                nearlyFullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertTrue(decision.isEmpty());
        }
    }

    public void testScaleUp_withWaitingJobs_WithFutureCapacity() {
        List<String> jobTasks = List.of("waiting_job", "waiting_job_2");
        List<String> analytics = List.of("analytics_waiting");
        List<NodeLoad> fullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(ByteSizeValue.ofGb(1).getBytes())
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD)
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(
            ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD,
            ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD
        );
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                2,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                null,
                currentScale
            );
            assertTrue(decision.isEmpty()); 
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                2,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                currentScale,
                currentScale
            );
            assertTrue(decision.isEmpty()); 
        }
        { 
            Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
                2,
                1,
                fullyLoadedNode,
                jobTasks,
                List.of(),
                analytics,
                List.of(),
                NativeMemoryCapacity.ZERO,
                currentScale
            );
            assertFalse(decision.isEmpty());
            assertThat(decision.get().nodeSize().getBytes(), equalTo(ByteSizeValue.ofGb(4).getBytes()));
            assertThat(decision.get().tierSize().getBytes(), equalTo(ByteSizeValue.ofMb(6496).getBytes()));
        }
    }

    public void testScaleUp_withWaitingModelAndAutoMemoryAndNoRoomInNodes() {
        when(mlMemoryTracker.getTrainedModelAssignmentMemoryRequirement(any())).thenReturn(ByteSizeValue.ofGb(2).getBytes());
        List<NodeLoad> fullyLoadedNode = List.of(
            NodeLoad.builder("any")
                .setMaxMemory(ByteSizeValue.ofGb(1).getBytes() + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes())
                .incNumAssignedAnomalyDetectorJobs()
                .build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(ByteSizeValue.ofGb(1).getBytes(), ByteSizeValue.ofGb(1).getBytes());
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
            0,
            0,
            fullyLoadedNode,
            List.of(),
            List.of(),
            List.of(),
            List.of("foo"),
            null,
            currentScale
        );
        assertFalse(decision.isEmpty());
        MlMemoryAutoscalingCapacity result = decision.get();
        long allowedBytesForMlNode = NativeMemoryCalculator.allowedBytesForMl(result.nodeSize().getBytes(), 30, true);
        long allowedBytesForMlTier = NativeMemoryCalculator.allowedBytesForMl(result.tierSize().getBytes(), 30, true);
        assertThat(allowedBytesForMlNode, greaterThanOrEqualTo(ByteSizeValue.ofGb(2).getBytes() + PER_NODE_OVERHEAD));
        assertThat(allowedBytesForMlTier, greaterThanOrEqualTo(ByteSizeValue.ofGb(2).getBytes() + PER_NODE_OVERHEAD));
    }

    public void testScaleUp_withWaitingModelsAndRoomInNodes() {
        when(nodeRealAvailabilityZoneMapper.getNumMlAvailabilityZones()).thenReturn(OptionalInt.of(2));
        List<NodeLoad> nodesWithRoom = List.of(
            NodeLoad.builder("partially_filled")
                .setMaxMemory(2 * TEST_JOB_SIZE + PER_NODE_OVERHEAD)
                .setUseMemory(true)
                .setMaxJobs(10)
                .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                .incAssignedAnomalyDetectorMemory(TEST_JOB_SIZE)
                .incNumAssignedAnomalyDetectorJobs()
                .build(),
            NodeLoad.builder("not_filled").setMaxMemory(TEST_JOB_SIZE + PER_NODE_OVERHEAD).setMaxJobs(10).setUseMemory(true).build()
        );
        NativeMemoryCapacity currentScale = new NativeMemoryCapacity(3 * TEST_JOB_SIZE, TEST_JOB_SIZE);
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        Optional<MlMemoryAutoscalingCapacity> decision = decider.checkForScaleUp(
            0,
            0,
            nodesWithRoom,
            List.of(),
            List.of(),
            List.of(),
            List.of("foo", "bar", "baz"),
            null,
            currentScale
        );
        assertTrue(decision.isPresent());
        assertThat(decision.get().nodeSize().getBytes(), equalTo(4 * (TEST_JOB_SIZE + PER_NODE_OVERHEAD)));
        assertThat(decision.get().tierSize().getBytes(), equalTo(4 * (4 * TEST_JOB_SIZE + 2 * PER_NODE_OVERHEAD)));
        assertFalse(
            decider.checkForScaleUp(1, 0, nodesWithRoom, List.of(), List.of(), List.of(), List.of("foo", "bar"), null, currentScale)
                .isPresent()
        );
    }

    public void testScaleDown() {
        when(nodeRealAvailabilityZoneMapper.getNumMlAvailabilityZones()).thenReturn(OptionalInt.of(3));
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setMaxMachineMemoryPercent(25);
        { 
            List<NodeLoad> nodeLoads = List.of(
                NodeLoad.builder("foo")
                    .setMaxMemory(ByteSizeValue.ofGb(5).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("bar")
                    .setMaxMemory(ByteSizeValue.ofGb(5).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("baz")
                    .setMaxMemory(ByteSizeValue.ofGb(5).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build()
            );
            Optional<MlMemoryAutoscalingCapacity> result = decider.checkForScaleDown(
                nodeLoads,
                ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD,
                new NativeMemoryCapacity(
                    ByteSizeValue.ofGb(15).getBytes() - 3 * PER_NODE_OVERHEAD,
                    ByteSizeValue.ofGb(5).getBytes() - PER_NODE_OVERHEAD
                )
            );
            assertThat(result, isPresent());
            MlMemoryAutoscalingCapacity deciderResult = result.get();
            assertThat(deciderResult.nodeSize().getBytes(), equalTo(4 * ByteSizeValue.ofGb(1).getBytes()));
            assertThat(deciderResult.tierSize().getBytes(), equalTo(ByteSizeValue.ofGb(12).getBytes()));
        }
        { 
            List<NodeLoad> nodeLoads = List.of(
                NodeLoad.builder("foo")
                    .setMaxMemory(ByteSizeValue.ofGb(1).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("bar")
                    .setMaxMemory(ByteSizeValue.ofGb(1).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("baz")
                    .setMaxMemory(ByteSizeValue.ofGb(1).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build()
            );
            Optional<MlMemoryAutoscalingCapacity> result = decider.checkForScaleDown(
                nodeLoads,
                ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD,
                new NativeMemoryCapacity(
                    ByteSizeValue.ofGb(3).getBytes() - 3 * PER_NODE_OVERHEAD,
                    ByteSizeValue.ofGb(1).getBytes() - PER_NODE_OVERHEAD
                )
            );
            assertThat(result, isPresent());
            MlMemoryAutoscalingCapacity deciderResult = result.get();
            assertThat(deciderResult.nodeSize().getBytes(), equalTo(4 * ByteSizeValue.ofMb(100).getBytes()));
            assertThat(deciderResult.tierSize().getBytes(), equalTo(ByteSizeValue.ofMb(100).getBytes() * 12));
        }
        { 
            List<NodeLoad> nodeLoads = List.of(
                NodeLoad.builder("foo")
                    .setMaxMemory(ByteSizeValue.ofMb(100).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("bar")
                    .setMaxMemory(ByteSizeValue.ofMb(100).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build(),
                NodeLoad.builder("baz")
                    .setMaxMemory(ByteSizeValue.ofMb(100).getBytes())
                    .incAssignedNativeCodeOverheadMemory(PER_NODE_OVERHEAD)
                    .incAssignedAnomalyDetectorMemory(ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD)
                    .incNumAssignedAnomalyDetectorJobs()
                    .build()
            );
            Optional<MlMemoryAutoscalingCapacity> result = decider.checkForScaleDown(
                nodeLoads,
                ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD,
                new NativeMemoryCapacity(
                    ByteSizeValue.ofMb(300).getBytes() - 3 * PER_NODE_OVERHEAD,
                    ByteSizeValue.ofMb(100).getBytes() - PER_NODE_OVERHEAD
                )
            );
            assertThat(result, isEmpty());
        }
    }

    public void testCpuModelAssignmentRequirements() {
        assertTrue(
            MlMemoryAutoscalingDecider.modelAssignmentsRequireMoreThanHalfCpu(
                List.of(
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            2,
                            3,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build(),
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            1,
                            1,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build()
                ),
                withMlNodes("ml_node_1", "ml_node_2"),
                1
            )
        );
        assertTrue(
            MlMemoryAutoscalingDecider.modelAssignmentsRequireMoreThanHalfCpu(
                List.of(
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            1,
                            3,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build(),
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            1,
                            1,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build()
                ),
                withMlNodes("ml_node_1", "ml_node_2"),
                1
            )
        );
        assertFalse(
            MlMemoryAutoscalingDecider.modelAssignmentsRequireMoreThanHalfCpu(
                List.of(
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            1,
                            3,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build(),
                    TrainedModelAssignment.Builder.empty(
                        new StartTrainedModelDeploymentAction.TaskParams(
                            "model1",
                            "deployment_1",
                            TEST_JOB_SIZE,
                            1,
                            1,
                            100,
                            null,
                            Priority.NORMAL,
                            0L,
                            0L
                        )
                    ).build()
                ),
                withMlNodes("ml_node_1", "ml_node_2", "ml_node_3", "ml_node_4"),
                1
            )
        );
    }

    public void testEnsureScaleDown() {
        assertThat(
            MlMemoryAutoscalingDecider.ensureScaleDown(
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(1), ByteSizeValue.ofGb(8)).build(),
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(2), ByteSizeValue.ofGb(4)).build()
            ),
            equalTo(MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(1), ByteSizeValue.ofGb(4)).build())
        );

        assertThat(
            MlMemoryAutoscalingDecider.ensureScaleDown(
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(3), ByteSizeValue.ofGb(8)).build(),
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(2), ByteSizeValue.ofGb(4)).build()
            ),
            equalTo(MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(2), ByteSizeValue.ofGb(4)).build())
        );

        assertThat(
            MlMemoryAutoscalingDecider.ensureScaleDown(
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(3), ByteSizeValue.ofGb(4)).build(),
                MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(2), ByteSizeValue.ofGb(3)).build()
            ),
            equalTo(MlMemoryAutoscalingCapacity.builder(ByteSizeValue.ofGb(2), ByteSizeValue.ofGb(3)).build())
        );
    }

    public void testFutureAvailableCapacity() {
        nodeLoadDetector = new NodeLoadDetector(mlMemoryTracker);
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        boolean waitingAnalytics = randomBoolean();
        boolean waitingAnomalyJobs = waitingAnalytics == false || randomBoolean();
        int maxWaitingAnalytics = randomIntBetween(1, 2);
        int maxWaitingAnomaly = randomIntBetween(1, 2);
        List<String> assignedAnomalyJobs = randomList(0, 2, () -> randomAlphaOfLength(10));
        List<String> batchAnomalyJobs = randomList(0, 2, () -> randomAlphaOfLength(10));
        List<String> assignedAnalyticsJobs = randomList(0, 2, () -> randomAlphaOfLength(10));
        ClusterState clusterState = clusterState(
            assignedAnomalyJobs,
            batchAnomalyJobs,
            assignedAnalyticsJobs,
            waitingAnomalyJobs ? randomList(1, maxWaitingAnomaly, () -> randomAlphaOfLength(10)) : List.of(),
            waitingAnalytics ? randomList(1, maxWaitingAnalytics, () -> randomAlphaOfLength(10)) : List.of()
        );

        Collection<DiscoveryNode> mlNodesInCluster = clusterState.getNodes().getNodes().values();
        Optional<NativeMemoryCapacity> nativeMemoryCapacity = decider.calculateFutureAvailableCapacity(mlNodesInCluster, clusterState);
        assertThat(nativeMemoryCapacity, isPresent());
        assertThat(nativeMemoryCapacity.get().getNodeMlNativeMemoryRequirementExcludingOverhead(), greaterThanOrEqualTo(TEST_JOB_SIZE));
        assertThat(
            nativeMemoryCapacity.get().getNodeMlNativeMemoryRequirementExcludingOverhead(),
            lessThanOrEqualTo(ML_MEMORY_FOR_TEST_NODE_SIZE)
        );
        assertThat(
            nativeMemoryCapacity.get().getTierMlNativeMemoryRequirementExcludingOverhead(),
            greaterThanOrEqualTo(TEST_JOB_SIZE * (assignedAnalyticsJobs.size() + batchAnomalyJobs.size()))
        );
        assertThat(
            nativeMemoryCapacity.get().getTierMlNativeMemoryRequirementExcludingOverhead(),
            lessThanOrEqualTo(mlNodesInCluster.size() * (ML_MEMORY_FOR_TEST_NODE_SIZE - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()))
        );
    }

    public void testScale_WithNoScaleUpButWaitingJobs() {
        nodeLoadDetector = new NodeLoadDetector(mlMemoryTracker);
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);
        boolean waitingAnalytics = randomBoolean();
        boolean waitingAnomalyJobs = waitingAnalytics == false || randomBoolean();
        int maxWaitingAnalytics = randomIntBetween(1, 2);
        int maxWaitingAnomaly = randomIntBetween(1, 2);
        ClusterState clusterState = clusterState(
            randomList(0, 2, () -> randomAlphaOfLength(10)),
            randomList(0, 2, () -> randomAlphaOfLength(10)),
            randomList(0, 2, () -> randomAlphaOfLength(10)),
            waitingAnomalyJobs ? randomList(1, maxWaitingAnomaly, () -> randomAlphaOfLength(10)) : List.of(),
            waitingAnalytics ? randomList(1, maxWaitingAnalytics, () -> randomAlphaOfLength(10)) : List.of()
        );

        Settings settings = Settings.builder()
            .put(MlAutoscalingDeciderService.NUM_ANALYTICS_JOBS_IN_QUEUE.getKey(), maxWaitingAnalytics)
            .put(MlAutoscalingDeciderService.NUM_ANOMALY_JOBS_IN_QUEUE.getKey(), maxWaitingAnomaly)
            .build();
        AutoscalingCapacity autoscalingCapacity = new AutoscalingCapacity(
            new AutoscalingCapacity.AutoscalingResources(ByteSizeValue.ofGb(1), ByteSizeValue.ofGb(1), null),
            new AutoscalingCapacity.AutoscalingResources(ByteSizeValue.ofGb(1), ByteSizeValue.ofGb(1), null)
        );

        DeciderContext deciderContext = new DeciderContext(clusterState, autoscalingCapacity);
        MlAutoscalingContext mlAutoscalingContext = new MlAutoscalingContext(clusterState);

        MlMemoryAutoscalingCapacity result = decider.scale(settings, deciderContext, mlAutoscalingContext, 1);
        assertThat(result.reason(), containsString("but the number in the queue is less than the configured maximum allowed"));
        assertThat(result.nodeSize(), equalTo(ByteSizeValue.ofGb(1)));
        assertThat(result.tierSize(), equalTo(ByteSizeValue.ofGb(1)));
    }

    public void testScale_WithNoMlNodesButWaitingAnalytics() {
        nodeLoadDetector = new NodeLoadDetector(mlMemoryTracker);
        MlMemoryAutoscalingDecider decider = buildDecider();
        decider.setUseAuto(true);

        final String analyticsId = "waiting-analytics";

        PersistentTasksCustomMetadata.Builder tasksBuilder = PersistentTasksCustomMetadata.builder();
        addAnalyticsTask(analyticsId, null, DataFrameAnalyticsState.STARTING, tasksBuilder);
        ClusterState.Builder clusterStateBuilder = ClusterState.builder(new ClusterName("_name"));
        Metadata.Builder metadata = Metadata.builder();
        metadata.putCustom(PersistentTasksCustomMetadata.TYPE, tasksBuilder.build());
        clusterStateBuilder.metadata(metadata);
        ClusterState clusterState = clusterStateBuilder.build();

        Settings settings = Settings.builder()
            .put(MlAutoscalingDeciderService.NUM_ANALYTICS_JOBS_IN_QUEUE.getKey(), 0)
            .put(MlAutoscalingDeciderService.NUM_ANOMALY_JOBS_IN_QUEUE.getKey(), 0)
            .build();

        DeciderContext deciderContext = new DeciderContext(clusterState, AutoscalingCapacity.ZERO);
        MlAutoscalingContext mlAutoscalingContext = new MlAutoscalingContext(clusterState);

        MlMemoryAutoscalingCapacity result = decider.scale(settings, deciderContext, mlAutoscalingContext, 1);
        assertThat(
            result.reason(),
            containsString(
                "requesting scale up as number of jobs in queues exceeded configured limit and there are no machine learning nodes"
            )
        );
        assertThat(result.nodeSize(), equalTo(ByteSizeValue.ofMb(714)));
        assertThat(result.tierSize(), equalTo(ByteSizeValue.ofMb(714)));
    }

    private MlMemoryAutoscalingDecider buildDecider() {
        return new MlMemoryAutoscalingDecider(
            settings,
            clusterService,
            nodeRealAvailabilityZoneMapper,
            nodeLoadDetector,
            new ScaleTimer(timeSupplier)
        );
    }

    private static ClusterState clusterState(
        List<String> ongoingAnomalyTasks,
        List<String> batchAnomalyTasks,
        List<String> analyticsTasks,
        List<String> waitingAnomalyTasks,
        List<String> waitingAnalyticsTasks
    ) {
        List<String> nodeNames = List.of("_node_id1", "_node_id2", "_node_id3");
        List<DiscoveryNode> nodeList = withMlNodes(nodeNames.toArray(String[]::new));
        DiscoveryNodes.Builder nodesBuilder = DiscoveryNodes.builder();
        for (DiscoveryNode node : nodeList) {
            nodesBuilder.add(node);
        }
        PersistentTasksCustomMetadata.Builder tasksBuilder = PersistentTasksCustomMetadata.builder();
        for (String jobId : ongoingAnomalyTasks) {
            OpenJobPersistentTasksExecutorTests.addJobTask(
                jobId,
                randomFrom(nodeNames),
                randomFrom(JobState.CLOSING, JobState.OPENED, JobState.OPENING, null),
                tasksBuilder
            );
        }
        for (String jobId : batchAnomalyTasks) {
            String nodeAssignment = randomFrom(nodeNames);
            OpenJobPersistentTasksExecutorTests.addJobTask(
                jobId,
                nodeAssignment,
                randomFrom(JobState.CLOSING, JobState.OPENED, JobState.OPENING, null),
                tasksBuilder
            );
            StartDatafeedAction.DatafeedParams dfParams = new StartDatafeedAction.DatafeedParams(jobId + "-datafeed", 0);
            dfParams.setEndTime(new Date().getTime());
            tasksBuilder.addTask(
                MlTasks.datafeedTaskId(jobId + "-datafeed"),
                MlTasks.DATAFEED_TASK_NAME,
                dfParams,
                new PersistentTasksCustomMetadata.Assignment(nodeAssignment, "test")
            );
        }
        for (String analyticsId : analyticsTasks) {
            addAnalyticsTask(
                analyticsId,
                randomFrom(nodeNames),
                randomFrom(
                    DataFrameAnalyticsState.STARTED,
                    DataFrameAnalyticsState.REINDEXING,
                    DataFrameAnalyticsState.ANALYZING,
                    DataFrameAnalyticsState.STOPPING,
                    DataFrameAnalyticsState.STARTING
                ),
                tasksBuilder
            );
        }
        for (String job : waitingAnalyticsTasks) {
            addAnalyticsTask(job, null, null, tasksBuilder);
        }
        for (String job : waitingAnomalyTasks) {
            addJobTask(job, null, null, tasksBuilder);
        }
        PersistentTasksCustomMetadata tasks = tasksBuilder.build();
        ClusterState.Builder cs = ClusterState.builder(new ClusterName("_name"));
        cs.nodes(nodesBuilder);
        Metadata.Builder metadata = Metadata.builder();
        metadata.putCustom(PersistentTasksCustomMetadata.TYPE, tasks);
        cs.metadata(metadata);
        return cs.build();
    }

    private static List<DiscoveryNode> withMlNodes(String... nodeName) {
        return Arrays.stream(nodeName)
            .map(
                n -> DiscoveryNodeUtils.create(
                    n,
                    buildNewFakeTransportAddress(),
                    Map.of(
                        MACHINE_MEMORY_NODE_ATTR,
                        String.valueOf(TEST_NODE_SIZE),
                        MAX_JVM_SIZE_NODE_ATTR,
                        String.valueOf(TEST_JVM_SIZE),
                        MachineLearning.ALLOCATED_PROCESSORS_NODE_ATTR,
                        String.valueOf(TEST_ALLOCATED_PROCESSORS)
                    ),
                    Set.of(DiscoveryNodeRole.ML_ROLE)
                )
            )
            .toList();
    }

    public static void addAnalyticsTask(
        String jobId,
        String nodeId,
        DataFrameAnalyticsState jobState,
        PersistentTasksCustomMetadata.Builder builder
    ) {
        builder.addTask(
            MlTasks.dataFrameAnalyticsTaskId(jobId),
            MlTasks.DATA_FRAME_ANALYTICS_TASK_NAME,
            new StartDataFrameAnalyticsAction.TaskParams(jobId, MlConfigVersion.CURRENT, true),
            nodeId == null ? AWAITING_LAZY_ASSIGNMENT : new PersistentTasksCustomMetadata.Assignment(nodeId, "test assignment")
        );
        if (jobState != null) {
            builder.updateTaskState(
                MlTasks.dataFrameAnalyticsTaskId(jobId),
                new DataFrameAnalyticsTaskState(jobState, builder.getLastAllocationId(), null, Instant.now())
            );
        }
    }

    public static void addJobTask(String jobId, String nodeId, JobState jobState, PersistentTasksCustomMetadata.Builder builder) {
        builder.addTask(
            MlTasks.jobTaskId(jobId),
            MlTasks.JOB_TASK_NAME,
            new OpenJobAction.JobParams(jobId),
            nodeId == null ? AWAITING_LAZY_ASSIGNMENT : new PersistentTasksCustomMetadata.Assignment(nodeId, "test assignment")
        );
        if (jobState != null) {
            builder.updateTaskState(
                MlTasks.jobTaskId(jobId),
                new JobTaskState(jobState, builder.getLastAllocationId(), null, Instant.now())
            );
        }
    }

    static class DeciderContext implements AutoscalingDeciderContext {

        private final ClusterState state;
        private final AutoscalingCapacity capacity;

        DeciderContext(ClusterState state, AutoscalingCapacity capacity) {
            this.state = state;
            this.capacity = capacity;
        }

        @Override
        public ClusterState state() {
            return state;
        }

        @Override
        public AutoscalingCapacity currentCapacity() {
            return capacity;
        }

        @Override
        public Set<DiscoveryNode> nodes() {
            return null;
        }

        @Override
        public Set<DiscoveryNodeRole> roles() {
            return null;
        }

        @Override
        public ClusterInfo info() {
            return null;
        }

        @Override
        public SnapshotShardSizeInfo snapshotShardSizeInfo() {
            return null;
        }

        @Override
        public void ensureNotCancelled() {

        }
    }

    private static long autoBytesForMl(Long nodeSize, Long jvmSize) {
        return NativeMemoryCalculator.allowedBytesForMl(
            DiscoveryNodeUtils.create(
                "node",
                ESTestCase.buildNewFakeTransportAddress(),
                Map.of(MAX_JVM_SIZE_NODE_ATTR, jvmSize.toString(), MACHINE_MEMORY_NODE_ATTR, nodeSize.toString()),
                Set.of(DiscoveryNodeRole.ML_ROLE)
            ),
            0, 
            true
        ).orElseThrow();
    }
}
