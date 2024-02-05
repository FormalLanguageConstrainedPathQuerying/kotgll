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
 * @library ../
 * @modules java.base/jdk.internal.foreign
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestLargeStub
 */

import org.testng.annotations.Test;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.util.stream.Stream;

public class TestLargeStub extends NativeTestHelper {

    MemoryLayout STRUCT_LL = MemoryLayout.structLayout(
        C_LONG_LONG,
        C_LONG_LONG
    ); 

    @Test
    public void testDowncall() {
        Linker.nativeLinker().downcallHandle(
                FunctionDescriptor.of(STRUCT_LL,
                        Stream.generate(() -> C_DOUBLE).limit(124).toArray(MemoryLayout[]::new)),
                Linker.Option.captureCallState("errno"));
    }

    @Test
    public void testDowncallAllowHeap() {
        Linker.nativeLinker().downcallHandle(
                FunctionDescriptor.of(STRUCT_LL,
                        Stream.generate(() -> C_POINTER).limit(83).toArray(MemoryLayout[]::new)),
                Linker.Option.critical(true));
    }

    @Test
    public void testUpcall() {
        Linker.nativeLinker().downcallHandle(
                FunctionDescriptor.of(STRUCT_LL,
                        Stream.generate(() -> C_DOUBLE).limit(125).toArray(MemoryLayout[]::new)));
    }
}
