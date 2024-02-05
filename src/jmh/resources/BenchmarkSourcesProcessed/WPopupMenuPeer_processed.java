/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.awt.*;
import java.awt.peer.*;

import sun.awt.AWTAccessor;

final class WPopupMenuPeer extends WMenuPeer implements PopupMenuPeer {

    WPopupMenuPeer(PopupMenu target) {
        this.target = target;
        MenuContainer parent = null;

        boolean isTrayIconPopup = AWTAccessor.getPopupMenuAccessor().isTrayIconPopup(target);
        if (isTrayIconPopup) {
            parent = AWTAccessor.getMenuComponentAccessor().getParent(target);
        } else {
            parent = target.getParent();
        }

        if (parent instanceof Component) {
            WComponentPeer parentPeer = (WComponentPeer) WToolkit.targetToPeer(parent);
            if (parentPeer == null) {
                parent = WToolkit.getNativeContainer((Component)parent);
                parentPeer = (WComponentPeer) WToolkit.targetToPeer(parent);
            }
            parentPeer.addChildPeer(this);
            createMenu(parentPeer);
            checkMenuCreation();
        } else {
            throw new IllegalArgumentException(
                "illegal popup menu container class");
        }
    }

    private native void createMenu(WComponentPeer parent);

    @SuppressWarnings("deprecation")
    public void show(Event e) {
        Component origin = (Component)e.target;
        WComponentPeer peer = (WComponentPeer) WToolkit.targetToPeer(origin);
        if (peer == null) {
            Component nativeOrigin = WToolkit.getNativeContainer(origin);
            e.target = nativeOrigin;

            for (Component c = origin; c != nativeOrigin; c = c.getParent()) {
                Point p = c.getLocation();
                e.x += p.x;
                e.y += p.y;
            }
        }
        _show(e);
    }

    /*
     * This overloaded method is for TrayIcon.
     * Its popup has special parent.
     */
    void show(Component origin, Point p) {
        WComponentPeer peer = (WComponentPeer) WToolkit.targetToPeer(origin);
        @SuppressWarnings("deprecation")
        Event e = new Event(origin, 0, Event.MOUSE_DOWN, p.x, p.y, 0, 0);
        if (peer == null) {
            Component nativeOrigin = WToolkit.getNativeContainer(origin);
            e.target = nativeOrigin;
        }
        e.x = p.x;
        e.y = p.y;
        _show(e);
    }

    @SuppressWarnings("deprecation")
    private native void _show(Event e);
}
