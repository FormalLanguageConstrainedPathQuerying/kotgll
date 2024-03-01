/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4413752
 * @summary Test SuspendThread with RawMonitor enter.
 * @requires vm.jvmti
 * @library /test/lib
 * @compile SuspendWithRawMonitorEnter.java
 * @run main/othervm/native -agentlib:SuspendWithRawMonitorEnter SuspendWithRawMonitorEnter
 */

import java.io.PrintStream;


public class SuspendWithRawMonitorEnter {
    private static final String AGENT_LIB = "SuspendWithRawMonitorEnter";
    private static final int exit_delta   = 95;

    private static final int DEF_TIME_MAX = 60;    
    private static final int JOIN_MAX     = 30;    

    public static final int TS_INIT              = 1;  
    public static final int TS_BLOCKER_RUNNING   = 2;  
    public static final int TS_CONTENDER_RUNNING = 3;  
    public static final int TS_RESUMER_RUNNING   = 4;  
    public static final int TS_CALL_SUSPEND      = 5;  
    public static final int TS_DONE_BLOCKING     = 6;  
    public static final int TS_READY_TO_RESUME   = 7;  
    public static final int TS_CALL_RESUME       = 8;  
    public static final int TS_CONTENDER_DONE    = 9;  

    public static Object barrierLaunch = new Object();   
    public static Object barrierBlocker = new Object();  
    public static Object barrierResumer = new Object();  

    public static long count = 0;
    public static boolean printDebug = false;
    public volatile static int testState;

    private static void log(String msg) { System.out.println(msg); }

    native static int createRawMonitor();
    native static int destroyRawMonitor();
    native static int suspendThread(SuspendWithRawMonitorEnterWorker thr);

    public static void main(String[] args) throws Exception {
        try {
            System.loadLibrary(AGENT_LIB);
            log("Loaded library: " + AGENT_LIB);
        } catch (UnsatisfiedLinkError ule) {
            log("Failed to load library: " + AGENT_LIB);
            log("java.library.path: " + System.getProperty("java.library.path"));
            throw ule;
        }

        int timeMax = 0;
        if (args.length == 0) {
            timeMax = DEF_TIME_MAX;
        } else {
            int argIndex = 0;
            int argsLeft = args.length;
            if (args[0].equals("-p")) {
                printDebug = true;
                argIndex = 1;
                argsLeft--;
            }
            if (argsLeft == 0) {
                timeMax = DEF_TIME_MAX;
            } else if (argsLeft == 1) {
                try {
                    timeMax = Integer.parseUnsignedInt(args[argIndex]);
                } catch (NumberFormatException nfe) {
                    System.err.println("'" + args[argIndex] +
                                       "': invalid timeMax value.");
                    usage();
                }
            } else {
                usage();
            }
        }

        System.exit(run(timeMax, System.out) + exit_delta);
    }

    public static void logDebug(String mesg) {
        if (printDebug) {
            System.err.println(Thread.currentThread().getName() + ": " + mesg);
        }
    }

    public static void usage() {
        System.err.println("Usage: " + AGENT_LIB + " [-p][time_max]");
        System.err.println("where:");
        System.err.println("    -p       ::= print debug info");
        System.err.println("    time_max ::= max looping time in seconds");
        System.err.println("                 (default is " + DEF_TIME_MAX +
                           " seconds)");
        System.exit(1);
    }

    public static int run(int timeMax, PrintStream out) {
        return (new SuspendWithRawMonitorEnter()).doWork(timeMax, out);
    }

    public static void checkTestState(int exp) {
        if (testState != exp) {
            System.err.println("Failure at " + count + " loops.");
            throw new InternalError("Unexpected test state value: "
                + "expected=" + exp + " actual=" + testState);
        }
    }

    public int doWork(int timeMax, PrintStream out) {
        SuspendWithRawMonitorEnterWorker blocker;    
        SuspendWithRawMonitorEnterWorker contender;  
        SuspendWithRawMonitorEnterWorker resumer;    

        int retCode = createRawMonitor();
        if (retCode != 0) {
            throw new RuntimeException("error in JVMTI CreateRawMonitor: " +
                                       "retCode=" + retCode);
        }
        logDebug("created threadLock");

        System.out.println("About to execute for " + timeMax + " seconds.");

        long start_time = System.currentTimeMillis();
        while (System.currentTimeMillis() < start_time + (timeMax * 1000)) {
            count++;
            testState = TS_INIT;  

            synchronized (barrierLaunch) {
                blocker = new SuspendWithRawMonitorEnterWorker("blocker");
                blocker.start();

                while (testState != TS_BLOCKER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  
                    } catch (InterruptedException ex) {
                    }
                }
            }

            synchronized (barrierLaunch) {
                contender = new SuspendWithRawMonitorEnterWorker("contender");
                contender.start();

                while (testState != TS_CONTENDER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  
                    } catch (InterruptedException ex) {
                    }
                }
            }

            synchronized (barrierLaunch) {
                resumer = new SuspendWithRawMonitorEnterWorker("resumer", contender);
                resumer.start();

                while (testState != TS_RESUMER_RUNNING) {
                    try {
                        barrierLaunch.wait(0);  
                    } catch (InterruptedException ex) {
                    }
                }
            }

            checkTestState(TS_RESUMER_RUNNING);
            testState = TS_CALL_SUSPEND;
            logDebug("before suspend thread");
            retCode = suspendThread(contender);
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI SuspendThread: " +
                                           "retCode=" + retCode);
            }
            logDebug("suspended thread");

            synchronized (barrierBlocker) {
                checkTestState(TS_CALL_SUSPEND);

                testState = TS_DONE_BLOCKING;
                barrierBlocker.notify();
            }

            synchronized (barrierResumer) {
                testState = TS_READY_TO_RESUME;
                barrierResumer.notify();

            }

            try {
                blocker.join();
                resumer.join(JOIN_MAX * 1000);
                if (resumer.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("resumer thread is stuck");
                }
                contender.join(JOIN_MAX * 1000);
                if (contender.isAlive()) {
                    System.err.println("Failure at " + count + " loops.");
                    throw new InternalError("contender thread is stuck");
                }
            } catch (InterruptedException ex) {
            }

            checkTestState(TS_CONTENDER_DONE);
        }
        retCode = destroyRawMonitor();
        if (retCode != 0) {
            throw new RuntimeException("error in JVMTI DestroyRawMonitor: " +
                                       "retCode=" + retCode);
        }
        logDebug("destroyed threadLock");

        System.out.println("Executed " + count + " loops in " + timeMax +
                           " seconds.");

        return 0;
    }
}

class SuspendWithRawMonitorEnterWorker extends Thread {
    private SuspendWithRawMonitorEnterWorker target;  

    public SuspendWithRawMonitorEnterWorker(String name) {
        super(name);
    }

    public SuspendWithRawMonitorEnterWorker(String name, SuspendWithRawMonitorEnterWorker target) {
        super(name);
        this.target = target;
    }

    native static int rawMonitorEnter();
    native static int rawMonitorExit();
    native static int resumeThread(SuspendWithRawMonitorEnterWorker thr);

    public void run() {
        SuspendWithRawMonitorEnter.logDebug("thread running");

        int retCode;
        if (getName().equals("blocker")) {
            SuspendWithRawMonitorEnter.logDebug("before enter threadLock");
            retCode = rawMonitorEnter();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorEnter: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("enter threadLock");

            SuspendWithRawMonitorEnter.checkTestState(SuspendWithRawMonitorEnter.TS_INIT);

            SuspendWithRawMonitorEnter.logDebug("before recursive enter threadLock");
            retCode = rawMonitorEnter();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorEnter: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("recursive enter threadLock");

            SuspendWithRawMonitorEnter.logDebug("before recursive exit threadLock");
            retCode = rawMonitorExit();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorExit: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("recursive exit threadLock");

            synchronized(SuspendWithRawMonitorEnter.barrierBlocker) {
                synchronized(SuspendWithRawMonitorEnter.barrierLaunch) {
                    SuspendWithRawMonitorEnter.testState = SuspendWithRawMonitorEnter.TS_BLOCKER_RUNNING;
                    SuspendWithRawMonitorEnter.barrierLaunch.notify();
                }
                SuspendWithRawMonitorEnter.logDebug("thread waiting");
                while (SuspendWithRawMonitorEnter.testState != SuspendWithRawMonitorEnter.TS_DONE_BLOCKING &&
                       SuspendWithRawMonitorEnter.testState != SuspendWithRawMonitorEnter.TS_READY_TO_RESUME) {
                    try {
                        SuspendWithRawMonitorEnter.barrierBlocker.wait(0);
                    } catch (InterruptedException ex) {
                    }
                }
                SuspendWithRawMonitorEnter.logDebug("before exit threadLock");
                retCode = rawMonitorExit();
                if (retCode != 0) {
                    throw new RuntimeException("error in JVMTI RawMonitorExit: "
                                               + "retCode=" + retCode);
                }
                SuspendWithRawMonitorEnter.logDebug("exit threadLock");
            }
        }
        else if (getName().equals("contender")) {
            synchronized(SuspendWithRawMonitorEnter.barrierLaunch) {
                SuspendWithRawMonitorEnter.testState = SuspendWithRawMonitorEnter.TS_CONTENDER_RUNNING;
                SuspendWithRawMonitorEnter.barrierLaunch.notify();
            }

            SuspendWithRawMonitorEnter.logDebug("before enter threadLock");
            retCode = rawMonitorEnter();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorEnter: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("enter threadLock");

            SuspendWithRawMonitorEnter.checkTestState(SuspendWithRawMonitorEnter.TS_CALL_RESUME);
            SuspendWithRawMonitorEnter.testState = SuspendWithRawMonitorEnter.TS_CONTENDER_DONE;

            SuspendWithRawMonitorEnter.logDebug("before exit threadLock");
            retCode = rawMonitorExit();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorExit: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("exit threadLock");
        }
        else if (getName().equals("resumer")) {
            synchronized(SuspendWithRawMonitorEnter.barrierResumer) {
                synchronized(SuspendWithRawMonitorEnter.barrierLaunch) {
                    SuspendWithRawMonitorEnter.testState = SuspendWithRawMonitorEnter.TS_RESUMER_RUNNING;
                    SuspendWithRawMonitorEnter.barrierLaunch.notify();
                }
                SuspendWithRawMonitorEnter.logDebug("thread waiting");
                while (SuspendWithRawMonitorEnter.testState != SuspendWithRawMonitorEnter.TS_READY_TO_RESUME) {
                    try {
                        SuspendWithRawMonitorEnter.barrierResumer.wait(0);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            SuspendWithRawMonitorEnter.logDebug("before enter threadLock");
            retCode = rawMonitorEnter();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorEnter: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("enter threadLock");

            SuspendWithRawMonitorEnter.checkTestState(SuspendWithRawMonitorEnter.TS_READY_TO_RESUME);
            SuspendWithRawMonitorEnter.testState = SuspendWithRawMonitorEnter.TS_CALL_RESUME;

            SuspendWithRawMonitorEnter.logDebug("before resume thread");
            retCode = resumeThread(target);
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI ResumeThread: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("resumed thread");

            SuspendWithRawMonitorEnter.logDebug("before exit threadLock");
            retCode = rawMonitorExit();
            if (retCode != 0) {
                throw new RuntimeException("error in JVMTI RawMonitorExit: " +
                                           "retCode=" + retCode);
            }
            SuspendWithRawMonitorEnter.logDebug("exit threadLock");
        }
    }
}
