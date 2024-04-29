/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 * @bug 8275610
 * @summary Null check for field access of object floats above null check resulting in a segfault.
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.TestEliminateNullCheckWithSplitIf::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:StressSeed=42 compiler.loopopts.TestEliminateNullCheckWithSplitIf
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.TestEliminateNullCheckWithSplitIf::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressIGVN compiler.loopopts.TestEliminateNullCheckWithSplitIf
 */

package compiler.loopopts;

public class TestEliminateNullCheckWithSplitIf {
    public static int[] iArrFld = new int[20];
    public static int[] iArrFld2 = new int[20];
    public static int iFld = 10;
    public static MyClass obj;

    public static void main(String[] strArr) {
        for (int i = 0; i < 10000; i++) {
            obj = (i % 100 == 0 ? null : new MyClass());
            test();
        }
    }

    public static void test() {
        int x = iArrFld[17]; 
        if (obj != null) { 
            int y = 0;
            for (int i = 0; i < 1; i++) { 
                y++;
            }
            x = iArrFld[y]; 
        } else {
            x = iArrFld2[18];
        }
        if (obj != null) { 
            x = iArrFld2[obj.iFld]; 
        }
    }
}

class MyClass {
    int iFld;
}




