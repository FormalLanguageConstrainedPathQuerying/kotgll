/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run driver ExportModule
 * @summary Tests involve exporting a module from the module path to a jar in the -cp.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Asserts;

public class ExportModule {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String TEST_MODULE1 = "com.greetings";
    private static final String TEST_MODULE2 = "org.astro";

    private static final String PKG_NAME = "com.nomodule";

    private static final String MAIN_CLASS = "com.greetings.Main";
    private static final String APP_CLASS = "org.astro.World";

    private static final String UNNAMED_MAIN = "com.nomodule.Main";

    private static Path moduleDir = null;
    private static Path moduleDir2 = null;
    private static Path appJar = null;
    private static Path appJar2 = null;

    public static void buildTestModule() throws Exception {

        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE2),
                                 MODS_DIR.resolve(TEST_MODULE2),
                                 null);

        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        Path jar = moduleDir.resolve(TEST_MODULE2 + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE2).toString();
        JarBuilder.createModularJar(jar.toString(), classes, null);

        moduleDir2 = Files.createTempDirectory(USER_DIR, "mlib2");
        appJar = moduleDir2.resolve(TEST_MODULE1 + ".jar");
        classes = MODS_DIR.resolve(TEST_MODULE1).toString();
        JarBuilder.createModularJar(appJar.toString(), classes, MAIN_CLASS);

        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(PKG_NAME),
                                    MODS_DIR.resolve(PKG_NAME),
                                    "--module-path", MODS_DIR.toString(),
                                    "--add-modules", TEST_MODULE2,
                                    "--add-exports", "org.astro/org.astro=ALL-UNNAMED");
        Asserts.assertTrue(compiled, "test package did not compile");

        appJar2 = moduleDir2.resolve(PKG_NAME + ".jar");
        classes = MODS_DIR.resolve(PKG_NAME).toString();
        JarBuilder.createModularJar(appJar2.toString(), classes, null);
    }

    public static void main(String... args) throws Exception {
        buildTestModule();
        String appClasses[] = {MAIN_CLASS, APP_CLASS};
        OutputAnalyzer output = TestCommon.createArchive(
                                        appJar.toString(), appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "--add-modules", TEST_MODULE2, MAIN_CLASS);
        TestCommon.checkDump(output);

        TestCommon.run("-Xlog:class+load=trace",
                              "-cp", appJar.toString(),
                              "--module-path", moduleDir.toString(),
                              "--add-modules", TEST_MODULE2, MAIN_CLASS)
            .assertNormalExit(
                "[class,load] org.astro.World source: shared objects file",
                "[class,load] com.greetings.Main source: shared objects file");

        String appClasses2[] = {UNNAMED_MAIN, APP_CLASS};
        output = TestCommon.createArchive(
                                        appJar2.toString(), appClasses2,
                                        "--module-path", moduleDir.toString(),
                                        "--add-modules", TEST_MODULE2,
                                        "--add-exports", "org.astro/org.astro=ALL-UNNAMED",
                                        UNNAMED_MAIN);
        TestCommon.checkDump(output);

        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", appJar2.toString(),
                       "--module-path", moduleDir.toString(),
                       "--add-modules", TEST_MODULE2,
                       "--add-exports", "org.astro/org.astro=ALL-UNNAMED",
                       UNNAMED_MAIN)
            .assertNormalExit(
                "[class,load] org.astro.World source: shared objects file",
                "[class,load] com.nomodule.Main source: shared objects file");
    }
}
