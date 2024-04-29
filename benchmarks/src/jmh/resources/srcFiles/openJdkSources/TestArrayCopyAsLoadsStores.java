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

/*
 * @test
 * @bug 6912521
 * @summary small array copy as loads/stores
 * @library /
 *
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestArrayCopyAsLoadsStores::m*
 *                   -XX:TypeProfileLevel=200
 *                   compiler.arraycopy.TestArrayCopyAsLoadsStores
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestArrayCopyAsLoadsStores::m*
 *                   -XX:TypeProfileLevel=200
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressArrayCopyMacroNode
 *                   compiler.arraycopy.TestArrayCopyAsLoadsStores
 */

/*
 * @test
 * @bug 8282590
 * @library /
 *
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestArrayCopyAsLoadsStores::m*
 *                   -XX:TypeProfileLevel=200
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressArrayCopyMacroNode
 *                   -XX:-TieredCompilation -XX:+StressReflectiveCode -XX:-ReduceInitialCardMarks
 *                   compiler.arraycopy.TestArrayCopyAsLoadsStores
 */

package compiler.arraycopy;

import java.util.Arrays;

public class TestArrayCopyAsLoadsStores extends TestArrayCopyUtils {

    @Args(src=ArraySrc.SMALL)
    static A[] m1() throws CloneNotSupportedException {
        return (A[])small_a_src.clone();
    }

    @Args(src=ArraySrc.SMALL)
    static int[] m2() throws CloneNotSupportedException {
        return (int[])small_int_src.clone();
    }

    @Args(src=ArraySrc.SMALL)
    static int m3() throws CloneNotSupportedException {
        int[] array_clone = (int[])small_int_src.clone();
        return array_clone[0] + array_clone[1] + array_clone[2] +
            array_clone[3] + array_clone[4];
    }

    @Args(src=ArraySrc.LARGE)
    static int[] m4() throws CloneNotSupportedException {
        return (int[])large_int_src.clone();
    }

    @Args(src=ArraySrc.ZERO)
    static int[] m5() throws CloneNotSupportedException {
        return (int[])zero_int_src.clone();
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NEW)
    static void m6(int[] src, int[] dest) {
        System.arraycopy(src, 0, dest, 0, 5);
    }

    @Args(src=ArraySrc.LARGE, dst=ArrayDst.NEW)
    static void m7(int[] src, int[] dest) {
        System.arraycopy(src, 0, dest, 0, 10);
    }

    @Args(src=ArraySrc.SMALL)
    static A[] m8(A[] src) {
        src[0] = src[0]; 
        A[] dest = new A[5];
        System.arraycopy(src, 0, dest, 0, 5);
        return dest;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NEW)
    static void m9(A[] src, A[] dest) {
        System.arraycopy(src, 0, dest, 0, 5);
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.SRC)
    static void m10(int[] src, int[] dest) {
        System.arraycopy(src, 0, dest, 1, 4);
    }

    static boolean m10_check(int[] src, int[] dest) {
        boolean failure = false;
        for (int i = 0; i < 5; i++) {
            int j = Math.max(i - 1, 0);
            if (dest[i] != src[j]) {
                System.out.println("Test m10 failed for " + i + " src[" + j +"]=" + src[j] + ", dest[" + i + "]=" + dest[i]);
                failure = true;
            }
        }
        return failure;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.SRC)
    static void m11(int[] src, int[] dest) {
        System.arraycopy(src, 1, dest, 0, 4);
    }

    static boolean m11_check(int[] src, int[] dest) {
        boolean failure = false;
        for (int i = 0; i < 5; i++) {
            int j = Math.min(i + 1, 4);
            if (dest[i] != src[j]) {
                System.out.println("Test m11 failed for " + i + " src[" + j +"]=" + src[j] + ", dest[" + i + "]=" + dest[i]);
                failure = true;
            }
        }
        return failure;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.SRC, extra_args={0,1})
    static void m12(int[] src, int[] dest, int srcPos, int destPos) {
        System.arraycopy(src, srcPos, dest, destPos, 4);
    }

    static boolean m12_check(int[] src, int[] dest) {
        boolean failure = false;
        for (int i = 0; i < 5; i++) {
            int j = Math.max(i - 1, 0);
            if (dest[i] != src[j]) {
                System.out.println("Test m10 failed for " + i + " src[" + j +"]=" + src[j] + ", dest[" + i + "]=" + dest[i]);
                failure = true;
            }
        }
        return failure;
    }

    @Args(src=ArraySrc.SMALL)
    static int m13(int[] src) {
        int[] dest = new int[5];
        System.arraycopy(src, 0, dest, 0, 5);
        return dest[0] + dest[1] + dest[2] + dest[3] + dest[4];
    }

    @Args(src=ArraySrc.ZERO, dst=ArrayDst.NEW)
    static void m14(int[] src, int[] dest) {
        System.arraycopy(src, 0, dest, 0, 0);
    }

    @Args(src=ArraySrc.SMALL)
    static A[] m15() {
        return Arrays.copyOf(small_a_src, 5, A[].class);
    }

    static Object[] helper16(int i) {
        Object[] arr = null;
        if ((i%2) == 0) {
            arr = small_a_src;
        } else {
            arr = small_object_src;
        }
        return arr;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NONE, extra_args={0})
    static A[] m16(A[] unused_src, int i) {
        Object[] arr = helper16(i);
        return Arrays.copyOf(arr, 5, A[].class);
    }

    static Object[] helper17_1(int i) {
        Object[] arr = null;
        if ((i%2) == 0) {
            arr = small_a_src;
        } else {
            arr = small_object_src;
        }
        return arr;
    }

    static A[] helper17_2(Object[] arr) {
        return Arrays.copyOf(arr, 5, A[].class);
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NONE, extra_args={0})
    static A[] m17(A[] unused_src, int i) {
        Object[] arr = helper17_1(i);
        return helper17_2(arr);
    }

    static Object[] helper18_1(int i) {
        Object[] arr = null;
        if ((i%2) == 0) {
            arr = small_a_src;
        } else {
            arr = small_object_src;
        }
        return arr;
    }

    static Object[] helper18_2(Object[] arr) {
        return Arrays.copyOf(arr, 5, Object[].class);
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NONE, extra_args={0})
    static Object[] m18(A[] unused_src, int i) {
        Object[] arr = helper18_1(i);
        return helper18_2(arr);
    }

    static Object[] helper19(int i) {
        Object[] arr = null;
        if ((i%2) == 0) {
            arr = small_a_src;
        } else {
            arr = small_object_src;
        }
        return arr;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NONE, extra_args={0})
    static A[] m19(A[] unused_src, int i) {
        Object[] arr = helper19(i);
        return Arrays.copyOf(arr, 5, A[].class);
    }

    @Args(src=ArraySrc.LARGE)
    static A[] m20() {
        return Arrays.copyOf(large_a_src, 10, A[].class);
    }

    @Args(src=ArraySrc.ZERO)
    static A[] m21() {
        return Arrays.copyOf(zero_a_src, 0, A[].class);
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.NEW, extra_args={0})
    static void m22(int[] src, int[] dest, int srcPos) {
        System.arraycopy(src, srcPos, dest, 0, 5);
    }

    @Args(src=ArraySrc.SMALL)
    static A[] m23() {
        return Arrays.copyOfRange(small_a_src, 1, 4, A[].class);
    }

    static boolean m23_check(A[] src, A[] dest) {
        boolean failure = false;
        for (int i = 0; i < 3; i++) {
            if (src[i+1] != dest[i]) {
                System.out.println("Test m23 failed for " + i + " src[" + (i+1) +"]=" + dest[i] + ", dest[" + i + "]=" + dest[i]);
                failure = true;
            }
        }
        return failure;
    }

    @Args(src=ArraySrc.SMALL)
    static A[] m24(Object[] src) {
        src[0] = src[0]; 
        A[] dest = new A[5];
        System.arraycopy(src, 0, dest, 0, 5);
        return dest;
    }

    @Args(src=ArraySrc.SMALL, dst=ArrayDst.SRC, extra_args={0,1})
    static void m25(int[] src, int[] dest, int srcPos, int destPos) {
        System.arraycopy(src, srcPos, dest, destPos, 1);
    }

    static boolean m25_check(int[] src, int[] dest) {
        boolean failure = false;
        if (dest[1] != src[0]) {
            System.out.println("Test m10 failed for src[0]=" + src[0] + ", dest[1]=" + dest[1]);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        TestArrayCopyAsLoadsStores test = new TestArrayCopyAsLoadsStores();

        test.doTest("m1");
        test.doTest("m2");
        test.doTest("m3");
        test.doTest("m4");
        test.doTest("m5");
        test.doTest("m6");
        test.doTest("m7");
        test.doTest("m8");
        test.doTest("m9");
        test.doTest("m10");
        test.doTest("m11");
        test.doTest("m12");
        test.doTest("m13");
        test.doTest("m14");
        test.doTest("m15");

        for (int i = 0; i < 20000; i++) {
            helper16(i);
        }

        test.doTest("m16");

        B b = new B();
        for (int i = 0; i < 20000; i++) {
            helper17_1(i);
        }

        test.doTest("m17");

        for (int i = 0; i < 20000; i++) {
            helper18_1(i);
        }
        test.doTest("m18");

        for (int i = 0; i < 20000; i++) {
            helper19(i);
        }

        for (int i = 0; i < 20000; i++) {
            m19(null, 0);
        }

        boolean m19_exception = false;
        for (int i = 0; i < 10; i++) {
            try {
                m19(null, 1);
            } catch(ArrayStoreException ase) {
                m19_exception = true;
            }
        }

        if (!m19_exception) {
            System.out.println("Test m19: exception wasn't thrown");
            test.success = false;
        }

        test.doTest("m19");

        test.doTest("m20");
        test.doTest("m21");

        int[] dst = new int[small_int_src.length];
        for (int i = 0; i < 20000; i++) {
            m22(small_int_src, dst, 0);
        }

        for (int i = 0; i < 10; i++) {
            try {
                m22(small_int_src, dst, 5);
            } catch(ArrayIndexOutOfBoundsException aioobe) {}
        }

        test.doTest("m22");
        test.doTest("m23");

        test.doTest("m24");
        boolean m24_exception = false;
        try {
            m24(small_object_src);
        } catch(ArrayStoreException ase) {
            m24_exception = true;
        }

        if (!m24_exception) {
            System.out.println("Test m24: exception wasn't thrown");
            test.success = false;
        }

        test.doTest("m25");

        if (!test.success) {
            throw new RuntimeException("some tests failed");
        }
    }
}
