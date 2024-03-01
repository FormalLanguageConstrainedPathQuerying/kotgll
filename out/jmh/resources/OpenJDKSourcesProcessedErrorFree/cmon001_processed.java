/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.stress.thread;

import java.lang.management.*;
import java.io.*;

import nsk.share.*;
import nsk.monitoring.share.*;

public class cmon001 {
    final static long CONST_BARRIER_TIME = 200;
    final static long ITERATIONS = 50;

    final static long PRECISION = 3; 

    final static long NANO_MILLI = 1000000;

    private static volatile boolean testFailed = false;
    private static Integer calculated;
    private static String calculatedSync = "abc";
    private static Object common = new Object();
    private static Object[] finishBarriers;
    private static long[] startTime;
    private static long[] endTime;
    private static long[] waitedTime;

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        ThreadMonitor monitor = Monitor.getThreadMonitor(log, argHandler);

        if (!monitor.isThreadContentionMonitoringSupported()) {
            log.display("Thread contention monitoring is not supported.");
            log.display("TEST PASSED.");
            return Consts.TEST_PASSED;
        }

        monitor.setThreadContentionMonitoringEnabled(true);

        int threadCount = argHandler.getThreadCount();
        MyThread threads[] = new MyThread[threadCount];
        finishBarriers = new Object[threadCount];
        startTime = new long[threadCount];
        endTime = new long[threadCount];
        waitedTime = new long[threadCount];

        for (int i = 0; i < threadCount; i++)
            finishBarriers[i] = new Object();

        for (int time = 0; time < ITERATIONS; time++) {
            log.display("Iteration: " + time);

            calculated = Integer.valueOf(0);

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new MyThread(i, time, log, monitor);
                threads[i].setDaemon(i % 2 == 0);
                threads[i].start();
            }

            while (calculated.intValue() < threadCount)
                Thread.currentThread().yield();
            log.display("All threads have finished calculation: " + calculated);

            for (int i = 0; i < threadCount; i++)
                synchronized (finishBarriers[i]) {
                    finishBarriers[i].notify();
                }

            for (int i = 0; i < threadCount; i++)
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            log.display("All threads have died.");


            if (calculated.intValue() != threadCount) {
                log.complain("Number of threads that accessed the variable: "
                           + calculated.intValue() + ", expected: "
                           + threadCount);
                testFailed = true;
            }

            for (int i = 0; i < threadCount; i++) {
                long liveNano = endTime[i] - startTime[i];
                long liveMilli = liveNano / NANO_MILLI;
                long leastWaitedTime = 2 * CONST_BARRIER_TIME + time;

                if (leastWaitedTime - 2 * PRECISION > waitedTime[i]) {
                    log.display("Thread " + i + " was waiting for a monitor "
                               + "for at least " + leastWaitedTime
                               + " milliseconds, but "
                               + "ThreadInfo.getWaitedTime() returned value "
                               + waitedTime[i]);
                }

                if (liveMilli + PRECISION < waitedTime[i]) {
                    log.complain("Life time of thread " + i + " is " + liveMilli
                               + " milliseconds, but "
                               + "ThreadInfo.getWaitedTime() returned value "
                               + waitedTime[i]);
                    testFailed = true;
                }
            }
        } 

        if (testFailed)
            log.complain("TEST FAILED.");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    } 

    private static class MyThread extends Thread {
        int num;
        int time;
        Log log;
        ThreadMonitor monitor;
        Object constBarrier = new Object();
        Object varBarrier = new Object();

        MyThread(int num, int time, Log log, ThreadMonitor monitor) {
            this.num = num;
            this.time = time;
            this.log = log;
            this.monitor = monitor;
        }

        public void run() {
            startTime[num] = System.nanoTime();

            synchronized (constBarrier) {
                try {
                    constBarrier.wait(CONST_BARRIER_TIME);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            synchronized (varBarrier) {
                try {
                    varBarrier.wait(CONST_BARRIER_TIME + time);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            synchronized (common) {
                synchronized (calculatedSync) {
                    calculated = Integer.valueOf(calculated.intValue() + 1);
                }
            }

            synchronized (finishBarriers[num]) {
                try {
                    finishBarriers[num].wait(10 * CONST_BARRIER_TIME);
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception");
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            }

            ThreadInfo info = monitor.getThreadInfo(this.getId(), 0);
            waitedTime[num] = info.getWaitedTime();
            endTime[num] = System.nanoTime();
        }
    } 
}
