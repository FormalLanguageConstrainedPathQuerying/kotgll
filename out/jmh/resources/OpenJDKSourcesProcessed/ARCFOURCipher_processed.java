/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.*;

/**
 * Implementation of the ARCFOUR cipher, an algorithm apparently compatible
 * with RSA Security's RC4(tm) cipher. The description of this algorithm was
 * taken from Bruce Schneier's book Applied Cryptography, 2nd ed.,
 * section 17.1.
 *
 * We support keys from 40 to 1024 bits. ARCFOUR would allow for keys shorter
 * than 40 bits, but that is too insecure for us to permit.
 *
 * Note that we subclass CipherSpi directly and do not use the CipherCore
 * framework. That was designed to simplify implementation of block ciphers
 * and does not offer any advantages for stream ciphers such as ARCFOUR.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public sealed class ARCFOURCipher extends CipherSpi
        permits PKCS12PBECipherCore.PBEWithSHA1AndRC4 {

    private final int[] S;

    private int is, js;

    private byte[] lastKey;

    public ARCFOURCipher() {
        S = new int[256];
    }

    private void init(byte[] key) {
        for (int i = 0; i < 256; i++) {
            S[i] = i;
        }

        for (int i = 0, j = 0, ki = 0; i < 256; i++) {
            int Si = S[i];
            j = (j + Si + key[ki]) & 0xff;
            S[i] = S[j];
            S[j] = Si;
            ki++;
            if (ki == key.length) {
                ki = 0;
            }
        }

        is = 0;
        js = 0;
    }

    private void crypt(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) {
        if (is < 0) {
            init(lastKey);
        }
        while (inLen-- > 0) {
            is = (is + 1) & 0xff;
            int Si = S[is];
            js = (js + Si) & 0xff;
            int Sj = S[js];
            S[is] = Sj;
            S[js] = Si;
            out[outOfs++] = (byte)(in[inOfs++] ^ S[(Si + Sj) & 0xff]);
        }
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (mode.equalsIgnoreCase("ECB") == false) {
            throw new NoSuchAlgorithmException("Unsupported mode " + mode);
        }
    }

    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if (padding.equalsIgnoreCase("NoPadding") == false) {
            throw new NoSuchPaddingException("Padding must be NoPadding");
        }
    }

    protected int engineGetBlockSize() {
        return 0;
    }

    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    protected byte[] engineGetIV() {
        return null;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {
        init(opmode, key);
    }

    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("Parameters not supported");
        }
        init(opmode, key);
    }

    protected void engineInit(int opmode, Key key,
            AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("Parameters not supported");
        }
        init(opmode, key);
    }

    private void init(int opmode, Key key) throws InvalidKeyException {


        if (lastKey != null) {
            Arrays.fill(lastKey, (byte)0);
        }

        lastKey = getEncodedKey(key);
        init(lastKey);
    }

    private static byte[] getEncodedKey(Key key) throws InvalidKeyException {
        String keyAlg = key.getAlgorithm();
        if (!keyAlg.equals("RC4") && !keyAlg.equals("ARCFOUR")) {
            throw new InvalidKeyException("Not an ARCFOUR key: " + keyAlg);
        }
        if ("RAW".equals(key.getFormat()) == false) {
            throw new InvalidKeyException("Key encoding format must be RAW");
        }
        byte[] encodedKey = key.getEncoded();
        if ((encodedKey.length < 5) || (encodedKey.length > 128)) {
            Arrays.fill(encodedKey, (byte)0);
            throw new InvalidKeyException
                ("Key length must be between 40 and 1024 bit");
        }
        return encodedKey;
    }

    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        byte[] out = new byte[inLen];
        crypt(in, inOfs, inLen, out, 0);
        return out;
    }

    protected int engineUpdate(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws ShortBufferException {
        if (out.length - outOfs < inLen) {
            throw new ShortBufferException("Output buffer too small");
        }
        crypt(in, inOfs, inLen, out, outOfs);
        return inLen;
    }

    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen) {
        byte[] out = engineUpdate(in, inOfs, inLen);
        is = -1;
        return out;
    }

    protected int engineDoFinal(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws ShortBufferException {
        int outLen = engineUpdate(in, inOfs, inLen, out, outOfs);
        is = -1;
        return outLen;
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException,
            InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if ((encoded == null) || (encoded.length == 0)) {
            throw new InvalidKeyException("Could not obtain encoded key");
        }
        try {
            return engineDoFinal(encoded, 0, encoded.length);
        } finally {
            Arrays.fill(encoded, (byte)0);
        }
    }

    protected Key engineUnwrap(byte[] wrappedKey, String algorithm,
            int type) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] encoded = null;
        try {
            encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            return ConstructKeys.constructKey(encoded, algorithm, type);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        byte[] encodedKey = getEncodedKey(key);
        Arrays.fill(encodedKey, (byte)0);
        return Math.multiplyExact(encodedKey.length, 8);
    }

}
