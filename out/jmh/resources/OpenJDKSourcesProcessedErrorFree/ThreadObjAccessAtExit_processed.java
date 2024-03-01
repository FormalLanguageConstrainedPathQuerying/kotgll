/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240588
 * @summary Use the WhiteBox API to ensure that we can safely access the
 *          threadObj oop of a JavaThread during termination, after it has
 *          removed itself from the main ThreadsList.
 * @library /testlibrary /test/lib
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @comment run with a small heap, but we need at least 11M for ZGC with JFR
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xmx11m -XX:-DisableExplicitGC ThreadObjAccessAtExit
 */

import jdk.test.whitebox.WhiteBox;


public class ThreadObjAccessAtExit {

    static class GCThread extends Thread {

        static Object[] arr = new Object[64*1024];

        public void run() {
            System.out.println("GCThread waiting ... ");
            try {
                while (target.getPriority() != Thread.NORM_PRIORITY + 2) {
                    Thread.sleep(10);
                }
            }
            catch(InterruptedException ie) {
                throw new RuntimeException(ie);
            }

            System.out.println("GCThread running ... ");

            arr = null;
            System.gc();
        }
    }

    static Thread target;  

    public static void main(String[] args) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();

        GCThread g = new GCThread();
        g.setName("GCThread");

        target = new Thread("Target") {
                public void run() {
                    Thread current = Thread.currentThread();
                    try {
                        while (current.getPriority() != Thread.NORM_PRIORITY + 1) {
                            Thread.sleep(10);
                        }
                    }
                    catch(InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                    System.out.println("Target is terminating");
                }
            };
        g.start();
        target.setPriority(Thread.NORM_PRIORITY); 
        target.start();
        wb.checkThreadObjOfTerminatingThread(target);
    }
}
