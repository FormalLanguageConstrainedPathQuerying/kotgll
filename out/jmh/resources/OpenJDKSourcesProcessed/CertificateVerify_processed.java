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
import java.security.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the CertificateVerify handshake message.
 */
final class CertificateVerify {
    static final SSLConsumer s30HandshakeConsumer =
        new S30CertificateVerifyConsumer();
    static final HandshakeProducer s30HandshakeProducer =
        new S30CertificateVerifyProducer();

    static final SSLConsumer t10HandshakeConsumer =
        new T10CertificateVerifyConsumer();
    static final HandshakeProducer t10HandshakeProducer =
        new T10CertificateVerifyProducer();

    static final SSLConsumer t12HandshakeConsumer =
        new T12CertificateVerifyConsumer();
    static final HandshakeProducer t12HandshakeProducer =
        new T12CertificateVerifyProducer();

    static final SSLConsumer t13HandshakeConsumer =
        new T13CertificateVerifyConsumer();
    static final HandshakeProducer t13HandshakeProducer =
        new T13CertificateVerifyProducer();

    /**
     * The CertificateVerify handshake message (SSL 3.0).
     */
    static final class S30CertificateVerifyMessage extends HandshakeMessage {
        private final byte[] signature;

        S30CertificateVerifyMessage(HandshakeContext context,
                X509Possession x509Possession) throws IOException {
            super(context);

            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            byte[] temporary;
            String algorithm = x509Possession.popPrivateKey.getAlgorithm();
            try {
                Signature signer =
                        getSignature(algorithm, x509Possession.popPrivateKey);
                byte[] hashes = chc.handshakeHash.digest(algorithm,
                        chc.handshakeSession.getMasterSecret());
                signer.update(hashes);
                temporary = signer.sign();
            } catch (NoSuchAlgorithmException nsae) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" + algorithm +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (GeneralSecurityException gse) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot produce CertificateVerify signature", gse);
            }

            this.signature = temporary;
        }

        S30CertificateVerifyMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (m.remaining() < 2) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid CertificateVerify message: no sufficient data");
            }

            this.signature = Record.getBytes16(m);
            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : shc.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null ||
                    x509Credentials.popPublicKey == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No X509 credentials negotiated for CertificateVerify");
            }

            String algorithm = x509Credentials.popPublicKey.getAlgorithm();
            try {
                Signature signer =
                        getSignature(algorithm, x509Credentials.popPublicKey);
                byte[] hashes = shc.handshakeHash.digest(algorithm,
                        shc.handshakeSession.getMasterSecret());
                signer.update(hashes);
                if (!signer.verify(signature)) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid CertificateVerify message: invalid signature");
                }
            } catch (NoSuchAlgorithmException nsae) {
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" + algorithm +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (GeneralSecurityException gse) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify CertificateVerify signature", gse);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_VERIFY;
        }

        @Override
        public int messageLength() {
            return 2 + signature.length;    
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes16(signature);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "CertificateVerify": '{'
                              "signature": '{'
                            {0}
                              '}'
                            '}'""",
                    Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                Utilities.indent(
                        hexEncoder.encodeBuffer(signature), "    ")
            };

            return messageFormat.format(messageFields);
        }

        /*
         * Get the Signature object appropriate for verification using the
         * given signature algorithm.
         */
        private static Signature getSignature(String algorithm,
                Key key) throws GeneralSecurityException {
            Signature signer;
            switch (algorithm) {
                case "RSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWRSA);
                    break;
                case "DSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWDSA);
                    break;
                case "EC":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWECDSA);
                    break;
                default:
                    throw new SignatureException("Unrecognized algorithm: "
                        + algorithm);
            }

            if (key instanceof PublicKey) {
                signer.initVerify((PublicKey)(key));
            } else {
                signer.initSign((PrivateKey)key);
            }

            return signer;
        }
    }

    /**
     * The "CertificateVerify" handshake message producer.
     */
    private static final
            class S30CertificateVerifyProducer implements HandshakeProducer {
        private S30CertificateVerifyProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            X509Possession x509Possession = null;
            for (SSLPossession possession : chc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null ||
                    x509Possession.popPrivateKey == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No X.509 credentials negotiated for CertificateVerify");
                }

                return null;
            }

            S30CertificateVerifyMessage cvm =
                    new S30CertificateVerifyMessage(chc, x509Possession);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced CertificateVerify handshake message", cvm);
            }

            cvm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            return null;
        }
    }

    /**
     * The "CertificateVerify" handshake message consumer.
     */
    private static final
            class S30CertificateVerifyConsumer implements SSLConsumer {
        private S30CertificateVerifyConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            shc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_VERIFY.id);

            if (shc.handshakeConsumers.containsKey(
                    SSLHandshake.CLIENT_KEY_EXCHANGE.id)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected CertificateVerify handshake message");
            }

            S30CertificateVerifyMessage cvm =
                    new S30CertificateVerifyMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming CertificateVerify handshake message", cvm);
            }


        }
    }

    /**
     * The CertificateVerify handshake message (TLS 1.0/1.1).
     */
    static final class T10CertificateVerifyMessage extends HandshakeMessage {
        private final byte[] signature;

        T10CertificateVerifyMessage(HandshakeContext context,
                X509Possession x509Possession) throws IOException {
            super(context);

            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            byte[] temporary;
            String algorithm = x509Possession.popPrivateKey.getAlgorithm();
            try {
                Signature signer =
                        getSignature(algorithm, x509Possession.popPrivateKey);
                byte[] hashes = chc.handshakeHash.digest(algorithm);
                signer.update(hashes);
                temporary = signer.sign();
            } catch (NoSuchAlgorithmException nsae) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" + algorithm +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (GeneralSecurityException gse) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Cannot produce CertificateVerify signature", gse);
            }

            this.signature = temporary;
        }

        T10CertificateVerifyMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (m.remaining() < 2) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid CertificateVerify message: no sufficient data");
            }

            this.signature = Record.getBytes16(m);
            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : shc.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null ||
                    x509Credentials.popPublicKey == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No X509 credentials negotiated for CertificateVerify");
            }

            String algorithm = x509Credentials.popPublicKey.getAlgorithm();
            try {
                Signature signer =
                        getSignature(algorithm, x509Credentials.popPublicKey);
                byte[] hashes = shc.handshakeHash.digest(algorithm);
                signer.update(hashes);
                if (!signer.verify(signature)) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid CertificateVerify message: invalid signature");
                }
            } catch (NoSuchAlgorithmException nsae) {
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" + algorithm +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (GeneralSecurityException gse) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify CertificateVerify signature", gse);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_VERIFY;
        }

        @Override
        public int messageLength() {
            return 2 + signature.length;    
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes16(signature);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "CertificateVerify": '{'
                              "signature": '{'
                            {0}
                              '}'
                            '}'""",
                    Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                Utilities.indent(
                        hexEncoder.encodeBuffer(signature), "    ")
            };

            return messageFormat.format(messageFields);
        }

        /*
         * Get the Signature object appropriate for verification using the
         * given signature algorithm.
         */
        private static Signature getSignature(String algorithm,
                Key key) throws GeneralSecurityException {
            Signature signer;
            switch (algorithm) {
                case "RSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWRSA);
                    break;
                case "DSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWDSA);
                    break;
                case "EC":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_RAWECDSA);
                    break;
                case "EdDSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_EDDSA);
                    break;
                default:
                    throw new SignatureException("Unrecognized algorithm: "
                        + algorithm);
            }

            if (key instanceof PublicKey) {
                signer.initVerify((PublicKey)(key));
            } else {
                signer.initSign((PrivateKey)key);
            }

            return signer;
        }
    }

    /**
     * The "CertificateVerify" handshake message producer.
     */
    private static final
            class T10CertificateVerifyProducer implements HandshakeProducer {
        private T10CertificateVerifyProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            X509Possession x509Possession = null;
            for (SSLPossession possession : chc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null ||
                    x509Possession.popPrivateKey == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No X.509 credentials negotiated for CertificateVerify");
                }

                return null;
            }

            T10CertificateVerifyMessage cvm =
                    new T10CertificateVerifyMessage(chc, x509Possession);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced CertificateVerify handshake message", cvm);
            }

            cvm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            return null;
        }
    }

    /**
     * The "CertificateVerify" handshake message consumer.
     */
    private static final
            class T10CertificateVerifyConsumer implements SSLConsumer {
        private T10CertificateVerifyConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            shc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_VERIFY.id);

            if (shc.handshakeConsumers.containsKey(
                    SSLHandshake.CLIENT_KEY_EXCHANGE.id)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected CertificateVerify handshake message");
            }

            T10CertificateVerifyMessage cvm =
                    new T10CertificateVerifyMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateVerify handshake message", cvm);
            }


        }
    }

    /**
     * The CertificateVerify handshake message (TLS 1.2).
     */
    static final class T12CertificateVerifyMessage extends HandshakeMessage {
        private final SignatureScheme signatureScheme;

        private final byte[] signature;

        T12CertificateVerifyMessage(HandshakeContext context,
                X509Possession x509Possession) throws IOException {
            super(context);

            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            Map.Entry<SignatureScheme, Signature> schemeAndSigner =
                    SignatureScheme.getSignerOfPreferableAlgorithm(
                    chc.sslConfig,
                    chc.algorithmConstraints,
                    chc.peerRequestedSignatureSchemes,
                    x509Possession,
                    chc.negotiatedProtocol);
            if (schemeAndSigner == null) {
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No supported CertificateVerify signature algorithm for " +
                    x509Possession.popPrivateKey.getAlgorithm() +
                    "  key");
            }

            this.signatureScheme = schemeAndSigner.getKey();
            byte[] temporary;
            try {
                Signature signer = schemeAndSigner.getValue();
                signer.update(chc.handshakeHash.archived());
                temporary = signer.sign();
            } catch (SignatureException ikse) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot produce CertificateVerify signature", ikse);
            }

            this.signature = temporary;
        }

        T12CertificateVerifyMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            ServerHandshakeContext shc =
                    (ServerHandshakeContext)handshakeContext;

            if (m.remaining() < 4) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid CertificateVerify message: no sufficient data");
            }

            int ssid = Record.getInt16(m);
            this.signatureScheme = SignatureScheme.valueOf(ssid);
            if (signatureScheme == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid signature algorithm (" + ssid +
                        ") used in CertificateVerify handshake message");
            }

            if (!shc.localSupportedSignAlgs.contains(signatureScheme)) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unsupported signature algorithm (" +
                        signatureScheme.name +
                        ") used in CertificateVerify handshake message");
            }

            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : shc.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null ||
                    x509Credentials.popPublicKey == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No X509 credentials negotiated for CertificateVerify");
            }

            this.signature = Record.getBytes16(m);
            try {
                Signature signer =
                    signatureScheme.getVerifier(x509Credentials.popPublicKey);
                signer.update(shc.handshakeHash.archived());
                if (!signer.verify(signature)) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid CertificateVerify signature");
                }
            } catch (NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException nsae) {
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" +
                        signatureScheme.name +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (InvalidKeyException | SignatureException ikse) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify CertificateVerify signature", ikse);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_VERIFY;
        }

        @Override
        public int messageLength() {
            return 4 + signature.length;    
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt16(signatureScheme.id);
            hos.putBytes16(signature);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "CertificateVerify": '{'
                              "signature algorithm": {0}
                              "signature": '{'
                            {1}
                              '}'
                            '}'""",
                    Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                signatureScheme.name,
                Utilities.indent(
                        hexEncoder.encodeBuffer(signature), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "CertificateVerify" handshake message producer.
     */
    private static final
            class T12CertificateVerifyProducer implements HandshakeProducer {
        private T12CertificateVerifyProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            X509Possession x509Possession = null;
            for (SSLPossession possession : chc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null ||
                    x509Possession.popPrivateKey == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No X.509 credentials negotiated for CertificateVerify");
                }

                return null;
            }

            T12CertificateVerifyMessage cvm =
                    new T12CertificateVerifyMessage(chc, x509Possession);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced CertificateVerify handshake message", cvm);
            }

            cvm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            return null;
        }
    }

    /**
     * The "CertificateVerify" handshake message consumer.
     */
    private static final
            class T12CertificateVerifyConsumer implements SSLConsumer {
        private T12CertificateVerifyConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            shc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_VERIFY.id);

            if (shc.handshakeConsumers.containsKey(
                    SSLHandshake.CLIENT_KEY_EXCHANGE.id)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected CertificateVerify handshake message");
            }

            T12CertificateVerifyMessage cvm =
                    new T12CertificateVerifyMessage(shc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateVerify handshake message", cvm);
            }


        }
    }

    /**
     * The CertificateVerify handshake message (TLS 1.3).
     */
    static final class T13CertificateVerifyMessage extends HandshakeMessage {
        private static final byte[] serverSignHead = new byte[] {
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,

            (byte)0x54, (byte)0x4c, (byte)0x53, (byte)0x20,
            (byte)0x31, (byte)0x2e, (byte)0x33, (byte)0x2c,
            (byte)0x20, (byte)0x73, (byte)0x65, (byte)0x72,
            (byte)0x76, (byte)0x65, (byte)0x72, (byte)0x20,
            (byte)0x43, (byte)0x65, (byte)0x72, (byte)0x74,
            (byte)0x69, (byte)0x66, (byte)0x69, (byte)0x63,
            (byte)0x61, (byte)0x74, (byte)0x65, (byte)0x56,
            (byte)0x65, (byte)0x72, (byte)0x69, (byte)0x66,
            (byte)0x79, (byte)0x00
        };

        private static final byte[] clientSignHead = new byte[] {
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,

            (byte)0x54, (byte)0x4c, (byte)0x53, (byte)0x20,
            (byte)0x31, (byte)0x2e, (byte)0x33, (byte)0x2c,
            (byte)0x20, (byte)0x63, (byte)0x6c, (byte)0x69,
            (byte)0x65, (byte)0x6e, (byte)0x74, (byte)0x20,
            (byte)0x43, (byte)0x65, (byte)0x72, (byte)0x74,
            (byte)0x69, (byte)0x66, (byte)0x69, (byte)0x63,
            (byte)0x61, (byte)0x74, (byte)0x65, (byte)0x56,
            (byte)0x65, (byte)0x72, (byte)0x69, (byte)0x66,
            (byte)0x79, (byte)0x00
        };


        private final SignatureScheme signatureScheme;

        private final byte[] signature;

        T13CertificateVerifyMessage(HandshakeContext context,
                X509Possession x509Possession) throws IOException {
            super(context);

            Map.Entry<SignatureScheme, Signature> schemeAndSigner =
                    SignatureScheme.getSignerOfPreferableAlgorithm(
                    context.sslConfig,
                    context.algorithmConstraints,
                    context.peerRequestedSignatureSchemes,
                    x509Possession,
                    context.negotiatedProtocol);
            if (schemeAndSigner == null) {
                throw context.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No supported CertificateVerify signature algorithm for " +
                    x509Possession.popPrivateKey.getAlgorithm() +
                    "  key");
            }

            this.signatureScheme = schemeAndSigner.getKey();

            byte[] hashValue = context.handshakeHash.digest();
            byte[] contentCovered;
            if (context.sslConfig.isClientMode) {
                contentCovered = Arrays.copyOf(clientSignHead,
                        clientSignHead.length + hashValue.length);
                System.arraycopy(hashValue, 0, contentCovered,
                        clientSignHead.length, hashValue.length);
            } else {
                contentCovered = Arrays.copyOf(serverSignHead,
                        serverSignHead.length + hashValue.length);
                System.arraycopy(hashValue, 0, contentCovered,
                        serverSignHead.length, hashValue.length);
            }

            byte[] temporary;
            try {
                Signature signer = schemeAndSigner.getValue();
                signer.update(contentCovered);
                temporary = signer.sign();
            } catch (SignatureException ikse) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot produce CertificateVerify signature", ikse);
            }

            this.signature = temporary;
        }

        T13CertificateVerifyMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
             super(context);

            if (m.remaining() < 4) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid CertificateVerify message: no sufficient data");
            }

            int ssid = Record.getInt16(m);
            this.signatureScheme = SignatureScheme.valueOf(ssid);
            if (signatureScheme == null) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid signature algorithm (" + ssid +
                        ") used in CertificateVerify handshake message");
            }

            if (!context.localSupportedSignAlgs.contains(signatureScheme)) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unsupported signature algorithm (" +
                        signatureScheme.name +
                        ") used in CertificateVerify handshake message");
            }

            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : context.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null ||
                    x509Credentials.popPublicKey == null) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No X509 credentials negotiated for CertificateVerify");
            }

            this.signature = Record.getBytes16(m);

            byte[] hashValue = context.handshakeHash.digest();
            byte[] contentCovered;
            if (context.sslConfig.isClientMode) {
                contentCovered = Arrays.copyOf(serverSignHead,
                        serverSignHead.length + hashValue.length);
                System.arraycopy(hashValue, 0, contentCovered,
                        serverSignHead.length, hashValue.length);
            } else {
                contentCovered = Arrays.copyOf(clientSignHead,
                        clientSignHead.length + hashValue.length);
                System.arraycopy(hashValue, 0, contentCovered,
                        clientSignHead.length, hashValue.length);
            }

            try {
                Signature signer =
                    signatureScheme.getVerifier(x509Credentials.popPublicKey);
                signer.update(contentCovered);
                if (!signer.verify(signature)) {
                    throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid CertificateVerify signature");
                }
            } catch (NoSuchAlgorithmException |
                    InvalidAlgorithmParameterException nsae) {
                throw context.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm (" +
                        signatureScheme.name +
                        ") used in CertificateVerify handshake message", nsae);
            } catch (InvalidKeyException | SignatureException ikse) {
                throw context.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify CertificateVerify signature", ikse);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_VERIFY;
        }

        @Override
        public int messageLength() {
            return 4 + signature.length;    
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt16(signatureScheme.id);
            hos.putBytes16(signature);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "CertificateVerify": '{'
                              "signature algorithm": {0}
                              "signature": '{'
                            {1}
                              '}'
                            '}'""",
                    Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                signatureScheme.name,
                Utilities.indent(
                        hexEncoder.encodeBuffer(signature), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "CertificateVerify" handshake message producer.
     */
    private static final
            class T13CertificateVerifyProducer implements HandshakeProducer {
        private T13CertificateVerifyProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            HandshakeContext hc = (HandshakeContext)context;

            X509Possession x509Possession = null;
            for (SSLPossession possession : hc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null ||
                    x509Possession.popPrivateKey == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "No X.509 credentials negotiated for CertificateVerify");
                }

                return null;
            }

            if (hc.sslConfig.isClientMode) {
                return onProduceCertificateVerify(
                        (ClientHandshakeContext)context, x509Possession);
            } else {
                return onProduceCertificateVerify(
                        (ServerHandshakeContext)context, x509Possession);
            }
        }

        private byte[] onProduceCertificateVerify(ServerHandshakeContext shc,
                X509Possession x509Possession) throws IOException {
            T13CertificateVerifyMessage cvm =
                    new T13CertificateVerifyMessage(shc, x509Possession);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced server CertificateVerify handshake message", cvm);
            }

            cvm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            return null;
        }

        private byte[] onProduceCertificateVerify(ClientHandshakeContext chc,
                X509Possession x509Possession) throws IOException {
            T13CertificateVerifyMessage cvm =
                    new T13CertificateVerifyMessage(chc, x509Possession);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced client CertificateVerify handshake message", cvm);
            }

            cvm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            return null;
        }
    }

    /**
     * The "CertificateVerify" handshake message consumer.
     */
    private static final
            class T13CertificateVerifyConsumer implements SSLConsumer {
        private T13CertificateVerifyConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            HandshakeContext hc = (HandshakeContext)context;

            hc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_VERIFY.id);

            T13CertificateVerifyMessage cvm =
                    new T13CertificateVerifyMessage(hc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateVerify handshake message", cvm);
            }


        }
    }
}
