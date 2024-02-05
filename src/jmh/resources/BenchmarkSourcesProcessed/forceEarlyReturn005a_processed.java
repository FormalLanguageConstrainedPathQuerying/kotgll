/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005;

import java.util.concurrent.locks.ReentrantLock;
import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.jdi.*;

class TestThread extends Thread {
    private Object javaLock = new Object();

    private Object nativeLock = new Object();

    private ReentrantLock reentrantLock = new ReentrantLock();

    private forceEarlyReturn005a testObject;

    public TestThread(forceEarlyReturn005a testObject) {
        this.testObject = testObject;
    }

    public void run() {
        methodForEarlyReturn();

        if (holdsLock(this)) {
            testObject.setSuccess(false);
            testObject.log().complain("Test thread didn't release lock(synchronized method): " + this);
        }
        if (holdsLock(javaLock)) {
            testObject.setSuccess(false);
            testObject.log().complain("Test thread didn't release lock(synchronized block): " + javaLock);
        }
        if (!holdsLock(nativeLock)) {
            testObject.setSuccess(false);
            testObject.log().complain("Test thread release native lock: " + nativeLock);
        }
        if (!reentrantLock.isLocked()) {
            testObject.setSuccess(false);
            testObject.log().complain("Test thread release reentrant lock: " + reentrantLock);
        }
    }

    static public final int BREAKPOINT_LINE = 83;

    static public final String BREAKPOINT_METHOD_NAME = "methodForEarlyReturn";

    public synchronized int methodForEarlyReturn() {

        forceEarlyReturn005a.nativeJNIMonitorEnter(nativeLock);

        reentrantLock.lock();

        synchronized (javaLock) {
            testObject.log().display(getName() + " in methodForEarlyReturn"); 
        }

        return 0;
    }
}

public class forceEarlyReturn005a extends AbstractJDIDebuggee {
    static {
        try {
            System.loadLibrary("forceEarlyReturn005a");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("UnsatisfiedLinkError when load library 'forceEarlyReturn005a'");
            ;
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }

        try {
            Class.forName("nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.TestThread");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException when load test thread class: " + e);
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    public static void main(String args[]) {
        new forceEarlyReturn005a().doTest(args);
    }

    static public final String testThreadName = "forceEarlyReturn004aTestThread";

    static public final String COMMAND_RUN_TEST_THREAD = "runTestThread";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_RUN_TEST_THREAD)) {
            Thread testThread = new TestThread(this);
            testThread.setName(testThreadName);
            testThread.start();

            return true;
        }

        return false;
    }

    private static final Class<?> jniError = nsk.share.TestJNIError.class;

    static native void nativeJNIMonitorEnter(Object object);

    Log log() {
        return log;
    }

    public void setSuccess(boolean value) {
        super.setSuccess(value);
    }
}
