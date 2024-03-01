/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.stream;

import jdk.internal.util.ByteArray;
import jdk.internal.util.ByteArrayLittleEndian;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteOrder;

/**
 * An abstract class implementing the {@code ImageOutputStream} interface.
 * This class is designed to reduce the number of methods that must
 * be implemented by subclasses.
 *
 */
public abstract class ImageOutputStreamImpl
    extends ImageInputStreamImpl
    implements ImageOutputStream {

    /**
     * Constructs an {@code ImageOutputStreamImpl}.
     */
    public ImageOutputStreamImpl() {
    }

    public abstract void write(int b) throws IOException;

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public abstract void write(byte[] b, int off, int len) throws IOException;

    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public void writeByte(int v) throws IOException {
        write(v);
    }

    public void writeShort(int v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            ByteArray.setUnsignedShort(byteBuf, 0, v);
        } else {
            ByteArrayLittleEndian.setUnsignedShort(byteBuf, 0, v);
        }
        write(byteBuf, 0, 2);
    }

    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            ByteArray.setInt(byteBuf, 0, v);
        } else {
            ByteArrayLittleEndian.setInt(byteBuf, 0, v);
        }
        write(byteBuf, 0, 4);
    }

    public void writeLong(long v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            ByteArray.setLong(byteBuf, 0, v);
        } else {
            ByteArrayLittleEndian.setLong(byteBuf, 0, v);
        }
        write(byteBuf, 0, 4);
        write(byteBuf, 4, 4);
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            write((byte)s.charAt(i));
        }
    }

    public void writeChars(String s) throws IOException {
        int len = s.length();

        byte[] b = new byte[len*2];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len ; i++) {
                char v = s.charAt(i);
                ByteArray.setChar(b, boff, v);
                boff += 2;
            }
        } else {
            for (int i = 0; i < len ; i++) {
                char v = s.charAt(i);
                ByteArrayLittleEndian.setChar(b, boff, v);
                boff += 2;
            }
        }

        write(b, 0, len*2);
    }

    /**
     * @throws UTFDataFormatException {@inheritDoc}
     */
    public void writeUTF(String s) throws IOException {
        int strlen = s.length();
        int utflen = 0;
        char[] charr = new char[strlen];
        int c, boff = 0;

        s.getChars(0, strlen, charr, 0);

        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535) {
            throw new UTFDataFormatException("utflen > 65536!");
        }

        byte[] b = new byte[utflen+2];
        b[boff++] = (byte) ((utflen >>> 8) & 0xFF);
        b[boff++] = (byte) ((utflen >>> 0) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            c = charr[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                b[boff++] = (byte) c;
            } else if (c > 0x07FF) {
                b[boff++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                b[boff++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                b[boff++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
                b[boff++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                b[boff++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        write(b, 0, utflen + 2);
    }

    public void writeShorts(short[] s, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > s.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > s.length!");
        }

        byte[] b = new byte[len*2];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len; i++) {
                short v = s[off + i];
                ByteArray.setShort(b, boff, v);
                boff += 2;
            }
        } else {
            for (int i = 0; i < len; i++) {
                short v = s[off + i];
                ByteArrayLittleEndian.setShort(b, boff, v);
                boff += 2;
            }
        }

        write(b, 0, len*2);
    }

    public void writeChars(char[] c, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > c.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > c.length!");
        }

        byte[] b = new byte[len*2];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len; i++) {
                char v = c[off + i];
                ByteArray.setChar(b, boff, v);
                boff += 2;
            }
        } else {
            for (int i = 0; i < len; i++) {
                char v = c[off + i];
                ByteArrayLittleEndian.setChar(b, boff, v);
                boff += 2;
            }
        }

        write(b, 0, len*2);
    }

    public void writeInts(int[] i, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > i.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > i.length!");
        }

        byte[] b = new byte[len*4];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < len; j++) {
                int v = i[off + j];
                ByteArray.setInt(b, boff, v);
                boff += 4;
            }
        } else {
            for (int j = 0; j < len; j++) {
                int v = i[off + j];
                ByteArrayLittleEndian.setInt(b, boff, v);
                boff += 4;
            }
        }

        write(b, 0, len*4);
    }

    public void writeLongs(long[] l, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > l.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > l.length!");
        }

        byte[] b = new byte[len*8];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len; i++) {
                long v = l[off + i];
                ByteArray.setLong(b, boff, v);
                boff += 8;
            }
        } else {
            for (int i = 0; i < len; i++) {
                long v = l[off + i];
                ByteArrayLittleEndian.setLong(b, boff, v);
                boff += 8;
            }
        }

        write(b, 0, len*8);
    }

    public void writeFloats(float[] f, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > f.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > f.length!");
        }

        byte[] b = new byte[len*4];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len; i++) {
                float v = f[off + i];
                ByteArray.setFloat(b, boff, v);
                boff += 4;
            }
        } else {
            for (int i = 0; i < len; i++) {
                float v = f[off + i];
                ByteArrayLittleEndian.setFloat(b, boff, v);
                boff += 4;
            }
        }

        write(b, 0, len*4);
    }

    public void writeDoubles(double[] d, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > d.length || off + len < 0) {
            throw new IndexOutOfBoundsException
                ("off < 0 || len < 0 || off + len > d.length!");
        }

        byte[] b = new byte[len*8];
        int boff = 0;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < len; i++) {
                double v = d[off + i];
                ByteArray.setDouble(b, boff, v);
                boff += 8;
            }
        } else {
            for (int i = 0; i < len; i++) {
                double v = d[off + i];
                ByteArrayLittleEndian.setDouble(b, boff, v);
                boff += 8;
            }
        }

        write(b, 0, len*8);
    }

    public void writeBit(int bit) throws IOException {
        writeBits((1L & bit), 1);
    }

    public void writeBits(long bits, int numBits) throws IOException {
        checkClosed();

        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException("Bad value for numBits!");
        }
        if (numBits == 0) {
            return;
        }


        if ((getStreamPosition() > 0) || (bitOffset > 0)) {
            int offset = bitOffset;  
            int partialByte = read();
            if (partialByte != -1) {
                seek(getStreamPosition() - 1);
            } else {
                partialByte = 0;
            }

            if (numBits + offset < 8) {
                int shift = 8 - (offset+numBits);
                int mask = -1 >>> (32 - numBits);
                partialByte &= ~(mask << shift);  
                partialByte |= ((bits & mask) << shift); 
                write(partialByte);
                seek(getStreamPosition() - 1);
                bitOffset = offset + numBits;
                numBits = 0;  
            } else {
                int num = 8 - offset;
                int mask = -1 >>> (32 - num);
                partialByte &= ~mask;  
                partialByte |= ((bits >> (numBits - num)) & mask);
                write(partialByte);
                numBits -= num;
            }
        }

        if (numBits > 7) {
            int extra = numBits % 8;
            for (int numBytes = numBits / 8; numBytes > 0; numBytes--) {
                int shift = (numBytes-1)*8+extra;
                int value = (int) ((shift == 0)
                                   ? bits & 0xFF
                                   : (bits>>shift) & 0xFF);
                write(value);
            }
            numBits = extra;
        }

        if (numBits != 0) {
            int partialByte = 0;
            partialByte = read();
            if (partialByte != -1) {
                seek(getStreamPosition() - 1);
            }
            else { 
                partialByte = 0;
            }

            int shift = 8 - numBits;
            int mask = -1 >>> (32 - numBits);
            partialByte &= ~(mask << shift);
            partialByte |= (bits & mask) << shift;
            write(partialByte);
            seek(getStreamPosition() - 1);
            bitOffset = numBits;
        }
    }

    /**
     * If the bit offset is non-zero, forces the remaining bits
     * in the current byte to 0 and advances the stream position
     * by one.  This method should be called by subclasses at the
     * beginning of the {@code write(int)} and
     * {@code write(byte[], int, int)} methods.
     *
     * @throws IOException if an I/O error occurs.
     */
    protected final void flushBits() throws IOException {
        checkClosed();
        if (bitOffset != 0) {
            int offset = bitOffset;
            int partialByte = read(); 
            if (partialByte < 0) {
                partialByte = 0;
                bitOffset = 0;
            }
            else {
                seek(getStreamPosition() - 1);
                partialByte &= -1 << (8 - offset);
            }
            write(partialByte);
        }
    }

}
