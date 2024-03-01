/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/timeout=480 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. MainModuleOnly
 * @summary Test some scenarios with a main modular jar specified in the --module-path and -cp options in the command line.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSTestUtils.Result;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

import jtreg.SkippedException;
import jdk.test.whitebox.code.Compiler;

public class MainModuleOnly {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String TEST_MODULE1 = "com.simple";

    private static final String MAIN_CLASS = "com.simple.Main";

    private static Path moduleDir = null;
    private static Path moduleDir2 = null;
    private static Path destJar = null;

    private static final String jarFileError = "This file is not the one used while building the shared archive file:";

    public static void buildTestModule() throws Exception {

        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.toString());


        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        moduleDir2 = Files.createTempDirectory(USER_DIR, "mlib2");

        Path srcJar = moduleDir.resolve(TEST_MODULE1 + ".jar");
        destJar = moduleDir2.resolve(TEST_MODULE1 + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE1).toString();
        JarBuilder.createModularJar(srcJar.toString(), classes, MAIN_CLASS);
        Files.copy(srcJar, destJar);

    }

    public static void main(String... args) throws Exception {
        buildTestModule();
        String appClasses[] = {MAIN_CLASS};
        OutputAnalyzer output = TestCommon.createArchive(
                                        destJar.toString(), appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "-m", TEST_MODULE1);
        TestCommon.checkDump(output);

        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", destJar.toString(),
                       "--module-path", moduleDir.toString(),
                       "-m", TEST_MODULE1)
            .assertNormalExit("[class,load] com.simple.Main source: shared objects file");

        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", destJar.toString(),
                       "--module-path", moduleDir.toString(),
                       MAIN_CLASS, "-m", TEST_MODULE1)
            .assertNormalExit(out ->
                out.shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar"));

        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", destJar.toString(),
                       "--module-path", MODS_DIR.toString(),
                       "-m", TEST_MODULE1 + "/" + MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldMatch(".class.load. com.simple.Main source:.*com.simple")
                   .shouldContain(MODS_DIR.toString());
            });

        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", destJar.toString(),
                       "--upgrade-module-path", moduleDir.toString(),
                       "--module-path", moduleDir.toString(),
                       "-m", TEST_MODULE1)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch("CDS is disabled when the.*option is specified")
                   .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });

        boolean skippedTest = false;
        if (!Compiler.isGraalEnabled()) {
            TestCommon.run("-Xlog:class+load=trace",
                           "-cp", destJar.toString(),
                           "--limit-modules", "java.base," + TEST_MODULE1,
                           "--module-path", moduleDir.toString(),
                           "-m", TEST_MODULE1)
                .assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch("CDS is disabled when the.*option is specified")
                       .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });
        } else {
            skippedTest = true;
        }
        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", destJar.toString(),
                       "--patch-module", TEST_MODULE1 + "=" + MODS_DIR.toString(),
                       "--module-path", moduleDir.toString(),
                       "-m", TEST_MODULE1)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch("CDS is disabled when the.*option is specified")
                   .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });
        (new File(destJar.toString())).setLastModified(System.currentTimeMillis() + 2000);
        Result res = TestCommon.run("-cp", destJar.toString(),
                       "-Xlog:cds",
                       "--module-path", moduleDir.toString(),
                       "-m", TEST_MODULE1);
        res.assertAbnormalExit(jarFileError);
        String mainModule = TEST_MODULE1;
        if (TestCommon.isDynamicArchive()) {
            mainModule += "/" + MAIN_CLASS;
        }
        output = TestCommon.createArchive(destJar.toString(), appClasses,
                                          "--module-path", MODS_DIR.toString(),
                                          "-m", mainModule);
        output.shouldHaveExitValue(1)
              .shouldMatch("Error: non-empty directory.*com.simple");

        if (Platform.isWindows()) {
            System.out.println("Long module path test cannot be tested on the Windows platform.");
            return;
        }
        Path longDir = USER_DIR;
        int pathLen = longDir.toString().length();
        int PATH_LEN = 2034;
        int MAX_DIR_LEN = 250;
        while (pathLen < PATH_LEN) {
            int remaining = PATH_LEN - pathLen;
            int subPathLen = remaining > MAX_DIR_LEN ? MAX_DIR_LEN : remaining;
            char[] chars = new char[subPathLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            longDir = Paths.get(longDir.toString(), subPath);
            pathLen = longDir.toString().length();
        }
        File longDirFile = new File(longDir.toString());
        try {
            longDirFile.mkdirs();
        } catch (Exception e) {
            throw e;
        }
        Path longDirJar = longDir.resolve(TEST_MODULE1 + ".jar");
        try {
            Files.copy(destJar, longDirJar);
        } catch (java.io.IOException ioe) {
            System.out.println("Caught IOException from Files.copy(). Cannot continue.");
            return;
        }
        output = TestCommon.createArchive(destJar.toString(), appClasses,
                                          "-Xlog:exceptions=trace",
                                          "--module-path", longDirJar.toString(),
                                          "-m", TEST_MODULE1);
        if (output.getExitValue() != 0) {
            output.shouldMatch("os::stat error.*CDS dump aborted");
        }

        if (skippedTest) {
            throw new SkippedException("Skipped --limit-modules test; it can't be run with Graal enabled");
        }
    }
}
