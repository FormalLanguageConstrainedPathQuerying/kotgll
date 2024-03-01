/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboPopup;

import sun.lwawt.macosx.CPlatformWindow;

@SuppressWarnings("serial") 
final class AquaComboBoxPopup extends BasicComboPopup {
    static final int FOCUS_RING_PAD_LEFT = 6;
    static final int FOCUS_RING_PAD_RIGHT = 6;
    static final int FOCUS_RING_PAD_BOTTOM = 5;

    protected Component topStrut;
    protected Component bottomStrut;
    protected boolean isPopDown = false;

    public AquaComboBoxPopup(final JComboBox<Object> cBox) {
        super(cBox);
    }

    @Override
    protected void configurePopup() {
        super.configurePopup();

        setBorderPainted(false);
        setBorder(null);
        updateContents(false);

        putClientProperty(CPlatformWindow.WINDOW_FADE_OUT, Integer.valueOf(150));
    }

    public void updateContents(final boolean remove) {

        isPopDown = isPopdown();
        if (isPopDown) {
            if (remove) {
                if (topStrut != null) {
                    this.remove(topStrut);
                }
                if (bottomStrut != null) {
                    this.remove(bottomStrut);
                }
            } else {
                add(scroller);
            }
        } else {
            if (topStrut == null) {
                topStrut = Box.createVerticalStrut(4);
                bottomStrut = Box.createVerticalStrut(4);
            }

            if (remove) remove(scroller);

            this.add(topStrut);
            this.add(scroller);
            this.add(bottomStrut);
        }
    }

    protected Dimension getBestPopupSizeForRowCount(final int maxRowCount) {
        final int currentElementCount = comboBox.getModel().getSize();
        final int rowCount = Math.min(maxRowCount, currentElementCount);

        final Dimension popupSize = new Dimension();
        final ListCellRenderer<Object> renderer = list.getCellRenderer();

        for (int i = 0; i < rowCount; i++) {
            final Object value = list.getModel().getElementAt(i);
            final Component c = renderer.getListCellRendererComponent(list, value, i, false, false);

            final Dimension prefSize = c.getPreferredSize();
            popupSize.height += prefSize.height;
            popupSize.width = Math.max(prefSize.width, popupSize.width);
        }

        popupSize.width += 10;

        return popupSize;
    }

    protected boolean shouldScroll() {
        return comboBox.getItemCount() > comboBox.getMaximumRowCount();
    }

    protected boolean isPopdown() {
        return shouldScroll() || AquaComboBoxUI.isPopdown(comboBox);
    }

    @Override
    public void show() {
        final int startItemCount = comboBox.getItemCount();

        final Rectangle popupBounds = adjustPopupAndGetBounds();
        if (popupBounds == null) return; 

        comboBox.firePopupMenuWillBecomeVisible();
        show(comboBox, popupBounds.x, popupBounds.y);

        final int afterShowItemCount = comboBox.getItemCount();
        if (afterShowItemCount == 0) {
            hide();
            return;
        }

        if (startItemCount != afterShowItemCount) {
            final Rectangle newBounds = adjustPopupAndGetBounds();
            list.setSize(newBounds.width, newBounds.height);
            pack();

            final Point newLoc = comboBox.getLocationOnScreen();
            setLocation(newLoc.x + newBounds.x, newLoc.y + newBounds.y);
        }

        list.requestFocusInWindow();
    }

    @Override
    @SuppressWarnings("serial") 
    protected JList<Object> createList() {
        return new JList<Object>(comboBox.getModel()) {
            @Override
            @SuppressWarnings("deprecation")
            public void processMouseEvent(MouseEvent e) {
                if (e.isMetaDown()) {
                    e = new MouseEvent((Component) e.getSource(), e.getID(),
                                       e.getWhen(),
                                       e.getModifiers() ^ InputEvent.META_MASK,
                                       e.getX(), e.getY(), e.getXOnScreen(),
                                       e.getYOnScreen(), e.getClickCount(),
                                       e.isPopupTrigger(), MouseEvent.NOBUTTON);
                }
                super.processMouseEvent(e);
            }
        };
    }

    protected Rectangle adjustPopupAndGetBounds() {
        if (isPopDown != isPopdown()) {
            updateContents(true);
        }

        int popupBoundsY = comboBox.getBounds().height;
        if (comboBox.isEditable() && comboBox.getBorder() != null) {
            Insets inset = comboBox.getBorder().getBorderInsets(comboBox);
            popupBoundsY += inset.top + inset.bottom;
        }

        final Dimension popupSize = getBestPopupSizeForRowCount(comboBox.getMaximumRowCount());
        final Rectangle popupBounds = computePopupBounds(0, popupBoundsY, popupSize.width, popupSize.height);
        if (popupBounds == null) return null; 

        final Dimension realPopupSize = popupBounds.getSize();
        scroller.setMaximumSize(realPopupSize);
        scroller.setPreferredSize(realPopupSize);
        scroller.setMinimumSize(realPopupSize);
        list.invalidate();

        final int selectedIndex = comboBox.getSelectedIndex();
        if (selectedIndex == -1) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(selectedIndex);
        }
        list.ensureIndexIsVisible(list.getSelectedIndex());

        return popupBounds;
    }

    Rectangle getBestScreenBounds(final Point p) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();
        for (final GraphicsDevice gd : gs) {
            final GraphicsConfiguration[] gc = gd.getConfigurations();
            for (final GraphicsConfiguration element0 : gc) {
                final Rectangle gcBounds = element0.getBounds();
                if (gcBounds.contains(p)) {
                    return getAvailableScreenArea(gcBounds, element0);
                }
            }
        }

        final Rectangle comboBoxBounds = comboBox.getBounds();
        comboBoxBounds.setLocation(p);
        for (final GraphicsDevice gd : gs) {
            final GraphicsConfiguration[] gc = gd.getConfigurations();
            for (final GraphicsConfiguration element0 : gc) {
                final Rectangle gcBounds = element0.getBounds();
                if (gcBounds.intersects(comboBoxBounds)) {
                    return getAvailableScreenArea(gcBounds, element0);
                }
            }
        }

        return null;
    }

    private Rectangle getAvailableScreenArea(Rectangle bounds,
                                             GraphicsConfiguration gc) {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                             bounds.width - insets.left - insets.right,
                             bounds.height - insets.top - insets.bottom);
    }

    private int getComboBoxEdge(int py, boolean bottom) {
        int offset = bottom ? 9 : -9;
        return Math.min((py / 2) + offset, py);
    }

    @Override
    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        final int itemCount = comboBox.getModel().getSize();
        final boolean isPopdown = isPopdown();
        final boolean isTableCellEditor = AquaComboBoxUI.isTableCellEditor(comboBox);
        if (isPopdown && !isTableCellEditor) {
            py = getComboBoxEdge(py, true);
        }


        final Point p = new Point(0, 0);
        SwingUtilities.convertPointToScreen(p, comboBox);
        final Rectangle scrBounds = getBestScreenBounds(p);

        if (scrBounds == null) return super.computePopupBounds(px, py, pw, ph);

        final Insets comboBoxInsets = comboBox.getInsets();
        final Rectangle comboBoxBounds = comboBox.getBounds();

        if (shouldScroll()) {
            pw += 15;
        }

        if (isPopdown) {
            pw += 4;
        }

        final int minWidth = comboBoxBounds.width - (comboBoxInsets.left + comboBoxInsets.right);
        pw = Math.max(minWidth, pw);

        final boolean leftToRight = AquaUtils.isLeftToRight(comboBox);
        if (leftToRight) {
            px += comboBoxInsets.left;
            if (!isPopDown) px -= FOCUS_RING_PAD_LEFT;
        } else {
            px = comboBoxBounds.width - pw - comboBoxInsets.right;
            if (!isPopDown) px += FOCUS_RING_PAD_RIGHT;
        }
        py -= (comboBoxInsets.bottom); 

        p.x += px;
        p.y += py; 
        if (p.x < scrBounds.x) {
            px = px + (scrBounds.x - p.x);
        }
        if (p.y < scrBounds.y) {
            py = py + (scrBounds.y - p.y);
        }

        final Point top = new Point(0, 0);
        SwingUtilities.convertPointFromScreen(top, comboBox);

        final int maxWidth = Math.min(scrBounds.width, top.x + scrBounds.x + scrBounds.width) - 2; 

        pw = Math.min(maxWidth, pw);
        if (pw < minWidth) {
            px -= (minWidth - pw);
            pw = minWidth;
        }

        if (!isPopdown) {
            pw -= 6;
            return computePopupBoundsForMenu(px, py, pw, ph, itemCount, scrBounds);
        }

        if (!isTableCellEditor) {
            pw -= (FOCUS_RING_PAD_LEFT + FOCUS_RING_PAD_RIGHT);
            if (leftToRight) {
                px += FOCUS_RING_PAD_LEFT;
            }
        }

        final Rectangle r = new Rectangle(px, py, pw, ph);
        if (r.y + r.height < top.y + scrBounds.y + scrBounds.height) {
            if (!comboBox.isEditable()) {
                r.y += (comboBoxInsets.top + comboBoxInsets.bottom) / 2;
            }
            return r;
        }
        int newY = getComboBoxEdge(comboBoxBounds.height, false) - ph - comboBoxInsets.top;
        if (newY > top.y + scrBounds.y) {
            return new Rectangle(px, newY, r.width, r.height);
        } else {
            r.y = top.y + scrBounds.y + Math.max(0, (scrBounds.height - ph) / 2 );
            r.height = Math.min(scrBounds.height, ph);
        }
        return r;
    }

    protected Rectangle computePopupBoundsForMenu(final int px, final int py,
                                                  final int pw, final int ph,
                                                  final int itemCount,
                                                  final Rectangle scrBounds) {
        int elementSize = 0; 
        if (list != null && itemCount > 0) {
            final Rectangle cellBounds = list.getCellBounds(0, 0);
            if (cellBounds != null) elementSize = cellBounds.height;
        }

        int offsetIndex = comboBox.getSelectedIndex();
        if (offsetIndex < 0) offsetIndex = 0;
        list.setSelectedIndex(offsetIndex);

        final int selectedLocation = elementSize * offsetIndex;

        final Point top = new Point(0, scrBounds.y);
        final Point bottom = new Point(0, scrBounds.y + scrBounds.height - 20); 
        SwingUtilities.convertPointFromScreen(top, comboBox);
        SwingUtilities.convertPointFromScreen(bottom, comboBox);

        final Rectangle popupBounds = new Rectangle(px, py, pw, ph);

        final int theRest = ph - selectedLocation;


        final boolean extendsOffscreenAtTop = selectedLocation > -top.y;
        final boolean extendsOffscreenAtBottom = theRest > bottom.y;

        if (extendsOffscreenAtTop) {
            popupBounds.y = top.y + 1;
            popupBounds.y = (popupBounds.y / elementSize) * elementSize;
        } else if (extendsOffscreenAtBottom) {
            popupBounds.y = bottom.y - popupBounds.height; 
        } else { 
            popupBounds.y = -selectedLocation;
        }

        final int height = comboBox.getHeight();
        final Insets insets = comboBox.getInsets();
        final int buttonSize = height - (insets.top + insets.bottom);
        final int diff = (buttonSize - elementSize) / 2 + insets.top;
        popupBounds.y += diff - FOCUS_RING_PAD_BOTTOM;

        return popupBounds;
    }
}
