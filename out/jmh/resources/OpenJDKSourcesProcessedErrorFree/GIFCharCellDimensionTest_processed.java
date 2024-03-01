/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6686236
 * @summary Checks whether GIF native metadata format returns appropriate max
 *          value that could be set for characterCellWidth and
 *          characterCellHeight attributes of PlainTextExtension node. Besides,
 *          the test also checks whether IIOInvalidTreeException is thrown when
 *          incorrect value is set on these two attributes.
 * @run main GIFCharCellDimensionTest
 */
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import java.util.Iterator;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;

public class GIFCharCellDimensionTest {
    private static IIOMetadata imageMetadata = null;
    private static IIOMetadataFormat metadataFormat = null;
    private static String formatName = null;

    private static void initializeTest() {
        ImageWriter gifWriter = null;
        Iterator<ImageWriter> iterGifWriter = null;
        BufferedImage bufImage = null;
        ImageTypeSpecifier imageType = null;

        try {
            iterGifWriter = ImageIO.getImageWritersBySuffix("GIF");
            if (iterGifWriter.hasNext()) {
                gifWriter = iterGifWriter.next();
                bufImage = new BufferedImage(32, 32, TYPE_4BYTE_ABGR);

                imageMetadata = gifWriter.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(bufImage),
                        gifWriter.getDefaultWriteParam());
                if (imageMetadata == null) {
                    reportException("Test Failed. Could not get image" +
                            " metadata.");
                }

                formatName = imageMetadata.getNativeMetadataFormatName();
                metadataFormat = imageMetadata.getMetadataFormat(formatName);
                if (metadataFormat == null) {
                    reportException("Test Failed. Could not get native" +
                            " metadata format.");
                }
            } else {
                reportException("Test Failed. No GIF image writer found.");
            }
        } finally {
            gifWriter.dispose();
        }
    }

    private static IIOMetadataNode createPlainTextExtensionNode(String value) {
        IIOMetadataNode rootNode = null;

        if (imageMetadata != null && formatName != null) {
            IIOMetadataNode plainTextNode = null;

            rootNode = new IIOMetadataNode(formatName);
            plainTextNode = new IIOMetadataNode("PlainTextExtension");
            plainTextNode.setAttribute("textGridLeft", "0");
            plainTextNode.setAttribute("textGridTop", "0");
            plainTextNode.setAttribute("textGridWidth", "32");
            plainTextNode.setAttribute("textGridHeight", "32");
            plainTextNode.setAttribute("characterCellWidth", value);
            plainTextNode.setAttribute("characterCellHeight", value);
            plainTextNode.setAttribute("textForegroundColor", "0");
            plainTextNode.setAttribute("textBackgroundColor", "1");
            rootNode.appendChild(plainTextNode);
        } else {
            reportException("Test Failed. Un-initialized image metadata.");
        }

        return rootNode;
    }

    private static void testCharacterCellDimensions() {
        if (imageMetadata != null && metadataFormat != null) {
            String cellWidth = metadataFormat.getAttributeMaxValue(
                    "PlainTextExtension",
                    "characterCellWidth");
            String cellHeight = metadataFormat.getAttributeMaxValue(
                    "PlainTextExtension",
                    "characterCellHeight");

            int maxCharCellWidth = Integer.parseInt(cellWidth);
            int maxCharCellHeight = Integer.parseInt(cellHeight);
            if (maxCharCellWidth > 255 || maxCharCellHeight > 255) {
                reportException("Test Failed. Invalid max range for" +
                        " character cell width or character cell height.");
            }

            try {
                IIOMetadataNode root = createPlainTextExtensionNode("256");
                imageMetadata.setFromTree(formatName, root);
            } catch (IIOInvalidTreeException exception) {
            }
        } else {
            reportException("Test Failed. Un-initialized image metadata or" +
                    " metadata format.");
        }
    }

    private static void reportException(String message) {
        throw new RuntimeException(message);
    }

    public static void main(String[] args) {
        initializeTest();

        testCharacterCellDimensions();
    }
}