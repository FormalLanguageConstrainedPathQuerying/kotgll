/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLEventWriterTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventWriterTest.XMLEventWriterTest
 * @run testng/othervm stream.XMLEventWriterTest.XMLEventWriterTest
 * @summary Test XMLEventWriter.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class XMLEventWriterTest {

    /**
     * Test XMLStreamWriter parsing a file with an external entity reference.
     */
    @Test
    public void testXMLStreamWriter() {

        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(System.out);
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            String file = getClass().getResource("XMLEventWriterTest.xml").getPath();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(new StreamSource(new File(file)));

            eventWriter.add(eventReader);
            eventWriter.flush();
            eventWriter.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail(exception.toString());
        }
    }

    /**
     * Inspired by CR 6245284 Sun Stax /sjsxp.jar does not behave properly
     * during merge of xml files.
     */
    @Test
    public void testMerge() {

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();

            InputStream inputStream1 = new FileInputStream(new File(XMLEventWriterTest.class.getResource("merge-1.xml").toURI()));
            XMLEventReader r1 = factory.createXMLEventReader(inputStream1);

            InputStream inputStream2 = new FileInputStream(new File(XMLEventWriterTest.class.getResource("merge-2.xml").toURI()));
            XMLEventReader r2 = factory.createXMLEventReader(inputStream2);

            XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

            XMLEventWriter xmlw = xmlof.createXMLEventWriter(System.out);

            QName bName = new QName("b");

            while (r1.hasNext()) {
                XMLEvent event = r1.nextEvent();

                if (event.getEventType() == XMLEvent.END_ELEMENT) {

                    QName name = event.asEndElement().getName();
                    if (name.equals(bName)) {

                        QName zName = new QName("z");

                        boolean isZr = false;

                        while (r2.hasNext()) {
                            XMLEvent event2 = r2.nextEvent();
                            if (event2.getEventType() == XMLEvent.START_ELEMENT && event2.asStartElement().getName().equals(zName)) {
                                isZr = true;
                            }

                            if (xmlw != null && isZr) {
                                xmlw.add(event2);
                            }

                            if (isZr && event2.getEventType() == XMLEvent.END_ELEMENT && event2.asEndElement().getName().equals(zName)) {
                                isZr = false;
                            }
                        }
                        xmlw.flush();
                    }
                }

                if (xmlw != null) {
                    xmlw.add(event);
                }
            }

            xmlw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }
    }
}
