/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8254631
 * @summary Better support ALPN byte wire values in SunJSSE
 * @library /javax/net/ssl/templates
 * @run main/othervm AlpnGreaseTest
 */
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A SSLEngine usage example which simplifies the presentation
 * by removing the I/O and multi-threading concerns.
 *
 * The test creates two SSLEngines, simulating a client and server.
 * The "transport" layer consists two byte buffers:  think of them
 * as directly connected pipes.
 *
 * Note, this is a *very* simple example: real code will be much more
 * involved.  For example, different threading and I/O models could be
 * used, transport mechanisms could close unexpectedly, and so on.
 *
 * When this application runs, notice that several messages
 * (wrap/unwrap) pass before any application data is consumed or
 * produced.
 */
public class AlpnGreaseTest extends SSLContextTemplate {

    private final SSLEngine clientEngine;     
    private final ByteBuffer clientOut;       
    private final ByteBuffer clientIn;        

    private final SSLEngine serverEngine;     
    private final ByteBuffer serverOut;       
    private final ByteBuffer serverIn;        

    private final ByteBuffer cTOs;      
    private final ByteBuffer sTOc;      

    private static final byte[] greaseBytes = new byte[] {
        (byte) 0x0A, (byte) 0x1A, (byte) 0x2A, (byte) 0x3A,
        (byte) 0x4A, (byte) 0x5A, (byte) 0x6A, (byte) 0x7A,
        (byte) 0x8A, (byte) 0x9A, (byte) 0xAA, (byte) 0xBA,
        (byte) 0xCA, (byte) 0xDA, (byte) 0xEA, (byte) 0xFA
    };

    private static final String greaseString =
            new String(greaseBytes, StandardCharsets.ISO_8859_1);

    private static void findGreaseInClientHello(byte[] bytes) throws Exception {
        for (int i = 0; i < bytes.length - greaseBytes.length + 1; i++) {
            if (Arrays.equals(bytes, i, i + greaseBytes.length,
                    greaseBytes, 0, greaseBytes.length)) {
                System.out.println("Found greaseBytes in ClientHello at: " + i);
                return;
            }
        }
        throw new Exception("Couldn't find greaseBytes");
    }

    private AlpnGreaseTest() throws Exception {
        serverEngine = configureServerEngine(
                createServerSSLContext().createSSLEngine());

        clientEngine = configureClientEngine(
                createClientSSLContext().createSSLEngine());

        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        clientIn = ByteBuffer.allocate(appBufferMax + 50);
        serverIn = ByteBuffer.allocate(appBufferMax + 50);

        cTOs = ByteBuffer.allocateDirect(netBufferMax);
        sTOc = ByteBuffer.allocateDirect(netBufferMax);

        clientOut = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
        serverOut = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
    }


    /*
     * Configure the client side engine.
     */
    protected SSLEngine configureClientEngine(SSLEngine clientEngine) {
        clientEngine.setUseClientMode(true);

        SSLParameters paramsClient = clientEngine.getSSLParameters();
        paramsClient.setApplicationProtocols(new String[] { greaseString });

        clientEngine.setSSLParameters(paramsClient);

        return clientEngine;
    }

    /*
     * Configure the server side engine.
     */
    protected SSLEngine configureServerEngine(SSLEngine serverEngine) {
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        SSLParameters paramsServer = serverEngine.getSSLParameters();
        paramsServer.setApplicationProtocols(new String[] { greaseString });
        serverEngine.setSSLParameters(paramsServer);

        return serverEngine;
    }

    public static void main(String[] args) throws Exception {
        new AlpnGreaseTest().runTest();
    }


    private void runTest() throws Exception {
        SSLEngineResult clientResult;
        SSLEngineResult serverResult;

        boolean dataDone = false;
        boolean firstClientWrap = true;
        while (isOpen(clientEngine) || isOpen(serverEngine)) {
            log("=================");

            log("---Client Wrap---");
            clientResult = clientEngine.wrap(clientOut, cTOs);
            logEngineStatus(clientEngine, clientResult);
            runDelegatedTasks(clientEngine);

            if (firstClientWrap) {
                firstClientWrap = false;
                byte[] bytes = new byte[cTOs.position()];
                cTOs.duplicate().flip().get(bytes);
                findGreaseInClientHello(bytes);
            }

            log("---Server Wrap---");
            serverResult = serverEngine.wrap(serverOut, sTOc);
            logEngineStatus(serverEngine, serverResult);
            runDelegatedTasks(serverEngine);

            cTOs.flip();
            sTOc.flip();

            log("---Client Unwrap---");
            clientResult = clientEngine.unwrap(sTOc, clientIn);
            logEngineStatus(clientEngine, clientResult);
            runDelegatedTasks(clientEngine);

            log("---Server Unwrap---");
            serverResult = serverEngine.unwrap(cTOs, serverIn);
            logEngineStatus(serverEngine, serverResult);
            runDelegatedTasks(serverEngine);

            cTOs.compact();
            sTOc.compact();

            if (!dataDone && (clientOut.limit() == serverIn.position()) &&
                    (serverOut.limit() == clientIn.position())) {

                String alpnServerValue = serverEngine.getApplicationProtocol();
                String alpnClientValue = clientEngine.getApplicationProtocol();

                if (!alpnServerValue.equals(greaseString)
                        || !alpnClientValue.equals(greaseString)) {
                    throw new Exception("greaseString didn't match");
                }

                checkTransfer(serverOut, clientIn);
                checkTransfer(clientOut, serverIn);

                log("\tClosing clientEngine's *OUTBOUND*...");
                clientEngine.closeOutbound();
                logEngineStatus(clientEngine);

                dataDone = true;
                log("\tClosing serverEngine's *OUTBOUND*...");
                serverEngine.closeOutbound();
                logEngineStatus(serverEngine);
            }
        }
    }

    private static boolean isOpen(SSLEngine engine) {
        return (!engine.isOutboundDone() || !engine.isInboundDone());
    }

    private static void logEngineStatus(SSLEngine engine) {
        log("\tCurrent HS State: " + engine.getHandshakeStatus());
        log("\tisInboundDone() : " + engine.isInboundDone());
        log("\tisOutboundDone(): " + engine.isOutboundDone());
    }

    private static void logEngineStatus(
            SSLEngine engine, SSLEngineResult result) {
        log("\tResult Status    : " + result.getStatus());
        log("\tResult HS Status : " + result.getHandshakeStatus());
        log("\tEngine HS Status : " + engine.getHandshakeStatus());
        log("\tisInboundDone()  : " + engine.isInboundDone());
        log("\tisOutboundDone() : " + engine.isOutboundDone());
        log("\tMore Result      : " + result);
    }

    private static void log(String message) {
        System.err.println(message);
    }

    private static void runDelegatedTasks(SSLEngine engine) throws Exception {
        if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("    running delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                        "handshake shouldn't need additional tasks");
            }
            logEngineStatus(engine);
        }
    }

    private static void checkTransfer(ByteBuffer a, ByteBuffer b)
            throws Exception {
        a.flip();
        b.flip();

        if (!a.equals(b)) {
            throw new Exception("Data didn't transfer cleanly");
        } else {
            log("\tData transferred cleanly");
        }

        a.position(a.limit());
        b.position(b.limit());
        a.limit(a.capacity());
        b.limit(b.capacity());
    }
}
