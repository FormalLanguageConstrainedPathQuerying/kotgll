/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.ThreadReference.Stop;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class stop001a {

    public static final String THREAD_NAME = "TestedThreadName";
    public static final String THREAD_FIELD_NAME = "thread";
    public static final String THROWABLE_FIELD_NAME = "throwable";

    public static final int FRAMES_COUNT = 10;

    private static Object threadStarting = new Object();
    private static Object threadWaiting = new Object();

    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        stop001a _stop001a = new stop001a();
        System.exit(stop001.JCK_STATUS_BASE + _stop001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);
        long timeout = argumentHandler.getWaitTime() * 60 * 1000; 

        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        log.display("Creating object of tested class");
        TestedClass.thread = new TestedClass(THREAD_NAME);

        synchronized (threadStarting) {
            TestedClass.thread.start();
            try {
                threadStarting.wait();
            } catch (InterruptedException e) {
                log.complain("Interruption while waiting for thread started:\n\t" + e);
                pipe.println(stop001.ERROR);
                log.display("Debugee FAILED");
                return stop001.FAILED;
            }

            synchronized (threadWaiting) {
                log.display("Sending signal to debugger: " + stop001.READY);
                pipe.println(stop001.READY);
            }
        }

        log.display("Waiting for signal from debugger: " + stop001.RUN);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        if (signal == null || !signal.equals(stop001.RUN)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + stop001.RUN + ")");
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        log.display("Waiting for tested thread finished for timeout: " + timeout);
        try {
            TestedClass.thread.join(timeout);
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished:\n\t" + e);
            pipe.println(stop001.ERROR);
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        if (TestedClass.thread.isAlive()) {
            log.display("Sending signal to debugger: " + stop001.NOT_STOPPED);
            pipe.println(stop001.NOT_STOPPED);
            TestedClass.thread.interrupt();
        } else {
            log.display("Sending signal to debugger: " + stop001.STOPPED);
            pipe.println(stop001.STOPPED);
        }

        log.display("Waiting for signal from debugger: " + stop001.QUIT);
        signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        if (signal == null || !signal.equals(stop001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + stop001.QUIT + ")");
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        log.display("Debugee PASSED");
        return stop001.PASSED;
    }

    public static class TestedClass extends Thread {

        public static volatile TestedClass thread = null;

        public static volatile Throwable throwable = new Throwable("Tested throwable");

        TestedClass(String name) {
            super(name);
        }

        public void run() {
            log.display("Tested thread started");

            synchronized (threadWaiting) {

                synchronized (threadStarting) {
                    threadStarting.notifyAll();
                }

                try {
                    threadWaiting.wait();
                    log.complain("Tested thread NOT interrupted");
                } catch (InterruptedException e) {
                    log.display("Tested thread interrupted");
                }
            }

            log.display("Tested thread finished");
        }

    }

}
