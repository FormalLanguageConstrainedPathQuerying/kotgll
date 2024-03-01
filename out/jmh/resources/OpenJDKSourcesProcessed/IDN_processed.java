/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package java.net;

import java.io.InputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jdk.internal.icu.impl.Punycode;
import jdk.internal.icu.text.StringPrep;
import jdk.internal.icu.text.UCharacterIterator;

/**
 * Provides methods to convert internationalized domain names (IDNs) between
 * a normal Unicode representation and an ASCII Compatible Encoding (ACE) representation.
 * Internationalized domain names can use characters from the entire range of
 * Unicode, while traditional domain names are restricted to ASCII characters.
 * ACE is an encoding of Unicode strings that uses only ASCII characters and
 * can be used with software (such as the Domain Name System) that only
 * understands traditional domain names.
 *
 * <p>Internationalized domain names are defined in <a href="http:
 * RFC 3490 defines two operations: ToASCII and ToUnicode. These 2 operations employ
 * <a href="http:
 * profile of <a href="http:
 * <a href="http:
 * domain name string back and forth.
 *
 * <p>The behavior of aforementioned conversion process can be adjusted by various flags:
 *   <ul>
 *     <li>If the ALLOW_UNASSIGNED flag is used, the domain name string to be converted
 *         can contain code points that are unassigned in Unicode 3.2, which is the
 *         Unicode version on which IDN conversion is based. If the flag is not used,
 *         the presence of such unassigned code points is treated as an error.
 *     <li>If the USE_STD3_ASCII_RULES flag is used, ASCII strings are checked against <a href="http:
 *         It is an error if they don't meet the requirements.
 *   </ul>
 * These flags can be logically OR'ed together.
 *
 * <p>The security consideration is important with respect to internationalization
 * domain name support. For example, English domain names may be <i>homographed</i>
 * - maliciously misspelled by substitution of non-Latin letters.
 * <a href="http:
 * discusses security issues of IDN support as well as possible solutions.
 * Applications are responsible for taking adequate security measures when using
 * international domain names.
 *
 * @spec https:
 *      RFC 1122: Requirements for Internet Hosts - Communication Layers
 * @spec https:
 *      RFC 1123: Requirements for Internet Hosts - Application and Support
 * @spec https:
 *      RFC 3454: Preparation of Internationalized Strings ("stringprep")
 * @spec https:
 *      RFC 3490: Internationalizing Domain Names in Applications (IDNA)
 * @spec https:
 *      RFC 3491: Nameprep: A Stringprep Profile for Internationalized Domain Names (IDN)
 * @spec https:
 *      RFC 3492: Punycode: A Bootstring encoding of Unicode for Internationalized Domain Names in Applications (IDNA)
 * @spec https:
 *      Unicode Security Considerations
 * @author Edward Wang
 * @since 1.6
 *
 */
public final class IDN {
    /**
     * Flag to allow processing of unassigned code points
     */
    public static final int ALLOW_UNASSIGNED = 0x01;

    /**
     * Flag to turn on the check against STD-3 ASCII rules
     */
    public static final int USE_STD3_ASCII_RULES = 0x02;


    /**
     * Translates a string from Unicode to ASCII Compatible Encoding (ACE),
     * as defined by the ToASCII operation of <a href="http:
     *
     * <p>ToASCII operation can fail. ToASCII fails if any step of it fails.
     * If ToASCII operation fails, an IllegalArgumentException will be thrown.
     * In this case, the input string should not be used in an internationalized domain name.
     *
     * <p> A label is an individual part of a domain name. The original ToASCII operation,
     * as defined in RFC 3490, only operates on a single label. This method can handle
     * both label and entire domain name, by assuming that labels in a domain name are
     * always separated by dots. The following characters are recognized as dots:
     * &#0092;u002E (full stop), &#0092;u3002 (ideographic full stop), &#0092;uFF0E (fullwidth full stop),
     * and &#0092;uFF61 (halfwidth ideographic full stop). if dots are
     * used as label separators, this method also changes all of them to &#0092;u002E (full stop)
     * in output translated string.
     *
     * @param input     the string to be processed
     * @param flag      process flag; can be 0 or any logical OR of possible flags
     *
     * @return          the translated {@code String}
     *
     * @throws IllegalArgumentException   if the input string doesn't conform to RFC 3490 specification
     * @spec https:
     *      RFC 3490: Internationalizing Domain Names in Applications (IDNA)
     */
    public static String toASCII(String input, int flag)
    {
        int p = 0, q = 0;
        StringBuilder out = new StringBuilder();

        if (isRootLabel(input)) {
            return ".";
        }

        while (p < input.length()) {
            q = searchDots(input, p);
            out.append(toASCIIInternal(input.substring(p, q),  flag));
            if (q != (input.length())) {
               out.append('.');
            }
            p = q + 1;
        }

        return out.toString();
    }


    /**
     * Translates a string from Unicode to ASCII Compatible Encoding (ACE),
     * as defined by the ToASCII operation of <a href="http:
     *
     * <p> This convenience method works as if by invoking the
     * two-argument counterpart as follows:
     * <blockquote>
     * {@link #toASCII(String, int) toASCII}(input,&nbsp;0);
     * </blockquote>
     *
     * @param input     the string to be processed
     *
     * @return          the translated {@code String}
     *
     * @throws IllegalArgumentException   if the input string doesn't conform to RFC 3490 specification
     * @spec https:
     *      RFC 3490: Internationalizing Domain Names in Applications (IDNA)
     */
    public static String toASCII(String input) {
        return toASCII(input, 0);
    }


    /**
     * Translates a string from ASCII Compatible Encoding (ACE) to Unicode,
     * as defined by the ToUnicode operation of <a href="http:
     *
     * <p>ToUnicode never fails. In case of any error, the input string is returned unmodified.
     *
     * <p> A label is an individual part of a domain name. The original ToUnicode operation,
     * as defined in RFC 3490, only operates on a single label. This method can handle
     * both label and entire domain name, by assuming that labels in a domain name are
     * always separated by dots. The following characters are recognized as dots:
     * &#0092;u002E (full stop), &#0092;u3002 (ideographic full stop), &#0092;uFF0E (fullwidth full stop),
     * and &#0092;uFF61 (halfwidth ideographic full stop).
     *
     * @param input     the string to be processed
     * @param flag      process flag; can be 0 or any logical OR of possible flags
     *
     * @return          the translated {@code String}
     * @spec https:
     *      RFC 3490: Internationalizing Domain Names in Applications (IDNA)
     */
    public static String toUnicode(String input, int flag) {
        int p = 0, q = 0;
        StringBuilder out = new StringBuilder();

        if (isRootLabel(input)) {
            return ".";
        }

        while (p < input.length()) {
            q = searchDots(input, p);
            out.append(toUnicodeInternal(input.substring(p, q),  flag));
            if (q != (input.length())) {
               out.append('.');
            }
            p = q + 1;
        }

        return out.toString();
    }


    /**
     * Translates a string from ASCII Compatible Encoding (ACE) to Unicode,
     * as defined by the ToUnicode operation of <a href="http:
     *
     * <p> This convenience method works as if by invoking the
     * two-argument counterpart as follows:
     * <blockquote>
     * {@link #toUnicode(String, int) toUnicode}(input,&nbsp;0);
     * </blockquote>
     *
     * @param input     the string to be processed
     *
     * @return          the translated {@code String}
     * @spec https:
     *      RFC 3490: Internationalizing Domain Names in Applications (IDNA)
     */
    public static String toUnicode(String input) {
        return toUnicode(input, 0);
    }


    /* ---------------- Private members -------------- */

    private static final String ACE_PREFIX = "xn--";
    private static final int ACE_PREFIX_LENGTH = ACE_PREFIX.length();

    private static final int MAX_LABEL_LENGTH   = 63;

    private static final StringPrep namePrep;

    static {
        StringPrep stringPrep = null;
        try {
            final String IDN_PROFILE = "/sun/net/idn/uidna.spp";
            @SuppressWarnings("removal")
            InputStream stream = System.getSecurityManager() != null
                    ? AccessController.doPrivileged(new PrivilegedAction<>() {
                            public InputStream run() {
                                return StringPrep.class.getResourceAsStream(IDN_PROFILE);
                            }})
                    : StringPrep.class.getResourceAsStream(IDN_PROFILE);

            stringPrep = new StringPrep(stream);
            stream.close();
        } catch (IOException e) {
            assert false;
        }
        namePrep = stringPrep;
    }


    /* ---------------- Private operations -------------- */


    private IDN() {}

    private static String toASCIIInternal(String label, int flag)
    {
        boolean isASCII  = isAllASCII(label);
        StringBuffer dest;

        if (!isASCII) {
            UCharacterIterator iter = UCharacterIterator.getInstance(label);
            try {
                dest = namePrep.prepare(iter, flag);
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            dest = new StringBuffer(label);
        }

        if (dest.length() == 0) {
            throw new IllegalArgumentException(
                        "Empty label is not a legal name");
        }

        boolean useSTD3ASCIIRules = ((flag & USE_STD3_ASCII_RULES) != 0);
        if (useSTD3ASCIIRules) {
            for (int i = 0; i < dest.length(); i++) {
                int c = dest.charAt(i);
                if (isNonLDHAsciiCodePoint(c)) {
                    throw new IllegalArgumentException(
                        "Contains non-LDH ASCII characters");
                }
            }

            if (dest.charAt(0) == '-' ||
                dest.charAt(dest.length() - 1) == '-') {

                throw new IllegalArgumentException(
                        "Has leading or trailing hyphen");
            }
        }

        if (!isASCII) {
            if (!isAllASCII(dest.toString())) {
                if(!startsWithACEPrefix(dest)){

                    try {
                        dest = Punycode.encode(dest, null);
                    } catch (java.text.ParseException e) {
                        throw new IllegalArgumentException(e);
                    }

                    dest = toASCIILower(dest);

                    dest.insert(0, ACE_PREFIX);
                } else {
                    throw new IllegalArgumentException("The input starts with the ACE Prefix");
                }

            }
        }

        if (dest.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("The label in the input is too long");
        }

        return dest.toString();
    }

    private static String toUnicodeInternal(String label, int flag) {
        boolean[] caseFlags = null;
        StringBuffer dest;

        boolean isASCII = isAllASCII(label);

        if(!isASCII){
            try {
                UCharacterIterator iter = UCharacterIterator.getInstance(label);
                dest = namePrep.prepare(iter, flag);
            } catch (Exception e) {
                return label;
            }
        } else {
            dest = new StringBuffer(label);
        }

        if(startsWithACEPrefix(dest)) {

            String temp = dest.substring(ACE_PREFIX_LENGTH, dest.length());

            try {
                StringBuffer decodeOut = Punycode.decode(new StringBuffer(temp), null);

                String toASCIIOut = toASCII(decodeOut.toString(), flag);

                if (toASCIIOut.equalsIgnoreCase(dest.toString())) {
                    return decodeOut.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return label;
    }


    private static boolean isNonLDHAsciiCodePoint(int ch){
        return (0x0000 <= ch && ch <= 0x002C) ||
               (0x002E <= ch && ch <= 0x002F) ||
               (0x003A <= ch && ch <= 0x0040) ||
               (0x005B <= ch && ch <= 0x0060) ||
               (0x007B <= ch && ch <= 0x007F);
    }

    private static int searchDots(String s, int start) {
        int i;
        for (i = start; i < s.length(); i++) {
            if (isLabelSeparator(s.charAt(i))) {
                break;
            }
        }

        return i;
    }

    private static boolean isRootLabel(String s) {
        return (s.length() == 1 && isLabelSeparator(s.charAt(0)));
    }

    private static boolean isLabelSeparator(char c) {
        return (c == '.' || c == '\u3002' || c == '\uFF0E' || c == '\uFF61');
    }

    private static boolean isAllASCII(String input) {
        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 0x7F) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }

    private static boolean startsWithACEPrefix(StringBuffer input){
        boolean startsWithPrefix = true;

        if(input.length() < ACE_PREFIX_LENGTH){
            return false;
        }
        for(int i = 0; i < ACE_PREFIX_LENGTH; i++){
            if(toASCIILower(input.charAt(i)) != ACE_PREFIX.charAt(i)){
                startsWithPrefix = false;
            }
        }
        return startsWithPrefix;
    }

    private static char toASCIILower(char ch){
        if('A' <= ch && ch <= 'Z'){
            return (char)(ch + 'a' - 'A');
        }
        return ch;
    }

    private static StringBuffer toASCIILower(StringBuffer input){
        StringBuffer dest = new StringBuffer();
        for(int i = 0; i < input.length();i++){
            dest.append(toASCIILower(input.charAt(i)));
        }
        return dest;
    }
}
