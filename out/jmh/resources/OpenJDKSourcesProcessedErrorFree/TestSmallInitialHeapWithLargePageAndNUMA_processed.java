/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestSmallInitialHeapWithLargePageAndNUMA
 * @bug 8023905
 * @requires os.family == "linux"
 * @requires vm.gc.Parallel
 * @summary Check large pages and NUMA are working together via the output message.
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @build TestSmallInitialHeapWithLargePageAndNUMA
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.arguments.TestSmallInitialHeapWithLargePageAndNUMA
*/

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.whitebox.WhiteBox;
import jtreg.SkippedException;

public class TestSmallInitialHeapWithLargePageAndNUMA {

  private static final String MSG_EXIT_TOO_SMALL_HEAP = "Failed initializing NUMA with large pages. Too small heap size";
  private static final String MSG_GC_TRIGGERED_BEFORE_INIT = "GC triggered before VM initialization completed.";

  public static void main(String[] args) throws Exception {

    WhiteBox wb = WhiteBox.getWhiteBox();
    long heapAlignment = wb.getHeapAlignment();

    long initHeap = heapAlignment;
    long maxHeap = heapAlignment * 2;

    OutputAnalyzer analyzer = GCArguments.executeLimitedTestJava(
        "-XX:+UseParallelGC",
        "-Xms" + String.valueOf(initHeap),
        "-Xmx" + String.valueOf(maxHeap),
        "-XX:+UseNUMA",
        "-XX:+PrintFlagsFinal",
        "-version");

    if (largePageOrNumaEnabled(analyzer)) {
      checkAnalyzerValues(analyzer, 1, MSG_EXIT_TOO_SMALL_HEAP);
    } else {
      throw new SkippedException("either NUMA or HugeTLB is not supported");
    }
  }

  private static boolean largePageOrNumaEnabled(OutputAnalyzer analyzer) {
    String output = analyzer.getOutput();

    return !output.contains("[Global flags]");
  }

  private static void checkAnalyzerValues(OutputAnalyzer analyzer, int expectedExitValue, String expectedMessage) {
    String output = analyzer.getOutput();

    if (!output.contains(MSG_GC_TRIGGERED_BEFORE_INIT)) {
      analyzer.shouldHaveExitValue(expectedExitValue);
    }
    if (expectedMessage != null) {
      analyzer.shouldContain(expectedMessage);
    }
  }
}
