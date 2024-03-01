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

/*
 * @test
 * @key stress randomness
 * @bug 8299975
 * @summary Limit underflow protection CMoveINode in PhaseIdealLoop::do_unroll must also protect type from underflow
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestCMoveLimitType::test*
 *                   -XX:CompileCommand=dontinline,compiler.loopopts.TestCMoveLimitType::dontInline
 *                   -XX:RepeatCompilation=50 -XX:+StressIGVN
 *                   -Xbatch
 *                   compiler.loopopts.TestCMoveLimitType
*/

/*
 * @test
 * @key stress randomness
 * @bug 8299975
 * @summary Limit underflow protection CMoveINode in PhaseIdealLoop::do_unroll must also protect type from underflow
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestCMoveLimitType::test*
 *                   -XX:CompileCommand=dontinline,compiler.loopopts.TestCMoveLimitType::dontInline
 *                   -XX:RepeatCompilation=50 -XX:+StressIGVN
 *                   -XX:+IgnoreUnrecognizedVMOptions -Xcomp -XX:+TraceLoopOpts
 *                   compiler.loopopts.TestCMoveLimitType
*/


package compiler.loopopts;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class TestCMoveLimitType {
    static int[] iArr = new int[10000];

    static int test_buf(CharBuffer src, ByteBuffer dst) {
        int outputSize = 0;
        byte[] outputByte;
        byte[] tmpBuf = new byte[3];

        while (src.hasRemaining()) {
            outputByte = tmpBuf;
            char c = src.get();
            if (c % 3 == 0) {
                outputSize = -2147483648; 
            } else {
                outputByte[0] = (byte) 0;
                outputByte[1] = (byte) 1;
                outputByte[2] = (byte) 2;
                outputSize = 3; 
            }
            if (dst.remaining() < outputSize) {
                return 102;
            }
            for (int i = 0; i < outputSize; i++) { 
                dst.put(outputByte[i]);
            }
        }
        return 103;
    }

    static CharBuffer makeSrc() {
        CharBuffer src = CharBuffer.allocate(100);
        for (int j = 0; j < 100; j++) {
            if (j % 31 == 0) {
                src.put((char)(0 + (j%3)*3)); 
            } else {
                src.put((char)(1 + (j%3)*3)); 
            }
        }
        src.position(0);
        return src;
    }

    static void test_simple(boolean flag) {
        int x = flag ? Integer.MIN_VALUE : 3;
        dontInline();
        for (int i = 0; i < x; i++) {
            iArr[i * 2] = 666 + i;
        }
    }

    static void dontInline() {}

    static public void main(String[] args) {
        for (int i = 0; i < 10_000; i++) {
            try {
                test_simple(i % 2 == 0);
            } catch (Exception e) {}
        }
        for (int i = 0; i < 6_000; i++) {
            CharBuffer src = makeSrc();
            ByteBuffer dst = ByteBuffer.allocate(10_000);
            test_buf(src, dst); 
        }
    }
}
