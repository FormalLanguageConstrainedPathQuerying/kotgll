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

package nsk.jvmti.scenarios.sampling.SP05;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class sp05t003 extends DebugeeClass {

    static {
        System.loadLibrary("sp05t003");
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new sp05t003().runIt(argv, out);
    }

    /* =================================================================== */

    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;

    static Object endingMonitor = new Object();

    static sp05t003Thread threads[] = null;

    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        threads = new sp05t003Thread[] {
            new sp05t003ThreadRunningJava(),
            new sp05t003ThreadRunningNative()
        };

        log.display("Sync: threads created");
        status = checkStatus(status);

        try {

            log.display("Starting tested threads");
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }

            for (int i = 0; i < threads.length; i++) {
                if (!threads[i].checkStarted()) {
                    throw new Failure("Unable to prepare thread #" + i + ": " + threads[i]);
                }
            }

            log.display("Sync: threads started");
            status = checkStatus(status);

        } finally {
            for (int i = 0; i < threads.length; i++) {
                threads[i].letFinish();
            }
        }

        log.display("Finishing tested threads");
        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Sync: threads finished");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

abstract class sp05t003Thread extends Thread {
    public abstract boolean checkStarted();

    public abstract void letFinish();
}

/* =================================================================== */

class sp05t003ThreadRunningJava extends sp05t003Thread {
    private volatile boolean hasStarted = false;
    private volatile boolean shouldFinish = false;

    public void run() {
        hasStarted = true;

        int i = 0;
        int n = 1000;
        while (!shouldFinish) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n = n - 1;
            }
            i = i + 1;
        }
    }

    public boolean checkStarted() {
        try {
            while(!hasStarted) {
                sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new Failure("Interrupted while waiting for thread started:\n\t" + e);
        }
        return hasStarted;
    }

    public void letFinish() {
        shouldFinish = true;
    }
}

class sp05t003ThreadRunningNative extends sp05t003Thread {
    public native void run();
    public native boolean checkStarted();
    public native void letFinish();
}
