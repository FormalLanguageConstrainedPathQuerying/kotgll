/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7174244 8234728
 * @summary Test for ciphersuites order
 * @run main/othervm CipherSuitesInOrder
 */
import java.util.*;
import javax.net.ssl.*;

public class CipherSuitesInOrder {

    private final static List<String> supportedCipherSuites
            = Arrays.<String>asList(
                    "TLS_AES_256_GCM_SHA384",
                    "TLS_AES_128_GCM_SHA256",
                    "TLS_CHACHA20_POLY1305_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_RSA_WITH_AES_256_CBC_SHA256",
                    "TLS_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
                    "TLS_DH_anon_WITH_AES_256_GCM_SHA384",
                    "TLS_DH_anon_WITH_AES_128_GCM_SHA256",
                    "TLS_DH_anon_WITH_AES_256_CBC_SHA256",
                    "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_256_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                    "SSL_RSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
                    "SSL_RSA_WITH_RC4_128_MD5",
                    "TLS_ECDH_anon_WITH_RC4_128_SHA",
                    "SSL_DH_anon_WITH_RC4_128_MD5",
                    "SSL_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                    "SSL_DH_anon_WITH_DES_CBC_SHA",
                    "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                    "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                    "TLS_RSA_WITH_NULL_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
                    "TLS_ECDHE_RSA_WITH_NULL_SHA",
                    "SSL_RSA_WITH_NULL_SHA",
                    "TLS_ECDH_ECDSA_WITH_NULL_SHA",
                    "TLS_ECDH_RSA_WITH_NULL_SHA",
                    "TLS_ECDH_anon_WITH_NULL_SHA",
                    "SSL_RSA_WITH_NULL_MD5",
                    "TLS_AES_128_CCM_SHA256",
                    "TLS_AES_128_CCM_8_SHA256"
            );

    private final static String[] protocols = {
        "", "SSL", "TLS", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"
    };

    public static void main(String[] args) throws Exception {
        showSuites(supportedCipherSuites.toArray(new String[0]),
                "All supported cipher suites");

        for (String protocol : protocols) {
            System.out.println("
            System.out.println("
                    + "Testing for SSLContext of " + protocol);
            System.out.println("
            checkForProtocols(protocol);
        }
    }

    public static void checkForProtocols(String protocol) throws Exception {
        SSLContext context;
        if (protocol.isEmpty()) {
            context = SSLContext.getDefault();
        } else {
            context = SSLContext.getInstance(protocol);
            context.init(null, null, null);
        }

        SSLParameters parameters = context.getDefaultSSLParameters();
        checkSuites(parameters.getCipherSuites(),
                "Default cipher suites in SSLContext");

        parameters = context.getSupportedSSLParameters();
        checkSuites(parameters.getCipherSuites(),
                "Supported cipher suites in SSLContext");

        SSLEngine engine = context.createSSLEngine();

        String[] ciphers = engine.getEnabledCipherSuites();
        checkSuites(ciphers,
                "Enabled cipher suites in SSLEngine");

        ciphers = engine.getSupportedCipherSuites();
        checkSuites(ciphers,
                "Supported cipher suites in SSLEngine");

        SSLSocketFactory factory = context.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {

            ciphers = socket.getEnabledCipherSuites();
            checkSuites(ciphers,
                    "Enabled cipher suites in SSLSocket");

            ciphers = socket.getSupportedCipherSuites();
            checkSuites(ciphers,
                    "Supported cipher suites in SSLSocket");
        }

        SSLServerSocketFactory serverFactory = context.getServerSocketFactory();
        try (SSLServerSocket serverSocket
                = (SSLServerSocket) serverFactory.createServerSocket()) {
            ciphers = serverSocket.getEnabledCipherSuites();
            checkSuites(ciphers,
                    "Enabled cipher suites in SSLServerSocket");

            ciphers = serverSocket.getSupportedCipherSuites();
            checkSuites(ciphers,
                    "Supported cipher suites in SSLServerSocket");
        }
    }

    private static void checkSuites(String[] suites, String title) {
        showSuites(suites, title);

        int loc = -1;
        int index = 0;
        for (String suite : suites) {
            index = supportedCipherSuites.indexOf(suite);
            if (index <= loc) {
                throw new RuntimeException(suite + " is not in order");
            }
            loc = index;
        }
    }

    private static void showSuites(String[] suites, String title) {
        System.out.println(title + "[" + suites.length + "]:");
        for (String suite : suites) {
            System.out.println("  " + suite);
        }
    }
}
