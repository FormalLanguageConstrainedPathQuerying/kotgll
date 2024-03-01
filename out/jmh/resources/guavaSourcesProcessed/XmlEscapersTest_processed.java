/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.xml;

import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.CharEscaper;
import junit.framework.TestCase;

/**
 * Tests for the {@link XmlEscapers} class.
 *
 * @author Alex Matevossian
 * @author David Beaumont
 */
@GwtCompatible
public class XmlEscapersTest extends TestCase {

  public void testXmlContentEscaper() throws Exception {
    CharEscaper xmlContentEscaper = (CharEscaper) XmlEscapers.xmlContentEscaper();
    assertBasicXmlEscaper(xmlContentEscaper, false, false);
    assertEquals("\"test\"", xmlContentEscaper.escape("\"test\""));
    assertEquals("'test'", xmlContentEscaper.escape("'test'"));
  }

  public void testXmlAttributeEscaper() throws Exception {
    CharEscaper xmlAttributeEscaper = (CharEscaper) XmlEscapers.xmlAttributeEscaper();
    assertBasicXmlEscaper(xmlAttributeEscaper, true, true);
    assertEquals("&quot;test&quot;", xmlAttributeEscaper.escape("\"test\""));
    assertEquals("&apos;test&apos;", xmlAttributeEscaper.escape("\'test'"));
    assertEquals(
        "a&quot;b&lt;c&gt;d&amp;e&quot;f&apos;", xmlAttributeEscaper.escape("a\"b<c>d&e\"f'"));
    assertEquals("a&#x9;b&#xA;c&#xD;d", xmlAttributeEscaper.escape("a\tb\nc\rd"));
  }

  static void assertBasicXmlEscaper(
      CharEscaper xmlEscaper, boolean shouldEscapeQuotes, boolean shouldEscapeWhitespaceChars) {
    assertEquals("xxx", xmlEscaper.escape("xxx"));
    assertEquals("test &amp; test &amp; test", xmlEscaper.escape("test & test & test"));
    assertEquals("test &lt;&lt; 1", xmlEscaper.escape("test << 1"));
    assertEquals("test &gt;&gt; 1", xmlEscaper.escape("test >> 1"));
    assertEquals("&lt;tab&gt;", xmlEscaper.escape("<tab>"));

    String s =
        "!@#$%^*()_+=-/?\\|]}[{,.;:"
            + "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "1234567890";
    assertEquals(s, xmlEscaper.escape(s));

    for (char ch = 0; ch < 0x20; ch++) {
      if (ch == '\t' || ch == '\n' || ch == '\r') {
        if (shouldEscapeWhitespaceChars) {
          assertEscaping(xmlEscaper, "&#x" + Integer.toHexString(ch).toUpperCase() + ";", ch);
        } else {
          assertUnescaped(xmlEscaper, ch);
        }
      } else {
        assertEscaping(xmlEscaper, "\uFFFD", ch);
      }
    }

    for (char ch = 0x20; ch <= 0xFFFD; ch++) {
      if (ch == '&') {
        assertEscaping(xmlEscaper, "&amp;", ch);
      } else if (ch == '<') {
        assertEscaping(xmlEscaper, "&lt;", ch);
      } else if (ch == '>') {
        assertEscaping(xmlEscaper, "&gt;", ch);
      } else if (shouldEscapeQuotes && ch == '\'') {
        assertEscaping(xmlEscaper, "&apos;", ch);
      } else if (shouldEscapeQuotes && ch == '"') {
        assertEscaping(xmlEscaper, "&quot;", ch);
      } else {
        String input = String.valueOf(ch);
        String escaped = xmlEscaper.escape(input);
        assertEquals(
            "char 0x" + Integer.toString(ch, 16) + " should not be escaped", input, escaped);
      }
    }

    assertEscaping(xmlEscaper, "\uFFFD", '\uFFFE');
    assertEscaping(xmlEscaper, "\uFFFD", '\uFFFF');

    assertEquals(
        "0xFFFE is forbidden and should be replaced during escaping",
        "[\uFFFD]",
        xmlEscaper.escape("[\ufffe]"));
    assertEquals(
        "0xFFFF is forbidden and should be replaced during escaping",
        "[\uFFFD]",
        xmlEscaper.escape("[\uffff]"));
  }
}
