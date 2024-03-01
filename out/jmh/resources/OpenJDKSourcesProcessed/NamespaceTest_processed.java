/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamWriterTest;

import java.io.ByteArrayOutputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.NamespaceTest
 * @run testng/othervm stream.XMLStreamWriterTest.NamespaceTest
 * @summary Test the writing of Namespaces.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NamespaceTest {

    /** debug output? */
    private static final boolean DEBUG = true;

    /** Factory to reuse. */
    XMLOutputFactory xmlOutputFactory = null;

    /** Writer to reuse. */
    XMLStreamWriter xmlStreamWriter = null;

    /** OutputStream to reuse. */
    ByteArrayOutputStream byteArrayOutputStream = null;

    @BeforeMethod
    public void setUp() {

        xmlOutputFactory = XMLOutputFactory.newInstance();
        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);

        byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(byteArrayOutputStream, "utf-8");

        } catch (XMLStreamException xmlStreamException) {
            Assert.fail(xmlStreamException.toString());
        }
    }

    /**
     * Reset Writer for reuse.
     */
    private void resetWriter() {
        try {
            byteArrayOutputStream.reset();
            xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(byteArrayOutputStream, "utf-8");
        } catch (XMLStreamException xmlStreamException) {
            Assert.fail(xmlStreamException.toString());
        }
    }

    @Test
    public void testDoubleXmlNs() {
        try {

            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("foo");
            xmlStreamWriter.writeNamespace("xml", XMLConstants.XML_NS_URI);
            xmlStreamWriter.writeAttribute("xml", XMLConstants.XML_NS_URI, "lang", "ja_JP");
            xmlStreamWriter.writeCharacters("Hello");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndDocument();

            xmlStreamWriter.flush();
            String actualOutput = byteArrayOutputStream.toString();

            if (DEBUG) {
                System.out.println("testDoubleXmlNs(): actualOutput: " + actualOutput);
            }

            Assert.assertTrue(actualOutput.split("xmlns:xml").length == 1, "Expected 0 xmlns:xml, actual output: " + actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDuplicateNamespaceURI() throws Exception {

        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement(new String(""), "localName", new String("nsUri"));
        xmlStreamWriter.writeNamespace(new String(""), new String("nsUri"));
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndDocument();

        xmlStreamWriter.flush();
        String actualOutput = byteArrayOutputStream.toString();

        if (DEBUG) {
            System.out.println("testDuplicateNamespaceURI(): actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);
    }



    private void startDocumentEmptyDefaultNamespace(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {

        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement("root");
        xmlStreamWriter.writeDefaultNamespace("");
    }

    private String endDocumentEmptyDefaultNamespace(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {

        xmlStreamWriter.writeEndDocument();

        xmlStreamWriter.flush();

        return byteArrayOutputStream.toString();
    }

    /**
     * Current default namespace is "".
     * writeStartElement("", "localName"", "")
     * requires no fixup
     */
    @Test
    public void testEmptyDefaultEmptyPrefix() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\">" + "<localName>" + "requires no fixup" + "</localName>" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("", "localName", "");
        xmlStreamWriter.writeCharacters("requires no fixup");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultEmptyPrefix(): actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeStartElement("prefix", "localName", "http:
     *
     * requires no fixup, but should generate a declaration for "prefix":
     * xmlns:prefix="http:
     *
     * necessary to generate a declaration in this test case.
     */
    @Test
    public void testEmptyDefaultSpecifiedPrefix() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\">" + "<prefix:localName xmlns:prefix=\"http:
                + "generate xmlns:prefix" + "</prefix:localName>" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("prefix", "localName", "http:
        xmlStreamWriter.writeCharacters("generate xmlns:prefix");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultSpecifiedPrefix(): actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeStartElement("prefix", "localName", "http:
     *
     * requires no fixup, but should generate a declaration for "prefix":
     * xmlns:prefix="http:
     *
     * not necessary to generate a declaration in this test case.
     */
    @Test
    public void testEmptyDefaultSpecifiedPrefixNoDeclarationGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\"" + " xmlns:prefix=\"http:
                + "not necessary to generate a declaration" + "</prefix:localName>" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("prefix", "http:

        xmlStreamWriter.writeStartElement("prefix", "localName", "http:
        xmlStreamWriter.writeCharacters("not necessary to generate a declaration");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultSpecifiedPrefixNoDeclarationGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultSpecifiedPrefixNoDeclarationGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeStartElement("", "localName", "http:
     *
     * should "fixup" the declaration for the default namespace:
     * xmlns="http:
     */
    @Test
    public void testEmptyDefaultSpecifiedDefault() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\">" + "<localName xmlns=\"http:
                + "</localName>" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("", "localName", "http:
        xmlStreamWriter.writeCharacters("generate xmlns");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultSpecifiedDefault(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultSpecifiedDefault():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeAttribute("", "", "attrName", "value")
     *
     * requires no fixup
     */
    @Test
    public void testEmptyDefaultEmptyPrefixWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\" attrName=\"value\">" + "requires no fixup" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "", "attrName", "value");
        xmlStreamWriter.writeCharacters("requires no fixup");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultEmptyPrefixWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultEmptyPrefixWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeAttribute("p", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * necessary to generate a declaration in this test case.
     */
    @Test
    public void testEmptyDefaultSpecifiedPrefixWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\" xmlns:p=\"http:
                + "generate xmlns:p=\"http:

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("generate xmlns:p=\"http:

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultSpecifiedPrefixWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultSpecifiedPrefixWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeAttribute("p", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * not necessary to generate a declaration in this test case.
     */
    @Test
    public void testEmptyDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\" xmlns:p=\"http:
                + "not necessary to generate a declaration" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("not necessary to generate a declaration");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeAttribute("", "http:
     *
     * XMLOutputFactory (Javadoc) : "If a writer isRepairingNamespaces it will
     * create a namespace declaration on the current StartElement for any
     * attribute that does not currently have a namespace declaration in scope.
     * If the StartElement has a uri but no prefix specified a prefix will be
     * assigned, if the prefix has not been declared in a parent of the current
     * StartElement it will be declared on the current StartElement. If the
     * defaultNamespace is bound and in scope and the default namespace matches
     * the URI of the attribute or StartElement QName no prefix will be
     * assigned."
     *
     * prefix needs to be assigned for this test case.
     */
    @Test
    public void testEmptyDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>"
                + "<root xmlns=\"\" xmlns:{generated prefix}=\"http:
                + "generate xmlns declaration {generated prefix}=\"http:

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "http:
        xmlStreamWriter.writeCharacters("generate xmlns declaration {generated prefix}=\"http:

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultUnspecifiedPrefixWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultUnspecifiedPrefixWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 2, "Expected 1 xmlns:{generated prefix}=\"\", actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":attrName=\"value\"").length == 2, "Expected 1 {generated prefix}:attrName=\"value\", actual output: "
                + actualOutput);
    }

    /**
     * Current default namespace is "".
     *
     * writeAttribute("", "http:
     *
     * XMLOutputFactory (Javadoc) : "If a writer isRepairingNamespaces it will
     * create a namespace declaration on the current StartElement for any
     * attribute that does not currently have a namespace declaration in scope.
     * If the StartElement has a uri but no prefix specified a prefix will be
     * assigned, if the prefix has not been declared in a parent of the current
     * StartElement it will be declared on the current StartElement. If the
     * defaultNamespace is bound and in scope and the default namespace matches
     * the URI of the attribute or StartElement QName no prefix will be
     * assigned."
     *
     * no prefix needs to be assigned for this test case
     */
    @Test
    public void testEmptyDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\" xmlns:p=\"http:
                + "no prefix generation" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:

        xmlStreamWriter.writeAttribute("", "http:
        xmlStreamWriter.writeCharacters("no prefix generation");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }


    private void startDocumentSpecifiedDefaultNamespace(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {

        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement("root");
        xmlStreamWriter.writeDefaultNamespace("http:
    }

    private String endDocumentSpecifiedDefaultNamespace(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {

        xmlStreamWriter.writeEndDocument();

        xmlStreamWriter.flush();

        return byteArrayOutputStream.toString();
    }

    /**
     * Current default namespace is "http:
     *
     * writeElement("", "localName", "")
     *
     * should "fixup" the declaration for the default namespace: xmlns=""
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefix() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "generate xmlns=\"\"" + "</localName>" + "</root>";

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("", "localName", "");
        xmlStreamWriter.writeCharacters("generate xmlns=\"\"");

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefix(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefix():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeStartElement("p", "localName", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * test case where it is necessary to generate a declaration.
     */
    @Test
    public void testSpecifiedDefaultSpecifiedPrefix() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "<p:localName xmlns:p=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("p", "localName", "http:
        xmlStreamWriter.writeCharacters("generate xmlns:p=\"http:

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultSpecifiedPrefix(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultSpecifiedPrefix():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeStartElement("p", "localName", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * test case where it is not necessary to generate a declaration.
     */
    @Test
    public void testSpecifiedDefaultSpecifiedPrefixNoPrefixGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"http:
                + " xmlns:p=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:

        xmlStreamWriter.writeStartElement("p", "localName", "http:
        xmlStreamWriter.writeCharacters("not necessary to generate a declaration");

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultSpecifiedPrefixNoPrefixGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultSpecifiedPrefixNoPrefixGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeStartElement("", "localName", "http:
     *
     * should "fixup" the declaration for the default namespace:
     * xmlns="http:
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURI() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "<localName xmlns=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("", "localName", "http:
        xmlStreamWriter.writeCharacters("generate xmlns=\"http:

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURI(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURI():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("", "", "attrName", "value")
     *
     * requires no fixup
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "</root>";

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "", "attrName", "value");
        xmlStreamWriter.writeCharacters("requires no fixup");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefixWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefixWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("p", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * test case where it is necessary to generate a declaration.
     */
    @Test
    public void testSpecifiedDefaultSpecifiedPrefixWriteAttribute() throws Exception { 

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>"
                + "<root xmlns=\"http:
                + "generate xmlns:p=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("generate xmlns:p=\"http:

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultSpecifiedPrefixWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultSpecifiedPrefixWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("p", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     *
     * test case where it is not necessary to generate a declaration.
     */
    @Test
    public void testSpecifiedDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>"
                + "<root xmlns=\"http:
                + "not necessary to generate a declaration" + "</root>";

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("not necessary to generate a declaration");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultSpecifiedPrefixWriteAttributeNoDeclarationGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("p", "http:
     *
     * requires no fixup, but should generate a declaration for "p":
     * xmlns:p="http:
     * potentially produce two namespace bindings with the same URI, xmlns="xxx"
     * and xmlns:p="xxx", but that's perfectly legal.)
     */
    @Test
    public void testSpecifiedDefaultSpecifiedPrefixSpecifiedNamespaceURIWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "</root>";
        final String EXPECTED_OUTPUT_2 = "<?xml version=\"1.0\" ?>"
                + "<root xmlns=\"http:
                + "</root>";

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("requires no fixup");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultSpecifiedPrefixSpecifiedNamespaceURIWriteAttribute: expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultSpecifiedPrefixSpecifiedNamespaceURIWriteAttribute: expectedOutput: " + EXPECTED_OUTPUT_2);
            System.out.println("testSpecifiedDefaultSpecifiedPrefixSpecifiedNamespaceURIWriteAttribute:   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.equals(EXPECTED_OUTPUT) || actualOutput.equals(EXPECTED_OUTPUT_2), "Expected: " + EXPECTED_OUTPUT + "\n" + "Actual: "
                + actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("", "http:
     *
     * XMLOutputFactory (Javadoc) : "If a writer isRepairingNamespaces it will
     * create a namespace declaration on the current StartElement for any
     * attribute that does not currently have a namespace declaration in scope.
     * If the StartElement has a uri but no prefix specified a prefix will be
     * assigned, if the prefix has not been declared in a parent of the current
     * StartElement it will be declared on the current StartElement. If the
     * defaultNamespace is bound and in scope and the default namespace matches
     * the URI of the attribute or StartElement QName no prefix will be
     * assigned."
     *
     * test case where prefix needs to be assigned.
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"http:
                + " xmlns:{generated prefix}=\"http:
                + "generate xmlns declaration {generated prefix}=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "http:
        xmlStreamWriter.writeCharacters("generate xmlns declaration {generated prefix}=\"http:

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 2, "Expected 1 xmlns:{generated prefix}=\"\", actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":attrName=\"value\"").length == 2, "Expected 1 {generated prefix}:attrName=\"value\", actual output: "
                + actualOutput);
    }

    /**
     * Current default namespace is "http:
     *
     * writeAttribute("", "http:
     *
     * XMLOutputFactory (Javadoc) : "If a writer isRepairingNamespaces it will
     * create a namespace declaration on the current StartElement for any
     * attribute that does not currently have a namespace declaration in scope.
     * If the StartElement has a uri but no prefix specified a prefix will be
     * assigned, if the prefix has not been declared in a parent of the current
     * StartElement it will be declared on the current StartElement. If the
     * defaultNamespace is bound and in scope and the default namespace matches
     * the URI of the attribute or StartElement QName no prefix will be
     * assigned."
     *
     * test case where no prefix needs to be assigned.
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"http:
                + " xmlns:p=\"http:

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:

        xmlStreamWriter.writeAttribute("", "http:
        xmlStreamWriter.writeCharacters("no prefix needs to be assigned");

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefixSpecifiedNamespaceURIWriteAttributeNoPrefixGeneration():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }



    /**
     * Current default namespace is "".
     *
     * write*("p", "myuri", ...); write*("p", "otheruri", ...);
     *
     * XMLOutputFactory (Javadoc) (If repairing of namespaces is enabled): "If
     * element and/or attribute names in the same start or empty-element tag are
     * bound to different namespace URIs and are using the same prefix then the
     * element or the first occurring attribute retains the original prefix and
     * the following attributes have their prefixes replaced with a new prefix
     * that is bound to the namespace URIs of those attributes."
     */
    @Test
    public void testSamePrefixDifferentURI() throws Exception {

        /**
         * writeAttribute("p", "http:
         * writeAttribute("p", "http:
         */
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"\"" + " xmlns:p=\"http:
                + " xmlns:{generated prefix}=\"http:
                + "remap xmlns declaration {generated prefix}=\"http:

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeAttribute("p", "http:
        xmlStreamWriter.writeCharacters("remap xmlns declaration {generated prefix}=\"http:

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSamePrefixDifferentURI(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSamePrefixDifferentURI():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 3, "Expected 2 xmlns:, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":attr").length == 3, "Expected 2 :attr, actual output: " + actualOutput);

        /**
         * writeStartElement("p", "localName", "http:
         * writeAttribute("p", "http:
         * "value");
         */
        final String EXPECTED_OUTPUT_2 = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"\">" + "<p:localName" + " xmlns:p=\"http:
                + " xmlns:{generated prefix}=\"http:

        resetWriter();
        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("p", "localName", "http:
        xmlStreamWriter.writeAttribute("p", "http:

        actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSamePrefixDifferentURI(): expectedOutput: " + EXPECTED_OUTPUT_2);
            System.out.println("testSamePrefixDifferentURI():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 3, "Expected 2 xmlns:, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("p:localName").length == 3, "Expected 2 p:localName, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":attrName").length == 2, "Expected 1 :attrName, actual output: " + actualOutput);

        /**
         * writeNamespace("p", "http:
         * writeAttribute("p", "http:
         * "value");
         */
        final String EXPECTED_OUTPUT_3 = "<?xml version=\"1.0\" ?>" + "<root" + " xmlns=\"\"" + " xmlns:p=\"http:
                + " xmlns:{generated prefix}=\"http:

        resetWriter();
        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeNamespace("p", "http:
        xmlStreamWriter.writeAttribute("p", "http:

        actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSamePrefixDifferentURI(): expectedOutput: " + EXPECTED_OUTPUT_3);
            System.out.println("testSamePrefixDifferentURI():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 2, "Expected 1 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 3, "Expected 2 xmlns:, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":attrName").length == 2, "Expected a :attrName, actual output: " + actualOutput);

        /**
         * writeNamespace("xmlns", ""); writeStartElement("", "localName",
         * "http:
         */
        final String EXPECTED_OUTPUT_4 = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\">" + "<localName xmlns=\"http:
                + "xmlns declaration =\"http:

        resetWriter();
        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeStartElement("", "localName", "http:
        xmlStreamWriter.writeCharacters("remap xmlns declaration {generated prefix}=\"http:

        actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSamePrefixDifferentURI(): expectedOutput: " + EXPECTED_OUTPUT_4);
            System.out.println("testSamePrefixDifferentURI():   actualOutput: " + actualOutput);
        }

        Assert.assertTrue(actualOutput.split("xmlns=").length == 3, "Expected 2 xmlns=, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split("xmlns:").length == 1, "Expected 0 xmlns:, actual output: " + actualOutput);

        Assert.assertTrue(actualOutput.split(":localName").length == 1, "Expected 0 :localName, actual output: " + actualOutput);
    }


    /**
     * The one case where you don't have to worry about fixup is on attributes
     * that do not have a prefix. Irrespective of the current namespace
     * bindings,
     *
     * writeAttribute("", "", "attrName", "value")
     *
     * is always correct and never requires fixup.
     */
    @Test
    public void testEmptyDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"\" attrName=\"value\">" + "never requires fixup" + "</root>";

        startDocumentEmptyDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "", "attrName", "value");
        xmlStreamWriter.writeCharacters("never requires fixup");

        String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testEmptyDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testEmptyDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    @Test
    public void testSpecifiedDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute() throws Exception {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
                + "</root>";

        startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        xmlStreamWriter.writeAttribute("", "", "attrName", "value");
        xmlStreamWriter.writeCharacters("never requires fixup");

        String actualOutput = endDocumentSpecifiedDefaultNamespace(xmlStreamWriter);

        if (DEBUG) {
            System.out.println("testSpecifiedDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute(): expectedOutput: " + EXPECTED_OUTPUT);
            System.out.println("testSpecifiedDefaultEmptyPrefixEmptyNamespaceURIWriteAttribute():   actualOutput: " + actualOutput);
        }

        Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
    }

    /*--------------- Negative tests with isRepairingNamespaces as FALSE ---------------------- */

    private void setUpForNoRepair() {

        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.FALSE);

        try {
            xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(byteArrayOutputStream);

        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        }
    }

    /*
     * Tries to assign default namespace to empty URI and again to a different
     * uri in element and attribute. Expects XMLStreamException .
     * writeNamespace("",""); writeAttribute("", "http:
     * "attrName", "value");
     */
    @Test
    public void testEmptyDefaultEmptyPrefixSpecifiedURIWriteAttributeNoRepair() {
        try {
            setUpForNoRepair();
            startDocumentEmptyDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeAttribute("", "http:
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Tries to assign default namespace to different uris in element and
     * attribute and expects XMLStreamException.
     * writeNamespace("","http:
     * "http:
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixSpecifiedURIWriteAttributeNoRepair() {
        try {
            setUpForNoRepair();
            startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeAttribute("", "http:
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Tries to assign default namespace to same uri twice in element and
     * attribute and expects XMLStreamException.
     * writeNamespace("","http:
     * "http:
     */
    @Test
    public void testSpecifiedDefaultEmptyPrefixSpecifiedDifferentURIWriteAttributeNoRepair() {
        try {
            setUpForNoRepair();
            startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeAttribute("", "http:
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Tries to assign prefix 'p' to different uris to attributes of the same
     * element and expects XMLStreamException. writeAttribute("p",
     * "http:
     * "http:
     */
    @Test
    public void testSamePrefixDiffrentURIWriteAttributeNoRepair() {
        try {
            setUpForNoRepair();
            startDocumentEmptyDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeAttribute("p", "http:
            xmlStreamWriter.writeAttribute("p", "http:
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Tries to assign prefix 'p' to different uris in element and attribute and
     * expects XMLStreamException.
     * writeStartElement("p","localName","http:
     * writeAttribute("p", "http:
     */
    @Test
    public void testSamePrefixDiffrentURIWriteElemAndWriteAttributeNoRepair() {
        try {
            setUpForNoRepair();
            startDocumentEmptyDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeAttribute("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Tries to write following and expects a StreamException. <root
     * xmlns=""http:
     * />
     */
    @Test
    public void testDefaultNamespaceDiffrentURIWriteElementNoRepair() {
        try {
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            setUpForNoRepair();
            startDocumentSpecifiedDefaultNamespace(xmlStreamWriter);
            xmlStreamWriter.writeNamespace("", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*--------------------------------------------------------------------------
     Miscelleneous tests for writeStartElement() & writeAttribute() methods
     in case of NOREPAIR
     --------------------------------------------------------------------------*/

    private void startDocument(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement("root");
    }

    @Test
    public void testSpecifiedPrefixSpecifiedURIWriteElementNoRepair() {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<p:localName></p:localName>" + "</root>";
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Caught an unexpected exception" + e.getMessage());
        }
    }

    @Test
    public void testSpecifiedPrefixSpecifiedURIWriteAttributeNoRepair() {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root p:attrName=\"value\">" + "</root>";
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeAttribute("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Caught an unexpected exception" + e.getMessage());
        }
    }

    @Test
    public void testSpecifiedPrefixSpecifiedURISpecifiedNamespcaeWriteElementNoRepair() {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<p:localName xmlns:p=\"http:
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);

            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeNamespace("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Caught an unexpected exception" + e.getMessage());
        }
    }

    /*
     * writeStartElement("p","localName", "http:
     * writeNamespace("p","http:
     * should generate an error as prefix 'p' is binded to different namespace
     * URIs in same namespace context and repairing is disabled.
     */

    @Test
    public void testSpecifiedPrefixSpecifiedURISpecifiedDifferentNamespcaeWriteElementNoRepair() {

        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeNamespace("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.fail("XMLStreamException is expected as 'p' is rebinded to a different URI in same namespace context");
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }

    @Test
    public void testEmptyPrefixEmptyURIWriteAttributeNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<localName attrName=\"value\"></localName>" + "</root>";
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("localName");
            xmlStreamWriter.writeAttribute("", "", "attrName", "value");
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Caught an unexpected exception" + e.getMessage());
        }
    }

    @Test
    public void testEmptyPrefixNullURIWriteAttributeNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<localName attrName=\"value\"></localName>" + "</root>";
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("localName");
            xmlStreamWriter.writeAttribute(null, null, "attrName", "value");
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.fail("XMLStreamException is expected, actualOutput: " + actualOutput);
        } catch (Exception e) {
            System.out.println("PASS: caught an expected exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDoubleXmlNsNoRepair() {
        try {
            setUpForNoRepair();

            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("foo");
            xmlStreamWriter.writeNamespace("xml", XMLConstants.XML_NS_URI);
            xmlStreamWriter.writeAttribute("xml", XMLConstants.XML_NS_URI, "lang", "ja_JP");
            xmlStreamWriter.writeCharacters("Hello");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndDocument();

            xmlStreamWriter.flush();
            String actualOutput = byteArrayOutputStream.toString();

            if (DEBUG) {
                System.out.println("testDoubleXmlNsNoRepair(): actualOutput: " + actualOutput);
            }

            Assert.assertTrue(actualOutput.split("xmlns:xml").length == 1, "Expected 0 xmlns:xml, actual output: " + actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSpecifiedURIWriteAttributeNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<p:localName p:attrName=\"value\"></p:localName>" + "</root>";
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeAttribute("http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }

    @Test
    public void testSpecifiedURIWriteAttributeWithRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>"
                + "<p:localName xmlns:p=\"http:
        try {
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("p", "localName", "http:
            xmlStreamWriter.writeNamespace("p", "http:
            xmlStreamWriter.writeAttribute("http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testSpecifiedDefaultInDifferentElementsNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root>" + "<localName xmlns=\"http:
                + "<child xmlns=\"http:
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.writeStartElement("localName");
            xmlStreamWriter.writeDefaultNamespace("http:
            xmlStreamWriter.writeStartElement("child");
            xmlStreamWriter.writeDefaultNamespace("http:
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    /*------------- Tests for setPrefix() and setDefaultNamespace() methods --------------------*/

    @Test
    public void testSetPrefixWriteNamespaceNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns:p=\"http:
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.setPrefix("p", "http:
            xmlStreamWriter.writeNamespace("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }

    @Test
    public void testSetPrefixWriteNamespaceWithRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns:p=\"http:
        try {
            startDocument(xmlStreamWriter);
            xmlStreamWriter.setPrefix("p", "http:
            xmlStreamWriter.writeNamespace("p", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }

    @Test
    public void testSetDefaultNamespaceWriteNamespaceNoRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
        try {
            setUpForNoRepair();
            startDocument(xmlStreamWriter);
            xmlStreamWriter.setDefaultNamespace("http:
            xmlStreamWriter.writeNamespace("", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }

    @Test
    public void testSetDefaultNamespaceWriteNamespaceWithRepair() {
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<root xmlns=\"http:
        try {
            startDocument(xmlStreamWriter);
            xmlStreamWriter.setDefaultNamespace("http:
            xmlStreamWriter.writeNamespace("", "http:
            xmlStreamWriter.writeEndElement();
            String actualOutput = endDocumentEmptyDefaultNamespace(xmlStreamWriter);
            System.out.println("actualOutput: " + actualOutput);
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.out.println("Caught an expected exception" + e.getMessage());
        }
    }
}
