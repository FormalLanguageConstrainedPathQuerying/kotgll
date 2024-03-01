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

package compiler.regalloc;

/**
 * @test
 * @bug 8317507
 * @summary Test that C2's PhaseRegAlloc::_node_regs (a post-register-allocation
 *          mapping from machine nodes to assigned registers) does not overflow
 *          in the face of a program with a high-density of CISC spilling
 *          candidate nodes.
 * @run main/othervm -Xcomp -XX:CompileOnly=compiler.regalloc.TestNodeRegArrayOverflow::testWithCompilerUnrolling
                     -XX:CompileCommand=dontinline,compiler.regalloc.TestNodeRegArrayOverflow::dontInline
                     compiler.regalloc.TestNodeRegArrayOverflow compiler
 * @run main/othervm -Xcomp -XX:CompileOnly=compiler.regalloc.TestNodeRegArrayOverflow::testWithManualUnrolling
                     -XX:CompileCommand=dontinline,compiler.regalloc.TestNodeRegArrayOverflow::dontInline
                     compiler.regalloc.TestNodeRegArrayOverflow manual
 */

public class TestNodeRegArrayOverflow {

    static int dontInline() {
        return 0;
    }

    static float testWithCompilerUnrolling(float inc) {
        int i = 0, j = 0;
        float f = dontInline();
        while (i++ < 128) {
            for (j = 0; j < 16; j++) {
                f += inc;
            }
        }
        return f;
    }

    static float testWithManualUnrolling(float inc) {
        int i = 0, j = 0;
        float f = dontInline();
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        f += inc;
        return f;
    }

    public static void main(String[] args) {
        switch (args[0]) {
        case "compiler":
            testWithCompilerUnrolling(0);
            break;
        case "manual":
            testWithManualUnrolling(0);
            break;
        default:
            throw new IllegalArgumentException("Invalid mode: " + args[0]);
        }
    }
}
