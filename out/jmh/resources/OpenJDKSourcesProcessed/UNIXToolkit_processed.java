/*
 * Copyright (c) 2004, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.RenderingHints;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

import java.awt.color.ColorSpace;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import sun.awt.X11.XBaseWindow;
import sun.security.action.GetIntegerAction;
import com.sun.java.swing.plaf.gtk.GTKConstants.TextDirection;
import sun.java2d.opengl.OGLRenderQueue;
import sun.security.action.GetPropertyAction;

public abstract class UNIXToolkit extends SunToolkit
{
    /** All calls into GTK should be synchronized on this lock */
    public static final Object GTK_LOCK = new Object();

    private static final int[] BAND_OFFSETS = { 0, 1, 2 };
    private static final int[] BAND_OFFSETS_ALPHA = { 0, 1, 2, 3 };
    private static final int DEFAULT_DATATRANSFER_TIMEOUT = 10000;

    private static final String GTK2_DEPRECATION_MESSAGE =
            "WARNING: the GTK 2 library is deprecated and " +
                    "its support will be removed in a future release";
    private static volatile boolean gtk2WarningIssued = false;

    public enum GtkVersions {
        ANY(0),
        GTK2(Constants.GTK2_MAJOR_NUMBER),
        GTK3(Constants.GTK3_MAJOR_NUMBER);

        static class Constants {
            static final int GTK2_MAJOR_NUMBER = 2;
            static final int GTK3_MAJOR_NUMBER = 3;
        }

        final int number;

        GtkVersions(int number) {
            this.number = number;
        }

        public static GtkVersions getVersion(int number) {
            switch (number) {
                case Constants.GTK2_MAJOR_NUMBER:
                    return GTK2;
                case Constants.GTK3_MAJOR_NUMBER:
                    return GTK3;
                default:
                    return ANY;
            }
        }

        public int getNumber() {
            return number;
        }
    }

    private Boolean nativeGTKAvailable;
    private Boolean nativeGTKLoaded;
    private BufferedImage tmpImage = null;

    public static int getDatatransferTimeout() {
        @SuppressWarnings("removal")
        Integer dt = AccessController.doPrivileged(
                new GetIntegerAction("sun.awt.datatransfer.timeout"));
        if (dt == null || dt <= 0) {
            return DEFAULT_DATATRANSFER_TIMEOUT;
        } else {
            return dt;
        }
    }

    @Override
    public String getDesktop() {
        String gnome = "gnome";
        @SuppressWarnings("removal")
        String gsi = AccessController.doPrivileged(
                        (PrivilegedAction<String>) ()
                                -> System.getenv("GNOME_DESKTOP_SESSION_ID"));
        if (gsi != null) {
            return gnome;
        }

        @SuppressWarnings("removal")
        String desktop = AccessController.doPrivileged(
                (PrivilegedAction<String>) ()
                        -> System.getenv("XDG_CURRENT_DESKTOP"));
        return (desktop != null && desktop.toLowerCase().contains(gnome))
                ? gnome : null;
    }

    /**
     * Returns true if the native GTK libraries are capable of being
     * loaded and are expected to work properly, false otherwise.  Note
     * that this method will not leave the native GTK libraries loaded if
     * they haven't already been loaded.  This allows, for example, Swing's
     * GTK L&F to test for the presence of native GTK support without
     * leaving the native libraries loaded.  To attempt long-term loading
     * of the native GTK libraries, use the loadGTK() method instead.
     */
    @Override
    public boolean isNativeGTKAvailable() {
        synchronized (GTK_LOCK) {
            if (nativeGTKLoaded != null) {
                return nativeGTKLoaded;

            } else if (nativeGTKAvailable != null) {
                return nativeGTKAvailable;

            } else {
                boolean success = check_gtk(getEnabledGtkVersion().getNumber());
                nativeGTKAvailable = success;
                return success;
            }
        }
    }

    /**
     * Loads the GTK libraries, if necessary.  The first time this method
     * is called, it will attempt to load the native GTK library.  If
     * successful, it leaves the library open and returns true; otherwise,
     * the library is left closed and returns false.  On future calls to
     * this method, the status of the first attempt is returned (a simple
     * lightweight boolean check, no native calls required).
     */
    public boolean loadGTK() {
        synchronized (GTK_LOCK) {
            if (nativeGTKLoaded == null) {
                nativeGTKLoaded = load_gtk(getEnabledGtkVersion().getNumber(),
                                                                isGtkVerbose());
            }
        }
        return nativeGTKLoaded;
    }

    /**
     * Overridden to handle GTK icon loading
     */
    @Override
    protected Object lazilyLoadDesktopProperty(String name) {
        if (name.startsWith("gtk.icon.")) {
            return lazilyLoadGTKIcon(name);
        }
        return super.lazilyLoadDesktopProperty(name);
    }

    /**
     * Load a native Gtk stock icon.
     *
     * @param longname a desktop property name. This contains icon name, size
     *        and orientation, e.g. {@code "gtk.icon.gtk-add.4.rtl"}
     * @return an {@code Image} for the icon, or {@code null} if the
     *         icon could not be loaded
     */
    protected Object lazilyLoadGTKIcon(String longname) {
        Object result = desktopProperties.get(longname);
        if (result != null) {
            return result;
        }

        String[] str = longname.split("\\.");
        if (str.length != 5) {
            return null;
        }

        int size = 0;
        try {
            size = Integer.parseInt(str[3]);
        } catch (NumberFormatException nfe) {
            return null;
        }

        TextDirection dir = ("ltr".equals(str[4]) ? TextDirection.LTR :
                                                    TextDirection.RTL);

        BufferedImage img = getStockIcon(-1, str[2], size, dir.ordinal(), null);
        if (img != null) {
            setDesktopProperty(longname, img);
        }
        return img;
    }

    /**
     * Returns a BufferedImage which contains the Gtk icon requested.  If no
     * such icon exists or an error occurs loading the icon the result will
     * be null.
     *
     * @param filename
     * @return The icon or null if it was not found or loaded.
     */
    public BufferedImage getGTKIcon(final String filename) {
        if (!loadGTK()) {
            return null;

        } else {
            synchronized (GTK_LOCK) {
                if (!load_gtk_icon(filename)) {
                    tmpImage = null;
                }
            }
        }
        return tmpImage;
    }

    /**
     * Returns a BufferedImage which contains the Gtk stock icon requested.
     * If no such stock icon exists the result will be null.
     *
     * @param widgetType one of WidgetType values defined in GTKNativeEngine or
     * -1 for system default stock icon.
     * @param stockId String which defines the stock id of the gtk item.
     * For a complete list reference the API at www.gtk.org for StockItems.
     * @param iconSize One of the GtkIconSize values defined in GTKConstants
     * @param direction One of the TextDirection values defined in
     * GTKConstants
     * @param detail Render detail that is passed to the native engine (feel
     * free to pass null)
     * @return The stock icon or null if it was not found or loaded.
     */
    public BufferedImage getStockIcon(final int widgetType, final String stockId,
                                final int iconSize, final int direction,
                                final String detail) {
        if (!loadGTK()) {
            return null;

        } else {
            synchronized (GTK_LOCK) {
                if (!load_stock_icon(widgetType, stockId, iconSize, direction, detail)) {
                    tmpImage = null;
                }
            }
        }
        return tmpImage;  
    }

    /**
     * This method is used by JNI as a callback from load_stock_icon.
     * Image data is passed back to us via this method and loaded into the
     * local BufferedImage and then returned via getStockIcon.
     *
     * Do NOT call this method directly.
     */
    public void loadIconCallback(byte[] data, int width, int height,
            int rowStride, int bps, int channels, boolean alpha) {
        tmpImage = null;

        DataBuffer dataBuf = new DataBufferByte(data, (rowStride * height));
        WritableRaster raster = Raster.createInterleavedRaster(dataBuf,
                width, height, rowStride, channels,
                (alpha ? BAND_OFFSETS_ALPHA : BAND_OFFSETS), null);
        ColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), alpha, false,
                ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        tmpImage = new BufferedImage(colorModel, raster, false, null);
    }

    private static native boolean check_gtk(int version);
    private static native boolean load_gtk(int version, boolean verbose);
    private static native boolean unload_gtk();
    private native boolean load_gtk_icon(String filename);
    private native boolean load_stock_icon(int widget_type, String stock_id,
            int iconSize, int textDirection, String detail);

    private native void nativeSync();
    private static native int get_gtk_version();

    @Override
    public void sync() {
        nativeSync();
        OGLRenderQueue.sync();
    }

    /*
     * This returns the value for the desktop property "awt.font.desktophints"
     * It builds this by querying the Gnome desktop properties to return
     * them as platform independent hints.
     * This requires that the Gnome properties have already been gathered.
     */
    public static final String FONTCONFIGAAHINT = "fontconfig/Antialias";

    @Override
    protected RenderingHints getDesktopAAHints() {

        Object aaValue = getDesktopProperty("gnome.Xft/Antialias");

        if (aaValue == null) {
            /* On a KDE desktop running KWin the rendering hint will
             * have been set as property "fontconfig/Antialias".
             * No need to parse further in this case.
             */
            aaValue = getDesktopProperty(FONTCONFIGAAHINT);
            if (aaValue != null) {
               return new RenderingHints(KEY_TEXT_ANTIALIASING, aaValue);
            } else {
                 return null; 
            }
        }

        /* 0 means off, 1 means some ON. What would any other value mean?
         * If we require "1" to enable AA then some new value would cause
         * us to default to "OFF". I don't think that's the best guess.
         * So if its !=0 then lets assume AA.
         */
        boolean aa = ((aaValue instanceof Number)
                        && ((Number) aaValue).intValue() != 0);
        Object aaHint;
        if (aa) {
            String subpixOrder =
                (String)getDesktopProperty("gnome.Xft/RGBA");

            if (subpixOrder == null || subpixOrder.equals("none")) {
                aaHint = VALUE_TEXT_ANTIALIAS_ON;
            } else if (subpixOrder.equals("rgb")) {
                aaHint = VALUE_TEXT_ANTIALIAS_LCD_HRGB;
            } else if (subpixOrder.equals("bgr")) {
                aaHint = VALUE_TEXT_ANTIALIAS_LCD_HBGR;
            } else if (subpixOrder.equals("vrgb")) {
                aaHint = VALUE_TEXT_ANTIALIAS_LCD_VRGB;
            } else if (subpixOrder.equals("vbgr")) {
                aaHint = VALUE_TEXT_ANTIALIAS_LCD_VBGR;
            } else {
                /* didn't recognise the string, but AA is requested */
                aaHint = VALUE_TEXT_ANTIALIAS_ON;
            }
        } else {
            aaHint = VALUE_TEXT_ANTIALIAS_DEFAULT;
        }
        return new RenderingHints(KEY_TEXT_ANTIALIASING, aaHint);
    }

    private native boolean gtkCheckVersionImpl(int major, int minor,
        int micro);

    /**
     * Returns {@code true} if the GTK+ library is compatible with the given
     * version.
     *
     * @param major
     *            The required major version.
     * @param minor
     *            The required minor version.
     * @param micro
     *            The required micro version.
     * @return {@code true} if the GTK+ library is compatible with the given
     *         version.
     */
    public boolean checkGtkVersion(int major, int minor, int micro) {
        if (loadGTK()) {
            return gtkCheckVersionImpl(major, minor, micro);
        }
        return false;
    }

    public static GtkVersions getEnabledGtkVersion() {
        @SuppressWarnings("removal")
        String version = AccessController.doPrivileged(
                new GetPropertyAction("jdk.gtk.version"));
        if (version == null) {
            return GtkVersions.ANY;
        } else if (version.startsWith("2")) {
            if (!gtk2WarningIssued) {
                System.err.println(GTK2_DEPRECATION_MESSAGE);
                gtk2WarningIssued = true;
            }
            return GtkVersions.GTK2;
        } else if("3".equals(version) ){
            return GtkVersions.GTK3;
        }
        return GtkVersions.ANY;
    }

    public static GtkVersions getGtkVersion() {
        return GtkVersions.getVersion(get_gtk_version());
    }

    @SuppressWarnings("removal")
    public static boolean isGtkVerbose() {
        return AccessController.doPrivileged((PrivilegedAction<Boolean>)()
                -> Boolean.getBoolean("jdk.gtk.verbose"));
    }

    private static volatile Boolean isOnWayland = null;

    @SuppressWarnings("removal")
    public static boolean isOnWayland() {
        Boolean result = isOnWayland;
        if (result == null) {
            synchronized (GTK_LOCK) {
                result = isOnWayland;
                if (result == null) {
                    isOnWayland
                            = result
                            = AccessController.doPrivileged(
                            (PrivilegedAction<Boolean>) () -> {
                                final String display =
                                        System.getenv("WAYLAND_DISPLAY");

                                return display != null
                                        && !display.trim().isEmpty();
                            }
                    );
                }
            }
        }
        return result;
    }

    @Override
    public boolean isRunningOnWayland() {
        return isOnWayland();
    }

    private static final WindowFocusListener waylandWindowFocusListener;

    static {
        if (isOnWayland()) {
            waylandWindowFocusListener = new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    Window window = e.getWindow();
                    Window oppositeWindow = e.getOppositeWindow();

                    if (oppositeWindow != null) {
                        if (window == oppositeWindow.getParent() ) {
                            addWaylandWindowFocusListenerToWindow(oppositeWindow);
                            return;
                        }
                        if (window.getParent() == oppositeWindow) {
                            return;
                        }
                    }

                    window.removeWindowFocusListener(this);

                    XBaseWindow.ungrabInput();

                    window.dispatchEvent(new UngrabEvent(window));
                }
            };
        } else {
            waylandWindowFocusListener = null;
        }
    }

    private static void addWaylandWindowFocusListenerToWindow(Window window) {
        if (!Arrays
                .asList(window.getWindowFocusListeners())
                .contains(waylandWindowFocusListener)
        ) {
            window.addWindowFocusListener(waylandWindowFocusListener);
        }
    }

    @Override
    public void dismissPopupOnFocusLostIfNeeded(Window invoker) {
        if (!isOnWayland() || invoker == null) {
            return;
        }

        addWaylandWindowFocusListenerToWindow(invoker);
    }

    @Override
    public void dismissPopupOnFocusLostIfNeededCleanUp(Window invoker) {
        if (!isOnWayland() || invoker == null) {
            return;
        }

        invoker.removeWindowFocusListener(waylandWindowFocusListener);
        for (Window ownedWindow : invoker.getOwnedWindows()) {
            ownedWindow.removeWindowFocusListener(waylandWindowFocusListener);
        }
    }
}
