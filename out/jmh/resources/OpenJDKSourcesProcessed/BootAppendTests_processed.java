/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary AppCDS tests for testing -Xbootclasspath/a
 * @requires vm.cds & !vm.graal.enabled
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile src/jdk/test/Main.java
 * @compile src/com/sun/tools/javac/MyMain.jasm
 * @compile src/sun/nio/cs/ext/MyClass.java
 * @compile src/sun/nio/cs/ext1/MyClass.java
 * @run driver BootAppendTests
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class BootAppendTests {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");

    private static final String MAIN_CLASS = "jdk.test.Main";
    private static final String APP_MODULE_CLASS = "com/sun/tools/javac/MyMain";     
    private static final String BOOT_APPEND_MODULE_CLASS = "sun/nio/cs/ext/MyClass"; 
    private static final String BOOT_APPEND_CLASS = "sun/nio/cs/ext1/MyClass";
    private static final String[] ARCHIVE_CLASSES =
         {APP_MODULE_CLASS, BOOT_APPEND_MODULE_CLASS, BOOT_APPEND_CLASS};

    private static String appJar;
    private static String bootAppendJar;
    private static String testArchiveName;

    public static void main(String... args) throws Exception {
        dumpArchive();

        System.out.println("TESTCASE: 1: testBootAppendModuleClassWithoutAppCDS");
        testBootAppendModuleClassWithoutAppCDS();

        System.out.println("TESTCASE: 2" );
        testBootAppendModuleClassWithAppCDS();

        System.out.println("TESTCASE: 3" );
        testBootAppendExcludedModuleClassWithoutAppCDS();

        System.out.println("TESTCASE: 4" );
        testBootAppendExcludedModuleClassWithAppCDS();

        System.out.println("TESTCASE: 5" );
        testBootAppendClassWithoutAppCDS();

        System.out.println("TESTCASE: 6" );
        testBootAppendClassWithAppCDS();

        System.out.println("TESTCASE: 7" );
        testBootAppendAppModuleClassWithoutAppCDS();

        System.out.println("TESTCASE: 9" );
        testBootAppendAppModuleClassWithAppCDS();

        System.out.println("TESTCASE: 9" );
        testBootAppendAppExcludeModuleClassWithoutAppCDS();

        System.out.println("TESTCASE: 10" );
        testBootAppendAppExcludeModuleClassAppCDS();
    }

    static void dumpArchive() throws Exception {
        JarBuilder.build("classpathtests", "jdk/test/Main");
        appJar = TestCommon.getTestJar("classpathtests.jar");

        JarBuilder.build("bootAppend",
                         APP_MODULE_CLASS, BOOT_APPEND_MODULE_CLASS, BOOT_APPEND_CLASS);
        bootAppendJar = TestCommon.getTestJar("bootAppend.jar");

        OutputAnalyzer output1  = TestCommon.dump(
            appJar, TestCommon.list(ARCHIVE_CLASSES), "-Xbootclasspath/a:" + bootAppendJar);
        TestCommon.checkDump(output1);

        if (!TestCommon.isUnableToMap(output1) &&
            !CDSTestUtils.DYNAMIC_DUMP 
            ) {
            for (String archiveClass : ARCHIVE_CLASSES) {
                String msg = "Preload Warning: Cannot find " + archiveClass;
                if (archiveClass.equals(BOOT_APPEND_CLASS)) {
                    output1.shouldNotContain(msg);
                } else {
                    output1.shouldContain(msg);
                }
            }
        }

        testArchiveName = TestCommon.getCurrentArchiveName();
    }

    public static void testBootAppendModuleClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar)
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #1", BOOT_APPEND_MODULE_CLASS, "false");

        CDSTestUtils.runWithArchiveAndCheck(opts);
    }

    public static void testBootAppendModuleClassWithAppCDS() throws Exception {
        OutputAnalyzer output = TestCommon.exec(
            appJar,
            "-Xbootclasspath/a:" + bootAppendJar,
            MAIN_CLASS,
            "Test #2", BOOT_APPEND_MODULE_CLASS, "false");
        TestCommon.checkExec(output);
    }


    public static void testBootAppendExcludedModuleClassWithoutAppCDS() throws Exception {
        TestCommon.run(
            "-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar,
            "-Xlog:class+load=info",
            "--limit-modules", "java.base",
            MAIN_CLASS, "Test #3", BOOT_APPEND_MODULE_CLASS, "true", "BOOT")
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch(".class.load. sun.nio.cs.ext.MyClass source:.*bootAppend.jar");
            });
    }

    public static void testBootAppendExcludedModuleClassWithAppCDS() throws Exception {
        TestCommon.run(
            "-cp", appJar, "-Xbootclasspath/a:" + bootAppendJar,
            "-Xlog:class+load=info",
            "--limit-modules", "java.base",
            MAIN_CLASS,
            "Test #4", BOOT_APPEND_MODULE_CLASS, "true", "BOOT")
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch(".class.load. sun.nio.cs.ext.MyClass source:.*bootAppend.jar");
            });
    }


    public static void testBootAppendClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar)
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #5", BOOT_APPEND_CLASS, "true", "BOOT");

        CDSTestUtils.runWithArchiveAndCheck(opts);
    }


    public static void testBootAppendClassWithAppCDS() throws Exception {
        OutputAnalyzer output = TestCommon.exec(
            appJar,
            "-Xbootclasspath/a:" + bootAppendJar,
            "-Xlog:class+load=info",
            MAIN_CLASS,
            "Test #6", BOOT_APPEND_CLASS, "true", "BOOT");
        TestCommon.checkExec(output);
        if (!TestCommon.isUnableToMap(output))
            output.shouldContain("[class,load] sun.nio.cs.ext1.MyClass source: shared objects file");
    }


    public static void testBootAppendAppModuleClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar)
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #7", APP_MODULE_CLASS, "false");

        CDSTestUtils.runWithArchiveAndCheck(opts);
    }


    public static void testBootAppendAppModuleClassWithAppCDS() throws Exception {
        OutputAnalyzer output = TestCommon.exec(
            appJar,
            "-Xbootclasspath/a:" + bootAppendJar,
            MAIN_CLASS,
            "Test #8", APP_MODULE_CLASS, "false");
        TestCommon.checkExec(output);
    }


    public static void testBootAppendAppExcludeModuleClassWithoutAppCDS()
        throws Exception {

        TestCommon.run(
            "-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar,
            "-Xlog:class+load=info",
            "--limit-modules", "java.base",
            MAIN_CLASS, "Test #9", APP_MODULE_CLASS, "true", "BOOT")
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch(".class.load. com.sun.tools.javac.MyMain source:.*bootAppend.jar");
            });
    }

    public static void testBootAppendAppExcludeModuleClassAppCDS() throws Exception {
        TestCommon.run(
            "-cp", appJar, "-Xbootclasspath/a:" + bootAppendJar,
            "-Xlog:class+load=info",
            "--limit-modules", "java.base",
            MAIN_CLASS, "Test #10", APP_MODULE_CLASS, "true", "BOOT")
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch(".class.load. com.sun.tools.javac.MyMain source:.*bootAppend.jar");
            });
    }
}
