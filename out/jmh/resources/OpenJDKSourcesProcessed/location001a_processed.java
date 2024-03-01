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

package nsk.jdi.ExceptionEvent.catchLocation;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.lang.Integer.*;
import java.io.*;



class location001a {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";
    static final String COMMAND_ERROR = "error";

    public static final int userExceptionLocation = 98;
    public static final int userErrorLocation     = 105;
    public static final int userThrowableLocation = 112;

    public static final int userExceptionCatchLocation = 99;
    public static final int userErrorCatchLocation     = 106;
    public static final int userThrowableCatchLocation = 113;
    public static final int javaExceptionCatchLocation = 120;
    public static final int javaErrorCatchLocation     = 127;

    private static boolean userExceptionThrown = false;
    private static boolean userErrorThrown     = false;
    private static boolean userThrowableThrown = false;
    private static boolean javaExceptionThrown = false;
    private static boolean javaErrorThrown     = false;

    public static void main(String args[]) throws Throwable {
        location001a _location001a = new location001a();
        System.exit(JCK_STATUS_BASE + _location001a.runIt(args, System.err));
    }

    int runIt(String args[], PrintStream out) throws Throwable {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Log log = new Log(out, argHandler);

        location001aException e1 = new location001aException ();
        location001aError     e2 = new location001aError ();
        location001aThrowable e3 = new location001aThrowable ();

        pipe.println(COMMAND_READY);

        String command = pipe.readln();
        if (!command.equals(COMMAND_GO)) {
             log.complain("TEST BUG: unknown command: " + command);
             return FAILED;
        }

        try {
            try {
                throw new location001aException(); 
            } catch (location001aException e) { 
                log.display("location001aException is thrown");
                userExceptionThrown = true;
            }

            try {
                throw new location001aError(); 
            } catch (location001aError e) { 
                log.display("location001aError is thrown");
                userErrorThrown = true;
            }

            try {
                throw new location001aThrowable(); 
            } catch (location001aThrowable e) { 
                log.display("location001aThrowable is thrown");
                userThrowableThrown = true;
            }

            try {
                int i = Integer.parseInt("foo");
            } catch (NumberFormatException e) { 
                log.display("NumberFormatException is thrown");
                javaExceptionThrown = true;
            }

            try {
                raiseStackOverflow();
            } catch (StackOverflowError e) { 
                log.display("StackOverflowError is thrown");
                javaErrorThrown = true;
            }

        } catch (Throwable e) {
            log.complain("Unexpected Throwable: " + e.getMessage());
            e.printStackTrace();
            if (e instanceof ThreadDeath) {
                 throw e;
            }
        }

        boolean thrown = true;
        if (!userExceptionThrown) {
            log.complain("TEST BUG: user exception NOT thrown");
            thrown = false;
        }
        if (!userErrorThrown) {
            log.complain("TEST BUG: user error NOT thrown");
            thrown = false;
        }
        if (!userThrowableThrown) {
            log.complain("TEST BUG: user Throwable NOT thrown");
            thrown = false;
        }
        if (!javaExceptionThrown) {
            log.complain("TEST BUG: java exception NOT thrown");
            thrown = false;
        }
        if (!javaErrorThrown) {
            log.complain("TEST BUG: java error NOT thrown");
            thrown = false;
        }

        if (thrown) {
            pipe.println(COMMAND_DONE);
        } else {
            pipe.println(COMMAND_ERROR);
        }

        command = pipe.readln();
        if (!command.equals(COMMAND_QUIT)) {
             log.complain("TEST BUG: unknown command: " + command);
             return FAILED;
        }

        return PASSED;
    }

    private void raiseStackOverflow () {
        raiseStackOverflow();
    }
}

class location001aException extends Exception {}

class location001aError extends Error {}

class location001aThrowable extends Throwable {}
