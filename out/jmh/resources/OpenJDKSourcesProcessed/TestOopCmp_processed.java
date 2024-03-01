/*
 * Copyright (c) 2019 SAP SE. All rights reserved.
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

/**
 * @test
 * @bug 8221083
 * @requires vm.gc.Serial
 * @requires vm.bits == 64 & vm.opt.final.UseCompressedOops == true
 * @summary On ppc64, C1 erroneously emits a 32-bit compare instruction for oop compares.
 * @modules java.base/jdk.internal.misc:+open
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbatch -XX:-UseTLAB -Xmx4m -XX:+UseSerialGC -XX:HeapBaseMinAddress=0x700000000
 *      -XX:CompileCommand=compileonly,compiler.codegen.TestOopCmp::nullTest
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.TestOopCmp
 * @author volker.simonis@gmail.com
 */

package compiler.codegen;

import jdk.test.whitebox.WhiteBox;

public class TestOopCmp {

    private static Object nullObj = null;

    public static boolean nullTest(Object o) {
        if (o == nullObj) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String args[]) {

        WhiteBox WB = WhiteBox.getWhiteBox();

        System.gc();
        String s = new String("I'm not null!!!");
        if (WB.getObjectAddress(s) == 0x700000000L) {
            System.out.println("Got object at address 0x700000000");
        }

        for (int i = 0; i < 30_000; i++) {
            if (nullTest(s)) {
                throw new RuntimeException("Comparing non-null object with null returned 'true'");
            }
        }
    }
}
