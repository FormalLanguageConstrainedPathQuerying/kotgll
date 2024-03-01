/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial") 
class AquaComboBoxRendererInternal<E> extends JLabel implements ListCellRenderer<E> {
    final JComboBox<?> fComboBox;
    boolean fSelected;
    boolean fChecked;
    boolean fInList;
    boolean fEditable;
    boolean fDrawCheckedItem = true;

    public AquaComboBoxRendererInternal(final JComboBox<?> comboBox) {
        super();
        fComboBox = comboBox;
    }

    public Dimension getPreferredSize() {
        final Dimension size;

        final String text = getText();
        if (text == null || text.isEmpty()) {
            setText(" ");
            size = super.getPreferredSize();
            setText("");
        } else {
            size = super.getPreferredSize();
        }
        return size;
    }

    protected void paintBorder(final Graphics g) {

    }

    public int getBaseline(int width, int height) {
        return super.getBaseline(width, height) - 1;
    }

    public Component getListCellRendererComponent(final JList<? extends E> list,
                                                  final E value, int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        fInList = (index >= 0); 
        fSelected = isSelected;
        if (index < 0) {
            index = fComboBox.getSelectedIndex();
        }


        if (index >= 0) {
            final Object item = fComboBox.getItemAt(index);
            fChecked = fInList && item != null && item.equals(fComboBox.getSelectedItem());
        } else {
            fChecked = false;
        }

        fEditable = fComboBox.isEditable();
        if (isSelected) {
            if (fEditable) {
                setBackground(UIManager.getColor("List.selectionBackground"));
                setForeground(UIManager.getColor("List.selectionForeground"));
            } else {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
        } else {
            if (fEditable) {
                setBackground(UIManager.getColor("List.background"));
                setForeground(UIManager.getColor("List.foreground"));
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }

        setFont(list.getFont());

        if (value instanceof Icon) {
            setIcon((Icon)value);
        } else {
            setText((value == null) ? " " : value.toString());
        }
        return this;
    }

    public Insets getInsets(Insets insets) {
        if (insets == null) insets = new Insets(0, 0, 0, 0);
        insets.top = 1;
        insets.bottom = 1;
        insets.right = 5;
        insets.left = (fInList && !fEditable ? 16 + 7 : 5);
        return insets;
    }

    protected void setDrawCheckedItem(final boolean drawCheckedItem) {
        this.fDrawCheckedItem = drawCheckedItem;
    }

    protected void paintComponent(final Graphics g) {
        if (fInList) {
            if (fSelected && !fEditable) {
                AquaMenuPainter.instance().paintSelectedMenuItemBackground(g, getWidth(), getHeight());
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            if (fChecked && !fEditable && fDrawCheckedItem) {
                final int y = getHeight() - 4;
                g.setColor(getForeground());
                SwingUtilities2.drawString(fComboBox, g, "\u2713", 6, y);
            }
        }
        super.paintComponent(g);
    }
}
