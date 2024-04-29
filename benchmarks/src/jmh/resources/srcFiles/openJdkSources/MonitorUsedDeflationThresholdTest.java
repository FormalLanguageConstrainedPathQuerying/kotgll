/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8226416
 * @summary Test the MonitorUsedDeflationThreshold and NoAsyncDeflationProgressMax options.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver MonitorUsedDeflationThresholdTest
 */

public class MonitorUsedDeflationThresholdTest {
    public static final int DELAY_SECS = 10;
    public static int inflate_count = 0;
    public static Object[] monitors;

    public static void do_work(int count) {
        System.out.println("Recursion count=" + count);
        if (count > inflate_count) {
            System.out.println("Exceeded inflate_count=" + inflate_count);

            System.out.println("Delaying for " + DELAY_SECS + " secs.");
            try {
                Thread.sleep(DELAY_SECS * 1000);
            } catch (InterruptedException ie) {
            }
            System.out.println("Done delaying for " + DELAY_SECS + " secs.");
            return;
        }

        synchronized(monitors[count]) {
            try {
                monitors[count].wait(1);  
            } catch (InterruptedException ie) {
            }
            do_work(count + 1);
        }
    }

    public static void usage() {
        System.err.println("Usage: java " +
                           "MonitorUsedDeflationThresholdTest inflate_count");
    }


    private static ProcessBuilder processCommand(String loggingLevel) {
        return ProcessTools.createLimitedTestJavaProcessBuilder(
            "-Xmx100M",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:AvgMonitorsPerThreadEstimate=1",
            "-XX:MonitorUsedDeflationThreshold=10",
            "-Xlog:monitorinflation=" + loggingLevel,
            "MonitorUsedDeflationThresholdTest", "33");
    }

    private static void testProcess1() throws Exception {
        ProcessBuilder pb = processCommand("info");

        OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());
        output_detail.shouldHaveExitValue(0);

        output_detail.shouldMatch("begin deflating: .*");
        System.out.println("Found beginning of a deflation cycle.");

        String too_many = output_detail.firstMatch("Too many deflations without progress; .*", 0);
        if (too_many == null) {
            output_detail.reportDiagnosticSummary();
            throw new RuntimeException("Did not find too_many string in output.\n");
        }
        System.out.println("too_many='" + too_many + "'");

        System.out.println("PASSED.");
    }

    private static void testProcess2() throws Exception {
        ProcessBuilder pb = processCommand("debug");
        OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());
        output_detail.shouldHaveExitValue(0);

        output_detail.shouldMatch(   ".debug..monitorinflation. Checking in_use_list:");
        output_detail.shouldNotMatch(".debug..monitorinflation. .*is_busy");

        output_detail.shouldMatch(".info ..monitorinflation. Checking in_use_list:");
        output_detail.shouldMatch(".info ..monitorinflation. .*is_busy");

        System.out.println("PASSED.");
    }

    private static void testProcess3() throws Exception {
        ProcessBuilder pb = processCommand("trace");
        OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());
        output_detail.shouldHaveExitValue(0);

        output_detail.shouldMatch(".debug..monitorinflation. Checking in_use_list:");
        output_detail.shouldMatch(".trace..monitorinflation. .*is_busy");

        output_detail.shouldMatch(".info ..monitorinflation. Checking in_use_list:");
        output_detail.shouldMatch(".info ..monitorinflation. .*is_busy");

        System.out.println("PASSED.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            testProcess1();
            testProcess2();
            testProcess3();
            return;
        }

        try {
            inflate_count = Integer.decode(args[0]);
        } catch (NumberFormatException nfe) {
            usage();
            throw new RuntimeException("ERROR: '" + args[0] +
                                       "': bad inflate_count.");
        }

        System.out.println("Hello from MonitorUsedDeflationThresholdTest!");
        System.out.println("inflate_count=" + inflate_count);

        monitors = new Object[inflate_count + 1];
        for (int i = 1; i <= inflate_count; i++) {
            monitors[i] = new Object();
        }
        do_work(1);
    }
}
