/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.conversions;

import java.util.Objects;
import java.util.Random;
import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8254317 8256730
 * @requires vm.compiler2.enabled
 * @summary Exercises the optimization that moves integer-to-long conversions
 *          upwards through different shapes of integer addition
 *          subgraphs. Contains three small functional tests and two stress
 *          tests that resulted in a compilation time and memory explosion
 *          before fixing bug 8254317. The stress tests run with -Xbatch to wait
 *          for C2, so that a timeout or an out-of-memory error is triggered if
 *          there was an explosion. These tests use a timeout of 30s to catch
 *          the explosion earlier.
 * @library /test/lib /
 * @run main/othervm
 *      compiler.conversions.TestMoveConvI2LOrCastIIThruAddIs functional
 * @run main/othervm/timeout=30 -Xbatch
 *      compiler.conversions.TestMoveConvI2LOrCastIIThruAddIs stress1
 * @run main/othervm/timeout=30 -Xbatch
 *      compiler.conversions.TestMoveConvI2LOrCastIIThruAddIs stress2
 * @run main/othervm/timeout=30 -Xbatch
 *      compiler.conversions.TestMoveConvI2LOrCastIIThruAddIs stress3
 * @run main/othervm/timeout=30 -Xbatch
 *      compiler.conversions.TestMoveConvI2LOrCastIIThruAddIs stress4
 */

public class TestMoveConvI2LOrCastIIThruAddIs {

    static final int N = 100_000;

    static long testChain(boolean cnd) {
        int a = cnd ? 1 : 2;
        int b = a + a;
        int c = b + b;
        int d = c + c;
        return d;
    }

    static long testTree(boolean cnd) {
        int a0 = cnd ? 1 : 2;
        int a1 = cnd ? 1 : 2;
        int a2 = cnd ? 1 : 2;
        int a3 = cnd ? 1 : 2;
        int a4 = cnd ? 1 : 2;
        int a5 = cnd ? 1 : 2;
        int a6 = cnd ? 1 : 2;
        int a7 = cnd ? 1 : 2;
        int b0 = a0 + a1;
        int b1 = a2 + a3;
        int b2 = a4 + a5;
        int b3 = a6 + a7;
        int c0 = b0 + b1;
        int c1 = b2 + b3;
        int d = c0 + c1;
        return d;
    }

    static long testDAG(boolean cnd) {
        int a0 = cnd ? 1 : 2;
        int a1 = cnd ? 1 : 2;
        int a2 = cnd ? 1 : 2;
        int a3 = cnd ? 1 : 2;
        int b0 = a0 + a1;
        int b1 = a1 + a2;
        int b2 = a2 + a3;
        int c0 = b0 + b1;
        int c1 = b1 + b2;
        int d = c0 + c1;
        return d;
    }

    static long testStress1(boolean cnd) {
        int a = cnd ? 1 : 2;
        for (int i = 0; i < 28; i++) {
            a = a + a;
        }
        return a;
    }

    static long testStress2(boolean cnd) {
        int a = cnd ? 1 : 2;
        int b = a;
        int c = a + a;
        for (int i = 0; i < 20; i++) {
            b = b + c;
            c = b + c;
        }
        int d = b + c;
        return d;
    }

    static long testStress3(int a) {
        Objects.checkIndex(a, 2);
        for (int i = 0; i < 28; i++) {
            a = a + a;
        }
        return Objects.checkIndex(a, 2);
    }

    static long testStress4(int a) {
        a = Objects.checkIndex(a, 2);
        int b = a;
        int c = a + a;
        for (int i = 0; i < 20; i++) {
            b = b + c;
            c = b + c;
        }
        int d = b + c;
        return Objects.checkIndex(d, 2);
    }

    public static void main(String[] args) {
        Random rnd = new Random();
        switch(args[0]) {
        case "functional":
            for (int i = 0; i < N; i++) {
                boolean cnd = rnd.nextBoolean();
                Asserts.assertEQ(testChain(cnd), cnd ? 8L : 16L);
                Asserts.assertEQ(testTree(cnd), cnd ? 8L : 16L);
                Asserts.assertEQ(testDAG(cnd), cnd ? 8L : 16L);
            }
            break;
        case "stress1":
            for (int i = 0; i < N; i++) {
                boolean cnd = rnd.nextBoolean();
                Asserts.assertEQ(testStress1(cnd),
                                 cnd ? 268435456L : 536870912L);
            }
            break;
        case "stress2":
            for (int i = 0; i < N; i++) {
                boolean cnd = rnd.nextBoolean();
                Asserts.assertEQ(testStress2(cnd),
                                 cnd ? 701408733L : 1402817466L);
            }
            break;
        case "stress3":
            for (int i = 0; i < N; i++) {
                testStress3(0);
            }
            break;
        case "stress4":
            for (int i = 0; i < N; i++) {
                testStress4(0);
            }
            break;
        default:
            System.out.println("invalid mode");
        }
    }
}
