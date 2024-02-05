/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary If -Djava.system.class.loader=xxx is specified in command-line, disable archived non-system classes
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/TestClassLoader.java
 * @compile test-classes/ReportMyLoader.java
 * @compile test-classes/TrySwitchMyLoader.java
 * @run driver SpecifySysLoaderProp
 */

import java.io.*;
import jdk.test.lib.process.OutputAnalyzer;

public class SpecifySysLoaderProp {

  public static void main(String[] args) throws Exception {
    JarBuilder.build("sysloader", "TestClassLoader", "ReportMyLoader", "TrySwitchMyLoader");

    String jarFileName = "sysloader.jar";
    String appJar = TestCommon.getTestJar(jarFileName);
    TestCommon.testDump(appJar, TestCommon.list("ReportMyLoader"));
    String warning = "Archived non-system classes are disabled because the java.system.class.loader property is specified";


    TestCommon.run(
        "-verbose:class",
        "-cp", appJar,
        "ReportMyLoader")
      .assertNormalExit("[class,load] ReportMyLoader source: shared objects file",
                        "ReportMyLoader's loader = jdk.internal.loader.ClassLoaders$AppClassLoader@");

    TestCommon.run(
        "-cp", appJar,
        "-Djava.system.class.loader=no.such.Klass",
        "ReportMyLoader")
      .assertAbnormalExit(output -> {
          output.shouldContain(warning);
          output.shouldContain("ClassNotFoundException: no.such.Klass");
        });

    TestCommon.run(
        "-verbose:class",
        "-cp", appJar,
        "-Xlog:cds",
        "-Djava.system.class.loader=TestClassLoader",
        "ReportMyLoader")
      .assertNormalExit("ReportMyLoader's loader = jdk.internal.loader.ClassLoaders$AppClassLoader@", 
             "TestClassLoader.called = true", 
             "TestClassLoader: loadClass(\"ReportMyLoader\",") 
      .assertNormalExit(output -> {
        output.shouldMatch(".class,load. TestClassLoader source: file:");
        output.shouldMatch(".class,load. ReportMyLoader source: file:.*" + jarFileName);
        output.shouldContain("full module graph: disabled due to incompatible property: java.system.class.loader=");
        });

    TestCommon.run(
        "-verbose:class",
        "-cp", appJar,
        "TrySwitchMyLoader")
      .assertNormalExit("[class,load] ReportMyLoader source: shared objects file",
             "TrySwitchMyLoader's loader = jdk.internal.loader.ClassLoaders$AppClassLoader@",
             "ReportMyLoader's loader = jdk.internal.loader.ClassLoaders$AppClassLoader@",
             "TestClassLoader.called = false");
  }
}
