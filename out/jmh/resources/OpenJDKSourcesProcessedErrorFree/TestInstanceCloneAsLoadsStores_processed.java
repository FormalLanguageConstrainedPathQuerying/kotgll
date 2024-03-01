/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6700100 8156760 8248226 8285301
 * @summary small instance clone as loads/stores
 * @library /
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressArrayCopyMacroNode
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:-ReduceInitialCardMarks
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:-ReduceInitialCardMarks -XX:-ReduceBulkZeroing
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+UnlockExperimentalVMOptions -XX:+AlwaysAtomicAccesses
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 */

package compiler.arraycopy;

public class TestInstanceCloneAsLoadsStores extends TestInstanceCloneUtils {

    static Object m1(D src) throws CloneNotSupportedException {
        return src.clone();
    }

    static int m2(D src) throws CloneNotSupportedException {
        D dest = (D)src.clone();
        return dest.i1 + dest.i2 + ((int)dest.i3) + dest.i4 + dest.i5;
    }

    static int m3(E src) throws CloneNotSupportedException {
        E dest = (E)src.clone();
        return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5 +
            dest.i6 + dest.i7 + dest.i8 + dest.i9;
    }

    static Object m4(A src) throws CloneNotSupportedException {
        return src.clone();
    }

    static int m5(A src) throws CloneNotSupportedException {
        A dest = (A)src.clone();
        return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5;
    }

    static Object m6(F src) throws CloneNotSupportedException {
        return src.clone();
    }

    static G m7(G src) throws CloneNotSupportedException {
        return (G)src.myclone();
    }

    static J m8(J src) throws CloneNotSupportedException {
        return (J)src.myclone();
    }

    public static void main(String[] args) throws Exception {

        TestInstanceCloneAsLoadsStores test = new TestInstanceCloneAsLoadsStores();

        test.doTest(d, "m1");
        test.doTest(d, "m2");
        test.doTest(e, "m3");
        test.doTest(a, "m4");
        test.doTest(a, "m5");
        test.doTest(f, "m6");
        test.doTest(g, "m7");
        test.doTest(k, "m8");

        if (!test.success) {
            throw new RuntimeException("some tests failed");
        }

    }
}
