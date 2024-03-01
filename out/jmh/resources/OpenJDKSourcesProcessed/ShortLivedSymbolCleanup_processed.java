/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8195100
 * @summary a short lived Symbol should be cleaned up
 * @requires vm.debug
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver ShortLivedSymbolCleanup
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.Scanner;

public class ShortLivedSymbolCleanup {

  static int getSymbolTableSize(ProcessBuilder pb) throws Exception {
    int size = 0;

    OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
    String output = analyzer.getStdout();
    analyzer.shouldHaveExitValue(0);

    String[] lines = output.split("\\R");
    for (String line : lines) {
      if (line.contains("Start size")) {
        Scanner scanner = new Scanner(line);
        scanner.next(); 
        scanner.next(); 
        scanner.next(); 
        size = Integer.parseInt(scanner.next()); 
        scanner.close();
      }
    }

    return size;
  }

  static void analyzeOutputOn(int size, ProcessBuilder pb) throws Exception {
    OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
    String output = analyzer.getStdout();
    analyzer.shouldHaveExitValue(0);

    String[] lines = output.split("\\R");
    for (String line : lines) {
      if (line.startsWith("  Total removed")) {
        Scanner scanner = new Scanner(line);
        scanner.next(); 
        scanner.next(); 
        int removed = Integer.parseInt(scanner.next()); 
        scanner.close();

        if (removed < (size/2)) {
          System.out.println(output);
          throw new RuntimeException("Did not clean dead temporary Symbols [removed:"+removed+", size:"+size+"]");
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder("-Xlog:symboltable=trace",
                                                                         "-version");
    int size = getSymbolTableSize(pb);

    pb = ProcessTools.createLimitedTestJavaProcessBuilder("-XX:+PrintSymbolTableSizeHistogram",
                                                          LotsOfTempSymbols.class.getName(),
                                                          Integer.toString(size));
    analyzeOutputOn(size, pb);
  }

  static class LotsOfTempSymbols {
    public static void main(String [] args) {
      int size = 2*Integer.parseInt(args[0]);
      for (int i=0; i<size; i++) {
        try {
          Class.forName(String.format("%05d", i), false, null);
        } catch (java.lang.ClassNotFoundException e) {}
      }
    }
  }
}
