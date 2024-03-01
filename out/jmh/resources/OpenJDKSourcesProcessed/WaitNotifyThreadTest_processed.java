/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary  Call Object.wait() method. Check that monitor information
 *           presented in the stack is correct. Call notifyAll method
 *           monitor info have to disappear from the stack.
 *           Repeats the same scenario calling interrupt() method
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData WaitNotifyThreadTest
 */
import common.ToolResults;
import java.util.Iterator;
import utils.*;

public class WaitNotifyThreadTest {

    private Object monitor = new Object();
    private final String OBJECT = "a java.lang.Object";
    private final String OBJECT_WAIT = "java.lang.Object.wait0";
    private final String RUN_METHOD = "WaitNotifyThreadTest$WaitThread.run";

    interface Action {
        void doAction(Thread thread);
    }

    class ActionNotify implements Action {

        @Override
        public void doAction(Thread thread) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
            while (thread.getState() != Thread.State.TIMED_WAITING) {}
        }
    }

    class ActionInterrupt implements Action {

        @Override
        public void doAction(Thread thread) {
            thread.interrupt();
            while (thread.getState() != Thread.State.TIMED_WAITING) {}
        }
    }

    class WaitThread extends Thread {

        @Override
        public void run() {
            try {
                synchronized (monitor) {
                    monitor.wait();
                }
            } catch (InterruptedException x) {

            }
            Utils.sleep();
        }
    }

    public static void main(String[] args) throws Exception {
        new WaitNotifyThreadTest().doTest();
    }

    private void doTest() throws Exception {

        doTest(new ActionNotify());

        doTest(new ActionInterrupt());
    }

    private void doTest(Action action) throws Exception {

        final String WAITING_THREAD_NAME = "MyWaitingThread";

        WaitThread waitThread = new WaitThread();
        waitThread.setName(WAITING_THREAD_NAME);
        waitThread.start();
        while (waitThread.getState() != Thread.State.WAITING) {}

        JstackTool jstackTool = new JstackTool(ProcessHandle.current().pid());
        ToolResults results = jstackTool.measure();

        JStack jstack1 = new DefaultFormat().parse(results.getStdoutString());
        ThreadStack ti1 = jstack1.getThreadStack(WAITING_THREAD_NAME);
        analyzeThreadStackWaiting(ti1);

        action.doAction(waitThread);

        results = jstackTool.measure();

        JStack jstack2 = new DefaultFormat().parse(results.getStdoutString());
        ThreadStack ti2 = jstack2.getThreadStack(WAITING_THREAD_NAME);
        analyzeThreadStackNoWaiting(ti2);
    }

    private void analyzeThreadStackWaiting(ThreadStack ti1) {
        Iterator<MethodInfo> it = ti1.getStack().iterator();

        String monitorAddress = null;
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(OBJECT_WAIT) && mi.getCompilationUnit() == null /*native method*/) {
                if (mi.getLocks().size() == 1) {
                    MonitorInfo monInfo = mi.getLocks().getFirst();
                    monitorAddress = monInfo.getMonitorAddress();
                    assertMonitorInfo("waiting on", monInfo, monitorAddress, OBJECT_WAIT);
                } else {
                    throw new RuntimeException(OBJECT_WAIT + " method has to contain one lock record but it contains "
                                               + mi.getLocks().size());
                }
            }

            if (mi.getName().startsWith(RUN_METHOD)) {
                if (monitorAddress == null) {
                    throw new RuntimeException("Cannot found monitor info associated with " + OBJECT_WAIT + " method");
                }
                if (mi.getLocks().size() == 1) {
                    MonitorInfo monInfo = mi.getLocks().getLast();
                    if (monitorAddress.equals("no object reference available")) {
                        monitorAddress = monInfo.getMonitorAddress();
                    }
                    assertMonitorInfo("locked", monInfo, monitorAddress, RUN_METHOD);
                }
                else {
                    throw new RuntimeException(RUN_METHOD + " method has to contain one lock record but it contains "
                                               + mi.getLocks().size());
                }
            }
        }
    }

    private void assertMonitorInfo(String expectedMessage, MonitorInfo monInfo, String monitorAddress, String method) {
        if (monInfo.getType().equals(expectedMessage)
                && compareMonitorClass(monInfo)
                && monInfo.getMonitorAddress().equals(
                        monitorAddress)) {
            System.out.println("Correct monitor info found in " + method + " method");
        } else {
            System.err.println("Error: incorrect monitor info: " + monInfo.getType() + ", " + monInfo.getMonitorClass() + ", " + monInfo.getMonitorAddress());
            System.err.println("Expected: " + expectedMessage + ", a java.lang.Object, " + monitorAddress);
            throw new RuntimeException("Incorrect lock record in " + method + " method");
        }
    }

    private boolean compareMonitorClass(MonitorInfo monInfo) {
        return OBJECT.equals(monInfo.getMonitorClass()) || (monInfo.getMonitorClass() == null);
    }

    private void analyzeThreadStackNoWaiting(ThreadStack ti2) {
        Iterator<MethodInfo> it = ti2.getStack().iterator();

        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getLocks().size() != 0) {
                throw new RuntimeException("Unexpected lock record in "
                        + mi.getName() + " method");
            }
        }
    }

}
