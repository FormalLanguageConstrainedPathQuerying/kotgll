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

import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandomParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class HashDrbg extends AbstractHashDrbg {

    private static final byte[] ZERO = new byte[1];
    private static final byte[] ONE = new byte[]{1};

    private MessageDigest digest;

    private byte[] v;
    private byte[] c;

    @SuppressWarnings("this-escape")
    public HashDrbg(SecureRandomParameters params) {
        mechName = "Hash_DRBG";
        configure(params);
    }

    /**
     * This call, used by the constructors, instantiates the digest.
     */
    @Override
    protected void initEngine() {
        try {
            /*
             * Use the local SUN implementation to avoid native
             * performance overhead.
             */
            digest = MessageDigest.getInstance(algorithm, "SUN");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            try {
                digest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException exc) {
                throw new InternalError(
                    "internal error: " + algorithm + " not available.", exc);
            }
        }
    }

    private byte[] hashDf(int requested, List<byte[]> inputs) {
        return hashDf(digest, outLen, requested, inputs);
    }

    /**
     * A hash-based derivation function defined in NIST SP 800-90Ar1 10.3.1.
     * The function is used inside Hash_DRBG, and can also be used as an
     * approved conditioning function as described in 800-90B 6.4.2.2.
     *
     * Note: In each current call, requested is seedLen, therefore small,
     * no need to worry about overflow.
     *
     * @param digest a {@code MessageDigest} object in reset state
     * @param outLen {@link MessageDigest#getDigestLength} of {@code digest}
     * @param requested requested output length, in bytes
     * @param inputs input data
     * @return the condensed/expanded output
     */
    public static byte[] hashDf(MessageDigest digest, int outLen,
                                int requested, List<byte[]> inputs) {
        int len = (requested + outLen - 1) / outLen;
        byte[] temp = new byte[len * outLen];
        int counter = 1;

        for (int i=0; i<len; i++) {
            digest.update((byte) counter);
            digest.update((byte)(requested >> 21)); 
            digest.update((byte)(requested >> 13));
            digest.update((byte)(requested >> 5));
            digest.update((byte)(requested << 3));
            for (byte[] input : inputs) {
                digest.update(input);
            }
            try {
                digest.digest(temp, i * outLen, outLen);
            } catch (DigestException e) {
                throw new AssertionError("will not happen", e);
            }
            counter++;
        }
        return temp.length == requested? temp: Arrays.copyOf(temp, requested);
    }

    @Override
    protected final synchronized void hashReseedInternal(List<byte[]> inputs) {

        byte[] seed;

        if (v != null) {
            inputs.add(0, ONE);
            inputs.add(1, v);
        }
        seed = hashDf(seedLen, inputs);

        v = seed;

        inputs = new ArrayList<>(2);
        inputs.add(ZERO);
        inputs.add(v);
        c = hashDf(seedLen, inputs);

        reseedCounter = 1;


    }

    private void status() {
        if (debug != null) {
            debug.println(this, "V = " + HexFormat.of().formatHex(v));
            debug.println(this, "C = " + HexFormat.of().formatHex(c));
            debug.println(this, "reseed counter = " + reseedCounter);
        }
    }

    /**
     * Adds byte arrays into an existing one.
     *
     * @param out existing array
     * @param data more arrays, can be of different length
     */
    private static void addBytes(byte[] out, int len, byte[]... data) {
        for (byte[] d: data) {
            int dlen = d.length;
            int carry = 0;
            for (int i = 0; i < len; i++) {
                int sum = (out[len - i - 1] & 0xff) + carry;
                if (i < dlen) {
                    sum += (d[dlen - i - 1] & 0xff);
                }
                out[len - i - 1] = (byte) sum;
                carry = sum >> 8;
                if (i >= dlen - 1 && carry == 0) break;
            }
        }
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param result the array to be filled in with random bytes.
     */
    @Override
    public final synchronized void generateAlgorithm(
            byte[] result, byte[] additionalInput) {

        if (debug != null) {
            debug.println(this, "generateAlgorithm");
        }



        if (additionalInput != null) {
            digest.update((byte)2);
            digest.update(v);
            digest.update(additionalInput);
            addBytes(v, seedLen, digest.digest());
        }

        hashGen(result, v);

        digest.update((byte)3);
        digest.update(v);
        byte[] h = digest.digest();

        byte[] rcBytes;
        if (reseedCounter < 256) {
            rcBytes = new byte[]{(byte)reseedCounter};
        } else {
            rcBytes = BigInteger.valueOf(reseedCounter).toByteArray();
        }
        addBytes(v, seedLen, h, c, rcBytes);

        reseedCounter++;


    }

    private void hashGen(byte[] output, byte[] v) {

        byte[] data = v;


        int pos = 0;
        int len = output.length;

        while (len > 0) {
            digest.update(data);
            if (len < outLen) {
                byte[] out = digest.digest();
                System.arraycopy(out, 0, output, pos, len);
                Arrays.fill(out, (byte)0);
            } else {
                try {
                    digest.digest(output, pos, outLen);
                } catch (DigestException e) {
                    throw new AssertionError("will not happen", e);
                }
            }
            len -= outLen;
            if (len <= 0) {
                break;
            }
            if (data == v) {
                data = Arrays.copyOf(v, v.length);
            }
            addBytes(data, seedLen, ONE);
            pos += outLen;
        }

    }
}
