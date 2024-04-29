/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.StandardConstants;

/*
 * @test
 * @bug 8301686
 * @summary verifies that if the server rejects session resumption due to SNI
 *          mismatch, during TLS handshake, then the subsequent communication
 *          between the server and the client happens correctly without any
 *          errors
 * @library /javax/net/ssl/templates
 * @run main/othervm -Djavax.net.debug=all
 *                   ServerNameRejectedTLSSessionResumption
 */
public class ServerNameRejectedTLSSessionResumption
        extends SSLContextTemplate {

    private static final String CLIENT_REQUESTED_SNI = "client.local";
    private static final String PEER_HOST = "foobar";
    private static final int PEER_PORT = 12345;

    public static void main(final String[] args) throws Exception {
        new ServerNameRejectedTLSSessionResumption().runTest();
    }

    private void runTest() throws Exception {
        final SSLContext clientSSLContext = createClientSSLContext();
        final SSLContext serverSSLContext = createServerSSLContext();
        final SSLEngine clientEngine = createClientSSLEngine(clientSSLContext);
        final SSLEngine serverEngine = createServerSSLEngine(serverSSLContext,
                new TestSNIMatcher(CLIENT_REQUESTED_SNI));
        communicate(clientEngine, serverEngine);

        final SSLEngine secondClientEngine =
                createClientSSLEngine(clientSSLContext);
        final SSLEngine secondServerEngine =
                createServerSSLEngine(serverSSLContext, null);
        communicate(secondClientEngine, secondServerEngine);
    }

    private static void communicate(final SSLEngine clientEngine,
                                    final SSLEngine serverEngine)
            throws Exception {

        final ByteBuffer msgFromClient =
                ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
        final ByteBuffer msgFromServer =
                ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
        final ByteBuffer clientBuffer = ByteBuffer.allocate(1 << 15);
        final ByteBuffer serverBuffer = ByteBuffer.allocate(1 << 15);
        /*
         * For data transport, this test uses local ByteBuffers
         */
        final ByteBuffer clientToServerTransport =
                ByteBuffer.allocateDirect(1 << 16);
        final ByteBuffer serverToClientTransport =
                ByteBuffer.allocateDirect(1 << 16);
        boolean isClientToServer = true;
        while (true) {
            if (isClientToServer) {
                SSLEngineResult result = clientEngine.wrap(msgFromClient,
                        clientToServerTransport);
                final HandshakeStatus hsStatus = checkAndRunTasks(clientEngine,
                        result.getHandshakeStatus());
                clientToServerTransport.flip(); 

                result = serverEngine.unwrap(clientToServerTransport,
                        serverBuffer);
                checkAndRunTasks(serverEngine, result.getHandshakeStatus());
                clientToServerTransport.compact();

                if (hsStatus == HandshakeStatus.NEED_UNWRAP) {
                    isClientToServer = false;
                } else if (hsStatus == HandshakeStatus.FINISHED) {
                    break;
                } else if (hsStatus != HandshakeStatus.NEED_WRAP) {
                    throw new Exception("Unexpected handshake result "
                            + result);
                }
            } else {
                SSLEngineResult result = serverEngine.wrap(msgFromServer,
                        serverToClientTransport);
                final HandshakeStatus hsStatus = checkAndRunTasks(serverEngine,
                        result.getHandshakeStatus());
                serverToClientTransport.flip(); 

                result = clientEngine.unwrap(serverToClientTransport,
                        clientBuffer);
                checkAndRunTasks(clientEngine, result.getHandshakeStatus());
                serverToClientTransport.compact();

                if (hsStatus == HandshakeStatus.NEED_UNWRAP) {
                    isClientToServer = true;
                } else if (hsStatus == HandshakeStatus.FINISHED) {
                    break;
                } else if (hsStatus != HandshakeStatus.NEED_WRAP) {
                    throw new Exception("Unexpected handshake result "
                            + result);
                }
            }
        }
        serverEngine.wrap(msgFromServer, serverToClientTransport);
        serverToClientTransport.flip();
        clientEngine.unwrap(serverToClientTransport, clientBuffer);
        serverToClientTransport.compact();
    }

    private static SSLEngine createServerSSLEngine(
            final SSLContext sslContext, final SNIMatcher sniMatcher) {
        final SSLEngine serverEngine = sslContext.createSSLEngine();
        serverEngine.setUseClientMode(false);
        if (sniMatcher != null) {
            final SSLParameters sslParameters =
                    serverEngine.getSSLParameters(); 
            sslParameters.setSNIMatchers(List.of(sniMatcher));
            serverEngine.setSSLParameters(sslParameters);
        }
        return serverEngine;
    }

    private static SSLEngine createClientSSLEngine(
            final SSLContext sslContext) {
        final SSLEngine clientEngine = sslContext.createSSLEngine(PEER_HOST,
                PEER_PORT);
        clientEngine.setUseClientMode(true);
        final SSLParameters params =
                clientEngine.getSSLParameters(); 
        params.setServerNames(List.of(new SNIHostName(CLIENT_REQUESTED_SNI)));
        clientEngine.setSSLParameters(params); 
        return clientEngine;
    }

    private static HandshakeStatus checkAndRunTasks(
            final SSLEngine engine, final HandshakeStatus handshakeStatus) {
        if (handshakeStatus != HandshakeStatus.NEED_TASK) {
            return handshakeStatus;
        }
        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            System.out.println("Running task " + runnable);
            runnable.run();
        }
        return engine.getHandshakeStatus();
    }

    private static final class TestSNIMatcher extends SNIMatcher {

        private final String recognizedSNIServerName;

        private TestSNIMatcher(final String recognizedSNIServerName) {
            super(StandardConstants.SNI_HOST_NAME);
            this.recognizedSNIServerName = recognizedSNIServerName;
        }

        @Override
        public boolean matches(final SNIServerName clientRequestedSNI) {
            Objects.requireNonNull(clientRequestedSNI);
            System.out.println("Attempting SNI match against client" +
                    " request SNI name: " + clientRequestedSNI +
                    " against server recognized SNI name "
                    + recognizedSNIServerName);
            if (!SNIHostName.class.isInstance(clientRequestedSNI)) {
                System.out.println("SNI match failed - client request" +
                        " SNI isn't a SNIHostName");
                return false;
            }
            final String requestedName =
                    ((SNIHostName) clientRequestedSNI).getAsciiName();
            final boolean matches =
                    recognizedSNIServerName.equals(requestedName);
            System.out.println("SNI match " + (matches ? "passed" : "failed"));
            return matches;
        }
    }
}
