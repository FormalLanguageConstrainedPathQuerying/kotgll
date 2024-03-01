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


package nsk.jdwp.ClassType.InvokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class invokemeth001a {

    public static final String THREAD_NAME = "testedThread";

    public static final int BREAKPOINT_LINE_NUMBER = 86;

    public static final int INITIAL_VALUE = 10;
    public static final int FINAL_VALUE = 1234;

    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        invokemeth001a _invokemeth001a = new invokemeth001a();
        System.exit(invokemeth001.JCK_STATUS_BASE + _invokemeth001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        log.display("Creating object of tested class");
        TestedObjectClass foo = new TestedObjectClass();
        log.display("  ... object created");

        TestedObjectClass.run();

        log.display("Debugee PASSED");
        return invokemeth001.PASSED;
    }

    public static class TestedObjectClass {
        public static volatile int result = INITIAL_VALUE;

        public static void run() {
            log.display("Tested thread: started");

            log.display("Breakpoint line reached");
            int foo = 0; 
            log.display("Breakpoint line passed");

            log.display("Tested thread: finished");
        }

        public static int testedMethod(int arg) {
            log.display("Tested method invoked with argument:" + arg);
            int old = result;
            result = arg;
            log.display("Tested method returned with result:" + old);
            return old;
        }
    }
}
