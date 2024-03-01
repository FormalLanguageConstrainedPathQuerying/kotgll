/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test jvmti class file loader hook interaction with AppCDS
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @requires vm.cds
 * @requires vm.jvmti
 * @build ClassFileLoadHook
 * @run main/othervm/native ClassFileLoadHookTest
 */


import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ClassFileLoadHookTest {
    public static String sharedClasses[] = {
        "ClassFileLoadHook",
        "ClassFileLoadHook$TestCaseId",
        "LoadMe",
        "java/sql/SQLException"
    };

    public static void main(String[] args) throws Exception {
        String wbJar =
            ClassFileInstaller.writeJar("WhiteBox.jar", "jdk.test.whitebox.WhiteBox");
        String appJar =
            ClassFileInstaller.writeJar("ClassFileLoadHook.jar", sharedClasses);
        String useWb = "-Xbootclasspath/a:" + wbJar;

        CDSOptions opts = (new CDSOptions())
            .setUseVersion(false)
            .setXShareMode("off")
            .addSuffix("-XX:+UnlockDiagnosticVMOptions",
                       "-XX:+WhiteBoxAPI",
                       useWb,
                       "-agentlib:SimpleClassFileLoadHook=LoadMe,beforeHook,after_Hook",
                       "ClassFileLoadHook",
                       "" + ClassFileLoadHook.TestCaseId.SHARING_OFF_CFLH_ON);
        CDSTestUtils.run(opts)
                    .assertNormalExit();

        TestCommon.testDump(appJar, sharedClasses, useWb);
        OutputAnalyzer out = TestCommon.exec(appJar,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI", useWb,
                "ClassFileLoadHook",
                "" + ClassFileLoadHook.TestCaseId.SHARING_ON_CFLH_OFF);

        TestCommon.checkExec(out);


        out = TestCommon.execAuto("-cp", appJar,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI", useWb,
                "-agentlib:SimpleClassFileLoadHook=LoadMe,beforeHook,after_Hook",
                "ClassFileLoadHook",
                "" + ClassFileLoadHook.TestCaseId.SHARING_AUTO_CFLH_ON);

        opts = (new CDSOptions()).setXShareMode("auto");
        TestCommon.checkExec(out, opts);

        out = TestCommon.exec(appJar,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI", useWb,
                "-agentlib:SimpleClassFileLoadHook=LoadMe,beforeHook,after_Hook",
                "ClassFileLoadHook",
                "" + ClassFileLoadHook.TestCaseId.SHARING_ON_CFLH_ON);
        TestCommon.checkExec(out);
    }
}
