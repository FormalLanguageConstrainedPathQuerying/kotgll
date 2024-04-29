/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.interfaces.*;
import javax.crypto.spec.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import sun.security.util.KeyUtil;

/**
 * KeyAgreement implementation class. This class currently supports
 * DH.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11KeyAgreement extends KeyAgreementSpi {

    private final Token token;

    private final String algorithm;

    private final long mechanism;

    private P11Key privateKey;

    private BigInteger publicValue;

    private int secretLen;

    private KeyAgreement multiPartyAgreement;

    private static class AllowKDF {

        private static final boolean VALUE = getValue();

        @SuppressWarnings("removal")
        private static boolean getValue() {
            return AccessController.doPrivileged(
                (PrivilegedAction<Boolean>)
                () -> Boolean.getBoolean("jdk.crypto.KeyAgreement.legacyKDF"));
        }
    }

    P11KeyAgreement(Token token, String algorithm, long mechanism) {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;
    }

    protected void engineInit(Key key, SecureRandom random)
            throws InvalidKeyException {
        if (!(key instanceof PrivateKey)) {
            throw new InvalidKeyException
                        ("Key must be instance of PrivateKey");
        }
        privateKey = P11KeyFactory.convertKey(token, key, algorithm);
        publicValue = null;
        multiPartyAgreement = null;
    }

    protected void engineInit(Key key, AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidKeyException,
            InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                        ("Parameters not supported");
        }
        engineInit(key, random);
    }

    protected Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {
        if (privateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (publicValue != null) {
            throw new IllegalStateException("Phase already executed");
        }
        if ((multiPartyAgreement != null) || (!lastPhase)) {
            if (multiPartyAgreement == null) {
                try {
                    multiPartyAgreement = KeyAgreement.getInstance
                        ("DH", P11Util.getSunJceProvider());
                    multiPartyAgreement.init(privateKey);
                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidKeyException
                        ("Could not initialize multi party agreement", e);
                }
            }
            return multiPartyAgreement.doPhase(key, lastPhase);
        }
        if ((!(key instanceof PublicKey))
                || (!key.getAlgorithm().equals(algorithm))) {
            throw new InvalidKeyException
                ("Key must be a PublicKey with algorithm DH");
        }
        BigInteger p, g, y;
        if (key instanceof DHPublicKey dhKey) {

            KeyUtil.validate(dhKey);

            y = dhKey.getY();
            DHParameterSpec params = dhKey.getParams();
            p = params.getP();
            g = params.getG();
        } else {
            P11DHKeyFactory kf = new P11DHKeyFactory(token, "DH");
            try {
                DHPublicKeySpec spec = kf.engineGetKeySpec(
                        key, DHPublicKeySpec.class);

                KeyUtil.validate(spec);

                y = spec.getY();
                p = spec.getP();
                g = spec.getG();
            } catch (InvalidKeySpecException e) {
                throw new InvalidKeyException("Could not obtain key values", e);
            }
        }
        if (privateKey instanceof DHPrivateKey dhKey) {
            DHParameterSpec params = dhKey.getParams();
            if ((!p.equals(params.getP()))
                                || (!g.equals(params.getG()))) {
                throw new InvalidKeyException
                ("PublicKey DH parameters must match PrivateKey DH parameters");
            }
        }
        publicValue = y;
        secretLen = (p.bitLength() + 7) >> 3;
        return null;
    }

    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if (multiPartyAgreement != null) {
            byte[] val = multiPartyAgreement.generateSecret();
            multiPartyAgreement = null;
            return val;
        }
        if ((privateKey == null) || (publicValue == null)) {
            throw new IllegalStateException("Not initialized correctly");
        }
        Session session = null;
        long privKeyID = privateKey.getKeyID();
        try {
            session = token.getOpSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_GENERIC_SECRET),
            };
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, CKK_GENERIC_SECRET, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, publicValue), privKeyID,
                    attributes);

            attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE)
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, attributes);
            byte[] secret = attributes[0].getByteArray();
            token.p11.C_DestroyObject(session.id(), keyID);
            if (secret.length == secretLen) {
                return secret;
            } else {
                if (secret.length > secretLen) {
                    throw new ProviderException("generated secret is out-of-range");
                }
                byte[] newSecret = new byte[secretLen];
                System.arraycopy(secret, 0, newSecret, secretLen - secret.length,
                    secret.length);
                return newSecret;
            }
        } catch (PKCS11Exception e) {
            throw new ProviderException("Could not derive key", e);
        } finally {
            privateKey.releaseKeyID();
            publicValue = null;
            token.releaseSession(session);
        }
    }

    protected int engineGenerateSecret(byte[] sharedSecret, int
            offset) throws IllegalStateException, ShortBufferException {
        if (multiPartyAgreement != null) {
            int n = multiPartyAgreement.generateSecret(sharedSecret, offset);
            multiPartyAgreement = null;
            return n;
        }
        if (offset + secretLen > sharedSecret.length) {
            throw new ShortBufferException("Need " + secretLen
                + " bytes, only " + (sharedSecret.length - offset) + " available");
        }
        byte[] secret = engineGenerateSecret();
        System.arraycopy(secret, 0, sharedSecret, offset, secret.length);
        return secret.length;
    }

    protected SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {
        if (multiPartyAgreement != null) {
            SecretKey key = multiPartyAgreement.generateSecret(algorithm);
            multiPartyAgreement = null;
            return key;
        }
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Algorithm must not be null");
        }

        if (algorithm.equals("TlsPremasterSecret")) {
            return nativeGenerateSecret(algorithm);
        }

        if (!algorithm.equalsIgnoreCase("TlsPremasterSecret") &&
            !AllowKDF.VALUE) {

            throw new NoSuchAlgorithmException("Unsupported secret key "
                                               + "algorithm: " + algorithm);
        }

        byte[] secret = engineGenerateSecret();
        int keyLen;
        if (algorithm.equalsIgnoreCase("DES")) {
            keyLen = 8;
        } else if (algorithm.equalsIgnoreCase("DESede")) {
            keyLen = 24;
        } else if (algorithm.equalsIgnoreCase("Blowfish")) {
            keyLen = Math.min(56, secret.length);
        } else if (algorithm.equalsIgnoreCase("TlsPremasterSecret")) {
            keyLen = secret.length;
        } else {
            throw new NoSuchAlgorithmException
                ("Unknown algorithm " + algorithm);
        }
        if (secret.length < keyLen) {
            throw new InvalidKeyException("Secret too short");
        }
        if (algorithm.equalsIgnoreCase("DES") ||
            algorithm.equalsIgnoreCase("DESede")) {
                for (int i = 0; i < keyLen; i+=8) {
                    P11SecretKeyFactory.fixDESParity(secret, i);
                }
        }
        return new SecretKeySpec(secret, 0, keyLen, algorithm);
    }

    private SecretKey nativeGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {
        if ((privateKey == null) || (publicValue == null)) {
            throw new IllegalStateException("Not initialized correctly");
        }
        long keyType = CKK_GENERIC_SECRET;
        Session session = null;
        long privKeyID = privateKey.getKeyID();
        try {
            session = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                new CK_ATTRIBUTE(CKA_KEY_TYPE, keyType),
            };
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, keyType, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, publicValue), privKeyID,
                    attributes);
            CK_ATTRIBUTE[] lenAttributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE_LEN),
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, lenAttributes);
            int keyLen = (int)lenAttributes[0].getLong();
            SecretKey key = P11Key.secretKey
                        (session, keyID, algorithm, keyLen << 3, attributes);
            if ("RAW".equals(key.getFormat())) {
                byte[] keyBytes = key.getEncoded();
                byte[] newBytes = KeyUtil.trimZeroes(keyBytes);
                if (keyBytes != newBytes) {
                    key = new SecretKeySpec(newBytes, algorithm);
                }
            }
            return key;
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not derive key", e);
        } finally {
            privateKey.releaseKeyID();
            publicValue = null;
            token.releaseSession(session);
        }
    }

}
