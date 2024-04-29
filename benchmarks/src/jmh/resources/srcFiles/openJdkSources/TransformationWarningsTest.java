/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package common;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8144593
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @compile -XDignore.symbol.file TestSAXDriver.java
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.TransformationWarningsTest
 * @run testng/othervm common.TransformationWarningsTest
 * @summary Check that warnings about unsupported properties from parsers
 * are suppressed during the transformation process.
 */
@Listeners({jaxp.library.BasePolicy.class, jaxp.library.InternalAPIPolicy.class})
public class TransformationWarningsTest extends WarningsTestBase {

    @BeforeClass
    public void setup() {
        setSystemProperty("org.xml.sax.driver", "common.TestSAXDriver");
    }

    @Test
    public void testTransformation() throws Exception {
        startTest();
    }

    void doOneTestIteration() throws Exception {
        StringWriter xmlResultString = new StringWriter();
        StreamResult xmlResultStream = new StreamResult(xmlResultString);
        Source src = new StreamSource(new StringReader(xml));
        Transformer t = createTransformer();
        t.transform(src, xmlResultStream);
    }

    Transformer createTransformer() throws Exception {
        Source xslsrc = new StreamSource(new StringReader(xsl));

        TransformerFactory tf;
        synchronized (TransformerFactory.class) {
            tf = TransformerFactory.newInstance();
        }
        Transformer t = tf.newTransformer(xslsrc);

        t.setURIResolver((String href, String base) -> new StreamSource(new StringReader(xml)));
        return t;
    }

    private static final String xsl = "<xsl:stylesheet version='2.0'"
            + " xmlns:xsl='http:
            + " <xsl:output method='xml' indent='yes' omit-xml-declaration='yes'/>"
            + " <xsl:template match='/'>"
            + " <test>Simple Transformation Result. No warnings should be printed to console</test>"
            + " </xsl:template>"
            + "</xsl:stylesheet>";
    private static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
}
