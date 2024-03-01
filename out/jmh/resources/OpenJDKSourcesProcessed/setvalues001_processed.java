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

package nsk.jdwp.ClassType.SetValues;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ClassType.SetValues.
 *
 * See setvalues001.README for description of test execution.
 *
 * Test is executed by invoking method runIt().
 * JDWP command is tested in method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class setvalues001 {

    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    static final String READY = "ready";
    static final String RUN = "run";
    static final String DONE = "done";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    static final String PACKAGE_NAME = "nsk.jdwp.ClassType.SetValues";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "setvalues001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "ClassType.SetValues";
    static final int JDWP_COMMAND_ID = JDWP.Command.ClassType.SetValues;

    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    static final String TARGET_VALUES_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TargetValuesClass";
    static final String TARGET_VALUES_CLASS_SIGNATURE = "L" + TARGET_VALUES_CLASS_NAME.replace('.', '/') + ";";

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
        return new setvalues001().runIt(argv, out);
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
                log.display("\n>>> Obtaining requred data from debugee \n");

                log.display("Getting classID for class with target values by signature:\n"
                            + "  " + TARGET_VALUES_CLASS_SIGNATURE);
                long targetValuesClassID =
                            debugee.getReferenceTypeID(TARGET_VALUES_CLASS_SIGNATURE);
                log.display("  got classID: " + targetValuesClassID);

                log.display("Getting fieldIDs for static fields of the class");
                long targetValuesFieldIDs[] = queryClassFieldIDs(targetValuesClassID);
                log.display("  got fields: " + targetValuesFieldIDs.length);
                int count = targetValuesFieldIDs.length;

                log.display("Getting values of the static fields");
                JDWP.Value targetValues[] =
                        queryClassFieldValues(targetValuesClassID, targetValuesFieldIDs);
                log.display("  got values: " + targetValues.length);
                if (targetValues.length != count) {
                    throw new Failure("Unexpected number of static fields values received: "
                                    + targetValues.length + "(expected: " + count + ")");
                }

                log.display("Getting tested classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long testedClassID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + testedClassID);

                log.display("Getting fieldIDs for static fields of the tested class");
                long testedFieldIDs[] = queryClassFieldIDs(testedClassID);
                log.display("  got fields: " + testedFieldIDs.length);
                if (testedFieldIDs.length != count) {
                    throw new Failure("Unexpected number of static fields of tested class received: "
                                    + testedFieldIDs.length + "(expected: " + count + ")");
                }

                log.display("\n>>> Testing JDWP command \n");
                testCommand(testedClassID, testedFieldIDs, targetValues);

                log.display("\n>>> Checking that the values have been set properly \n");
                checkValuesChanged();

            } finally {
                log.display("\n>>> Finishing test \n");
                quitDebugee();
            }

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            e.printStackTrace(out);
            success = false;
        } catch (Exception e) {
            log.complain("Caught unexpected exception:\n" + e);
            e.printStackTrace(out);
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
        if (! signal.equals(READY)) {
            throw new TestBug("Unexpected signal received form debugee: " + signal
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
     * Query debugee for fieldID's of the class static fields.
     */
    long[] queryClassFieldIDs(long classID) {
        CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.Fields);
        command.addReferenceTypeID(classID);
        command.setLength();

        ReplyPacket reply = debugee.receiveReplyFor(command);

        try {
            reply.resetPosition();

            int declared = reply.getInt();
            long[] fieldIDs = new long[declared];

            for (int i = 0; i < declared; i++ ) {
                long fieldID = reply.getFieldID();
                String name = reply.getString();
                String signature = reply.getString();
                int modBits = reply.getInt();

                fieldIDs[i] = fieldID;
            }
            return fieldIDs;
        } catch (BoundException e) {
            log.complain("Unable to parse reply packet for ReferenceType.Fields command:\n\t"
                        + e);
            log.complain("Received reply packet:\n"
                        + reply);
            throw new Failure("Error occured while getting static fieldIDs for classID: " + classID);
        }
    }

    /**
     * Query debugee for values of the class static fields.
     */
    JDWP.Value[] queryClassFieldValues(long classID, long fieldIDs[]) {
        int count = fieldIDs.length;
        CommandPacket command = new CommandPacket(JDWP.Command.ReferenceType.GetValues);
        command.addReferenceTypeID(classID);
        command.addInt(count);
        for (int i = 0; i < count; i++) {
            command.addFieldID(fieldIDs[i]);
        }
        command.setLength();

        ReplyPacket reply = debugee.receiveReplyFor(command);

        try {
            reply.resetPosition();

            int valuesCount = reply.getInt();
            JDWP.Value values[] = new JDWP.Value[valuesCount];
            for (int i = 0; i < valuesCount; i++ ) {
                JDWP.Value value = reply.getValue();
                values[i] = value;
            }
            return values;
        } catch (BoundException e) {
            log.complain("Unable to parse reply packet for ReferenceType.GetValues command:\n\t"
                        + e);
            log.complain("Received reply packet:\n"
                        + reply);
            throw new Failure("Error occured while getting static fields values for classID: " + classID);
        }
    }

    /**
     * Perform testing JDWP command for specified classID.
     */
    void testCommand(long classID, long fieldIDs[], JDWP.Value values[]) {
        int count = fieldIDs.length;

        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);

        log.display("  classID: " + classID);
        command.addReferenceTypeID(classID);
        log.display("  values: " + count);
        command.addInt(count);
        for (int i = 0; i < count; i++) {
            log.display("    field #" + i +":");
            log.display("      fieldID: " + fieldIDs[i]);
            command.addFieldID(fieldIDs[i]);

            JDWP.Value value = values[i];
            JDWP.UntaggedValue untaggedValue =
                    new JDWP.UntaggedValue(value.getValue());
            log.display("      untagged_value: " + untaggedValue.getValue());
            command.addUntaggedValue(untaggedValue, value.getTag());
        }
        command.setLength();

        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n" + e);
            success = false;
            return;
        }

        ReplyPacket reply = new ReplyPacket();

        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("Reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n" + e);
            success = false;
            return;
        }

        try{
            log.display("Checking reply packet header");
            reply.checkHeader(command.getPacketID());
        } catch (BoundException e) {
            log.complain("Bad header of reply packet: " + e.getMessage());
            success = false;
        }

        log.display("Parsing reply packet:");
        reply.resetPosition();


        if (! reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + "0x" + reply.toHexString(reply.currentDataPosition(), 4));
            success = false;
        }
    }

    /**
     * Check confiramtion from debuggee that values are changed.
     */
    void checkValuesChanged() {
        log.display("Sending signal to debugee: " + RUN);
        pipe.println(RUN);

        log.display("Waiting for signal from debugee: " + DONE);
        String signal = pipe.readln();
        log.display("Received signal from debugee: " + signal);

        if (signal == null) {
            throw new TestBug("<null> signal received from debugee: " + signal
                            + " (expected: " + DONE + ")");
        } else if (signal.equals(DONE)) {
            log.display("All static fields values have been correctly set into debuggee VM");
        } else if (signal.equals(ERROR)) {
            log.complain("Not all static fields values have been correctly set into debuggee VM");
            success = false;
        } else {
            throw new TestBug("Unexpected signal received from debugee: " + signal
                            + " (expected: " + DONE + ")");
        }
    }

}
