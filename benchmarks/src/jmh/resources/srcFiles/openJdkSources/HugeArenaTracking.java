/*
 * Copyright (c) 2019, 2023, Red Hat, Inc. All rights reserved.
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
 * @key randomness
 * @library /test/lib
 * @requires vm.bits == 64
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=summary HugeArenaTracking
 */

import java.util.Random;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;
import jdk.test.whitebox.WhiteBox;

public class HugeArenaTracking {
  private static final long MB = 1024 * 1024;
  private static final long GB = MB * 1024;

  public static void main(String args[]) throws Exception {
    final WhiteBox wb = WhiteBox.getWhiteBox();

    long arena1 = wb.NMTNewArena(1024);
    long arena2 = wb.NMTNewArena(1024);

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=K" },
            new String[] { "Test (reserved=2KB, committed=2KB)",
                           "(arena=2KB #2) (at peak)" });

    Random rand = Utils.getRandomInstance();

    long total = 0;
    while (total < 2 * GB) {
      wb.NMTArenaMalloc(arena1, MB);
      total += MB;
    }

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=G" },
            new String[] { "Test (reserved=2GB, committed=2GB)",
                           "(arena=2GB #2) (at peak)" });

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=M" },
            new String[] { "Test (reserved=2048MB, committed=2048MB)",
                           "(arena=2048MB #2) (at peak)" });

    wb.NMTFreeArena(arena1);

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=G" },
            new String[] { "Test (reserved=0GB, committed=0GB)",
                           "(arena=0GB #1) (peak=2GB #2)" });

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=M" },
            new String[] { "Test (reserved=0MB, committed=0MB)",
                           "(arena=0MB #1) (peak=2048MB #2)" });

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=K" },
            new String[] { "Test (reserved=1KB, committed=1KB)",
                           "(arena=1KB #1) (peak=" });

    wb.NMTFreeArena(arena2);

    NMTTestUtils.runJcmdSummaryReportAndCheckOutput(
            new String[] { "scale=G" },
            new String[] { "Test (reserved=0GB, committed=0GB)",
                           "(arena=0GB #0) (peak=2GB #2)" });
  }
}
