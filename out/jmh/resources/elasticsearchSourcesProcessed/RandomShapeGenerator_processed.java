/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.legacygeo.test;

import com.carrotsearch.randomizedtesting.generators.RandomNumbers;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.legacygeo.builders.CoordinatesBuilder;
import org.elasticsearch.legacygeo.builders.GeometryCollectionBuilder;
import org.elasticsearch.legacygeo.builders.LineStringBuilder;
import org.elasticsearch.legacygeo.builders.MultiLineStringBuilder;
import org.elasticsearch.legacygeo.builders.MultiPointBuilder;
import org.elasticsearch.legacygeo.builders.PointBuilder;
import org.elasticsearch.legacygeo.builders.PolygonBuilder;
import org.elasticsearch.legacygeo.builders.ShapeBuilder;
import org.junit.Assert;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.impl.Range;

import java.util.Random;

import static org.locationtech.spatial4j.shape.SpatialRelation.CONTAINS;

/**
 * Random geoshape generation utilities for randomized {@code geo_shape} type testing
 * depends on jts and spatial4j
 */
public class RandomShapeGenerator extends RandomGeoGenerator {

    protected static JtsSpatialContext ctx = ShapeBuilder.SPATIAL_CONTEXT;
    protected static final double xDIVISIBLE = 2;
    protected static boolean ST_VALIDATE = true;

    public enum ShapeType {
        POINT,
        MULTIPOINT,
        LINESTRING,
        MULTILINESTRING,
        POLYGON;

        private static final ShapeType[] types = values();

        public static ShapeType randomType(Random r) {
            return types[RandomNumbers.randomIntBetween(r, 0, types.length - 1)];
        }
    }

    public static ShapeBuilder<?, ?, ?> createShape(Random r) throws InvalidShapeException {
        return createShapeNear(r, null);
    }

    public static ShapeBuilder<?, ?, ?> createShape(Random r, ShapeType st) {
        return createShapeNear(r, null, st);
    }

    public static ShapeBuilder<?, ?, ?> createShapeNear(Random r, Point nearPoint) throws InvalidShapeException {
        return createShape(r, nearPoint, null, null);
    }

    public static ShapeBuilder<?, ?, ?> createShapeNear(Random r, Point nearPoint, ShapeType st) throws InvalidShapeException {
        return createShape(r, nearPoint, null, st);
    }

    public static ShapeBuilder<?, ?, ?> createShapeWithin(Random r, Rectangle bbox) throws InvalidShapeException {
        return createShape(r, null, bbox, null);
    }

    public static ShapeBuilder<?, ?, ?> createShapeWithin(Random r, Rectangle bbox, ShapeType st) throws InvalidShapeException {
        return createShape(r, null, bbox, st);
    }

    public static GeometryCollectionBuilder createGeometryCollection(Random r) throws InvalidShapeException {
        return createGeometryCollection(r, null, null, 0);
    }

    public static GeometryCollectionBuilder createGeometryCollectionNear(Random r, Point nearPoint) throws InvalidShapeException {
        return createGeometryCollection(r, nearPoint, null, 0);
    }

    public static GeometryCollectionBuilder createGeometryCollectionNear(Random r, Point nearPoint, int size) throws InvalidShapeException {
        return createGeometryCollection(r, nearPoint, null, size);
    }

    public static GeometryCollectionBuilder createGeometryCollectionWithin(Random r, Rectangle within) throws InvalidShapeException {
        return createGeometryCollection(r, null, within, 0);
    }

    public static GeometryCollectionBuilder createGeometryCollectionWithin(Random r, Rectangle within, int size)
        throws InvalidShapeException {
        return createGeometryCollection(r, null, within, size);
    }

    protected static GeometryCollectionBuilder createGeometryCollection(Random r, Point nearPoint, Rectangle bounds, int numGeometries)
        throws InvalidShapeException {
        if (numGeometries <= 0) {
            numGeometries = RandomNumbers.randomIntBetween(r, 2, 4);
        }

        if (nearPoint == null) {
            nearPoint = xRandomPoint(r);
        }

        if (bounds == null) {
            bounds = xRandomRectangle(r, nearPoint);
        }

        GeometryCollectionBuilder gcb = new GeometryCollectionBuilder();
        for (int i = 0; i < numGeometries;) {
            ShapeBuilder<?, ?, ?> builder = createShapeWithin(r, bounds);
            if (builder != null) {
                gcb.shape(builder);
                ++i;
            }
        }
        return gcb;
    }

    private static ShapeBuilder<?, ?, ?> createShape(Random r, Point nearPoint, Rectangle within, ShapeType st)
        throws InvalidShapeException {
        ShapeBuilder<?, ?, ?> shape;
        short i = 0;
        do {
            shape = createShape(r, nearPoint, within, st, ST_VALIDATE);
            if (shape != null) {
                return shape;
            }
        } while (++i != 100);
        throw new InvalidShapeException("Unable to create a valid random shape with provided seed");
    }

    /**
     * Creates a random shape useful for randomized testing, NOTE: exercise caution when using this to build random GeometryCollections
     * as creating a large random number of random shapes can result in massive resource consumption
     *
     * The following options are included
     * @param nearPoint Create a shape near a provided point
     * @param within Create a shape within the provided rectangle (note: if not null this will override the provided point)
     * @param st Create a random shape of the provided type
     * @return the ShapeBuilder for a random shape
     */
    private static ShapeBuilder<?, ?, ?> createShape(Random r, Point nearPoint, Rectangle within, ShapeType st, boolean validate)
        throws InvalidShapeException {

        if (st == null) {
            st = ShapeType.randomType(r);
        }

        if (within == null) {
            within = xRandomRectangle(r, nearPoint);
        }

        switch (st) {
            case POINT:
                Point p = xRandomPointIn(r, within);
                PointBuilder pb = new PointBuilder().coordinate(new Coordinate(p.getX(), p.getY(), Double.NaN));
                return pb;
            case MULTIPOINT:
            case LINESTRING:
                int numPoints = RandomNumbers.randomIntBetween(r, 3, 10);
                CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();
                for (int i = 0; i < numPoints; ++i) {
                    p = xRandomPointIn(r, within);
                    coordinatesBuilder.coordinate(p.getX(), p.getY());
                }
                ShapeBuilder<?, ?, ?> pcb = (st == ShapeType.MULTIPOINT)
                    ? new MultiPointBuilder(coordinatesBuilder.build())
                    : new LineStringBuilder(coordinatesBuilder);
                return pcb;
            case MULTILINESTRING:
                MultiLineStringBuilder mlsb = new MultiLineStringBuilder();
                for (int i = 0; i < RandomNumbers.randomIntBetween(r, 1, 10); ++i) {
                    mlsb.linestring((LineStringBuilder) createShape(r, nearPoint, within, ShapeType.LINESTRING, false));
                }
                return mlsb;
            case POLYGON:
                numPoints = RandomNumbers.randomIntBetween(r, 5, 25);
                Coordinate[] coordinates = new Coordinate[numPoints];
                for (int i = 0; i < numPoints; ++i) {
                    p = (Point) createShape(r, nearPoint, within, ShapeType.POINT, false).buildS4J();
                    coordinates[i] = new Coordinate(p.getX(), p.getY());
                }
                Geometry shell = new ConvexHull(coordinates, ctx.getGeometryFactory()).getConvexHull();
                Coordinate[] shellCoords = shell.getCoordinates();
                if (shellCoords.length < 3) {
                    shellCoords = new Coordinate[4];
                    shellCoords[0] = new Coordinate(within.getMinX(), within.getMinY());
                    shellCoords[1] = new Coordinate(within.getMinX(), within.getMaxY());
                    shellCoords[2] = new Coordinate(within.getMaxX(), within.getMaxY());
                    shellCoords[3] = new Coordinate(within.getMaxX(), within.getMinY());
                }
                PolygonBuilder pgb = new PolygonBuilder(new CoordinatesBuilder().coordinates(shellCoords).close());
                if (validate) {
                    try {
                        pgb.buildS4J();
                    } catch (AssertionError | InvalidShapeException e) {
                        return null;
                    }
                }
                return pgb;
            default:
                throw new ElasticsearchException("Unable to create shape of type [" + st + "]");
        }
    }

    public static Point xRandomPoint(Random r) {
        return xRandomPointIn(r, ctx.getWorldBounds());
    }

    protected static Point xRandomPointIn(Random rand, Rectangle r) {
        double[] pt = new double[2];
        RandomGeoGenerator.randomPointIn(rand, r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY(), pt);
        Point p = ctx.makePoint(pt[0], pt[1]);
        Assert.assertEquals(CONTAINS, r.relate(p));
        return p;
    }

    private static Rectangle xRandomRectangle(Random r, Point nearP, Rectangle bounds, boolean small) {
        if (nearP == null) nearP = xRandomPointIn(r, bounds);

        if (small) {
            final double latRange = 3 * r.nextDouble() + 3;
            final double lonRange = 3 * r.nextDouble() + 3;

            double minX = nearP.getX();
            double maxX = minX + lonRange;
            if (maxX > 180) {
                maxX = minX;
                minX -= lonRange;
            }
            double minY = nearP.getY();
            double maxY = nearP.getY() + latRange;
            if (maxY > 90) {
                maxY = minY;
                minY -= latRange;
            }

            return ctx.makeRectangle(minX, maxX, minY, maxY);
        }

        Range xRange = xRandomRange(r, rarely(r) ? 0 : nearP.getX(), Range.xRange(bounds, ctx));
        Range yRange = xRandomRange(r, rarely(r) ? 0 : nearP.getY(), Range.yRange(bounds, ctx));

        return xMakeNormRect(
            xDivisible(xRange.getMin() * 10e3) / 10e3,
            xDivisible(xRange.getMax() * 10e3) / 10e3,
            xDivisible(yRange.getMin() * 10e3) / 10e3,
            xDivisible(yRange.getMax() * 10e3) / 10e3
        );
    }

    /** creates a small random rectangle by default to keep shape test performance at bay */
    public static Rectangle xRandomRectangle(Random r, Point nearP) {
        return xRandomRectangle(r, nearP, ctx.getWorldBounds(), true);
    }

    public static Rectangle xRandomRectangle(Random r, Point nearP, boolean small) {
        return xRandomRectangle(r, nearP, ctx.getWorldBounds(), small);
    }

    private static boolean rarely(Random r) {
        return r.nextInt(100) >= 90;
    }

    private static Range xRandomRange(Random r, double near, Range bounds) {
        double mid = near + r.nextGaussian() * bounds.getWidth() / 6;
        double width = Math.abs(r.nextGaussian()) * bounds.getWidth() / 6;
        return new Range(mid - width / 2, mid + width / 2);
    }

    private static double xDivisible(double v, double divisible) {
        return (int) (Math.round(v / divisible) * divisible);
    }

    private static double xDivisible(double v) {
        return xDivisible(v, xDIVISIBLE);
    }

    protected static Rectangle xMakeNormRect(double minX, double maxX, double minY, double maxY) {
        minX = DistanceUtils.normLonDEG(minX);
        maxX = DistanceUtils.normLonDEG(maxX);

        if (maxX < minX) {
            double t = minX;
            minX = maxX;
            maxX = t;
        }

        double minWorldY = ctx.getWorldBounds().getMinY();
        double maxWorldY = ctx.getWorldBounds().getMaxY();
        if (minY < minWorldY || minY > maxWorldY) {
            minY = DistanceUtils.normLatDEG(minY);
        }
        if (maxY < minWorldY || maxY > maxWorldY) {
            maxY = DistanceUtils.normLatDEG(maxY);
        }
        if (maxY < minY) {
            double t = minY;
            minY = maxY;
            maxY = t;
        }
        return ctx.makeRectangle(minX, maxX, minY, maxY);
    }
}
