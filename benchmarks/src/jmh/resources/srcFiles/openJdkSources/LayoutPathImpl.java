/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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
/*
 * (C) Copyright IBM Corp. 2005, All Rights Reserved.
 */
package sun.font;


import java.awt.Shape;
import java.awt.font.LayoutPath;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Formatter;
import java.util.ArrayList;

import static java.awt.geom.PathIterator.*;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

public abstract class LayoutPathImpl extends LayoutPath {


    public Point2D pointToPath(double x, double y) {
        Point2D.Double pt = new Point2D.Double(x, y);
        pointToPath(pt, pt);
        return pt;
    }

    public Point2D pathToPoint(double a, double o, boolean preceding) {
        Point2D.Double pt = new Point2D.Double(a, o);
        pathToPoint(pt, preceding, pt);
        return pt;
    }

    public void pointToPath(double x, double y, Point2D pt) {
        pt.setLocation(x, y);
        pointToPath(pt, pt);
    }

    public void pathToPoint(double a, double o, boolean preceding, Point2D pt) {
        pt.setLocation(a, o);
        pathToPoint(pt, preceding, pt);
    }


    public abstract double start();
    public abstract double end();
    public abstract double length();
    public abstract Shape mapShape(Shape s);


    private static final boolean LOGMAP = false;
    private static final Formatter LOG = new Formatter(System.out);

    /**
     * Indicate how positions past the start and limit of the
     * path are treated.  PINNED adjusts these positions so
     * as to be within start and limit.  EXTENDED ignores the
     * start and limit and effectively extends the first and
     * last segments of the path 'infinitely'.  CLOSED wraps
     * positions around the ends of the path.
     */
    public static enum EndType {
        PINNED, EXTENDED, CLOSED;
        public boolean isPinned() { return this == PINNED; }
        public boolean isExtended() { return this == EXTENDED; }
        public boolean isClosed() { return this == CLOSED; }
    }


    /**
     * Return a path representing the path from the origin through the points in order.
     */
    public static LayoutPathImpl getPath(EndType etype, double ... coords) {
        if ((coords.length & 0x1) != 0) {
            throw new IllegalArgumentException("odd number of points not allowed");
        }

        return SegmentPath.get(etype, coords);
    }

    /**
     * Use to build a SegmentPath.  This takes the data and preanalyzes it for
     * information that the SegmentPath needs, then constructs a SegmentPath
     * from that.  Mainly, this lets SegmentPath cache the lengths along
     * the path to each line segment, and so avoid calculating them over and over.
     */
    public static final class SegmentPathBuilder {
        private double[] data;
        private int w;
        private double px;
        private double py;
        private double a;
        private boolean pconnect;

        /**
         * Construct a SegmentPathBuilder.
         */
        public SegmentPathBuilder() {
        }

        /**
         * Reset the builder for a new path.  Datalen is a hint of how many
         * points will be in the path, and the working buffer will be sized
         * to accommodate at least this number of points.  If datalen is zero,
         * the working buffer is freed (it will be allocated on first use).
         */
        public void reset(int datalen) {
            if (data == null || datalen > data.length) {
                data = new double[datalen];
            } else if (datalen == 0) {
                data = null;
            }
            w = 0;
            px = py = 0;
            pconnect = false;
        }

        /**
         * Automatically build from a list of points represented by pairs of
         * doubles.  Initial advance is zero.
         */
        public SegmentPath build(EndType etype, double... pts) {
            assert(pts.length % 2 == 0);

            reset(pts.length / 2 * 3);

            for (int i = 0; i < pts.length; i += 2) {
                nextPoint(pts[i], pts[i+1], i != 0);
            }

            return complete(etype);
        }

        /**
         * Move to a new point.  If there is no data, this will become the
         * first point.  If there is data, and the previous call was a lineTo, this
         * point is checked against the previous point, and if different, this
         * starts a new segment at the same advance as the end of the last
         * segment.  If there is data, and the previous call was a moveTo, this
         * replaces the point used for that previous call.
         *
         * Calling this is optional, lineTo will suffice and the initial point
         * will be set to 0, 0.
         */
        public void moveTo(double x, double y) {
            nextPoint(x, y, false);
        }

        /**
         * Connect to a new point.  If there is no data, the previous point
         * is presumed to be 0, 0.  This point is checked against
         * the previous point, and if different, this point is added to
         * the path and the advance extended.  If this point is the same as the
         * previous point, the path remains unchanged.
         */
        public void lineTo(double x, double y) {
            nextPoint(x, y, true);
        }

        /**
         * Add a new point, and increment advance if connect is true.
         *
         * This automatically rejects duplicate points and multiple disconnected points.
         */
        private void nextPoint(double x, double y, boolean connect) {

            if (x == px && y == py) {
                return;
            }

            if (w == 0) { 
                if (data == null) {
                    data = new double[6];
                }
                if (connect) {
                    w = 3; 
                }
            }

            if (w != 0 && !connect && !pconnect) {
                data[w-3] = px = x;
                data[w-2] = py = y;
                return;
            }

            if (w == data.length) {
                double[] t = new double[w * 2];
                System.arraycopy(data, 0, t, 0, w);
                data = t;
            }

            if (connect) {
                double dx = x - px;
                double dy = y - py;
                a += sqrt(dx * dx + dy * dy);
            }

            data[w++] = x;
            data[w++] = y;
            data[w++] = a;

            px = x;
            py = y;
            pconnect = connect;
        }

        public SegmentPath complete() {
            return complete(EndType.EXTENDED);
        }

        /**
         * Complete building a SegmentPath.  Once this is called, the builder is restored
         * to its initial state and information about the previous path is released.  The
         * end type indicates whether to treat the path as closed, extended, or pinned.
         */
        public SegmentPath complete(EndType etype) {
            SegmentPath result;

            if (data == null || w < 6) {
                return null;
            }

            if (w == data.length) {
                result = new SegmentPath(data, etype);
                reset(0); 
            } else {
                double[] dataToAdopt = new double[w];
                System.arraycopy(data, 0, dataToAdopt, 0, w);
                result = new SegmentPath(dataToAdopt, etype);
                reset(2); 
            }

            return result;
        }
    }

    /**
     * Represents a path built from segments.  Each segment is
     * represented by a triple: x, y, and cumulative advance.
     * These represent the end point of the segment.  The start
     * point of the first segment is represented by the triple
     * at position 0.
     *
     * The path might have breaks in it, e.g. it is not connected.
     * These will be represented by pairs of triplets that share the
     * same advance.
     *
     * The path might be extended, pinned, or closed.  If extended,
     * the initial and final segments are considered to extend
     * 'indefinitely' past the bounds of the advance.  If pinned,
     * they end at the bounds of the advance.  If closed,
     * advances before the start or after the end 'wrap around' the
     * path.
     *
     * The start of the path is the initial triple.  This provides
     * the nominal advance at the given x, y position (typically
     * zero).  The end of the path is the final triple.  This provides
     * the advance at the end, the total length of the path is
     * thus the ending advance minus the starting advance.
     *
     * Note: We might want to cache more auxiliary data than the
     * advance, but this seems adequate for now.
     */
    public static final class SegmentPath extends LayoutPathImpl {
        private double[] data; 
        EndType etype;

        public static SegmentPath get(EndType etype, double... pts) {
            return new SegmentPathBuilder().build(etype, pts);
        }

        /**
         * Internal, use SegmentPathBuilder or one of the static
         * helper functions to construct a SegmentPath.
         */
        SegmentPath(double[] data, EndType etype) {
            this.data = data;
            this.etype = etype;
        }


        public void pathToPoint(Point2D location, boolean preceding, Point2D point) {
            locateAndGetIndex(location, preceding, point);
        }


        public boolean pointToPath(Point2D pt, Point2D result) {
            double x = pt.getX();               
            double y = pt.getY();

            double bx = data[0];                
            double by = data[1];
            double bl = data[2];

            double cd2 = Double.MAX_VALUE;       
            double cx = 0;                       
            double cy = 0;                       
            double cl = 0;                       
            int ci = 0;                          

            for (int i = 3; i < data.length; i += 3) {
                double nx = data[i];             
                double ny = data[i+1];
                double nl = data[i+2];

                double dx = nx - bx;             
                double dy = ny - by;
                double dl = nl - bl;

                double px = x - bx;              
                double py = y - by;


                double dot = dx * px + dy * py;      
                double vcx, vcy, vcl;                
                int vi;                              
                do {                                 
                    if (dl == 0 ||                   
                        (dot < 0 &&                  
                         (!etype.isExtended() ||
                          i != 3))) {                
                        vcx = bx;
                        vcy = by;
                        vcl = bl;
                        vi = i;
                    } else {
                        double l2 = dl * dl;         
                        if (dot <= l2 ||             
                            (etype.isExtended() &&   
                             i == data.length - 3)) {
                            double p = dot / l2;     
                            vcx = bx + p * dx;       
                            vcy = by + p * dy;
                            vcl = bl + p * dl;
                            vi = i;
                        } else {
                            if (i == data.length - 3) {
                                vcx = nx;            
                                vcy = ny;
                                vcl = nl;
                                vi = data.length;
                            } else {
                                break;               
                            }
                        }
                    }

                    double tdx = x - vcx;        
                    double tdy = y - vcy;
                    double td2 = tdx * tdx + tdy * tdy;
                    if (td2 <= cd2) {            
                        cd2 = td2;
                        cx = vcx;
                        cy = vcy;
                        cl = vcl;
                        ci = vi;
                    }
                } while (false);

                bx = nx;
                by = ny;
                bl = nl;
            }

            bx = data[ci-3];
            by = data[ci-2];
            if (cx != bx || cy != by) {     
                double nx = data[ci];
                double ny = data[ci+1];
                double co = sqrt(cd2);     
                if ((x-cx)*(ny-by) > (y-cy)*(nx-bx)) {
                    co = -co;              
                }
                result.setLocation(cl, co);
                return false;
            } else {                        
                boolean havePrev = ci != 3 && data[ci-1] != data[ci-4];
                boolean haveFoll = ci != data.length && data[ci-1] != data[ci+2];
                boolean doExtend = etype.isExtended() && (ci == 3 || ci == data.length);
                if (havePrev && haveFoll) {
                    Point2D.Double pp = new Point2D.Double(x, y);
                    calcoffset(ci - 3, doExtend, pp);
                    Point2D.Double fp = new Point2D.Double(x, y);
                    calcoffset(ci, doExtend, fp);
                    if (abs(pp.y) > abs(fp.y)) {
                        result.setLocation(pp);
                        return true; 
                    } else {
                        result.setLocation(fp);
                        return false; 
                    }
                } else if (havePrev) {
                    result.setLocation(x, y);
                    calcoffset(ci - 3, doExtend, result);
                    return true;
                } else {
                    result.setLocation(x, y);
                    calcoffset(ci, doExtend, result);
                    return false;
                }
            }
        }

        /**
         * Return the location of the point passed in result as mapped to the
         * line indicated by index.  If doExtend is true, extend the
         * x value without pinning to the ends of the line.
         * this assumes that index is valid and references a line that has
         * non-zero length.
         */
        private void calcoffset(int index, boolean doExtend, Point2D result) {
            double bx = data[index-3];
            double by = data[index-2];
            double px = result.getX() - bx;
            double py = result.getY() - by;
            double dx = data[index] - bx;
            double dy = data[index+1] - by;
            double l = data[index+2] - data[index - 1];

            double rx = (px * dx + py * dy) / l;
            double ry = (px * -dy + py * dx) / l;
            if (!doExtend) {
                if (rx < 0) rx = 0;
                else if (rx > l) rx = l;
            }
            rx += data[index-1];
            result.setLocation(rx, ry);
        }


        public Shape mapShape(Shape s) {
            return new Mapper().mapShape(s);
        }

        public double start() {
            return data[2];
        }

        public double end() {
            return data[data.length - 1];
        }

        public double length() {
            return data[data.length-1] - data[2];
        }


        /**
         * Get the 'modulus' of an advance on a closed path.
         */
        private double getClosedAdvance(double a, boolean preceding) {
            if (etype.isClosed()) {
                a -= data[2];
                int count = (int)(a/length());
                a -= count * length();
                if (a < 0 || (a == 0 && preceding)) {
                    a += length();

                }
                a += data[2];
            }
            return a;
        }

        /**
         * Return the index of the segment associated with advance. This
         * points to the start of the triple and is a multiple of 3 between
         * 3 and data.length-3 inclusive.  It never points to a 'moveto' triple.
         *
         * If the path is closed, 'a' is mapped to
         * a value between the start and end of the path, inclusive.
         * If preceding is true, and 'a' lies on a segment boundary,
         * return the index of the preceding segment, else return the index
         * of the current segment (if it is not a moveto segment) otherwise
         * the following segment (which is never a moveto segment).
         *
         * Note: if the path is not closed, the advance might not actually
         * lie on the returned segment-- it might be before the first, or
         * after the last.  The first or last segment (as appropriate)
         * will be returned in this case.
         */
        private int getSegmentIndexForAdvance(double a, boolean preceding) {
            a = getClosedAdvance(a, preceding);

            int i, lim;
            for (i = 5, lim = data.length-1; i < lim; i += 3) {
                double v = data[i];
                if (a < v || (a == v && preceding)) {
                    break;
                }
            }
            return i-2; 
        }

        /**
         * Map a location based on the provided segment, returning in pt.
         * Seg must be a valid 'lineto' segment.  Note: if the path is
         * closed, x must be within the start and end of the path.
         */
        private void map(int seg, double a, double o, Point2D pt) {
            double dx = data[seg] - data[seg-3];
            double dy = data[seg+1] - data[seg-2];
            double dl = data[seg+2] - data[seg-1];

            double ux = dx/dl; 
            double uy = dy/dl;

            a -= data[seg-1];

            pt.setLocation(data[seg-3] + a * ux - o * uy,
                           data[seg-2] + a * uy + o * ux);
        }

        /**
         * Map the point, and return the segment index.
         */
        private int locateAndGetIndex(Point2D loc, boolean preceding, Point2D result) {
            double a = loc.getX();
            double o = loc.getY();
            int seg = getSegmentIndexForAdvance(a, preceding);
            map(seg, a, o, result);

            return seg;
        }


        /**
         * This represents a line segment from the iterator.  Each target segment will
         * interpret it, and since this process needs slope along the line
         * segment, this lets us compute it once and pass it around easily.
         */
        class LineInfo {
            double sx, sy; 
            double lx, ly; 
            double m;      

            /**
             * Set the lineinfo to this line
             */
            void set(double sx, double sy, double lx, double ly) {
                this.sx = sx;
                this.sy = sy;
                this.lx = lx;
                this.ly = ly;
                double dx = lx - sx;
                if (dx == 0) {
                    m = 0; 
                } else {
                    double dy = ly - sy;
                    m = dy / dx;
                }
            }

            void set(LineInfo rhs) {
                this.sx = rhs.sx;
                this.sy = rhs.sy;
                this.lx = rhs.lx;
                this.ly = rhs.ly;
                this.m  = rhs.m;
            }

            /**
             * Return true if we intersect the infinitely tall rectangle with
             * lo <= x < hi.  If we do, also return the pinned portion of ourselves in
             * result.
             */
            boolean pin(double lo, double hi, LineInfo result) {
                result.set(this);
                if (lx >= sx) {
                    if (sx < hi && lx >= lo) {
                        if (sx < lo) {
                            if (m != 0) result.sy = sy + m * (lo - sx);
                            result.sx = lo;
                        }
                        if (lx > hi) {
                            if (m != 0) result.ly = ly + m * (hi - lx);
                            result.lx = hi;
                        }
                        return true;
                    }
                } else {
                    if (lx < hi && sx >= lo) {
                        if (lx < lo) {
                            if (m != 0) result.ly = ly + m * (lo - lx);
                            result.lx = lo;
                        }
                        if (sx > hi) {
                            if (m != 0) result.sy = sy + m * (hi - sx);
                            result.sx = hi;
                        }
                        return true;
                    }
                }
                return false;
            }

            /**
             * Return true if we intersect the segment at ix.  This takes
             * the path end type into account and computes the relevant
             * parameters to pass to pin(double, double, LineInfo).
             */
            boolean pin(int ix, LineInfo result) {
                double lo = data[ix-1];
                double hi = data[ix+2];
                switch (SegmentPath.this.etype) {
                case PINNED:
                    break;
                case EXTENDED:
                    if (ix == 3) lo = Double.NEGATIVE_INFINITY;
                    if (ix == data.length - 3) hi = Double.POSITIVE_INFINITY;
                    break;
                case CLOSED:
                    break;
                }

                return pin(lo, hi, result);
            }
        }

        /**
         * Each segment will construct its own general path, mapping the provided lines
         * into its own simple space.
         */
        class Segment {
            final int ix;        
            final double ux, uy; 

            final LineInfo temp; 

            boolean broken;      
            double cx, cy;       
            GeneralPath gp;      

            Segment(int ix) {
                this.ix = ix;
                double len = data[ix+2] - data[ix-1];
                this.ux = (data[ix] - data[ix-3]) / len;
                this.uy = (data[ix+1] - data[ix-2]) / len;
                this.temp = new LineInfo();
            }

            void init() {
                if (LOGMAP) LOG.format("s(%d) init\n", ix);
                broken = true;
                cx = cy = Double.MIN_VALUE;
                this.gp = new GeneralPath();
            }

            void move() {
                if (LOGMAP) LOG.format("s(%d) move\n", ix);
                broken = true;
            }

            void close() {
                if (!broken) {
                    if (LOGMAP) LOG.format("s(%d) close\n[cp]\n", ix);
                    gp.closePath();
                }
            }

            void line(LineInfo li) {
                if (LOGMAP) LOG.format("s(%d) line %g, %g to %g, %g\n", ix, li.sx, li.sy, li.lx, li.ly);

                if (li.pin(ix, temp)) {
                    if (LOGMAP) LOG.format("pin: %g, %g to %g, %g\n", temp.sx, temp.sy, temp.lx, temp.ly);

                    temp.sx -= data[ix-1];
                    double sx = data[ix-3] + temp.sx * ux - temp.sy * uy;
                    double sy = data[ix-2] + temp.sx * uy + temp.sy * ux;
                    temp.lx -= data[ix-1];
                    double lx = data[ix-3] + temp.lx * ux - temp.ly * uy;
                    double ly = data[ix-2] + temp.lx * uy + temp.ly * ux;

                    if (LOGMAP) LOG.format("points: %g, %g to %g, %g\n", sx, sy, lx, ly);

                    if (sx != cx || sy != cy) {
                        if (broken) {
                            if (LOGMAP) LOG.format("[mt %g, %g]\n", sx, sy);
                            gp.moveTo((float)sx, (float)sy);
                        } else {
                            if (LOGMAP) LOG.format("[lt %g, %g]\n", sx, sy);
                            gp.lineTo((float)sx, (float)sy);
                        }
                    }
                    if (LOGMAP) LOG.format("[lt %g, %g]\n", lx, ly);
                    gp.lineTo((float)lx, (float)ly);

                    broken = false;
                    cx = lx;
                    cy = ly;
                }
            }
        }

        class Mapper {
            final LineInfo li;                 
            final ArrayList<Segment> segments; 
            final Point2D.Double mpt;          
            final Point2D.Double cpt;          
            boolean haveMT;                    

            Mapper() {
                li = new LineInfo();
                segments = new ArrayList<Segment>();
                for (int i = 3; i < data.length; i += 3) {
                    if (data[i+2] != data[i-1]) { 
                        segments.add(new Segment(i));
                    }
                }

                mpt = new Point2D.Double();
                cpt = new Point2D.Double();
            }

            void init() {
                if (LOGMAP) LOG.format("init\n");
                haveMT = false;
                for (Segment s: segments) {
                    s.init();
                }
            }

            void moveTo(double x, double y) {
                if (LOGMAP) LOG.format("moveto %g, %g\n", x, y);
                mpt.x = x;
                mpt.y = y;
                haveMT = true;
            }

            void lineTo(double x, double y) {
                if (LOGMAP) LOG.format("lineto %g, %g\n", x, y);

                if (haveMT) {
                    cpt.x = mpt.x;
                    cpt.y = mpt.y;
                }

                if (x == cpt.x && y == cpt.y) {
                    return;
                }

                if (haveMT) {
                    haveMT = false;
                    for (Segment s: segments) {
                        s.move();
                    }
                }

                li.set(cpt.x, cpt.y, x, y);
                for (Segment s: segments) {
                    s.line(li);
                }

                cpt.x = x;
                cpt.y = y;
            }

            void close() {
                if (LOGMAP) LOG.format("close\n");
                lineTo(mpt.x, mpt.y);
                for (Segment s: segments) {
                    s.close();
                }
            }

            public Shape mapShape(Shape s) {
                if (LOGMAP) LOG.format("mapshape on path: %s\n", LayoutPathImpl.SegmentPath.this);
                PathIterator pi = s.getPathIterator(null, 1); 

                if (LOGMAP) LOG.format("start\n");
                init();

                final double[] coords = new double[2];
                while (!pi.isDone()) {
                    switch (pi.currentSegment(coords)) {
                    case SEG_CLOSE: close(); break;
                    case SEG_MOVETO: moveTo(coords[0], coords[1]); break;
                    case SEG_LINETO: lineTo(coords[0], coords[1]); break;
                    default: break;
                    }

                    pi.next();
                }
                if (LOGMAP) LOG.format("finish\n\n");

                GeneralPath gp = new GeneralPath();
                for (Segment seg: segments) {
                    gp.append(seg.gp, false);
                }
                return gp;
            }
        }


        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("{");
            b.append(etype.toString());
            b.append(" ");
            for (int i = 0; i < data.length; i += 3) {
                if (i > 0) {
                    b.append(",");
                }
                float x = ((int)(data[i] * 100))/100.0f;
                float y = ((int)(data[i+1] * 100))/100.0f;
                float l = ((int)(data[i+2] * 10))/10.0f;
                b.append("{");
                b.append(x);
                b.append(",");
                b.append(y);
                b.append(",");
                b.append(l);
                b.append("}");
            }
            b.append("}");
            return b.toString();
        }
    }


    public static class EmptyPath extends LayoutPathImpl {
        private AffineTransform tx;

        public EmptyPath(AffineTransform tx) {
            this.tx = tx;
        }

        public void pathToPoint(Point2D location, boolean preceding, Point2D point) {
            if (tx != null) {
                tx.transform(location, point);
            } else {
                point.setLocation(location);
            }
        }

        public boolean pointToPath(Point2D pt, Point2D result) {
            result.setLocation(pt);
            if (tx != null) {
                try {
                    tx.inverseTransform(pt, result);
                }
                catch (NoninvertibleTransformException ex) {
                }
            }
            return result.getX() > 0;
        }

        public double start() { return 0; }

        public double end() { return 0; }

        public double length() { return 0; }

        public Shape mapShape(Shape s) {
            if (tx != null) {
                return tx.createTransformedShape(s);
            }
            return s;
        }
    }
}
