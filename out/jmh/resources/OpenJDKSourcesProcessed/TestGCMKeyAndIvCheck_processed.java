/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6996769
 * @library ../UTIL
 * @build TestUtil
 * @run main TestGCMKeyAndIvCheck
 * @summary Ensure that same key+iv can't be repeated used for encryption.
 * @author Valerie Peng
 */


import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;

import java.util.*;

public class TestGCMKeyAndIvCheck {

    private static final byte[] AAD = new byte[5];
    private static final byte[] PT = new byte[18];

    private static void checkISE(Cipher c) throws Exception {
        try {
            c.updateAAD(AAD);
            throw new Exception("Should throw ISE for updateAAD()");
        } catch (IllegalStateException ise) {
        }

        try {
            c.update(PT);
            throw new Exception("Should throw ISE for update()");
        } catch (IllegalStateException ise) {
        }
        try {
            c.doFinal(PT);
            throw new Exception("Should throw ISE for doFinal()");
        } catch (IllegalStateException ise) {
        }
    }

    public void test() throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");

        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        c.updateAAD(AAD);
        byte[] ctPlusTag = c.doFinal(PT);

        checkISE(c);

        AlgorithmParameters params = c.getParameters();
        if (params == null) {
            throw new Exception("getParameters() should not return null");
        }
        GCMParameterSpec spec = params.getParameterSpec(GCMParameterSpec.class);
        if (spec.getTLen() != (ctPlusTag.length - PT.length)*8) {
            throw new Exception("Parameters contains incorrect TLen value");
        }
        if (!Arrays.equals(spec.getIV(), c.getIV())) {
            throw new Exception("Parameters contains incorrect IV value");
        }

        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        byte[] recovered = c.doFinal(ctPlusTag);
        if (!Arrays.equals(recovered, PT)) {
            throw new Exception("decryption result mismatch");
        }

        try {
            c.init(Cipher.ENCRYPT_MODE, key, params);
            throw new Exception("Should throw exception when same key+iv is used");
        } catch (InvalidAlgorithmParameterException iape) {
        }

        c.init(Cipher.ENCRYPT_MODE, key);
        c.doFinal(PT);

        byte[] iv = c.getIV();
        if (Arrays.equals(spec.getIV(), iv)) {
            throw new Exception("IV should be different now");
        }

        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, new byte[30]));
        c.updateAAD(AAD);
        c.doFinal(PT);
        checkISE(c);

        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);

        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);
        if (!Arrays.equals(recovered, PT)) {
            throw new Exception("decryption result mismatch");
        }

        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);

        try {
            c.init(Cipher.DECRYPT_MODE, key);
            throw new Exception("Should throw IKE for dec w/o params");
        } catch (InvalidKeyException ike) {
        }

        try {
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            throw new Exception("Should throw IAPE");
        } catch (InvalidAlgorithmParameterException iape) {
        }
        try {
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            throw new Exception("Should throw IAPE");
        } catch (InvalidAlgorithmParameterException iape) {
        }

        System.out.println("Test Passed!");
    }

    public static void main (String[] args) throws Exception {
        TestGCMKeyAndIvCheck t = new TestGCMKeyAndIvCheck();
        t.test();
    }
}

