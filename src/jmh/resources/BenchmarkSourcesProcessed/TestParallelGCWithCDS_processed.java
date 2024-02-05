/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @test Loading CDS archived heap objects into ParallelGC
 * @bug 8274788
 * @requires vm.cds
 * @requires vm.gc.Parallel
 * @requires vm.gc.G1
 *
 * @comment don't run this test if any -XX::+Use???GC options are specified, since they will
 *          interfere with the test.
 * @requires vm.gc == null
 *
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/Hello.java
 * @run driver TestParallelGCWithCDS
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

public class TestParallelGCWithCDS {
    public final static String HELLO = "Hello World";
    static String helloJar;

    public static void main(String... args) throws Exception {
        helloJar = JarBuilder.build("hello", "Hello");

        test(false, true);
        test(true,  false);
        test(true,  true);

        if (Platform.is64bit()) test(false, true, true);
    }

    final static String G1 = "-XX:+UseG1GC";
    final static String Parallel = "-XX:+UseParallelGC";

    static void test(boolean dumpWithParallel, boolean execWithParallel) throws Exception {
        test(dumpWithParallel, execWithParallel, false);
    }

    static void test(boolean dumpWithParallel, boolean execWithParallel, boolean useSmallRegions) throws Exception {
        String dumpGC = dumpWithParallel ? Parallel : G1;
        String execGC = execWithParallel ? Parallel : G1;
        String small1 = useSmallRegions ? "-Xmx256m" : "-showversion";
        String small2 = useSmallRegions ? "-XX:ObjectAlignmentInBytes=64" : "-showversion";
        OutputAnalyzer out;

        System.out.println("0. Dump with " + dumpGC);
        out = TestCommon.dump(helloJar,
                              new String[] {"Hello"},
                              dumpGC,
                              small1,
                              small2,
                              "-Xlog:cds");
        out.shouldContain("Dumping shared data to file:");
        out.shouldHaveExitValue(0);

        System.out.println("1. Exec with " + execGC);
        out = TestCommon.exec(helloJar,
                              execGC,
                              small1,
                              small2,
                              "-Xlog:cds",
                              "Hello");
        out.shouldContain(HELLO);
        out.shouldHaveExitValue(0);

        int n = 2;
        if (!dumpWithParallel && execWithParallel) {
            String[] sizes = {
                "4m",   
                "2m",   
                "1m"    
            };
            for (String sz : sizes) {
                String xmx = "-Xmx" + sz;
                System.out.println("=======\n" + n + ". Exec with " + execGC + " " + xmx);
                out = TestCommon.exec(helloJar,
                                      execGC,
                                      small1,
                                      small2,
                                      xmx,
                                      "-Xlog:cds",
                                      "Hello");
                if (out.getExitValue() == 0) {
                    out.shouldContain(HELLO);
                } else {
                    String pattern = "((Too small maximum heap)" +
                                       "|(GC triggered before VM initialization completed)" +
                                       "|(java.lang.OutOfMemoryError: Java heap space))";
                    out.shouldMatch(pattern);
                }
                n++;
            }
        }
    }
}
