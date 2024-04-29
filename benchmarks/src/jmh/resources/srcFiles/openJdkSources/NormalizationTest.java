/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package catalog;

import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.ResolutionChecker.checkPubIdResolution;
import static catalog.ResolutionChecker.checkSysIdResolution;
import static catalog.ResolutionChecker.checkUriResolution;

import javax.xml.catalog.CatalogResolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8077931
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.NormalizationTest
 * @run testng/othervm catalog.NormalizationTest
 * @summary Before matching identifiers and URI references, it has to normalize
 *          the passed identifiers and URI references. And then the catalog
 *          resolver uses the normalized stuff to search the counterparts in
 *          catalog files.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NormalizationTest {

    private static final String CATALOG_NORMALIZATION = "normalization.xml";

    @Test(dataProvider = "systemId_uri-matchedUri")
    public void testNormalizationOnSysId(String sytemId, String matchedUri) {
        checkSysIdResolution(createEntityResolver(), sytemId, matchedUri);
    }

    @Test(dataProvider = "publicId-matchedUri")
    public void testNormalizationOnPubId(String publicId, String matchedUri) {
        checkPubIdResolution(createEntityResolver(), publicId, matchedUri);
    }

    @Test(dataProvider = "systemId_uri-matchedUri")
    public void testNormalizationOnUri(String uri, String matchedUri) {
        checkUriResolution(createUriResolver(), uri, matchedUri);
    }

    @DataProvider(name = "systemId_uri-matchedUri")
    public Object[][] dataOnSysIdAndUri() {
        return new Object[][] {
                { "  http:
                        "http:

                { "http:
                        "http:

                { "http:
                        "http:

                { "http:
                        "http:
    }

    @DataProvider(name = "publicId-matchedUri")
    public Object[][] dataOnPubId() {
        return new Object[][] {
                { "   -
                        "http:

                { "-
                        "http:

                { "  -
                        "http:
    }

    private CatalogResolver createEntityResolver() {
        return catalogResolver(CATALOG_NORMALIZATION);
    }

    private CatalogResolver createUriResolver() {
        return catalogUriResolver(CATALOG_NORMALIZATION);
    }
}
