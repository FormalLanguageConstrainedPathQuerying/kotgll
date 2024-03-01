/*
 * Copyright (c) 2002, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.awt.peer.ComponentPeer;
import java.awt.peer.WindowPeer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.DisplayChangedListener;
import sun.awt.IconInfo;
import sun.awt.SunToolkit;
import sun.awt.X11GraphicsDevice;
import sun.awt.X11GraphicsEnvironment;
import sun.java2d.pipe.Region;
import sun.util.logging.PlatformLogger;

import static java.nio.charset.StandardCharsets.UTF_8;

class XWindowPeer extends XPanelPeer implements WindowPeer,
                                                DisplayChangedListener {

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XWindowPeer");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.XWindowPeer");
    private static final PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XWindowPeer");
    private static final PlatformLogger grabLog = PlatformLogger.getLogger("sun.awt.X11.grab.XWindowPeer");
    private static final PlatformLogger iconLog = PlatformLogger.getLogger("sun.awt.X11.icon.XWindowPeer");

    private static Set<XWindowPeer> windows = new HashSet<XWindowPeer>();


    private boolean cachedFocusableWindow;
    XWarningWindow warningWindow;

    private boolean alwaysOnTop;
    private boolean locationByPlatform;

    Dialog modalBlocker;
    boolean delayedModalBlocking = false;
    Dimension targetMinimumSize = null;

    private XWindowPeer ownerPeer;

    protected XWindowPeer prevTransientFor, nextTransientFor;
    private XBaseWindow curRealTransientFor;

    private boolean grab = false; 

    private boolean isMapped = false; 
    private boolean mustControlStackPosition = false; 
    private XEventDispatcher rootPropertyEventDispatcher = null;

    private static final AtomicBoolean isStartupNotificationRemoved = new AtomicBoolean();

    /*
     * Focus related flags
     */
    private boolean isUnhiding = false;             
    private boolean isBeforeFirstMapNotify = false; 

    /**
     * The type of the window.
     *
     * The type is supposed to be immutable while the peer object exists.
     * The value gets initialized in the preInit() method.
     */
    private Window.Type windowType = Window.Type.NORMAL;

    public final Window.Type getWindowType() {
        return windowType;
    }

    protected Vector <ToplevelStateListener> toplevelStateListeners = new Vector<ToplevelStateListener>();
    XWindowPeer(XCreateWindowParams params) {
        super(params.putIfNull(PARENT_WINDOW, Long.valueOf(0)));
    }

    XWindowPeer(Window target) {
        super(new XCreateWindowParams(new Object[] {
            TARGET, target,
            PARENT_WINDOW, Long.valueOf(0)}));
    }

    /*
     * This constant defines icon size recommended for using.
     * Apparently, we should use XGetIconSizes which should
     * return icon sizes would be most appreciated by the WM.
     * However, XGetIconSizes always returns 0 for some reason.
     * So the constant has been introduced.
     */
    private static final int PREFERRED_SIZE_FOR_ICON = 128;

    /*
     * Sometimes XChangeProperty(_NET_WM_ICON) doesn't work if
     * image buffer is too large. This constant holds maximum
     * length of buffer which can be used with _NET_WM_ICON hint.
     * It holds int's value.
     */
    private static final int MAXIMUM_BUFFER_LENGTH_NET_WM_ICON = (2<<15) - 1;

    void preInit(XCreateWindowParams params) {
        target = (Component)params.get(TARGET);
        windowType = ((Window)target).getType();
        params.put(REPARENTED,
                   Boolean.valueOf(isOverrideRedirect() || isSimpleWindow()));
        super.preInit(params);
        params.putIfNull(BIT_GRAVITY, Integer.valueOf(XConstants.NorthWestGravity));

        long eventMask = 0;
        if (params.containsKey(EVENT_MASK)) {
            eventMask = ((Long)params.get(EVENT_MASK));
        }
        eventMask |= XConstants.VisibilityChangeMask;
        params.put(EVENT_MASK, eventMask);

        XA_NET_WM_STATE = XAtom.get("_NET_WM_STATE");


        params.put(OVERRIDE_REDIRECT, Boolean.valueOf(isOverrideRedirect()));

        SunToolkit.awtLock();
        try {
            windows.add(this);
        } finally {
            SunToolkit.awtUnlock();
        }

        cachedFocusableWindow = isFocusableWindow();

        if (!target.isFontSet()) {
               target.setFont(XWindow.getDefaultFont());
        }
        if (!target.isBackgroundSet()) {
               target.setBackground(SystemColor.window);

        }
        if (!target.isForegroundSet()) {
               target.setForeground(SystemColor.windowText);
        }


        alwaysOnTop = ((Window)target).isAlwaysOnTop() && ((Window)target).isAlwaysOnTopSupported();

        GraphicsConfiguration gc = getGraphicsConfiguration();
        ((X11GraphicsDevice)gc.getDevice()).addDisplayChangedListener(this);
    }

    protected String getWMName() {
        String name = target.getName();
        if (name == null || name.trim().isEmpty()) {
            name = " ";
        }
        return name;
    }

    private static native String getLocalHostname();
    private static native int getJvmPID();

    @SuppressWarnings("deprecation")
    void postInit(XCreateWindowParams params) {
        super.postInit(params);

        initWMProtocols();

        XAtom.get("WM_CLIENT_MACHINE").setProperty(getWindow(), getLocalHostname());
        XAtom.get("_NET_WM_PID").setCard32Property(getWindow(), getJvmPID());

        Window t_window = (Window)target;
        Window owner = t_window.getOwner();
        if (owner != null) {
            ownerPeer = AWTAccessor.getComponentAccessor().getPeer(owner);
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("Owner is " + owner);
                focusLog.finer("Owner peer is " + ownerPeer);
                focusLog.finer("Owner X window " + Long.toHexString(ownerPeer.getWindow()));
                focusLog.finer("Owner content X window " + Long.toHexString(ownerPeer.getContentWindow()));
            }
            long ownerWindow = ownerPeer.getWindow();
            if (ownerWindow != 0) {
                XToolkit.awtLock();
                try {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                        focusLog.fine("Setting transient on " + Long.toHexString(getWindow())
                                      + " for " + Long.toHexString(ownerWindow));
                    }
                    setToplevelTransientFor(this, ownerPeer, false, true);

                    XWMHints hints = getWMHints();
                    hints.set_flags(hints.get_flags() | (int)XUtilConstants.WindowGroupHint);
                    hints.set_window_group(ownerWindow);
                    XlibWrapper.XSetWMHints(XToolkit.getDisplay(), getWindow(), hints.pData);
                }
                finally {
                    XToolkit.awtUnlock();
                }
            }
        }

        if (owner != null || isSimpleWindow()) {
            XNETProtocol protocol = XWM.getWM().getNETProtocol();
            if (protocol != null && protocol.active()) {
                XToolkit.awtLock();
                try {
                    XAtomList net_wm_state = getNETWMState();
                    net_wm_state.add(protocol.XA_NET_WM_STATE_SKIP_TASKBAR);
                    setNETWMState(net_wm_state);
                } finally {
                    XToolkit.awtUnlock();
                }

            }
        }

        if (((Window)target).getWarningString() != null) {
            if (!AWTAccessor.getWindowAccessor().isTrayIconWindow((Window)target)) {
                warningWindow = new XWarningWindow((Window)target, getWindow(), this);
            }
        }

        setSaveUnder(true);

        updateIconImages();

        updateShape();
        updateOpacity();
    }

    public void updateIconImages() {
        Window target = (Window)this.target;
        java.util.List<Image> iconImages = target.getIconImages();
        XWindowPeer ownerPeer = getOwnerPeer();
        winAttr.icons = new ArrayList<IconInfo>();
        if (iconImages.size() != 0) {
            winAttr.iconsInherited = false;
            for (Image image : iconImages) {
                if (image == null) {
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("XWindowPeer.updateIconImages: Skipping the image passed into Java because it's null.");
                    }
                    continue;
                }
                IconInfo iconInfo;
                try {
                    iconInfo = new IconInfo(image);
                } catch (Exception e){
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("XWindowPeer.updateIconImages: Perhaps the image passed into Java is broken. Skipping this icon.");
                    }
                    continue;
                }
                if (iconInfo.isValid()) {
                    winAttr.icons.add(iconInfo);
                }
            }
        }

        winAttr.icons = normalizeIconImages(winAttr.icons);

        if (winAttr.icons.size() == 0) {
            if (ownerPeer != null) {
                winAttr.iconsInherited = true;
                winAttr.icons = ownerPeer.getIconInfo();
            } else {
                winAttr.iconsInherited = false;
                winAttr.icons = getDefaultIconInfo();
            }
        }
        recursivelySetIcon(winAttr.icons);
    }

    /*
     * Sometimes XChangeProperty(_NET_WM_ICON) doesn't work if
     * image buffer is too large. This function help us accommodate
     * initial list of the icon images to certainly-acceptable.
     * It does scale some of these icons to appropriate size
     * if it's necessary.
     */
    static java.util.List<IconInfo> normalizeIconImages(java.util.List<IconInfo> icons) {
        java.util.List<IconInfo> result = new ArrayList<IconInfo>();
        int totalLength = 0;
        boolean haveLargeIcon = false;

        for (IconInfo icon : icons) {
            int width = icon.getWidth();
            int height = icon.getHeight();
            int length = icon.getRawLength();

            if (width > PREFERRED_SIZE_FOR_ICON || height > PREFERRED_SIZE_FOR_ICON) {
                if (haveLargeIcon) {
                    continue;
                }
                int scaledWidth = width;
                int scaledHeight = height;
                while (scaledWidth > PREFERRED_SIZE_FOR_ICON ||
                       scaledHeight > PREFERRED_SIZE_FOR_ICON) {
                    scaledWidth = scaledWidth / 2;
                    scaledHeight = scaledHeight / 2;
                }

                icon.setScaledSize(scaledWidth, scaledHeight);
                length = icon.getRawLength();
            }

            if (totalLength + length <= MAXIMUM_BUFFER_LENGTH_NET_WM_ICON) {
                totalLength += length;
                result.add(icon);
                if (width > PREFERRED_SIZE_FOR_ICON || height > PREFERRED_SIZE_FOR_ICON) {
                    haveLargeIcon = true;
                }
            }
        }

        if (iconLog.isLoggable(PlatformLogger.Level.FINEST)) {
            iconLog.finest(">>> Length_ of buffer of icons data: " + totalLength +
                           ", maximum length: " + MAXIMUM_BUFFER_LENGTH_NET_WM_ICON);
        }

        return result;
    }

    /*
     * Dumps each icon from the list
     */
    static void dumpIcons(java.util.List<IconInfo> icons) {
        if (iconLog.isLoggable(PlatformLogger.Level.FINEST)) {
            iconLog.finest(">>> Sizes of icon images:");
            for (IconInfo icon : icons) {
                iconLog.finest("    {0}", icon);
            }
        }
    }

    public void recursivelySetIcon(java.util.List<IconInfo> icons) {
        dumpIcons(winAttr.icons);
        setIconHints(icons);
        Window target = (Window)this.target;
        Window[] children = target.getOwnedWindows();
        int cnt = children.length;
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        for (int i = 0; i < cnt; i++) {
            final ComponentPeer childPeer = acc.getPeer(children[i]);
            if (childPeer instanceof XWindowPeer xWindowPeer) {
                if (xWindowPeer.winAttr.iconsInherited) {
                    xWindowPeer.winAttr.icons = icons;
                    xWindowPeer.recursivelySetIcon(icons);
                }
            }
        }
    }

    java.util.List<IconInfo> getIconInfo() {
        return winAttr.icons;
    }
    void setIconHints(java.util.List<IconInfo> icons) {
    }

    private static ArrayList<IconInfo> defaultIconInfo;
    protected static synchronized java.util.List<IconInfo> getDefaultIconInfo() {
        if (defaultIconInfo == null) {
            defaultIconInfo = new ArrayList<IconInfo>();
            if (XlibWrapper.dataModel == 32) {
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon32_java_icon16_png.java_icon16_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon32_java_icon24_png.java_icon24_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon32_java_icon32_png.java_icon32_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon32_java_icon48_png.java_icon48_png));
            } else {
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon64_java_icon16_png.java_icon16_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon64_java_icon24_png.java_icon24_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon64_java_icon32_png.java_icon32_png));
                defaultIconInfo.add(new IconInfo(sun.awt.AWTIcon64_java_icon48_png.java_icon48_png));
            }
        }
        return defaultIconInfo;
    }

    private void updateShape() {
        Shape shape = ((Window)target).getShape();
        if (shape != null) {
            applyShape(Region.getInstance(shape, null));
        }
    }

    private void updateOpacity() {
        float opacity = ((Window)target).getOpacity();
        if (opacity < 1.0f) {
            setOpacity(opacity);
        }
    }

    public void updateMinimumSize() {
        targetMinimumSize = (target.isMinimumSizeSet()) ?
            target.getMinimumSize() : null;
    }

    public Dimension getTargetMinimumSize() {
        return (targetMinimumSize == null) ? null : new Dimension(targetMinimumSize);
    }

    public XWindowPeer getOwnerPeer() {
        return ownerPeer;
    }

    public void setBounds(int x, int y, int width, int height, int op) {
        XToolkit.awtLock();
        try {
            Rectangle oldBounds = getBounds();

            super.setBounds(x, y, width, height, op);

            Rectangle bounds = getBounds();

            XSizeHints hints = getHints();
            setSizeHints(hints.get_flags() | XUtilConstants.PPosition | XUtilConstants.PSize,
                             bounds.x, bounds.y, bounds.width, bounds.height);
            XWM.setMotifDecor(this, false, 0, 0);

            boolean isResized = !bounds.getSize().equals(oldBounds.getSize());
            boolean isMoved = !bounds.getLocation().equals(oldBounds.getLocation());
            if (isMoved || isResized) {
                repositionSecurityWarning();
            }
            if (isResized) {
                postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_RESIZED));
            }
            if (isMoved) {
                postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_MOVED));
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    void updateFocusability() {
        updateFocusableWindowState();
        XToolkit.awtLock();
        try {
            XWMHints hints = getWMHints();
            hints.set_flags(hints.get_flags() | (int)XUtilConstants.InputHint);
            hints.set_input(false/*isNativelyNonFocusableWindow() ? (0):(1)*/);
            XlibWrapper.XSetWMHints(XToolkit.getDisplay(), getWindow(), hints.pData);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    public void handleIconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_ICONIFIED));
    }

    public void handleDeiconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_DEICONIFIED));
    }

    public void handleStateChange(int oldState, int newState) {
        postEvent(new WindowEvent((Window)target,
                                  WindowEvent.WINDOW_STATE_CHANGED,
                                  oldState, newState));
    }

    boolean isAutoRequestFocus() {
        if (XToolkit.isToolkitThread()) {
            return AWTAccessor.getWindowAccessor().isAutoRequestFocus((Window)target);
        } else {
            return ((Window)target).isAutoRequestFocus();
        }
    }

    /*
     * Retrieves real native focused window and converts it into Java peer.
     */
    static XWindowPeer getNativeFocusedWindowPeer() {
        XBaseWindow baseWindow = XToolkit.windowToXWindow(xGetInputFocus());
        return (baseWindow instanceof XWindowPeer) ? (XWindowPeer)baseWindow :
               (baseWindow instanceof XFocusProxyWindow) ?
               ((XFocusProxyWindow)baseWindow).getOwner() : null;
    }

    /*
     * Retrieves real native focused window and converts it into Java window.
     */
    static Window getNativeFocusedWindow() {
        XWindowPeer peer = getNativeFocusedWindowPeer();
        return peer != null ? (Window)peer.target : null;
    }

    boolean isFocusableWindow() {
        if (XToolkit.isToolkitThread() || SunToolkit.isAWTLockHeldByCurrentThread())
        {
            return cachedFocusableWindow;
        } else {
            return ((Window)target).isFocusableWindow();
        }
    }

    /* WARNING: don't call client code in this method! */
    boolean isFocusedWindowModalBlocker() {
        return false;
    }

    long getFocusTargetWindow() {
        return getContentWindow();
    }

    /**
     * Returns whether or not this window peer has native X window
     * configured as non-focusable window. It might happen if:
     * - Java window is non-focusable
     * - Java window is simple Window(not Frame or Dialog)
     */
    boolean isNativelyNonFocusableWindow() {
        if (XToolkit.isToolkitThread() || SunToolkit.isAWTLockHeldByCurrentThread())
        {
            return isSimpleWindow() || !cachedFocusableWindow;
        } else {
            return isSimpleWindow() || !(((Window)target).isFocusableWindow());
        }
    }

    public void handleWindowFocusIn_Dispatch() {
        if (EventQueue.isDispatchThread()) {
            XKeyboardFocusManagerPeer.getInstance().setCurrentFocusedWindow((Window) target);
            WindowEvent we = new WindowEvent((Window)target, WindowEvent.WINDOW_GAINED_FOCUS);
            SunToolkit.setSystemGenerated(we);
            target.dispatchEvent(we);
        }
    }

    public void handleWindowFocusInSync(long serial) {
        WindowEvent we = new WindowEvent((Window)target, WindowEvent.WINDOW_GAINED_FOCUS);
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusedWindow((Window) target);
        sendEvent(we);
    }
    public void handleWindowFocusIn(long serial) {
        WindowEvent we = new WindowEvent((Window)target, WindowEvent.WINDOW_GAINED_FOCUS);
        /* wrap in Sequenced, then post*/
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusedWindow((Window) target);
        postEvent(wrapInSequenced((AWTEvent) we));
    }

    public void handleWindowFocusOut(Window oppositeWindow, long serial) {
        WindowEvent we = new WindowEvent((Window)target, WindowEvent.WINDOW_LOST_FOCUS, oppositeWindow);
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusedWindow(null);
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusOwner(null);
        /* wrap in Sequenced, then post*/
        postEvent(wrapInSequenced((AWTEvent) we));
    }
    public void handleWindowFocusOutSync(Window oppositeWindow, long serial) {
        WindowEvent we = new WindowEvent((Window)target, WindowEvent.WINDOW_LOST_FOCUS, oppositeWindow);
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusedWindow(null);
        XKeyboardFocusManagerPeer.getInstance().setCurrentFocusOwner(null);
        sendEvent(we);
    }

/* --- DisplayChangedListener Stuff --- */

    /* Xinerama
     * called to check if we've been moved onto a different screen
     * Based on checkNewXineramaScreen() in awt_GraphicsEnv.c
     */
    public void checkIfOnNewScreen(Rectangle newBounds) {
        if (!XToolkit.localEnv.runningXinerama()) {
            return;
        }

        if (log.isLoggable(PlatformLogger.Level.FINEST)) {
            log.finest("XWindowPeer: Check if we've been moved to a new screen since we're running in Xinerama mode");
        }

        int area = newBounds.width * newBounds.height;
        int intAmt, vertAmt, horizAmt;
        int largestAmt = 0;
        int curScreenNum = ((X11GraphicsDevice)getGraphicsConfiguration().getDevice()).getScreen();
        int newScreenNum = 0;
        GraphicsDevice[] gds = XToolkit.localEnv.getScreenDevices();
        GraphicsConfiguration newGC = null;
        Rectangle screenBounds;

        XToolkit.awtUnlock();
        try {
            for (int i = 0; i < gds.length; i++) {
                screenBounds = gds[i].getDefaultConfiguration().getBounds();
                if (newBounds.intersects(screenBounds)) {
                    horizAmt = Math.min(newBounds.x + newBounds.width,
                                        screenBounds.x + screenBounds.width) -
                               Math.max(newBounds.x, screenBounds.x);
                    vertAmt = Math.min(newBounds.y + newBounds.height,
                                       screenBounds.y + screenBounds.height)-
                              Math.max(newBounds.y, screenBounds.y);
                    intAmt = horizAmt * vertAmt;
                    if (intAmt == area) {
                        newScreenNum = i;
                        newGC = gds[i].getDefaultConfiguration();
                        break;
                    }
                    if (intAmt > largestAmt) {
                        largestAmt = intAmt;
                        newScreenNum = i;
                        newGC = gds[i].getDefaultConfiguration();
                    }
                }
            }
        } finally {
            XToolkit.awtLock();
        }
        if (newScreenNum != curScreenNum) {
            if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                log.finest("XWindowPeer: Moved to a new screen");
            }
            executeDisplayChangedOnEDT(newGC);
        }
    }

    /**
     * Helper method that executes the displayChanged(screen) method on
     * the event dispatch thread.  This method is used in the Xinerama case
     * and after display mode change events.
     */
    private void executeDisplayChangedOnEDT(final GraphicsConfiguration gc) {
        Runnable dc = new Runnable() {
            public void run() {
                AWTAccessor.getComponentAccessor().
                    setGraphicsConfiguration(target, gc);
            }
        };
        SunToolkit.executeOnEventHandlerThread(target, dc);
    }

    /**
     * From the DisplayChangedListener interface; called from
     * X11GraphicsDevice when the display mode has been changed.
     */
    public void displayChanged() {
        executeDisplayChangedOnEDT(getGraphicsConfiguration());
    }

    /**
     * From the DisplayChangedListener interface; top-levels do not need
     * to react to this event.
     */
    public void paletteChanged() {
    }

    private Point queryXLocation()
    {
        return XlibUtil.translateCoordinates(getContentWindow(), XlibWrapper
                                             .RootWindow(XToolkit.getDisplay(),
                                             getScreenNumber()),
                                             new Point(0, 0), getScale());
    }

    protected Point getNewLocation(XConfigureEvent xe, int leftInset, int topInset) {
        Rectangle targetBounds = AWTAccessor.getComponentAccessor().getBounds(target);

        int runningWM = XWM.getWMID();
        Point newLocation = targetBounds.getLocation();
        if (xe.get_send_event() || runningWM == XWM.NO_WM || XWM.isNonReparentingWM()) {
            newLocation = new Point(scaleDown(xe.get_x()) - leftInset,
                                    scaleDown(xe.get_y()) - topInset);
        } else {
            switch (runningWM) {
                case XWM.CDE_WM:
                case XWM.MOTIF_WM:
                case XWM.METACITY_WM:
                case XWM.MUTTER_WM:
                case XWM.SAWFISH_WM:
                case XWM.UNITY_COMPIZ_WM:
                {
                    Point xlocation = queryXLocation();
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("New X location: {0}", xlocation);
                    }
                    if (xlocation != null) {
                        newLocation = xlocation;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return newLocation;
    }

    /*
     * Overridden to check if we need to update our GraphicsDevice/Config
     * Added for 4934052.
     */
    @Override
    public void handleConfigureNotifyEvent(XEvent xev) {
        assert (SunToolkit.isAWTLockHeldByCurrentThread());
        XConfigureEvent xe = xev.get_xconfigure();
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine(xe.toString());
        }
        checkIfOnNewScreen(new Rectangle(scaleDown(xe.get_x()),
                scaleDown(xe.get_y()),
                scaleDown(xe.get_width()),
                scaleDown(xe.get_height())));

        Rectangle oldBounds = getBounds();

        x = scaleDown(xe.get_x());
        y = scaleDown(xe.get_y());
        width = scaleDown(xe.get_width());
        height = scaleDown(xe.get_height());

        if (!getBounds().getSize().equals(oldBounds.getSize())) {
            AWTAccessor.getComponentAccessor().setSize(target, width, height);
            postEvent(new ComponentEvent(target, ComponentEvent.COMPONENT_RESIZED));
        }
        if (!getBounds().getLocation().equals(oldBounds.getLocation())) {
            AWTAccessor.getComponentAccessor().setLocation(target, x, y);
            postEvent(new ComponentEvent(target, ComponentEvent.COMPONENT_MOVED));
        }
        repositionSecurityWarning();
    }

    final void requestXFocus(long time) {
        requestXFocus(time, true);
    }

    final void requestXFocus() {
        requestXFocus(0, false);
    }

    /**
     * Requests focus to this top-level. Descendants should override to provide
     * implementations based on a class of top-level.
     */
    protected void requestXFocus(long time, boolean timeProvided) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine("Requesting window focus");
        }
        requestWindowFocus(time, timeProvided);
    }

    public final boolean focusAllowedFor() {
        if (isNativelyNonFocusableWindow()) {
            return false;
        }
/*
        Window target = (Window)this.target;
        if (!target.isVisible() ||
            !target.isEnabled() ||
            !target.isFocusable())
        {
            return false;
        }
*/
        if (isModalBlocked()) {
            return false;
        }
        return true;
    }

    public void handleFocusEvent(XEvent xev) {
        XFocusChangeEvent xfe = xev.get_xfocus();
        FocusEvent fe;
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine("{0}", xfe);
        }
        if (isEventDisabled(xev)) {
            return;
        }
        if (xev.get_type() == XConstants.FocusIn)
        {
            if (focusAllowedFor()) {
                if (xfe.get_mode() == XConstants.NotifyNormal 
                    || xfe.get_mode() == XConstants.NotifyWhileGrabbed) 
                {
                    handleWindowFocusIn(xfe.get_serial());
                }
            }
        }
        else
        {
            if (xfe.get_mode() == XConstants.NotifyNormal 
                || xfe.get_mode() == XConstants.NotifyWhileGrabbed) 
            {
                if (!isNativelyNonFocusableWindow()) {
                    XWindowPeer oppositeXWindow = getNativeFocusedWindowPeer();
                    Object oppositeTarget = (oppositeXWindow!=null)? oppositeXWindow.getTarget() : null;
                    Window oppositeWindow = null;
                    if (oppositeTarget instanceof Window) {
                        oppositeWindow = (Window) oppositeTarget;
                    }
                    if (oppositeXWindow != null && oppositeXWindow.isNativelyNonFocusableWindow()) {
                        return;
                    }
                    if (this == oppositeXWindow) {
                        oppositeWindow = null;
                    } else if (oppositeXWindow instanceof XDecoratedPeer) {
                        if (((XDecoratedPeer) oppositeXWindow).actualFocusedWindow != null) {
                            oppositeXWindow = ((XDecoratedPeer) oppositeXWindow).actualFocusedWindow;
                            oppositeTarget = oppositeXWindow.getTarget();
                            if (oppositeTarget instanceof Window
                                && oppositeXWindow.isVisible()
                                && oppositeXWindow.isNativelyNonFocusableWindow())
                            {
                                oppositeWindow = ((Window) oppositeTarget);
                            }
                        }
                    }
                    handleWindowFocusOut(oppositeWindow, xfe.get_serial());
                }
            }
        }
    }

    void setSaveUnder(boolean state) {}

    public void toFront() {
        if (isOverrideRedirect() && mustControlStackPosition) {
            mustControlStackPosition = false;
            removeRootPropertyEventDispatcher();
        }
        if (isVisible()) {
            super.toFront();
            if (isFocusableWindow() && isAutoRequestFocus() &&
                !isModalBlocked() && !isWithdrawn())
            {
                requestInitialFocus();
            }
        } else {
            setVisible(true);
        }
    }

    public void toBack() {
        XToolkit.awtLock();
        try {
            if(!isOverrideRedirect()) {
                XlibWrapper.XLowerWindow(XToolkit.getDisplay(), getWindow());
            }else{
                lowerOverrideRedirect();
            }
        }
        finally {
            XToolkit.awtUnlock();
        }
    }
    private void lowerOverrideRedirect() {
        HashSet<Long> toplevels = new HashSet<>();
        long topl = 0, mytopl = 0;

        for (XWindowPeer xp : windows) {
            topl = getToplevelWindow( xp.getWindow() );
            if( xp.equals( this ) ) {
                mytopl = topl;
            }
            if( topl > 0 )
                toplevels.add( Long.valueOf( topl ) );
        }

        long laux,     wDesktop = -1, wBottom = -1;
        int  iMy = -1, iDesktop = -1, iBottom = -1;
        int i = 0;
        XQueryTree xqt = new XQueryTree(XToolkit.getDefaultRootWindow());
        try {
            if( xqt.execute() > 0 ) {
                int nchildren = xqt.get_nchildren();
                long children = xqt.get_children();
                for(i = 0; i < nchildren; i++) {
                    laux = Native.getWindow(children, i);
                    if( laux == mytopl ) {
                        iMy = i;
                    }else if( isDesktopWindow( laux ) ) {
                        iDesktop = i;
                        wDesktop = laux;
                    }else if(iBottom < 0 &&
                             toplevels.contains( Long.valueOf(laux) ) &&
                             laux != mytopl) {
                        iBottom = i;
                        wBottom = laux;
                    }
                }
            }

            if( (iMy < iBottom || iBottom < 0 )&& iDesktop < iMy)
                return; 

            long to_restack = Native.allocateLongArray(2);
            Native.putLong(to_restack, 0, wBottom);
            Native.putLong(to_restack, 1,  mytopl);
            XlibWrapper.XRestackWindows(XToolkit.getDisplay(), to_restack, 2);
            XlibWrapper.unsafe.freeMemory(to_restack);


            if( !mustControlStackPosition ) {
                mustControlStackPosition = true;
                addRootPropertyEventDispatcher();
            }
        } finally {
            xqt.dispose();
        }
    }
    /**
        Get XID of closest to root window in a given window hierarchy.
        FIXME: do not call it "toplevel" as it is misleading.
        On error return 0.
    */
    private long getToplevelWindow( long w ) {
        long wi = w, ret, root;
        do {
            ret = wi;
            XQueryTree qt = new XQueryTree(wi);
            try {
                if (qt.execute() == 0) {
                    return 0;
                }
                root = qt.get_root();
                wi = qt.get_parent();
            } finally {
                qt.dispose();
            }

        } while (wi != root);

        return ret;
    }

    private static boolean isDesktopWindow( long wi ) {
        return XWM.getWM().isDesktopWindow( wi );
    }

    private void updateAlwaysOnTop() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Promoting always-on-top state {0}", Boolean.valueOf(alwaysOnTop));
        }
        XWM.getWM().setLayer(this,
                alwaysOnTop ?
                        XLayerProtocol.LAYER_ALWAYS_ON_TOP :
                        XLayerProtocol.LAYER_NORMAL);
    }

    public void updateAlwaysOnTopState() {
        this.alwaysOnTop = ((Window) this.target).isAlwaysOnTop();
        if (ownerPeer != null) {
            XToolkit.awtLock();
            try {
                restoreTransientFor(this);
                applyWindowType();
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
        updateAlwaysOnTop();
    }

    boolean isLocationByPlatform() {
        return locationByPlatform;
    }

    private void promoteDefaultPosition() {
        this.locationByPlatform = ((Window)target).isLocationByPlatform();
        if (locationByPlatform) {
            XToolkit.awtLock();
            try {
                Rectangle bounds = getBounds();
                XSizeHints hints = getHints();
                setSizeHints(hints.get_flags() & ~(XUtilConstants.USPosition | XUtilConstants.PPosition),
                             bounds.x, bounds.y, bounds.width, bounds.height);
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }

    public void setVisible(boolean vis) {
        if (!isVisible() && vis) {
            isBeforeFirstMapNotify = true;
            winAttr.initialFocus = isAutoRequestFocus();
            if (!winAttr.initialFocus) {
                /*
                 * It's easier and safer to temporary suppress WM_TAKE_FOCUS
                 * protocol itself than to ignore WM_TAKE_FOCUS client message.
                 * Because we will have to make the difference between
                 * the message come after showing and the message come after
                 * activation. Also, on Metacity, for some reason, we have _two_
                 * WM_TAKE_FOCUS client messages when showing a frame/dialog.
                 */
                suppressWmTakeFocus(true);
            }
        }
        updateFocusability();
        promoteDefaultPosition();
        if (!vis && warningWindow != null) {
            warningWindow.setSecurityWarningVisible(false, false);
        }
        boolean refreshChildsTransientFor = isVisible() != vis;
        super.setVisible(vis);
        if (refreshChildsTransientFor) {
            for (Window child : ((Window) target).getOwnedWindows()) {
                XToolkit.awtLock();
                try {
                    if(!child.isLightweight() && child.isVisible()) {
                        ComponentPeer childPeer = AWTAccessor.
                                getComponentAccessor().getPeer(child);
                        if(childPeer instanceof XWindowPeer) {
                            XWindowPeer windowPeer = (XWindowPeer) childPeer;
                            restoreTransientFor(windowPeer);
                            windowPeer.applyWindowType();
                        }
                    }
                }
                finally {
                    XToolkit.awtUnlock();
                }
            }
        }
        if (!vis && !isWithdrawn()) {
            XToolkit.awtLock();
            try {
                XUnmapEvent unmap = new XUnmapEvent();
                unmap.set_window(window);
                unmap.set_event(XToolkit.getDefaultRootWindow());
                unmap.set_type(XConstants.UnmapNotify);
                unmap.set_from_configure(false);
                XlibWrapper.XSendEvent(XToolkit.getDisplay(), XToolkit.getDefaultRootWindow(),
                        false, XConstants.SubstructureNotifyMask | XConstants.SubstructureRedirectMask,
                        unmap.pData);
                unmap.dispose();
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
        if (isOverrideRedirect() && vis) {
            updateChildrenSizes();
        }
        repositionSecurityWarning();
    }

    protected void suppressWmTakeFocus(boolean doSuppress) {
    }

    final boolean isSimpleWindow() {
        return !(target instanceof Frame || target instanceof Dialog);
    }
    boolean hasWarningWindow() {
        return ((Window)target).getWarningString() != null;
    }

    int getMenuBarHeight() {
        return 0;
    }

    void updateChildrenSizes() {
    }

    public void repositionSecurityWarning() {
        if (warningWindow != null) {
            ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
            int x = compAccessor.getX(target);
            int y = compAccessor.getY(target);
            int width = compAccessor.getWidth(target);
            int height = compAccessor.getHeight(target);
            warningWindow.reposition(x, y, width, height);
        }
    }

    @Override
    protected void setMouseAbove(boolean above) {
        super.setMouseAbove(above);
        updateSecurityWarningVisibility();
    }

    @Override
    public void setFullScreenExclusiveModeState(boolean state) {
        super.setFullScreenExclusiveModeState(state);
        updateSecurityWarningVisibility();
    }

    public void updateSecurityWarningVisibility() {
        if (warningWindow == null) {
            return;
        }

        if (!isVisible()) {
            return; 
        }

        boolean show = false;

        if (!isFullScreenExclusiveMode()) {
            int state = getWMState();

            if (isVisible() && (state == XUtilConstants.NormalState || isSimpleWindow())) {
                if (XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow() ==
                        getTarget())
                {
                    show = true;
                }

                if (isMouseAbove() || warningWindow.isMouseAbove())
                {
                    show = true;
                }
            }
        }

        warningWindow.setSecurityWarningVisible(show, true);
    }

    boolean isOverrideRedirect() {
        return XWM.getWMID() == XWM.OPENLOOK_WM ||
            Window.Type.POPUP.equals(getWindowType());
    }

    final boolean isOLWMDecorBug() {
        return XWM.getWMID() == XWM.OPENLOOK_WM &&
            winAttr.nativeDecor == false;
    }

    public void dispose() {
        if (isGrabbed()) {
            if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                grabLog.fine("Generating UngrabEvent on {0} because of the window disposal", this);
            }
            postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
        }

        SunToolkit.awtLock();

        try {
            windows.remove(this);
        } finally {
            SunToolkit.awtUnlock();
        }

        if (warningWindow != null) {
            warningWindow.destroy();
        }

        removeRootPropertyEventDispatcher();
        mustControlStackPosition = false;
        super.dispose();

        /*
         * Fix for 6457980.
         * When disposing an owned Window we should implicitly
         * return focus to its decorated owner because it won't
         * receive WM_TAKE_FOCUS.
         */
        if (isSimpleWindow()) {
            if (target == XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow()) {
                Window owner = getDecoratedOwner((Window)target);
                ((XWindowPeer)AWTAccessor.getComponentAccessor().getPeer(owner)).requestWindowFocus();
            }
        }
    }

    boolean isResizable() {
        return winAttr.isResizable;
    }

    public void handleVisibilityEvent(XEvent xev) {
        super.handleVisibilityEvent(xev);
        XVisibilityEvent ve = xev.get_xvisibility();
        winAttr.visibilityState = ve.get_state();
        repositionSecurityWarning();
    }

    void handleRootPropertyNotify(XEvent xev) {
        XPropertyEvent ev = xev.get_xproperty();
        if( mustControlStackPosition &&
            ev.get_atom() == XAtom.get("_NET_CLIENT_LIST_STACKING").getAtom()){
            if(isOverrideRedirect()) {
                toBack();
            }
        }
    }

    private void removeStartupNotification() {
        if (isStartupNotificationRemoved.getAndSet(true)) {
            return;
        }

        @SuppressWarnings("removal")
        final String desktopStartupId = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return XToolkit.getEnv("DESKTOP_STARTUP_ID");
            }
        });
        if (desktopStartupId == null) {
            return;
        }

        final StringBuilder messageBuilder = new StringBuilder("remove: ID=");
        messageBuilder.append('"');
        for (int i = 0; i < desktopStartupId.length(); i++) {
            if (desktopStartupId.charAt(i) == '"' || desktopStartupId.charAt(i) == '\\') {
                messageBuilder.append('\\');
            }
            messageBuilder.append(desktopStartupId.charAt(i));
        }
        messageBuilder.append('"');
        messageBuilder.append('\0');
        final byte[] message = messageBuilder.toString().getBytes(UTF_8);

        XClientMessageEvent req = null;

        XToolkit.awtLock();
        try {
            final XAtom netStartupInfoBeginAtom = XAtom.get("_NET_STARTUP_INFO_BEGIN");
            final XAtom netStartupInfoAtom = XAtom.get("_NET_STARTUP_INFO");

            req = new XClientMessageEvent();
            req.set_type(XConstants.ClientMessage);
            req.set_window(getWindow());
            req.set_message_type(netStartupInfoBeginAtom.getAtom());
            req.set_format(8);

            for (int pos = 0; pos < message.length; pos += 20) {
                final int msglen = Math.min(message.length - pos, 20);
                int i = 0;
                for (; i < msglen; i++) {
                    XlibWrapper.unsafe.putByte(req.get_data() + i, message[pos + i]);
                }
                for (; i < 20; i++) {
                    XlibWrapper.unsafe.putByte(req.get_data() + i, (byte)0);
                }
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                    XlibWrapper.RootWindow(XToolkit.getDisplay(), getScreenNumber()),
                    false,
                    XConstants.PropertyChangeMask,
                    req.pData);
                req.set_message_type(netStartupInfoAtom.getAtom());
            }
        } finally {
            XToolkit.awtUnlock();
            if (req != null) {
                req.dispose();
            }
        }
    }

    public void handleMapNotifyEvent(XEvent xev) {
        removeStartupNotification();

        isUnhiding |= isWMStateNetHidden();

        super.handleMapNotifyEvent(xev);
        if (!winAttr.initialFocus) {
            suppressWmTakeFocus(false); 
            /*
             * For some reason, on Metacity, a frame/dialog being shown
             * without WM_TAKE_FOCUS protocol doesn't get moved to the front.
             * So, we do it evidently.
             */
            XToolkit.awtLock();
            try {
                XlibWrapper.XRaiseWindow(XToolkit.getDisplay(), getWindow());
            } finally {
                XToolkit.awtUnlock();
            }
        }
        if (shouldFocusOnMapNotify()) {
            focusLog.fine("Automatically request focus on window");
            requestInitialFocus();
        }
        isUnhiding = false;
        isBeforeFirstMapNotify = false;
        updateAlwaysOnTop();

        synchronized (getStateLock()) {
            if (!isMapped) {
                isMapped = true;
            }
        }
    }

    public void handleUnmapNotifyEvent(XEvent xev) {
        super.handleUnmapNotifyEvent(xev);

        isUnhiding |= isWMStateNetHidden();

        synchronized (getStateLock()) {
            if (isMapped) {
                isMapped = false;
            }
        }
    }

    private boolean shouldFocusOnMapNotify() {
        boolean res = false;

        if (isBeforeFirstMapNotify) {
            res = (winAttr.initialFocus ||          
                   isFocusedWindowModalBlocker());
        } else {
            res = isUnhiding;                       
        }
        res = res &&
            isFocusableWindow() &&                  
            !isModalBlocked();                      

        return res;
    }

    protected boolean isWMStateNetHidden() {
        XNETProtocol protocol = XWM.getWM().getNETProtocol();
        return (protocol != null && protocol.isWMStateNetHidden(this));
    }

    protected void requestInitialFocus() {
        requestXFocus();
    }

    public void addToplevelStateListener(ToplevelStateListener l){
        toplevelStateListeners.add(l);
    }

    public void removeToplevelStateListener(ToplevelStateListener l){
        toplevelStateListeners.remove(l);
    }

    /**
     * Override this methods to get notifications when top-level window state changes. The state is
     * meant in terms of ICCCM: WithdrawnState, IconicState, NormalState
     */
    @Override
    protected void stateChanged(long time, int oldState, int newState) {
        updateTransientFor();

        for (ToplevelStateListener topLevelListenerTmp : toplevelStateListeners) {
            topLevelListenerTmp.stateChangedICCCM(oldState, newState);
        }

        updateSecurityWarningVisibility();
    }

    boolean isWithdrawn() {
        return getWMState() == XUtilConstants.WithdrawnState;
    }

    boolean hasDecorations(int decor) {
        if (!winAttr.nativeDecor) {
            return false;
        }
        else {
            int myDecor = winAttr.decorations;
            boolean hasBits = ((myDecor & decor) == decor);
            if ((myDecor & XWindowAttributesData.AWT_DECOR_ALL) != 0)
                return !hasBits;
            else
                return hasBits;
        }
    }

    void setReparented(boolean newValue) {
        super.setReparented(newValue);
        XToolkit.awtLock();
        try {
            if (isReparented() && delayedModalBlocking) {
                addToTransientFors(AWTAccessor.getComponentAccessor().getPeer(modalBlocker));
                delayedModalBlocking = false;
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * Returns a Vector of all Java top-level windows,
     * sorted by their current Z-order
     */
    static Vector<XWindowPeer> collectJavaToplevels() {
        Vector<XWindowPeer> javaToplevels = new Vector<XWindowPeer>();
        ArrayList<Long> v = new ArrayList<Long>();
        X11GraphicsEnvironment ge =
            (X11GraphicsEnvironment)GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (!ge.runningXinerama() && (gds.length > 1)) {
            for (GraphicsDevice gd : gds) {
                int screen = ((X11GraphicsDevice)gd).getScreen();
                long rootWindow = XlibWrapper.RootWindow(XToolkit.getDisplay(), screen);
                v.add(rootWindow);
            }
        } else {
            v.add(XToolkit.getDefaultRootWindow());
        }
        final int windowsCount = windows.size();
        while ((v.size() > 0) && (javaToplevels.size() < windowsCount)) {
            long win = v.remove(0);
            XQueryTree qt = new XQueryTree(win);
            try {
                if (qt.execute() != 0) {
                    int nchildren = qt.get_nchildren();
                    long children = qt.get_children();
                    for (int i = 0; i < nchildren; i++) {
                        long child = Native.getWindow(children, i);
                        XBaseWindow childWindow = XToolkit.windowToXWindow(child);
                        if ((childWindow != null) && !(childWindow instanceof XWindowPeer)) {
                            continue;
                        } else {
                            v.add(child);
                        }
                        if (childWindow instanceof XWindowPeer) {
                            XWindowPeer np = (XWindowPeer)childWindow;
                            javaToplevels.add(np);
                            int k = 0;
                            XWindowPeer toCheck = javaToplevels.get(k);
                            while (toCheck != np) {
                                XWindowPeer toCheckOwnerPeer = toCheck.getOwnerPeer();
                                if (toCheckOwnerPeer == np) {
                                    javaToplevels.remove(k);
                                    javaToplevels.add(toCheck);
                                } else {
                                    k++;
                                }
                                toCheck = javaToplevels.get(k);
                            }
                        }
                    }
                }
            } finally {
                qt.dispose();
            }
        }
        return javaToplevels;
    }

    public void setModalBlocked(Dialog d, boolean blocked) {
        setModalBlocked(d, blocked, null);
    }
    public void setModalBlocked(Dialog d, boolean blocked,
                                Vector<XWindowPeer> javaToplevels)
    {
        XToolkit.awtLock();
        try {
            synchronized(getStateLock()) {
                XDialogPeer blockerPeer = AWTAccessor.getComponentAccessor().getPeer(d);
                if (blocked) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("{0} is blocked by {1}", this, blockerPeer);
                    }
                    modalBlocker = d;

                    if (isReparented() || XWM.isNonReparentingWM()) {
                        addToTransientFors(blockerPeer, javaToplevels);
                    } else {
                        delayedModalBlocking = true;
                    }
                } else {
                    if (d != modalBlocker) {
                        throw new IllegalStateException("Trying to unblock window blocked by another dialog");
                    }
                    modalBlocker = null;

                    if (isReparented() || XWM.isNonReparentingWM()) {
                        removeFromTransientFors();
                    } else {
                        delayedModalBlocking = false;
                    }
                }

                updateTransientFor();
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * Sets the TRANSIENT_FOR hint to the given top-level window. This
     *  method is used when a window is modal blocked/unblocked or
     *  changed its state from/to NormalState to/from other states.
     * If window or transientForWindow are embedded frames, the containing
     *  top-level windows are used.
     *
     * @param window specifies the top-level window that the hint
     *  is to be set to
     * @param transientForWindow the top-level window
     * @param updateChain specifies if next/prevTransientFor fields are
     *  to be updated
     * @param allStates if set to {@code true} then TRANSIENT_FOR hint
     *  is set regardless of the state of window and transientForWindow,
     *  otherwise it is set only if both are in the same state
     */
    static void setToplevelTransientFor(XWindowPeer window, XWindowPeer transientForWindow,
                                                boolean updateChain, boolean allStates)
    {
        if ((window == null) || (transientForWindow == null)) {
            return;
        }
        if (updateChain) {
            window.prevTransientFor = transientForWindow;
            transientForWindow.nextTransientFor = window;
        }
        if (!allStates && (window.getWMState() != transientForWindow.getWMState())) {
            return;
        }
        if (window.getScreenNumber() != transientForWindow.getScreenNumber()) {
            return;
        }
        long bpw = window.getWindow();
        while (!XlibUtil.isToplevelWindow(bpw) && !XlibUtil.isXAWTToplevelWindow(bpw)) {
            bpw = XlibUtil.getParentWindow(bpw);
        }
        long tpw = transientForWindow.getWindow();
        XBaseWindow parent = transientForWindow;
        while (tpw != 0 && ((!XlibUtil.isToplevelWindow(tpw) &&
                !XlibUtil.isXAWTToplevelWindow(tpw)) || !parent.isVisible())) {
            tpw = XlibUtil.getParentWindow(tpw);
            parent = XToolkit.windowToXWindow(tpw);
        }

        if (parent instanceof XLightweightFramePeer) {
            XLightweightFramePeer peer = (XLightweightFramePeer) parent;
            long ownerWindowPtr = peer.getOverriddenWindowHandle();
            if (ownerWindowPtr != 0) {
                tpw = ownerWindowPtr;
            }
        }
        XlibWrapper.XSetTransientFor(XToolkit.getDisplay(), bpw, tpw);
        window.curRealTransientFor = parent;
    }

    /*
     * This method does nothing if this window is not blocked by any modal dialog.
     * For modal blocked windows this method looks up for the nearest
     *  prevTransientFor window that is in the same state (Normal/Iconified/Withdrawn)
     *  as this one and makes this window transient for it. The same operation is
     *  performed for nextTransientFor window.
     * Values of prevTransientFor and nextTransientFor fields are not changed.
     */
    void updateTransientFor() {
        int state = getWMState();
        XWindowPeer p = prevTransientFor;
        while ((p != null) && ((p.getWMState() != state) || (p.getScreenNumber() != getScreenNumber()))) {
            p = p.prevTransientFor;
        }
        if (p != null) {
            setToplevelTransientFor(this, p, false, false);
        } else {
            restoreTransientFor(this);
        }
        XWindowPeer n = nextTransientFor;
        while ((n != null) && ((n.getWMState() != state) || (n.getScreenNumber() != getScreenNumber()))) {
            n = n.nextTransientFor;
        }
        if (n != null) {
            setToplevelTransientFor(n, this, false, false);
        }
    }

    /*
     * Removes the TRANSIENT_FOR hint from the given top-level window.
     * If window or transientForWindow are embedded frames, the containing
     *  top-level windows are used.
     *
     * @param window specifies the top-level window that the hint
     *  is to be removed from
     */
    private static void removeTransientForHint(XWindowPeer window) {
        XAtom XA_WM_TRANSIENT_FOR = XAtom.get(XAtom.XA_WM_TRANSIENT_FOR);
        long bpw = window.getWindow();
        while (!XlibUtil.isToplevelWindow(bpw) && !XlibUtil.isXAWTToplevelWindow(bpw)) {
            bpw = XlibUtil.getParentWindow(bpw);
        }
        XlibWrapper.XDeleteProperty(XToolkit.getDisplay(), bpw, XA_WM_TRANSIENT_FOR.getAtom());
        window.curRealTransientFor = null;
    }

    /*
     * When a modal dialog is shown, all its blocked windows are lined up into
     *  a chain in such a way that each window is a transient_for window for
     *  the next one. That allows us to keep the modal dialog above all its
     *  blocked windows (even if there are some another modal dialogs between
     *  them).
     * This method adds this top-level window to the chain of the given modal
     *  dialog. To keep the current relative z-order, we should use the
     *  XQueryTree to find the place to insert this window to. As each window
     *  can be blocked by only one modal dialog (such checks are performed in
     *  shared code), both this and blockerPeer are on the top of their chains
     *  (chains may be empty).
     * If this window is a modal dialog and has its own chain, these chains are
     *  merged according to the current z-order (XQueryTree is used again).
     *  Below are some simple examples (z-order is from left to right, -- is
     *  modal blocking).
     *
     * Example 0:
     *     T (current chain of this, no windows are blocked by this)
     *  W1---B (current chain of blockerPeer, W2 is blocked by blockerPeer)
     *  Result is:
     *  W1-T-B (merged chain, all the windows are blocked by blockerPeer)
     *
     * Example 1:
     *  W1-T (current chain of this, W1 is blocked by this)
     *       W2-B (current chain of blockerPeer, W2 is blocked by blockerPeer)
     *  Result is:
     *  W1-T-W2-B (merged chain, all the windows are blocked by blockerPeer)
     *
     * Example 2:
     *  W1----T (current chain of this, W1 is blocked by this)
     *     W2---B (current chain of blockerPeer, W2 is blocked by blockerPeer)
     *  Result is:
     *  W1-W2-T-B (merged chain, all the windows are blocked by blockerPeer)
     *
     * This method should be called under the AWT lock.
     *
     * @see #removeFromTransientFors
     * @see #setModalBlocked
     */
    private void addToTransientFors(XDialogPeer blockerPeer) {
        addToTransientFors(blockerPeer, null);
    }

    private void addToTransientFors(XDialogPeer blockerPeer, Vector<XWindowPeer> javaToplevels)
    {
        XWindowPeer blockerChain = blockerPeer;
        while (blockerChain.prevTransientFor != null) {
            blockerChain = blockerChain.prevTransientFor;
        }
        XWindowPeer thisChain = this;
        while (thisChain.prevTransientFor != null) {
            thisChain = thisChain.prevTransientFor;
        }
        if (blockerChain == blockerPeer) {
            setToplevelTransientFor(blockerPeer, this, true, false);
        } else {
            if (javaToplevels == null) {
                javaToplevels = collectJavaToplevels();
            }
            XWindowPeer mergedChain = null;
            for (XWindowPeer w : javaToplevels) {
                XWindowPeer prevMergedChain = mergedChain;
                if (w == thisChain) {
                    if (thisChain == this) {
                        if (prevMergedChain != null) {
                            setToplevelTransientFor(this, prevMergedChain, true, false);
                        }
                        setToplevelTransientFor(blockerChain, this, true, false);
                        break;
                    } else {
                        mergedChain = thisChain;
                        thisChain = thisChain.nextTransientFor;
                    }
                } else if (w == blockerChain) {
                    mergedChain = blockerChain;
                    blockerChain = blockerChain.nextTransientFor;
                } else {
                    continue;
                }
                if (prevMergedChain == null) {
                    mergedChain.prevTransientFor = null;
                } else {
                    setToplevelTransientFor(mergedChain, prevMergedChain, true, false);
                    mergedChain.updateTransientFor();
                }
                if (blockerChain == blockerPeer) {
                    setToplevelTransientFor(thisChain, mergedChain, true, false);
                    setToplevelTransientFor(blockerChain, this, true, false);
                    break;
                }
            }
        }

        XToolkit.XSync();
    }

    static void restoreTransientFor(XWindowPeer window) {
        XWindowPeer ownerPeer = window.getOwnerPeer();
        if (ownerPeer != null) {
            setToplevelTransientFor(window, ownerPeer, false, true);
        } else {
            removeTransientForHint(window);
        }
    }

    /*
     * When a window is modally unblocked, it should be removed from its blocker
     *  chain, see {@link #addToTransientFor addToTransientFors} method for the
     *  chain definition.
     * The problem is that we cannot simply restore window's original
     *  TRANSIENT_FOR hint (if any) and link prevTransientFor and
     *  nextTransientFor together as the whole chain could be created as a merge
     *  of two other chains in addToTransientFors. In that case, if this window is
     *  a modal dialog, it would lost all its own chain, if we simply exclude it
     *  from the chain.
     * The correct behaviour of this method should be to split the chain, this
     *  window is currently in, into two chains. First chain is this window own
     *  chain (i. e. all the windows blocked by this one, directly or indirectly),
     *  if any, and the rest windows from the current chain.
     *
     * Example:
     *  Original state:
     *   W1-B1 (window W1 is blocked by B1)
     *   W2-B2 (window W2 is blocked by B2)
     *  B3 is shown and blocks B1 and B2:
     *   W1-W2-B1-B2-B3 (a single chain after B1.addToTransientFors() and B2.addToTransientFors())
     *  If we then unblock B1, the state should be:
     *   W1-B1 (window W1 is blocked by B1)
     *   W2-B2-B3 (window W2 is blocked by B2 and B2 is blocked by B3)
     *
     * This method should be called under the AWT lock.
     *
     * @see #addToTransientFors
     * @see #setModalBlocked
     */
    private void removeFromTransientFors() {
        XWindowPeer thisChain = this;
        XWindowPeer otherChain = nextTransientFor;
        Set<XWindowPeer> thisChainBlockers = new HashSet<XWindowPeer>();
        thisChainBlockers.add(this);
        XWindowPeer chainToSplit = prevTransientFor;
        while (chainToSplit != null) {
            XWindowPeer blocker = AWTAccessor.getComponentAccessor().getPeer(chainToSplit.modalBlocker);
            if (thisChainBlockers.contains(blocker)) {
                setToplevelTransientFor(thisChain, chainToSplit, true, false);
                thisChain = chainToSplit;
                thisChainBlockers.add(chainToSplit);
            } else {
                setToplevelTransientFor(otherChain, chainToSplit, true, false);
                otherChain = chainToSplit;
            }
            chainToSplit = chainToSplit.prevTransientFor;
        }
        restoreTransientFor(thisChain);
        thisChain.prevTransientFor = null;
        restoreTransientFor(otherChain);
        otherChain.prevTransientFor = null;
        nextTransientFor = null;

        XToolkit.XSync();
    }

    boolean isModalBlocked() {
        return modalBlocker != null;
    }

    static Window getDecoratedOwner(Window window) {
        while ((null != window) && !(window instanceof Frame || window instanceof Dialog)) {
            window = (Window) AWTAccessor.getComponentAccessor().getParent(window);
        }
        return window;
    }

    public boolean requestWindowFocus(XWindowPeer actualFocusedWindow) {
        setActualFocusedWindow(actualFocusedWindow);
        return requestWindowFocus();
    }

    public boolean requestWindowFocus() {
        return requestWindowFocus(0, false);
    }

    public boolean requestWindowFocus(long time, boolean timeProvided) {
        focusLog.fine("Request for window focus");
        Window ownerWindow  = XWindowPeer.getDecoratedOwner((Window)target);
        Window focusedWindow = XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow();
        Window activeWindow = XWindowPeer.getDecoratedOwner(focusedWindow);

        if (isWMStateNetHidden()) {
            focusLog.fine("The window is unmapped, so rejecting the request");
            return false;
        }
        if (activeWindow == ownerWindow) {
            focusLog.fine("Parent window is active - generating focus for this window");
            handleWindowFocusInSync(-1);
            return true;
        }
        focusLog.fine("Parent window is not active");

        XDecoratedPeer wpeer = AWTAccessor.getComponentAccessor().getPeer(ownerWindow);
        if (wpeer != null && wpeer.requestWindowFocus(this, time, timeProvided)) {
            focusLog.fine("Parent window accepted focus request - generating focus for this window");
            return true;
        }
        focusLog.fine("Denied - parent window is not active and didn't accept focus request");
        return false;
    }

    void setActualFocusedWindow(XWindowPeer actualFocusedWindow) {
    }

    /**
     * Applies the current window type.
     */
    private void applyWindowType() {
        XNETProtocol protocol = XWM.getWM().getNETProtocol();
        if (protocol == null) {
            return;
        }

        XAtom typeAtom = null;

        switch (getWindowType())
        {
            case NORMAL:
                typeAtom = curRealTransientFor == null ?
                               protocol.XA_NET_WM_WINDOW_TYPE_NORMAL :
                               protocol.XA_NET_WM_WINDOW_TYPE_DIALOG;
                break;
            case UTILITY:
                typeAtom = protocol.XA_NET_WM_WINDOW_TYPE_UTILITY;
                break;
            case POPUP:
                typeAtom = protocol.XA_NET_WM_WINDOW_TYPE_POPUP_MENU;
                break;
        }

        if (typeAtom != null) {
            XAtomList wtype = new XAtomList();
            wtype.add(typeAtom);
            protocol.XA_NET_WM_WINDOW_TYPE.
                setAtomListProperty(getWindow(), wtype);
        } else {
            protocol.XA_NET_WM_WINDOW_TYPE.
                DeleteProperty(getWindow());
        }
    }

    @Override
    public void xSetVisible(boolean visible) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Setting visible on " + this + " to " + visible);
        }
        XToolkit.awtLock();
        try {
            this.visible = visible;
            if (visible) {
                applyWindowType();
                XlibWrapper.XMapRaised(XToolkit.getDisplay(), getWindow());
            } else {
                XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), getWindow());
            }
            XlibWrapper.XFlush(XToolkit.getDisplay());
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    private int dropTargetCount = 0;

    public void addDropTarget() {
        XToolkit.awtLock();
        try {
            if (dropTargetCount == 0) {
                long window = getWindow();
                if (window != 0) {
                    XDropTargetRegistry.getRegistry().registerDropSite(window);
                }
            }
            dropTargetCount++;
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void removeDropTarget() {
        XToolkit.awtLock();
        try {
            dropTargetCount--;
            if (dropTargetCount == 0) {
                long window = getWindow();
                if (window != 0) {
                    XDropTargetRegistry.getRegistry().unregisterDropSite(window);
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }
    void addRootPropertyEventDispatcher() {
        if( rootPropertyEventDispatcher == null ) {
            rootPropertyEventDispatcher = new XEventDispatcher() {
                public void dispatchEvent(XEvent ev) {
                    if( ev.get_type() == XConstants.PropertyNotify ) {
                        handleRootPropertyNotify( ev );
                    }
                }
            };
            XlibWrapper.XSelectInput( XToolkit.getDisplay(),
                                      XToolkit.getDefaultRootWindow(),
                                      XConstants.PropertyChangeMask);
            XToolkit.addEventDispatcher(XToolkit.getDefaultRootWindow(),
                                                rootPropertyEventDispatcher);
        }
    }
    void removeRootPropertyEventDispatcher() {
        if( rootPropertyEventDispatcher != null ) {
            XToolkit.removeEventDispatcher(XToolkit.getDefaultRootWindow(),
                                                rootPropertyEventDispatcher);
            rootPropertyEventDispatcher = null;
        }
    }
    public void updateFocusableWindowState() {
        cachedFocusableWindow = isFocusableWindow();
    }

    XAtom XA_NET_WM_STATE;
    XAtomList net_wm_state;
    public XAtomList getNETWMState() {
        if (net_wm_state == null) {
            net_wm_state = XA_NET_WM_STATE.getAtomListPropertyList(this);
        }
        return net_wm_state;
    }

    public void setNETWMState(XAtomList state) {
        net_wm_state = state;
        if (state != null) {
            XA_NET_WM_STATE.setAtomListProperty(this, state);
        }
    }

    public PropMwmHints getMWMHints() {
        if (mwm_hints == null) {
            mwm_hints = new PropMwmHints();
            if (!XWM.XA_MWM_HINTS.getAtomData(getWindow(), mwm_hints.pData, MWMConstants.PROP_MWM_HINTS_ELEMENTS)) {
                mwm_hints.zero();
            }
        }
        return mwm_hints;
    }

    public void setMWMHints(PropMwmHints hints) {
        mwm_hints = hints;
        if (hints != null) {
            XWM.XA_MWM_HINTS.setAtomData(getWindow(), mwm_hints.pData, MWMConstants.PROP_MWM_HINTS_ELEMENTS);
        }
    }

    protected void updateDropTarget() {
        XToolkit.awtLock();
        try {
            if (dropTargetCount > 0) {
                long window = getWindow();
                if (window != 0) {
                    XDropTargetRegistry.getRegistry().unregisterDropSite(window);
                    XDropTargetRegistry.getRegistry().registerDropSite(window);
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void setGrab(boolean grab) {
        this.grab = grab;
        if (grab) {
            pressTarget = this;
            grabInput();
        } else {
            ungrabInput();
        }
    }

    public boolean isGrabbed() {
        return grab && XAwtState.getGrabWindow() == this;
    }

    public void handleXCrossingEvent(XEvent xev) {
        XCrossingEvent xce = xev.get_xcrossing();
        if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
            grabLog.fine("{0}, when grabbed {1}, contains {2}",
                         xce, isGrabbed(),
                         containsGlobal(scaleDown(xce.get_x_root()),
                                        scaleDown(xce.get_y_root())));
        }
        if (isGrabbed()) {
            XBaseWindow target = XToolkit.windowToXWindow(xce.get_window());
            if (grabLog.isLoggable(PlatformLogger.Level.FINER)) {
                grabLog.finer("  -  Grab event target {0}", target);
            }
            if (target != null && target != this) {
                target.dispatchEvent(xev);
                return;
            }
        }
        super.handleXCrossingEvent(xev);
    }

    public void handleMotionNotify(XEvent xev) {
        XMotionEvent xme = xev.get_xmotion();
        if (grabLog.isLoggable(PlatformLogger.Level.FINER)) {
            grabLog.finer("{0}, when grabbed {1}, contains {2}",
                          xme, isGrabbed(),
                          containsGlobal(scaleDown(xme.get_x_root()),
                                         scaleDown(xme.get_y_root())));
        }
        if (isGrabbed()) {
            boolean dragging = false;
            final int buttonsNumber = XToolkit.getNumberOfButtonsForMask();

            for (int i = 0; i < buttonsNumber; i++){
                if ((i != 4) && (i != 5)){
                    dragging = dragging || ((xme.get_state() & XlibUtil.getButtonMask(i + 1)) != 0);
                }
            }
            XBaseWindow target = XToolkit.windowToXWindow(xme.get_window());
            if (dragging && pressTarget != target) {
                target = pressTarget.isVisible() ? pressTarget : this;
                xme.set_window(target.getWindow());
                Point localCoord = target.toLocal(scaleDown(xme.get_x_root()),
                                                  scaleDown(xme.get_y_root()));
                xme.set_x(scaleUp(localCoord.x));
                xme.set_y(scaleUp(localCoord.y));
            }
            if (grabLog.isLoggable(PlatformLogger.Level.FINER)) {
                grabLog.finer("  -  Grab event target {0}", target);
            }
            if (target != null) {
                if (target != getContentXWindow() && target != this) {
                    target.dispatchEvent(xev);
                    return;
                }
            }

            if (!containsGlobal(scaleDown(xme.get_x_root()),
                                scaleDown(xme.get_y_root()))
                    && !dragging) {
                return;
            }
        }
        super.handleMotionNotify(xev);
    }

    private XBaseWindow pressTarget = this;

    public void handleButtonPressRelease(XEvent xev) {
        XButtonEvent xbe = xev.get_xbutton();
        /*
         * Ignore the buttons above 20 due to the bit limit for
         * InputEvent.BUTTON_DOWN_MASK.
         * One more bit is reserved for FIRST_HIGH_BIT.
         */
        if (xbe.get_button() > SunToolkit.MAX_BUTTONS_SUPPORTED) {
            return;
        }
        if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
            grabLog.fine("{0}, when grabbed {1}, contains {2} ({3}, {4}, {5}x{6})",
                         xbe, isGrabbed(),
                         containsGlobal(scaleDown(xbe.get_x_root()),
                                        scaleDown(xbe.get_y_root())),
                         getAbsoluteX(), getAbsoluteY(),
                         getWidth(), getHeight());
        }
        if (isGrabbed()) {
            XBaseWindow target = XToolkit.windowToXWindow(xbe.get_window());
            try {
                if (grabLog.isLoggable(PlatformLogger.Level.FINER)) {
                    grabLog.finer("  -  Grab event target {0} (press target {1})", target, pressTarget);
                }
                if (xbe.get_type() == XConstants.ButtonPress
                    && xbe.get_button() == XConstants.buttons[0])
                {
                    pressTarget = target;
                } else if (xbe.get_type() == XConstants.ButtonRelease
                           && xbe.get_button() == XConstants.buttons[0]
                           && pressTarget != target)
                {
                    target = pressTarget.isVisible() ? pressTarget : this;
                    xbe.set_window(target.getWindow());
                    Point localCoord = target.toLocal(scaleDown(xbe.get_x_root()),
                                                      scaleDown(xbe.get_y_root()));
                    xbe.set_x(scaleUp(localCoord.x));
                    xbe.set_y(scaleUp(localCoord.y));
                    pressTarget = this;
                }
                if (target != null && target != getContentXWindow() && target != this) {
                    target.dispatchEvent(xev);
                    return;
                }
            } finally {
                if (target != null) {
                    if ((target == this || target == getContentXWindow())
                            && !containsGlobal(scaleDown(xbe.get_x_root()),
                                               scaleDown(xbe.get_y_root())))
                    {
                        if (xbe.get_type() == XConstants.ButtonPress) {
                            if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                                grabLog.fine("Generating UngrabEvent on {0} because not inside of shell", this);
                            }
                            if ((xbe.get_button() != XConstants.buttons[3])
                                && (xbe.get_button() != XConstants.buttons[4])) {
                                postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                            }
                            return;
                        }
                    }
                    XWindowPeer toplevel = target.getToplevelXWindow();
                    if (toplevel != null) {
                        Window w = (Window)toplevel.target;
                        while (w != null && toplevel != this && !(toplevel instanceof XDialogPeer)) {
                            w = (Window) AWTAccessor.getComponentAccessor().getParent(w);
                            if (w != null) {
                                toplevel = AWTAccessor.getComponentAccessor().getPeer(w);
                            }
                        }
                        if (w == null || (w != this.target && w instanceof Dialog)) {
                            if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                                grabLog.fine("Generating UngrabEvent on {0} because hierarchy ended", this);
                            }
                            if (xbe.get_type() != XConstants.ButtonPress) {
                                postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                            } else if ((xbe.get_button() != XConstants.buttons[3])
                                   && (xbe.get_button() != XConstants.buttons[4])) {
                                postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                            }
                        }
                    } else {
                        if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                            grabLog.fine("Generating UngrabEvent on {0} because toplevel is null", this);
                        }
                        if (xbe.get_type() != XConstants.ButtonPress) {
                            postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                        } else if ((xbe.get_button() != XConstants.buttons[3])
                               && (xbe.get_button() != XConstants.buttons[4])) {
                            postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                        }
                        return;
                    }
                } else {
                    if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                        grabLog.fine("Generating UngrabEvent on because target is null {0}", this);
                    }
                    if (xbe.get_type() != XConstants.ButtonPress) {
                        postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                    } else if ((xbe.get_button() != XConstants.buttons[3])
                            && (xbe.get_button() != XConstants.buttons[4])) {
                        postEventToEventQueue(new sun.awt.UngrabEvent(getEventSource()));
                    }
                    return;
                }
            }
        }
        super.handleButtonPressRelease(xev);
    }

    public void print(Graphics g) {
        Shape shape = ((Window)target).getShape();
        if (shape != null) {
            g.setClip(shape);
        }
        super.print(g);
    }

    @Override
    public void setOpacity(float opacity) {
        final long maxOpacity = 0xffffffffl;
        long iOpacity = (long)(opacity * maxOpacity);
        if (iOpacity < 0) {
            iOpacity = 0;
        }
        if (iOpacity > maxOpacity) {
            iOpacity = maxOpacity;
        }

        XAtom netWmWindowOpacityAtom = XAtom.get("_NET_WM_WINDOW_OPACITY");

        if (iOpacity == maxOpacity) {
            netWmWindowOpacityAtom.DeleteProperty(getWindow());
        } else {
            netWmWindowOpacityAtom.setCard32Property(getWindow(), iOpacity);
        }
    }

    @Override
    public void setOpaque(boolean isOpaque) {
    }

    @Override
    public void updateWindow() {
    }
}
