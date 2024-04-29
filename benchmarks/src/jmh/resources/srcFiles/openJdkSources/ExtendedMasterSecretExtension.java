/*
 * Copyright (c) 2017, Red Hat, Inc. and/or its affiliates.
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
import java.nio.ByteBuffer;
import javax.net.ssl.SSLProtocolException;
import static sun.security.ssl.SSLExtension.CH_EXTENDED_MASTER_SECRET;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import static sun.security.ssl.SSLExtension.SH_EXTENDED_MASTER_SECRET;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;

/**
 * Pack of the "extended_master_secret" extensions [RFC 7627].
 */
final class ExtendedMasterSecretExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHExtendedMasterSecretProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHExtendedMasterSecretConsumer();
    static final HandshakeAbsence chOnLoadAbsence =
            new CHExtendedMasterSecretAbsence();

    static final HandshakeProducer shNetworkProducer =
            new SHExtendedMasterSecretProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHExtendedMasterSecretConsumer();
    static final HandshakeAbsence shOnLoadAbsence =
            new SHExtendedMasterSecretAbsence();

    static final SSLStringizer emsStringizer =
            new ExtendedMasterSecretStringizer();

    /**
     * The "extended_master_secret" extension.
     */
    static final class ExtendedMasterSecretSpec implements SSLExtensionSpec {
        static final ExtendedMasterSecretSpec NOMINAL =
                new ExtendedMasterSecretSpec();

        private ExtendedMasterSecretSpec() {
        }

        private ExtendedMasterSecretSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException {
            if (m.hasRemaining()) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid extended_master_secret extension data: " +
                    "not empty"));
            }
        }

        @Override
        public String toString() {
            return "<empty>";
        }
    }

    private static final
            class ExtendedMasterSecretStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new ExtendedMasterSecretSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of an "extended_master_secret" extension in
     * the ClientHello handshake message.
     */
    private static final
            class CHExtendedMasterSecretProducer implements HandshakeProducer {
        private CHExtendedMasterSecretProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslConfig.isAvailable(CH_EXTENDED_MASTER_SECRET) ||
                    !SSLConfiguration.useExtendedMasterSecret ||
                    !chc.conContext.protocolVersion.useTLS10PlusSpec()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extended_master_secret extension");
                }

                return null;
            }

            if (chc.handshakeSession == null ||
                    chc.handshakeSession.useExtendedMasterSecret) {
                byte[] extData = new byte[0];
                chc.handshakeExtensions.put(CH_EXTENDED_MASTER_SECRET,
                        ExtendedMasterSecretSpec.NOMINAL);

                return extData;
            }

            return null;
        }
    }

    /**
     * Network data producer of an "extended_master_secret" extension in
     * the ServerHello handshake message.
     */
    private static final
            class CHExtendedMasterSecretConsumer implements ExtensionConsumer {
        private CHExtendedMasterSecretConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(CH_EXTENDED_MASTER_SECRET) ||
                    !SSLConfiguration.useExtendedMasterSecret ||
                    !shc.negotiatedProtocol.useTLS10PlusSpec()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("Ignore unavailable extension: " +
                            CH_EXTENDED_MASTER_SECRET.name);
                }
                return;     
            }

            ExtendedMasterSecretSpec spec =
                    new ExtendedMasterSecretSpec(shc, buffer);
            if (shc.isResumption && shc.resumingSession != null &&
                    !shc.resumingSession.useExtendedMasterSecret) {
                shc.isResumption = false;
                shc.resumingSession = null;
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "abort session resumption which did not use " +
                        "Extended Master Secret extension");
                }
            }

            shc.handshakeExtensions.put(
                CH_EXTENDED_MASTER_SECRET, ExtendedMasterSecretSpec.NOMINAL);

        }
    }

    /**
     * The absence processing if an "extended_master_secret" extension is
     * not present in the ClientHello handshake message.
     */
    private static final
            class CHExtendedMasterSecretAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(CH_EXTENDED_MASTER_SECRET) ||
                    !SSLConfiguration.useExtendedMasterSecret) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("Ignore unavailable extension: " +
                            CH_EXTENDED_MASTER_SECRET.name);
                }
                return;     
            }

            if (shc.negotiatedProtocol.useTLS10PlusSpec() &&
                    !SSLConfiguration.allowLegacyMasterSecret) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Extended Master Secret extension is required");
            }

            if (shc.isResumption && shc.resumingSession != null) {
                if (shc.resumingSession.useExtendedMasterSecret) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Missing Extended Master Secret extension " +
                            "on session resumption");
                } else {
                    if (!SSLConfiguration.allowLegacyResumption) {
                        throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Missing Extended Master Secret extension " +
                            "on session resumption");
                    } else {  
                        shc.isResumption = false;
                        shc.resumingSession = null;
                        if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                            SSLLogger.fine(
                                "abort session resumption, " +
                                "missing Extended Master Secret extension");
                        }
                    }
                }
            }
        }
    }

    /**
     * Network data producer of an "extended_master_secret" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHExtendedMasterSecretProducer implements HandshakeProducer {
        private SHExtendedMasterSecretProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (shc.handshakeSession.useExtendedMasterSecret) {
                byte[] extData = new byte[0];
                shc.handshakeExtensions.put(SH_EXTENDED_MASTER_SECRET,
                        ExtendedMasterSecretSpec.NOMINAL);

                return extData;
            }

            return null;
        }
    }

    /**
     * Network data consumer of an "extended_master_secret" extension in
     * the ServerHello handshake message.
     */
    private static final
            class SHExtendedMasterSecretConsumer implements ExtensionConsumer {
        private SHExtendedMasterSecretConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            ExtendedMasterSecretSpec requstedSpec = (ExtendedMasterSecretSpec)
                    chc.handshakeExtensions.get(CH_EXTENDED_MASTER_SECRET);
            if (requstedSpec == null) {
                throw chc.conContext.fatal(Alert.UNSUPPORTED_EXTENSION,
                        "Server sent the extended_master_secret " +
                        "extension improperly");
            }

            ExtendedMasterSecretSpec spec =
                    new ExtendedMasterSecretSpec(chc, buffer);
            if (chc.isResumption && chc.resumingSession != null &&
                    !chc.resumingSession.useExtendedMasterSecret) {
                throw chc.conContext.fatal(Alert.UNSUPPORTED_EXTENSION,
                        "Server sent an unexpected extended_master_secret " +
                        "extension on session resumption");
            }

            chc.handshakeExtensions.put(
                SH_EXTENDED_MASTER_SECRET, ExtendedMasterSecretSpec.NOMINAL);

        }
    }

    /**
     * The absence processing if an "extended_master_secret" extension is
     * not present in the ServerHello handshake message.
     */
    private static final
            class SHExtendedMasterSecretAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (SSLConfiguration.useExtendedMasterSecret &&
                    !SSLConfiguration.allowLegacyMasterSecret) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Extended Master Secret extension is required");
            }

            if (chc.isResumption && chc.resumingSession != null) {
                if (chc.resumingSession.useExtendedMasterSecret) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "Missing Extended Master Secret extension " +
                            "on session resumption");
                } else if (SSLConfiguration.useExtendedMasterSecret &&
                        !SSLConfiguration.allowLegacyResumption &&
                        chc.negotiatedProtocol.useTLS10PlusSpec()) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Extended Master Secret extension is required");
                }
            }
        }
    }
}

