/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtilControlSize.*;

public abstract class AquaBorder implements Border, UIResource {
    protected final AquaPainter<? extends JRSUIState> painter;
    protected final SizeDescriptor sizeDescriptor;
    protected SizeVariant sizeVariant;

    protected AquaBorder(final SizeDescriptor sizeDescriptor) {
        this.sizeDescriptor = sizeDescriptor;
        this.sizeVariant = sizeDescriptor.get(Size.REGULAR);
        this.painter = createPainter();
    }

    protected AquaPainter<? extends JRSUIState> createPainter() {
        final AquaPainter<JRSUIState> painter = AquaPainter.create(JRSUIState.getInstance());
        painter.state.set(AlignmentVertical.CENTER);
        painter.state.set(AlignmentHorizontal.CENTER);
        return painter;
    }

    protected AquaBorder(final AquaBorder other) {
        this.sizeDescriptor = other.sizeDescriptor;
        this.sizeVariant = other.sizeVariant;
        this.painter = AquaPainter.create(other.painter.state.derive());
        painter.state.set(AlignmentVertical.CENTER);
        painter.state.set(AlignmentHorizontal.CENTER);
    }

    protected void setSize(final Size size) {
        sizeVariant = sizeDescriptor.get(size);
        painter.state.set(size);
    }

    @Override
    public Insets getBorderInsets(final Component c) {
        return (Insets) sizeVariant.margins.clone();
    }

    protected AquaBorder deriveBorderForSize(final Size size) {
        try {
            final Class<? extends AquaBorder> clazz = getClass();
            final AquaBorder border = clazz.getConstructor(new Class<?>[] { clazz }).newInstance(new Object[] { this });
            border.setSize(size);
            return border;
        } catch (final Throwable e) {
            return null;
        }
    }

    public static void repaintBorder(final JComponent c) {
        JComponent borderedComponent = c;
        Border border = c.getBorder();
        if (border == null) {
            final Container p = c.getParent();
            if (p instanceof JViewport) {
                borderedComponent = (JComponent)p.getParent();
                if (borderedComponent != null) border = borderedComponent.getBorder();
            }
        }

        if (border == null || borderedComponent == null) return;

        final int width = borderedComponent.getWidth();
        final int height = borderedComponent.getHeight();
        final Insets i = borderedComponent.getInsets();

        borderedComponent.repaint(0, 0, width, i.top); 
        borderedComponent.repaint(0, 0, i.left, height); 
        borderedComponent.repaint(0, height - i.bottom, width, i.bottom); 
        borderedComponent.repaint(width - i.right, 0, i.right, height); 
    }

    protected boolean isFocused(final Component c) {
        Component focusable = c;

        if (c instanceof JScrollPane) {
            final JViewport vp = ((JScrollPane)c).getViewport();
            if (vp != null) {
                focusable = vp.getView();
                if (focusable instanceof JTextComponent) return false;
            }
        } else if (focusable instanceof JTextComponent) {
            if (!((javax.swing.text.JTextComponent)focusable).isEditable()) return false;
        }

        return (focusable instanceof JComponent jComponent) && jComponent.hasFocus();
    }

    @Override
    public boolean isBorderOpaque() { return false; }

    @Override
    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
        painter.paint(g, c, x, y, w, h);
    }

    static class Default extends AquaBorder {
        Default() { super(new SizeDescriptor(new SizeVariant())); }
    }
}
