/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.security.AccessController;
import java.util.Arrays;
import sun.awt.geom.PathConsumer2D;
import static sun.java2d.marlin.MarlinUtils.logInfo;
import sun.java2d.ReentrantContextProvider;
import sun.java2d.ReentrantContextProviderCLQ;
import sun.java2d.ReentrantContextProviderTL;
import sun.java2d.pipe.AATileGenerator;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.RenderingEngine;
import sun.security.action.GetPropertyAction;

/**
 * Marlin RendererEngine implementation (derived from Pisces)
 */
public final class DMarlinRenderingEngine extends RenderingEngine
                                          implements MarlinConst
{
    static final boolean DISABLE_2ND_STROKER_CLIPPING = true;

    static final boolean DO_TRACE_PATH = false;

    static final boolean DO_CLIP = MarlinProperties.isDoClip();
    static final boolean DO_CLIP_FILL = true;
    static final boolean DO_CLIP_RUNTIME_ENABLE = MarlinProperties.isDoClipRuntimeFlag();

    private static final float MIN_PEN_SIZE = 1.0f / MIN_SUBPIXELS;

    static final double UPPER_BND = Float.MAX_VALUE / 2.0d;
    static final double LOWER_BND = -UPPER_BND;

    private enum NormMode {
        ON_WITH_AA {
            @Override
            PathIterator getNormalizingPathIterator(final RendererContext rdrCtx,
                                                    final PathIterator src)
            {
                return rdrCtx.nPCPathIterator.init(src);
            }
        },
        ON_NO_AA{
            @Override
            PathIterator getNormalizingPathIterator(final RendererContext rdrCtx,
                                                    final PathIterator src)
            {
                return rdrCtx.nPQPathIterator.init(src);
            }
        },
        OFF{
            @Override
            PathIterator getNormalizingPathIterator(final RendererContext rdrCtx,
                                                    final PathIterator src)
            {
                return src;
            }
        };

        abstract PathIterator getNormalizingPathIterator(RendererContext rdrCtx,
                                                         PathIterator src);
    }

    /**
     * Public constructor
     */
    public DMarlinRenderingEngine() {
        super();
        logSettings(DMarlinRenderingEngine.class.getName());
    }

    /**
     * Create a widened path as specified by the parameters.
     * <p>
     * The specified {@code src} {@link Shape} is widened according
     * to the specified attribute parameters as per the
     * {@link BasicStroke} specification.
     *
     * @param src the source path to be widened
     * @param width the width of the widened path as per {@code BasicStroke}
     * @param caps the end cap decorations as per {@code BasicStroke}
     * @param join the segment join decorations as per {@code BasicStroke}
     * @param miterlimit the miter limit as per {@code BasicStroke}
     * @param dashes the dash length array as per {@code BasicStroke}
     * @param dashphase the initial dash phase as per {@code BasicStroke}
     * @return the widened path stored in a new {@code Shape} object
     * @since 1.7
     */
    @Override
    public Shape createStrokedShape(Shape src,
                                    float width,
                                    int caps,
                                    int join,
                                    float miterlimit,
                                    float[] dashes,
                                    float dashphase)
    {
        final RendererContext rdrCtx = getRendererContext();
        try {
            final Path2D.Double p2d = rdrCtx.getPath2D();

            strokeTo(rdrCtx,
                     src,
                     null,
                     width,
                     NormMode.OFF,
                     caps,
                     join,
                     miterlimit,
                     dashes,
                     dashphase,
                     rdrCtx.transformerPC2D.wrapPath2D(p2d)
                    );

            return new Path2D.Double(p2d);

        } finally {
            returnRendererContext(rdrCtx);
        }
    }

    /**
     * Sends the geometry for a widened path as specified by the parameters
     * to the specified consumer.
     * <p>
     * The specified {@code src} {@link Shape} is widened according
     * to the parameters specified by the {@link BasicStroke} object.
     * Adjustments are made to the path as appropriate for the
     * {@link java.awt.RenderingHints#VALUE_STROKE_NORMALIZE} hint if the
     * {@code normalize} boolean parameter is true.
     * Adjustments are made to the path as appropriate for the
     * {@link java.awt.RenderingHints#VALUE_ANTIALIAS_ON} hint if the
     * {@code antialias} boolean parameter is true.
     * <p>
     * The geometry of the widened path is forwarded to the indicated
     * {@link PathConsumer2D} object as it is calculated.
     *
     * @param src the source path to be widened
     * @param at the transform to be applied to the shape and the
     *           stroke attributes
     * @param bs the {@code BasicStroke} object specifying the
     *           decorations to be applied to the widened path
     * @param thin true if the transformed stroke attributes are smaller
     *             than the minimum dropout pen width
     * @param normalize indicates whether stroke normalization should
     *                  be applied
     * @param antialias indicates whether or not adjustments appropriate
     *                  to antialiased rendering should be applied
     * @param consumer the {@code PathConsumer2D} instance to forward
     *                 the widened geometry to
     * @since 1.7
     */
    @Override
    public void strokeTo(Shape src,
                         AffineTransform at,
                         BasicStroke bs,
                         boolean thin,
                         boolean normalize,
                         boolean antialias,
                         final PathConsumer2D consumer)
    {
        strokeTo(src, at, null, bs, thin, normalize, antialias, consumer);
    }

    /**
     * Sends the geometry for a widened path as specified by the parameters
     * to the specified consumer.
     * <p>
     * The specified {@code src} {@link Shape} is widened according
     * to the parameters specified by the {@link BasicStroke} object.
     * Adjustments are made to the path as appropriate for the
     * {@link java.awt.RenderingHints#VALUE_STROKE_NORMALIZE} hint if the
     * {@code normalize} boolean parameter is true.
     * Adjustments are made to the path as appropriate for the
     * {@link java.awt.RenderingHints#VALUE_ANTIALIAS_ON} hint if the
     * {@code antialias} boolean parameter is true.
     * <p>
     * The geometry of the widened path is forwarded to the indicated
     * {@link PathConsumer2D} object as it is calculated.
     *
     * @param src the source path to be widened
     * @param at the transform to be applied to the shape and the
     *           stroke attributes
     * @param clip the current clip in effect in device coordinates
     * @param bs the {@code BasicStroke} object specifying the
     *           decorations to be applied to the widened path
     * @param thin true if the transformed stroke attributes are smaller
     *             than the minimum dropout pen width
     * @param normalize indicates whether stroke normalization should
     *                  be applied
     * @param antialias indicates whether or not adjustments appropriate
     *                  to antialiased rendering should be applied
     * @param consumer the {@code PathConsumer2D} instance to forward
     *                 the widened geometry to
     * @since 17
     */
/*    @Override (only for 17+) */
    public void strokeTo(Shape src,
                         AffineTransform at,
                         Region clip,
                         BasicStroke bs,
                         boolean thin,
                         boolean normalize,
                         boolean antialias,
                         final PathConsumer2D consumer)
    {
        final AffineTransform _at = (at != null && !at.isIdentity()) ? at
                                    : null;

        final NormMode norm = (normalize) ?
                ((antialias) ? NormMode.ON_WITH_AA : NormMode.ON_NO_AA)
                : NormMode.OFF;

        final RendererContext rdrCtx = getRendererContext();
        try {
            if ((clip != null) &&
                    (DO_CLIP || (DO_CLIP_RUNTIME_ENABLE && MarlinProperties.isDoClipAtRuntime()))) {
                final double[] clipRect = rdrCtx.clipRect;

                final double rdrOffX = 0.25d; 
                final double rdrOffY = 0.25d; 

                final double margin = 1e-3d;

                clipRect[0] = clip.getLoY()
                                - margin + rdrOffY;
                clipRect[1] = clip.getLoY() + clip.getHeight()
                                + margin + rdrOffY;
                clipRect[2] = clip.getLoX()
                                - margin + rdrOffX;
                clipRect[3] = clip.getLoX() + clip.getWidth()
                                + margin + rdrOffX;

                if (MarlinConst.DO_LOG_CLIP) {
                    MarlinUtils.logInfo("clipRect (clip): "
                                        + Arrays.toString(rdrCtx.clipRect));
                }

                rdrCtx.doClip = true;
            }

            strokeTo(rdrCtx, src, _at, bs, thin, norm, antialias,
                     rdrCtx.p2dAdapter.init(consumer));
        } finally {
            returnRendererContext(rdrCtx);
        }
    }

    void strokeTo(final RendererContext rdrCtx,
                  Shape src,
                  AffineTransform at,
                  BasicStroke bs,
                  boolean thin,
                  NormMode normalize,
                  boolean antialias,
                  DPathConsumer2D pc2d)
    {
        double lw;
        if (thin) {
            if (antialias) {
                lw = userSpaceLineWidth(at, MIN_PEN_SIZE);
            } else {
                lw = userSpaceLineWidth(at, 1.0d);
            }
        } else {
            lw = bs.getLineWidth();
        }
        strokeTo(rdrCtx,
                 src,
                 at,
                 lw,
                 normalize,
                 bs.getEndCap(),
                 bs.getLineJoin(),
                 bs.getMiterLimit(),
                 bs.getDashArray(),
                 bs.getDashPhase(),
                 pc2d);
    }

    private double userSpaceLineWidth(AffineTransform at, double lw) {

        double widthScale;

        if (at == null) {
            widthScale = 1.0d;
        } else if ((at.getType() & (AffineTransform.TYPE_GENERAL_TRANSFORM  |
                                    AffineTransform.TYPE_GENERAL_SCALE)) != 0) {
            widthScale = Math.sqrt(Math.abs(at.getDeterminant()));
        } else {
            double A = at.getScaleX();       
            double C = at.getShearX();       
            double B = at.getShearY();       
            double D = at.getScaleY();       

            /*
             * Given a 2 x 2 affine matrix [ A B ] such that
             *                             [ C D ]
             * v' = [x' y'] = [Ax + Cy, Bx + Dy], we want to
             * find the maximum magnitude (norm) of the vector v'
             * with the constraint (x^2 + y^2 = 1).
             * The equation to maximize is
             *     |v'| = sqrt((Ax+Cy)^2+(Bx+Dy)^2)
             * or  |v'| = sqrt((AA+BB)x^2 + 2(AC+BD)xy + (CC+DD)y^2).
             * Since sqrt is monotonic we can maximize |v'|^2
             * instead and plug in the substitution y = sqrt(1 - x^2).
             * Trigonometric equalities can then be used to get
             * rid of most of the sqrt terms.
             */

            double EA = A*A + B*B;          
            double EB = 2.0d * (A*C + B*D); 
            double EC = C*C + D*D;          

            /*
             * There is a lot of calculus omitted here.
             *
             * Conceptually, in the interests of understanding the
             * terms that the calculus produced we can consider
             * that EA and EC end up providing the lengths along
             * the major axes and the hypot term ends up being an
             * adjustment for the additional length along the off-axis
             * angle of rotated or sheared ellipses as well as an
             * adjustment for the fact that the equation below
             * averages the two major axis lengths.  (Notice that
             * the hypot term contains a part which resolves to the
             * difference of these two axis lengths in the absence
             * of rotation.)
             *
             * In the calculus, the ratio of the EB and (EA-EC) terms
             * ends up being the tangent of 2*theta where theta is
             * the angle that the long axis of the ellipse makes
             * with the horizontal axis.  Thus, this equation is
             * calculating the length of the hypotenuse of a triangle
             * along that axis.
             */

            double hypot = Math.sqrt(EB*EB + (EA-EC)*(EA-EC));
            double widthsquared = ((EA + EC + hypot) / 2.0d);

            widthScale = Math.sqrt(widthsquared);
        }

        return (lw / widthScale);
    }

    void strokeTo(final RendererContext rdrCtx,
                  Shape src,
                  AffineTransform at,
                  double width,
                  NormMode norm,
                  int caps,
                  int join,
                  float miterlimit,
                  float[] dashes,
                  float dashphase,
                  DPathConsumer2D pc2d)
    {

        AffineTransform strokerat = null;

        int dashLen = -1;
        boolean recycleDashes = false;
        double[] dashesD = null;

        if (dashes != null) {
            recycleDashes = true;
            dashLen = dashes.length;
            dashesD = rdrCtx.dasher.copyDashArray(dashes);
        }

        if (at != null && !at.isIdentity()) {
            final double a = at.getScaleX();
            final double b = at.getShearX();
            final double c = at.getShearY();
            final double d = at.getScaleY();
            final double det = a * d - c * b;

            if (Math.abs(det) <= (2.0d * Double.MIN_VALUE)) {

                pc2d.moveTo(0.0d, 0.0d);
                pc2d.pathDone();
                return;
            }

            if (nearZero(a*b + c*d) && nearZero(a*a + c*c - (b*b + d*d))) {
                final double scale = Math.sqrt(a*a + c*c);

                if (dashesD != null) {
                    for (int i = 0; i < dashLen; i++) {
                        dashesD[i] *= scale;
                    }
                    dashphase *= scale;
                }
                width *= scale;

            } else {
                strokerat = at;

            }
        } else {
            at = null;
        }

        final TransformingPathConsumer2D transformerPC2D = rdrCtx.transformerPC2D;

        if (DO_TRACE_PATH) {
            pc2d = transformerPC2D.traceStroker(pc2d);
        }

        if (USE_SIMPLIFIER) {
            pc2d = rdrCtx.simplifier.init(pc2d);
        }

        pc2d = transformerPC2D.deltaTransformConsumer(pc2d, strokerat);

        pc2d = rdrCtx.stroker.init(pc2d, width, caps, join, miterlimit,
                (dashesD == null));

        rdrCtx.monotonizer.init(width);

        if (dashesD != null) {
            if (DO_TRACE_PATH) {
                pc2d = transformerPC2D.traceDasher(pc2d);
            }
            pc2d = rdrCtx.dasher.init(pc2d, dashesD, dashLen, dashphase,
                                      recycleDashes);

            if (DISABLE_2ND_STROKER_CLIPPING) {
                rdrCtx.stroker.disableClipping();
            }

        } else if (rdrCtx.doClip && (caps != CAP_BUTT)) {
            if (DO_TRACE_PATH) {
                pc2d = transformerPC2D.traceClosedPathDetector(pc2d);
            }

            pc2d = transformerPC2D.detectClosedPath(pc2d);
        }
        pc2d = transformerPC2D.inverseDeltaTransformConsumer(pc2d, strokerat);

        if (DO_TRACE_PATH) {
            pc2d = transformerPC2D.traceInput(pc2d);
        }

        final PathIterator pi = norm.getNormalizingPathIterator(rdrCtx,
                                         src.getPathIterator(at));

        pathTo(rdrCtx, pi, pc2d);

        /*
         * Pipeline seems to be:
         * shape.getPathIterator(at)
         * -> (NormalizingPathIterator)
         * -> (inverseDeltaTransformConsumer)
         * -> (Dasher)
         * -> Stroker
         * -> (deltaTransformConsumer)
         *
         * -> (CollinearSimplifier) to remove redundant segments
         *
         * -> pc2d = Renderer (bounding box)
         */
    }

    private static boolean nearZero(final double num) {
        return Math.abs(num) < 2.0d * Math.ulp(num);
    }

    abstract static class NormalizingPathIterator implements PathIterator {

        private PathIterator src;

        private double curx_adjust, cury_adjust;
        private double movx_adjust, movy_adjust;

        private final double[] tmp;

        NormalizingPathIterator(final double[] tmp) {
            this.tmp = tmp;
        }

        final NormalizingPathIterator init(final PathIterator src) {
            this.src = src;
            return this; 
        }

        /**
         * Disposes this path iterator:
         * clean up before reusing this instance
         */
        final void dispose() {
            this.src = null;
        }

        @Override
        public final int currentSegment(final double[] coords) {
            int lastCoord;
            final int type = src.currentSegment(coords);

            switch(type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    lastCoord = 0;
                    break;
                case PathIterator.SEG_QUADTO:
                    lastCoord = 2;
                    break;
                case PathIterator.SEG_CUBICTO:
                    lastCoord = 4;
                    break;
                case PathIterator.SEG_CLOSE:
                    curx_adjust = movx_adjust;
                    cury_adjust = movy_adjust;
                    return type;
                default:
                    throw new InternalError("Unrecognized curve type");
            }

            double coord, x_adjust, y_adjust;

            coord = coords[lastCoord];
            x_adjust = normCoord(coord); 
            coords[lastCoord] = x_adjust;
            x_adjust -= coord;

            coord = coords[lastCoord + 1];
            y_adjust = normCoord(coord); 
            coords[lastCoord + 1] = y_adjust;
            y_adjust -= coord;

            switch(type) {
                case PathIterator.SEG_MOVETO:
                    movx_adjust = x_adjust;
                    movy_adjust = y_adjust;
                    break;
                case PathIterator.SEG_LINETO:
                    break;
                case PathIterator.SEG_QUADTO:
                    coords[0] += (curx_adjust + x_adjust) / 2.0d;
                    coords[1] += (cury_adjust + y_adjust) / 2.0d;
                    break;
                case PathIterator.SEG_CUBICTO:
                    coords[0] += curx_adjust;
                    coords[1] += cury_adjust;
                    coords[2] += x_adjust;
                    coords[3] += y_adjust;
                    break;
                case PathIterator.SEG_CLOSE:
                default:
            }
            curx_adjust = x_adjust;
            cury_adjust = y_adjust;
            return type;
        }

        abstract double normCoord(final double coord);

        @Override
        public final int currentSegment(final float[] coords) {
            final double[] _tmp = tmp; 
            int type = this.currentSegment(_tmp);
            for (int i = 0; i < 6; i++) {
                coords[i] = (float)_tmp[i];
            }
            return type;
        }

        @Override
        public final int getWindingRule() {
            return src.getWindingRule();
        }

        @Override
        public final boolean isDone() {
            if (src.isDone()) {
                dispose();
                return true;
            }
            return false;
        }

        @Override
        public final void next() {
            src.next();
        }

        static final class NearestPixelCenter
                                extends NormalizingPathIterator
        {
            NearestPixelCenter(final double[] tmp) {
                super(tmp);
            }

            @Override
            double normCoord(final double coord) {
                return Math.floor(coord) + 0.5d;
            }
        }

        static final class NearestPixelQuarter
                                extends NormalizingPathIterator
        {
            NearestPixelQuarter(final double[] tmp) {
                super(tmp);
            }

            @Override
            double normCoord(final double coord) {
                return Math.floor(coord + 0.25d) + 0.25d;
            }
        }
    }

    private static void pathTo(final RendererContext rdrCtx, final PathIterator pi,
                               DPathConsumer2D pc2d)
    {
        if (USE_PATH_SIMPLIFIER) {
            pc2d = rdrCtx.pathSimplifier.init(pc2d);
        }

        rdrCtx.dirty = true;

        pathToLoop(rdrCtx.double6, pi, pc2d);

        rdrCtx.dirty = false;
    }

    private static void pathToLoop(final double[] coords, final PathIterator pi,
                                   final DPathConsumer2D pc2d)
    {
        boolean subpathStarted = false;

        for (; !pi.isDone(); pi.next()) {
            switch (pi.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                /* Checking SEG_MOVETO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Skipping next path segment in case of
                 * invalid data.
                 */
                if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                    coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                {
                    pc2d.moveTo(coords[0], coords[1]);
                    subpathStarted = true;
                }
                break;
            case PathIterator.SEG_LINETO:
                /* Checking SEG_LINETO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid data. If segment is skipped its endpoint
                 * (if valid) is used to begin new subpath.
                 */
                if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                    coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                {
                    if (subpathStarted) {
                        pc2d.lineTo(coords[0], coords[1]);
                    } else {
                        pc2d.moveTo(coords[0], coords[1]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_QUADTO:
                /* Checking SEG_QUADTO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid endpoints's data. Equivalent to the SEG_LINETO
                 * if endpoint coordinates are valid but there are invalid data
                 * among other coordinates
                 */
                if (coords[2] < UPPER_BND && coords[2] > LOWER_BND &&
                    coords[3] < UPPER_BND && coords[3] > LOWER_BND)
                {
                    if (subpathStarted) {
                        if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                            coords[1] < UPPER_BND && coords[1] > LOWER_BND)
                        {
                            pc2d.quadTo(coords[0], coords[1],
                                        coords[2], coords[3]);
                        } else {
                            pc2d.lineTo(coords[2], coords[3]);
                        }
                    } else {
                        pc2d.moveTo(coords[2], coords[3]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_CUBICTO:
                /* Checking SEG_CUBICTO coordinates if they are out of the
                 * [LOWER_BND, UPPER_BND] range. This check also handles NaN
                 * and Infinity values. Ignoring current path segment in case
                 * of invalid endpoints's data. Equivalent to the SEG_LINETO
                 * if endpoint coordinates are valid but there are invalid data
                 * among other coordinates
                 */
                if (coords[4] < UPPER_BND && coords[4] > LOWER_BND &&
                    coords[5] < UPPER_BND && coords[5] > LOWER_BND)
                {
                    if (subpathStarted) {
                        if (coords[0] < UPPER_BND && coords[0] > LOWER_BND &&
                            coords[1] < UPPER_BND && coords[1] > LOWER_BND &&
                            coords[2] < UPPER_BND && coords[2] > LOWER_BND &&
                            coords[3] < UPPER_BND && coords[3] > LOWER_BND)
                        {
                            pc2d.curveTo(coords[0], coords[1],
                                         coords[2], coords[3],
                                         coords[4], coords[5]);
                        } else {
                            pc2d.lineTo(coords[4], coords[5]);
                        }
                    } else {
                        pc2d.moveTo(coords[4], coords[5]);
                        subpathStarted = true;
                    }
                }
                break;
            case PathIterator.SEG_CLOSE:
                if (subpathStarted) {
                    pc2d.closePath();
                }
                break;
            default:
            }
        }
        pc2d.pathDone();
    }

    /**
     * Construct an antialiased tile generator for the given shape with
     * the given rendering attributes and store the bounds of the tile
     * iteration in the bbox parameter.
     * The {@code at} parameter specifies a transform that should affect
     * both the shape and the {@code BasicStroke} attributes.
     * The {@code clip} parameter specifies the current clip in effect
     * in device coordinates and can be used to prune the data for the
     * operation, but the renderer is not required to perform any
     * clipping.
     * If the {@code BasicStroke} parameter is null then the shape
     * should be filled as is, otherwise the attributes of the
     * {@code BasicStroke} should be used to specify a draw operation.
     * The {@code thin} parameter indicates whether or not the
     * transformed {@code BasicStroke} represents coordinates smaller
     * than the minimum resolution of the antialiasing rasterizer as
     * specified by the {@code getMinimumAAPenWidth()} method.
     * <p>
     * Upon returning, this method will fill the {@code bbox} parameter
     * with 4 values indicating the bounds of the iteration of the
     * tile generator.
     * The iteration order of the tiles will be as specified by the
     * pseudo-code:
     * <pre>
     *     for (y = bbox[1]; y < bbox[3]; y += tileheight) {
     *         for (x = bbox[0]; x < bbox[2]; x += tilewidth) {
     *         }
     *     }
     * </pre>
     * If there is no output to be rendered, this method may return
     * null.
     *
     * @param s the shape to be rendered (fill or draw)
     * @param at the transform to be applied to the shape and the
     *           stroke attributes
     * @param clip the current clip in effect in device coordinates
     * @param bs if non-null, a {@code BasicStroke} whose attributes
     *           should be applied to this operation
     * @param thin true if the transformed stroke attributes are smaller
     *             than the minimum dropout pen width
     * @param normalize true if the {@code VALUE_STROKE_NORMALIZE}
     *                  {@code RenderingHint} is in effect
     * @param bbox returns the bounds of the iteration
     * @return the {@code AATileGenerator} instance to be consulted
     *         for tile coverages, or null if there is no output to render
     * @since 1.7
     */
    @Override
    public AATileGenerator getAATileGenerator(Shape s,
                                              AffineTransform at,
                                              Region clip,
                                              BasicStroke bs,
                                              boolean thin,
                                              boolean normalize,
                                              int[] bbox)
    {
        MarlinTileGenerator ptg = null;
        Renderer r = null;

        final RendererContext rdrCtx = getRendererContext();
        try {
            if (DO_CLIP || (DO_CLIP_RUNTIME_ENABLE && MarlinProperties.isDoClipAtRuntime())) {
                final double[] clipRect = rdrCtx.clipRect;

                final double rdrOffX = Renderer.RDR_OFFSET_X;
                final double rdrOffY = Renderer.RDR_OFFSET_Y;

                final double margin = 1e-3d;

                clipRect[0] = clip.getLoY()
                                - margin + rdrOffY;
                clipRect[1] = clip.getLoY() + clip.getHeight()
                                + margin + rdrOffY;
                clipRect[2] = clip.getLoX()
                                - margin + rdrOffX;
                clipRect[3] = clip.getLoX() + clip.getWidth()
                                + margin + rdrOffX;

                if (MarlinConst.DO_LOG_CLIP) {
                    MarlinUtils.logInfo("clipRect (clip): "
                                        + Arrays.toString(rdrCtx.clipRect));
                }

                rdrCtx.doClip = true;
            }

            final AffineTransform _at = (at != null && !at.isIdentity()) ? at
                                        : null;

            final NormMode norm = (normalize) ? NormMode.ON_WITH_AA : NormMode.OFF;

            if (bs == null) {
                final PathIterator pi = norm.getNormalizingPathIterator(rdrCtx,
                                                 s.getPathIterator(_at));

                r = rdrCtx.renderer.init(clip.getLoX(), clip.getLoY(),
                                         clip.getWidth(), clip.getHeight(),
                                         pi.getWindingRule());

                DPathConsumer2D pc2d = r;

                if (DO_CLIP_FILL && rdrCtx.doClip) {
                    if (DO_TRACE_PATH) {
                        pc2d = rdrCtx.transformerPC2D.traceFiller(pc2d);
                    }
                    pc2d = rdrCtx.transformerPC2D.pathClipper(pc2d);
                }

                if (DO_TRACE_PATH) {
                    pc2d = rdrCtx.transformerPC2D.traceInput(pc2d);
                }
                pathTo(rdrCtx, pi, pc2d);

            } else {
                r = rdrCtx.renderer.init(clip.getLoX(), clip.getLoY(),
                                         clip.getWidth(), clip.getHeight(),
                                         WIND_NON_ZERO);

                strokeTo(rdrCtx, s, _at, bs, thin, norm, true, r);
            }
            if (r.endRendering()) {
                ptg = rdrCtx.ptg.init();
                ptg.getBbox(bbox);
                r = null;
            }
        } finally {
            if (r != null) {
                r.dispose();
            }
        }

        return ptg;
    }

    @Override
    public AATileGenerator getAATileGenerator(double x, double y,
                                              double dx1, double dy1,
                                              double dx2, double dy2,
                                              double lw1, double lw2,
                                              Region clip,
                                              int[] bbox)
    {
        double ldx1, ldy1, ldx2, ldy2;
        boolean innerpgram = (lw1 > 0.0d && lw2 > 0.0d);

        if (innerpgram) {
            ldx1 = dx1 * lw1;
            ldy1 = dy1 * lw1;
            ldx2 = dx2 * lw2;
            ldy2 = dy2 * lw2;
            x -= (ldx1 + ldx2) / 2.0d;
            y -= (ldy1 + ldy2) / 2.0d;
            dx1 += ldx1;
            dy1 += ldy1;
            dx2 += ldx2;
            dy2 += ldy2;
            if (lw1 > 1.0d && lw2 > 1.0d) {
                innerpgram = false;
            }
        } else {
            ldx1 = ldy1 = ldx2 = ldy2 = 0.0d;
        }

        MarlinTileGenerator ptg = null;
        Renderer r = null;

        final RendererContext rdrCtx = getRendererContext();
        try {
            r = rdrCtx.renderer.init(clip.getLoX(), clip.getLoY(),
                                     clip.getWidth(), clip.getHeight(),
                                     WIND_EVEN_ODD);

            r.moveTo( x,  y);
            r.lineTo( (x+dx1),  (y+dy1));
            r.lineTo( (x+dx1+dx2),  (y+dy1+dy2));
            r.lineTo( (x+dx2),  (y+dy2));
            r.closePath();

            if (innerpgram) {
                x += ldx1 + ldx2;
                y += ldy1 + ldy2;
                dx1 -= 2.0d * ldx1;
                dy1 -= 2.0d * ldy1;
                dx2 -= 2.0d * ldx2;
                dy2 -= 2.0d * ldy2;
                r.moveTo( x,  y);
                r.lineTo( (x+dx1),  (y+dy1));
                r.lineTo( (x+dx1+dx2),  (y+dy1+dy2));
                r.lineTo( (x+dx2),  (y+dy2));
                r.closePath();
            }
            r.pathDone();

            if (r.endRendering()) {
                ptg = rdrCtx.ptg.init();
                ptg.getBbox(bbox);
                r = null;
            }
        } finally {
            if (r != null) {
                r.dispose();
            }
        }

        return ptg;
    }

    /**
     * Returns the minimum pen width that the antialiasing rasterizer
     * can represent without dropouts occurring.
     * @since 1.7
     */
    @Override
    public float getMinimumAAPenSize() {
        return MIN_PEN_SIZE;
    }

    static {
        if (PathIterator.WIND_NON_ZERO != WIND_NON_ZERO ||
            PathIterator.WIND_EVEN_ODD != WIND_EVEN_ODD ||
            BasicStroke.JOIN_MITER != JOIN_MITER ||
            BasicStroke.JOIN_ROUND != JOIN_ROUND ||
            BasicStroke.JOIN_BEVEL != JOIN_BEVEL ||
            BasicStroke.CAP_BUTT != CAP_BUTT ||
            BasicStroke.CAP_ROUND != CAP_ROUND ||
            BasicStroke.CAP_SQUARE != CAP_SQUARE)
        {
            throw new InternalError("mismatched renderer constants");
        }
    }

    private static final boolean USE_THREAD_LOCAL;

    static final int REF_TYPE;

    private static final ReentrantContextProvider<RendererContext> RDR_CTX_PROVIDER;

    static {
        USE_THREAD_LOCAL = MarlinProperties.isUseThreadLocal();

        @SuppressWarnings("removal")
        final String refType = AccessController.doPrivileged(
                            new GetPropertyAction("sun.java2d.renderer.useRef",
                            "soft"));
        switch (refType) {
            default:
            case "soft":
                REF_TYPE = ReentrantContextProvider.REF_SOFT;
                break;
            case "weak":
                REF_TYPE = ReentrantContextProvider.REF_WEAK;
                break;
            case "hard":
                REF_TYPE = ReentrantContextProvider.REF_HARD;
                break;
        }

        if (USE_THREAD_LOCAL) {
            RDR_CTX_PROVIDER = new ReentrantContextProviderTL<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        } else {
            RDR_CTX_PROVIDER = new ReentrantContextProviderCLQ<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        }
    }

    private static boolean SETTINGS_LOGGED = !ENABLE_LOGS;

    private static void logSettings(final String reClass) {
        if (SETTINGS_LOGGED) {
            return;
        }
        SETTINGS_LOGGED = true;

        String refType;
        switch (REF_TYPE) {
            default:
            case ReentrantContextProvider.REF_HARD:
                refType = "hard";
                break;
            case ReentrantContextProvider.REF_SOFT:
                refType = "soft";
                break;
            case ReentrantContextProvider.REF_WEAK:
                refType = "weak";
                break;
        }

        logInfo("=========================================================="
                + "=====================");

        logInfo("Marlin software rasterizer           = ENABLED");
        logInfo("Version                              = ["
                + Version.getVersion() + "]");
        logInfo("sun.java2d.renderer                  = "
                + reClass);
        logInfo("sun.java2d.renderer.useThreadLocal   = "
                + USE_THREAD_LOCAL);
        logInfo("sun.java2d.renderer.useRef           = "
                + refType);

        logInfo("sun.java2d.renderer.edges            = "
                + MarlinConst.INITIAL_EDGES_COUNT);
        logInfo("sun.java2d.renderer.pixelWidth       = "
                + MarlinConst.INITIAL_PIXEL_WIDTH);
        logInfo("sun.java2d.renderer.pixelHeight      = "
                + MarlinConst.INITIAL_PIXEL_HEIGHT);

        logInfo("sun.java2d.renderer.profile          = "
                + (MarlinProperties.isProfileQuality() ?
                    "quality" : "speed"));

        logInfo("sun.java2d.renderer.subPixel_log2_X  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_X);
        logInfo("sun.java2d.renderer.subPixel_log2_Y  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_Y);

        logInfo("sun.java2d.renderer.tileSize_log2    = "
                + MarlinConst.TILE_H_LG);
        logInfo("sun.java2d.renderer.tileWidth_log2   = "
                + MarlinConst.TILE_W_LG);
        logInfo("sun.java2d.renderer.blockSize_log2   = "
                + MarlinConst.BLOCK_SIZE_LG);


        logInfo("sun.java2d.renderer.forceRLE         = "
                + MarlinProperties.isForceRLE());
        logInfo("sun.java2d.renderer.forceNoRLE       = "
                + MarlinProperties.isForceNoRLE());
        logInfo("sun.java2d.renderer.useTileFlags     = "
                + MarlinProperties.isUseTileFlags());
        logInfo("sun.java2d.renderer.useTileFlags.useHeuristics = "
                + MarlinProperties.isUseTileFlagsWithHeuristics());
        logInfo("sun.java2d.renderer.rleMinWidth      = "
                + MarlinCache.RLE_MIN_WIDTH);

        logInfo("sun.java2d.renderer.useSimplifier    = "
                + MarlinConst.USE_SIMPLIFIER);
        logInfo("sun.java2d.renderer.usePathSimplifier= "
                + MarlinConst.USE_PATH_SIMPLIFIER);
        logInfo("sun.java2d.renderer.pathSimplifier.pixTol = "
                + MarlinProperties.getPathSimplifierPixelTolerance());

        logInfo("sun.java2d.renderer.stroker.joinError= "
                + MarlinProperties.getStrokerJoinError());
        logInfo("sun.java2d.renderer.stroker.joinStyle= "
                + MarlinProperties.getStrokerJoinStyle());

        logInfo("sun.java2d.renderer.clip             = "
                + MarlinProperties.isDoClip());
        logInfo("sun.java2d.renderer.clip.runtime.enable = "
                + MarlinProperties.isDoClipRuntimeFlag());

        logInfo("sun.java2d.renderer.clip.subdivider  = "
                + MarlinProperties.isDoClipSubdivider());
        logInfo("sun.java2d.renderer.clip.subdivider.minLength = "
                + MarlinProperties.getSubdividerMinLength());

        logInfo("sun.java2d.renderer.doStats          = "
                + MarlinConst.DO_STATS);
        logInfo("sun.java2d.renderer.doMonitors       = "
                + MarlinConst.DO_MONITORS);
        logInfo("sun.java2d.renderer.doChecks         = "
                + MarlinConst.DO_CHECKS);

        logInfo("sun.java2d.renderer.skip_rdr         = "
                + MarlinProperties.isSkipRenderer());
        logInfo("sun.java2d.renderer.skip_pipe        = "
                + MarlinProperties.isSkipRenderTiles());

        logInfo("sun.java2d.renderer.useLogger        = "
                + MarlinConst.USE_LOGGER);
        logInfo("sun.java2d.renderer.logCreateContext = "
                + MarlinConst.LOG_CREATE_CONTEXT);
        logInfo("sun.java2d.renderer.logUnsafeMalloc  = "
                + MarlinConst.LOG_UNSAFE_MALLOC);

        logInfo("sun.java2d.renderer.curve_len_err    = "
                + MarlinProperties.getCurveLengthError());
        logInfo("sun.java2d.renderer.cubic_dec_d2     = "
                + MarlinProperties.getCubicDecD2());
        logInfo("sun.java2d.renderer.cubic_inc_d1     = "
                + MarlinProperties.getCubicIncD1());
        logInfo("sun.java2d.renderer.quad_dec_d2      = "
                + MarlinProperties.getQuadDecD2());

        logInfo("Renderer settings:");
        logInfo("SORT         = " + MergeSort.SORT_TYPE);
        logInfo("CUB_DEC_BND  = " + Renderer.CUB_DEC_BND);
        logInfo("CUB_INC_BND  = " + Renderer.CUB_INC_BND);
        logInfo("QUAD_DEC_BND = " + Renderer.QUAD_DEC_BND);

        logInfo("INITIAL_EDGES_CAPACITY               = "
                + MarlinConst.INITIAL_EDGES_CAPACITY);
        logInfo("INITIAL_CROSSING_COUNT               = "
                + Renderer.INITIAL_CROSSING_COUNT);

        logInfo("=========================================================="
                + "=====================");
    }

    /**
     * Get the RendererContext instance dedicated to the current thread
     * @return RendererContext instance
     */
    @SuppressWarnings({"unchecked"})
    static RendererContext getRendererContext() {
        final RendererContext rdrCtx = RDR_CTX_PROVIDER.acquire();
        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.start();
        }
        return rdrCtx;
    }

    /**
     * Reset and return the given RendererContext instance for reuse
     * @param rdrCtx RendererContext instance
     */
    static void returnRendererContext(final RendererContext rdrCtx) {
        rdrCtx.dispose();

        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.stop();
        }
        RDR_CTX_PROVIDER.release(rdrCtx);
    }
}
