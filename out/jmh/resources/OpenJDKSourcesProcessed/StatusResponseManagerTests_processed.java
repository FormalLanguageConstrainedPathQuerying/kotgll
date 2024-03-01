/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.cert.*;
import java.util.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;

import static sun.security.ssl.CertStatusExtension.*;

/*
 * Checks that the hash value for a certificate's issuer name is generated
 * correctly. Requires any certificate that is not self-signed.
 *
 * NOTE: this test uses Sun private classes which are subject to change.
 */
public class StatusResponseManagerTests {

    private static final boolean debug = true;
    private static final boolean ocspDebug = false;

    private static Field responseCacheField;

    static String passwd = "passphrase";
    static String ROOT_ALIAS = "root";
    static String INT_ALIAS = "intermediate";
    static String SSL_ALIAS = "ssl";
    static KeyStore rootKeystore;           
    static KeyStore intKeystore;            
    static KeyStore serverKeystore;         
    static KeyStore trustStore;             
    static X509Certificate rootCert;
    static X509Certificate intCert;
    static X509Certificate sslCert;
    static SimpleOCSPServer rootOcsp;       
    static int rootOcspPort;                
    static SimpleOCSPServer intOcsp;        
    static int intOcspPort;                 

    static X509Certificate[] chain;

    public static void main(String[] args) throws Exception {
        responseCacheField =
                StatusResponseManager.class.getDeclaredField("responseCache");
        responseCacheField.setAccessible(true);

        Map<String, TestCase> testList =
                new LinkedHashMap<String, TestCase>() {{
            put("Basic OCSP fetch test", testOcspFetch);
            put("Clear StatusResponseManager cache", testClearSRM);
            put("Basic OCSP_MULTI fetch test", testOcspMultiFetch);
            put("Test Cache Expiration", testCacheExpiry);
        }};

        createPKI();

        sslCert = (X509Certificate)serverKeystore.getCertificate(SSL_ALIAS);
        intCert = (X509Certificate)intKeystore.getCertificate(INT_ALIAS);
        rootCert = (X509Certificate)rootKeystore.getCertificate(ROOT_ALIAS);
        chain = new X509Certificate[3];
        chain[0] = sslCert;
        chain[1] = intCert;
        chain[2] = rootCert;

        runTests(testList);

        intOcsp.stop();
        rootOcsp.stop();
    }

    public static final TestCase testOcspFetch = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP;

            try {
                Map<X509Certificate, byte[]> responseMap = srm.get(
                        CertStatusRequestType.OCSP, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                if (responseMap.size() != 1) {
                    message = "Incorrect number of responses: expected 1, got "
                            + responseMap.size();
                } else if (!responseMap.containsKey(sslCert)) {
                    message = "Response map key is incorrect, expected " +
                            sslCert.getSubjectX500Principal().toString();
                } else if (responseCacheSize(srm) != 1) {
                    message = "Incorrect number of cache entries: " +
                            "expected 1, got " + responseCacheSize(srm);
                } else {
                    pass = Boolean.TRUE;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase testClearSRM = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                srm.get(CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                if (responseCacheSize(srm) != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + responseCacheSize(srm);
                } else {
                    clearResponseCache(srm);
                    if (responseCacheSize(srm) != 0) {
                        message = "Incorrect number of responses: expected 0," +
                                " got " + responseCacheSize(srm);
                    } else {
                        pass = Boolean.TRUE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase testOcspMultiFetch = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                Map<X509Certificate, byte[]> responseMap = srm.get(
                        CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                if (responseMap.size() != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + responseMap.size();
                } else if (!responseMap.containsKey(sslCert) ||
                        !responseMap.containsKey(intCert)) {
                    message = "Response map keys are incorrect, expected " +
                            sslCert.getSubjectX500Principal().toString() +
                            " and " +
                            intCert.getSubjectX500Principal().toString();
                } else if (responseCacheSize(srm) != 2) {
                    message = "Incorrect number of cache entries: " +
                            "expected 2, got " + responseCacheSize(srm);
                } else {
                    pass = Boolean.TRUE;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase testCacheExpiry = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            System.setProperty("jdk.tls.stapling.cacheLifetime", "5");
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                srm.get(CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                if (responseCacheSize(srm) != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + responseCacheSize(srm);
                } else {
                    Thread.sleep(7000);
                    if (responseCacheSize(srm) != 0) {
                        message = "Incorrect number of responses: expected 0," +
                                " got " + responseCacheSize(srm);
                    } else {
                        pass = Boolean.TRUE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            System.setProperty("jdk.tls.stapling.cacheLifetime", "");
            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    /**
     * Creates the PKI components necessary for this test, including
     * Root CA, Intermediate CA and SSL server certificates, the keystores
     * for each entity, a client trust store, and starts the OCSP responders.
     */
    private static void createPKI() throws Exception {
        CertificateBuilder cbld = new CertificateBuilder();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyStore.Builder keyStoreBuilder =
                KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(passwd.toCharArray()));

        KeyPair rootCaKP = keyGen.genKeyPair();
        log("Generated Root CA KeyPair");
        KeyPair intCaKP = keyGen.genKeyPair();
        log("Generated Intermediate CA KeyPair");
        KeyPair sslKP = keyGen.genKeyPair();
        log("Generated SSL Cert KeyPair");

        cbld.setSubjectName("CN=Root CA Cert, O=SomeCompany");
        cbld.setPublicKey(rootCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("1"));
        long start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60);
        long end = start + TimeUnit.DAYS.toMillis(1085);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, rootCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        X509Certificate rootCert = cbld.build(null, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Root CA Created:\n" + certInfo(rootCert));

        rootKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] rootChain = {rootCert};
        rootKeystore.setKeyEntry(ROOT_ALIAS, rootCaKP.getPrivate(),
                passwd.toCharArray(), rootChain);

        rootOcsp = new SimpleOCSPServer(rootKeystore, passwd, ROOT_ALIAS, null);
        rootOcsp.enableLog(ocspDebug);
        rootOcsp.setNextUpdateInterval(3600);
        rootOcsp.start();

        boolean readyStatus = rootOcsp.awaitServerReady(5, TimeUnit.SECONDS);
        if (!readyStatus) {
            throw new RuntimeException("Server not ready");
        }

        rootOcspPort = rootOcsp.getPort();
        String rootRespURI = "http:
        log("Root OCSP Responder URI is " + rootRespURI);

        cbld.reset();
        cbld.setSubjectName("CN=Intermediate CA Cert, O=SomeCompany");
        cbld.setPublicKey(intCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("100"));
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        end = start + TimeUnit.DAYS.toMillis(730);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, intCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        cbld.addAIAExt(Collections.singletonList(rootRespURI));
        X509Certificate intCaCert = cbld.build(rootCert, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Intermediate CA Created:\n" + certInfo(intCaCert));

        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
            new HashMap<>();
        revInfo.put(intCaCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        rootOcsp.updateStatusDb(revInfo);

        intKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] intChain = {intCaCert, rootCert};
        intKeystore.setKeyEntry(INT_ALIAS, intCaKP.getPrivate(),
                passwd.toCharArray(), intChain);
        intKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        intOcsp = new SimpleOCSPServer(intKeystore, passwd,
                INT_ALIAS, null);
        intOcsp.enableLog(ocspDebug);
        intOcsp.setNextUpdateInterval(3600);
        intOcsp.start();

        readyStatus = intOcsp.awaitServerReady(5, TimeUnit.SECONDS);
        if (!readyStatus) {
            throw new RuntimeException("Server not ready");
        }

        intOcspPort = intOcsp.getPort();
        String intCaRespURI = "http:
        log("Intermediate CA OCSP Responder URI is " + intCaRespURI);

        cbld.reset();
        cbld.setSubjectName("CN=SSLCertificate, O=SomeCompany");
        cbld.setPublicKey(sslKP.getPublic());
        cbld.setSerialNumber(new BigInteger("4096"));
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        end = start + TimeUnit.DAYS.toMillis(365);
        cbld.setValidity(new Date(start), new Date(end));

        addCommonExts(cbld, sslKP.getPublic(), intCaKP.getPublic());
        boolean[] kuBits = {true, false, true, false, false, false,
            false, false, false};
        cbld.addKeyUsageExt(kuBits);
        List<String> ekuOids = new ArrayList<>();
        ekuOids.add("1.3.6.1.5.5.7.3.1");
        ekuOids.add("1.3.6.1.5.5.7.3.2");
        cbld.addExtendedKeyUsageExt(ekuOids);
        cbld.addSubjectAltNameDNSExt(Collections.singletonList("localhost"));
        cbld.addAIAExt(Collections.singletonList(intCaRespURI));
        X509Certificate sslCert = cbld.build(intCaCert, intCaKP.getPrivate(),
                "SHA256withRSA");
        log("SSL Certificate Created:\n" + certInfo(sslCert));

        revInfo = new HashMap<>();
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        intOcsp.updateStatusDb(revInfo);

        serverKeystore = keyStoreBuilder.getKeyStore();
        Certificate[] sslChain = {sslCert, intCaCert, rootCert};
        serverKeystore.setKeyEntry(SSL_ALIAS, sslKP.getPrivate(),
                passwd.toCharArray(), sslChain);
        serverKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        trustStore = keyStoreBuilder.getKeyStore();
        trustStore.setCertificateEntry(ROOT_ALIAS, rootCert);
    }

    private static void addCommonExts(CertificateBuilder cbld,
            PublicKey subjKey, PublicKey authKey) throws IOException {
        cbld.addSubjectKeyIdExt(subjKey);
        cbld.addAuthorityKeyIdExt(authKey);
    }

    private static void addCommonCAExts(CertificateBuilder cbld)
            throws IOException {
        cbld.addBasicConstraintsExt(true, true, -1);
        boolean[] kuBitSettings = {true, false, false, false, false, true,
            true, false, false};
        cbld.addKeyUsageExt(kuBitSettings);
    }

    private static int responseCacheSize(
            StatusResponseManager srm) throws IllegalAccessException {
        return ((sun.security.util.Cache)responseCacheField.get(srm)).size();
    }

    private static void clearResponseCache(
            StatusResponseManager srm) throws IllegalAccessException {
        ((sun.security.util.Cache)responseCacheField.get(srm)).clear();
    }

    /**
     * Helper routine that dumps only a few cert fields rather than
     * the whole toString() output.
     *
     * @param cert An X509Certificate to be displayed
     *
     * @return The {@link String} output of the issuer, subject and
     * serial number
     */
    private static String certInfo(X509Certificate cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issuer: ").append(cert.getIssuerX500Principal()).
                append("\n");
        sb.append("Subject: ").append(cert.getSubjectX500Principal()).
                append("\n");
        sb.append("Serial: ").append(cert.getSerialNumber()).append("\n");
        return sb.toString();
    }

    /**
     * Log a message on stdout
     *
     * @param message The message to log
     */
    private static void log(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    public static void runTests(Map<String, TestCase> testList) {
        int testNo = 0;
        int numberFailed = 0;
        Map.Entry<Boolean, String> result;

        System.out.println("============ Tests ============");
        for (String testName : testList.keySet()) {
            System.out.println("Test " + ++testNo + ": " + testName);
            result = testList.get(testName).runTest();
            System.out.print("Result: " + (result.getKey() ? "PASS" : "FAIL"));
            System.out.println(" " +
                    (result.getValue() != null ? result.getValue() : ""));
            System.out.println("-------------------------------------------");
            if (!result.getKey()) {
                numberFailed++;
            }
        }

        System.out.println("End Results: " + (testList.size() - numberFailed) +
                " Passed" + ", " + numberFailed + " Failed.");
        if (numberFailed > 0) {
            throw new RuntimeException(
                    "One or more tests failed, see test output for details");
        }
    }

    public interface TestCase {
        Map.Entry<Boolean, String> runTest();
    }
}
