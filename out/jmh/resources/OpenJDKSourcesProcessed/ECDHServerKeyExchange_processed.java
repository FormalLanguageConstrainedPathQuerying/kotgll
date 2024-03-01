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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of the ServerKeyExchange handshake message.
 */
final class ECDHServerKeyExchange {
    static final SSLConsumer ecdheHandshakeConsumer =
            new ECDHServerKeyExchangeConsumer();
    static final HandshakeProducer ecdheHandshakeProducer =
            new ECDHServerKeyExchangeProducer();

    /**
     * The ECDH ServerKeyExchange handshake message.
     */
    private static final
            class ECDHServerKeyExchangeMessage extends HandshakeMessage {
        private static final byte CURVE_NAMED_CURVE = (byte)0x03;

        private final NamedGroup namedGroup;

        private final byte[] publicPoint;

        private final byte[] paramsSignature;

        private final boolean useExplicitSigAlgorithm;

        private final SignatureScheme signatureScheme;

        private SSLCredentials sslCredentials;

        ECDHServerKeyExchangeMessage(
                HandshakeContext handshakeContext) throws IOException {
            super(handshakeContext);

            ServerHandshakeContext shc =
                    (ServerHandshakeContext)handshakeContext;

            NamedGroupPossession namedGroupPossession = null;
            X509Possession x509Possession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof NamedGroupPossession) {
                    namedGroupPossession = (NamedGroupPossession)possession;
                    if (x509Possession != null) {
                        break;
                    }
                } else if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    if (namedGroupPossession != null) {
                        break;
                    }
                }
            }

            if (namedGroupPossession == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "No ECDHE credentials negotiated for server key exchange");
            }

            namedGroup = namedGroupPossession.getNamedGroup();
            if ((namedGroup == null) || (!namedGroup.isAvailable)) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Missing or improper named group: " + namedGroup);
            }

            publicPoint = namedGroup.encodePossessionPublicKey(
                    namedGroupPossession);
            if (publicPoint == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Missing public point for named group: " + namedGroup);
            }

            if (x509Possession == null) {
                paramsSignature = null;
                signatureScheme = null;
                useExplicitSigAlgorithm = false;
            } else {
                useExplicitSigAlgorithm =
                        shc.negotiatedProtocol.useTLS12PlusSpec();
                Signature signer;
                if (useExplicitSigAlgorithm) {
                    Map.Entry<SignatureScheme, Signature> schemeAndSigner =
                            SignatureScheme.getSignerOfPreferableAlgorithm(
                                shc.sslConfig,
                                shc.algorithmConstraints,
                                shc.peerRequestedSignatureSchemes,
                                x509Possession,
                                shc.negotiatedProtocol);
                    if (schemeAndSigner == null) {
                        throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                                "No supported signature algorithm for " +
                                x509Possession.popPrivateKey.getAlgorithm() +
                                "  key");
                    } else {
                        signatureScheme = schemeAndSigner.getKey();
                        signer = schemeAndSigner.getValue();
                    }
                } else {
                    signatureScheme = null;
                    try {
                        signer = getSignature(
                                x509Possession.popPrivateKey.getAlgorithm(),
                                x509Possession.popPrivateKey);
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Unsupported signature algorithm: " +
                            x509Possession.popPrivateKey.getAlgorithm(), e);
                    }
                }

                byte[] signature;
                try {
                    updateSignature(signer, shc.clientHelloRandom.randomBytes,
                            shc.serverHelloRandom.randomBytes,
                            namedGroup.id, publicPoint);
                    signature = signer.sign();
                } catch (SignatureException ex) {
                    throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Failed to sign ecdhe parameters: " +
                        x509Possession.popPrivateKey.getAlgorithm(), ex);
                }
                paramsSignature = signature;
            }
        }

        ECDHServerKeyExchangeMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            ClientHandshakeContext chc =
                    (ClientHandshakeContext)handshakeContext;

            byte curveType = (byte)Record.getInt8(m);
            if (curveType != CURVE_NAMED_CURVE) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unsupported ECCurveType: " + curveType);
            }

            int namedGroupId = Record.getInt16(m);
            this.namedGroup = NamedGroup.valueOf(namedGroupId);
            if (namedGroup == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unknown named group ID: " + namedGroupId);
            }

            if (!NamedGroup.isEnabled(chc.sslConfig, namedGroup)) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Unsupported named group: " + namedGroup);
            }

            publicPoint = Record.getBytes8(m);
            if (publicPoint.length == 0) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Insufficient Point data: " + namedGroup);
            }

            try {
                sslCredentials =
                        namedGroup.decodeCredentials(publicPoint);
                if (handshakeContext.algorithmConstraints != null &&
                        sslCredentials instanceof
                                NamedGroupCredentials namedGroupCredentials) {
                    if (!handshakeContext.algorithmConstraints.permits(
                            EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                            namedGroupCredentials.getPublicKey())) {
                        chc.conContext.fatal(Alert.INSUFFICIENT_SECURITY,
                            "ServerKeyExchange for " + namedGroup +
                            " does not comply with algorithm constraints");
                    }
                }
            } catch (GeneralSecurityException ex) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Cannot decode named group: " +
                        NamedGroup.nameOf(namedGroupId));
            }

            X509Credentials x509Credentials = null;
            for (SSLCredentials cd : chc.handshakeCredentials) {
                if (cd instanceof X509Credentials) {
                    x509Credentials = (X509Credentials)cd;
                    break;
                }
            }

            if (x509Credentials == null) {
                if (m.hasRemaining()) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid DH ServerKeyExchange: unknown extra data");
                }
                this.signatureScheme = null;
                this.paramsSignature = null;
                this.useExplicitSigAlgorithm = false;

                return;
            }

            this.useExplicitSigAlgorithm =
                    chc.negotiatedProtocol.useTLS12PlusSpec();
            if (useExplicitSigAlgorithm) {
                int ssid = Record.getInt16(m);
                signatureScheme = SignatureScheme.valueOf(ssid);
                if (signatureScheme == null) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid signature algorithm (" + ssid +
                        ") used in ECDH ServerKeyExchange handshake message");
                }

                if (!chc.localSupportedSignAlgs.contains(signatureScheme)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Unsupported signature algorithm (" +
                        signatureScheme.name +
                        ") used in ECDH ServerKeyExchange handshake message");
                }
            } else {
                signatureScheme = null;
            }

            paramsSignature = Record.getBytes16(m);
            Signature signer;
            if (useExplicitSigAlgorithm) {
                try {
                    signer = signatureScheme.getVerifier(
                            x509Credentials.popPublicKey);
                } catch (NoSuchAlgorithmException | InvalidKeyException |
                        InvalidAlgorithmParameterException nsae) {
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm: " +
                        signatureScheme.name, nsae);
                }
            } else {
                try {
                    signer = getSignature(
                            x509Credentials.popPublicKey.getAlgorithm(),
                            x509Credentials.popPublicKey);
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Unsupported signature algorithm: " +
                        x509Credentials.popPublicKey.getAlgorithm(), e);
                }
            }

            try {
                updateSignature(signer,
                        chc.clientHelloRandom.randomBytes,
                        chc.serverHelloRandom.randomBytes,
                        namedGroup.id, publicPoint);

                if (!signer.verify(paramsSignature)) {
                    throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Invalid ECDH ServerKeyExchange signature");
                }
            } catch (SignatureException ex) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Cannot verify ECDH ServerKeyExchange signature", ex);
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.SERVER_KEY_EXCHANGE;
        }

        @Override
        public int messageLength() {
            int sigLen = 0;
            if (paramsSignature != null) {
                sigLen = 2 + paramsSignature.length;
                if (useExplicitSigAlgorithm) {
                    sigLen += SignatureScheme.sizeInRecord();
                }
            }

            return 4 + publicPoint.length + sigLen;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt8(CURVE_NAMED_CURVE);
            hos.putInt16(namedGroup.id);
            hos.putBytes8(publicPoint);
            if (paramsSignature != null) {
                if (useExplicitSigAlgorithm) {
                    hos.putInt16(signatureScheme.id);
                }

                hos.putBytes16(paramsSignature);
            }
        }

        @Override
        public String toString() {
            if (useExplicitSigAlgorithm) {
                MessageFormat messageFormat = new MessageFormat(
                        """
                                "ECDH ServerKeyExchange": '{'
                                  "parameters": '{'
                                    "named group": "{0}"
                                    "ecdh public": '{'
                                {1}
                                    '}',
                                  '}',
                                  "digital signature":  '{'
                                    "signature algorithm": "{2}"
                                    "signature": '{'
                                {3}
                                    '}',
                                  '}'
                                '}'""",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    namedGroup.name,
                    Utilities.indent(
                            hexEncoder.encodeBuffer(publicPoint), "      "),
                    signatureScheme.name,
                    Utilities.indent(
                            hexEncoder.encodeBuffer(paramsSignature), "      ")
                };
                return messageFormat.format(messageFields);
            } else if (paramsSignature != null) {
                MessageFormat messageFormat = new MessageFormat(
                        """
                                "ECDH ServerKeyExchange": '{'
                                  "parameters":  '{'
                                    "named group": "{0}"
                                    "ecdh public": '{'
                                {1}
                                    '}',
                                  '}',
                                  "signature": '{'
                                {2}
                                  '}'
                                '}'""",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    namedGroup.name,
                    Utilities.indent(
                            hexEncoder.encodeBuffer(publicPoint), "      "),
                    Utilities.indent(
                            hexEncoder.encodeBuffer(paramsSignature), "    ")
                };

                return messageFormat.format(messageFields);
            } else {    
                MessageFormat messageFormat = new MessageFormat(
                        """
                                "ECDH ServerKeyExchange": '{'
                                  "parameters":  '{'
                                    "named group": "{0}"
                                    "ecdh public": '{'
                                {1}
                                    '}',
                                  '}'
                                '}'""",
                    Locale.ENGLISH);

                HexDumpEncoder hexEncoder = new HexDumpEncoder();
                Object[] messageFields = {
                    namedGroup.name,
                    Utilities.indent(
                            hexEncoder.encodeBuffer(publicPoint), "      "),
                };

                return messageFormat.format(messageFields);
            }
        }

        private static Signature getSignature(String keyAlgorithm,
                Key key) throws NoSuchAlgorithmException, InvalidKeyException {
            Signature signer;
            switch (keyAlgorithm) {
                case "EC":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_ECDSA);
                    break;
                case "EdDSA":
                    signer = Signature.getInstance(JsseJce.SIGNATURE_EDDSA);
                    break;
                case "RSA":
                    signer = RSASignature.getInstance();
                    break;
                default:
                    throw new NoSuchAlgorithmException(
                        "neither an RSA or a EC key : " + keyAlgorithm);
            }

            if (key instanceof PublicKey) {
                signer.initVerify((PublicKey)(key));
            } else {
                signer.initSign((PrivateKey)key);
            }

            return signer;
        }

        private static void updateSignature(Signature sig,
                byte[] clntNonce, byte[] svrNonce, int namedGroupId,
                byte[] publicPoint) throws SignatureException {
            sig.update(clntNonce);
            sig.update(svrNonce);

            sig.update(CURVE_NAMED_CURVE);
            sig.update((byte)((namedGroupId >> 8) & 0xFF));
            sig.update((byte)(namedGroupId & 0xFF));
            sig.update((byte)publicPoint.length);
            sig.update(publicPoint);
        }
    }

    /**
     * The ECDH "ServerKeyExchange" handshake message producer.
     */
    private static final
            class ECDHServerKeyExchangeProducer implements HandshakeProducer {
        private ECDHServerKeyExchangeProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ECDHServerKeyExchangeMessage skem =
                    new ECDHServerKeyExchangeMessage(shc);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced ECDH ServerKeyExchange handshake message", skem);
            }

            skem.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            return null;
        }
    }

    /**
     * The ECDH "ServerKeyExchange" handshake message consumer.
     */
    private static final
            class ECDHServerKeyExchangeConsumer implements SSLConsumer {
        private ECDHServerKeyExchangeConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            ECDHServerKeyExchangeMessage skem =
                    new ECDHServerKeyExchangeMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Consuming ECDH ServerKeyExchange handshake message", skem);
            }

            chc.handshakeCredentials.add(skem.sslCredentials);

        }
    }
}

