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

/*
 * @test
 * @bug 8078262 8177095
 * @summary Tests correct dominator information after loop peeling.
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.loopopts.TestLoopPeeling::test*
 *      compiler.loopopts.TestLoopPeeling
 */

package compiler.loopopts;

public class TestLoopPeeling {

    public int[] array = new int[100];

    public static void main(String args[]) {
        TestLoopPeeling test = new TestLoopPeeling();
        try {
            test.testArrayAccess1(0, 1);
            test.testArrayAccess2(0);
            test.testArrayAccess3(0, false);
            test.testArrayAllocation(0, 1);
        } catch (Exception e) {
        }
    }

    public void testArrayAccess1(int index, int inc) {
        int storeIndex = -1;

        for (; index < 10; index += inc) {
            if (inc == 42) return;

            if (storeIndex > 0 && array[storeIndex] == 42) return;

            if (index == 42) {
                array[storeIndex] = 1;
                return;
            }

            storeIndex++;
        }
    }

    public int testArrayAccess2(int index) {
        int storeIndex = Integer.MIN_VALUE;
        for (; index < 10; ++index) {
            if (index == 42) {
                return array[storeIndex-1]; 
            }
            storeIndex = 0;
        }
        return array[42]; 
    }

    public int testArrayAccess3(int index, boolean b) {
        int storeIndex = Integer.MIN_VALUE;
        for (; index < 10; ++index) {
            if (b) {
                return 0;
            }
            if (index == 42) {
                return array[storeIndex-1]; 
            }
            storeIndex = 0;
        }
        return array[42]; 
    }

    public byte[] testArrayAllocation(int index, int inc) {
        int allocationCount = -1;
        byte[] result;

        for (; index < 10; index += inc) {
            if (inc == 42) return null;

            if (index == 42) {
                result = new byte[allocationCount];
                return result;
            }

            allocationCount++;
        }
        return null;
    }
}

