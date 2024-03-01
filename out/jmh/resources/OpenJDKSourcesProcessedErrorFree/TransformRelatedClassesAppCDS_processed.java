/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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


import java.io.File;
import java.util.ArrayList;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;


public class TransformRelatedClassesAppCDS extends TransformRelatedClasses {
    private static void log(String msg, Object... args) {
        String msg0 = String.format(msg, args);
        System.out.println("TransformRelatedClassesAppCDS: " + msg0);
    }

    public static void main(String args[]) throws Exception {
        TransformRelatedClassesAppCDS test =
            new TransformRelatedClassesAppCDS(args[0], args[1]);

        test.prepareAgent(agentClasses);

        ArrayList<TestEntry> testTable = new ArrayList<>();

        testTable.add(new TestEntry(0, false, false, true, true));

        testTable.add(new TestEntry(1, true, false, false, false));

        testTable.add(new TestEntry(2, true, true, false, false));

        testTable.add(new TestEntry(3, false, true, true, false));

        test.runWithAppLoader(testTable);
        test.runWithCustomLoader(testTable);
    }


    public TransformRelatedClassesAppCDS(String parent, String child) {
        super(parent, child);

        CustomLoaderApp.ping();
    }


    private void prepareAgent(String[] agentClasses) throws Exception {
        String manifest = "../../../../../testlibrary/jvmti/TransformerAgent.mf";
        agentJar = ClassFileInstaller.writeJar("TransformerAgent.jar",
                   ClassFileInstaller.Manifest.fromSourceFile(manifest),
                                           agentClasses);
    }


    private void runWithAppLoader(ArrayList<TestEntry> testTable) throws Exception {
        String appJar = writeJar("app", testClasses);

        OutputAnalyzer out = TestCommon.dump(appJar, testClasses);
        TestCommon.checkDump(out);

        for (TestEntry entry : testTable) {
            log("runTestWithAppLoader(): testCaseId = %d", entry.testCaseId);
            String params = TransformTestCommon.getAgentParams(entry, parent, child);
            String agentParam = String.format("-javaagent:%s=%s", agentJar, params);
            TestCommon.run("-Xlog:class+load=info", "-cp", appJar,
                           agentParam, child)
              .assertNormalExit(output -> TransformTestCommon.checkResults(entry, output, parent, child));
        }
    }


    private String[] getCustomClassList(String loaderType, String customJar) {
        String type = child + "-" + loaderType;

        switch (type) {

        case "SubClass-unregistered":
            return new String[] {
                "CustomLoaderApp",
                "java/lang/Object id: 0",
                parent + " id: 1 super: 0 source: " + customJar,
                child +  " id: 2 super: 1 source: " + customJar,
            };

        case "Implementor-unregistered":
            return new String[] {
                "CustomLoaderApp",
                "java/lang/Object id: 0",
                parent + " id: 1 super: 0 source: " + customJar,
                child +  " id: 2 super: 0 interfaces: 1 source: " + customJar,
            };

        default:
            throw new IllegalArgumentException("getCustomClassList - wrong type: " + type);
        }
    }


    private void runWithCustomLoader(ArrayList<TestEntry> testTable) throws Exception {
        if (!Platform.areCustomLoadersSupportedForCDS()) {
            log("custom loader not supported for this platform" +
                " - skipping test case for custom loader");
            return;
        }

        if (TestCommon.isDynamicArchive()) {
            log("custom loader class list not applicable to dynamic archive" +
                " - skipping test case for custom loader");
            return;
        }

        String appClasses[] = {
            "CustomLoaderApp",
        };

        String customClasses[] = { parent, child };

        String appJar = writeJar("custldr-app", appClasses);
        String customJar = writeJar("custldr-custom", customClasses);

        for (TestEntry entry : testTable) {
            log("runTestWithCustomLoader(): testCaseId = %d", entry.testCaseId);
            String[] classList = getCustomClassList("unregistered",customJar);
            execAndCheckWithCustomLoader(entry, "unregistered", classList,
                                         appJar, agentJar, customJar);
        }
    }


    private void
        execAndCheckWithCustomLoader(TestEntry entry, String loaderType,
                                     String[] classList, String appJar,
                                     String agentJar, String customJar)
        throws Exception {

        OutputAnalyzer out = TestCommon.dump(appJar, classList);
        TestCommon.checkDump(out);

        String agentParam = "-javaagent:" + agentJar + "=" +
            TransformTestCommon.getAgentParams(entry, parent, child);

        TestCommon.run("-Xlog:class+load=info",
                       "-cp", appJar,
                       agentParam,
                       "CustomLoaderApp",
                       customJar, loaderType, child)
          .assertNormalExit(output -> TransformTestCommon.checkResults(entry, output, parent, child));
    }


    private String writeJar(String type, String[] classes)
        throws Exception {
        String jarName = String.format("%s-%s.jar", child, type);
        return ClassFileInstaller.writeJar(jarName, classes);
    }
}
