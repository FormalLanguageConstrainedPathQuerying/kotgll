/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.AWTPermission;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;
import java.awt.peer.WindowPeer;
import java.util.ArrayList;

import sun.awt.windows.WWindowPeer;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.opengl.WGLGraphicsConfig;
import sun.java2d.windows.WindowsFlags;

import static java.awt.peer.ComponentPeer.SET_BOUNDS;

import static sun.awt.Win32GraphicsEnvironment.debugScaleX;
import static sun.awt.Win32GraphicsEnvironment.debugScaleY;

/**
 * This is an implementation of a GraphicsDevice object for a single
 * Win32 screen.
 *
 * @see GraphicsEnvironment
 * @see GraphicsConfiguration
 */
public class Win32GraphicsDevice extends GraphicsDevice implements
 DisplayChangedListener {
    int screen;
    ColorModel dynamicColorModel;   
    ColorModel colorModel;          
    protected GraphicsConfiguration[] configs;
    protected GraphicsConfiguration defaultConfig;

    private final String idString;
    protected String descString;
    private boolean valid;

    private SunDisplayChanger topLevels = new SunDisplayChanger();
    protected static boolean pfDisabled;
    private static AWTPermission fullScreenExclusivePermission;
    private DisplayMode defaultDisplayMode;
    private WindowListener fsWindowListener;

    private float scaleX;
    private float scaleY;

    static {

        @SuppressWarnings("removal")
        String nopixfmt = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("sun.awt.nopixfmt"));
        pfDisabled = (nopixfmt != null);
        initIDs();
    }

    private static native void initIDs();

    native void initDevice(int screen);
    native void initNativeScale(int screen);
    native void setNativeScale(int screen, float scaleX, float scaleY);
    native float getNativeScaleX(int screen);
    native float getNativeScaleY(int screen);

    public Win32GraphicsDevice(int screennum) {
        this.screen = screennum;
        idString = "\\Display"+screen;
        descString = "Win32GraphicsDevice[screen=" + screen;
        valid = true;

        initDevice(screennum);
        initScaleFactors();
    }

    /**
     * Returns the type of the graphics device.
     * @see #TYPE_RASTER_SCREEN
     * @see #TYPE_PRINTER
     * @see #TYPE_IMAGE_BUFFER
     */
    @Override
    public int getType() {
        return TYPE_RASTER_SCREEN;
    }

    /**
     * Returns the Win32 screen of the device.
     */
    public int getScreen() {
        return screen;
    }

    public float getDefaultScaleX() {
        return scaleX;
    }

    public float getDefaultScaleY() {
        return scaleY;
    }

    private void initScaleFactors() {
        if (SunGraphicsEnvironment.isUIScaleEnabled()) {
            if (debugScaleX > 0 && debugScaleY > 0) {
                scaleX = debugScaleX;
                scaleY = debugScaleY;
                setNativeScale(screen, scaleX, scaleY);
            } else {
                initNativeScale(screen);
                scaleX = getNativeScaleX(screen);
                scaleY = getNativeScaleY(screen);
            }
        } else {
            scaleX = 1;
            scaleY = 1;
        }
    }

    /**
     * Returns whether this is a valid device. Device can become
     * invalid as a result of device removal event.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Called from native code when the device was removed.
     *
     * @param defaultScreen the current default screen
     */
    protected void invalidate(int defaultScreen) {
        valid = false;
        screen = defaultScreen;
    }

    /**
     * Returns the identification string associated with this graphics
     * device.
     */
    @Override
    public String getIDstring() {
        return idString;
    }


    /**
     * Returns all of the graphics
     * configurations associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        if (configs==null) {
            if (WindowsFlags.isOGLEnabled() && isDefaultDevice()) {
                defaultConfig = getDefaultConfiguration();
                if (defaultConfig != null) {
                    configs = new GraphicsConfiguration[1];
                    configs[0] = defaultConfig;
                    return configs.clone();
                }
            }

            int max = getMaxConfigs(screen);
            int defaultPixID = getDefaultPixID(screen);
            ArrayList<GraphicsConfiguration> v = new ArrayList<>( max );
            if (defaultPixID == 0) {
                defaultConfig = Win32GraphicsConfig.getConfig(this,
                                                              defaultPixID);
                v.add(defaultConfig);
            }
            else {
                for (int i = 1; i <= max; i++) {
                    if (isPixFmtSupported(i, screen)) {
                        if (i == defaultPixID) {
                            defaultConfig = Win32GraphicsConfig.getConfig(
                             this, i);
                            v.add(defaultConfig);
                        }
                        else {
                            v.add(Win32GraphicsConfig.getConfig(
                             this, i));
                        }
                    }
                }
            }
            configs = v.toArray(new GraphicsConfiguration[0]);
        }
        return configs.clone();
    }

    /**
     * Returns the maximum number of graphics configurations available, or 1
     * if PixelFormat calls fail or are disabled.
     * This number is less than or equal to the number of graphics
     * configurations supported.
     */
    protected int getMaxConfigs(int screen) {
        if (pfDisabled) {
            return 1;
        } else {
            return getMaxConfigsImpl(screen);
        }
    }

    private native int getMaxConfigsImpl(int screen);

    /**
     * Returns whether or not the PixelFormat indicated by index is
     * supported.  Supported PixelFormats support drawing to a Window
     * (PFD_DRAW_TO_WINDOW), support GDI (PFD_SUPPORT_GDI), and in the
     * case of an 8-bit format (cColorBits <= 8) uses indexed colors
     * (iPixelType == PFD_TYPE_COLORINDEX).
     * We use the index 0 to indicate that PixelFormat calls don't work, or
     * are disabled.  Do not call this function with an index of 0.
     * @param index a PixelFormat index
     */
    private native boolean isPixFmtSupported(int index, int screen);

    /**
     * Returns the PixelFormatID of the default graphics configuration
     * associated with this graphics device, or 0 if PixelFormats calls fail or
     * are disabled.
     */
    protected int getDefaultPixID(int screen) {
        if (pfDisabled) {
            return 0;
        } else {
            return getDefaultPixIDImpl(screen);
        }
    }

    /**
     * Returns the default PixelFormat ID from GDI.  Do not call if PixelFormats
     * are disabled.
     */
    private native int getDefaultPixIDImpl(int screen);

    /**
     * Returns the default graphics configuration
     * associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        if (defaultConfig == null) {
            if (WindowsFlags.isOGLEnabled() && isDefaultDevice()) {
                int defPixID = WGLGraphicsConfig.getDefaultPixFmt(screen);
                defaultConfig = WGLGraphicsConfig.getConfig(this, defPixID);
                if (WindowsFlags.isOGLVerbose()) {
                    if (defaultConfig != null) {
                        System.out.print("OpenGL pipeline enabled");
                    } else {
                        System.out.print("Could not enable OpenGL pipeline");
                    }
                    System.out.println(" for default config on screen " +
                                       screen);
                }
            }

            if (defaultConfig == null) {
                defaultConfig = Win32GraphicsConfig.getConfig(this, 0);
            }
        }
        return defaultConfig;
    }

    @Override
    public String toString() {
        return valid ? descString + "]" : descString + ", removed]";
    }

    /**
     * Returns true if this is the default GraphicsDevice for the
     * GraphicsEnvironment.
     */
    private boolean isDefaultDevice() {
        return (this ==
                GraphicsEnvironment.
                    getLocalGraphicsEnvironment().getDefaultScreenDevice());
    }

    private static boolean isFSExclusiveModeAllowed() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            if (fullScreenExclusivePermission == null) {
                fullScreenExclusivePermission =
                    new AWTPermission("fullScreenExclusive");
            }
            try {
                security.checkPermission(fullScreenExclusivePermission);
            } catch (SecurityException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * returns true unless we're not allowed to use fullscreen mode.
     */
    @Override
    public boolean isFullScreenSupported() {
        return isFSExclusiveModeAllowed();
    }

    @Override
    public synchronized void setFullScreenWindow(Window w) {
        Window old = getFullScreenWindow();
        if (w == old) {
            return;
        }
        if (!isFullScreenSupported()) {
            super.setFullScreenWindow(w);
            return;
        }

        if (old != null) {
            if (defaultDisplayMode != null) {
                setDisplayMode(defaultDisplayMode);
                defaultDisplayMode = null;
            }
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(old);
            if (peer != null) {
                peer.setFullScreenExclusiveModeState(false);
                synchronized(peer) {
                    exitFullScreenExclusive(screen, peer);
                }
            }
            removeFSWindowListener(old);
        }
        super.setFullScreenWindow(w);
        if (w != null) {
            defaultDisplayMode = getDisplayMode();
            addFSWindowListener(w);
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
            if (peer != null) {
                synchronized(peer) {
                    enterFullScreenExclusive(screen, peer);
                }
                peer.setFullScreenExclusiveModeState(true);
            }

            peer.updateGC();
        }
    }

    protected native void enterFullScreenExclusive(int screen, WindowPeer w);
    protected native void exitFullScreenExclusive(int screen, WindowPeer w);

    /**
     * Reapplies the size of this graphics device to
     * the given full-screen window.
     * @param w a Window that needs resizing
     * @param b new full-screen window bounds
     */
    private static void resizeFSWindow(final Window w, final Rectangle b) {
        if (w != null) {
            WindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
            if (peer != null) {
                peer.setBounds(b.x, b.y, b.width, b.height, SET_BOUNDS);
            }
        }
    }

    @Override
    public boolean isDisplayChangeSupported() {
        return (isFullScreenSupported() && getFullScreenWindow() != null);
    }

    @Override
    public synchronized void setDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            super.setDisplayMode(dm);
            return;
        }
        if (dm == null || (dm = getMatchingDisplayMode(dm)) == null) {
            throw new IllegalArgumentException("Invalid display mode");
        }
        if (getDisplayMode().equals(dm)) {
            return;
        }
        Window w = getFullScreenWindow();
        if (w != null) {
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
            configDisplayMode(screen, peer, dm.getWidth(), dm.getHeight(),
                dm.getBitDepth(), dm.getRefreshRate());
        } else {
            throw new IllegalStateException("Must be in fullscreen mode " +
                                            "in order to set display mode");
        }
    }

    protected native DisplayMode getCurrentDisplayMode(int screen);
    protected native void configDisplayMode(int screen, WindowPeer w, int width,
                                          int height, int bitDepth,
                                          int refreshRate);
    protected native void enumDisplayModes(int screen, ArrayList<DisplayMode> modes);

    @Override
    public synchronized DisplayMode getDisplayMode() {
        DisplayMode res = getCurrentDisplayMode(screen);
        return res;
    }

    @Override
    public synchronized DisplayMode[] getDisplayModes() {
        ArrayList<DisplayMode> modes = new ArrayList<>();
        enumDisplayModes(screen, modes);
        int listSize = modes.size();
        DisplayMode[] retArray = new DisplayMode[listSize];
        for (int i = 0; i < listSize; i++) {
            retArray[i] = modes.get(i);
        }
        return retArray;
    }

    protected synchronized DisplayMode getMatchingDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            return null;
        }
        DisplayMode[] modes = getDisplayModes();
        for (DisplayMode mode : modes) {
            if (dm.equals(mode) ||
                (dm.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN &&
                 dm.getWidth() == mode.getWidth() &&
                 dm.getHeight() == mode.getHeight() &&
                 dm.getBitDepth() == mode.getBitDepth()))
            {
                return mode;
            }
        }
        return null;
    }

    /*
     * From the DisplayChangeListener interface.
     * Called from Win32GraphicsEnvironment when the display settings have
     * changed.
     */
    @Override
    public void displayChanged() {
        dynamicColorModel = null;
        defaultConfig = null;
        configs = null;
        initScaleFactors();

        Rectangle screenBounds = getDefaultConfiguration().getBounds();
        resizeFSWindow(getFullScreenWindow(), screenBounds);

        topLevels.notifyListeners();
    }

    /**
     * Part of the DisplayChangedListener interface: devices
     * do not need to react to this event
     */
    @Override
    public void paletteChanged() {
    }

    /*
     * Add a DisplayChangeListener to be notified when the display settings
     * are changed.  Typically, only top-level containers need to be added
     * to Win32GraphicsDevice.
     */
    public void addDisplayChangedListener(DisplayChangedListener client) {
        topLevels.add(client);
    }

    /*
     * Remove a DisplayChangeListener from this Win32GraphicsDevice
     */
     public void removeDisplayChangedListener(DisplayChangedListener client) {
        topLevels.remove(client);
    }

    /**
     * Creates and returns the color model associated with this device
     */
    private native ColorModel makeColorModel (int screen,
                                              boolean dynamic);

    /**
     * Returns a dynamic ColorModel which is updated when there
     * are any changes (e.g., palette changes) in the device
     */
    public ColorModel getDynamicColorModel() {
        if (dynamicColorModel == null) {
            dynamicColorModel = makeColorModel(screen, true);
        }
        return dynamicColorModel;
    }

    /**
     * Returns the non-dynamic ColorModel associated with this device
     */
    public ColorModel getColorModel() {
        if (colorModel == null)  {
            colorModel = makeColorModel(screen, false);
        }
        return colorModel;
    }

    /**
     * WindowAdapter class responsible for de/iconifying full-screen window
     * of this device.
     *
     * The listener restores the default display mode when window is iconified
     * and sets it back to the one set by the user on de-iconification.
     */
    private static class Win32FSWindowAdapter extends WindowAdapter {
        private Win32GraphicsDevice device;
        private DisplayMode dm;

        Win32FSWindowAdapter(Win32GraphicsDevice device) {
            this.device = device;
        }

        private void setFSWindowsState(Window other, int state) {
            GraphicsDevice[] gds =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getScreenDevices();
            if (other != null) {
                for (GraphicsDevice gd : gds) {
                    if (other == gd.getFullScreenWindow()) {
                        return;
                    }
                }
            }
            for (GraphicsDevice gd : gds) {
                Window fsw = gd.getFullScreenWindow();
                if (fsw instanceof Frame) {
                    ((Frame)fsw).setExtendedState(state);
                }
            }
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            setFSWindowsState(e.getOppositeWindow(), Frame.ICONIFIED);
        }

        @Override
        public void windowActivated(WindowEvent e) {
            setFSWindowsState(e.getOppositeWindow(), Frame.NORMAL);
        }

        @Override
        public void windowIconified(WindowEvent e) {
            DisplayMode ddm = device.defaultDisplayMode;
            if (ddm != null) {
                dm = device.getDisplayMode();
                device.setDisplayMode(ddm);
            }
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            if (dm != null) {
                device.setDisplayMode(dm);
                dm = null;
            }
        }
    }

    /**
     * Adds a WindowListener to be used as
     * activation/deactivation listener for the current full-screen window.
     *
     * @param w full-screen window
     */
    protected void addFSWindowListener(final Window w) {
        fsWindowListener = new Win32FSWindowAdapter(this);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                w.addWindowListener(fsWindowListener);
            }
        });
    }

    /**
     * Removes the fs window listener.
     *
     * @param w full-screen window
     */
    protected void removeFSWindowListener(Window w) {
        w.removeWindowListener(fsWindowListener);
        fsWindowListener = null;
    }
}
