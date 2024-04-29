/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.Escaper;

/**
 * {@code Escaper} instances suitable for strings to be included in particular sections of URLs.
 *
 * <p>If the resulting URLs are inserted into an HTML or XML document, they will require additional
 * escaping with {@link com.google.common.html.HtmlEscapers} or {@link
 * com.google.common.xml.XmlEscapers}.
 *
 * @author David Beaumont
 * @author Chris Povirk
 * @since 15.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class UrlEscapers {
  private UrlEscapers() {}


  static final String URL_FORM_PARAMETER_OTHER_SAFE_CHARS = "-_.*";

  static final String URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS =
      "-._~" 
          + "!$'()*,;&=" 
          + "@:"; 

  /**
   * Returns an {@link Escaper} instance that escapes strings so they can be safely included in <a
   * href="https:
   * with the UTF-8 character encoding. The caller is responsible for <a
   * href="https:
   * with a CR+LF pair</a> on any non-file inputs before escaping them with this escaper.
   *
   * <p>When escaping a String, the following rules apply:
   *
   * <ul>
   *   <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain
   *       the same.
   *   <li>The special characters ".", "-", "*", and "_" remain the same.
   *   <li>The space character " " is converted into a plus sign "+".
   *   <li>All other characters are converted into one or more bytes using UTF-8 encoding and each
   *       byte is then represented by the 3-character string "%XY", where "XY" is the two-digit,
   *       uppercase, hexadecimal representation of the byte value.
   * </ul>
   *
   * <p>This escaper is suitable for escaping parameter names and values even when <a
   * href="https:
   * a parameter delimiter. Nevertheless, we recommend using the ampersand unless you must
   * interoperate with systems that require semicolons.
   *
   * <p><b>Note:</b> Unlike other escapers, URL escapers produce <a
   * href="https:
   *
   */
  public static Escaper urlFormParameterEscaper() {
    return URL_FORM_PARAMETER_ESCAPER;
  }

  private static final Escaper URL_FORM_PARAMETER_ESCAPER =
      new PercentEscaper(URL_FORM_PARAMETER_OTHER_SAFE_CHARS, true);

  /**
   * Returns an {@link Escaper} instance that escapes strings so they can be safely included in <a
   * href="https:
   * characters, even though <a href="https:
   * URLs</a>. (<a href="https:
   * unescaped, they would be escaped by the consumer at parse time, anyway.</a>) Additionally, the
   * escaper escapes the slash character ("/"). While slashes are acceptable in URL paths, they are
   * considered by the specification to be separators between "path segments." This implies that, if
   * you wish for your path to contain slashes, you must escape each segment separately and then
   * join them.
   *
   * <p>When escaping a String, the following rules apply:
   *
   * <ul>
   *   <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain
   *       the same.
   *   <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   *   <li>The general delimiters "@" and ":" remain the same.
   *   <li>The subdelimiters "!", "$", "&amp;", "'", "(", ")", "*", "+", ",", ";", and "=" remain
   *       the same.
   *   <li>The space character " " is converted into %20.
   *   <li>All other characters are converted into one or more bytes using UTF-8 encoding and each
   *       byte is then represented by the 3-character string "%XY", where "XY" is the two-digit,
   *       uppercase, hexadecimal representation of the byte value.
   * </ul>
   *
   * <p><b>Note:</b> Unlike other escapers, URL escapers produce <a
   * href="https:
   */
  public static Escaper urlPathSegmentEscaper() {
    return URL_PATH_SEGMENT_ESCAPER;
  }

  private static final Escaper URL_PATH_SEGMENT_ESCAPER =
      new PercentEscaper(URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS + "+", false);

  /**
   * Returns an {@link Escaper} instance that escapes strings so they can be safely included in a <a
   * href="https:
   * characters, even though <a href="https:
   * URLs</a>.
   *
   * <p>When escaping a String, the following rules apply:
   *
   * <ul>
   *   <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain
   *       the same.
   *   <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   *   <li>The general delimiters "@" and ":" remain the same.
   *   <li>The subdelimiters "!", "$", "&amp;", "'", "(", ")", "*", "+", ",", ";", and "=" remain
   *       the same.
   *   <li>The space character " " is converted into %20.
   *   <li>Fragments allow unescaped "/" and "?", so they remain the same.
   *   <li>All other characters are converted into one or more bytes using UTF-8 encoding and each
   *       byte is then represented by the 3-character string "%XY", where "XY" is the two-digit,
   *       uppercase, hexadecimal representation of the byte value.
   * </ul>
   *
   * <p><b>Note:</b> Unlike other escapers, URL escapers produce <a
   * href="https:
   */
  public static Escaper urlFragmentEscaper() {
    return URL_FRAGMENT_ESCAPER;
  }

  private static final Escaper URL_FRAGMENT_ESCAPER =
      new PercentEscaper(URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS + "+/?", false);
}
