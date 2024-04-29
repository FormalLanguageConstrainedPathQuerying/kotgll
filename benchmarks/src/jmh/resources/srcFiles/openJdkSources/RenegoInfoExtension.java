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
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.ClientHello.ClientHelloMessage;
import static sun.security.ssl.SSLExtension.CH_RENEGOTIATION_INFO;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import static sun.security.ssl.SSLExtension.SH_RENEGOTIATION_INFO;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.ByteArrays;

/**
 * Pack of the "renegotiation_info" extensions [RFC 5746].
 */
final class RenegoInfoExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHRenegotiationInfoProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHRenegotiationInfoConsumer();
    static final HandshakeAbsence chOnLoadAbsence =
            new CHRenegotiationInfoAbsence();

    static final HandshakeProducer shNetworkProducer =
            new SHRenegotiationInfoProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHRenegotiationInfoConsumer();
    static final HandshakeAbsence shOnLoadAbsence =
            new SHRenegotiationInfoAbsence();

    static final SSLStringizer rniStringizer =
            new RenegotiationInfoStringizer();

    /**
     * The "renegotiation_info" extension.
     */
    static final class RenegotiationInfoSpec implements SSLExtensionSpec {
        static final RenegotiationInfoSpec NOMINAL =
                new RenegotiationInfoSpec(new byte[0]);

        private final byte[] renegotiatedConnection;

        private RenegotiationInfoSpec(byte[] renegotiatedConnection) {
            this.renegotiatedConnection = Arrays.copyOf(
                    renegotiatedConnection, renegotiatedConnection.length);
        }

        private RenegotiationInfoSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException {
            if (!m.hasRemaining() || m.remaining() < 1) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid renegotiation_info extension data: " +
                    "insufficient data"));
            }
            this.renegotiatedConnection = Record.getBytes8(m);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"renegotiated connection\": '['{0}']'", Locale.ENGLISH);
            Object[] messageFields;
            if (renegotiatedConnection.length == 0) {
                messageFields = new Object[]{
                        "<no renegotiated connection>"
                };
            } else {
                messageFields = new Object[]{
                        Utilities.toHexString(renegotiatedConnection)
                };
            }
            return messageFormat.format(messageFields);
        }
    }

    private static final
            class RenegotiationInfoStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new RenegotiationInfoSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "renegotiation_info" extension in
     * the ClientHello handshake message.
     */
    private static final
            class CHRenegotiationInfoProducer implements HandshakeProducer {
        private CHRenegotiationInfoProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslConfig.isAvailable(CH_RENEGOTIATION_INFO)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable renegotiation_info extension");
                }

                return null;
            }

            if (!chc.conContext.isNegotiated) {
                if (chc.activeCipherSuites.contains(
                        CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)) {
                    return null;
                }

                byte[] extData = new byte[] { 0x00 };
                chc.handshakeExtensions.put(
                        CH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

                return extData;
            } else if (chc.conContext.secureRenegotiation) {
                byte[] extData =
                        new byte[chc.conContext.clientVerifyData.length + 1];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putBytes8(m, chc.conContext.clientVerifyData);

                chc.handshakeExtensions.put(
                        CH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

                return extData;
            } else {    
                if (HandshakeContext.allowUnsafeRenegotiation) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning("Using insecure renegotiation");
                    }

                    return null;
                } else {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "insecure renegotiation is not allowed");
                }
            }
        }
    }

    /**
     * Network data producer of a "renegotiation_info" extension in
     * the ServerHello handshake message.
     */
    private static final
            class CHRenegotiationInfoConsumer implements ExtensionConsumer {
        private CHRenegotiationInfoConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(CH_RENEGOTIATION_INFO)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("Ignore unavailable extension: " +
                            CH_RENEGOTIATION_INFO.name);
                }
                return;     
            }

            RenegotiationInfoSpec spec = new RenegotiationInfoSpec(shc, buffer);
            if (!shc.conContext.isNegotiated) {
                if (spec.renegotiatedConnection.length != 0) {
                    throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Invalid renegotiation_info extension data: not empty");
                }
                shc.conContext.secureRenegotiation = true;
            } else {
                if (!shc.conContext.secureRenegotiation) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "The renegotiation_info is present in a insecure " +
                            "renegotiation");
                } else {
                    if (!MessageDigest.isEqual(shc.conContext.clientVerifyData,
                            spec.renegotiatedConnection)) {
                        throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                            "Invalid renegotiation_info extension data: " +
                            "incorrect verify data in ClientHello");
                    }
                }
            }

            shc.handshakeExtensions.put(
                    CH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

        }
    }

    /**
     * The absence processing if a "renegotiation_info" extension is
     * not present in the ClientHello handshake message.
     */
    private static final
            class CHRenegotiationInfoAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            if (!shc.conContext.isNegotiated) {
                for (int id : clientHello.cipherSuiteIds) {
                    if (id ==
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV.id) {
                        if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                            SSLLogger.finest(
                                "Safe renegotiation, using the SCSV signaling");
                        }
                        shc.conContext.secureRenegotiation = true;
                        return;
                    }
                }

                if (!HandshakeContext.allowLegacyHelloMessages) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Failed to negotiate the use of secure renegotiation");
                }   

                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("Warning: No renegotiation " +
                        "indication in ClientHello, allow legacy ClientHello");
                }

                shc.conContext.secureRenegotiation = false;
            } else if (shc.conContext.secureRenegotiation) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Inconsistent secure renegotiation indication");
            } else {    
                if (HandshakeContext.allowUnsafeRenegotiation) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning("Using insecure renegotiation");
                    }
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine("Terminate insecure renegotiation");
                    }
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unsafe renegotiation is not allowed");
                }
            }
        }
    }

    /**
     * Network data producer of a "renegotiation_info" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHRenegotiationInfoProducer implements HandshakeProducer {
        private SHRenegotiationInfoProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            RenegotiationInfoSpec requestedSpec = (RenegotiationInfoSpec)
                    shc.handshakeExtensions.get(CH_RENEGOTIATION_INFO);
            if (requestedSpec == null && !shc.conContext.secureRenegotiation) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable renegotiation_info extension");
                }
                return null;        
            }

            if (!shc.conContext.secureRenegotiation) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "No secure renegotiation has been negotiated");
                }
                return null;        
            }

            if (!shc.conContext.isNegotiated) {
                byte[] extData = new byte[] { 0x00 };

                shc.handshakeExtensions.put(
                        SH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

                return extData;
            } else {
                int infoLen = shc.conContext.clientVerifyData.length +
                              shc.conContext.serverVerifyData.length;
                byte[] extData = new byte[infoLen + 1];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putInt8(m, infoLen);
                m.put(shc.conContext.clientVerifyData);
                m.put(shc.conContext.serverVerifyData);

                shc.handshakeExtensions.put(
                        SH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

                return extData;
            }
        }
    }

    /**
     * Network data consumer of a "renegotiation_info" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHRenegotiationInfoConsumer implements ExtensionConsumer {
        private SHRenegotiationInfoConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            RenegotiationInfoSpec requestedSpec = (RenegotiationInfoSpec)
                    chc.handshakeExtensions.get(CH_RENEGOTIATION_INFO);
            if (requestedSpec == null &&
                    !chc.activeCipherSuites.contains(
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "Missing renegotiation_info and SCSV detected in " +
                    "ClientHello");
            }

            RenegotiationInfoSpec spec = new RenegotiationInfoSpec(chc, buffer);
            if (!chc.conContext.isNegotiated) {     
                if (spec.renegotiatedConnection.length != 0) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid renegotiation_info in ServerHello: " +
                        "not empty renegotiated_connection");
                }

                chc.conContext.secureRenegotiation = true;
            } else {        
                int infoLen = chc.conContext.clientVerifyData.length +
                              chc.conContext.serverVerifyData.length;
                if (spec.renegotiatedConnection.length != infoLen) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid renegotiation_info in ServerHello: " +
                        "invalid renegotiated_connection length (" +
                        spec.renegotiatedConnection.length + ")");
                }

                byte[] cvd = chc.conContext.clientVerifyData;
                if (!ByteArrays.isEqual(spec.renegotiatedConnection,
                        0, cvd.length, cvd, 0, cvd.length)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid renegotiation_info in ServerHello: " +
                        "unmatched client_verify_data value");
                }
                byte[] svd = chc.conContext.serverVerifyData;
                if (!ByteArrays.isEqual(spec.renegotiatedConnection,
                        cvd.length, infoLen, svd, 0, svd.length)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid renegotiation_info in ServerHello: " +
                        "unmatched server_verify_data value");
                }
            }

            chc.handshakeExtensions.put(
                    SH_RENEGOTIATION_INFO, RenegotiationInfoSpec.NOMINAL);

        }
    }

    /**
     * The absence processing if a "renegotiation_info" extension is
     * not present in the ServerHello handshake message.
     */
    private static final
            class SHRenegotiationInfoAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            RenegotiationInfoSpec requestedSpec = (RenegotiationInfoSpec)
                    chc.handshakeExtensions.get(CH_RENEGOTIATION_INFO);
            if (requestedSpec == null &&
                    !chc.activeCipherSuites.contains(
                            CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "Missing renegotiation_info and SCSV detected in " +
                    "ClientHello");
            }

            if (!chc.conContext.isNegotiated) {
                if (!HandshakeContext.allowLegacyHelloMessages) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Failed to negotiate the use of secure renegotiation");
                }   

                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("Warning: No renegotiation " +
                        "indication in ServerHello, allow legacy ServerHello");
                }

                chc.conContext.secureRenegotiation = false;
            } else if (chc.conContext.secureRenegotiation) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Inconsistent secure renegotiation indication");
            } else {    
                if (HandshakeContext.allowUnsafeRenegotiation) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning("Using insecure renegotiation");
                    }
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine("Terminate insecure renegotiation");
                    }
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unsafe renegotiation is not allowed");
                }
            }
        }
    }
}
