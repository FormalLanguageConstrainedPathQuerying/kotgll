/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.bmp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import com.sun.imageio.plugins.common.I18N;
import com.sun.imageio.plugins.common.ImageUtil;
import com.sun.imageio.plugins.common.ReaderUtil;

/** This class is the Java Image IO plugin reader for BMP images.
 *  It may subsample the image, clip the image, select sub-bands,
 *  and shift the decoded image origin if the proper decoding parameter
 *  are set in the provided {@code ImageReadParam}.
 *
 *  This class supports Microsoft Windows Bitmap Version 3-5,
 *  as well as OS/2 Bitmap Version 2.x (for single-image BMP file).
 *  It also supports undocumented DIB header of type BITMAPV2INFOHEADER
 *  & BITMAPV3INFOHEADER.
 */
public class BMPImageReader extends ImageReader implements BMPConstants {
    private static final int VERSION_2_1_BIT = 0;
    private static final int VERSION_2_4_BIT = 1;
    private static final int VERSION_2_8_BIT = 2;
    private static final int VERSION_2_24_BIT = 3;

    private static final int VERSION_3_1_BIT = 4;
    private static final int VERSION_3_4_BIT = 5;
    private static final int VERSION_3_8_BIT = 6;
    private static final int VERSION_3_24_BIT = 7;

    private static final int VERSION_3_NT_16_BIT = 8;
    private static final int VERSION_3_NT_32_BIT = 9;

    private static final int VERSION_3_EXT_1_BIT = 10;
    private static final int VERSION_3_EXT_4_BIT = 11;
    private static final int VERSION_3_EXT_8_BIT = 12;
    private static final int VERSION_3_EXT_16_BIT = 13;
    private static final int VERSION_3_EXT_24_BIT = 14;
    private static final int VERSION_3_EXT_32_BIT = 15;

    private static final int VERSION_4_1_BIT = 16;
    private static final int VERSION_4_4_BIT = 17;
    private static final int VERSION_4_8_BIT = 18;
    private static final int VERSION_4_16_BIT = 19;
    private static final int VERSION_4_24_BIT = 20;
    private static final int VERSION_4_32_BIT = 21;

    private static final int VERSION_3_XP_EMBEDDED = 22;
    private static final int VERSION_3_EXT_EMBEDDED = 23;
    private static final int VERSION_4_XP_EMBEDDED = 24;
    private static final int VERSION_5_XP_EMBEDDED = 25;

    private long bitmapFileSize;
    private long bitmapOffset;
    private long bitmapStart;
    private long compression;
    private long imageSize;
    private byte[] palette;
    private int imageType;
    private int numBands;
    private boolean isBottomUp;
    private int bitsPerPixel;
    private int redMask, greenMask, blueMask, alphaMask;

    private SampleModel sampleModel, originalSampleModel;
    private ColorModel colorModel, originalColorModel;

    /** The input stream where reads from */
    private ImageInputStream iis = null;

    /** Indicates whether the header is read. */
    private boolean gotHeader = false;

    /** The original image width. */
    private int width;

    /** The original image height. */
    private int height;

    /** The destination region. */
    private Rectangle destinationRegion;

    /** The source region. */
    private Rectangle sourceRegion;

    /** The metadata from the stream. */
    private BMPMetadata metadata;

    /** The destination image. */
    private BufferedImage bi;

    /** Indicates whether subsampled, subregion is required, and offset is
     *  defined
     */
    private boolean noTransform = true;

    /** Indicates whether subband is selected. */
    private boolean seleBand = false;

    /** The scaling factors. */
    private int scaleX, scaleY;

    /** source and destination bands. */
    private int[] sourceBands, destBands;

    /** Constructs {@code BMPImageReader} from the provided
     *  {@code ImageReaderSpi}.
     */
    public BMPImageReader(ImageReaderSpi originator) {
        super(originator);
    }

    @Override
    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        iis = (ImageInputStream) input; 
        if(iis != null)
            iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        resetHeaderInfo();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        if (iis == null) {
            throw new IllegalStateException(I18N.getString("GetNumImages0"));
        }
        if (seekForwardOnly && allowSearch) {
            throw new IllegalStateException(I18N.getString("GetNumImages1"));
        }
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkIndex(imageIndex);
        try {
            readHeader();
        } catch (IllegalArgumentException e) {
            throw new IIOException(I18N.getString("BMPImageReader6"), e);
        }
        return width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkIndex(imageIndex);
        try {
            readHeader();
        } catch (IllegalArgumentException e) {
            throw new IIOException(I18N.getString("BMPImageReader6"), e);
        }
        return height;
    }

    private void checkIndex(int imageIndex) {
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException(I18N.getString("BMPImageReader0"));
        }
    }

    private void readColorPalette(int sizeOfPalette) throws IOException {
        palette = ReaderUtil.staggeredReadByteStream(iis, sizeOfPalette);
    }

    /**
     * Process the image header.
     *
     * @throws IllegalStateException if source stream is not set.
     *
     * @throws IOException if image stream is corrupted.
     *
     * @throws IllegalArgumentException if the image stream does not contain
     *             a BMP image, or if a sample model instance to describe the
     *             image can not be created.
     */
    protected void readHeader() throws IOException, IllegalArgumentException {
        if (gotHeader)
            return;

        if (iis == null) {
            throw new IllegalStateException("Input source not set!");
        }
        int profileData = 0, profileSize = 0;

        this.metadata = new BMPMetadata();
        iis.mark();

        byte[] marker = new byte[2];
        iis.read(marker);
        if (marker[0] != 0x42 || marker[1] != 0x4d)
            throw new IllegalArgumentException(I18N.getString("BMPImageReader1"));

        bitmapFileSize = iis.readUnsignedInt();
        iis.skipBytes(4);

        bitmapOffset = iis.readUnsignedInt();

        long size = iis.readUnsignedInt();

        if (size == 12) {
            width = iis.readShort();
            height = iis.readShort();
        } else {
            width = iis.readInt();
            height = iis.readInt();
        }

        metadata.width = width;
        metadata.height = height;

        int planes = iis.readUnsignedShort();
        bitsPerPixel = iis.readUnsignedShort();

        metadata.bitsPerPixel = (short)bitsPerPixel;

        numBands = 3;

        if (size == 12) {
            metadata.bmpVersion = VERSION_2;

            if (bitsPerPixel == 1) {
                imageType = VERSION_2_1_BIT;
            } else if (bitsPerPixel == 4) {
                imageType = VERSION_2_4_BIT;
            } else if (bitsPerPixel == 8) {
                imageType = VERSION_2_8_BIT;
            } else if (bitsPerPixel == 24) {
                imageType = VERSION_2_24_BIT;
            } else {
                throw new IIOException(I18N.getString("BMPImageReader8"));
            }

            int numberOfEntries = (int)((bitmapOffset - 14 - size) / 3);
            int sizeOfPalette = numberOfEntries*3;
            readColorPalette(sizeOfPalette);
            metadata.palette = palette;
            metadata.paletteSize = numberOfEntries;
        } else {
            compression = iis.readUnsignedInt();
            imageSize = iis.readUnsignedInt();
            long xPelsPerMeter = iis.readInt();
            long yPelsPerMeter = iis.readInt();
            long colorsUsed = iis.readUnsignedInt();
            long colorsImportant = iis.readUnsignedInt();

            metadata.compression = (int)compression;
            metadata.xPixelsPerMeter = (int)xPelsPerMeter;
            metadata.yPixelsPerMeter = (int)yPelsPerMeter;
            metadata.colorsUsed = (int)colorsUsed;
            metadata.colorsImportant = (int)colorsImportant;

            if (size == 40) {
                switch((int)compression) {

                case BI_JPEG:
                case BI_PNG:
                    metadata.bmpVersion = VERSION_3;
                    imageType = VERSION_3_XP_EMBEDDED;
                    break;

                case BI_RGB:  
                case BI_RLE8:  
                case BI_RLE4:  

                    if (bitmapOffset < (size + 14)) {
                        throw new IIOException(I18N.getString("BMPImageReader7"));
                    }
                    int numberOfEntries = (int)((bitmapOffset-14-size) / 4);
                    int sizeOfPalette = numberOfEntries * 4;
                    readColorPalette(sizeOfPalette);

                    metadata.palette = palette;
                    metadata.paletteSize = numberOfEntries;

                    if (bitsPerPixel == 1) {
                        imageType = VERSION_3_1_BIT;
                    } else if (bitsPerPixel == 4) {
                        imageType = VERSION_3_4_BIT;
                    } else if (bitsPerPixel == 8) {
                        imageType = VERSION_3_8_BIT;
                    } else if (bitsPerPixel == 24) {
                        imageType = VERSION_3_24_BIT;
                    } else if (bitsPerPixel == 16) {
                        imageType = VERSION_3_NT_16_BIT;

                        redMask = 0x7C00;
                        greenMask = 0x3E0;
                        blueMask =  (1 << 5) - 1;
                        metadata.redMask = redMask;
                        metadata.greenMask = greenMask;
                        metadata.blueMask = blueMask;
                    } else if (bitsPerPixel == 32) {
                        imageType = VERSION_3_NT_32_BIT;
                        redMask   = 0x00FF0000;
                        greenMask = 0x0000FF00;
                        blueMask  = 0x000000FF;
                        metadata.redMask = redMask;
                        metadata.greenMask = greenMask;
                        metadata.blueMask = blueMask;
                    } else {
                        throw new
                            IIOException(I18N.getString("BMPImageReader8"));
                    }

                    metadata.bmpVersion = VERSION_3;
                    break;

                case BI_BITFIELDS:

                    if (bitsPerPixel == 16) {
                        imageType = VERSION_3_NT_16_BIT;
                    } else if (bitsPerPixel == 32) {
                        imageType = VERSION_3_NT_32_BIT;
                    } else {
                        throw new
                            IIOException(I18N.getString("BMPImageReader8"));
                    }

                    redMask = (int)iis.readUnsignedInt();
                    greenMask = (int)iis.readUnsignedInt();
                    blueMask = (int)iis.readUnsignedInt();
                    metadata.redMask = redMask;
                    metadata.greenMask = greenMask;
                    metadata.blueMask = blueMask;

                    if (colorsUsed != 0) {
                        sizeOfPalette = (int)colorsUsed*4;
                        readColorPalette(sizeOfPalette);

                        metadata.palette = palette;
                        metadata.paletteSize = (int)colorsUsed;
                    }
                    metadata.bmpVersion = VERSION_3_NT;

                    break;
                default:
                    throw new
                        IIOException(I18N.getString("BMPImageReader2"));
                }
            } else if (size == 52 || size == 56) {
                redMask = (int)iis.readUnsignedInt();
                greenMask = (int)iis.readUnsignedInt();
                blueMask = (int)iis.readUnsignedInt();
                if (size == 56) {
                    alphaMask = (int)iis.readUnsignedInt();
                }

                metadata.bmpVersion = VERSION_3_EXT;
                int numberOfEntries = (int)((bitmapOffset-14-size) / 4);
                int sizeOfPalette = numberOfEntries*4;
                readColorPalette(sizeOfPalette);
                metadata.palette = palette;
                metadata.paletteSize = numberOfEntries;

                switch((int)compression) {

                    case BI_JPEG:
                    case BI_PNG:
                        imageType = VERSION_3_EXT_EMBEDDED;
                        break;
                    default:
                        if (bitsPerPixel == 1) {
                            imageType = VERSION_3_EXT_1_BIT;
                        } else if (bitsPerPixel == 4) {
                            imageType = VERSION_3_EXT_4_BIT;
                        } else if (bitsPerPixel == 8) {
                            imageType = VERSION_3_EXT_8_BIT;
                        } else if (bitsPerPixel == 16) {
                            imageType = VERSION_3_EXT_16_BIT;
                            if ((int)compression == BI_RGB) {
                                redMask = 0x7C00;
                                greenMask = 0x3E0;
                                blueMask = 0x1F;
                            }
                        } else if (bitsPerPixel == 24) {
                            imageType = VERSION_3_EXT_24_BIT;
                        } else if (bitsPerPixel == 32) {
                            imageType = VERSION_3_EXT_32_BIT;
                            if ((int)compression == BI_RGB) {
                                redMask   = 0x00FF0000;
                                greenMask = 0x0000FF00;
                                blueMask  = 0x000000FF;
                            }
                        } else {
                            throw new
                            IIOException(I18N.getString("BMPImageReader8"));
                        }

                        metadata.redMask = redMask;
                        metadata.greenMask = greenMask;
                        metadata.blueMask = blueMask;
                        metadata.alphaMask = alphaMask;
                }
            } else if (size == 108 || size == 124) {
                if (size == 108)
                    metadata.bmpVersion = VERSION_4;
                else if (size == 124)
                    metadata.bmpVersion = VERSION_5;

                redMask = (int)iis.readUnsignedInt();
                greenMask = (int)iis.readUnsignedInt();
                blueMask = (int)iis.readUnsignedInt();
                alphaMask = (int)iis.readUnsignedInt();
                long csType = iis.readUnsignedInt();
                int redX = iis.readInt();
                int redY = iis.readInt();
                int redZ = iis.readInt();
                int greenX = iis.readInt();
                int greenY = iis.readInt();
                int greenZ = iis.readInt();
                int blueX = iis.readInt();
                int blueY = iis.readInt();
                int blueZ = iis.readInt();
                long gammaRed = iis.readUnsignedInt();
                long gammaGreen = iis.readUnsignedInt();
                long gammaBlue = iis.readUnsignedInt();

                if (size == 124) {
                    metadata.intent = iis.readInt();
                    profileData = iis.readInt();
                    profileSize = iis.readInt();
                    iis.skipBytes(4);
                }

                metadata.colorSpace = (int)csType;

                if (csType == LCS_CALIBRATED_RGB) {
                    metadata.redX = redX;
                    metadata.redY = redY;
                    metadata.redZ = redZ;
                    metadata.greenX = greenX;
                    metadata.greenY = greenY;
                    metadata.greenZ = greenZ;
                    metadata.blueX = blueX;
                    metadata.blueY = blueY;
                    metadata.blueZ = blueZ;
                    metadata.gammaRed = (int)gammaRed;
                    metadata.gammaGreen = (int)gammaGreen;
                    metadata.gammaBlue = (int)gammaBlue;
                }

                int numberOfEntries = (int)((bitmapOffset-14-size) / 4);
                int sizeOfPalette = numberOfEntries*4;
                readColorPalette(sizeOfPalette);
                metadata.palette = palette;
                metadata.paletteSize = numberOfEntries;

                switch ((int)compression) {
                case BI_JPEG:
                case BI_PNG:
                    if (size == 108) {
                        imageType = VERSION_4_XP_EMBEDDED;
                    } else if (size == 124) {
                        imageType = VERSION_5_XP_EMBEDDED;
                    }
                    break;
                default:
                    if (bitsPerPixel == 1) {
                        imageType = VERSION_4_1_BIT;
                    } else if (bitsPerPixel == 4) {
                        imageType = VERSION_4_4_BIT;
                    } else if (bitsPerPixel == 8) {
                        imageType = VERSION_4_8_BIT;
                    } else if (bitsPerPixel == 16) {
                        imageType = VERSION_4_16_BIT;
                        if ((int)compression == BI_RGB) {
                            redMask = 0x7C00;
                            greenMask = 0x3E0;
                            blueMask = 0x1F;
                        }
                    } else if (bitsPerPixel == 24) {
                        imageType = VERSION_4_24_BIT;
                    } else if (bitsPerPixel == 32) {
                        imageType = VERSION_4_32_BIT;
                        if ((int)compression == BI_RGB) {
                            redMask   = 0x00FF0000;
                            greenMask = 0x0000FF00;
                            blueMask  = 0x000000FF;
                        }
                    } else {
                        throw new
                            IIOException(I18N.getString("BMPImageReader8"));
                    }

                    metadata.redMask = redMask;
                    metadata.greenMask = greenMask;
                    metadata.blueMask = blueMask;
                    metadata.alphaMask = alphaMask;
                }
            } else {
                throw new
                    IIOException(I18N.getString("BMPImageReader3"));
            }
        }

        if (height > 0) {
            isBottomUp = true;
        } else {
            isBottomUp = false;
            height = Math.abs(height);
        }

        if (metadata.compression == BI_RGB &&
            metadata.paletteSize == 0 &&
            metadata.bitsPerPixel >= 16) {
            long imageDataSize = (((long)width * height * bitsPerPixel) / 8);
            if (imageDataSize > (bitmapFileSize - bitmapOffset)) {
                throw new IIOException(I18N.getString("BMPImageReader9"));
            }
        }

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        if (metadata.colorSpace == PROFILE_LINKED ||
            metadata.colorSpace == PROFILE_EMBEDDED) {

            iis.mark();
            iis.skipBytes(profileData - size);
            byte[] profile = ReaderUtil.
                    staggeredReadByteStream(iis, profileSize);
            iis.reset();

            if (metadata.colorSpace == PROFILE_LINKED &&
                isLinkedProfileAllowed())
            {
                String path = new String(profile, "windows-1252");

                colorSpace =
                    new ICC_ColorSpace(ICC_Profile.getInstance(path));
            } else if (metadata.colorSpace == PROFILE_EMBEDDED) {
                colorSpace =
                    new ICC_ColorSpace(ICC_Profile.getInstance(profile));
            }
        }

        if (bitsPerPixel == 0 ||
            compression == BI_JPEG || compression == BI_PNG )
        {
            colorModel = null;
            sampleModel = null;
        } else if (bitsPerPixel == 1 || bitsPerPixel == 4 || bitsPerPixel == 8) {
            numBands = 1;

            if (bitsPerPixel == 8) {
                int[] bandOffsets = new int[numBands];
                for (int i = 0; i < numBands; i++) {
                    bandOffsets[i] = numBands -1 -i;
                }
                sampleModel =
                    new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                    width, height,
                                                    numBands,
                                                    numBands * width,
                                                    bandOffsets);
            } else {
                sampleModel =
                    new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                    width, height,
                                                    bitsPerPixel);
            }

            byte[] r, g, b;
            if (imageType == VERSION_2_1_BIT ||
                imageType == VERSION_2_4_BIT ||
                imageType == VERSION_2_8_BIT) {


                size = palette.length/3;

                if (size > 256) {
                    size = 256;
                }

                int off;
                r = new byte[(int)size];
                g = new byte[(int)size];
                b = new byte[(int)size];
                for (int i=0; i<(int)size; i++) {
                    off = 3 * i;
                    b[i] = palette[off];
                    g[i] = palette[off+1];
                    r[i] = palette[off+2];
                }
            } else {
                size = palette.length/4;

                if (size > 256) {
                    size = 256;
                }

                int off;
                r = new byte[(int)size];
                g = new byte[(int)size];
                b = new byte[(int)size];
                for (int i=0; i<size; i++) {
                    off = 4 * i;
                    b[i] = palette[off];
                    g[i] = palette[off+1];
                    r[i] = palette[off+2];
                }
            }

            if (ImageUtil.isIndicesForGrayscale(r, g, b))
                colorModel =
                    ImageUtil.createColorModel(null, sampleModel);
            else
                colorModel = new IndexColorModel(bitsPerPixel, (int)size, r, g, b);
        } else if (bitsPerPixel == 16) {
            numBands = 3;
            sampleModel =
                new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT,
                                                 width, height,
                                                 new int[] {redMask, greenMask, blueMask});

            colorModel =
                new DirectColorModel(colorSpace,
                                     16, redMask, greenMask, blueMask, 0,
                                     false, DataBuffer.TYPE_USHORT);

        } else if (bitsPerPixel == 32) {
            numBands = alphaMask == 0 ? 3 : 4;

            int[] bitMasks = numBands == 3 ?
                new int[] {redMask, greenMask, blueMask} :
                new int[] {redMask, greenMask, blueMask, alphaMask};

                sampleModel =
                    new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
                                                     width, height,
                                                     bitMasks);

                colorModel =
                    new DirectColorModel(colorSpace,
                                         32, redMask, greenMask, blueMask, alphaMask,
                                         false, DataBuffer.TYPE_INT);
        } else {
            numBands = 3;
            int[] bandOffsets = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                bandOffsets[i] = numBands -1 -i;
            }

            sampleModel =
                new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                width, height,
                                                numBands,
                                                numBands * width,
                                                bandOffsets);

            colorModel =
                ImageUtil.createColorModel(colorSpace, sampleModel);
        }

        originalSampleModel = sampleModel;
        originalColorModel = colorModel;

        iis.reset();
        iis.skipBytes(bitmapOffset);
        bitmapStart = iis.getStreamPosition();

        gotHeader = true;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
      throws IOException {
        checkIndex(imageIndex);
        try {
            readHeader();
        } catch (IllegalArgumentException e) {
            throw new IIOException(I18N.getString("BMPImageReader6"), e);
        }
        ArrayList<ImageTypeSpecifier> list = new ArrayList<>(1);
        list.add(new ImageTypeSpecifier(originalColorModel,
                                        originalSampleModel));
        return list.iterator();
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex)
      throws IOException {
        checkIndex(imageIndex);
        if (metadata == null) {
            try {
                readHeader();
            } catch (IllegalArgumentException e) {
                throw new IIOException(I18N.getString("BMPImageReader6"), e);
            }
        }
        return metadata;
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    @Override
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        checkIndex(imageIndex);
        try {
            readHeader();
        } catch (IllegalArgumentException e) {
            throw new IIOException(I18N.getString("BMPImageReader6"), e);
        }
        return metadata.compression == BI_RGB;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param)
        throws IOException {

        if (iis == null) {
            throw new IllegalStateException(I18N.getString("BMPImageReader5"));
        }

        checkIndex(imageIndex);
        clearAbortRequest();
        processImageStarted(imageIndex);
        if (abortRequested()) {
            processReadAborted();
            return bi;
        }

        if (param == null)
            param = getDefaultReadParam();

        try {
            readHeader();
        } catch (IllegalArgumentException e) {
            throw new IIOException(I18N.getString("BMPImageReader6"), e);
        }

        sourceRegion = new Rectangle(0, 0, 0, 0);
        destinationRegion = new Rectangle(0, 0, 0, 0);

        computeRegions(param, this.width, this.height,
                       param.getDestination(),
                       sourceRegion,
                       destinationRegion);

        scaleX = param.getSourceXSubsampling();
        scaleY = param.getSourceYSubsampling();

        sourceBands = param.getSourceBands();
        destBands = param.getDestinationBands();

        seleBand = (sourceBands != null) && (destBands != null);
        noTransform =
            destinationRegion.equals(new Rectangle(0, 0, width, height)) ||
            seleBand;

        if (!seleBand) {
            sourceBands = new int[numBands];
            destBands = new int[numBands];
            for (int i = 0; i < numBands; i++)
                destBands[i] = sourceBands[i] = i;
        }

        bi = param.getDestination();

        WritableRaster raster = null;

        if (bi == null) {
            if (sampleModel != null && colorModel != null) {
                sampleModel =
                    sampleModel.createCompatibleSampleModel(destinationRegion.x +
                                                            destinationRegion.width,
                                                            destinationRegion.y +
                                                            destinationRegion.height);
                if (seleBand)
                    sampleModel = sampleModel.createSubsetSampleModel(sourceBands);
                raster = Raster.createWritableRaster(sampleModel, new Point());
                bi = new BufferedImage(colorModel, raster, false, null);
            }
        } else {
            raster = bi.getWritableTile(0, 0);
            sampleModel = bi.getSampleModel();
            colorModel = bi.getColorModel();

            noTransform &=  destinationRegion.equals(raster.getBounds());
        }

        byte[] bdata = null; 
        short[] sdata = null; 
        int[] idata = null; 

        if (sampleModel != null) {
            if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE)
                bdata = ((DataBufferByte)raster.getDataBuffer()).getData();
            else if (sampleModel.getDataType() == DataBuffer.TYPE_USHORT)
                sdata = ((DataBufferUShort)raster.getDataBuffer()).getData();
            else if (sampleModel.getDataType() == DataBuffer.TYPE_INT)
                idata = ((DataBufferInt)raster.getDataBuffer()).getData();
        }

        iis.seek(bitmapStart);

        switch(imageType) {

        case VERSION_2_1_BIT:
            read1Bit(bdata);
            break;

        case VERSION_2_4_BIT:
            read4Bit(bdata);
            break;

        case VERSION_2_8_BIT:
            read8Bit(bdata);
            break;

        case VERSION_2_24_BIT:
            read24Bit(bdata);
            break;

        case VERSION_3_1_BIT:
            read1Bit(bdata);
            break;

        case VERSION_3_4_BIT:
            switch((int)compression) {
            case BI_RGB:
                read4Bit(bdata);
                break;

            case BI_RLE4:
                readRLE4(bdata);
                break;

            default:
                throw new
                    IIOException(I18N.getString("BMPImageReader1"));
            }
            break;

        case VERSION_3_8_BIT:
            switch((int)compression) {
            case BI_RGB:
                read8Bit(bdata);
                break;

            case BI_RLE8:
                readRLE8(bdata);
                break;

            default:
                throw new
                    IIOException(I18N.getString("BMPImageReader1"));
            }

            break;

        case VERSION_3_24_BIT:
            read24Bit(bdata);
            break;

        case VERSION_3_NT_16_BIT:
            read16Bit(sdata);
            break;

        case VERSION_3_NT_32_BIT:
            read32Bit(idata);
            break;

        case VERSION_3_XP_EMBEDDED:
        case VERSION_3_EXT_EMBEDDED:
        case VERSION_4_XP_EMBEDDED:
        case VERSION_5_XP_EMBEDDED:
            bi = readEmbedded((int)compression, bi, param);
            break;

        case VERSION_3_EXT_1_BIT:
        case VERSION_4_1_BIT:
            read1Bit(bdata);
            break;

        case VERSION_3_EXT_4_BIT:
        case VERSION_4_4_BIT:
            switch((int)compression) {

            case BI_RGB:
                read4Bit(bdata);
                break;

            case BI_RLE4:
                readRLE4(bdata);
                break;

            default:
                throw new
                    IIOException(I18N.getString("BMPImageReader1"));
            }
            break;

        case VERSION_3_EXT_8_BIT:
        case VERSION_4_8_BIT:
            switch((int)compression) {

            case BI_RGB:
                read8Bit(bdata);
                break;

            case BI_RLE8:
                readRLE8(bdata);
                break;

            default:
                throw new
                    IIOException(I18N.getString("BMPImageReader1"));
            }
            break;

        case VERSION_3_EXT_16_BIT:
        case VERSION_4_16_BIT:
            read16Bit(sdata);
            break;

        case VERSION_3_EXT_24_BIT:
        case VERSION_4_24_BIT:
            read24Bit(bdata);
            break;

        case VERSION_3_EXT_32_BIT:
        case VERSION_4_32_BIT:
            read32Bit(idata);
            break;
        }

        if (abortRequested())
            processReadAborted();
        else
            processImageComplete();

        return bi;
    }

    @Override
    public boolean canReadRaster() {
        return true;
    }

    @Override
    public Raster readRaster(int imageIndex,
                             ImageReadParam param) throws IOException {
        BufferedImage bi = read(imageIndex, param);
        return bi.getData();
    }

    private void resetHeaderInfo() {
        gotHeader = false;
        bi = null;
        sampleModel = originalSampleModel = null;
        colorModel = originalColorModel = null;
    }

    @Override
    public void reset() {
        super.reset();
        iis = null;
        resetHeaderInfo();
    }

    private void read1Bit(byte[] bdata) throws IOException {
        int bytesPerScanline = (width + 7) / 8;
        int padding = bytesPerScanline % 4;
        if (padding != 0) {
            padding = 4 - padding;
        }

        int lineLength = bytesPerScanline + padding;

        if (noTransform) {
            int j = isBottomUp ? (height -1)*bytesPerScanline : 0;

            for (int i=0; i<height; i++) {
                iis.readFully(bdata, j, bytesPerScanline);
                iis.skipBytes(padding);
                j += isBottomUp ? -bytesPerScanline : bytesPerScanline;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            byte[] buf = new byte[lineLength];
            int lineStride =
                ((MultiPixelPackedSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(lineLength * (height - 1 - lastLine));
            } else
                iis.skipBytes(lineLength * sourceRegion.y);

            int skipLength = lineLength * (scaleY - 1);

            int[] srcOff = new int[destinationRegion.width];
            int[] destOff = new int[destinationRegion.width];
            int[] srcPos = new int[destinationRegion.width];
            int[] destPos = new int[destinationRegion.width];

            for (int i = destinationRegion.x, x = sourceRegion.x, j = 0;
                 i < destinationRegion.x + destinationRegion.width;
                 i++, j++, x += scaleX) {
                srcPos[j] = x >> 3;
                srcOff[j] = 7 - (x & 7);
                destPos[j] = i >> 3;
                destOff[j] = 7 - (i & 7);
            }

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.read(buf, 0, lineLength);
                for (int i = 0; i < destinationRegion.width; i++) {
                    int v = (buf[srcPos[i]] >> srcOff[i]) & 1;
                    bdata[k + destPos[i]] |= v << destOff[i];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void read4Bit(byte[] bdata) throws IOException {

        int bytesPerScanline = (width + 1) / 2;

        int padding = bytesPerScanline % 4;
        if (padding != 0)
            padding = 4 - padding;

        int lineLength = bytesPerScanline + padding;

        if (noTransform) {
            int j = isBottomUp ? (height -1) * bytesPerScanline : 0;

            for (int i=0; i<height; i++) {
                iis.readFully(bdata, j, bytesPerScanline);
                iis.skipBytes(padding);
                j += isBottomUp ? -bytesPerScanline : bytesPerScanline;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            byte[] buf = new byte[lineLength];
            int lineStride =
                ((MultiPixelPackedSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(lineLength * (height - 1 - lastLine));
            } else
                iis.skipBytes(lineLength * sourceRegion.y);

            int skipLength = lineLength * (scaleY - 1);

            int[] srcOff = new int[destinationRegion.width];
            int[] destOff = new int[destinationRegion.width];
            int[] srcPos = new int[destinationRegion.width];
            int[] destPos = new int[destinationRegion.width];

            for (int i = destinationRegion.x, x = sourceRegion.x, j = 0;
                 i < destinationRegion.x + destinationRegion.width;
                 i++, j++, x += scaleX) {
                srcPos[j] = x >> 1;
                srcOff[j] = (1 - (x & 1)) << 2;
                destPos[j] = i >> 1;
                destOff[j] = (1 - (i & 1)) << 2;
            }

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.read(buf, 0, lineLength);
                for (int i = 0; i < destinationRegion.width; i++) {
                    int v = (buf[srcPos[i]] >> srcOff[i]) & 0x0F;
                    bdata[k + destPos[i]] |= v << destOff[i];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void read8Bit(byte[] bdata) throws IOException {

        int padding = width % 4;
        if (padding != 0) {
            padding = 4 - padding;
        }

        int lineLength = width + padding;

        if (noTransform) {
            int j = isBottomUp ? (height -1) * width : 0;

            for (int i=0; i<height; i++) {
                iis.readFully(bdata, j, width);
                iis.skipBytes(padding);
                j += isBottomUp ? -width : width;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            byte[] buf = new byte[lineLength];
            int lineStride =
                ((ComponentSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(lineLength * (height - 1 - lastLine));
            } else
                iis.skipBytes(lineLength * sourceRegion.y);

            int skipLength = lineLength * (scaleY - 1);

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;
            k += destinationRegion.x;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.read(buf, 0, lineLength);
                for (int i = 0, m = sourceRegion.x;
                     i < destinationRegion.width; i++, m += scaleX) {
                    bdata[k + i] = buf[m];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void read24Bit(byte[] bdata) throws IOException {
        int padding = width * 3 % 4;
        if ( padding != 0)
            padding = 4 - padding;

        int lineStride = width * 3;
        int lineLength = lineStride + padding;

        if (noTransform) {
            int j = isBottomUp ? (height -1) * width * 3 : 0;

            for (int i=0; i<height; i++) {
                iis.readFully(bdata, j, lineStride);
                iis.skipBytes(padding);
                j += isBottomUp ? -lineStride : lineStride;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            byte[] buf = new byte[lineLength];
            lineStride =
                ((ComponentSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(lineLength * (height - 1 - lastLine));
            } else
                iis.skipBytes(lineLength * sourceRegion.y);

            int skipLength = lineLength * (scaleY - 1);

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;
            k += destinationRegion.x * 3;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.read(buf, 0, lineLength);
                for (int i = 0, m = 3 * sourceRegion.x;
                     i < destinationRegion.width; i++, m += 3 * scaleX) {
                    int n = 3 * i + k;
                    for (int b = 0; b < destBands.length; b++)
                        bdata[n + destBands[b]] = buf[m + sourceBands[b]];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void read16Bit(short[] sdata) throws IOException {
        int padding = width * 2 % 4;

        if ( padding != 0)
            padding = 4 - padding;

        int lineLength = width + padding / 2;

        if (noTransform) {
            int j = isBottomUp ? (height -1) * width : 0;
            for (int i=0; i<height; i++) {
                iis.readFully(sdata, j, width);
                iis.skipBytes(padding);

                j += isBottomUp ? -width : width;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            short[] buf = new short[lineLength];
            int lineStride =
                ((SinglePixelPackedSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(lineLength * (height - 1 - lastLine) << 1);
            } else
                iis.skipBytes(lineLength * sourceRegion.y << 1);

            int skipLength = lineLength * (scaleY - 1) << 1;

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;
            k += destinationRegion.x;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.readFully(buf, 0, lineLength);
                for (int i = 0, m = sourceRegion.x;
                     i < destinationRegion.width; i++, m += scaleX) {
                    sdata[k + i] = buf[m];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void read32Bit(int[] idata) throws IOException {
        if (noTransform) {
            int j = isBottomUp ? (height -1) * width : 0;

            for (int i=0; i<height; i++) {
                iis.readFully(idata, j, width);
                j += isBottomUp ? -width : width;
                processImageUpdate(bi, 0, i,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F * i/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        } else {
            int[] buf = new int[width];
            int lineStride =
                ((SinglePixelPackedSampleModel)sampleModel).getScanlineStride();

            if (isBottomUp) {
                int lastLine =
                    sourceRegion.y + (destinationRegion.height - 1) * scaleY;
                iis.skipBytes(width * (height - 1 - lastLine) << 2);
            } else
                iis.skipBytes(width * sourceRegion.y << 2);

            int skipLength = width * (scaleY - 1) << 2;

            int k = destinationRegion.y * lineStride;
            if (isBottomUp)
                k += (destinationRegion.height - 1) * lineStride;
            k += destinationRegion.x;

            for (int j = 0, y = sourceRegion.y;
                 j < destinationRegion.height; j++, y+=scaleY) {
                iis.readFully(buf, 0, width);
                for (int i = 0, m = sourceRegion.x;
                     i < destinationRegion.width; i++, m += scaleX) {
                    idata[k + i] = buf[m];
                }

                k += isBottomUp ? -lineStride : lineStride;
                iis.skipBytes(skipLength);
                processImageUpdate(bi, 0, j,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                processImageProgress(100.0F*j/destinationRegion.height);
                if (abortRequested()) {
                    break;
                }
            }
        }
    }

    private void readRLE8(byte[] bdata) throws IOException {
        int imSize = (int)imageSize;
        if (imSize == 0) {
            imSize = (int)(bitmapFileSize - bitmapOffset);
        }

        int padding = 0;
        int remainder = width % 4;
        if (remainder != 0) {
            padding = 4 - remainder;
        }

        byte[] values = ReaderUtil.
            staggeredReadByteStream(iis, imSize);

        decodeRLE8(imSize, padding, values, bdata);
    }

    private boolean copyRLE8ScanlineToDst(int lineNo,
                                          byte[] val,
                                          byte[] bdata) {
        boolean isSuccess = false;

        if (lineNo >= sourceRegion.y &&
            lineNo < sourceRegion.y + sourceRegion.height) {
            if (noTransform) {
                int pos = lineNo * width;
                for(int i = 0; i < width; i++)
                    bdata[pos++] = val[i];
                processImageUpdate(bi, 0, lineNo,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                isSuccess = true;
            } else if ((lineNo - sourceRegion.y) % scaleY == 0) {
                int lineStride =
                    ((ComponentSampleModel)sampleModel).getScanlineStride();
                int currentLine = (lineNo - sourceRegion.y) / scaleY +
                    destinationRegion.y;
                int pos = currentLine * lineStride;
                pos += destinationRegion.x;
                for (int i = sourceRegion.x;
                     i < sourceRegion.x + sourceRegion.width;
                     i += scaleX)
                    bdata[pos++] = val[i];
                processImageUpdate(bi, 0, currentLine,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                isSuccess = true;
            }
            for(int scIndex = 0; scIndex < width; scIndex++) {
                val[scIndex] = 0;
            }
        }

        return isSuccess;
    }

    private void decodeRLE8(int imSize,
                            int padding,
                            byte[] values,
                            byte[] bdata) throws IOException {

        byte[] val = new byte[width];
        int count = 0, l = 0;
        int value;
        boolean flag = false;
        int lineNo = isBottomUp ? height - 1 : 0;
        int finished = 0;

        while ((count + 1) < imSize) {
            value = values[count++] & 0xff;
            if (value == 0) {
                switch(values[count++] & 0xff) {

                case 0:
                    if (copyRLE8ScanlineToDst(lineNo, val, bdata)) {
                        finished++;
                    }
                    processImageProgress(100.0F * finished / destinationRegion.height);
                    lineNo += isBottomUp ? -1 : 1;
                    l = 0;

                    if (abortRequested()) {
                        flag = true;
                    }
                    break;

                case 1:
                    flag = true;

                    if (l != 0) {
                        if (copyRLE8ScanlineToDst(lineNo, val, bdata)) {
                            finished++;
                        }
                        processImageProgress(100.0F * finished / destinationRegion.height);
                        lineNo += isBottomUp ? -1 : 1;
                        l = 0;
                    }
                    break;

                case 2:
                    if ((count+1) < imSize) {
                        int xoff = values[count++] & 0xff;
                        int yoff = values[count++] & 0xff;

                        if (yoff != 0) {
                            if (copyRLE8ScanlineToDst(lineNo, val, bdata)) {
                                finished++;
                            }
                            processImageProgress(100.0F * finished
                                                 / destinationRegion.height);
                            lineNo += isBottomUp ? -yoff : yoff;
                        }

                        l += xoff + yoff*width;
                        l %= width;
                    }
                    break;

                default:
                    int end = values[count-1] & 0xff;
                    byte readByte = 0;
                    for (int i=0; (i < end) && (count < imSize); i++) {
                        readByte = (byte)(values[count++] & 0xff);
                        if (l < width) {
                            val[l++] = readByte;
                        }
                    }

                    if ((end & 1) == 1) {
                        count++;
                    }
                    break;
                }
            } else {
                if (count < imSize) {
                    for (int i=0; (i < value) && (l < width); i++) {
                        val[l++] = (byte)(values[count] & 0xff);
                    }
                }

                count++;
            }

            if (flag) {
                break;
            }
        }
    }

    private void readRLE4(byte[] bdata) throws IOException {

        int imSize = (int)imageSize;
        if (imSize == 0) {
            imSize = (int)(bitmapFileSize - bitmapOffset);
        }

        int padding = 0;
        int remainder = width % 4;
        if (remainder != 0) {
            padding = 4 - remainder;
        }

        byte[] values = ReaderUtil.
            staggeredReadByteStream(iis, imSize);

        decodeRLE4(imSize, padding, values, bdata);
    }

    private boolean copyRLE4ScanlineToDst(int lineNo,
                                          byte[] val,
                                          byte[] bdata) throws IOException {
        boolean isSuccess = false;

        if (lineNo >= sourceRegion.y &&
            lineNo < sourceRegion.y + sourceRegion.height) {
            if (noTransform) {
                int pos = lineNo * (width + 1 >> 1);
                for(int i = 0, j = 0; i < width >> 1; i++)
                    bdata[pos++] =
                        (byte)((val[j++] << 4) | val[j++]);
                if ((width & 1) == 1)
                    bdata[pos] |= val[width - 1] << 4;

                processImageUpdate(bi, 0, lineNo,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                isSuccess = true;
            } else if ((lineNo - sourceRegion.y) % scaleY == 0) {
                int lineStride =
                    ((MultiPixelPackedSampleModel)sampleModel).getScanlineStride();
                int currentLine = (lineNo - sourceRegion.y) / scaleY +
                    destinationRegion.y;
                int pos = currentLine * lineStride;
                pos += destinationRegion.x >> 1;
                int shift = (1 - (destinationRegion.x & 1)) << 2;
                for (int i = sourceRegion.x;
                     i < sourceRegion.x + sourceRegion.width;
                     i += scaleX) {
                    bdata[pos] |= val[i] << shift;
                    shift += 4;
                    if (shift == 4) {
                        pos++;
                    }
                    shift &= 7;
                }
                processImageUpdate(bi, 0, currentLine,
                                   destinationRegion.width, 1, 1, 1,
                                   new int[]{0});
                isSuccess = true;
            }
            for(int scIndex = 0; scIndex < width; scIndex++) {
                val[scIndex] = 0;
            }
        }
        return isSuccess;
    }

    private void decodeRLE4(int imSize,
                            int padding,
                            byte[] values,
                            byte[] bdata) throws IOException {
        byte[] val = new byte[width];
        int count = 0, l = 0;
        int value;
        boolean flag = false;
        int lineNo = isBottomUp ? height - 1 : 0;
        int finished = 0;

        while ((count + 1) < imSize) {

            value = values[count++] & 0xFF;
            if (value == 0) {

                switch(values[count++] & 0xFF) {

                case 0:
                    if (copyRLE4ScanlineToDst(lineNo, val, bdata)) {
                        finished++;
                    }
                    processImageProgress(100.0F * finished / destinationRegion.height);
                    lineNo += isBottomUp ? -1 : 1;
                    l = 0;

                    if (abortRequested()) {
                        flag = true;
                    }

                    break;

                case 1:
                    flag = true;

                    if (l != 0) {
                        if (copyRLE4ScanlineToDst(lineNo, val, bdata)) {
                            finished++;
                        }
                        processImageProgress(100.0F * finished / destinationRegion.height);
                        lineNo += isBottomUp ? -1 : 1;
                        l = 0;
                    }
                    break;

                case 2:
                    if ((count + 1) < imSize) {
                        int xoff = values[count++] & 0xFF;
                        int yoff = values[count++] & 0xFF;

                        if (yoff != 0) {
                            if (copyRLE4ScanlineToDst(lineNo, val, bdata)) {
                                finished++;
                            }
                            processImageProgress(100.0F * finished
                                                 / destinationRegion.height);
                            lineNo += isBottomUp ? -yoff : yoff;
                        }

                        l += xoff + yoff*width;
                        l %= width;
                    }
                    break;

                default:
                    int end = values[count-1] & 0xFF;
                    byte readByte = 0;
                    for (int i = 0; (i < end) && (count < imSize); i++) {
                        readByte = (byte)(((i & 1) == 0) ?
                                        (values[count] & 0xf0) >> 4 :
                                        (values[count++] & 0x0f));
                        if (l < width) {
                            val[l++] = readByte;
                        }
                    }

                    if ((end & 1) == 1) {
                        count++;
                    }

                    if ((((end + 1) / 2) & 1) == 1) {
                        count++;
                    }
                    break;
                }
            } else {
                if (count < imSize) {
                    int[] alternate = { (values[count] & 0xf0) >> 4,
                                        values[count] & 0x0f };
                    for (int i=0; (i < value) && (l < width); i++) {
                        val[l++] = (byte)alternate[i & 1];
                    }
                }

                count++;
            }

            if (flag) {
                break;
            }
        }
    }

    /** Decodes the jpeg/png image embedded in the bitmap using any jpeg
     *  ImageIO-style plugin.
     *
     * @param bi The destination {@code BufferedImage}.
     * @param bmpParam The {@code ImageReadParam} for decoding this
     *          BMP image.  The parameters for subregion, band selection and
     *          subsampling are used in decoding the jpeg image.
     */

    private BufferedImage readEmbedded(int type,
                              BufferedImage bi, ImageReadParam bmpParam)
      throws IOException {
        String format;
        switch(type) {
          case BI_JPEG:
              format = "JPEG";
              break;
          case BI_PNG:
              format = "PNG";
              break;
          default:
              throw new
                  IOException("Unexpected compression type: " + type);
        }
        ImageReader reader =
            ImageIO.getImageReadersByFormatName(format).next();
        if (reader == null) {
            throw new RuntimeException(I18N.getString("BMPImageReader4") +
                                       " " + format);
        }
        byte[] buff = new byte[(int)imageSize];
        iis.read(buff);
        reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(buff)));
        if (bi == null) {
            ImageTypeSpecifier embType = reader.getImageTypes(0).next();
            bi = embType.createBufferedImage(destinationRegion.x +
                                             destinationRegion.width,
                                             destinationRegion.y +
                                             destinationRegion.height);
        }

        reader.addIIOReadProgressListener(new EmbeddedProgressAdapter() {
                @Override
                public void imageProgress(ImageReader source,
                                          float percentageDone)
                {
                    processImageProgress(percentageDone);
                }
            });

        reader.addIIOReadUpdateListener(new IIOReadUpdateListener() {
                @Override
                public void imageUpdate(ImageReader source,
                                        BufferedImage theImage,
                                        int minX, int minY,
                                        int width, int height,
                                        int periodX, int periodY,
                                        int[] bands)
                {
                    processImageUpdate(theImage, minX, minY,
                                       width, height,
                                       periodX, periodY, bands);
                }
                @Override
                public void passComplete(ImageReader source,
                                         BufferedImage theImage)
                {
                    processPassComplete(theImage);
                }
                @Override
                public void passStarted(ImageReader source,
                                        BufferedImage theImage,
                                        int pass,
                                        int minPass, int maxPass,
                                        int minX, int minY,
                                        int periodX, int periodY,
                                        int[] bands)
                {
                    processPassStarted(theImage, pass, minPass, maxPass,
                                       minX, minY, periodX, periodY,
                                       bands);
                }
                @Override
                public void thumbnailPassComplete(ImageReader source,
                                                  BufferedImage thumb) {}
                @Override
                public void thumbnailPassStarted(ImageReader source,
                                                 BufferedImage thumb,
                                                 int pass,
                                                 int minPass, int maxPass,
                                                 int minX, int minY,
                                                 int periodX, int periodY,
                                                 int[] bands) {}
                @Override
                public void thumbnailUpdate(ImageReader source,
                                            BufferedImage theThumbnail,
                                            int minX, int minY,
                                            int width, int height,
                                            int periodX, int periodY,
                                            int[] bands) {}
            });

        reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                @Override
                public void warningOccurred(ImageReader source, String warning)
                {
                    processWarningOccurred(warning);
                }
            });

        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(bi);
        param.setDestinationBands(bmpParam.getDestinationBands());
        param.setDestinationOffset(bmpParam.getDestinationOffset());
        param.setSourceBands(bmpParam.getSourceBands());
        param.setSourceRegion(bmpParam.getSourceRegion());
        param.setSourceSubsampling(bmpParam.getSourceXSubsampling(),
                                   bmpParam.getSourceYSubsampling(),
                                   bmpParam.getSubsamplingXOffset(),
                                   bmpParam.getSubsamplingYOffset());
        reader.read(0, param);
        return bi;
    }

    private static class EmbeddedProgressAdapter implements IIOReadProgressListener {
        @Override
        public void imageComplete(ImageReader src) {}
        @Override
        public void imageProgress(ImageReader src, float percentageDone) {}
        @Override
        public void imageStarted(ImageReader src, int imageIndex) {}
        @Override
        public void thumbnailComplete(ImageReader src) {}
        @Override
        public void thumbnailProgress(ImageReader src, float percentageDone) {}
        @Override
        public void thumbnailStarted(ImageReader src, int iIdx, int tIdx) {}
        @Override
        public void sequenceComplete(ImageReader src) {}
        @Override
        public void sequenceStarted(ImageReader src, int minIndex) {}
        @Override
        public void readAborted(ImageReader src) {}
    }

    private static Boolean isLinkedProfileAllowed = null;

    @SuppressWarnings("removal")
    private static boolean isLinkedProfileAllowed() {
        if (isLinkedProfileAllowed == null) {
            PrivilegedAction<Boolean> a = new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.
                        getBoolean("sun.imageio.bmp.enableLinkedProfiles");
                }
            };
            isLinkedProfileAllowed = AccessController.doPrivileged(a);
        }
        return isLinkedProfileAllowed;
    }
}
