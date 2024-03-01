/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jimage.decompressor;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Index compressor. Use the minimal amount of bytes required to store
 * an integer.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public class CompressIndexes {
    private static final int COMPRESSED_FLAG = 1 << (Byte.SIZE - 1);
    private static final int HEADER_WIDTH = 3;
    private static final int HEADER_SHIFT = Byte.SIZE - HEADER_WIDTH;

    public static List<Integer> decompressFlow(byte[] values) {
        List<Integer> lst = new ArrayList<>();

        for (int i = 0; i < values.length; i += getHeaderLength(values[i])) {
            int decompressed = decompress(values, i);
            lst.add(decompressed);
        }

        return lst;
    }

    public static int readInt(DataInputStream cr) throws IOException {
        byte header = cr.readByte();
        int size = getHeaderLength(header);
        int result = getHeaderValue(header);

        for (int i = 1; i < size; i++) {
            result <<= Byte.SIZE;
            result |= cr.readByte() & 0xFF;
        }

        return result;
    }

    private static boolean isCompressed(byte b) {
        return (b & COMPRESSED_FLAG) != 0;
    }

    private static int getHeaderLength(byte b) {
        return isCompressed(b) ? (b >> HEADER_SHIFT) & 3 : Integer.BYTES;
    }

    private static int getHeaderValue(byte b) {
        return isCompressed(b) ? b & (1 << HEADER_SHIFT) - 1 : b;
    }

    public static int decompress(byte[] value, int offset) {
        byte header = value[offset];
        int size = getHeaderLength(header);
        int result = getHeaderValue(header);

        for (int i = 1; i < size; i++) {
            result <<= Byte.SIZE;
            result |= value[offset + i] & 0xFF;
        }

        return result;
    }

    public static byte[] compress(int value) {
        if (value < 0) {
            throw new  IllegalArgumentException("value < 0");
        }

        int width = 32 - Integer.numberOfLeadingZeros(value);
        int size = Math.min(((width + HEADER_WIDTH - 1) >> 3) + 1, Integer.BYTES);

        byte[] result = new byte[size];

        for (int i = 0; i < size; i++) {
            result[i] = (byte)(value >> ((size - i - 1) * Byte.SIZE));
        }

        if (size < Integer.BYTES) {
            result[0] |= (byte)(COMPRESSED_FLAG | (size << HEADER_SHIFT));
        }

        return result;
    }

}
