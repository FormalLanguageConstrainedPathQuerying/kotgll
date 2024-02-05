/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug 8254717 8263404
 * @summary isAssignableFrom checks in KeyFactorySpi.engineGetKeySpec appear to be backwards.
 * @author Greg Rubin, Ziyi Luo
 */

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.*;

public class KeyFactoryGetKeySpecForInvalidSpec {

    public static RSAPrivateKey privateCrtToPrivate(RSAPrivateCrtKey crtKey) {
        return new RSAPrivateKey() {
            @Override
            public BigInteger getPrivateExponent() {
                return crtKey.getPrivateExponent();
            }

            @Override
            public String getAlgorithm() {
                return crtKey.getAlgorithm();
            }

            @Override
            public String getFormat() {
                return crtKey.getFormat();
            }

            @Override
            public byte[] getEncoded() {
                return crtKey.getEncoded();
            }

            @Override
            public BigInteger getModulus() {
                return crtKey.getModulus();
            }
        };
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        kg.initialize(2048);
        KeyPair pair = kg.generateKeyPair();

        KeyFactory factory = KeyFactory.getInstance("RSA");

        KeySpec spec = factory.getKeySpec(pair.getPrivate(), RSAPrivateKeySpec.class);
        if (!(spec instanceof RSAPrivateCrtKeySpec)) {
            throw new Exception("Spec should be an instance of RSAPrivateCrtKeySpec");
        }

        spec = factory.getKeySpec(pair.getPrivate(), RSAPrivateCrtKeySpec.class);
        if (!(spec instanceof RSAPrivateCrtKeySpec)) {
            throw new Exception("Spec should be an instance of RSAPrivateCrtKeySpec");
        }

        RSAPrivateKey notCrtKey = privateCrtToPrivate((RSAPrivateCrtKey)pair.getPrivate());
        KeySpec notCrtSpec = factory.getKeySpec(notCrtKey, RSAPrivateKeySpec.class);
        if (notCrtSpec instanceof RSAPrivateCrtKeySpec) {
            throw new Exception("Spec should be an instance of RSAPrivateKeySpec not RSAPrivateCrtKeySpec");
        }
        if (!(notCrtSpec instanceof RSAPrivateKeySpec)) {
            throw new Exception("Spec should be an instance of RSAPrivateKeySpec");
        }

        try {
            factory.getKeySpec(notCrtKey, RSAPrivateCrtKeySpec.class);
            throw new Exception("InvalidKeySpecException is expected but not thrown");
        } catch (InvalidKeySpecException e) {
        }

        try {
            spec = factory.getKeySpec(pair.getPublic(), FakeX509Spec.class);
            throw new Exception("InvalidKeySpecException is expected but not thrown");
        } catch (final ClassCastException ex) {
            throw new Exception("InvalidKeySpecException is expected ClassCastException is thrown", ex);
        } catch (final InvalidKeySpecException ex) {
        }
    }

    public static class FakeX509Spec extends X509EncodedKeySpec {
        public FakeX509Spec(byte[] encodedKey) {
            super(encodedKey);
        }

        public FakeX509Spec(byte[] encodedKey, String algorithm) {
            super(encodedKey, algorithm);
        }
    }
}
