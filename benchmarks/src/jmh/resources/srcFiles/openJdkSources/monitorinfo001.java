/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.ObjectReference.MonitorInfo;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ObjectReference.MonitorInfo.
 *
 * See monitorinfo001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class monitorinfo001 {

    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    static final String READY = "ready";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    static final String PACKAGE_NAME = "nsk.jdwp.ObjectReference.MonitorInfo";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "monitorinfo001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final int VM_CAPABILITY_NUMBER = JDWP.Capability.CAN_GET_MONITOR_INFO;
    static final String VM_CAPABILITY_NAME = "canGetMonitorInfo";

    static final String JDWP_COMMAND_NAME = "ObjectReference.MonitorInfo";
    static final int JDWP_COMMAND_ID = JDWP.Command.ObjectReference.MonitorInfo;

    static final String TESTED_CLASS_NAME =
                            DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE =
                            "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    static final String OBJECT_FIELD_NAME =
                            monitorinfo001a.OBJECT_FIELD_NAME;
    static final String MONITOR_OWNER_FIELD_NAME =
                            monitorinfo001a.MONITOR_OWNER_FIELD_NAME;
    static final String MONITOR_WAITER_FIELD_NAMES[] =
                            monitorinfo001a.MONITOR_WAITER_FIELD_NAMES;

    long ownerThreadID = 0;
    long waiterThreadIDs[] = null;

    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    IOPipe pipe = null;

    boolean success = true;


    /**
     * Start test from command line.
     */
    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    /**
     * Start JCK-compilant test.
     */
    public static int run(String argv[], PrintStream out) {
        return new monitorinfo001().runIt(argv, out);
    }


    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);

        try {
            log.display("\n>>> Preparing debugee for testing \n");

            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            pipe = debugee.createIOPipe();

            prepareDebugee();

            try {
                log.display("\n>>> Checking VM capability \n");

                log.display("Checking VM capability: " + VM_CAPABILITY_NAME);
                if (!debugee.getCapability(VM_CAPABILITY_NUMBER, VM_CAPABILITY_NAME)) {
                    out.println("TEST PASSED: unsupported VM capability: "
                                + VM_CAPABILITY_NAME);
                    return PASSED;
                }

                log.display("\n>>> Obtaining requred data from debugee \n");

                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                log.display("Getting objectID value from static field: "
                            + OBJECT_FIELD_NAME);
                long objectID = queryObjectID(classID,
                            OBJECT_FIELD_NAME, JDWP.Tag.OBJECT);
                log.display("  got objectID: " + objectID);

                queryThreadIDs(classID);

                log.display("\n>>> Testing JDWP command \n");

                log.display("Suspending all threads into debuggee");
                debugee.suspend();
                log.display("  debuggee suspended");

                testCommand(objectID);

            } finally {
                log.display("\n>>> Finishing test \n");

                log.display("resuming all threads into debuggee");
                debugee.resume();
                log.display("  debuggee resumed");

                quitDebugee();
            }

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
            success = false;
        }

        if (!success) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

    /**
     * Prepare debugee for testing and waiting for ready signal.
     */
    void prepareDebugee() {
        log.display("Waiting for VM_INIT event");
        debugee.waitForVMInit();

        log.display("Querying for IDSizes");
        debugee.queryForIDSizes();

        log.display("Resuming debugee VM");
        debugee.resume();

        log.display("Waiting for signal from debugee: " + READY);
        String signal = pipe.readln();
        log.display("Received signal from debugee: " + signal);
        if (signal == null) {
            throw new TestBug("Null signal received from debugee: " + signal
                            + " (expected: " + READY + ")");
        } else if (signal.equals(ERROR)) {
            throw new TestBug("Debugee was not able to start tested threads"
                            + " (received signal: " + signal + ")");
        } else if (!signal.equals(READY)) {
            throw new TestBug("Unexpected signal received from debugee: " + signal
                            + " (expected: " + READY + ")");
        }
    }

    /**
     * Sending debugee signal to quit and waiting for it exits.
     */
    void quitDebugee() {
        log.display("Sending signal to debugee: " + QUIT);
        pipe.println(QUIT);

        log.display("Waiting for debugee exits");
        int code = debugee.waitFor();

        if (code == JCK_STATUS_BASE + PASSED) {
            log.display("Debugee PASSED with exit code: " + code);
        } else {
            log.complain("Debugee FAILED with exit code: " + code);
            success = false;
        }
    }

    /**
     * Query debuggee for objectID value of static class field.
     */
    long queryObjectID(long classID, String fieldName, byte tag) {
        long fieldID = debugee.getClassFieldID(classID, fieldName, true);
        JDWP.Value value = debugee.getStaticFieldValue(classID, fieldID);

        if (value.getTag() != tag) {
            throw new Failure("Wrong objectID tag received from field \"" + fieldName
                            + "\": " + value.getTag() + " (expected: " + tag + ")");
        }

        long objectID = ((Long)value.getValue()).longValue();
        return objectID;
    }

    /**
     * Query debuggee for threadID values of static class fields.
     */
    void queryThreadIDs(long classID) {

        ownerThreadID = queryObjectID(classID, MONITOR_OWNER_FIELD_NAME, JDWP.Tag.THREAD);

        int count = MONITOR_WAITER_FIELD_NAMES.length;
        waiterThreadIDs = new long[count];
        for (int i = 0; i < count; i++) {
            waiterThreadIDs[i] = queryObjectID(classID,
                                    MONITOR_WAITER_FIELD_NAMES[i], JDWP.Tag.THREAD);
        }
    }

    /**
     * Perform testing JDWP command for specified objectID.
     */
    void testCommand(long objectID) {
        int expectedWaiters = waiterThreadIDs.length;
        int foundWaiters[] = new int[expectedWaiters];
        for (int i = 0; i < expectedWaiters; i++) {
            foundWaiters[i] = 0;
        }

        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  objectID: " + objectID);
        command.addObjectID(objectID);
        command.setLength();

        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            return;
        }

        ReplyPacket reply = new ReplyPacket();

        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("Reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            return;
        }

        try{
            log.display("Checking reply packet header");
            reply.checkHeader(command.getPacketID());
        } catch (BoundException e) {
            log.complain("Bad header of reply packet:\n\t" + e.getMessage());
            success = false;
            return;
        }

        log.display("Parsing reply packet:");
        reply.resetPosition();

        long owner = 0;
        try {
            owner = reply.getObjectID();
            log.display("    owner: " + owner);
        } catch (BoundException e) {
            log.complain("Unable to extract monitor owner threadID from reply packet:\n\t"
                    + e.getMessage());
            success = false;
            return;
        }

        if (owner < 0) {
            log.complain("Negative value of monitor owner threadID received: "
                        + owner);
            success = false;
        } else if (owner == 0) {
            log.complain("No monitor owner threadID received: "
                        + owner + " (expected: " + ownerThreadID + ")");
            success = false;
        } else if (owner != ownerThreadID) {
            log.complain("Unexpected monitor owner threadID received: "
                        + owner + " (expected: " + ownerThreadID + ")");
            success = false;
        }

        int entryCount = 0;
        try {
            entryCount = reply.getInt();
            log.display("  entryCount: " + entryCount);
        } catch (BoundException e) {
            log.complain("Unable to extract monitor entryCount from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        if (entryCount < 0) {
            log.complain("Negative number of monitor entryCount received: "
                        + entryCount);
            success = false;
        } else if (entryCount == 0) {
            log.complain("Zero number of monitor entryCount received: "
                        + entryCount);
            success = false;
        }

        int waiters = 0;
        try {
            waiters = reply.getInt();
            log.display("  waiters: " + waiters);
        } catch (BoundException e) {
            log.complain("Unable to extract number of monitor waiters from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        if (waiters < 0) {
            log.complain("Negative number of monitor waiters received: "
                        + waiters);
            success = false;
        }

        if (waiters != expectedWaiters) {
            log.complain("Unexpected number of monitors waiters received: "
                        + waiters + " (expected: " + expectedWaiters + ")");
            success = false;
        }

        for (int i = 0; i < waiters; i++) {

            log.display("  waiter #" + i + ":");

            long threadID = 0;
            try {
                threadID = reply.getObjectID();
                log.display("    threadID: " + threadID);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " monitor waiter threadID from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            boolean found = false;
            for (int j = 0; j < expectedWaiters; j++) {
                if (threadID == waiterThreadIDs[j]) {
                    foundWaiters[j]++;
                    found = true;
                }
            }
            if (!found) {
                log.complain("Unexpected monitor waiter threadID received: " + threadID);
                success = false;
            }

        }

        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

        for (int j = 0; j < expectedWaiters; j++) {
            if (foundWaiters[j] <= 0) {
                log.complain("Expected monitor waiter threadID NOT received: "
                            + waiterThreadIDs[j]);
                success = false;
            } else if (foundWaiters[j] > 1) {
                log.complain("Expected monitor waiter threadID (" +
                            + waiterThreadIDs[j] + ") received multiply times: "
                            + foundWaiters[j]);
                success = false;
            }
        }

    }

}
