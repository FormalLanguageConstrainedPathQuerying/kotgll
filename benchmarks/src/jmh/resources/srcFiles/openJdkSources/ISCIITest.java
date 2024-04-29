/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4328178
 * @summary Performs baseline and regression test on the ISCII91 charset
 * @modules jdk.charsets
 */

import java.io.*;

public class ISCIITest {

    private static void failureReport() {
        System.err.println ("Failed ISCII91 Regression Test");
    }

    private static void mapEquiv(int start,
                                 int end,
                                 String testName)
    throws Exception
    {
        byte[] singleByte = new byte[1];
        byte[] encoded = new byte[1];

        for (int i = start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            try {
                String unicodeStr =
                        new String (singleByte,"ISCII91");

                if (i != (int)unicodeStr.charAt(0)) {
                    System.err.println ("FAILED ISCII91 Regression test"
                                        + "input byte is " + i );
                    throw new Exception("");
                }
                encoded = unicodeStr.getBytes("ISCII91");

                if (encoded[0] != singleByte[0]) {
                   System.err.println("Encoding error " + testName);
                   throw new Exception("Failed ISCII91 Regression test");
                }

            } catch (UnsupportedEncodingException e) {
                failureReport();
            }
        }
        return;
    }

    private static void checkUnmapped(int start,
                                      int end,
                                      String testName)
    throws Exception {

        byte[] singleByte = new byte[1];

        for (int i = start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            try {
                String unicodeStr = new String (singleByte, "ISCII91");

                if (unicodeStr.charAt(0) != '\uFFFD') {
                    System.err.println("FAILED " + testName +
                                        "input byte is " + i );
                    throw new Exception ("Failed ISCII91 regression test");
                }
            } catch (UnsupportedEncodingException e) {
                System.err.println("Unsupported character encoding");
            }
        }
        return;
    }

    /*
     *
     */
    private static void checkRange(int start, int end,
                                   char[] expectChars,
                                   String testName)
                                   throws Exception {
        byte[] singleByte = new byte[1];
        byte[] encoded = new byte[1];
        int lookupOffset = 0;

        for (int i=start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            String unicodeStr = new String (singleByte, "ISCII91");
            if (unicodeStr.charAt(0) != expectChars[lookupOffset++]) {
                throw new Exception ("Failed ISCII91 Regression Test");
            }
            encoded = unicodeStr.getBytes("ISCII");
        }
        return;
    }

    /*
     * Tests the ISCII91 Indic character encoding
     * as per IS 13194:1991 Bureau of Indian Standards.
     */

    private static void test () throws Exception {

        try {



            mapEquiv(0, 0x7f, "7 bit ASCII range");


            checkUnmapped(0x81, 0x9f, "UNMAPPED");


            byte[] testByte = new byte[1];
            char[] vowelModChars = {
                '\u0901', 
                '\u0902', 
                '\u0903'  
            };

            checkRange(0xa1, 0xa3, vowelModChars, "INDIC VOWEL MODIFIER CHARS");

            char[] expectChars = {
                '\u0905', 
                '\u0906', 
                '\u0907', 
                '\u0908', 
                '\u0909', 
                '\u090a', 
                '\u090b', 
                '\u090e', 
                '\u090f', 
                '\u0910', 
                '\u090d', 
                '\u0912', 
                '\u0913', 
                '\u0914', 
                '\u0911', 
            };

            checkRange(0xa4, 0xb2, expectChars, "INDIC VOWELS");

            char[] expectConsChars =
            {
                '\u0915', 
                '\u0916', 
                '\u0917', 
                '\u0918', 
                '\u0919', 
                '\u091a', 
                '\u091b', 
                '\u091c', 
                '\u091d', 
                '\u091e', 
                '\u091f', 
                '\u0920', 
                '\u0921', 
                '\u0922', 
                '\u0923', 
                '\u0924', 
                '\u0925', 
                '\u0926', 
                '\u0927', 
                '\u0928', 
                '\u0929', 
                '\u092a', 
                '\u092b', 
                '\u092c', 
                '\u092d', 
                '\u092e', 
                '\u092f', 
                '\u095f', 
                '\u0930', 
                '\u0931', 
                '\u0932', 
                '\u0933', 
                '\u0934', 
                '\u0935', 
                '\u0936', 
                '\u0937', 
                '\u0938', 
                '\u0939', 
            };

            checkRange(0xb3, 0xd8, expectConsChars, "INDIC CONSONANTS");

            char[] matraChars = {
                '\u093e', 
                '\u093f', 
                '\u0940', 
                '\u0941', 
                '\u0942', 
                '\u0943', 
                '\u0946', 
                '\u0947', 
                '\u0948', 
                '\u0945', 
                '\u094a', 
                '\u094b', 
                '\u094c', 
                '\u0949' 
            };


            checkRange(0xda, 0xe7, matraChars, "INDIC MATRAS");

            char[] loneContextModifierChars = {
            '\u094d', 
            '\u093c', 
            '\u0964' 
            };

            checkRange(0xe8, 0xea,
                       loneContextModifierChars, "LONE INDIC CONTEXT CHARS");



            char[] expectNumeralChars =
            {
                '\u0966', 
                '\u0967', 
                '\u0968', 
                '\u0969', 
                '\u096a', 
                '\u096b', 
                '\u096c', 
                '\u096d', 
                '\u096e', 
                '\u096f'  
            };

            checkRange(0xf1, 0xfa,
                       expectNumeralChars, "NUMERAL/DIGIT CHARACTERS");
            int lookupOffset = 0;

            char[] expectNuktaSub = {
                '\u0950',
                '\u090c',
                '\u0961',
                '\u0960',
                '\u0962',
                '\u0963',
                '\u0944',
                '\u093d'
            };

            /*
             * ISCII uses a number of code extension techniques
             * to access a number of lesser used characters.
             * The Nukta character which ordinarily signifies
             * a diacritic is used in combination with existing
             * characters to escape them to a different character.
             * value.
            */

            byte[] codeExtensionBytes = {
                (byte)0xa1 , (byte)0xe9, 
                (byte)0xa6 , (byte)0xe9, 
                (byte)0xa7 , (byte)0xe9, 
                (byte)0xaa , (byte)0xe9, 
                (byte)0xdb , (byte)0xe9, 
                (byte)0xdc , (byte)0xe9, 

                (byte)0xdf , (byte)0xe9, 
                (byte)0xea , (byte)0xe9  
            };

            lookupOffset = 0;
            byte[] bytePair = new byte[2];

            for (int i=0; i < (codeExtensionBytes.length)/2; i++ ) {
                bytePair[0] = (byte) codeExtensionBytes[lookupOffset++];
                bytePair[1] = (byte) codeExtensionBytes[lookupOffset++];

                String unicodeStr = new String (bytePair,"ISCII91");
                if (unicodeStr.charAt(0) != expectNuktaSub[i]) {
                    throw new Exception("Failed Nukta Sub");
                }
            }

            lookupOffset = 0;
            byte[] comboBytes = {
                (byte)0xe8 , (byte)0xe8, 
                (byte)0xe8 , (byte)0xe9  
            };
            char[] expectCombChars = {
                '\u094d',
                '\u200c',
                '\u094d',
                '\u200d'
            };

            for (int i=0; i < (comboBytes.length)/2; i++ ) {
                bytePair[0] = (byte) comboBytes[lookupOffset++];
                bytePair[1] = (byte) comboBytes[lookupOffset];
                String unicodeStr = new String (bytePair, "ISCII91");
                if (unicodeStr.charAt(0) != expectCombChars[lookupOffset-1]
                    && unicodeStr.charAt(1) != expectCombChars[lookupOffset]) {
                    throw new Exception("Failed ISCII91 Regression Test");
                }
                lookupOffset++;
            }

        } catch (UnsupportedEncodingException e) {
             System.err.println ("ISCII91 encoding not supported");
             throw new Exception ("Failed ISCII91 Regression Test");
        }
    }

    public static void main (String[] args) throws Exception {
        test();
    }
}
