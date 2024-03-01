/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.d3d;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.util.ThreadGroupUtils;
import sun.awt.Win32GraphicsConfig;
import sun.awt.windows.WComponentPeer;
import sun.java2d.InvalidPipeException;
import sun.java2d.ScreenUpdateManager;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.windows.GDIWindowSurfaceData;
import sun.java2d.d3d.D3DSurfaceData.D3DWindowSurfaceData;
import sun.java2d.windows.WindowsFlags;

/**
 * This class handles rendering to the screen with the D3D pipeline.
 *
 * Since it is not possible to render directly to the front buffer
 * with D3D9, we create a swap chain surface (with COPY effect) in place of the
 * GDIWindowSurfaceData. A background thread handles the swap chain flips.
 *
 * There are some restrictions to which windows we would use this for.
 * @see #createScreenSurface
 */
public class D3DScreenUpdateManager extends ScreenUpdateManager
    implements Runnable
{
    /**
     * A window must be at least MIN_WIN_SIZE in one or both dimensions
     * to be considered for the update manager.
     */
    private static final int MIN_WIN_SIZE = 150;

    private volatile boolean done;
    private volatile Thread screenUpdater;
    private boolean needsUpdateNow;

    /**
     * Object used by the screen updater thread for waiting
     */
    private Object runLock = new Object();
    /**
     * List of D3DWindowSurfaceData surfaces. Surfaces are added to the
     * list when a graphics object is created, and removed when the surface
     * is invalidated.
     */
    private ArrayList<D3DWindowSurfaceData> d3dwSurfaces;
    /**
     * Cache of GDIWindowSurfaceData surfaces corresponding to the
     * D3DWindowSurfaceData surfaces. Surfaces are added to the list when
     * a d3dw surface is lost and could not be restored (due to lack of vram,
     * for example), and removed then the d3dw surface is invalidated.
     */
    private HashMap<D3DWindowSurfaceData, GDIWindowSurfaceData> gdiSurfaces;

    @SuppressWarnings("removal")
    public D3DScreenUpdateManager() {
        done = false;
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Runnable shutdownRunnable = () -> {
                done = true;
                wakeUpUpdateThread();
            };
            Thread shutdown = new Thread(
                    ThreadGroupUtils.getRootThreadGroup(), shutdownRunnable,
                    "ScreenUpdater", 0, false);
            shutdown.setContextClassLoader(null);
            try {
                Runtime.getRuntime().addShutdownHook(shutdown);
            } catch (Exception e) {
                done = true;
            }
            return null;
        });
    }

    /**
     * If possible, creates a D3DWindowSurfaceData (which is actually
     * a back-buffer surface). If the creation fails, returns GDI
     * onscreen surface instead.
     *
     * Note that the created D3D surface does not initialize the native
     * resources (and is marked lost) to avoid wasting video memory. It is
     * restored when a graphics object is requested from the peer.
     *
     * Note that this method is called from a synchronized block in
     * WComponentPeer, so we don't need to synchronize
     *
     * Note that we only create a substitute d3dw surface if certain conditions
     * are met
     * <ul>
     *  <li>the fake d3d rendering on screen is not disabled via flag
     *  <li>d3d on the device is enabled
     *  <li>surface is larger than MIN_WIN_SIZE (don't bother for smaller ones)
     *  <li>it doesn't have a backBuffer for a BufferStrategy already
     *  <li>the peer is either Canvas, Panel, Window, Frame,
     *  Dialog or EmbeddedFrame
     * </ul>
     *
     * @param gc GraphicsConfiguration on associated with the surface
     * @param peer peer for which the surface is to be created
     * @param bbNum number of back-buffers requested. if this number is >0,
     * method returns GDI surface (we don't want to have two swap chains)
     * @param isResize whether this surface is being created in response to
     * a component resize event. This determines whether a repaint event will
     * be issued after a surface is created: it will be if {@code isResize}
     * is {@code true}.
     * @return surface data to be use for onscreen rendering
     */
    @Override
    public SurfaceData createScreenSurface(Win32GraphicsConfig gc,
                                           WComponentPeer peer,
                                           int bbNum, boolean isResize)
    {
        if (done || !(gc instanceof D3DGraphicsConfig)) {
            return super.createScreenSurface(gc, peer, bbNum, isResize);
        }

        SurfaceData sd = null;

        if (canUseD3DOnScreen(peer, gc, bbNum)) {
            try {
                sd = D3DSurfaceData.createData(peer);
            }  catch (InvalidPipeException ipe) {
                sd = null;
            }
        }
        if (sd == null) {
            sd = GDIWindowSurfaceData.createData(peer);
        }

        if (isResize) {
            repaintPeerTarget(peer);
        }

        return sd;
    }

    /**
     * Determines if we can use a d3d surface for onscreen rendering for this
     * peer.
     * We only create onscreen d3d surfaces if the following conditions are met:
     *  - d3d is enabled on this device and onscreen emulation is enabled
     *  - window is big enough to bother (either dimension > MIN_WIN_SIZE)
     *  - this heavyweight doesn't have a BufferStrategy
     *  - if we are in full-screen mode then it must be the peer of the
     *    full-screen window (since there could be only one SwapChain in fs)
     *    and it must not have any heavyweight children
     *    (as Present() doesn't respect component clipping in fullscreen mode)
     *  - it's one of the classes likely to have custom rendering worth
     *    accelerating
     *
     * @return true if we can use a d3d surface for this peer's onscreen
     *         rendering
     */
    public static boolean canUseD3DOnScreen(final WComponentPeer peer,
                                            final Win32GraphicsConfig gc,
                                            final int bbNum)
    {
        if (!(gc instanceof D3DGraphicsConfig)) {
            return false;
        }
        D3DGraphicsConfig d3dgc = (D3DGraphicsConfig)gc;
        D3DGraphicsDevice d3dgd = d3dgc.getD3DDevice();
        String peerName = peer.getClass().getName();
        Rectangle r = peer.getBounds();
        Component target = (Component)peer.getTarget();
        Window fsw = d3dgd.getFullScreenWindow();

        return
            WindowsFlags.isD3DOnScreenEnabled() &&
            d3dgd.isD3DEnabledOnDevice() &&
            peer.isAccelCapable() &&
            (r.width > MIN_WIN_SIZE || r.height > MIN_WIN_SIZE) &&
            bbNum == 0 &&
            (fsw == null || (fsw == target && !hasHWChildren(target))) &&
            (peerName.equals("sun.awt.windows.WCanvasPeer") ||
             peerName.equals("sun.awt.windows.WDialogPeer") ||
             peerName.equals("sun.awt.windows.WPanelPeer")  ||
             peerName.equals("sun.awt.windows.WWindowPeer") ||
             peerName.equals("sun.awt.windows.WFramePeer")  ||
             peerName.equals("sun.awt.windows.WEmbeddedFramePeer"));
    }

    /**
     * Creates a graphics object for the passed in surface data. If
     * the surface is lost, it is restored.
     * If the surface wasn't lost or the restoration was successful
     * the surface is added to the list of maintained surfaces
     * (if it hasn't been already).
     *
     * If the updater thread hasn't been created yet , it will be created and
     * started.
     *
     * @param sd surface data for which to create SunGraphics2D
     * @param peer peer associated with the surface data
     * @param fgColor fg color to be used in graphics
     * @param bgColor bg color to be used in graphics
     * @param font font to be used in graphics
     * @return a SunGraphics2D object for the surface (or for temp GDI
     * surface data)
     */
    @Override
    public Graphics2D createGraphics(SurfaceData sd,
            WComponentPeer peer, Color fgColor, Color bgColor, Font font)
    {
        if (!done && sd instanceof D3DWindowSurfaceData) {
            D3DWindowSurfaceData d3dw = (D3DWindowSurfaceData)sd;
            if (!d3dw.isSurfaceLost() || validate(d3dw)) {
                trackScreenSurface(d3dw);
                return new SunGraphics2D(sd, fgColor, bgColor, font);
            }
            sd = getGdiSurface(d3dw);
        }
        return super.createGraphics(sd, peer, fgColor, bgColor, font);
    }

    /**
     * Posts a repaint event for the peer's target to the EDT
     * @param peer for which target's the repaint should be issued
     */
    private void repaintPeerTarget(WComponentPeer peer) {
        Component target = (Component)peer.getTarget();
        Rectangle bounds = AWTAccessor.getComponentAccessor().getBounds(target);
        peer.handlePaint(0, 0, bounds.width, bounds.height);
    }

    /**
     * Adds a surface to the list of tracked surfaces.
     *
     * @param sd the surface to be added
     */
    private void trackScreenSurface(SurfaceData sd) {
        if (!done && sd instanceof D3DWindowSurfaceData) {
            synchronized (this) {
                if (d3dwSurfaces == null) {
                    d3dwSurfaces = new ArrayList<D3DWindowSurfaceData>();
                }
                D3DWindowSurfaceData d3dw = (D3DWindowSurfaceData)sd;
                if (!d3dwSurfaces.contains(d3dw)) {
                    d3dwSurfaces.add(d3dw);
                }
            }
            startUpdateThread();
        }
    }

    @Override
    public synchronized void dropScreenSurface(SurfaceData sd) {
        if (d3dwSurfaces != null && sd instanceof D3DWindowSurfaceData) {
            D3DWindowSurfaceData d3dw = (D3DWindowSurfaceData)sd;
            removeGdiSurface(d3dw);
            d3dwSurfaces.remove(d3dw);
        }
    }

    @Override
    public SurfaceData getReplacementScreenSurface(WComponentPeer peer,
                                                   SurfaceData sd)
    {
        SurfaceData newSurface = super.getReplacementScreenSurface(peer, sd);
        trackScreenSurface(newSurface);
        return newSurface;
    }

    /**
     * Remove the gdi surface corresponding to the passed d3dw surface
     * from list of the cached gdi surfaces.
     *
     * @param d3dw surface for which associated gdi surface is to be removed
     */
    private void removeGdiSurface(final D3DWindowSurfaceData d3dw) {
        if (gdiSurfaces != null) {
            GDIWindowSurfaceData gdisd = gdiSurfaces.get(d3dw);
            if (gdisd != null) {
                gdisd.invalidate();
                gdiSurfaces.remove(d3dw);
            }
        }
    }

    /**
     * If the update thread hasn't yet been created, it will be;
     * otherwise it is awaken
     */
    @SuppressWarnings("removal")
    private synchronized void startUpdateThread() {
        if (screenUpdater == null) {
            screenUpdater = AccessController.doPrivileged((PrivilegedAction<Thread>) () -> {
                String name = "D3D Screen Updater";
                Thread t = new Thread(
                        ThreadGroupUtils.getRootThreadGroup(), this, name,
                        0, false);
                t.setPriority(Thread.NORM_PRIORITY + 2);
                t.setDaemon(true);
                return t;
            });
            screenUpdater.start();
        } else {
            wakeUpUpdateThread();
        }
    }

    /**
     * Wakes up the screen updater thread.
     *
     * This method is not synchronous, it doesn't wait
     * for the updater thread to complete the updates.
     *
     * It should be used when it is not necessary to wait for the
     * completion, for example, when a new surface had been added
     * to the list of tracked surfaces (which means that it's about
     * to be rendered to).
     */
    public void wakeUpUpdateThread() {
        synchronized (runLock) {
            runLock.notifyAll();
        }
    }

    /**
     * Wakes up the screen updater thread and waits for the completion
     * of the update.
     *
     * This method is called from Toolkit.sync() or
     * when there was a copy from a VI to the screen
     * so that swing applications would not appear to be
     * sluggish.
     */
    public void runUpdateNow() {
        synchronized (this) {
            if (done || screenUpdater == null ||
                d3dwSurfaces  == null || d3dwSurfaces.size() == 0)
            {
                return;
            }
        }
        synchronized (runLock) {
            needsUpdateNow = true;
            runLock.notifyAll();
            while (needsUpdateNow) {
                try {
                    runLock.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

    public void run() {
        while (!done) {
            synchronized (runLock) {
                long timeout = d3dwSurfaces.size() > 0 ? 100 : 0;

                if (!needsUpdateNow) {
                    try { runLock.wait(timeout); }
                        catch (InterruptedException e) {}
                }
            }

            D3DWindowSurfaceData[] surfaces = new D3DWindowSurfaceData[] {};
            synchronized (this) {
                surfaces = d3dwSurfaces.toArray(surfaces);
            }
            for (D3DWindowSurfaceData sd : surfaces) {
                if (sd.isValid() && (sd.isDirty() || sd.isSurfaceLost())) {
                    if (!sd.isSurfaceLost()) {
                        D3DRenderQueue rq = D3DRenderQueue.getInstance();
                        rq.lock();
                        try {
                            Rectangle r = sd.getBounds();
                            D3DSurfaceData.swapBuffers(sd, 0, 0,
                                                       r.width, r.height);
                            sd.markClean();
                        } finally {
                            rq.unlock();
                        }
                    } else if (!validate(sd)) {
                        sd.getPeer().replaceSurfaceDataLater();
                    }
                }
            }
            synchronized (runLock) {
                needsUpdateNow = false;
                runLock.notifyAll();
            }
        }
    }

    /**
     * Restores the passed surface if it was lost, resets the lost status.
     * @param sd surface to be validated
     * @return true if surface wasn't lost or if restoration was successful,
     * false otherwise
     */
    private boolean validate(D3DWindowSurfaceData sd) {
        if (sd.isSurfaceLost()) {
            try {
                sd.restoreSurface();
                Color bg = sd.getPeer().getBackgroundNoSync();
                SunGraphics2D sg2d = new SunGraphics2D(sd, bg, bg, null);
                sg2d.fillRect(0, 0, sd.getBounds().width, sd.getBounds().height);
                sg2d.dispose();
                sd.markClean();
                repaintPeerTarget(sd.getPeer());
            } catch (InvalidPipeException ipe) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates (or returns a cached one) gdi surface for the same peer as
     * the passed d3dw surface has.
     *
     * @param d3dw surface used as key into the cache
     * @return gdi window surface associated with the d3d window surfaces' peer
     */
    private synchronized SurfaceData getGdiSurface(D3DWindowSurfaceData d3dw) {
        if (gdiSurfaces == null) {
            gdiSurfaces =
                new HashMap<D3DWindowSurfaceData, GDIWindowSurfaceData>();
        }
        GDIWindowSurfaceData gdisd = gdiSurfaces.get(d3dw);
        if (gdisd == null) {
            gdisd = GDIWindowSurfaceData.createData(d3dw.getPeer());
            gdiSurfaces.put(d3dw, gdisd);
        }
        return gdisd;
    }

    /**
     * Returns true if the component has heavyweight children.
     *
     * @param comp component to check for hw children
     * @return true if Component has heavyweight children
     */
    private static boolean hasHWChildren(Component comp) {
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        if (comp instanceof Container) {
            for (Component c : ((Container)comp).getComponents()) {
                if (acc.getPeer(c) instanceof WComponentPeer || hasHWChildren(c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
