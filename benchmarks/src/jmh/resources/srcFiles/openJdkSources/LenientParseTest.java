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
 * @summary Test suite for NumberFormat parsing when lenient.
 * @run junit/othervm -Duser.language=en -Duser.country=US LenientParseTest
 * @run junit/othervm -Duser.language=ja -Duser.country=JP LenientParseTest
 * @run junit/othervm -Duser.language=zh -Duser.country=CN LenientParseTest
 * @run junit/othervm -Duser.language=tr -Duser.country=TR LenientParseTest
 * @run junit/othervm -Duser.language=de -Duser.country=DE LenientParseTest
 * @run junit/othervm -Duser.language=fr -Duser.country=FR LenientParseTest
 * @run junit/othervm -Duser.language=ar -Duser.country=AR LenientParseTest
 */

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

public class LenientParseTest {

    private static final DecimalFormatSymbols dfs =
            new DecimalFormatSymbols(Locale.getDefault());
    private static final DecimalFormat dFmt = (DecimalFormat)
            NumberFormat.getNumberInstance(Locale.getDefault());
    private static final DecimalFormat cFmt =
            (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
    private static final DecimalFormat pFmt =
            (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
    private static final CompactNumberFormat cmpctFmt =
            (CompactNumberFormat) NumberFormat.getCompactNumberInstance(Locale.getDefault(),
                    NumberFormat.Style.SHORT);

    static {
        cmpctFmt.setParseIntegerOnly(false);
        cmpctFmt.setGroupingUsed(true);
    }

    @ParameterizedTest
    @MethodSource("badParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void numFmtFailParseTest(String toParse, int expectedErrorIndex) {
        DecimalFormat nonLocalizedDFmt = new DecimalFormat("a#,#00.00b");
        failParse(nonLocalizedDFmt, toParse, expectedErrorIndex);
    }

    @ParameterizedTest
    @MethodSource("validFullParseStrings")
    public void numFmtSuccessFullParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(dFmt, toParse, toParse.length()));
    }

    @ParameterizedTest
    @MethodSource("validPartialParseStrings")
    public void numFmtSuccessPartialParseTest(String toParse, double expectedValue,
                                              int expectedIndex) {
        assertEquals(expectedValue, successParse(dFmt, toParse, expectedIndex));
    }

    @ParameterizedTest
    @MethodSource("noGroupingParseStrings")
    public void numFmtStrictGroupingNotUsed(String toParse, double expectedValue, int expectedIndex) {
        dFmt.setGroupingUsed(false);
        assertEquals(expectedValue, successParse(dFmt, toParse, expectedIndex));
        dFmt.setGroupingUsed(true);
    }

    @ParameterizedTest
    @MethodSource("integerOnlyParseStrings")
    public void numFmtStrictIntegerOnlyUsed(String toParse, int expectedValue, int expectedIndex) {
        dFmt.setParseIntegerOnly(true);
        assertEquals(expectedValue, successParse(dFmt, toParse, expectedIndex));
        dFmt.setParseIntegerOnly(false);
    }

    @ParameterizedTest
    @MethodSource("currencyValidFullParseStrings")
    public void currFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(cFmt, toParse, toParse.length()));
    }

    @ParameterizedTest
    @MethodSource("currencyValidPartialParseStrings")
    public void currFmtParseTest(String toParse, double expectedValue,
                                 int expectedIndex) {
        if (cFmt.getPositiveSuffix().length() > 0) {
            failParse(cFmt, toParse, expectedIndex);
        } else {
            assertEquals(expectedValue, successParse(cFmt, toParse, expectedIndex));
        }
    }

    @ParameterizedTest
    @MethodSource("percentValidFullParseStrings")
    public void percentFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(pFmt, toParse, toParse.length()));
    }

    @ParameterizedTest
    @MethodSource("percentValidPartialParseStrings")
    public void percentFmtParseTest(String toParse, double expectedValue,
                                 int expectedIndex) {
        if (pFmt.getPositiveSuffix().length() > 0) {
            failParse(pFmt, toParse, expectedIndex);
        } else {
            assertEquals(expectedValue, successParse(pFmt, toParse, expectedIndex));
        }
    }

    @ParameterizedTest
    @MethodSource("compactValidPartialParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtFailParseTest(String toParse, double expectedValue, int expectedErrorIndex) {
        assertEquals(expectedValue, successParse(cmpctFmt, toParse, expectedErrorIndex));
    }


    @ParameterizedTest
    @MethodSource("compactValidFullParseStrings")
    @EnabledIfSystemProperty(named = "user.language", matches = "en")
    public void compactFmtSuccessParseTest(String toParse, double expectedValue) {
        assertEquals(expectedValue, successParse(cmpctFmt, toParse, toParse.length()));
    }


    private double successParse(NumberFormat fmt, String toParse, int expectedIndex) {
        Number parsedValue = assertDoesNotThrow(() -> fmt.parse(toParse));
        ParsePosition pp = new ParsePosition(0);
        assertDoesNotThrow(() -> fmt.parse(toParse, pp));
        assertEquals(-1, pp.getErrorIndex(),
                "ParsePosition ErrorIndex is not in correct location");
        assertEquals(expectedIndex, pp.getIndex(),
                "ParsePosition Index is not in correct location");
        return parsedValue.doubleValue();
    }

    private void failParse(NumberFormat fmt, String toParse, int expectedErrorIndex) {
        ParsePosition pp = new ParsePosition(0);
        assertThrows(ParseException.class, () -> fmt.parse(toParse));
        assertNull(fmt.parse(toParse, pp));
        assertEquals(expectedErrorIndex, pp.getErrorIndex());
    }


    private static Stream<Arguments> badParseStrings() {
        return Stream.of(
                Arguments.of("1,1b", 0),
                Arguments.of("a1,11", 5),
                Arguments.of("a1,11,z", 5),
                Arguments.of("a1,11,", 5),
                Arguments.of("1,11", 0),
                Arguments.of("ac1,11", 0));
    }


    private static Stream<Arguments> validFullParseStrings() {
        return Stream.of(
                Arguments.of("1,,,1", 11d),
                Arguments.of("11,,,11,,,11", 111111d),
                Arguments.of("1,1.", 11d),
                Arguments.of("11,111,11.", 1111111d),
                Arguments.of("1,1.1", 11.1d),
                Arguments.of("1,11.1", 111.1d),
                Arguments.of("1,1111.1", 11111.1d),
                Arguments.of("11,111,11.1", 1111111.1d),
                Arguments.of(",111,,1,1", 11111d),
                Arguments.of(",1", 1d),
                Arguments.of(",,1", 1d),
                Arguments.of("000,1,1", 11d),
                Arguments.of("000,111,11,,1", 111111d),
                Arguments.of("0,000,1,,1,1", 111d),
                Arguments.of("1,234.00", 1234d),
                Arguments.of("1,234.0", 1234d),
                Arguments.of("1,234.", 1234d),
                Arguments.of("1,234.00123", 1234.00123d),
                Arguments.of("1,234.012", 1234.012d),
                Arguments.of("1,234.224", 1234.224d),
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

    private static Stream<Arguments> validPartialParseStrings() {
        return Stream.of(
                Arguments.of("11,", 11d, 2),
                Arguments.of("11,,", 11d, 3),
                Arguments.of("11,,,", 11d, 4),
                Arguments.of("1,1P111", 11d, 3),
                Arguments.of("1.1P111", 1.1d, 3),
                Arguments.of("1P,1111", 1d, 1),
                Arguments.of("1P.1111", 1d, 1),
                Arguments.of("1,1111P", 11111d, 6),
                Arguments.of("1.11,11", 1.11d, 4),
                Arguments.of("1.,11,11", 1d, 2))
                .map(args -> Arguments.of(
                        localizeText(String.valueOf(args.get()[0])), args.get()[1], args.get()[2]));
    }

    private static Stream<Arguments> integerOnlyParseStrings() {
        return Stream.of(
                Arguments.of("1234.1234", 1234, 4),
                Arguments.of("1234.12", 1234, 4),
                Arguments.of("1234.1a", 1234, 4),
                Arguments.of("1234.", 1234, 4))
                .map(args -> Arguments.of(
                        localizeText(String.valueOf(args.get()[0])), args.get()[1], args.get()[2]));
    }

    private static Stream<Arguments> noGroupingParseStrings() {
        return Stream.of(
                Arguments.of("12,34", 12d, 2),
                Arguments.of("1234,", 1234d, 4),
                Arguments.of("123,456.789", 123d, 3))
                .map(args -> Arguments.of(
                        localizeText(String.valueOf(args.get()[0])), args.get()[1], args.get()[2]));
    }

    private static Stream<Arguments> percentValidPartialParseStrings() {
        return validPartialParseStrings().map(args ->
                Arguments.of(pFmt.getPositivePrefix() + args.get()[0] + pFmt.getPositiveSuffix(),
                        (double) args.get()[1] / 100, (int) args.get()[2] + pFmt.getPositivePrefix().length())
        );
    }

    private static Stream<Arguments> percentValidFullParseStrings() {
        return validFullParseStrings().map(args -> Arguments.of(
                pFmt.getPositivePrefix() + args.get()[0] + pFmt.getPositiveSuffix(),
                (double) args.get()[1] / 100)
        );
    }

    private static Stream<Arguments> currencyValidPartialParseStrings() {
        return validPartialParseStrings().map(args -> Arguments.of(
                cFmt.getPositivePrefix() + String.valueOf(args.get()[0])
                        .replace(dfs.getGroupingSeparator(), dfs.getMonetaryGroupingSeparator())
                        .replace(dfs.getDecimalSeparator(), dfs.getMonetaryDecimalSeparator())
                        + cFmt.getPositiveSuffix(),
                args.get()[1], (int) args.get()[2] + cFmt.getPositivePrefix().length())
        );
    }

    private static Stream<Arguments> currencyValidFullParseStrings() {
        return validFullParseStrings().map(args -> Arguments.of(
                cFmt.getPositivePrefix() + String.valueOf(args.get()[0])
                        .replace(dfs.getGroupingSeparator(), dfs.getMonetaryGroupingSeparator())
                        .replace(dfs.getDecimalSeparator(), dfs.getMonetaryDecimalSeparator())
                        + cFmt.getPositiveSuffix(),
                args.get()[1])
        );
    }

    private static Stream<Arguments> compactValidPartialParseStrings() {
        return Stream.concat(validPartialParseStrings().map(args -> Arguments.of(args.get()[0],
                args.get()[1], args.get()[2])), validPartialParseStrings().map(args -> Arguments.of(args.get()[0] + "K",
                args.get()[1], args.get()[2]))
        );
    }

    private static Stream<Arguments> compactValidFullParseStrings() {
        return Stream.concat(validFullParseStrings().map(args -> Arguments.of(args.get()[0],
                args.get()[1])), validFullParseStrings().map(args -> Arguments.of(args.get()[0] + "K",
                (double)args.get()[1] * 1000.0))
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
