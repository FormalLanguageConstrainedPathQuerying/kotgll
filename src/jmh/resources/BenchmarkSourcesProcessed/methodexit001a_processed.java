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

package nsk.jdi.MethodExitEvent._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

public class methodexit001a {

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    public static final int STARTING_BREAKPOINT_LINE = 81;
    public static final int ENDING_BREAKPOINT_LINE = 90;

    static private ArgumentHandler argHandler;
    static private Log log;
    static private IOPipe pipe;

    static private boolean methodInvoked;

    public static void main(String args[]) {
        methodexit001a _methodexit001a = new methodexit001a();
        System.exit(JCK_STATUS_BASE + _methodexit001a.run(args, System.err));
    }

    static int run(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        pipe = argHandler.createDebugeeIOPipe();

        pipe.println(COMMAND_READY);

        String command = pipe.readln();
        if (!command.equals(COMMAND_GO)) {
            log.complain("TEST BUG: Debugee: unknown command: " + command);
            return FAILED;
        }

        methodInvoked = false; 

        try {
            foo();
        } catch (methodexit001e e) {
        }

        methodInvoked = true; 

        pipe.println(COMMAND_DONE);

        command = pipe.readln();
        if (!command.equals(COMMAND_QUIT)) {
            System.err.println("TEST BUG: Debugee: unknown command: " + command);
            return FAILED;
        }

        return PASSED;
    }

    static void foo() throws methodexit001e {
        throw new methodexit001e();
    }
}

class methodexit001e extends Exception {}
