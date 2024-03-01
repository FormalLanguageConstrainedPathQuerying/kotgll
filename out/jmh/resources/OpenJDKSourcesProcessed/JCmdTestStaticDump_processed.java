/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8259070
 * @summary Test jcmd to dump static shared archive.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @modules jdk.jcmd/sun.tools.common:+open
 * @build jdk.test.lib.apps.LingeredApp jdk.test.whitebox.WhiteBox Hello
 *         JCmdTestDumpBase JCmdTestLingeredApp JCmdTestStaticDump
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/timeout=480 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JCmdTestStaticDump
 */

import jdk.test.lib.apps.LingeredApp;

public class JCmdTestStaticDump extends JCmdTestDumpBase {

    static final String STATIC_DUMP_FILE    = "mystatic";
    static final String[] STATIC_MESSAGES   = {"JCmdTestLingeredApp source: shared objects file",
                                               "LingeredApp source: shared objects file",
                                               "Hello source: shared objects file"};

    private static String[] noDumpFlags  =
        {"-Xshare:dump"};
    private static String[] excludeFlags = {
         "-XX:DumpLoadedClassList=AnyFileName.classlist",
         "-XX:+RecordDynamicDumpInfo",
         "-Xshare:on",
         "-Xshare:auto",
         "-XX:SharedClassListFile=non-exist.classlist",
         "-XX:SharedArchiveFile=non-exist.jsa",
         "-XX:ArchiveClassesAtExit=tmp.jsa"};

    private static final int ITERATION_TIMES = 2;

    static void test() throws Exception {
        setIsStatic(true);
        buildJars();

        LingeredApp app = null;
        long pid;

        int  test_count = 1;
        print2ln(test_count++ + " Static dump with default name multiple times.");
        app  = createLingeredApp("-cp", allJars);
        pid = app.getPid();
        for (int i = 0; i < ITERATION_TIMES; i++) {
            test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        }
        app.stopApp();

        print2ln(test_count++ + " Test static dump with given file name.");
        app = createLingeredApp("-cp", allJars);
        pid = app.getPid();
        for (int i = 0; i < ITERATION_TIMES; i++) {
            test("0" + i + ".jsa", pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        }
        app.stopApp();

        print2ln(test_count++ + " Test static dump with flags with which dumping should fail.");
        for (String flag : noDumpFlags) {
            app = createLingeredApp("-cp", allJars, flag, "-XX:SharedArchiveFile=tmp.jsa");
            if (app != null && app.getProcess().isAlive()) {
                pid = app.getPid();
                test(null, pid, noBoot, EXPECT_FAIL);
                app.stopApp();
                throw new RuntimeException("Should not dump successful with " + flag);
            }
        }

        print2ln(test_count++ + " Test static with -Xbootassath/a:boot.jar");
        app = createLingeredApp("-Xbootclasspath/a:" + bootJar, "-cp", testJar);
        pid = app.getPid();
        test(null, pid, useBoot, EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();

        print2ln(test_count++ + " Test static with --limit-modules java.base.");
        app = createLingeredApp("--limit-modules", "java.base", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot, EXPECT_FAIL);
        app.stopApp();

        print2ln(test_count++ + " Test static dump with flags which will be filtered before dumping.");
        for (String flag : excludeFlags) {
            app = createLingeredApp("-cp", allJars, flag);
            pid = app.getPid();
            test(null, pid, noBoot, EXPECT_PASS, STATIC_MESSAGES);
            app.stopApp();
        }

        print2ln(test_count++ + " Test static with -Xshare:off will be OK to dump.");
        app = createLingeredApp("-Xshare:off", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();

        print2ln(test_count++ + " Test static with -XX:+RecordDynamicDumpInfo will be OK to dump.");
        app = createLingeredApp("-XX:+RecordDynamicDumpInfo", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();
    }

    public static void main(String... args) throws Exception {
        runTest(JCmdTestStaticDump::test);
    }
}
