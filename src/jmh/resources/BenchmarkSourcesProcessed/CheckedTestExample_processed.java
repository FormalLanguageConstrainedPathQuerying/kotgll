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
 */

package ir_framework.examples;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.test.TestVM; 

/*
 * @test
 * @summary Example test to use the new test framework.
 * @library /test/lib /
 * @run driver ir_framework.examples.CheckedTestExample
 */

/**
 * If there is no non-default warm-up specified, the Test Framework will do the following:
 * <ol>
 *     <li><p>Invoke @Test method {@link TestVM#WARMUP_ITERATIONS} many times.</li>
 *     <li><p>By default, after each invocation, the @Check method of the @Test method is invoked. This can be disabled
 *            by using {@link CheckAt#COMPILED}</li>
 *     <li><p>After the warm-up, the @Test method is compiled.</li>
 *     <li><p>Invoke @Test method once again and then always invoke the @Check method once again.</li>
 * </ol>
 * <p>
 *
 * Configurable things for checked tests:
 * <ul>
 *     <li><p>At @Test method:</li>
 *     <ul>
 *         <li><p>@Warmup: Change warm-up iterations of test (defined by default by TestVM.WARMUP_ITERATIONS)</li>
 *         <li><p>@Arguments: If a @Test method specifies arguments, you need to provide arguments by using @Arguments
 *                            such that the framework knows how to call the method. If you need more complex values, use a
 *                            custom run test with @Run.</li>
 *         <li><p>@IR: Arbitrary number of @IR rules.</li>
 *     </ul>
 *     <li><p>At @Check method:</li>
 *     <ul>
 *         <li><p>{@link Check#when}: When should the @Check method be invoked.</li>
 *         <li><p>No @IR annotations.</li>
 *     </ul>
 * </ul>
 *
 * @see Check
 * @see Test
 * @see Arguments
 * @see Warmup
 * @see TestFramework
 */
public class CheckedTestExample {

    public static void main(String[] args) {
        TestFramework.run(); 
    }

    @Test
    @Arguments(Argument.DEFAULT) 
    @Warmup(100) 
    public int test(int x) {
        return 42;
    }

    @Check(test = "test") 
    public void basicCheck() {
    }

    @Test
    public int test2() {
        return 42;
    }

    @Check(test = "test2")
    public void checkWithReturn(int returnValue) {
        if (returnValue != 42) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test3() {
        return 42;
    }

    @Check(test = "test3")
    public void checkWithTestInfo(TestInfo info) {
        if (!info.isWarmUp()) {
        }
    }

    @Test
    public int test4() {
        return 42;
    }

    @Check(test = "test4")
    public void checkWithReturnAndTestInfo(int returnValue, TestInfo info) {
        if (returnValue != 42) {
            throw new RuntimeException("Must match");
        }
        if (!info.isWarmUp()) {
        }
    }

    @Test
    public int test5() {
        return 42;
    }

    @Check(test = "test5", when = CheckAt.COMPILED) 
    public void checkAfterCompiled(TestInfo info) {
        TestFramework.assertCompiled(info.getTest()); 
    }

}
