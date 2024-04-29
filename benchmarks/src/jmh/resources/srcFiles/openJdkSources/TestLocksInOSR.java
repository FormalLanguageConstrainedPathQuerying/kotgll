/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8322743
 * @summary EA incorrectly marks locks for elimination for escaped object which comes from Interpreter in OSR compilation.
 * @run main/othervm -XX:-TieredCompilation -Xcomp -XX:CompileCommand=compileonly,TestLocksInOSR*::* -XX:CompileCommand=quiet TestLocksInOSR
 * @run main TestLocksInOSR
 */

public class TestLocksInOSR {

    public static void main(String[] args) throws Exception {
        test1();

        test2();
    }

    static void test1() throws Exception {
        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 2; ++i) {
                    synchronized (new Object()) {
                        for (int j = 0; j < 100_000; ++j) {
                        }
                    }
                }
            }
        });
        writeThread.start();
        writeThread.join();
    }

    static void test2() {
        for (int i = 0; i < 2; ++i) {
            synchronized (new Object()) {
                for (int j = 0; j < 100_000; ++j) {
                }
            }
        }
    }
}
