/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.SuspendThread;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class suspendthrd003 extends DebugeeClass {
    private final static String AGENT_LIB = "suspendthrd003";
    private final static int DEF_TIME_MAX = 30;  

    public static Wicket mainEntrance;

    static {
        System.loadLibrary(AGENT_LIB);
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new suspendthrd003().runIt(argv, out);
    }

    /* =================================================================== */

    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    suspendthrd003Thread thread = null;

    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; 

        String[] args = argHandler.getArguments();
        int timeMax = 0;
        if (args.length == 0) {
            timeMax = DEF_TIME_MAX;
        } else {
            try {
                timeMax = Integer.parseUnsignedInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.err.println("'" + args[0] + "': invalid timeMax value.");
                usage();
            }
        }

        System.out.println("About to execute for " + timeMax + " seconds.");

        long count = 0;
        int res = -1;
        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            log.clearLogBuffer();
            count++;

            thread = new suspendthrd003Thread("TestedThread");
            mainEntrance = new Wicket();

            log.display("Starting tested thread");
            try {
                thread.start();
                mainEntrance.waitFor();

                log.display("Sync: thread started");
                status = checkStatus(status);
            } finally {
                thread.letFinish();
            }

            log.display("Finishing tested thread");
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new Failure(e);
            }

            log.display("Sync: thread finished");
            status = checkStatus(status);


            if (status != Consts.TEST_PASSED) {
                break;
            }

            resetAgentData();  
        }

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return status;
    }

    public static void usage() {
        System.err.println("Usage: " + AGENT_LIB + " [time_max]");
        System.err.println("where:");
        System.err.println("    time_max  max looping time in seconds");
        System.err.println("              (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }
}

/* =================================================================== */

class suspendthrd003Thread extends Thread {
    private volatile boolean shouldFinish = false;

    public suspendthrd003Thread(String name) {
        super(name);
    }

    public void run() {
        suspendthrd003.mainEntrance.unlock();
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

    public void letFinish() {
        shouldFinish = true;
    }
}
