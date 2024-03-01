/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLHandshakeException;
import sun.security.ssl.PskKeyExchangeModesExtension.PskKeyExchangeMode;
import sun.security.ssl.PskKeyExchangeModesExtension.PskKeyExchangeModesSpec;
import sun.security.ssl.SessionTicketExtension.SessionTicketSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.HexDumpEncoder;

import static sun.security.ssl.SSLHandshake.NEW_SESSION_TICKET;

/**
 * Pack of the NewSessionTicket handshake message.
 */
final class NewSessionTicket {
    static final int MAX_TICKET_LIFETIME = 604800;  
    static final SSLConsumer handshakeConsumer =
        new T13NewSessionTicketConsumer();
    static final SSLConsumer handshake12Consumer =
        new T12NewSessionTicketConsumer();
    static final SSLProducer t13PosthandshakeProducer =
        new T13NewSessionTicketProducer();
    static final HandshakeProducer handshake12Producer =
        new T12NewSessionTicketProducer();

    /**
     * The NewSessionTicketMessage handshake messages.
     */
    abstract static class NewSessionTicketMessage extends HandshakeMessage {
        int ticketLifetime;
        byte[] ticket = new byte[0];

        NewSessionTicketMessage(HandshakeContext context) {
            super(context);
        }

        @Override
        public SSLHandshake handshakeType() {
            return NEW_SESSION_TICKET;
        }

        int getTicketAgeAdd() throws IOException {
            throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "TicketAgeAdd not part of RFC 5077.");
        }

        byte[] getTicketNonce() throws IOException {
            throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "TicketNonce not part of RFC 5077.");
        }

        boolean isValid() {
            return (ticket.length > 0);
        }
    }
    /**
     * NewSessionTicket for TLS 1.2 and below (RFC 5077)
     */
    static final class T12NewSessionTicketMessage extends NewSessionTicketMessage {

        T12NewSessionTicketMessage(HandshakeContext context,
                int ticketLifetime, byte[] ticket) {
            super(context);

            this.ticketLifetime = ticketLifetime;
            this.ticket = ticket;
        }

        T12NewSessionTicketMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {


            super(context);
            if (m.remaining() < 6) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid NewSessionTicket message: insufficient data");
            }

            this.ticketLifetime = Record.getInt32(m);
            this.ticket = Record.getBytes16(m);
        }

        @Override
        public SSLHandshake handshakeType() {
            return NEW_SESSION_TICKET;
        }

        @Override
        public int messageLength() {
            return 4 + 
                    2 + ticket.length;  
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt32(ticketLifetime);
            hos.putBytes16(ticket);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "NewSessionTicket": '{'
                              "ticket_lifetime"      : "{0}",
                              "ticket"               : '{'
                            {1}
                              '}''}'""",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                    ticketLifetime,
                    Utilities.indent(hexEncoder.encode(ticket), "    "),
            };
            return messageFormat.format(messageFields);
        }
    }

    /**
     * NewSessionTicket defined by the TLS 1.3
     */
    static final class T13NewSessionTicketMessage extends NewSessionTicketMessage {
        int ticketAgeAdd;
        byte[] ticketNonce;
        SSLExtensions extensions;

        T13NewSessionTicketMessage(HandshakeContext context,
                int ticketLifetime, SecureRandom generator,
                byte[] ticketNonce, byte[] ticket) {
            super(context);

            this.ticketLifetime = ticketLifetime;
            this.ticketAgeAdd = generator.nextInt();
            this.ticketNonce = ticketNonce;
            this.ticket = ticket;
            this.extensions = new SSLExtensions(this);
        }

        T13NewSessionTicketMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);


            if (m.remaining() < 14) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid NewSessionTicket message: insufficient data");
            }

            this.ticketLifetime = Record.getInt32(m);
            this.ticketAgeAdd = Record.getInt32(m);
            this.ticketNonce = Record.getBytes8(m);

            if (m.remaining() < 5) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid NewSessionTicket message: insufficient ticket" +
                            " data");
            }

            this.ticket = Record.getBytes16(m);
            if (ticket.length == 0) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No ticket in the NewSessionTicket handshake message");
                }
            }

            if (m.remaining() < 2) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid NewSessionTicket message: extra data");
            }

            SSLExtension[] supportedExtensions =
                    context.sslConfig.getEnabledExtensions(
                            NEW_SESSION_TICKET);
            this.extensions = new SSLExtensions(this, m, supportedExtensions);
        }

        @Override
        public SSLHandshake handshakeType() {
            return NEW_SESSION_TICKET;
        }

        int getTicketAgeAdd() {
            return ticketAgeAdd;
        }

        byte[] getTicketNonce() {
            return ticketNonce;
        }

        @Override
        public int messageLength() {

            int extLen = extensions.length();
            if (extLen == 0) {
                extLen = 2;     
            }

            return 4 +
                    4 + 
                    1 + ticketNonce.length + 
                    2 + ticket.length + 
                    extLen;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt32(ticketLifetime);
            hos.putInt32(ticketAgeAdd);
            hos.putBytes8(ticketNonce);
            hos.putBytes16(ticket);

            if (extensions.length() == 0) {
                hos.putInt16(0);
            } else {
                extensions.send(hos);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "NewSessionTicket": '{'
                              "ticket_lifetime"      : "{0}",
                              "ticket_age_add"       : "{1}",
                              "ticket_nonce"         : "{2}",
                              "ticket"               : '{'
                            {3}
                              '}'  "extensions"           : [
                            {4}
                              ]
                            '}'""",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                ticketLifetime,
                "<omitted>",    
                Utilities.toHexString(ticketNonce),
                Utilities.indent(hexEncoder.encode(ticket), "    "),
                Utilities.indent(extensions.toString(), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    private static SecretKey derivePreSharedKey(CipherSuite.HashAlg hashAlg,
            SecretKey resumptionMasterSecret, byte[] nonce) throws IOException {
        try {
            HKDF hkdf = new HKDF(hashAlg.name);
            byte[] hkdfInfo = SSLSecretDerivation.createHkdfInfo(
                    "tls13 resumption".getBytes(), nonce, hashAlg.hashLength);
            return hkdf.expand(resumptionMasterSecret, hkdfInfo,
                    hashAlg.hashLength, "TlsPreSharedKey");
        } catch  (GeneralSecurityException gse) {
            throw new SSLHandshakeException("Could not derive PSK", gse);
        }
    }

    private static final
            class T13NewSessionTicketProducer implements SSLProducer {
        private T13NewSessionTicketProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context) throws IOException {
            HandshakeContext hc = (HandshakeContext)context;

            if (hc.conContext.hasDelegatedFinished) {
                hc.conContext.hasDelegatedFinished = false;
                hc.conContext.needHandshakeFinishedStatus = true;
            }

            if (hc instanceof ServerHandshakeContext) {
                if (!hc.handshakeSession.isRejoinable()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "No session ticket produced: " +
                                "session is not resumable");
                    }

                    return null;
                }

                PskKeyExchangeModesSpec pkemSpec =
                        (PskKeyExchangeModesSpec) hc.handshakeExtensions.get(
                                SSLExtension.PSK_KEY_EXCHANGE_MODES);
                if (pkemSpec == null ||
                        !pkemSpec.contains(PskKeyExchangeMode.PSK_DHE_KE)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "No session ticket produced: " +
                                "client does not support psk_dhe_ke");
                    }

                    return null;
                }
            } else {     
                if (!hc.handshakeSession.isPSKable()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "No session ticket produced: " +
                                "No session ticket allowed in this session");
                    }

                    return null;
                }
            }

            SSLSessionContextImpl sessionCache = (SSLSessionContextImpl)
                hc.sslContext.engineGetServerSessionContext();
            SessionId newId = new SessionId(true,
                hc.sslContext.getSecureRandom());

            SecretKey resumptionMasterSecret =
                hc.handshakeSession.getResumptionMasterSecret();
            if (resumptionMasterSecret == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "No session ticket produced: " +
                            "no resumption secret");
                }

                return null;
            }

            BigInteger nonce = hc.handshakeSession.incrTicketNonceCounter();
            byte[] nonceArr = nonce.toByteArray();
            SecretKey psk = derivePreSharedKey(
                    hc.negotiatedCipherSuite.hashAlg,
                    resumptionMasterSecret, nonceArr);

            int sessionTimeoutSeconds = sessionCache.getSessionTimeout();
            if (sessionTimeoutSeconds > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "No session ticket produced: " +
                            "session timeout");
                }

                return null;
            }

            NewSessionTicketMessage nstm = null;

            SSLSessionImpl sessionCopy =
                    new SSLSessionImpl(hc.handshakeSession, newId);
            sessionCopy.setPreSharedKey(psk);
            sessionCopy.setPskIdentity(newId.getId());

            if (hc.statelessResumption &&
                    hc.handshakeSession.isStatelessable()) {
                nstm = new T13NewSessionTicketMessage(hc,
                        sessionTimeoutSeconds,
                        hc.sslContext.getSecureRandom(),
                        nonceArr,
                        new SessionTicketSpec().encrypt(hc, sessionCopy));
                if (!nstm.isValid()) {
                    hc.statelessResumption = false;
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "Produced NewSessionTicket stateless " +
                            "post-handshake message", nstm);
                    }
                }
            }

            if (!hc.statelessResumption ||
                    !hc.handshakeSession.isStatelessable()) {
                nstm = new T13NewSessionTicketMessage(hc, sessionTimeoutSeconds,
                        hc.sslContext.getSecureRandom(), nonceArr,
                        newId.getId());
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Produced NewSessionTicket post-handshake message",
                            nstm);
                }

                hc.handshakeSession.addChild(sessionCopy);
                sessionCopy.setTicketAgeAdd(nstm.getTicketAgeAdd());
                sessionCache.put(sessionCopy);
            }

            if (nstm != null) {
                nstm.write(hc.handshakeOutput);
                hc.handshakeOutput.flush();

                if (hc.conContext.needHandshakeFinishedStatus) {
                    hc.conContext.needHandshakeFinishedStatus = false;
                }
            }

            hc.conContext.finishPostHandshake();

            return null;
        }
    }

    /**
     * The "NewSessionTicket" handshake message producer for RFC 5077
     */
    private static final class T12NewSessionTicketProducer
            implements HandshakeProducer {

        private T12NewSessionTicketProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.handshakeSession.isRejoinable()) {
                return null;
            }

            SessionId newId = shc.handshakeSession.getSessionId();

            SSLSessionContextImpl sessionCache = (SSLSessionContextImpl)
                    shc.sslContext.engineGetServerSessionContext();
            int sessionTimeoutSeconds = sessionCache.getSessionTimeout();
            if (sessionTimeoutSeconds > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Session timeout is too long. No ticket sent.");
                }
                return null;
            }

            SSLSessionImpl sessionCopy =
                    new SSLSessionImpl(shc.handshakeSession, newId);
            sessionCopy.setPskIdentity(newId.getId());

            NewSessionTicketMessage nstm = new T12NewSessionTicketMessage(shc,
                    sessionTimeoutSeconds,
                    new SessionTicketSpec().encrypt(shc, sessionCopy));
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced NewSessionTicket stateless handshake message",
                    nstm);
            }

            nstm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            return null;
        }
    }

    private static final
    class T13NewSessionTicketConsumer implements SSLConsumer {
        private T13NewSessionTicketConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {


            HandshakeContext hc = (HandshakeContext)context;
            NewSessionTicketMessage nstm =
                    new T13NewSessionTicketMessage(hc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming NewSessionTicket message", nstm);
            }

            SSLSessionContextImpl sessionCache = (SSLSessionContextImpl)
                    hc.sslContext.engineGetClientSessionContext();

            if (nstm.ticketLifetime <= 0 ||
                nstm.ticketLifetime > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Discarding NewSessionTicket with lifetime " +
                            nstm.ticketLifetime, nstm);
                }
                sessionCache.remove(hc.handshakeSession.getSessionId());
                return;
            }

            if (sessionCache.getSessionTimeout() > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Session cache lifetime is too long. " +
                        "Discarding ticket.");
                }
                return;
            }

            SSLSessionImpl sessionToSave = hc.conContext.conSession;
            SecretKey resumptionMasterSecret =
                    sessionToSave.getResumptionMasterSecret();
            if (resumptionMasterSecret == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Session has no resumption master secret. " +
                            "Ignoring ticket.");
                }
                return;
            }

            SecretKey psk = derivePreSharedKey(
                    sessionToSave.getSuite().hashAlg,
                    resumptionMasterSecret, nstm.getTicketNonce());

            SessionId newId =
                    new SessionId(true, hc.sslContext.getSecureRandom());
            SSLSessionImpl sessionCopy = new SSLSessionImpl(sessionToSave,
                    newId);
            sessionToSave.addChild(sessionCopy);
            sessionCopy.setPreSharedKey(psk);
            sessionCopy.setTicketAgeAdd(nstm.getTicketAgeAdd());
            sessionCopy.setPskIdentity(nstm.ticket);
            sessionCache.put(sessionCopy);

            hc.conContext.finishPostHandshake();
        }
    }

    private static final
    class T12NewSessionTicketConsumer implements SSLConsumer {
        private T12NewSessionTicketConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {

            HandshakeContext hc = (HandshakeContext)context;
            hc.handshakeConsumers.remove(NEW_SESSION_TICKET.id);

            NewSessionTicketMessage nstm = new T12NewSessionTicketMessage(hc,
                    message);
            if (nstm.ticket.length == 0) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("NewSessionTicket ticket was empty");
                }
                return;
            }

            if (nstm.ticketLifetime <= 0 ||
                nstm.ticketLifetime > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Discarding NewSessionTicket with lifetime " +
                            nstm.ticketLifetime, nstm);
                }
                return;
            }

            SSLSessionContextImpl sessionCache = (SSLSessionContextImpl)
                    hc.sslContext.engineGetClientSessionContext();

            if (sessionCache.getSessionTimeout() > MAX_TICKET_LIFETIME) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Session cache lifetime is too long. " +
                        "Discarding ticket.");
                }
                return;
            }

            hc.handshakeSession.setPskIdentity(nstm.ticket);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consuming NewSessionTicket\n" +
                        nstm.toString());
            }
        }
    }
}

