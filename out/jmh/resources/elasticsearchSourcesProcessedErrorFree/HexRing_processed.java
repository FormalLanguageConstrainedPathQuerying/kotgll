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
 * Computes the neighbour H3 index from a given index.
 */
final class HexRing {

    private static final int INVALID_BASE_CELL = 127;

    /** Neighboring base cell ID in each IJK direction.
     *
     * For each base cell, for each direction, the neighboring base
     * cell ID is given. 127 indicates there is no neighbor in that direction.
     */
    private static final int[][] baseCellNeighbors = new int[][] {
        { 0, 1, 5, 2, 4, 3, 8 },                          
        { 1, 7, 6, 9, 0, 3, 2 },                          
        { 2, 6, 10, 11, 0, 1, 5 },                        
        { 3, 13, 1, 7, 4, 12, 0 },                        
        { 4, INVALID_BASE_CELL, 15, 8, 3, 0, 12 },        
        { 5, 2, 18, 10, 8, 0, 16 },                       
        { 6, 14, 11, 17, 1, 9, 2 },                       
        { 7, 21, 9, 19, 3, 13, 1 },                       
        { 8, 5, 22, 16, 4, 0, 15 },                       
        { 9, 19, 14, 20, 1, 7, 6 },                       
        { 10, 11, 24, 23, 5, 2, 18 },                     
        { 11, 17, 23, 25, 2, 6, 10 },                     
        { 12, 28, 13, 26, 4, 15, 3 },                     
        { 13, 26, 21, 29, 3, 12, 7 },                     
        { 14, INVALID_BASE_CELL, 17, 27, 9, 20, 6 },      
        { 15, 22, 28, 31, 4, 8, 12 },                     
        { 16, 18, 33, 30, 8, 5, 22 },                     
        { 17, 11, 14, 6, 35, 25, 27 },                    
        { 18, 24, 30, 32, 5, 10, 16 },                    
        { 19, 34, 20, 36, 7, 21, 9 },                     
        { 20, 14, 19, 9, 40, 27, 36 },                    
        { 21, 38, 19, 34, 13, 29, 7 },                    
        { 22, 16, 41, 33, 15, 8, 31 },                    
        { 23, 24, 11, 10, 39, 37, 25 },                   
        { 24, INVALID_BASE_CELL, 32, 37, 10, 23, 18 },    
        { 25, 23, 17, 11, 45, 39, 35 },                   
        { 26, 42, 29, 43, 12, 28, 13 },                   
        { 27, 40, 35, 46, 14, 20, 17 },                   
        { 28, 31, 42, 44, 12, 15, 26 },                   
        { 29, 43, 38, 47, 13, 26, 21 },                   
        { 30, 32, 48, 50, 16, 18, 33 },                   
        { 31, 41, 44, 53, 15, 22, 28 },                   
        { 32, 30, 24, 18, 52, 50, 37 },                   
        { 33, 30, 49, 48, 22, 16, 41 },                   
        { 34, 19, 38, 21, 54, 36, 51 },                   
        { 35, 46, 45, 56, 17, 27, 25 },                   
        { 36, 20, 34, 19, 55, 40, 54 },                   
        { 37, 39, 52, 57, 24, 23, 32 },                   
        { 38, INVALID_BASE_CELL, 34, 51, 29, 47, 21 },    
        { 39, 37, 25, 23, 59, 57, 45 },                   
        { 40, 27, 36, 20, 60, 46, 55 },                   
        { 41, 49, 53, 61, 22, 33, 31 },                   
        { 42, 58, 43, 62, 28, 44, 26 },                   
        { 43, 62, 47, 64, 26, 42, 29 },                   
        { 44, 53, 58, 65, 28, 31, 42 },                   
        { 45, 39, 35, 25, 63, 59, 56 },                   
        { 46, 60, 56, 68, 27, 40, 35 },                   
        { 47, 38, 43, 29, 69, 51, 64 },                   
        { 48, 49, 30, 33, 67, 66, 50 },                   
        { 49, INVALID_BASE_CELL, 61, 66, 33, 48, 41 },    
        { 50, 48, 32, 30, 70, 67, 52 },                   
        { 51, 69, 54, 71, 38, 47, 34 },                   
        { 52, 57, 70, 74, 32, 37, 50 },                   
        { 53, 61, 65, 75, 31, 41, 44 },                   
        { 54, 71, 55, 73, 34, 51, 36 },                   
        { 55, 40, 54, 36, 72, 60, 73 },                   
        { 56, 68, 63, 77, 35, 46, 45 },                   
        { 57, 59, 74, 78, 37, 39, 52 },                   
        { 58, INVALID_BASE_CELL, 62, 76, 44, 65, 42 },    
        { 59, 63, 78, 79, 39, 45, 57 },                   
        { 60, 72, 68, 80, 40, 55, 46 },                   
        { 61, 53, 49, 41, 81, 75, 66 },                   
        { 62, 43, 58, 42, 82, 64, 76 },                   
        { 63, INVALID_BASE_CELL, 56, 45, 79, 59, 77 },    
        { 64, 47, 62, 43, 84, 69, 82 },                   
        { 65, 58, 53, 44, 86, 76, 75 },                   
        { 66, 67, 81, 85, 49, 48, 61 },                   
        { 67, 66, 50, 48, 87, 85, 70 },                   
        { 68, 56, 60, 46, 90, 77, 80 },                   
        { 69, 51, 64, 47, 89, 71, 84 },                   
        { 70, 67, 52, 50, 83, 87, 74 },                   
        { 71, 89, 73, 91, 51, 69, 54 },                   
        { 72, INVALID_BASE_CELL, 73, 55, 80, 60, 88 },    
        { 73, 91, 72, 88, 54, 71, 55 },                   
        { 74, 78, 83, 92, 52, 57, 70 },                   
        { 75, 65, 61, 53, 94, 86, 81 },                   
        { 76, 86, 82, 96, 58, 65, 62 },                   
        { 77, 63, 68, 56, 93, 79, 90 },                   
        { 78, 74, 59, 57, 95, 92, 79 },                   
        { 79, 78, 63, 59, 93, 95, 77 },                   
        { 80, 68, 72, 60, 99, 90, 88 },                   
        { 81, 85, 94, 101, 61, 66, 75 },                  
        { 82, 96, 84, 98, 62, 76, 64 },                   
        { 83, INVALID_BASE_CELL, 74, 70, 100, 87, 92 },   
        { 84, 69, 82, 64, 97, 89, 98 },                   
        { 85, 87, 101, 102, 66, 67, 81 },                 
        { 86, 76, 75, 65, 104, 96, 94 },                  
        { 87, 83, 102, 100, 67, 70, 85 },                 
        { 88, 72, 91, 73, 99, 80, 105 },                  
        { 89, 97, 91, 103, 69, 84, 71 },                  
        { 90, 77, 80, 68, 106, 93, 99 },                  
        { 91, 73, 89, 71, 105, 88, 103 },                 
        { 92, 83, 78, 74, 108, 100, 95 },                 
        { 93, 79, 90, 77, 109, 95, 106 },                 
        { 94, 86, 81, 75, 107, 104, 101 },                
        { 95, 92, 79, 78, 109, 108, 93 },                 
        { 96, 104, 98, 110, 76, 86, 82 },                 
        { 97, INVALID_BASE_CELL, 98, 84, 103, 89, 111 },  
        { 98, 110, 97, 111, 82, 96, 84 },                 
        { 99, 80, 105, 88, 106, 90, 113 },                
        { 100, 102, 83, 87, 108, 114, 92 },               
        { 101, 102, 107, 112, 81, 85, 94 },               
        { 102, 101, 87, 85, 114, 112, 100 },              
        { 103, 91, 97, 89, 116, 105, 111 },               
        { 104, 107, 110, 115, 86, 94, 96 },               
        { 105, 88, 103, 91, 113, 99, 116 },               
        { 106, 93, 99, 90, 117, 109, 113 },               
        { 107, INVALID_BASE_CELL, 101, 94, 115, 104, 112 },                                
        { 108, 100, 95, 92, 118, 114, 109 },    
        { 109, 108, 93, 95, 117, 118, 106 },    
        { 110, 98, 104, 96, 119, 111, 115 },    
        { 111, 97, 110, 98, 116, 103, 119 },    
        { 112, 107, 102, 101, 120, 115, 114 },  
        { 113, 99, 116, 105, 117, 106, 121 },   
        { 114, 112, 100, 102, 118, 120, 108 },  
        { 115, 110, 107, 104, 120, 119, 112 },  
        { 116, 103, 119, 111, 113, 105, 121 },  
        { 117, INVALID_BASE_CELL, 109, 118, 113, 121, 106 },                                
        { 118, 120, 108, 114, 117, 121, 109 },  
        { 119, 111, 115, 110, 121, 116, 120 },  
        { 120, 115, 114, 112, 121, 119, 118 },  
        { 121, 116, 120, 119, 117, 113, 118 },  
    };

    /** @brief Neighboring base cell rotations in each IJK direction.
     *
     * For each base cell, for each direction, the number of 60 degree
     * CCW rotations to the coordinate system of the neighbor is given.
     * -1 indicates there is no neighbor in that direction.
     */
    private static final int[][] baseCellNeighbor60CCWRots = new int[][] {
        { 0, 5, 0, 0, 1, 5, 1 },   
        { 0, 0, 1, 0, 1, 0, 1 },   
        { 0, 0, 0, 0, 0, 5, 0 },   
        { 0, 5, 0, 0, 2, 5, 1 },   
        { 0, -1, 1, 0, 3, 4, 2 },  
        { 0, 0, 1, 0, 1, 0, 1 },   
        { 0, 0, 0, 3, 5, 5, 0 },   
        { 0, 0, 0, 0, 0, 5, 0 },   
        { 0, 5, 0, 0, 0, 5, 1 },   
        { 0, 0, 1, 3, 0, 0, 1 },   
        { 0, 0, 1, 3, 0, 0, 1 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 5, 0, 0, 3, 5, 1 },   
        { 0, 0, 1, 0, 1, 0, 1 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 5, 0, 0, 4, 5, 1 },   
        { 0, 0, 0, 0, 0, 5, 0 },   
        { 0, 3, 3, 3, 3, 0, 3 },   
        { 0, 0, 0, 3, 5, 5, 0 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 3, 3, 3, 0, 3, 0 },   
        { 0, 0, 0, 3, 5, 5, 0 },   
        { 0, 0, 1, 0, 1, 0, 1 },   
        { 0, 3, 3, 3, 0, 3, 0 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 0, 0, 3, 0, 0, 3 },   
        { 0, 0, 0, 0, 0, 5, 0 },   
        { 0, 3, 0, 0, 0, 3, 3 },   
        { 0, 0, 1, 0, 1, 0, 1 },   
        { 0, 0, 1, 3, 0, 0, 1 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 0, 0, 0, 5, 0 },   
        { 0, 3, 3, 3, 3, 0, 3 },   
        { 0, 0, 1, 3, 0, 0, 1 },   
        { 0, 3, 3, 3, 3, 0, 3 },   
        { 0, 0, 3, 0, 3, 0, 3 },   
        { 0, 0, 0, 3, 0, 0, 3 },   
        { 0, 3, 0, 0, 0, 3, 3 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 3, 0, 0, 3, 3, 0 },   
        { 0, 3, 0, 0, 3, 3, 0 },   
        { 0, 0, 0, 3, 5, 5, 0 },   
        { 0, 0, 0, 3, 5, 5, 0 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 1, 3, 0, 0, 1 },   
        { 0, 0, 3, 0, 0, 3, 3 },   
        { 0, 0, 0, 3, 0, 3, 0 },   
        { 0, 3, 3, 3, 0, 3, 0 },   
        { 0, 3, 3, 3, 0, 3, 0 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 0, 0, 3, 0, 0, 3 },   
        { 0, 3, 0, 0, 0, 3, 3 },   
        { 0, 0, 3, 0, 3, 0, 3 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 3, 0, 3, 0, 3 },   
        { 0, 0, 3, 0, 0, 3, 3 },   
        { 0, 3, 3, 3, 0, 0, 3 },   
        { 0, 0, 0, 3, 0, 3, 0 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 3, 3, 3, 3, 3, 0 },   
        { 0, 3, 3, 3, 3, 3, 0 },   
        { 0, 3, 3, 3, 3, 0, 3 },   
        { 0, 3, 3, 3, 3, 0, 3 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 0, 0, 3, 0, 0, 3 },   
        { 0, 3, 3, 3, 0, 3, 0 },   
        { 0, 3, 0, 0, 0, 3, 3 },   
        { 0, 3, 0, 0, 3, 3, 0 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 3, 0, 0, 3, 3, 0 },   
        { 0, 0, 3, 0, 0, 3, 3 },   
        { 0, 0, 0, 3, 0, 3, 0 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 3, 3, 3, 0, 0, 3 },   
        { 0, 3, 3, 3, 0, 0, 3 },   
        { 0, 0, 0, 3, 0, 0, 3 },   
        { 0, 3, 0, 0, 0, 3, 3 },   
        { 0, 0, 0, 3, 0, 5, 0 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 1, 3, 1, 0, 1 },   
        { 0, 0, 1, 3, 1, 0, 1 },   
        { 0, 0, 3, 0, 3, 0, 3 },   
        { 0, 0, 3, 0, 3, 0, 3 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 0, 3, 0, 0, 3, 3 },   
        { 0, 0, 0, 3, 0, 3, 0 },   
        { 0, 3, 0, 0, 3, 3, 0 },   
        { 0, 3, 3, 3, 3, 3, 0 },   
        { 0, 0, 0, 3, 0, 5, 0 },   
        { 0, 3, 3, 3, 3, 3, 0 },   
        { 0, 0, 0, 0, 0, 0, 1 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 0, 3, 0, 5, 0 },   
        { 0, 5, 0, 0, 5, 5, 0 },   
        { 0, 0, 3, 0, 0, 3, 3 },   
        { 0, 0, 0, 0, 0, 0, 1 },   
        { 0, 0, 0, 3, 0, 3, 0 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 3, 3, 3, 0, 0, 3 },   
        { 0, 5, 0, 0, 5, 5, 0 },   
        { 0, 0, 1, 3, 1, 0, 1 },   
        { 0, 3, 3, 3, 0, 0, 3 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 1, 3, 1, 0, 1 },   
        { 0, 3, 3, 3, 3, 3, 0 },   
        { 0, 0, 0, 0, 0, 0, 1 },   
        { 0, 0, 1, 0, 3, 5, 1 },   
        { 0, -1, 3, 0, 5, 2, 0 },  
        { 0, 5, 0, 0, 5, 5, 0 },   
        { 0, 0, 1, 0, 4, 5, 1 },   
        { 0, 3, 3, 3, 0, 0, 0 },   
        { 0, 0, 0, 3, 0, 5, 0 },   
        { 0, 0, 0, 3, 0, 5, 0 },   
        { 0, 0, 1, 0, 2, 5, 1 },   
        { 0, 0, 0, 0, 0, 0, 1 },   
        { 0, 0, 1, 3, 1, 0, 1 },   
        { 0, 5, 0, 0, 5, 5, 0 },   
        { 0, -1, 1, 0, 3, 4, 2 },  
        { 0, 0, 1, 0, 0, 5, 1 },   
        { 0, 0, 0, 0, 0, 0, 1 },   
        { 0, 5, 0, 0, 5, 5, 0 },   
        { 0, 0, 1, 0, 1, 5, 1 },   
    };

    private static final int E_SUCCESS = 0; 
    private static final int E_PENTAGON = 9;  
    private static final int E_CELL_INVALID = 5; 
    private static final int E_FAILED = 1;  

    /**
     * Directions used for traversing a hexagonal ring counterclockwise around
     * {1, 0, 0}
     *
     * <pre>
     *      _
     *    _/ \\_
     *   / \\5/ \\
     *   \\0/ \\4/
     *   / \\_/ \\
     *   \\1/ \\3/
     *     \\2/
     * </pre>
     */
    static final CoordIJK.Direction[] DIRECTIONS = new CoordIJK.Direction[] {
        CoordIJK.Direction.J_AXES_DIGIT,
        CoordIJK.Direction.JK_AXES_DIGIT,
        CoordIJK.Direction.K_AXES_DIGIT,
        CoordIJK.Direction.IK_AXES_DIGIT,
        CoordIJK.Direction.I_AXES_DIGIT,
        CoordIJK.Direction.IJ_AXES_DIGIT };

    /**
     * New digit when traversing along class II grids.
     *
     * Current digit -> direction -> new digit.
     */
    private static final CoordIJK.Direction[][] NEW_DIGIT_II = new CoordIJK.Direction[][] {
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT },
        {
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT },
        {
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT },
        {
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT },
        {
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT },
        {
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT } };

    /**
     * New traversal direction when traversing along class II grids.
     *
     * Current digit -> direction -> new ap7 move (at coarser level).
     */
    private static final CoordIJK.Direction[][] NEW_ADJUSTMENT_II = new CoordIJK.Direction[][] {
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT } };

    /**
     * New traversal direction when traversing along class III grids.
     *
     * Current digit -> direction -> new ap7 move (at coarser level).
     */
    private static final CoordIJK.Direction[][] NEW_DIGIT_III = new CoordIJK.Direction[][] {
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT },
        {
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT },
        {
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT },
        {
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT },
        {
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT },
        {
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT } };

    /**
     * New traversal direction when traversing along class III grids.
     *
     * Current digit -> direction -> new ap7 move (at coarser level).
     */
    private static final CoordIJK.Direction[][] NEW_ADJUSTMENT_III = new CoordIJK.Direction[][] {
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.J_AXES_DIGIT,
            CoordIJK.Direction.JK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.K_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.IK_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT },
        {
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.I_AXES_DIGIT,
            CoordIJK.Direction.CENTER_DIGIT,
            CoordIJK.Direction.IJ_AXES_DIGIT } };

    private static final CoordIJK.Direction[] NEIGHBORSETCLOCKWISE = new CoordIJK.Direction[] {
        CoordIJK.Direction.CENTER_DIGIT,
        CoordIJK.Direction.JK_AXES_DIGIT,
        CoordIJK.Direction.IJ_AXES_DIGIT,
        CoordIJK.Direction.J_AXES_DIGIT,
        CoordIJK.Direction.IK_AXES_DIGIT,
        CoordIJK.Direction.K_AXES_DIGIT,
        CoordIJK.Direction.I_AXES_DIGIT };

    private static final CoordIJK.Direction[] NEIGHBORSETCOUNTERCLOCKWISE = new CoordIJK.Direction[] {
        CoordIJK.Direction.CENTER_DIGIT,
        CoordIJK.Direction.IK_AXES_DIGIT,
        CoordIJK.Direction.JK_AXES_DIGIT,
        CoordIJK.Direction.K_AXES_DIGIT,
        CoordIJK.Direction.IJ_AXES_DIGIT,
        CoordIJK.Direction.I_AXES_DIGIT,
        CoordIJK.Direction.J_AXES_DIGIT };

    /**
     * Returns whether or not the provided H3Indexes are neighbors.
     * @param origin The origin H3 index.
     * @param destination The destination H3 index.
     * @return true if the indexes are neighbors, false otherwise
     */
    public static boolean areNeighbours(long origin, long destination) {
        if (H3Index.H3_get_mode(origin) != Constants.H3_CELL_MODE) {
            throw new IllegalArgumentException("Invalid cell: " + origin);
        }

        if (H3Index.H3_get_mode(destination) != Constants.H3_CELL_MODE) {
            throw new IllegalArgumentException("Invalid cell: " + destination);
        }

        if (origin == destination) {
            return false;
        }

        final int resolution = H3Index.H3_get_resolution(origin);
        if (resolution != H3Index.H3_get_resolution(destination)) {
            return false;
        }

        if (resolution > 1) {
            long originParent = H3.h3ToParent(origin);
            long destinationParent = H3.h3ToParent(destination);
            if (originParent == destinationParent) {
                int originResDigit = H3Index.H3_get_index_digit(origin, resolution);
                int destinationResDigit = H3Index.H3_get_index_digit(destination, resolution);
                if (originResDigit == CoordIJK.Direction.CENTER_DIGIT.digit()
                    || destinationResDigit == CoordIJK.Direction.CENTER_DIGIT.digit()) {
                    return true;
                }
                if (originResDigit >= CoordIJK.Direction.INVALID_DIGIT.digit()) {
                    throw new IllegalArgumentException("");
                }
                if ((originResDigit == CoordIJK.Direction.K_AXES_DIGIT.digit()
                    || destinationResDigit == CoordIJK.Direction.K_AXES_DIGIT.digit()) && H3.isPentagon(originParent)) {
                    throw new IllegalArgumentException("Undefined error checking for neighbors");
                }
                if (NEIGHBORSETCLOCKWISE[originResDigit].digit() == destinationResDigit
                    || NEIGHBORSETCOUNTERCLOCKWISE[originResDigit].digit() == destinationResDigit) {
                    return true;
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            long neighbor = h3NeighborInDirection(origin, DIRECTIONS[i].digit());
            if (neighbor != -1) {
                if (destination == neighbor) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the hexagon index neighboring the origin, in the direction dir.
     *
     * Implementation note: The only reachable case where this returns -1 is if the
     * origin is a pentagon and the translation is in the k direction. Thus,
     * -1 can only be returned if origin is a pentagon.
     *
     * @param origin Origin index
     * @param dir Direction to move in
     * @return H3Index of the specified neighbor or -1 if there is no more neighbor
     */
    static long h3NeighborInDirection(long origin, int dir) {
        long current = origin;

        int newRotations = 0;
        int oldBaseCell = H3Index.H3_get_base_cell(current);
        if (oldBaseCell < 0 || oldBaseCell >= Constants.NUM_BASE_CELLS) {  
            throw new IllegalArgumentException("Invalid base cell looking for neighbor");
        }
        int oldLeadingDigit = H3Index.h3LeadingNonZeroDigit(current);

        int r = H3Index.H3_get_resolution(current) - 1;
        while (true) {
            if (r == -1) {
                current = H3Index.H3_set_base_cell(current, baseCellNeighbors[oldBaseCell][dir]);
                newRotations = baseCellNeighbor60CCWRots[oldBaseCell][dir];

                if (H3Index.H3_get_base_cell(current) == INVALID_BASE_CELL) {
                    current = H3Index.H3_set_base_cell(current, baseCellNeighbors[oldBaseCell][CoordIJK.Direction.IK_AXES_DIGIT.digit()]);
                    newRotations = baseCellNeighbor60CCWRots[oldBaseCell][CoordIJK.Direction.IK_AXES_DIGIT.digit()];

                    current = H3Index.h3Rotate60ccw(current);
                }

                break;
            } else {
                int oldDigit = H3Index.H3_get_index_digit(current, r + 1);
                int nextDir;
                if (oldDigit == CoordIJK.Direction.INVALID_DIGIT.digit()) {
                    throw new IllegalArgumentException();
                } else if (H3Index.isResolutionClassIII(r + 1)) {
                    current = H3Index.H3_set_index_digit(current, r + 1, NEW_DIGIT_II[oldDigit][dir].digit());
                    nextDir = NEW_ADJUSTMENT_II[oldDigit][dir].digit();
                } else {
                    current = H3Index.H3_set_index_digit(current, r + 1, NEW_DIGIT_III[oldDigit][dir].digit());
                    nextDir = NEW_ADJUSTMENT_III[oldDigit][dir].digit();
                }

                if (nextDir != CoordIJK.Direction.CENTER_DIGIT.digit()) {
                    dir = nextDir;
                    r--;
                } else {
                    break;
                }
            }
        }

        int newBaseCell = H3Index.H3_get_base_cell(current);
        if (BaseCells.isBaseCellPentagon(newBaseCell)) {
            if (H3Index.h3LeadingNonZeroDigit(current) == CoordIJK.Direction.K_AXES_DIGIT.digit()) {
                if (oldBaseCell != newBaseCell) {

                    if (BaseCells.baseCellIsCwOffset(newBaseCell, BaseCells.getBaseFaceIJK(oldBaseCell).face)) {
                        current = H3Index.h3Rotate60cw(current);
                    } else {
                        current = H3Index.h3Rotate60ccw(current);  
                    }
                } else {
                    if (oldLeadingDigit == CoordIJK.Direction.CENTER_DIGIT.digit()) {
                        return -1L;
                    } else if (oldLeadingDigit == CoordIJK.Direction.JK_AXES_DIGIT.digit()) {
                        current = H3Index.h3Rotate60ccw(current);
                    } else if (oldLeadingDigit == CoordIJK.Direction.IK_AXES_DIGIT.digit()) {
                        current = H3Index.h3Rotate60cw(current);
                    } else {
                        throw new IllegalArgumentException("Undefined error looking for neighbor");  
                    }
                }
            }

            for (int i = 0; i < newRotations; i++) {
                current = H3Index.h3RotatePent60ccw(current);
            }
        } else {
            for (int i = 0; i < newRotations; i++) {
                current = H3Index.h3Rotate60ccw(current);
            }
        }
        return current;
    }

}
