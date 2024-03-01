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
 * @bug 8296389
 * @summary Peeling of Irreducible loop can lead to NeverBranch being visited from either side
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:PerMethodTrapLimit=0
 *      -XX:CompileCommand=compileonly,TestPhaseCFGNeverBranchToGotoMain::test
 *      TestPhaseCFGNeverBranchToGotoMain
 */

/*
 * @test
 * @bug 8296389
 * @compile TestPhaseCFGNeverBranchToGoto.jasm
 * @summary Peeling of Irreducible loop can lead to NeverBranch being visited from either side
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:PerMethodTrapLimit=0
 *      -XX:CompileCommand=compileonly,TestPhaseCFGNeverBranchToGoto::test
 *      TestPhaseCFGNeverBranchToGoto
 */


public class TestPhaseCFGNeverBranchToGotoMain {
    public static void main (String[] args) {
        test(false, false);
    }

    public static void test(boolean flag1, boolean flag2) {
        if (flag1) { 
            int a = 77;
            int b = 0;
            do { 
                a--;
                b++;
            } while (a > 0);
            int p = 0;
            for (int i = 0; i < 4; i++) {
                if ((i % 2) == 0) {
                    p = 1;
                }
            }
            int x = 1;
            if (flag2) {
                x = 3;
            } 
            do { 
                do {
                    x *= 2;
                    if (p == 0) { 
                        break;
                    }
            } while (b == 77);
            int y = 5;
                do {
                    y *= 3;
                } while (b == 77);
            } while (true);
        }
    }
}
