/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
* @test
* @key stress randomness
* @bug 8299259
* @requires vm.compiler2.enabled
* @summary Test various cases of divisions/modulo which should not be split through iv phis.
* @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:LoopUnrollLimit=0 -XX:+StressGCM -XX:StressSeed=884154126
*                   -XX:CompileCommand=compileonly,compiler.splitif.TestSplitDivisionThroughPhi::*
*                   compiler.splitif.TestSplitDivisionThroughPhi
*/

/**
* @test
* @key stress randomness
* @bug 8299259
* @requires vm.compiler2.enabled
* @summary Test various cases of divisions/modulo which should not be split through iv phis.
* @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:LoopUnrollLimit=0 -XX:+StressGCM
*                   -XX:CompileCommand=compileonly,compiler.splitif.TestSplitDivisionThroughPhi::*
*                   compiler.splitif.TestSplitDivisionThroughPhi
*/

package compiler.splitif;

public class TestSplitDivisionThroughPhi {
    static int iFld;
    static long lFld;
    static boolean flag;


    public static void main(String[] strArr) {
        for (int i = 0; i < 5000; i++) {
            testPushDivIThruPhi();
            testPushDivIThruPhiInChain();
            testPushModIThruPhi();
            testPushModIThruPhiInChain();
            testPushDivLThruPhi();
            testPushDivLThruPhiInChain();
            testPushModLThruPhi();
            testPushModLThruPhiInChain();
        }
    }

    static void testPushDivIThruPhi() {
        for (int i = 10; i > 1; i -= 2) {
            iFld = 10 / i;
        }
    }

    static void testPushDivIThruPhiInChain() {
        for (int i = 10; i > 1; i -= 2) {
            for (int j = 0; j < 1; j++) {
            }
            iFld = 10 / (i * 100);
        }
    }

    static void testPushModIThruPhi() {
        for (int i = 10; i > 1; i -= 2) {
            iFld = 10 / i;
        }
    }

    static void testPushModIThruPhiInChain() {
        for (int i = 10; i > 1; i -= 2) {
            for (int j = 0; j < 1; j++) {
            }
            iFld = 10 / (i * 100);
        }
    }


    static void testPushDivLThruPhi() {
        for (long i = 10; i > 1; i -= 2) {
            lFld = 10L / i;

            for (int j = 0; j < 10; j++) {
                flag = !flag;
            }
        }
    }

    static void testPushDivLThruPhiInChain() {
        for (long i = 10; i > 1; i -= 2) {
            for (int j = 0; j < 1; j++) {
            }
            lFld = 10L / (i * 100L);

            for (int j = 0; j < 10; j++) {
                flag = !flag;
            }
        }
    }

    static void testPushModLThruPhi() {
        for (long i = 10; i > 1; i -= 2) {
            lFld = 10L % i;

            for (int j = 0; j < 10; j++) {
                flag = !flag;
            }
        }
    }

    static void testPushModLThruPhiInChain() {
        for (long i = 10; i > 1; i -= 2) {
            for (int j = 0; j < 1; j++) {
            }
            lFld = 10L % (i * 100L);

            for (int j = 0; j < 10; j++) {
                flag = !flag;
            }
        }
    }
}
