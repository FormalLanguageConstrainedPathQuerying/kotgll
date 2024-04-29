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

package stream.EventsTest;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 8173111
 * @summary tests that filtering out nested elements doesn't end up in
 *          a StackOverflowException
 * @run testng/othervm stream.EventsTest.EventFilterSupportTest
 * @author danielfuchs
 */
public class EventFilterSupportTest {
    static final String ROOT = "xml";
    static final String NEXT = "foo";
    static final String SMOKE = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>"
            + "<xml><foo><foo><foo></foo></foo></foo></xml>";
    static final int MAX = 100_000;

    public static void main(String[] args)
            throws XMLStreamException, IOException {
        smokeTest();
        testNextEvent(MAX);
        testNextTag(MAX);
        System.out.println("Tests passed...");
    }

    @Test
    public static void smokeTest() throws IOException {
        System.out.println("\nSmoke test...");
        StringBuilder sb = new StringBuilder();
        try (InputStream ts = new TestInputStream(4)) {
            int c;
            while ((c = ts.read()) != -1) {
                System.out.print((char)c);
                sb.append((char)c);
            }
        }
        assertEquals(sb.toString(), SMOKE, "Smoke test failed");
        System.out.println("\nSmoke test passed\n");
    }

    @Test
    public static void testNextEvent() throws IOException, XMLStreamException {
        testNextEvent(MAX);
    }

    private static void testNextEvent(int max)
            throws IOException, XMLStreamException {
        System.out.println("\nTest nextEvent (" + max + ")...");
        XMLEventReader reader = createXmlReader(max);
        XMLEvent event;
        do {
            event = reader.nextEvent();
            System.out.println(event);
        } while (event.getEventType() != XMLEvent.END_DOCUMENT);
        System.out.println("nextEvent passed\n");
    }

    @Test
    public static void testNextTag() throws IOException, XMLStreamException {
        testNextTag(MAX);
    }

    private static void testNextTag(int max)
            throws IOException, XMLStreamException {
        System.out.println("\nTest nextTag (" + max + ")...");
        XMLEventReader reader = createXmlReader(max);
        XMLEvent event;
        do {
            event = reader.nextTag();
            System.out.println(event);
            if (event.getEventType() == XMLEvent.END_ELEMENT
                && event.asEndElement().getName().getLocalPart().equals(ROOT)) {
                break;
            }
        } while (true);
        System.out.println("nextTag passed\n");
    }

    private static XMLEventReader createXmlReader(int max)
            throws XMLStreamException {
        TestInputStream ts = new TestInputStream(max);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLEventReader reader = xif.createXMLEventReader(ts);
        return xif.createFilteredReader(reader, new TagFilter(max));
    }

    public static class TestInputStream extends InputStream {

        int open = 0;
        int i = 0;
        int n = 0;
        final int max;

        public TestInputStream(int max) {
            this.max = max;
        }

        String tag() {
            if (n == 0) {
                return "?xml version=\"1.0\" encoding=\"US-ASCII\"?><" + ROOT;
            }
            if (n == 2 * max -1) {
                return ROOT;
            }
            return NEXT;
        }

        @Override
        public int read() throws IOException {
            if (n >= 2 * max) return -1;
            if (open == 0) {
                open = 1;
                return '<';
            }
            if (open == 1 && n >= max) {
                open = 2;
                return '/';
            }
            String tag = tag();
            if (open > 0 && i < tag.length()) {
                return tag.charAt(i++);
            }
            if (open > 0 && i == tag.length()) {
                open = 0; i = 0; n++;
                return '>';
            }
            return -1;
        }
    }

    public static final class TagFilter implements EventFilter {
        int count;
        final int max;

        public TagFilter(int max) {
            this.max = max;
        }

        @Override
        public boolean accept(XMLEvent event) {
            int type = event.getEventType();
            if (type == XMLEvent.START_ELEMENT) {
                String loc = event.asStartElement().getName().getLocalPart();
                if (count == 0 || count == 1) System.out.println("<" + loc + ">");
                count++;
                return ROOT.equals(loc);
            }
            if (type == XMLEvent.END_ELEMENT) {
                if (count == max) System.out.println("Got " + count + " elements");
                String loc = event.asEndElement().getName().getLocalPart();
                count--;
                if (count == 0 || count == 1) System.out.println("</" + loc + ">");
                return ROOT.equals(loc);
            }
            if (type == XMLEvent.PROCESSING_INSTRUCTION) return true;
            if (type == XMLEvent.START_DOCUMENT) return true;
            if (type == XMLEvent.END_DOCUMENT) return true;
            return false;
        }
    }

}
