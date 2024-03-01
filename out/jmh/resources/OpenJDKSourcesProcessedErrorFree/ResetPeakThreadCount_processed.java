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

/*
 * @test
 * @bug     4892507 8020875 8021335
 * @summary Basic Test for the following reset methods:
 *          - ThreadMXBean.resetPeakThreadCount()
 * @author  Mandy Chung
 * @author  Jaroslav Bachorik
 *
 * @build ResetPeakThreadCount
 * @build ThreadDump
 * @run main/othervm ResetPeakThreadCount
 */

import java.lang.management.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ResetPeakThreadCount {
    private static final int DAEMON_THREADS_1 = 80;

    private static final int TERMINATE_1 = 40;

    private static final int DAEMON_THREADS_2 = 20;

    private static final int DAEMON_THREADS_3 = 20;

    private static final Barrier barrier = new Barrier(DAEMON_THREADS_1);

    private static final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    private static volatile boolean testFailed = false;

    private static final List<MyThread> threads = new LinkedList<>();
    private static final Object liveSync = new Object();

    public static void main(String[] argv) throws Exception {
        resetPeak();

        startThreads(DAEMON_THREADS_1);

        int beforeTerminate = checkPeakThreadCount(threads.size() + 1, -1); 

        terminateThreads(TERMINATE_1);

        int afterTerminate = checkPeakThreadCount(beforeTerminate, -1);

        startThreads(DAEMON_THREADS_2);

        checkPeakThreadCount(-1, afterTerminate);

        int beforeThreads3 = resetPeak();
        startThreads(DAEMON_THREADS_3);

        checkPeakThreadCount(threads.size() + 1, -1); 
        checkPeakThreadCount(beforeThreads3, -1);

        if (testFailed) {
            throw new RuntimeException("TEST FAILED.");
        }

        System.out.println("Test passed");
    }

    private static void startThreads(int count) throws InterruptedException {
        int peak1 = mbean.getPeakThreadCount();

        System.out.println("Starting " + count + " threads....");
        barrier.set(count);
        synchronized (liveSync) {
            for (int i = 0; i < count; i++) {
                MyThread newThread = new MyThread();
                threads.add(newThread);
                newThread.start();
            }
        }
        barrier.await();

        int peak2 = mbean.getPeakThreadCount();

        System.out.println("   Current = " + mbean.getThreadCount() +
            " Peak before = " + peak1 + " after: " + peak2);

        int current = mbean.getThreadCount();
        System.out.println("   Live thread count before returns " + current);
    }

    private static void terminateThreads(int count) throws InterruptedException {
        int peak1 = mbean.getPeakThreadCount();

        System.out.println("Terminating " + count + " threads....");
        barrier.set(count);
        synchronized(liveSync) {
            Iterator<MyThread> iter = threads.iterator();
            for (int i = 0; i < count; i++) {
                MyThread thread = iter.next();
                thread.live = false;
            }
            liveSync.notifyAll();
        }
        barrier.await();

        int peak2 = mbean.getPeakThreadCount();
        if (peak2 != peak1) {
            throw new RuntimeException("Current Peak = " + peak2 +
                " Expected to be = previous peak = " + peak1);
        }

        for (int i = 0; i < count; i++) {
            MyThread thread = threads.remove(0);
            thread.join();
        }

        int current = mbean.getThreadCount();
        System.out.println("   Live thread count before returns " + current);
    }

    private static int resetPeak() {
        int peak3 = mbean.getPeakThreadCount();
        int current = mbean.getThreadCount();

        mbean.resetPeakThreadCount();

        int afterResetPeak = mbean.getPeakThreadCount();
        int afterResetCurrent = mbean.getThreadCount();
        System.out.println("Reset peak before = " + peak3 +
            " current = " + current +
            " after reset peak = " + afterResetPeak +
            " current = " + afterResetCurrent);
        return afterResetPeak;
    }

    private static void fail(String msg) {
        ThreadDump.threadDump();
        throw new RuntimeException(msg);
    }

    private static int checkPeakThreadCount(int min, int max) {
        int value = mbean.getPeakThreadCount();
        if (min > 0 && value < min) {
            fail("***** Unexpected thread count: " + value + ", minimum expected " + min + " *****");
        }
        if (max > 0 && value > max) {
            fail("***** Unexpected thread count: " + value + ", maximum expected " + max + " *****");
        }
        return value;
    }


    private static class MyThread extends Thread {
        volatile boolean live;

        MyThread() {
            live = true;
            setDaemon(true);
        }

        public void run() {
            barrier.signal();
            synchronized(liveSync) {
                while (live) {
                    try {
                        liveSync.wait(100);
                    } catch (InterruptedException e) {
                        System.out.println("Unexpected exception is thrown.");
                        e.printStackTrace(System.out);
                        testFailed = true;
                    }
                }
            }
            barrier.signal();
        }
    }

}
