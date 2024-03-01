/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.sampling.SP03;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class sp03t001 extends DebugeeClass {

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new sp03t001().runIt(argv, out);
    }

    /* =================================================================== */

    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static Object endingMonitor = new Object();

    static final int DEFAULT_THREADS_NUMBER = 1;
    int threadsKinds = 6;
    int threadsCount = 0;
    sp03t001Thread threads[][] = null;

    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; 

        threadsCount = argHandler.findOptionIntValue("threads", DEFAULT_THREADS_NUMBER);

        threads = new sp03t001Thread[threadsKinds][];
        for (int i = 0; i < threadsKinds; i++) {
            threads[i] = new sp03t001Thread[threadsCount];
            for (int j = 0; j < threadsCount; j++) {
                switch (i) {
                    case 0:
                        threads[i][j] = new sp03t001ThreadRunning("ThreadRunning");
                        break;
                    case 1:
                        threads[i][j] = new sp03t001ThreadEntering("ThreadEntering");
                        break;
                    case 2:
                        threads[i][j] = new sp03t001ThreadWaiting("ThreadWaiting");
                        break;
                    case 3:
                        threads[i][j] = new sp03t001ThreadSleeping("ThreadSleeping");
                        break;
                    case 4:
                        threads[i][j] = new sp03t001ThreadRunningInterrupted("ThreadRunningInterrupted");
                        break;
                    case 5:
                        threads[i][j] = new sp03t001ThreadRunningNative("ThreadRunningNative");
                        break;
                    default:
                        throw new TestBug("Unknown thread kind #" + i);
                }
            }
        }

        log.display("Starting tested threads");
        try {
            synchronized (endingMonitor) {
                for (int i = 0; i < threadsKinds; i++) {
                    for (int j = 0; j < threads[i].length; j++) {
                        synchronized (threads[i][j].startingMonitor) {
                            threads[i][j].start();
                            threads[i][j].startingMonitor.wait();
                        }
                        if (!threads[i][j].checkReady()) {
                            throw new Failure("Unable to prepare thread #"
                                                + i + "," + j + ": " + threads[i][j]);
                        }
                    }
                }

                log.display("Testing sync: threads ready");
                status = checkStatus(status);
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        } finally {
            for (int i = 0; i < threadsKinds; i++) {
                for (int j = 0; j < threads[i].length; j++) {
                    threads[i][j].letFinish();
                }
            }
        }

        log.display("Finishing tested threads");
        try {
            for (int i = 0; i < threads.length; i++) {
                for (int j = 0; j < threads[i].length; j++) {
                    threads[i][j].join();
                }
            }
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        return status;
    }
}

/* =================================================================== */

abstract class sp03t001Thread extends Thread {
    public Object startingMonitor = new Object();

    public sp03t001Thread(String name) {
        super(name);
    }

    public boolean checkReady() {
        return true;
    }

    public void letFinish() {
    }
}

/* =================================================================== */

class sp03t001ThreadRunning extends sp03t001Thread {
    private volatile boolean shouldFinish = false;

    public sp03t001ThreadRunning(String name) {
        super(name);
    }

    public void run() {
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        int i = 0;
        int n = 1000;
        while (!shouldFinish) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n = n - 1;
            }
            i = i + 1;
        }
    }

    public void letFinish() {
        shouldFinish = true;
    }
}

class sp03t001ThreadEntering extends sp03t001Thread {
    public sp03t001ThreadEntering(String name) {
        super(name);
    }

    public void run() {
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        synchronized (sp03t001.endingMonitor) {
        }
    }
}

class sp03t001ThreadWaiting extends sp03t001Thread {
    private Object waitingMonitor = new Object();

    public sp03t001ThreadWaiting(String name) {
        super(name);
    }

    public void run() {
        synchronized (waitingMonitor) {

            synchronized (startingMonitor) {
                startingMonitor.notifyAll();
            }

            try {
                waitingMonitor.wait();
            } catch (InterruptedException ignore) {
            }
        }
    }

    public boolean checkReady() {
        synchronized (waitingMonitor) {
        }
        return true;
    }

    public void letFinish() {
        synchronized (waitingMonitor) {
            waitingMonitor.notifyAll();
        }
    }
}

class sp03t001ThreadSleeping extends sp03t001Thread {
    public sp03t001ThreadSleeping(String name) {
        super(name);
    }

    public void run() {
        long timeout = 7 * 24 * 60 * 60 * 1000; 

        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        try {
            sleep(timeout);
        } catch (InterruptedException ignore) {
        }
    }

    public void letFinish() {
        interrupt();
    }
}

class sp03t001ThreadRunningInterrupted extends sp03t001Thread {
    private Object waitingMonitor = new Object();
    private volatile boolean shouldFinish = false;

    public sp03t001ThreadRunningInterrupted(String name) {
        super(name);
    }

    public void run() {
        synchronized (waitingMonitor) {
            synchronized (startingMonitor) {
                startingMonitor.notifyAll();
            }

            try {
                waitingMonitor.wait();
            } catch (InterruptedException ignore) {
            }
        }

        int i = 0;
        int n = 1000;
        while (!shouldFinish) {
            if (n <= 0) {
                n = 1000;
            }
            if (i > n) {
                i = 0;
                n = n - 1;
            }
            i = i + 1;
        }
    }

    public boolean checkReady() {
        synchronized (waitingMonitor) {
            interrupt();
        }

        return true;
    }

    public void letFinish() {
        shouldFinish = true;
    }
}

class sp03t001ThreadRunningNative extends sp03t001Thread {
    public sp03t001ThreadRunningNative(String name) {
        super(name);
    }

    public void run() {
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        nativeMethod();
    }

    public native void nativeMethod();

    public native boolean checkReady();
    public native void letFinish();
}
