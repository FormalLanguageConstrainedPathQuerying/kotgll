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

package nsk.jvmti.SetThreadLocalStorage;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class setthrdstor001 extends DebugeeClass {

    static {
        System.loadLibrary("setthrdstor001");
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setthrdstor001().runIt(argv, out);
    }

    /* =================================================================== */

    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    setthrdstor001Thread thread = null;

    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; 

        thread = new setthrdstor001Thread("TestedThread");

        log.display("Staring tested thread");
        try {
            synchronized (thread.endingMonitor) {
                synchronized (thread.runningMonitor) {
                    synchronized (thread.startingMonitor) {
                        thread.start();
                        thread.startingMonitor.wait();
                    }

                    log.display("Sync: thread started");
                    status = checkStatus(status);

                    thread.runningMonitor.wait();

                    log.display("Sync: thread ran");
                    status = checkStatus(status);
                }

            }

            log.display("Finishing tested thread");
            thread.join();
        } catch (InterruptedException e) {
            throw new Failure("Interruption while running tested thread: \n\t" + e);
        }

        return status;
    }
}

/* =================================================================== */

class setthrdstor001Thread extends Thread {
    public Object startingMonitor = new Object();
    public Object runningMonitor = new Object();
    public Object endingMonitor = new Object();

    public setthrdstor001Thread(String name) {
        super(name);
    }

    public void run() {
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        synchronized (runningMonitor) {
            runningMonitor.notifyAll();
        }

        synchronized (endingMonitor) {
        }
    }
}
