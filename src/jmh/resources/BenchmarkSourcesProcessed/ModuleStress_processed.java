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

/*
 * @test
 * @bug 8159262
 * @summary Test differing scenarios where a module's readability list and a package's exportability list should be walked
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile ../AccessCheck/ModuleLibrary.java
 * @compile ModuleSameCLMain.java
 * @compile ModuleNonBuiltinCLMain.java
 * @compile CustomSystemClassLoader.java
 * @run driver ModuleStress
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ModuleStress {

    public static void main(String[] args) throws Exception {

        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
             "-Xbootclasspath/a:.",
             "-Xlog:module=trace",
             "-version");

        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldNotContain("must be walked")
          .shouldNotContain("being walked")
          .shouldHaveExitValue(0);

        String source1 = "package p1;"   +
                         "import p2.c2;" +
                         "public class c1 {" +
                         "    public c1() {" +
                         "       p2.c2 c2_obj = new p2.c2();" +
                         "       c2_obj.method2();" +
                         "   }" +
                         "}";

        String source2 = "package p2;" +
                         "public class c2 {" +
                         "    public void method2() { }" +
                         "}";

        ClassFileInstaller.writeClassToDisk("p2/c2",
             InMemoryJavaCompiler.compile("p2.c2", source2),  System.getProperty("test.classes"));

        ClassFileInstaller.writeClassToDisk("p1/c1",
             InMemoryJavaCompiler.compile("p1.c1", source1), System.getProperty("test.classes"));

        pb = ProcessTools.createLimitedTestJavaProcessBuilder(
             "-Xbootclasspath/a:.",
             "-Xlog:module=trace",
             "ModuleSameCLMain");

        oa = new OutputAnalyzer(pb.start());
        oa.shouldNotContain("must be walked")
          .shouldNotContain("being walked")
          .shouldHaveExitValue(0);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder(
             "-Xbootclasspath/a:.",
             "-Xlog:module=trace",
             "ModuleNonBuiltinCLMain");

        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("module m1x reads list must be walked")
          .shouldContain("package p2 defined in module m2x, exports list must be walked")
          .shouldNotContain("module m2x reads list must be walked")
          .shouldHaveExitValue(0);

        pb = ProcessTools.createLimitedTestJavaProcessBuilder(
             "-Djava.system.class.loader=CustomSystemClassLoader",
             "-Xbootclasspath/a:.",
             "-Xlog:module=trace",
             "ModuleNonBuiltinCLMain");

        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("package p2 defined in module m2x, exports list must be walked")
          .shouldNotContain("module m2x reads list must be walked")
          .shouldHaveExitValue(0);

    }
}
