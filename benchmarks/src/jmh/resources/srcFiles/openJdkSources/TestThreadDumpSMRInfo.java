/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8167108
 * @summary Checks whether jstack reports a "Threads class SMR info" section.
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+EnableThreadSMRStatistics TestThreadDumpSMRInfo
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class TestThreadDumpSMRInfo {
    final static String JSTACK = JDKToolFinder.getTestJDKTool("jstack");
    final static String PID = "" + ProcessHandle.current().pid();


    final static String HEADER_STR = "Threads class SMR info:";

    static boolean verbose = false;

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            int arg_i = 0;
            if (args[arg_i].equals("-v")) {
                verbose = true;
                arg_i++;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(JSTACK, PID);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (verbose) {
            System.out.println("stdout: " + output.getStdout());
        }

        output.shouldHaveExitValue(0);
        System.out.println("INFO: jstack ran successfully.");

        output.shouldContain(HEADER_STR);
        System.out.println("INFO: Found: '" + HEADER_STR + "' in jstack output.");

        System.out.println("Test PASSED.");
    }
}
