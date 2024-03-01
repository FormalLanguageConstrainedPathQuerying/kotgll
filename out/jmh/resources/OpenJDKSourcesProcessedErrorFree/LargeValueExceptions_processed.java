/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8200698
 * @summary Tests that exceptions are thrown for ops which would overflow
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 4g)
 * @run testng/othervm -Xmx4g LargeValueExceptions
 */
import java.math.BigInteger;
import static java.math.BigInteger.ONE;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class LargeValueExceptions {
    private static final int MAX_INTS = 1 << 26;

    private static final long MAX_BITS = (0xffffffffL & MAX_INTS) << 5L;

    private static final int MAX_INTS_HALF = MAX_INTS / 2;

    @AfterMethod
    public void getRunTime(ITestResult tr) {
        long time = tr.getEndMillis() - tr.getStartMillis();
        System.out.printf("Run time: %d ms%n", time);
    }


    @Test(enabled=false)
    public void squareNoOverflow() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS - 1).subtract(ONE);
        BigInteger y = x.multiply(x);
    }

    @Test(enabled=false)
    public void squareIndefiniteOverflowSuccess() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS - 1);
        BigInteger y = x.multiply(x);
    }

    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void squareIndefiniteOverflowFailure() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS).subtract(ONE);
        BigInteger y = x.multiply(x);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void squareDefiniteOverflow() {
        BigInteger x = ONE.shiftLeft(16*MAX_INTS);
        BigInteger y = x.multiply(x);
    }


    @Test(enabled=false)
    public void multiplyNoOverflow() {
        final int halfMaxBits = MAX_INTS_HALF << 5;

        BigInteger x = ONE.shiftLeft(halfMaxBits).subtract(ONE);
        BigInteger y = ONE.shiftLeft(halfMaxBits - 1).subtract(ONE);
        BigInteger z = x.multiply(y);
    }

    @Test(enabled=false)
    public void multiplyIndefiniteOverflowSuccess() {
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS/2) - 1);
        long m = MAX_BITS - x.bitLength();

        BigInteger y = ONE.shiftLeft((int)(MAX_BITS/2) - 1);
        long n = MAX_BITS - y.bitLength();

        if (m + n != MAX_BITS) {
            throw new RuntimeException("Unexpected leading zero sum");
        }

        BigInteger z = x.multiply(y);
    }

    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void multiplyIndefiniteOverflowFailure() {
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS/2)).subtract(ONE);
        long m = MAX_BITS - x.bitLength();

        BigInteger y = ONE.shiftLeft((int)(MAX_BITS/2)).subtract(ONE);
        long n = MAX_BITS - y.bitLength();

        if (m + n != MAX_BITS) {
            throw new RuntimeException("Unexpected leading zero sum");
        }

        BigInteger z = x.multiply(y);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void multiplyDefiniteOverflow() {
        byte[] xmag = new byte[4*MAX_INTS_HALF];
        xmag[0] = (byte)0xff;
        BigInteger x = new BigInteger(1, xmag);

        byte[] ymag = new byte[4*MAX_INTS_HALF + 1];
        ymag[0] = (byte)0xff;
        BigInteger y = new BigInteger(1, ymag);

        BigInteger z = x.multiply(y);
    }


    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow() {
        BigInteger.TEN.pow(Integer.MAX_VALUE);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow1() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent));
        BigInteger y = x.pow(exponent);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void powOverflow2() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent)).add(ONE);
        BigInteger y = x.pow(exponent);
    }

    @Test(expectedExceptions=ArithmeticException.class,enabled=false)
    public void powOverflow3() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent)).subtract(ONE);
        BigInteger y = x.pow(exponent);
    }

    @Test(enabled=false)
    public void powOverflow4() {
        int shift = 20;
        int exponent = 1 << shift;
        BigInteger x = ONE.shiftLeft((int)(MAX_BITS / exponent - 1)).add(ONE);
        BigInteger y = x.pow(exponent);
    }
}
