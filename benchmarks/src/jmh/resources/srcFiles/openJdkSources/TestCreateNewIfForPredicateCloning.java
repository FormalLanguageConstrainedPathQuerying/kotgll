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

/**
 * @test
 * @bug 8290850
 * @summary Test cloning of pinned phi input nodes in create_new_if_for_predicate().
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.loopopts.TestCreateNewIfForPredicateCloning::*
 *                   compiler.loopopts.TestCreateNewIfForPredicateCloning
 */

package compiler.loopopts;

public class TestCreateNewIfForPredicateCloning {
    static int iFld, iFld2, iFld3, nonZero = 2, nonZero2 = 3;
    static boolean bFld = true, bFld2 = false;
    static int[] iArrFld = new int[100];

    public static void main(String[] args) {
        try {
            testUnswitching();
            testLoopPredicatation();
            testLoopPredicatationComplex();
            testUnswitchingWithPredicates();
            testUnswitchingWithPredicatesDiv();
            testFuzzer1();
            testFuzzer2();
            testFuzzer3();
        } catch (Exception e) {
        }
    }

    static void testUnswitching() {
        int x = 3;

        int limit = 2;
        int constantAfterCCP = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            constantAfterCCP = 6; 
        }

        for (int i = 51; i > 9; i -= 3) {
            if (bFld) {
                x *= 6;
            }
            x -= 5;

            for (int j = 1; j < 10; j++) {
                if (bFld) { 
                    continue;
                }
                x = 34; 
                int y = 34;
                if (constantAfterCCP == 2) {
                    y = 35;
                }
                if (y == iFld) { 
                    continue;
                }
                iFld3 = 34; 

            }
        }

        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 3;
        }
    }

    static void testLoopPredicatation() {
        int x = 3;

        int limit = 2;
        int constantAfterCCP = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            constantAfterCCP = 6; 
        }

        for (int i = 51; i > 9; i -= 3) {
            if (bFld) {
                x *= 6;
            }
            x -= 5;

            for (int j = 1; j < 10; j++) {
                if (bFld) { 
                    continue;
                }
                x = 34; 
                int y = iArrFld[j]; 
            }
        }

        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 3;
        }
    }

    static void testLoopPredicatationComplex() {
        int x = 3;

        int limit = 2;
        int constantAfterCCP = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            constantAfterCCP = 6; 
        }

        for (int i = 51; i > 9; i -= 3) {
            if (bFld) {
                x *= 6;
            }
            x -= 5;

            double d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));
            d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));
            d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));
            d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));
            d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));
            d1 = 5 + (double) x;
            x = (int)((d1 + iFld2) - (d1 + iFld));

            for (int j = 1; j < 10; j++) {
                if (bFld) { 
                    continue;
                }
                x = 34; 
                int y = iArrFld[j]; 
            }
        }

        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 3;
        }
    }

    static void testUnswitchingWithPredicates() {
        int x = 3;
        if (iArrFld == null) {
            return;
        }
        int limit = 2;
        int constantAfterCCP = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            constantAfterCCP = 6; 
        }

        for (int i = 51; i > 9; i -= 3) {
            if (bFld) {
                x *= 6;
            }
            x -= 5;

            for (int j = 1; j < 10; j++) {
                if (bFld) { 
                    continue;
                }
                x = 34; 
                int z = iArrFld[j]; 
                int y = 34;
                if (constantAfterCCP == 2) {
                    y = 35;
                }
                if (y == iFld) { 
                    continue;
                }
                iFld3 = 34; 
            }
        }

        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 3;
        }
    }

    static void testUnswitchingWithPredicatesDiv() {
        int x = 3;
        if (iArrFld == null) {
            return;
        }
        int limit = 2;
        int constantAfterCCP = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            constantAfterCCP = 6; 
        }

        for (int i = 51; i > 9; i -= 3) {
            if (bFld) {
                x *= 6;
            }
            x -= 5;

            double d = 5.5f + (double) x;
            int a = (int)d;
            x = (a / nonZero) - (a / nonZero2);


            for (int j = 1; j < 10; j++) {
                if (bFld) { 
                    continue;
                }
                x = 34; 
                int z = iArrFld[j]; 
                int y = 34;
                if (constantAfterCCP == 2) {
                    y = 35;
                }
                if (y == iFld) { 
                    continue;
                }
                iFld3 = 34; 
            }
        }

        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 3;
        }
    }

    static void testFuzzer1() {
        int x = 0;
        int[] iArr = new int[400];
        boolean b = true;
        long[] lArr = new long[400];
        for (long l1 : lArr) {
            for (int i = 63; i > 1; i -= 3) {
                for (int j = 1; j < 4; j++) {
                    if (!b) {
                        x -= 5;
                    }
                }
                for (int j = 1; j < 4; j++) {
                    if (!b) {
                        x = iArr[j];
                    }
                    if (i == 0) {
                        l1 += 5;
                    }
                }
            }
        }
    }

    static void testFuzzer2() {
        int i, i1, i17 = 6, i18;
        short s1;
        boolean b2 = true;
        float f3;
        long lArr[][] = new long[400][];
        byte byArrFld[] = new byte[4];
        i = 1;
        do {
            for (i1 = 14; 6 < i1; i1--)
                ;
            i17 -= i18 = 1;
            while (i18 < 4) {
                i18 <<= i17 = 2;
                switch (i1) {
                    case 114:
                        s1 = byArrFld[1];
                        break;
                    case 116:
                        lArr[1][i18] = iFld;
                        if (b2)
                            continue;
                    case 118:
                        f3 = iFld;
                }
            }
            i++;
        } while (i < 10000);
    }

    static void testFuzzer3() {
        int x = 8;
        int y = 4;
        for (int i : iArrFld) {
            x += 2;
            if (bFld) {
                x = 3;
            } else {
                y = 2;
            }
            for (int j = 0; j < 10; j++) {
                x = 0;
                y += 5;
                if (!bFld) {
                    iArrFld[1] = 5;
                }
            }
        }
    }
}

