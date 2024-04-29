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
 */

/*
 * @test
 * @summary Tests WB::isMethodCompilable(m) in combination with compiler directives that prevent a compilation of m.
 * @bug 8263582
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:CompileCommand=compileonly,compiler.whitebox.TestMethodCompilableCompilerDirectives::doesNotExist
 *      compiler.whitebox.TestMethodCompilableCompilerDirectives
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:CompileCommand=exclude,compiler.whitebox.TestMethodCompilableCompilerDirectives::*
 *      compiler.whitebox.TestMethodCompilableCompilerDirectives
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import jdk.test.whitebox.WhiteBox;
import java.lang.reflect.Method;

public class TestMethodCompilableCompilerDirectives {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public static int c1Compiled() {
        return 3;
    }


    public static int c2Compiled() {
        for (int i = 0; i < 100; i++);
        return 3;
    }

    public static void main(String[] args) throws NoSuchMethodException {
        Method c1CompiledMethod = TestMethodCompilableCompilerDirectives.class.getDeclaredMethod("c1Compiled");
        Method c2CompiledMethod = TestMethodCompilableCompilerDirectives.class.getDeclaredMethod("c2Compiled");

        boolean compilable = WhiteBox.getWhiteBox().isMethodCompilable(c1CompiledMethod);
        Asserts.assertFalse(compilable);
        for (int i = 0; i < 3000; i++) {
            c1Compiled();
        }
        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c1CompiledMethod);
        Asserts.assertFalse(compilable);


        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c2CompiledMethod);
        Asserts.assertFalse(compilable);
        for (int i = 0; i < 3000; i++) {
            c2Compiled();
        }
        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c2CompiledMethod);
        Asserts.assertFalse(compilable);
    }
}
