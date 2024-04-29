/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("serial") 
public class CCustomCursor extends Cursor {
    static Dimension sMaxCursorSize;
    static Dimension getMaxCursorSize() {
        if (sMaxCursorSize != null) return sMaxCursorSize;
        final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        return sMaxCursorSize = new Dimension(bounds.width / 2, bounds.height / 2);
    }

    Image fImage;
    Point fHotspot;
    int fWidth;
    int fHeight;

    public CCustomCursor(final Image cursor, final Point hotSpot, final String name) throws IndexOutOfBoundsException, HeadlessException {
        super(name);
        fImage = cursor;
        fHotspot = hotSpot;

        final Toolkit toolkit = Toolkit.getDefaultToolkit();

        final Component c = new Canvas(); 
        final MediaTracker tracker = new MediaTracker(c);
        tracker.addImage(fImage, 0);
        try {
            tracker.waitForAll();
        } catch (final InterruptedException e) {}

        int width = fImage.getWidth(c);
        int height = fImage.getHeight(c);

        if (tracker.isErrorAny() || width < 0 || height < 0) {
            fHotspot.x = fHotspot.y = 0;
            width = height = 1;
            fImage = createTransparentImage(width, height);
        } else {
            final Dimension nativeSize = toolkit.getBestCursorSize(width, height);
            width = nativeSize.width;
            height = nativeSize.height;
        }

        fWidth = width;
        fHeight = height;

        if (fHotspot.x >= width || fHotspot.y >= height || fHotspot.x < 0 || fHotspot.y < 0) {
            throw new IndexOutOfBoundsException("invalid hotSpot");
        }

        if (fHotspot.x >= width) {
            fHotspot.x = width - 1; 
        } else if (fHotspot.x < 0) {
            fHotspot.x = 0;
        }
        if (fHotspot.y >= height) {
            fHotspot.y = height - 1; 
        } else if (fHotspot.y < 0) {
            fHotspot.y = 0;
        }
    }

    private static BufferedImage createTransparentImage(int w, int h) {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();

        BufferedImage img = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, w, h);
        g.dispose();

        return img;
    }

    public static Dimension getBestCursorSize(final int preferredWidth, final int preferredHeight) {
        final Dimension maxCursorSize = getMaxCursorSize();
        final Dimension d = new Dimension(Math.max(1, Math.abs(preferredWidth)), Math.max(1, Math.abs(preferredHeight)));
        return new Dimension(Math.min(d.width, maxCursorSize.width), Math.min(d.height, maxCursorSize.height));
    }

    CImage fCImage;
    long getImageData() {
        if (fCImage != null) {
            return fCImage.ptr;
        }

        try {
            fCImage = CImage.getCreator().createFromImage(fImage);
            if (fCImage == null) {
                return 0L;
            } else {
                fCImage.resizeRepresentations(fWidth, fHeight);
                return fCImage.ptr;
            }
        } catch (IllegalArgumentException iae) {
            return 0L;
        }
    }

    Point getHotSpot() {
        return fHotspot;
    }
}
