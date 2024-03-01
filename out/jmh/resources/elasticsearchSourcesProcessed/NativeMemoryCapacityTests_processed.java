/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.autoscaling;

import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.utils.NativeMemoryCalculator;

import java.util.function.BiConsumer;

import static org.elasticsearch.xpack.ml.MachineLearning.NATIVE_EXECUTABLE_CODE_OVERHEAD;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

public class NativeMemoryCapacityTests extends ESTestCase {

    private static final int NUM_TEST_RUNS = 10;

    public void testMerge() {
        NativeMemoryCapacity capacity = new NativeMemoryCapacity(
            ByteSizeValue.ofGb(1).getBytes(),
            ByteSizeValue.ofMb(200).getBytes(),
            ByteSizeValue.ofMb(50).getBytes()
        );
        capacity = capacity.merge(new NativeMemoryCapacity(ByteSizeValue.ofGb(1).getBytes(), ByteSizeValue.ofMb(100).getBytes()));
        assertThat(capacity.getTierMlNativeMemoryRequirementExcludingOverhead(), equalTo(ByteSizeValue.ofGb(1).getBytes() * 2L));
        assertThat(capacity.getNodeMlNativeMemoryRequirementExcludingOverhead(), equalTo(ByteSizeValue.ofMb(200).getBytes()));
        assertThat(capacity.getJvmSize(), nullValue());

        capacity = capacity.merge(new NativeMemoryCapacity(ByteSizeValue.ofGb(1).getBytes(), ByteSizeValue.ofMb(300).getBytes()));

        assertThat(capacity.getTierMlNativeMemoryRequirementExcludingOverhead(), equalTo(ByteSizeValue.ofGb(1).getBytes() * 3L));
        assertThat(capacity.getNodeMlNativeMemoryRequirementExcludingOverhead(), equalTo(ByteSizeValue.ofMb(300).getBytes()));
        assertThat(capacity.getJvmSize(), nullValue());
    }

    /**
     * This situation arises while finding current capacity when scaling up from zero.
     */
    public void testAutoscalingCapacityFromZero() {

        MlMemoryAutoscalingCapacity autoscalingCapacity = NativeMemoryCapacity.ZERO.autoscalingCapacity(
            randomIntBetween(5, 90),
            randomBoolean(),
            randomLongBetween(100000000L, 10000000000L),
            randomIntBetween(0, 3)
        ).build();
        assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(0L));
        assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(0L));
    }

    public void testAutoscalingCapacity() {

        final long BYTES_IN_64GB = ByteSizeValue.ofGb(64).getBytes();
        final long AUTO_ML_MEMORY_FOR_64GB_NODE = NativeMemoryCalculator.allowedBytesForMl(BYTES_IN_64GB, randomIntBetween(5, 90), true);

        NativeMemoryCapacity capacity = new NativeMemoryCapacity(
            ByteSizeValue.ofGb(4).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
            ByteSizeValue.ofGb(1).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
            ByteSizeValue.ofMb(50).getBytes()
        );

        {
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                25,
                false,
                NativeMemoryCalculator.allowedBytesForMl(BYTES_IN_64GB, 25, false),
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(ByteSizeValue.ofGb(1).getBytes() * 4L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(ByteSizeValue.ofGb(4).getBytes() * 4L));
        }
        {
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(1335885824L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(4557111296L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(1).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(2134900736L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(7503609856L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(1).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(2134900736L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(7851737088L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(1).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(2134900736L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(8195670018L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(3).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(5712642048L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(7503609856L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(3).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(5712642048L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(7851737088L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(4).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(3).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(5712642048L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(8195670018L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(41750102016L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(47706013696L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(53666119680L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(20).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(29817307136L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(41750102016L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(20).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(29817307136L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(47706013696L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(30).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(20).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(29817307136L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(53666119680L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(100).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(131222994944L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(100).getBytes() - 2 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(131222994944L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(100).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(5).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(9294577664L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(137170518018L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(155).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(50).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                1
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(65611497472L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(202794598401L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(155).getBytes() - 4 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(50).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                2
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(65611497472L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(208758898688L));
        }
        {
            capacity = new NativeMemoryCapacity(
                ByteSizeValue.ofGb(155).getBytes() - 3 * NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes(),
                ByteSizeValue.ofGb(50).getBytes() - NATIVE_EXECUTABLE_CODE_OVERHEAD.getBytes()
            );
            MlMemoryAutoscalingCapacity autoscalingCapacity = capacity.autoscalingCapacity(
                randomIntBetween(5, 90),
                true,
                AUTO_ML_MEMORY_FOR_64GB_NODE,
                3
            ).build();
            assertThat(autoscalingCapacity.nodeSize().getBytes(), equalTo(65611497472L));
            assertThat(autoscalingCapacity.tierSize().getBytes(), equalTo(202794598401L));
        }
    }

    public void testAutoscalingCapacityConsistency() {
        final BiConsumer<NativeMemoryCapacity, Integer> consistentAutoAssertions = (nativeMemory, memoryPercentage) -> {
            MlMemoryAutoscalingCapacity autoscalingCapacity = nativeMemory.autoscalingCapacity(25, true, Long.MAX_VALUE, 1).build();
            assertThat(
                autoscalingCapacity.tierSize().getBytes(),
                greaterThan(nativeMemory.getTierMlNativeMemoryRequirementExcludingOverhead())
            );
            assertThat(
                autoscalingCapacity.nodeSize().getBytes(),
                greaterThan(nativeMemory.getNodeMlNativeMemoryRequirementExcludingOverhead())
            );
            assertThat(autoscalingCapacity.tierSize().getBytes(), greaterThanOrEqualTo(autoscalingCapacity.nodeSize().getBytes()));
        };

        { 
            assertThat(
                NativeMemoryCalculator.calculateApproxNecessaryNodeSize(
                    0L,
                    randomLongBetween(0L, ByteSizeValue.ofGb(100).getBytes()),
                    randomIntBetween(0, 100),
                    randomBoolean()
                ),
                equalTo(0L)
            );
            assertThat(
                NativeMemoryCalculator.calculateApproxNecessaryNodeSize(0L, null, randomIntBetween(0, 100), randomBoolean()),
                equalTo(0L)
            );
        }
        for (int i = 0; i < NUM_TEST_RUNS; i++) {
            int memoryPercentage = randomIntBetween(5, 200);
            { 
                long nodeMemory = randomLongBetween(ByteSizeValue.ofKb(100).getBytes(), ByteSizeValue.ofMb(500).getBytes());
                consistentAutoAssertions.accept(
                    new NativeMemoryCapacity(randomLongBetween(nodeMemory, nodeMemory * 4), nodeMemory),
                    memoryPercentage
                );
            }
            { 
                long nodeMemory = randomLongBetween(ByteSizeValue.ofMb(500).getBytes(), ByteSizeValue.ofGb(4).getBytes());
                consistentAutoAssertions.accept(
                    new NativeMemoryCapacity(randomLongBetween(nodeMemory, nodeMemory * 4), nodeMemory),
                    memoryPercentage
                );
            }
            { 
                long nodeMemory = randomLongBetween(ByteSizeValue.ofGb(30).getBytes(), ByteSizeValue.ofGb(60).getBytes());
                consistentAutoAssertions.accept(
                    new NativeMemoryCapacity(randomLongBetween(nodeMemory, nodeMemory * 4), nodeMemory),
                    memoryPercentage
                );
            }
        }
    }

}
