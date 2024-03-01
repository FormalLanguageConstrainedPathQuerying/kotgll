/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/SoftReference/SoftReferenceTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test that all SoftReferences has been cleared at time of OOM.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.SoftReference.SoftReferenceTest.SoftReferenceTest -stressTime 600
 */

package gc.gctests.SoftReference.SoftReferenceTest;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Random;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;

/**
 * Tests for the SoftReference handling in JRockit.
 */
public final class SoftReferenceTest extends GCTestBase {

        private ExecutionController stresser;

    /**
     * Test that all SoftReferences has been cleared at
     * time of OOM.
     *
     * @return success if all SoftReferences are NULL at
     * time of OOM.
     */
    public void run() {
        stresser = new Stresser("Stresser to limit execution time", runParams.getStressOptions());
        stresser.start(1);

        long seed;
        int minSize;
        int maxSize;
        int keepEveryXthObject;
        long counter = 0;

        seed = runParams.getSeed();
        minSize = 2048;
        maxSize = 32768;
        keepEveryXthObject = 4;


        Random rndGenerator = new Random(seed);
        long multiplier = maxSize - minSize;
        ArrayList arrSoftRefs = new ArrayList();
        ArrayList arrObjects = new ArrayList();
        long numberOfNotNulledObjects = 0;
        long oomSoftArraySize = 0;

        try {
            while (true && stresser.continueExecution()) {
                int allocationSize = ((int) (rndGenerator.nextDouble()
                        * multiplier)) + minSize;
                byte[] tmp = new byte[allocationSize];

                if (counter % keepEveryXthObject == 0) {
                    arrObjects.add(tmp);
                } else {
                    arrSoftRefs.add(new SoftReference(tmp));
                }

                tmp = null;

                counter++;
                if (counter == Long.MAX_VALUE) {
                    counter = 0;
                }
            }
        } catch (OutOfMemoryError oome) {
            oomSoftArraySize = arrSoftRefs.size();

            for (int i = 0; i < arrSoftRefs.size(); i++) {
                SoftReference sr = (SoftReference) arrSoftRefs.get(i);
                Object o = sr.get();

                if (o != null) {
                    numberOfNotNulledObjects++;
                }
            }


            arrSoftRefs = null;
            arrObjects = null;

            if (numberOfNotNulledObjects > 0) {
                throw new TestFailure(numberOfNotNulledObjects + " out of "
                        + oomSoftArraySize + " SoftReferences was not "
                        + "null at time of OutOfMemoryError");
            }
        } finally {
            arrSoftRefs = null;
            arrObjects = null;
        }


    }

    public static void main(String[] args) {
        GC.runTest(new SoftReferenceTest(), args);
    }
}
