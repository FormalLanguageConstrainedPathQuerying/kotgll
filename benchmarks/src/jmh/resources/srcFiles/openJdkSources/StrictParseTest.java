/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8327640
 * @summary Test suite for NumberFormat parsing with strict leniency
 * @run junit/othervm -Duser.language=en -Duser.country=US StrictParseTest
 * @run junit/othervm -Duser.language=ja -Duser.country=JP StrictParseTest
 * @run junit/othervm -Duser.language=zh -Duser.country=CN StrictParseTest
 * @run junit/othervm -Duser.language=tr -Duser.country=TR StrictParseTest
 * @run junit/othervm -Duser.language=de -Duser.country=DE StrictParseTest
 * @run junit/othervm -Duser.language=fr -Duser.country=FR StrictParseTest
 * @run junit/othervm -Duser.language=ar -Duser.country=AR StrictParseTest
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.CompactNumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StrictParseTest {

    private static final DecimalFormatSymbols dfs =
            new DecimalFormatSymbols(Locale.getDefault());
    private static final DecimalFormat dFmt =
            (DecimalFormat) NumberFormat.getNumberInstance(Locale.getDefault());
    private static final DecimalFormat cFmt =
            (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
    private static final DecimalFormat pFmt =
            (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
    private static final CompactNumberFormat cmpctFmt =
            (CompactNumberFormat) NumberFormat.getCompactNumberInstance(Locale.getDefault(),
                    NumberFormat.Style.SHORT);


    static {
        dFmt.setStrict(true);
        pFmt.setStrict(true);
        cFmt.setStrict(true);
        cmpctFmt.setStrict(true);
        cmpctFmt.setParseIntegerOnly(false);
        cmpctFmt.setGroupingUsed(true);
        cmpctFmt.setGroupingSize(3);
    }


    @Test 
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void uniqueCaseNumberFormatTest() {
        DecimalFormat nonLocalizedDFmt = new DecimalFormat("a#,#00.00b");
        nonLocalizedDFmt.setStrict(true);
        failParse(nonLocalizedDFmt, "a12bfoo", 3);
        failParse(nonLocalizedDFmt, "a123,456.00bc", 11);
        failParse(nonLocalizedDFmt, "ac123", 0);
        failParse(nonLocalizedDFmt, "a123", 4);
        failParse(nonLocalizedDFmt, ".a123", 0);
        nonLocalizedDFmt.setGroupingSize(1);
        successParse(nonLocalizedDFmt, "a1,2,3,4b");
        failParse(nonLocalizedDFmt, "a1,2,3,45,6b", 8);
        nonLocalizedDFmt.setGroupingSize(5);
        successParse(nonLocalizedDFmt, "a12345,67890b");
        successParse(nonLocalizedDFmt, "a1234,67890b");
        failParse(nonLocalizedDFmt, "a123456,7890b", 6);

    }

    @ParameterizedTest
    @MethodSource("badParseStrings")
    public void numFmtFailParseTest(String toParse, int expectedErrorIndex) {
        failParse(dFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("validParseStrings")
    public void numFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(dFmt, toParse));
    }

    @ParameterizedTest
    @MethodSource("negativeBadParseStrings")
    public void negNumFmtFailParseTest(String toParse, int expectedErrorIndex) {
        failParse(dFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("negativeValidParseStrings")
    public void negNumFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(dFmt, toParse));
    }

    @ParameterizedTest
    @MethodSource({"validParseStrings", "noGroupingParseStrings"})
    public void numFmtStrictGroupingNotUsed(String toParse) {
        dFmt.setGroupingUsed(false);
        int failIndex = toParse.indexOf(
                dFmt.getDecimalFormatSymbols().getGroupingSeparator());
        if (failIndex > -1) {
            failParse(dFmt, toParse, failIndex);
        } else {
            successParse(dFmt, toParse);
        }
        dFmt.setGroupingUsed(true);
    }

    @ParameterizedTest
    @MethodSource({"validParseStrings", "integerOnlyParseStrings"})
    public void numFmtStrictIntegerOnlyUsed(String toParse) {
        dFmt.setParseIntegerOnly(true);
        int failIndex = toParse.indexOf(dfs.getDecimalSeparator());
        if (failIndex > -1) {
            failParse(dFmt, toParse, failIndex);
        } else {
            successParse(dFmt, toParse);
        }
        dFmt.setParseIntegerOnly(false);
    }

    @ParameterizedTest
    @MethodSource("currencyBadParseStrings")
    public void currFmtFailParseTest(String toParse, int expectedErrorIndex) {
        failParse(cFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("currencyValidParseStrings")
    public void currFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(cFmt, toParse));
    }

    @ParameterizedTest
    @MethodSource("percentBadParseStrings")
    public void percentFmtFailParseTest(String toParse, int expectedErrorIndex) {
        failParse(pFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("percentValidParseStrings")
    public void percentFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(pFmt, toParse));
    }

    @ParameterizedTest
    @MethodSource("compactBadParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtFailParseTest(String toParse, int expectedErrorIndex) {
        failParse(cmpctFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("compactValidParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(cmpctFmt, toParse));
    }

    @Test 
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtEdgeParseTest() {
        CompactNumberFormat cnf = new CompactNumberFormat("a##0.0#b", DecimalFormatSymbols
                .getInstance(Locale.US), new String[]{"", "c0d"});
        cnf.setStrict(true);

        failParse(cnf, "12345d", 0);
        failParse(cnf, "1b", 0);
        failParse(cnf, "aa1d", 0);
        failParse(cnf, "cc1d", 0);
        failParse(cnf, "aa1b", 0);
        failParse(cnf, "cc1b", 0);

        failParse(cnf, "a1", 2);
        failParse(cnf, "c1", 2);
        failParse(cnf, "a1dd", 2);
        failParse(cnf, "c1dd", 2);
        failParse(cnf, "a1bb", 2);
        failParse(cnf, "c1bb", 2);
    }

    @Test
    public void parsePositionIndexTest() {
        failParse(dFmt, localizeText("123,456,,789.00"), 8, 4);
    }


    private double successParse(NumberFormat fmt, String toParse) {
        if (!toParse.contains(String.valueOf(dfs.getGroupingSeparator())) &&
                !toParse.contains(String.valueOf(dfs.getMonetaryGroupingSeparator()))) {
            fmt.setGroupingUsed(false);
        }
        Number parsedValue = assertDoesNotThrow(() -> fmt.parse(toParse));
        ParsePosition pp = new ParsePosition(0);
        assertDoesNotThrow(() -> fmt.parse(toParse, pp));
        assertEquals(-1, pp.getErrorIndex(),
                "ParsePosition ErrorIndex is not in correct location");
        assertEquals(toParse.length(), pp.getIndex(),
                "ParsePosition Index is not in correct location");
        fmt.setGroupingUsed(true);
        return parsedValue.doubleValue();
    }

    private void failParse(NumberFormat fmt, String toParse, int expectedErrorIndex) {
        failParse(fmt, toParse, expectedErrorIndex, 0);
    }

    private void failParse(NumberFormat fmt, String toParse,
                           int expectedErrorIndex, int initialParseIndex) {
        ParsePosition pp = new ParsePosition(initialParseIndex);
        assertThrows(ParseException.class, () -> fmt.parse(toParse));
        assertNull(fmt.parse(toParse, pp));
        assertEquals(expectedErrorIndex, pp.getErrorIndex());
        assertEquals(initialParseIndex, pp.getIndex());
    }


    private static Stream<Arguments> badParseStrings() {
        return Stream.of(
                Arguments.of("1,.", 2),
                Arguments.of("1,.1", 2),
                Arguments.of("1,1", 2),
                Arguments.of("1,11", 3),
                Arguments.of("1,1111", 5),
                Arguments.of("11,111,11", 8),
                Arguments.of("1,1.", 3),
                Arguments.of("1,11.", 4),
                Arguments.of("1,1111.", 5),
                Arguments.of("11,111,11.", 9),
                Arguments.of("11,111,", 6),
                Arguments.of("11,", 2),
                Arguments.of("11,,", 3),
                Arguments.of("11,a", 2),
                Arguments.of("1,1.1", 3),
                Arguments.of("1,11.1", 4),
                Arguments.of("1,1111.1", 5),
                Arguments.of("11,111,11.1", 9),
                Arguments.of("1,,1", 2),
                Arguments.of("1,1,,1", 3),
                Arguments.of("1,,1,1", 2),
                Arguments.of("1,11,111", 4),
                Arguments.of("11,11,111", 5),
                Arguments.of("111,11,11", 6),
                Arguments.of("1111,11,111", 3),
                Arguments.of("00000,11,111", 3),
                Arguments.of("111,1111111111", 7),
                Arguments.of("111,11", 5),
                Arguments.of("111,1111111111.", 7),
                Arguments.of("111,11.", 6),
                Arguments.of("111,1111111111.", 7),
                Arguments.of(",111,,1,1", 0),
                Arguments.of(",1", 0),
                Arguments.of(",,1", 0),
                Arguments.of("000,1,1", 5),
                Arguments.of("000,111,11,,1", 10),
                Arguments.of("0,000,1,,1,1", 7),
                Arguments.of("1a", 1),
                Arguments.of("123a4", 3),
                Arguments.of("123.4a5", 5),
                Arguments.of("123,456.77a", 10),
                Arguments.of("1,234a", 5),
                Arguments.of("1,.a", 2),
                Arguments.of("1.a", 2),
                Arguments.of(".22a", 3),
                Arguments.of(".1a1", 2),
                Arguments.of("1,234,a", 5))
                .map(args -> Arguments.of(
                        localizeText(String.valueOf(args.get()[0])), args.get()[1]));
    }

    private static Stream<Arguments> validParseStrings() {
        return Stream.of(
                Arguments.of("1,234.00", 1234d),
                Arguments.of("1,234.0", 1234d),
                Arguments.of("1,234.", 1234d),
                Arguments.of("1", 1d),
                Arguments.of("10", 10d),
                Arguments.of("100", 100d),
                Arguments.of("1000", 1000d),
                Arguments.of("1,000", 1000d),
                Arguments.of("10,000", 10000d),
                Arguments.of("10000", 10000d),
                Arguments.of("100,000", 100000d),
                Arguments.of("1,000,000", 1000000d),
                Arguments.of("10,000,000", 10000000d))
                .map(args -> Arguments.of(
                        localizeText(String.valueOf(args.get()[0])), args.get()[1]));
    }

    private static Stream<Arguments> integerOnlyParseStrings() {
        return Stream.of(
                Arguments.of("234.a"),
                Arguments.of("234.a1"),
                Arguments.of("234.1"),
                Arguments.of("234.1a"),
                Arguments.of("234."))
                .map(args -> Arguments.of(localizeText(String.valueOf(args.get()[0]))));
    }

    private static Stream<Arguments> noGroupingParseStrings() {
        return Stream.of(
                Arguments.of("12,34.a"),
                Arguments.of("123,.a1"),
                Arguments.of(",1234"),
                Arguments.of("123,"))
                .map(args -> Arguments.of(localizeText(String.valueOf(args.get()[0]))));
    }

    private static Stream<Arguments> negativeBadParseStrings() {
        return badParseStrings().map(args -> Arguments.of(
                dFmt.getNegativePrefix() + args.get()[0] + dFmt.getNegativeSuffix(),
                (int)args.get()[1] + dFmt.getNegativePrefix().length())
        );
    }

    private static Stream<Arguments> negativeValidParseStrings() {
        return validParseStrings().map(args -> Arguments.of(
                dFmt.getNegativePrefix() + args.get()[0] + dFmt.getNegativeSuffix(),
                (double) args.get()[1] * -1)
        );
    }

    private static Stream<Arguments> percentBadParseStrings() {
        return badParseStrings().map(args -> Arguments.of(
                pFmt.getPositivePrefix() + args.get()[0] + pFmt.getPositiveSuffix(),
                        (int)args.get()[1] + pFmt.getPositivePrefix().length())
        );
    }

    private static Stream<Arguments> percentValidParseStrings() {
        return validParseStrings().map(args -> Arguments.of(
                pFmt.getPositivePrefix() + args.get()[0] + pFmt.getPositiveSuffix(),
                (double)args.get()[1] / 100.0)
        );
    }

    private static Stream<Arguments> currencyBadParseStrings() {
        return badParseStrings().map(args -> Arguments.of(
                cFmt.getPositivePrefix() + String.valueOf(args.get()[0])
                        .replace(dfs.getGroupingSeparator(), dfs.getMonetaryGroupingSeparator())
                        .replace(dfs.getDecimalSeparator(), dfs.getMonetaryDecimalSeparator())
                        + cFmt.getPositiveSuffix(),
                (int)args.get()[1] + cFmt.getPositivePrefix().length())
        );
    }

    private static Stream<Arguments> currencyValidParseStrings() {
        return validParseStrings().map(args -> Arguments.of(
                cFmt.getPositivePrefix() + String.valueOf(args.get()[0])
                        .replace(dfs.getGroupingSeparator(), dfs.getMonetaryGroupingSeparator())
                        .replace(dfs.getDecimalSeparator(), dfs.getMonetaryDecimalSeparator())
                        + cFmt.getPositiveSuffix(),
                args.get()[1])
        );
    }

    private static Stream<Arguments> compactBadParseStrings() {
        return Stream.concat(
                badParseStrings().map(args -> Arguments.of(args.get()[0], args.get()[1])),
                badParseStrings().map(args -> Arguments.of(args.get()[0] + "K", args.get()[1]))
        );
    }

    private static Stream<Arguments> compactValidParseStrings() {
        return Stream.concat(
                validParseStrings().map(args -> Arguments.of(
                        args.get()[0], args.get()[1])),
                validParseStrings().map(args -> Arguments.of(
                        args.get()[0] + "K", (double) args.get()[1] * 1000))
        );
    }

    private static String localizeText(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ',') {
                sb.append(dfs.getGroupingSeparator());
            } else if (c == '.') {
                sb.append(dfs.getDecimalSeparator());
            } else if (c == '0') {
                sb.append(dfs.getZeroDigit());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
