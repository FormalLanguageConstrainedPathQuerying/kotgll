/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.geo;

import org.elasticsearch.common.Strings;
import org.elasticsearch.geometry.Circle;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.GeometryCollection;
import org.elasticsearch.geometry.Line;
import org.elasticsearch.geometry.LinearRing;
import org.elasticsearch.geometry.MultiLine;
import org.elasticsearch.geometry.MultiPoint;
import org.elasticsearch.geometry.MultiPolygon;
import org.elasticsearch.geometry.Point;
import org.elasticsearch.geometry.Polygon;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.geometry.utils.GeographyValidator;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParseException;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.json.JsonXContent;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@code GeoJSONShapeParser}
 */
public class GeoJsonParserTests extends ESTestCase {

    public void testParsePoint() throws IOException {
        XContentBuilder pointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Point")
            .startArray("coordinates")
            .value(100.0)
            .value(0.0)
            .endArray()
            .endObject();
        Point expected = new Point(100.0, 0.0);
        assertGeometryEquals(expected, pointGeoJson);
    }

    public void testParseLineString() throws IOException {
        XContentBuilder lineGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "LineString")
            .startArray("coordinates")
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .endArray()
            .endObject();

        Line expected = new Line(new double[] { 100.0, 101.0 }, new double[] { 0.0, 1.0 });
        try (XContentParser parser = createParser(lineGeoJson)) {
            parser.nextToken();
            assertEquals(expected, GeoJson.fromXContent(GeographyValidator.instance(true), false, false, parser));
        }
    }

    public void testParseMultiLineString() throws IOException {
        XContentBuilder multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "MultiLineString")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .value(102.0)
            .value(2.0)
            .endArray()
            .startArray()
            .value(103.0)
            .value(3.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        MultiLine expected = new MultiLine(
            Arrays.asList(
                new Line(new double[] { 100.0, 101.0 }, new double[] { 0.0, 1.0 }),
                new Line(new double[] { 102.0, 103.0 }, new double[] { 2.0, 3.0 })

            )
        );

        assertGeometryEquals(expected, multilinesGeoJson);
    }

    public void testParseCircle() throws IOException {
        XContentBuilder multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "circle")
            .startArray("coordinates")
            .value(100.0)
            .value(0.0)
            .endArray()
            .field("radius", "200m")
            .endObject();

        Circle expected = new Circle(100.0, 0.0, 200);
        assertGeometryEquals(expected, multilinesGeoJson);
    }

    public void testParseMultiDimensionShapes() throws IOException {
        XContentBuilder pointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Point")
            .startArray("coordinates")
            .value(100.0)
            .value(0.0)
            .value(15.0)
            .value(18.0)
            .endArray()
            .endObject();

        try (XContentParser parser = createParser(pointGeoJson)) {
            parser.nextToken();
            expectThrows(
                XContentParseException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, false, parser)
            );
            assertNull(parser.nextToken());
        }

        XContentBuilder lineGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "LineString")
            .startArray("coordinates")
            .startArray()
            .value(100.0)
            .value(0.0)
            .value(15.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .value(18.0)
            .value(19.0)
            .endArray()
            .endArray()
            .endObject();

        try (XContentParser parser = createParser(lineGeoJson)) {
            parser.nextToken();
            expectThrows(
                XContentParseException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, false, parser)
            );
            assertNull(parser.nextToken());
        }
    }

    public void testParseEnvelope() throws IOException {
        XContentBuilder multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", randomBoolean() ? "envelope" : "bbox")
            .startArray("coordinates")
            .startArray()
            .value(-50)
            .value(30)
            .endArray()
            .startArray()
            .value(50)
            .value(-30)
            .endArray()
            .endArray()
            .endObject();
        Rectangle expected = new Rectangle(-50, 50, 30, -30);
        assertGeometryEquals(expected, multilinesGeoJson);

        multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", randomBoolean() ? "envelope" : "bbox")
            .startArray("coordinates")
            .startArray()
            .value(50)
            .value(30)
            .endArray()
            .startArray()
            .value(-50)
            .value(-30)
            .endArray()
            .endArray()
            .endObject();

        expected = new Rectangle(50, -50, 30, -30);
        assertGeometryEquals(expected, multilinesGeoJson);

        multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", randomBoolean() ? "envelope" : "bbox")
            .startArray("coordinates")
            .startArray()
            .value(50)
            .value(30)
            .endArray()
            .startArray()
            .value(-50)
            .value(-30)
            .endArray()
            .startArray()
            .value(50)
            .value(-39)
            .endArray()
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(multilinesGeoJson)) {
            parser.nextToken();
            expectThrows(
                XContentParseException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, false, parser)
            );
            assertNull(parser.nextToken());
        }

        multilinesGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", randomBoolean() ? "envelope" : "bbox")
            .startArray("coordinates")
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(multilinesGeoJson)) {
            parser.nextToken();
            expectThrows(
                XContentParseException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, false, parser)
            );
            assertNull(parser.nextToken());
        }
    }

    public void testParsePolygon() throws IOException {
        XContentBuilder polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        Polygon p = new Polygon(new LinearRing(new double[] { 100d, 101d, 101d, 100d, 100d }, new double[] { 1d, 1d, 0d, 0d, 1d }));
        assertGeometryEquals(p, polygonGeoJson);
    }

    public void testParse3DPolygon() throws IOException {
        XContentBuilder polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(0.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .value(10.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        Polygon expected = new Polygon(
            new LinearRing(
                new double[] { 100.0, 101.0, 101.0, 100.0, 100.0 },
                new double[] { 1.0, 1.0, 0.0, 0.0, 1.0 },
                new double[] { 10.0, 10.0, 10.0, 10.0, 10.0 }
            )
        );
        try (XContentParser parser = createParser(polygonGeoJson)) {
            parser.nextToken();
            assertEquals(expected, GeoJson.fromXContent(GeographyValidator.instance(true), false, true, parser));
        }
    }

    public void testInvalidDimensionalPolygon() throws IOException {
        XContentBuilder polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(0.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .value(10.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(polygonGeoJson)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(true), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidPoint() throws IOException {
        XContentBuilder invalidPoint1 = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "point")
            .startArray("coordinates")
            .startArray()
            .value(-74.011)
            .value(40.753)
            .endArray()
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(invalidPoint1)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        XContentBuilder invalidPoint2 = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "point")
            .startArray("coordinates")
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(invalidPoint2)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidMultipoint() throws IOException {
        XContentBuilder invalidMultipoint1 = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "multipoint")
            .startArray("coordinates")
            .value(-74.011)
            .value(40.753)
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(invalidMultipoint1)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        XContentBuilder invalidMultipoint2 = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "multipoint")
            .startArray("coordinates")
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(invalidMultipoint2)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        XContentBuilder invalidMultipoint3 = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "multipoint")
            .startArray("coordinates")
            .startArray()
            .endArray()
            .endArray()
            .endObject();
        try (XContentParser parser = createParser(invalidMultipoint3)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidDimensionalMultiPolygon() throws IOException {
        String multiPolygonGeoJson = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "MultiPolygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .startArray()
                .value(102.0)
                .value(2.0)
                .endArray()
                .startArray()
                .value(103.0)
                .value(2.0)
                .endArray()
                .startArray()
                .value(103.0)
                .value(3.0)
                .endArray()
                .startArray()
                .value(102.0)
                .value(3.0)
                .endArray()
                .startArray()
                .value(102.0)
                .value(2.0)
                .endArray()
                .endArray()
                .endArray()
                .startArray()
                .startArray()
                .startArray()
                .value(100.0)
                .value(0.0)
                .endArray()
                .startArray()
                .value(101.0)
                .value(0.0)
                .endArray()
                .startArray()
                .value(101.0)
                .value(1.0)
                .endArray()
                .startArray()
                .value(100.0)
                .value(1.0)
                .endArray()
                .startArray()
                .value(100.0)
                .value(0.0)
                .endArray()
                .endArray()
                .startArray()
                .startArray()
                .value(100.2)
                .value(0.8)
                .endArray()
                .startArray()
                .value(100.2)
                .value(0.2)
                .value(10.0)
                .endArray()
                .startArray()
                .value(100.8)
                .value(0.2)
                .endArray()
                .startArray()
                .value(100.8)
                .value(0.8)
                .endArray()
                .startArray()
                .value(100.2)
                .value(0.8)
                .endArray()
                .endArray()
                .endArray()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, multiPolygonGeoJson)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidPolygon() throws IOException {
        /*
         * The following 3 test cases ensure proper error handling of invalid polygons
         * per the GeoJSON specification
         */
        String invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .value(-74.011)
                .value(40.753)
                .endArray()
                .startArray()
                .value(-75.022)
                .value(41.783)
                .endArray()
                .endArray()
                .endArray()
                .endObject()
        );
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .value(-74.011)
                .value(40.753)
                .endArray()
                .endArray()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .endArray()
                .endArray()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .startArray()
                .startArray()
                .nullValue()
                .nullValue()
                .endArray()
                .endArray()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .nullValue()
                .nullValue()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder().startObject().field("type", "polygon").startArray("coordinates").endArray().endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        invalidPoly = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .field("type", "polygon")
                .startArray("coordinates")
                .startArray()
                .value(-74.011)
                .value(40.753)
                .endArray()
                .endArray()
                .endObject()
        );

        try (XContentParser parser = createParser(JsonXContent.jsonXContent, invalidPoly)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParsePolygonWithHole() throws IOException {
        XContentBuilder polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .value(100.2)
            .value(0.8)
            .endArray()
            .startArray()
            .value(100.2)
            .value(0.2)
            .endArray()
            .startArray()
            .value(100.8)
            .value(0.2)
            .endArray()
            .startArray()
            .value(100.8)
            .value(0.8)
            .endArray()
            .startArray()
            .value(100.2)
            .value(0.8)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        LinearRing hole = new LinearRing(
            new double[] { 100.2d, 100.2d, 100.8d, 100.8d, 100.2d },
            new double[] { 0.8d, 0.2d, 0.2d, 0.8d, 0.8d }
        );
        Polygon p = new Polygon(
            new LinearRing(new double[] { 100d, 101d, 101d, 100d, 100d }, new double[] { 1d, 1d, 0d, 0d, 1d }),
            Collections.singletonList(hole)
        );
        assertGeometryEquals(p, polygonGeoJson);
    }

    public void testParseMultiPoint() throws IOException {
        XContentBuilder multiPointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "MultiPoint")
            .startArray("coordinates")
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .endArray()
            .endObject();

        assertGeometryEquals(new MultiPoint(Arrays.asList(new Point(100, 0), new Point(101, 1))), multiPointGeoJson);
    }

    public void testParseMultiPolygon() throws IOException {
        XContentBuilder multiPolygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "MultiPolygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .startArray()
            .value(102.0)
            .value(2.0)
            .endArray()
            .startArray()
            .value(103.0)
            .value(2.0)
            .endArray()
            .startArray()
            .value(103.0)
            .value(3.0)
            .endArray()
            .startArray()
            .value(102.0)
            .value(3.0)
            .endArray()
            .startArray()
            .value(102.0)
            .value(2.0)
            .endArray()
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(1.0)
            .endArray()
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .value(100.2)
            .value(0.8)
            .endArray()
            .startArray()
            .value(100.2)
            .value(0.2)
            .endArray()
            .startArray()
            .value(100.8)
            .value(0.2)
            .endArray()
            .startArray()
            .value(100.8)
            .value(0.8)
            .endArray()
            .startArray()
            .value(100.2)
            .value(0.8)
            .endArray()
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        LinearRing hole = new LinearRing(
            new double[] { 100.2d, 100.2d, 100.8d, 100.8d, 100.2d },
            new double[] { 0.8d, 0.2d, 0.2d, 0.8d, 0.8d }
        );

        MultiPolygon polygons = new MultiPolygon(
            Arrays.asList(
                new Polygon(new LinearRing(new double[] { 102d, 103d, 103d, 102d, 102d }, new double[] { 2d, 2d, 3d, 3d, 2d })),
                new Polygon(
                    new LinearRing(new double[] { 100d, 101d, 101d, 100d, 100d }, new double[] { 0d, 0d, 1d, 1d, 0d }),
                    Collections.singletonList(hole)
                )
            )
        );

        assertGeometryEquals(polygons, multiPolygonGeoJson);
    }

    public void testParseGeometryCollection() throws IOException {
        XContentBuilder geometryCollectionGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "GeometryCollection")
            .startArray("geometries")
            .startObject()
            .field("type", "LineString")
            .startArray("coordinates")
            .startArray()
            .value(100.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(101.0)
            .value(1.0)
            .endArray()
            .endArray()
            .endObject()
            .startObject()
            .field("type", "Point")
            .startArray("coordinates")
            .value(102.0)
            .value(2.0)
            .endArray()
            .endObject()
            .startObject()
            .field("type", "Polygon")
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(-177.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(15.0)
            .endArray()
            .startArray()
            .value(172.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(-15.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(-10.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(10.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject()
            .endArray()
            .endObject();

        GeometryCollection<Geometry> geometryExpected = new GeometryCollection<>(
            Arrays.asList(
                new Line(new double[] { 100d, 101d }, new double[] { 0d, 1d }),
                new Point(102d, 2d),
                new Polygon(new LinearRing(new double[] { -177, 176, 172, 176, -177, -177 }, new double[] { 10, 15, 0, -15, -10, 10 }))
            )
        );
        assertGeometryEquals(geometryExpected, geometryCollectionGeoJson);
    }

    public void testThatParserExtractsCorrectTypeAndCoordinatesFromArbitraryJson() throws IOException, ParseException {
        XContentBuilder pointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .startObject("crs")
            .field("type", "name")
            .startObject("properties")
            .field("name", "urn:ogc:def:crs:OGC:1.3:CRS84")
            .endObject()
            .endObject()
            .field("bbox", "foobar")
            .field("type", "point")
            .field("bubu", "foobar")
            .startArray("coordinates")
            .value(100.0)
            .value(0.0)
            .endArray()
            .startObject("nested")
            .startArray("coordinates")
            .value(200.0)
            .value(0.0)
            .endArray()
            .endObject()
            .startObject("lala")
            .field("type", "NotAPoint")
            .endObject()
            .endObject();

        Point expectedPt = new Point(100, 0);
        assertGeometryEquals(expectedPt, pointGeoJson);
    }

    public void testParseOrientationOption() throws IOException {
        XContentBuilder polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .field("orientation", randomFrom("ccw", "right"))
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(176.0)
            .value(15.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(-10.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(-15.0)
            .endArray()
            .startArray()
            .value(172.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(15.0)
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .value(-172.0)
            .value(8.0)
            .endArray()
            .startArray()
            .value(174.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(-172.0)
            .value(-8.0)
            .endArray()
            .startArray()
            .value(-172.0)
            .value(8.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        Polygon expected = new Polygon(
            new LinearRing(
                new double[] { 176.0, -177.0, -177.0, 176.0, 172.0, 176.0 },
                new double[] { 15.0, 10.0, -10.0, -15.0, 0.0, 15.0 }
            ),
            Collections.singletonList(new LinearRing(new double[] { -172.0, 174.0, -172.0, -172.0 }, new double[] { 8.0, 10.0, -8.0, 8.0 }))
        );
        assertGeometryEquals(expected, polygonGeoJson);

        polygonGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Polygon")
            .field("orientation", randomFrom("cw", "left"))
            .startArray("coordinates")
            .startArray()
            .startArray()
            .value(176.0)
            .value(15.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(-177.0)
            .value(-10.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(-15.0)
            .endArray()
            .startArray()
            .value(172.0)
            .value(0.0)
            .endArray()
            .startArray()
            .value(176.0)
            .value(15.0)
            .endArray()
            .endArray()
            .startArray()
            .startArray()
            .value(-172.0)
            .value(8.0)
            .endArray()
            .startArray()
            .value(174.0)
            .value(10.0)
            .endArray()
            .startArray()
            .value(-172.0)
            .value(-8.0)
            .endArray()
            .startArray()
            .value(-172.0)
            .value(8.0)
            .endArray()
            .endArray()
            .endArray()
            .endObject();

        expected = new Polygon(
            new LinearRing(
                new double[] { 176.0, 172.0, 176.0, -177.0, -177.0, 176.0 },
                new double[] { 15.0, 0.0, -15.0, -10.0, 10.0, 15.0 }
            ),
            Collections.singletonList(new LinearRing(new double[] { -172.0, -172.0, 174.0, -172.0 }, new double[] { 8.0, -8.0, 10.0, 8.0 }))
        );
        assertGeometryEquals(expected, polygonGeoJson);
    }

    public void testParseInvalidShapes() throws IOException {
        XContentBuilder tooLittlePointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Point")
            .startArray("coordinates")
            .value(10.0)
            .endArray()
            .endObject();

        try (XContentParser parser = createParser(tooLittlePointGeoJson)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }

        XContentBuilder emptyPointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .field("type", "Point")
            .startObject("coordinates")
            .field("foo", "bar")
            .endObject()
            .endObject();

        try (XContentParser parser = createParser(emptyPointGeoJson)) {
            parser.nextToken();
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidPointMissingFields() throws IOException {
        XContentBuilder tooLittlePointGeoJson = XContentFactory.jsonBuilder()
            .startObject()
            .startArray("coordinates")
            .value(10.0)
            .value(10.0)
            .endArray()
            .endObject();

        try (XContentParser parser = createParser(tooLittlePointGeoJson)) {
            parser.nextToken();
            IllegalArgumentException e = expectThrows(
                IllegalArgumentException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser)
            );
            assertThat(e.getMessage(), containsString("Required [type]"));
            assertNull(parser.nextToken());
        }

        XContentBuilder emptyPointGeoJson = XContentFactory.jsonBuilder().startObject().field("type", "Point").endObject();

        try (XContentParser parser = createParser(emptyPointGeoJson)) {
            parser.nextToken();
            XContentParseException e = expectThrows(
                XContentParseException.class,
                () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser)
            );
            assertThat(e.getCause().getMessage(), containsString("coordinates not included"));
            assertNull(parser.nextToken());
        }
    }

    public void testParseInvalidGeometryCollectionShapes() throws IOException {
        XContentBuilder invalidPoints = XContentFactory.jsonBuilder()
            .startObject()
            .startObject("foo")
            .field("type", "geometrycollection")
            .startArray("geometries")
            .startObject()
            .field("type", "polygon")
            .startArray("coordinates")
            .startArray()
            .value("46.6022226498514")
            .value("24.7237442867977")
            .endArray()
            .startArray()
            .value("46.6031857243798")
            .value("24.722968774929")
            .endArray()
            .endArray() 
            .endObject()
            .endArray() 
            .endObject()
            .endObject();
        try (XContentParser parser = createParser(invalidPoints)) {
            parser.nextToken(); 
            parser.nextToken(); 
            parser.nextToken(); 
            expectThrows(XContentParseException.class, () -> GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
            assertEquals(XContentParser.Token.END_OBJECT, parser.nextToken()); 
            assertNull(parser.nextToken()); 
        }
    }

    private void assertGeometryEquals(org.elasticsearch.geometry.Geometry expected, XContentBuilder geoJson) throws IOException {
        try (XContentParser parser = createParser(geoJson)) {
            parser.nextToken();
            assertEquals(expected, GeoJson.fromXContent(GeographyValidator.instance(false), false, true, parser));
        }
    }
}
