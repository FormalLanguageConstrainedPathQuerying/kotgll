/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4473201 4696726 4652234 4482298 4784385 4966197 4267354 5015668
        6911753 8071447 8186751 8242541 8260265 8301119
 * @summary Check that registered charsets are actually registered
 * @modules jdk.charsets
 * @run junit RegisteredCharsets
 * @run junit/othervm -Djdk.charset.GB18030=2000 RegisteredCharsets
 */

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegisteredCharsets {

    @Test
    public void defaultCharsetTest() {
        assertThrows(UnsupportedCharsetException.class,
                () -> Charset.forName("default"));
    }

    /**
     * Tests that the aliases of the input String convert
     * to the same Charset. This is validated by ensuring the input String
     * and Charset.name() values are equal.
     */
    @ParameterizedTest
    @MethodSource("aliases")
    public void testAliases(String canonicalName, String[] aliasNames) {
        for (String aliasName : aliasNames) {
            Charset cs = Charset.forName(aliasName);
            assertEquals(cs.name(), canonicalName);
        }
    }

    /**
     * Tests charsets to ensure that they are registered in the
     * IANA Charset Registry.
     */
    @ParameterizedTest
    @MethodSource("ianaRegistered")
    public void registeredTest(String cs) throws Exception {
        check(cs, true);
    }

    /**
     * Tests charsets to ensure that they are NOT registered in the
     * IANA Charset Registry.
     */
    @ParameterizedTest
    @MethodSource("ianaUnregistered")
    public void unregisteredTest(String cs) throws Exception {
        check(cs, false);
    }

    /**
     * Helper method which checks if a charset is registered and whether
     * it should be.
     */
    static void check(String csn, boolean testRegistered) throws Exception {
        if (!Charset.forName(csn).isRegistered() && testRegistered) {
            throw new Exception("Not registered: " + csn);
        }
        else if (Charset.forName(csn).isRegistered() && !testRegistered) {
            throw new Exception("Registered: " + csn + "should be unregistered");
        }
    }

    private static Stream<String> ianaRegistered() {
        return Stream.of(
                "US-ASCII", "UTF8", "Big5", "EUC-JP",
                "GBK", "GB18030", "ISO-2022-KR", "ISO-2022-JP",
                "GB2312",  
                "ISO-8859-1", "ISO-8859-2", "ISO-8859-3",
                "ISO-8859-4", "ISO-8859-5", "ISO-8859-6",
                "ISO-8859-7", "ISO-8859-8", "ISO-8859-9",
                "ISO-8859-13", "ISO-8859-15", "ISO-8859-16",
                "windows-1251",
                "windows-1252", "windows-1253", "windows-1254",
                "windows-1255", "windows-1256", "windows-31j",
                "Shift_JIS", "JIS_X0201", "JIS_X0212-1990",
                "TIS-620", "Big5-HKSCS",
                "ISO-2022-CN",
                "IBM850",
                "IBM852",
                "IBM855",
                "IBM857",
                "IBM860",
                "IBM861",
                "IBM862",
                "IBM863",
                "IBM864",
                "IBM865",
                "IBM866",
                "IBM868",
                "IBM869",
                "IBM437",
                "IBM775",
                "IBM037",
                "IBM1026",
                "IBM273",
                "IBM277",
                "IBM278",
                "IBM280",
                "IBM284",
                "IBM285",
                "IBM297",
                "IBM420",
                "IBM424",
                "IBM500",
                "IBM-Thai",
                "IBM870",
                "IBM871",
                "IBM918",
                "IBM1047",
                "IBM01140",
                "IBM01141",
                "IBM01142",
                "IBM01143",
                "IBM01144",
                "IBM01145",
                "IBM01146",
                "IBM01147",
                "IBM01148",
                "IBM01149",
                "IBM00858"
        );
    }

    private static Stream<String> ianaUnregistered() {
        return Stream.of(
                "x-EUC-TW", "x-ISCII91",
                "x-windows-949", "x-windows-950",
                "x-mswin-936", "x-JIS0208",
                "x-ISO-8859-11",
                "x-windows-874",
                "x-PCK", "x-JISAutoDetect", "x-Johab",
                "x-MS950-HKSCS",
                "x-Big5-Solaris",
                "x-ISO-2022-CN-CNS",
                "x-ISO-2022-CN-GB",
                "x-MacArabic",
                "x-MacCentralEurope",
                "x-MacCroatian",
                "x-MacCyrillic",
                "x-MacDingbat",
                "x-MacGreek",
                "x-MacHebrew",
                "x-MacIceland",
                "x-MacRoman",
                "x-MacRomania",
                "x-MacSymbol",
                "x-MacThai",
                "x-MacTurkish",
                "x-MacUkraine",
                "x-IBM942",
                "x-IBM942C",
                "x-IBM943",
                "x-IBM943C",
                "x-IBM948",
                "x-IBM950",
                "x-IBM930",
                "x-IBM935",
                "x-IBM937",
                "x-IBM856",
                "x-IBM874",
                "x-IBM737",
                "x-IBM1006",
                "x-IBM1046",
                "x-IBM1098",
                "x-IBM1025",
                "x-IBM1112",
                "x-IBM1122",
                "x-IBM1123",
                "x-IBM1124",
                "x-IBM1129",
                "x-IBM1166",
                "x-IBM875",
                "x-IBM921",
                "x-IBM922",
                "x-IBM1097",
                "x-IBM949",
                "x-IBM949C",
                "x-IBM939",
                "x-IBM933",
                "x-IBM1381",
                "x-IBM1383",
                "x-IBM970",
                "x-IBM964",
                "x-IBM33722",
                "x-IBM1006",
                "x-IBM1046",
                "x-IBM1097",
                "x-IBM1098",
                "x-IBM1112",
                "x-IBM1122",
                "x-IBM1123",
                "x-IBM1124",
                "x-IBM33722",
                "x-IBM737",
                "x-IBM856",
                "x-IBM874",
                "x-IBM875",
                "x-IBM922",
                "x-IBM933",
                "x-IBM964"
        );
    }

    private static Stream<Arguments> aliases() {
        return Stream.of(
            Arguments.of("US-ASCII",
                    new String[] {"ascii","ANSI_X3.4-1968",
                    "iso-ir-6","ANSI_X3.4-1986", "ISO_646.irv:1991",
                    "ASCII", "ISO646-US","us","IBM367","cp367",
                    "csASCII"}),

            Arguments.of("UTF-8",
                    new String[] {
                        "UTF8",
                        "unicode-1-1-utf-8"
                    }),

            Arguments.of("UTF-16",
                    new String[] {
                        "UTF_16",
                        "utf16"
                    }),

            Arguments.of("UTF-16BE",
                    new String[] {
                        "UTF_16BE",
                        "ISO-10646-UCS-2",
                        "X-UTF-16BE",
                        "UnicodeBigUnmarked"
                    }),

            Arguments.of("UTF-16LE",
                    new String[] {
                        "UTF_16LE",
                        "X-UTF-16LE",
                        "UnicodeLittleUnmarked"
                    }),

            Arguments.of("Big5",
                    new String[] {
                        "csBig5"
                    }),

            Arguments.of("Big5-HKSCS",
                    new String[] {
                        "Big5_HKSCS",
                        "big5hk",
                        "big5-hkscs",
                        "big5hkscs"
                    }),

            Arguments.of("x-MS950-HKSCS",
                    new String[] {
                        "MS950_HKSCS"
                    }),

            Arguments.of("GB18030",
                    "2000".equals(System.getProperty("jdk.charset.GB18030")) ?
                    new String[] {
                        "gb18030-2000"
                    } :
                    new String[] {
                        "gb18030-2022"
                    }),

            Arguments.of("ISO-2022-KR", new String[] {"csISO2022KR"}),
            Arguments.of("ISO-2022-JP", new String[] {"csISO2022JP"}),
            Arguments.of("EUC-KR", new String[] { "csEUCKR"}),
            Arguments.of("ISO-8859-1",
                    new String[] {

                        "iso-ir-100",
                        "ISO_8859-1",
                        "latin1",
                        "l1",
                        "IBM819",
                        "cp819",
                        "csISOLatin1",

                        "819",
                        "IBM-819",
                        "ISO8859_1",
                        "ISO_8859-1:1987",
                        "ISO_8859_1",
                        "8859_1",
                        "ISO8859-1",

                    }),

            Arguments.of("ISO-8859-2",
                new String[] {
                    "ISO_8859-2",
                    "ISO_8859-2:1987",
                    "iso-ir-101",
                    "latin2",
                    "l2",
                    "8859_2",
                    "iso_8859-2:1987",
                    "iso8859-2",
                    "ibm912",
                    "ibm-912",
                    "cp912",
                    "912",
                    "csISOLatin2"}),

            Arguments.of("ISO-8859-3",
                    new String[] {"latin3",
                    "ISO_8859-3:1988",
                    "iso-ir-109",
                    "l3",
                    "8859_3",
                    "iso_8859-3:1988",
                    "iso8859-3",
                    "ibm913",
                    "ibm-913",
                    "cp913",
                    "913",
                    "csISOLatin3"}),

            Arguments.of("ISO-8859-4",
                    new String[] {"csISOLatin4",
                        "ISO_8859-4:1988",
                        "iso-ir-110",
                        "latin4",
                        "8859_4",
                        "iso_8859-4:1988",
                        "iso8859-4",
                        "ibm914",
                        "ibm-914",
                        "cp914",
                        "914",
                        "l4"}),

            Arguments.of("ISO-8859-5",
                    new String[] {
                        "iso8859_5", 
                        "8859_5",
                        "iso-ir-144",
                        "ISO_8859-5",
                        "ISO_8859-5:1988",
                        "ISO8859-5",
                        "cyrillic",
                        "ibm915",
                        "ibm-915",
                        "915",
                        "cp915",
                        "csISOLatinCyrillic"
                    }),

            Arguments.of("ISO-8859-6",
                    new String[] {"ISO_8859-6:1987",
                    "iso-ir-127",
                    "ISO_8859-6",
                    "ECMA-114",
                    "ASMO-708",
                    "arabic",
                    "8859_6",
                    "iso_8859-6:1987",
                    "iso8859-6",
                    "ibm1089",
                    "ibm-1089",
                    "cp1089",
                    "1089",
                    "csISOLatinArabic"}),

            Arguments.of("ISO-8859-7",
                    new String[] {"ISO_8859-7:1987",
                    "iso-ir-126",
                    "ISO_8859-7",
                    "ELOT_928",
                    "ECMA-118",
                    "greek",
                    "greek8",
                    "8859_7",
                    "iso_8859-7:1987",
                    "iso8859-7",
                    "ibm813",
                    "ibm-813",
                    "cp813",
                    "813",
                    "csISOLatinGreek"}),

            Arguments.of("ISO-8859-8",
                    new String[] {
                    "ISO_8859-8:1988",
                    "iso-ir-138",
                    "ISO_8859-8",
                    "hebrew",
                    "8859_8",
                    "iso_8859-8:1988",
                    "iso8859-8",
                    "ibm916",
                    "ibm-916",
                    "cp916",
                    "916",
                    "csISOLatinHebrew"}),

            Arguments.of("ISO-8859-9",
                    new String[] {"ISO_8859-9:1989",
                    "iso-ir-148",
                    "ISO_8859-9",
                    "latin5",
                    "l5",
                    "8859_9",
                    "iso8859-9",
                    "ibm920",
                    "ibm-920",
                    "cp920",
                    "920",
                    "csISOLatin5"}),

            Arguments.of("ISO-8859-13",
                    new String[] {
                        "iso8859_13", 
                        "iso_8859-13",
                        "8859_13",
                        "ISO8859-13"
                    }),

            Arguments.of("ISO-8859-15",
                    new String[] {
                        "ISO_8859-15",
                        "Latin-9",
                        "csISO885915",
                        "8859_15",
                        "ISO-8859-15",
                        "ISO_8859-15",
                        "ISO8859-15",
                        "ISO8859_15",
                        "IBM923",
                        "IBM-923",
                        "cp923",
                        "923",
                        "LATIN0",
                        "LATIN9",
                        "L9",
                        "csISOlatin0",
                        "csISOlatin9",
                        "ISO8859_15_FDIS"
                    }),

            Arguments.of("ISO-8859-16",
                    new String[] {
                        "iso-ir-226",
                        "ISO_8859-16:2001",
                        "ISO_8859-16",
                        "ISO8859_16",
                        "latin10",
                        "l10",
                        "csISO885916"
                       }),

            Arguments.of("JIS_X0212-1990",
                    new String[] {
                    "iso-ir-159",
                    "csISO159JISX02121990"}),

            Arguments.of("JIS_X0201",
                    new String[]{
                    "X0201",
                    "csHalfWidthKatakana"}),

            Arguments.of("KOI8-R",
                    new String[] {
                    "KOI8_R",
                    "csKOI8R"}),

            Arguments.of("GBK",
                    new String[] {
                    "windows-936"}),

            Arguments.of("Shift_JIS",
                    new String[] {
                    "MS_Kanji",
                    "csShiftJIS"}),

            Arguments.of("EUC-JP",
                    new String[] {
                    "Extended_UNIX_Code_Packed_Format_for_Japanese",
                    "csEUCPkdFmtJapanese"}),

            Arguments.of("Big5", new String[] {"csBig5"}),

            Arguments.of("windows-31j", new String[] {"csWindows31J"}),

            Arguments.of("x-iso-8859-11",
                        new String[] { "iso-8859-11", "iso8859_11" }),

            Arguments.of("windows-1250",
                    new String[] {
                        "cp1250",
                        "cp5346"
                    }),

            Arguments.of("windows-1251",
                    new String[] {
                        "cp1251",
                        "cp5347",
                        "ansi-1251"
                    }),

            Arguments.of("windows-1252",
                    new String[] {
                        "cp1252",
                        "cp5348"
                    }),

            Arguments.of("windows-1253",
                    new String[] {
                        "cp1253",
                        "cp5349"
                    }),

            Arguments.of("windows-1254",
                    new String[] {
                        "cp1254",
                        "cp5350"
                    }),

            Arguments.of("windows-1255",
                    new String[] {
                        "cp1255"
                    }),

            Arguments.of("windows-1256",
                    new String[] {
                        "cp1256"
                    }),

            Arguments.of("windows-1257",
                    new String[] {
                        "cp1257",
                        "cp5353"
                    }),

            Arguments.of("windows-1258",
                    new String[] {
                        "cp1258"
                    }),

            Arguments.of("x-windows-874",
                    new String[] {
                        "ms874", "ms-874", "windows-874" }),

            Arguments.of("GB2312",
                    new String[] {
                        "x-EUC-CN",
                        "gb2312-80",
                        "gb2312-1980",
                        "euc-cn",
                        "euccn" }),

            Arguments.of("x-IBM942" ,
                    new String[] {
                        "cp942", 
                        "ibm942",
                        "ibm-942",
                        "942"
                    }),

            Arguments.of("x-IBM942C" ,
                    new String[] {
                        "cp942C", 
                        "ibm942C",
                        "ibm-942C",
                        "942C"
                    } ),

            Arguments.of("x-IBM943" ,
                    new String[] {
                        "cp943", 
                        "ibm943",
                        "ibm-943",
                        "943"
                    } ),

            Arguments.of("x-IBM943C" ,
                    new String[] {
                        "cp943c", 
                        "ibm943C",
                        "ibm-943C",
                        "943C"
                    } ),

            Arguments.of("x-IBM948" ,
                    new String[] {
                        "cp948", 
                        "ibm948",
                        "ibm-948",
                        "948"
                    } ),

            Arguments.of("x-IBM950" ,
                    new String[] {
                        "cp950", 
                        "ibm950",
                        "ibm-950",
                        "950"
                    } ),

            Arguments.of("x-IBM930" ,
                    new String[] {
                        "cp930", 
                        "ibm930",
                        "ibm-930",
                        "930"
                    } ),

            Arguments.of("x-IBM935" ,
                    new String[] {
                        "cp935", 
                        "ibm935",
                        "ibm-935",
                        "935"
                    } ),

            Arguments.of("x-IBM937" ,
                    new String[] {
                        "cp937", 
                        "ibm937",
                        "ibm-937",
                        "937"
                    } ),

            Arguments.of("IBM850" ,
                    new String[] {
                        "cp850", 
                        "ibm-850",
                        "ibm850",
                        "850",
                        "cspc850multilingual"
                    } ),

            Arguments.of("IBM852" ,
                    new String[] {
                        "cp852", 
                        "ibm852",
                        "ibm-852",
                        "852",
                        "csPCp852"
                    } ),

            Arguments.of("IBM855" ,
                    new String[] {
                        "cp855", 
                        "ibm-855",
                        "ibm855",
                        "855",
                        "cspcp855"
                    } ),

            Arguments.of("x-IBM856" ,
                    new String[] {
                        "cp856", 
                        "ibm-856",
                        "ibm856",
                        "856"
                    } ),

            Arguments.of("IBM857" ,
                    new String[] {
                        "cp857", 
                        "ibm857",
                        "ibm-857",
                        "857",
                        "csIBM857"
                    } ),

            Arguments.of("IBM860" ,
                    new String[] {
                        "cp860", 
                        "ibm860",
                        "ibm-860",
                        "860",
                        "csIBM860"
                    } ),
            Arguments.of("IBM861" ,
                    new String[] {
                        "cp861", 
                        "ibm861",
                        "ibm-861",
                        "861",
                        "csIBM861"
                    } ),

            Arguments.of("IBM862" ,
                    new String[] {
                        "cp862", 
                        "ibm862",
                        "ibm-862",
                        "862",
                        "csIBM862"
                    } ),

            Arguments.of("IBM863" ,
                    new String[] {
                        "cp863", 
                        "ibm863",
                        "ibm-863",
                        "863",
                        "csIBM863"
                    } ),

            Arguments.of("IBM864" ,
                    new String[] {
                        "cp864", 
                        "ibm864",
                        "ibm-864",
                        "864",
                        "csIBM864"
                    } ),

            Arguments.of("IBM865" ,
                    new String[] {
                        "cp865", 
                        "ibm865",
                        "ibm-865",
                        "865",
                        "csIBM865"
                    } ),

            Arguments.of("IBM866" , new String[] {
                        "cp866", 
                        "ibm866",
                        "ibm-866",
                        "866",
                        "csIBM866"
                    } ),
            Arguments.of("IBM868" ,
                    new String[] {
                        "cp868", 
                        "ibm868",
                        "ibm-868",
                        "868",
                        "cp-ar",
                        "csIBM868"
                    } ),

            Arguments.of("IBM869" ,
                    new String[] {
                        "cp869", 
                        "ibm869",
                        "ibm-869",
                        "869",
                        "cp-gr",
                        "csIBM869"
                    } ),

            Arguments.of("IBM437" ,
                    new String[] {
                        "cp437", 
                        "ibm437",
                        "ibm-437",
                        "437",
                        "cspc8codepage437",
                        "windows-437"
                    } ),

            Arguments.of("x-IBM874" ,
                    new String[] {
                        "cp874", 
                        "ibm874",
                        "ibm-874",
                        "874"
                    } ),
            Arguments.of("x-IBM737" ,
                    new String[] {
                        "cp737", 
                        "ibm737",
                        "ibm-737",
                        "737"
                    } ),

            Arguments.of("IBM775" ,
                    new String[] {
                        "cp775", 
                        "ibm775",
                        "ibm-775",
                        "775"
                    } ),

            Arguments.of("x-IBM921" ,
                    new String[] {
                        "cp921", 
                        "ibm921",
                        "ibm-921",
                        "921"
                    } ),

            Arguments.of("x-IBM1006" ,
                    new String[] {
                        "cp1006", 
                        "ibm1006",
                        "ibm-1006",
                        "1006"
                    } ),

            Arguments.of("x-IBM1046" ,
                    new String[] {
                        "cp1046", 
                        "ibm1046",
                        "ibm-1046",
                        "1046"
                    } ),

            Arguments.of("IBM1047" ,
                    new String[] {
                        "cp1047", 
                        "ibm-1047",
                        "1047"
                    } ),

            Arguments.of("x-IBM1098" ,
                    new String[] {
                        "cp1098", 
                        "ibm1098",
                        "ibm-1098",
                        "1098",
                    } ),

            Arguments.of("IBM037" ,
                    new String[] {
                        "cp037", 
                        "ibm037",
                        "csIBM037",
                        "cs-ebcdic-cp-us",
                        "cs-ebcdic-cp-ca",
                        "cs-ebcdic-cp-wt",
                        "cs-ebcdic-cp-nl",
                        "ibm-037",
                        "ibm-37",
                        "cpibm37",
                        "037"
                    } ),

            Arguments.of("x-IBM1025" ,
                    new String[] {
                        "cp1025", 
                        "ibm1025",
                        "ibm-1025",
                        "1025"
                    } ),

            Arguments.of("IBM1026" ,
                    new String[] {
                        "cp1026", 
                        "ibm1026",
                        "ibm-1026",
                        "1026"
                    } ),

            Arguments.of("x-IBM1112" ,
                    new String[] {
                        "cp1112", 
                        "ibm1112",
                        "ibm-1112",
                        "1112"
                    } ),

            Arguments.of("x-IBM1122" ,
                    new String[] {
                        "cp1122", 
                        "ibm1122",
                        "ibm-1122",
                        "1122"
                    } ),

            Arguments.of("x-IBM1123" ,
                    new String[] {
                        "cp1123", 
                        "ibm1123",
                        "ibm-1123",
                        "1123"
                    } ),

            Arguments.of("x-IBM1124" ,
                    new String[] {
                        "cp1124", 
                        "ibm1124",
                        "ibm-1124",
                        "1124"
                    } ),

            Arguments.of("x-IBM1129" ,
                    new String[] {
                        "cp1129", 
                        "ibm1129",
                        "ibm-1129",
                        "1129"
                    } ),

            Arguments.of("x-IBM1166" ,
                    new String[] {
                        "cp1166", 
                        "ibm1166",
                        "ibm-1166",
                        "1166"
                    } ),

            Arguments.of("IBM273" ,
                    new String[] {
                        "cp273", 
                        "ibm273",
                        "ibm-273",
                        "273"
                    } ),

            Arguments.of("IBM277" ,
                    new String[] {
                        "cp277", 
                        "ibm277",
                        "ibm-277",
                        "277"
                    } ),

            Arguments.of("IBM278" ,
                    new String[] {
                        "cp278", 
                        "ibm278",
                        "ibm-278",
                        "278",
                        "ebcdic-sv",
                        "ebcdic-cp-se",
                        "csIBM278"
                    } ),

            Arguments.of("IBM280" ,
                    new String[] {
                        "cp280", 
                        "ibm280",
                        "ibm-280",
                        "280"
                    } ),

            Arguments.of("IBM284" ,
                    new String[] {
                        "cp284", 
                        "ibm284",
                        "ibm-284",
                        "284",
                        "csIBM284",
                        "cpibm284"
                    } ),

            Arguments.of("IBM285" ,
                    new String[] {
                        "cp285", 
                        "ibm285",
                        "ibm-285",
                        "285",
                        "ebcdic-cp-gb",
                        "ebcdic-gb",
                        "csIBM285",
                        "cpibm285"
                    } ),

            Arguments.of("IBM297" ,
                    new String[] {
                        "cp297", 
                        "ibm297",
                        "ibm-297",
                        "297",
                        "ebcdic-cp-fr",
                        "cpibm297",
                        "csIBM297",
                    } ),

            Arguments.of("IBM420" ,
                    new String[] {
                        "cp420", 
                        "ibm420",
                        "ibm-420",
                        "ebcdic-cp-ar1",
                        "420",
                        "csIBM420"
                    } ),

            Arguments.of("IBM424" ,
                    new String[] {
                        "cp424", 
                        "ibm424",
                        "ibm-424",
                        "424",
                        "ebcdic-cp-he",
                        "csIBM424"
                    } ),

            Arguments.of("IBM500" ,
                    new String[] {
                        "cp500", 
                        "ibm500",
                        "ibm-500",
                        "500",
                        "ebcdic-cp-ch",
                        "ebcdic-cp-bh",
                        "csIBM500"
                    } ),

            Arguments.of("IBM-Thai" ,
                    new String[] {
                        "cp838", 
                        "ibm838",
                        "ibm-838",
                        "ibm838",
                        "838"
                    } ),

            Arguments.of("IBM870" ,
                    new String[] {
                        "cp870", 
                        "ibm870",
                        "ibm-870",
                        "870",
                        "ebcdic-cp-roece",
                        "ebcdic-cp-yu",
                        "csIBM870"
                    } ),

            Arguments.of("IBM871" ,
                    new String[] {
                        "cp871", 
                        "ibm871",
                        "ibm-871",
                        "871",
                        "ebcdic-cp-is",
                        "csIBM871"
                    } ),

            Arguments.of("x-IBM875" ,
                    new String[] {
                        "cp875", 
                        "ibm875",
                        "ibm-875",
                        "875"
                    } ),

            Arguments.of("IBM918" ,
                    new String[] {
                        "cp918", 
                        "ibm-918",
                        "918",
                        "ebcdic-cp-ar2"
                    } ),

            Arguments.of("x-IBM922" ,
                    new String[] {
                        "cp922", 
                        "ibm922",
                        "ibm-922",
                        "922"
                    } ),

            Arguments.of("x-IBM1097" ,
                    new String[] {
                        "cp1097", 
                        "ibm1097",
                        "ibm-1097",
                        "1097"
                    } ),

            Arguments.of("x-IBM949" ,
                    new String[] {
                        "cp949", 
                        "ibm949",
                        "ibm-949",
                        "949"
                    } ),

            Arguments.of("x-IBM949C" ,
                    new String[] {
                        "cp949C", 
                        "ibm949C",
                        "ibm-949C",
                        "949C"
                    } ),

            Arguments.of("x-IBM939" ,
                    new String[] {
                        "cp939", 
                        "ibm939",
                        "ibm-939",
                        "939"
                    } ),

            Arguments.of("x-IBM933" ,
                    new String[] {
                        "cp933", 
                        "ibm933",
                        "ibm-933",
                        "933"
                    } ),

            Arguments.of("x-IBM1381" ,
                    new String[] {
                        "cp1381", 
                        "ibm1381",
                        "ibm-1381",
                        "1381"
                    } ),

            Arguments.of("x-IBM1383" ,
                    new String[] {
                        "cp1383", 
                        "ibm1383",
                        "ibm-1383",
                        "1383"
                    } ),

            Arguments.of("x-IBM970" ,
                    new String[] {
                        "cp970", 
                        "ibm970",
                        "ibm-970",
                        "ibm-eucKR",
                        "970"
                    } ),

            Arguments.of("x-IBM964" ,
                    new String[] {
                        "cp964", 
                        "ibm964",
                        "ibm-964",
                        "964"
                    } ),

            Arguments.of("x-IBM33722" ,
                    new String[] {
                        "cp33722", 
                        "ibm33722",
                        "ibm-33722",
                        "ibm-5050", 
                        "ibm-33722_vascii_vpua", 
                        "33722"
                    } ),

            Arguments.of("IBM01140" ,
                    new String[] {
                        "cp1140", 
                        "ccsid01140",
                        "cp01140",
                    } ),

            Arguments.of("IBM01141" ,
                    new String[] {
                        "cp1141", 
                        "ccsid01141",
                        "cp01141",
                    } ),

            Arguments.of("IBM01142" ,
                    new String[] {
                        "cp1142", 
                        "ccsid01142",
                        "cp01142",
                    } ),

            Arguments.of("IBM01143" ,
                    new String[] {
                        "cp1143", 
                        "ccsid01143",
                        "cp01143",
                    } ),

            Arguments.of("IBM01144" ,
                    new String[] {
                        "cp1144", 
                        "ccsid01144",
                        "cp01144",
                    } ),

            Arguments.of("IBM01145" ,
                    new String[] {
                        "cp1145", 
                        "ccsid01145",
                        "cp01145",
                    } ),

            Arguments.of("IBM01146" ,
                    new String[] {
                        "cp1146", 
                        "ccsid01146",
                        "cp01146",
                    } ),

            Arguments.of("IBM01147" ,
                    new String[] {
                        "cp1147", 
                        "ccsid01147",
                        "cp01147",
                    } ),

            Arguments.of("IBM01148" ,
                    new String[] {
                        "cp1148", 
                        "ccsid01148",
                        "cp01148",
                    } ),

            Arguments.of("IBM01149" ,
                    new String[] {
                        "cp1149", 
                        "ccsid01149",
                        "cp01149",
                    } ),

            Arguments.of("IBM00858" ,
                    new String[] {
                        "cp858", 
                        "ccsid00858",
                        "cp00858",
                    } ),

            Arguments.of("x-MacRoman",
                    new String[] {
                        "MacRoman" 
                    }),

            Arguments.of("x-MacCentralEurope",
                    new String[] {
                        "MacCentralEurope" 
                    }),

            Arguments.of("x-MacCroatian",
                    new String[] {
                        "MacCroatian" 
                    }),


            Arguments.of("x-MacCroatian",
                    new String[] {
                        "MacCroatian" 
                    }),


            Arguments.of("x-MacGreek",
                    new String[] {
                        "MacGreek" 
                    }),

            Arguments.of("x-MacCyrillic",
                    new String[] {
                        "MacCyrillic" 
                    }),

            Arguments.of("x-MacUkraine",
                    new String[] {
                        "MacUkraine" 
                    }),

            Arguments.of("x-MacTurkish",
                    new String[] {
                        "MacTurkish" 
                    }),

            Arguments.of("x-MacArabic",
                    new String[] {
                        "MacArabic" 
                    }),

            Arguments.of("x-MacHebrew",
                    new String[] {
                        "MacHebrew" 
                    }),

            Arguments.of("x-MacIceland",
                    new String[] {
                        "MacIceland" 
                    }),

            Arguments.of("x-MacRomania",
                    new String[] {
                        "MacRomania" 
                    }),

            Arguments.of("x-MacThai",
                    new String[] {
                        "MacThai" 
                    }),

            Arguments.of("x-MacSymbol",
                    new String[] {
                        "MacSymbol" 
                    }),

            Arguments.of("x-MacDingbat",
                    new String[] {
                        "MacDingbat" 
                    })
        );
    }
}
