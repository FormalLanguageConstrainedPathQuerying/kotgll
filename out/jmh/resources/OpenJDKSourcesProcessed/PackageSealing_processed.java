/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary AppCDS handling of package.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/C1.java test-classes/C2.java test-classes/C3.java
 *          test-classes/PackageSealingTest.java test-classes/Hello.java
 * @run driver PackageSealing
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class PackageSealing {
    public static void main(String args[]) throws Exception {
        String[] classList = {"foo/C1", "pkg/C2", "PackageSealingTest"};
        String appJar = ClassFileInstaller.writeJar("foo-sealed.jar",
            ClassFileInstaller.Manifest.fromSourceFile("test-classes/package_seal.mf"),
            "PackageSealingTest", "foo/C1", "pkg/C2");

        String helloJar = JarBuilder.getOrCreateHelloJar();
        String jars = helloJar + File.pathSeparator + appJar;

        TestCommon.testDump(jars, TestCommon.list(classList));
        OutputAnalyzer output;
        output = TestCommon.exec(jars, "PackageSealingTest",
                                 "foo/C1", "sealed", "pkg/C2", "notSealed");
        TestCommon.checkExec(output, "OK");

        TestCommon.dump(helloJar, TestCommon.list(classList),
                        "-Xbootclasspath/a:" + appJar);
        output = TestCommon.exec(helloJar, "-Xbootclasspath/a:" + appJar,
                                 "PackageSealingTest",
                                 "foo/C1", "sealed", "pkg/C2", "notSealed");
        TestCommon.checkExec(output, "OK");

        classList[1] = "foo/C3"; 
        String[] classList2 = {"foo/C3", "foo/C1", "PackageSealingTest"};
        String nonSealedJar = ClassFileInstaller.writeJar("foo-unsealed.jar", "foo/C3");
        jars = helloJar + File.pathSeparator + nonSealedJar;
        TestCommon.testDump(jars, TestCommon.list(classList2));
        jars += File.pathSeparator + appJar;
        output = TestCommon.exec(jars, "-Xlog:class+load", "PackageSealingTest",
                                 "foo/C3", "notSealed", "foo/C1", "sealed");
        TestCommon.checkExec(output,
                             "foo.C3 source: shared objects file",
                             "sealing violation: can't seal package foo: already defined");

        jars = helloJar + File.pathSeparator + appJar;
        TestCommon.testDump(jars, TestCommon.list(classList2));
        jars += File.pathSeparator + nonSealedJar;
        output = TestCommon.exec(jars, "-Xlog:class+load", "PackageSealingTest",
                                 "foo/C1", "sealed", "foo/C3", "notSealed");
        TestCommon.checkExec(output,
                             "foo.C1 source: shared objects file",
                             "sealing violation: package foo is sealed");
    }
}
