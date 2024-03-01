/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package xpath;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

/*
 * @test
 * @bug     8289949
 * @summary Tests the XPath operator expressions
 * @library /javax/xml/jaxp/unittest
 *
 * @run testng xpath.XPathOperatorExpTest
 */
public class XPathOperatorExpTest extends XPathTestBase {
    private static final Document doc = getDocument();

    /*
     * DataProvider for testing the XPath operator expressions.
     * Data columns:
     *  see parameters of the test "testOperatorExp"
     */
    @DataProvider(name = "operatorExpTestCases")
    public Object[][] getOperatorExp() {
        return new Object[][]{
                {"string(
                {"string(
                {"string(
                {"count(
                {"count(
                {"count(
                {"count(

                {"string(
                {"string(
                {"string(
                {"string(
                {"string(

                {"count(
                        2},
                {"count(
                        " 

                {"1 + 2 * 3 + 3", 10.0},
                {"1 + 1 div 2 + 2", 3.5},
                {"1 + 1 mod 2 + 2", 4.0},
                {"1 * 1 mod 2 div 2", 0},
                {"1 * (1 mod 2) div 2", 0.5},
                {"(1 + 2) * (3 + 3)", 18.0},
                {"(1 + 2) div (3 + 3)", 0.5},
                {"1 - 2 < 3 + 3", true},
                {"1 * 2 >= 3 div 3", true},
                {"3 > 2 > 1", false},
                {"3 > (2 > 1)", true},
                {"3 > 2 = 1", true},
                {"1 = 3 > 2", true},
                {"1 = 2 or 1 <= 2 and 2 != 2", false},
        };
    }

    /*
     * DataProvider for testing XPathExpressionException being thrown on
     * invalid operator usage.
     * Data columns:
     *  see parameters of the test "testExceptionOnEval"
     */
    @DataProvider(name = "exceptionExpTestCases")
    public Object[][] getExceptionExp() {
        return new Object[][]{
                {"string(
                {"string(
                {"count(
                {"count(
                {"count(

                {"
        };
    }

    /**
     * Verifies that the result of evaluating XPath operators matches the
     * expected result.
     *
     * @param exp      XPath expression
     * @param expected expected result
     * @throws Exception if test fails
     */
    @Test(dataProvider = "operatorExpTestCases")
    void testOperatorExp(String exp, Object expected) throws Exception {
        if (expected instanceof Double d) {
            testExp(doc, exp, d, Double.class);
        } else if (expected instanceof String s) {
            testExp(doc, exp, s, String.class);
        } else if (expected instanceof  Boolean b) {
            testExp(doc, exp, b, Boolean.class);
        }
    }

    /**
     * Verifies that XPathExpressionException is thrown on xpath evaluation.
     *
     * @param exp XPath expression
     */
    @Test(dataProvider = "exceptionExpTestCases")
    void testExceptionOnEval(String exp) {
        Assert.assertThrows(XPathExpressionException.class, () -> testEval(doc,
                exp));
    }
}
