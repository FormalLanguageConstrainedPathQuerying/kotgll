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
package nsk.share.jdwp;

import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;

/*
 * This class can be used as base for debugger.
 * Class initialize log, pipe, debuggee, transport.
 *
 * Subclasses should implement method doTest() and provide debugee class name via method getDebugeeClassName()
 */
abstract public class TestDebuggerType1 {
    protected ArgumentHandler argumentHandler;

    protected Log log;

    protected IOPipe pipe;

    protected Debugee debuggee;

    protected Transport transport;

    private boolean success = true;

    public static String createTypeSignature(String name) {
        return "L" + name.replace('.', '/') + ";";
    }

    protected void setSuccess(boolean value) {
        success = value;
    }

    protected boolean getSuccess() {
        return success;
    }

    protected String getDebugeeClassName() {
        return AbstractJDWPDebuggee.class.getName();
    }

    abstract protected void doTest();

    protected ReplyPacket getReply(CommandPacket command, boolean expectError, int errorCode) throws IOException,
            BoundException {
        log.display("Waiting for reply packet");
        ReplyPacket reply = new ReplyPacket();
        transport.read(reply);
        log.display("Reply packet received:\n" + reply);

        if (expectError) {
            if (reply.getErrorCode() != errorCode) {
                setSuccess(false);
                log.complain("Reply doesn't contain expected error " + errorCode + ", error code = "
                        + reply.getErrorCode());
            } else {
                log.display("Expected error: " + errorCode);
            }
        } else {
            log.display("Checking reply packet header");
            reply.checkHeader(command.getPacketID());
            log.display("Parsing reply packet:");
            reply.resetPosition();
        }

        return reply;
    }

    protected ReplyPacket getReply(CommandPacket command) throws IOException, BoundException {
        return getReply(command, false, 0);
    }

    protected String[] doInit(String args[]) {
        return args;
    }

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new ArgumentHandler(doInit(args));
        log = new Log(out, argumentHandler);

        try {
            log.display("\n>>> Preparing debugee for testing \n");

            Binder binder = new Binder(argumentHandler, log);
            log.display("Launching debugee");
            debuggee = binder.bindToDebugee(getDebugeeClassName());
            transport = debuggee.getTransport();
            pipe = debuggee.createIOPipe();

            prepareDebugee();

            doTest();
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Caught unexpected exception:\n" + t);
            t.printStackTrace(log.getOutStream());
        } finally {
            if (pipe != null)
                quitDebugee();
        }

        if (getSuccess()) {
            log.display("TEST PASSED");
            return Consts.TEST_PASSED;
        } else {
            log.display("TEST FAILED");
            return Consts.TEST_FAILED;
        }
    }

    protected void prepareDebugee() {
        log.display("Waiting for VM_INIT event");
        debuggee.waitForVMInit();

        log.display("Querying for IDSizes");
        debuggee.queryForIDSizes();

        log.display("Resuming debugee VM");
        debuggee.resume();

        log.display("Waiting for signal from debugee: " + AbstractDebuggeeTest.COMMAND_READY);

        if (!isDebuggeeReady())
            return;
    }

    protected boolean isDebuggeeReady() {
        String signal = pipe.readln();
        log.display("Received signal from debugee: " + signal);

        if (!signal.equals(AbstractDebuggeeTest.COMMAND_READY)) {
            setSuccess(false);
            log.complain("Unexpected signal received form debugee: " + signal + " (expected: "
                    + AbstractDebuggeeTest.COMMAND_READY + ")");
            return false;
        }

        return true;
    }

    protected void quitDebugee() {
        log.display("Sending signal to debugee: " + AbstractDebuggeeTest.COMMAND_QUIT);
        pipe.println(AbstractDebuggeeTest.COMMAND_QUIT);

        log.display("Waiting for debugee exits");
        int code = debuggee.waitFor();

        if (code == (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.display("Debugee PASSED with exit code: " + code);
        } else {
            setSuccess(false);
            log.complain("Debugee FAILED with exit code: " + code);
        }
    }

    protected int defaultBreakpointRequestID;

    protected long queryObjectID(long classID, String fieldName, byte tag) {
        long fieldID = debuggee.getClassFieldID(classID, fieldName, true);

        JDWP.Value value = debuggee.getStaticFieldValue(classID, fieldID);

        if (value.getTag() != tag) {
            throw new Failure("Wrong objectID tag received from field \"" + fieldName + "\": " + value.getTag()
                    + " (expected: " + tag + ")");
        }

        long objectID = ((Long) value.getValue()).longValue();

        return objectID;
    }

    protected long queryObjectID(long classID, String fieldName) {

        long fieldID = debuggee.getClassFieldID(classID, fieldName, true);

        JDWP.Value value = debuggee.getStaticFieldValue(classID, fieldID);

        long objectID = ((Long) value.getValue()).longValue();

        return objectID;
    }

    protected void clearRequest(byte eventKind, int requestID) {
        try {
            CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Clear);
            command.addByte(eventKind);
            command.addInt(requestID);
            command.setLength();
            transport.write(command);
            ReplyPacket reply;

            reply = getReply(command);

            if (!reply.isParsed()) {
                log.complain("Extra trailing bytes found in request reply packet at: " + reply.offsetString());
            }
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    protected EventPacket receiveSingleEvent(byte expectedEventKind, int expectedRequestID) {
        try {
            EventPacket eventPacket = debuggee.getEventPacket();

            eventPacket.checkHeader();
            eventPacket.resetPosition();

            eventPacket.getByte();

            int eventCount = eventPacket.getInt();

            if (eventCount != 1) {
                setSuccess(false);
                log.complain("Unexpected event count: " + eventCount + ", expected is " + 1);
            }

            byte eventKind = eventPacket.getByte();

            if (eventKind != expectedEventKind) {
                setSuccess(false);
                log.complain("Unexpected event kind: " + eventKind + ", expected is " + expectedEventKind);
            }

            int requestID = eventPacket.getInt();

            if (requestID != expectedRequestID) {
                setSuccess(false);
                log.complain("Unexpected request id: " + requestID + ", expected is " + expectedRequestID);
            }

            return eventPacket;
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());

            return null;
        }
    }


    private boolean currentSuccess = false;
    protected void forceGC() {
        pipe.println(AbstractDebuggeeTest.COMMAND_FORCE_GC);
        if (!isDebuggeeReady())
            return;
        currentSuccess = getSuccess();
    }

    protected void resetStatusIfGC() {
        pipe.println(AbstractDebuggeeTest.COMMAND_GC_COUNT);
        String command = pipe.readln();
        if (command.startsWith(AbstractDebuggeeTest.COMMAND_GC_COUNT)) {
            if (!isDebuggeeReady()) {
                return;
            }
            if (Integer.valueOf(command.substring(AbstractDebuggeeTest.COMMAND_GC_COUNT.length() + 1)) > 0) {
                log.display("WARNING: The GC worked during tests. Results are skipped.");
                setSuccess(currentSuccess);
            }
            return;
        }
        setSuccess(false);
    }

}
