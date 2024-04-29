/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestHumongousConcurrentStartUndo
 * @summary Tests an alternating sequence of Concurrent Mark and Concurrent Undo
 * cycles.
 * reclaim heap occupancy falls below the IHOP value.
 * @requires vm.gc.G1
 * @library /test/lib /testlibrary /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 *             jdk.test.whitebox.WhiteBox$WhiteBoxPermission
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   gc.g1.TestHumongousConcurrentStartUndo
 */

import gc.testlibrary.Helpers;

import jdk.test.whitebox.WhiteBox;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.lang.ref.Reference;

public class TestHumongousConcurrentStartUndo {
    private static final int HeapSize                       = 224; 
    private static final int HeapRegionSize                 = 1;   
    private static final int InitiatingHeapOccupancyPercent = 50;  
    private static final int YoungSize                      = HeapSize / 8;

    public static void main(String[] args) throws Exception {
        OutputAnalyzer output = ProcessTools.executeLimitedTestJava(
            "-Xbootclasspath/a:.",
            "-XX:+UseG1GC",
            "-Xms" + HeapSize + "m",
            "-Xmx" + HeapSize + "m",
            "-Xmn" + YoungSize + "m",
            "-XX:G1HeapRegionSize=" + HeapRegionSize + "m",
            "-XX:InitiatingHeapOccupancyPercent=" + InitiatingHeapOccupancyPercent,
            "-XX:-G1UseAdaptiveIHOP",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:gc*",
            EdenObjectAllocatorWithHumongousAllocation.class.getName());

        output.shouldContain("Pause Young (Concurrent Start) (G1 Humongous Allocation)");
        output.shouldContain("Concurrent Undo Cycle");
        output.shouldContain("Concurrent Mark Cycle");
        output.shouldHaveExitValue(0);
        System.out.println(output.getStdout());
    }

    static class EdenObjectAllocatorWithHumongousAllocation {
        private static final WhiteBox WB = WhiteBox.getWhiteBox();

        private static final int M = 1024 * 1024;
        private static final int HumongousObjectSize =
                (int)(HeapRegionSize * M * 0.75);
        private static final int NumHumongousObjectAllocations =
                (int)(((HeapSize - YoungSize) * 80 / 100.0) / HeapRegionSize);


        private static void allocateHumongous(int num, Object[] holder) {
            for (int i = 0; i < num; i++) {
                if (i % 10 == 0) {
                    System.out.println("Allocating humongous object " + i + "/" + num +
                                       " of size " + HumongousObjectSize + " bytes");
                }
                holder[i % holder.length] = new byte[HumongousObjectSize];
            }
        }

        private static void runConcurrentUndoCycle() {
            WB.fullGC();
            allocateHumongous(NumHumongousObjectAllocations, new Object[1]);
            Helpers.waitTillCMCFinished(WB, 1);
        }

        private static void runConcurrentMarkCycle() {
            Object[] a = new Object[NumHumongousObjectAllocations];
            WB.fullGC();
            try {
                System.out.println("Acquire CM control");
                WB.concurrentGCAcquireControl();
                allocateHumongous(NumHumongousObjectAllocations, a);
            } finally {
                System.out.println("Release CM control");
                WB.concurrentGCReleaseControl();
            }
            allocateHumongous(1, new Object[1]);
            Helpers.waitTillCMCFinished(WB, 1);

            Reference.reachabilityFence(a);
        }

        public static void main(String [] args) throws Exception {
            for (int iterate = 0; iterate < 3; iterate++) {
                runConcurrentUndoCycle();
                runConcurrentMarkCycle();
            }
        }
    }
}
