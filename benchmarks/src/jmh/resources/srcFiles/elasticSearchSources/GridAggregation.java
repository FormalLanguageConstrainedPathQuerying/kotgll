/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.vectortile.rest;

import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.h3.H3;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoTileGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoTileUtils;
import org.elasticsearch.xpack.spatial.common.H3CartesianUtil;
import org.elasticsearch.xpack.spatial.search.aggregations.bucket.geogrid.GeoHexGridAggregationBuilder;
import org.elasticsearch.xpack.vectortile.feature.FeatureFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Enum containing the basic operations for different GeoGridAggregations.
 */
enum GridAggregation {
    GEOTILE {
        @Override
        public GeoGridAggregationBuilder newAgg(String aggName) {
            return new GeoTileGridAggregationBuilder(aggName);
        }

        @Override
        public Rectangle bufferTile(Rectangle tile, int z, int gridPrecision) {
            return tile;
        }

        @Override
        public int gridPrecisionToAggPrecision(int z, int gridPrecision) {
            return Math.min(GeoTileUtils.MAX_ZOOM, z + gridPrecision);
        }

        @Override
        public byte[] toGrid(String bucketKey, FeatureFactory featureFactory) throws IOException {
            final Rectangle r = toRectangle(bucketKey);
            return featureFactory.box(r.getMinLon(), r.getMaxLon(), r.getMinLat(), r.getMaxLat());
        }

        @Override
        public Rectangle toRectangle(String bucketKey) {
            return GeoTileUtils.toBoundingBox(bucketKey);
        }

        @Override
        public boolean needsBounding(int z, int gridPrecision) {
            return true;
        }
    },
    GEOHEX {

        private static final double[] LAT_BUFFER_SIZE = new double[16];
        private static final double[] LON_BUFFER_SIZE = new double[16];
        static {
            LAT_BUFFER_SIZE[0] = LAT_BUFFER_SIZE[1] = Double.NaN;
            LON_BUFFER_SIZE[0] = LON_BUFFER_SIZE[1] = Double.NaN;
            LAT_BUFFER_SIZE[2] = 3.7;
            LON_BUFFER_SIZE[2] = 51.2;
            for (int i = 3; i < LON_BUFFER_SIZE.length; i++) {
                LAT_BUFFER_SIZE[i] = LAT_BUFFER_SIZE[i - 1] / 2.5;
                LON_BUFFER_SIZE[i] = LON_BUFFER_SIZE[i - 1] / 2.5;
            }
        }
        private static final int[] ZOOM2RESOLUTION = new int[] {
            0,
            0,
            0,
            1,
            1,
            2,
            2,
            3,
            3,
            3,
            4,
            4,
            5,
            6,
            6,
            7,
            8,
            9,
            9,
            10,
            11,
            11,
            12,
            13,
            14,
            14,
            15,
            15,
            15,
            15 };

        @Override
        public GeoGridAggregationBuilder newAgg(String aggName) {
            return new GeoHexGridAggregationBuilder(aggName);
        }

        @Override
        public Rectangle bufferTile(Rectangle tile, int z, int gridPrecision) {
            if (z == 0 || gridPrecision == 0) {
                return tile;
            }
            final int aggPrecision = gridPrecisionToAggPrecision(z, gridPrecision);
            if (aggPrecision < 2) {
                return new Rectangle(-180, 180, GeoTileUtils.LATITUDE_MASK, -GeoTileUtils.LATITUDE_MASK);
            }
            return new Rectangle(
                GeoUtils.normalizeLon(tile.getMinX() - LON_BUFFER_SIZE[aggPrecision]),
                GeoUtils.normalizeLon(tile.getMaxX() + LON_BUFFER_SIZE[aggPrecision]),
                Math.min(GeoTileUtils.LATITUDE_MASK, tile.getMaxY() + LAT_BUFFER_SIZE[aggPrecision]),
                Math.max(-GeoTileUtils.LATITUDE_MASK, tile.getMinY() - LAT_BUFFER_SIZE[aggPrecision])
            );
        }

        @Override
        public int gridPrecisionToAggPrecision(int z, int gridPrecision) {
            return ZOOM2RESOLUTION[GEOTILE.gridPrecisionToAggPrecision(z, gridPrecision)];
        }

        @Override
        public byte[] toGrid(String bucketKey, FeatureFactory featureFactory) {
            final List<byte[]> x = featureFactory.getFeatures(H3CartesianUtil.getNormalizeGeometry(H3.stringToH3(bucketKey)));
            return x.size() > 0 ? x.get(0) : null;
        }

        @Override
        public Rectangle toRectangle(String bucketKey) {
            return H3CartesianUtil.toBoundingBox(H3.stringToH3(bucketKey));
        }

        @Override
        public boolean needsBounding(int z, int gridPrecision) {
            /*
              Bounded geohex aggregation can be expensive, in particular where there is lots of data outside the bounding
              box. Because we are buffering our queries, this is magnified for low precision tiles. Because the total number
              of buckets up to precision 3 is lower than the default max buckets, we better not bound those aggregations
              which results in much better performance.
             */
            return gridPrecisionToAggPrecision(z, gridPrecision) > 3;
        }
    };

    /**
     * New {@link GeoGridAggregationBuilder} instance.
     */
    public abstract GeoGridAggregationBuilder newAgg(String aggName);

    /**
     * Buffer the query bounding box so the bins of an aggregation see
     * all data that is inside them.
     */
    public abstract Rectangle bufferTile(Rectangle tile, int z, int gridPrecision);

    /**
     * Transform the provided grid precision at the given zoom to the
     * agg precision.
     */
    public abstract int gridPrecisionToAggPrecision(int z, int gridPrecision);

    /**
     * transforms the geometry of a given bin into the vector tile feature.
     */
    public abstract byte[] toGrid(String bucketKey, FeatureFactory featureFactory) throws IOException;

    /**
     * Returns the bounding box of the bin.
     */
    public abstract Rectangle toRectangle(String bucketKey);

    /**
     * If false, the aggregation at the given zoom and grid precision is not bound.
     */
    public abstract boolean needsBounding(int z, int gridPrecision);

    public static GridAggregation fromString(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "geotile" -> GEOTILE;
            case "geohex" -> GEOHEX;
            default -> throw new IllegalArgumentException("Invalid agg type [" + type + "]");
        };
    }
}
