/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestMembarDependencies
 * @bug 8172850
 * @summary Tests correct scheduling of memory loads around MembarVolatile emitted by GC barriers.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver compiler.membars.TestMembarDependencies
 */

package compiler.membars;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestMembarDependencies {
    private static TestMembarDependencies f1;
    private static TestMembarDependencies f2;

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            OutputAnalyzer oa = ProcessTools.executeTestJava("-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:-TieredCompilation", "-XX:-BackgroundCompilation", "-XX:+PrintOpto",
                "-XX:CompileCommand=compileonly,compiler.membars.TestMembarDependencies::test*",
                "-XX:CompileCommand=dontinline,compiler.membars.TestMembarDependencies::test_m1",
                TestMembarDependencies.class.getName(), "run");
            oa.shouldHaveExitValue(0);
            oa.shouldNotMatch("Bailout: Recompile without subsuming loads");
            System.out.println(oa.getOutput());
        } else {
            f2 = new TestMembarDependencies();
            for (int i = 0; i < 10_000; ++i) {
              f2.test1(f2);
              f2.test2(f2);
            }
        }
    }

    public void test_m1() { }
    public void test_m2() { }

    public void test1(TestMembarDependencies obj) {
        try {
            test_m1();
        } catch (Exception e) {

        } finally {
            f1 = obj;
        }
        f2.test_m2();
    }

    public void test2(TestMembarDependencies obj) {
        test_m1();
        f1 = obj;
        f2.test_m2();
    }
}
