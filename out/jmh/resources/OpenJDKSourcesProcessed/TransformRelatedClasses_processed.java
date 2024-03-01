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
 */



import java.io.File;
import java.util.ArrayList;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;


public class TransformRelatedClasses {
    static final String archiveName = "./TransformRelatedClasses.jsa";
    static String agentClasses[] = {
        "TransformerAgent",
        "TransformerAgent$SimpleTransformer",
        "TransformUtil"
    };

    String parent;
    String child;
    String[] testClasses = new String[2];
    String[] testNames = new String[2];
    String testJar;
    String agentJar;


    private static void log(String msg) {
        System.out.println("TransformRelatedClasses: " + msg);
    }


    public static void main(String args[]) throws Exception {
        TransformRelatedClasses test = new TransformRelatedClasses(args[0], args[1]);
        test.prepare();

        ArrayList<TestEntry> testTable = new ArrayList<>();

        testTable.add(new TestEntry(0, false, false, true, true));

        testTable.add(new TestEntry(1, true, false, false, false));

        testTable.add(new TestEntry(2, true, true, false, false));

        testTable.add(new TestEntry(3, false, true, true, false));

        for (TestEntry entry : testTable) {
            test.runTest(entry);
        }
    }


    public TransformRelatedClasses(String parent, String child) {
        log("Constructor: parent = " + parent + ", child = " + child);
        this.parent = parent;
        this.child = child;
        testClasses[0] = parent;
        testClasses[1] = child;
        testNames[0] = parent.replace('.', '/');
        testNames[1] = child.replace('.', '/');
    }


    private void prepare() throws Exception {
        String pathToManifest = "../../../../testlibrary/jvmti/TransformerAgent.mf";
        agentJar = ClassFileInstaller.writeJar("TransformerAgent.jar",
                       ClassFileInstaller.Manifest.fromSourceFile(pathToManifest),
                                           agentClasses);

        testJar =
            ClassFileInstaller.writeJar(parent + "-" + child + ".jar",
                                           testClasses);

        String classList =
            CDSTestUtils.makeClassList("transform-" + parent, testNames).getPath();

        CDSTestUtils.createArchiveAndCheck("-Xbootclasspath/a:" + testJar,
            "-XX:ExtraSharedClassListFile=" + classList);
    }


    private void runTest(TestEntry entry) throws Exception {
        log("runTest(): testCaseId = " + entry.testCaseId);

        String agentParam = "-javaagent:" + agentJar + "=" +
            TransformTestCommon.getAgentParams(entry, parent, child);

        CDSOptions opts = new CDSOptions()
            .addPrefix("-Xbootclasspath/a:" + testJar, "-Xlog:class+load=info")
            .setUseVersion(false)
            .addSuffix( "-showversion",agentParam, child);

        OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
        TransformTestCommon.checkResults(entry, out, parent, child);
    }
}
