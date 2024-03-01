/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTKeyStroke;
import java.awt.Point;
import java.awt.Toolkit;

import sun.awt.AWTAccessor;
import sun.awt.EmbeddedFrame;
import sun.lwawt.LWWindowPeer;

@SuppressWarnings("serial") 
public class CEmbeddedFrame extends EmbeddedFrame {

    private CPlatformResponder responder;
    private static final Object classLock = new Object();
    private static volatile CEmbeddedFrame globalFocusedWindow;
    private CEmbeddedFrame browserWindowFocusedApplet;
    private boolean parentWindowActive = true;

    public CEmbeddedFrame() {
        show();
    }

    public void addNotify() {
        if (!isDisplayable()) {
            LWCToolkit toolkit = (LWCToolkit)Toolkit.getDefaultToolkit();
            LWWindowPeer peer = toolkit.createEmbeddedFrame(this);
            setPeer(peer);
            responder = new CPlatformResponder(peer, true);
        }
        super.addNotify();
    }

    public void registerAccelerator(AWTKeyStroke stroke) {}

    public void unregisterAccelerator(AWTKeyStroke stroke) {}

    protected long getLayerPtr() {
        return AWTAccessor.getComponentAccessor().<LWWindowPeer>getPeer(this)
                          .getLayerPtr();
    }


    public void handleMouseEvent(int eventType, int modifierFlags, double pluginX,
                                 double pluginY, int buttonNumber, int clickCount) {
        int x = (int)pluginX;
        int y = (int)pluginY;
        Point locationOnScreen = getLocationOnScreen();
        int absX = locationOnScreen.x + x;
        int absY = locationOnScreen.y + y;

        if (eventType == CocoaConstants.NPCocoaEventMouseEntered) {
            CCursorManager.nativeSetAllowsCursorSetInBackground(true);
        } else if (eventType == CocoaConstants.NPCocoaEventMouseExited) {
            CCursorManager.nativeSetAllowsCursorSetInBackground(false);
        }

        responder.handleMouseEvent(eventType, modifierFlags, buttonNumber,
                                   clickCount, x, y, absX, absY);
    }

    public void handleScrollEvent(double pluginX, double pluginY, int modifierFlags,
                                  double deltaX, double deltaY, double deltaZ) {
        int x = (int)pluginX;
        int y = (int)pluginY;
        Point locationOnScreen = getLocationOnScreen();
        int absX = locationOnScreen.x + x;
        int absY = locationOnScreen.y + y;

        responder.handleScrollEvent(x, y, absX, absY, modifierFlags, deltaX,
                                    deltaY, NSEvent.SCROLL_PHASE_UNSUPPORTED);
    }

    public void handleKeyEvent(int eventType, int modifierFlags, String characters,
                               String charsIgnoringMods, boolean isRepeat, short keyCode,
                               boolean needsKeyTyped) {
        responder.handleKeyEvent(eventType, modifierFlags, characters, charsIgnoringMods,
                keyCode, needsKeyTyped, isRepeat);
    }

    public void handleInputEvent(String text) {
        responder.handleInputEvent(text);
    }

    public void handleFocusEvent(boolean focused) {
        synchronized (classLock) {
            globalFocusedWindow = (focused) ? this
                    : ((globalFocusedWindow == this) ? null : globalFocusedWindow);
        }
        if (globalFocusedWindow == this) {
            CClipboard clipboard = (CClipboard) Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.checkPasteboardAndNotify();
        }
        if (parentWindowActive) {
            responder.handleWindowFocusEvent(focused, null);
        }
    }

    /**
     * When the parent window is activated this method is called for all EmbeddedFrames in it.
     *
     * For the CEmbeddedFrame which had focus before the deactivation this method triggers
     * focus events in the following order:
     *  1. WINDOW_ACTIVATED for this EmbeddedFrame
     *  2. WINDOW_GAINED_FOCUS for this EmbeddedFrame
     *  3. FOCUS_GAINED for the most recent focus owner in this EmbeddedFrame
     *
     * The caller must not requestFocus on the EmbeddedFrame together with calling this method.
     *
     * @param parentWindowActive true if the window is activated, false otherwise
     */
    public void handleWindowFocusEvent(boolean parentWindowActive) {
        this.parentWindowActive = parentWindowActive;
        synchronized (classLock) {
            if (!parentWindowActive) {
                this.browserWindowFocusedApplet = globalFocusedWindow;
            }
            if (parentWindowActive && globalFocusedWindow != this && isParentWindowChanged()) {
                globalFocusedWindow = (this.browserWindowFocusedApplet != null) ? this.browserWindowFocusedApplet
                        : this;
            }
        }
        if (globalFocusedWindow == this) {
            responder.handleWindowFocusEvent(parentWindowActive, null);
        }
    }

    public boolean isParentWindowActive() {
        return parentWindowActive;
    }

    private boolean isParentWindowChanged() {
        return globalFocusedWindow != null ? !globalFocusedWindow.isParentWindowActive() : true;
    }

    @Override
    public void synthesizeWindowActivation(boolean doActivate) {
        if (isParentWindowActive() != doActivate) {
            handleWindowFocusEvent(doActivate);
        }
    }

    public static void updateGlobalFocusedWindow(CEmbeddedFrame newGlobalFocusedWindow) {
        synchronized (classLock) {
            if (newGlobalFocusedWindow.isParentWindowActive()) {
                globalFocusedWindow = newGlobalFocusedWindow;
            }
        }
    }
}
