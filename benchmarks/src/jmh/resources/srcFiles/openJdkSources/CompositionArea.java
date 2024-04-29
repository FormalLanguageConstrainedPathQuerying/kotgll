/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.im;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputMethodRequests;
import java.io.Serial;
import java.text.AttributedCharacterIterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * A composition area is used to display text that's being composed
 * using an input method in its own user interface environment,
 * typically in a root window.
 *
 * @author JavaSoft International
 */

public final class CompositionArea extends JPanel implements InputMethodListener {

    @SuppressWarnings("serial") 
    private CompositionAreaHandler handler;

    @SuppressWarnings("serial") 
    private TextLayout composedTextLayout;
    @SuppressWarnings("serial") 
    private TextHitInfo caret = null;
    private JFrame compositionWindow;
    private static final int TEXT_ORIGIN_X = 5;
    private static final int TEXT_ORIGIN_Y = 15;
    private static final int PASSIVE_WIDTH = 480;
    private static final int WIDTH_MARGIN=10;
    private static final int HEIGHT_MARGIN=3;

    CompositionArea() {
        String windowTitle = Toolkit.getProperty("AWT.CompositionWindowTitle", "Input Window");
        compositionWindow =
            (JFrame)InputMethodContext.createInputMethodWindow(windowTitle, null, true);

        setOpaque(true);
        setBorder(LineBorder.createGrayLineBorder());
        setForeground(Color.black);
        setBackground(Color.white);

        enableInputMethods(true);
        enableEvents(AWTEvent.KEY_EVENT_MASK);

        compositionWindow.getContentPane().add(this);
        compositionWindow.addWindowListener(new FrameWindowAdapter());
        addInputMethodListener(this);
        compositionWindow.enableInputMethods(false);
        compositionWindow.pack();
        Dimension windowSize = compositionWindow.getSize();
        Dimension screenSize = (getToolkit()).getScreenSize();
        compositionWindow.setLocation(screenSize.width - windowSize.width-20,
                                    screenSize.height - windowSize.height-100);
        compositionWindow.setVisible(false);
    }

    /**
     * Sets the composition area handler that currently owns this
     * composition area, and its input context.
     */
    synchronized void setHandlerInfo(CompositionAreaHandler handler, InputContext inputContext) {
        this.handler = handler;
        ((InputMethodWindow) compositionWindow).setInputContext(inputContext);
    }

    /**
     * @see java.awt.Component#getInputMethodRequests
     */
    public InputMethodRequests getInputMethodRequests() {
        return handler;
    }

    private Rectangle getCaretRectangle(TextHitInfo caret) {
        int caretLocation = 0;
        TextLayout layout = composedTextLayout;
        if (layout != null) {
            caretLocation = Math.round(layout.getCaretInfo(caret)[0]);
        }
        Graphics g = getGraphics();
        FontMetrics metrics = null;
        try {
            metrics = g.getFontMetrics();
        } finally {
            g.dispose();
        }
        return new Rectangle(TEXT_ORIGIN_X + caretLocation,
                             TEXT_ORIGIN_Y - metrics.getAscent(),
                             0, metrics.getAscent() + metrics.getDescent());
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(getForeground());
        TextLayout layout = composedTextLayout;
        if (layout != null) {
            layout.draw((Graphics2D) g, TEXT_ORIGIN_X, TEXT_ORIGIN_Y);
        }
        if (caret != null) {
            Rectangle rectangle = getCaretRectangle(caret);
            g.setXORMode(getBackground());
            g.fillRect(rectangle.x, rectangle.y, 1, rectangle.height);
            g.setPaintMode();
        }
    }

    void setCompositionAreaVisible(boolean visible) {
        compositionWindow.setVisible(visible);
    }

    boolean isCompositionAreaVisible() {
        return compositionWindow.isVisible();
    }

    class FrameWindowAdapter extends WindowAdapter {
        public void windowActivated(WindowEvent e) {
            requestFocus();
        }
    }

    public void inputMethodTextChanged(InputMethodEvent event) {
        handler.inputMethodTextChanged(event);
    }

    public void caretPositionChanged(InputMethodEvent event) {
        handler.caretPositionChanged(event);
    }

    /**
     * Sets the text and caret to be displayed in this composition area.
     * Shows the window if it contains text, hides it if not.
     */
    void setText(AttributedCharacterIterator composedText, TextHitInfo caret) {
        composedTextLayout = null;
        if (composedText == null) {
            compositionWindow.setVisible(false);
            this.caret = null;
        } else {
            /* since we have composed text, make sure the window is shown.
               This is necessary to get a valid graphics object. See 6181385.
            */
            if (!compositionWindow.isVisible()) {
                compositionWindow.setVisible(true);
            }

            Graphics g = getGraphics();

            if (g == null) {
                return;
            }

            try {
                updateWindowLocation();

                FontRenderContext context = ((Graphics2D)g).getFontRenderContext();
                composedTextLayout = new TextLayout(composedText, context);
                Rectangle2D bounds = composedTextLayout.getBounds();

                this.caret = caret;

                FontMetrics metrics = g.getFontMetrics();
                Rectangle2D maxCharBoundsRec = metrics.getMaxCharBounds(g);
                int newHeight = (int)maxCharBoundsRec.getHeight() + HEIGHT_MARGIN;
                int newFrameHeight = newHeight +compositionWindow.getInsets().top
                                               +compositionWindow.getInsets().bottom;
                InputMethodRequests req = handler.getClientInputMethodRequests();
                int newWidth = (req==null) ? PASSIVE_WIDTH : (int)bounds.getWidth() + WIDTH_MARGIN;
                int newFrameWidth = newWidth + compositionWindow.getInsets().left
                                             + compositionWindow.getInsets().right;
                setPreferredSize(new Dimension(newWidth, newHeight));
                compositionWindow.setSize(new Dimension(newFrameWidth, newFrameHeight));

                paint(g);
            }
            finally {
                g.dispose();
            }
        }
    }

    /**
     * Sets the caret to be displayed in this composition area.
     * The text is not changed.
     */
    void setCaret(TextHitInfo caret) {
        this.caret = caret;
        if (compositionWindow.isVisible()) {
            Graphics g = getGraphics();
            try {
                paint(g);
            } finally {
                g.dispose();
            }
        }
    }

    /**
     * Positions the composition window near (usually below) the
     * insertion point in the client component if the client
     * component is an active client (below-the-spot input).
     */
    void updateWindowLocation() {
        InputMethodRequests req = handler.getClientInputMethodRequests();
        if (req == null) {
            return;
        }

        Point windowLocation = new Point();

        Rectangle caretRect = req.getTextLocation(null);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = compositionWindow.getSize();
        final int SPACING = 2;

        if (caretRect.x + windowSize.width > screenSize.width) {
            windowLocation.x = screenSize.width - windowSize.width;
        } else {
            windowLocation.x = caretRect.x;
        }

        if (caretRect.y + caretRect.height + SPACING + windowSize.height > screenSize.height) {
            windowLocation.y = caretRect.y - SPACING - windowSize.height;
        } else {
            windowLocation.y = caretRect.y + caretRect.height + SPACING;
        }

        compositionWindow.setLocation(windowLocation);
    }

    Rectangle getTextLocation(TextHitInfo offset) {
        Rectangle rectangle = getCaretRectangle(offset);
        Point location = getLocationOnScreen();
        rectangle.translate(location.x, location.y);
        return rectangle;
    }

   TextHitInfo getLocationOffset(int x, int y) {
        TextLayout layout = composedTextLayout;
        if (layout == null) {
            return null;
        } else {
            Point location = getLocationOnScreen();
            x -= location.x + TEXT_ORIGIN_X;
            y -= location.y + TEXT_ORIGIN_Y;
            if (layout.getBounds().contains(x, y)) {
                return layout.hitTestChar(x, y);
            } else {
                return null;
            }
        }
    }

    void setCompositionAreaUndecorated(boolean setUndecorated){
          if (compositionWindow.isDisplayable()){
              compositionWindow.removeNotify();
          }
          compositionWindow.setUndecorated(setUndecorated);
          compositionWindow.pack();
    }

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1057247068746557444L;
}
