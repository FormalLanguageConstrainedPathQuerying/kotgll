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
 * @bug 8330819
 * @summary Case where VPointer finds an "adr" CastX2P, which contains a CastLL,
 *          that has a ctrl after the pre-loop. This value cannot be used in the
 *          pre-loop limit for main-loop adjustment.
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.util
 * @run main/othervm -Xbatch compiler.loopopts.superword.TestMemorySegmentMainLoopAlignment
 */

package compiler.loopopts.superword;

import java.lang.foreign.*;
import jdk.internal.misc.Unsafe;
import jdk.internal.util.Preconditions;

public class TestMemorySegmentMainLoopAlignment {
    static final ValueLayout.OfInt ELEMENT_LAYOUT = ValueLayout.JAVA_INT.withByteAlignment(1);
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static long RANGE = 6400;

    interface MSOp {
        int apply(MemorySegment memory, long offset, int i);
    }

    interface MemoryUnsafeOp {
        int apply(long base, long offset, int i);
    }

    public static void main(String[] args) {
        MemorySegment ms = Arena.ofAuto().allocate(6400, Integer.SIZE);
        for (int i = 0; i < 10_000; i++) {
            test1(ms, 0, TestMemorySegmentMainLoopAlignment::memorySegmentGet);
        }
        long base = UNSAFE.allocateMemory(6400);
        for (int i = 0; i < 10_000; i++) {
            test2(base, 0, TestMemorySegmentMainLoopAlignment::memoryUnsafeGet);
        }
    }

    static int memorySegmentGet(MemorySegment ms, long o, int i) {
        return ms.get(ELEMENT_LAYOUT, o + i * 4L);
    }

    static int test1(MemorySegment a, long offset, MSOp f) {
        int size = 16;
        int[] res = new int[size];
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += f.apply(a, offset, i) + res[i];
        }
        return sum;
    }

    static int memoryUnsafeGet(long base, long o, int i) {
        long index = o + i * 4L;
        Preconditions.checkIndex(index, RANGE, null);
        return UNSAFE.getInt(base + index);
    }

    static int test2(long base, long offset, MemoryUnsafeOp f) {
        int size = 16;
        int[] res = new int[size];
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += f.apply(base, offset, i) + res[i];
        }
        return sum;
    }
}
