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

package nsk.jdwp.ObjectReference.IsCollected;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class iscollected001a {

    public static final String OBJECT_FIELD_NAME = "object";

    public static void main(String args[]) {
        iscollected001a _iscollected001a = new iscollected001a();
        System.exit(iscollected001.JCK_STATUS_BASE + _iscollected001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        log.display("Creating object of tested class");
        TestedClass.object = new TestedClass();

        log.display("Sending signal to debugger: " + iscollected001.READY);
        pipe.println(iscollected001.READY);

        log.display("Waiting for signal from debugger: " + iscollected001.QUIT);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        if (signal == null || !signal.equals(iscollected001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + iscollected001.QUIT + ")");
            log.display("Debugee FAILED");
            return iscollected001.FAILED;
        }

        log.display("Debugee PASSED");
        return iscollected001.PASSED;
    }

    public static class TestedClass {

        public static volatile TestedClass object = null;

        private int foo = 0;

        public TestedClass() {
            foo = 100;
        }
    }

}
