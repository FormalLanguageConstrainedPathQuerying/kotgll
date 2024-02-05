/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.java.math;

import java.math.BigInteger;
import java.util.Random;

public final class Shared {


    private Shared() {
        throw new AssertionError("This is a utility class");
    }

    public static Pair createPair(int nBits) {
        if (nBits < 0) {
            throw new IllegalArgumentException(String.valueOf(nBits));
        } else if (nBits == 0) {
            var zero = new BigInteger(nBits, new byte[0]);
            var one = new BigInteger(/* positive */ 1, new byte[]{1});
            return new Pair(zero, one);
        } else if (nBits == 1) {
            var one = new BigInteger(/* positive */ 1, new byte[]{1});
            var two = new BigInteger(/* positive */ 1, new byte[]{2});
            return new Pair(one, two);
        }
        int nBytes = (nBits + 7) / 8;
        var r = new Random();
        var bytes = new byte[nBytes];
        r.nextBytes(bytes);
        bytes[0] |= (byte) 0b1000_0000;
        var x = new BigInteger(/* positive */ 1, bytes)
                .shiftRight(nBytes * 8 - nBits);
        var y = x.flipBit(0);
        if (x.bitLength() != nBits)
            throw new AssertionError(x.bitLength() + ", " + nBits);
        return new Pair(x, y);
    }

    public record Pair(BigInteger x, BigInteger y) {
        public Pair {
            if (x.signum() == -y.signum()) 
                throw new IllegalArgumentException("x.signum()=" + x.signum()
                        + ", y=signum()=" + y.signum());
            if (y.bitLength() - x.bitLength() > 1)
                throw new IllegalArgumentException("x.bitLength()=" + x.bitLength()
                        + ", y.bitLength()=" + y.bitLength());
        }
    }

    public static BigInteger createSingle(int nBits) {
        if (nBits < 0) {
            throw new IllegalArgumentException(String.valueOf(nBits));
        }
        if (nBits == 0) {
            return new BigInteger(nBits, new byte[0]);
        }
        int nBytes = (nBits + 7) / 8;
        var r = new Random();
        var bytes = new byte[nBytes];
        r.nextBytes(bytes);
        bytes[0] |= (byte) 0b1000_0000;
        var x = new BigInteger(/* positive */ 1, bytes)
                .shiftRight(nBytes * 8 - nBits);
        if (x.bitLength() != nBits)
            throw new AssertionError(x.bitLength() + ", " + nBits);
        return x;
    }
}
