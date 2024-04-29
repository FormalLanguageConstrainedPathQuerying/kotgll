/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211936
 * @summary Tests of BigDecimal.intValue() and BigDecimal.longValue()
 */
import java.math.BigDecimal;
import java.util.Map;

public class IntegralValueTests {
    public static void main(String... args) {
        int failures =
            integralValuesTest(INT_VALUES, true) +
            integralValuesTest(LONG_VALUES, false);
        if (failures != 0) {
            throw new RuntimeException
                ("Incurred " + failures + " failures for {int,long}Value().");
        }
    }

    private static final Map<BigDecimal, Number> INT_VALUES =
        Map.ofEntries(

         Map.entry(new BigDecimal("2147483647"), Integer.MAX_VALUE),
         Map.entry(new BigDecimal("2147483647.0"), Integer.MAX_VALUE),
         Map.entry(new BigDecimal("2147483647.00"), Integer.MAX_VALUE),

         Map.entry(new BigDecimal("-2147483647"), -Integer.MAX_VALUE),
         Map.entry(new BigDecimal("-2147483647.0"), -Integer.MAX_VALUE),

         Map.entry(new BigDecimal("-2147483648"), Integer.MIN_VALUE),
         Map.entry(new BigDecimal("-2147483648.1"), Integer.MIN_VALUE),
         Map.entry(new BigDecimal("-2147483648.01"), Integer.MIN_VALUE),

         Map.entry(new BigDecimal("-2147483649"), Integer.MAX_VALUE),

         Map.entry(new BigDecimal("4294967295"), -1),

         Map.entry(new BigDecimal("4294967296"), 0),

         Map.entry(new BigDecimal("1e32"), 0),

         Map.entry(new BigDecimal("1e31"), Integer.MIN_VALUE),

         Map.entry(new BigDecimal("1e0"), 1),

         Map.entry(new BigDecimal("9e-1"), 0),

         Map.entry(new BigDecimal("900e-1"), 90), 
         Map.entry(new BigDecimal("900e-2"), 9),
         Map.entry(new BigDecimal("900e-3"), 0),

         Map.entry(new BigDecimal("123456789e-9"), 0),

         Map.entry(new BigDecimal("123456789e-8"), 1),

         Map.entry(new BigDecimal("10000001e1"), 100000010),
         Map.entry(new BigDecimal("10000001e10"), -1315576832),
         Map.entry(new BigDecimal("10000001e100"), 0),
         Map.entry(new BigDecimal("10000001e1000"), 0),
         Map.entry(new BigDecimal("10000001e10000"), 0),
         Map.entry(new BigDecimal("10000001e100000"), 0),
         Map.entry(new BigDecimal("10000001e1000000"), 0),
         Map.entry(new BigDecimal("10000001e10000000"), 0),
         Map.entry(new BigDecimal("10000001e100000000"), 0),
         Map.entry(new BigDecimal("10000001e1000000000"), 0),

         Map.entry(new BigDecimal("10000001e-1"), 1000000),
         Map.entry(new BigDecimal("10000001e-10"), 0),
         Map.entry(new BigDecimal("10000001e-100"), 0),
         Map.entry(new BigDecimal("10000001e-1000"), 0),
         Map.entry(new BigDecimal("10000001e-10000"), 0),
         Map.entry(new BigDecimal("10000001e-100000"), 0),
         Map.entry(new BigDecimal("10000001e-1000000"), 0),
         Map.entry(new BigDecimal("10000001e-10000000"), 0),
         Map.entry(new BigDecimal("10000001e-100000000"), 0),
         Map.entry(new BigDecimal("10000001e-1000000000"), 0),

         Map.entry(new BigDecimal("12345.0001"), 12345),
         Map.entry(new BigDecimal("12345.9999"), 12345),
         Map.entry(new BigDecimal("-12345.0001"), -12345),
         Map.entry(new BigDecimal("-12345.9999"), -12345));

    private static final Map<BigDecimal, Number> LONG_VALUES =
        Map.ofEntries(
         Map.entry(new BigDecimal("9223372036854775807"), Long.MAX_VALUE),
         Map.entry(new BigDecimal("9223372036854775807.0"), Long.MAX_VALUE),
         Map.entry(new BigDecimal("9223372036854775807.00"), Long.MAX_VALUE),

         Map.entry(new BigDecimal("-9223372036854775808"), Long.MIN_VALUE),
         Map.entry(new BigDecimal("-9223372036854775808.1"), Long.MIN_VALUE),
         Map.entry(new BigDecimal("-9223372036854775808.01"), Long.MIN_VALUE),

         Map.entry(new BigDecimal("-9223372036854775809"), 9223372036854775807L),

         Map.entry(new BigDecimal("18446744073709551615"), -1L),

         Map.entry(new BigDecimal("18446744073709551616"), 0L),

         Map.entry(new BigDecimal("1e63"),  -9223372036854775808L),
         Map.entry(new BigDecimal("-1e63"), -9223372036854775808L),
         Map.entry(new BigDecimal("1e64"), 0L),
         Map.entry(new BigDecimal("-1e64"), 0L),
         Map.entry(new BigDecimal("1e65"), 0L),
         Map.entry(new BigDecimal("-1e65"), 0L),

         Map.entry(new BigDecimal("1e0"), 1L),

         Map.entry(new BigDecimal("9e-1"), 0L),

         Map.entry(new BigDecimal("900e-1"), 90L), 
         Map.entry(new BigDecimal("900e-2"), 9L),
         Map.entry(new BigDecimal("900e-3"), 0L),

         Map.entry(new BigDecimal("123456789e-9"), 0L),

         Map.entry(new BigDecimal("123456789e-8"), 1L),

         Map.entry(new BigDecimal("10000001e1"), 100000010L),
         Map.entry(new BigDecimal("10000001e10"), 100000010000000000L),
         Map.entry(new BigDecimal("10000001e100"), 0L),
         Map.entry(new BigDecimal("10000001e1000"), 0L),
         Map.entry(new BigDecimal("10000001e10000"), 0L),
         Map.entry(new BigDecimal("10000001e100000"), 0L),
         Map.entry(new BigDecimal("10000001e1000000"), 0L),
         Map.entry(new BigDecimal("10000001e10000000"), 0L),
         Map.entry(new BigDecimal("10000001e100000000"), 0L),
         Map.entry(new BigDecimal("10000001e1000000000"), 0L),

         Map.entry(new BigDecimal("10000001e-1"), 1000000L),
         Map.entry(new BigDecimal("10000001e-10"), 0L),
         Map.entry(new BigDecimal("10000001e-100"), 0L),
         Map.entry(new BigDecimal("10000001e-1000"), 0L),
         Map.entry(new BigDecimal("10000001e-10000"), 0L),
         Map.entry(new BigDecimal("10000001e-100000"), 0L),
         Map.entry(new BigDecimal("10000001e-1000000"), 0L),
         Map.entry(new BigDecimal("10000001e-10000000"), 0L),
         Map.entry(new BigDecimal("10000001e-100000000"), 0L),
         Map.entry(new BigDecimal("10000001e-1000000000"), 0L),

         Map.entry(new BigDecimal("12345.0001"), 12345L),
         Map.entry(new BigDecimal("12345.9999"), 12345L),
         Map.entry(new BigDecimal("-12345.0001"), -12345L),
         Map.entry(new BigDecimal("-12345.9999"), -12345L));

    private static int integralValuesTest(Map<BigDecimal, Number> v, boolean isInt) {
        System.err.format("Testing %s%n", isInt ? "Integer" : "Long");
        int failures = 0;
        for (var testCase : v.entrySet()) {
            BigDecimal bd = testCase.getKey();
            Number expected = testCase.getValue();
            try {
                if (isInt) {
                    int intValue = bd.intValue();
                    if (intValue != (int)expected) {
                        failures += reportError(bd, expected, intValue, isInt);
                    }
                } else {
                    long longValue = bd.longValue();
                    if (longValue != (long)expected) {
                        failures += reportError(bd, expected, longValue, isInt);
                    }
                }
            } catch (Exception e) {
                failures++;
                System.err.format("Unexpected exception %s for %s%n",
                                  e, bd.toString());
            }
        }
        return failures;
    }

    private static int reportError(BigDecimal bd, Number expected, long longValue, boolean isInt) {
        System.err.format("For %s, scale=%d, expected %d, actual %d, simple %d%n",
                          bd.toString(), bd.scale(),
                          (isInt ? (Integer) expected : (Long) expected ),
                          longValue,
                          (isInt ? simpleIntValue(bd): simpleLongValue(bd) ));
        return 1;
    }

    private static long simpleLongValue(BigDecimal bd) {
        return bd.toBigInteger().longValue();
    }

    private static int simpleIntValue(BigDecimal bd) {
        return bd.toBigInteger().intValue();
    }
}
