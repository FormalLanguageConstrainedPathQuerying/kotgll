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
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.SSLException;
import sun.security.ssl.SSLCipher.SSLReadCipher;
import sun.security.ssl.SSLCipher.SSLWriteCipher;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SSLTrafficKeyDerivation.LegacyTrafficKeyDerivation;

/**
 * Pack of the ChangeCipherSpec message.
 */
final class ChangeCipherSpec {
    static final SSLConsumer t10Consumer =
            new T10ChangeCipherSpecConsumer();
    static final HandshakeProducer t10Producer =
            new T10ChangeCipherSpecProducer();
    static final SSLConsumer t13Consumer =
            new T13ChangeCipherSpecConsumer();

    /**
     * The "ChangeCipherSpec" message producer.
     */
    private static final
            class T10ChangeCipherSpecProducer implements HandshakeProducer {
        private T10ChangeCipherSpecProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            HandshakeContext hc = (HandshakeContext)context;
            SSLKeyDerivation kd = hc.handshakeKeyDerivation;

            if (!(kd instanceof LegacyTrafficKeyDerivation tkd)) {
                throw new UnsupportedOperationException("Not supported.");
            }
            CipherSuite ncs = hc.negotiatedCipherSuite;
            Authenticator writeAuthenticator;
            if (ncs.bulkCipher.cipherType == CipherType.AEAD_CIPHER) {
                writeAuthenticator =
                        Authenticator.valueOf(hc.negotiatedProtocol);
            } else {
                try {
                    writeAuthenticator = Authenticator.valueOf(
                            hc.negotiatedProtocol, ncs.macAlg,
                            tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                    "clientMacKey" : "serverMacKey"));
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw new SSLException("Algorithm missing:  ", e);
                }
            }

            SecretKey writeKey =
                    tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                    "clientWriteKey" : "serverWriteKey");
            SecretKey writeIv =
                    tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                    "clientWriteIv" : "serverWriteIv");
            IvParameterSpec iv = (writeIv == null) ? null :
                    new IvParameterSpec(writeIv.getEncoded());
            SSLWriteCipher writeCipher;
            try {
                writeCipher = ncs.bulkCipher.createWriteCipher(
                        writeAuthenticator,
                        hc.negotiatedProtocol, writeKey, iv,
                        hc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                throw new SSLException("Algorithm missing:  ", gse);
            }

            if (writeCipher == null) {
                throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + ncs +
                    ") and protocol version (" + hc.negotiatedProtocol + ")");
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced ChangeCipherSpec message");
            }

            hc.conContext.outputRecord.changeWriteCiphers(writeCipher, true);

            return null;
        }
    }

    /**
     * The "ChangeCipherSpec" message producer.
     */
    private static final
            class T10ChangeCipherSpecConsumer implements SSLConsumer {
        private T10ChangeCipherSpecConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            TransportContext tc = (TransportContext)context;

            tc.consumers.remove(ContentType.CHANGE_CIPHER_SPEC.id);

            if (message.remaining() != 1 || message.get() != 1) {
                throw tc.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Malformed or unexpected ChangeCipherSpec message");
            }
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consuming ChangeCipherSpec message");
            }

            if (tc.handshakeContext == null) {
                throw tc.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unexpected ChangeCipherSpec message");
            }


            HandshakeContext hc = tc.handshakeContext;

            if (hc.handshakeKeyDerivation == null) {
                throw tc.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected ChangeCipherSpec message");
            }

            SSLKeyDerivation kd = hc.handshakeKeyDerivation;
            if (kd instanceof LegacyTrafficKeyDerivation tkd) {
                CipherSuite ncs = hc.negotiatedCipherSuite;
                Authenticator readAuthenticator;
                if (ncs.bulkCipher.cipherType == CipherType.AEAD_CIPHER) {
                    readAuthenticator =
                            Authenticator.valueOf(hc.negotiatedProtocol);
                } else {
                    try {
                        readAuthenticator = Authenticator.valueOf(
                                hc.negotiatedProtocol, ncs.macAlg,
                                tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                        "serverMacKey" : "clientMacKey"));
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        throw new SSLException("Algorithm missing:  ", e);
                    }
                }

                SecretKey readKey =
                        tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                        "serverWriteKey" : "clientWriteKey");
                SecretKey readIv =
                        tkd.getTrafficKey(hc.sslConfig.isClientMode ?
                                        "serverWriteIv" : "clientWriteIv");
                IvParameterSpec iv = (readIv == null) ? null :
                        new IvParameterSpec(readIv.getEncoded());
                SSLReadCipher readCipher;
                try {
                    readCipher = ncs.bulkCipher.createReadCipher(
                            readAuthenticator,
                            hc.negotiatedProtocol, readKey, iv,
                            hc.sslContext.getSecureRandom());
                } catch (GeneralSecurityException gse) {
                    throw new SSLException("Algorithm missing:  ", gse);
                }

                if (readCipher == null) {
                    throw hc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Illegal cipher suite (" + hc.negotiatedCipherSuite +
                        ") and protocol version (" + hc.negotiatedProtocol +
                        ")");
                }

                tc.inputRecord.changeReadCiphers(readCipher);
            } else {
                throw new UnsupportedOperationException("Not supported.");
            }
        }
    }

    private static final
            class T13ChangeCipherSpecConsumer implements SSLConsumer {
        private T13ChangeCipherSpecConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            TransportContext tc = (TransportContext)context;

            tc.consumers.remove(ContentType.CHANGE_CIPHER_SPEC.id);

            if (message.remaining() != 1 || message.get() != 1) {
                throw tc.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Malformed or unexpected ChangeCipherSpec message");
            }
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consuming ChangeCipherSpec message");
            }

        }
    }
}
