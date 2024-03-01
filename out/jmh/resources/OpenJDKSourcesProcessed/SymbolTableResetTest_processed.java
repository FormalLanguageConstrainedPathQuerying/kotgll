/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package sax;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 8173390 8176168
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -Djdk.xml.resetSymbolTable=false sax.SymbolTableResetTest
 * @run testng/othervm -Djdk.xml.resetSymbolTable=true sax.SymbolTableResetTest
 * @run testng/othervm -Djdk.xml.resetSymbolTable=false -DrunSecMngr=true -Djava.security.manager=allow sax.SymbolTableResetTest
 * @run testng/othervm -Djdk.xml.resetSymbolTable=true -DrunSecMngr=true -Djava.security.manager=allow sax.SymbolTableResetTest
 * @summary Test that SAXParser reallocates symbol table during
 *          subsequent parse operations
 */
@Listeners({jaxp.library.BasePolicy.class})
public class SymbolTableResetTest {

    /*
     * Test verifies the following use cases when the parser feature is not set:
     *  a) Reset symbol table is requested via the system property
     *  b) Reset symbol table is not requested via the system property
     *     and therefore the default value should be used - reset
     *     operation should not occur.
     */
    @Test
    public void testNoFeatureSet() throws Exception {
        parseAndCheckReset(false, false);
    }


    /*
     * Test that when symbol table reset is requested through parser
     * feature it is not affected by the system property value
     */
    @Test
    public void testResetEnabled() throws Exception {
        parseAndCheckReset(true, true);
    }

    /*
     * Test that when symbol table reset is disabled through parser
     * feature it is not affected by the system property value
     */
    @Test
    public void testResetDisabled() throws Exception {
        parseAndCheckReset(true, false);
    }

    /*
     * Test mimics the SAXParser usage in SAAJ-RI that reuses the
     * parsers from the internal pool. To avoid memory leaks, symbol
     * table associated with the parser should be reallocated during each
     * parse() operation.
     */
    private void parseAndCheckReset(boolean setFeature, boolean value) throws Exception {
        boolean resetExpected = setFeature && value;
        boolean spSet = runWithAllPerm(() -> System.getProperty(RESET_FEATURE)) != null;
        String input = "<dummy>Test</dummy>";

        if (!setFeature && spSet) {
            resetExpected = runWithAllPerm(() -> Boolean.getBoolean(RESET_FEATURE));
        }

        SAXParserFactory spf = SAXParserFactory.newInstance();
        if (setFeature) {
            spf.setFeature(RESET_FEATURE, value);
        }
        SAXParser p = spf.newSAXParser();

        p.parse(new InputSource(new StringReader(input)), new DefaultHandler());
        Object symTable1 = p.getProperty(SYMBOL_TABLE_PROPERTY);

        p.reset();

        p.parse(new InputSource(new StringReader(input)), new DefaultHandler());
        Object symTable2 = p.getProperty(SYMBOL_TABLE_PROPERTY);

        if (resetExpected) {
            Assert.assertNotSame(symTable1, symTable2, "Symbol table references");
        } else {
            Assert.assertSame(symTable1, symTable2, "Symbol table references");
        }
    }

    private static final String RESET_FEATURE = "jdk.xml.resetSymbolTable";

    private static final String SYMBOL_TABLE_PROPERTY = "http:
}
