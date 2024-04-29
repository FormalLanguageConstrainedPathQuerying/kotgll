/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.allocation.AP01;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap01t001 extends DebugeeClass implements Cloneable {
    /* number of interations to provoke garbage collecting */
    final static int GC_TRYS = 4;
    static ap01t001 keepAlive;

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return (keepAlive = new ap01t001()).runThis(argv, out);
    }
/*
    private native void setTag();
*/
    private static native Object newObject();

    private static native Object allocObject();

    private native void flushObjectFreeEvents();

    private ap01t001[] ap01t001arr = new ap01t001[6];

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; 

        Class<? extends ap01t001> ap01t001Cls = this.getClass();

        try {
            ap01t001arr[0] = new ap01t001();

            ap01t001arr[1] = (ap01t001) this.clone();

            ap01t001arr[2] = ap01t001Cls.newInstance();

            ap01t001arr[3] = ap01t001Cls.getConstructor().newInstance();

            ap01t001arr[4] = (ap01t001) newObject();

            ap01t001arr[5] = (ap01t001) allocObject();

        } catch (Exception e) {
            e.printStackTrace(out);
            status = Consts.TEST_FAILED;
        }

        log.display("Sync: Debugee started");
        status = checkStatus(status);

        ap01t001arr = null;

        for (int i= 0; i < GC_TRYS; i++)
            System.gc();

        log.display("Sync: GC called");

        flushObjectFreeEvents();

        status = checkStatus(status);
        return status;
    }
}
