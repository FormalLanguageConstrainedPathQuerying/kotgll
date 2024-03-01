/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Node;
import com.sun.imageio.plugins.common.ImageUtil;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.plugins.tiff.TIFFTagSet;

public class TIFFImageReader extends ImageReader {

    private static final int SAMPLES_PER_PIXEL_MAX = 1024;

    private static final int BITS_PER_SAMPLE_MAX = 64;

    private ImageInputStream stream = null;

    private boolean gotHeader = false;

    private ImageReadParam imageReadParam = getDefaultReadParam();

    private TIFFStreamMetadata streamMetadata = null;

    private int currIndex = -1;

    private TIFFImageMetadata imageMetadata = null;

    private List<Long> imageStartPosition = new ArrayList<Long>();

    private int numImages = -1;

    private HashMap<Integer, List<ImageTypeSpecifier>> imageTypeMap
            = new HashMap<Integer, List<ImageTypeSpecifier>>();

    private BufferedImage theImage = null;

    private int width = -1;
    private int height = -1;
    private int numBands = -1;
    private int tileOrStripWidth = -1, tileOrStripHeight = -1;

    private int planarConfiguration = BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;

    private int compression;
    private int photometricInterpretation;
    private int samplesPerPixel;
    private int[] sampleFormat;
    private int[] bitsPerSample;
    private int[] extraSamples;
    private char[] colorMap;

    private int sourceXOffset;
    private int sourceYOffset;
    private int srcXSubsampling;
    private int srcYSubsampling;

    private int dstWidth;
    private int dstHeight;
    private int dstMinX;
    private int dstMinY;
    private int dstXOffset;
    private int dstYOffset;

    private int tilesAcross;
    private int tilesDown;

    private int pixelsRead;
    private int pixelsToRead;

    public TIFFImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public void setInput(Object input,
            boolean seekForwardOnly,
            boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        resetLocal();

        if (input != null) {
            if (!(input instanceof ImageInputStream)) {
                throw new IllegalArgumentException("input not an ImageInputStream!");
            }
            this.stream = (ImageInputStream) input;
        } else {
            this.stream = null;
        }
    }

    private void readHeader() throws IIOException {
        if (gotHeader) {
            return;
        }
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        this.streamMetadata = new TIFFStreamMetadata();

        try {
            int byteOrder = stream.readUnsignedShort();
            if (byteOrder == 0x4d4d) {
                streamMetadata.byteOrder = ByteOrder.BIG_ENDIAN;
                stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            } else if (byteOrder == 0x4949) {
                streamMetadata.byteOrder = ByteOrder.LITTLE_ENDIAN;
                stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            } else {
                processWarningOccurred(
                        "Bad byte order in header, assuming little-endian");
                streamMetadata.byteOrder = ByteOrder.LITTLE_ENDIAN;
                stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            }

            int magic = stream.readUnsignedShort();
            if (magic != 42) {
                processWarningOccurred(
                        "Bad magic number in header, continuing");
            }

            long offset = stream.readUnsignedInt();
            stream.seek(offset);
            imageStartPosition.add(Long.valueOf(offset));
        } catch (IOException e) {
            throw new IIOException("I/O error reading header!", e);
        }

        gotHeader = true;
    }

    private int locateImage(int imageIndex) throws IIOException {
        readHeader();

        int index = Math.min(imageIndex, imageStartPosition.size() - 1);

        try {
            Long l = imageStartPosition.get(index);
            stream.seek(l.longValue());

            while (index < imageIndex) {
                int count = stream.readUnsignedShort();
                if (count == 0) {
                    imageIndex = index > 0 ? index - 1 : 0;
                    break;
                }
                stream.skipBytes(12 * count);

                long offset = stream.readUnsignedInt();
                if (offset == 0) {
                    return index;
                }

                stream.seek(offset);
                imageStartPosition.add(Long.valueOf(offset));
                ++index;
            }
        } catch (EOFException eofe) {
            forwardWarningMessage("Ignored " + eofe);

            imageIndex = index > 0 ? index - 1 : 0;
        } catch (IOException ioe) {
            throw new IIOException("Couldn't seek!", ioe);
        }

        if (currIndex != imageIndex) {
            imageMetadata = null;
        }
        currIndex = imageIndex;
        return imageIndex;
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }
        if (seekForwardOnly && allowSearch) {
            throw new IllegalStateException("seekForwardOnly and allowSearch can't both be true!");
        }

        if (numImages > 0) {
            return numImages;
        }
        if (allowSearch) {
            this.numImages = locateImage(Integer.MAX_VALUE) + 1;
        }
        return numImages;
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IIOException {
        readHeader();
        return streamMetadata;
    }

    private void checkIndex(int imageIndex) {
        if (imageIndex < minIndex) {
            throw new IndexOutOfBoundsException("imageIndex < minIndex!");
        }
        if (seekForwardOnly) {
            minIndex = imageIndex;
        }
    }

    private void seekToImage(int imageIndex) throws IIOException {
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException("imageIndex out of bounds!");
        }

        readMetadata();

        initializeFromMetadata();
    }

    private void readMetadata() throws IIOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        if (imageMetadata != null) {
            return;
        }
        try {
            List<TIFFTagSet> tagSets;
            boolean readUnknownTags = false;
            if (imageReadParam instanceof TIFFImageReadParam) {
                TIFFImageReadParam tp = (TIFFImageReadParam)imageReadParam;
                tagSets = tp.getAllowedTagSets();
                readUnknownTags = tp.getReadUnknownTags();
            } else {
                tagSets = new ArrayList<TIFFTagSet>(1);
                tagSets.add(BaselineTIFFTagSet.getInstance());
            }

            this.imageMetadata = new TIFFImageMetadata(tagSets);
            imageMetadata.initializeFromStream(stream, ignoreMetadata,
                                               readUnknownTags);
        } catch (IIOException iioe) {
            throw iioe;
        } catch (IOException ioe) {
            throw new IIOException("I/O error reading image metadata!", ioe);
        }
    }

    private int getWidth() {
        return this.width;
    }

    private int getHeight() {
        return this.height;
    }

    private int getTileOrStripWidth() {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_WIDTH);
        return (f == null) ? getWidth() : f.getAsInt(0);
    }

    private int getTileOrStripHeight() {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_LENGTH);
        if (f != null) {
            return f.getAsInt(0);
        }

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_ROWS_PER_STRIP);
        int h = (f == null) ? -1 : f.getAsInt(0);
        return (h == -1) ? getHeight() : h;
    }

    private int getPlanarConfiguration() {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION);
        if (f != null) {
            int planarConfigurationValue = f.getAsInt(0);
            if (planarConfigurationValue
                    == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
                if (getCompression()
                        == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG
                        && imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT)
                        != null) {
                    processWarningOccurred("PlanarConfiguration \"Planar\" value inconsistent with JPEGInterchangeFormat; resetting to \"Chunky\".");
                    planarConfigurationValue
                            = BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
                } else {
                    TIFFField offsetField
                            = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_OFFSETS);
                    if (offsetField == null) {
                        offsetField
                                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_STRIP_OFFSETS);
                        int tw = getTileOrStripWidth();
                        int th = getTileOrStripHeight();
                        int tAcross = (getWidth() + tw - 1) / tw;
                        int tDown = (getHeight() + th - 1) / th;
                        int tilesPerImage = tAcross * tDown;
                        long[] offsetArray = offsetField.getAsLongs();
                        if (offsetArray != null
                                && offsetArray.length == tilesPerImage) {
                            processWarningOccurred("PlanarConfiguration \"Planar\" value inconsistent with TileOffsets field value count; resetting to \"Chunky\".");
                            planarConfigurationValue
                                    = BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
                        }
                    } else {
                        int rowsPerStrip = getTileOrStripHeight();
                        int stripsPerImage
                                = (getHeight() + rowsPerStrip - 1) / rowsPerStrip;
                        long[] offsetArray = offsetField.getAsLongs();
                        if (offsetArray != null
                                && offsetArray.length == stripsPerImage) {
                            processWarningOccurred("PlanarConfiguration \"Planar\" value inconsistent with StripOffsets field value count; resetting to \"Chunky\".");
                            planarConfigurationValue
                                    = BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
                        }
                    }
                }
            }
            return planarConfigurationValue;
        }

        return BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
    }

    private long getTileOrStripOffset(int tileIndex) throws IIOException {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_OFFSETS);
        if (f == null) {
            f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_STRIP_OFFSETS);
        }
        if (f == null) {
            f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT);
        }

        if (f == null) {
            throw new IIOException("Missing required strip or tile offsets field.");
        }

        return f.getAsLong(tileIndex);
    }

    private long getTileOrStripByteCount(int tileIndex) throws IOException {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS);
        if (f == null) {
            f
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS);
        }
        if (f == null) {
            f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
        }

        long tileOrStripByteCount;
        if (f != null) {
            tileOrStripByteCount = f.getAsLong(tileIndex);
        } else {
            processWarningOccurred("TIFF directory contains neither StripByteCounts nor TileByteCounts field: attempting to calculate from strip or tile width and height.");

            int bitsPerPixel = bitsPerSample[0];
            for (int i = 1; i < samplesPerPixel; i++) {
                bitsPerPixel += bitsPerSample[i];
            }
            int bytesPerRow = (getTileOrStripWidth() * bitsPerPixel + 7) / 8;
            tileOrStripByteCount = bytesPerRow * getTileOrStripHeight();

            long streamLength = stream.length();
            if (streamLength != -1) {
                tileOrStripByteCount
                        = Math.min(tileOrStripByteCount,
                                streamLength - getTileOrStripOffset(tileIndex));
            } else {
                processWarningOccurred("Stream length is unknown: cannot clamp estimated strip or tile byte count to EOF.");
            }
        }

        return tileOrStripByteCount;
    }

    private int getCompression() {
        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_COMPRESSION);
        if (f == null) {
            return BaselineTIFFTagSet.COMPRESSION_NONE;
        } else {
            return f.getAsInt(0);
        }
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        seekToImage(imageIndex);
        return getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        seekToImage(imageIndex);
        return getHeight();
    }

    /**
     * Initializes these instance variables from the image metadata:
     * <pre>
     * compression
     * width
     * height
     * samplesPerPixel
     * numBands
     * colorMap
     * photometricInterpretation
     * sampleFormat
     * bitsPerSample
     * extraSamples
     * tileOrStripWidth
     * tileOrStripHeight
     * </pre>
     */
    private void initializeFromMetadata() throws IIOException {
        TIFFField f;

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_COMPRESSION);
        if (f == null) {
            processWarningOccurred("Compression field is missing; assuming no compression");
            compression = BaselineTIFFTagSet.COMPRESSION_NONE;
        } else {
            compression = f.getAsInt(0);
        }

        boolean isMissingDimension = false;

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
        if (f != null) {
            this.width = f.getAsInt(0);
        } else {
            processWarningOccurred("ImageWidth field is missing.");
            isMissingDimension = true;
        }

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH);
        if (f != null) {
            this.height = f.getAsInt(0);
        } else {
            processWarningOccurred("ImageLength field is missing.");
            isMissingDimension = true;
        }

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
        if (f != null) {
            samplesPerPixel = f.getAsInt(0);
        } else {
            samplesPerPixel = 1;
            isMissingDimension = true;
        }

        int defaultBitDepth = 1;
        if (isMissingDimension
                && (f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT)) != null) {
            Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("JPEG");
            if (iter != null && iter.hasNext()) {
                ImageReader jreader = iter.next();
                try {
                    stream.mark();
                    stream.seek(f.getAsLong(0));
                    jreader.setInput(stream);
                    if (imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) == null) {
                        this.width = jreader.getWidth(0);
                    }
                    if (imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH) == null) {
                        this.height = jreader.getHeight(0);
                    }
                    ImageTypeSpecifier imageType = jreader.getRawImageType(0);
                    if (imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL) == null) {
                        this.samplesPerPixel =
                            imageType != null ?
                                imageType.getSampleModel().getNumBands() : 3;
                    }
                    stream.reset();
                    defaultBitDepth =
                        imageType != null ?
                        imageType.getColorModel().getComponentSize(0) : 8;
                } catch (IOException e) {
                }
                jreader.dispose();
            }
        }

        if (samplesPerPixel < 1) {
            throw new IIOException("Samples per pixel < 1!");
        } else if (samplesPerPixel > SAMPLES_PER_PIXEL_MAX) {
            throw new IIOException
                ("Samples per pixel (" + samplesPerPixel
                + ") greater than allowed maximum ("
                + SAMPLES_PER_PIXEL_MAX + ")");
        }

        numBands = samplesPerPixel;

        this.colorMap = null;
        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_COLOR_MAP);
        if (f != null) {
            colorMap = f.getAsChars();
        }

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
        if (f == null) {
            if (compression == BaselineTIFFTagSet.COMPRESSION_CCITT_RLE
                    || compression == BaselineTIFFTagSet.COMPRESSION_CCITT_T_4
                    || compression == BaselineTIFFTagSet.COMPRESSION_CCITT_T_6) {
                processWarningOccurred("PhotometricInterpretation field is missing; "
                        + "assuming WhiteIsZero");
                photometricInterpretation
                        = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO;
            } else if (this.colorMap != null) {
                photometricInterpretation
                        = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR;
            } else if (samplesPerPixel == 3 || samplesPerPixel == 4) {
                photometricInterpretation
                        = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB;
            } else {
                processWarningOccurred("PhotometricInterpretation field is missing; "
                        + "assuming BlackIsZero");
                photometricInterpretation
                        = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO;
            }
        } else {
            photometricInterpretation = f.getAsInt(0);
        }

        boolean replicateFirst = false;
        int first = -1;

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_SAMPLE_FORMAT);
        sampleFormat = new int[samplesPerPixel];
        replicateFirst = false;
        if (f == null) {
            replicateFirst = true;
            first = BaselineTIFFTagSet.SAMPLE_FORMAT_UNDEFINED;
        } else if (f.getCount() != samplesPerPixel) {
            replicateFirst = true;
            first = f.getAsInt(0);
        }

        for (int i = 0; i < samplesPerPixel; i++) {
            sampleFormat[i] = replicateFirst ? first : f.getAsInt(i);
            if (sampleFormat[i]
                    != BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER
                    && sampleFormat[i]
                    != BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER
                    && sampleFormat[i]
                    != BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT
                    && sampleFormat[i]
                    != BaselineTIFFTagSet.SAMPLE_FORMAT_UNDEFINED) {
                processWarningOccurred(
                        "Illegal value for SAMPLE_FORMAT, assuming SAMPLE_FORMAT_UNDEFINED");
                sampleFormat[i] = BaselineTIFFTagSet.SAMPLE_FORMAT_UNDEFINED;
            }
        }

        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
        this.bitsPerSample = new int[samplesPerPixel];
        replicateFirst = false;
        if (f == null) {
            replicateFirst = true;
            first = defaultBitDepth;
        } else if (f.getCount() != samplesPerPixel) {
            replicateFirst = true;
            first = f.getAsInt(0);
        }

        for (int i = 0; i < samplesPerPixel; i++) {
            bitsPerSample[i] = replicateFirst ? first : f.getAsInt(i);
            if (bitsPerSample[i] > BITS_PER_SAMPLE_MAX) {
                throw new IIOException
                    ("Bits per sample (" + bitsPerSample[i]
                    + ") greater than allowed maximum ("
                    + BITS_PER_SAMPLE_MAX + ")");
            }
        }

        this.extraSamples = null;
        f = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES);
        if (f != null) {
            extraSamples = f.getAsInts();
        }
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IIOException {
        List<ImageTypeSpecifier> l; 

        Integer imageIndexInteger = Integer.valueOf(imageIndex);
        if (imageTypeMap.containsKey(imageIndexInteger)) {
            l = imageTypeMap.get(imageIndexInteger);
        } else {
            l = new ArrayList<ImageTypeSpecifier>(1);

            seekToImage(imageIndex);
            ImageTypeSpecifier itsRaw
                    = TIFFDecompressor.getRawImageTypeSpecifier(photometricInterpretation,
                            compression,
                            samplesPerPixel,
                            bitsPerSample,
                            sampleFormat,
                            extraSamples,
                            colorMap);

            TIFFField iccProfileField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_ICC_PROFILE);

            if (iccProfileField != null
                    && itsRaw.getColorModel() instanceof ComponentColorModel) {
                ColorModel cmRaw = itsRaw.getColorModel();
                ColorSpace csRaw = cmRaw.getColorSpace();
                SampleModel smRaw = itsRaw.getSampleModel();

                ColorSpace iccColorSpace = null;
                try {
                    byte[] iccProfileValue = iccProfileField.getAsBytes();
                    ICC_Profile iccProfile
                        = ICC_Profile.getInstance(iccProfileValue);
                    iccColorSpace = new ICC_ColorSpace(iccProfile);

                    float[] rgb =
                        iccColorSpace.toRGB(new float[] {1.0F, 1.0F, 1.0F});
                } catch (Exception iccProfileException) {
                    processWarningOccurred("Superseding bad ICC profile: "
                        + iccProfileException.getMessage());

                    if (iccColorSpace != null) {
                        switch (iccColorSpace.getType()) {
                            case ColorSpace.TYPE_GRAY:
                                iccColorSpace =
                                    ColorSpace.getInstance(ColorSpace.CS_GRAY);
                                break;
                            case ColorSpace.TYPE_RGB:
                                iccColorSpace =
                                    ColorSpace.getInstance(ColorSpace.CS_sRGB);
                                break;
                            default:
                                iccColorSpace = csRaw;
                                break;
                        }
                    } else {
                        iccColorSpace = csRaw;
                    }
                }

                int numBands = smRaw.getNumBands();
                int numComponents = iccColorSpace.getNumComponents();

                if (numBands == numComponents
                        || numBands == numComponents + 1) {
                    boolean hasAlpha = numComponents != numBands;
                    boolean isAlphaPre
                            = hasAlpha && cmRaw.isAlphaPremultiplied();

                    ColorModel iccColorModel
                            = new ComponentColorModel(iccColorSpace,
                                    cmRaw.getComponentSize(),
                                    hasAlpha,
                                    isAlphaPre,
                                    cmRaw.getTransparency(),
                                    cmRaw.getTransferType());

                    l.add(new ImageTypeSpecifier(iccColorModel, smRaw));

                    if (csRaw.getType() == iccColorSpace.getType()
                            && csRaw.getNumComponents()
                            == iccColorSpace.getNumComponents()) {
                        l.add(itsRaw);
                    }
                } else { 
                    l.add(itsRaw);
                }
            } else { 
                l.add(itsRaw);
            }

            imageTypeMap.put(imageIndexInteger, l);
        }

        return l.iterator();
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IIOException {
        seekToImage(imageIndex);
        TIFFImageMetadata im
                = new TIFFImageMetadata(imageMetadata.getRootIFD().getTagSetList());
        Node root
                = imageMetadata.getAsTree(TIFFImageMetadata.NATIVE_METADATA_FORMAT_NAME);
        im.setFromTree(TIFFImageMetadata.NATIVE_METADATA_FORMAT_NAME, root);
        return im;
    }

    public IIOMetadata getStreamMetadata(int imageIndex) throws IIOException {
        readHeader();
        TIFFStreamMetadata sm = new TIFFStreamMetadata();
        Node root = sm.getAsTree(TIFFStreamMetadata.NATIVE_METADATA_FORMAT_NAME);
        sm.setFromTree(TIFFStreamMetadata.NATIVE_METADATA_FORMAT_NAME, root);
        return sm;
    }

    @Override
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        if (currIndex != -1) {
            seekToImage(currIndex);
            return getCompression() == BaselineTIFFTagSet.COMPRESSION_NONE;
        } else {
            return false;
        }
    }

    public boolean readSupportsThumbnails() {
        return false;
    }

    @Override
    public boolean hasThumbnails(int imageIndex) {
        return false;
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        return 0;
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new TIFFImageReadParam();
    }

    @Override
    public boolean isImageTiled(int imageIndex) throws IOException {
        seekToImage(imageIndex);

        TIFFField f
                = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_TILE_WIDTH);
        return f != null;
    }

    @Override
    public int getTileWidth(int imageIndex) throws IOException {
        seekToImage(imageIndex);
        return getTileOrStripWidth();
    }

    @Override
    public int getTileHeight(int imageIndex) throws IOException {
        seekToImage(imageIndex);
        return getTileOrStripHeight();
    }

    @Override
    public BufferedImage readTile(int imageIndex, int tileX, int tileY)
            throws IOException {

        int w = getWidth(imageIndex);
        int h = getHeight(imageIndex);
        int tw = getTileWidth(imageIndex);
        int th = getTileHeight(imageIndex);

        int x = tw * tileX;
        int y = th * tileY;

        if (tileX < 0 || tileY < 0 || x >= w || y >= h) {
            throw new IllegalArgumentException("Tile indices are out of bounds!");
        }

        if (x + tw > w) {
            tw = w - x;
        }

        if (y + th > h) {
            th = h - y;
        }

        ImageReadParam param = getDefaultReadParam();
        Rectangle tileRect = new Rectangle(x, y, tw, th);
        param.setSourceRegion(tileRect);

        return read(imageIndex, param);
    }

    @Override
    public boolean canReadRaster() {
        return false;
    }

    @Override
    public Raster readRaster(int imageIndex, ImageReadParam param)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    private int[] sourceBands;
    private int[] destinationBands;

    private TIFFDecompressor decompressor;

    private static int ifloor(int num, int den) {
        if (num < 0) {
            num -= den - 1;
        }
        return num / den;
    }

    private static int iceil(int num, int den) {
        if (num > 0) {
            num += den - 1;
        }
        return num / den;
    }

    private void prepareRead(int imageIndex, ImageReadParam param)
            throws IOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        if (param == null) {
            param = getDefaultReadParam();
        }

        this.imageReadParam = param;

        seekToImage(imageIndex);

        this.tileOrStripWidth = getTileOrStripWidth();
        this.tileOrStripHeight = getTileOrStripHeight();
        this.planarConfiguration = getPlanarConfiguration();

        this.sourceBands = param.getSourceBands();
        if (sourceBands == null) {
            sourceBands = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                sourceBands[i] = i;
            }
        }

        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        ImageTypeSpecifier theImageType
                = ImageUtil.getDestinationType(param, imageTypes);

        int destNumBands = theImageType.getSampleModel().getNumBands();

        this.destinationBands = param.getDestinationBands();
        if (destinationBands == null) {
            destinationBands = new int[destNumBands];
            for (int i = 0; i < destNumBands; i++) {
                destinationBands[i] = i;
            }
        }

        if (sourceBands.length != destinationBands.length) {
            throw new IllegalArgumentException(
                    "sourceBands.length != destinationBands.length");
        }

        for (int i = 0; i < sourceBands.length; i++) {
            int sb = sourceBands[i];
            if (sb < 0 || sb >= numBands) {
                throw new IllegalArgumentException(
                        "Source band out of range!");
            }
            int db = destinationBands[i];
            if (db < 0 || db >= destNumBands) {
                throw new IllegalArgumentException(
                        "Destination band out of range!");
            }
        }
    }

    @Override
    public RenderedImage readAsRenderedImage(int imageIndex,
            ImageReadParam param)
            throws IOException {
        prepareRead(imageIndex, param);
        return new TIFFRenderedImage(this, imageIndex, imageReadParam,
                width, height);
    }

    private void decodeTile(int ti, int tj, int band) throws IOException {
        Rectangle tileRect = new Rectangle(ti * tileOrStripWidth,
                tj * tileOrStripHeight,
                tileOrStripWidth,
                tileOrStripHeight);

        if (!isImageTiled(currIndex)) {
            tileRect
                    = tileRect.intersection(new Rectangle(0, 0, width, height));
        }

        if (tileRect.width <= 0 || tileRect.height <= 0) {
            return;
        }

        int srcMinX = tileRect.x;
        int srcMinY = tileRect.y;
        int srcWidth = tileRect.width;
        int srcHeight = tileRect.height;

        dstMinX = iceil(srcMinX - sourceXOffset, srcXSubsampling);
        int dstMaxX = ifloor(srcMinX + srcWidth - 1 - sourceXOffset,
                srcXSubsampling);

        dstMinY = iceil(srcMinY - sourceYOffset, srcYSubsampling);
        int dstMaxY = ifloor(srcMinY + srcHeight - 1 - sourceYOffset,
                srcYSubsampling);

        dstWidth = dstMaxX - dstMinX + 1;
        dstHeight = dstMaxY - dstMinY + 1;

        dstMinX += dstXOffset;
        dstMinY += dstYOffset;

        Rectangle dstRect = new Rectangle(dstMinX, dstMinY,
                dstWidth, dstHeight);
        dstRect
                = dstRect.intersection(theImage.getRaster().getBounds());

        dstMinX = dstRect.x;
        dstMinY = dstRect.y;
        dstWidth = dstRect.width;
        dstHeight = dstRect.height;

        if (dstWidth <= 0 || dstHeight <= 0) {
            return;
        }

        int activeSrcMinX = (dstMinX - dstXOffset) * srcXSubsampling
                + sourceXOffset;
        int sxmax
                = (dstMinX + dstWidth - 1 - dstXOffset) * srcXSubsampling
                + sourceXOffset;
        int activeSrcWidth = sxmax - activeSrcMinX + 1;

        int activeSrcMinY = (dstMinY - dstYOffset) * srcYSubsampling
                + sourceYOffset;
        int symax
                = (dstMinY + dstHeight - 1 - dstYOffset) * srcYSubsampling
                + sourceYOffset;
        int activeSrcHeight = symax - activeSrcMinY + 1;

        decompressor.setSrcMinX(srcMinX);
        decompressor.setSrcMinY(srcMinY);
        decompressor.setSrcWidth(srcWidth);
        decompressor.setSrcHeight(srcHeight);

        decompressor.setDstMinX(dstMinX);
        decompressor.setDstMinY(dstMinY);
        decompressor.setDstWidth(dstWidth);
        decompressor.setDstHeight(dstHeight);

        decompressor.setActiveSrcMinX(activeSrcMinX);
        decompressor.setActiveSrcMinY(activeSrcMinY);
        decompressor.setActiveSrcWidth(activeSrcWidth);
        decompressor.setActiveSrcHeight(activeSrcHeight);

        int tileIndex = tj * tilesAcross + ti;

        if (planarConfiguration
                == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
            tileIndex += band * tilesAcross * tilesDown;
        }

        long offset = getTileOrStripOffset(tileIndex);
        long byteCount = getTileOrStripByteCount(tileIndex);

        decompressor.setPlanarBand(band);
        decompressor.setStream(stream);
        decompressor.setOffset(offset);
        decompressor.setByteCount((int) byteCount);

        decompressor.beginDecoding();

        stream.mark();
        decompressor.decode();
        stream.reset();
    }

    private void reportProgress() {
        pixelsRead += dstWidth * dstHeight;
        processImageProgress(100.0f * pixelsRead / pixelsToRead);
        processImageUpdate(theImage,
                dstMinX, dstMinY, dstWidth, dstHeight,
                1, 1,
                destinationBands);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param)
            throws IOException {
        prepareRead(imageIndex, param);
        this.theImage = getDestination(param,
                getImageTypes(imageIndex),
                width, height);

        srcXSubsampling = imageReadParam.getSourceXSubsampling();
        srcYSubsampling = imageReadParam.getSourceYSubsampling();

        Point p = imageReadParam.getDestinationOffset();
        dstXOffset = p.x;
        dstYOffset = p.y;

        Rectangle srcRegion = new Rectangle(0, 0, 0, 0);
        Rectangle destRegion = new Rectangle(0, 0, 0, 0);

        computeRegions(imageReadParam, width, height, theImage,
                srcRegion, destRegion);

        sourceXOffset = srcRegion.x;
        sourceYOffset = srcRegion.y;

        pixelsToRead = destRegion.width * destRegion.height;
        pixelsRead = 0;

        clearAbortRequest();
        processImageStarted(imageIndex);
        if (abortRequested()) {
            processReadAborted();
            return theImage;
        }

        tilesAcross = (width + tileOrStripWidth - 1) / tileOrStripWidth;
        tilesDown = (height + tileOrStripHeight - 1) / tileOrStripHeight;

        int compression = getCompression();

        if (compression == BaselineTIFFTagSet.COMPRESSION_NONE) {
            TIFFField fillOrderField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_FILL_ORDER);

            if (fillOrderField != null && fillOrderField.getAsInt(0) == 2) {
                this.decompressor = new TIFFLSBDecompressor();
            } else {
                this.decompressor = new TIFFNullDecompressor();
            }
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_CCITT_T_6) {
            this.decompressor = new TIFFFaxDecompressor();
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_CCITT_T_4) {
            this.decompressor = new TIFFFaxDecompressor();
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_CCITT_RLE) {
            this.decompressor = new TIFFFaxDecompressor();
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_PACKBITS) {
            this.decompressor = new TIFFPackBitsDecompressor();
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_LZW) {
            TIFFField predictorField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_PREDICTOR);
            int predictor = ((predictorField == null)
                    ? BaselineTIFFTagSet.PREDICTOR_NONE
                    : predictorField.getAsInt(0));

            TIFFField fillOrderField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_FILL_ORDER);
            int fillOrder = ((fillOrderField == null)
                    ? BaselineTIFFTagSet.FILL_ORDER_LEFT_TO_RIGHT
                    : fillOrderField.getAsInt(0));

            this.decompressor = new TIFFLZWDecompressor(predictor, fillOrder);
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_JPEG) {
            this.decompressor = new TIFFJPEGDecompressor();
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_ZLIB
                || compression
                == BaselineTIFFTagSet.COMPRESSION_DEFLATE) {
            TIFFField predictorField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_PREDICTOR);
            int predictor = ((predictorField == null)
                    ? BaselineTIFFTagSet.PREDICTOR_NONE
                    : predictorField.getAsInt(0));
            this.decompressor = new TIFFDeflateDecompressor(predictor);
        } else if (compression
                == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) {
            TIFFField JPEGProcField
                    = imageMetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_PROC);
            if (JPEGProcField == null) {
                processWarningOccurred("JPEGProc field missing; assuming baseline sequential JPEG process.");
            } else if (JPEGProcField.getAsInt(0)
                    != BaselineTIFFTagSet.JPEG_PROC_BASELINE) {
                throw new IIOException("Old-style JPEG supported for baseline sequential JPEG process only!");
            }
            this.decompressor = new TIFFOldJPEGDecompressor();
        } else {
            throw new IIOException("Unsupported compression type (tag value = "
                    + compression + ")!");
        }

        if (photometricInterpretation
                == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_Y_CB_CR
                && compression != BaselineTIFFTagSet.COMPRESSION_JPEG
                && compression != BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) {
            boolean convertYCbCrToRGB
                    = theImage.getColorModel().getColorSpace().getType()
                    == ColorSpace.TYPE_RGB;
            TIFFDecompressor wrappedDecompressor
                    = this.decompressor instanceof TIFFNullDecompressor
                            ? null : this.decompressor;
            this.decompressor
                    = new TIFFYCbCrDecompressor(wrappedDecompressor,
                            convertYCbCrToRGB);
        }

        TIFFColorConverter colorConverter = null;
        if (photometricInterpretation
                == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CIELAB
                && theImage.getColorModel().getColorSpace().getType()
                == ColorSpace.TYPE_RGB) {
            colorConverter = new TIFFCIELabColorConverter();
        } else if (photometricInterpretation
                == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_Y_CB_CR
                && !(this.decompressor instanceof TIFFYCbCrDecompressor)
                && compression != BaselineTIFFTagSet.COMPRESSION_JPEG
                && compression != BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) {
            colorConverter = new TIFFYCbCrColorConverter(imageMetadata);
        }

        decompressor.setReader(this);
        decompressor.setMetadata(imageMetadata);
        decompressor.setImage(theImage);

        decompressor.setPhotometricInterpretation(photometricInterpretation);
        decompressor.setCompression(compression);
        decompressor.setSamplesPerPixel(samplesPerPixel);
        decompressor.setBitsPerSample(bitsPerSample);
        decompressor.setSampleFormat(sampleFormat);
        decompressor.setExtraSamples(extraSamples);
        decompressor.setColorMap(colorMap);

        decompressor.setColorConverter(colorConverter);

        decompressor.setSourceXOffset(sourceXOffset);
        decompressor.setSourceYOffset(sourceYOffset);
        decompressor.setSubsampleX(srcXSubsampling);
        decompressor.setSubsampleY(srcYSubsampling);

        decompressor.setDstXOffset(dstXOffset);
        decompressor.setDstYOffset(dstYOffset);

        decompressor.setSourceBands(sourceBands);
        decompressor.setDestinationBands(destinationBands);

        int minTileX
                = TIFFImageWriter.XToTileX(srcRegion.x, 0, tileOrStripWidth);
        int minTileY
                = TIFFImageWriter.YToTileY(srcRegion.y, 0, tileOrStripHeight);
        int maxTileX
                = TIFFImageWriter.XToTileX(srcRegion.x + srcRegion.width - 1,
                        0, tileOrStripWidth);
        int maxTileY
                = TIFFImageWriter.YToTileY(srcRegion.y + srcRegion.height - 1,
                        0, tileOrStripHeight);

        if (planarConfiguration
                == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {

            decompressor.setPlanar(true);

            int[] sb = new int[1];
            int[] db = new int[1];
            for (int tj = minTileY; tj <= maxTileY; tj++) {
                for (int ti = minTileX; ti <= maxTileX; ti++) {
                    for (int band = 0; band < numBands; band++) {
                        sb[0] = sourceBands[band];
                        decompressor.setSourceBands(sb);
                        db[0] = destinationBands[band];
                        decompressor.setDestinationBands(db);

                        decodeTile(ti, tj, band);
                    }

                    reportProgress();
                    if (abortRequested()) {
                        processReadAborted();
                        return theImage;
                    }
                }
            }
        } else {
            for (int tj = minTileY; tj <= maxTileY; tj++) {
                for (int ti = minTileX; ti <= maxTileX; ti++) {
                    decodeTile(ti, tj, -1);

                    reportProgress();
                    if (abortRequested()) {
                        processReadAborted();
                        return theImage;
                    }
                }
            }
        }
        processImageComplete();
        return theImage;
    }

    @Override
    public void reset() {
        super.reset();
        resetLocal();
    }

    protected void resetLocal() {
        stream = null;
        gotHeader = false;
        imageReadParam = getDefaultReadParam();
        streamMetadata = null;
        currIndex = -1;
        imageMetadata = null;
        imageStartPosition = new ArrayList<Long>();
        numImages = -1;
        imageTypeMap = new HashMap<Integer, List<ImageTypeSpecifier>>();
        width = -1;
        height = -1;
        numBands = -1;
        tileOrStripWidth = -1;
        tileOrStripHeight = -1;
        planarConfiguration = BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
    }

    /**
     * Package scope method to allow decompressors, for example, to emit warning
     * messages.
     */
    void forwardWarningMessage(String warning) {
        processWarningOccurred(warning);
    }
}
