/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.instrument.Instrumentation;

public class LockDuringDumpAgent implements Runnable {
    static boolean threadStarted = false;
    static Object lock = new Object();

    static String LITERAL = "@@LockDuringDump@@LITERAL"; 

    public static void premain(String agentArg, Instrumentation instrumentation) {
        System.out.println("inside LockDuringDumpAgent: " + LockDuringDumpAgent.class.getClassLoader());

        Thread t = new Thread(new LockDuringDumpAgent());
        t.setDaemon(true);
        t.start();

        waitForThreadStart();
    }

    static void waitForThreadStart() {
        try {
            long started = System.currentTimeMillis();
            long timeout = 10000;
            synchronized (LITERAL) {
                Thread.sleep(1);
            }
            synchronized (lock) {
                while (!threadStarted) {
                    lock.wait(timeout);
                    long elapsed = System.currentTimeMillis() - started;
                    if (elapsed >= timeout) {
                        System.out.println("This JVM may decide to not launch any Java threads during -Xshare:dump.");
                        System.out.println("This is OK because no string objects could be in a locked state during heap dump.");
                        System.out.println("LockDuringDumpAgent timeout after " + elapsed + " ms");
                        return;
                    }
                }
                System.out.println("Thread has started");
            }
        } catch (Throwable t) {
            System.err.println("Unexpected: " + t);
            throw new RuntimeException(t);
        }
    }

    public void run() {
        try {
            synchronized (LITERAL) {
                System.out.println("Let's hold the lock on the literal string \"" + LITERAL + "\" +  forever .....");
                synchronized (lock) {
                    threadStarted = true;
                    lock.notifyAll();
                }
                while (true) {
                    Thread.sleep(1);
                }
            }
        } catch (Throwable t) {
            System.err.println("Unexpected: " + t);
            throw new RuntimeException(t);
        }
    }
}
