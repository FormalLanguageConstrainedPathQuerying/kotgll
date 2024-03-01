/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=actalisauthenticationrootca
 * @bug 8189131
 * @summary Interoperability tests with Actalis CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp
 *  CAInterop actalisauthenticationrootca OCSP
 * @run main/othervm/timeout=180 -Djava.security.debug=certpath,ocsp
 *  CAInterop actalisauthenticationrootca CRL
 */

/*
 * @test id=amazonrootca1
 * @bug 8233223
 * @summary Interoperability tests with Amazon's CA1
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop amazonrootca1 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop amazonrootca1 CRL
 */

/*
 * @test id=amazonrootca2
 * @bug 8233223
 * @summary Interoperability tests with Amazon's CA2
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop amazonrootca2 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop amazonrootca2 CRL
 */

/*
 * @test id=amazonrootca3
 * @bug 8233223
 * @summary Interoperability tests with Amazon's CA3
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop amazonrootca3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop amazonrootca3 CRL
 */

/*
 * @test id=amazonrootca4
 * @bug 8233223
 * @summary Interoperability tests with Amazon's CA4
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop amazonrootca4 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop amazonrootca4 CRL
 */

/*
 * @test id=buypassclass2ca
 * @bug 8189131
 * @summary Interoperability tests with Buypass Class 2 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop buypassclass2ca OCSP
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop buypassclass2ca CRL
 */

/*
 * @test id=buypassclass3ca
 * @bug 8189131
 * @summary Interoperability tests with Buypass Class 3 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop buypassclass3ca OCSP
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop buypassclass3ca CRL
 */

/*
 * @test id=comodorsaca
 * @bug 8189131
 * @summary Interoperability tests with Comodo RSA CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop comodorsaca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop comodorsaca CRL
 */

/*
 * @test id=comodoeccca
 * @bug 8189131
 * @summary Interoperability tests with Comodo ECC CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop comodoeccca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop comodoeccca CRL
 */

/*
 * @test id=usertrustrsaca
 * @bug 8189131
 * @summary Interoperability tests with Comodo userTrust RSA CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop usertrustrsaca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop usertrustrsaca CRL
 */

/*
 * @test id=usertrusteccca
 * @bug 8189131
 * @summary Interoperability tests with Comodo userTrust ECC CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop usertrusteccca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop usertrusteccca CRL
 */

/*
 * @test id=letsencryptisrgx1
 * @bug 8189131
 * @summary Interoperability tests with Let's Encrypt ISRG Root X1 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop letsencryptisrgx1 DEFAULT
 */

/*
 * @test id=letsencryptisrgx2
 * @bug 8317374
 * @summary Interoperability tests with Let's Encrypt ISRG Root X2 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop letsencryptisrgx2 DEFAULT
 */

/*
 * @test id=globalsignrootcar6
 * @bug 8216577
 * @summary Interoperability tests with GlobalSign R6 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop globalsignrootcar6 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop globalsignrootcar6 CRL
 */

/*
 * @test id=entrustrootcaec1
 * @bug 8195774
 * @summary Interoperability tests with Entrust CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop entrustrootcaec1 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop entrustrootcaec1 CRL
 */

/*
 * @test id=entrustrootcag4
 * @bug 8243321
 * @summary Interoperability tests with Entrust CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop entrustrootcag4 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop entrustrootcag4 CRL
 */

/*
 * @test id=godaddyrootg2ca
 * @bug 8196141
 * @summary Interoperability tests with GoDaddy CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop godaddyrootg2ca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop godaddyrootg2ca CRL
 */

/*
 * @test id=starfieldrootg2ca
 * @bug 8196141
 * @summary Interoperability tests with Starfield CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop starfieldrootg2ca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop starfieldrootg2ca CRL
 */

/*
 * @test id=globalsigneccrootcar4
 * @bug 8307134
 * @summary Interoperability tests with Google's GlobalSign R4 and GTS Root certificates
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop globalsigneccrootcar4 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop globalsigneccrootcar4 CRL
 */

/*
 * @test id=gtsrootcar1
 * @bug 8307134
 * @summary Interoperability tests with Google's GlobalSign R4 and GTS Root certificates
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop gtsrootcar1 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop gtsrootcar1 CRL
 */

/*
 * @test id=gtsrootcar2
 * @bug 8307134
 * @summary Interoperability tests with Google's GlobalSign R4 and GTS Root certificates
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop gtsrootcar2 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop gtsrootcar2 CRL
 */

/*
 * @test id=gtsrootecccar3
 * @bug 8307134
 * @summary Interoperability tests with Google's GlobalSign R4 and GTS Root certificates
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop gtsrootecccar3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop gtsrootecccar3 CRL
 */

/*
 * @test id=gtsrootecccar4
 * @bug 8307134
 * @summary Interoperability tests with Google's GlobalSign R4 and GTS Root certificates
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop gtsrootecccar4 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop gtsrootecccar4 CRL
 */

/*
 * @test id=microsoftecc2017
 * @bug 8304760
 * @summary Interoperability tests with Microsoft TLS root CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop microsoftecc2017 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop microsoftecc2017 CRL
 */

/*
 * @test id=microsoftrsa2017
 * @bug 8304760
 * @summary Interoperability tests with Microsoft TLS root CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop microsoftrsa2017 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop microsoftrsa2017 CRL
 */

/*
 * @test id=quovadisrootca1g3
 * @bug 8189131
 * @summary Interoperability tests with QuoVadis Root CA1 G3 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop quovadisrootca1g3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop quovadisrootca1g3 CRL
 */

/*
 * @test id=quovadisrootca2g3
 * @bug 8189131
 * @summary Interoperability tests with QuoVadis Root CA2 G3 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop quovadisrootca2g3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop quovadisrootca2g3 CRL
 */

/*
 * @test id=quovadisrootca3g3
 * @bug 8189131
 * @summary Interoperability tests with QuoVadis Root CA3 G3 CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop quovadisrootca3g3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop quovadisrootca3g3 CRL
 */

/*
 * @test id=digicerttlseccrootg5
 * @bug 8318759
 * @summary Interoperability tests with DigiCert TLS ECC P384 Root G5
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop digicerttlseccrootg5 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop digicerttlseccrootg5 CRL
 */

/*
 * @test id=digicerttlsrsarootg5
 * @bug 8318759
 * @summary Interoperability tests with DigiCert TLS RSA4096 Root G5
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop digicerttlsrsarootg5 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop digicerttlsrsarootg5 CRL
 */

/*
 * @test id=sslrootrsaca
 * @bug 8243320
 * @summary Interoperability tests with SSL.com's RSA CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop sslrootrsaca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop sslrootrsaca CRL
 */

/*
 * @test id=sslrootevrsaca
 * @bug 8243320
 * @summary Interoperability tests with SSL.com's EV RSA CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop sslrootevrsaca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop sslrootevrsaca CRL
 */

/*
 * @test id=sslrooteccca
 * @bug 8243320
 * @summary Interoperability tests with SSL.com's ECC CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop sslrooteccca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop sslrooteccca CRL
 */

/*
 * @test id=teliasonerarootcav1
 * @bug 8210432
 * @summary Interoperability tests with TeliaSonera Root CA v1
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop teliasonerarootcav1 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop teliasonerarootcav1 CRL
 */

/*
 * @test id=twcaglobalrootca
 * @bug 8305975
 * @summary Interoperability tests with TWCA Global Root CA from TAIWAN-CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop twcaglobalrootca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop twcaglobalrootca CRL
 */

/*
 * @test id=certignarootca
 * @bug 8314960
 * @summary Interoperability tests with Certigna Root CAs from Dhimyotis
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop certignarootca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop certignarootca CRL
 */

/*
 * @test id=affirmtrustcommercialca
 * @bug 8040012
 * @summary Interoperability tests with AffirmTrust Commercial CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop affirmtrustcommercialca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop affirmtrustcommercialca CRL
 */

/*
 * @test id=affirmtrustnetworkingca
 * @bug 8040012
 * @summary Interoperability tests with AffirmTrust Networking CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop affirmtrustnetworkingca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop affirmtrustnetworkingca CRL
 */

/*
 * @test id=affirmtrustpremiumca
 * @bug 8040012
 * @summary Interoperability tests with AffirmTrust Premium CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop affirmtrustpremiumca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop affirmtrustpremiumca CRL
 */

/*
 * @test id=affirmtrustpremiumeccca
 * @bug 8040012
 * @summary Interoperability tests with AffirmTrust Premium ECC CA
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop affirmtrustpremiumeccca OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop affirmtrustpremiumeccca CRL
 */

/*
 * @test id=teliarootcav2
 * @bug 8317373
 * @summary Interoperability tests with Telia Root CA V2
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop teliarootcav2 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop teliarootcav2 CRL
 */

/*
 * @test id=emsignrootcag1
 * @bug 8319187
 * @summary Interoperability tests with eMudhra Root CA G1
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop emsignrootcag1 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop emsignrootcag1 CRL
 */

/*
 * @test id=emsigneccrootcag3
 * @bug 8319187
 * @summary Interoperability tests with eMudhra ECC Root CA G3
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath,ocsp CAInterop emsigneccrootcag3 OCSP
 * @run main/othervm -Djava.security.debug=certpath CAInterop emsigneccrootcag3 CRL
 */

/**
 * Collection of certificate validation tests for interoperability with external CAs
 */
public class CAInterop {

    /**
     * Returns the test configuration for CA
     *
     * @param alias from the cacerts file without [jdk]
     * @return CATestURLs
     */
    private CATestURLs getTestURLs(String alias) {
        return switch (alias) {
            case "actalisauthenticationrootca" ->
                    new CATestURLs("https:
                            "https:

            case "amazonrootca1" ->
                    new CATestURLs("https:
                    "https:
            case "amazonrootca2" ->
                    new CATestURLs("https:
                    "https:
            case "amazonrootca3" ->
                    new CATestURLs("https:
                    "https:
            case "amazonrootca4" ->
                    new CATestURLs("https:
                    "https:

            case "buypassclass2ca" ->
                    new CATestURLs("https:
                    "https:
            case "buypassclass3ca" ->
                    new CATestURLs("https:
                    "https:

            case "comodorsaca" ->
                    new CATestURLs("https:
                    "https:
            case "comodoeccca" ->
                    new CATestURLs("https:
                    "https:
            case "usertrustrsaca" ->
                    new CATestURLs("https:
                    "https:
            case "usertrusteccca" ->
                    new CATestURLs("https:
                    "https:

            case "letsencryptisrgx1" ->
                    new CATestURLs("https:
                            "https:
            case "letsencryptisrgx2" ->
                    new CATestURLs("https:
                            "https:

            case "globalsignrootcar6" ->
                    new CATestURLs("https:
                            "https:

            case "entrustrootcaec1" ->
                    new CATestURLs("https:
                            "https:
            case "entrustrootcag4" ->
                    new CATestURLs("https:
                            "https:

            case "godaddyrootg2ca" ->
                    new CATestURLs("https:
                    "https:
            case "starfieldrootg2ca" ->
                    new CATestURLs("https:
                    "https:

            case "globalsigneccrootcar4" ->
                    new CATestURLs("https:
                    "https:
            case "gtsrootcar1" ->
                    new CATestURLs("https:
                    "https:
            case "gtsrootcar2" ->
                    new CATestURLs("https:
                    "https:
            case "gtsrootecccar3" ->
                    new CATestURLs("https:
                    "https:
            case "gtsrootecccar4" ->
                    new CATestURLs("https:
                    "https:

            case "microsoftecc2017" ->
                    new CATestURLs("https:
                    "https:
            case "microsoftrsa2017" ->
                    new CATestURLs("https:
                    "https:

            case "quovadisrootca1g3" ->
                    new CATestURLs("https:
                    "https:
            case "quovadisrootca2g3" ->
                    new CATestURLs("https:
                    "https:
            case "quovadisrootca3g3" ->
                    new CATestURLs("https:
                    "https:
            case "digicerttlseccrootg5" ->
                    new CATestURLs("https:
                            "https:
            case "digicerttlsrsarootg5" ->
                    new CATestURLs("https:
                            "https:

            case "sslrootrsaca" ->
                    new CATestURLs("https:
                    "https:
            case "sslrootevrsaca" ->
                    new CATestURLs("https:
                    "https:
            case "sslrooteccca" ->
                    new CATestURLs("https:
                    "https:

            case "teliasonerarootcav1" ->
                    new CATestURLs("https:
                            "https:

            case "twcaglobalrootca" ->
                    new CATestURLs("https:
                            "https:

            case "certignarootca" ->
                    new CATestURLs("https:
                            "https:

            case "affirmtrustcommercialca" ->
                    new CATestURLs("https:
                            "https:
            case "affirmtrustnetworkingca" ->
                    new CATestURLs("https:
                            "https:
            case "affirmtrustpremiumca" ->
                    new CATestURLs("https:
                            "https:
            case "affirmtrustpremiumeccca" ->
                    new CATestURLs("https:
                            "https:

            case "teliarootcav2" ->
                    new CATestURLs("https:
                            "https:

            case "emsignrootcag1" ->
                    new CATestURLs("https:
                            "https:
            case "emsigneccrootcag3" ->
                    new CATestURLs("https:
                            "https:

            default -> throw new RuntimeException("No test setup found for: " + alias);
        };
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new RuntimeException("Run as: CAInterop <alias> <OCSP/CRL/DEFAULT>");
        }

        String caAlias = args[0];

        CAInterop caInterop = new CAInterop(args[1]);
        CATestURLs caTestURLs = caInterop.getTestURLs(caAlias);

        caInterop.validate(caAlias + " [jdk]",
                caTestURLs.getVALID_URL(),
                caTestURLs.getREVOKED_URL());
    }

    static class CATestURLs {
        final String VALID_URL;
        final String REVOKED_URL;

        public CATestURLs(String validURL,
                           String revokedURL) {
            VALID_URL = validURL;
            REVOKED_URL = revokedURL;
        }

        public String getVALID_URL() {
            return VALID_URL;
        }

        public String getREVOKED_URL() {
            return REVOKED_URL;
        }
    }

    /**
     * Constructor for interoperability test with third party CA.
     *
     * @param revocationMode revocation checking mode to use
     */
    public CAInterop(String revocationMode) {
        if ("CRL".equalsIgnoreCase(revocationMode)) {
            ValidatePathWithURL.enableCRLOnly();
        } else if ("OCSP".equalsIgnoreCase(revocationMode)) {
            ValidatePathWithURL.enableOCSPOnly();
        } else {
            ValidatePathWithURL.enableOCSPAndCRL();
        }

        ValidatePathWithURL.logRevocationSettings();
    }

    /**
     * Validates provided URLs using <code>HttpsURLConnection</code> making sure they
     * anchor to the root CA found in <code>cacerts</code> using provided alias.
     *
     * @param caAlias        CA alis from <code>cacerts</code> file
     * @param validCertURL   valid test URL
     * @param revokedCertURL revoked test URL
     * @throws Exception thrown when certificate can't be validated as valid or revoked
     */
    public void validate(String caAlias,
                         String validCertURL,
                         String revokedCertURL) throws Exception {

        ValidatePathWithURL validatePathWithURL = new ValidatePathWithURL(caAlias);

        if (validCertURL != null) {
            validatePathWithURL.validateDomain(validCertURL, false);
        }

        if (revokedCertURL != null) {
            validatePathWithURL.validateDomain(revokedCertURL, true);
        }
    }
}
