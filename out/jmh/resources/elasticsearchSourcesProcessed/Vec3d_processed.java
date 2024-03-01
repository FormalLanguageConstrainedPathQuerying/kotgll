/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * This project is based on a modification of https:
 *
 * Copyright 2018, 2020-2021 Uber Technologies, Inc.
 */

package org.elasticsearch.h3;

/**
 *  3D floating-point vector
 */
final class Vec3d {

    /** icosahedron face centers in x/y/z on the unit sphere */
    public static final Vec3d[] faceCenterPoint = new Vec3d[] {
        new Vec3d(0.2199307791404606, 0.6583691780274996, 0.7198475378926182),     
        new Vec3d(-0.2139234834501421, 0.1478171829550703, 0.9656017935214205),    
        new Vec3d(0.1092625278784797, -0.4811951572873210, 0.8697775121287253),    
        new Vec3d(0.7428567301586791, -0.3593941678278028, 0.5648005936517033),    
        new Vec3d(0.8112534709140969, 0.3448953237639384, 0.4721387736413930),     
        new Vec3d(-0.1055498149613921, 0.9794457296411413, 0.1718874610009365),    
        new Vec3d(-0.8075407579970092, 0.1533552485898818, 0.5695261994882688),    
        new Vec3d(-0.2846148069787907, -0.8644080972654206, 0.4144792552473539),   
        new Vec3d(0.7405621473854482, -0.6673299564565524, -0.0789837646326737),   
        new Vec3d(0.8512303986474293, 0.4722343788582681, -0.2289137388687808),    
        new Vec3d(-0.7405621473854481, 0.6673299564565524, 0.0789837646326737),    
        new Vec3d(-0.8512303986474292, -0.4722343788582682, 0.2289137388687808),   
        new Vec3d(0.1055498149613919, -0.9794457296411413, -0.1718874610009365),   
        new Vec3d(0.8075407579970092, -0.1533552485898819, -0.5695261994882688),   
        new Vec3d(0.2846148069787908, 0.8644080972654204, -0.4144792552473539),   
        new Vec3d(-0.7428567301586791, 0.3593941678278027, -0.5648005936517033),   
        new Vec3d(-0.8112534709140971, -0.3448953237639382, -0.4721387736413930),  
        new Vec3d(-0.2199307791404607, -0.6583691780274996, -0.7198475378926182),  
        new Vec3d(0.2139234834501420, -0.1478171829550704, -0.9656017935214205),   
        new Vec3d(-0.1092625278784796, 0.4811951572873210, -0.8697775121287253)   
    };

    private final double x, y, z;

    private Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Calculate the square of the distance between two 3D coordinates.
     *
     * @param x The first 3D coordinate.
     * @param y The second 3D coordinate.
     * @param z The third 3D coordinate.
     * @return The square of the distance between the given points.
     */
    private double pointSquareDist(double x, double y, double z) {
        return square(x - this.x) + square(y - this.y) + square(z - this.z);
    }

    /**
     * Encodes a coordinate on the sphere to the corresponding H3 index.
     *
     * @param res The desired H3 resolution for the encoding.
     * @param lat The coordinate latitude in radians.
     * @param lon The coordinate longitude in radians.
     * @return The H3 index.
     */
    static long geoToH3(int res, double lat, double lon) {
        final double cosLat = FastMath.cos(lat);
        final double z = FastMath.sin(lat);
        final double x = FastMath.cos(lon) * cosLat;
        final double y = FastMath.sin(lon) * cosLat;
        int face = 0;
        double sqd = Vec3d.faceCenterPoint[0].pointSquareDist(x, y, z);
        for (int i = 1; i < Vec3d.faceCenterPoint.length; i++) {
            final double sqdT = Vec3d.faceCenterPoint[i].pointSquareDist(x, y, z);
            if (sqdT < sqd) {
                face = i;
                sqd = sqdT;
            }
        }
        double r = FastMath.acos(1 - sqd / 2);

        if (r < Constants.EPSILON) {
            return FaceIJK.faceIjkToH3(res, face, new CoordIJK(0, 0, 0));
        }

        double theta = Vec2d.posAngleRads(
            Vec2d.faceAxesAzRadsCII[face][0] - Vec2d.posAngleRads(faceCenterPoint[face].geoAzimuthRads(x, y, z))
        );

        if (H3Index.isResolutionClassIII(res)) {
            theta = Vec2d.posAngleRads(theta - Constants.M_AP7_ROT_RADS);
        }

        r = FastMath.tan(r);

        r /= Constants.RES0_U_GNOMONIC;
        for (int i = 0; i < res; i++) {
            r *= Constants.M_SQRT7;
        }
        return FaceIJK.faceIjkToH3(res, face, Vec2d.hex2dToCoordIJK(r * FastMath.cos(theta), r * FastMath.sin(theta)));
    }

    /**
     * Square of a number
     *
     * @param x The input number.
     * @return The square of the input number.
     */
    private static double square(double x) {
        return x * x;
    }

    /**
     * Determines the azimuth to the provided 3D coordinate.
     *
     * @param x The first 3D coordinate.
     * @param y The second 3D coordinate.
     * @param z The third 3D coordinate.
     * @return The azimuth in radians.
     */
    double geoAzimuthRads(double x, double y, double z) {
        final double c1X = this.y * z - this.z * y;
        final double c1Y = this.z * x - this.x * z;
        final double c1Z = this.x * y - this.y * x;

        final double c2X = this.y;
        final double c2Y = -this.x;
        final double c2Z = 0d;

        final double c1c2X = c1Y * c2Z - c1Z * c2Y;
        final double c1c2Y = c1Z * c2X - c1X * c2Z;
        final double c1c2Z = c1X * c2Y - c1Y * c2X;

        final double sign = Math.signum(dotProduct(this.x, this.y, this.z, c1c2X, c1c2Y, c1c2Z));
        return FastMath.atan2(sign * magnitude(c1c2X, c1c2Y, c1c2Z), dotProduct(c1X, c1Y, c1Z, c2X, c2Y, c2Z));
    }

    /**
     * Computes the point on the sphere with a specified azimuth and distance from
     * this point.
     *
     * @param az       The desired azimuth.
     * @param distance The desired distance.
     * @return The LatLng point.
     */
    LatLng geoAzDistanceRads(double az, double distance) {
        az = Vec2d.posAngleRads(az);

        final double magnitude = magnitude(this.x, this.y, 0);
        final double deX = -this.y / magnitude;
        final double deY = this.x / magnitude;

        final double dnX = -this.z * deY;
        final double dnY = this.z * deX;
        final double dnZ = this.x * deY - this.y * deX;

        final double sinAz = FastMath.sin(az);
        final double cosAz = FastMath.cos(az);
        final double sinDistance = FastMath.sin(distance);
        final double cosDistance = FastMath.cos(distance);

        final double dX = dnX * cosAz + deX * sinAz;
        final double dY = dnY * cosAz + deY * sinAz;
        final double dZ = dnZ * cosAz;

        final double n2X = this.x * cosDistance + dX * sinDistance;
        final double n2Y = this.y * cosDistance + dY * sinDistance;
        final double n2Z = this.z * cosDistance + dZ * sinDistance;

        return new LatLng(FastMath.asin(n2Z), FastMath.atan2(n2Y, n2X));
    }

    /**
     * Calculate the dot product between two 3D coordinates.
     *
     * @param x1 The first 3D coordinate from the first set of coordinates.
     * @param y1 The second 3D coordinate from the first set of coordinates.
     * @param z1 The third 3D coordinate from the first set of coordinates.
     * @param x2 The first 3D coordinate from the second set of coordinates.
     * @param y2 The second 3D coordinate from the second set of coordinates.
     * @param z2 The third 3D coordinate from the second set of coordinates.
     * @return The dot product.
     */
    private static double dotProduct(double x1, double y1, double z1, double x2, double y2, double z2) {
        return x1 * x2 + y1 * y2 + z1 * z2;
    }

    /**
     * Calculate the magnitude of 3D coordinates.
     *
     * @param x The first 3D coordinate.
     * @param y The second 3D coordinate.
     * @param z The third 3D coordinate.
     * @return The magnitude of the provided coordinates.
     */
    private static double magnitude(double x, double y, double z) {
        return Math.sqrt(square(x) + square(y) + square(z));
    }

}
