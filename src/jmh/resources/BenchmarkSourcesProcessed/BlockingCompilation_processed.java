/*
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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
 * @bug 8150646 8153013
 * @summary Add support for blocking compiles through whitebox API
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @requires vm.compiler1.enabled | !vm.graal.enabled
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -Xmixed
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -XX:+PrintCompilation
 *        compiler.whitebox.BlockingCompilation
 */

package compiler.whitebox;

import compiler.testlibrary.CompilerUtils;
import jdk.test.whitebox.WhiteBox;

import java.lang.reflect.Method;

public class BlockingCompilation {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static int foo() {
        return 42; 
    }

    public static void main(String[] args) throws Exception {
        Method m = BlockingCompilation.class.getMethod("foo");
        int[] levels = CompilerUtils.getAvailableCompilationLevels();
        int highest_level = levels[levels.length-1];

        if (levels.length == 0) return;

        WB.lockCompilation();

        if (WB.isMethodCompiled(m)) {
            throw new Exception("Should not be compiled after deoptimization");
        }
        if (WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Should not be enqueued on any level");
        }

        if (!WB.enqueueMethodForCompilation(m, highest_level)) {
            throw new Exception("Failed to enqueue method on level: " + highest_level);
        }

        if (!WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Must be enqueued because of locked compilation");
        }

        WB.unlockCompilation();
        while (!WB.isMethodCompiled(m)) {
          Thread.sleep(100);
        }
        WB.deoptimizeMethod(m);
        WB.clearMethodState(m);

        String directive = "[{ match: \""
                + BlockingCompilation.class.getName().replace('.', '/')
                + ".foo\", BackgroundCompilation: false }]";
        if (WB.addCompilerDirective(directive) != 1) {
            throw new Exception("Failed to add compiler directive");
        }

        try {
            for (int l : levels) {
                WB.deoptimizeMethod(m);

                if (WB.isMethodCompiled(m)) {
                    throw new Exception("Should not be compiled after deoptimization");
                }
                if (WB.isMethodQueuedForCompilation(m)) {
                    throw new Exception("Should not be enqueued on any level");
                }

                if (!WB.enqueueMethodForCompilation(m, l)) {
                    throw new Exception("Could not be enqueued for compilation");
                }

                if (!WB.isMethodCompiled(m)) {
                    throw new Exception("Must be compiled here");
                }
                if (WB.getMethodCompilationLevel(m) != l) {
                    String msg = m + " should be compiled at level " + l +
                                 "(but is actually compiled at level " +
                                 WB.getMethodCompilationLevel(m) + ")";
                    System.out.println("==> " + msg);
                    throw new Exception(msg);
                }
            }
        } finally {
            WB.removeCompilerDirective(1);
        }

        WB.deoptimizeMethod(m);
        WB.clearMethodState(m);

        WB.lockCompilation();

        if (WB.isMethodCompiled(m)) {
            throw new Exception("Should not be compiled after deoptimization");
        }
        if (WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Should not be enqueued on any level");
        }

        WB.enqueueMethodForCompilation(m, highest_level);

        WB.unlockCompilation();
    }
}
