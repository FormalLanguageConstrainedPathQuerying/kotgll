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
 *
 */

/*
 * @test
 * @bug 8308892
 * @summary Test that Parse Predicates immediately following other Parse Predicates
            are cleaned up properly.
 * @run main/othervm -Xbatch compiler.predicates.TestWrongPredicateOrder
 */

package compiler.predicates;

public class TestWrongPredicateOrder {
    static boolean flag;
    static int iFld = 0;
    static int iFld2 = 34;
    static int iArr[] = new int[1005];
    static int iArr2[] = new int[2];


    public static void main(String[] strArr) {
        for (int i = 0; i < 10000; i++) {
            flag = !flag;
            test();
        }
    }

    public static void test() {
        int limit = flag ? Integer.MAX_VALUE - 1 : 1000;

        int i = 0;
        while (i < limit) {
            i += 3;
            iArr2[iFld] = 1; 

            if (flag) {
                return;
            }

            iArr2[1] = 5; 

            iArr[i] = 34; 

            if (iFld2 == 5555) {
                i++; 
            }
        }
    }
}

