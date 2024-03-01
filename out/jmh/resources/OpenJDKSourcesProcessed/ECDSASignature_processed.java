/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import java.nio.ByteBuffer;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Optional;

import sun.security.jca.JCAUtil;
import sun.security.util.*;
import static sun.security.ec.ECOperations.IntermediateValueException;

/**
 * ECDSA signature implementation. This class currently supports the
 * following algorithm names:
 *
 *   . "NONEwithECDSA"
 *   . "SHA1withECDSA"
 *   . "SHA224withECDSA"
 *   . "SHA256withECDSA"
 *   . "SHA384withECDSA"
 *   . "SHA512withECDSA"
 *   . "SHA3-224withECDSA"
 *   . "SHA3-256withECDSA"
 *   . "SHA3-384withECDSA"
 *   . "SHA3-512withECDSA"
 *   . "NONEwithECDSAinP1363Format"
 *   . "SHA1withECDSAinP1363Format"
 *   . "SHA224withECDSAinP1363Format"
 *   . "SHA256withECDSAinP1363Format"
 *   . "SHA384withECDSAinP1363Format"
 *   . "SHA512withECDSAinP1363Format"
 *   . "SHA3-224withECDSAinP1363Format"
 *   . "SHA3-256withECDSAinP1363Format"
 *   . "SHA3-384withECDSAinP1363Format"
 *   . "SHA3-512withECDSAinP1363Format"
 *
 * @since   1.7
 */
abstract class ECDSASignature extends SignatureSpi {

    private final MessageDigest messageDigest;

    private SecureRandom random;

    private boolean needsReset;

    private ECPrivateKey privateKey;

    private ECPublicKey publicKey;

    private final boolean p1363Format;

    /**
     * Constructs a new ECDSASignature.
     *
     * @exception ProviderException if the native ECC library is unavailable.
     */
    ECDSASignature() {
        this(false);
    }

    /**
     * Constructs a new ECDSASignature that will use the specified
     * signature format. {@code p1363Format} should be {@code true} to
     * use the IEEE P1363 format. If {@code p1363Format} is {@code false},
     * the DER-encoded ASN.1 format will be used. This constructor is
     * used by the RawECDSA subclasses.
     */
    ECDSASignature(boolean p1363Format) {
        this.messageDigest = null;
        this.p1363Format = p1363Format;
    }

    /**
     * Constructs a new ECDSASignature. Used by subclasses.
     */
    ECDSASignature(String digestName) {
        this(digestName, false);
    }

    /**
     * Constructs a new ECDSASignature that will use the specified
     * digest and signature format. {@code p1363Format} should be
     * {@code true} to use the IEEE P1363 format. If {@code p1363Format}
     * is {@code false}, the DER-encoded ASN.1 format will be used. This
     * constructor is used by subclasses.
     */
    ECDSASignature(String digestName, boolean p1363Format) {
        try {
            messageDigest = MessageDigest.getInstance(digestName);
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException(e);
        }
        this.needsReset = false;
        this.p1363Format = p1363Format;
    }

    static class RawECDSA extends ECDSASignature {

        private static final int RAW_ECDSA_MAX = 64;

        private final byte[] precomputedDigest;
        private int offset = 0;

        RawECDSA(boolean p1363Format) {
            super(p1363Format);
            precomputedDigest = new byte[RAW_ECDSA_MAX];
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
            if (offset >= precomputedDigest.length) {
                offset = RAW_ECDSA_MAX + 1;
                return;
            }
            precomputedDigest[offset++] = b;
        }

        @Override
        protected void engineUpdate(byte[] b, int off, int len)
        throws SignatureException {
            if (offset >= precomputedDigest.length) {
                offset = RAW_ECDSA_MAX + 1;
                return;
            }
            System.arraycopy(b, off, precomputedDigest, offset, len);
            offset += len;
        }

        @Override
        protected void engineUpdate(ByteBuffer byteBuffer) {
            int len = byteBuffer.remaining();
            if (len <= 0) {
                return;
            }
            if (len >= precomputedDigest.length - offset) {
                offset = RAW_ECDSA_MAX + 1;
                return;
            }
            byteBuffer.get(precomputedDigest, offset, len);
            offset += len;
        }

        @Override
        protected void resetDigest() {
            offset = 0;
        }

        @Override
        protected byte[] getDigestValue() throws SignatureException {
            if (offset > RAW_ECDSA_MAX) {
                throw new SignatureException("Message digest is too long");

            }
            byte[] result = new byte[offset];
            System.arraycopy(precomputedDigest, 0, result, 0, offset);
            offset = 0;

            return result;
        }
    }

    public static final class Raw extends RawECDSA {
        public Raw() {
            super(false);
        }
    }

    public static final class RawinP1363Format extends RawECDSA {
        public RawinP1363Format() {
            super(true);
        }
    }

    public static final class SHA1 extends ECDSASignature {
        public SHA1() {
            super("SHA1");
        }
    }

    public static final class SHA1inP1363Format extends ECDSASignature {
        public SHA1inP1363Format() {
            super("SHA1", true);
        }
    }

    public static final class SHA224 extends ECDSASignature {
        public SHA224() {
            super("SHA-224");
        }
    }

    public static final class SHA224inP1363Format extends ECDSASignature {
        public SHA224inP1363Format() {
            super("SHA-224", true);
        }
    }

    public static final class SHA256 extends ECDSASignature {
        public SHA256() {
            super("SHA-256");
        }
    }

    public static final class SHA256inP1363Format extends ECDSASignature {
        public SHA256inP1363Format() {
            super("SHA-256", true);
        }
    }

    public static final class SHA384 extends ECDSASignature {
        public SHA384() {
            super("SHA-384");
        }
    }

    public static final class SHA384inP1363Format extends ECDSASignature {
        public SHA384inP1363Format() {
            super("SHA-384", true);
        }
    }

    public static final class SHA512 extends ECDSASignature {
        public SHA512() {
            super("SHA-512");
        }
    }

    public static final class SHA512inP1363Format extends ECDSASignature {
        public SHA512inP1363Format() {
            super("SHA-512", true);
        }
    }

    public static final class SHA3_224 extends ECDSASignature {
        public SHA3_224() {
           super("SHA3-224");
        }
    }

    public static final class SHA3_224inP1363Format extends ECDSASignature {
        public SHA3_224inP1363Format() {
            super("SHA3-224", true);
        }
    }

    public static final class SHA3_256 extends ECDSASignature {
        public SHA3_256() {
            super("SHA3-256");
        }
    }

    public static final class SHA3_256inP1363Format extends ECDSASignature {
        public SHA3_256inP1363Format() {
            super("SHA3-256", true);
        }
    }

    public static final class SHA3_384 extends ECDSASignature {
        public SHA3_384() {
            super("SHA3-384");
        }
    }

    public static final class SHA3_384inP1363Format extends ECDSASignature {
        public SHA3_384inP1363Format() {
            super("SHA3-384", true);
        }
    }

    public static final class SHA3_512 extends ECDSASignature {
        public SHA3_512() {
            super("SHA3-512");
        }
    }

    public static final class SHA3_512inP1363Format extends ECDSASignature {
        public SHA3_512inP1363Format() {
            super("SHA3-512", true);
        }
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey)
    throws InvalidKeyException {
        ECPublicKey key = (ECPublicKey) ECKeyFactory.toECKey(publicKey);
        this.publicKey = key;
        this.privateKey = null;
        resetDigest();
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey)
    throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
    throws InvalidKeyException {
        ECPrivateKey key = (ECPrivateKey) ECKeyFactory.toECKey(privateKey);
        ECUtil.checkPrivateKey(key);
        this.privateKey = key;
        this.publicKey = null;
        this.random = random;
        resetDigest();
    }

    /**
     * Resets the message digest if needed.
     */
    protected void resetDigest() {
        if (needsReset) {
            if (messageDigest != null) {
                messageDigest.reset();
            }
            needsReset = false;
        }
    }

    /**
     * Returns the message digest value.
     */
    protected byte[] getDigestValue() throws SignatureException {
        needsReset = false;
        return messageDigest.digest();
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        messageDigest.update(b);
        needsReset = true;
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len)
    throws SignatureException {
        messageDigest.update(b, off, len);
        needsReset = true;
    }

    @Override
    protected void engineUpdate(ByteBuffer byteBuffer) {
        int len = byteBuffer.remaining();
        if (len <= 0) {
            return;
        }

        messageDigest.update(byteBuffer);
        needsReset = true;
    }

    private byte[] signDigestImpl(ECDSAOperations ops, int seedBits,
        byte[] digest, ECPrivateKey priv, SecureRandom random)
        throws SignatureException {

        byte[] seedBytes = new byte[(seedBits + 7) / 8];
        byte[] s = priv instanceof ECPrivateKeyImpl
                ? ((ECPrivateKeyImpl)priv).getArrayS()
                : ECUtil.sArray(priv.getS(), priv.getParams());

        int numAttempts = 128;
        for (int i = 0; i < numAttempts; i++) {
            random.nextBytes(seedBytes);
            ECDSAOperations.Seed seed = new ECDSAOperations.Seed(seedBytes);
            try {
                return ops.signDigest(s, digest, seed);
            } catch (IntermediateValueException ex) {
            }
        }

        throw new SignatureException("Unable to produce signature after "
            + numAttempts + " attempts");
    }


    @Override
    protected byte[] engineSign() throws SignatureException {

        if (random == null) {
            random = JCAUtil.getSecureRandom();
        }

        byte[] digest = getDigestValue();
        ECParameterSpec params = privateKey.getParams();

        int seedBits = params.getOrder().bitLength() + 64;
        Optional<ECDSAOperations> opsOpt =
            ECDSAOperations.forParameters(params);
        if (opsOpt.isEmpty()) {
            throw new SignatureException("Curve not supported: " + params);
        }
        byte[] sig = signDigestImpl(opsOpt.get(), seedBits, digest, privateKey,
            random);

        if (p1363Format) {
            return sig;
        } else {
            return ECUtil.encodeSignature(sig);
        }
    }

    @Override
    protected boolean engineVerify(byte[] signature) throws SignatureException {

        ECPoint w = publicKey.getW();
        ECParameterSpec params = publicKey.getParams();

        try {
            ECUtil.validatePublicKey(w, params);
        } catch (InvalidKeyException e) {
            return false;
        }

        ECDSAOperations ops = ECDSAOperations.forParameters(params)
                .orElseThrow(() -> new SignatureException("Curve not supported: " + params));

        if (params.getCofactor() != 1) {
            if (!ops.getEcOperations().checkOrder(w)) {
                return false;
            }
        }

        byte[] sig;
        if (p1363Format) {
            sig = signature;
        } else {
            sig = ECUtil.decodeSignature(signature);
        }
        return ops.verifySignedDigest(getDigestValue(), sig, w);
    }

    @Override
    @Deprecated
    protected void engineSetParameter(String param, Object value)
    throws InvalidParameterException {
        throw new UnsupportedOperationException("setParameter() not supported");
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        if (params == null) {
            return;
        }
        if (!(params instanceof ECParameterSpec ecparams)) {
            throw new InvalidAlgorithmParameterException(
                    "Parameters must be of type ECParameterSpec");
        }
        ECKey key = (this.privateKey == null? this.publicKey : this.privateKey);
        if ((key != null) && !ECUtil.equals(ecparams, key.getParams())) {
            throw new InvalidAlgorithmParameterException
                ("Signature params does not match key params");
        }
    }

    @Override
    @Deprecated
    protected Object engineGetParameter(String param)
    throws InvalidParameterException {
        throw new UnsupportedOperationException("getParameter() not supported");
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }
}
