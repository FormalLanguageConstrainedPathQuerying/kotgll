/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/gctests/AllocateWithoutOomTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 *   Small stress test that should be able to run for a specified
 *   time without hitting an OOM.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:-UseGCOverheadLimit
 *      gc.gctests.AllocateWithoutOomTest.AllocateWithoutOomTest
 */

package gc.gctests.AllocateWithoutOomTest;

import java.util.ArrayList;
import java.util.Random;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.test.Stresser;

/**
 * Small stress test that should be able to run for a specified
 * time without hitting an OOM.
 */
public class AllocateWithoutOomTest extends GCTestBase {

    /**
     * Small stress test that allocates objects in a certain interval
     * and runs for a specified time. It should not throw any OOM during
     * the execution.
     *
     * @return success if the test runs for the specified time without
     *         and exceptions being thrown.
     */
    @Override
    public void run() {
        int minSize;
        int maxSize;



        minSize = 2048;
        maxSize = 32768;


        ArrayList placeholder = new ArrayList();
        long multiplier = maxSize - minSize;
        Random rndGenerator = new Random(runParams.getSeed());

        long memoryUpperLimit = runParams.getTestMemory();
        long memoryLowerLimit = runParams.getTestMemory() / 3;
        long memoryAllocatedLowerLimit = memoryUpperLimit
                - memoryLowerLimit;


        long totalAllocatedMemory = 0;
        long totalAllocatedObjects = 0;
        int allocationSize = -1;
        long roundCounter = 1;

        try {
            Stresser stresser = new Stresser(runParams.getStressOptions());
            stresser.start(0);
            while (stresser.continueExecution()) {
                while (totalAllocatedMemory < memoryUpperLimit) {
                    allocationSize = ((int) (rndGenerator.nextDouble()
                            * multiplier)) + minSize;
                    byte[] tmp = new byte[allocationSize];
                    totalAllocatedMemory += allocationSize;
                    totalAllocatedObjects++;
                    placeholder.add(tmp);
                    tmp = null;
                }

                int indexToRemove = 1;

                while (totalAllocatedMemory > memoryAllocatedLowerLimit) {
                    if (placeholder.size() == 0) {
                        throw new TestFailure("No more objects to free, "
                                + "so we can't continue");
                    }

                    if (indexToRemove >= placeholder.size()) {
                        indexToRemove = (placeholder.size() == 1) ? 0 : 1;
                    }

                    byte[] tmp = (byte[]) placeholder.remove(indexToRemove);

                    totalAllocatedMemory -= tmp.length;
                    totalAllocatedObjects--;

                    tmp = null;
                    indexToRemove++;
                }

                roundCounter++;
            }
            placeholder = null;
            log.info("Passed. Completed " + roundCounter
                    + " rounds during the test");
        } catch (OutOfMemoryError oome) {
            placeholder = null;
            throw new TestFailure("OOM thrown when allocating an object of size "
                    + allocationSize, oome);
        } finally {
            placeholder = null;
        }
    }

    public static void main(String[] args) {
        GC.runTest(new AllocateWithoutOomTest(), args);
    }
}
