/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.net;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.UnicodeEscaper;
import javax.annotation.CheckForNull;

/**
 * A {@code UnicodeEscaper} that escapes some set of Java characters using a UTF-8 based percent
 * encoding scheme. The set of safe characters (those which remain unescaped) can be specified on
 * construction.
 *
 * <p>This class is primarily used for creating URI escapers in {@link UrlEscapers} but can be used
 * directly if required. While URI escapers impose specific semantics on which characters are
 * considered 'safe', this class has a minimal set of restrictions.
 *
 * <p>When escaping a String, the following rules apply:
 *
 * <ul>
 *   <li>All specified safe characters remain unchanged.
 *   <li>If {@code plusForSpace} was specified, the space character " " is converted into a plus
 *       sign {@code "+"}.
 *   <li>All other characters are converted into one or more bytes using UTF-8 encoding and each
 *       byte is then represented by the 3-character string "%XX", where "XX" is the two-digit,
 *       uppercase, hexadecimal representation of the byte value.
 * </ul>
 *
 * <p>For performance reasons the only currently supported character encoding of this class is
 * UTF-8.
 *
 * <p><b>Note:</b> This escaper produces <a
 * href="https:
 *
 * @author David Beaumont
 * @since 15.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class PercentEscaper extends UnicodeEscaper {

  private static final char[] PLUS_SIGN = {'+'};

  private static final char[] UPPER_HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  /** If true we should convert space to the {@code +} character. */
  private final boolean plusForSpace;

  /**
   * An array of flags where for any {@code char c} if {@code safeOctets[c]} is true then {@code c}
   * should remain unmodified in the output. If {@code c >= safeOctets.length} then it should be
   * escaped.
   */
  private final boolean[] safeOctets;

  /**
   * Constructs a percent escaper with the specified safe characters and optional handling of the
   * space character.
   *
   * <p>Not that it is allowed, but not necessarily desirable to specify {@code %} as a safe
   * character. This has the effect of creating an escaper which has no well-defined inverse but it
   * can be useful when escaping additional characters.
   *
   * @param safeChars a non-null string specifying additional safe characters for this escaper (the
   *     ranges 0..9, a..z and A..Z are always safe and should not be specified here)
   * @param plusForSpace true if ASCII space should be escaped to {@code +} rather than {@code %20}
   * @throws IllegalArgumentException if any of the parameters were invalid
   */
  public PercentEscaper(String safeChars, boolean plusForSpace) {
    checkNotNull(safeChars); 
    if (safeChars.matches(".*[0-9A-Za-z].*")) {
      throw new IllegalArgumentException(
          "Alphanumeric characters are always 'safe' and should not be explicitly specified");
    }
    safeChars += "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    if (plusForSpace && safeChars.contains(" ")) {
      throw new IllegalArgumentException(
          "plusForSpace cannot be specified when space is a 'safe' character");
    }
    this.plusForSpace = plusForSpace;
    this.safeOctets = createSafeOctets(safeChars);
  }

  /**
   * Creates a boolean array with entries corresponding to the character values specified in
   * safeChars set to true. The array is as small as is required to hold the given character
   * information.
   */
  private static boolean[] createSafeOctets(String safeChars) {
    int maxChar = -1;
    char[] safeCharArray = safeChars.toCharArray();
    for (char c : safeCharArray) {
      maxChar = Math.max(c, maxChar);
    }
    boolean[] octets = new boolean[maxChar + 1];
    for (char c : safeCharArray) {
      octets[c] = true;
    }
    return octets;
  }

  /*
   * Overridden for performance. For unescaped strings this improved the performance of the uri
   * escaper from ~760ns to ~400ns as measured by {@link CharEscapersBenchmark}.
   */
  @Override
  protected int nextEscapeIndex(CharSequence csq, int index, int end) {
    checkNotNull(csq);
    for (; index < end; index++) {
      char c = csq.charAt(index);
      if (c >= safeOctets.length || !safeOctets[c]) {
        break;
      }
    }
    return index;
  }

  /*
   * Overridden for performance. For unescaped strings this improved the performance of the uri
   * escaper from ~400ns to ~170ns as measured by {@link CharEscapersBenchmark}.
   */
  @Override
  public String escape(String s) {
    checkNotNull(s);
    int slen = s.length();
    for (int index = 0; index < slen; index++) {
      char c = s.charAt(index);
      if (c >= safeOctets.length || !safeOctets[c]) {
        return escapeSlow(s, index);
      }
    }
    return s;
  }

  /** Escapes the given Unicode code point in UTF-8. */
  @Override
  @CheckForNull
  protected char[] escape(int cp) {
    if (cp < safeOctets.length && safeOctets[cp]) {
      return null;
    } else if (cp == ' ' && plusForSpace) {
      return PLUS_SIGN;
    } else if (cp <= 0x7F) {
      char[] dest = new char[3];
      dest[0] = '%';
      dest[2] = UPPER_HEX_DIGITS[cp & 0xF];
      dest[1] = UPPER_HEX_DIGITS[cp >>> 4];
      return dest;
    } else if (cp <= 0x7ff) {
      char[] dest = new char[6];
      dest[0] = '%';
      dest[3] = '%';
      dest[5] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[4] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[2] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[1] = UPPER_HEX_DIGITS[0xC | cp];
      return dest;
    } else if (cp <= 0xffff) {
      char[] dest = new char[9];
      dest[0] = '%';
      dest[1] = 'E';
      dest[3] = '%';
      dest[6] = '%';
      dest[8] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[7] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[5] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[4] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[2] = UPPER_HEX_DIGITS[cp];
      return dest;
    } else if (cp <= 0x10ffff) {
      char[] dest = new char[12];
      dest[0] = '%';
      dest[1] = 'F';
      dest[3] = '%';
      dest[6] = '%';
      dest[9] = '%';
      dest[11] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[10] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[8] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[7] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[5] = UPPER_HEX_DIGITS[cp & 0xF];
      cp >>>= 4;
      dest[4] = UPPER_HEX_DIGITS[0x8 | (cp & 0x3)];
      cp >>>= 2;
      dest[2] = UPPER_HEX_DIGITS[cp & 0x7];
      return dest;
    } else {
      throw new IllegalArgumentException("Invalid unicode character value " + cp);
    }
  }
}
