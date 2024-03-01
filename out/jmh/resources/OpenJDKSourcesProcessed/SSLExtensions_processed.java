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
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.*;

import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.HexDumpEncoder;

/**
 * SSL/(D)TLS extensions in a handshake message.
 */
final class SSLExtensions {
    private final HandshakeMessage handshakeMessage;
    private final Map<SSLExtension, byte[]> extMap = new LinkedHashMap<>();
    private int encodedLength;

    private final Map<Integer, byte[]> logMap =
            SSLLogger.isOn ? new LinkedHashMap<>() : null;

    SSLExtensions(HandshakeMessage handshakeMessage) {
        this.handshakeMessage = handshakeMessage;
        this.encodedLength = 2;         
    }

    SSLExtensions(HandshakeMessage hm,
            ByteBuffer m, SSLExtension[] extensions) throws IOException {
        this.handshakeMessage = hm;

        if (m.remaining() < 2) {
            throw hm.handshakeContext.conContext.fatal(
                    Alert.DECODE_ERROR,
                    "Incorrect extensions: no length field");
        }

        int len = Record.getInt16(m);
        if (len > m.remaining()) {
            throw hm.handshakeContext.conContext.fatal(
                    Alert.DECODE_ERROR,
                    "Insufficient extensions data");
        }

        encodedLength = len + 2;        
        while (len > 0) {
            int extId = Record.getInt16(m);
            int extLen = Record.getInt16(m);
            if (extLen > m.remaining()) {
                throw hm.handshakeContext.conContext.fatal(
                        Alert.DECODE_ERROR,
                        "Error parsing extension (" + extId +
                        "): no sufficient data");
            }

            boolean isSupported = true;
            SSLHandshake handshakeType = hm.handshakeType();
            if (SSLExtension.isConsumable(extId) &&
                    SSLExtension.valueOf(handshakeType, extId) == null) {
                if (extId == SSLExtension.CH_SUPPORTED_GROUPS.id &&
                        handshakeType == SSLHandshake.SERVER_HELLO) {
                    isSupported = false;
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                                "Received buggy supported_groups extension " +
                                "in the ServerHello handshake message");
                    }
                } else if (handshakeType == SSLHandshake.SERVER_HELLO) {
                    throw hm.handshakeContext.conContext.fatal(
                            Alert.UNSUPPORTED_EXTENSION, "extension (" +
                                    extId + ") should not be presented in " +
                                    handshakeType.name);
                } else {
                    isSupported = false;
                }
            }

            if (isSupported) {
                isSupported = false;
                for (SSLExtension extension : extensions) {
                    if ((extension.id != extId) ||
                            (extension.onLoadConsumer == null)) {
                        continue;
                    }

                    if (extension.handshakeType != handshakeType) {
                        throw hm.handshakeContext.conContext.fatal(
                                Alert.UNSUPPORTED_EXTENSION,
                                "extension (" + extId + ") should not be " +
                                "presented in " + handshakeType.name);
                    }

                    byte[] extData = new byte[extLen];
                    m.get(extData);
                    extMap.put(extension, extData);
                    if (logMap != null) {
                        logMap.put(extId, extData);
                    }

                    isSupported = true;
                    break;
                }
            }

            if (!isSupported) {
                if (logMap != null) {
                    byte[] extData = new byte[extLen];
                    m.get(extData);
                    logMap.put(extId, extData);

                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                                "Ignore unknown or unsupported extension",
                                toString(extId, extData));
                    }
                } else {
                    int pos = m.position() + extLen;
                    m.position(pos);
                }
            }

            len -= extLen + 4;
        }
    }

    byte[] get(SSLExtension ext) {
        return extMap.get(ext);
    }

    /**
     * Consume the specified extensions.
     */
    void consumeOnLoad(HandshakeContext context,
            SSLExtension[] extensions) throws IOException {
        for (SSLExtension extension : extensions) {
            if (context.negotiatedProtocol != null &&
                    !extension.isAvailable(context.negotiatedProtocol)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unsupported extension: " + extension.name);
                }
                continue;
            }

            if (!extMap.containsKey(extension)) {
                if (extension.onLoadAbsence != null) {
                    extension.absentOnLoad(context, handshakeMessage);
                } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " + extension.name);
                }
                continue;
            }


            if (extension.onLoadConsumer == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                        "Ignore unsupported extension: " + extension.name);
                }
                continue;
            }

            ByteBuffer m = ByteBuffer.wrap(extMap.get(extension));
            extension.consumeOnLoad(context, handshakeMessage, m);

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consumed extension: " + extension.name);
            }
        }
    }

    /**
     * Consider impact of the specified extensions.
     */
    void consumeOnTrade(HandshakeContext context,
            SSLExtension[] extensions) throws IOException {
        for (SSLExtension extension : extensions) {
            if (!extMap.containsKey(extension)) {
                if (extension.onTradeAbsence != null) {
                    extension.absentOnTrade(context, handshakeMessage);
                } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " + extension.name);
                }
                continue;
            }

            if (extension.onTradeConsumer == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore impact of unsupported extension: " +
                            extension.name);
                }
                continue;
            }

            extension.consumeOnTrade(context, handshakeMessage);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Populated with extension: " + extension.name);
            }
        }
    }

    /**
     * Produce extension values for the specified extensions.
     */
    void produce(HandshakeContext context,
            SSLExtension[] extensions) throws IOException {
        for (SSLExtension extension : extensions) {
            if (extMap.containsKey(extension)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore, duplicated extension: " +
                            extension.name);
                }
                continue;
            }

            if (extension.networkProducer == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no extension producer defined: " +
                            extension.name);
                }
                continue;
            }

            byte[] encoded = extension.produce(context, handshakeMessage);
            if (encoded != null) {
                extMap.put(extension, encoded);
                encodedLength += encoded.length + 4; 
            } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Ignore, context unavailable extension: " +
                        extension.name);
            }
        }
    }

    /**
     * Produce extension values for the specified extensions, replacing if
     * there is an existing extension value for a specified extension.
     */
    void reproduce(HandshakeContext context,
            SSLExtension[] extensions) throws IOException {
        for (SSLExtension extension : extensions) {
            if (extension.networkProducer == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "Ignore, no extension producer defined: " +
                            extension.name);
                }
                continue;
            }

            byte[] encoded = extension.produce(context, handshakeMessage);
            if (encoded != null) {
                if (extMap.containsKey(extension)) {
                    byte[] old = extMap.replace(extension, encoded);
                    if (old != null) {
                        encodedLength -= old.length + 4;
                    }
                } else {
                    extMap.put(extension, encoded);
                }
                encodedLength += encoded.length + 4;
            } else if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Ignore, context unavailable extension: " +
                        extension.name);
            }
        }
    }

    int length() {
        if (extMap.isEmpty()) {
            return 0;
        } else {
            return encodedLength;
        }
    }

    void send(HandshakeOutStream hos) throws IOException {
        int extsLen = length();
        if (extsLen == 0) {
            return;
        }
        hos.putInt16(extsLen - 2);
        for (SSLExtension ext : SSLExtension.values()) {
            byte[] extData = extMap.get(ext);
            if (extData != null) {
                hos.putInt16(ext.id);
                hos.putBytes16(extData);
            }
        }
    }

    @Override
    public String toString() {
        if (extMap.isEmpty() && (logMap == null || logMap.isEmpty())) {
            return "<no extension>";
        } else {
            StringBuilder builder = new StringBuilder(512);
            if (logMap != null && !logMap.isEmpty()) {
                for (Map.Entry<Integer, byte[]> en : logMap.entrySet()) {
                    SSLExtension ext = SSLExtension.valueOf(
                            handshakeMessage.handshakeType(), en.getKey());
                    if (builder.length() != 0) {
                        builder.append(",\n");
                    }
                    if (ext != null) {
                        builder.append(
                            ext.toString(handshakeMessage.handshakeContext,
                                    ByteBuffer.wrap(en.getValue())));
                    } else {
                        builder.append(toString(en.getKey(), en.getValue()));
                    }
                }

            } else {
                for (Map.Entry<SSLExtension, byte[]> en : extMap.entrySet()) {
                    if (builder.length() != 0) {
                        builder.append(",\n");
                    }
                    builder.append(
                        en.getKey().toString(handshakeMessage.handshakeContext,
                                ByteBuffer.wrap(en.getValue())));
                }

            }
            return builder.toString();
        }
    }

    private static String toString(int extId, byte[] extData) {
        String extName = SSLExtension.nameOf(extId);
        MessageFormat messageFormat = new MessageFormat(
                """
                        "{0} ({1})": '{'
                        {2}
                        '}'""",
            Locale.ENGLISH);

        HexDumpEncoder hexEncoder = new HexDumpEncoder();
        String encoded = hexEncoder.encodeBuffer(extData);

        Object[] messageFields = {
            extName,
            extId,
            Utilities.indent(encoded)
        };

        return messageFormat.format(messageFields);
    }
}
