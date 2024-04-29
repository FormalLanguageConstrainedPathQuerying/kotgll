/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.numa;

/**
 * @test TestG1NUMATouchRegions
 * @summary Ensure the bottom of the given heap regions are properly touched with requested NUMA id.
 * @requires vm.gc.G1
 * @requires os.family == "linux"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+UseG1GC -Xbootclasspath/a:. -XX:+UseNUMA
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   gc.g1.numa.TestG1NUMATouchRegions
 */

import java.util.LinkedList;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.whitebox.WhiteBox;

public class TestG1NUMATouchRegions {
    enum NUMASupportStatus {
        NOT_CHECKED,
        SUPPORT,
        NOT_SUPPORT
    };

    static int G1HeapRegionSize1MB = 1;
    static int G1HeapRegionSize8MB = 8;

    static NUMASupportStatus status = NUMASupportStatus.NOT_CHECKED;

    public static void main(String[] args) throws Exception {
        testMemoryTouch("-XX:-UseLargePages", G1HeapRegionSize1MB);
        testMemoryTouch("-XX:+UseLargePages", G1HeapRegionSize1MB);
        testMemoryTouch("-XX:+UseLargePages", G1HeapRegionSize8MB);
    }

    static NUMASupportStatus checkNUMAIsEnabled(OutputAnalyzer output) {
        boolean supportNUMA = Boolean.parseBoolean(output.firstMatch("\\bUseNUMA\\b.*?=.*?([a-z]+)", 1));
        System.out.println("supportNUMA=" + supportNUMA);
        return supportNUMA ? NUMASupportStatus.SUPPORT : NUMASupportStatus.NOT_SUPPORT;
    }

    static long parseSizeString(String size) {
        long multiplier = 1;

        if (size.endsWith("B")) {
            multiplier = 1;
        } else if (size.endsWith("K")) {
            multiplier = 1024;
        } else if (size.endsWith("M")) {
            multiplier = 1024 * 1024;
        } else if (size.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
        } else {
            throw new IllegalArgumentException("Expected memory string '" + size + "'to end with either of: B, K, M, G");
        }

        long longSize = Long.parseUnsignedLong(size.substring(0, size.length() - 1));

        return longSize * multiplier;
    }

    static long heapPageSize(OutputAnalyzer output) {
        String HeapPageSizePattern = "Heap:  .* page_size=(\\d+[BKMG])";
        String str = output.firstMatch(HeapPageSizePattern, 1);

        if (str == null) {
            output.reportDiagnosticSummary();
            throw new RuntimeException("Match from '" + HeapPageSizePattern + "' got 'null'");
        }

        return parseSizeString(str);
    }

    static void checkCase1Pattern(OutputAnalyzer output, int index, long g1HeapRegionSize, long actualPageSize, int[] memoryNodeIds) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%6d", index));
        sb.append("| .* | ");

        sb.append(memoryNodeIds[index]);

        output.shouldMatch(sb.toString());
    }

    static void checkCase2Pattern(OutputAnalyzer output, int index, long g1HeapRegionSize, long actualPageSize, int[] memoryNodeIds) throws Exception {
        StringBuilder sb = new StringBuilder();

        int lines_to_print = (int)(actualPageSize / g1HeapRegionSize);
        for (int i = 0; i < lines_to_print; i++) {
            sb.append(String.format("%6d", index * lines_to_print + i));
            sb.append("| .* | ");

            sb.append(memoryNodeIds[index]);

            output.shouldMatch(sb.toString());
            sb.setLength(0);
        }
    }

    static void checkNUMALog(OutputAnalyzer output, int regionSizeInMB) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        long g1HeapRegionSize = regionSizeInMB * 1024 * 1024;
        long actualPageSize = heapPageSize(output);
        long defaultPageSize = (long)wb.getVMPageSize();
        int memoryNodeCount = wb.g1ActiveMemoryNodeCount();
        int[] memoryNodeIds = wb.g1MemoryNodeIds();

        System.out.println("node count=" + memoryNodeCount + ", actualPageSize=" + actualPageSize);
        for (int index = 0; index < memoryNodeCount; index++) {
            if (actualPageSize <= defaultPageSize) {
                checkCase1Pattern(output, index, g1HeapRegionSize, actualPageSize, memoryNodeIds);
            } else {
                checkCase2Pattern(output, index, g1HeapRegionSize, actualPageSize, memoryNodeIds);
            }
        }
    }

    static void testMemoryTouch(String largePagesSetting, int regionSizeInMB) throws Exception {
        if (status == NUMASupportStatus.NOT_SUPPORT) {
            System.out.println("NUMA is not supported");
            return;
        }

        OutputAnalyzer output = ProcessTools.executeLimitedTestJava(
                                              "-Xbootclasspath/a:.",
                                              "-Xlog:pagesize,gc+heap+region=trace",
                                              "-XX:+UseG1GC",
                                              "-Xmx128m",
                                              "-Xms128m",
                                              "-XX:+UnlockDiagnosticVMOptions",
                                              "-XX:+WhiteBoxAPI",
                                              "-XX:+PrintFlagsFinal",
                                              "-XX:+UseNUMA",
                                              "-XX:+AlwaysPreTouch",
                                              largePagesSetting,
                                              "-XX:G1HeapRegionSize=" + regionSizeInMB + "m",
                                              GCTest.class.getName());

        if (status == NUMASupportStatus.NOT_CHECKED) {
            status = checkNUMAIsEnabled(output);
        }

        if (status == NUMASupportStatus.SUPPORT) {
            checkNUMALog(output, regionSizeInMB);
        } else {
            System.out.println("NUMA is not supported");
        }
    }

  static class GCTest {
    public static final int M = 1024*1024;
    public static LinkedList<Object> garbageList = new LinkedList<Object>();
    static int[] filler = new int[10 * M];

    public static void genGarbage() {
      for (int i = 0; i < 32*1024; i++) {
        garbageList.add(new int[100]);
      }
      garbageList.clear();
    }

    public static void main(String[] args) {

      int[] large = new int[M];
      Object ref = large;

      System.out.println("Creating garbage");
      for (int i = 0; i < 100; i++) {
        large = new int[6*M];
        genGarbage();
        System.out.println(large);
      }

      System.out.println(ref);
      System.out.println("Done");
    }
  }
}
