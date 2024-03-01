/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import jdk.internal.misc.Blocker;
import jdk.internal.misc.Unsafe;

/* package */ class MappedMemoryUtils {

    static boolean isLoaded(long address, boolean isSync, long size) {
        if (isSync) {
            return true;
        }
        if ((address == 0) || (size == 0))
            return true;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        return isLoaded0(mappingAddress(address, offset), length, Bits.pageCount(length));
    }

    static void load(long address, boolean isSync, long size) {
        if (isSync) {
            return;
        }
        if ((address == 0) || (size == 0))
            return;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        load0(mappingAddress(address, offset), length);

        Unsafe unsafe = Unsafe.getUnsafe();
        int ps = Bits.pageSize();
        long count = Bits.pageCount(length);
        long a = mappingAddress(address, offset);
        byte x = 0;
        for (long i=0; i<count; i++) {
            x ^= unsafe.getByte(a);
            a += ps;
        }
        if (unused != 0)
            unused = x;
    }

    private static byte unused;

    static void unload(long address, boolean isSync, long size) {
        if (isSync) {
            return;
        }
        if ((address == 0) || (size == 0))
            return;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        unload0(mappingAddress(address, offset), length);
    }

    static void force(FileDescriptor fd, long address, boolean isSync, long index, long length) {
        if (isSync) {
            Unsafe.getUnsafe().writebackMemory(address + index, length);
        } else {
            long offset = mappingOffset(address, index);
            long mappingAddress = mappingAddress(address, offset, index);
            long mappingLength = mappingLength(offset, length);
            long comp = Blocker.begin();
            try {
                force0(fd, mappingAddress, mappingLength);
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            } finally {
                Blocker.end(comp);
            }
        }
    }


    private static native boolean isLoaded0(long address, long length, long pageCount);
    private static native void load0(long address, long length);
    private static native void unload0(long address, long length);
    private static native void force0(FileDescriptor fd, long address, long length) throws IOException;


    private static long mappingOffset(long address) {
        return mappingOffset(address, 0);
    }

    private static long mappingOffset(long address, long index) {
        int ps = Bits.pageSize();
        long indexAddress = address + index;
        long baseAddress = alignDown(indexAddress, ps);
        return indexAddress - baseAddress;
    }

    private static long mappingAddress(long address, long mappingOffset) {
        return mappingAddress(address, mappingOffset, 0);
    }

    private static long mappingAddress(long address, long mappingOffset, long index) {
        long indexAddress = address + index;
        return indexAddress - mappingOffset;
    }

    private static long mappingLength(long mappingOffset, long length) {
        return length + mappingOffset;
    }

    private static long alignDown(long address, int pageSize) {
        return address & ~(pageSize - 1);
    }
}
