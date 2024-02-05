/*
 * Copyright (c) 2005, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.mscapi;

import java.util.UUID;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

import sun.security.rsa.RSAKeyFactory;
import static sun.security.util.SecurityProviderConstants.DEF_RSA_KEY_SIZE;

/**
 * RSA keypair generator.
 *
 * Standard algorithm, minimum key length is 512 bit, maximum is 16,384.
 * Generates a private key that is exportable.
 *
 * @since 1.6
 */
public abstract class CKeyPairGenerator extends KeyPairGeneratorSpi {

    protected String keyAlg;

    public CKeyPairGenerator(String keyAlg) {
        this.keyAlg = keyAlg;
    }

    public static class RSA extends CKeyPairGenerator {
        public RSA() {
            super("RSA");
            initialize(DEF_RSA_KEY_SIZE, null);
        }

        static final int KEY_SIZE_MIN = 512; 
        static final int KEY_SIZE_MAX = 16384;

        private int keySize;

        @Override
        public void initialize(int keySize, SecureRandom random) {

            try {
                RSAKeyFactory.checkKeyLengths(keySize, null,
                        KEY_SIZE_MIN, KEY_SIZE_MAX);
            } catch (InvalidKeyException e) {
                throw new InvalidParameterException(e);
            }

            this.keySize = keySize;
        }

        @Override
        public void initialize(AlgorithmParameterSpec params, SecureRandom random)
                throws InvalidAlgorithmParameterException {

            int tmpSize;
            if (params == null) {
                tmpSize = DEF_RSA_KEY_SIZE;
            } else if (params instanceof RSAKeyGenParameterSpec) {

                if (((RSAKeyGenParameterSpec) params).getPublicExponent() != null) {
                    throw new InvalidAlgorithmParameterException
                            ("Exponent parameter is not supported");
                }
                tmpSize = ((RSAKeyGenParameterSpec) params).getKeysize();

            } else {
                throw new InvalidAlgorithmParameterException
                        ("Params must be an instance of RSAKeyGenParameterSpec");
            }

            try {
                RSAKeyFactory.checkKeyLengths(tmpSize, null,
                        KEY_SIZE_MIN, KEY_SIZE_MAX);
            } catch (InvalidKeyException e) {
                throw new InvalidAlgorithmParameterException(
                        "Invalid Key sizes", e);
            }

            this.keySize = tmpSize;
        }

        @Override
        public KeyPair generateKeyPair() {

            try {
                CKeyPair keys =
                        generateCKeyPair(keyAlg, keySize,
                                "{" + UUID.randomUUID().toString() + "}");
                return new KeyPair(keys.getPublic(), keys.getPrivate());

            } catch (KeyException e) {
                throw new ProviderException(e);
            }
        }

        private static native CKeyPair generateCKeyPair(String alg, int keySize,
                String keyContainerName) throws KeyException;
    }
}
