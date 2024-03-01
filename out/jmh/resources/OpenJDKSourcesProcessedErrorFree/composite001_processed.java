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

package nsk.jdwp.Event.Composite;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * Test for JDWP command: Event.Composite.
 *
 * See composite001.README for description of test execution.
 *
 * This class represents debugger part of the test.
 * Test is executed by invoking method runIt().
 * JDWP command is tested in the method testCommand().
 *
 * @see #runIt()
 * @see #waitForTestedEvent()
 */
public class composite001 {

    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;

    static final String PACKAGE_NAME = "nsk.jdwp.Event.Composite";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "composite001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "Event.Composite";
    static final int JDWP_COMMAND_ID = JDWP.Command.Event.Composite;
    static final byte TESTED_EVENT_KIND = JDWP.EventKind.VM_START;
    static final byte TESTED_EVENT_SUSPEND_POLICY = JDWP.SuspendPolicy.ALL;

    ArgumentHandler argumentHandler = null;
    Log log = null;
    Binder binder = null;
    Debugee debugee = null;
    Transport transport = null;
    int waitTime = 0;  
    long timeout = 0;  
    boolean dead = false;
    boolean success = true;


    /**
     * Start test from command line.
     */
    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    /**
     * Start test from JCK-compilant environment.
     */
    public static int run(String argv[], PrintStream out) {
        return new composite001().runIt(argv, out);
    }


    /**
     * Perform test execution.
     */
    public int runIt(String argv[], PrintStream out) {

        argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);
        waitTime = argumentHandler.getWaitTime();
        timeout = waitTime * 60 * 1000;

        try {
            log.display("\n>>> Starting debugee \n");

            binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
            transport = debugee.getTransport();
            log.display("  ... debugee launched");
            log.display("");

            log.display("Setting timeout for debuggee responces: " + waitTime + " minute(s)");
            transport.setReadTimeout(timeout);
            log.display("  ... timeout set");

            log.display("Waiting for VM_INIT event");
            debugee.waitForVMInit();
            log.display("  ... VM_INIT event received");

            log.display("Querying for IDSizes");
            debugee.queryForIDSizes();
            log.display("  ... size of VM-dependent types adjusted");

            log.display("\n>>> Testing JDWP event \n");
            testCommand();

            log.display("\n>>> Finishing debuggee \n");

            log.display("Resuming debuggee");
            debugee.resume();
            log.display("  ... debuggee resumed");

            log.display("Waiting for VM_DEATH event");
            debugee.waitForVMDeath();
            dead = true;
            log.display("  ... VM_DEATH event received");

        } catch (Failure e) {
            log.complain("TEST FAILED: " + e.getMessage());
            success = false;
        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while running the test:\n\t" + e);
            success = false;
        } finally {
            log.display("\n>>> Finishing test \n");
            quitDebugee();
        }

        if (!success) {
            log.complain("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

    /**
     * Test JDWP command.
     */
    void testCommand() {
        log.display("Create command packet: " + JDWP_COMMAND_NAME);
        CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
        log.display("    suspendPolicy: " + TESTED_EVENT_SUSPEND_POLICY);
        command.addByte(TESTED_EVENT_SUSPEND_POLICY);
        log.display("    events: " + 1);
        command.addInt(1);
        log.display("      eventKind: " + TESTED_EVENT_KIND);
        command.addByte(TESTED_EVENT_KIND);
        log.display("      requestID: " + 0);
        command.addInt(0);
        log.display("      threadID: " + 0);
        command.addObjectID(0);
        command.setLength();
        log.display("  ... command packet created");

        try {
            log.display("Sending command packet:\n" + command);
            transport.write(command);
            log.display("  ... command packet sent");
        } catch (IOException e) {
            log.complain("Unable to send command packet:\n\t" + e);
            success = false;
            return;
        }

        ReplyPacket reply = new ReplyPacket();

        try {
            log.display("Waiting for reply packet");
            transport.read(reply);
            log.display("  ... reply packet received:\n" + reply);
        } catch (IOException e) {
            log.complain("Unable to read reply packet:\n\t" + e);
            success = false;
            return;
        }

        log.display("Checking header of reply packet:");

        int length = reply.getLength();
        log.display("    length: " + length);
        if (length != reply.length()) {
            log.complain("Unexpected value of length field in header of reply packet: "
                        + length + "(actual: " + reply.length() + ")");
            success = false;
        }

        int id = reply.getPacketID();
        log.display("    id: " + id);
        if (id != command.getPacketID()) {
            log.complain("Unexpected value of id field in header of reply packet: "
                        + id + "(expected: " + command.getPacketID() + ")");
            success = false;
        }

        byte flags = reply.getFlags();
        log.display("    flags: " + flags);
        if (flags != JDWP.Flag.REPLY_PACKET) {
            log.complain("Unexpected value of flags field in header of reply packet: "
                        + flags + "(expected: " + JDWP.Flag.REPLY_PACKET + ")");
            success = false;
        }

        int error = reply.getErrorCode();
        log.display("    error: " + error);
        if (error == JDWP.Error.NONE) {
            log.complain("Unexpected null value of error field in header of reply packet: "
                        + error + "(expected: not " + JDWP.Error.NONE + ")");
            success = false;
        }

        log.display("  ... packet header is parsed");

        log.display("Parsing reply packet data:");
        reply.resetPosition();

        log.display("    no out data");

        log.display("  ... packet data is parsed");

        if (!reply.isParsed()) {
            log.complain("Extra trailing bytes found in reply packet at: "
                        + reply.offsetString());
            success = false;
        }
    }

    /**
     * Disconnect debuggee and wait for it exited.
     */
    void quitDebugee() {
        if (debugee == null)
            return;

        if (!dead) {
            try {
                log.display("Disconnecting debuggee");
                debugee.dispose();
                log.display("  ... debuggee disconnected");
            } catch (Failure e) {
                log.display("Failed to finally disconnect debuggee:\n\t"
                            + e.getMessage());
            }
        }

        log.display("Waiting for debuggee exit");
        int code = debugee.waitFor();
        log.display("  ... debuggee exited with exit code: " + code);

        if (code != JCK_STATUS_BASE + PASSED) {
            log.complain("Debuggee FAILED with exit code: " + code);
            success = false;
        }
    }

}
