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

package sun.security.rsa;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.interfaces.*;

import java.util.Arrays;
import java.util.Hashtable;

import sun.security.util.*;
import sun.security.jca.JCAUtil;


/**
 * PKCS#1 v2.2 RSASSA-PSS signatures with various message digest algorithms.
 * RSASSA-PSS implementation takes the message digest algorithm, MGF algorithm,
 * and salt length values through the required signature PSS parameters.
 * We support SHA-1, SHA-2 family and SHA3 family of message digest algorithms,
 * and MGF1 mask generation function.
 *
 * @since   11
 */
public class RSAPSSSignature extends SignatureSpi {

    private static final boolean DEBUG = false;

    private boolean isDigestEqual(String stdAlg, String givenAlg) {
        if (stdAlg == null || givenAlg == null) return false;

        if (givenAlg.contains("-")) {
            return stdAlg.equalsIgnoreCase(givenAlg);
        } else {
            if (stdAlg.equals("SHA-1")) {
                return (givenAlg.equalsIgnoreCase("SHA")
                        || givenAlg.equalsIgnoreCase("SHA1"));
            } else {
                StringBuilder sb = new StringBuilder(givenAlg);
                if (givenAlg.regionMatches(true, 0, "SHA", 0, 3)) {
                    givenAlg = sb.insert(3, "-").toString();
                    return stdAlg.equalsIgnoreCase(givenAlg);
                } else {
                    throw new ProviderException("Unsupported digest algorithm "
                            + givenAlg);
                }
            }
        }
    }

    private static final byte[] EIGHT_BYTES_OF_ZEROS = new byte[8];

    private static final Hashtable<KnownOIDs, Integer> DIGEST_LENGTHS =
            new Hashtable<>();
    static {
        DIGEST_LENGTHS.put(KnownOIDs.SHA_1, 20);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_224, 28);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_256, 32);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_384, 48);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_512, 64);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_512$224, 28);
        DIGEST_LENGTHS.put(KnownOIDs.SHA_512$256, 32);
        DIGEST_LENGTHS.put(KnownOIDs.SHA3_224, 28);
        DIGEST_LENGTHS.put(KnownOIDs.SHA3_256, 32);
        DIGEST_LENGTHS.put(KnownOIDs.SHA3_384, 48);
        DIGEST_LENGTHS.put(KnownOIDs.SHA3_512, 64);
    }

    private MessageDigest md;
    private boolean digestReset = true;

    private RSAPrivateKey privKey = null;
    private RSAPublicKey pubKey = null;
    private PSSParameterSpec sigParams = null; 

    private SecureRandom random;

    /**
     * Construct a new RSAPSSSignature with arbitrary digest algorithm
     */
    public RSAPSSSignature() {
        this.md = null;
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException {
        if (publicKey instanceof RSAPublicKey rsaPubKey) {
            isPublicKeyValid(rsaPubKey);
            this.pubKey = rsaPubKey;
            this.privKey = null;
            resetDigest();
        } else {
            throw new InvalidKeyException("key must be RSAPublicKey");
        }
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey)
            throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
            throws InvalidKeyException {
        if (privateKey instanceof RSAPrivateKey rsaPrivateKey) {
            isPrivateKeyValid(rsaPrivateKey);
            this.privKey = rsaPrivateKey;
            this.pubKey = null;
            this.random =
                    (random == null ? JCAUtil.getSecureRandom() : random);
            resetDigest();
        } else {
            throw new InvalidKeyException("key must be RSAPrivateKey");
        }
    }

    /**
     * Utility method for checking the key PSS parameters against signature
     * PSS parameters.
     * Returns false if any of the digest/MGF algorithms and trailerField
     * values does not match or if the salt length in key parameters is
     * larger than the value in signature parameters.
     */
    private static boolean isCompatible(AlgorithmParameterSpec keyParams,
            PSSParameterSpec sigParams) {
        if (keyParams == null) {
            return true;
        }
        if (!(keyParams instanceof PSSParameterSpec pssKeyParams)) {
            return false;
        }
        if (sigParams == null) {
            return true;
        }
        if (pssKeyParams.getSaltLength() > sigParams.getSaltLength()) {
            return false;
        }

        PSSParameterSpec keyParams2 =
            new PSSParameterSpec(pssKeyParams.getDigestAlgorithm(),
                    pssKeyParams.getMGFAlgorithm(),
                    pssKeyParams.getMGFParameters(),
                    sigParams.getSaltLength(),
                    pssKeyParams.getTrailerField());
        PSSParameters ap = new PSSParameters();
        try {
            ap.engineInit(keyParams2);
            byte[] encoded = ap.engineGetEncoded();
            ap.engineInit(sigParams);
            byte[] encoded2 = ap.engineGetEncoded();
            return Arrays.equals(encoded, encoded2);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Validate the specified RSAPrivateKey
     */
    private void isPrivateKeyValid(RSAPrivateKey prKey)  throws InvalidKeyException {
        try {
            if (prKey instanceof RSAPrivateCrtKey crtKey) {
                if (RSAPrivateCrtKeyImpl.checkComponents(crtKey)) {
                    RSAKeyFactory.checkRSAProviderKeyLengths(
                            crtKey.getModulus().bitLength(),
                            crtKey.getPublicExponent());
                } else {
                    throw new InvalidKeyException(
                            "Some of the CRT-specific components are not available");
                }
            } else {
                RSAKeyFactory.checkRSAProviderKeyLengths(
                        prKey.getModulus().bitLength(),
                        null);
            }
        } catch (InvalidKeyException ikEx) {
            throw ikEx;
        } catch (Exception e) {
            throw new InvalidKeyException(
                    "Can not access private key components", e);
        }
        isValid(prKey);
    }

    /**
     * Validate the specified RSAPublicKey
     */
    private void isPublicKeyValid(RSAPublicKey pKey)  throws InvalidKeyException {
        try {
            RSAKeyFactory.checkRSAProviderKeyLengths(
                    pKey.getModulus().bitLength(),
                    pKey.getPublicExponent());
        } catch (InvalidKeyException ikEx) {
            throw ikEx;
        } catch (Exception e) {
            throw new InvalidKeyException(
                    "Can not access public key components", e);
        }
        isValid(pKey);
    }

    /**
     * Validate the specified RSAKey and its associated parameters against
     * internal signature parameters.
     */
    private void isValid(RSAKey rsaKey) throws InvalidKeyException {
        if (!isCompatible(rsaKey.getParams(), this.sigParams)) {
            throw new InvalidKeyException
                ("Key contains incompatible PSS parameter values");
        }
        if (this.sigParams != null) {
            String digestAlgo = this.sigParams.getDigestAlgorithm();
            KnownOIDs ko = KnownOIDs.findMatch(digestAlgo);
            if (ko != null) {
                Integer hLen = DIGEST_LENGTHS.get(ko);
                if (hLen != null) {
                    checkKeyLength(rsaKey, hLen,
                            this.sigParams.getSaltLength());
                } else {
                    throw new ProviderException
                            ("Unsupported digest algo: " + digestAlgo);
                }
            } else {
                throw new ProviderException
                        ("Unrecognized digest algo: " + digestAlgo);
            }
        }
    }

    /**
     * Validate the specified Signature PSS parameters.
     */
    private PSSParameterSpec validateSigParams(AlgorithmParameterSpec p)
            throws InvalidAlgorithmParameterException {
        if (p == null) {
            throw new InvalidAlgorithmParameterException
                ("Parameters cannot be null");
        }
        if (!(p instanceof PSSParameterSpec params)) {
            throw new InvalidAlgorithmParameterException
                ("parameters must be type PSSParameterSpec");
        }
        if (params == this.sigParams) return params;

        RSAKey key = (this.privKey == null? this.pubKey : this.privKey);
        if (key != null) {
            if (!isCompatible(key.getParams(), params)) {
                throw new InvalidAlgorithmParameterException
                    ("Signature parameters does not match key parameters");
            }
        }
        if (!(params.getMGFAlgorithm().equalsIgnoreCase("MGF1"))) {
            throw new InvalidAlgorithmParameterException("Only supports MGF1");

        }
        if (params.getTrailerField() != PSSParameterSpec.TRAILER_FIELD_BC) {
            throw new InvalidAlgorithmParameterException
                ("Only supports TrailerFieldBC(1)");

        }

        if (key != null) {
            String digestAlgo = params.getDigestAlgorithm();
            KnownOIDs ko = KnownOIDs.findMatch(digestAlgo);
            if (ko != null) {
                Integer hLen = DIGEST_LENGTHS.get(ko);
                if (hLen != null) {
                    try {
                        checkKeyLength(key, hLen, params.getSaltLength());
                    } catch (InvalidKeyException e) {
                        throw new InvalidAlgorithmParameterException(e);
                    }
                } else {
                    throw new InvalidAlgorithmParameterException
                            ("Unsupported digest algo: " + digestAlgo);
                }
            } else {
                throw new InvalidAlgorithmParameterException
                        ("Unrecognized digest algo: " + digestAlgo);
            }
        }
        return params;
    }

    /**
     * Ensure the object is initialized with key and parameters and
     * reset digest
     */
    private void ensureInit() throws SignatureException {
        RSAKey key = (this.privKey == null? this.pubKey : this.privKey);
        if (key == null) {
            throw new SignatureException("Missing key");
        }
        if (this.sigParams == null) {
            throw new SignatureException
                ("Parameters required for RSASSA-PSS signatures");
        }
    }

    /**
     * Utility method for checking key length against digest length and
     * salt length
     */
    private static void checkKeyLength(RSAKey key, int digestLen,
            int saltLen) throws InvalidKeyException {
        if (key != null) {
            int keyLength = (getKeyLengthInBits(key) + 7) >> 3;
            int minLength = Math.addExact(Math.addExact(digestLen, saltLen), 2);
            if (keyLength < minLength) {
                throw new InvalidKeyException
                    ("Key is too short, need min " + minLength + " bytes");
            }
        }
    }

    /**
     * Reset the message digest if it is not already reset.
     */
    private void resetDigest() {
        if (!digestReset) {
            this.md.reset();
            digestReset = true;
        }
    }

    /**
     * Return the message digest value.
     */
    private byte[] getDigestValue() {
        digestReset = true;
        return this.md.digest();
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        ensureInit();
        this.md.update(b);
        digestReset = false;
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len)
            throws SignatureException {
        ensureInit();
        this.md.update(b, off, len);
        digestReset = false;
    }

    @Override
    protected void engineUpdate(ByteBuffer b) {
        try {
            ensureInit();
        } catch (SignatureException se) {
            throw new RuntimeException(se.getMessage());
        }
        this.md.update(b);
        digestReset = false;
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        ensureInit();
        byte[] mHash = getDigestValue();
        try {
            byte[] encoded = encodeSignature(mHash);
            return RSACore.rsa(encoded, privKey, true);
        } catch (GeneralSecurityException e) {
            throw new SignatureException("Could not sign data", e);
        } catch (IOException e) {
            throw new SignatureException("Could not encode data", e);
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        ensureInit();
        try {
            if (sigBytes.length != RSACore.getByteLength(this.pubKey)) {
                throw new SignatureException
                    ("Signature length not correct: got "
                    + sigBytes.length + " but was expecting "
                    + RSACore.getByteLength(this.pubKey));
            }
            byte[] mHash = getDigestValue();
            byte[] decrypted = RSACore.rsa(sigBytes, this.pubKey);
            return decodeSignature(mHash, decrypted);
        } catch (javax.crypto.BadPaddingException e) {
            return false;
        } catch (IOException e) {
            throw new SignatureException("Signature encoding error", e);
        } finally {
            resetDigest();
        }
    }

    private static int getKeyLengthInBits(RSAKey k) {
        if (k != null) {
            return k.getModulus().bitLength();
        }
        return -1;
    }

    /**
     * Encode the digest 'mHash', return the to-be-signed data.
     * Also used by the PKCS#11 provider.
     */
    private byte[] encodeSignature(byte[] mHash)
        throws IOException, DigestException {
        AlgorithmParameterSpec mgfParams = this.sigParams.getMGFParameters();
        String mgfDigestAlgo;
        if (mgfParams != null) {
            mgfDigestAlgo =
                ((MGF1ParameterSpec) mgfParams).getDigestAlgorithm();
        } else {
            mgfDigestAlgo = this.md.getAlgorithm();
        }
        try {
            int emBits = getKeyLengthInBits(this.privKey) - 1;
            int emLen = (emBits + 7) >> 3;
            int hLen = this.md.getDigestLength();
            int dbLen = emLen - hLen - 1;
            int sLen = this.sigParams.getSaltLength();

            byte[] em = new byte[emLen];

            em[dbLen - sLen - 1] = (byte) 1; 
            em[em.length - 1] = (byte) 0xBC; 

            if (!digestReset) {
                throw new ProviderException("Digest should be reset");
            }
            this.md.update(EIGHT_BYTES_OF_ZEROS);
            digestReset = false; 
            this.md.update(mHash);
            if (sLen != 0) {
                byte[] salt = new byte[sLen];
                this.random.nextBytes(salt);
                this.md.update(salt);

                System.arraycopy(salt, 0, em, dbLen - sLen, sLen);
            }
            this.md.digest(em, dbLen, hLen); 
            digestReset = true;


            MGF1 mgf1 = new MGF1(mgfDigestAlgo);
            mgf1.generateAndXor(em, dbLen, hLen, dbLen, em, 0);

            int numZeroBits = (emLen << 3) - emBits;

            if (numZeroBits != 0) {
                byte MASK = (byte) (0xff >>> numZeroBits);
                em[0] = (byte) (em[0] & MASK);
            }

            return em;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.toString());
        }
    }

    /**
     * Decode the signature data as under RFC8017 sec9.1.2 EMSA-PSS-VERIFY
     */
    private boolean decodeSignature(byte[] mHash, byte[] em)
            throws IOException {
        int hLen = mHash.length;
        int sLen = this.sigParams.getSaltLength();
        int emBits = getKeyLengthInBits(this.pubKey) - 1;
        int emLen = (emBits + 7) >> 3;

        int emOfs = em.length - emLen;
        if ((emOfs == 1) && (em[0] != 0)) {
            return false;
        }

        if (emLen < (hLen + sLen + 2)) {
            return false;
        }

        if (em[emOfs + emLen - 1] != (byte) 0xBC) {
            return false;
        }

        int numZeroBits = (emLen << 3) - emBits;

        if (numZeroBits != 0) {
            byte MASK = (byte) (0xff << (8 - numZeroBits));
            if ((em[emOfs] & MASK) != 0) {
                return false;
            }
        }
        String mgfDigestAlgo;
        AlgorithmParameterSpec mgfParams = this.sigParams.getMGFParameters();
        if (mgfParams != null) {
            mgfDigestAlgo =
                ((MGF1ParameterSpec) mgfParams).getDigestAlgorithm();
        } else {
            mgfDigestAlgo = this.md.getAlgorithm();
        }
        int dbLen = emLen - hLen - 1;
        try {
            MGF1 mgf1 = new MGF1(mgfDigestAlgo);
            mgf1.generateAndXor(em, emOfs + dbLen, hLen, dbLen,
                    em, emOfs);
        } catch (NoSuchAlgorithmException nsae) {
            throw new IOException(nsae.toString());
        }

        if (numZeroBits != 0) {
            byte MASK = (byte) (0xff >>> numZeroBits);
            em[emOfs] = (byte) (em[emOfs] & MASK);
        }

        int i = emOfs;
        for (; i < emOfs + (dbLen - sLen - 1); i++) {
            if (em[i] != 0) {
                return false;
            }
        }
        if (em[i] != 0x01) {
            return false;
        }
        this.md.update(EIGHT_BYTES_OF_ZEROS);
        digestReset = false;
        this.md.update(mHash);
        if (sLen > 0) {
            this.md.update(em, emOfs + (dbLen - sLen), sLen);
        }
        byte[] digest2 = this.md.digest();
        digestReset = true;

        byte[] digestInEM = Arrays.copyOfRange(em, emOfs + dbLen,
                emOfs + emLen - 1);
        return MessageDigest.isEqual(digest2, digestInEM);
    }

    @Deprecated
    @Override
    protected void engineSetParameter(String param, Object value)
            throws InvalidParameterException {
        throw new UnsupportedOperationException("setParameter() not supported");
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.sigParams = validateSigParams(params);
        if (!digestReset) {
            throw new ProviderException
                ("Cannot set parameters during operations");
        }
        String newHashAlg = this.sigParams.getDigestAlgorithm();
        if ((this.md == null) ||
            !(this.md.getAlgorithm().equalsIgnoreCase(newHashAlg))) {
            try {
                this.md = MessageDigest.getInstance(newHashAlg);
            } catch (NoSuchAlgorithmException nsae) {
                throw new InvalidAlgorithmParameterException
                    ("Unsupported digest algorithm " +
                     newHashAlg, nsae);
            }
        }
    }

    @Deprecated
    @Override
    protected Object engineGetParameter(String param)
            throws InvalidParameterException {
        throw new UnsupportedOperationException("getParameter() not supported");
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        AlgorithmParameters ap = null;
        if (this.sigParams != null) {
            try {
                ap = AlgorithmParameters.getInstance("RSASSA-PSS");
                ap.init(this.sigParams);
            } catch (GeneralSecurityException gse) {
                throw new ProviderException(gse.getMessage());
            }
        }
        return ap;
    }
}
