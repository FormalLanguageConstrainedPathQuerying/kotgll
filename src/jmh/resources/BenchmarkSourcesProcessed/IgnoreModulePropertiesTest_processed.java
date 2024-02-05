/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8136930
 * @summary Test that the VM ignores explicitly specified module internal properties.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver IgnoreModulePropertiesTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class IgnoreModulePropertiesTest {

    public static void testProperty(String prop, String value) throws Exception {
        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
            "-D" + prop + "=" + value, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain(" version ");
        output.shouldHaveExitValue(0);

        if (System.getProperty(prop) != null) {
            throw new RuntimeException(
                "Unexpected non-null value for property " + prop);
        }
    }

    public static void testOption(boolean shouldVMFail,
                                  String option, String value,
                                  String prop, String result) throws Exception {
        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
            option + "=" + value, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        if (shouldVMFail) {
            output.shouldNotHaveExitValue(0);
        } else {
            output.shouldHaveExitValue(0);
        }
        output.shouldContain(result);
        testProperty(prop, value);
    }

    public static void main(String[] args) throws Exception {
        testOption(/*shouldVMFail=*/true, "--add-modules", "java.sqlx", "jdk.module.addmods.0", "java.lang.module.FindException");
        testOption(/*shouldVMFail=*/true, "--limit-modules", "java.sqlx", "jdk.module.limitmods", "java.lang.module.FindException");
        testOption(/*shouldVMFail=*/true, "--patch-module", "=d", "jdk.module.patch.0", "Unable to parse --patch-module");

        testOption(/*shouldVMFail=*/false,  "--add-reads", "xyzz=yyzd", "jdk.module.addreads.0", "WARNING: Unknown module: xyzz");
        testOption(/*shouldVMFail=*/false,  "--add-exports", "java.base/xyzz=yyzd", "jdk.module.addexports.0",
                   "WARNING: package xyzz not in java.base");
    }
}
