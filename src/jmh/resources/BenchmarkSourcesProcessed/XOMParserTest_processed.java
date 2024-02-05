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

/**
 * @test
 * @bug 8022548
 * @summary test that a parser can use DTDConfiguration
 * @modules java.xml/com.sun.org.apache.xerces.internal.impl
 *          java.xml/com.sun.org.apache.xerces.internal.parsers
 *          java.xml/com.sun.org.apache.xerces.internal.util
 *          java.xml/com.sun.org.apache.xerces.internal.xni.parser
 * @run main XOMParserTest
 */
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.parsers.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;

/**
 * <p>Test {@link javax.xml.transform.Transformer} for JDK-8022548: SPECJVM2008
 * has errors introduced in 7u40-b34
 *
 * Test XOM is supported after jaxp 1.5 </p>
 *
 * @author Joe Wang <huizhe.wang@oracle.com>
 *
 */
public class XOMParserTest extends TestBase {

    public XOMParserTest(String name) {
        super(name);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        XOMParserTest test = new XOMParserTest("XOM parser test");
        test.setUp();
        test.testTransform();
        test.tearDown();
    }

    public final void testTransform() {
        String inFilename = filePath + "/JDK8022548.xml";
        String xslFilename = filePath + "/JDK8022548.xsl";
        String outFilename = "JDK8022548.out";

        try (InputStream xslInput = new FileInputStream(xslFilename);
             InputStream xmlInput = new FileInputStream(inFilename);
             OutputStream out = new FileOutputStream(outFilename);
        ) {


            StringWriter sw = new StringWriter();
            TransformerFactory factory = TransformerFactory.newInstance();

            Templates template = factory.newTemplates(new StreamSource(xslInput));
            Transformer xformer = template.newTransformer();
            Source source = new StreamSource(xmlInput);
            Result result = new StreamResult(outFilename);
            xformer.transform(source, result);

            /**
             * String out = sw.toString(); if (out.indexOf("<p>") < 0 ) {
             * fail(out); }
             */
            String canonicalizedFileName = outFilename + ".canonicalized";
            canonicalize(outFilename, canonicalizedFileName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void canonicalize(String inName, String outName) {
        try (
                FileInputStream inputStream = new FileInputStream(inName);) {
            JDK15XML1_0Parser parser = new JDK15XML1_0Parser();
            parser.parse(new InputSource(inputStream));
            success("test passed");
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    class JDK15XML1_0Parser extends SAXParser {

        JDK15XML1_0Parser() throws org.xml.sax.SAXException {

            super(new DTDConfiguration());
            com.sun.org.apache.xerces.internal.util.SecurityManager manager =
                    new com.sun.org.apache.xerces.internal.util.SecurityManager();
            setProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY, manager);

        }
    }
}
