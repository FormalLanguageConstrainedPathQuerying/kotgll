/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.io;

import static org.junit.Assert.assertThrows;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.EOFException;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.List;

/**
 * Unit test for {@link CharStreams}.
 *
 * @author Chris Nokleberg
 */
public class CharStreamsTest extends IoTestCase {

  private static final String TEXT = "The quick brown fox jumped over the lazy dog.";

  public void testToString() throws IOException {
    assertEquals(TEXT, CharStreams.toString(new StringReader(TEXT)));
  }

  public void testReadLines() throws IOException {
    List<String> lines = CharStreams.readLines(new StringReader("a\nb\nc"));
    assertEquals(ImmutableList.of("a", "b", "c"), lines);
  }

  public void testReadLines_withLineProcessor() throws IOException {
    String text = "a\nb\nc";

    Reader r = new StringReader(text);
    LineProcessor<Integer> alwaysFalse =
        new LineProcessor<Integer>() {
          int seen;

          @Override
          public boolean processLine(String line) {
            seen++;
            return false;
          }

          @Override
          public Integer getResult() {
            return seen;
          }
        };
    assertEquals(
        "processLine was called more than once",
        1,
        CharStreams.readLines(r, alwaysFalse).intValue());

    r = new StringReader(text);
    LineProcessor<Integer> alwaysTrue =
        new LineProcessor<Integer>() {
          int seen;

          @Override
          public boolean processLine(String line) {
            seen++;
            return true;
          }

          @Override
          public Integer getResult() {
            return seen;
          }
        };
    assertEquals(
        "processLine was not called for all the lines",
        3,
        CharStreams.readLines(r, alwaysTrue).intValue());

    r = new StringReader(text);
    final StringBuilder sb = new StringBuilder();
    LineProcessor<Integer> conditional =
        new LineProcessor<Integer>() {
          int seen;

          @Override
          public boolean processLine(String line) {
            seen++;
            sb.append(line);
            return seen < 2;
          }

          @Override
          public Integer getResult() {
            return seen;
          }
        };
    assertEquals(2, CharStreams.readLines(r, conditional).intValue());
    assertEquals("ab", sb.toString());
  }

  public void testSkipFully_EOF() throws IOException {
    Reader reader = new StringReader("abcde");
    assertThrows(EOFException.class, () -> CharStreams.skipFully(reader, 6));
  }

  public void testSkipFully() throws IOException {
    String testString = "abcdef";
    Reader reader = new StringReader(testString);

    assertEquals(testString.charAt(0), reader.read());
    CharStreams.skipFully(reader, 1);
    assertEquals(testString.charAt(2), reader.read());
    CharStreams.skipFully(reader, 2);
    assertEquals(testString.charAt(5), reader.read());

    assertEquals(-1, reader.read());
  }

  public void testAsWriter() {
    Appendable plainAppendable = new StringBuilder();
    Writer result = CharStreams.asWriter(plainAppendable);
    assertNotSame(plainAppendable, result);
    assertNotNull(result);

    Appendable secretlyAWriter = new StringWriter();
    result = CharStreams.asWriter(secretlyAWriter);
    assertSame(secretlyAWriter, result);
  }


  public void testCopy() throws IOException {
    StringBuilder builder = new StringBuilder();
    long copied =
        CharStreams.copy(
            wrapAsGenericReadable(new StringReader(ASCII)), wrapAsGenericAppendable(builder));
    assertEquals(ASCII, builder.toString());
    assertEquals(ASCII.length(), copied);

    StringBuilder builder2 = new StringBuilder();
    copied =
        CharStreams.copy(
            wrapAsGenericReadable(new StringReader(I18N)), wrapAsGenericAppendable(builder2));
    assertEquals(I18N, builder2.toString());
    assertEquals(I18N.length(), copied);
  }

  public void testCopy_toStringBuilder_fromReader() throws IOException {
    StringBuilder builder = new StringBuilder();
    long copied = CharStreams.copy(new StringReader(ASCII), builder);
    assertEquals(ASCII, builder.toString());
    assertEquals(ASCII.length(), copied);

    StringBuilder builder2 = new StringBuilder();
    copied = CharStreams.copy(new StringReader(I18N), builder2);
    assertEquals(I18N, builder2.toString());
    assertEquals(I18N.length(), copied);
  }

  public void testCopy_toStringBuilder_fromReadable() throws IOException {
    StringBuilder builder = new StringBuilder();
    long copied = CharStreams.copy(wrapAsGenericReadable(new StringReader(ASCII)), builder);
    assertEquals(ASCII, builder.toString());
    assertEquals(ASCII.length(), copied);

    StringBuilder builder2 = new StringBuilder();
    copied = CharStreams.copy(wrapAsGenericReadable(new StringReader(I18N)), builder2);
    assertEquals(I18N, builder2.toString());
    assertEquals(I18N.length(), copied);
  }

  public void testCopy_toWriter_fromReader() throws IOException {
    StringWriter writer = new StringWriter();
    long copied = CharStreams.copy(new StringReader(ASCII), writer);
    assertEquals(ASCII, writer.toString());
    assertEquals(ASCII.length(), copied);

    StringWriter writer2 = new StringWriter();
    copied = CharStreams.copy(new StringReader(I18N), writer2);
    assertEquals(I18N, writer2.toString());
    assertEquals(I18N.length(), copied);
  }

  public void testCopy_toWriter_fromReadable() throws IOException {
    StringWriter writer = new StringWriter();
    long copied = CharStreams.copy(wrapAsGenericReadable(new StringReader(ASCII)), writer);
    assertEquals(ASCII, writer.toString());
    assertEquals(ASCII.length(), copied);

    StringWriter writer2 = new StringWriter();
    copied = CharStreams.copy(wrapAsGenericReadable(new StringReader(I18N)), writer2);
    assertEquals(I18N, writer2.toString());
    assertEquals(I18N.length(), copied);
  }

  /**
   * Test for Guava issue 1061: http:
   *
   * <p>CharStreams.copy was failing to clear its CharBuffer after each read call, which effectively
   * reduced the available size of the buffer each time a call to read didn't fill up the available
   * space in the buffer completely. In general this is a performance problem since the buffer size
   * is permanently reduced, but with certain Reader implementations it could also cause the buffer
   * size to reach 0, causing an infinite loop.
   */
  public void testCopyWithReaderThatDoesNotFillBuffer() throws IOException {
    String string = Strings.repeat("0123456789", 100);
    StringBuilder b = new StringBuilder();
    long copied = CharStreams.copy(newNonBufferFillingReader(new StringReader(string)), b);
    assertEquals(string, b.toString());
    assertEquals(string.length(), copied);
  }

  public void testExhaust_reader() throws IOException {
    Reader reader = new StringReader(ASCII);
    assertEquals(ASCII.length(), CharStreams.exhaust(reader));
    assertEquals(-1, reader.read());
    assertEquals(0, CharStreams.exhaust(reader));

    Reader empty = new StringReader("");
    assertEquals(0, CharStreams.exhaust(empty));
    assertEquals(-1, empty.read());
  }

  public void testExhaust_readable() throws IOException {
    CharBuffer buf = CharBuffer.wrap(ASCII);
    assertEquals(ASCII.length(), CharStreams.exhaust(buf));
    assertEquals(0, buf.remaining());
    assertEquals(0, CharStreams.exhaust(buf));

    CharBuffer empty = CharBuffer.wrap("");
    assertEquals(0, CharStreams.exhaust(empty));
    assertEquals(0, empty.remaining());
  }

  public void testNullWriter() throws Exception {
    Writer nullWriter = CharStreams.nullWriter();
    nullWriter.write('n');
    String test = "Test string for NullWriter";
    nullWriter.write(test);
    nullWriter.write(test, 2, 10);
    nullWriter.append(null);
    nullWriter.append(null, 0, 4);

    assertThrows(IndexOutOfBoundsException.class, () -> nullWriter.append(null, -1, 4));

    assertThrows(IndexOutOfBoundsException.class, () -> nullWriter.append(null, 0, 5));

    assertSame(CharStreams.nullWriter(), CharStreams.nullWriter());
  }

  /**
   * Returns a reader wrapping the given reader that only reads half of the maximum number of
   * characters that it could read in read(char[], int, int).
   */
  private static Reader newNonBufferFillingReader(Reader reader) {
    return new FilterReader(reader) {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        if (len <= 0) {
          fail("read called with a len of " + len);
        }
        return in.read(cbuf, off, Math.max(len - 1024, 0));
      }
    };
  }

  /** Wrap an appendable in an appendable to defeat any type specific optimizations. */
  private static Appendable wrapAsGenericAppendable(final Appendable a) {
    return new Appendable() {

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        a.append(csq);
        return this;
      }

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        a.append(csq, start, end);
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        a.append(c);
        return this;
      }
    };
  }

  /** Wrap a readable in a readable to defeat any type specific optimizations. */
  private static Readable wrapAsGenericReadable(final Readable a) {
    return new Readable() {
      @Override
      public int read(CharBuffer cb) throws IOException {
        return a.read(cb);
      }
    };
  }
}
