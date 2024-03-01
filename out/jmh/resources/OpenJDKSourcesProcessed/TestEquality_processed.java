/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177552 8222756
 * @summary Checks the equals and hashCode method of CompactNumberFormat
 * @modules jdk.localedata
 * @run testng/othervm TestEquality
 *
 */

import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import org.testng.annotations.Test;

public class TestEquality {

    @Test
    public void testEquality() {
        CompactNumberFormat cnf1 = (CompactNumberFormat) NumberFormat
                .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

        CompactNumberFormat cnf2 = (CompactNumberFormat) NumberFormat
                .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

        String decimalPattern = "#,##0.###";
        String[] compactPatterns = new String[]{
                "",
                "",
                "",
                "{one:0K other:0K}",
                "{one:00K other:00K}",
                "{one:000K other:000K}",
                "{one:0M other:0M}",
                "{one:00M other:00M}",
                "{one:000M other:000M}",
                "{one:0B other:0B}",
                "{one:00B other:00B}",
                "{one:000B other:000B}",
                "{one:0T other:0T}",
                "{one:00T other:00T}",
                "{one:000T other:000T}"
        };
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        CompactNumberFormat cnf3 =
            new CompactNumberFormat(decimalPattern, symbols, compactPatterns, "one:i = 1 and v = 0");

        CompactNumberFormat cnf4 = new CompactNumberFormat("#,#0.0#", symbols, compactPatterns);

        CompactNumberFormat cnf5 = new CompactNumberFormat(decimalPattern,
                DecimalFormatSymbols.getInstance(Locale.JAPAN), compactPatterns);

        CompactNumberFormat cnf6 = new CompactNumberFormat(decimalPattern,
                symbols, new String[]{"", "", "", "0K", "00K", "000K"});

        if (!cnf1.equals(cnf1)) {
            throw new RuntimeException("[testEquality() reflexivity FAILED: The compared"
                    + " objects must be equal]");
        }

        if (!cnf1.equals(cnf2) || !cnf2.equals(cnf1)) {
            throw new RuntimeException("[testEquality() symmetry FAILED: The compared"
                    + " objects must be equal]");
        }

        if (!cnf1.equals(cnf2) || !cnf2.equals(cnf3) || !cnf1.equals(cnf3)) {
            throw new RuntimeException("[testEquality() transitivity FAILED: The compared"
                    + " objects must be equal]");
        }

        checkEquals(cnf3, cnf4, false, "1st", "different decimal pattern");

        checkEquals(cnf3, cnf5, false, "2nd", "different format symbols");

        checkEquals(cnf3, cnf6, false, "3rd", "different compact patterns");

        cnf1.setMinimumIntegerDigits(5);
        checkEquals(cnf1, cnf2, false, "4th", "different min integer digits");

        cnf2.setMinimumIntegerDigits(5);
        checkEquals(cnf1, cnf2, true, "5th", "");

        cnf1.setGroupingSize(4);
        checkEquals(cnf1, cnf2, false, "6th", "different grouping size");

        cnf2.setGroupingSize(4);
        checkEquals(cnf1, cnf2, true, "7th", "");

        cnf1.setParseBigDecimal(true);
        checkEquals(cnf1, cnf2, false, "8th", "different parse big decimal");

    }

    private void checkEquals(CompactNumberFormat cnf1, CompactNumberFormat cnf2,
            boolean mustEqual, String nthComparison, String message) {
        if (cnf1.equals(cnf2) != mustEqual) {
            if (mustEqual) {
                throw new RuntimeException("[testEquality() " + nthComparison
                        + " comparison FAILED: The compared objects must be equal]");
            } else {
                throw new RuntimeException("[testEquality() " + nthComparison
                        + " comparison FAILED: The compared objects must"
                        + " not be equal because of " + message + "]");
            }
        }
    }

    @Test
    public void testHashCode() {
        NumberFormat cnf1 = NumberFormat
                .getCompactNumberInstance(Locale.JAPAN, NumberFormat.Style.SHORT);
        NumberFormat cnf2 = NumberFormat
                .getCompactNumberInstance(Locale.JAPAN, NumberFormat.Style.SHORT);

        if (cnf1.hashCode() != cnf2.hashCode()) {
            throw new RuntimeException("[testHashCode() FAILED: hashCode of the"
                    + " compared objects must match]");
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        NumberFormat cnf1 = NumberFormat
                .getCompactNumberInstance(Locale.of("hi", "IN"), NumberFormat.Style.SHORT);
        cnf1.setMinimumIntegerDigits(5);
        NumberFormat cnf2 = NumberFormat
                .getCompactNumberInstance(Locale.of("hi", "IN"), NumberFormat.Style.SHORT);
        cnf2.setMinimumIntegerDigits(5);
        if (cnf1.equals(cnf2)) {
            if (cnf1.hashCode() != cnf2.hashCode()) {
                throw new RuntimeException("[testEqualsAndHashCode() FAILED: two"
                        + " equal objects must have same hashCode]");
            }
        } else {
            throw new RuntimeException("[testEqualsAndHashCode() FAILED: The"
                    + " compared objects must be equal]");
        }
    }

}
