/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.lang.management.ManagementFactory;
import utils.GarbageProducer;
import common.TmTool;
import utils.JstatResults;

/**
 * Base class for jstat testing which uses GarbageProducer to allocate garbage.
 */
public class GarbageProducerTest {

    private final static int ITERATIONS = 10;
    private final static float TARGET_MEMORY_USAGE = 0.7f;
    private final static float MEASUREMENT_TOLERANCE = 0.05f;
    private final GarbageProducer garbageProducer;
    private final TmTool<? extends JstatResults> jstatTool;

    public GarbageProducerTest(TmTool<? extends JstatResults> tool) {
        garbageProducer = new GarbageProducer(TARGET_MEMORY_USAGE);
        jstatTool = tool;
    }

    public void run() throws Exception {
        JstatResults measurement1 = jstatTool.measure();
        measurement1.assertConsistency();
        System.gc();
        garbageProducer.allocateMetaspaceAndHeap();
        System.gc();
        int i = 0;
        long collectionCountBefore = getCollectionCount();
        JstatResults measurement2 = jstatTool.measure();
        do {
            System.out.println("Measurement #" + i);
            long currentCounter = getCollectionCount();
            if (currentCounter == collectionCountBefore) {
                measurement2.assertConsistency();
                checkOldGenMeasurement(measurement2);
                return;
            } else {
                System.out.println("GC happened during measurement.");
            }
            collectionCountBefore = getCollectionCount();
            measurement2 = jstatTool.measure();

        } while (i++ < ITERATIONS);
        checkOldGenMeasurement(measurement2);
    }

    private void checkOldGenMeasurement(JstatResults measurement2) {
        float oldGenAllocationRatio = garbageProducer.getOldGenAllocationRatio() - MEASUREMENT_TOLERANCE;
        JstatResults.assertSpaceUtilization(measurement2, TARGET_MEMORY_USAGE, oldGenAllocationRatio);
    }

    private static long getCollectionCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(b -> b.getCollectionCount())
                .sum();
    }
}
