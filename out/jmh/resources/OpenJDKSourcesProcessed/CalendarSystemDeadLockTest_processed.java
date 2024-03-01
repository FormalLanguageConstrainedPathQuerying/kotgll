/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @test
 * @bug 8273790
 * @summary Verify that concurrent classloading of sun.util.calendar.Gregorian and
 * sun.util.calendar.CalendarSystem doesn't lead to a deadlock
 * @modules java.base/sun.util.calendar:open
 * @run main/othervm CalendarSystemDeadLockTest
 * @run main/othervm CalendarSystemDeadLockTest
 * @run main/othervm CalendarSystemDeadLockTest
 * @run main/othervm CalendarSystemDeadLockTest
 * @run main/othervm CalendarSystemDeadLockTest
 */
public class CalendarSystemDeadLockTest {

    public static void main(final String[] args) throws Exception {
        testConcurrentClassLoad();
    }

    /**
     * Loads {@code sun.util.calendar.Gregorian} and {@code sun.util.calendar.CalendarSystem}
     * and invokes {@code sun.util.calendar.CalendarSystem#getGregorianCalendar()} concurrently
     * in a thread of their own and expects the classloading of both those classes
     * to succeed. Additionally, after these tasks are done, calls the
     * sun.util.calendar.CalendarSystem#getGregorianCalendar() and expects it to return a singleton
     * instance
     */
    private static void testConcurrentClassLoad() throws Exception {
        final int numTasks = 7;
        final CountDownLatch taskTriggerLatch = new CountDownLatch(numTasks);
        final List<Callable<?>> tasks = new ArrayList<>();
        tasks.add(new ClassLoadTask("sun.util.calendar.Gregorian", taskTriggerLatch));
        tasks.add(new ClassLoadTask("sun.util.calendar.CalendarSystem", taskTriggerLatch));
        tasks.add(new ClassLoadTask("java.util.GregorianCalendar", taskTriggerLatch));
        tasks.add(new ClassLoadTask("java.util.Date", taskTriggerLatch));
        tasks.add(new ClassLoadTask("java.util.JapaneseImperialCalendar", taskTriggerLatch));
        tasks.add(new GetGregorianCalTask(taskTriggerLatch));
        tasks.add(new GetGregorianCalTask(taskTriggerLatch));
        if (numTasks != tasks.size()) {
            throw new RuntimeException("Test setup failure - unexpected number of tasks " + tasks.size()
                    + ", expected " + numTasks);
        }
        final ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            final Future<?>[] results = new Future[tasks.size()];
            int i = 0;
            for (final Callable<?> task : tasks) {
                results[i++] = executor.submit(task);
            }
            for (i = 0; i < tasks.size(); i++) {
                results[i].get();
            }
        } finally {
            executor.shutdownNow();
        }
        final Object gCal = callCalSystemGetGregorianCal();
        if (gCal == null) {
            throw new RuntimeException("sun.util.calendar.CalendarSystem#getGregorianCalendar()" +
                    " unexpectedly returned null");
        }
        if (GetGregorianCalTask.instances.size() != 2) {
            throw new RuntimeException("Unexpected number of results from call " +
                    "to sun.util.calendar.CalendarSystem#getGregorianCalendar()");
        }
        if ((gCal != GetGregorianCalTask.instances.get(0)) || (gCal != GetGregorianCalTask.instances.get(1))) {
            throw new RuntimeException("sun.util.calendar.CalendarSystem#getGregorianCalendar()" +
                    " returned different instances");
        }
    }

    /**
     * Reflectively calls sun.util.calendar.CalendarSystem#getGregorianCalendar() and returns
     * the result
     */
    private static Object callCalSystemGetGregorianCal() throws Exception {
        final Class<?> k = Class.forName("sun.util.calendar.CalendarSystem");
        return k.getDeclaredMethod("getGregorianCalendar").invoke(null);
    }

    private static class ClassLoadTask implements Callable<Class<?>> {
        private final String className;
        private final CountDownLatch latch;

        private ClassLoadTask(final String className, final CountDownLatch latch) {
            this.className = className;
            this.latch = latch;
        }

        @Override
        public Class<?> call() {
            System.out.println(Thread.currentThread().getName() + " loading " + this.className);
            try {
                latch.countDown();
                latch.await();
                return Class.forName(this.className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class GetGregorianCalTask implements Callable<Object> {
        private static final List<Object> instances = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch latch;

        private GetGregorianCalTask(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Object call() {
            System.out.println(Thread.currentThread().getName()
                    + " calling  sun.util.calendar.CalendarSystem#getGregorianCalendar()");
            try {
                latch.countDown();
                latch.await();
                final Object inst = callCalSystemGetGregorianCal();
                instances.add(inst);
                return inst;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
