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

package nsk.jdwp.ThreadReference.Resume;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class resume001a {

    public static final String THREAD_NAME = "TestedThreadName";
    public static final String FIELD_NAME = "thread";

    private static Object threadStarted = new Object();
    private static Object threadLock = new Object();

    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        resume001a _resume001a = new resume001a();
        System.exit(resume001.JCK_STATUS_BASE + _resume001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        synchronized (threadLock) {

            log.display("Creating object of tested class");
            TestedClass.thread = new TestedClass(THREAD_NAME);

            synchronized (threadStarted) {
                TestedClass.thread.start();
                try {
                    threadStarted.wait();
                    log.display("Sending signal to debugger: " + resume001.READY);
                    pipe.println(resume001.READY);
                } catch (InterruptedException e) {
                    log.complain("Interruption while waiting for thread started: " + e);
                    pipe.println(resume001.ERROR);
                }
            }

            log.display("Waiting for signal from debugger: " + resume001.QUIT);
            String signal = pipe.readln();
            log.display("Received signal from debugger: " + signal);

            if (signal == null || !signal.equals(resume001.QUIT)) {
                log.complain("Unexpected communication signal from debugee: " + signal
                            + " (expected: " + resume001.QUIT + ")");
                log.display("Debugee FAILED");
                return resume001.FAILED;
            }

        }

        log.display("Debugee PASSED");
        return resume001.PASSED;
    }

    public static class TestedClass extends Thread {

        public static volatile TestedClass thread = null;

        TestedClass(String name) {
            super(name);
        }

        public void run() {
            log.display("Tested thread started");

            synchronized (threadStarted) {
                threadStarted.notifyAll();
            }

            synchronized (threadLock) {
                log.display("Tested thread finished");
            }
        }
    }

}
