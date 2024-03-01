/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8292088
 * @requires vm.compiler2.enabled
 * @summary Test that OuterStripMinedLoop and its CountedLoop are both removed after the removal of Opaque1 and 2 nodes
            which allows the loop backedge to be optimized out.
 * @run main/othervm -XX:LoopMaxUnroll=0 -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.c2.TestRemoveSingleIterationLoop::test*
 *                   -XX:CompileCommand=dontinline,compiler.c2.TestRemoveSingleIterationLoop::dontInline
 *                   compiler.c2.TestRemoveSingleIterationLoop
 * @run main/othervm -XX:LoopMaxUnroll=2 -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.c2.TestRemoveSingleIterationLoop::test*
 *                   -XX:CompileCommand=dontinline,compiler.c2.TestRemoveSingleIterationLoop::dontInline
 *                   compiler.c2.TestRemoveSingleIterationLoop
 */
package compiler.c2;

public class TestRemoveSingleIterationLoop {
    static int N = 400;
    static int x = 3;
    static int y = 3;
    static volatile int[] iArr = new int[N];

    public static void main(String[] args) {
        testKnownLimit();
        testUnknownLimit();
        testKnownLimit2();
        testFuzzer();
    }

    private static void testKnownLimit() {
        int i = -10000;
        int limit = 500;

        int a = 2;
        for (; a < 4; a *= 2);
        for (int b = 2; b < a; b++) {
            i = 5;
        }

        if (i < limit + 1) { 
            for (; i < limit; i++) {
                y = 3;
            }

            int j = 6;
            while (j > i - 1) {
                j--;
                iArr[23] = 3;
            }
        }
    }

    private static void testUnknownLimit() {
        int i = -10000;
        int limit = x;

        int a = 2;
        for (; a < 4; a *= 2);
        for (int b = 2; b < a; b++) {
            i = 0;
        }
        if (i + 1 < limit) {
            i++;
            for (; i < limit; i++) {
                y = 3;
            }
            int t = 2;
            while (t > i - 1) {
                t--;
                iArr[23] = 3;
            }
        }
    }

    static int testKnownLimit2() {
        int i = -10000, j;
        int[][] iArr1 = new int[N][N];

        int a = 2;
        for (; a < 4; a *= 2);
        for (int b = 2; b < a; b++) {
            i = 1;
        }

        while (++i < 318) {
            dontInline();
        }

        for (j = 6; j > i; j--) {
            iArr[1] = 25327;
        }

        for (int y = 0; y < iArr1.length; y++) {
            dontInline(iArr1[2]);
        }
        return i;
    }

    static int testFuzzer() {
        int i = 1, j, iArr1[][] = new int[N][N];
        while (++i < 318) {
            dontInline();
            for (j = 5; j > i; j--) {
                iArr[1] = 25327;
            }
        }

        for (int y = 0; y < iArr1.length; y++) {
            dontInline(iArr1[2]);
        }
        return i;
    }

    static void dontInline() {}

    static void dontInline(int[] iArr) {}
}
