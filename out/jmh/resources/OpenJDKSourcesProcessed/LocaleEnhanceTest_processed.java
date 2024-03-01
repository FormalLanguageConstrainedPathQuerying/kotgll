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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @test
 * @bug 6875847 6992272 7002320 7015500 7023613 7032820 7033504 7004603
 *    7044019 8008577 8176853 8255086 8263202 8287868
 * @summary test API changes to Locale
 * @modules jdk.localedata
 * @compile LocaleEnhanceTest.java
 * @run junit/othervm -Djava.locale.providers=JRE,SPI -esa LocaleEnhanceTest
 */
public class LocaleEnhanceTest {

    public LocaleEnhanceTest() {
    }


    /** A canonical language code. */
    private static final String l = "en";

    /** A canonical script code.. */
    private static final String s = "Latn";

    /** A canonical region code. */
    private static final String c = "US";

    /** A canonical variant code. */
    private static final String v = "NewYork";

    /**
     * Ensure that Builder builds locales that have the expected
     * tag and java6 ID.  Note the odd cases for the ID.
     */
    @Test
    public void testCreateLocaleCanonicalValid() {
        String[] valids = {
            "en-Latn-US-NewYork", "en_US_NewYork_#Latn",
            "en-Latn-US", "en_US_#Latn",
            "en-Latn-NewYork", "en__NewYork_#Latn", 
            "en-Latn", "en__#Latn", 
            "en-US-NewYork", "en_US_NewYork",
            "en-US", "en_US",
            "en-NewYork", "en__NewYork", 
            "en", "en",
            "und-Latn-US-NewYork", "_US_NewYork_#Latn",
            "und-Latn-US", "_US_#Latn",
            "und-Latn-NewYork", "", 
            "und-Latn", "",
            "und-US-NewYork", "_US_NewYork",
            "und-US", "_US",
            "und-NewYork", "", 
            "und", ""
        };

        Builder builder = new Builder();

        for (int i = 0; i < valids.length; i += 2) {
            String tag = valids[i];
            String id = valids[i+1];

            String idl = (i & 16) == 0 ? l : "";
            String ids = (i & 8) == 0 ? s : "";
            String idc = (i & 4) == 0 ? c : "";
            String idv = (i & 2) == 0 ? v : "";

            String msg = String.valueOf(i/2) + ": '" + tag + "' ";

            try {
                Locale l = builder
                    .setLanguage(idl)
                    .setScript(ids)
                    .setRegion(idc)
                    .setVariant(idv)
                    .build();
                assertEquals(msg + "language", idl, l.getLanguage());
                assertEquals(msg + "script", ids, l.getScript());
                assertEquals(msg + "country", idc, l.getCountry());
                assertEquals(msg + "variant", idv, l.getVariant());
                assertEquals(msg + "tag", tag, l.toLanguageTag());
                assertEquals(msg + "id", id, l.toString());
            }
            catch (IllegalArgumentException e) {
                fail(msg + e.getMessage());
            }
        }
    }

    /**
     * Test that locale construction works with 'multiple variants'.
     * <p>
     * The string "Newer__Yorker" is treated as three subtags,
     * "Newer", "", and "Yorker", and concatenated into one
     * subtag by omitting empty subtags and joining the remainer
     * with underscores.  So the resulting variant tag is "Newer_Yorker".
     * Note that 'New' and 'York' are invalid BCP47 variant subtags
     * because they are too short.
     */
    @Test
    public void testCreateLocaleMultipleVariants() {

        String[] valids = {
            "en-Latn-US-Newer-Yorker",  "en_US_Newer_Yorker_#Latn",
            "en-Latn-Newer-Yorker",     "en__Newer_Yorker_#Latn",
            "en-US-Newer-Yorker",       "en_US_Newer_Yorker",
            "en-Newer-Yorker",          "en__Newer_Yorker",
            "und-Latn-US-Newer-Yorker", "_US_Newer_Yorker_#Latn",
            "und-Latn-Newer-Yorker",    "",
            "und-US-Newer-Yorker",      "_US_Newer_Yorker",
            "und-Newer-Yorker",         "",
        };

        Builder builder = new Builder(); 

        final String idv = "Newer_Yorker";
        for (int i = 0; i < valids.length; i += 2) {
            String tag = valids[i];
            String id = valids[i+1];

            String idl = (i & 8) == 0 ? l : "";
            String ids = (i & 4) == 0 ? s : "";
            String idc = (i & 2) == 0 ? c : "";

            String msg = String.valueOf(i/2) + ": " + tag + " ";
            try {
                Locale l = builder
                    .setLanguage(idl)
                    .setScript(ids)
                    .setRegion(idc)
                    .setVariant(idv)
                    .build();

                assertEquals(msg + " language", idl, l.getLanguage());
                assertEquals(msg + " script", ids, l.getScript());
                assertEquals(msg + " country", idc, l.getCountry());
                assertEquals(msg + " variant", idv, l.getVariant());

                assertEquals(msg + "tag", tag, l.toLanguageTag());
                assertEquals(msg + "id", id, l.toString());
            }
            catch (IllegalArgumentException e) {
                fail(msg + e.getMessage());
            }
        }
    }

    /**
     * Ensure that all these invalid formats are not recognized by
     * forLanguageTag.
     */
    @Test
    public void testCreateLocaleCanonicalInvalidSeparator() {
        String[] invalids = {
            "en_Latn_US_NewYork_",
            "en_Latn_US_",
            "en_Latn_",
            "en_",
            "_",

            "en_Latn_US__NewYork",
            "_Latn_US__NewYork",
            "en_US__NewYork",
            "_US__NewYork",


            "__US",
            "__NewYork",

            "en___NewYork",
            "en_Latn___NewYork",
            "_Latn___NewYork",
            "___NewYork",
        };

        for (int i = 0; i < invalids.length; ++i) {
            String id = invalids[i];
            Locale l = Locale.forLanguageTag(id);
            assertEquals(id, "und", l.toLanguageTag());
        }
    }

    /**
     * Ensure that all current locale ids parse.  Use DateFormat as a proxy
     * for all current locale ids.
     */
    @Test
    public void testCurrentLocales() {
        Locale[] locales = java.text.DateFormat.getAvailableLocales();
        Builder builder = new Builder();

        for (Locale target : locales) {
            String tag = target.toLanguageTag();

            Locale tagResult = Locale.forLanguageTag(tag);
            if (!target.getVariant().equals("NY")) {
                assertEquals("tagResult", target, tagResult);
            }

            Locale builderResult = builder.setLocale(target).build();
            if (target.getVariant().length() != 2) {
                assertEquals("builderResult", target, builderResult);
            }
        }
    }

    /**
     * Ensure that all icu locale ids parse.
     */
    @Test
    public void testIcuLocales() throws Exception {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                LocaleEnhanceTest.class.getResourceAsStream("icuLocales.txt"),
                "UTF-8"));
        String id = null;
        while (null != (id = br.readLine())) {
            Locale result = Locale.forLanguageTag(id);
            assertEquals("ulocale", id, result.toLanguageTag());
        }
    }


    @Test
    public void testConstructor() {
        String[][] tests = {
            { "X", "y", "z", "x", "Y" },
            { "xXxXxXxXxXxX", "yYyYyYyYyYyYyYyY", "zZzZzZzZzZzZzZzZ",
              "xxxxxxxxxxxx", "YYYYYYYYYYYYYYYY" },
            { "he", "IL", "", "he" },
            { "iw", "IL", "", "he" },
            { "yi", "DE", "", "yi" },
            { "ji", "DE", "", "yi" },
            { "id", "ID", "", "id" },
            { "in", "ID", "", "id" },
            { "ja", "JP", "JP" },
            { "th", "TH", "TH" },
            { "no", "NO", "NY" },
            { "no", "NO", "NY" },
            { "eng", "US", "" }
        };
        for (int i = 0; i < tests.length; ++ i) {
            String[] test = tests[i];
            String id = String.valueOf(i);
            Locale locale = Locale.of(test[0], test[1], test[2]);
            assertEquals(id + " lang", test.length > 3 ? test[3] : test[0], locale.getLanguage());
            assertEquals(id + " region", test.length > 4 ? test[4] : test[1], locale.getCountry());
            assertEquals(id + " variant", test.length > 5 ? test[5] : test[2], locale.getVariant());
        }
    }


    @Test
    public void testGetScript() {
        Locale locale = Locale.forLanguageTag("und-latn");
        assertEquals("forLanguageTag", "Latn", locale.getScript());

        locale = new Builder().setScript("LATN").build();
        assertEquals("builder", "Latn", locale.getScript());

        locale = Locale.forLanguageTag("und");
        assertEquals("script is empty string", "", locale.getScript());
    }

    @Test
    public void testGetExtension() {
        Locale locale = Locale.forLanguageTag("und-a-some_ex-tension");
        assertEquals("some_ex-tension", null, locale.getExtension('a'));

        locale = new Builder().setExtension('a', "some-ex-tension").build();
        assertEquals("builder", "some-ex-tension", locale.getExtension('a'));

        assertEquals("empty b", null, locale.getExtension('b'));

        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getExtension('\uD800'); }};

        locale = Locale.forLanguageTag("x-y-z-blork");
        assertEquals("x", "y-z-blork", locale.getExtension('x'));
    }

    @Test
    public void testGetExtensionKeys() {
        Locale locale = Locale.forLanguageTag("und-a-xx-yy-b-zz-ww");
        Set<Character> result = locale.getExtensionKeys();
        assertEquals("result size", 2, result.size());
        assertTrue("'a','b'", result.contains('a') && result.contains('b'));

        try {
            result.add('x');
            fail("expected exception on add to extension key set");
        }
        catch (UnsupportedOperationException e) {
        }

        locale = Locale.forLanguageTag("und");
        assertTrue("empty result", locale.getExtensionKeys().isEmpty());
    }

    @Test
    public void testGetUnicodeLocaleAttributes() {
        Locale locale = Locale.forLanguageTag("en-US-u-abc-def");
        Set<String> attributes = locale.getUnicodeLocaleAttributes();
        assertEquals("number of attributes", 2, attributes.size());
        assertTrue("attribute abc", attributes.contains("abc"));
        assertTrue("attribute def", attributes.contains("def"));

        locale = Locale.forLanguageTag("en-US-u-ca-gregory");
        attributes = locale.getUnicodeLocaleAttributes();
        assertTrue("empty attributes", attributes.isEmpty());
    }

    @Test
    public void testGetUnicodeLocaleType() {
        Locale locale = Locale.forLanguageTag("und-u-co-japanese-nu-thai");
        assertEquals("collation", "japanese", locale.getUnicodeLocaleType("co"));
        assertEquals("numbers", "thai", locale.getUnicodeLocaleType("nu"));

        assertEquals("key case", "japanese", locale.getUnicodeLocaleType("Co"));

        assertEquals("locale keyword not present", null, locale.getUnicodeLocaleType("xx"));

        locale = Locale.forLanguageTag("und");
        assertEquals("locale extension not present", null, locale.getUnicodeLocaleType("co"));

        locale = Locale.forLanguageTag("und-u-kn");
        assertEquals("typeless keyword", "", locale.getUnicodeLocaleType("kn"));

        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType("q"); }};
        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType("abcdefghi"); }};

        new ExpectNPE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType(null); }};
    }

    @Test
    public void testGetUnicodeLocaleKeys() {
        Locale locale = Locale.forLanguageTag("und-u-co-japanese-nu-thai");
        Set<String> result = locale.getUnicodeLocaleKeys();
        assertEquals("two keys", 2, result.size());
        assertTrue("co and nu", result.contains("co") && result.contains("nu"));

        try {
            result.add("frobozz");
            fail("expected exception when add to locale key set");
        }
        catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testPrivateUseExtension() {
        Locale locale = Locale.forLanguageTag("x-y-x-blork-");
        assertEquals("blork", "y-x-blork", locale.getExtension(Locale.PRIVATE_USE_EXTENSION));

        locale = Locale.forLanguageTag("und");
        assertEquals("no privateuse", null, locale.getExtension(Locale.PRIVATE_USE_EXTENSION));
    }

    @Test
    public void testToLanguageTag() {
        String[][] tests = {
            { "", "", "", "und" },
            { "", "", "NewYork", "und-NewYork" },
            { "", "Us", "", "und-US" },
            { "", "US", "NewYork", "und-US-NewYork" },
            { "EN", "", "", "en" },
            { "EN", "", "NewYork", "en-NewYork" },
            { "EN", "US", "", "en-US" },
            { "EN", "US", "NewYork", "en-US-NewYork" },
            { "en", "US", "Newer_Yorker", "en-US-Newer-Yorker" },
            { "en", "US", "new_yorker", "en-US-x-lvariant-new-yorker" },
            { "en", "US", "Windows_XP_Home", "en-US-Windows-x-lvariant-XP-Home" },
            { "en", "US", "WindowsVista_SP2", "en-US" },
            { "en", "USA", "", "en" },
            { "e", "US", "", "und-US" },
            { "Eng", "", "", "eng" },
            { "he", "IL", "", "he-IL" },
            { "iw", "IL", "", "he-IL" },
            { "yi", "DE", "", "yi-DE" },
            { "ji", "DE", "", "yi-DE" },
            { "id", "ID", "", "id-ID" },
            { "in", "ID", "", "id-ID" },
            { "ja", "JP", "JP", "ja-JP-u-ca-japanese-x-lvariant-JP" },
            { "th", "TH", "TH", "th-TH-u-nu-thai-x-lvariant-TH" },
            { "no", "NO", "NY", "nn-NO" }
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            Locale locale = Locale.of(test[0], test[1], test[2]);
            assertEquals("case " + i, test[3], locale.toLanguageTag());
        }

        String[][] tests1 = {
            { "EN-us", "en-US" },
            { "en-Latn-US", "en-Latn-US" },
            { "de-u-co-phonebk-ca-gregory", "de-u-ca-gregory-co-phonebk" },
            { "x-elmer", "x-elmer" },
            { "x-lvariant-JP", "x-lvariant-JP" },
        };
        for (String[] test : tests1) {
            Locale locale = Locale.forLanguageTag(test[0]);
            assertEquals("case " + test[0], test[1], locale.toLanguageTag());
        }

    }

    @Test
    public void testForLanguageTag() {

        String[][] tests = {
            { "x-abc", "x-abc" },
            { "x-a-b-c", "x-a-b-c" },
            { "x-a-12345678", "x-a-12345678" },

            { "i-ami", "ami" },
            { "i-bnn", "bnn" },
            { "i-hak", "hak" },
            { "i-klingon", "tlh" },
            { "i-lux", "lb" }, 
            { "i-navajo", "nv" }, 
            { "i-pwn", "pwn" },
            { "i-tao", "tao" },
            { "i-tay", "tay" },
            { "i-tsu", "tsu" },
            { "art-lojban", "jbo" },
            { "no-bok", "nb" },
            { "no-nyn", "nn" },
            { "sgn-BE-FR", "sfb" },
            { "sgn-BE-NL", "vgt" },
            { "sgn-CH-DE", "sgg" },
            { "zh-guoyu", "cmn" },
            { "zh-hakka", "hak" },
            { "zh-min-nan", "nan" },
            { "zh-xiang", "hsn" },

            { "i-default", "en-x-i-default" },
            { "i-enochian", "x-i-enochian" },
            { "i-mingo", "see-x-i-mingo" },
            { "en-GB-oed", "en-GB-x-oed" },
            { "zh-min", "nan-x-zh-min" },
            { "cel-gaulish", "xtg-x-cel-gaulish" },
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            Locale locale = Locale.forLanguageTag(test[0]);
            assertEquals("legacy language tag case " + i, test[1], locale.toLanguageTag());
        }

        tests = new String[][] {
            { "valid",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-12345678-z" },
            { "segment of private use tag too long",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-123456789-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y" },
            { "segment of private use tag is empty",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y--12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y" },
            { "first segment of private use tag is empty",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x--y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def" },
            { "illegal extension tag",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-\uD800-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def" },
            { "locale subtag with no value",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-x-y-12345678-z" },
            { "locale key subtag invalid",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-123456789-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc" },
            { "locale key subtag invalid in earlier position",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-123456789-abc-bb-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd" },
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            String msg = "syntax error case " + i + " " + test[0];
            try {
                Locale locale = Locale.forLanguageTag(test[1]);
                assertEquals(msg, test[2], locale.toLanguageTag());
            }
            catch (IllegalArgumentException e) {
                fail(msg + " caught exception: " + e);
            }
        }

        Locale locale = Locale.forLanguageTag("und-d-aa-00-bb-01-D-AA-10-cc-11-c-1234");
        assertEquals("extension", "aa-00-bb-01", locale.getExtension('d'));
        assertEquals("extension c", "1234", locale.getExtension('c'));

        locale = Locale.forLanguageTag("und-U-ca-gregory-u-ca-japanese");
        assertEquals("Unicode extension", "ca-gregory", locale.getExtension(Locale.UNICODE_LOCALE_EXTENSION));

        locale = Locale.forLanguageTag("und-u-aa-000-bb-001-bB-002-cc-003-c-1234");
        assertEquals("Unicode keywords", "aa-000-bb-001-cc-003", locale.getExtension(Locale.UNICODE_LOCALE_EXTENSION));
        assertEquals("Duplicated Unicode locake key followed by an extension", "1234", locale.getExtension('c'));
    }

    @Test
    public void testGetDisplayScript() {
        Locale latnLocale = Locale.forLanguageTag("und-latn");
        Locale hansLocale = Locale.forLanguageTag("und-hans");

        Locale oldLocale = Locale.getDefault();

        Locale.setDefault(Locale.US);
        assertEquals("latn US", "Latin", latnLocale.getDisplayScript());
        assertEquals("hans US", "Simplified", hansLocale.getDisplayScript());

        Locale.setDefault(Locale.GERMANY);
        assertEquals("latn DE", "Lateinisch", latnLocale.getDisplayScript());
        assertEquals("hans DE", "Vereinfacht", hansLocale.getDisplayScript());

        Locale.setDefault(oldLocale);
    }

    @Test
    public void testGetDisplayScriptWithLocale() {
        Locale latnLocale = Locale.forLanguageTag("und-latn");
        Locale hansLocale = Locale.forLanguageTag("und-hans");

        assertEquals("latn US", "Latin", latnLocale.getDisplayScript(Locale.US));
        assertEquals("hans US", "Simplified", hansLocale.getDisplayScript(Locale.US));

        assertEquals("latn DE", "Lateinisch", latnLocale.getDisplayScript(Locale.GERMANY));
        assertEquals("hans DE", "Vereinfacht", hansLocale.getDisplayScript(Locale.GERMANY));
    }

    @Test
    public void testGetDisplayName() {
        final Locale[] testLocales = {
                Locale.ROOT,
                Locale.ENGLISH,
                Locale.US,
                Locale.of("", "US"),
                Locale.of("no", "NO", "NY"),
                Locale.of("", "", "NY"),
                Locale.forLanguageTag("zh-Hans"),
                Locale.forLanguageTag("zh-Hant"),
                Locale.forLanguageTag("zh-Hans-CN"),
                Locale.forLanguageTag("und-Hans"),
        };

        final String[] displayNameEnglish = {
                "",
                "English",
                "English (United States)",
                "United States",
                "Norwegian (Norway,Nynorsk)",
                "Nynorsk",
                "Chinese (Simplified)",
                "Chinese (Traditional)",
                "Chinese (Simplified,China)",
                "Simplified",
        };

        final String[] displayNameSimplifiedChinese = {
                "",
                "\u82f1\u8bed",
                "\u82f1\u8bed (\u7f8e\u56fd)",
                "\u7f8e\u56fd",
                "\u632a\u5a01\u8bed (\u632a\u5a01,Nynorsk)",
                "Nynorsk",
                "\u4e2d\u6587 (\u7b80\u4f53)",
                "\u4e2d\u6587 (\u7e41\u4f53)",
                "\u4e2d\u6587 (\u7b80\u4f53,\u4e2d\u56fd)",
                "\u7b80\u4f53",
        };

        for (int i = 0; i < testLocales.length; i++) {
            Locale loc = testLocales[i];
            assertEquals("English display name for " + loc.toLanguageTag(),
                    displayNameEnglish[i], loc.getDisplayName(Locale.ENGLISH));
            assertEquals("Simplified Chinese display name for " + loc.toLanguageTag(),
                    displayNameSimplifiedChinese[i], loc.getDisplayName(Locale.CHINA));
        }
    }


    @Test
    public void testBuilderSetLocale() {
        Builder builder = new Builder();
        Builder lenientBuilder = new Builder();

        String languageTag = "en-Latn-US-NewYork-a-bb-ccc-u-co-japanese-x-y-z";
        String target = "en-Latn-US-NewYork-a-bb-ccc-u-co-japanese-x-y-z";

        Locale locale = Locale.forLanguageTag(languageTag);
        Locale result = lenientBuilder
            .setLocale(locale)
            .build();
        assertEquals("long tag", target, result.toLanguageTag());
        assertEquals("long tag", locale, result);

        new BuilderNPE("locale") {
            public void call() { b.setLocale(null); }
        };

        locale = builder.setLocale(Locale.of("ja", "JP", "JP")).build();
        assertEquals("ja_JP_JP languagetag", "ja-JP-u-ca-japanese", locale.toLanguageTag());
        assertEquals("ja_JP_JP variant", "", locale.getVariant());

        locale = builder.setLocale(Locale.of("th", "TH", "TH")).build();
        assertEquals("th_TH_TH languagetag", "th-TH-u-nu-thai", locale.toLanguageTag());
        assertEquals("th_TH_TH variant", "", locale.getVariant());

        locale = builder.setLocale(Locale.of("no", "NO", "NY")).build();
        assertEquals("no_NO_NY languagetag", "nn-NO", locale.toLanguageTag());
        assertEquals("no_NO_NY language", "nn", locale.getLanguage());
        assertEquals("no_NO_NY variant", "", locale.getVariant());

        new BuilderILE("123_4567_89") {
            public void call() {
                b.setLocale(Locale.of("123", "4567", "89"));
            }
        };
    }

    @Test
    public void testBuilderSetLanguageTag() {
        String source = "eN-LaTn-Us-NewYork-A-Xx-B-Yy-X-1-2-3";
        String target = "en-Latn-US-NewYork-a-xx-b-yy-x-1-2-3";
        Builder builder = new Builder();
        String result = builder
            .setLanguageTag(source)
            .build()
            .toLanguageTag();
        assertEquals("language", target, result);

        new BuilderILE() { public void call() { b.setLanguageTag("und-a-xx-yy-b-ww-A-00-11-c-vv"); }};

        new BuilderILE() { public void call() { b.setLanguageTag("und-u-nu-thai-NU-chinese-xx-1234"); }};
    }

    @Test
    public void testBuilderSetLanguage() {
        String source = "eN";
        String target = "en";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setLanguage(source)
            .build()
            .getLanguage();
        assertEquals("en", target, result);

        result = builder
            .setLanguage(target)
            .setLanguage("")
            .build()
            .getLanguage();
        assertEquals("empty", defaulted, result);

        result = builder
                .setLanguage(target)
                .setLanguage(null)
                .build()
                .getLanguage();
        assertEquals("null", defaulted, result);

        new BuilderILE("q", "abcdefghi", "13") { public void call() { b.setLanguage(arg); }};

        assertNotNull("2alpha", builder.setLanguage("zz").build());
        assertNotNull("8alpha", builder.setLanguage("abcdefgh").build());

        result = builder
            .setLanguage("eng")
            .build()
            .getLanguage();
        assertEquals("eng", "eng", result);
    }

    @Test
    public void testBuilderSetScript() {
        String source = "lAtN";
        String target = "Latn";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setScript(source)
            .build()
            .getScript();
        assertEquals("script", target, result);

        result = builder
            .setScript(target)
            .setScript("")
            .build()
            .getScript();
        assertEquals("empty", defaulted, result);

        result = builder
                .setScript(target)
                .setScript(null)
                .build()
                .getScript();
        assertEquals("null", defaulted, result);

        new BuilderILE("abc", "abcde", "l3tn") { public void call() { b.setScript(arg); }};

        assertEquals("4alpha", "Wxyz", builder.setScript("wxyz").build().getScript());
    }

    @Test
    public void testBuilderSetRegion() {
        String source = "uS";
        String target = "US";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setRegion(source)
            .build()
            .getCountry();
        assertEquals("us", target, result);

        result = builder
            .setRegion(target)
            .setRegion("")
            .build()
            .getCountry();
        assertEquals("empty", defaulted, result);

        result = builder
                .setRegion(target)
                .setRegion(null)
                .build()
                .getCountry();
        assertEquals("null", defaulted, result);

        new BuilderILE("q", "abc", "12", "1234", "a3", "12a") { public void call() { b.setRegion(arg); }};

        assertEquals("2alpha", "ZZ", builder.setRegion("ZZ").build().getCountry());
        assertEquals("3digit", "000", builder.setRegion("000").build().getCountry());
    }

    @Test
    public void testBuilderSetVariant() {
        String source = "NewYork";
        String target = source;
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setVariant(source)
            .build()
            .getVariant();
        assertEquals("NewYork", target, result);

        result = builder
            .setVariant("NeWeR_YoRkEr")
            .build()
            .toLanguageTag();
        assertEquals("newer yorker", "und-NeWeR-YoRkEr", result);

        result = builder
            .setVariant("zzzzz_yyyyy_xxxxx")
            .build()
            .getVariant();
        assertEquals("zyx", "zzzzz_yyyyy_xxxxx", result);

        result = builder
            .setVariant(target)
            .setVariant("")
            .build()
            .getVariant();
        assertEquals("empty", defaulted, result);

        result = builder
                .setVariant(target)
                .setVariant(null)
                .build()
                .getVariant();
        assertEquals("null", defaulted, result);

        new BuilderILE("abcd", "abcdefghi", "1ab", "1abcdefgh") { public void call() { b.setVariant(arg); }};

        assertEquals("digit+3alpha", "1abc", builder.setVariant("1abc").build().getVariant());

        new BuilderILE("abcde-fg") { public void call() { b.setVariant(arg); }};
    }

    @Test
    public void testBuilderSetExtension() {
        final char sourceKey = 'a';
        final String sourceValue = "aB-aBcdefgh-12-12345678";
        String target = "ab-abcdefgh-12-12345678";
        Builder builder = new Builder();
        String result = builder
            .setExtension(sourceKey, sourceValue)
            .build()
            .getExtension(sourceKey);
        assertEquals("extension", target, result);

        result = builder
            .setExtension(sourceKey, sourceValue)
            .setExtension(sourceKey, "")
            .build()
            .getExtension(sourceKey);
        assertEquals("empty", null, result);

        result = builder
                .setExtension(sourceKey, sourceValue)
                .setExtension(sourceKey, null)
                .build()
                .getExtension(sourceKey);
        assertEquals("null", null, result);

        new BuilderILE("$") { public void call() { b.setExtension('$', sourceValue); }};

        new BuilderILE("ab-cd-123456789") { public void call() { b.setExtension(sourceKey, arg); }};

        new BuilderILE("ab--cd") { public void call() { b.setExtension(sourceKey, arg); }};

        Locale locale = builder
            .setExtension('u', "co-japanese")
            .build();
        assertEquals("locale extension", "japanese", locale.getUnicodeLocaleType("co"));

        Locale locale2 = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .build();
        assertEquals("locales with extension", locale, locale2);

        Locale locale3 = builder
            .setExtension('u', "xxx-nu-thai")
            .build();
        assertEquals("remove co", null, locale3.getUnicodeLocaleType("co"));
        assertEquals("override thai", "thai", locale3.getUnicodeLocaleType("nu"));
        assertEquals("override attribute", 1, locale3.getUnicodeLocaleAttributes().size());

        Locale locale4 = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .build();
        assertEquals("extend", "japanese", locale4.getUnicodeLocaleType("co"));
        assertEquals("extend", "thai", locale4.getUnicodeLocaleType("nu"));

        result = builder
            .clear()
            .setExtension('u', "456-123-zz-123-yy-456-xx-789")
            .build()
            .toLanguageTag();
        assertEquals("reorder", "und-u-123-456-xx-789-yy-456-zz-123", result);

        result = builder
            .clear()
            .setExtension('u', "nu-thai-foobar")
            .build()
            .getUnicodeLocaleType("nu");
        assertEquals("multiple types", "thai-foobar", result);

        result = builder
            .clear()
            .setExtension('u', "nu-thai-NU-chinese-xx-1234")
            .build()
            .toLanguageTag();
        assertEquals("duplicate keys", "und-u-nu-thai-xx-1234", result);
    }

    @Test
    public void testBuilderAddUnicodeLocaleAttribute() {
        Builder builder = new Builder();
        Locale locale = builder
            .addUnicodeLocaleAttribute("def")
            .addUnicodeLocaleAttribute("abc")
            .build();

        Set<String> uattrs = locale.getUnicodeLocaleAttributes();
        assertEquals("number of attributes", 2, uattrs.size());
        assertTrue("attribute abc", uattrs.contains("abc"));
        assertTrue("attribute def", uattrs.contains("def"));

        locale = builder.removeUnicodeLocaleAttribute("xxx")
            .build();

        assertEquals("remove bogus", 2, uattrs.size());

        locale = builder.addUnicodeLocaleAttribute("abc")
            .build();
        assertEquals("add duplicate", 2, uattrs.size());

        new BuilderNPE("null attribute") { public void call() { b.addUnicodeLocaleAttribute(null); }};
        new BuilderNPE("null attribute removal") { public void call() { b.removeUnicodeLocaleAttribute(null); }};

        new BuilderILE("invalid attribute") { public void call() { b.addUnicodeLocaleAttribute("ca"); }};
    }

    @Test
    public void testBuildersetUnicodeLocaleKeyword() {
        Builder builder = new Builder();
        Locale locale = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .setUnicodeLocaleKeyword("nu", "thai")
            .build();
        assertEquals("co", "japanese", locale.getUnicodeLocaleType("co"));
        assertEquals("nu", "thai", locale.getUnicodeLocaleType("nu"));
        assertEquals("keys", 2, locale.getUnicodeLocaleKeys().size());

        String result = builder
            .setUnicodeLocaleKeyword("co", null)
            .build()
            .toLanguageTag();
        assertEquals("empty co", "und-u-nu-thai", result);

        result = builder
            .setUnicodeLocaleKeyword("nu", null)
            .build()
            .toLanguageTag();
        assertEquals("empty nu", "und", result);

        result = builder
            .setUnicodeLocaleKeyword("zz", "012")
            .setUnicodeLocaleKeyword("aa", "345")
            .build()
            .toLanguageTag();
        assertEquals("reordered", "und-u-aa-345-zz-012", result);

        new BuilderNPE("keyword") { public void call() { b.setUnicodeLocaleKeyword(null, "thai"); }};

        new BuilderILE("a", "abc") { public void call() { b.setUnicodeLocaleKeyword(arg, "value"); }};

        new BuilderILE("ab", "abcdefghi") { public void call() { b.setUnicodeLocaleKeyword("ab", arg); }};
    }

    @Test
    public void testBuilderPrivateUseExtension() {
        String source = "c-B-a";
        String target = "c-b-a";
        Builder builder = new Builder();
        String result = builder
            .setExtension(Locale.PRIVATE_USE_EXTENSION, source)
            .build()
            .getExtension(Locale.PRIVATE_USE_EXTENSION);
        assertEquals("abc", target, result);

        new BuilderILE("a--b") { public void call() { b.setExtension(Locale.PRIVATE_USE_EXTENSION, arg); }};
    }

    @Test
    public void testBuilderClear() {
        String monster = "en-latn-US-NewYork-a-bb-cc-u-co-japanese-x-z-y-x-x";
        Builder builder = new Builder();
        Locale locale = Locale.forLanguageTag(monster);
        String result = builder
            .setLocale(locale)
            .clear()
            .build()
            .toLanguageTag();
        assertEquals("clear", "und", result);
    }

    @Test
    public void testBuilderRemoveUnicodeAttribute() {
    }

    @Test
    public void testBuilderBuild() {
    }

    @Test
    public void testSerialize() {
        final Locale[] testLocales = {
            Locale.ROOT,
            Locale.ENGLISH,
            Locale.US,
            Locale.of("en", "US", "Win"),
            Locale.of("en", "US", "Win_XP"),
            Locale.JAPAN,
            Locale.of("ja", "JP", "JP"),
            Locale.of("th", "TH"),
            Locale.of("th", "TH", "TH"),
            Locale.of("no", "NO"),
            Locale.of("nb", "NO"),
            Locale.of("nn", "NO"),
            Locale.of("no", "NO", "NY"),
            Locale.of("nn", "NO", "NY"),
            Locale.of("he", "IL"),
            Locale.of("he", "IL", "var"),
            Locale.of("Language", "Country", "Variant"),
            Locale.of("", "US"),
            Locale.of("", "", "Java"),
            Locale.forLanguageTag("en-Latn-US"),
            Locale.forLanguageTag("zh-Hans"),
            Locale.forLanguageTag("zh-Hant-TW"),
            Locale.forLanguageTag("ja-JP-u-ca-japanese"),
            Locale.forLanguageTag("und-Hant"),
            Locale.forLanguageTag("und-a-123-456"),
            Locale.forLanguageTag("en-x-java"),
            Locale.forLanguageTag("th-TH-u-ca-buddist-nu-thai-x-lvariant-TH"),
        };

        for (Locale locale : testLocales) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(locale);

                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                assertEquals("roundtrip " + locale, locale, o);
            } catch (Exception e) {
                fail(locale + " encountered exception:" + e.getLocalizedMessage());
            }
        }
    }

    @Test
    public void testDeserialize6() {
        final String TESTFILEPREFIX = "java6locale_";

        File dataDir = null;
        String dataDirName = System.getProperty("serialized.data.dir");
        if (dataDirName == null) {
            URL resdirUrl = getClass().getClassLoader().getResource("serialized");
            if (resdirUrl != null) {
                try {
                    dataDir = new File(resdirUrl.toURI());
                } catch (URISyntaxException urie) {
                }
            }
        } else {
            dataDir = new File(dataDirName);
        }

        if (dataDir == null) {
            fail("'dataDir' is null. serialized.data.dir Property value is "+dataDirName);
            return;
        } else if (!dataDir.isDirectory()) {
            fail("'dataDir' is not a directory. dataDir: "+dataDir.toString());
            return;
        }

        File[] files = dataDir.listFiles();
        for (File testfile : files) {
            if (testfile.isDirectory()) {
                continue;
            }
            String name = testfile.getName();
            if (!name.startsWith(TESTFILEPREFIX)) {
                continue;
            }
            Locale locale;
            String locStr = name.substring(TESTFILEPREFIX.length());
            if (locStr.equals("ROOT")) {
                locale = Locale.ROOT;
            } else {
                String[] fields = locStr.split("_", 3);
                String lang = fields[0];
                String country = (fields.length >= 2) ? fields[1] : "";
                String variant = (fields.length == 3) ? fields[2] : "";
                locale = Locale.of(lang, country, variant);
            }

            try (FileInputStream fis = new FileInputStream(testfile);
                 ObjectInputStream ois = new ObjectInputStream(fis))
            {
                Object o = ois.readObject();
                assertEquals("Deserialize Java 6 Locale " + locale, o, locale);
            } catch (Exception e) {
                fail("Exception while reading " + testfile.getAbsolutePath() + " - " + e.getMessage());
            }
        }
    }

    @Test
    public void testBug7002320() {
        String[][] testdata = {
            {"ja-JP-x-lvariant-JP", "ja-JP-u-ca-japanese-x-lvariant-JP"},   
            {"ja-JP-x-lvariant-JP-XXX"},
            {"ja-JP-u-ca-japanese-x-lvariant-JP"},
            {"ja-JP-u-ca-gregory-x-lvariant-JP"},
            {"ja-JP-u-cu-jpy-x-lvariant-JP"},
            {"ja-x-lvariant-JP"},
            {"th-TH-x-lvariant-TH", "th-TH-u-nu-thai-x-lvariant-TH"},   
            {"th-TH-u-nu-thai-x-lvariant-TH"},
            {"en-US-x-lvariant-JP"},
        };

        Builder bldr = new Builder();

        for (String[] data : testdata) {
            String in = data[0];
            String expected = (data.length == 1) ? data[0] : data[1];

            Locale loc = Locale.forLanguageTag(in);
            String out = loc.toLanguageTag();
            assertEquals("Language tag roundtrip by forLanguageTag with input: " + in, expected, out);

            bldr.clear();
            bldr.setLanguageTag(in);
            loc = bldr.build();
            out = loc.toLanguageTag();
            assertEquals("Language tag roundtrip by Builder.setLanguageTag with input: " + in, expected, out);
        }
    }

    @Test
    public void testBug7023613() {
        String[][] testdata = {
            {"en-Latn", "en__#Latn"},
            {"en-u-ca-japanese", "en__#u-ca-japanese"},
        };

        for (String[] data : testdata) {
            String in = data[0];
            String expected = (data.length == 1) ? data[0] : data[1];

            Locale loc = Locale.forLanguageTag(in);
            String out = loc.toString();
            assertEquals("Empty country field with non-empty script/extension with input: " + in, expected, out);
        }
    }

    /*
     * 7033504: (lc) incompatible behavior change for ja_JP_JP and th_TH_TH locales
     */
    @Test
    public void testBug7033504() {
        checkCalendar(Locale.of("ja", "JP", "jp"), "java.util.GregorianCalendar");
        checkCalendar(Locale.of("ja", "jp", "jp"), "java.util.GregorianCalendar");
        checkCalendar(Locale.of("ja", "JP", "JP"), "java.util.JapaneseImperialCalendar");
        checkCalendar(Locale.of("ja", "jp", "JP"), "java.util.JapaneseImperialCalendar");
        checkCalendar(Locale.forLanguageTag("en-u-ca-japanese"),
                      "java.util.JapaneseImperialCalendar");

        checkDigit(Locale.of("th", "TH", "th"), '0');
        checkDigit(Locale.of("th", "th", "th"), '0');
        checkDigit(Locale.of("th", "TH", "TH"), '\u0e50');
        checkDigit(Locale.of("th", "TH", "TH"), '\u0e50');
        checkDigit(Locale.forLanguageTag("en-u-nu-thai"), '\u0e50');
    }

    private void checkCalendar(Locale loc, String expected) {
        Calendar cal = Calendar.getInstance(loc);
        assertEquals("Wrong calendar", expected, cal.getClass().getName());
    }

    private void checkDigit(Locale loc, Character expected) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(loc);
        Character zero = dfs.getZeroDigit();
        assertEquals("Wrong digit zero char", expected, zero);
    }


    private void assertTrue(String msg, boolean v) {
        if (!v) {
            fail(msg + ": expected true");
        }
    }

    private void assertFalse(String msg, boolean v) {
        if (v) {
            fail(msg + ": expected false");
        }
    }

    private void assertEquals(String msg, Object e, Object v) {
        if (e == null ? v != null : !e.equals(v)) {
            if (e != null) {
                e = "'" + e + "'";
            }
            if (v != null) {
                v = "'" + v + "'";
            }
            fail(msg + ": expected " + e + " but got " + v);
        }
    }

    private void assertNotEquals(String msg, Object e, Object v) {
        if (e == null ? v == null : e.equals(v)) {
            if (e != null) {
                e = "'" + e + "'";
            }
            fail(msg + ": expected not equal " + e);
        }
    }

    private void assertNull(String msg, Object o) {
        if (o != null) {
            fail(msg + ": expected null but got '" + o + "'");
        }
    }

    private void assertNotNull(String msg, Object o) {
        if (o == null) {
            fail(msg + ": expected non null");
        }
    }

    private abstract class ExceptionTest {
        private final Class<? extends Exception> exceptionClass;

        ExceptionTest(Class<? extends Exception> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public void run() {
            String failMsg = null;
            try {
                call();
                failMsg = "expected " + exceptionClass.getName() + "  but no exception thrown.";
            }
            catch (Exception e) {
                if (!exceptionClass.isAssignableFrom(e.getClass())) {
                    failMsg = "expected " + exceptionClass.getName() + " but caught " + e;
                }
            }
            if (failMsg != null) {
                String msg = message();
                msg = msg == null ? "" : msg + " ";
                fail(msg + failMsg);
            }
        }

        public String message() {
            return null;
        }

        public abstract void call();
    }

    private abstract class ExpectNPE extends ExceptionTest {
        ExpectNPE() {
            super(NullPointerException.class);
            run();
        }
    }

    private abstract class BuilderNPE extends ExceptionTest {
        protected final String msg;
        protected final Builder b = new Builder();

        BuilderNPE(String msg) {
            super(NullPointerException.class);

            this.msg = msg;

            run();
        }

        public String message() {
            return msg;
        }
    }

    private abstract class ExpectIAE extends ExceptionTest {
        ExpectIAE() {
            super(IllegalArgumentException.class);
            run();
        }
    }

    private abstract class BuilderILE extends ExceptionTest {
        protected final String[] args;
        protected final Builder b = new Builder();

        protected String arg; 

        BuilderILE(String... args) {
            super(IllformedLocaleException.class);

            this.args = args;

            run();
        }

        public void run() {
            for (String arg : args) {
                this.arg = arg;
                super.run();
            }
        }

        public String message() {
            return "arg: '" + arg + "'";
        }
    }
}
