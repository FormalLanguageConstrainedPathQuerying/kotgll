/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.PopFrame;

import java.io.PrintStream;

public class popframe010 {

    final static int JCK_STATUS_BASE = 95;
    final static int NESTING_DEPTH = 8;

    static {
        try {
            System.loadLibrary("popframe010");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load popframe010 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls, int depth);
    native static int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        TestThread thr = new TestThread();
        getReady(TestThread.class, NESTING_DEPTH + 1);

        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return check();
    }

    static class TestThread extends Thread {
        public void run() {
            countDown(NESTING_DEPTH);
        }

        public void countDown(int nestingCount) {
            if (nestingCount > 0) {
                countDown(nestingCount - 1);
            } else {
                checkPoint();
            }
        }

        void checkPoint() {
        }
    }
}
