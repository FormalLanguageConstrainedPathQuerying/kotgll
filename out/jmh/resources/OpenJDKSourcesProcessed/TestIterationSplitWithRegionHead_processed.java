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
 * @requires vm.compiler2.enabled
 * @bug 8279837
 * @summary Tests infinite loop with region head in iteration split.
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *      -XX:CompileCommand=compileonly,compiler.loopopts.TestIterationSplitWithRegionHead::test
 *      -XX:CompileCommand=dontinline,compiler.loopopts.TestIterationSplitWithRegionHead::*
 *      compiler.loopopts.TestIterationSplitWithRegionHead
 */

package compiler.loopopts;

public class TestIterationSplitWithRegionHead {

    static boolean flagFalse = false;

    public static void main(String[] args) {
        test();
    }

    public static void test() {

        while (flagFalse) {
            while (dontInlineFalse()) {
            }
        }
        dontInlineFalse();

        while (flagFalse) {
            while (true) ;  
        }
    }

    public static boolean dontInlineFalse() {
        return false;
    }
}
