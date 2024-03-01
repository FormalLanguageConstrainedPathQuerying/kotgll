/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOException;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;

/**
 * {@code TIFFDecompressor} for "Old JPEG" compression.
 */
public class TIFFOldJPEGDecompressor extends TIFFJPEGDecompressor {

    private static final int DHT = 0xC4;

    private static final int DQT = 0xDB;

    private static final int DRI = 0xDD;

    private static final int SOF0 = 0xC0;

    private static final int SOS = 0xDA;


    private boolean isInitialized = false;

    private Long JPEGStreamOffset = null;
    private int SOFPosition = -1;
    private byte[] SOSMarker = null;

    private int subsamplingX = 2;

    private int subsamplingY = 2;

    public TIFFOldJPEGDecompressor() {}

    private synchronized void initialize() throws IOException {
        if(isInitialized) {
            return;
        }

        TIFFImageMetadata tim = (TIFFImageMetadata)metadata;

        TIFFField JPEGInterchangeFormatField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT);

        TIFFField segmentOffsetField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_TILE_OFFSETS);
        if(segmentOffsetField == null) {
            segmentOffsetField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_STRIP_OFFSETS);
            if(segmentOffsetField == null) {
                segmentOffsetField = JPEGInterchangeFormatField;
            }
        }
        long[] segmentOffsets = segmentOffsetField.getAsLongs();

        boolean isTiled = segmentOffsets.length > 1;

        if(!isTiled) {

            stream.seek(offset);
            stream.mark();
            if(stream.read() == 0xff && stream.read() == SOI) {
                JPEGStreamOffset = Long.valueOf(offset);

                ((TIFFImageReader)reader).forwardWarningMessage("SOI marker detected at start of strip or tile.");
                isInitialized = true;
                return;
            }
            stream.reset();

            if(JPEGInterchangeFormatField != null) {
                long jpegInterchangeOffset =
                    JPEGInterchangeFormatField.getAsLong(0);

                TIFFField JPEGInterchangeFormatLengthField =
                    tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

                if(JPEGInterchangeFormatLengthField == null) {
                    JPEGStreamOffset = Long.valueOf(jpegInterchangeOffset);

                    ((TIFFImageReader)reader).forwardWarningMessage("JPEGInterchangeFormatLength field is missing");
                    isInitialized = true;
                    return;
                } else {
                    long jpegInterchangeLength =
                        JPEGInterchangeFormatLengthField.getAsLong(0);

                    if(jpegInterchangeOffset < segmentOffsets[0] &&
                       (jpegInterchangeOffset + jpegInterchangeLength) >
                       segmentOffsets[0]) {
                        JPEGStreamOffset = Long.valueOf(jpegInterchangeOffset);

                        isInitialized = true;
                        return;
                    }
                }
            }
        }

        TIFFField YCbCrSubsamplingField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING);
        if(YCbCrSubsamplingField != null) {
            subsamplingX = YCbCrSubsamplingField.getAsChars()[0];
            subsamplingY = YCbCrSubsamplingField.getAsChars()[1];
        }

        if(JPEGInterchangeFormatField != null) {
            long jpegInterchangeOffset =
                JPEGInterchangeFormatField.getAsLong(0);

            TIFFField JPEGInterchangeFormatLengthField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

            if(JPEGInterchangeFormatLengthField != null) {
                long jpegInterchangeLength =
                    JPEGInterchangeFormatLengthField.getAsLong(0);

                if(jpegInterchangeLength >= 2 &&
                   jpegInterchangeOffset + jpegInterchangeLength <=
                   segmentOffsets[0]) {
                    stream.mark();
                    stream.seek(jpegInterchangeOffset+jpegInterchangeLength-2);
                    if(stream.read() == 0xff && stream.read() == EOI) {
                        this.tables = new byte[(int)(jpegInterchangeLength-2)];
                    } else {
                        this.tables = new byte[(int)jpegInterchangeLength];
                    }
                    stream.reset();

                    stream.mark();
                    stream.seek(jpegInterchangeOffset);
                    stream.readFully(tables);
                    stream.reset();

                    ((TIFFImageReader)reader).forwardWarningMessage("Incorrect JPEG interchange format: using JPEGInterchangeFormat offset to derive tables.");
                } else {
                    ((TIFFImageReader)reader).forwardWarningMessage("JPEGInterchangeFormat+JPEGInterchangeFormatLength > offset to first strip or tile.");
                }
            }
        }

        if(this.tables == null) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            long streamLength = stream.length();

            baos.write(0xff);
            baos.write(SOI);

            TIFFField f =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_Q_TABLES);
            if(f == null) {
                throw new IIOException("JPEGQTables field missing!");
            }
            long[] off = f.getAsLongs();

            for(int i = 0; i < off.length; i++) {
                baos.write(0xff); 
                baos.write(DQT);

                char markerLength = (char)67;
                baos.write((markerLength >>> 8) & 0xff); 
                baos.write(markerLength & 0xff);

                baos.write(i); 

                byte[] qtable = new byte[64];
                if(streamLength != -1 && off[i] > streamLength) {
                    throw new IIOException("JPEGQTables offset for index "+
                                           i+" is not in the stream!");
                }
                stream.seek(off[i]);
                stream.readFully(qtable);

                baos.write(qtable); 
            }

            for(int k = 0; k < 2; k++) {
                int tableTagNumber = k == 0 ?
                    BaselineTIFFTagSet.TAG_JPEG_DC_TABLES :
                    BaselineTIFFTagSet.TAG_JPEG_AC_TABLES;
                f = tim.getTIFFField(tableTagNumber);
                String fieldName =
                    tableTagNumber ==
                    BaselineTIFFTagSet.TAG_JPEG_DC_TABLES ?
                    "JPEGDCTables" : "JPEGACTables";

                if(f == null) {
                    throw new IIOException(fieldName+" field missing!");
                }
                off = f.getAsLongs();

                for(int i = 0; i < off.length; i++) {
                    baos.write(0xff); 
                    baos.write(DHT);

                    byte[] blengths = new byte[16];
                    if(streamLength != -1 && off[i] > streamLength) {
                        throw new IIOException(fieldName+" offset for index "+
                                               i+" is not in the stream!");
                    }
                    stream.seek(off[i]);
                    stream.readFully(blengths);
                    int numCodes = 0;
                    for(int j = 0; j < 16; j++) {
                        numCodes += blengths[j]&0xff;
                    }

                    char markerLength = (char)(19 + numCodes);

                    baos.write((markerLength >>> 8) & 0xff); 
                    baos.write(markerLength & 0xff);

                    baos.write(i | (k << 4)); 

                    baos.write(blengths); 

                    byte[] bcodes = new byte[numCodes];
                    stream.readFully(bcodes);
                    baos.write(bcodes); 
                }
            }

            baos.write((byte)0xff); 
            baos.write((byte)SOF0);
            short sval = (short)(8 + 3*samplesPerPixel); 
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            baos.write((byte)8); 
            sval = (short)srcHeight; 
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            sval = (short)srcWidth; 
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            baos.write((byte)samplesPerPixel); 
            if(samplesPerPixel == 1) {
                baos.write((byte)1); 
                baos.write((byte)0x11); 
                baos.write((byte)0); 
            } else { 
                for(int i = 0; i < 3; i++) {
                    baos.write((byte)(i + 1)); 
                    baos.write((i != 0) ?
                               (byte)0x11 :
                               (byte)(((subsamplingX & 0x0f) << 4) |
                                      (subsamplingY & 0x0f)));

                    baos.write((byte)i); 
                }
            }


            f = tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_RESTART_INTERVAL);
            if(f != null) {
                char restartInterval = f.getAsChars()[0];

                if(restartInterval != 0) {
                    baos.write((byte)0xff); 
                    baos.write((byte)DRI);

                    sval = 4;
                    baos.write((byte)((sval >>> 8) & 0xff)); 
                    baos.write((byte)(sval & 0xff));

                    baos.write((byte)((restartInterval >>> 8) & 0xff));
                    baos.write((byte)(restartInterval & 0xff));
                }
            }

            tables = baos.toByteArray();
        }

        int idx = 0;
        int idxMax = tables.length - 1;
        while(idx < idxMax) {
            if((tables[idx]&0xff) == 0xff &&
               (tables[idx+1]&0xff) == SOF0) {
                SOFPosition = idx;
                break;
            }
            idx++;
        }

        if(SOFPosition == -1) {
            byte[] tmpTables =
                new byte[tables.length + 10 + 3*samplesPerPixel];
            System.arraycopy(tables, 0, tmpTables, 0, tables.length);
            int tmpOffset = tables.length;
            SOFPosition = tables.length;
            tables = tmpTables;

            tables[tmpOffset++] = (byte)0xff; 
            tables[tmpOffset++] = (byte)SOF0;
            short sval = (short)(8 + 3*samplesPerPixel); 
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            tables[tmpOffset++] = (byte)8; 
            sval = (short)srcHeight; 
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            sval = (short)srcWidth; 
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            tables[tmpOffset++] = (byte)samplesPerPixel; 
            if(samplesPerPixel == 1) {
                tables[tmpOffset++] = (byte)1; 
                tables[tmpOffset++] = (byte)0x11; 
                tables[tmpOffset++] = (byte)0; 
            } else { 
                for(int i = 0; i < 3; i++) {
                    tables[tmpOffset++] = (byte)(i + 1); 
                    tables[tmpOffset++] = (i != 0) ?
                        (byte)0x11 :
                        (byte)(((subsamplingX & 0x0f) << 4) |
                               (subsamplingY & 0x0f));

                    tables[tmpOffset++] = (byte)i; 
                }
            }
        }

        stream.mark();
        stream.seek(segmentOffsets[0]);
        if(stream.read() == 0xff && stream.read() == SOS) {
            int SOSLength = (stream.read()<<8)|stream.read();
            SOSMarker = new byte[SOSLength+2];
            SOSMarker[0] = (byte)0xff;
            SOSMarker[1] = (byte)SOS;
            SOSMarker[2] = (byte)((SOSLength & 0xff00) >> 8);
            SOSMarker[3] = (byte)(SOSLength & 0xff);
            stream.readFully(SOSMarker, 4, SOSLength - 2);
        } else {
            SOSMarker = new byte[2 + 6 + 2*samplesPerPixel];
            int SOSMarkerIndex = 0;
            SOSMarker[SOSMarkerIndex++] = (byte)0xff; 
            SOSMarker[SOSMarkerIndex++] = (byte)SOS;
            short sval = (short)(6 + 2*samplesPerPixel); 
            SOSMarker[SOSMarkerIndex++] = (byte)((sval >>> 8) & 0xff);
            SOSMarker[SOSMarkerIndex++] = (byte)(sval & 0xff);
            SOSMarker[SOSMarkerIndex++] = (byte)samplesPerPixel;
            if(samplesPerPixel == 1) {
                SOSMarker[SOSMarkerIndex++] = (byte)1; 
                SOSMarker[SOSMarkerIndex++] = (byte)0; 
            } else { 
                for(int i = 0; i < 3; i++) {
                    SOSMarker[SOSMarkerIndex++] =
                        (byte)(i + 1); 
                    SOSMarker[SOSMarkerIndex++] =
                        (byte)((i << 4) | i); 
                }
            }
            SOSMarker[SOSMarkerIndex++] = (byte)0;
            SOSMarker[SOSMarkerIndex++] = (byte)0x3f;
            SOSMarker[SOSMarkerIndex++] = (byte)0;
        }
        stream.reset();

        isInitialized = true;
    }

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {

        initialize();

        TIFFImageMetadata tim = (TIFFImageMetadata)metadata;

        if(JPEGStreamOffset != null) {
            stream.seek(JPEGStreamOffset.longValue());
            JPEGReader.setInput(stream, false, true);
        } else {
            int tableLength = tables.length;
            int bufLength =
                tableLength + SOSMarker.length + byteCount + 2; 
            byte[] buf = new byte[bufLength];
            System.arraycopy(tables, 0, buf, 0, tableLength);
            int bufOffset = tableLength;

            short sval = (short)srcHeight; 
            buf[SOFPosition + 5] = (byte)((sval >>> 8) & 0xff);
            buf[SOFPosition + 6] = (byte)(sval & 0xff);
            sval = (short)srcWidth; 
            buf[SOFPosition + 7] = (byte)((sval >>> 8) & 0xff);
            buf[SOFPosition + 8] = (byte)(sval & 0xff);

            stream.seek(offset);

            byte[] twoBytes = new byte[2];
            stream.readFully(twoBytes);
            if(!((twoBytes[0]&0xff) == 0xff && (twoBytes[1]&0xff) == SOS)) {
                System.arraycopy(SOSMarker, 0, buf, bufOffset,
                                 SOSMarker.length);
                bufOffset += SOSMarker.length;
            }

            buf[bufOffset++] = twoBytes[0];
            buf[bufOffset++] = twoBytes[1];
            stream.readFully(buf, bufOffset, byteCount - 2);
            bufOffset += byteCount - 2;

            buf[bufOffset++] = (byte)0xff; 
            buf[bufOffset++] = (byte)EOI;

            ByteArrayInputStream bais =
                new ByteArrayInputStream(buf, 0, bufOffset);
            ImageInputStream is = new MemoryCacheImageInputStream(bais);

            JPEGReader.setInput(is, true, true);
        }

        JPEGParam.setDestination(rawImage);
        JPEGReader.read(0, JPEGParam);
    }

    @SuppressWarnings("removal")
    protected void finalize() throws Throwable {
        super.finalize();
        JPEGReader.dispose();
    }
}
