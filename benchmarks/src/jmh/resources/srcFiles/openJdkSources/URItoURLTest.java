/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  4768755 4677045 8147462
 * @summary URL.equal(URL) is inconsistent for opaque URI.toURL()
 *              and new URL(URI.toString)
 *          URI.toURL() does not always work as specified
 *          Ensure URIs representing invalid/malformed URLs throw similar
 *              exception with new URL(URI.toString()) and URI.toURL()
 */

import java.net.*;
import java.util.Objects;

public class URItoURLTest {

    public static void main(String args[]) throws Exception {

        URL classUrl = new URL("jrt:/java.base/java/lang/Object.class");

        String[] uris = {
                        "mailto:xyz@abc.de",
                        "file:xyz#ab",
                        "http:abc/xyz/pqr",
                        "http:abc/xyz/pqr?id=x%0a&ca=true",
                        "file:/C:/v700/dev/unitTesting/tests/apiUtil/uri",
                        "http:
                        "file:/C:/v700/dev/unitTesting/tests/apiUtil/uri",
                        "file:/C:/v700/dev%20src/unitTesting/tests/apiUtil/uri",
                        "file:/C:/v700/dev%20src/./unitTesting/./tests/apiUtil/uri",
                        "http:
                        "file:./test/./x",
                        "file:./././%20#i=3",
                        "file:?hmm",
                        "file:.#hmm",
                        classUrl.toExternalForm(),
                        };

        String[] malformedUrls = {
                        "test:/test",
                        "fiel:test",
                        };

        String[] illegalUris = {
                        "./test",
                        "/test",
                        };

        boolean isTestFailed = false;
        boolean isURLFailed = false;

        for (String uriString : uris) {
            URI uri = URI.create(uriString);

            URL url1 = new URL(uri.toString());
            URL url2 = uri.toURL();
            System.out.println("Testing URI " + uri);

            if (!url1.equals(url2)) {
                System.out.println("equals() FAILED");
                isURLFailed = true;
            }
            if (url1.hashCode() != url2.hashCode()) {
                System.out.println("hashCode() DIDN'T MATCH");
                isURLFailed = true;
            }
            if (!url1.sameFile(url2)) {
                System.out.println("sameFile() FAILED");
                isURLFailed = true;
            }

            if (!equalsComponents("getPath()", url1.getPath(),
                                            url2.getPath())) {
                isURLFailed = true;
            }
            if (!equalsComponents("getFile()", url1.getFile(),
                                            url2.getFile())) {
                isURLFailed = true;
            }
            if (!equalsComponents("getHost()", url1.getHost(),
                                            url2.getHost())) {
                isURLFailed = true;
            }
            if (!equalsComponents("getAuthority()",
                                url1.getAuthority(), url2.getAuthority())) {
                isURLFailed = true;
            }
            if (!equalsComponents("getRef()", url1.getRef(),
                                            url2.getRef())) {
                isURLFailed = true;
            }
            if (!equalsComponents("getUserInfo()", url1.getUserInfo(),
                                            url2.getUserInfo())) {
                isURLFailed = true;
            }
            if (!equalsComponents("toString()", url1.toString(),
                                            url2.toString())) {
                isURLFailed = true;
            }

            if (isURLFailed) {
                isTestFailed = true;
            } else {
                System.out.println("PASSED ..");
            }
            System.out.println();
            isURLFailed = false;
        }
        for (String malformedUrl : malformedUrls) {
            Exception toURLEx = null;
            Exception newURLEx = null;
            try {
                new URI(malformedUrl).toURL();
            } catch (Exception e) {
                toURLEx = e;
            }
            try {
                new URL(new URI(malformedUrl).toString());
            } catch (Exception e) {
                newURLEx = e;
            }
            if (!(toURLEx instanceof MalformedURLException) ||
                    !(newURLEx instanceof MalformedURLException) ||
                    !toURLEx.getMessage().equals(newURLEx.getMessage())) {
                isTestFailed = true;
                System.out.println("Expected the same MalformedURLException: " +
                    newURLEx + " vs " + toURLEx);
            }
        }
        for (String illegalUri : illegalUris) {
             try {
                 new URI(illegalUri).toURL();
             } catch (IllegalArgumentException e) {
             }

             try {
                 new URL(illegalUri);
             } catch (MalformedURLException e) {
             }
        }
        if (isTestFailed) {
            throw new Exception("URI.toURL() test failed");
        }
    }

    static boolean equalsComponents(String method, String comp1, String comp2) {
        if ((comp1 != null) && (!comp1.equals(comp2))) {
            System.out.println(method + " DIDN'T MATCH" +
                        "  ===>");
                System.out.println("    URL(URI.toString()) returns:" + comp1);
                System.out.println("    URI.toURL() returns:" + comp2);
                return false;
        }
        return true;
    }
}
