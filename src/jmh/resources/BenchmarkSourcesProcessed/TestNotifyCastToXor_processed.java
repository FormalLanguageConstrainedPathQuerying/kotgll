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

/*
 * @test
 * @bug 8318049
 * @summary Test that xor nodes are properly notified when constraint casts change.
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler.c2.TestNotifyCastToXor::test
                     -XX:+IgnoreUnrecognizedVMOptions -XX:VerifyIterativeGVN=10 compiler.c2.TestNotifyCastToXor
 */

package compiler.c2;

public class TestNotifyCastToXor {
    public static long longField = 0L;

    public static void test() {
        int ind = -15;

        ind %= ind;
        for (int i = 0; i < 40; ++i) {
            int j = 1;

            do {
                ind ^= (int)longField;

                for (int k = 1; k < 1; k++) {
                }
            } while (j++ < 10);
        }
    }

    public static void main(String[] args) {
        test();
    }
}
