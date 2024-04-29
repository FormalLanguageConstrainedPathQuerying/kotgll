/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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

/* Type-specific source code for unit test
 *
 * Regenerate the BasicX classes via genBasic.sh whenever this file changes.
 * We check in the generated source files so that the test tree can be used
 * independently of the rest of the source tree.
 */


import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.chrono.*;
import java.time.temporal.ChronoField;
import java.util.*;

import static java.util.Calendar.*;
import static java.util.SimpleTimeZone.*;
import java.util.regex.Pattern;

public class BasicDateTime extends Basic {
    private static void testDateTime(String fs, String exp, Calendar c) {
        testDateTime(fs, exp, c, true);
    }

    private static void testDateTime(String fs, String exp, Calendar c, boolean upper) {

        test(fs, exp, c);
        test((Locale)null, fs, exp, c);
        test(Locale.US, fs, exp, c);

        String nexp = (fs.equals("%tZ") || fs.equals("%TZ")
                       || fs.equals("%tc") || fs.equals("%Tc")
                       ? exp.replace("PST", "GMT-08:00")
                       : exp);

        Date d = c.getTime();
        test(fs, nexp, d);
        test((Locale)null, fs, nexp, d);
        test(Locale.US, fs, nexp, d);

        long l = c.getTimeInMillis();
        test(fs, nexp, l);
        test((Locale)null, fs, nexp, l);
        test(Locale.US, fs, nexp, l);

        if (upper)
            testDateTime(Pattern.compile("t").matcher(fs).replaceFirst("T"),
                         exp.toUpperCase(), c, false);
    }

    private static void testHours() {
        for (int i = 0; i < 24; i++) {
            Calendar c = new GregorianCalendar(1995, MAY, 23, i, 48, 34);

            String exp = Integer.toString(i);
            testDateTime("%tk", exp, c);

            int v = i % 12;
            v = (v == 0 ? 12 : v);
            String exp2 = Integer.toString(v);
            testDateTime("%tl", exp2, c);

            if (exp.length() < 2) exp = "0" + exp;
            testDateTime("%tH", exp, c);

            if (exp2.length() < 2) exp2 = "0" + exp2;
            testDateTime("%tI", exp2, c);

            testDateTime("%tp", (i <12 ? "am" : "pm"), c);
        }
    }



    public static void test() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-0800"));

        tryCatch("%q", UnknownFormatConversionException.class);
        tryCatch("%t&", UnknownFormatConversionException.class);
        tryCatch("%&d", UnknownFormatConversionException.class);
        tryCatch("%^b", UnknownFormatConversionException.class);

        test(Locale.FRANCE, "e = %+10.4f", "e =    +2,7183", Math.E);
        test("%4$2s %3$2s %2$2s %1$2s", " d  c  b  a", "a", "b", "c", "d");
        test("Amount gained or lost since last statement: $ %,(.2f",
             "Amount gained or lost since last statement: $ (6,217.58)",
             (new BigDecimal("-6217.58")));
        Calendar c = new GregorianCalendar(1969, JULY, 20, 16, 17, 0);
        testSysOut("Local time: %tT", "Local time: 16:17:00", c);

        test("Unable to open file '%1$s': %2$s",
             "Unable to open file 'food': No such file or directory",
             "food", "No such file or directory");
        Calendar duke = new GregorianCalendar(1995, MAY, 23, 19, 48, 34);
        duke.set(Calendar.MILLISECOND, 584);
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke);
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke.getTime());
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke.getTimeInMillis());

        test("%4$s %3$s %2$s %1$s %4$s %3$s %2$s %1$s",
             "d c b a d c b a", "a", "b", "c", "d");
        test("%s %s %<s %<s", "a b b b", "a", "b", "c", "d");
        test("%s %s %s %s", "a b c d", "a", "b", "c", "d");
        test("%2$s %s %<s %s", "b a a b", "a", "b", "c", "d");

        test("%b", "true", true);
        test("%b", "false", false);
        test("%B", "TRUE", true);
        test("%B", "FALSE", false);
        test("%b", "true", Boolean.TRUE);
        test("%b", "false", Boolean.FALSE);
        test("%B", "TRUE", Boolean.TRUE);
        test("%B", "FALSE", Boolean.FALSE);
        test("%14b", "          true", true);
        test("%-14b", "true          ", true);
        test("%5.1b", "    f", false);
        test("%-5.1b", "f    ", false);

        test("%b", "true", "foo");
        test("%b", "false", (Object)null);

        test(Locale.FRANCE, "%b", "true", true);
        test(Locale.FRANCE, "%b", "false", false);

        test("%b", "false", (Object[])new String[2]);
        test("%b", "true", new String[2], new String[2]);

        int [] ia = { 1, 2, 3 };
        test("%b", "true", ia);

        tryCatch("%#b", FormatFlagsConversionMismatchException.class);
        tryCatch("%-b", MissingFormatWidthException.class);
        tryCatch("%.b", UnknownFormatConversionException.class);
        tryCatch("%,b", FormatFlagsConversionMismatchException.class);

        test("%c", "i", 'i');
        test("%C", "I", 'i');
        test("%4c",  "   i", 'i');
        test("%-4c", "i   ", 'i');
        test("%4C",  "   I", 'i');
        test("%-4C", "I   ", 'i');
        test("%c", "i", Character.valueOf('i'));
        test("%c", "H", (byte) 72);
        test("%c", "i", (short) 105);
        test("%c", "!", (int) 33);
        test("%c", "\u007F", Byte.MAX_VALUE);
        test("%c", new String(Character.toChars(Short.MAX_VALUE)),
             Short.MAX_VALUE);
        test("%c", "null", (Object) null);

        tryCatch("%c", IllegalFormatConversionException.class,
                 Boolean.TRUE);
        tryCatch("%c", IllegalFormatConversionException.class,
                 (float) 0.1);
        tryCatch("%c", IllegalFormatConversionException.class,
                 new Object());
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Byte.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Short.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Integer.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Integer.MAX_VALUE);

        tryCatch("%#c", FormatFlagsConversionMismatchException.class);
        tryCatch("%,c", FormatFlagsConversionMismatchException.class);
        tryCatch("%(c", FormatFlagsConversionMismatchException.class);
        tryCatch("%$c", UnknownFormatConversionException.class);
        tryCatch("%.2c", IllegalFormatPrecisionException.class);

        test("%s", "Hello, Duke", "Hello, Duke");
        test("%S", "HELLO, DUKE", "Hello, Duke");
        test("%20S", "         HELLO, DUKE", "Hello, Duke");
        test("%20s", "         Hello, Duke", "Hello, Duke");
        test("%-20s", "Hello, Duke         ", "Hello, Duke");
        test("%-20.5s", "Hello               ", "Hello, Duke");
        test("%s", "null", (Object)null);

        StringBuffer sb = new StringBuffer("foo bar");
        test("%s", sb.toString(), sb);
        test("%S", sb.toString().toUpperCase(), sb);

        tryCatch("%-s", MissingFormatWidthException.class);
        tryCatch("%--s", DuplicateFormatFlagsException.class);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, 0);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, 0.5f);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, "hello");
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, (Object)null);

        test("%h", Integer.toHexString("Hello, Duke".hashCode()),
             "Hello, Duke");
        test("%10h", "  ddf63471", "Hello, Duke");
        test("%-10h", "ddf63471  ", "Hello, Duke");
        test("%-10H", "DDF63471  ", "Hello, Duke");
        test("%10h", "  402e0000", 15.0);
        test("%10H", "  402E0000", 15.0);

        tryCatch("%#h", FormatFlagsConversionMismatchException.class);

        tryCatch("%F", UnknownFormatConversionException.class);

        tryCatch("%#g", FormatFlagsConversionMismatchException.class);




        test("%tA", "null", (Object)null);
        test("%TA", "NULL", (Object)null);

        tryCatch("%t", UnknownFormatConversionException.class);
        tryCatch("%T", UnknownFormatConversionException.class);
        tryCatch("%tP", UnknownFormatConversionException.class);
        tryCatch("%TP", UnknownFormatConversionException.class);
        tryCatch("%.5tB", IllegalFormatPrecisionException.class);
        tryCatch("%#tB", FormatFlagsConversionMismatchException.class);
        tryCatch("%-tB", MissingFormatWidthException.class);


        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone tz = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        Calendar c0 = new GregorianCalendar(tz, Locale.US);
        c0.set(1995, MAY, 23, 19, 48, 34);
        c0.set(Calendar.MILLISECOND, 584);

        testDateTime("%tM", "48", c0);
        testDateTime("%tN", "584000000", c0);
        testDateTime("%tL", "584", c0);

        testDateTime("%ts", String.valueOf(c0.getTimeInMillis() / 1000), c0);
        testDateTime("%tS", "34", c0);
        testDateTime("%tT", "19:48:34", c0);

        testHours();

        testDateTime("%ta", "Tue", c0);
        testDateTime("%tA", "Tuesday", c0);
        testDateTime("%tb", "May", c0);
        testDateTime("%tB", "May", c0);
        testDateTime("%tC", "19", c0);
        testDateTime("%td", "23", c0);
        testDateTime("%te", "23", c0);
        testDateTime("%th", "May", c0);
        testDateTime("%tj", "143", c0);
        testDateTime("%tm", "05", c0);
        testDateTime("%ty", "95", c0);
        testDateTime("%tY", "1995", c0);

        testDateTime("%tz", "-0800", c0);
        testDateTime("%tZ", "PST", c0);

        TimeZone dtz = TimeZone.getDefault();

        TimeZone atz = new SimpleTimeZone(-8 * 60 * 60 * 1000, "AlwaysDST",
            JANUARY, 1, 0, 0, STANDARD_TIME,
            DECEMBER, 31, 0, 60 * 60 * 1000 * 24 - 1, STANDARD_TIME,
            (int)(60 * 60 * 1000 * 3.25));
        TimeZone.setDefault(atz);
        testDateTime("%tz", "-0445", Calendar.getInstance(atz));

        TimeZone.setDefault(dtz);
        if (atz.hasSameRules(TimeZone.getDefault()))
            throw new RuntimeException("Default TimeZone not restored");

        testDateTime("%tr", "07:48:34 PM", c0);
        testDateTime("%tR", "19:48", c0);
        testDateTime("%tc", "Tue May 23 19:48:34 PST 1995", c0);
        testDateTime("%tD", "05/23/95", c0);
        testDateTime("%tF", "1995-05-23", c0);
        testDateTime("%-12tF", "1995-05-23  ", c0);
        testDateTime("%12tF", "  1995-05-23", c0);

        test("%n", System.getProperty("line.separator"), (Object)null);
        test("%n", System.getProperty("line.separator"), "");

        tryCatch("%,n", IllegalFormatFlagsException.class);
        tryCatch("%.n", UnknownFormatConversionException.class);
        tryCatch("%5.n", UnknownFormatConversionException.class);
        tryCatch("%5n", IllegalFormatWidthException.class);
        tryCatch("%.7n", IllegalFormatPrecisionException.class);
        tryCatch("%<n", IllegalFormatFlagsException.class);

        test("%%", "%", (Object)null);
        test("%%", "%", "");

        test("%5%", "    %", (Object)null);
        test("%5%", "    %", "");
        test("%-5%", "%    ", (Object)null);
        test("%-5%", "%    ", "");

        tryCatch("%.5%", IllegalFormatPrecisionException.class);
        tryCatch("%5.5%", IllegalFormatPrecisionException.class);

        tryCatch("%%%", UnknownFormatConversionException.class);
        tryCatch("%<%", IllegalFormatFlagsException.class);

        test("%tF", "2023-01-13", LocalDate.of(2023, 1, 13));
        test("%tF", "2023-10-03", LocalDate.of(2023, 10, 3));
        test("%tF", "0001-10-03", LocalDate.of(1, 10, 3));
        test("%tF", "0012-10-03", LocalDate.of(12, 10, 3));
        test("%tF", "0123-10-03", LocalDate.of(123, 10, 3));
        test("%tF", "+12345-10-03", LocalDate.of(12345, 10, 3));
        test("%tF", "+12345-10-03", LocalDateTime.of(12345, 10, 3, 0, 0, 0));
        test("%tF", "+12345-10-03", OffsetDateTime.of(LocalDateTime.of(12345, 10, 3, 0, 0, 0), ZoneOffset.UTC));
        test("%tF", "+12345-10-03", ZonedDateTime.of(LocalDateTime.of(12345, 10, 3, 0, 0, 0), ZoneOffset.UTC));
        test("%tF", "-0001-10-03", LocalDate.of(-1, 10, 3));
        test("%tF", "-0012-10-03", LocalDate.of(-12, 10, 3));
        test("%tF", "-0123-10-03", LocalDate.of(-123, 10, 3));
        test("%tF", "-1234-10-03", LocalDate.of(-1234, 10, 3));
        test("%tF", "-12345-10-03", LocalDate.of(-12345, 10, 3));
        test("%tF", "-12345-10-03", LocalDate.of(-12345, 10, 3));
        test("%tF", "-12345-10-03", LocalDateTime.of(-12345, 10, 3, 0, 0, 0));
        test("%tF", "-12345-10-03", OffsetDateTime.of(LocalDateTime.of(-12345, 10, 3, 0, 0, 0), ZoneOffset.UTC));
        test("%tF", "-12345-10-03", ZonedDateTime.of(LocalDateTime.of(-12345, 10, 3, 0, 0, 0), ZoneOffset.UTC));

        int year = 2023, month = 1, dayOfMonth = 13;
        String specifier = "%tF";
        for (Locale locale : Locale.getAvailableLocales()) {
            char minusSign = DecimalFormatSymbols.getInstance(locale).getMinusSign();
            String str = new Formatter(new StringBuilder(), locale)
                    .format(specifier, LocalDate.of(year, month, dayOfMonth))
                    .toString();
            test(locale, specifier, minusSign + str, LocalDate.of(-year, month, dayOfMonth));
        }

        ChronoLocalDate jpDate = Chronology
                .ofLocale(Locale.forLanguageTag("ja-JP-u-ca-japanese"))
                .dateNow();
        test(Locale.JAPANESE,
                "%tF",
                String.format(
                        "%04d-%02d-%02d",
                        jpDate.get(ChronoField.YEAR_OF_ERA),
                        jpDate.get(ChronoField.MONTH_OF_YEAR),
                        jpDate.get(ChronoField.DAY_OF_MONTH)),
                jpDate);

        ChronoLocalDate jpDate1 = JapaneseChronology.INSTANCE.dateNow();
        test(Locale.JAPANESE,
                "%tF",
                String.format(
                        "%04d-%02d-%02d",
                        jpDate1.get(ChronoField.YEAR_OF_ERA),
                        jpDate1.get(ChronoField.MONTH_OF_YEAR),
                        jpDate1.get(ChronoField.DAY_OF_MONTH)),
                jpDate1);
    }
}
