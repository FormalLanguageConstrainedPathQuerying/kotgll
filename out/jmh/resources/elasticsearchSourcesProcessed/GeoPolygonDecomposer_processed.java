/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.geo;

import org.elasticsearch.core.Tuple;
import org.elasticsearch.geometry.LinearRing;
import org.elasticsearch.geometry.MultiPolygon;
import org.elasticsearch.geometry.Point;
import org.elasticsearch.geometry.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.common.geo.GeoUtils.normalizeLat;
import static org.elasticsearch.common.geo.GeoUtils.normalizeLon;

/**
 * Splits polygons by datelines.
 */
class GeoPolygonDecomposer {

    private static final double DATELINE = 180;
    private static final Comparator<Edge> INTERSECTION_ORDER = Comparator.comparingDouble(o -> o.intersect.getY());

    private GeoPolygonDecomposer() {
    }

    public static boolean needsDecomposing(Polygon polygon) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        LinearRing linearRing = polygon.getPolygon();
        for (int i = 0; i < linearRing.length(); i++) {
            if (GeoUtils.needsNormalizeLat(linearRing.getLat(i)) || GeoUtils.needsNormalizeLon(linearRing.getLon(i))) {
                return true;
            }
            minX = Math.min(minX, linearRing.getLon(i));
            maxX = Math.max(maxX, linearRing.getLon(i));
        }
        final double rng = maxX - minX;
        return rng > DATELINE && rng != 2 * DATELINE;
    }

    public static void decomposeMultiPolygon(MultiPolygon multiPolygon, boolean orientation, List<Polygon> collector) {
        for (Polygon polygon : multiPolygon) {
            decomposePolygon(polygon, orientation, collector);
        }
    }

    /**
     * Splits the specified polygon by datelines and adds them to the supplied polygon array
     */
    public static void decomposePolygon(Polygon polygon, boolean orientation, List<Polygon> collector) {
        if (polygon.isEmpty()) {
            return;
        }
        LinearRing shell = filterRing(polygon.getPolygon());
        LinearRing[] holes = new LinearRing[polygon.getNumberOfHoles()];
        int numEdges = shell.length() - 1; 
        for (int i = 0; i < polygon.getNumberOfHoles(); i++) {
            holes[i] = filterRing(polygon.getHole(i));
            numEdges += holes[i].length() - 1;
            validateHole(shell, holes[i]);
        }

        Edge[] edges = new Edge[numEdges];
        Edge[] holeComponents = new Edge[holes.length];
        final AtomicBoolean translated = new AtomicBoolean(false);
        int offset = createEdges(0, orientation, shell, null, edges, 0, translated);
        for (int i = 0; i < polygon.getNumberOfHoles(); i++) {
            int length = createEdges(i + 1, orientation, shell, holes[i], edges, offset, translated);
            holeComponents[i] = edges[offset];
            offset += length;
        }

        int numHoles = holeComponents.length;

        numHoles = merge(edges, 0, intersections(+DATELINE, edges), holeComponents, numHoles);
        numHoles = merge(edges, 0, intersections(-DATELINE, edges), holeComponents, numHoles);

        compose(edges, holeComponents, numHoles, collector);
    }

    /**
     * This method removes duplicated points and coplanar points on vertical lines (vertical lines
     * do not cross the dateline).
     */
    private static LinearRing filterRing(LinearRing linearRing) {
        int numPoints = linearRing.length();
        int count = 2;
        for (int i = 1; i < numPoints - 1; i++) {
            if (skipPoint(linearRing, i) == false) {
                count++;
            }
        }
        if (numPoints == count) {
            return linearRing;
        }
        double[] lons = new double[count];
        double[] lats = new double[count];
        lats[0] = lats[count - 1] = linearRing.getLat(0);
        lons[0] = lons[count - 1] = linearRing.getLon(0);
        count = 0;
        for (int i = 1; i < numPoints - 1; i++) {
            if (skipPoint(linearRing, i) == false) {
                count++;
                lats[count] = linearRing.getLat(i);
                lons[count] = linearRing.getLon(i);
            }
        }
        return new LinearRing(lons, lats);
    }

    private static boolean skipPoint(LinearRing linearRing, int i) {
        if (linearRing.getLon(i - 1) == linearRing.getLon(i)) {
            if (linearRing.getLat(i - 1) == linearRing.getLat(i)) {
                return true;
            }
            if (linearRing.getLon(i - 1) == linearRing.getLon(i + 1)
                && linearRing.getLat(i - 1) > linearRing.getLat(i) != linearRing.getLat(i + 1) > linearRing.getLat(i)) {
                return true;
            }
        }
        return false;
    }

    private static void validateHole(LinearRing shell, LinearRing hole) {
        Set<Point> exterior = new HashSet<>();
        Set<Point> interior = new HashSet<>();
        for (int i = 0; i < shell.length(); i++) {
            exterior.add(new Point(shell.getX(i), shell.getY(i)));
        }
        for (int i = 0; i < hole.length(); i++) {
            interior.add(new Point(hole.getX(i), hole.getY(i)));
        }
        exterior.retainAll(interior);
        if (exterior.size() >= 2) {
            throw new IllegalArgumentException("Invalid polygon, interior cannot share more than one point with the exterior");
        }
    }

    private static int createEdges(
        int component,
        boolean orientation,
        LinearRing shell,
        LinearRing hole,
        Edge[] edges,
        int offset,
        final AtomicBoolean translated
    ) {
        boolean direction = (component == 0 ^ orientation);
        Point[] points = (hole != null) ? points(hole) : points(shell);
        ring(component, direction, orientation == false, points, 0, edges, offset, points.length - 1, translated);
        return points.length - 1;
    }

    private static Point[] points(LinearRing linearRing) {
        Point[] points = new Point[linearRing.length()];
        for (int i = 0; i < linearRing.length(); i++) {
            points[i] = new Point(linearRing.getX(i), linearRing.getY(i));
        }
        return points;
    }

    /**
     * Create a connected list of a list of coordinates
     *
     * @param points array of point
     * @param offset index of the first point
     * @param length number of points
     * @return Array of edges
     */
    private static Edge[] ring(
        int component,
        boolean direction,
        boolean handedness,
        Point[] points,
        int offset,
        Edge[] edges,
        int toffset,
        int length,
        final AtomicBoolean translated
    ) {
        double signedArea = 0;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = offset; i < offset + length; i++) {
            signedArea += points[i].getX() * points[i + 1].getY() - points[i].getY() * points[i + 1].getX();
            minX = Math.min(minX, points[i].getX());
            maxX = Math.max(maxX, points[i].getX());
        }
        boolean orientation = signedArea == 0 ? handedness : signedArea < 0;


        final double rng = maxX - minX;
        boolean incorrectOrientation = component == 0 && handedness != orientation;
        if ((incorrectOrientation && (rng > DATELINE && rng != 2 * DATELINE)) || (translated.get() && component != 0)) {
            translate(points);
            if (component == 0) {
                translated.set(true);
            }
            if (component == 0 || (component != 0 && handedness == orientation)) {
                orientation = orientation == false;
            }
        }
        return concat(component, direction ^ orientation, points, offset, edges, toffset, length);
    }

    /**
     * Transforms coordinates in the eastern hemisphere (-180:0) to a (180:360) range
     */
    private static void translate(Point[] points) {
        for (int i = 0; i < points.length; i++) {
            if (points[i].getX() < 0) {
                points[i] = new Point(points[i].getX() + 2 * DATELINE, points[i].getY());
            }
        }
    }

    private static int merge(Edge[] intersections, int offset, int length, Edge[] holes, int numHoles) {

        for (int i = 0; i < length; i += 2) {
            Edge e1 = intersections[offset + i + 0];
            Edge e2 = intersections[offset + i + 1];

            if (e2.component > 0) {
                numHoles--;
                holes[e2.component - 1] = holes[numHoles];
                holes[numHoles] = null;
            }
            if (e1.intersect != Edge.MAX_COORDINATE
                && e2.intersect != Edge.MAX_COORDINATE
                && (e1.next.next.coordinate.equals(e2.coordinate)
                    && Math.abs(e1.next.coordinate.getX()) == DATELINE
                    && Math.abs(e2.coordinate.getX()) == DATELINE) == false) {
                connect(e1, e2);
            }
        }
        return numHoles;
    }

    private static void connect(Edge in, Edge out) {
        assert in != null && out != null;
        assert in != out;
        if (in.intersect != in.next.coordinate) {
            Edge e1 = new Edge(in.intersect, in.next);

            if (out.intersect != out.next.coordinate) {
                Edge e2 = new Edge(out.intersect, out.next);
                in.next = new Edge(in.intersect, e2, in.intersect);
            } else {
                in.next = new Edge(in.intersect, out.next, in.intersect);
            }
            out.next = new Edge(out.intersect, e1, out.intersect);
        } else if (in.next != out && in.coordinate != out.intersect) {
            Edge e2 = new Edge(out.intersect, in.next, out.intersect);

            if (out.intersect != out.next.coordinate) {
                Edge e1 = new Edge(out.intersect, out.next);
                in.next = new Edge(in.intersect, e1, in.intersect);

            } else {
                in.next = new Edge(in.intersect, out.next, in.intersect);
            }
            out.next = e2;
        }
    }

    /**
     * Concatenate a set of points to a polygon
     *
     * @param component   component id of the polygon
     * @param direction   direction of the ring
     * @param points      list of points to concatenate
     * @param pointOffset index of the first point
     * @param edges       Array of edges to write the result to
     * @param edgeOffset  index of the first edge in the result
     * @param length      number of points to use
     * @return the edges creates
     */
    private static Edge[] concat(
        int component,
        boolean direction,
        Point[] points,
        final int pointOffset,
        Edge[] edges,
        final int edgeOffset,
        int length
    ) {
        assert edges.length >= length + edgeOffset;
        assert points.length >= length + pointOffset;
        edges[edgeOffset] = new Edge(new Point(points[pointOffset].getX(), points[pointOffset].getY()), null);
        for (int i = 1; i < length; i++) {
            Point nextPoint = new Point(points[pointOffset + i].getX(), points[pointOffset + i].getY());
            if (direction) {
                edges[edgeOffset + i] = new Edge(nextPoint, edges[edgeOffset + i - 1]);
                edges[edgeOffset + i].component = component;
            } else if (edges[edgeOffset + i - 1].coordinate.equals(nextPoint) == false) {
                edges[edgeOffset + i - 1].next = edges[edgeOffset + i] = new Edge(nextPoint, null);
                edges[edgeOffset + i - 1].component = component;
            } else {
                throw new IllegalArgumentException("Provided shape has duplicate consecutive coordinates at: (" + nextPoint + ")");
            }
        }

        if (direction) {
            edges[edgeOffset].setNext(edges[edgeOffset + length - 1]);
            edges[edgeOffset].component = component;
        } else {
            edges[edgeOffset + length - 1].setNext(edges[edgeOffset]);
            edges[edgeOffset + length - 1].component = component;
        }

        return edges;
    }

    /**
     * Calculate all intersections of line segments and a vertical line. The
     * Array of edges will be ordered asc by the y-coordinate of the
     * intersections of edges.
     *
     * @param dateline x-coordinate of the dateline
     * @param edges    set of edges that may intersect with the dateline
     * @return number of intersecting edges
     */
    private static int intersections(double dateline, Edge[] edges) {
        int numIntersections = 0;
        assert Double.isNaN(dateline) == false;
        int maxComponent = 0;
        for (int i = 0; i < edges.length; i++) {
            Point p1 = edges[i].coordinate;
            Point p2 = edges[i].next.coordinate;
            assert Double.isNaN(p2.getX()) == false && Double.isNaN(p1.getX()) == false;
            edges[i].intersect = Edge.MAX_COORDINATE;

            double position = intersection(p1.getX(), p2.getX(), dateline);
            if (Double.isNaN(position) == false) {
                edges[i].setIntersection(position, dateline);
                numIntersections++;
                maxComponent = Math.max(maxComponent, edges[i].component);
            }
        }
        if (maxComponent > 0) {
            for (int i = 0; i < maxComponent; i++) {
                if (clearComponentTouchingDateline(edges, i + 1)) {
                    numIntersections--;
                }
            }
        }
        Arrays.sort(edges, INTERSECTION_ORDER);
        return numIntersections;
    }

    /**
     * Checks the number of dateline intersections detected for a component. If there is only
     * one, it clears it as it means that the component just touches the dateline.
     *
     * @param edges    set of edges that may intersect with the dateline
     * @param component    The component to check
     * @return true if the component touches the dateline.
     */
    private static boolean clearComponentTouchingDateline(Edge[] edges, int component) {
        Edge intersection = null;
        for (int j = 0; j < edges.length; j++) {
            if (edges[j].intersect != Edge.MAX_COORDINATE && edges[j].component == component) {
                if (intersection == null) {
                    intersection = edges[j];
                } else {
                    return false;
                }
            }
        }
        if (intersection != null) {
            intersection.intersect = Edge.MAX_COORDINATE;
        }
        return intersection != null;
    }

    private static Edge[] edges(Edge[] edges, int numHoles, List<List<Point[]>> components) {
        ArrayList<Edge> mainEdges = new ArrayList<>(edges.length);

        for (int i = 0; i < edges.length; i++) {
            if (edges[i].component >= 0) {
                double[] partitionPoint = new double[3];
                int length = component(edges[i], -(components.size() + numHoles + 1), mainEdges, partitionPoint);
                List<Point[]> component = new ArrayList<>();
                component.add(coordinates(edges[i], new Point[length + 1], partitionPoint));
                components.add(component);
            }
        }

        return mainEdges.toArray(new Edge[mainEdges.size()]);
    }

    private static void compose(Edge[] edges, Edge[] holes, int numHoles, List<Polygon> collector) {
        final List<List<Point[]>> components = new ArrayList<>();
        assign(holes, holes(holes, numHoles), numHoles, edges(edges, numHoles, components), components);
        buildPoints(components, collector);
    }

    private static void assign(Edge[] holes, Point[][] points, int numHoles, Edge[] edges, List<List<Point[]>> components) {
        for (int i = 0; i < numHoles; i++) {

            final Edge current = new Edge(holes[i].coordinate, holes[i].next);
            current.intersect = current.coordinate;
            final int intersections = intersections(current.coordinate.getX(), edges);

            if (intersections == 0) {
                throw new IllegalArgumentException("Invalid shape: Hole is not within polygon");
            }


            final int pos;
            boolean sharedVertex = false;
            if (((pos = Arrays.binarySearch(edges, 0, intersections, current, INTERSECTION_ORDER)) >= 0)
                && (sharedVertex = (edges[pos].intersect.equals(current.coordinate))) == false) {

                throw new IllegalArgumentException("Invalid shape: Hole is not within polygon");
            }

            final int index;
            if (sharedVertex) {
                index = 0; 
            } else if (pos == -1) {
                index = 0;
            } else {
                index = -(pos + 2);
            }

            final int component = -edges[index].component - numHoles - 1;

            components.get(component).add(points[i]);
        }
    }

    /**
     * This method sets the component id of all edges in a ring to a given id and shifts the
     * coordinates of this component according to the dateline
     *
     * @param edge  An arbitrary edge of the component
     * @param id    id to apply to the component
     * @param edges a list of edges to which all edges of the component will be added (could be <code>null</code>)
     * @return number of edges that belong to this component
     */
    private static int component(final Edge edge, final int id, final ArrayList<Edge> edges, double[] partitionPoint) {
        Edge any = edge;
        while (any.coordinate.getX() == +DATELINE || any.coordinate.getX() == -DATELINE) {
            if ((any = any.next) == edge) {
                break;
            }
        }

        double shiftOffset = any.coordinate.getX() > DATELINE ? DATELINE : (any.coordinate.getX() < -DATELINE ? -DATELINE : 0);

        int length = 0, connectedComponents = 0;
        int splitIndex = 1;
        Edge current = edge;
        Edge prev = edge;
        HashMap<Point, Tuple<Edge, Edge>> visitedEdge = new HashMap<>();
        do {
            current.coordinate = shift(current.coordinate, shiftOffset);
            current.component = id;

            if (edges != null) {
                if (visitedEdge.containsKey(current.coordinate)) {
                    partitionPoint[0] = current.coordinate.getX();
                    partitionPoint[1] = current.coordinate.getY();
                    partitionPoint[2] = current.coordinate.getZ();
                    if (connectedComponents > 0 && current.next != edge) {
                        throw new IllegalArgumentException("Shape contains more than one shared point");
                    }

                    final int visitID = -id;
                    Edge firstAppearance = visitedEdge.get(current.coordinate).v2();
                    Edge temp = firstAppearance.next;
                    firstAppearance.next = current.next;
                    current.next = temp;
                    current.component = visitID;
                    do {
                        prev.component = visitID;
                        prev = visitedEdge.get(prev.coordinate).v1();
                        ++splitIndex;
                    } while (current.coordinate.equals(prev.coordinate) == false);
                    ++connectedComponents;
                } else {
                    visitedEdge.put(current.coordinate, new Tuple<Edge, Edge>(prev, current));
                }
                edges.add(current);
                prev = current;
            }
            length++;
        } while (connectedComponents == 0 && (current = current.next) != edge);

        return (splitIndex != 1) ? length - splitIndex : length;
    }

    /**
     * Compute all coordinates of a component
     *
     * @param component   an arbitrary edge of the component
     * @param coordinates Array of coordinates to write the result to
     * @return the coordinates parameter
     */
    private static Point[] coordinates(Edge component, Point[] coordinates, double[] partitionPoint) {
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = (component = component.next).coordinate;
        }
        if (coordinates[0].equals(coordinates[coordinates.length - 1]) == false) {
            if (Double.isNaN(partitionPoint[2])) {
                throw new IllegalArgumentException(
                    "Self-intersection at or near point [" + partitionPoint[0] + "," + partitionPoint[1] + "]"
                );
            } else {
                throw new IllegalArgumentException(
                    "Self-intersection at or near point [" + partitionPoint[0] + "," + partitionPoint[1] + "," + partitionPoint[2] + "]"
                );
            }
        }
        return coordinates;
    }

    private static void buildPoints(List<List<Point[]>> components, List<Polygon> collector) {
        for (List<Point[]> component : components) {
            collector.add(buildPolygon(component));
        }
    }

    private static Polygon buildPolygon(List<Point[]> polygon) {
        List<LinearRing> holes;
        Point[] shell = polygon.get(0);
        if (polygon.size() > 1) {
            holes = new ArrayList<>(polygon.size() - 1);
            for (int i = 1; i < polygon.size(); ++i) {
                Point[] coords = polygon.get(i);
                double[] x = new double[coords.length];
                double[] y = new double[coords.length];
                for (int c = 0; c < coords.length; ++c) {
                    x[c] = normalizeLon(coords[c].getX());
                    y[c] = normalizeLat(coords[c].getY());
                }
                holes.add(new LinearRing(x, y));
            }
        } else {
            holes = Collections.emptyList();
        }

        double[] x = new double[shell.length];
        double[] y = new double[shell.length];
        for (int i = 0; i < shell.length; ++i) {
            x[i] = normalizeLonMinus180Inclusive(shell[i].getX());
            y[i] = normalizeLat(shell[i].getY());
        }

        return new Polygon(new LinearRing(x, y), holes);
    }

    private static Point[][] holes(Edge[] holes, int numHoles) {
        if (numHoles == 0) {
            return new Point[0][];
        }
        final Point[][] points = new Point[numHoles][];

        for (int i = 0; i < numHoles; i++) {
            double[] partitionPoint = new double[3];
            int length = component(holes[i], -(i + 1), null, partitionPoint); 
            points[i] = coordinates(holes[i], new Point[length + 1], partitionPoint);
        }

        return points;
    }

    /**
     * Normalizes longitude while accepting -180 degrees as a valid value
     */
    private static double normalizeLonMinus180Inclusive(double lon) {
        return Math.abs(lon) > 180 ? normalizeLon(lon) : lon;
    }

    private static Point shift(Point coordinate, double dateline) {
        if (dateline == 0) {
            return coordinate;
        } else {
            return new Point(-2 * dateline + coordinate.getX(), coordinate.getY());
        }
    }

    /**
     * Calculate the intersection of a line segment and a vertical dateline.
     *
     * @param p1x      longitude of the start-point of the line segment
     * @param p2x      longitude of the end-point of the line segment
     * @param dateline x-coordinate of the vertical dateline
     * @return position of the intersection in the open range (0..1] if the line
     * segment intersects with the line segment. Otherwise this method
     * returns {@link Double#NaN}
     */
    private static double intersection(double p1x, double p2x, double dateline) {
        if (p1x == p2x && p1x != dateline) {
            return Double.NaN;
        } else if (p1x == p2x && p1x == dateline) {
            return 1.0;
        } else {
            final double t = (dateline - p1x) / (p2x - p1x);
            if (t > 1 || t <= 0) {
                return Double.NaN;
            } else {
                return t;
            }
        }
    }

    /**
     * This helper class implements a linked list for {@link Point}. It contains
     * fields for a dateline intersection and component id
     */
    private static final class Edge {
        Point coordinate; 
        Edge next; 
        Point intersect; 
        int component = -1; 
        static final Point MAX_COORDINATE = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        Edge(Point coordinate, Edge next, Point intersection) {
            this.coordinate = coordinate;
            this.setNext(next);
            this.intersect = intersection;
            if (next != null) {
                this.component = next.component;
            }
        }

        Edge(Point coordinate, Edge next) {
            this(coordinate, next, Edge.MAX_COORDINATE);
        }

        void setNext(Edge next) {
            if (next != null) {
                if (this.coordinate.equals(next.coordinate)) {
                    throw new IllegalArgumentException("Provided shape has duplicate consecutive coordinates at: " + this.coordinate);
                }
                this.next = next;
            }
        }

        /**
         * Set the intersection of this line segment with the given dateline
         *
         * @param position position of the intersection [0..1]
         * @param dateline of the intersection
         */
        void setIntersection(double position, double dateline) {
            if (position == 0) {
                this.intersect = coordinate;
            } else if (position == 1) {
                this.intersect = next.coordinate;
            } else {
                final double y = coordinate.getY() + position * (next.coordinate.getY() - coordinate.getY());
                this.intersect = new Point(dateline, y);
            }
        }

        @Override
        public String toString() {
            return "Edge[Component=" + component + "; start=" + coordinate + " " + "; intersection=" + intersect + "]";
        }
    }
}
