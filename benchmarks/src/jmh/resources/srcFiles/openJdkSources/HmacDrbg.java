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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandomParameters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

public class HmacDrbg extends AbstractHashDrbg {

    private Mac mac;

    private String macAlg;

    private byte[] v;
    private byte[] k;

    @SuppressWarnings("this-escape")
    public HmacDrbg(SecureRandomParameters params) {
        mechName = "HMAC_DRBG";
        configure(params);
    }

    private void status() {
        if (debug != null) {
            debug.println(this, "V = " + HexFormat.of().formatHex(v));
            debug.println(this, "Key = " + HexFormat.of().formatHex(k));
            debug.println(this, "reseed counter = " + reseedCounter);
        }
    }

    private void update(List<byte[]> inputs) {
        try {
            mac.init(new SecretKeySpec(k, macAlg));
            mac.update(v);
            mac.update((byte) 0);
            for (byte[] input: inputs) {
                mac.update(input);
            }
            k = mac.doFinal();

            mac.init(new SecretKeySpec(k, macAlg));
            v = mac.doFinal(v);

            if (!inputs.isEmpty()) {
                mac.update(v);
                mac.update((byte) 1);
                for (byte[] input: inputs) {
                    mac.update(input);
                }
                k = mac.doFinal();

                mac.init(new SecretKeySpec(k, macAlg));
                v = mac.doFinal(v);
            } 

        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
    }

    /**
     * This call, used by the constructors, instantiates the digest.
     */
    @Override
    protected void initEngine() {
        macAlg = "HmacSHA" + algorithm.substring(4);
        try {
            /*
             * Use the local SunJCE implementation to avoid native
             * performance overhead.
             */
            mac = Mac.getInstance(macAlg, "SunJCE");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            try {
                mac = Mac.getInstance(macAlg);
            } catch (NoSuchAlgorithmException exc) {
                throw new InternalError(
                    "internal error: " + macAlg + " not available.", exc);
            }
        }
    }

    @Override
    protected final synchronized void hashReseedInternal(List<byte[]> input) {

        if (v == null) {
            k = new byte[outLen];
            v = new byte[outLen];
            Arrays.fill(v, (byte) 1);
        }

        update(input);

        reseedCounter = 1;

    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param result the array to be filled in with random bytes.
     */
    @Override
    public synchronized void generateAlgorithm(
            byte[] result, byte[] additionalInput) {

        if (debug != null) {
            debug.println(this, "generateAlgorithm");
        }



        if (additionalInput != null) {
            update(Collections.singletonList(additionalInput));
        }

        int pos = 0;
        int len = result.length;

        while (len > 0) {
            try {
                mac.init(new SecretKeySpec(k, macAlg));
            } catch (InvalidKeyException e) {
                throw new InternalError(e);
            }
            v = mac.doFinal(v);
            System.arraycopy(v, 0, result, pos,
                    Math.min(len, outLen));

            len -= outLen;
            if (len <= 0) {
                break;
            }
            pos += outLen;
        }


        if (additionalInput != null) {
            update(Collections.singletonList(additionalInput));
        } else {
            update(Collections.emptyList());
        }

        reseedCounter++;


    }
}
