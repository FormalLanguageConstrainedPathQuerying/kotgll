/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

import java.lang.ref.WeakReference;
import java.awt.Image;
import java.awt.image.ImageObserver;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class ImageWatched {
    public static Link endlink = new Link();

    public Link watcherList;

    public ImageWatched() {
        watcherList = endlink;
    }

    /*
     * This class defines a node on a linked list of ImageObservers.
     * The base class defines the dummy implementation used for the
     * last link on all chains and a subsequent subclass then
     * defines the standard implementation that manages a weak
     * reference to a real ImageObserver.
     */
    public static class Link {
        /*
         * Check if iw is the referent of this Link or any
         * subsequent Link objects on this chain.
         */
        public boolean isWatcher(ImageObserver iw) {
            return false;  
        }

        /*
         * Remove this Link from the chain if its referent
         * is the indicated target or if it has been nulled
         * out by the garbage collector.
         * Return the new remainder of the chain.
         * The argument may be null which will trigger
         * the chain to remove only the dead (null) links.
         * This method is only ever called inside a
         * synchronized block so Link.next modifications
         * will be safe.
         */
        public Link removeWatcher(ImageObserver iw) {
            return this;  
        }

        /*
         * Deliver the indicated image update information
         * to the referent of this Link and return a boolean
         * indicating whether or not some referent became
         * null or has indicated a lack of interest in
         * further updates to its imageUpdate() method.
         * This method is not called inside a synchronized
         * block so Link.next modifications are not safe.
         */
        public boolean newInfo(Image img, int info,
                               int x, int y, int w, int h)
        {
            return false;  
        }
    }

    static class AccWeakReference<T> extends WeakReference<T> {

         @SuppressWarnings("removal")
         private final AccessControlContext acc;

         @SuppressWarnings("removal")
         AccWeakReference(T ref) {
             super(ref);
             acc = AccessController.getContext();
         }
    }

    /*
     * Standard Link implementation to manage a Weak Reference
     * to an ImageObserver.
     */
    public static class WeakLink extends Link {
        private final AccWeakReference<ImageObserver> myref;
        private Link next;

        public WeakLink(ImageObserver obs, Link next) {
            myref = new AccWeakReference<ImageObserver>(obs);
            this.next = next;
        }

        public boolean isWatcher(ImageObserver iw) {
            return (myref.get() == iw || next.isWatcher(iw));
        }

        public Link removeWatcher(ImageObserver iw) {
            ImageObserver myiw = myref.get();
            if (myiw == null) {
                return next.removeWatcher(iw);
            }
            if (myiw == iw) {
                return next;
            }
            next = next.removeWatcher(iw);
            return this;
        }

        @SuppressWarnings("removal")
        private static boolean update(ImageObserver iw, AccessControlContext acc,
                                      Image img, int info,
                                      int x, int y, int w, int h) {

            if (acc != null || System.getSecurityManager() != null) {
                return AccessController.doPrivileged(
                       (PrivilegedAction<Boolean>) () -> {
                            return iw.imageUpdate(img, info, x, y, w, h);
                      }, acc);
            }
            return false;
        }

        public boolean newInfo(Image img, int info,
                               int x, int y, int w, int h)
        {
            boolean ret = next.newInfo(img, info, x, y, w, h);
            ImageObserver myiw = myref.get();
            if (myiw == null) {
                ret = true;
            } else if (update(myiw, myref.acc, img, info, x, y, w, h) == false) {
                myref.clear();
                ret = true;
            }
            return ret;
        }
    }

    public synchronized void addWatcher(ImageObserver iw) {
        if (iw != null && !isWatcher(iw)) {
            watcherList = new WeakLink(iw, watcherList);
        }
        watcherList = watcherList.removeWatcher(null);
    }

    public synchronized boolean isWatcher(ImageObserver iw) {
        return watcherList.isWatcher(iw);
    }

    public void removeWatcher(ImageObserver iw) {
        synchronized (this) {
            watcherList = watcherList.removeWatcher(iw);
        }
        if (watcherList == endlink) {
            notifyWatcherListEmpty();
        }
    }

    public boolean isWatcherListEmpty() {
        synchronized (this) {
            watcherList = watcherList.removeWatcher(null);
        }
        return (watcherList == endlink);
    }

    public void newInfo(Image img, int info, int x, int y, int w, int h) {
        if (watcherList.newInfo(img, info, x, y, w, h)) {
            removeWatcher(null);
        }
    }

    protected abstract void notifyWatcherListEmpty();
}
