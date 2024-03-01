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
 * Copyright 2016-2021 Uber Technologies, Inc.
 */
package org.elasticsearch.h3;

/**
 * Mutable face number and ijk coordinates on that face-centered coordinate system.
 *
 *  References the Vec2d cartesian coordinate systems hex2d: local face-centered
 *  coordinate system scaled a specific H3 grid resolution unit length and
 *  with x-axes aligned with the local i-axes
 */
final class FaceIJK {

    /** enum representing overage type */
    enum Overage {
        /**
         * Digit representing overage type
         */
        NO_OVERAGE,
        /**
         * On face edge (only occurs on substrate grids)
         */
        FACE_EDGE,
        /**
         * Overage on new face interior
         */
        NEW_FACE
    }

    /**
     * IJ quadrant faceNeighbors table direction
     */
    private static final int IJ = 1;
    /**
     * KI quadrant faceNeighbors table direction
     */
    private static final int KI = 2;
    /**
     * JK quadrant faceNeighbors table direction
     */
    private static final int JK = 3;

    /**
     * overage distance table
     */
    private static final int[] maxDimByCIIres = {
        2,        
        -1,       
        14,       
        -1,       
        98,       
        -1,       
        686,      
        -1,       
        4802,     
        -1,       
        33614,    
        -1,       
        235298,   
        -1,       
        1647086,  
        -1,       
        11529602  
    };

    private static final Vec2d[][] maxDimByCIIVec2d = new Vec2d[maxDimByCIIres.length][3];
    static {
        for (int i = 0; i < maxDimByCIIres.length; i++) {
            maxDimByCIIVec2d[i][0] = new Vec2d(3.0 * maxDimByCIIres[i], 0.0);
            maxDimByCIIVec2d[i][1] = new Vec2d(-1.5 * maxDimByCIIres[i], 3.0 * Constants.M_SQRT3_2 * maxDimByCIIres[i]);
            maxDimByCIIVec2d[i][2] = new Vec2d(-1.5 * maxDimByCIIres[i], -3.0 * Constants.M_SQRT3_2 * maxDimByCIIres[i]);
        }
    }

    /**
     * unit scale distance table
     */
    private static final int[] unitScaleByCIIres = {
        1,       
        -1,      
        7,       
        -1,      
        49,      
        -1,      
        343,     
        -1,      
        2401,    
        -1,      
        16807,   
        -1,      
        117649,  
        -1,      
        823543,  
        -1,      
        5764801  
    };

    /**
     * direction from the origin face to the destination face, relative to
     * the origin face's coordinate system, or -1 if not adjacent.
     */
    private static final int[][] adjacentFaceDir = new int[][] {
        { 0, KI, -1, -1, IJ, JK, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { IJ, 0, KI, -1, -1, -1, JK, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { -1, IJ, 0, KI, -1, -1, -1, JK, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { -1, -1, IJ, 0, KI, -1, -1, -1, JK, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { KI, -1, -1, IJ, 0, -1, -1, -1, -1, JK, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { JK, -1, -1, -1, -1, 0, -1, -1, -1, -1, IJ, -1, -1, -1, KI, -1, -1, -1, -1, -1 },  
        { -1, JK, -1, -1, -1, -1, 0, -1, -1, -1, KI, IJ, -1, -1, -1, -1, -1, -1, -1, -1 },  
        { -1, -1, JK, -1, -1, -1, -1, 0, -1, -1, -1, KI, IJ, -1, -1, -1, -1, -1, -1, -1 },  
        { -1, -1, -1, JK, -1, -1, -1, -1, 0, -1, -1, -1, KI, IJ, -1, -1, -1, -1, -1, -1 },  
        { -1, -1, -1, -1, JK, -1, -1, -1, -1, 0, -1, -1, -1, KI, IJ, -1, -1, -1, -1, -1 },  
        { -1, -1, -1, -1, -1, IJ, KI, -1, -1, -1, 0, -1, -1, -1, -1, JK, -1, -1, -1, -1 },  
        { -1, -1, -1, -1, -1, -1, IJ, KI, -1, -1, -1, 0, -1, -1, -1, -1, JK, -1, -1, -1 },  
        { -1, -1, -1, -1, -1, -1, -1, IJ, KI, -1, -1, -1, 0, -1, -1, -1, -1, JK, -1, -1 },  
        { -1, -1, -1, -1, -1, -1, -1, -1, IJ, KI, -1, -1, -1, 0, -1, -1, -1, -1, JK, -1 },  
        { -1, -1, -1, -1, -1, KI, -1, -1, -1, IJ, -1, -1, -1, -1, 0, -1, -1, -1, -1, JK },  
        { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, JK, -1, -1, -1, -1, 0, IJ, -1, -1, KI },  
        { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, JK, -1, -1, -1, KI, 0, IJ, -1, -1 },  
        { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, JK, -1, -1, -1, KI, 0, IJ, -1 },  
        { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, JK, -1, -1, -1, KI, 0, IJ },  
        { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, JK, IJ, -1, -1, KI, 0 }  
    };

    /** Maximum input for any component to face-to-base-cell lookup functions */
    private static final int MAX_FACE_COORD = 2;

    /**
     *  Information to transform into an adjacent face IJK system
     */
    private static class FaceOrientIJK {
        final int face;
        final int translateI;
        final int translateJ;
        final int translateK;
        final int ccwRot60;

        FaceOrientIJK(int face, int translateI, int translateJ, int translateK, int ccwRot60) {
            this.face = face;
            this.translateI = translateI;
            this.translateJ = translateJ;
            this.translateK = translateK;
            this.ccwRot60 = ccwRot60;
        }
    }

    /**
     *  Definition of which faces neighbor each other.
     */
    private static final FaceOrientIJK[][] faceNeighbors = new FaceOrientIJK[][] {
        {
            new FaceOrientIJK(0, 0, 0, 0, 0),  
            new FaceOrientIJK(4, 2, 0, 2, 1),  
            new FaceOrientIJK(1, 2, 2, 0, 5),  
            new FaceOrientIJK(5, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(1, 0, 0, 0, 0),  
            new FaceOrientIJK(0, 2, 0, 2, 1),  
            new FaceOrientIJK(2, 2, 2, 0, 5),  
            new FaceOrientIJK(6, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(2, 0, 0, 0, 0),  
            new FaceOrientIJK(1, 2, 0, 2, 1),  
            new FaceOrientIJK(3, 2, 2, 0, 5),  
            new FaceOrientIJK(7, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(3, 0, 0, 0, 0),  
            new FaceOrientIJK(2, 2, 0, 2, 1),  
            new FaceOrientIJK(4, 2, 2, 0, 5),  
            new FaceOrientIJK(8, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(4, 0, 0, 0, 0),  
            new FaceOrientIJK(3, 2, 0, 2, 1),  
            new FaceOrientIJK(0, 2, 2, 0, 5),  
            new FaceOrientIJK(9, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(5, 0, 0, 0, 0),   
            new FaceOrientIJK(10, 2, 2, 0, 3),  
            new FaceOrientIJK(14, 2, 0, 2, 3),  
            new FaceOrientIJK(0, 0, 2, 2, 3)    
        },
        {
            new FaceOrientIJK(6, 0, 0, 0, 0),   
            new FaceOrientIJK(11, 2, 2, 0, 3),  
            new FaceOrientIJK(10, 2, 0, 2, 3),  
            new FaceOrientIJK(1, 0, 2, 2, 3)    
        },
        {
            new FaceOrientIJK(7, 0, 0, 0, 0),   
            new FaceOrientIJK(12, 2, 2, 0, 3),  
            new FaceOrientIJK(11, 2, 0, 2, 3),  
            new FaceOrientIJK(2, 0, 2, 2, 3)    
        },
        {
            new FaceOrientIJK(8, 0, 0, 0, 0),   
            new FaceOrientIJK(13, 2, 2, 0, 3),  
            new FaceOrientIJK(12, 2, 0, 2, 3),  
            new FaceOrientIJK(3, 0, 2, 2, 3)    
        },
        {
            new FaceOrientIJK(9, 0, 0, 0, 0),   
            new FaceOrientIJK(14, 2, 2, 0, 3),  
            new FaceOrientIJK(13, 2, 0, 2, 3),  
            new FaceOrientIJK(4, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(10, 0, 0, 0, 0),  
            new FaceOrientIJK(5, 2, 2, 0, 3),   
            new FaceOrientIJK(6, 2, 0, 2, 3),   
            new FaceOrientIJK(15, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(11, 0, 0, 0, 0),  
            new FaceOrientIJK(6, 2, 2, 0, 3),   
            new FaceOrientIJK(7, 2, 0, 2, 3),   
            new FaceOrientIJK(16, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(12, 0, 0, 0, 0),  
            new FaceOrientIJK(7, 2, 2, 0, 3),   
            new FaceOrientIJK(8, 2, 0, 2, 3),   
            new FaceOrientIJK(17, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(13, 0, 0, 0, 0),  
            new FaceOrientIJK(8, 2, 2, 0, 3),   
            new FaceOrientIJK(9, 2, 0, 2, 3),   
            new FaceOrientIJK(18, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(14, 0, 0, 0, 0),  
            new FaceOrientIJK(9, 2, 2, 0, 3),   
            new FaceOrientIJK(5, 2, 0, 2, 3),   
            new FaceOrientIJK(19, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(15, 0, 0, 0, 0),  
            new FaceOrientIJK(16, 2, 0, 2, 1),  
            new FaceOrientIJK(19, 2, 2, 0, 5),  
            new FaceOrientIJK(10, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(16, 0, 0, 0, 0),  
            new FaceOrientIJK(17, 2, 0, 2, 1),  
            new FaceOrientIJK(15, 2, 2, 0, 5),  
            new FaceOrientIJK(11, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(17, 0, 0, 0, 0),  
            new FaceOrientIJK(18, 2, 0, 2, 1),  
            new FaceOrientIJK(16, 2, 2, 0, 5),  
            new FaceOrientIJK(12, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(18, 0, 0, 0, 0),  
            new FaceOrientIJK(19, 2, 0, 2, 1),  
            new FaceOrientIJK(17, 2, 2, 0, 5),  
            new FaceOrientIJK(13, 0, 2, 2, 3)   
        },
        {
            new FaceOrientIJK(19, 0, 0, 0, 0),  
            new FaceOrientIJK(15, 2, 0, 2, 1),  
            new FaceOrientIJK(18, 2, 2, 0, 5),  
            new FaceOrientIJK(14, 0, 2, 2, 3)   
        } };

    private static final int[][] VERTEX_CLASSIII = new int[][] {
        { 5, 4, 0 },  
        { 1, 5, 0 },  
        { 0, 5, 4 },  
        { 0, 1, 5 },  
        { 4, 0, 5 },  
        { 5, 0, 1 }   
    };

    private static final int[][] VERTEX_CLASSII = new int[][] {
        { 2, 1, 0 },  
        { 1, 2, 0 },  
        { 0, 2, 1 },  
        { 0, 1, 2 },  
        { 1, 0, 2 },  
        { 2, 0, 1 }   
    };

    int face;        
    CoordIJK coord;  

    FaceIJK(int face, CoordIJK coord) {
        this.face = face;
        this.coord = coord;
    }

    /**
     * Adjusts this FaceIJK address so that the resulting cell address is
     * relative to the correct icosahedral face.
     *
     * @param res          The H3 resolution of the cell.
     * @param pentLeading4 Whether or not the cell is a pentagon with a leading
     *                     digit 4.
     * @param substrate    Whether or not the cell is in a substrate grid.
     * @return 0 if on original face (no overage); 1 if on face edge (only occurs
     * on substrate grids); 2 if overage on new face interior
     */
    public Overage adjustOverageClassII(int res, boolean pentLeading4, boolean substrate) {
        Overage overage = Overage.NO_OVERAGE;
        int maxDim = maxDimByCIIres[res];
        if (substrate) {
            maxDim *= 3;
        }

        if (substrate && this.coord.i + this.coord.j + this.coord.k == maxDim) { 
            overage = Overage.FACE_EDGE;
        } else if (this.coord.i + this.coord.j + this.coord.k > maxDim) { 
            overage = Overage.NEW_FACE;
            final FaceOrientIJK fijkOrient;
            if (this.coord.k > 0) {
                if (this.coord.j > 0) { 
                    fijkOrient = faceNeighbors[this.face][JK];
                } else { 
                    fijkOrient = faceNeighbors[this.face][KI];
                    if (pentLeading4) {
                        this.coord.ijkSub(maxDim, 0, 0);
                        this.coord.ijkRotate60cw();
                        this.coord.ijkAdd(maxDim, 0, 0);
                    }
                }
            } else { 
                fijkOrient = faceNeighbors[this.face][IJ];
            }

            this.face = fijkOrient.face;

            for (int i = 0; i < fijkOrient.ccwRot60; i++) {
                this.coord.ijkRotate60ccw();
            }

            int unitScale = unitScaleByCIIres[res];
            if (substrate) {
                unitScale *= 3;
            }
            this.coord.ijkAdd(fijkOrient.translateI * unitScale, fijkOrient.translateJ * unitScale, fijkOrient.translateK * unitScale);
            this.coord.ijkNormalize();

            if (substrate && this.coord.i + this.coord.j + this.coord.k == maxDim) { 
                overage = Overage.FACE_EDGE;
            }
        }
        return overage;
    }

    /**
     * Computes the center point in spherical coordinates of a cell given by
     * a FaceIJK address at a specified resolution.
     *
     * @param res The H3 resolution of the cell.
     */
    public LatLng faceIjkToGeo(int res) {
        return coord.ijkToGeo(face, res, false);
    }

    /**
     * Computes the cell boundary in spherical coordinates for a pentagonal cell
     * for this FaceIJK address at a specified resolution.
     *
     * @param res    The H3 resolution of the cell.
     * @param start  The first topological vertex to return.
     * @param length The number of topological vertexes to return.
     */
    public CellBoundary faceIjkPentToCellBoundary(int res, int start, int length) {
        this.coord.downAp3();
        this.coord.downAp3r();
        int adjRes = res;
        if (H3Index.isResolutionClassIII(res)) {
            this.coord.downAp7r();
            adjRes += 1;
        }
        final int additionalIteration = length == Constants.NUM_PENT_VERTS ? 1 : 0;
        final boolean isResolutionClassIII = H3Index.isResolutionClassIII(res);
        final CellBoundary boundary = new CellBoundary();
        final CoordIJK scratch = new CoordIJK(0, 0, 0);
        final FaceIJK fijk = new FaceIJK(this.face, scratch);
        final int[][] coord = isResolutionClassIII ? VERTEX_CLASSIII : VERTEX_CLASSII;
        final CoordIJK lastCoord = new CoordIJK(0, 0, 0);
        int lastFace = this.face;
        for (int vert = start; vert < start + length + additionalIteration; vert++) {
            final int v = vert % Constants.NUM_PENT_VERTS;
            scratch.reset(coord[v][0], coord[v][1], coord[v][2]);
            scratch.ijkAdd(this.coord.i, this.coord.j, this.coord.k);
            scratch.ijkNormalize();
            fijk.face = this.face;

            fijk.adjustPentVertOverage(adjRes);

            if (isResolutionClassIII && vert > start) {
                final Vec2d orig2d0 = lastCoord.ijkToHex2d();

                final int currentToLastDir = adjacentFaceDir[fijk.face][lastFace];
                final FaceOrientIJK fijkOrient = faceNeighbors[fijk.face][currentToLastDir];

                lastCoord.reset(fijk.coord.i, fijk.coord.j, fijk.coord.k);
                for (int i = 0; i < fijkOrient.ccwRot60; i++) {
                    lastCoord.ijkRotate60ccw();
                }

                final int unitScale = unitScaleByCIIres[adjRes] * 3;
                lastCoord.ijkAdd(
                    Math.multiplyExact(fijkOrient.translateI, unitScale),
                    Math.multiplyExact(fijkOrient.translateJ, unitScale),
                    Math.multiplyExact(fijkOrient.translateK, unitScale)
                );
                lastCoord.ijkNormalize();

                final Vec2d orig2d1 = lastCoord.ijkToHex2d();

                final Vec2d edge0;
                final Vec2d edge1;
                switch (adjacentFaceDir[fijkOrient.face][fijk.face]) {
                    case IJ -> {
                        edge0 = maxDimByCIIVec2d[adjRes][0];
                        edge1 = maxDimByCIIVec2d[adjRes][1];
                    }
                    case JK -> {
                        edge0 = maxDimByCIIVec2d[adjRes][1];
                        edge1 = maxDimByCIIVec2d[adjRes][2];
                    }
                    default -> {
                        assert (adjacentFaceDir[fijkOrient.face][fijk.face] == KI);
                        edge0 = maxDimByCIIVec2d[adjRes][2];
                        edge1 = maxDimByCIIVec2d[adjRes][0];
                    }
                }

                final Vec2d inter = Vec2d.v2dIntersect(orig2d0, orig2d1, edge0, edge1);
                final LatLng point = inter.hex2dToGeo(fijkOrient.face, adjRes, true);
                boundary.add(point);
            }

            if (vert < start + Constants.NUM_PENT_VERTS) {
                final LatLng point = fijk.coord.ijkToGeo(fijk.face, adjRes, true);
                boundary.add(point);
            }
            lastFace = fijk.face;
            lastCoord.reset(fijk.coord.i, fijk.coord.j, fijk.coord.k);
        }
        return boundary;
    }

    /**
     * Generates the cell boundary in spherical coordinates for a cell given by this
     * FaceIJK address at a specified resolution.
     *
     * @param res    The H3 resolution of the cell.
     * @param start  The first topological vertex to return.
     * @param length The number of topological vertexes to return.
     */
    public CellBoundary faceIjkToCellBoundary(final int res, final int start, final int length) {
        this.coord.downAp3();
        this.coord.downAp3r();

        int adjRes = res;
        if (H3Index.isResolutionClassIII(res)) {
            this.coord.downAp7r();
            adjRes += 1;
        }

        final int additionalIteration = length == Constants.NUM_HEX_VERTS ? 1 : 0;
        final boolean isResolutionClassIII = H3Index.isResolutionClassIII(res);
        final CellBoundary boundary = new CellBoundary();
        final CoordIJK scratch1 = new CoordIJK(0, 0, 0);
        final FaceIJK fijk = new FaceIJK(this.face, scratch1);
        final CoordIJK scratch2 = isResolutionClassIII ? new CoordIJK(0, 0, 0) : null;
        final int[][] verts = isResolutionClassIII ? VERTEX_CLASSIII : VERTEX_CLASSII;
        int lastFace = -1;
        Overage lastOverage = Overage.NO_OVERAGE;
        for (int vert = start; vert < start + length + additionalIteration; vert++) {
            int v = vert % Constants.NUM_HEX_VERTS;
            scratch1.reset(verts[v][0], verts[v][1], verts[v][2]);
            scratch1.ijkAdd(this.coord.i, this.coord.j, this.coord.k);
            scratch1.ijkNormalize();
            fijk.face = this.face;

            final Overage overage = fijk.adjustOverageClassII(adjRes, false, true);

            /*
            Check for edge-crossing. Each face of the underlying icosahedron is a
            different projection plane. So if an edge of the hexagon crosses an
            icosahedron edge, an additional vertex must be introduced at that
            intersection point. Then each half of the cell edge can be projected
            to geographic coordinates using the appropriate icosahedron face
            projection. Note that Class II cell edges have vertices on the face
            edge, with no edge line intersections.
            */
            if (isResolutionClassIII && vert > start && fijk.face != lastFace && lastOverage != Overage.FACE_EDGE) {
                final int lastV = (v + 5) % Constants.NUM_HEX_VERTS;
                final int[] vertexLast = verts[lastV];
                final int[] vertexV = verts[v];
                scratch2.reset(
                    Math.addExact(vertexLast[0], this.coord.i),
                    Math.addExact(vertexLast[1], this.coord.j),
                    Math.addExact(vertexLast[2], this.coord.k)
                );
                scratch2.ijkNormalize();
                final Vec2d orig2d0 = scratch2.ijkToHex2d();
                scratch2.reset(
                    Math.addExact(vertexV[0], this.coord.i),
                    Math.addExact(vertexV[1], this.coord.j),
                    Math.addExact(vertexV[2], this.coord.k)
                );
                scratch2.ijkNormalize();
                final Vec2d orig2d1 = scratch2.ijkToHex2d();

                final int face2 = ((lastFace == this.face) ? fijk.face : lastFace);
                final Vec2d edge0;
                final Vec2d edge1;
                switch (adjacentFaceDir[this.face][face2]) {
                    case IJ -> {
                        edge0 = maxDimByCIIVec2d[adjRes][0];
                        edge1 = maxDimByCIIVec2d[adjRes][1];
                    }
                    case JK -> {
                        edge0 = maxDimByCIIVec2d[adjRes][1];
                        edge1 = maxDimByCIIVec2d[adjRes][2];
                    }
                    default -> {
                        assert (adjacentFaceDir[this.face][face2] == KI);
                        edge0 = maxDimByCIIVec2d[adjRes][2];
                        edge1 = maxDimByCIIVec2d[adjRes][0];
                    }
                }
                final Vec2d inter = Vec2d.v2dIntersect(orig2d0, orig2d1, edge0, edge1);
                /*
                If a point of intersection occurs at a hexagon vertex, then each
                adjacent hexagon edge will lie completely on a single icosahedron
                face, and no additional vertex is required.
                */
                final boolean isIntersectionAtVertex = orig2d0.numericallyIdentical(inter) || orig2d1.numericallyIdentical(inter);
                if (isIntersectionAtVertex == false) {
                    final LatLng point = inter.hex2dToGeo(this.face, adjRes, true);
                    boundary.add(point);
                }
            }

            if (vert < start + Constants.NUM_HEX_VERTS) {
                final LatLng point = fijk.coord.ijkToGeo(fijk.face, adjRes, true);
                boundary.add(point);
            }
            lastFace = fijk.face;
            lastOverage = overage;
        }
        return boundary;
    }

    /**
     * compute the corresponding H3Index.
     * @param res The cell resolution.
     * @param face The face.
     * @param coord The CoordIJK.
     * @return The encoded H3Index
     */
    static long faceIjkToH3(int res, int face, CoordIJK coord) {
        long h = H3Index.H3_INIT;
        h = H3Index.H3_set_mode(h, Constants.H3_CELL_MODE);
        h = H3Index.H3_set_resolution(h, res);

        if (res == 0) {
            if (coord.i > MAX_FACE_COORD || coord.j > MAX_FACE_COORD || coord.k > MAX_FACE_COORD) {
                throw new IllegalArgumentException(" out of range input");
            }

            return H3Index.H3_set_base_cell(h, BaseCells.getBaseCell(face, coord));
        }


        final CoordIJK scratch = new CoordIJK(0, 0, 0);
        for (int r = res; r > 0; r--) {
            final int lastI = coord.i;
            final int lastJ = coord.j;
            final int lastK = coord.k;
            if (H3Index.isResolutionClassIII(r)) {
                coord.upAp7();
                scratch.reset(coord.i, coord.j, coord.k);
                scratch.downAp7();
            } else {
                coord.upAp7r();
                scratch.reset(coord.i, coord.j, coord.k);
                scratch.downAp7r();
            }
            scratch.reset(Math.subtractExact(lastI, scratch.i), Math.subtractExact(lastJ, scratch.j), Math.subtractExact(lastK, scratch.k));
            scratch.ijkNormalize();
            h = H3Index.H3_set_index_digit(h, r, scratch.unitIjkToDigit());
        }

        if (coord.i > MAX_FACE_COORD || coord.j > MAX_FACE_COORD || coord.k > MAX_FACE_COORD) {
            throw new IllegalArgumentException(" out of range input");
        }

        final int baseCell = BaseCells.getBaseCell(face, coord);
        h = H3Index.H3_set_base_cell(h, baseCell);

        final int numRots = BaseCells.getBaseCellCCWrot60(face, coord);
        if (BaseCells.isBaseCellPentagon(baseCell)) {
            if (H3Index.h3LeadingNonZeroDigit(h) == CoordIJK.Direction.K_AXES_DIGIT.digit()) {
                if (BaseCells.baseCellIsCwOffset(baseCell, face)) {
                    h = H3Index.h3Rotate60cw(h);
                } else {
                    h = H3Index.h3Rotate60ccw(h);
                }
            }

            for (int i = 0; i < numRots; i++) {
                h = H3Index.h3RotatePent60ccw(h);
            }
        } else {
            for (int i = 0; i < numRots; i++) {
                h = H3Index.h3Rotate60ccw(h);
            }
        }

        return h;
    }

    /**
     * Adjusts a FaceIJK address for a pentagon vertex in a substrate grid in
     * place so that the resulting cell address is relative to the correct
     * icosahedral face.
     *
     * @param res The H3 resolution of the cell.
     */
    private void adjustPentVertOverage(int res) {
        Overage overage;
        do {
            overage = adjustOverageClassII(res, false, true);
        } while (overage == Overage.NEW_FACE);
    }
}
