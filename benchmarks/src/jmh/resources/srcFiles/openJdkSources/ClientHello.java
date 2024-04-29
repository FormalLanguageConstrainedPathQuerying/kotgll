/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import static sun.security.ssl.ClientAuthType.CLIENT_AUTH_REQUIRED;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SupportedVersionsExtension.CHSupportedVersionsSpec;

/**
 * Pack of the ClientHello handshake message.
 */
final class ClientHello {
    static final SSLProducer kickstartProducer =
        new ClientHelloKickstartProducer();
    static final SSLConsumer handshakeConsumer =
        new ClientHelloConsumer();
    static final HandshakeProducer handshakeProducer =
        new ClientHelloProducer();

    private static final HandshakeConsumer t12HandshakeConsumer =
            new T12ClientHelloConsumer();
    private static final HandshakeConsumer t13HandshakeConsumer =
            new T13ClientHelloConsumer();
    private static final HandshakeConsumer d12HandshakeConsumer =
            new D12ClientHelloConsumer();
    private static final HandshakeConsumer d13HandshakeConsumer =
            new D13ClientHelloConsumer();

    /**
     * The ClientHello handshake message.
     *
     * See RFC 5264/4346/2246/6347 for the specifications.
     */
    static final class ClientHelloMessage extends HandshakeMessage {
        private final boolean       isDTLS;

        final int                   clientVersion;
        final RandomCookie          clientRandom;
        final SessionId             sessionId;
        private byte[]              cookie;         
        final int[]                 cipherSuiteIds;
        final List<CipherSuite>     cipherSuites;   
        final byte[]                compressionMethod;
        final SSLExtensions         extensions;

        private static final byte[]  NULL_COMPRESSION = new byte[] {0};

        ClientHelloMessage(HandshakeContext handshakeContext,
                int clientVersion, SessionId sessionId,
                List<CipherSuite> cipherSuites, SecureRandom generator) {
            super(handshakeContext);
            this.isDTLS = handshakeContext.sslContext.isDTLS();

            this.clientVersion = clientVersion;
            this.clientRandom = new RandomCookie(generator);
            this.sessionId = sessionId;
            if (isDTLS) {
                this.cookie = new byte[0];
            } else {
                this.cookie = null;
            }

            this.cipherSuites = cipherSuites;
            this.cipherSuiteIds = getCipherSuiteIds(cipherSuites);
            this.extensions = new SSLExtensions(this);

            this.compressionMethod = NULL_COMPRESSION;
        }

        /* Read up to the binders in the PSK extension. After this method
         * returns, the ByteBuffer position will be at end of the message
         * fragment that should be hashed to produce the PSK binder values.
         * The client of this method can use this position to determine the
         * message fragment and produce the binder values.
         */
        static void readPartial(TransportContext tc,
                ByteBuffer m) throws IOException {
            boolean isDTLS = tc.sslContext.isDTLS();

            Record.getInt16(m);

            new RandomCookie(m);

            Record.getBytes8(m);

            if (isDTLS) {
                Record.getBytes8(m);
            }

            Record.getBytes16(m);
            Record.getBytes8(m);
            if (m.remaining() >= 2) {
                int remaining = Record.getInt16(m);
                while (remaining > 0) {
                    int id = Record.getInt16(m);
                    int extLen = Record.getInt16(m);
                    remaining -= extLen + 4;

                    if (id == SSLExtension.CH_PRE_SHARED_KEY.id) {
                        if (remaining > 0) {
                            throw tc.fatal(Alert.ILLEGAL_PARAMETER,
                                    "pre_shared_key extension is not last");
                        }
                        Record.getBytes16(m);
                        return;
                    } else {
                        m.position(m.position() + extLen);

                    }
                }
            }   
        }

        ClientHelloMessage(HandshakeContext handshakeContext, ByteBuffer m,
                SSLExtension[] supportedExtensions) throws IOException {
            super(handshakeContext);
            this.isDTLS = handshakeContext.sslContext.isDTLS();

            this.clientVersion = ((m.get() & 0xFF) << 8) | (m.get() & 0xFF);
            this.clientRandom = new RandomCookie(m);
            this.sessionId = new SessionId(Record.getBytes8(m));
            try {
                sessionId.checkLength(clientVersion);
            } catch (SSLProtocolException ex) {
                throw handshakeContext.conContext.fatal(
                        Alert.ILLEGAL_PARAMETER, ex);
            }
            if (isDTLS) {
                this.cookie = Record.getBytes8(m);
            } else {
                this.cookie = null;
            }

            byte[] encodedIds = Record.getBytes16(m);
            if (encodedIds.length == 0 || (encodedIds.length & 0x01) != 0) {
                throw handshakeContext.conContext.fatal(
                        Alert.ILLEGAL_PARAMETER,
                        "Invalid ClientHello message");
            }

            this.cipherSuiteIds = new int[encodedIds.length >> 1];
            for (int i = 0, j = 0; i < encodedIds.length; i++, j++) {
                cipherSuiteIds[j] =
                    ((encodedIds[i++] & 0xFF) << 8) | (encodedIds[i] & 0xFF);
            }
            this.cipherSuites = getCipherSuites(cipherSuiteIds);

            this.compressionMethod = Record.getBytes8(m);
            if (m.hasRemaining()) {
                this.extensions =
                        new SSLExtensions(this, m, supportedExtensions);
            } else {
                this.extensions = new SSLExtensions(this);
            }
        }

        void setHelloCookie(byte[] cookie) {
            this.cookie = cookie;
        }

        byte[] getHelloCookieBytes() {
            HandshakeOutStream hos = new HandshakeOutStream(null);
            try {
                hos.putInt8((byte)((clientVersion >>> 8) & 0xFF));
                hos.putInt8((byte)(clientVersion & 0xFF));
                hos.write(clientRandom.randomBytes, 0, 32);
                hos.putBytes8(sessionId.getId());
                hos.putBytes16(getEncodedCipherSuites());
                hos.putBytes8(compressionMethod);
                extensions.send(hos);       
            } catch (IOException ioe) {
            }

            return hos.toByteArray();
        }

        byte[] getHeaderBytes() {
            HandshakeOutStream hos = new HandshakeOutStream(null);
            try {
                hos.putInt8((byte)((clientVersion >>> 8) & 0xFF));
                hos.putInt8((byte)(clientVersion & 0xFF));
                hos.write(clientRandom.randomBytes, 0, 32);
                hos.putBytes8(sessionId.getId());
                hos.putBytes16(getEncodedCipherSuites());
                hos.putBytes8(compressionMethod);
            } catch (IOException ioe) {
            }

            return hos.toByteArray();
        }

        private static int[] getCipherSuiteIds(
                List<CipherSuite> cipherSuites) {
            if (cipherSuites != null) {
                int[] ids = new int[cipherSuites.size()];
                int i = 0;
                for (CipherSuite cipherSuite : cipherSuites) {
                    ids[i++] = cipherSuite.id;
                }

                return ids;
            }

            return new int[0];
        }

        private static List<CipherSuite> getCipherSuites(int[] ids) {
            List<CipherSuite> cipherSuites = new LinkedList<>();
            for (int id : ids) {
                CipherSuite cipherSuite = CipherSuite.valueOf(id);
                if (cipherSuite != null) {
                    cipherSuites.add(cipherSuite);
                }
            }

            return Collections.unmodifiableList(cipherSuites);
        }

        private List<String> getCipherSuiteNames() {
            List<String> names = new LinkedList<>();
            for (int id : cipherSuiteIds) {
                names.add(CipherSuite.nameOf(id) +
                        "(" + Utilities.byte16HexString(id) + ")");            }

            return names;
        }

        private byte[] getEncodedCipherSuites() {
            byte[] encoded = new byte[cipherSuiteIds.length << 1];
            int i = 0;
            for (int id : cipherSuiteIds) {
                encoded[i++] = (byte)(id >> 8);
                encoded[i++] = (byte)id;
            }
            return encoded;
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CLIENT_HELLO;
        }

        @Override
        public int messageLength() {
            /*
             * Add fixed size parts of each field...
             * version + random + session + cipher + compress
             */
            return (2 + 32 + 1 + 2 + 1
                + sessionId.length()        /* ... + variable parts */
                + (isDTLS ? (1 + cookie.length) : 0)
                + (cipherSuiteIds.length * 2)
                + compressionMethod.length)
                + extensions.length();      
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            sendCore(hos);
            extensions.send(hos);       
        }

        void sendCore(HandshakeOutStream hos) throws IOException {
            hos.putInt8((byte) (clientVersion >>> 8));
            hos.putInt8((byte) clientVersion);
            hos.write(clientRandom.randomBytes, 0, 32);
            hos.putBytes8(sessionId.getId());
            if (isDTLS) {
                hos.putBytes8(cookie);
            }
            hos.putBytes16(getEncodedCipherSuites());
            hos.putBytes8(compressionMethod);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat;
            Object[] messageFields;
            if (isDTLS) {
                messageFormat = new MessageFormat(
                        """
                                "ClientHello": '{'
                                  "client version"      : "{0}",
                                  "random"              : "{1}",
                                  "session id"          : "{2}",
                                  "cookie"              : "{3}",
                                  "cipher suites"       : "{4}",
                                  "compression methods" : "{5}",
                                  "extensions"          : [
                                {6}
                                  ]
                                '}'""",
                        Locale.ENGLISH);
                messageFields = new Object[]{
                        ProtocolVersion.nameOf(clientVersion),
                        Utilities.toHexString(clientRandom.randomBytes),
                        sessionId.toString(),
                        Utilities.toHexString(cookie),
                        getCipherSuiteNames().toString(),
                        Utilities.toHexString(compressionMethod),
                        Utilities.indent(Utilities.indent(extensions.toString()))
                };

            } else {
                messageFormat = new MessageFormat(
                        """
                                "ClientHello": '{'
                                  "client version"      : "{0}",
                                  "random"              : "{1}",
                                  "session id"          : "{2}",
                                  "cipher suites"       : "{3}",
                                  "compression methods" : "{4}",
                                  "extensions"          : [
                                {5}
                                  ]
                                '}'""",
                        Locale.ENGLISH);
                messageFields = new Object[]{
                        ProtocolVersion.nameOf(clientVersion),
                        Utilities.toHexString(clientRandom.randomBytes),
                        sessionId.toString(),
                        getCipherSuiteNames().toString(),
                        Utilities.toHexString(compressionMethod),
                        Utilities.indent(Utilities.indent(extensions.toString()))
                };

            }
            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "ClientHello" handshake message kick-start producer.
     */
    private static final
            class ClientHelloKickstartProducer implements SSLProducer {
        private ClientHelloKickstartProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            chc.handshakeProducers.remove(SSLHandshake.CLIENT_HELLO.id);

            SessionId sessionId = new SessionId(new byte[0]);

            List<CipherSuite> cipherSuites = chc.activeCipherSuites;

            SSLSessionContextImpl ssci = (SSLSessionContextImpl)
                    chc.sslContext.engineGetClientSessionContext();
            SSLSessionImpl session = ssci.get(
                    chc.conContext.transport.getPeerHost(),
                    chc.conContext.transport.getPeerPort());
            if (session != null) {
                if (!ClientHandshakeContext.allowUnsafeServerCertChange &&
                        session.isSessionResumption()) {
                    try {
                        chc.reservedServerCerts =
                            (X509Certificate[])session.getPeerCertificates();
                    } catch (SSLPeerUnverifiedException puve) {
                    }
                }

                if (!session.isRejoinable()) {
                    session = null;
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                            "Can't resume, the session is not rejoinable");
                    }
                }
            }

            CipherSuite sessionSuite = null;
            if (session != null) {
                sessionSuite = session.getSuite();
                if (!chc.isNegotiable(sessionSuite)) {
                    session = null;
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                            "Can't resume, unavailable session cipher suite");
                    }
                }
            }

            ProtocolVersion sessionVersion = null;
            if (session != null) {
                sessionVersion = session.getProtocolVersion();
                if (!chc.isNegotiable(sessionVersion)) {
                    session = null;
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                            "Can't resume, unavailable protocol version");
                    }
                }
            }

            if (session != null &&
                !sessionVersion.useTLS13PlusSpec() &&
                SSLConfiguration.useExtendedMasterSecret) {

                boolean isEmsAvailable = chc.sslConfig.isAvailable(
                        SSLExtension.CH_EXTENDED_MASTER_SECRET, sessionVersion);
                if (isEmsAvailable && !session.useExtendedMasterSecret &&
                        !SSLConfiguration.allowLegacyResumption) {
                     session = null;
                }

                if ((session != null) &&
                        !ClientHandshakeContext.allowUnsafeServerCertChange) {
                    String identityAlg = chc.sslConfig.identificationProtocol;
                    if (identityAlg == null || identityAlg.isEmpty()) {
                        if (isEmsAvailable) {
                            if (!session.useExtendedMasterSecret) {
                                session = null;
                            }   
                        } else {
                            session = null;
                        }
                    }
                }
            }

            String identityAlg = chc.sslConfig.identificationProtocol;
            if (session != null && identityAlg != null) {
                String sessionIdentityAlg =
                    session.getIdentificationProtocol();
                if (!identityAlg.equalsIgnoreCase(sessionIdentityAlg)) {
                    if (SSLLogger.isOn &&
                    SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest("Can't resume, endpoint id" +
                            " algorithm does not match, requested: " +
                            identityAlg + ", cached: " + sessionIdentityAlg);
                    }
                    session = null;
                }
            }

            if (session != null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake,verbose")) {
                    SSLLogger.finest("Try resuming session", session);
                }

                if (!session.getProtocolVersion().useTLS13PlusSpec()) {
                    sessionId = session.getSessionId();
                }

                if (!chc.sslConfig.enableSessionCreation) {
                    if (!chc.conContext.isNegotiated &&
                        !sessionVersion.useTLS13PlusSpec() &&
                        cipherSuites.contains(
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)) {
                        cipherSuites = Arrays.asList(sessionSuite,
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
                    } else {    
                        cipherSuites = List.of(sessionSuite);
                    }

                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                            "No new session is allowed, so try to resume " +
                            "the session cipher suite only", sessionSuite);
                    }
                }

                chc.isResumption = true;
                chc.resumingSession = session;
            }

            if (session == null) {
                if (!chc.sslConfig.enableSessionCreation) {
                    throw new SSLHandshakeException(
                            "No new session is allowed and " +
                            "no existing session can be resumed");
                }
            }
            if (sessionId.length() == 0 &&
                    chc.maximumActiveProtocol.useTLS13PlusSpec() &&
                    SSLConfiguration.useCompatibilityMode) {
                sessionId =
                        new SessionId(true, chc.sslContext.getSecureRandom());
            }

            ProtocolVersion minimumVersion = ProtocolVersion.NONE;
            for (ProtocolVersion pv : chc.activeProtocols) {
                if (minimumVersion == ProtocolVersion.NONE ||
                        pv.compare(minimumVersion) < 0) {
                    minimumVersion = pv;
                }
            }

            if (!minimumVersion.useTLS13PlusSpec()) {
                if (chc.conContext.secureRenegotiation &&
                        cipherSuites.contains(
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)) {
                    cipherSuites = new LinkedList<>(cipherSuites);
                    cipherSuites.remove(
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
                }
            }

            boolean negotiable = false;
            for (CipherSuite suite : cipherSuites) {
                if (chc.isNegotiable(suite)) {
                    negotiable = true;
                    break;
                }
            }
            if (!negotiable) {
                throw new SSLHandshakeException("No negotiable cipher suite");
            }

            ProtocolVersion clientHelloVersion = chc.maximumActiveProtocol;
            if (clientHelloVersion.useTLS13PlusSpec()) {
                if (clientHelloVersion.isDTLS) {
                    clientHelloVersion = ProtocolVersion.DTLS12;
                } else {
                    clientHelloVersion = ProtocolVersion.TLS12;
                }
            }

            ClientHelloMessage chm = new ClientHelloMessage(chc,
                    clientHelloVersion.id, sessionId, cipherSuites,
                    chc.sslContext.getSecureRandom());

            chc.clientHelloRandom = chm.clientRandom;
            chc.clientHelloVersion = clientHelloVersion.id;

            SSLExtension[] extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.CLIENT_HELLO, chc.activeProtocols);
            chm.extensions.produce(chc, extTypes);

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced ClientHello handshake message", chm);
            }

            chm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            chc.initialClientHelloMsg = chm;

            chc.handshakeConsumers.put(
                    SSLHandshake.SERVER_HELLO.id, SSLHandshake.SERVER_HELLO);
            if (chc.sslContext.isDTLS() &&
                    !minimumVersion.useTLS13PlusSpec()) {
                chc.handshakeConsumers.put(
                        SSLHandshake.HELLO_VERIFY_REQUEST.id,
                        SSLHandshake.HELLO_VERIFY_REQUEST);
            }

            return null;
        }
    }

    private static final
            class ClientHelloProducer implements HandshakeProducer {
        private ClientHelloProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            SSLHandshake ht = message.handshakeType();
            if (ht == null) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            switch (ht) {
                case HELLO_REQUEST:
                    try {
                        chc.kickstart();
                    } catch (IOException ioe) {
                        throw chc.conContext.fatal(
                                Alert.HANDSHAKE_FAILURE, ioe);
                    }

                    return null;
                case HELLO_VERIFY_REQUEST:
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Produced ClientHello(cookie) handshake message",
                            chc.initialClientHelloMsg);
                    }

                    chc.initialClientHelloMsg.write(chc.handshakeOutput);
                    chc.handshakeOutput.flush();

                    chc.handshakeConsumers.put(SSLHandshake.SERVER_HELLO.id,
                            SSLHandshake.SERVER_HELLO);

                    ProtocolVersion minimumVersion = ProtocolVersion.NONE;
                    for (ProtocolVersion pv : chc.activeProtocols) {
                        if (minimumVersion == ProtocolVersion.NONE ||
                                pv.compare(minimumVersion) < 0) {
                            minimumVersion = pv;
                        }
                    }
                    if (chc.sslContext.isDTLS() &&
                            !minimumVersion.useTLS13PlusSpec()) {
                        chc.handshakeConsumers.put(
                                SSLHandshake.HELLO_VERIFY_REQUEST.id,
                                SSLHandshake.HELLO_VERIFY_REQUEST);
                    }

                    return null;
                case HELLO_RETRY_REQUEST:
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Produced ClientHello(HRR) handshake message",
                            chc.initialClientHelloMsg);
                    }

                    chc.initialClientHelloMsg.write(chc.handshakeOutput);
                    chc.handshakeOutput.flush();

                    chc.conContext.consumers.putIfAbsent(
                            ContentType.CHANGE_CIPHER_SPEC.id,
                            ChangeCipherSpec.t13Consumer);
                    chc.handshakeConsumers.put(SSLHandshake.SERVER_HELLO.id,
                            SSLHandshake.SERVER_HELLO);

                    return null;
                default:
                    throw new UnsupportedOperationException(
                            "Not supported yet.");
            }
        }
    }

    /**
     * The "ClientHello" handshake message consumer.
     */
    private static final class ClientHelloConsumer implements SSLConsumer {
        private ClientHelloConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            shc.handshakeConsumers.remove(SSLHandshake.CLIENT_HELLO.id);
            if (!shc.handshakeConsumers.isEmpty()) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "No more handshake message allowed " +
                        "in a ClientHello flight");
            }

            SSLExtension[] enabledExtensions =
                    shc.sslConfig.getEnabledExtensions(
                            SSLHandshake.CLIENT_HELLO);

            ClientHelloMessage chm =
                    new ClientHelloMessage(shc, message, enabledExtensions);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consuming ClientHello handshake message", chm);
            }

            shc.clientHelloVersion = chm.clientVersion;
            onClientHello(shc, chm);
        }

        private void onClientHello(ServerHandshakeContext context,
                ClientHelloMessage clientHello) throws IOException {
            SSLExtension[] extTypes = new SSLExtension[] {
                    SSLExtension.CH_SUPPORTED_VERSIONS
                };
            clientHello.extensions.consumeOnLoad(context, extTypes);

            ProtocolVersion negotiatedProtocol;
            CHSupportedVersionsSpec svs =
                    (CHSupportedVersionsSpec)context.handshakeExtensions.get(
                            SSLExtension.CH_SUPPORTED_VERSIONS);
            if (svs != null) {
                negotiatedProtocol =
                        negotiateProtocol(context, svs.requestedProtocols);
            } else {
                negotiatedProtocol =
                        negotiateProtocol(context, clientHello.clientVersion);
            }
            context.negotiatedProtocol = negotiatedProtocol;
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Negotiated protocol version: " + negotiatedProtocol.name);
            }

            if (negotiatedProtocol.isDTLS) {
                if (negotiatedProtocol.useTLS13PlusSpec()) {
                    d13HandshakeConsumer.consume(context, clientHello);
                } else {
                    d12HandshakeConsumer.consume(context, clientHello);
                }
            } else {
                if (negotiatedProtocol.useTLS13PlusSpec()) {
                    t13HandshakeConsumer.consume(context, clientHello);
                } else {
                    t12HandshakeConsumer.consume(context, clientHello);
                }
            }
        }

        private ProtocolVersion negotiateProtocol(
                ServerHandshakeContext context,
                int clientHelloVersion) throws SSLException {

            int chv = clientHelloVersion;
            if (context.sslContext.isDTLS()) {
                if (chv < ProtocolVersion.DTLS12.id) {
                    chv = ProtocolVersion.DTLS12.id;
                }
            } else {
                if (chv > ProtocolVersion.TLS12.id) {
                    chv = ProtocolVersion.TLS12.id;
                }
            }

            ProtocolVersion pv = ProtocolVersion.selectedFrom(
                    context.activeProtocols, chv);
            if (pv == null || pv == ProtocolVersion.NONE ||
                    pv == ProtocolVersion.SSL20Hello) {
                throw context.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "Client requested protocol " +
                    ProtocolVersion.nameOf(clientHelloVersion) +
                    " is not enabled or supported in server context");
            }

            return pv;
        }

        private ProtocolVersion negotiateProtocol(
                ServerHandshakeContext context,
                int[] clientSupportedVersions) throws SSLException {

            for (ProtocolVersion spv : context.activeProtocols) {
                if (spv == ProtocolVersion.SSL20Hello) {
                    continue;
                }
                for (int cpv : clientSupportedVersions) {
                    if (cpv == ProtocolVersion.SSL20Hello.id) {
                        continue;
                    }
                    if (spv.id == cpv) {
                        return spv;
                    }
                }
            }

            throw context.conContext.fatal(Alert.PROTOCOL_VERSION,
                "The client supported protocol versions " + Arrays.toString(
                    ProtocolVersion.toStringArray(clientSupportedVersions)) +
                " are not accepted by server preferences " +
                context.activeProtocols);
        }
    }

    /**
     * The "ClientHello" handshake message consumer for TLS 1.2 and
     * prior SSL/TLS protocol versions.
     */
    private static final
            class T12ClientHelloConsumer implements HandshakeConsumer {
        private T12ClientHelloConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;


            if (shc.conContext.isNegotiated) {
                if (!shc.conContext.secureRenegotiation &&
                        !HandshakeContext.allowUnsafeRenegotiation) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Unsafe renegotiation is not allowed");
                }

                if (ServerHandshakeContext.rejectClientInitiatedRenego &&
                        !shc.kickstartMessageDelivered) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Client initiated renegotiation is not allowed");
                }
            }

            SSLExtension[] ext = new SSLExtension[]{
                    SSLExtension.CH_SESSION_TICKET
            };
            clientHello.extensions.consumeOnLoad(shc, ext);

            if (clientHello.sessionId.length() != 0 || shc.statelessResumption) {
                SSLSessionContextImpl cache = (SSLSessionContextImpl)shc.sslContext
                        .engineGetServerSessionContext();

                SSLSessionImpl previous;
                if (shc.statelessResumption) {
                    previous = shc.resumingSession;
                } else {
                    previous = cache.get(clientHello.sessionId.getId());
                }

                boolean resumingSession =
                        (previous != null) && previous.isRejoinable();
                if (!resumingSession) {
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                                "Can't resume, " +
                                "the existing session is not rejoinable");
                    }
                }
                if (resumingSession) {
                    ProtocolVersion sessionProtocol =
                            previous.getProtocolVersion();
                    if (sessionProtocol != shc.negotiatedProtocol) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, not the same protocol version");
                        }
                    }
                }

                if (resumingSession &&
                    (shc.sslConfig.clientAuthType == CLIENT_AUTH_REQUIRED)) {
                    try {
                        previous.getPeerPrincipal();
                    } catch (SSLPeerUnverifiedException e) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, " +
                                "client authentication is required");
                        }
                    }
                }

                if (resumingSession) {
                    CipherSuite suite = previous.getSuite();
                    if ((!shc.isNegotiable(suite)) ||
                            (!clientHello.cipherSuites.contains(suite))) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, " +
                                "the session cipher suite is absent");
                        }
                    }
                }

                String identityAlg = shc.sslConfig.identificationProtocol;
                if (resumingSession && identityAlg != null) {
                    String sessionIdentityAlg =
                        previous.getIdentificationProtocol();
                    if (!identityAlg.equalsIgnoreCase(sessionIdentityAlg)) {
                        if (SSLLogger.isOn &&
                        SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest("Can't resume, endpoint id" +
                            " algorithm does not match, requested: " +
                            identityAlg + ", cached: " + sessionIdentityAlg);
                        }
                        resumingSession = false;
                    }
                }

                shc.isResumption = resumingSession;
                shc.resumingSession = resumingSession ? previous : null;

                if (!resumingSession && SSLLogger.isOn &&
                        SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("Session not resumed.");
                }
            }

            shc.clientHelloRandom = clientHello.clientRandom;

            SSLExtension[] extTypes = shc.sslConfig.getExclusiveExtensions(
                    SSLHandshake.CLIENT_HELLO,
                    List.of(SSLExtension.CH_SESSION_TICKET));
            clientHello.extensions.consumeOnLoad(shc, extTypes);

            if (!shc.conContext.isNegotiated) {
                shc.conContext.protocolVersion = shc.negotiatedProtocol;
                shc.conContext.outputRecord.setVersion(shc.negotiatedProtocol);
            }

            shc.handshakeProducers.put(SSLHandshake.SERVER_HELLO.id,
                    SSLHandshake.SERVER_HELLO);

            SSLHandshake[] probableHandshakeMessages = new SSLHandshake[] {
                SSLHandshake.SERVER_HELLO,

                SSLHandshake.CERTIFICATE,
                SSLHandshake.CERTIFICATE_STATUS,
                SSLHandshake.SERVER_KEY_EXCHANGE,
                SSLHandshake.CERTIFICATE_REQUEST,
                SSLHandshake.SERVER_HELLO_DONE,

                SSLHandshake.FINISHED
            };

            for (SSLHandshake hs : probableHandshakeMessages) {
                HandshakeProducer handshakeProducer =
                        shc.handshakeProducers.remove(hs.id);
                if (handshakeProducer != null) {
                    handshakeProducer.produce(context, clientHello);
                }
            }
        }
    }

    /**
     * The "ClientHello" handshake message consumer for TLS 1.3.
     */
    private static final
            class T13ClientHelloConsumer implements HandshakeConsumer {
        private T13ClientHelloConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            if (shc.conContext.isNegotiated) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Received unexpected renegotiation handshake message");
            }

            if (clientHello.clientVersion != ProtocolVersion.TLS12.id) {
                throw shc.conContext.fatal(Alert.PROTOCOL_VERSION,
                        "The ClientHello.legacy_version field is not TLS 1.2");
            }

            shc.conContext.consumers.putIfAbsent(
                    ContentType.CHANGE_CIPHER_SPEC.id,
                    ChangeCipherSpec.t13Consumer);

            shc.isResumption = true;
            SSLExtension[] extTypes = new SSLExtension[] {
                    SSLExtension.PSK_KEY_EXCHANGE_MODES,
                    SSLExtension.CH_PRE_SHARED_KEY
                };
            clientHello.extensions.consumeOnLoad(shc, extTypes);

            extTypes = shc.sslConfig.getExclusiveExtensions(
                    SSLHandshake.CLIENT_HELLO,
                    Arrays.asList(
                            SSLExtension.PSK_KEY_EXCHANGE_MODES,
                            SSLExtension.CH_PRE_SHARED_KEY,
                            SSLExtension.CH_SUPPORTED_VERSIONS));
            clientHello.extensions.consumeOnLoad(shc, extTypes);

            if (!shc.handshakeProducers.isEmpty()) {
                goHelloRetryRequest(shc, clientHello);
            } else {
                goServerHello(shc, clientHello);
            }
        }

        private void goHelloRetryRequest(ServerHandshakeContext shc,
                ClientHelloMessage clientHello) throws IOException {
            HandshakeProducer handshakeProducer =
                    shc.handshakeProducers.remove(
                            SSLHandshake.HELLO_RETRY_REQUEST.id);
            if (handshakeProducer != null) {
                    handshakeProducer.produce(shc, clientHello);
            } else {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No HelloRetryRequest producer: " + shc.handshakeProducers);
            }

            if (!shc.handshakeProducers.isEmpty()) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "unknown handshake producers: " + shc.handshakeProducers);
            }
        }

        private void goServerHello(ServerHandshakeContext shc,
                ClientHelloMessage clientHello) throws IOException {
            shc.clientHelloRandom = clientHello.clientRandom;

            if (!shc.conContext.isNegotiated) {
                shc.conContext.protocolVersion = shc.negotiatedProtocol;
                shc.conContext.outputRecord.setVersion(shc.negotiatedProtocol);
            }

            shc.handshakeProducers.put(SSLHandshake.SERVER_HELLO.id,
                SSLHandshake.SERVER_HELLO);

            SSLHandshake[] probableHandshakeMessages = new SSLHandshake[] {
                SSLHandshake.SERVER_HELLO,

                SSLHandshake.ENCRYPTED_EXTENSIONS,
                SSLHandshake.CERTIFICATE_REQUEST,
                SSLHandshake.CERTIFICATE,
                SSLHandshake.CERTIFICATE_VERIFY,
                SSLHandshake.FINISHED
            };

            for (SSLHandshake hs : probableHandshakeMessages) {
                HandshakeProducer handshakeProducer =
                        shc.handshakeProducers.remove(hs.id);
                if (handshakeProducer != null) {
                    handshakeProducer.produce(shc, clientHello);
                }
            }
        }
    }

    /**
     * The "ClientHello" handshake message consumer for DTLS 1.2 and
     * previous DTLS protocol versions.
     */
    private static final
            class D12ClientHelloConsumer implements HandshakeConsumer {
        private D12ClientHelloConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;


            if (shc.conContext.isNegotiated) {
                if (!shc.conContext.secureRenegotiation &&
                        !HandshakeContext.allowUnsafeRenegotiation) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Unsafe renegotiation is not allowed");
                }

                if (ServerHandshakeContext.rejectClientInitiatedRenego &&
                        !shc.kickstartMessageDelivered) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Client initiated renegotiation is not allowed");
                }
            }


            if (clientHello.sessionId.length() != 0) {
                SSLSessionContextImpl cache = (SSLSessionContextImpl)shc.sslContext
                        .engineGetServerSessionContext();

                SSLExtension[] ext = new SSLExtension[]{
                        SSLExtension.CH_SESSION_TICKET
                };
                clientHello.extensions.consumeOnLoad(shc, ext);

                SSLSessionImpl previous;
                if (shc.statelessResumption) {
                    previous = shc.resumingSession;
                } else {
                    previous = cache.get(clientHello.sessionId.getId());
                }

                boolean resumingSession =
                        (previous != null) && previous.isRejoinable();
                if (!resumingSession) {
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest(
                            "Can't resume, " +
                            "the existing session is not rejoinable");
                    }
                }
                if (resumingSession) {
                    ProtocolVersion sessionProtocol =
                            previous.getProtocolVersion();
                    if (sessionProtocol != shc.negotiatedProtocol) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, not the same protocol version");
                        }
                    }
                }

                if (resumingSession &&
                    (shc.sslConfig.clientAuthType == CLIENT_AUTH_REQUIRED)) {

                    try {
                        previous.getPeerPrincipal();
                    } catch (SSLPeerUnverifiedException e) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, " +
                                "client authentication is required");
                        }
                    }
                }

                if (resumingSession) {
                    CipherSuite suite = previous.getSuite();
                    if ((!shc.isNegotiable(suite)) ||
                            (!clientHello.cipherSuites.contains(suite))) {
                        resumingSession = false;
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake,verbose")) {
                            SSLLogger.finest(
                                "Can't resume, " +
                                "the session cipher suite is absent");
                        }
                    }
                }

                shc.isResumption = resumingSession;
                shc.resumingSession = resumingSession ? previous : null;
            }


            if (!shc.isResumption || SSLConfiguration.enableDtlsResumeCookie) {
                HelloCookieManager hcm =
                        shc.sslContext.getHelloCookieManager(ProtocolVersion.DTLS10);
                if (!hcm.isCookieValid(shc, clientHello, clientHello.cookie)) {
                    shc.handshakeProducers.put(
                            SSLHandshake.HELLO_VERIFY_REQUEST.id,
                            SSLHandshake.HELLO_VERIFY_REQUEST);

                    SSLHandshake.HELLO_VERIFY_REQUEST.produce(context, clientHello);

                    return;
                }
            }

            shc.clientHelloRandom = clientHello.clientRandom;

            SSLExtension[] extTypes = shc.sslConfig.getEnabledExtensions(
                    SSLHandshake.CLIENT_HELLO);
            clientHello.extensions.consumeOnLoad(shc, extTypes);

            if (!shc.conContext.isNegotiated) {
                shc.conContext.protocolVersion = shc.negotiatedProtocol;
                shc.conContext.outputRecord.setVersion(shc.negotiatedProtocol);
            }

            shc.handshakeProducers.put(SSLHandshake.SERVER_HELLO.id,
                    SSLHandshake.SERVER_HELLO);

            SSLHandshake[] probableHandshakeMessages = new SSLHandshake[] {
                SSLHandshake.SERVER_HELLO,

                SSLHandshake.CERTIFICATE,
                SSLHandshake.CERTIFICATE_STATUS,
                SSLHandshake.SERVER_KEY_EXCHANGE,
                SSLHandshake.CERTIFICATE_REQUEST,
                SSLHandshake.SERVER_HELLO_DONE,

                SSLHandshake.FINISHED
            };

            for (SSLHandshake hs : probableHandshakeMessages) {
                HandshakeProducer handshakeProducer =
                        shc.handshakeProducers.remove(hs.id);
                if (handshakeProducer != null) {
                    handshakeProducer.produce(context, clientHello);
                }
            }
        }
    }

    /**
     * The "ClientHello" handshake message consumer for DTLS 1.3.
     */
    private static final
            class D13ClientHelloConsumer implements HandshakeConsumer {
        private D13ClientHelloConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
