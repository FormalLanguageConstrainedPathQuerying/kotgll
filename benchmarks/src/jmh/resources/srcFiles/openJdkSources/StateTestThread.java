/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jpda;

import nsk.share.TestBug;
import nsk.share.locks.MonitorLockingThread;

/*
 *  StateTestThread sequentially switches its state in following order:
 *  - thread not started
 *  - thread is running
 *  - thread is sleeping
 *  - thread in Object.wait()
 *  - thread wait on java monitor
 *  - thread is finished
 *
 *  To use this class create new instance of StateTestThread and sequentially call method nextState().
 */
public class StateTestThread extends Thread {
    public static String stateTestThreadStates[] = { "UNKNOWN", "RUNNING", "SLEEPING", "WAIT", "MONITOR", "ZOMBIE" };

    private Object waitOnObject = new Object();

    public StateTestThread(String name) {
        super(name);
    }

    private volatile boolean isRunning;

    private volatile boolean waitState;

    public int getCurrentState() {
        return currentState;
    }

    private MonitorLockingThread auxiliaryThread = new MonitorLockingThread(this);

    private boolean isExecutedWithErrors;

    private volatile boolean readyToBeBlocked;

    private String errorMessage;

    public void run() {
        isRunning = true;

        while (isRunning)
            ;

        try {
            sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
        }

        synchronized (waitOnObject) {
            try {
                while (waitState)
                    waitOnObject.wait();
            } catch (InterruptedException e) {
                isExecutedWithErrors = true;
                errorMessage = "StateTestThread was unexpected interrupted during waiting";
            }
        }

        auxiliaryThread.acquireLock();

        readyToBeBlocked = true;

        synchronized (this) {

        }
    }

    private int currentState = 1;

    public void nextState() {
        if (isExecutedWithErrors)
            throw new TestBug(errorMessage);

        switch (currentState++) {
        case 1:
            start();

            while (!isRunning)
                Thread.yield();

            break;
        case 2:
            isRunning = false;

            while (this.getState() != Thread.State.TIMED_WAITING)
                Thread.yield();

            break;
        case 3:
            waitState = true;

            interrupt();

            while (getState() != Thread.State.WAITING)
                Thread.yield();

            break;
        case 4:
            waitState = false;

            synchronized (waitOnObject) {
                waitOnObject.notify();
            }

            while (!readyToBeBlocked || (getState() != Thread.State.BLOCKED))
                Thread.yield();

            break;
        case 5:
            auxiliaryThread.releaseLock();
            try {
                join();
            } catch (InterruptedException e) {
                throw new TestBug("Unexpected exception: " + e);
            }
            break;

        default:
            throw new TestBug("Invalid thread state");
        }
    }
}
