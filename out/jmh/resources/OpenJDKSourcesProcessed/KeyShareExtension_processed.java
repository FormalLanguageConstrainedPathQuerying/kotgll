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
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.*;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.NamedGroup.NamedGroupSpec;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the "key_share" extensions.
 */
final class KeyShareExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHKeyShareProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHKeyShareConsumer();
    static final HandshakeAbsence chOnTradAbsence =
            new CHKeyShareOnTradeAbsence();
    static final SSLStringizer chStringizer =
            new CHKeyShareStringizer();

    static final HandshakeProducer shNetworkProducer =
            new SHKeyShareProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHKeyShareConsumer();
    static final HandshakeAbsence shOnLoadAbsence =
            new SHKeyShareAbsence();
    static final SSLStringizer shStringizer =
            new SHKeyShareStringizer();

    static final HandshakeProducer hrrNetworkProducer =
            new HRRKeyShareProducer();
    static final ExtensionConsumer hrrOnLoadConsumer =
            new HRRKeyShareConsumer();
    static final HandshakeProducer hrrNetworkReproducer =
            new HRRKeyShareReproducer();
    static final SSLStringizer hrrStringizer =
            new HRRKeyShareStringizer();

    /**
     * The key share entry used in "key_share" extensions.
     */
    private static final class KeyShareEntry {
        final int namedGroupId;
        final byte[] keyExchange;

        private KeyShareEntry(int namedGroupId, byte[] keyExchange) {
            this.namedGroupId = namedGroupId;
            this.keyExchange = keyExchange;
        }

        private byte[] getEncoded() {
            byte[] buffer = new byte[keyExchange.length + 4];
            ByteBuffer m = ByteBuffer.wrap(buffer);
            try {
                Record.putInt16(m, namedGroupId);
                Record.putBytes16(m, keyExchange);
            } catch (IOException ioe) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "Unlikely IOException", ioe);
                }
            }

            return buffer;
        }

        private int getEncodedSize() {
            return keyExchange.length + 4;  
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """

                            '{'
                              "named group": {0}
                              "key_exchange": '{'
                            {1}
                              '}'
                            '}',""", Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                NamedGroup.nameOf(namedGroupId),
                Utilities.indent(hexEncoder.encode(keyExchange), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "key_share" extension in a ClientHello handshake message.
     */
    static final class CHKeyShareSpec implements SSLExtensionSpec {
        final List<KeyShareEntry> clientShares;

        private CHKeyShareSpec(List<KeyShareEntry> clientShares) {
            this.clientShares = clientShares;
        }

        private CHKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() < 2) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "insufficient data (length=" + buffer.remaining() + ")"));
            }

            int listLen = Record.getInt16(buffer);
            if (listLen != buffer.remaining()) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "incorrect list length (length=" + listLen + ")"));
            }

            List<KeyShareEntry> keyShares = new LinkedList<>();
            while (buffer.hasRemaining()) {
                int namedGroupId = Record.getInt16(buffer);
                byte[] keyExchange = Record.getBytes16(buffer);
                if (keyExchange.length == 0) {
                    throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                            new SSLProtocolException(
                        "Invalid key_share extension: empty key_exchange"));
                }

                keyShares.add(new KeyShareEntry(namedGroupId, keyExchange));
            }

            this.clientShares = Collections.unmodifiableList(keyShares);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"client_shares\": '['{0}\n']'", Locale.ENGLISH);

            StringBuilder builder = new StringBuilder(512);
            for (KeyShareEntry entry : clientShares) {
                builder.append(entry.toString());
            }

            Object[] messageFields = {
                Utilities.indent(builder.toString())
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class CHKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(
                HandshakeContext handshakeContext, ByteBuffer buffer) {
            try {
                return (new CHKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a ClientHello
     * handshake message.
     */
    private static final
            class CHKeyShareProducer implements HandshakeProducer {
        private CHKeyShareProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslConfig.isAvailable(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable key_share extension");
                }
                return null;
            }

            List<NamedGroup> namedGroups;
            if (chc.serverSelectedNamedGroup != null) {
                namedGroups = List.of(chc.serverSelectedNamedGroup);
            } else {
                namedGroups = chc.clientRequestedNamedGroups;
                if (namedGroups == null || namedGroups.isEmpty()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Ignore key_share extension, no supported groups");
                    }
                    return null;
                }
            }

            List<KeyShareEntry> keyShares = new LinkedList<>();
            EnumSet<NamedGroupSpec> ngTypes =
                    EnumSet.noneOf(NamedGroupSpec.class);
            byte[] keyExchangeData;
            for (NamedGroup ng : namedGroups) {
                if (!ngTypes.contains(ng.spec)) {
                    if ((keyExchangeData = getShare(chc, ng)) != null) {
                        keyShares.add(new KeyShareEntry(ng.id,
                                keyExchangeData));
                        ngTypes.add(ng.spec);
                        if (ngTypes.size() == 2) {
                            break;
                        }
                    }
                }
            }

            int listLen = 0;
            for (KeyShareEntry entry : keyShares) {
                listLen += entry.getEncodedSize();
            }
            byte[] extData = new byte[listLen + 2];     
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putInt16(m, listLen);
            for (KeyShareEntry entry : keyShares) {
                m.put(entry.getEncoded());
            }

            chc.handshakeExtensions.put(SSLExtension.CH_KEY_SHARE,
                    new CHKeyShareSpec(keyShares));

            return extData;
        }

        private static byte[] getShare(ClientHandshakeContext chc,
                NamedGroup ng) {
            SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
            if (ke == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "No key exchange for named group " + ng.name);
                }
            } else {
                SSLPossession[] poses = ke.createPossessions(chc);
                for (SSLPossession pos : poses) {
                    chc.handshakePossessions.add(pos);
                    if (pos instanceof NamedGroupPossession) {
                        return pos.encode();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Network data consumer of the extension in a ClientHello
     * handshake message.
     */
    private static final class CHKeyShareConsumer implements ExtensionConsumer {
        private CHKeyShareConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (shc.handshakeExtensions.containsKey(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "The key_share extension has been loaded");
                }
                return;
            }

            if (!shc.sslConfig.isAvailable(SSLExtension.CH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable key_share extension");
                }
                return;     
            }

            CHKeyShareSpec spec = new CHKeyShareSpec(shc, buffer);
            List<SSLCredentials> credentials = new LinkedList<>();
            for (KeyShareEntry entry : spec.clientShares) {
                NamedGroup ng = NamedGroup.valueOf(entry.namedGroupId);
                if (ng == null || !NamedGroup.isActivatable(shc.sslConfig,
                        shc.algorithmConstraints, ng)) {
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "Ignore unsupported named group: " +
                                NamedGroup.nameOf(entry.namedGroupId));
                    }
                    continue;
                }

                try {
                    SSLCredentials kaCred =
                        ng.decodeCredentials(entry.keyExchange);
                    if (shc.algorithmConstraints != null &&
                            kaCred instanceof
                                NamedGroupCredentials namedGroupCredentials) {
                        if (!shc.algorithmConstraints.permits(
                                EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                                namedGroupCredentials.getPublicKey())) {
                            if (SSLLogger.isOn &&
                                    SSLLogger.isOn("ssl,handshake")) {
                                SSLLogger.warning(
                                    "key share entry of " + ng + " does not " +
                                    " comply with algorithm constraints");
                            }

                            kaCred = null;
                        }
                    }

                    if (kaCred != null) {
                        credentials.add(kaCred);
                    }
                } catch (GeneralSecurityException ex) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                                "Cannot decode named group: " +
                                NamedGroup.nameOf(entry.namedGroupId));
                    }
                }
            }

            if (!credentials.isEmpty()) {
                shc.handshakeCredentials.addAll(credentials);
            } else {
                shc.handshakeProducers.put(
                        SSLHandshake.HELLO_RETRY_REQUEST.id,
                        SSLHandshake.HELLO_RETRY_REQUEST);
            }

            shc.handshakeExtensions.put(SSLExtension.CH_KEY_SHARE, spec);
        }
    }

    /**
     * The absence processing if the extension is not present in
     * a ClientHello handshake message.
     */
    private static final class CHKeyShareOnTradeAbsence
            implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (shc.negotiatedProtocol.useTLS13PlusSpec() &&
                    shc.handshakeExtensions.containsKey(
                            SSLExtension.CH_SUPPORTED_GROUPS)) {
                throw shc.conContext.fatal(Alert.MISSING_EXTENSION,
                        "No key_share extension to work with " +
                        "the supported_groups extension");
            }
        }
    }


    /**
     * The key share entry used in ServerHello "key_share" extensions.
     */
    static final class SHKeyShareSpec implements SSLExtensionSpec {
        final KeyShareEntry serverShare;

        SHKeyShareSpec(KeyShareEntry serverShare) {
            this.serverShare = serverShare;
        }

        private SHKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() < 5) {       
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "insufficient data (length=" + buffer.remaining() + ")"));
            }

            int namedGroupId = Record.getInt16(buffer);
            byte[] keyExchange = Record.getBytes16(buffer);

            if (buffer.hasRemaining()) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: unknown extra data"));
            }

            this.serverShare = new KeyShareEntry(namedGroupId, keyExchange);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "server_share": '{'
                              "named group": {0}
                              "key_exchange": '{'
                            {1}
                              '}'
                            '}',""", Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                NamedGroup.nameOf(serverShare.namedGroupId),
                Utilities.indent(
                        hexEncoder.encode(serverShare.keyExchange), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class SHKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext handshakeContext,
                ByteBuffer buffer) {
            try {
                return (new SHKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a ServerHello
     * handshake message.
     */
    private static final class SHKeyShareProducer implements HandshakeProducer {
        private SHKeyShareProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            CHKeyShareSpec kss =
                    (CHKeyShareSpec)shc.handshakeExtensions.get(
                            SSLExtension.CH_KEY_SHARE);
            if (kss == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no client key_share extension");
                }
                return null;
            }

            if (!shc.sslConfig.isAvailable(SSLExtension.SH_KEY_SHARE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no available server key_share extension");
                }
                return null;
            }

            if ((shc.handshakeCredentials == null) ||
                    shc.handshakeCredentials.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "No available client key share entries");
                }
                return null;
            }

            KeyShareEntry keyShare = null;
            for (SSLCredentials cd : shc.handshakeCredentials) {
                NamedGroup ng = null;
                if (cd instanceof NamedGroupCredentials creds) {
                    ng = creds.getNamedGroup();
                }

                if (ng == null) {
                    continue;
                }

                SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
                if (ke == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "No key exchange for named group " + ng.name);
                    }
                    continue;
                }

                SSLPossession[] poses = ke.createPossessions(shc);
                for (SSLPossession pos : poses) {
                    if (!(pos instanceof NamedGroupPossession)) {
                        continue;
                    }

                    shc.handshakeKeyExchange = ke;
                    shc.handshakePossessions.add(pos);
                    keyShare = new KeyShareEntry(ng.id, pos.encode());
                    break;
                }

                if (keyShare != null) {
                    for (Map.Entry<Byte, HandshakeProducer> me :
                            ke.getHandshakeProducers(shc)) {
                        shc.handshakeProducers.put(
                                me.getKey(), me.getValue());
                    }

                    break;
                }
            }

            if (keyShare == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "No available server key_share extension");
                }
                return null;
            }

            byte[] extData = keyShare.getEncoded();

            SHKeyShareSpec spec = new SHKeyShareSpec(keyShare);
            shc.handshakeExtensions.put(SSLExtension.SH_KEY_SHARE, spec);

            return extData;
        }
    }

    /**
     * Network data consumer of the extension in a ServerHello
     * handshake message.
     */
    private static final class SHKeyShareConsumer implements ExtensionConsumer {
        private SHKeyShareConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            if (chc.clientRequestedNamedGroups == null ||
                    chc.clientRequestedNamedGroups.isEmpty()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in ServerHello");
            }

            if (!chc.sslConfig.isAvailable(SSLExtension.SH_KEY_SHARE)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in ServerHello");
            }

            SHKeyShareSpec spec = new SHKeyShareSpec(chc, buffer);
            KeyShareEntry keyShare = spec.serverShare;
            NamedGroup ng = NamedGroup.valueOf(keyShare.namedGroupId);
            if (ng == null || !NamedGroup.isActivatable(chc.sslConfig,
                    chc.algorithmConstraints, ng)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported named group: " +
                        NamedGroup.nameOf(keyShare.namedGroupId));
            }

            SSLKeyExchange ke = SSLKeyExchange.valueOf(ng);
            if (ke == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "No key exchange for named group " + ng.name);
            }

            SSLCredentials credentials = null;
            try {
                SSLCredentials kaCred =
                        ng.decodeCredentials(keyShare.keyExchange);
                if (chc.algorithmConstraints != null &&
                        kaCred instanceof
                                NamedGroupCredentials namedGroupCredentials) {
                    if (!chc.algorithmConstraints.permits(
                            EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                            namedGroupCredentials.getPublicKey())) {
                        chc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                            "key share entry of " + ng + " does not " +
                            " comply with algorithm constraints");
                    }
                }

                if (kaCred != null) {
                    credentials = kaCred;
                }
            } catch (GeneralSecurityException ex) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Cannot decode named group: " +
                        NamedGroup.nameOf(keyShare.namedGroupId));
            }

            if (credentials == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported named group: " + ng.name);
            }

            chc.handshakeKeyExchange = ke;
            chc.handshakeCredentials.add(credentials);
            chc.handshakeExtensions.put(SSLExtension.SH_KEY_SHARE, spec);
        }
    }

    /**
     * The absence processing if the extension is not present in
     * the ServerHello handshake message.
     */
    private static final class SHKeyShareAbsence implements HandshakeAbsence {
        @Override
        public void absent(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                SSLLogger.fine(
                        "No key_share extension in ServerHello, " +
                        "cleanup the key shares if necessary");
            }
            chc.handshakePossessions.clear();
        }
    }

    /**
     * The key share entry used in HelloRetryRequest "key_share" extensions.
     */
    static final class HRRKeyShareSpec implements SSLExtensionSpec {
        final int selectedGroup;

        HRRKeyShareSpec(NamedGroup serverGroup) {
            this.selectedGroup = serverGroup.id;
        }

        private HRRKeyShareSpec(HandshakeContext handshakeContext,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() != 2) {
                throw handshakeContext.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid key_share extension: " +
                    "improper data (length=" + buffer.remaining() + ")"));
            }

            this.selectedGroup = Record.getInt16(buffer);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"selected group\": '['{0}']'", Locale.ENGLISH);

            Object[] messageFields = {
                    NamedGroup.nameOf(selectedGroup)
                };
            return messageFormat.format(messageFields);
        }
    }

    private static final class HRRKeyShareStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext handshakeContext,
                ByteBuffer buffer) {
            try {
                return (new HRRKeyShareSpec(handshakeContext, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of the extension in a HelloRetryRequest
     * handshake message.
     */
    private static final
            class HRRKeyShareProducer implements HandshakeProducer {
        private HRRKeyShareProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            if (shc.clientRequestedNamedGroups == null ||
                    shc.clientRequestedNamedGroups.isEmpty()) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in HelloRetryRequest");
            }

            NamedGroup selectedGroup = null;
            for (NamedGroup ng : shc.clientRequestedNamedGroups) {
                if (NamedGroup.isActivatable(shc.sslConfig,
                        shc.algorithmConstraints, ng)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "HelloRetryRequest selected named group: " +
                                ng.name);
                    }

                    selectedGroup = ng;
                    break;
                }
            }

            if (selectedGroup == null) {
                throw shc.conContext.fatal(
                        Alert.UNEXPECTED_MESSAGE, "No common named group");
            }

            byte[] extdata = new byte[] {
                    (byte)((selectedGroup.id >> 8) & 0xFF),
                    (byte)(selectedGroup.id & 0xFF)
                };

            shc.serverSelectedNamedGroup = selectedGroup;
            shc.handshakeExtensions.put(SSLExtension.HRR_KEY_SHARE,
                    new HRRKeyShareSpec(selectedGroup));

            return extdata;
        }
    }

    /**
     * Network data producer of the extension for stateless
     * HelloRetryRequest reconstruction.
     */
    private static final
            class HRRKeyShareReproducer implements HandshakeProducer {
        private HRRKeyShareReproducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            CHKeyShareSpec spec = (CHKeyShareSpec)shc.handshakeExtensions.get(
                    SSLExtension.CH_KEY_SHARE);
            if (spec != null && spec.clientShares != null &&
                    spec.clientShares.size() == 1) {
                int namedGroupId = spec.clientShares.get(0).namedGroupId;

                return new byte[] {
                        (byte)((namedGroupId >> 8) & 0xFF),
                        (byte)(namedGroupId & 0xFF)
                    };
            }

            return null;
        }
    }

    /**
     * Network data consumer of the extension in a HelloRetryRequest
     * handshake message.
     */
    private static final
            class HRRKeyShareConsumer implements ExtensionConsumer {
        private HRRKeyShareConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslConfig.isAvailable(SSLExtension.HRR_KEY_SHARE)) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unsupported key_share extension in HelloRetryRequest");
            }

            if (chc.clientRequestedNamedGroups == null ||
                    chc.clientRequestedNamedGroups.isEmpty()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected key_share extension in HelloRetryRequest");
            }

            HRRKeyShareSpec spec = new HRRKeyShareSpec(chc, buffer);
            NamedGroup serverGroup = NamedGroup.valueOf(spec.selectedGroup);
            if (serverGroup == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Unsupported HelloRetryRequest selected group: " +
                                NamedGroup.nameOf(spec.selectedGroup));
            }

            if (!chc.clientRequestedNamedGroups.contains(serverGroup)) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Unexpected HelloRetryRequest selected group: " +
                                serverGroup.name);
            }
            CHKeyShareSpec chKsSpec = (CHKeyShareSpec)
                    chc.handshakeExtensions.get(SSLExtension.CH_KEY_SHARE);
            if (chKsSpec != null) {
                for (KeyShareEntry kse : chKsSpec.clientShares) {
                    if (serverGroup.id == kse.namedGroupId) {
                        throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                                "Illegal HelloRetryRequest selected group: " +
                                        serverGroup.name);
                    }
                }
            } else {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unable to retrieve ClientHello key_share extension " +
                                "during HRR processing");
            }


            chc.serverSelectedNamedGroup = serverGroup;
            chc.handshakeExtensions.put(SSLExtension.HRR_KEY_SHARE, spec);
        }
    }
}
