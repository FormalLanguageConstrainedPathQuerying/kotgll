/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package compiler.c2.irTests;

import compiler.lib.ir_framework.*;

/*
 * @test
 * @bug 8267532
 * @summary check that uncommon trap is generated for unhandled catch block
 * @library /test/lib /
 * @requires vm.opt.DeoptimizeALot != true
 * @run driver compiler.c2.irTests.TestPrunedExHandler
 */

public class TestPrunedExHandler {
    public static void main(String[] args) {
        TestFramework.runWithFlags(
            "-XX:+TieredCompilation", 
            "-XX:CompileCommand=inline,compiler.c2.irTests.TestPrunedExHandler::inlinee",
            "-XX:CompileCommand=dontinline,compiler.c2.irTests.TestPrunedExHandler::outOfLine");
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "1"})
    public static void testTrap() {
        try {
            outOfLine(false);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private static void outOfLine(boolean shouldThrow) {
        if (shouldThrow) {
            throw new IllegalStateException();
        }
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "0"})
    public static void testNoTrap(boolean shouldThrow) {
        try {
            outOfLine(shouldThrow);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Run(test = "testNoTrap", mode = RunMode.STANDALONE)
    public static void runNoTrap(RunInfo info) {
        for (int i = 0; i < 2_000; i++) { 
            testNoTrap(false);
        }

        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_FULL_PROFILE);

        testNoTrap(true); 

        for (int i = 0; i < 20_000; i++) { 
            testNoTrap(false); 
        }
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "0"})
    public static void testNoTrapAfterDeopt(boolean shouldThrow) {
        try {
            outOfLine(shouldThrow);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Run(test = "testNoTrapAfterDeopt", mode = RunMode.STANDALONE)
    public static void runNoTrapAfterDeopt(RunInfo info) {
        for (int i = 0; i < 20_000; i++) { 
            testNoTrapAfterDeopt(false);
        }

        TestFramework.assertCompiledByC2(info.getTest());

        testNoTrapAfterDeopt(true); 

        TestFramework.assertDeoptimizedByC2(info.getTest());

        for (int i = 0; i < 20_000; i++) { 
            testNoTrapAfterDeopt(false); 
        }
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "0"})
    public static void testNoTrapAfterDeoptInlined(boolean shouldThrow) {
        try {
            inlinee(shouldThrow);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private static void inlinee(boolean shouldThrow) {
        outOfLine(shouldThrow);
    }

    @Run(test = "testNoTrapAfterDeoptInlined", mode = RunMode.STANDALONE)
    public static void runNoTrapAfterDeoptInlined(RunInfo info) {
        for (int i = 0; i < 20_000; i++) { 
            testNoTrapAfterDeoptInlined(false);
        }

        TestFramework.assertCompiledByC2(info.getTest());

        testNoTrapAfterDeoptInlined(true); 

        TestFramework.assertDeoptimizedByC2(info.getTest());

        for (int i = 0; i < 20_000; i++) { 
            testNoTrapAfterDeoptInlined(false); 
        }
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "1"})
    public static void testThrowBeforeProfiling(boolean shouldThrow) {
        try {
            outOfLine(shouldThrow);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Run(test = "testThrowBeforeProfiling", mode = RunMode.STANDALONE)
    public static void runThrowBeforeProfiling(RunInfo info) {
        testThrowBeforeProfiling(true);

        for (int i = 0; i < 20_000; i++) { 
            testThrowBeforeProfiling(false);
        }
    }

    @Test
    @IR(counts = {IRNode.UNREACHED_TRAP, "0"})
    public static void testInterpreterProfiling(boolean takeBranch, boolean shouldThrow) {
        if (takeBranch) {
            System.out.println("testInterpreterProfiling: branch taken");
        }

        try {
            outOfLine(shouldThrow);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Run(test = "testInterpreterProfiling", mode = RunMode.STANDALONE)
    public static void runInterpreterProfiling(RunInfo info) {
        for (int i = 0; i < 20_000; i++) { 
            testInterpreterProfiling(false, false);
        }
        TestFramework.assertCompiledByC2(info.getTest());

        testInterpreterProfiling(true, false); 
        TestFramework.assertDeoptimizedByC2(info.getTest());

        testInterpreterProfiling(false, false);
        testInterpreterProfiling(false, true);

        for (int i = 0; i < 20_000; i++) { 
            testInterpreterProfiling(false, false);
        }
    }

}
