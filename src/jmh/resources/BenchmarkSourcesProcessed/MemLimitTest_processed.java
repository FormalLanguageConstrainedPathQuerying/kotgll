/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright Red Hat, Inc. All Rights Reserved.
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
 * @bug 8319301
 * @summary Tests various ways to call memlimit
 * @library /test/lib /
 *
 * @run driver compiler.compilercontrol.commands.MemLimitTest
 */

package compiler.compilercontrol.commands;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class MemLimitTest {

    static void do_test(String option, boolean expectSuccess, int expectedValue) throws Exception {
        OutputAnalyzer output = ProcessTools.executeTestJava("-Xmx64m", "-XX:CompileCommand=" + option, "-version");
        if (expectSuccess) {
            output.shouldHaveExitValue(0);
            output.shouldNotContain("error occurred");
            output.shouldContain("CompileCommand: MemLimit *.* intx MemLimit = " + expectedValue);
        } else {
            output.shouldNotHaveExitValue(0);
            output.shouldNotMatch("# A fatal error.*");
            output.shouldContain("CompileCommand: An error occurred during parsing");
            output.shouldNotContain("CompileCommand: MemStat"); 
        }
    }

    public static void main(String[] args) throws Exception {


        do_test("MemLimit,*.*", false, 0);

        do_test("MemLimit,*.*,hallo", false, 0);

        do_test("MemLimit,*.*,444m~hallo", false, 0);


        do_test("MemLimit,*.*,444m~stop", true, 465567744);

        do_test("MemLimit,*.*,444m~crash", true, -465567744);

        do_test("MemLimit,*.*,444m", true, 465567744);

    }
}
