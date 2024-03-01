/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.net.*;
import javax.swing.*;
import sun.awt.AppContext;
import sun.swing.plaf.synth.Paint9Painter;

/**
 * ImagePainter fills in the specified region using an Image. The Image
 * is split into 9 segments: north, north east, east, south east, south,
 * south west, west, north west and the center. The corners are defined
 * by way of an insets, and the remaining regions are either tiled or
 * scaled to fit.
 *
 * @author Scott Violet
 */
class ImagePainter extends SynthPainter {
    private static final StringBuffer CACHE_KEY =
                               new StringBuffer("SynthCacheKey");

    private Image image;
    private Insets sInsets;
    private Insets dInsets;
    private URL path;
    private boolean tiles;
    private boolean paintCenter;
    private Paint9Painter imageCache;
    private boolean center;

    private static Paint9Painter getPaint9Painter() {
        synchronized(CACHE_KEY) {
            @SuppressWarnings("unchecked")
            WeakReference<Paint9Painter> cacheRef =
                     (WeakReference<Paint9Painter>)AppContext.getAppContext().
                     get(CACHE_KEY);
            Paint9Painter painter;
            if (cacheRef == null || (painter = cacheRef.get()) == null) {
                painter = new Paint9Painter(30);
                cacheRef = new WeakReference<Paint9Painter>(painter);
                AppContext.getAppContext().put(CACHE_KEY, cacheRef);
            }
            return painter;
        }
    }

    ImagePainter(boolean tiles, boolean paintCenter,
                 Insets sourceInsets, Insets destinationInsets, URL path,
                 boolean center) {
        if (sourceInsets != null) {
            this.sInsets = (Insets)sourceInsets.clone();
        }
        if (destinationInsets == null) {
            dInsets = sInsets;
        }
        else {
            this.dInsets = (Insets)destinationInsets.clone();
        }
        this.tiles = tiles;
        this.paintCenter = paintCenter;
        this.imageCache = getPaint9Painter();
        this.path = path;
        this.center = center;
    }

    public boolean getTiles() {
        return tiles;
    }

    public boolean getPaintsCenter() {
        return paintCenter;
    }

    public boolean getCenter() {
        return center;
    }

    public Insets getInsets(Insets insets) {
        if (insets == null) {
            return (Insets)this.dInsets.clone();
        }
        insets.left = this.dInsets.left;
        insets.right = this.dInsets.right;
        insets.top = this.dInsets.top;
        insets.bottom = this.dInsets.bottom;
        return insets;
    }

    public Image getImage() {
        if (image == null) {
            image = new ImageIcon(path, null).getImage();
        }
        return image;
    }

    private void paint(SynthContext context, Graphics g, int x, int y, int w,
                       int h) {
        Image image = getImage();
        if (Paint9Painter.validImage(image)) {
            Paint9Painter.PaintType type;
            if (getCenter()) {
                type = Paint9Painter.PaintType.CENTER;
            }
            else if (!getTiles()) {
                type = Paint9Painter.PaintType.PAINT9_STRETCH;
            }
            else {
                type = Paint9Painter.PaintType.PAINT9_TILE;
            }
            int mask = Paint9Painter.PAINT_ALL;
            if (!getCenter() && !getPaintsCenter()) {
                mask |= Paint9Painter.PAINT_CENTER;
            }
            imageCache.paint(context.getComponent(), g, x, y, w, h,
                             image, sInsets, dInsets, type,
                             mask);
        }
    }


    public void paintArrowButtonBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintArrowButtonBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintArrowButtonForeground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h,
                                           int direction) {
        paint(context, g, x, y, w, h);
    }

    public void paintButtonBackground(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintButtonBorder(SynthContext context,
                                  Graphics g, int x, int y,
                                  int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxMenuItemBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxMenuItemBorder(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxBorder(SynthContext context,
                                    Graphics g, int x, int y,
                                    int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintColorChooserBackground(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintColorChooserBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintComboBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintComboBoxBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopIconBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopIconBorder(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopPaneBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopPaneBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintEditorPaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintEditorPaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFileChooserBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFileChooserBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFormattedTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFormattedTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameTitlePaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameTitlePaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintLabelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintLabelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintListBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintListBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintOptionPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintOptionPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPanelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPanelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPasswordFieldBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPasswordFieldBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPopupMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPopupMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRootPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRootPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBorder(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarTrackBackground(SynthContext context,
                                              Graphics g, int x, int y,
                                              int w, int h, int orientation) {
         paint(context, g, x, y, w, h);
     }

    public void paintScrollBarTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarTrackBorder(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBackground(SynthContext context,
                                         Graphics g, int x, int y,
                                         int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBorder(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBackground(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBorder(SynthContext context,
                                  Graphics g, int x, int y,
                                  int w, int h, int orientation) {
         paint(context, g, x, y, w, h);
     }

    public void paintSliderThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderTrackBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }


    public void paintSliderTrackBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSpinnerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSpinnerBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDividerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDividerBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDividerForeground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDragDivider(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                             Graphics g, int x, int y,
                                             int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int tabIndex, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneContentBackground(SynthContext context,
                                         Graphics g, int x, int y, int w,
                                         int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneContentBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableHeaderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableHeaderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToggleButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToggleButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBackground(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBorder(SynthContext context,
                                   Graphics g, int x, int y,
                                   int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBackground(SynthContext context,
                                              Graphics g, int x, int y,
                                              int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBorder(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBorder(SynthContext context,
                                             Graphics g, int x, int y,
                                             int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolTipBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolTipBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeCellBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeCellBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeCellFocus(SynthContext context,
                                   Graphics g, int x, int y,
                                   int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintViewportBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintViewportBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }
}
