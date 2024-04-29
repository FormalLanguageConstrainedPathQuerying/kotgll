/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.util.Arrays;

import java.nio.ByteBuffer;

import javax.crypto.MacSpi;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.*;

import sun.security.x509.AlgorithmId;

/**
 * This class constitutes the core of HMAC-<MD> algorithms, where
 * <MD> is the digest algorithm used by HMAC as in RFC 2104
 * "HMAC: Keyed-Hashing for Message Authentication".
 *
 * It also contains implementation classes for:
 * - HmacMD5
 * - HmacSHA1
 * - HMAC with SHA-2 family of digests, i.e. HmacSHA224, HmacSHA256,
 *   HmacSHA384, HmacSHA512, HmacSHA512/224, HmacSHA512/256, and
 * - HMAC with SHA-3 family of digests, i.e. HmacSHA3-224, HmacSHA3-256,
 *   HmacSHA3-384, HmacSHA3-512
 *
 * @author Jan Luehe
 */
abstract class HmacCore extends MacSpi implements Cloneable {

    private MessageDigest md;
    private byte[] k_ipad; 
    private byte[] k_opad; 
    private boolean first;       

    private final int blockLen;

    /**
     * Standard constructor, creates a new HmacCore instance instantiating
     * a MessageDigest of the specified name.
     */
    HmacCore(String digestAlgo, int bl) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(digestAlgo);
        if (!(md instanceof Cloneable)) {
            Provider sun = Security.getProvider("SUN");
            if (sun != null) {
                md = MessageDigest.getInstance(digestAlgo, sun);
            } else {
                String noCloneProv = md.getProvider().getName();
                md = null;
                Provider[] provs = Security.getProviders();
                for (Provider p : provs) {
                    try {
                        if (!p.getName().equals(noCloneProv)) {
                            MessageDigest md2 =
                                MessageDigest.getInstance(digestAlgo, p);
                            if (md2 instanceof Cloneable) {
                                md = md2;
                                break;
                            }
                        }
                    } catch (NoSuchAlgorithmException nsae) {
                        continue;
                    }
                }
                if (md == null) {
                    throw new NoSuchAlgorithmException
                            ("No Cloneable digest found for " + digestAlgo);
                }
            }
        }
        this.md = md;
        this.blockLen = bl;
        this.k_ipad = new byte[blockLen];
        this.k_opad = new byte[blockLen];
        first = true;
    }

    /**
     * Returns the length of the HMAC in bytes.
     *
     * @return the HMAC length in bytes.
     */
    protected int engineGetMacLength() {
        return this.md.getDigestLength();
    }

    /**
     * Initializes the HMAC with the given secret key and algorithm parameters.
     *
     * @param key the secret key.
     * @param params the algorithm parameters.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this MAC.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this MAC.
     */
    protected void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("HMAC does not use parameters");
        }

        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Secret key expected");
        }

        byte[] secret = key.getEncoded();
        if (secret == null) {
            throw new InvalidKeyException("Missing key data");
        }

        if (secret.length > blockLen) {
            byte[] tmp = md.digest(secret);
            Arrays.fill(secret, (byte)0);
            secret = tmp;
        }

        for (int i = 0; i < blockLen; i++) {
            int si = (i < secret.length) ? secret[i] : 0;
            k_ipad[i] = (byte)(si ^ 0x36);
            k_opad[i] = (byte)(si ^ 0x5c);
        }

        Arrays.fill(secret, (byte)0);
        secret = null;

        engineReset();
    }

    /**
     * Processes the given byte.
     *
     * @param input the input byte to be processed.
     */
    protected void engineUpdate(byte input) {
        if (first == true) {
            md.update(k_ipad);
            first = false;
        }

        md.update(input);
    }

    /**
     * Processes the first <code>len</code> bytes in <code>input</code>,
     * starting at <code>offset</code>.
     *
     * @param input the input buffer.
     * @param offset the offset in <code>input</code> where the input starts.
     * @param len the number of bytes to process.
     */
    protected void engineUpdate(byte input[], int offset, int len) {
        if (first == true) {
            md.update(k_ipad);
            first = false;
        }

        md.update(input, offset, len);
    }

    /**
     * Processes the <code>input.remaining()</code> bytes in the ByteBuffer
     * <code>input</code>.
     *
     * @param input the input byte buffer.
     */
    protected void engineUpdate(ByteBuffer input) {
        if (first == true) {
            md.update(k_ipad);
            first = false;
        }

        md.update(input);
    }

    /**
     * Completes the HMAC computation and resets the HMAC for further use,
     * maintaining the secret key that the HMAC was initialized with.
     *
     * @return the HMAC result.
     */
    protected byte[] engineDoFinal() {
        if (first == true) {
            md.update(k_ipad);
        } else {
            first = true;
        }

        try {
            byte[] tmp = md.digest();

            md.update(k_opad);
            md.update(tmp);

            md.digest(tmp, 0, tmp.length);
            return tmp;
        } catch (DigestException e) {
            throw new ProviderException(e);
        }
    }

    /**
     * Resets the HMAC for further use, maintaining the secret key that the
     * HMAC was initialized with.
     */
    protected void engineReset() {
        if (first == false) {
            md.reset();
            first = true;
        }
    }

    /*
     * Clones this object.
     */
    public Object clone() throws CloneNotSupportedException {
        HmacCore copy = (HmacCore) super.clone();
        copy.md = (MessageDigest) md.clone();
        copy.k_ipad = k_ipad.clone();
        copy.k_opad = k_opad.clone();
        return copy;
    }

    public static final class HmacSHA224 extends HmacCore {
        public HmacSHA224() throws NoSuchAlgorithmException {
            super("SHA-224", 64);
        }
    }

    public static final class HmacSHA256 extends HmacCore {
        public HmacSHA256() throws NoSuchAlgorithmException {
            super("SHA-256", 64);
        }
    }

    public static final class HmacSHA384 extends HmacCore {
        public HmacSHA384() throws NoSuchAlgorithmException {
            super("SHA-384", 128);
        }
    }

    public static final class HmacSHA512 extends HmacCore {
        public HmacSHA512() throws NoSuchAlgorithmException {
            super("SHA-512", 128);
        }
    }

    public static final class HmacSHA512_224 extends HmacCore {
        public HmacSHA512_224() throws NoSuchAlgorithmException {
            super("SHA-512/224", 128);
        }
    }

    public static final class HmacSHA512_256 extends HmacCore {
        public HmacSHA512_256() throws NoSuchAlgorithmException {
            super("SHA-512/256", 128);
        }
    }

    public static final class HmacSHA3_224 extends HmacCore {
        public HmacSHA3_224() throws NoSuchAlgorithmException {
            super("SHA3-224", 144);
        }
    }

    public static final class HmacSHA3_256 extends HmacCore {
        public HmacSHA3_256() throws NoSuchAlgorithmException {
            super("SHA3-256", 136);
        }
    }

    public static final class HmacSHA3_384 extends HmacCore {
        public HmacSHA3_384() throws NoSuchAlgorithmException {
            super("SHA3-384", 104);
        }
    }

    public static final class HmacSHA3_512 extends HmacCore {
        public HmacSHA3_512() throws NoSuchAlgorithmException {
            super("SHA3-512", 72);
        }
    }
}
