/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.locks;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.TestBug;
import nsk.share.TestJNIError;
import nsk.share.Wicket;

/*
 Thread with possibility acquiring monitors in different ways:
 - entering synchronized method
 - entering synchronized method for thread object itself
 - entering synchronized static method
 - entering synchronized method for thread class itself
 - entering synchronized block on non-static object
 - entering synchronized block on non-static on thread object itself
 - entering synchronized block on static object
 - entering synchronized block on static thread object itself
 - JNI MonitorEnter.

 Description of required thread stack should be passed to LockingThread in constructor.
 When started locking thread create required stack and sleep until not interrupted.

 LockingThread can relinquish acquired monitors in follows ways:
 - relinquish single monitor through Object.wait - relinquishMonitor(int monitorIndex),
 - relinquish single monitor through exiting from synchronized blocks/methods or through JNI MonitorExit  - exitSingleFrame(),
 - relinquish all monitors(exit from all synchronized blocks/methods) - stopLockingThread()

 Debug information about each acquired/relinquished monitor is stored and can be obtained through getMonitorsInfo().

 To be sure that LockingThread have reached required state call method LockingThread.waitState().

 Usage example:

 List<String> stackFramesDescription = new ArrayList<String>();
 stackFramesDescription.add(LockingThread.SYNCHRONIZED_METHOD);
 stackFramesDescription.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);

 LockingThread lockingThread = new LockingThread(log, stackFramesDescription);

 lockingThread.start();

  lockingThread.waitState();

  lockingThread.exitSingleFrame();

   lockingThread.waitState();
   */
public class LockingThread extends Thread {
    private static final Class<?> jniErrorKlass = TestJNIError.class;
    static {
        try {
            System.loadLibrary("LockingThread");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Unexpected UnsatisfiedLinkError on loading library 'LockingThread'");
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    /*
     *  Information about acquired monitor
     */
    public static class DebugMonitorInfo {
        public DebugMonitorInfo(Object monitor, int stackDepth, Thread thread, boolean isNative) {
            this.monitor = monitor;
            this.stackDepth = stackDepth;
            this.thread = thread;
            this.isNative = isNative;
        }

        public Object monitor;

        public int stackDepth;

        public Thread thread;

        boolean isNative;
    }

    public static final String JNI_MONITOR_ENTER = "JNI_MONITOR_ENTER";

    public static final String SYNCHRONIZED_STATIC_METHOD = "SYNCHRONIZED_STATIC_METHOD";

    public static final String SYNCHRONIZED_STATIC_THREAD_METHOD = "SYNCHRONIZED_STATIC_THREAD_METHOD";

    public static final String SYNCHRONIZED_METHOD = "SYNCHRONIZED_METHOD";

    public static final String SYNCHRONIZED_THREAD_METHOD = "SYNCHRONIZED_THREAD_METHOD";

    public static final String SYNCHRONIZED_THIS_BLOCK = "SYNCHRONIZED_THIS_BLOCK";

    public static final String SYNCHRONIZED_OBJECT_BLOCK = "SYNCHRONIZED_OBJECT_BLOCK";

    public static final String SYNCHRONIZED_BLOCK_STATIC_OBJECT = "SYNCHRONIZED_BLOCK_STATIC_OBJECT";

    public static final String SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT = "SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT";

    public static final String FRAME_WITHOUT_LOCK = "FRAME_WITHOUT_LOCK";

    private List<DebugMonitorInfo> monitorsInfo = new ArrayList<DebugMonitorInfo>();

    private List<String> stackFramesDescription;

    private Log log;

    private boolean executedWithErrors;

    public boolean isExecutedWithErrors() {
        return executedWithErrors;
    }

    public LockingThread(Log log, List<String> stackFramesDescription) {
        this.log = log;
        this.stackFramesDescription = stackFramesDescription;
    }

    public DebugMonitorInfo[] getMonitorsInfo(boolean returnJNIMonitors) {
        Map<Object, DebugMonitorInfo> result = new HashMap<Object, DebugMonitorInfo>();

        for (int i = monitorsInfo.size() - 1; i >= 0; i--) {
            DebugMonitorInfo monitorInfo = monitorsInfo.get(i);

            if ((returnJNIMonitors || !monitorInfo.isNative) &&

                    (monitorInfo.monitor != relinquishedMonitor) &&

                    !result.containsKey(monitorInfo.monitor)) {
                result.put(monitorInfo.monitor, monitorInfo);
            }
        }

        return result.values().toArray(new DebugMonitorInfo[] {});
    }

    void log(String message) {
        log.display(Thread.currentThread().getName() + ": " + message);
    }

    void addMonitorInfo(DebugMonitorInfo monitorInfo) {
        monitorsInfo.add(monitorInfo);
    }

    void removeMonitorInfo(DebugMonitorInfo removedMonitor) {
        for (DebugMonitorInfo monitor : monitorsInfo) {
            if (monitor.stackDepth > removedMonitor.stackDepth)
                monitor.stackDepth -= 2;
        }

        monitorsInfo.remove(removedMonitor);
    }

    private int currentIndex;








    void createStackFrame() {
        if (currentIndex < stackFramesDescription.size()) {
            String frameDescription = stackFramesDescription.get(currentIndex);

            currentIndex++;

            if (frameDescription.equals(JNI_MONITOR_ENTER)) {
                int currentStackDepth = -1;
                Object object = new Object();
                DebugMonitorInfo monitorInfo = new DebugMonitorInfo(object, currentStackDepth, this, true);
                addMonitorInfo(monitorInfo);
                log("Enter JNI monitor");
                nativeJNIMonitorEnter(object);
                log("Exit JNI monitor");
                removeMonitorInfo(monitorInfo);
            } else if (frameDescription.equals(SYNCHRONIZED_BLOCK_STATIC_OBJECT)) {
                synchronizedBlockStaticObject();
            } else if (frameDescription.equals(SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT)) {
                synchronizedBlockStaticThreadObject();
            } else if (frameDescription.equals(SYNCHRONIZED_METHOD)) {
                new ClassWithSynchronizedMethods().synchronizedMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_THREAD_METHOD)) {
                synchronizedMethod();
            } else if (frameDescription.equals(SYNCHRONIZED_STATIC_METHOD)) {
                ClassWithSynchronizedMethods.synchronizedStaticMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_STATIC_THREAD_METHOD)) {
                synchronizedStaticMethod(this);
            } else if (frameDescription.equals(SYNCHRONIZED_THIS_BLOCK)) {
                synchronizedThisBlock();
            } else if (frameDescription.equals(SYNCHRONIZED_OBJECT_BLOCK)) {
                synchronizedObjectBlock();
            } else if (frameDescription.equals(FRAME_WITHOUT_LOCK)) {
                frameWithoutLock();
            } else
                throw new TestBug("Invalid stack frame description: " + frameDescription);
        } else {
            ready();
            doWait();
        }

        if (exitSingleFrame) {
            if (currentIndex-- < stackFramesDescription.size()) {
                ready();
                doWait();
            }
        }
    }

    public Object getRelinquishedMonitor() {
        return relinquishedMonitor;
    }

    private Object relinquishedMonitor;

    private Wicket waitStateWicket = new Wicket();

    private Thread.State requiredState;

    public void waitState() {
        int result = waitStateWicket.waitFor(60000);

        if (result != 0) {
            throw new TestBug("Locking thread can't reach required state (waitStateWicket wasn't unlocked)");
        }

        if (requiredState == null)
            throw new TestBug("Required state not specified");

        long startTime = System.currentTimeMillis();

        while (this.getState() != requiredState) {

            if ((System.currentTimeMillis() - startTime) > 60000) {
                throw new TestBug("Locking thread can't reach required state (state: " + requiredState + " wasn't reached) in 1 minute");
            }

            Thread.yield();
        }

        requiredState = null;

        Object relinquishedMonitor = getRelinquishedMonitor();
        /*
         * Changing thread state and release of lock is not single/atomic operation.
         * As result there is a potential race when thread state (LockingThread) has
         * been changed but the lock has not been released yet. To avoid this current
         * thread is trying to acquire the same monitor, so current thread proceeds
         * execution only when monitor has been really relinquished by LockingThread.
         */
        if (relinquishedMonitor != null) {
            synchronized (relinquishedMonitor) {
            }
        }
    }

    private void ready() {
        waitStateWicket.unlockAll();
    }

    private volatile boolean relinquishMonitor;

    private int relinquishedMonitorIndex;

    public void relinquishMonitor(int index) {
        if (index >= monitorsInfo.size()) {
            throw new TestBug("Invalid monitor index: " + index);
        }

        requiredState = Thread.State.WAITING;
        waitStateWicket = new Wicket();
        relinquishMonitor = true;
        relinquishedMonitorIndex = index;

        interrupt();

        DebugMonitorInfo monitorInfo = monitorsInfo.get(relinquishedMonitorIndex);

        if (monitorInfo == null)
            throw new TestBug("Invalid monitor index: " + relinquishedMonitorIndex);
    }

    public void acquireRelinquishedMonitor() {
        if (relinquishedMonitor == null) {
            throw new TestBug("There is no relinquished monitor");
        }

        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        relinquishMonitor = false;

        synchronized (relinquishedMonitor) {
            relinquishedMonitor.notifyAll();
        }
    }

    public void stopLockingThread() {
        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        exitSingleFrame = false;

        interrupt();
    }

    private boolean exitSingleFrame;

    public void exitSingleFrame() {
        requiredState = Thread.State.TIMED_WAITING;

        waitStateWicket = new Wicket();
        exitSingleFrame = true;

        interrupt();
    }

    private void doWait() {
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }

            if (relinquishMonitor) {
                try {
                    DebugMonitorInfo monitorInfo = monitorsInfo.get(relinquishedMonitorIndex);

                    if (monitorInfo == null)
                        throw new TestBug("Invalid monitor index: " + relinquishedMonitorIndex);

                    relinquishedMonitor = monitorInfo.monitor;

                    log("Relinquish monitor: " + relinquishedMonitor);

                    ready();


                    while (relinquishMonitor)
                        relinquishedMonitor.wait(0);

                    log("Acquire relinquished monitor: " + relinquishedMonitor);
                } catch (Exception e) {
                    executedWithErrors = true;
                    log("Unexpected exception: " + e);
                    e.printStackTrace(log.getOutStream());
                }

                relinquishedMonitor = null;

                ready();
            } else
                break;
        }
    }

    public void run() {
        requiredState = Thread.State.TIMED_WAITING;

        createStackFrame();

        ready();
        doWait();
    }

    static synchronized void synchronizedStaticMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized static thread method");

        DebugMonitorInfo monitorInfo = new DebugMonitorInfo(LockingThread.class, currentStackDepth, lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized static thread method");
    }

    int expectedDepth() {
        return (stackFramesDescription.size() - currentIndex) * 2 + 3;
    }

    private native void nativeJNIMonitorEnter(Object object);

    synchronized void synchronizedMethod() {
        int currentStackDepth = expectedDepth();

        log("Enter synchronized thread method");

        DebugMonitorInfo monitorInfo = new DebugMonitorInfo(this, currentStackDepth, this, false);
        addMonitorInfo(monitorInfo);
        createStackFrame();
        removeMonitorInfo(monitorInfo);

        log("Exit synchronized thread method");
    }

    void synchronizedThisBlock() {
        int currentStackDepth = expectedDepth();

        log("Enter synchronized(this) block");

        synchronized (this) {
            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(this, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(this) block");
    }

    private static Object staticObject;

    private static ReentrantLock staticObjectInitializingLock = new ReentrantLock();

    void synchronizedBlockStaticObject() {
        int currentStackDepth = expectedDepth();

        staticObjectInitializingLock.lock();

        staticObject = new Object();

        log("Enter synchronized(static object) block");

        synchronized (staticObject) {
            staticObjectInitializingLock.unlock();

            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(staticObject, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(static object) block");
    }

    private static LockingThread staticLockingThread;

    void synchronizedBlockStaticThreadObject() {
        int currentStackDepth = expectedDepth();

        staticObjectInitializingLock.lock();

        staticLockingThread = this;

        log("Enter synchronized(static thread object) block");

        synchronized (staticLockingThread) {
            staticObjectInitializingLock.unlock();

            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(staticLockingThread, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(static thread object) block");
    }

    void synchronizedObjectBlock() {
        int currentStackDepth = expectedDepth();

        Object object = new Object();

        log("Enter synchronized(object) block");

        synchronized (object) {
            DebugMonitorInfo monitorInfo = new DebugMonitorInfo(object, currentStackDepth, this, false);
            addMonitorInfo(monitorInfo);
            createStackFrame();
            removeMonitorInfo(monitorInfo);
        }

        log("Exit synchronized(object) block");
    }

    private void frameWithoutLock() {
        log("Enter frameWithoutLock");

        createStackFrame();

        for (DebugMonitorInfo monitor : monitorsInfo)
            monitor.stackDepth -= 2;

        log("Exit frameWithoutLock");
    }
}

class ClassWithSynchronizedMethods {
    public synchronized void synchronizedMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized method");

        LockingThread.DebugMonitorInfo monitorInfo = new LockingThread.DebugMonitorInfo(this, currentStackDepth, lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized method");
    }

    public static synchronized void synchronizedStaticMethod(LockingThread lockingThread) {
        int currentStackDepth = lockingThread.expectedDepth();

        lockingThread.log("Enter synchronized static method");

        LockingThread.DebugMonitorInfo monitorInfo = new LockingThread.DebugMonitorInfo(ClassWithSynchronizedMethods.class, currentStackDepth,
                lockingThread, false);
        lockingThread.addMonitorInfo(monitorInfo);
        lockingThread.createStackFrame();
        lockingThread.removeMonitorInfo(monitorInfo);

        lockingThread.log("Exit synchronized static method");
    }
}
