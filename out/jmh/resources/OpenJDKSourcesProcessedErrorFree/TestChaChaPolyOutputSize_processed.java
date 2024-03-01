/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255410
 * @summary Check ChaCha20-Poly1305 cipher output size
 * @library /test/lib ..
 * @build jdk.test.lib.Convert
 * @run main TestChaChaPolyOutputSize
 */

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TestChaChaPolyOutputSize extends PKCS11Test {

    private static final SecureRandom SR = new SecureRandom();

    private static final SecretKeySpec KEY = new SecretKeySpec(new byte[32],
            "ChaCha20");

    private static final String ALGO = "ChaCha20-Poly1305";

    public static void main(String args[]) throws Exception {
        main(new TestChaChaPolyOutputSize(), args);
    }

    @Override
    public void main(Provider p) throws GeneralSecurityException {
        System.out.println("Testing " + p.getName());
        try {
            Cipher.getInstance(ALGO, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Skip; no support for " + ALGO);
            return;
        }
        testGetOutSize(p);
        testMultiPartAEADDec(p);
    }

    private static void testGetOutSize(Provider p)
            throws GeneralSecurityException {

        Cipher ccp = Cipher.getInstance(ALGO, p);
        ccp.init(Cipher.ENCRYPT_MODE, KEY,
                new IvParameterSpec(getRandBuf(12)));

        testOutLen(ccp, 0, 16);
        testOutLen(ccp, 5, 21);
        testOutLen(ccp, 5120, 5136);
        byte[] input = new byte[5120];
        SR.nextBytes(input);
        byte[] updateOut = ccp.update(input);
        testOutLen(ccp, 1024, 1040 +
                (5120 - (updateOut == null? 0 : updateOut.length)));

        ccp.init(Cipher.DECRYPT_MODE, KEY, new IvParameterSpec(getRandBuf(12)));
        testOutLen(ccp, 0, 0);
        testOutLen(ccp, 5, 0);
        testOutLen(ccp, 16, 0);
        testOutLen(ccp, 5120, 5104);
        updateOut = ccp.update(input);
        testOutLen(ccp, 1024, 6128 - (updateOut == null? 0 : updateOut.length));
    }

    private static void testMultiPartAEADDec(Provider p)
            throws GeneralSecurityException {
        IvParameterSpec ivps = new IvParameterSpec(getRandBuf(12));

        byte[] pText = getRandBuf(2048);
        ByteBuffer pTextBase = ByteBuffer.wrap(pText);

        Cipher enc = Cipher.getInstance(ALGO, p);
        enc.init(Cipher.ENCRYPT_MODE, KEY, ivps);
        ByteBuffer ctBuf = ByteBuffer.allocateDirect(
                enc.getOutputSize(pText.length));
        enc.doFinal(pTextBase, ctBuf);

        ByteBuffer ptBuf = ByteBuffer.allocateDirect(pText.length);

        ctBuf.position(0).limit(1024);

        Cipher dec = Cipher.getInstance(ALGO, p);
        dec.init(Cipher.DECRYPT_MODE, KEY, ivps);
        dec.update(ctBuf, ptBuf);
        System.out.println("CTBuf: " + ctBuf);
        System.out.println("PTBuf: " + ptBuf);
        ctBuf.limit(ctBuf.capacity());
        dec.doFinal(ctBuf, ptBuf);

        ptBuf.flip();
        pTextBase.flip();
        System.out.println("PT Base:" + pTextBase);
        System.out.println("PT Actual:" + ptBuf);

        if (pTextBase.compareTo(ptBuf) != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Plaintext mismatch: Original: ").
                    append(pTextBase.toString()).append("\nActual :").
                    append(ptBuf);
            throw new RuntimeException(sb.toString());
        }
    }

    private static void testOutLen(Cipher c, int inLen, int expOut) {
        int actualOut = c.getOutputSize(inLen);
        if (actualOut != expOut) {
            throw new RuntimeException("Cipher " + c + ", in: " + inLen +
                    ", expOut: " + expOut + ", actual: " + actualOut);
        }
    }

    private static byte[] getRandBuf(int len) {
        byte[] buf = new byte[len];
        SR.nextBytes(buf);
        return buf;
    }
}
