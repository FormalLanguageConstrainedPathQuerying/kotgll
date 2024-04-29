/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Handling of directories in -cp is based on the classlist
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/Super.java
 * @run driver DirClasspathTest
 */

import jdk.test.lib.Platform;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class DirClasspathTest {
    private static final int MAX_PATH = 260;

    static OutputAnalyzer doDump(String path, String classList[],
                                        String... suffix) throws Exception {
        String helloJar = JarBuilder.getOrCreateHelloJar();
        return TestCommon.dump(helloJar + File.pathSeparator + path, classList, suffix);
    }

    public static void main(String[] args) throws Exception {
        File dir = CDSTestUtils.getOutputDirAsFile();
        File emptydir = new File(dir, "emptydir");
        emptydir.mkdir();


        String bootClassList[] = {"java/lang/Object"};

        OutputAnalyzer output;
        output = doDump(emptydir.getPath(), bootClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        Path classDir = Paths.get(System.getProperty("test.classes"));
        Path destDir = classDir;
        int subDirLen = MAX_PATH - classDir.toString().length() - 2;
        if (subDirLen > 0) {
            char[] chars = new char[subDirLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            destDir = Paths.get(System.getProperty("test.classes"), subPath);
        }
        File longDir = destDir.toFile();
        longDir.mkdir();
        File subDir = new File(longDir, "subdir");
        subDir.mkdir();
        output = doDump(subDir.getPath(), bootClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        output = doDump(dir.getPath(), bootClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        output = doDump(longDir.getPath(), bootClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        String appClassList[] = {"java/lang/Object", "com/sun/tools/javac/Main"};

        output = doDump(dir.getPath(), appClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        output = doDump(longDir.getPath(), appClassList, "-Xlog:class+path=info");
        TestCommon.checkDump(output);

        String appClassList2[] = {"Super", "java/lang/Object", "com/sun/tools/javac/Main"};
        output = doDump(classDir.toString(), appClassList2, "-Xlog:class+path=info,class+load=trace");
        output.shouldNotHaveExitValue(0);
        output.shouldContain("Cannot have non-empty directory in paths");

        File srcClass = new File(classDir.toFile(), "Super.class");
        File destClass = new File(longDir, "Super.class");
        Files.copy(srcClass.toPath(), destClass.toPath(), StandardCopyOption.REPLACE_EXISTING);
        output = doDump(longDir.getPath(), appClassList2, "-Xlog:class+path=info");
        output.shouldNotHaveExitValue(0);
        output.shouldContain("Cannot have non-empty directory in paths");
    }
}
