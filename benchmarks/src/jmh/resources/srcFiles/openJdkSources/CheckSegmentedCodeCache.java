/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @test CheckSegmentedCodeCache
 * @bug 8015774
 * @summary Checks VM options related to the segmented code cache
 * @library /test/lib
 * @requires vm.flagless
 *
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.codecache.CheckSegmentedCodeCache
 */

package compiler.codecache;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.whitebox.WhiteBox;

public class CheckSegmentedCodeCache {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final String NON_METHOD = "CodeHeap 'non-nmethods'";
    private static final String PROFILED = "CodeHeap 'profiled nmethods'";
    private static final String NON_PROFILED = "CodeHeap 'non-profiled nmethods'";

    private static void verifySegmentedCodeCache(ProcessBuilder pb, boolean enabled) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);
        if (enabled) {
            try {
                out.shouldContain(NON_METHOD);
            } catch (RuntimeException e) {
                if (Platform.isTieredSupported()) {
                    throw new RuntimeException("No code cache segmentation.");
                }
            }
        } else {
            out.shouldNotContain(NON_METHOD);
        }
    }

    private static void verifyCodeHeapNotExists(ProcessBuilder pb, String... heapNames) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);
        for (String name : heapNames) {
            out.shouldNotContain(name);
        }
    }

    private static void failsWith(ProcessBuilder pb, String message) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldContain(message);
        out.shouldHaveExitValue(1);
    }

    private static void verifyCodeHeapSize(ProcessBuilder pb, String heapName, long heapSize) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);

        long actualHeapSize = Long.parseLong(out.firstMatch(heapName + "\\s+=\\s(\\d+)", 1));
        if (heapSize != actualHeapSize) {
            throw new RuntimeException("Unexpected " + heapName + " size: " + actualHeapSize + " != " + heapSize);
        }


        long nonNMethodCodeHeapSize = Long.parseLong(out.firstMatch("NonNMethodCodeHeapSize\\s+=\\s(\\d+)", 1));
        long nonProfiledCodeHeapSize = Long.parseLong(out.firstMatch("NonProfiledCodeHeapSize\\s+=\\s(\\d+)", 1));
        long profiledCodeHeapSize = Long.parseLong(out.firstMatch(" ProfiledCodeHeapSize\\s+=\\s(\\d+)", 1));
        long reservedCodeCacheSize = Long.parseLong(out.firstMatch("ReservedCodeCacheSize\\s+=\\s(\\d+)", 1));

        if (reservedCodeCacheSize != nonNMethodCodeHeapSize + nonProfiledCodeHeapSize + profiledCodeHeapSize) {
            throw new RuntimeException("Unexpected segments size sum: " + reservedCodeCacheSize + " != " +
                    nonNMethodCodeHeapSize + "+" + nonProfiledCodeHeapSize + "+" + profiledCodeHeapSize);
        }

        if ((reservedCodeCacheSize % 1024 != 0) || (nonNMethodCodeHeapSize % 1024 != 0) ||
            (nonProfiledCodeHeapSize % 1024 != 0) || (profiledCodeHeapSize % 1024 != 0)) {
            throw new RuntimeException("Unexpected segments size alignment: " + reservedCodeCacheSize + ", " +
                    nonNMethodCodeHeapSize + ", " + nonProfiledCodeHeapSize + ", " + profiledCodeHeapSize);
        }
    }

    /**
    * Check the result of segmented code cache related VM options.
    */
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:ReservedCodeCacheSize=239m",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifySegmentedCodeCache(pb, false);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:-TieredCompilation",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifySegmentedCodeCache(pb, false);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+TieredCompilation",
                                                              "-XX:ReservedCodeCacheSize=240m",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifySegmentedCodeCache(pb, true);
        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+TieredCompilation",
                                                              "-XX:ReservedCodeCacheSize=400m",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifySegmentedCodeCache(pb, true);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:-TieredCompilation",
                                                              "-XX:ReservedCodeCacheSize=239m",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifySegmentedCodeCache(pb, true);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-Xint",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifyCodeHeapNotExists(pb, PROFILED, NON_PROFILED);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:TieredStopAtLevel=0",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifyCodeHeapNotExists(pb, PROFILED);
        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:TieredStopAtLevel=1",
                                                              "-XX:+PrintCodeCache",
                                                              "-version");
        verifyCodeHeapNotExists(pb, PROFILED);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:NonNMethodCodeHeapSize=100K",
                                                              "-version");
        failsWith(pb, "Invalid NonNMethodCodeHeapSize");

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:ReservedCodeCacheSize=10M",
                                                              "-XX:NonNMethodCodeHeapSize=5M",
                                                              "-XX:ProfiledCodeHeapSize=5M",
                                                              "-XX:NonProfiledCodeHeapSize=5M",
                                                              "-version");
        failsWith(pb, "Invalid code heap sizes");

        long minUseSpace = WHITE_BOX.getUintxVMFlag("CodeCacheMinimumUseSpace");
        long minSize = (Platform.isDebugBuild() ? 3 : 1) * minUseSpace;
        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:NonNMethodCodeHeapSize=" + minSize,
                                                              "-XX:ReservedCodeCacheSize=" + minSize,
                                                              "-XX:InitialCodeCacheSize=100K",
                                                              "-version");
        failsWith(pb, "Not enough space in non-nmethod code heap to run VM");


        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:ReservedCodeCacheSize=" + minSize,
                                                              "-XX:InitialCodeCacheSize=100K",
                                                              "-version");
        failsWith(pb, "Invalid code heap sizes");


        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:ReservedCodeCacheSize=100M",
                                                              "-XX:NonNMethodCodeHeapSize=10M",
                                                              "-XX:ProfiledCodeHeapSize=10M",
                                                              "-XX:NonProfiledCodeHeapSize=10M",
                                                              "-version");
        failsWith(pb, "Invalid code heap sizes");

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:NonNMethodCodeHeapSize=10M",
                                                              "-XX:ProfiledCodeHeapSize=10M",
                                                              "-XX:NonProfiledCodeHeapSize=10M",
                                                              "-XX:+PrintFlagsFinal",
                                                              "-version");
        verifyCodeHeapSize(pb, "ReservedCodeCacheSize", 31457280);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:ReservedCodeCacheSize=100M",
                                                              "-XX:NonNMethodCodeHeapSize=10M",
                                                              "-XX:+PrintFlagsFinal",
                                                              "-version");
        verifyCodeHeapSize(pb, " ProfiledCodeHeapSize", 47185920);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                              "-XX:ReservedCodeCacheSize=100M",
                                                              "-XX:ProfiledCodeHeapSize=10M",
                                                              "-XX:NonProfiledCodeHeapSize=10M",
                                                              "-XX:+PrintFlagsFinal",
                                                              "-version");
        verifyCodeHeapSize(pb, "NonNMethodCodeHeapSize", 83886080);
    }
}
