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

package nsk.jdwp.ThreadReference.Frames;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: ThreadReference.Frames.
 *
 * See frames001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #testCommand()
 */
public class frames001 {

    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    static final String READY = "ready";
    static final String ERROR = "error";
    static final String QUIT = "quit";

    static final String PACKAGE_NAME = "nsk.jdwp.ThreadReference.Frames";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "frames001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "ThreadReference.Frames";
    static final int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.Frames;

    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    static final String TESTED_CLASS_FIELD_NAME = frames001a.FIELD_NAME;
    static final String TESTED_THREAD_NAME = frames001a.THREAD_NAME;

    static final String TESTED_METHOD_NAME = frames001a.METHOD_NAME;
    static final String RUN_METHOD_NAME = "run";

    static final int START_FRAME_INDEX = 2;
    static final int FRAMES_COUNT = frames001a.FRAMES_COUNT - START_FRAME_INDEX;

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
        return new frames001().runIt(argv, out);
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

            long threadID = 0;
            try {
                log.display("\n>>> Obtaining requred data from debugee \n");

                log.display("Getting classID by signature:\n"
                            + "  " + TESTED_CLASS_SIGNATURE);
                long classID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);
                log.display("  got classID: " + classID);

                log.display("Getting methodID by name: " + TESTED_METHOD_NAME);
                long methodID = debugee.getMethodID(classID, TESTED_METHOD_NAME, true);
                log.display("  got methodID: " + methodID);

                log.display("Getting methodID by name: " + RUN_METHOD_NAME);
                long runMethodID = debugee.getMethodID(classID, RUN_METHOD_NAME, true);
                log.display("  got methodID: " + runMethodID);

                log.display("Getting threadID value from static field: "
                            + TESTED_CLASS_FIELD_NAME);
                threadID = queryThreadID(classID, TESTED_CLASS_FIELD_NAME);
                log.display("  got threadID: " + threadID);

                log.display("Suspending thread into debuggee for threadID: " + threadID);
                debugee.suspendThread(threadID);
                log.display("  thread suspended");

                log.display("\n>>> Testing JDWP command \n");
                testCommand(threadID, methodID, runMethodID, classID);

            } finally {
                log.display("\n>>> Finishing test \n");

                if (threadID != 0) {
                    log.display("Resuming suspended thread");
                    debugee.resumeThread(threadID);
                }

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
            throw new TestBug("Debugee was not able to start tested thread"
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
     * Query debuggee for threadID value of statuic field of the class.
     */
    long queryThreadID(long classID, String fieldName) {
        long fieldID = debugee.getClassFieldID(classID, fieldName, true);
        JDWP.Value value = debugee.getStaticFieldValue(classID, fieldID);

        if (value.getTag() != JDWP.Tag.THREAD) {
            throw new Failure("Not threadID value returned from debuggee: " + value);
        }

        long threadID = ((Long)value.getValue()).longValue();
        return threadID;
    }

    /**
     * Perform testing JDWP command for specified threadID.
     */
    void testCommand(long threadID, long methodID, long runMethodID, long classID) {
        log.display("Create command packet:");
        log.display("Command: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("  threadID: " + threadID);
        command.addObjectID(threadID);
        log.display("  startFrame: " + START_FRAME_INDEX);
        command.addInt(START_FRAME_INDEX);
        log.display("  length: " + FRAMES_COUNT);
        command.addInt(FRAMES_COUNT);
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

        int frames = 0;
        try {
            frames = reply.getInt();
            log.display("  frames: " + frames);
        } catch (BoundException e) {
            log.complain("Unable to extract number of frames from reply packet:\n\t"
                        + e.getMessage());
            success = false;
            return;
        }

        if (frames < 0) {
            log.complain("Negative value of frames count in reply packet: "
                        + frames);
            success = false;
        }

        if (frames != FRAMES_COUNT) {
            log.complain("Unexpected number of frames returned: "
                        + frames + " (expected: " + FRAMES_COUNT + ")");
            success = false;
        }

        long checkedMethodID = methodID;

        for (int i = 0; i < frames; i++) {

            log.display("  frame #" + i + ":");

            long frameID = 0;
            try {
                frameID = reply.getFrameID();
                log.display("    frameID: " + frameID);
            } catch (BoundException e) {
                log.complain("Unable to extract " + i + " frameID from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            JDWP.Location location = null;
            try {
                location = reply.getLocation();
                log.display("    location: " + location);
            } catch (BoundException e) {
                e.printStackTrace(log.getOutStream());
                log.complain("Unable to extract " + i + " frame location from reply packet:\n\t"
                        + e.getMessage());
                success = false;
                return;
            }

            if (frameID < 0) {
                log.complain("Negative value of " + i + " frameID: "
                            + frameID);
                success = false;
            }

            if (location.getTag() != JDWP.TypeTag.CLASS) {
                log.complain("Unexpected type tag of " + i + " frame location: "
                            + location.getTag() + "(expected: " + JDWP.TypeTag.CLASS + ")");
                success = false;
            }

            if (location.getClassID() != classID) {
                log.complain("Unexpected classID of " + i + " frame location: "
                            + location.getClassID() + "(expected: " + classID + ")");
                success = false;
            }

            if (i == frames - 1) {
                checkedMethodID = runMethodID;
            }

            if (location.getMethodID() != checkedMethodID) {
                log.complain("Unexpected methodID of " + i + " frame location: "
                            + location.getMethodID() + "(expected: " + checkedMethodID + ")");
                success = false;
            }

            if (location.getIndex() < 0) {
                log.complain("Negative value of index of " + i + " frame location: "
                            + location.getIndex());
                success = false;
            }

        }

        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }

    }

}
