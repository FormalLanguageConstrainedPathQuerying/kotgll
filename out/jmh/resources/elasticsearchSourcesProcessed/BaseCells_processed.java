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
 * Copyright 2016-2018 Uber Technologies, Inc.
 */
package org.elasticsearch.h3;

/**
 *  Base cell related lookup tables and access functions.
 */
final class BaseCells {

    private static class BaseCellData {
        final int homeFace;
        final int homeI;
        final int homeJ;
        final int homeK;
        final boolean isPentagon;
        final int[] cwOffsetPent;

        BaseCellData(int homeFace, int homeI, int homeJ, int homeK, boolean isPentagon, int[] cwOffsetPent) {
            this.homeFace = homeFace;
            this.homeI = homeI;
            this.homeJ = homeJ;
            this.homeK = homeK;
            this.isPentagon = isPentagon;
            this.cwOffsetPent = cwOffsetPent;
        }
    }

    /**
     * Resolution 0 base cell data table.
     * <p>
     * For each base cell, gives the "home" face and ijk+ coordinates on that face,
     * whether or not the base cell is a pentagon. Additionally, if the base cell
     * is a pentagon, the two cw offset rotation adjacent faces are given (-1
     * indicates that no cw offset rotation faces exist for this base cell).
     */
    private static final BaseCellData[] baseCellData = new BaseCellData[] {
        new BaseCellData(1, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 1, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(1, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 2, 0, 0, true, new int[] { -1, -1 }),   
        new BaseCellData(1, 1, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(1, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(1, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(1, 0, 1, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 1, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(11, 2, 0, 0, true, new int[] { 2, 6 }),    
        new BaseCellData(4, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(6, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 0, 1, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(7, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(2, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 1, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(6, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(10, 2, 0, 0, true, new int[] { 1, 5 }),    
        new BaseCellData(6, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(11, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(4, 1, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 0, 1, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(4, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(5, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(0, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(7, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(11, 1, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(7, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(10, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(12, 2, 0, 0, true, new int[] { 3, 7 }),    
        new BaseCellData(6, 1, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(7, 1, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(4, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(3, 0, 1, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(4, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(6, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(11, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(8, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(5, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(14, 2, 0, 0, true, new int[] { 0, 9 }),    
        new BaseCellData(5, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(12, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(10, 1, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(4, 0, 1, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(12, 1, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(7, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(11, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(10, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(13, 2, 0, 0, true, new int[] { 4, 8 }),    
        new BaseCellData(10, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(11, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(9, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(8, 0, 1, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(6, 2, 0, 0, true, new int[] { 11, 15 }),   
        new BaseCellData(8, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(9, 0, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(14, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(5, 1, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(16, 0, 1, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(8, 1, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(5, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(12, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(7, 2, 0, 0, true, new int[] { 12, 16 }),   
        new BaseCellData(12, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(10, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(9, 0, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(13, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(16, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(15, 0, 1, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(15, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(16, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(14, 1, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(13, 1, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(5, 2, 0, 0, true, new int[] { 10, 19 }),   
        new BaseCellData(8, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(14, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(9, 1, 0, 1, false, new int[] { 0, 0 }),     
        new BaseCellData(14, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(12, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(16, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 0, 1, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(15, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(16, 1, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(9, 1, 0, 0, false, new int[] { 0, 0 }),     
        new BaseCellData(15, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(13, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(8, 2, 0, 0, true, new int[] { 13, 17 }),   
        new BaseCellData(13, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 1, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(14, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 0, 1, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(13, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(16, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(9, 2, 0, 0, true, new int[] { 14, 18 }),   
        new BaseCellData(15, 1, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(15, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 0, 1, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 0, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(17, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 0, 1, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 1, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 2, 0, 0, true, new int[] { -1, -1 }),  
        new BaseCellData(19, 1, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 0, 0, 0, false, new int[] { 0, 0 }),    
        new BaseCellData(19, 1, 0, 1, false, new int[] { 0, 0 }),    
        new BaseCellData(18, 1, 0, 0, false, new int[] { 0, 0 })     
    };

    /**
     *  base cell at a given ijk and required rotations into its system
     */
    private static class BaseCellRotation {
        final int baseCell;  
        final int ccwRot60;  

        BaseCellRotation(int baseCell, int ccwRot60) {
            this.baseCell = baseCell;
            this.ccwRot60 = ccwRot60;
        }
    }

    /** @brief Resolution 0 base cell lookup table for each face.
     *
     * Given the face number and a resolution 0 ijk+ coordinate in that face's
     * face-centered ijk coordinate system, gives the base cell located at that
     * coordinate and the number of 60 ccw rotations to rotate into that base
     * cell's orientation.
     *
     * Valid lookup coordinates are from (0, 0, 0) to (2, 2, 2).
     *
     * This table can be accessed using the functions `_faceIjkToBaseCell` and
     * `_faceIjkToBaseCellCCWrot60`
     */
    private static final BaseCellRotation[][][][] faceIjkBaseCells = new BaseCellRotation[][][][] {
        {
            {
                { new BaseCellRotation(16, 0), new BaseCellRotation(18, 0), new BaseCellRotation(24, 0) },  
                { new BaseCellRotation(33, 0), new BaseCellRotation(30, 0), new BaseCellRotation(32, 3) },  
                { new BaseCellRotation(49, 1), new BaseCellRotation(48, 3), new BaseCellRotation(50, 3) }   
            },
            {
                { new BaseCellRotation(8, 0), new BaseCellRotation(5, 5), new BaseCellRotation(10, 5) },    
                { new BaseCellRotation(22, 0), new BaseCellRotation(16, 0), new BaseCellRotation(18, 0) },  
                { new BaseCellRotation(41, 1), new BaseCellRotation(33, 0), new BaseCellRotation(30, 0) }   
            },
            {
                { new BaseCellRotation(4, 0), new BaseCellRotation(0, 5), new BaseCellRotation(2, 5) },    
                { new BaseCellRotation(15, 1), new BaseCellRotation(8, 0), new BaseCellRotation(5, 5) },   
                { new BaseCellRotation(31, 1), new BaseCellRotation(22, 0), new BaseCellRotation(16, 0) }  
            } },
        {
            {
                { new BaseCellRotation(2, 0), new BaseCellRotation(6, 0), new BaseCellRotation(14, 0) },    
                { new BaseCellRotation(10, 0), new BaseCellRotation(11, 0), new BaseCellRotation(17, 3) },  
                { new BaseCellRotation(24, 1), new BaseCellRotation(23, 3), new BaseCellRotation(25, 3) }   
            },
            {
                { new BaseCellRotation(0, 0), new BaseCellRotation(1, 5), new BaseCellRotation(9, 5) },    
                { new BaseCellRotation(5, 0), new BaseCellRotation(2, 0), new BaseCellRotation(6, 0) },    
                { new BaseCellRotation(18, 1), new BaseCellRotation(10, 0), new BaseCellRotation(11, 0) }  
            },
            {
                { new BaseCellRotation(4, 1), new BaseCellRotation(3, 5), new BaseCellRotation(7, 5) },  
                { new BaseCellRotation(8, 1), new BaseCellRotation(0, 0), new BaseCellRotation(1, 5) },  
                { new BaseCellRotation(16, 1), new BaseCellRotation(5, 0), new BaseCellRotation(2, 0) }  
            } },
        {
            {
                { new BaseCellRotation(7, 0), new BaseCellRotation(21, 0), new BaseCellRotation(38, 0) },  
                { new BaseCellRotation(9, 0), new BaseCellRotation(19, 0), new BaseCellRotation(34, 3) },  
                { new BaseCellRotation(14, 1), new BaseCellRotation(20, 3), new BaseCellRotation(36, 3) }  
            },
            {
                { new BaseCellRotation(3, 0), new BaseCellRotation(13, 5), new BaseCellRotation(29, 5) },  
                { new BaseCellRotation(1, 0), new BaseCellRotation(7, 0), new BaseCellRotation(21, 0) },   
                { new BaseCellRotation(6, 1), new BaseCellRotation(9, 0), new BaseCellRotation(19, 0) }    
            },
            {
                { new BaseCellRotation(4, 2), new BaseCellRotation(12, 5), new BaseCellRotation(26, 5) },  
                { new BaseCellRotation(0, 1), new BaseCellRotation(3, 0), new BaseCellRotation(13, 5) },   
                { new BaseCellRotation(2, 1), new BaseCellRotation(1, 0), new BaseCellRotation(7, 0) }     
            } },
        {
            {
                { new BaseCellRotation(26, 0), new BaseCellRotation(42, 0), new BaseCellRotation(58, 0) },  
                { new BaseCellRotation(29, 0), new BaseCellRotation(43, 0), new BaseCellRotation(62, 3) },  
                { new BaseCellRotation(38, 1), new BaseCellRotation(47, 3), new BaseCellRotation(64, 3) }   
            },
            {
                { new BaseCellRotation(12, 0), new BaseCellRotation(28, 5), new BaseCellRotation(44, 5) },  
                { new BaseCellRotation(13, 0), new BaseCellRotation(26, 0), new BaseCellRotation(42, 0) },  
                { new BaseCellRotation(21, 1), new BaseCellRotation(29, 0), new BaseCellRotation(43, 0) }   
            },
            {
                { new BaseCellRotation(4, 3), new BaseCellRotation(15, 5), new BaseCellRotation(31, 5) },  
                { new BaseCellRotation(3, 1), new BaseCellRotation(12, 0), new BaseCellRotation(28, 5) },  
                { new BaseCellRotation(7, 1), new BaseCellRotation(13, 0), new BaseCellRotation(26, 0) }   
            } },
        {
            {
                { new BaseCellRotation(31, 0), new BaseCellRotation(41, 0), new BaseCellRotation(49, 0) },  
                { new BaseCellRotation(44, 0), new BaseCellRotation(53, 0), new BaseCellRotation(61, 3) },  
                { new BaseCellRotation(58, 1), new BaseCellRotation(65, 3), new BaseCellRotation(75, 3) }   
            },
            {
                { new BaseCellRotation(15, 0), new BaseCellRotation(22, 5), new BaseCellRotation(33, 5) },  
                { new BaseCellRotation(28, 0), new BaseCellRotation(31, 0), new BaseCellRotation(41, 0) },  
                { new BaseCellRotation(42, 1), new BaseCellRotation(44, 0), new BaseCellRotation(53, 0) }   
            },
            {
                { new BaseCellRotation(4, 4), new BaseCellRotation(8, 5), new BaseCellRotation(16, 5) },    
                { new BaseCellRotation(12, 1), new BaseCellRotation(15, 0), new BaseCellRotation(22, 5) },  
                { new BaseCellRotation(26, 1), new BaseCellRotation(28, 0), new BaseCellRotation(31, 0) }   
            } },
        {
            {
                { new BaseCellRotation(50, 0), new BaseCellRotation(48, 0), new BaseCellRotation(49, 3) },  
                { new BaseCellRotation(32, 0), new BaseCellRotation(30, 3), new BaseCellRotation(33, 3) },  
                { new BaseCellRotation(24, 3), new BaseCellRotation(18, 3), new BaseCellRotation(16, 3) }   
            },
            {
                { new BaseCellRotation(70, 0), new BaseCellRotation(67, 0), new BaseCellRotation(66, 3) },  
                { new BaseCellRotation(52, 3), new BaseCellRotation(50, 0), new BaseCellRotation(48, 0) },  
                { new BaseCellRotation(37, 3), new BaseCellRotation(32, 0), new BaseCellRotation(30, 3) }   
            },
            {
                { new BaseCellRotation(83, 0), new BaseCellRotation(87, 3), new BaseCellRotation(85, 3) },  
                { new BaseCellRotation(74, 3), new BaseCellRotation(70, 0), new BaseCellRotation(67, 0) },  
                { new BaseCellRotation(57, 1), new BaseCellRotation(52, 3), new BaseCellRotation(50, 0) }   
            } },
        {
            {
                { new BaseCellRotation(25, 0), new BaseCellRotation(23, 0), new BaseCellRotation(24, 3) },  
                { new BaseCellRotation(17, 0), new BaseCellRotation(11, 3), new BaseCellRotation(10, 3) },  
                { new BaseCellRotation(14, 3), new BaseCellRotation(6, 3), new BaseCellRotation(2, 3) }     
            },
            {
                { new BaseCellRotation(45, 0), new BaseCellRotation(39, 0), new BaseCellRotation(37, 3) },  
                { new BaseCellRotation(35, 3), new BaseCellRotation(25, 0), new BaseCellRotation(23, 0) },  
                { new BaseCellRotation(27, 3), new BaseCellRotation(17, 0), new BaseCellRotation(11, 3) }   
            },
            {
                { new BaseCellRotation(63, 0), new BaseCellRotation(59, 3), new BaseCellRotation(57, 3) },  
                { new BaseCellRotation(56, 3), new BaseCellRotation(45, 0), new BaseCellRotation(39, 0) },  
                { new BaseCellRotation(46, 3), new BaseCellRotation(35, 3), new BaseCellRotation(25, 0) }   
            } },
        {
            {
                { new BaseCellRotation(36, 0), new BaseCellRotation(20, 0), new BaseCellRotation(14, 3) },  
                { new BaseCellRotation(34, 0), new BaseCellRotation(19, 3), new BaseCellRotation(9, 3) },   
                { new BaseCellRotation(38, 3), new BaseCellRotation(21, 3), new BaseCellRotation(7, 3) }    
            },
            {
                { new BaseCellRotation(55, 0), new BaseCellRotation(40, 0), new BaseCellRotation(27, 3) },  
                { new BaseCellRotation(54, 3), new BaseCellRotation(36, 0), new BaseCellRotation(20, 0) },  
                { new BaseCellRotation(51, 3), new BaseCellRotation(34, 0), new BaseCellRotation(19, 3) }   
            },
            {
                { new BaseCellRotation(72, 0), new BaseCellRotation(60, 3), new BaseCellRotation(46, 3) },  
                { new BaseCellRotation(73, 3), new BaseCellRotation(55, 0), new BaseCellRotation(40, 0) },  
                { new BaseCellRotation(71, 3), new BaseCellRotation(54, 3), new BaseCellRotation(36, 0) }   
            } },
        {
            {
                { new BaseCellRotation(64, 0), new BaseCellRotation(47, 0), new BaseCellRotation(38, 3) },  
                { new BaseCellRotation(62, 0), new BaseCellRotation(43, 3), new BaseCellRotation(29, 3) },  
                { new BaseCellRotation(58, 3), new BaseCellRotation(42, 3), new BaseCellRotation(26, 3) }   
            },
            {
                { new BaseCellRotation(84, 0), new BaseCellRotation(69, 0), new BaseCellRotation(51, 3) },  
                { new BaseCellRotation(82, 3), new BaseCellRotation(64, 0), new BaseCellRotation(47, 0) },  
                { new BaseCellRotation(76, 3), new BaseCellRotation(62, 0), new BaseCellRotation(43, 3) }   
            },
            {
                { new BaseCellRotation(97, 0), new BaseCellRotation(89, 3), new BaseCellRotation(71, 3) },  
                { new BaseCellRotation(98, 3), new BaseCellRotation(84, 0), new BaseCellRotation(69, 0) },  
                { new BaseCellRotation(96, 3), new BaseCellRotation(82, 3), new BaseCellRotation(64, 0) }   
            } },
        {
            {
                { new BaseCellRotation(75, 0), new BaseCellRotation(65, 0), new BaseCellRotation(58, 3) },  
                { new BaseCellRotation(61, 0), new BaseCellRotation(53, 3), new BaseCellRotation(44, 3) },  
                { new BaseCellRotation(49, 3), new BaseCellRotation(41, 3), new BaseCellRotation(31, 3) }   
            },
            {
                { new BaseCellRotation(94, 0), new BaseCellRotation(86, 0), new BaseCellRotation(76, 3) },  
                { new BaseCellRotation(81, 3), new BaseCellRotation(75, 0), new BaseCellRotation(65, 0) },  
                { new BaseCellRotation(66, 3), new BaseCellRotation(61, 0), new BaseCellRotation(53, 3) }   
            },
            {
                { new BaseCellRotation(107, 0), new BaseCellRotation(104, 3), new BaseCellRotation(96, 3) },  
                { new BaseCellRotation(101, 3), new BaseCellRotation(94, 0), new BaseCellRotation(86, 0) },   
                { new BaseCellRotation(85, 3), new BaseCellRotation(81, 3), new BaseCellRotation(75, 0) }     
            } },
        {
            {
                { new BaseCellRotation(57, 0), new BaseCellRotation(59, 0), new BaseCellRotation(63, 3) },  
                { new BaseCellRotation(74, 0), new BaseCellRotation(78, 3), new BaseCellRotation(79, 3) },  
                { new BaseCellRotation(83, 3), new BaseCellRotation(92, 3), new BaseCellRotation(95, 3) }   
            },
            {
                { new BaseCellRotation(37, 0), new BaseCellRotation(39, 3), new BaseCellRotation(45, 3) },  
                { new BaseCellRotation(52, 0), new BaseCellRotation(57, 0), new BaseCellRotation(59, 0) },  
                { new BaseCellRotation(70, 3), new BaseCellRotation(74, 0), new BaseCellRotation(78, 3) }   
            },
            {
                { new BaseCellRotation(24, 0), new BaseCellRotation(23, 3), new BaseCellRotation(25, 3) },  
                { new BaseCellRotation(32, 3), new BaseCellRotation(37, 0), new BaseCellRotation(39, 3) },  
                { new BaseCellRotation(50, 3), new BaseCellRotation(52, 0), new BaseCellRotation(57, 0) }   
            } },
        {
            {
                { new BaseCellRotation(46, 0), new BaseCellRotation(60, 0), new BaseCellRotation(72, 3) },  
                { new BaseCellRotation(56, 0), new BaseCellRotation(68, 3), new BaseCellRotation(80, 3) },  
                { new BaseCellRotation(63, 3), new BaseCellRotation(77, 3), new BaseCellRotation(90, 3) }   
            },
            {
                { new BaseCellRotation(27, 0), new BaseCellRotation(40, 3), new BaseCellRotation(55, 3) },  
                { new BaseCellRotation(35, 0), new BaseCellRotation(46, 0), new BaseCellRotation(60, 0) },  
                { new BaseCellRotation(45, 3), new BaseCellRotation(56, 0), new BaseCellRotation(68, 3) }   
            },
            {
                { new BaseCellRotation(14, 0), new BaseCellRotation(20, 3), new BaseCellRotation(36, 3) },  
                { new BaseCellRotation(17, 3), new BaseCellRotation(27, 0), new BaseCellRotation(40, 3) },  
                { new BaseCellRotation(25, 3), new BaseCellRotation(35, 0), new BaseCellRotation(46, 0) }   
            } },
        {
            {
                { new BaseCellRotation(71, 0), new BaseCellRotation(89, 0), new BaseCellRotation(97, 3) },   
                { new BaseCellRotation(73, 0), new BaseCellRotation(91, 3), new BaseCellRotation(103, 3) },  
                { new BaseCellRotation(72, 3), new BaseCellRotation(88, 3), new BaseCellRotation(105, 3) }   
            },
            {
                { new BaseCellRotation(51, 0), new BaseCellRotation(69, 3), new BaseCellRotation(84, 3) },  
                { new BaseCellRotation(54, 0), new BaseCellRotation(71, 0), new BaseCellRotation(89, 0) },  
                { new BaseCellRotation(55, 3), new BaseCellRotation(73, 0), new BaseCellRotation(91, 3) }   
            },
            {
                { new BaseCellRotation(38, 0), new BaseCellRotation(47, 3), new BaseCellRotation(64, 3) },  
                { new BaseCellRotation(34, 3), new BaseCellRotation(51, 0), new BaseCellRotation(69, 3) },  
                { new BaseCellRotation(36, 3), new BaseCellRotation(54, 0), new BaseCellRotation(71, 0) }   
            } },
        {
            {
                { new BaseCellRotation(96, 0), new BaseCellRotation(104, 0), new BaseCellRotation(107, 3) },  
                { new BaseCellRotation(98, 0), new BaseCellRotation(110, 3), new BaseCellRotation(115, 3) },  
                { new BaseCellRotation(97, 3), new BaseCellRotation(111, 3), new BaseCellRotation(119, 3) }   
            },
            {
                { new BaseCellRotation(76, 0), new BaseCellRotation(86, 3), new BaseCellRotation(94, 3) },   
                { new BaseCellRotation(82, 0), new BaseCellRotation(96, 0), new BaseCellRotation(104, 0) },  
                { new BaseCellRotation(84, 3), new BaseCellRotation(98, 0), new BaseCellRotation(110, 3) }   
            },
            {
                { new BaseCellRotation(58, 0), new BaseCellRotation(65, 3), new BaseCellRotation(75, 3) },  
                { new BaseCellRotation(62, 3), new BaseCellRotation(76, 0), new BaseCellRotation(86, 3) },  
                { new BaseCellRotation(64, 3), new BaseCellRotation(82, 0), new BaseCellRotation(96, 0) }   
            } },
        {
            {
                { new BaseCellRotation(85, 0), new BaseCellRotation(87, 0), new BaseCellRotation(83, 3) },     
                { new BaseCellRotation(101, 0), new BaseCellRotation(102, 3), new BaseCellRotation(100, 3) },  
                { new BaseCellRotation(107, 3), new BaseCellRotation(112, 3), new BaseCellRotation(114, 3) }   
            },
            {
                { new BaseCellRotation(66, 0), new BaseCellRotation(67, 3), new BaseCellRotation(70, 3) },   
                { new BaseCellRotation(81, 0), new BaseCellRotation(85, 0), new BaseCellRotation(87, 0) },   
                { new BaseCellRotation(94, 3), new BaseCellRotation(101, 0), new BaseCellRotation(102, 3) }  
            },
            {
                { new BaseCellRotation(49, 0), new BaseCellRotation(48, 3), new BaseCellRotation(50, 3) },  
                { new BaseCellRotation(61, 3), new BaseCellRotation(66, 0), new BaseCellRotation(67, 3) },  
                { new BaseCellRotation(75, 3), new BaseCellRotation(81, 0), new BaseCellRotation(85, 0) }   
            } },
        {
            {
                { new BaseCellRotation(95, 0), new BaseCellRotation(92, 0), new BaseCellRotation(83, 0) },  
                { new BaseCellRotation(79, 0), new BaseCellRotation(78, 0), new BaseCellRotation(74, 3) },  
                { new BaseCellRotation(63, 1), new BaseCellRotation(59, 3), new BaseCellRotation(57, 3) }   
            },
            {
                { new BaseCellRotation(109, 0), new BaseCellRotation(108, 0), new BaseCellRotation(100, 5) },  
                { new BaseCellRotation(93, 1), new BaseCellRotation(95, 0), new BaseCellRotation(92, 0) },     
                { new BaseCellRotation(77, 1), new BaseCellRotation(79, 0), new BaseCellRotation(78, 0) }      
            },
            {
                { new BaseCellRotation(117, 4), new BaseCellRotation(118, 5), new BaseCellRotation(114, 5) },  
                { new BaseCellRotation(106, 1), new BaseCellRotation(109, 0), new BaseCellRotation(108, 0) },  
                { new BaseCellRotation(90, 1), new BaseCellRotation(93, 1), new BaseCellRotation(95, 0) }      
            } },
        {
            {
                { new BaseCellRotation(90, 0), new BaseCellRotation(77, 0), new BaseCellRotation(63, 0) },  
                { new BaseCellRotation(80, 0), new BaseCellRotation(68, 0), new BaseCellRotation(56, 3) },  
                { new BaseCellRotation(72, 1), new BaseCellRotation(60, 3), new BaseCellRotation(46, 3) }   
            },
            {
                { new BaseCellRotation(106, 0), new BaseCellRotation(93, 0), new BaseCellRotation(79, 5) },  
                { new BaseCellRotation(99, 1), new BaseCellRotation(90, 0), new BaseCellRotation(77, 0) },   
                { new BaseCellRotation(88, 1), new BaseCellRotation(80, 0), new BaseCellRotation(68, 0) }    
            },
            {
                { new BaseCellRotation(117, 3), new BaseCellRotation(109, 5), new BaseCellRotation(95, 5) },  
                { new BaseCellRotation(113, 1), new BaseCellRotation(106, 0), new BaseCellRotation(93, 0) },  
                { new BaseCellRotation(105, 1), new BaseCellRotation(99, 1), new BaseCellRotation(90, 0) }    
            } },
        {
            {
                { new BaseCellRotation(105, 0), new BaseCellRotation(88, 0), new BaseCellRotation(72, 0) },  
                { new BaseCellRotation(103, 0), new BaseCellRotation(91, 0), new BaseCellRotation(73, 3) },  
                { new BaseCellRotation(97, 1), new BaseCellRotation(89, 3), new BaseCellRotation(71, 3) }    
            },
            {
                { new BaseCellRotation(113, 0), new BaseCellRotation(99, 0), new BaseCellRotation(80, 5) },   
                { new BaseCellRotation(116, 1), new BaseCellRotation(105, 0), new BaseCellRotation(88, 0) },  
                { new BaseCellRotation(111, 1), new BaseCellRotation(103, 0), new BaseCellRotation(91, 0) }   
            },
            {
                { new BaseCellRotation(117, 2), new BaseCellRotation(106, 5), new BaseCellRotation(90, 5) },  
                { new BaseCellRotation(121, 1), new BaseCellRotation(113, 0), new BaseCellRotation(99, 0) },  
                { new BaseCellRotation(119, 1), new BaseCellRotation(116, 1), new BaseCellRotation(105, 0) }  
            } },
        {
            {
                { new BaseCellRotation(119, 0), new BaseCellRotation(111, 0), new BaseCellRotation(97, 0) },  
                { new BaseCellRotation(115, 0), new BaseCellRotation(110, 0), new BaseCellRotation(98, 3) },  
                { new BaseCellRotation(107, 1), new BaseCellRotation(104, 3), new BaseCellRotation(96, 3) }   
            },
            {
                { new BaseCellRotation(121, 0), new BaseCellRotation(116, 0), new BaseCellRotation(103, 5) },  
                { new BaseCellRotation(120, 1), new BaseCellRotation(119, 0), new BaseCellRotation(111, 0) },  
                { new BaseCellRotation(112, 1), new BaseCellRotation(115, 0), new BaseCellRotation(110, 0) }   
            },
            {
                { new BaseCellRotation(117, 1), new BaseCellRotation(113, 5), new BaseCellRotation(105, 5) },  
                { new BaseCellRotation(118, 1), new BaseCellRotation(121, 0), new BaseCellRotation(116, 0) },  
                { new BaseCellRotation(114, 1), new BaseCellRotation(120, 1), new BaseCellRotation(119, 0) }   
            } },
        {
            {
                { new BaseCellRotation(114, 0), new BaseCellRotation(112, 0), new BaseCellRotation(107, 0) },  
                { new BaseCellRotation(100, 0), new BaseCellRotation(102, 0), new BaseCellRotation(101, 3) },  
                { new BaseCellRotation(83, 1), new BaseCellRotation(87, 3), new BaseCellRotation(85, 3) }      
            },
            {
                { new BaseCellRotation(118, 0), new BaseCellRotation(120, 0), new BaseCellRotation(115, 5) },  
                { new BaseCellRotation(108, 1), new BaseCellRotation(114, 0), new BaseCellRotation(112, 0) },  
                { new BaseCellRotation(92, 1), new BaseCellRotation(100, 0), new BaseCellRotation(102, 0) }    
            },
            {
                { new BaseCellRotation(117, 0), new BaseCellRotation(121, 5), new BaseCellRotation(119, 5) },  
                { new BaseCellRotation(109, 1), new BaseCellRotation(118, 0), new BaseCellRotation(120, 0) },  
                { new BaseCellRotation(95, 1), new BaseCellRotation(108, 1), new BaseCellRotation(114, 0) }    
            } } };

    /**
     *  Return whether or not the indicated base cell is a pentagon.
     */
    public static boolean isBaseCellPentagon(int baseCell) {
        if (baseCell < 0 || baseCell >= Constants.NUM_BASE_CELLS) {  
            return false;
        }
        return baseCellData[baseCell].isPentagon;
    }

    /**
     *  Return whether or not the indicated base cell is a pentagon.
     */
    public static FaceIJK getBaseFaceIJK(int baseCell) {
        if (baseCell < 0 || baseCell >= Constants.NUM_BASE_CELLS) {  
            throw new IllegalArgumentException("Illegal base cell");
        }
        BaseCellData cellData = baseCellData[baseCell];
        return new FaceIJK(cellData.homeFace, new CoordIJK(cellData.homeI, cellData.homeJ, cellData.homeK));
    }

    /** Find base cell given a face and a CoordIJK.
     *
     * Given the face number and a resolution 0 ijk+ coordinate in that face's
     * face-centered ijk coordinate system, return the base cell located at that
     * coordinate.
     *
     * Valid ijk+ lookup coordinates are from (0, 0, 0) to (2, 2, 2).
     */
    public static int getBaseCell(int face, CoordIJK coord) {
        return faceIjkBaseCells[face][coord.i][coord.j][coord.k].baseCell;
    }

    /** Find base cell given a face and a CoordIJK.
     *
     * Given the face number and a resolution 0 ijk+ coordinate in that face's
     * face-centered ijk coordinate system, return the number of 60' ccw rotations
     * to rotate into the coordinate system of the base cell at that coordinates.
     *
     * Valid ijk+ lookup coordinates are from (0, 0, 0) to (2, 2, 2).
     */
    public static int getBaseCellCCWrot60(int face, CoordIJK coord) {
        return faceIjkBaseCells[face][coord.i][coord.j][coord.k].ccwRot60;
    }

    /**  Return whether or not the tested face is a cw offset face.
     */
    public static boolean baseCellIsCwOffset(int baseCell, int testFace) {
        return baseCellData[baseCell].cwOffsetPent[0] == testFace || baseCellData[baseCell].cwOffsetPent[1] == testFace;
    }

    /** Return whether the indicated base cell is a pentagon where all
     * neighbors are oriented towards it. */
    public static boolean isBaseCellPolarPentagon(int baseCell) {
        return baseCell == 4 || baseCell == 117;
    }

}
