/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7143872
 * @summary Improve certificate extension processing
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRLEntry;
import java.util.Date;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.*;

public class OrderAndDup {
    public static void main(String[] args) throws Exception {

        int count = 20;
        BigInteger[] serials = new BigInteger[count];
        for (int i=0; i<count; i++) {
            serials[i] = BigInteger.valueOf(i*7%10);
        }

        X509CRLEntry[] badCerts = new X509CRLEntry[count];
        for (int i=0; i<count; i++) {
            badCerts[i] = new X509CRLEntryImpl(serials[i],
                    new Date(System.currentTimeMillis()+i*1000));
        }
        X500Name owner = new X500Name("CN=CA");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        X509CRLImpl crl = X509CRLImpl.newSigned(
                new X509CRLImpl.TBSCertList(owner, new Date(), new Date(), badCerts),
                kpg.genKeyPair().getPrivate(), "SHA1withRSA");
        byte[] data = crl.getEncodedInternal();

        checkData(crl, data, serials);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRLImpl crl2 = (X509CRLImpl)cf.generateCRL(new ByteArrayInputStream(data));

        data = crl2.getEncodedInternal();
        checkData(crl2, data, serials);
    }

    static void checkData(X509CRLImpl c, byte[] data, BigInteger[] expected)
            throws Exception {
        if (c.getRevokedCertificates().size() != expected.length) {
            throw new Exception("Wrong count in CRL object, now " +
                    c.getRevokedCertificates().size());
        }
        DerValue d1 = new DerValue(data);
        DerValue[] d2 = new DerInputStream(
                d1.data.getSequence(0)[4].toByteArray())
                .getSequence(0);
        if (d2.length != expected.length) {
            throw new Exception("Wrong count in raw data, now " + d2.length);
        }
        for (int i=0; i<d2.length; i++) {
            BigInteger bi = d2[i].data.getBigInteger();
            if (!bi.equals(expected[i])) {
                throw new Exception("Entry at #" + i + " is " + bi
                        + ", should be " + expected[i]);
            }
        }
    }
}

