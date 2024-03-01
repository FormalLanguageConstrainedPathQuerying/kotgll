/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8292217
 * @summary Test co-located events (CLE) for MethodEntry, SingleStep, and Breakpoint events.
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g CLETest.java
 * @run driver CLETest
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;

class CLEClass1 {
    public static void foo() {
    }
}
class CLEClass2 {
    public static void foo() {
    }
}

/*
 * The debuggee has a large number of breakpoints pre-setup to help control the test.
 * They are each hit just once, and in the order of their number. No instructions in the
 * debuggee are ever executed more than once.
 *
 * NOTE: the breakpoints are sensitive to the their line number within the method.
 * If that changes, then the "breakpoints" table needs to be updated.
 */
class CLEDebugee {
    public static void main(String[] args) {
        runTests();
    }

    public static void runTests() {
        test1();
        test2();
        test3(); 
        test4(); 
        test5(); 
        test6(); 
    }

    public static void test1() {
        CLEClass1.foo();  
    }
    public static void test2() {
        CLEClass2.foo();  
    }

    public static void test3() {
        int x = 1;   
    }

    public static void test4() {
        int x = 1;   
    }

    public static void test5() {
        int x = 1;    
    }

    public static void test6() {
        int x = 1;
    }
}

public class CLETest extends TestScaffold {
    ClassType targetClass;
    EventRequestManager erm;
    StepRequest stepRequest;
    MethodEntryRequest entryRequest;
    MethodExitRequest exitRequest;
    int methodEntryCount = 0;
    int breakpointCount = 0;
    boolean testcaseFailed = false;
    int testcase = 0;

    CLETest(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        CLETest cle = new CLETest(args);
        cle.startTests();
    }

    static class MethodBreakpointData {
        final String method;
        final String signature;
        final int lineNumber;
        public MethodBreakpointData(String method, String signature, int lineNumber) {
            this.method     = method;
            this.signature  = signature;
            this.lineNumber = lineNumber;
        }
    }

    static MethodBreakpointData[] breakpoints = new MethodBreakpointData[] {
        new MethodBreakpointData("runTests", "()V", 3), 
        new MethodBreakpointData("runTests", "()V", 4), 
        new MethodBreakpointData("runTests", "()V", 5), 
        new MethodBreakpointData("runTests", "()V", 6), 
        new MethodBreakpointData("test1", "()V", 1), 
        new MethodBreakpointData("test2", "()V", 1), 
        new MethodBreakpointData("test3", "()V", 1), 
        new MethodBreakpointData("test4", "()V", 1), 
        new MethodBreakpointData("test5", "()V", 1)  
    };

    public static void printStack(ThreadReference thread) {
        try {
            List<StackFrame> frames = thread.frames();
            Iterator<StackFrame> iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame frame = iter.next();
                System.out.println(getLocationString(frame.location()));
            }
        } catch (Exception e) {
            System.out.println("printStack: exception " + e);
        }
    }

    public static String getLocationString(Location loc) {
        return
            loc.declaringType().name() + "." +
            loc.method().name() + ":" +
            loc.lineNumber();
    }

    /*
     * Returns true if the specified event types are all co-located in this EventSet,
     * and no other events are included. Note that the order of the events (when present)
     * is required to be: MethodEntryEvent, StepEvent, BreakpointEvent.
     */
    public boolean isColocated(EventSet set, boolean needEntry, boolean needStep, boolean needBreakpoint) {
        int expectedSize = (needEntry ? 1 : 0) + (needStep ? 1 : 0) + (needBreakpoint ? 1 : 0);
        if (set.size() != expectedSize) {
            return false;
        }
        EventIterator iter = set.eventIterator();
        if (needEntry) {
            Event meEvent = iter.next();
            if (!(meEvent instanceof MethodEntryEvent)) {
                return false;
            }
        }
        if (needStep) {
            Event ssEvent = iter.next();
            if (!(ssEvent instanceof StepEvent)) {
                return false;
            }
        }
        if (needBreakpoint) {
            Event bpEvent = iter.next();
            if (!(bpEvent instanceof BreakpointEvent)) {
                return false;
            }
        }
        return true;
    }

    public void eventSetReceived(EventSet set) {
        System.out.println("\nEventSet for test case #" + testcase + ": " + set);
        switch (testcase) {
        case 1:
        case 2: {
            if (set.size() != 1) {
                testcaseFailed = true;
                if (testcase == 1) {
                    testFailed = true;
                }
                System.out.println("TESTCASE #" + testcase + " FAILED" +
                                   (testcase == 2 ? "(ignoring)" : "") +
                                   ": too many events in EventSet: " + set.size());
            }
            break;
        }
        case 3: {
            if (isColocated(set, true, true, true)) {
                testcaseFailed = false;
            }
            break;
        }
        case 4: {
            if (isColocated(set, true, false, true)) {
                testcaseFailed = false;
            }
            break;
        }
        case 5: {
            if (isColocated(set, false, true, true)) {
                testcaseFailed = false;
            }
            break;
        }
        case 6: {
            if (isColocated(set, true, true, false)) {
                testcaseFailed = false;
            }
            break;
        }
        }
    }

    /*
     * Most of the control flow of the test is handled via breakpoints. There is one at the start
     * of each test case that is used to enable other events that we check for during the test case.
     * In some cases there is an additional Breakpoint enabled for the test cases that is
     * also used to determine when the test case is complete. Other test cases are completed
     * when a Step or MethodEntry event arrives.
     */
    public void breakpointReached(BreakpointEvent event) {
        breakpointCount++;
        if (breakpointCount != 4 && breakpointCount != 6 && breakpointCount != 8) {
            testcase++;
        }
        System.out.println("Got BreakpointEvent(" + breakpointCount + "): " + getLocationString(event.location()));
        event.request().disable();

        if (breakpointCount == 1) {
            testcaseFailed = false; 
            entryRequest.enable();
            exitRequest.enable();
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_OVER);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            stepRequest.enable();
        }

        if (breakpointCount == 2) {
            testcaseFailed = false; 
            entryRequest.enable();
            exitRequest.enable();
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_INTO);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            stepRequest.enable();
        }

        if (breakpointCount == 3) {
            testcaseFailed = true; 
            entryRequest.enable();
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_INTO);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            stepRequest.enable();
        }
        if (breakpointCount == 4) {
            if (testcaseFailed) {
                testFailed = true;
                System.out.println("TESTCASE #3 FAILED: did not get MethodEntry, Step, and Breakpoint co-located events");
            } else {
                System.out.println("TESTCASE #3 PASSED");
            }
        }

        if (breakpointCount == 5) {
            testcaseFailed = true; 
            entryRequest.enable();
        }
        if (breakpointCount == 6) {
            entryRequest.disable();
            if (testcaseFailed) {
                testFailed = true;
                System.out.println("TESTCASE #4 FAILED: did not get MethodEntry and Breakpoint co-located events");
            } else {
                System.out.println("TESTCASE #4 PASSED");
            }
        }

        if (breakpointCount == 7) {
            testcaseFailed = true; 
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_INTO);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            stepRequest.enable();
        }
        if (breakpointCount == 8) {
            if (testcaseFailed) {
                testFailed = true;
                System.out.println("TESTCASE #5 FAILED: did not get Step and Breakpoint co-located events");
            } else {
                System.out.println("TESTCASE #5 PASSED");
            }
        }

        if (breakpointCount == 9) {
            testcaseFailed = true; 
            entryRequest.enable();
            stepRequest = erm.createStepRequest(mainThread,
                                                StepRequest.STEP_LINE,
                                                StepRequest.STEP_INTO);
            stepRequest.addCountFilter(1);
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            stepRequest.enable();
        }
    }

    public void stepCompleted(StepEvent event) {
        System.out.println("Got StepEvent: " + getLocationString(event.location()));
        event.request().disable();
        entryRequest.disable();
        if (testcase == 6 && testcaseFailed) {
            testFailed = true;
            System.out.println("TESTCASE #6 FAILED: did not get MethodEntry and Step co-located events");
        }
        if (testcase == 1 || testcase == 2 || testcase == 6) {
            exitRequest.disable();
            if (!testcaseFailed) {  
                System.out.println("TESTCASE #" + testcase + " PASSED");
            }
        }
    }

    public void methodEntered(MethodEntryEvent event) {
        System.out.println("Got MethodEntryEvent: " + getLocationString(event.location()));
        if (methodEntryCount++ == 25) {
            entryRequest.disable(); 
        }
    }

    public void methodExited(MethodExitEvent event) {
        System.out.println("Got MethodExitEvent: " + getLocationString(event.location()));
        exitRequest.disable();
        entryRequest.disable();
    }

    protected void runTests() throws Exception {
        System.out.println("Starting CLETest");
        BreakpointEvent bpe = startToMain("CLEDebugee");
        targetClass = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();
        System.out.println("Got main thread: " + mainThread);
        erm = eventRequestManager();

        try {
            for (MethodBreakpointData bpData : breakpoints) {
                Location loc = findMethodLocation(targetClass, bpData.method,
                                                  bpData.signature, bpData.lineNumber);
                BreakpointRequest req = erm.createBreakpointRequest(loc);
                req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                req.enable();
            }

            entryRequest = erm.createMethodEntryRequest();
            entryRequest.addThreadFilter(mainThread);
            entryRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);

            exitRequest = erm.createMethodExitRequest();
            exitRequest.addThreadFilter(mainThread);
            exitRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);

            System.out.println("Waiting for events: ");

            listenUntilVMDisconnect();
            System.out.println("All done...");
        } catch (Exception ex){
            ex.printStackTrace();
            testFailed = true;
        }

        if (!testFailed) {
            println("CLETest: passed");
        } else {
            throw new Exception("CLETest: failed");
        }
    }
}
