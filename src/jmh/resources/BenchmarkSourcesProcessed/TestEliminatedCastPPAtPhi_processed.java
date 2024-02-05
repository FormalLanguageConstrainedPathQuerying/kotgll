/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139771
 * @summary Eliminating CastPP nodes at Phis when they all come from a unique input may cause crash
 * @requires vm.gc=="Serial" | vm.gc=="Parallel"
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM
 *      compiler.controldependency.TestEliminatedCastPPAtPhi
 *
 */

package compiler.controldependency;

public class TestEliminatedCastPPAtPhi {

    static TestEliminatedCastPPAtPhi saved;
    static TestEliminatedCastPPAtPhi saved_not_null;

    int f;

    static int test(TestEliminatedCastPPAtPhi obj, int[] array, boolean flag) {
        int ret = array[0] + array[20];
        saved = obj;
        if (obj == null) {
            return ret;
        }
        saved_not_null = obj;

        int i = 0;
        for (; i < 10; i++);

        ret += array[i];

        TestEliminatedCastPPAtPhi res;
        if (flag) {
            res = saved;
        } else {
            res = saved_not_null;
        }






        return ret + res.f;
    }

    static public void main(String[] args) {
        int[] array = new int[100];
        TestEliminatedCastPPAtPhi obj = new TestEliminatedCastPPAtPhi();
        for (int i = 0; i < 20000; i++) {
            test(obj, array, (i%2) == 0);
        }
        test(null, array, true);
    }

}
