/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
 * @summary Test that overflowed integers passed to arraycopy don't do any harm. This might
 *          be the case on platforms where C-code expects that ints passed to a call
 *          are properly sign extended to 64 bit (e.g., PPC64, s390x). This can fail
 *          if slow_arraycopy_C() is commpiled by the C compiler without any imlicit
 *          casts (as spill stores to the stack that are done with 4-byte instruction).
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestArrayCopyOverflowArguments
 */

package compiler.arraycopy;

public class TestArrayCopyOverflowArguments {

    static volatile int mod = Integer.MAX_VALUE;

    public static int[] m1(Object src) {
        if (src == null) return null;
        int[] dest = new int[10];
        try {
            int pos   =  8 + mod + mod; 
            int start =  2 + mod + mod; 
            int len   = 12 + mod + mod; 
            System.arraycopy(src, pos, dest, 0, 10);
        } catch (ArrayStoreException npe) {
        }
        return dest;
    }

    static public void main(String[] args) throws Exception {
        int[] src = new int[20];

        for (int i  = 0; i < 20; ++i) {
            src[i] = i * (i-1);
        }

        for (int i = 0; i < 20000; i++) {
            m1(src);
        }
    }
}

