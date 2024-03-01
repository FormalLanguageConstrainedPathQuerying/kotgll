/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @test    id=G1
 * @bug     6173675 8231209 8304074 8313081
 * @summary Basic test of ThreadMXBean.getThreadAllocatedBytes
 * @requires vm.gc.G1
 * @run main/othervm -XX:+UseG1GC ThreadAllocatedMemory
 */

/*
 * @test    id=Serial
 * @bug     6173675 8231209 8304074 8313081
 * @summary Basic test of ThreadMXBean.getThreadAllocatedBytes
 * @requires vm.gc.Serial
 * @run main/othervm -XX:+UseSerialGC ThreadAllocatedMemory
 */

import java.lang.management.*;

public class ThreadAllocatedMemory {
    private static com.sun.management.ThreadMXBean mbean =
        (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private static boolean testFailed = false;
    private static volatile boolean done = false;
    private static volatile boolean done1 = false;
    private static Object obj = new Object();
    private static final int NUM_THREADS = 10;
    private static Thread[] threads = new Thread[NUM_THREADS];
    private static long[] sizes = new long[NUM_THREADS];

    public static void main(String[] argv)
        throws Exception {

        testSupportEnableDisable();

        testGetCurrentThreadAllocatedBytes();
        testCurrentThreadGetThreadAllocatedBytes();

        testGetThreadAllocatedBytes();

        testGetThreadsAllocatedBytes();

        testGetTotalThreadAllocatedBytes();

        if (testFailed) {
            throw new RuntimeException("TEST FAILED");
        }

        System.out.println("Test passed");
    }

    private static void testSupportEnableDisable() {
        if (!mbean.isThreadAllocatedMemorySupported()) {
            return;
        }

        if (mbean.isThreadAllocatedMemoryEnabled()) {
            mbean.setThreadAllocatedMemoryEnabled(false);
        }

        if (mbean.isThreadAllocatedMemoryEnabled()) {
            throw new RuntimeException(
                "ThreadAllocatedMemory is expected to be disabled");
        }

        long s = mbean.getCurrentThreadAllocatedBytes();
        if (s != -1) {
            throw new RuntimeException(
                "Invalid ThreadAllocatedBytes returned = " +
                s + " expected = -1");
        }

        if (!mbean.isThreadAllocatedMemoryEnabled()) {
            mbean.setThreadAllocatedMemoryEnabled(true);
        }

        if (!mbean.isThreadAllocatedMemoryEnabled()) {
            throw new RuntimeException(
                "ThreadAllocatedMemory is expected to be enabled");
        }
    }

    private static void testGetCurrentThreadAllocatedBytes() {
        Thread curThread = Thread.currentThread();

        long size = mbean.getCurrentThreadAllocatedBytes();
        ensureValidSize(curThread, size);

        doit();

        checkResult(curThread, size,
                    mbean.getCurrentThreadAllocatedBytes());
    }

    private static void testCurrentThreadGetThreadAllocatedBytes() {
        Thread curThread = Thread.currentThread();
        long id = curThread.getId();

        long size = mbean.getThreadAllocatedBytes(id);
        ensureValidSize(curThread, size);

        doit();

        checkResult(curThread, size, mbean.getThreadAllocatedBytes(id));
    }

    private static void testGetThreadAllocatedBytes()
        throws Exception {

        done = false;
        done1 = false;
        Thread curThread = new MyThread("MyThread");
        curThread.start();
        long id = curThread.getId();

        waitUntilThreadBlocked(curThread);

        long size = mbean.getThreadAllocatedBytes(id);
        ensureValidSize(curThread, size);

        synchronized (obj) {
            done = true;
            obj.notifyAll();
        }

        goSleep(400);

        checkResult(curThread, size, mbean.getThreadAllocatedBytes(id));

        synchronized (obj) {
            done1 = true;
            obj.notifyAll();
        }

        try {
            curThread.join();
        } catch (InterruptedException e) {
            reportUnexpected(e, "during join");
        }
    }

    private static void testGetThreadsAllocatedBytes()
        throws Exception {

        done = false;
        done1 = false;
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new MyThread("MyThread-" + i);
            threads[i].start();
        }

        waitUntilThreadsBlocked();

        for (int i = 0; i < NUM_THREADS; i++) {
            sizes[i] = mbean.getThreadAllocatedBytes(threads[i].getId());
            ensureValidSize(threads[i], sizes[i]);
        }

        synchronized (obj) {
            done = true;
            obj.notifyAll();
        }

        goSleep(400);

        for (int i = 0; i < NUM_THREADS; i++) {
            checkResult(threads[i], sizes[i],
                        mbean.getThreadAllocatedBytes(threads[i].getId()));
        }

        synchronized (obj) {
            done1 = true;
            obj.notifyAll();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                reportUnexpected(e, "during join");
                break;
            }
        }
    }

    private static void testGetTotalThreadAllocatedBytes()
        throws Exception {

        Thread curThread = Thread.currentThread();
        long cumulativeSize = mbean.getTotalThreadAllocatedBytes();
        if (cumulativeSize <= 0) {
            throw new RuntimeException(
                "Invalid allocated bytes returned for " + curThread.getName() + " = " + cumulativeSize);
        }

        done = false;
        done1 = false;
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new MyThread("MyThread-" + i);
            threads[i].start();
        }

        waitUntilThreadsBlocked();

        cumulativeSize = checkResult(curThread, cumulativeSize, mbean.getTotalThreadAllocatedBytes());

        synchronized (obj) {
            done = true;
            obj.notifyAll();
        }

        goSleep(400);

        System.out.println("Done sleeping");

        cumulativeSize = checkResult(curThread, cumulativeSize, mbean.getTotalThreadAllocatedBytes());

        synchronized (obj) {
            done1 = true;
            obj.notifyAll();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                reportUnexpected(e, "during join");
                break;
            }
        }

        checkResult(curThread, cumulativeSize, mbean.getTotalThreadAllocatedBytes());
    }

    private static void ensureValidSize(Thread curThread, long size) {
        if (size < 0) {
            throw new RuntimeException(
                "Invalid allocated bytes returned for thread " +
                curThread.getName() + " = " + size);
        }
    }

    private static long checkResult(Thread curThread,
                                    long prevSize, long currSize) {
        System.out.println(curThread.getName() +
                           " Previous allocated bytes = " + prevSize +
                           " Current allocated bytes = " + currSize);
        if (currSize < prevSize) {
            throw new RuntimeException("TEST FAILED: " +
                                       curThread.getName() +
                                       " previous allocated bytes = " + prevSize +
                                       " > current allocated bytes = " + currSize);
        }
        return currSize;
    }

    private static void reportUnexpected(Exception e, String when) {
        System.out.println("Unexpected exception thrown " + when + ".");
        e.printStackTrace(System.out);
        testFailed = true;
    }

    private static void goSleep(long ms) throws Exception {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw e;
        }
    }

    private static void waitUntilThreadBlocked(Thread thread)
        throws Exception {
        while (true) {
            goSleep(100);
            ThreadInfo info = mbean.getThreadInfo(thread.getId());
            if (info.getThreadState() == Thread.State.WAITING) {
                break;
            }
        }
    }

    private static void waitUntilThreadsBlocked()
        throws Exception {
        int count = 0;
        while (count != NUM_THREADS) {
            goSleep(100);
            count = 0;
            for (int i = 0; i < NUM_THREADS; i++) {
                ThreadInfo info = mbean.getThreadInfo(threads[i].getId());
                if (info.getThreadState() == Thread.State.WAITING) {
                    count++;
                }
            }
        }
    }

    public static void doit() {
        String tmp = "";
        long hashCode = 0;
        for (int counter = 0; counter < 1000; counter++) {
            tmp += counter;
            hashCode = tmp.hashCode();
        }
        System.out.println(Thread.currentThread().getName() +
                           " hashcode: " + hashCode);
    }

    static class MyThread extends Thread {
        public MyThread(String name) {
            super(name);
        }

        public void run() {
            ThreadAllocatedMemory.doit();

            synchronized (obj) {
                while (!done) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {
                        reportUnexpected(e, "while !done");
                        break;
                    }
                }
            }

            long prevSize = mbean.getThreadAllocatedBytes(getId());
            ThreadAllocatedMemory.doit();
            long currSize = mbean.getThreadAllocatedBytes(getId());
            checkResult(this, prevSize, currSize);

            synchronized (obj) {
                while (!done1) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {
                        reportUnexpected(e, "while !done1");
                        break;
                    }
                }
            }
        }
    }
}
