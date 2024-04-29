/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

/**
 * This class implements the ShapeIterator interface for a Region.
 * This is useful as the source iterator of a device clip region
 * (in its native guise), and also as the result of clipping a
 * Region to a rectangle.
 */
public class RegionSpanIterator implements SpanIterator {
    RegionIterator ri;

    int lox, loy, hix, hiy;

    int curloy, curhiy;

    boolean done = false;

    boolean isrect;

    /**
     * Constructs an instance based on the given Region
     */
    public RegionSpanIterator(Region r) {
        int[] bounds = new int[4];

        r.getBounds(bounds);
        lox = bounds[0];
        loy = bounds[1];
        hix = bounds[2];
        hiy = bounds[3];
        isrect = r.isRectangular();

        ri = r.getIterator();
    }

    /**
     * Gets the bbox of the available region spans.
     */
    public void getPathBox(int[] pathbox) {
        pathbox[0] = lox;
        pathbox[1] = loy;
        pathbox[2] = hix;
        pathbox[3] = hiy;
    }

    /**
     * Intersect the box used for clipping the output spans with the
     * given box.
     */
    public void intersectClipBox(int clox, int cloy, int chix, int chiy) {
        if (clox > lox) {
            lox = clox;
        }
        if (cloy > loy) {
            loy = cloy;
        }
        if (chix < hix) {
            hix = chix;
        }
        if (chiy < hiy) {
            hiy = chiy;
        }
        done = lox >= hix || loy >= hiy;
    }

    /**
     * Fetches the next span that needs to be operated on.
     * If the return value is false then there are no more spans.
     */
    public boolean nextSpan(int[] spanbox) {

        if (done) {
            return false;
        }

        if (isrect) {
            getPathBox(spanbox);
            done = true;
            return true;
        }

        int curlox, curhix;
        int curloy = this.curloy;
        int curhiy = this.curhiy;

        while (true) {
            if (!ri.nextXBand(spanbox)) {
                if (!ri.nextYRange(spanbox)) {
                    done = true;
                    return false;
                }
                curloy = spanbox[1];
                curhiy = spanbox[3];
                if (curloy < loy) {
                    curloy = loy;
                }
                if (curhiy > hiy) {
                    curhiy = hiy;
                }
                if (curloy >= hiy) {
                    done = true;
                    return false;
                }
                continue;
            }
            curlox = spanbox[0];
            curhix = spanbox[2];
            if (curlox < lox) {
                curlox = lox;
            }
            if (curhix > hix) {
                curhix = hix;
            }
            if (curlox < curhix && curloy < curhiy) {
                break;
            }
        }

        spanbox[0] = curlox;
        spanbox[1] = this.curloy = curloy;
        spanbox[2] = curhix;
        spanbox[3] = this.curhiy = curhiy;
        return true;
    }

    /**
     * This method tells the iterator that it may skip all spans
     * whose Y range is completely above the indicated Y coordinate.
     */
    public void skipDownTo(int y) {
        loy = y;
    }

    /**
     * This method returns a native pointer to a function block that
     * can be used by a native method to perform the same iteration
     * cycle that the above methods provide while avoiding upcalls to
     * the Java object.
     * The definition of the structure whose pointer is returned by
     * this method is defined in:
     * <pre>
     *     src/share/native/sun/java2d/pipe/SpanIterator.h
     * </pre>
     */
    public long getNativeIterator() {
        return 0;
    }
}
