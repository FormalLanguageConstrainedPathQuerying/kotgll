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


package nsk.jdwp.Event.EXCEPTION;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class exception001a {

    static final int BREAKPOINT_LINE = 102;
    static final int EXCEPTION_THROW_LINE = 114;
    static final int EXCEPTION_CATCH_LINE = 121; 

    static ArgumentHandler argumentHandler = null;
    static Log log = null;

    public static void main(String args[]) {
        exception001a _exception001a = new exception001a();
        System.exit(exception001.JCK_STATUS_BASE + _exception001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        log.display("Creating tested thread");
        TestedThreadClass thread = new TestedThreadClass(exception001.TESTED_THREAD_NAME);
        log.display("  ... thread created");

        log.display("Creating tested exception object");
        TestedThreadClass.exception = new TestedExceptionClass("tested exception");
        log.display("  ... exception object created");

        log.display("Starting tested thread");
        thread.start();
        log.display("  ... thread started");

        try {
            log.display("Waiting for tested thread finished");
            thread.join();
            log.display("  ... thread finished");
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished");
            return exception001.FAILED;
        }

        log.display("Debugee PASSED");
        return exception001.PASSED;
    }

    public static class TestedThreadClass extends Thread {

        public static volatile TestedExceptionClass exception = null;

        public TestedThreadClass(String name) {
            super(name);
        }

        public void run() {
            log.display("Tested thread: started");

            log.display("Breakpoint line reached");
            int foo = 0; 
            log.display("Breakpoint line passed");

            methodForCatch();

            log.display("Tested thread: finished");
        }

        public void methodForThrow() throws TestedExceptionClass {
            log.display("Throwing tested exception:\n\t" + exception);
            throw exception; 
        }

        public void methodForCatch() {
            try {
                methodForThrow();
            } catch (TestedExceptionClass e) { 
                log.display("Caught tested exception:\n\t" + e);
            }
        }

    }

    public static class TestedExceptionClass extends Exception {
        public TestedExceptionClass(String message) {
            super(message);
        }
    }
}
