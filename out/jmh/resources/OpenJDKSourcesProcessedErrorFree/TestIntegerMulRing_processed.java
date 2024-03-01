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
package compiler.c2.irTests.igvn;

import compiler.lib.ir_framework.*;
import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8299546
 * @summary Test that IntegerMulRing works correctly and returns correct (and optimized) types.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.igvn.TestIntegerMulRing
 */
public class TestIntegerMulRing {
    public static int iFld, iFld2, iFld3, iFld4;
    public static long lFld, lFld2, lFld3, lFld4;

    public static void main(String[] args) {
        TestFramework.runWithFlags("-XX:-SplitIfBlocks");
    }

    @Test
    @IR(failOn = {IRNode.STORE_I, IRNode.IF}, counts = {IRNode.STORE_L, "2"})
    public static void testLongPositive() {
        long l = 111111111111111111L;
        if (l * 81 == 1L) {
            iFld = 23;
        }
        if (l * 81 == 8999999999999999991L) {
            lFld = 23;
        }
        if (l * 83 == 1L) {
            iFld2 = 34;
        }
        if (l * 83 == 9222222222222222213L) {
            lFld2 = 23;
        }
    }

    @Test
    @IR(failOn = {IRNode.STORE_I, IRNode.IF}, counts = {IRNode.STORE_L, "2"})
    public static void testLongPositive2() {
        long l = -111111111111111111L;
        if (l * -81 == 1L) {
            iFld = 23;
        }
        if (l * -81 == 8999999999999999991L) {
            lFld = 23;
        }
        if (l * -83 == 1L) {
            iFld2 = 34;
        }
        if (l * -83 == 9222222222222222213L) {
            lFld2 = 23;
        }
    }

    @Test
    @IR(failOn = {IRNode.STORE_I, IRNode.IF}, counts = {IRNode.STORE_L, "2"})
    public static void testLongNegative() {
        long l = -111111111111111111L;
        if (l * 81 == 1L) {
            iFld = 23;
        }
        if (l * 81 == -8999999999999999991L) {
            lFld = 23;
        }
        if (l * 83 == 1L) {
            iFld2 = 34;
        }
        if (l * 83 == -9222222222222222213L) {
            lFld2 = 23;
        }
    }

    @Test
    @IR(failOn = {IRNode.STORE_I, IRNode.IF}, counts = {IRNode.STORE_L, "2"})
    public static void testLongNegative2() {
        long l = 111111111111111111L;
        if (l * -81 == 1L) {
            iFld = 23;
        }
        if (l * -81 == -8999999999999999991L) {
            lFld = 23;
        }
        if (l * -83 == 1L) {
            iFld2 = 34;
        }
        if (l * -83 == -9222222222222222213L) {
            lFld2 = 23;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongMinValueMinus1(boolean flag, boolean flag2) {
        long l = flag ? -1 : Long.MIN_VALUE;
        int x = flag2 ? -1 : 0;

        if (l * x != 2L) { 
            lFld = 23;
        } else {
            lFld = 34; 
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_L, IRNode.STORE_L}, counts = {IRNode.STORE_I, "1"})
    public static void testLongMinValuePlus1(boolean flag, boolean flag2) {
        long l = flag ? -1 : Long.MIN_VALUE;
        int x = flag2 ? 1 : 0;

        if (l * x <= 0L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_L, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testLongMinValueUnderflowOnce(boolean flag, boolean flag2) {
        long l = flag ? Long.MIN_VALUE/2 : Long.MIN_VALUE/2 + 1;
        int x = flag2 ? 4 : 6;

        if (l * x <= 4L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1", IRNode.MUL_L, "1"})
    public static void testLongMinValueUnderflowOnceTwice(boolean flag, boolean flag2) {
        long l = flag ? Long.MIN_VALUE/2 : Long.MIN_VALUE/2 + 1;
        int x = flag2 ? 6 : 8;

        if (l * x <= 4L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_L, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testLongMinValueUnderflowTwice(boolean flag, boolean flag2) {
        long l = flag ? Long.MIN_VALUE/2 : Long.MIN_VALUE/2 + 1;
        int x = flag2 ? 8 : 10;

        if (l * x <= 8L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_L, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testLongMaxValueOverflowOnce(boolean flag, boolean flag2) {
        long l = flag2 ? Long.MAX_VALUE/2 - 1 : Long.MAX_VALUE/2;
        int x = flag ? 4 : 6;

        if (l * x >= -8L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1", IRNode.MUL_L, "1"})
    public static void testLongMaxValueOverflowOnceTwice(boolean flag, boolean flag2) {
        long l = flag2 ? Long.MAX_VALUE/2 - 1 : Long.MAX_VALUE/2;
        int x = flag ? 6 : 8;

        if (l * x >= -8L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_L, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testLongMaxValueOverflowTwice(boolean flag, boolean flag2) {
        long l = flag2 ? Long.MAX_VALUE/2 - 1 : Long.MAX_VALUE/2;
        int x = flag ? 8 : 10;

        if (l * x >= -16L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_L, counts = {IRNode.STORE_L, "1"})
    public static void testLongProductsOverflowOnceAtMin(boolean flag, boolean flag2) {
        long l = flag ? Long.MAX_VALUE/2 + 1 : Long.MAX_VALUE/2 + 2;
        int x = flag2 ? 2 : 3;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_L, counts = {IRNode.STORE_L, "1"})
    public static void testLongProductsOverflowOnceAtMax(boolean flag, boolean flag2) {
        long l = flag ? 88971434439113592L : 88971434439113593L;
        int x = flag2 ? 310 : 311;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_L, counts = {IRNode.STORE_L, "1"})
    public static void testLongProductsUnderflowOnceAtMin(boolean flag, boolean flag2) {
        long l = flag ? Long.MIN_VALUE/3 - 1 : Long.MIN_VALUE/3 - 2;
        int x = flag2 ? 3 : 4;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_L, counts = {IRNode.STORE_L, "1"})
    public static void testLongProductsUnderflowOnceAtMax(boolean flag, boolean flag2) {
        long l = flag ? -6917529027641081856L : -6917529027641081855L;
        int x = flag2 ? 3 : 4;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongProductsDifferentNumberOfOverflow(boolean flag, boolean flag2) {
        long l = flag ? 88971434439113593L : 88971434439113594L;
        int x = flag2 ? 310 : 311;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongProductsDifferentNumberOfUnderflows(boolean flag, boolean flag2) {
        long l = flag ? -6917529027641081856L : -6917529027641081857L;
        int x = flag2 ? 3 : 4;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongNotSameOverflow1(boolean flag, boolean flag2) {
        long l = flag ? 1 : Long.MAX_VALUE;
        int x = flag2 ? -1 : 2;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongNotSameOverflow2(boolean flag, boolean flag2) {
        long l = flag ? 1 : Long.MIN_VALUE;
        int x = flag2 ? -1 : 2;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongNotSameOverflow3(boolean flag, boolean flag2) {
        long l = flag ? -1 : Long.MIN_VALUE;
        long x = flag2 ? Long.MIN_VALUE : -1;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongNotSameOverflow4(boolean flag, boolean flag2) {
        long l = flag ? -1 : Long.MAX_VALUE;
        long x = flag2 ? Long.MAX_VALUE : -1;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_L, "1"})
    public static void testLongNotSameOverflow5(boolean flag, boolean flag2) {
        long l = flag ? Long.MIN_VALUE : Long.MAX_VALUE;
        long x = flag2 ? Long.MAX_VALUE : -1;

        if (l * x != 2L) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @IR(failOn = IRNode.IF, counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1"})
    public static void testIntPositive() {
        int i = 26000000;
        if (i * 81 == 1) {
            iFld = 23;
        }
        if (i * 81 == 2106000000) {
            iFld = 34;
        }

        if (i * 83 == 1) {
            lFld = 23;
        }
        if (i * 83 == -2136967296) {
            lFld = 34;
        }
    }

    @Test
    @IR(failOn = IRNode.IF, counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1"})
    public static void testIntPositive2() {
        int i = -26000000;
        if (i * -81 == 1) {
            iFld = 23;
        }
        if (i * -81 == 2106000000) {
            iFld = 34;
        }

        if (i * -83 == 1) {
            lFld = 23;
        }
        if (i * -83 == -2136967296) {
            lFld = 34;
        }
    }

    @Test
    @IR(failOn = IRNode.IF, counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1"})
    public static void testIntNegative() {
        int i = 26000000;
        if (i * -81 == 1) {
            iFld = 23;
        }
        if (i * -81 == -2106000000) {
            iFld = 34;
        }

        if (i * -83 == 1) {
            lFld = 23;
        }
        if (i * -83 == 2136967296) {
            lFld = 34;
        }
    }

    @Test
    @IR(failOn = IRNode.IF, counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1"})
    public static void testIntNegative2() {
        int i = -26000000;
        if (i * 81 == 1) {
            iFld = 23;
        }
        if (i * 81 == -2106000000) {
            iFld = 34;
        }

        if (i * 83 == 1) {
            lFld = 23;
        }
        if (i * 83 == 2136967296) {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntMinValueMinus1(boolean flag, boolean flag2) {
        int l = flag ? -1 : Integer.MIN_VALUE;
        int x = flag2 ? -1 : 0;

        if (l * x != 2) { 
            lFld = 23;
        } else {
            lFld = 34; 
        }
    }


    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_I, IRNode.STORE_L}, counts = {IRNode.STORE_I, "1"})
    public static void testIntMinValuePlus1(boolean flag, boolean flag2) {
        int l = flag ? -1 : Integer.MIN_VALUE;
        int x = flag2 ? 1 : 0;

        if (l * x <= 0) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_I, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testIntMinValueUnderflowOnce(boolean flag, boolean flag2) {
        int l = flag ? Integer.MIN_VALUE/2 : Integer.MIN_VALUE/2 + 1;
        int x = flag2 ? 4 : 6;

        if (l * x <= 4) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1", IRNode.MUL_I, "1"})
    public static void testIntMinValueUnderflowOnceTwice(boolean flag, boolean flag2) {
        int l = flag ? Integer.MIN_VALUE/2 : Integer.MIN_VALUE/2 + 1;
        int x = flag2 ? 6 : 8;

        if (l * x <= 4) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_I, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testIntMinValueUnderflowTwice(boolean flag, boolean flag2) {
        int l = flag ? Integer.MIN_VALUE/2 : Integer.MIN_VALUE/2 + 1;
        int x = flag2 ? 8 : 10;

        if (l * x <= 8) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_I, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testIntMaxValueOverflowOnce(boolean flag, boolean flag2) {
        int l = flag2 ? Integer.MAX_VALUE/2 - 1 : Integer.MAX_VALUE/2;
        int x = flag ? 4 : 6;

        if (l * x >= -8) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_I, "1", IRNode.STORE_L, "1", IRNode.MUL_I, "1"})
    public static void testIntMaxValueOverflowOnceTwice(boolean flag, boolean flag2) {
        int l = flag2 ? Integer.MAX_VALUE/2 - 1 : Integer.MAX_VALUE/2;
        int x = flag ? 6 : 8;

        if (l * x >= -8) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = {IRNode.MUL_I, IRNode.STORE_L, IRNode.LSHIFT}, counts = {IRNode.STORE_I, "1"})
    public static void testIntMaxValueOverflowTwice(boolean flag, boolean flag2) {
        int l = flag2 ? Integer.MAX_VALUE/2 - 1 : Integer.MAX_VALUE/2;
        int x = flag ? 8 : 10;

        if (l * x >= -16L) {
            iFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_I, counts = {IRNode.STORE_L, "1"})
    public static void testIntProductsOverflowOnceAtMin(boolean flag, boolean flag2) {
        int l = flag ? Integer.MAX_VALUE/2 + 1 : Integer.MAX_VALUE/2 + 2;
        int x = flag2 ? 2 : 3;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_I, counts = {IRNode.STORE_L, "1"})
    public static void testIntProductsOverflowOnceAtMax(boolean flag, boolean flag2) {
        int l = flag ? 63786642 : 63786643;
        int x = flag2 ? 100 : 101;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_I, counts = {IRNode.STORE_L, "1"})
    public static void testIntProductsUnderflowOnceAtMin(boolean flag, boolean flag2) {
        int l = flag ? Integer.MIN_VALUE/3 - 1 : Integer.MIN_VALUE/3 - 2;
        int x = flag2 ? 3 : 4;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(failOn = IRNode.MUL_I, counts = {IRNode.STORE_L, "1"})
    public static void testIntProductsUnderflowOnceAtMax(boolean flag, boolean flag2) {
        int l = flag ? -1610612736 : -1610612735;
        int x = flag2 ? 3 : 4;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntProductsDifferentNumberOfOverflow(boolean flag, boolean flag2) {
        int l = flag ? 63786643 : 63786644;
        int x = flag2 ? 100 : 101;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntProductsDifferentNumberOfUnderflows(boolean flag, boolean flag2) {
        int l = flag ? -1610612736 : -1610612737;
        int x = flag2 ? 3 : 4;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntNotSameOverflow1(boolean flag, boolean flag2) {
        int l = flag ? 1 : Integer.MAX_VALUE;
        int x = flag2 ? -1 : 2;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntNotSameOverflow2(boolean flag, boolean flag2) {
        int l = flag ? 1 : Integer.MIN_VALUE;
        int x = flag2 ? -1 : 2;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntNotSameOverflow3(boolean flag, boolean flag2) {
        int l = flag ? -1 : Integer.MIN_VALUE;
        int x = flag2 ? Integer.MIN_VALUE : -1;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntNotSameOverflow4(boolean flag, boolean flag2) {
        int l = flag ? -1 : Integer.MAX_VALUE;
        int x = flag2 ? Integer.MAX_VALUE : -1;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    @Warmup(0)
    @Arguments({Argument.TRUE, Argument.FALSE})
    @IR(counts = {IRNode.STORE_L, "2", IRNode.MUL_I, "1"})
    public static void testIntNotSameOverflow5(boolean flag, boolean flag2) {
        int l = flag ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int x = flag2 ? Integer.MAX_VALUE : -1;

        if (l * x != 2) {
            lFld = 23;
        } else {
            lFld = 34;
        }
    }

    @Test
    public static void test() {
        iFld = 1073741823 * 2;
        iFld2 = 1073741824 * 2; 
        iFld3 = -1073741824 * 2;
        iFld4 = -1073741825 * 2; 
        lFld = 4611686018427387903L * 2;
        lFld2 = 4611686018427387904L * 2; 
        lFld3 = -4611686018427387904L * 2;
        lFld4 = -4611686018427387905L * 2; 
    }

    @Run(test = "test")
    public static void run() {
        test();
        Asserts.assertEQ(iFld, 2147483646);
        Asserts.assertEQ(iFld2, -2147483648);
        Asserts.assertEQ(iFld3, -2147483648);
        Asserts.assertEQ(iFld4, 2147483646);
        Asserts.assertEQ(lFld, 9223372036854775806L);
        Asserts.assertEQ(lFld2, -9223372036854775808L);
        Asserts.assertEQ(lFld3, -9223372036854775808L);
        Asserts.assertEQ(lFld4, 9223372036854775806L);
    }
}
