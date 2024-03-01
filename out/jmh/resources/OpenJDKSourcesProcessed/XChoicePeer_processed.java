/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Choice;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.AWTEvent;
import java.awt.Insets;
import java.awt.Font;

import java.awt.peer.ChoicePeer;

import java.awt.event.FocusEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;

import sun.util.logging.PlatformLogger;






public final class XChoicePeer extends XComponentPeer implements ChoicePeer, ToplevelStateListener {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XChoicePeer");

    private static final int MAX_UNFURLED_ITEMS = 10;  
    public static final int TEXT_SPACE = 1;
    public static final int BORDER_WIDTH = 1;
    public static final int ITEM_MARGIN = 1;
    public static final int SCROLLBAR_WIDTH = 15;


    private static final Insets focusInsets = new Insets(0,0,0,0);


    static final int WIDGET_OFFSET = 18;

    static final int            TEXT_XPAD = 8;
    static final int            TEXT_YPAD = 6;

    static final Color focusColor = Color.black;


    private boolean unfurled = false;        

    private boolean dragging = false;        

    private boolean mouseInSB = false;       

    private boolean firstPress = false;      

    private boolean wasDragged = false;
    private ListHelper helper;
    private UnfurledChoice unfurledChoice;


    private boolean drawSelectedItem = true;

    private Component alignUnder;

    private int dragStartIdx = -1;

    private XChoicePeerListener choiceListener;

    XChoicePeer(Choice target) {
        super(target);
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        Choice target = (Choice)this.target;
        int numItems = target.getItemCount();
        unfurledChoice = new UnfurledChoice(target);
        getToplevelXWindow().addToplevelStateListener(this);
        helper = new ListHelper(unfurledChoice,
                                getGUIcolors(),
                                numItems,
                                false,
                                true,
                                false,
                                target.getFont(),
                                MAX_UNFURLED_ITEMS,
                                TEXT_SPACE,
                                ITEM_MARGIN,
                                BORDER_WIDTH,
                                SCROLLBAR_WIDTH);
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        Choice target = (Choice)this.target;
        int numItems = target.getItemCount();

        for (int i = 0; i < numItems; i++) {
            helper.add(target.getItem(i));
        }
        if (!helper.isEmpty()) {
            helper.select(target.getSelectedIndex());
            helper.setFocusedIndex(target.getSelectedIndex());
        }
        helper.updateColors(getGUIcolors());
        updateMotifColors(getPeerBackground());
    }

    public boolean isFocusable() { return true; }

    public void setBounds(int x, int y, int width, int height, int op) {
        int oldX = this.x;
        int oldY = this.y;
        int oldWidth = this.width;
        int oldHeight = this.height;
        super.setBounds(x, y, width, height, op);
        if (unfurled && (oldX != this.x || oldY != this.y || oldWidth != this.width || oldHeight != this.height) ) {
            hidePopdownMenu();
        }
    }

    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        repaint();
    }

    /*
     * Fix for 6246503 : Disabling a choice after selection locks keyboard, mouse and makes the system unusable, Xtoolkit
     * if setEnabled(false) invoked we should close opened choice in
     * order to prevent keyboard/mouse lock.
     */
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        helper.updateColors(getGUIcolors());
        if (!value && unfurled){
            hidePopdownMenu();
        }
    }

    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        repaint();
    }

    void ungrabInputImpl() {
        if (unfurled) {
            unfurled = false;
            dragging = false;
            mouseInSB = false;
            unfurledChoice.setVisible(false);
        }

        super.ungrabInputImpl();
    }

    void handleJavaKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            keyPressed(e);
        }
    }

    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_KP_DOWN: {
              if (helper.getItemCount() > 1) {
                  helper.down();
                  int newIdx = helper.getSelectedIndex();

                  if (((Choice)target).getSelectedIndex() != newIdx) {
                        ((Choice)target).select(newIdx);
                        postEvent(new ItemEvent((Choice)target,
                                                ItemEvent.ITEM_STATE_CHANGED,
                                                ((Choice)target).getItem(newIdx),
                                                ItemEvent.SELECTED));
                        repaint();
                  }
              }
              break;
          }
          case KeyEvent.VK_UP:
          case KeyEvent.VK_KP_UP: {
              if (helper.getItemCount() > 1) {
                  helper.up();
                  int newIdx = helper.getSelectedIndex();

                  if (((Choice)target).getSelectedIndex() != newIdx) {
                        ((Choice)target).select(newIdx);
                        postEvent(new ItemEvent((Choice)target,
                                                ItemEvent.ITEM_STATE_CHANGED,
                                                ((Choice)target).getItem(newIdx),
                                                ItemEvent.SELECTED));
                        repaint();
                  }
              }
              break;
          }
          case KeyEvent.VK_PAGE_DOWN:
              if (unfurled && !dragging) {
                  int oldIdx = helper.getSelectedIndex();
                  helper.pageDown();
                  int newIdx = helper.getSelectedIndex();
                  if (oldIdx != newIdx) {
                      ((Choice)target).select(newIdx);
                      postEvent(new ItemEvent((Choice)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              ((Choice)target).getItem(newIdx),
                                              ItemEvent.SELECTED));
                      repaint();
                  }
              }
              break;
          case KeyEvent.VK_PAGE_UP:
              if (unfurled && !dragging) {
                  int oldIdx = helper.getSelectedIndex();
                  helper.pageUp();
                  int newIdx = helper.getSelectedIndex();
                  if (oldIdx != newIdx) {
                      ((Choice)target).select(newIdx);
                      postEvent(new ItemEvent((Choice)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              ((Choice)target).getItem(newIdx),
                                              ItemEvent.SELECTED));
                      repaint();
                  }
              }
              break;
          case KeyEvent.VK_ESCAPE:
          case KeyEvent.VK_ENTER:
              if (unfurled) {
                  if (dragging){
                      if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
                          helper.select(dragStartIdx);
                      } else { 
                          int newIdx = helper.getSelectedIndex();
                          if (newIdx != (((Choice)target).getSelectedIndex())) {
                            ((Choice)target).select(newIdx);
                            postEvent(new ItemEvent((Choice)target,
                                                    ItemEvent.ITEM_STATE_CHANGED,
                                                    ((Choice)target).getItem(newIdx),
                                                    ItemEvent.SELECTED));
                          }
                      }
                  }
                  hidePopdownMenu();
                  dragging = false;
                  wasDragged = false;
                  mouseInSB = false;

                  if (choiceListener != null){
                      choiceListener.unfurledChoiceClosing();
                  }
              }
              break;
          default:
              if (unfurled) {
                  Toolkit.getDefaultToolkit().beep();
              }
              break;
        }
    }

    public boolean handlesWheelScrolling() { return true; }

    void handleJavaMouseWheelEvent(MouseWheelEvent e) {
        if (unfurled && helper.isVSBVisible()) {
            if (ListHelper.doWheelScroll(helper.getVSB(), null, e)) {
                repaint();
            }
        }
    }

    void handleJavaMouseEvent(MouseEvent e) {
        super.handleJavaMouseEvent(e);
        int i = e.getID();
        switch (i) {
          case MouseEvent.MOUSE_PRESSED:
              mousePressed(e);
              break;
          case MouseEvent.MOUSE_RELEASED:
              mouseReleased(e);
              break;
          case MouseEvent.MOUSE_DRAGGED:
              mouseDragged(e);
              break;
        }
    }

    public void mousePressed(MouseEvent e) {
        /*
         * fix for 5003166: a Choice on XAWT shouldn't react to any
         * mouse button presses except left. This involves presses on
         * Choice but not on opened part of choice.
         */
        if (e.getButton() == MouseEvent.BUTTON1){
            dragStartIdx = helper.getSelectedIndex();
            if (unfurled) {
                if (! (isMouseEventInChoice(e) ||
                       unfurledChoice.isMouseEventInside(e)))
                {
                    hidePopdownMenu();
                }
                unfurledChoice.trackMouse(e);
            }
            else {
                grabInput();
                unfurledChoice.toFront();
                firstPress = true;
                wasDragged = false;
                unfurled = true;
            }
        }
    }

    /*
     * helper method for mouseReleased routine
     */
    void hidePopdownMenu(){
        ungrabInput();
        unfurledChoice.setVisible(false);
        unfurled = false;
    }

    public void mouseReleased(MouseEvent e) {
        if (unfurled) {
            if (mouseInSB) {
                unfurledChoice.trackMouse(e);
            }
            else {

                boolean isMouseEventInside = unfurledChoice.isMouseEventInside( e );
                boolean isMouseInListArea = unfurledChoice.isMouseInListArea( e );

                if (!helper.isEmpty() && !isMouseInListArea && dragging) {
                    ((Choice)target).select(dragStartIdx);
                }

                if ( !firstPress && isMouseInListArea) {
                    hidePopdownMenu();
                }
                if ( !firstPress && !isMouseEventInside) {
                    hidePopdownMenu();
                }
                if ( firstPress && dragging) {
                    hidePopdownMenu();
                }
                /* this could happen when user has opened a Choice and
                 * released mouse button. Then he drags mouse on the
                 * Scrollbar and releases mouse again.
                 */
                if ( !firstPress && !isMouseInListArea &&
                     isMouseEventInside && dragging)
                {
                    hidePopdownMenu();
                }

                if (!helper.isEmpty()) {
                    if (unfurledChoice.isMouseInListArea(e)) {
                        int newIdx = helper.getSelectedIndex();
                        if (newIdx >= 0) {
                            int currentItem = ((Choice)target).getSelectedIndex();
                            if (newIdx != dragStartIdx) {
                                ((Choice)target).select(newIdx);
                            }
                            if (wasDragged && e.getButton() != MouseEvent.BUTTON1){
                                ((Choice)target).select(dragStartIdx);
                            }

                            /*fix for 6239941 : Choice triggers ItemEvent when selecting an item with right mouse button, Xtoolkit
                            * We should generate ItemEvent if only
                            * LeftMouseButton used */
                            if (e.getButton() == MouseEvent.BUTTON1 &&
                                (!firstPress || wasDragged ) &&
                                (newIdx != currentItem))
                            {
                                ((Choice)target).select(newIdx);
                                postEvent(new ItemEvent((Choice)target,
                                                        ItemEvent.ITEM_STATE_CHANGED,
                                                        ((Choice)target).getItem(newIdx),
                                                        ItemEvent.SELECTED));
                            }

                            if (choiceListener != null) {
                                choiceListener.unfurledChoiceClosing();
                            }
                        }
                    }
                }
                unfurledChoice.trackMouse(e);
            }
        }

        dragging = false;
        wasDragged = false;
        firstPress = false;
        dragStartIdx = -1;
    }
    @SuppressWarnings("deprecation")
    public void mouseDragged(MouseEvent e) {
        /*
         * fix for 5003166. On Motif user are unable to drag
         * mouse inside opened Choice if he drags the mouse with
         * different from LEFT mouse button ( e.g. RIGHT or MIDDLE).
         * This fix make impossible to drag mouse inside opened choice
         * with other mouse buttons rather then LEFT one.
         */
        if ( e.getModifiers() == MouseEvent.BUTTON1_MASK ){
            dragging = true;
            wasDragged = true;
            unfurledChoice.trackMouse(e);
        }
    }

    @SuppressWarnings("deprecation")
    public Dimension getMinimumSize() {
        FontMetrics fm = getFontMetrics(target.getFont());
        Choice c = (Choice)target;
        int w = 0;
        for (int i = c.countItems() ; i-- > 0 ;) {
            w = Math.max(fm.stringWidth(c.getItem(i)), w);
        }
        return new Dimension(w + TEXT_XPAD + WIDGET_OFFSET,
                             fm.getMaxAscent() + fm.getMaxDescent() + TEXT_YPAD);
    }

    /*
     * Layout the...
     */
    public void layout() {
        /*
          Dimension size = target.getSize();
          Font f = target.getFont();
          FontMetrics fm = target.getFontMetrics(f);
          String text = ((Choice)target).getLabel();

          textRect.height = fm.getHeight();

          checkBoxSize = getChoiceSize(fm);

          cbX = borderInsets.left + checkBoxInsetFromText;
          cbY = size.height / 2 - checkBoxSize / 2;
          int minTextX = borderInsets.left + 2 * checkBoxInsetFromText + checkBoxSize;
          textRect.width = fm.stringWidth(text == null ? "" : text);
          textRect.x = Math.max(minTextX, size.width / 2 - textRect.width / 2);
          textRect.y = size.height / 2 - textRect.height / 2 + borderInsets.top;

          focusRect.x = focusInsets.left;
          focusRect.y = focusInsets.top;
          focusRect.width = size.width-(focusInsets.left+focusInsets.right)-1;
          focusRect.height = size.height-(focusInsets.top+focusInsets.bottom)-1;

          myCheckMark = AffineTransform.getScaleInstance((double)target.getFont().getSize() / MASTER_SIZE, (double)target.getFont().getSize() / MASTER_SIZE).createTransformedShape(MASTER_CHECKMARK);
        */

    }

    /**
     * Paint the choice
     */
    @Override
    void paintPeer(final Graphics g) {
        flush();
        Dimension size = getPeerSize();
        g.setColor(getPeerBackground());
        g.fillRect(0, 0, width, height);

        drawMotif3DRect(g, 1, 1, width-2, height-2, false);
        drawMotif3DRect(g, width - WIDGET_OFFSET, (height / 2) - 3, 12, 6, false);

        if (!helper.isEmpty() && helper.getSelectedIndex() != -1) {
            g.setFont(getPeerFont());
            FontMetrics fm = g.getFontMetrics();
            String lbl = helper.getItem(helper.getSelectedIndex());
            if (lbl != null && drawSelectedItem) {
                g.setClip(1, 1, width - WIDGET_OFFSET - 2, height);
                if (isEnabled()) {
                    g.setColor(getPeerForeground());
                    g.drawString(lbl, 5, (height + fm.getMaxAscent()-fm.getMaxDescent())/2);
                }
                else {
                    g.setColor(getPeerBackground().brighter());
                    g.drawString(lbl, 5, (height + fm.getMaxAscent()-fm.getMaxDescent())/2);
                    g.setColor(getPeerBackground().darker());
                    g.drawString(lbl, 4, ((height + fm.getMaxAscent()-fm.getMaxDescent())/2)-1);
                }
                g.setClip(0, 0, width, height);
            }
        }
        if (hasFocus()) {
            paintFocus(g,focusInsets.left,focusInsets.top,size.width-(focusInsets.left+focusInsets.right)-1,size.height-(focusInsets.top+focusInsets.bottom)-1);
        }
        if (unfurled) {
            unfurledChoice.repaint();
        }
        flush();
    }

    protected void paintFocus(Graphics g,
                              int x, int y, int w, int h) {
        g.setColor(focusColor);
        g.drawRect(x,y,w,h);
    }



    /*
     * ChoicePeer methods stolen from TinyChoicePeer
     */

    public void select(int index) {
        helper.select(index);
        helper.setFocusedIndex(index);
        repaint();
    }

    public void add(String item, int index) {
        helper.add(item, index);
        repaint();
    }

    public void remove(int index) {
        boolean selected = (index == helper.getSelectedIndex());
        boolean visibled = (index >= helper.firstDisplayedIndex() && index <= helper.lastDisplayedIndex());
        helper.remove(index);
        if (selected) {
            if (helper.isEmpty()) {
                helper.select(-1);
            }
            else {
                helper.select(0);
            }
        }
        /*
         * Fix for 6248016
         * After removing the item of the choice we need to reshape unfurled choice
         * in order to keep actual bounds of the choice
         */

        /*
         * condition added only for performance
         */
        if (!unfurled) {
            if (helper.isEmpty()) {
                repaint();
            }
            return;
        }

        /*
         * condition added only for performance
         * the count of the visible items changed
         */
        if (visibled){
            Rectangle r = unfurledChoice.placeOnScreen();
            unfurledChoice.reshape(r.x, r.y, r.width, r.height);
            return;
        }

        /*
         * condition added only for performance
         * the structure of visible items changed
         * if removable item is non visible and non selected then there is no repaint
         */
        if (visibled || selected){
            repaint();
        }
    }

    public void removeAll() {
        helper.removeAll();
        helper.select(-1);
        /*
         * Fix for 6248016
         * After removing the item of the choice we need to reshape unfurled choice
         * in order to keep actual bounds of the choice
         */
        Rectangle r = unfurledChoice.placeOnScreen();
        unfurledChoice.reshape(r.x, r.y, r.width, r.height);
        repaint();
    }

    public void setFont(Font font) {
        super.setFont(font);
        helper.setFont(this.font);
    }

    public void setForeground(Color c) {
        super.setForeground(c);
        helper.updateColors(getGUIcolors());
    }

    public void setBackground(Color c) {
        super.setBackground(c);
        unfurledChoice.setBackground(c);
        helper.updateColors(getGUIcolors());
        updateMotifColors(c);
    }

    public void setDrawSelectedItem(boolean value) {
        drawSelectedItem = value;
    }

    public void setAlignUnder(Component comp) {
        alignUnder = comp;
    }

    public void addXChoicePeerListener(XChoicePeerListener l){
        choiceListener = l;
    }

    public void removeXChoicePeerListener(){
        choiceListener = null;
    }

    public boolean isUnfurled(){
        return unfurled;
    }

    /* fix for 6261352. We should detect if current parent Window (containing a Choice) become iconified and hide pop-down menu with grab release.
     * In this case we should hide pop-down menu.
     */
    public void stateChangedICCCM(int oldState, int newState) {
        if (unfurled && oldState != newState){
                hidePopdownMenu();
        }
    }

    public void stateChangedJava(int oldState, int newState) {
        if (unfurled && oldState != newState){
            hidePopdownMenu();
        }
    }

    @Override
    protected void initGraphicsConfiguration() {
        super.initGraphicsConfiguration();
        if (unfurledChoice != null) {
            unfurledChoice.initGraphicsConfiguration();
            unfurledChoice.doValidateSurface();
        }
    }

    /**************************************************************************/
    /* Common functionality between List & Choice
       /**************************************************************************/

    /**
     * Inner class for the unfurled Choice list
     * Much, much more docs
     */
    final class UnfurledChoice extends XWindow /*implements XScrollbarClient*/ {


        public UnfurledChoice(Component target) {
            super(target);
        }

        public void preInit(XCreateWindowParams params) {
            params.delete(PARENT_WINDOW);
            super.preInit(params);
            params.remove(BOUNDS);
            params.add(OVERRIDE_REDIRECT, Boolean.TRUE);
        }

        Rectangle placeOnScreen() {
            int numItemsDisplayed;
            if (helper.isEmpty()) {
                numItemsDisplayed = 1;
            }
            else {
                int numItems = helper.getItemCount();
                numItemsDisplayed = Math.min(MAX_UNFURLED_ITEMS, numItems);
            }
            Point global = XChoicePeer.this.toGlobal(0,0);
            Rectangle screenBounds = graphicsConfig.getBounds();

            if (alignUnder != null) {
                Rectangle choiceRec = XChoicePeer.this.getBounds();
                choiceRec.setLocation(0, 0);
                choiceRec = XChoicePeer.this.toGlobal(choiceRec);
                Rectangle alignUnderRec = new Rectangle(alignUnder.getLocationOnScreen(), alignUnder.getSize()); 
                Rectangle result = choiceRec.union(alignUnderRec);
                width = result.width;
                x = result.x;
                y = result.y + result.height;
                height = 2*BORDER_WIDTH +
                    numItemsDisplayed*(helper.getItemHeight()+2*ITEM_MARGIN);
            } else {
                x = global.x;
                y = global.y + XChoicePeer.this.height;
                width = Math.max(XChoicePeer.this.width,
                                 helper.getMaxItemWidth() + 2 * (BORDER_WIDTH + ITEM_MARGIN + TEXT_SPACE) + (helper.isVSBVisible() ? SCROLLBAR_WIDTH : 0));
                height = 2*BORDER_WIDTH +
                    numItemsDisplayed*(helper.getItemHeight()+2*ITEM_MARGIN);
            }
            if (x < screenBounds.x) {
                x = screenBounds.x;
            }
            else if (x + width > screenBounds.x + screenBounds.width) {
                x = screenBounds.x + screenBounds.width - width;
            }

            if (y + height > screenBounds.y + screenBounds.height) {
                y = global.y - height;
            }
            if (y < screenBounds.y) {
                y = screenBounds.y;
            }
            return new Rectangle(x, y, width, height);
        }

        public void toFront() {
            if (choiceListener != null)
                choiceListener.unfurledChoiceOpening(helper);

            Rectangle r = placeOnScreen();
            reshape(r.x, r.y, r.width, r.height);
            super.toFront();
            setVisible(true);
        }

        /*
         * Track a MouseEvent (either a drag or a press) and paint a new
         * selected item, if necessary.
         */
        public void trackMouse(MouseEvent e) {
            Point local = toLocalCoords(e);


            switch (e.getID()) {
              case MouseEvent.MOUSE_PRESSED:
                  if (helper.isInVertSB(getBounds(), local.x, local.y)) {
                      mouseInSB = true;
                      helper.handleVSBEvent(e, getBounds(), local.x, local.y);
                  }
                  else {
                      trackSelection(local.x, local.y);
                  }
                  break;
              case MouseEvent.MOUSE_RELEASED:
                  if (mouseInSB) {
                      mouseInSB = false;
                      helper.handleVSBEvent(e, getBounds(), local.x, local.y);
                  }else{
                      helper.trackMouseReleasedScroll();
                  }
                  /*
                    else {
                    trackSelection(local.x, local.y);
                    }
                  */
                  break;
              case MouseEvent.MOUSE_DRAGGED:
                  if (mouseInSB) {
                      helper.handleVSBEvent(e, getBounds(), local.x, local.y);
                  }
                  else {
                      helper.trackMouseDraggedScroll(local.x, local.y, width, height);
                      trackSelection(local.x, local.y);
                  }
                  break;
            }
        }

        private void trackSelection(int transX, int transY) {
            if (!helper.isEmpty()) {
                if (transX > 0 && transX < width &&
                    transY > 0 && transY < height) {
                    int newIdx = helper.y2index(transY);
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("transX=" + transX + ", transY=" + transY
                                 + ",width=" + width + ", height=" + height
                                 + ", newIdx=" + newIdx + " on " + target);
                    }
                    if ((newIdx >=0) && (newIdx < helper.getItemCount())
                        && (newIdx != helper.getSelectedIndex()))
                    {
                        helper.select(newIdx);
                        unfurledChoice.repaint();
                    }
                }
            }
        }

        /*
         * fillRect with current Background color on the whole dropdown list.
         */
        public void paintBackground() {
            final Graphics g = getGraphics();
            if (g != null) {
                try {
                    g.setColor(getPeerBackground());
                    g.fillRect(0, 0, width, height);
                } finally {
                    g.dispose();
                }
            }
        }
        /*
         * 6405689. In some cases we should erase background to eliminate painting
         * artefacts.
         */
        @Override
        public void repaint() {
            if (!isVisible()) {
                return;
            }
            if (helper.checkVsbVisibilityChangedAndReset()){
                paintBackground();
            }
            super.repaint();
        }
        @Override
        public void paintPeer(Graphics g) {
            Choice choice = (Choice)target;
            Color[] colors = XChoicePeer.this.getGUIcolors();
            draw3DRect(g, getSystemColors(), 0, 0, width - 1, height - 1, true);
            draw3DRect(g, getSystemColors(), 1, 1, width - 3, height - 3, true);

            helper.paintAllItems(g,
                                 colors,
                                 getBounds());
        }

        public void setVisible(boolean vis) {
            xSetVisible(vis);

            if (!vis && alignUnder != null) {
                alignUnder.requestFocusInWindow();
            }
        }

        /**
         * Return a MouseEvent's Point in coordinates relative to the
         * UnfurledChoice.
         */
        private Point toLocalCoords(MouseEvent e) {
            Point global = e.getLocationOnScreen();

            global.x -= x;
            global.y -= y;
            return global;
        }

        /* Returns true if the MouseEvent coords (which are based on the Choice)
         * are inside of the UnfurledChoice.
         */
        private boolean isMouseEventInside(MouseEvent e) {
            Point local = toLocalCoords(e);
            if (local.x > 0 && local.x < width &&
                local.y > 0 && local.y < height) {
                return true;
            }
            return false;
        }

        /**
         * Tests if the mouse cursor is in the Unfurled Choice, yet not
         * in the vertical scrollbar
         */
        private boolean isMouseInListArea(MouseEvent e) {
            if (isMouseEventInside(e)) {
                Point local = toLocalCoords(e);
                Rectangle bounds = getBounds();
                if (!helper.isInVertSB(bounds, local.x, local.y)) {
                    return true;
                }
            }
            return false;
        }

        /*
         * Overridden from XWindow() because we don't want to send
         * ComponentEvents
         */
        public void handleConfigureNotifyEvent(XEvent xev) {}
        public void handleMapNotifyEvent(XEvent xev) {}
        public void handleUnmapNotifyEvent(XEvent xev) {}
    } 

    public void dispose() {
        if (unfurledChoice != null) {
            unfurledChoice.destroy();
        }
        super.dispose();
    }

    /*
     * fix for 6239938 : Choice drop-down does not disappear when it loses
     * focus, on XToolkit
     * We are able to handle all _Key_ events received by Choice when
     * it is in opened state without sending it to EventQueue.
     * If Choice is in closed state we should behave like before: send
     * all events to EventQueue.
     * To be compatible with Motif we should handle all KeyEvents in
     * Choice if it is opened. KeyEvents should be sent into Java if Choice is not opened.
     */
    boolean prePostEvent(final AWTEvent e) {
        if (unfurled){
            if (e instanceof MouseWheelEvent){
                return super.prePostEvent(e);
            }
            if (e instanceof KeyEvent){
                InvocationEvent ev = new InvocationEvent(target, new Runnable() {
                    public void run() {
                        if(target.isFocusable() &&
                                getParentTopLevel().isFocusableWindow() )
                        {
                            handleJavaKeyEvent((KeyEvent)e);
                        }
                    }
                });
                postEvent(ev);

                return true;
            } else {
                if (e instanceof MouseEvent){
                    MouseEvent me = (MouseEvent)e;
                    int eventId = e.getID();
                    if ((unfurledChoice.isMouseEventInside(me) ||
                         (!firstPress && eventId == MouseEvent.MOUSE_DRAGGED)))
                    {
                        return handleMouseEventByChoice(me);
                    }
                    if (eventId == MouseEvent.MOUSE_MOVED){
                        return handleMouseEventByChoice(me);
                    }
                    if (  !firstPress && !( isMouseEventInChoice(me) ||
                             unfurledChoice.isMouseEventInside(me)) &&
                             ( eventId == MouseEvent.MOUSE_PRESSED ||
                               eventId == MouseEvent.MOUSE_RELEASED ||
                               eventId == MouseEvent.MOUSE_CLICKED )
                          )
                    {
                        return handleMouseEventByChoice(me);
                    }
                }
            }
        }
        return super.prePostEvent(e);
    }

    public boolean handleMouseEventByChoice(final MouseEvent me){
        InvocationEvent ev = new InvocationEvent(target, new Runnable() {
            public void run() {
                handleJavaMouseEvent(me);
            }
        });
        postEvent(ev);

        return true;
    }

    /* Returns true if the MouseEvent coords
     * are inside of the Choice itself (it doesn't depend on
     * whether this choice opened or not).
     */
    private boolean isMouseEventInChoice(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Rectangle choiceRect = getBounds();

        if (x < 0 || x > choiceRect.width ||
            y < 0 || y > choiceRect.height)
        {
            return false;
        }
        return true;
    }
}
