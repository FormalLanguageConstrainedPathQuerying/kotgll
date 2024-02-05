/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

public class TIFFNullDecompressor extends TIFFDecompressor {

    /**
     * Whether to read the active source region only.
     */
    private boolean isReadActiveOnly = false;

    /** The original value of {@code srcMinX}. */
    private int originalSrcMinX;

    /** The original value of {@code srcMinY}. */
    private int originalSrcMinY;

    /** The original value of {@code srcWidth}. */
    private int originalSrcWidth;

    /** The original value of {@code srcHeight}. */
    private int originalSrcHeight;

    public TIFFNullDecompressor() {}

    public void beginDecoding() {
        int bitsPerPixel = 0;
        for(int i = 0; i < bitsPerSample.length; i++) {
            bitsPerPixel += bitsPerSample[i];
        }

        if((activeSrcMinX != srcMinX || activeSrcMinY != srcMinY ||
            activeSrcWidth != srcWidth || activeSrcHeight != srcHeight) &&
           ((activeSrcMinX - srcMinX)*bitsPerPixel) % 8 == 0) {
            isReadActiveOnly = true;

            originalSrcMinX = srcMinX;
            originalSrcMinY = srcMinY;
            originalSrcWidth = srcWidth;
            originalSrcHeight = srcHeight;

            srcMinX = activeSrcMinX;
            srcMinY = activeSrcMinY;
            srcWidth = activeSrcWidth;
            srcHeight = activeSrcHeight;
        } else {
            isReadActiveOnly = false;
        }

        super.beginDecoding();
    }

    public void decode() throws IOException {
        super.decode();

        if(isReadActiveOnly) {
            srcMinX = originalSrcMinX;
            srcMinY = originalSrcMinY;
            srcWidth = originalSrcWidth;
            srcHeight = originalSrcHeight;

            isReadActiveOnly = false;
        }
    }

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        if(isReadActiveOnly) {

            int activeBytesPerRow = (activeSrcWidth*bitsPerPixel + 7)/8;
            int totalBytesPerRow = (originalSrcWidth*bitsPerPixel + 7)/8;
            int bytesToSkipPerRow = totalBytesPerRow - activeBytesPerRow;

            stream.seek(offset +
                        (activeSrcMinY - originalSrcMinY)*totalBytesPerRow +
                        ((activeSrcMinX - originalSrcMinX)*bitsPerPixel)/8);

            int lastRow = activeSrcHeight - 1;
            for (int y = 0; y < activeSrcHeight; y++) {
                stream.readFully(b, dstOffset, activeBytesPerRow);
                dstOffset += scanlineStride;

                if(y != lastRow) {
                    stream.skipBytes(bytesToSkipPerRow);
                }
            }
        } else {
            stream.seek(offset);
            int bytesPerRow = (srcWidth*bitsPerPixel + 7)/8;
            if(bytesPerRow == scanlineStride) {
                stream.readFully(b, dstOffset, bytesPerRow*srcHeight);
            } else {
                for (int y = 0; y < srcHeight; y++) {
                    stream.readFully(b, dstOffset, bytesPerRow);
                    dstOffset += scanlineStride;
                }
            }
        }
    }
}
