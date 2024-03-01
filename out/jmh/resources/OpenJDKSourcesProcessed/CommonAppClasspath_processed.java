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
 *
 */

/*
 * @test
 * @bug 8279366
 * @summary Test app class paths checking with the longest common path taken into account.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloMore.java
 * @run driver CommonAppClasspath
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.cds.CDSTestUtils;

public class CommonAppClasspath {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());
    private static final String failedMessage = "APP classpath mismatch";
    private static final String successMessage1 = "Hello source: shared objects file";
    private static final String successMessage2 = "HelloMore source: shared objects file";

    private static void runtimeTest(String classPath, String mainClass, int expectedExitValue,
                                    String ... checkMessages) throws Exception {
        CDSTestUtils.Result result = TestCommon.run(
            "-Xshare:on",
            "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
            "-cp", classPath,
            "-Xlog:class+load=trace,class+path=info",
            mainClass);
        if (expectedExitValue == 0) {
            result.assertNormalExit( output -> {
                for (String s : checkMessages) {
                    output.shouldContain(s);
                }
            });
        } else {
            result.assertAbnormalExit( output -> {
                for (String s : checkMessages) {
                    output.shouldContain(s);
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appJar2 = JarBuilder.build("AppendClasspath_HelloMore", "HelloMore");
        String jars = appJar;
        TestCommon.testDump(jars, TestCommon.list("Hello"));

        Path destPath = CDSTestUtils.copyFile(appJar, System.getProperty("java.io.tmpdir"));

        runtimeTest(destPath.toString(), "Hello", 0, successMessage1);

        jars = appJar + File.pathSeparator + appJar2;
        TestCommon.testDump(jars, TestCommon.list("Hello", "HelloMore"));

        String newDir = USER_DIR.toString() + File.separator + "deploy";
        destPath = CDSTestUtils.copyFile(appJar, newDir);

        Path destPath2 = CDSTestUtils.copyFile(appJar2, newDir);

        runtimeTest(destPath.toString() + File.pathSeparator + destPath2.toString(),
                    "HelloMore", 0, successMessage1, successMessage2);

        runtimeTest(appJar + File.pathSeparator + destPath2.toString(),
                    "HelloMore", 1, failedMessage);

        runtimeTest(destPath.toString() + File.pathSeparator + appJar2,
                    "HelloMore", 1, failedMessage);

        jars = destPath.toString() + File.pathSeparator + appJar2;
        TestCommon.testDump(jars, TestCommon.list("Hello", "HelloMore"));

        runtimeTest(destPath.toString() + File.pathSeparator + appJar2,
                    "HelloMore", 0, successMessage1, successMessage2);

        runtimeTest(destPath.toString() + File.pathSeparator + destPath2.toString(),
                    "HelloMore", 1, failedMessage);

        destPath = CDSTestUtils.copyFile(appJar, USER_DIR.toString() + File.separator + "a");

        destPath2 = CDSTestUtils.copyFile(appJar2, USER_DIR.toString() + File.separator + "aa");

        jars = destPath.toString() + File.pathSeparator + destPath2.toString();
        TestCommon.testDump(jars, TestCommon.list("Hello", "HelloMore"));

        Path runPath = CDSTestUtils.copyFile(appJar, USER_DIR.toString() + File.separator + "x" + File.separator + "a");

        Path runPath2= CDSTestUtils.copyFile(appJar2, USER_DIR.toString() + File.separator + "x" + File.separator + "aa");

        runtimeTest(runPath.toString() + File.pathSeparator + runPath2.toString(),
                    "HelloMore", 0, successMessage1, successMessage2);

        runPath2= CDSTestUtils.copyFile(appJar2, USER_DIR.toString() + File.separator + "x" + File.separator + "a");

        runtimeTest(runPath.toString() + File.pathSeparator + runPath2.toString(),
                    "HelloMore", 1, failedMessage);
    }
}
