/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.tiff;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.IIOException;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class TIFFIFD extends TIFFDirectory {
    private static final long MAX_SAMPLES_PER_PIXEL = 0xffff;
    private static final long MAX_ASCII_SIZE  = 0xffff;

    private long stripOrTileByteCountsPosition = -1;
    private long stripOrTileOffsetsPosition = -1;
    private long lastPosition = -1;

    private static volatile Set<Integer> essentialTags;

    private static void initializeEssentialTags() {
        Set<Integer> tags = essentialTags;
        if (tags == null) {
            essentialTags = Set.of(
                BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE,
                BaselineTIFFTagSet.TAG_COLOR_MAP,
                BaselineTIFFTagSet.TAG_COMPRESSION,
                BaselineTIFFTagSet.TAG_EXTRA_SAMPLES,
                BaselineTIFFTagSet.TAG_FILL_ORDER,
                BaselineTIFFTagSet.TAG_ICC_PROFILE,
                BaselineTIFFTagSet.TAG_IMAGE_LENGTH,
                BaselineTIFFTagSet.TAG_IMAGE_WIDTH,
                BaselineTIFFTagSet.TAG_JPEG_AC_TABLES,
                BaselineTIFFTagSet.TAG_JPEG_DC_TABLES,
                BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT,
                BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                BaselineTIFFTagSet.TAG_JPEG_PROC,
                BaselineTIFFTagSet.TAG_JPEG_Q_TABLES,
                BaselineTIFFTagSet.TAG_JPEG_RESTART_INTERVAL,
                BaselineTIFFTagSet.TAG_JPEG_TABLES,
                BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION,
                BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION,
                BaselineTIFFTagSet.TAG_PREDICTOR,
                BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE,
                BaselineTIFFTagSet.TAG_ROWS_PER_STRIP,
                BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL,
                BaselineTIFFTagSet.TAG_SAMPLE_FORMAT,
                BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS,
                BaselineTIFFTagSet.TAG_STRIP_OFFSETS,
                BaselineTIFFTagSet.TAG_T4_OPTIONS,
                BaselineTIFFTagSet.TAG_T6_OPTIONS,
                BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS,
                BaselineTIFFTagSet.TAG_TILE_LENGTH,
                BaselineTIFFTagSet.TAG_TILE_OFFSETS,
                BaselineTIFFTagSet.TAG_TILE_WIDTH,
                BaselineTIFFTagSet.TAG_Y_CB_CR_COEFFICIENTS,
                BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING
            );
        }
    }

    /**
     * Converts a {@code TIFFDirectory} to a {@code TIFFIFD}.
     */
    public static TIFFIFD getDirectoryAsIFD(TIFFDirectory dir) {
        if(dir instanceof TIFFIFD) {
            return (TIFFIFD)dir;
        }

        TIFFIFD ifd = new TIFFIFD(Arrays.asList(dir.getTagSets()),
                                  dir.getParentTag());
        TIFFField[] fields = dir.getTIFFFields();
        int numFields = fields.length;
        for(int i = 0; i < numFields; i++) {
            TIFFField f = fields[i];
            TIFFTag tag = f.getTag();
            if(tag.isIFDPointer()) {
                TIFFDirectory subDir = null;
                if (f.hasDirectory()) {
                    subDir = f.getDirectory();
                } else if (f.getData() instanceof TIFFDirectory) {
                    subDir = (TIFFDirectory)f.getData();
                }
                if (subDir != null) {
                    TIFFDirectory subIFD = getDirectoryAsIFD(subDir);
                    f = new TIFFField(tag, f.getType(), (long)f.getCount(),
                                      subIFD);
                } else {
                    f = null;
                }
            }
            if (f != null) {
                ifd.addTIFFField(f);
            }
        }

        return ifd;
    }

    public static TIFFTag getTag(int tagNumber, List<TIFFTagSet> tagSets) {
        for (TIFFTagSet tagSet : tagSets) {
            TIFFTag tag = tagSet.getTag(tagNumber);
            if (tag != null) {
                return tag;
            }
        }

        return null;
    }

    public static TIFFTag getTag(String tagName, List<TIFFTagSet> tagSets) {
        for (TIFFTagSet tagSet : tagSets) {
            TIFFTag tag = tagSet.getTag(tagName);
            if (tag != null) {
                return tag;
            }
        }

        return null;
    }

    private static void writeTIFFFieldToStream(TIFFField field,
                                               ImageOutputStream stream)
        throws IOException {
        int count = field.getCount();
        Object data = field.getData();

        switch (field.getType()) {
        case TIFFTag.TIFF_ASCII:
            for (int i = 0; i < count; i++) {
                String s = ((String[])data)[i];
                int length = s.length();
                for (int j = 0; j < length; j++) {
                    stream.writeByte(s.charAt(j) & 0xff);
                }
                stream.writeByte(0);
            }
            break;
        case TIFFTag.TIFF_UNDEFINED:
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_SBYTE:
            stream.write((byte[])data);
            break;
        case TIFFTag.TIFF_SHORT:
            stream.writeChars((char[])data, 0, ((char[])data).length);
            break;
        case TIFFTag.TIFF_SSHORT:
            stream.writeShorts((short[])data, 0, ((short[])data).length);
            break;
        case TIFFTag.TIFF_SLONG:
            stream.writeInts((int[])data, 0, ((int[])data).length);
            break;
        case TIFFTag.TIFF_LONG:
            for (int i = 0; i < count; i++) {
                stream.writeInt((int)(((long[])data)[i]));
            }
            break;
        case TIFFTag.TIFF_IFD_POINTER:
            stream.writeInt(0); 
            break;
        case TIFFTag.TIFF_FLOAT:
            stream.writeFloats((float[])data, 0, ((float[])data).length);
            break;
        case TIFFTag.TIFF_DOUBLE:
            stream.writeDoubles((double[])data, 0, ((double[])data).length);
            break;
        case TIFFTag.TIFF_SRATIONAL:
            for (int i = 0; i < count; i++) {
                stream.writeInt(((int[][])data)[i][0]);
                stream.writeInt(((int[][])data)[i][1]);
            }
            break;
        case TIFFTag.TIFF_RATIONAL:
            for (int i = 0; i < count; i++) {
                long num = ((long[][])data)[i][0];
                long den = ((long[][])data)[i][1];
                stream.writeInt((int)num);
                stream.writeInt((int)den);
            }
            break;
        default:
        }
    }

    public TIFFIFD(List<TIFFTagSet> tagSets, TIFFTag parentTag) {
        super(tagSets.toArray(new TIFFTagSet[tagSets.size()]),
              parentTag);
    }

    public TIFFIFD(List<TIFFTagSet> tagSets) {
        this(tagSets, null);
    }

    public List<TIFFTagSet> getTagSetList() {
        return Arrays.asList(getTagSets());
    }

    /**
     * Returns an {@code Iterator} over the TIFF fields. The
     * traversal is in the order of increasing tag number.
     */
    public Iterator<TIFFField> iterator() {
        return Arrays.asList(getTIFFFields()).iterator();
    }

    /**
     * Read the value of a field. The {@code data} parameter should be
     * an array of length 1 of Object.
     *
     * @param stream the input stream
     * @param type the type as read from the stream
     * @param count the count read from the stream
     * @param data a container for the data
     * @return the updated count
     * @throws IOException
     */
    private static int readFieldValue(ImageInputStream stream,
        int type, int count, Object[] data) throws IOException {
        Object obj;
        final int UNIT_SIZE = 1024000;

        switch (type) {
            case TIFFTag.TIFF_BYTE:
            case TIFFTag.TIFF_SBYTE:
            case TIFFTag.TIFF_UNDEFINED:
            case TIFFTag.TIFF_ASCII:
                if (type == TIFFTag.TIFF_ASCII) {
                    byte[] bvalues = new byte[count];
                    stream.readFully(bvalues, 0, count);
                    ArrayList<String> v = new ArrayList<>();
                    boolean inString = false;
                    int prevIndex = 0;
                    for (int index = 0; index <= count; index++) {
                        if (index < count && bvalues[index] != 0) {
                            if (!inString) {
                                prevIndex = index;
                                inString = true;
                            }
                        } else { 
                            if (inString) {
                                String s = new String(bvalues, prevIndex,
                                        index - prevIndex, US_ASCII);
                                v.add(s);
                                inString = false;
                            }
                        }
                    }

                    count = v.size();
                    String[] strings;
                    if (count != 0) {
                        strings = new String[count];
                        for (int c = 0; c < count; c++) {
                            strings[c] = v.get(c);
                        }
                    } else {
                        count = 1;
                        strings = new String[]{""};
                    }

                    obj = strings;
                } else {
                    if (count < UNIT_SIZE) {
                        byte[] bvalues = new byte[count];
                        stream.readFully(bvalues, 0, count);
                        obj = bvalues;
                    } else {
                        int bytesToRead = count;
                        int bytesRead = 0;
                        List<byte[]> bufs = new ArrayList<>();
                        while (bytesToRead != 0) {
                            int sz = Math.min(bytesToRead, UNIT_SIZE);
                            byte[] unit = new byte[sz];
                            stream.readFully(unit, 0, sz);
                            bufs.add(unit);
                            bytesRead += sz;
                            bytesToRead -= sz;
                        }
                        byte[] tagData = new byte[bytesRead];
                        int copiedBytes = 0;
                        for (byte[] ba : bufs) {
                            System.arraycopy(ba, 0, tagData, copiedBytes, ba.length);
                            copiedBytes += ba.length;
                        }
                        obj = tagData;
                    }
                }
                break;

            case TIFFTag.TIFF_SHORT:
                final int SHORT_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_SHORT);
                if (count < SHORT_TILE_SIZE) {
                    char[] cvalues = new char[count];
                    for (int j = 0; j < count; j++) {
                        cvalues[j] = (char) (stream.readUnsignedShort());
                    }
                    obj = cvalues;
                } else {
                    int charsToRead = count;
                    int charsRead = 0;
                    List<char[]> bufs = new ArrayList<>();
                    while (charsToRead != 0) {
                        int sz = Math.min(charsToRead, SHORT_TILE_SIZE);
                        char[] unit = new char[sz];
                        for (int i = 0; i < sz ; i++) {
                            unit[i] = (char) (stream.readUnsignedShort());
                        }
                        bufs.add(unit);
                        charsRead += sz;
                        charsToRead -= sz;
                    }
                    char[] tagData = new char[charsRead];
                    int copiedChars = 0;
                    for (char[] ca : bufs) {
                        System.arraycopy(ca, 0, tagData, copiedChars, ca.length);
                        copiedChars += ca.length;
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_LONG:
            case TIFFTag.TIFF_IFD_POINTER:
                final int LONG_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_LONG);
                if (count < LONG_TILE_SIZE) {
                    long[] lvalues = new long[count];
                    for (int j = 0; j < count; j++) {
                        lvalues[j] = stream.readUnsignedInt();
                    }
                    obj = lvalues;
                } else {
                    int longsToRead = count;
                    int longsRead = 0;
                    List<long[]> bufs = new ArrayList<>();
                    while (longsToRead != 0) {
                        int sz = Math.min(longsToRead, LONG_TILE_SIZE);
                        long[] unit = new long[sz];
                        for (int i = 0; i < sz ; i++) {
                            unit[i] = stream.readUnsignedInt();
                        }
                        bufs.add(unit);
                        longsRead += sz;
                        longsToRead -= sz;
                    }
                    long[] tagData = new long[longsRead];
                    int copiedLongs = 0;
                    for (long[] la : bufs) {
                        System.arraycopy(la, 0, tagData, copiedLongs, la.length);
                        copiedLongs += la.length;
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_RATIONAL:
                final int RATIONAL_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_RATIONAL);
                if (count < RATIONAL_TILE_SIZE) {
                    long[][] llvalues = new long[count][2];
                    for (int j = 0; j < count; j++) {
                        llvalues[j][0] = stream.readUnsignedInt();
                        llvalues[j][1] = stream.readUnsignedInt();
                    }
                    obj = llvalues;
                } else {
                    int rationalsToRead = count;
                    int rationalsRead = 0;
                    List<long[]> bufs = new ArrayList<>();
                    while (rationalsToRead != 0) {
                        int sz = Math.min(rationalsToRead, RATIONAL_TILE_SIZE);
                        long[] unit = new long[sz * 2];
                        for (int i = 0; i < (sz * 2) ; i++) {
                            unit[i] = stream.readUnsignedInt();
                        }
                        bufs.add(unit);
                        rationalsRead += sz;
                        rationalsToRead -= sz;
                    }
                    long[][] tagData = new long[rationalsRead][2];
                    int copiedRationals = 0;
                    for (long[] la : bufs) {
                        for (int i = 0; i < la.length; i = i + 2) {
                            tagData[copiedRationals + i][0] = la[i];
                            tagData[copiedRationals + i][1] = la[i + 1];
                        }
                        copiedRationals += (la.length / 2);
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_SSHORT:
                final int SSHORT_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_SSHORT);
                if (count < SSHORT_TILE_SIZE) {
                    short[] svalues = new short[count];
                    for (int j = 0; j < count; j++) {
                        svalues[j] = stream.readShort();
                    }
                    obj = svalues;
                } else {
                    int shortsToRead = count;
                    int shortsRead = 0;
                    List<short[]> bufs = new ArrayList<>();
                    while (shortsToRead != 0) {
                        int sz = Math.min(shortsToRead, SSHORT_TILE_SIZE);
                        short[] unit = new short[sz];
                        stream.readFully(unit, 0, sz);
                        bufs.add(unit);
                        shortsRead += sz;
                        shortsToRead -= sz;
                    }
                    short[] tagData = new short[shortsRead];
                    int copiedShorts = 0;
                    for (short[] sa : bufs) {
                        System.arraycopy(sa, 0, tagData, copiedShorts, sa.length);
                        copiedShorts += sa.length;
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_SLONG:
                final int INT_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_SLONG);
                if (count < INT_TILE_SIZE) {
                    int[] ivalues = new int[count];
                    for (int j = 0; j < count; j++) {
                        ivalues[j] = stream.readInt();
                    }
                    obj = ivalues;
                } else {
                    int intsToRead = count;
                    int intsRead = 0;
                    List<int[]> bufs = new ArrayList<>();
                    while (intsToRead != 0) {
                        int sz = Math.min(intsToRead, INT_TILE_SIZE);
                        int[] unit = new int[sz];
                        stream.readFully(unit, 0, sz);
                        bufs.add(unit);
                        intsRead += sz;
                        intsToRead -= sz;
                    }
                    int[] tagData = new int[intsRead];
                    int copiedInts = 0;
                    for (int[] ia : bufs) {
                        System.arraycopy(ia, 0, tagData, copiedInts, ia.length);
                        copiedInts += ia.length;
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_SRATIONAL:
                final int SRATIONAL_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_SRATIONAL);
                if (count < SRATIONAL_TILE_SIZE) {
                    int[][] iivalues = new int[count][2];
                    for (int j = 0; j < count; j++) {
                        iivalues[j][0] = stream.readInt();
                        iivalues[j][1] = stream.readInt();
                    }
                    obj = iivalues;
                } else {
                    int srationalsToRead = count;
                    int srationalsRead = 0;
                    List<int[]> bufs = new ArrayList<>();
                    while (srationalsToRead != 0) {
                        int sz = Math.min(srationalsToRead, SRATIONAL_TILE_SIZE);
                        int[] unit = new int[sz * 2];
                        stream.readFully(unit, 0, (sz * 2));
                        bufs.add(unit);
                        srationalsRead += sz;
                        srationalsToRead -= sz;
                    }
                    int[][] tagData = new int[srationalsRead][2];
                    int copiedSrationals = 0;
                    for (int[] ia : bufs) {
                        for (int i = 0; i < ia.length; i = i + 2) {
                            tagData[copiedSrationals + i][0] = ia[i];
                            tagData[copiedSrationals + i][1] = ia[i + 1];
                        }
                        copiedSrationals += (ia.length / 2);
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_FLOAT:
                final int FLOAT_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_FLOAT);
                if (count < FLOAT_TILE_SIZE) {
                    float[] fvalues = new float[count];
                    for (int j = 0; j < count; j++) {
                        fvalues[j] = stream.readFloat();
                    }
                    obj = fvalues;
                } else {
                    int floatsToRead = count;
                    int floatsRead = 0;
                    List<float[]> bufs = new ArrayList<>();
                    while (floatsToRead != 0) {
                        int sz = Math.min(floatsToRead, FLOAT_TILE_SIZE);
                        float[] unit = new float[sz];
                        stream.readFully(unit, 0, sz);
                        bufs.add(unit);
                        floatsRead += sz;
                        floatsToRead -= sz;
                    }
                    float[] tagData = new float[floatsRead];
                    int copiedFloats = 0;
                    for (float[] fa : bufs) {
                        System.arraycopy(fa, 0, tagData, copiedFloats, fa.length);
                        copiedFloats += fa.length;
                    }
                    obj = tagData;
                }
                break;

            case TIFFTag.TIFF_DOUBLE:
                final int DOUBLE_TILE_SIZE =
                    UNIT_SIZE / TIFFTag.getSizeOfType(TIFFTag.TIFF_DOUBLE);
                if (count < DOUBLE_TILE_SIZE) {
                    double[] dvalues = new double[count];
                    for (int j = 0; j < count; j++) {
                        dvalues[j] = stream.readDouble();
                    }
                    obj = dvalues;
                } else {
                    int doublesToRead = count;
                    int doublesRead = 0;
                    List<double[]> bufs = new ArrayList<>();
                    while (doublesToRead != 0) {
                        int sz = Math.min(doublesToRead, DOUBLE_TILE_SIZE);
                        double[] unit = new double[sz];
                        stream.readFully(unit, 0, sz);
                        bufs.add(unit);
                        doublesRead += sz;
                        doublesToRead -= sz;
                    }
                    double[] tagData = new double[doublesRead];
                    int copiedDoubles = 0;
                    for (double[] da : bufs) {
                        System.arraycopy(da, 0, tagData, copiedDoubles, da.length);
                        copiedDoubles += da.length;
                    }
                    obj = tagData;
                }
                break;
            default:
                obj = null;
                break;
        }

        data[0] = obj;

        return count;
    }

    private static class TIFFIFDEntry {
        public final TIFFTag tag;
        public final int type;
        public final int count;
        public final long offset;

        TIFFIFDEntry(TIFFTag tag, int type, int count, long offset) {
            this.tag = tag;
            this.type = type;
            this.count = count;
            this.offset = offset;
        }
    }

    private long getFieldAsLong(int tagNumber) {
        TIFFField f = getTIFFField(tagNumber);
        return f == null ? -1 : f.getAsLong(0);
    }

    private int getFieldAsInt(int tagNumber) {
        TIFFField f = getTIFFField(tagNumber);
        return f == null ? -1 : f.getAsInt(0);
    }

    private boolean calculateByteCounts(int expectedSize,
        List<TIFFField> byteCounts) {
        if (!byteCounts.isEmpty()) {
            throw new IllegalArgumentException("byteCounts is not empty");
        }

        if (getFieldAsInt(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION) ==
            BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
            return false;
        }

        if (getFieldAsInt(BaselineTIFFTagSet.TAG_COMPRESSION) !=
            BaselineTIFFTagSet.COMPRESSION_NONE) {
            return false;
        }

        long w = getFieldAsLong(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
        if (w < 0) {
            return false;
        }
        long h = getFieldAsLong(BaselineTIFFTagSet.TAG_IMAGE_LENGTH);
        if (h < 0) {
            return false;
        }

        long tw = getFieldAsLong(BaselineTIFFTagSet.TAG_TILE_WIDTH);
        if (tw < 0) {
            tw = w;
        }
        long th = getFieldAsLong(BaselineTIFFTagSet.TAG_TILE_LENGTH);
        if (th < 0) {
            th = getFieldAsLong(BaselineTIFFTagSet.TAG_ROWS_PER_STRIP);
            if (th < 0) {
                th = h;
            }
        }

        int[] bitsPerSample = null;
        TIFFField f = getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
        if (f != null) {
            bitsPerSample = f.getAsInts();
        } else {
            int samplesPerPixel =
                getFieldAsInt(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
            if (samplesPerPixel < 0) {
                samplesPerPixel = 1;
            }
            bitsPerSample = new int[samplesPerPixel];
            Arrays.fill(bitsPerSample, 8);
        }

        int bitsPerPixel = 0;
        for (int bps : bitsPerSample) {
            bitsPerPixel += bps;
        }

        int bytesPerRow = (int)(tw*bitsPerPixel + 7)/8;
        int bytesPerPacket = (int)th*bytesPerRow;

        long nx = (w + tw - 1)/tw;
        long ny = (h + th - 1)/th;

        if (nx*ny != expectedSize) {
            return false;
        }

        boolean isTiled =
            getTIFFField(BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS) != null;

        int tagNumber;
        if (isTiled) {
            tagNumber = BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS;
        } else {
            tagNumber = BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS;
        }

        TIFFTag t = BaselineTIFFTagSet.getInstance().getTag(tagNumber);
        f = getTIFFField(tagNumber);
        if (f != null) {
            removeTIFFField(tagNumber);
        }

        int numPackets = (int)(nx*ny);
        long[] packetByteCounts = new long[numPackets];
        Arrays.fill(packetByteCounts, bytesPerPacket);

        if (tw <= w && h % th != 0) {
            int numRowsInLastStrip = (int)(h - (ny - 1)*th);
            packetByteCounts[numPackets - 1] = numRowsInLastStrip*bytesPerRow;
        }

        f = new TIFFField(t, TIFFTag.TIFF_LONG, numPackets, packetByteCounts);
        addTIFFField(f);
        byteCounts.add(f);

        return true;
    }

    private void checkFieldOffsets(long streamLength) throws IIOException {
        if (streamLength < 0) {
            return;
        }

        List<TIFFField> offsets = new ArrayList<>();
        TIFFField f = getTIFFField(BaselineTIFFTagSet.TAG_STRIP_OFFSETS);
        int count = 0;
        if (f != null) {
            count = f.getCount();
            offsets.add(f);
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_TILE_OFFSETS);
        if (f != null) {
            int sz = offsets.size();
            int newCount = f.getCount();
            if (sz > 0 && newCount != count) {
                throw new IIOException
                    ("StripOffsets count != TileOffsets count");
            }

            if (sz == 0) {
                count = newCount;
            }
            offsets.add(f);
        }

        List<TIFFField> byteCounts = new ArrayList<>();
        if (offsets.size() > 0) {
            f = getTIFFField(BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS);
            if (f != null) {
                if (f.getCount() != count) {
                    throw new IIOException
                        ("StripByteCounts count != number of offsets");
                }
                byteCounts.add(f);
            }

            f = getTIFFField(BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS);
            if (f != null) {
                if (f.getCount() != count) {
                    throw new IIOException
                        ("TileByteCounts count != number of offsets");
                }
                byteCounts.add(f);
            }

            if (byteCounts.size() > 0) {
                for (TIFFField offset : offsets) {
                    for (TIFFField byteCount : byteCounts) {
                        for (int i = 0; i < count; i++) {
                            long dataOffset = offset.getAsLong(i);
                            long dataByteCount = byteCount.getAsLong(i);
                            if (dataOffset + dataByteCount > streamLength) {
                                throw new IIOException
                                    ("Data segment out of stream");
                            }
                        }
                    }
                }
            }
        }

        TIFFField jpegOffset =
            getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT);
        if (jpegOffset != null) {
            TIFFField jpegLength =
                getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
            if (jpegLength != null) {
                if (jpegOffset.getAsLong(0) + jpegLength.getAsLong(0)
                    > streamLength) {
                    throw new IIOException
                        ("JPEGInterchangeFormat data out of stream");
                }
            }
        }

        if (jpegOffset == null
            && (offsets.size() == 0 || byteCounts.size() == 0)) {
            boolean throwException = true;
            if (offsets.size() != 0 && byteCounts.size() == 0) {
                int expectedSize = offsets.get(0).getCount();
                throwException =
                    !calculateByteCounts(expectedSize, byteCounts);
            }
            if (throwException) {
                throw new IIOException
                    ("Insufficient data offsets or byte counts");
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_JPEG_Q_TABLES);
        if (f != null) {
            long[] tableOffsets = f.getAsLongs();
            for (long off : tableOffsets) {
                if (off + 64 > streamLength) {
                    throw new IIOException("JPEGQTables data out of stream");
                }
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_JPEG_DC_TABLES);
        if (f != null) {
            long[] tableOffsets = f.getAsLongs();
            for (long off : tableOffsets) {
                if (off + 16 > streamLength) {
                    throw new IIOException("JPEGDCTables data out of stream");
                }
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_JPEG_AC_TABLES);
        if (f != null) {
            long[] tableOffsets = f.getAsLongs();
            for (long off : tableOffsets) {
                if (off + 16 > streamLength) {
                    throw new IIOException("JPEGACTables data out of stream");
                }
            }
        }
    }

    public void initialize(ImageInputStream stream, boolean isPrimaryIFD,
        boolean ignoreMetadata, boolean readUnknownTags) throws IOException {

        removeTIFFFields();

        long streamLength = stream.length();
        boolean haveStreamLength = streamLength != -1;

        List<TIFFTagSet> tagSetList = getTagSetList();

        boolean ensureEssentialTags = false;
        TIFFTagSet baselineTagSet = null;
        if (isPrimaryIFD &&
            (ignoreMetadata ||
             (!readUnknownTags &&
              !tagSetList.contains(BaselineTIFFTagSet.getInstance())))) {
            ensureEssentialTags = true;
            initializeEssentialTags();
            baselineTagSet = BaselineTIFFTagSet.getInstance();
        }

        List<Object> entries = new ArrayList<>();
        Object[] entryData = new Object[1]; 

        int numEntries = stream.readUnsignedShort();
        for (int i = 0; i < numEntries; i++) {
            int tagNumber = stream.readUnsignedShort();
            int type = stream.readUnsignedShort();
            int sizeOfType;
            try {
                sizeOfType = TIFFTag.getSizeOfType(type);
            } catch (IllegalArgumentException ignored) {
                stream.skipBytes(4);
                continue;
            }
            long longCount = stream.readUnsignedInt();

            TIFFTag tag = getTag(tagNumber, tagSetList);

            if (tag == null && ensureEssentialTags
                && essentialTags.contains(tagNumber)) {
                tag = baselineTagSet.getTag(tagNumber);
            }

            if((ignoreMetadata &&
                (!ensureEssentialTags || !essentialTags.contains(tagNumber)))
                || (tag == null && !readUnknownTags)
                || (tag != null && !tag.isDataTypeOK(type))
                || longCount > Integer.MAX_VALUE) {
                stream.skipBytes(4);

                continue;
            }

            int count = (int)longCount;

            if (tag == null) {
                tag = new TIFFTag(TIFFTag.UNKNOWN_TAG_NAME, tagNumber,
                    1 << type, count);
            } else {
                int expectedCount = tag.getCount();
                if (expectedCount > 0) {
                    if (count != expectedCount) {
                        throw new IIOException("Unexpected count "
                            + count + " for " + tag.getName() + " field");
                    }
                } else if (type == TIFFTag.TIFF_ASCII) {
                    int asciiSize = TIFFTag.getSizeOfType(TIFFTag.TIFF_ASCII);
                    if (count*asciiSize > MAX_ASCII_SIZE) {
                        count = (int)(MAX_ASCII_SIZE/asciiSize);
                    }
                }
            }

            long longSize = longCount*sizeOfType;
            if (longSize > Integer.MAX_VALUE) {
                stream.skipBytes(4);
                continue;
            }
            int size = (int)longSize;

            if (size > 4 || tag.isIFDPointer()) {
                long offset = stream.readUnsignedInt();

                if (haveStreamLength && offset + size > streamLength) {
                    continue;
                }

                entries.add(new TIFFIFDEntry(tag, type, count, offset));
            } else {
                Object obj = null;
                try {
                    count = readFieldValue(stream, type, count, entryData);
                    obj = entryData[0];
                } catch (EOFException eofe) {
                    if (BaselineTIFFTagSet.getInstance().getTag(tagNumber) == null) {
                        throw eofe;
                    }
                }

                if (size < 4) {
                    stream.skipBytes(4 - size);
                }

                entries.add(new TIFFField(tag, type, count, obj));
            }
        }

        long nextIFDOffset = stream.getStreamPosition();

        Object[] fieldData = new Object[1];
        for (Object entry : entries) {
            if (entry instanceof TIFFField) {
                addTIFFField((TIFFField)entry);
            } else {
                TIFFIFDEntry e = (TIFFIFDEntry)entry;
                TIFFTag tag = e.tag;
                int tagNumber = tag.getNumber();
                int type = e.type;
                int count = e.count;

                stream.seek(e.offset);

                if (tag.isIFDPointer()) {
                    List<TIFFTagSet> tagSets = new ArrayList<TIFFTagSet>(1);
                    tagSets.add(tag.getTagSet());
                    TIFFIFD subIFD = new TIFFIFD(tagSets);

                    subIFD.initialize(stream, false, ignoreMetadata,
                                      readUnknownTags);
                    TIFFField f = new TIFFField(tag, type, e.offset, subIFD);
                    addTIFFField(f);
                } else {
                    if (tagNumber == BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS
                            || tagNumber == BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS
                            || tagNumber == BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH) {
                        this.stripOrTileByteCountsPosition
                                = stream.getStreamPosition();
                    } else if (tagNumber == BaselineTIFFTagSet.TAG_STRIP_OFFSETS
                            || tagNumber == BaselineTIFFTagSet.TAG_TILE_OFFSETS
                            || tagNumber == BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT) {
                        this.stripOrTileOffsetsPosition
                                = stream.getStreamPosition();
                    }

                    Object obj = null;
                    try {
                        count = readFieldValue(stream, type, count, fieldData);
                        obj = fieldData[0];
                    } catch (EOFException eofe) {
                        if (BaselineTIFFTagSet.getInstance().getTag(tagNumber) != null) {
                            throw eofe;
                        }
                    }

                    if (obj == null) {
                        continue;
                    }

                    TIFFField f = new TIFFField(tag, type, count, obj);
                    addTIFFField(f);
                }
            }
        }

        if(isPrimaryIFD && haveStreamLength) {
            checkFieldOffsets(streamLength);
        }

        stream.seek(nextIFDOffset);
        this.lastPosition = stream.getStreamPosition();
    }

    public void writeToStream(ImageOutputStream stream)
        throws IOException {

        int numFields = getNumTIFFFields();
        stream.writeShort(numFields);

        long nextSpace = stream.getStreamPosition() + 12*numFields + 4;

        Iterator<TIFFField> iter = iterator();
        while (iter.hasNext()) {
            TIFFField f = iter.next();

            TIFFTag tag = f.getTag();

            int type = f.getType();
            int count = f.getCount();

            if (type == 0) {
                type = TIFFTag.TIFF_UNDEFINED;
            }
            int size = count*TIFFTag.getSizeOfType(type);

            if (type == TIFFTag.TIFF_ASCII) {
                int chars = 0;
                for (int i = 0; i < count; i++) {
                    chars += f.getAsString(i).length() + 1;
                }
                count = chars;
                size = count;
            }

            int tagNumber = f.getTagNumber();
            stream.writeShort(tagNumber);
            stream.writeShort(type);
            stream.writeInt(count);

            stream.writeInt(0);
            stream.mark(); 
            stream.skipBytes(-4);

            long pos;

            if (size > 4 || tag.isIFDPointer()) {
                nextSpace = (nextSpace + 3) & ~0x3;

                stream.writeInt((int)nextSpace);
                stream.seek(nextSpace);
                pos = nextSpace;

                if (tag.isIFDPointer() && f.hasDirectory()) {
                    TIFFIFD subIFD = getDirectoryAsIFD(f.getDirectory());
                    subIFD.writeToStream(stream);
                    nextSpace = subIFD.lastPosition;
                } else {
                    writeTIFFFieldToStream(f, stream);
                    nextSpace = stream.getStreamPosition();
                }
            } else {
                pos = stream.getStreamPosition();
                writeTIFFFieldToStream(f, stream);
            }

            if (tagNumber ==
                BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS ||
                tagNumber == BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS ||
                tagNumber == BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH) {
                this.stripOrTileByteCountsPosition = pos;
            } else if (tagNumber ==
                       BaselineTIFFTagSet.TAG_STRIP_OFFSETS ||
                       tagNumber ==
                       BaselineTIFFTagSet.TAG_TILE_OFFSETS ||
                       tagNumber ==
                       BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT) {
                this.stripOrTileOffsetsPosition = pos;
            }

            stream.reset(); 
        }

        this.lastPosition = nextSpace;
    }

    public long getStripOrTileByteCountsPosition() {
        return stripOrTileByteCountsPosition;
    }

    public long getStripOrTileOffsetsPosition() {
        return stripOrTileOffsetsPosition;
    }

    public long getLastPosition() {
        return lastPosition;
    }

    void setPositions(long stripOrTileOffsetsPosition,
                      long stripOrTileByteCountsPosition,
                      long lastPosition) {
        this.stripOrTileOffsetsPosition = stripOrTileOffsetsPosition;
        this.stripOrTileByteCountsPosition = stripOrTileByteCountsPosition;
        this.lastPosition = lastPosition;
    }

    /**
     * Returns a {@code TIFFIFD} wherein all fields from the
     * {@code BaselineTIFFTagSet} are copied by value and all other
     * fields copied by reference.
     */
    public TIFFIFD getShallowClone() {
        TIFFTagSet baselineTagSet = BaselineTIFFTagSet.getInstance();

        List<TIFFTagSet> tagSetList = getTagSetList();
        if(!tagSetList.contains(baselineTagSet)) {
            return this;
        }

        TIFFIFD shallowClone = new TIFFIFD(tagSetList, getParentTag());

        Set<Integer> baselineTagNumbers = baselineTagSet.getTagNumbers();

        Iterator<TIFFField> fields = iterator();
        while(fields.hasNext()) {
            TIFFField field = fields.next();

            Integer tagNumber = Integer.valueOf(field.getTagNumber());

            TIFFField fieldClone;
            if(baselineTagNumbers.contains(tagNumber)) {
                Object fieldData = field.getData();

                int fieldType = field.getType();

                try {
                    switch (fieldType) {
                    case TIFFTag.TIFF_BYTE:
                    case TIFFTag.TIFF_SBYTE:
                    case TIFFTag.TIFF_UNDEFINED:
                        fieldData = ((byte[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_ASCII:
                        fieldData = ((String[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SHORT:
                        fieldData = ((char[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_LONG:
                    case TIFFTag.TIFF_IFD_POINTER:
                        fieldData = ((long[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_RATIONAL:
                        fieldData = ((long[][])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SSHORT:
                        fieldData = ((short[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SLONG:
                        fieldData = ((int[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SRATIONAL:
                        fieldData = ((int[][])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_FLOAT:
                        fieldData = ((float[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_DOUBLE:
                        fieldData = ((double[])fieldData).clone();
                        break;
                    default:
                    }
                } catch(Exception e) {
                }

                fieldClone = new TIFFField(field.getTag(), fieldType,
                                           field.getCount(), fieldData);
            } else {
                fieldClone = field;
            }

            shallowClone.addTIFFField(fieldClone);
        }

        shallowClone.setPositions(stripOrTileOffsetsPosition,
                                  stripOrTileByteCountsPosition,
                                  lastPosition);

        return shallowClone;
    }
}
