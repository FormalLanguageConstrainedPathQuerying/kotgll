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

package sun.lwawt;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.peer.DropTargetPeer;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.PaintEvent;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import com.sun.java.swing.SwingUtilities3;
import sun.awt.AWTAccessor;
import sun.awt.CGraphicsDevice;
import sun.awt.PaintEventDispatcher;
import sun.awt.RepaintArea;
import sun.awt.SunToolkit;
import sun.awt.event.IgnorePaintEvent;
import sun.awt.image.SunVolatileImage;
import sun.java2d.SunGraphics2D;
import sun.java2d.metal.MTLRenderQueue;
import sun.java2d.opengl.OGLRenderQueue;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.RenderQueue;
import sun.util.logging.PlatformLogger;

public abstract class LWComponentPeer<T extends Component, D extends JComponent>
    implements ComponentPeer, DropTargetPeer
{
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.lwawt.focus.LWComponentPeer");

    /**
     * State lock is to be used for modifications to this peer's fields (e.g.
     * bounds, background, font, etc.) It should be the last lock in the lock
     * chain
     */
    private final Object stateLock = new Object();

    /**
     * The lock to operate with the peers hierarchy. AWT tree lock is not used
     * as there are many peers related ops to be done on the toolkit thread, and
     * we don't want to depend on a public lock on this thread
     */
    private static final Object peerTreeLock = new Object();

    /**
     * The associated AWT object.
     */
    private final T target;

    /**
     * Container peer. It may not be the peer of the target's direct parent, for
     * example, in the case of hw/lw mixing. However, let's skip this scenario
     * for the time being. We also assume the container peer is not null, which
     * might also be false if addNotify() is called for a component outside of
     * the hierarchy. The exception is LWWindowPeers: their containers are
     * always null
     */
    private final LWContainerPeer<?, ?> containerPeer;

    /**
     * Handy reference to the top-level window peer. Window peer is borrowed
     * from the containerPeer in constructor, and should also be updated when
     * the component is reparented to another container
     */
    private final LWWindowPeer windowPeer;

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    private final Rectangle bounds = new Rectangle();
    private Region region;

    private boolean visible = false;
    private boolean enabled = true;

    private Color background;
    private Color foreground;
    private Font font;

    /**
     * Paint area to coalesce all the paint events and store the target dirty
     * area.
     */
    private final RepaintArea targetPaintArea;

    private volatile boolean isLayouting;

    private final D delegate;
    private Container delegateContainer;
    private Component delegateDropTarget;
    private final Object dropTargetLock = new Object();

    private int fNumDropTargets = 0;
    private PlatformDropTarget fDropTarget = null;

    private final PlatformComponent platformComponent;

    /**
     * Character with reasonable value between the minimum width and maximum.
     */
    static final char WIDE_CHAR = '0';

    /**
     * The back buffer provide user with a BufferStrategy.
     */
    private Image backBuffer;

    /**
     * All Swing delegates use delegateContainer as a parent. This container
     * intentionally do not use parent of the peer.
     */
    @SuppressWarnings("serial")
    private final class DelegateContainer extends Container {
        {
            enableEvents(0xFFFFFFFF);
        }

        @Override
        public boolean isLightweight() {
            return false;
        }

        @Override
        public Point getLocation() {
            return getLocationOnScreen();
        }

        @Override
        public Point getLocationOnScreen() {
            return LWComponentPeer.this.getLocationOnScreen();
        }

        @Override
        public int getX() {
            return getLocation().x;
        }

        @Override
        public int getY() {
            return getLocation().y;
        }
    }

    LWComponentPeer(final T target, final PlatformComponent platformComponent) {
        targetPaintArea = new LWRepaintArea();
        this.target = target;
        this.platformComponent = platformComponent;

        final Container container = SunToolkit.getNativeContainer(target);
        containerPeer = (LWContainerPeer) LWToolkit.targetToPeer(container);
        windowPeer = containerPeer != null ? containerPeer.getWindowPeerOrSelf()
                                           : null;
        if (containerPeer != null) {
            containerPeer.addChildPeer(this);
        }

        AWTEventListener toolkitListener = null;
        synchronized (Toolkit.getDefaultToolkit()) {
            try {
                toolkitListener = getToolkitAWTEventListener();
                setToolkitAWTEventListener(null);

                synchronized (getDelegateLock()) {
                    delegate = createDelegate();
                    if (delegate != null) {
                        delegate.setVisible(false);
                        delegateContainer = new DelegateContainer();
                        delegateContainer.add(delegate);
                        delegateContainer.addNotify();
                        delegate.addNotify();
                        resetColorsAndFont(delegate);
                        delegate.setOpaque(true);
                    } else {
                        return;
                    }
                }

            } finally {
                setToolkitAWTEventListener(toolkitListener);
            }

            SwingUtilities3.setDelegateRepaintManager(delegate, new RepaintManager() {
                @Override
                public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
                    repaintPeer(SwingUtilities.convertRectangle(
                            c, new Rectangle(x, y, w, h), getDelegate()));
                }
            });
        }
    }

    /**
     * This method must be called under Toolkit.getDefaultToolkit() lock
     * and followed by setToolkitAWTEventListener()
     */
    @SuppressWarnings("removal")
    protected final AWTEventListener getToolkitAWTEventListener() {
        return AccessController.doPrivileged(new PrivilegedAction<AWTEventListener>() {
            public AWTEventListener run() {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                try {
                    Field field = Toolkit.class.getDeclaredField("eventListener");
                    field.setAccessible(true);
                    return (AWTEventListener) field.get(toolkit);
                } catch (Exception e) {
                    throw new InternalError(e.toString());
                }
            }
        });
    }

    @SuppressWarnings("removal")
    protected final void setToolkitAWTEventListener(final AWTEventListener listener) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                try {
                    Field field = Toolkit.class.getDeclaredField("eventListener");
                    field.setAccessible(true);
                    field.set(toolkit, listener);
                } catch (Exception e) {
                    throw new InternalError(e.toString());
                }
                return null;
            }
        });
    }

    /**
     * This method is called under getDelegateLock().
     * Overridden in subclasses.
     */
    D createDelegate() {
        return null;
    }

    final D getDelegate() {
        return delegate;
    }

    /**
     * This method should be called under getDelegateLock().
     */
    Component getDelegateFocusOwner() {
        return getDelegate();
    }

    /**
     * Initializes this peer. The call to initialize() is not placed to
     * LWComponentPeer ctor to let the subclass ctor to finish completely first.
     * Instead, it's the LWToolkit object who is responsible for initialization.
     * Note that we call setVisible() at the end of initialization.
     */
    public final void initialize() {
        platformComponent.initialize(getPlatformWindow());
        initializeImpl();
        setVisible(target.isVisible());
    }

    /**
     * Fetching general properties from the target. Should be overridden in
     * subclasses to initialize specific peers properties.
     */
    void initializeImpl() {
        setBackground(target.getBackground());
        setForeground(target.getForeground());
        setFont(target.getFont());
        setBounds(target.getBounds());
        setEnabled(target.isEnabled());
    }

    private static void resetColorsAndFont(final Container c) {
        c.setBackground(null);
        c.setForeground(null);
        c.setFont(null);
        for (int i = 0; i < c.getComponentCount(); i++) {
            resetColorsAndFont((Container) c.getComponent(i));
        }
    }

    final Object getStateLock() {
        return stateLock;
    }

    /**
     * Synchronize all operations with the Swing delegates under AWT tree lock,
     * using a new separate lock to synchronize access to delegates may lead
     * deadlocks. Think of it as a 'virtual EDT'.
     *
     * @return DelegateLock
     */
    final Object getDelegateLock() {
        return getTarget().getTreeLock();
    }

    protected static final Object getPeerTreeLock() {
        return peerTreeLock;
    }

    public final T getTarget() {
        return target;
    }

    protected final LWWindowPeer getWindowPeer() {
        return windowPeer;
    }

    protected LWWindowPeer getWindowPeerOrSelf() {
        return getWindowPeer();
    }

    protected final LWContainerPeer<?, ?> getContainerPeer() {
        return containerPeer;
    }

    public PlatformWindow getPlatformWindow() {
        LWWindowPeer windowPeer = getWindowPeer();
        return windowPeer.getPlatformWindow();
    }


    public LWToolkit getLWToolkit() {
        return LWToolkit.getLWToolkit();
    }

    @Override
    public final void dispose() {
        if (disposed.compareAndSet(false, true)) {
            disposeImpl();
        }
    }

    protected void disposeImpl() {
        destroyBuffers();
        LWContainerPeer<?, ?> cp = getContainerPeer();
        if (cp != null) {
            cp.removeChildPeer(this);
        }
        platformComponent.dispose();
        LWToolkit.targetDisposedPeer(getTarget(), this);
    }

    public final boolean isDisposed() {
        return disposed.get();
    }

    /*
     * GraphicsConfiguration is borrowed from the parent peer. The
     * return value must not be null.
     *
     * Overridden in LWWindowPeer.
     */
    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        return getWindowPeer().getGraphicsConfiguration();
    }


    public final LWGraphicsConfig getLWGC() {
        return (LWGraphicsConfig) getGraphicsConfiguration();
    }

    /*
     * Overridden in LWWindowPeer to replace its surface
     * data and back buffer.
     */
    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        return false;
    }

    @Override
    public Graphics getGraphics() {
        final Graphics g = getOnscreenGraphics();
        if (g != null) {
            synchronized (getPeerTreeLock()){
                applyConstrain(g);
            }
        }
        return g;
    }

    /*
     * Peer Graphics is borrowed from the parent peer, while
     * foreground and background colors and font are specific to
     * this peer.
     */
    public final Graphics getOnscreenGraphics() {
        final LWWindowPeer wp = getWindowPeerOrSelf();
        return wp.getOnscreenGraphics(getForeground(), getBackground(),
                                      getFont());

    }

    private void applyConstrain(final Graphics g) {
        final SunGraphics2D sg2d = (SunGraphics2D) g;
        final Rectangle size = localToWindow(getSize());
        sg2d.constrain(size.x, size.y, size.width, size.height, getVisibleRegion());
    }

    Region getVisibleRegion() {
        return computeVisibleRect(this, getRegion());
    }

    static final Region computeVisibleRect(final LWComponentPeer<?, ?> c,
                                           Region region) {
        final LWContainerPeer<?, ?> p = c.getContainerPeer();
        if (p != null) {
            final Rectangle r = c.getBounds();
            region = region.getTranslatedRegion(r.x, r.y);
            region = region.getIntersection(p.getRegion());
            region = region.getIntersection(p.getContentSize());
            region = p.cutChildren(region, c);
            region = computeVisibleRect(p, region);
            region = region.getTranslatedRegion(-r.x, -r.y);
        }
        return region;
    }

    @Override
    public ColorModel getColorModel() {
        return getGraphicsConfiguration().getColorModel();
    }

    public boolean isTranslucent() {
        return false;
    }

    @Override
    public final void createBuffers(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        getLWGC().assertOperationSupported(numBuffers, caps);
        final Image buffer = getLWGC().createBackBuffer(this);
        synchronized (getStateLock()) {
            backBuffer = buffer;
        }
    }

    @Override
    public final Image getBackBuffer() {
        synchronized (getStateLock()) {
            if (backBuffer != null) {
                return backBuffer;
            }
        }
        throw new IllegalStateException("Buffers have not been created");
    }

    @Override
    public final void flip(int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction) {
        getLWGC().flip(this, getBackBuffer(), x1, y1, x2, y2, flipAction);
    }

    @Override
    public final void destroyBuffers() {
        final Image oldBB;
        synchronized (getStateLock()) {
            oldBB = backBuffer;
            backBuffer = null;
        }
        getLWGC().destroyBackBuffer(oldBB);
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height, SET_BOUNDS);
    }

    /**
     * This method could be called on the toolkit thread.
     */
    @Override
    public void setBounds(int x, int y, int w, int h, int op) {
        setBounds(x, y, w, h, op, true, false);
    }

    protected void setBounds(int x, int y, int w, int h, int op, boolean notify,
                             final boolean updateTarget) {
        Rectangle oldBounds;
        synchronized (getStateLock()) {
            oldBounds = new Rectangle(bounds);
            if ((op & (SET_LOCATION | SET_BOUNDS)) != 0) {
                bounds.x = x;
                bounds.y = y;
            }
            if ((op & (SET_SIZE | SET_BOUNDS)) != 0) {
                bounds.width = w;
                bounds.height = h;
            }
        }
        boolean moved = (oldBounds.x != x) || (oldBounds.y != y);
        boolean resized = (oldBounds.width != w) || (oldBounds.height != h);
        if (!moved && !resized) {
            return;
        }
        final D delegate = getDelegate();
        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegateContainer.setBounds(0, 0, w, h);
                delegate.setBounds(delegateContainer.getBounds());
                delegate.validate();
            }
        }

        final Point locationInWindow = localToWindow(0, 0);
        platformComponent.setBounds(locationInWindow.x, locationInWindow.y, w,
                                    h);
        if (notify) {
            repaintOldNewBounds(oldBounds);
            if (resized) {
                handleResize(w, h, updateTarget);
            }
            if (moved) {
                handleMove(x, y, updateTarget);
            }
        }
    }

    public final Rectangle getBounds() {
        synchronized (getStateLock()) {
            return bounds.getBounds();
        }
    }

    public final Rectangle getSize() {
        synchronized (getStateLock()) {
            return new Rectangle(bounds.width, bounds.height);
        }
    }

    @Override
    public Point getLocationOnScreen() {
        Point windowLocation = getWindowPeer().getLocationOnScreen();
        Point locationInWindow = localToWindow(0, 0);
        return new Point(windowLocation.x + locationInWindow.x,
                windowLocation.y + locationInWindow.y);
    }

    /**
     * Returns the cursor of the peer, which is cursor of the target by default,
     * but peer can override this behavior.
     *
     * @param p Point relative to the peer.
     * @return Cursor of the peer or null if default cursor should be used.
     */
    Cursor getCursor(final Point p) {
        return getTarget().getCursor();
    }

    @Override
    public void setBackground(final Color c) {
        final Color oldBg = getBackground();
        if (oldBg == c || (oldBg != null && oldBg.equals(c))) {
            return;
        }
        synchronized (getStateLock()) {
            background = c;
        }
        final D delegate = getDelegate();
        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegate.setBackground(c);
            }
        } else {
            repaintPeer();
        }
    }

    public final Color getBackground() {
        synchronized (getStateLock()) {
            return background;
        }
    }

    @Override
    public void setForeground(final Color c) {
        final Color oldFg = getForeground();
        if (oldFg == c || (oldFg != null && oldFg.equals(c))) {
            return;
        }
        synchronized (getStateLock()) {
            foreground = c;
        }
        final D delegate = getDelegate();
        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegate.setForeground(c);
            }
        } else {
            repaintPeer();
        }
    }

    protected final Color getForeground() {
        synchronized (getStateLock()) {
            return foreground;
        }
    }

    @Override
    public void setFont(final Font f) {
        final Font oldF = getFont();
        if (oldF == f || (oldF != null && oldF.equals(f))) {
            return;
        }
        synchronized (getStateLock()) {
            font = f;
        }
        final D delegate = getDelegate();
        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegate.setFont(f);
            }
        } else {
            repaintPeer();
        }
    }

    protected final Font getFont() {
        synchronized (getStateLock()) {
            return font;
        }
    }

    @Override
    public FontMetrics getFontMetrics(final Font f) {
        final Graphics g = getOnscreenGraphics();
        if (g != null) {
            try {
                return g.getFontMetrics(f);
            } finally {
                g.dispose();
            }
        }
        synchronized (getDelegateLock()) {
            return delegateContainer.getFontMetrics(f);
        }
    }

    @Override
    public void setEnabled(final boolean e) {
        boolean status = e;
        final LWComponentPeer<?, ?> cp = getContainerPeer();
        if (cp != null) {
            status &= cp.isEnabled();
        }
        synchronized (getStateLock()) {
            if (enabled == status) {
                return;
            }
            enabled = status;
        }

        final D delegate = getDelegate();

        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegate.setEnabled(status);
            }
        } else {
            repaintPeer();
        }
    }

    public final boolean isEnabled() {
        synchronized (getStateLock()) {
            return enabled;
        }
    }

    @Override
    public void setVisible(final boolean v) {
        synchronized (getStateLock()) {
            if (visible == v) {
                return;
            }
            visible = v;
        }
        setVisibleImpl(v);
    }

    protected void setVisibleImpl(final boolean v) {
        final D delegate = getDelegate();

        if (delegate != null) {
            synchronized (getDelegateLock()) {
                delegate.setVisible(v);
            }
        }
        if (visible) {
            repaintPeer();
        } else {
            repaintParent(getBounds());
        }
    }

    public final boolean isVisible() {
        synchronized (getStateLock()) {
            return visible;
        }
    }

    @Override
    public void paint(final Graphics g) {
        getTarget().paint(g);
    }

    @Override
    public void print(final Graphics g) {
        getTarget().print(g);
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
        throw new UnsupportedOperationException("ComponentPeer.reparent()");
    }

    @Override
    public boolean isReparentSupported() {
        return false;
    }

    @Override
    public void setZOrder(final ComponentPeer above) {
        LWContainerPeer<?, ?> cp = getContainerPeer();
        cp.setChildPeerZOrder(this, (LWComponentPeer<?, ?>) above);
    }

    @Override
    public void coalescePaintEvent(PaintEvent e) {
        if (!(e instanceof IgnorePaintEvent)) {
            Rectangle r = e.getUpdateRect();
            if ((r != null) && !r.isEmpty()) {
                targetPaintArea.add(r, e.getID());
            }
        }
    }

    /*
     * Should be overridden in subclasses which use complex Swing components.
     */
    @Override
    public void layout() {
    }

    @Override
    public boolean isObscured() {
        return false;
    }

    @Override
    public boolean canDetermineObscurity() {
        return false;
    }

    /**
     * Determines the preferred size of the component. By default forwards the
     * request to the Swing helper component. Should be overridden in subclasses
     * if required.
     */
    @Override
    public Dimension getPreferredSize() {
        final Dimension size;
        synchronized (getDelegateLock()) {
            size = getDelegate().getPreferredSize();
        }
        return validateSize(size);
    }

    /**
     * Determines the minimum size of the component. By default forwards the
     * request to the Swing helper component. Should be overridden in subclasses
     * if required.
     */
    @Override
    public Dimension getMinimumSize() {
        final Dimension size;
        synchronized (getDelegateLock()) {
            size = getDelegate().getMinimumSize();
        }
        return validateSize(size);
    }

    /**
     * In some situations delegates can return empty minimum/preferred size.
     * (For example: empty JLabel, etc), but awt components never should be
     * empty. In the XPeers or WPeers we use some magic constants, but here we
     * try to use something more useful,
     */
    private Dimension validateSize(final Dimension size) {
        if (size.width == 0 || size.height == 0) {
            final FontMetrics fm = getFontMetrics(getFont());
            size.width = fm.charWidth(WIDE_CHAR);
            size.height = fm.getHeight();
        }
        return size;
    }

    @Override
    public void updateCursorImmediately() {
        getLWToolkit().getCursorManager().updateCursor();
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public boolean requestFocus(Component lightweightChild, boolean temporary,
                                boolean focusedWindowChangeAllowed, long time,
                                FocusEvent.Cause cause)
    {
        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest("lightweightChild=" + lightweightChild + ", temporary=" + temporary +
                            ", focusedWindowChangeAllowed=" + focusedWindowChangeAllowed +
                            ", time= " + time + ", cause=" + cause);
        }
        if (LWKeyboardFocusManagerPeer.processSynchronousLightweightTransfer(
                getTarget(), lightweightChild, temporary,
                focusedWindowChangeAllowed, time)) {
            return true;
        }

        int result = LWKeyboardFocusManagerPeer.shouldNativelyFocusHeavyweight(
                getTarget(), lightweightChild, temporary,
                focusedWindowChangeAllowed, time, cause);
        switch (result) {
            case LWKeyboardFocusManagerPeer.SNFH_FAILURE:
                return false;
            case LWKeyboardFocusManagerPeer.SNFH_SUCCESS_PROCEED:
                Window parentWindow = SunToolkit.getContainingWindow(getTarget());
                if (parentWindow == null) {
                    focusLog.fine("request rejected, parentWindow is null");
                    LWKeyboardFocusManagerPeer.removeLastFocusRequest(getTarget());
                    return false;
                }
                final LWWindowPeer parentPeer =
                        AWTAccessor.getComponentAccessor()
                                   .getPeer(parentWindow);
                if (parentPeer == null) {
                    focusLog.fine("request rejected, parentPeer is null");
                    LWKeyboardFocusManagerPeer.removeLastFocusRequest(getTarget());
                    return false;
                }

                if (!focusedWindowChangeAllowed) {
                    LWWindowPeer decoratedPeer = parentPeer.isSimpleWindow() ?
                        LWWindowPeer.getOwnerFrameDialog(parentPeer) : parentPeer;

                    if (decoratedPeer == null || !decoratedPeer.getPlatformWindow().isActive()) {
                        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                            focusLog.fine("request rejected, focusedWindowChangeAllowed==false, " +
                                          "decoratedPeer is inactive: " + decoratedPeer);
                        }
                        LWKeyboardFocusManagerPeer.removeLastFocusRequest(getTarget());
                        return false;
                    }
                }

                boolean res = parentPeer.requestWindowFocus(cause);
                if (!res || !parentWindow.isFocused()) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                        focusLog.fine("request rejected, res= " + res + ", parentWindow.isFocused()=" +
                                      parentWindow.isFocused());
                    }
                    LWKeyboardFocusManagerPeer.removeLastFocusRequest(getTarget());
                    return false;
                }

                KeyboardFocusManagerPeer kfmPeer = LWKeyboardFocusManagerPeer.getInstance();
                Component focusOwner = kfmPeer.getCurrentFocusOwner();
                return LWKeyboardFocusManagerPeer.deliverFocus(lightweightChild,
                        getTarget(), temporary,
                        focusedWindowChangeAllowed,
                        time, cause, focusOwner);

            case LWKeyboardFocusManagerPeer.SNFH_SUCCESS_HANDLED:
                return true;
        }

        return false;
    }

    @Override
    public final Image createImage(final int width, final int height) {
        return getLWGC().createAcceleratedImage(getTarget(), width, height);
    }

    @Override
    public final VolatileImage createVolatileImage(final int w, final int h) {
        return new SunVolatileImage(getTarget(), w, h);
    }

    @Override
    public boolean handlesWheelScrolling() {
        return false;
    }

    @Override
    public final void applyShape(final Region shape) {
        synchronized (getStateLock()) {
            if (region == shape || (region != null && region.equals(shape))) {
                return;
            }
        }
        applyShapeImpl(shape);
    }

    void applyShapeImpl(final Region shape) {
        synchronized (getStateLock()) {
            if (shape != null) {
                region = Region.WHOLE_REGION.getIntersection(shape);
            } else {
                region = null;
            }
        }
        repaintParent(getBounds());
    }

    protected final Region getRegion() {
        synchronized (getStateLock()) {
            return isShaped() ? region : Region.getInstance(getSize());
        }
    }

    public boolean isShaped() {
        synchronized (getStateLock()) {
            return region != null;
        }
    }

    @Override
    public void addDropTarget(DropTarget dt) {
        LWWindowPeer winPeer = getWindowPeerOrSelf();
        if (winPeer != null && winPeer != this) {
            winPeer.addDropTarget(dt);
        } else {
            synchronized (dropTargetLock) {
                if (++fNumDropTargets == 1) {
                    if (fDropTarget != null) {
                        throw new IllegalStateException("Current drop target is not null");
                    }
                    fDropTarget = LWToolkit.getLWToolkit().createDropTarget(dt, target, this);
                }
            }
        }
    }

    @Override
    public void removeDropTarget(DropTarget dt) {
        LWWindowPeer winPeer = getWindowPeerOrSelf();
        if (winPeer != null && winPeer != this) {
            winPeer.removeDropTarget(dt);
        } else {
            synchronized (dropTargetLock){
                if (--fNumDropTargets == 0) {
                    if (fDropTarget != null) {
                        fDropTarget.dispose();
                        fDropTarget = null;
                    } else
                        System.err.println("CComponent.removeDropTarget(): current drop target is null.");
                }
            }
        }
    }


    /**
     * Called when this peer's location has been changed either as a result
     * of target.setLocation() or as a result of user actions (window is
     * dragged with mouse).
     *
     * This method could be called on the toolkit thread.
     */
    protected final void handleMove(final int x, final int y,
                                    final boolean updateTarget) {
        if (updateTarget) {
            AWTAccessor.getComponentAccessor().setLocation(getTarget(), x, y);
            postEvent(new ComponentEvent(getTarget(),
                                         ComponentEvent.COMPONENT_MOVED));
        }
    }

    /**
     * Called when this peer's size has been changed either as a result of
     * target.setSize() or as a result of user actions (window is resized).
     *
     * This method could be called on the toolkit thread.
     */
    protected final void handleResize(final int w, final int h,
                                      final boolean updateTarget) {
        Image oldBB = null;
        synchronized (getStateLock()) {
            if (backBuffer != null) {
                oldBB = backBuffer;
                backBuffer = getLWGC().createBackBuffer(this);
            }
        }
        getLWGC().destroyBackBuffer(oldBB);

        if (updateTarget) {
            AWTAccessor.getComponentAccessor().setSize(getTarget(), w, h);
            postEvent(new ComponentEvent(getTarget(),
                                         ComponentEvent.COMPONENT_RESIZED));
        }
    }

    protected final void repaintOldNewBounds(final Rectangle oldB) {
        repaintParent(oldB);
        repaintPeer(getSize());
    }

    protected final void repaintParent(final Rectangle oldB) {
        final LWContainerPeer<?, ?> cp = getContainerPeer();
        if (cp != null) {
            cp.repaintPeer(cp.getContentSize().intersection(oldB));
        }
    }


    /**
     * Post an event to the proper Java EDT.
     */
    public void postEvent(final AWTEvent event) {
        LWToolkit.postEvent(event);
    }

    protected void postPaintEvent(int x, int y, int w, int h) {
        if (AWTAccessor.getComponentAccessor().getIgnoreRepaint(target)) {
            return;
        }
        PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().
                createPaintEvent(getTarget(), x, y, w, h);
        if (event != null) {
            postEvent(event);
        }
    }

    /*
     * Gives a chance for the peer to handle the event after it's been
     * processed by the target.
     */
    @Override
    public void handleEvent(AWTEvent e) {
        if ((e instanceof InputEvent) && ((InputEvent) e).isConsumed()) {
            return;
        }
        switch (e.getID()) {
            case FocusEvent.FOCUS_GAINED:
            case FocusEvent.FOCUS_LOST:
                handleJavaFocusEvent((FocusEvent) e);
                break;
            case PaintEvent.PAINT:
            case PaintEvent.UPDATE:
                handleJavaPaintEvent();
                break;
            case MouseEvent.MOUSE_PRESSED:
                handleJavaMouseEvent((MouseEvent)e);
        }

        sendEventToDelegate(e);
    }

    protected void sendEventToDelegate(final AWTEvent e) {
        if (getDelegate() == null || !isShowing() || !isEnabled()) {
            return;
        }
        synchronized (getDelegateLock()) {
            AWTEvent delegateEvent = createDelegateEvent(e);
            if (delegateEvent != null) {
                AWTAccessor.getComponentAccessor()
                        .processEvent((Component) delegateEvent.getSource(),
                                delegateEvent);
                if (delegateEvent instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) delegateEvent;
                    SwingUtilities.processKeyBindings(ke);
                }
            }
        }
    }

    /**
     * Changes the target of the AWTEvent from awt component to appropriate
     * swing delegate.
     */
    @SuppressWarnings("deprecation")
    private AWTEvent createDelegateEvent(final AWTEvent e) {
        AWTEvent delegateEvent = null;
        if (e instanceof MouseWheelEvent) {
            MouseWheelEvent me = (MouseWheelEvent) e;
            delegateEvent = new MouseWheelEvent(
                    delegate, me.getID(), me.getWhen(),
                    me.getModifiers(),
                    me.getX(), me.getY(),
                    me.getXOnScreen(), me.getYOnScreen(),
                    me.getClickCount(),
                    me.isPopupTrigger(),
                    me.getScrollType(),
                    me.getScrollAmount(),
                    me.getWheelRotation(),
                    me.getPreciseWheelRotation());
        } else if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;

            Component eventTarget = SwingUtilities.getDeepestComponentAt(delegate, me.getX(), me.getY());

            if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
                if (delegateDropTarget == null) {
                    delegateDropTarget = eventTarget;
                } else {
                    eventTarget = delegateDropTarget;
                }
            }
            if (me.getID() == MouseEvent.MOUSE_RELEASED && delegateDropTarget != null) {
                eventTarget = delegateDropTarget;
                delegateDropTarget = null;
            }
            if (eventTarget == null) {
                eventTarget = delegate;
            }
            delegateEvent = SwingUtilities.convertMouseEvent(getTarget(), me, eventTarget);
        } else if (e instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) e;
            delegateEvent = new KeyEvent(getDelegateFocusOwner(), ke.getID(), ke.getWhen(),
                    ke.getModifiers(), ke.getKeyCode(), ke.getKeyChar(), ke.getKeyLocation());
            AWTAccessor.getKeyEventAccessor().setExtendedKeyCode((KeyEvent) delegateEvent,
                    ke.getExtendedKeyCode());
        } else if (e instanceof FocusEvent) {
            FocusEvent fe = (FocusEvent) e;
            delegateEvent = new FocusEvent(getDelegateFocusOwner(), fe.getID(), fe.isTemporary());
        }
        return delegateEvent;
    }

    protected void handleJavaMouseEvent(MouseEvent e) {
        Component target = getTarget();
        assert (e.getSource() == target);

        if (!target.isFocusOwner() && LWKeyboardFocusManagerPeer.shouldFocusOnClick(target)) {
            LWKeyboardFocusManagerPeer.requestFocusFor(target, FocusEvent.Cause.MOUSE_EVENT);
        }
    }

    /**
     * Handler for FocusEvents.
     */
    void handleJavaFocusEvent(final FocusEvent e) {
        KeyboardFocusManagerPeer kfmPeer = LWKeyboardFocusManagerPeer.getInstance();
        kfmPeer.setCurrentFocusOwner(e.getID() == FocusEvent.FOCUS_GAINED ? getTarget() : null);
    }

    /**
     * All peers should clear background before paint.
     *
     * @return false on components that DO NOT require a clearRect() before
     *         painting.
     */
    protected final boolean shouldClearRectBeforePaint() {
        return true;
    }

    /**
     * Handler for PAINT and UPDATE PaintEvents.
     */
    private void handleJavaPaintEvent() {
        if (!isLayouting()) {
            targetPaintArea.paint(getTarget(), shouldClearRectBeforePaint());
        }
    }


    /**
     * Finds a top-most visible component for the given point. The location is
     * specified relative to the peer's parent.
     */
    LWComponentPeer<?, ?> findPeerAt(final int x, final int y) {
        final Rectangle r = getBounds();
        final Region sh = getRegion();
        final boolean found = isVisible() && sh.contains(x - r.x, y - r.y);
        return found ? this : null;
    }

    /*
     * Translated the given point in Window coordinates to the point in
     * coordinates local to this component. The given window peer must be
     * the window where this component is in.
     */
    public Point windowToLocal(int x, int y, LWWindowPeer wp) {
        return windowToLocal(new Point(x, y), wp);
    }

    public Point windowToLocal(Point p, LWWindowPeer wp) {
        LWComponentPeer<?, ?> cp = this;
        while (cp != wp) {
            Rectangle cpb = cp.getBounds();
            p.x -= cpb.x;
            p.y -= cpb.y;
            cp = cp.getContainerPeer();
        }
        return new Point(p);
    }

    public Rectangle windowToLocal(Rectangle r, LWWindowPeer wp) {
        Point p = windowToLocal(r.getLocation(), wp);
        return new Rectangle(p, r.getSize());
    }

    public Point localToWindow(int x, int y) {
        return localToWindow(new Point(x, y));
    }

    public Point localToWindow(Point p) {
        LWComponentPeer<?, ?> cp = getContainerPeer();
        Rectangle r = getBounds();
        while (cp != null) {
            p.x += r.x;
            p.y += r.y;
            r = cp.getBounds();
            cp = cp.getContainerPeer();
        }
        return new Point(p);
    }

    public Rectangle localToWindow(Rectangle r) {
        Point p = localToWindow(r.getLocation());
        return new Rectangle(p, r.getSize());
    }

    public final void repaintPeer() {
        repaintPeer(getSize());
    }

    void repaintPeer(final Rectangle r) {
        final Rectangle toPaint = getSize().intersection(r);
        if (!isShowing() || toPaint.isEmpty()) {
            return;
        }

        postPaintEvent(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
    }

    /**
     * Determines whether this peer is showing on screen. This means that the
     * peer must be visible, and it must be in a container that is visible and
     * showing.
     *
     * @see #isVisible()
     */
    protected final boolean isShowing() {
        synchronized (getPeerTreeLock()) {
            if (isVisible()) {
                final LWContainerPeer<?, ?> container = getContainerPeer();
                return (container == null) || container.isShowing();
            }
        }
        return false;
    }

    /**
     * Paints the peer. Delegate the actual painting to Swing components.
     */
    protected final void paintPeer(final Graphics g) {
        final D delegate = getDelegate();
        if (delegate != null) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new InternalError("Painting must be done on EDT");
            }
            synchronized (getDelegateLock()) {
                getDelegate().print(g);
            }
        }
    }

    protected static final void flushOnscreenGraphics(){
        RenderQueue rq =  CGraphicsDevice.usingMetalPipeline() ?
                MTLRenderQueue.getInstance() : OGLRenderQueue.getInstance();
        rq.lock();
        try {
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    /**
     * Used by ContainerPeer to skip all the paint events during layout.
     *
     * @param isLayouting layouting state.
     */
    protected final void setLayouting(final boolean isLayouting) {
        this.isLayouting = isLayouting;
    }

    /**
     * Returns layouting state. Used by ComponentPeer to skip all the paint
     * events during layout.
     *
     * @return true during layout, false otherwise.
     */
    private boolean isLayouting() {
        return isLayouting;
    }
}
