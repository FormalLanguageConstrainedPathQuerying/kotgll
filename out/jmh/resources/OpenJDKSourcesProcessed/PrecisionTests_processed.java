/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 1234567
 * @summary Test that precision() is computed properly.
 * @author Joseph D. Darcy
 */

import java.math.*;
import static java.math.BigDecimal.*;

public class PrecisionTests {
    private static BigDecimal NINE = valueOf(9);

    public static void main(String argv[]) {
        int failures = 0;

        BigDecimal[] testValues = {
            valueOf(1), valueOf(9),
        };

        failures += testPrecision(new BigDecimal(0), 1);

        for(int i = 1; i < 100; i++) {
            for(BigDecimal bd : testValues) {
                failures += testPrecision(bd, i);
                failures += testPrecision(bd.negate(), i);
            }

            testValues[0] = testValues[0].multiply(TEN);
            testValues[1] = testValues[1].multiply(TEN).add(NINE);
        }

        BigDecimal[] randomTestValues = {
            valueOf(2147483648L),          
            valueOf(-2147483648L),         
            valueOf(98893745455L),         
            valueOf(3455436789887L),       
            valueOf(140737488355328L),     
            valueOf(-140737488355328L),    
            valueOf(7564232235739573L),    
            valueOf(25335434990002322L),   
            valueOf(9223372036854775807L), 
            valueOf(-9223372036854775807L) 
        };
        int[] expectedPrecision = {10, 10, 11, 13, 15, 15, 16, 17, 19, 19};
        for (int i = 0; i < randomTestValues.length; i++) {
            failures += testPrecision(randomTestValues[i], expectedPrecision[i]);
        }

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing precision.");
        }
    }

    private static int testPrecision(BigDecimal bd, int expected) {
        int precision = bd.precision();


        if (precision != expected) {
            System.err.printf("For (%s).precision expected %d, got %d%n",
                               bd, expected, precision);
            return 1;
        }
        return 0;
    }
}
