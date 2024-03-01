/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

public class CtrDrbg extends AbstractDrbg {

    private static final int AES_LIMIT;

    static {
        try {
            AES_LIMIT = Cipher.getMaxAllowedKeyLength("AES");
        } catch (Exception e) {
            throw new AssertionError("Cannot detect AES", e);
        }
    }

    private Cipher cipher;

    private String cipherAlg;
    private String keyAlg;

    private int ctrLen;
    private int blockLen;
    private int keyLen;
    private int seedLen;

    private byte[] v;
    private byte[] k;

    public CtrDrbg(SecureRandomParameters params) {
        mechName = "CTR_DRBG";
        configure(params);
    }

    private static int alg2strength(String algorithm) {
        switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "AES-128":
                return 128;
            case "AES-192":
                return 192;
            case "AES-256":
                return 256;
            default:
                throw new IllegalArgumentException(algorithm +
                        " not supported in CTR_DBRG");
        }
    }

    @Override
    protected void chooseAlgorithmAndStrength() {
        if (requestedAlgorithm != null) {
            algorithm = requestedAlgorithm.toUpperCase(Locale.ROOT);
            int supportedStrength = alg2strength(algorithm);
            if (requestedInstantiationSecurityStrength >= 0) {
                int tryStrength = getStandardStrength(
                        requestedInstantiationSecurityStrength);
                if (tryStrength > supportedStrength) {
                    throw new IllegalArgumentException(algorithm +
                            " does not support strength " +
                            requestedInstantiationSecurityStrength);
                }
                this.securityStrength = tryStrength;
            } else {
                this.securityStrength = Math.min(DEFAULT_STRENGTH, supportedStrength);
            }
        } else {
            int tryStrength = (requestedInstantiationSecurityStrength < 0) ?
                    DEFAULT_STRENGTH : requestedInstantiationSecurityStrength;
            tryStrength = getStandardStrength(tryStrength);
            if (tryStrength <= 128 && AES_LIMIT < 256) {
                algorithm = "AES-128";
            } else if (AES_LIMIT >= 256) {
                algorithm = "AES-256";
            } else {
                throw new IllegalArgumentException("unsupported strength " +
                        requestedInstantiationSecurityStrength);
            }
            this.securityStrength = tryStrength;
        }
        switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "AES-128":
            case "AES-192":
            case "AES-256":
                this.keyAlg = "AES";
                this.cipherAlg = "AES/ECB/NoPadding";
                switch (algorithm) {
                    case "AES-128":
                        this.keyLen = 128 / 8;
                        break;
                    case "AES-192":
                        this.keyLen = 192 / 8;
                        if (AES_LIMIT < 192) {
                            throw new IllegalArgumentException(algorithm +
                                " not available (because policy) in CTR_DBRG");
                        }
                        break;
                    case "AES-256":
                        this.keyLen = 256 / 8;
                        if (AES_LIMIT < 256) {
                            throw new IllegalArgumentException(algorithm +
                                " not available (because policy) in CTR_DBRG");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(algorithm +
                            " not supported in CTR_DBRG");
                }
                this.blockLen = 128 / 8;
                break;
            default:
                throw new IllegalArgumentException(algorithm +
                        " not supported in CTR_DBRG");
        }
        this.seedLen = this.blockLen + this.keyLen;
        this.ctrLen = this.blockLen;    
        if (usedf) {
            this.minLength = this.securityStrength / 8;
        } else {
            this.minLength = this.maxLength =
                    this.maxPersonalizationStringLength =
                            this.maxAdditionalInputLength = seedLen;
        }
    }

    /**
     * This call, used by the constructors, instantiates the digest.
     */
    @Override
    protected void initEngine() {
        try {
            /*
             * Use the local SunJCE implementation to avoid native
             * performance overhead.
             */
            cipher = Cipher.getInstance(cipherAlg, "SunJCE");
        } catch (NoSuchProviderException | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            try {
                cipher = Cipher.getInstance(cipherAlg);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException exc) {
                throw new InternalError(
                    "internal error: " + cipherAlg + " not available.", exc);
            }
        }
    }

    private void status() {
        if (debug != null) {
            debug.println(this, "Key = " + HexFormat.of().formatHex(k));
            debug.println(this, "V   = " + HexFormat.of().formatHex(v));
            debug.println(this, "reseed counter = " + reseedCounter);
        }
    }

    private void update(byte[] input) {
        if (input.length != seedLen) {
            throw new IllegalArgumentException("input length not seedLen: "
                    + input.length);
        }
        try {

            int m = (seedLen + blockLen - 1) / blockLen;
            byte[] temp = new byte[m * blockLen];


            for (int i = 0; i < m; i++) {
                addOne(v, ctrLen);
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                cipher.doFinal(v, 0, blockLen, temp, i * blockLen);
            }

            temp = Arrays.copyOf(temp, seedLen);

            for (int i = 0; i < seedLen; i++) {
                temp[i] ^= input[i];
            }

            k = Arrays.copyOf(temp, keyLen);

            v = Arrays.copyOfRange(temp, seedLen - blockLen, seedLen);

        } catch (GeneralSecurityException e) {
            throw new InternalError(e);
        }
    }

    @Override
    protected void instantiateAlgorithm(byte[] ei) {
        if (debug != null) {
            debug.println(this, "instantiate");
        }
        byte[] more;
        if (usedf) {
            if (personalizationString == null) {
                more = nonce;
            } else {
                if (nonce.length + personalizationString.length < 0) {
                    throw new IllegalArgumentException(
                            "nonce plus personalization string is too long");
                }
                more = Arrays.copyOf(
                        nonce, nonce.length + personalizationString.length);
                System.arraycopy(personalizationString, 0, more, nonce.length,
                        personalizationString.length);
            }
        } else {
            more = personalizationString;
        }
        reseedAlgorithm(ei, more);
    }

    /**
     * Block_cipher_df in 10.3.2
     *
     * @param input the input string
     * @return the output block (always of seedLen)
     */
    private byte[] df(byte[] input) {
        int l = input.length;
        int n = seedLen;
        byte[] ln = new byte[8];
        ln[0] = (byte)(l >> 24);
        ln[1] = (byte)(l >> 16);
        ln[2] = (byte)(l >> 8);
        ln[3] = (byte)(l);
        ln[4] = (byte)(n >> 24);
        ln[5] = (byte)(n >> 16);
        ln[6] = (byte)(n >> 8);
        ln[7] = (byte)(n);


        byte[] k = new byte[keyLen];
        for (int i = 0; i < k.length; i++) {
            k[i] = (byte)i;
        }

        byte[] temp = new byte[seedLen];

        for (int i = 0; i * blockLen < temp.length; i++) {
            byte[] iv = new byte[blockLen];
            iv[0] = (byte)(i >> 24);
            iv[1] = (byte)(i >> 16);
            iv[2] = (byte)(i >> 8);
            iv[3] = (byte)(i);

            int tailLen = temp.length - blockLen*i;
            if (tailLen > blockLen) {
                tailLen = blockLen;
            }
            System.arraycopy(bcc(k, iv, ln, input, new byte[]{(byte)0x80}),
                    0, temp, blockLen*i, tailLen);
        }

        k = Arrays.copyOf(temp, keyLen);

        byte[] x = Arrays.copyOfRange(temp, keyLen, temp.length);


        for (int i = 0; i * blockLen < seedLen; i++) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                int tailLen = temp.length - blockLen*i;
                if (tailLen > blockLen) {
                    tailLen = blockLen;
                }
                x = cipher.doFinal(x);
                System.arraycopy(x, 0, temp, blockLen * i, tailLen);
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
        }

        return temp;
    }

    /**
     * Block_Encrypt in 10.3.3
     *
     * @param k the key
     * @param data after concatenated, the data to be operated upon. This is
     *             a series of byte[], each with an arbitrary length. Note
     *             that the full length is not necessarily a multiple of
     *             outlen. XOR with zero is no-op.
     * @return the result
     */
    private byte[] bcc(byte[] k, byte[]... data) {
        byte[] chain = new byte[blockLen];
        int n1 = 0; 
        int n2 = 0; 
        while (n1 < data.length) {
            int j;
            out: for (j = 0; j < blockLen; j++) {
                while (n2 >= data[n1].length) {
                    n1++;
                    if (n1 >= data.length) {
                        break out;
                    }
                    n2 = 0;
                }
                chain[j] ^= data[n1][n2];
                n2++;
            }
            if (j == 0) { 
                break;
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                chain = cipher.doFinal(chain);
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
        }
        return chain;
    }

    @Override
    protected synchronized void reseedAlgorithm(
            byte[] ei,
            byte[] additionalInput) {
        if (usedf) {

            if (additionalInput != null) {
                if (ei.length + additionalInput.length < 0) {
                    throw new IllegalArgumentException(
                            "entropy plus additional input is too long");
                }
                byte[] temp = Arrays.copyOf(
                        ei, ei.length + additionalInput.length);
                System.arraycopy(additionalInput, 0, temp, ei.length,
                        additionalInput.length);
                ei = temp;
            }
            ei = df(ei);
        } else {
            if (additionalInput != null) {
                for (int i = 0; i < additionalInput.length; i++) {
                    ei[i] ^= additionalInput[i];
                }
            }
        }

        if (v == null) {
            k = new byte[keyLen];
            v = new byte[blockLen];
        }

        update(ei);
        reseedCounter = 1;

    }

    /**
     * Add one to data, only touch the last len bytes.
     */
    private static void addOne(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            data[data.length - 1 - i]++;
            if (data[data.length - 1 - i] != 0) {
                break;
            }
        }
    }

    @Override
    public synchronized void generateAlgorithm(
            byte[] result, byte[] additionalInput) {

        if (debug != null) {
            debug.println(this, "generateAlgorithm");
        }



        if (additionalInput != null) {
            if (usedf) {
                additionalInput = df(additionalInput);
            } else {
                additionalInput = Arrays.copyOf(additionalInput, seedLen);
            }
            update(additionalInput);
        } else {
            additionalInput = new byte[seedLen];
        }

        int pos = 0;
        int len = result.length;

        while (len > 0) {
            addOne(v, ctrLen);
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                if (len > blockLen) {
                    cipher.doFinal(v, 0, blockLen, result, pos);
                } else {
                    byte[] out = cipher.doFinal(v);
                    System.arraycopy(out, 0, result, pos, len);
                    Arrays.fill(out, (byte)0);
                }
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
            len -= blockLen;
            if (len <= 0) {
                break;
            }
            pos += blockLen;
        }

        update(additionalInput);

        reseedCounter++;


    }

    @Override
    public String toString() {
        return super.toString() + ","
                + (usedf ? "use_df" : "no_df");
    }
}
