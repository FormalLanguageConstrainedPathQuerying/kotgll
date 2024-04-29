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


package nsk.jdwp.ClassType.NewInstance;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class newinst001a {

    public static final String THREAD_NAME = "testedThread";

    public static final int BREAKPOINT_LINE_NUMBER = 88;

    public static final int INITIAL_VALUE = 10;
    public static final int FINAL_VALUE = 1234;

    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        newinst001a _newinst001a = new newinst001a();
        System.exit(newinst001.JCK_STATUS_BASE + _newinst001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        log.display("Accessing to tested class");
        TestedObjectClass.foo = 100;
        log.display("  ... class loaded");

        TestedObjectClass.run();

        log.display("Debugee PASSED");
        return newinst001.PASSED;
    }

    public static class TestedObjectClass {
        public static int foo = 0;

        public static volatile int result = INITIAL_VALUE;

        public static void run() {
            log.display("Tested thread: started");

            log.display("Breakpoint line reached");
            int foo = 0; 
            log.display("Breakpoint line passed");

            log.display("Tested thread: finished");
        }

        public TestedObjectClass(int arg) {
            log.display("Tested constructor invoked with argument:" + arg);
            if (arg != FINAL_VALUE) {
                log.complain("Unexpected value of constructor argument: " + arg
                            + " (expected: " + FINAL_VALUE + ")");
            }
            result = arg;
        }
    }
}
