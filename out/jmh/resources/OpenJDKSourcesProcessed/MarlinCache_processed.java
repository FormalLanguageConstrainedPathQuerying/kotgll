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

import jdk.internal.misc.Unsafe;

/**
 * An object used to cache pre-rendered complex paths.
 *
 * @see Renderer
 */
public final class MarlinCache implements MarlinConst {

    static final boolean FORCE_RLE = MarlinProperties.isForceRLE();
    static final boolean FORCE_NO_RLE = MarlinProperties.isForceNoRLE();
    static final int RLE_MIN_WIDTH
        = Math.max(BLOCK_SIZE, MarlinProperties.getRLEMinWidth());
    static final int RLE_MAX_WIDTH = 1 << (24 - 1);

    static final long INITIAL_CHUNK_ARRAY = TILE_H * INITIAL_PIXEL_WIDTH >> 2; 

    static final byte[] ALPHA_MAP;

    static final OffHeapArray ALPHA_MAP_UNSAFE;

    static {
        final byte[] _ALPHA_MAP = buildAlphaMap(MAX_AA_ALPHA);

        ALPHA_MAP_UNSAFE = new OffHeapArray(_ALPHA_MAP, _ALPHA_MAP.length); 
        ALPHA_MAP =_ALPHA_MAP;

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long addr = ALPHA_MAP_UNSAFE.address;

        for (int i = 0; i < _ALPHA_MAP.length; i++) {
            _unsafe.putByte(addr + i, _ALPHA_MAP[i]);
        }
    }

    int bboxX0, bboxY0, bboxX1, bboxY1;

    final long[] rowAAChunkIndex = new long[TILE_H];
    final int[] rowAAx0 = new int[TILE_H];
    final int[] rowAAx1 = new int[TILE_H];
    final int[] rowAAEnc = new int[TILE_H];
    final long[] rowAALen = new long[TILE_H];
    final long[] rowAAPos = new long[TILE_H];

    final OffHeapArray rowAAChunk;

    long rowAAChunkPos;

    int[] touchedTile;

    final RendererStats rdrStats;

    private final ArrayCacheIntClean.Reference touchedTile_ref;

    int tileMin, tileMax;

    boolean useRLE = false;

    MarlinCache(final RendererContext rdrCtx) {
        this.rdrStats = rdrCtx.stats();

        rowAAChunk = rdrCtx.newOffHeapArray(INITIAL_CHUNK_ARRAY); 

        touchedTile_ref = rdrCtx.newCleanIntArrayRef(INITIAL_ARRAY); 
        touchedTile     = touchedTile_ref.initial;

        tileMin = Integer.MAX_VALUE;
        tileMax = Integer.MIN_VALUE;
    }

    void init(int minx, int miny, int maxx, int maxy)
    {
        bboxX0 = minx;
        bboxY0 = miny;
        bboxX1 = maxx;
        bboxY1 = maxy;

        final int width = (maxx - minx);

        if (FORCE_NO_RLE) {
            useRLE = false;
        } else if (FORCE_RLE) {
            useRLE = true;
        } else {

            useRLE = (width > RLE_MIN_WIDTH && width < RLE_MAX_WIDTH);
        }

        final int nxTiles = (width + TILE_W) >> TILE_W_LG;

        if (nxTiles > INITIAL_ARRAY) {
            if (DO_STATS) {
                rdrStats.stat_array_marlincache_touchedTile.add(nxTiles);
            }
            touchedTile = touchedTile_ref.getArray(nxTiles);
        }
    }

    /**
     * Disposes this cache:
     * clean up before reusing this instance
     */
    void dispose() {
        resetTileLine(0);

        if (DO_STATS) {
            rdrStats.totalOffHeap += rowAAChunk.length;
        }

        if (touchedTile_ref.doSetRef(touchedTile)) {
            touchedTile = touchedTile_ref.putArrayClean(touchedTile); 
        }

        if (rowAAChunk.length != INITIAL_CHUNK_ARRAY) {
            rowAAChunk.resize(INITIAL_CHUNK_ARRAY);
        }
        if (DO_CLEAN_DIRTY) {
            rowAAChunk.fill(BYTE_0);
        }
    }

    void resetTileLine(final int pminY) {
        bboxY0 = pminY;

        if (DO_STATS) {
            rdrStats.stat_cache_rowAAChunk.add(rowAAChunkPos);
        }
        rowAAChunkPos = 0L;

        if (tileMin != Integer.MAX_VALUE) {
            if (DO_STATS) {
                rdrStats.stat_cache_tiles.add(tileMax - tileMin);
            }
            if (tileMax == 1) {
                touchedTile[0] = 0;
            } else {
                ArrayCacheInt.fill(touchedTile, tileMin, tileMax, 0);
            }
            tileMin = Integer.MAX_VALUE;
            tileMax = Integer.MIN_VALUE;
        }

        if (DO_CLEAN_DIRTY) {
            rowAAChunk.fill(BYTE_0);
        }
    }

    void clearAARow(final int y) {
        final int row = y - bboxY0;

        rowAAx0[row]  = 0; 
        rowAAx1[row]  = 0; 
        rowAAEnc[row] = 0; 

    }

    /**
     * Copy the given alpha data into the rowAA cache
     * @param alphaRow alpha data to copy from
     * @param y y pixel coordinate
     * @param px0 first pixel inclusive x0
     * @param px1 last pixel exclusive x1
     */
    void copyAARowNoRLE(final int[] alphaRow, final int y,
                   final int px0, final int px1)
    {
        final int px_bbox1 = FloatMath.min(px1, bboxX1);

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("row = [" + px0 + " ... " + px_bbox1
                                + " (" + px1 + ") [ for y=" + y);
        }

        final int row = y - bboxY0;

        rowAAx0[row]  = px0;      
        rowAAx1[row]  = px_bbox1; 
        rowAAEnc[row] = 0; 

        final long pos = rowAAChunkPos;
        rowAAChunkIndex[row] = pos;

        final long needSize = pos + ((px_bbox1 - px0 + 3) & -4);

        rowAAChunkPos = needSize;

        final OffHeapArray _rowAAChunk = rowAAChunk;
        if (_rowAAChunk.length < needSize) {
            expandRowAAChunk(needSize);
        }
        if (DO_STATS) {
            rdrStats.stat_cache_rowAA.add(px_bbox1 - px0);
        }

        final int[] _touchedTile = touchedTile;
        final int _TILE_SIZE_LG = TILE_W_LG;

        final int from = px0      - bboxX0; 
        final int to   = px_bbox1 - bboxX0; 

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_BYTE = 1L;
        final long addr_alpha = ALPHA_MAP_UNSAFE.address;
        long addr_off = _rowAAChunk.address + pos;

        for (int x = from, val = 0; x < to; x++) {
            val += alphaRow[x]; 

            if (DO_AA_RANGE_CHECK) {
                if (val < 0) {
                    MarlinUtils.logInfo("Invalid coverage = " + val);
                    val = 0;
                }
                if (val > MAX_AA_ALPHA) {
                    MarlinUtils.logInfo("Invalid coverage = " + val);
                    val = MAX_AA_ALPHA;
                }
            }

            if (val == 0) {
                _unsafe.putByte(addr_off, (byte)0); 
            } else {
                _unsafe.putByte(addr_off, _unsafe.getByte(addr_alpha + val)); 

                _touchedTile[x >> _TILE_SIZE_LG] += val;
            }
            addr_off += SIZE_BYTE;
        }

        int tx = from >> _TILE_SIZE_LG; 
        if (tx < tileMin) {
            tileMin = tx;
        }

        tx = ((to - 1) >> _TILE_SIZE_LG) + 1; 
        if (tx > tileMax) {
            tileMax = tx;
        }

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("clear = [" + from + " ... " + to + "[");
        }

        ArrayCacheInt.fill(alphaRow, from, px1 + 1 - bboxX0, 0);
    }

    void copyAARowRLE_WithBlockFlags(final int[] blkFlags, final int[] alphaRow,
                      final int y, final int px0, final int px1)
    {
        final int _bboxX0 = bboxX0;

        final int row  =   y -  bboxY0;
        final int from = px0 - _bboxX0; 

        final int px_bbox1 = FloatMath.min(px1, bboxX1);
        final int to       = px_bbox1 - _bboxX0; 

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("row = [" + px0 + " ... " + px_bbox1
                                + " (" + px1 + ") [ for y=" + y);
        }

        final long initialPos = startRLERow(row, px0, px_bbox1);

        final long needSize = initialPos + ((to - from) << 2);

        OffHeapArray _rowAAChunk = rowAAChunk;
        if (_rowAAChunk.length < needSize) {
            expandRowAAChunk(needSize);
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_INT = 4L;
        final long addr_alpha = ALPHA_MAP_UNSAFE.address;
        long addr_off = _rowAAChunk.address + initialPos;

        final int[] _touchedTile = touchedTile;
        final int _TILE_SIZE_LG = TILE_W_LG;
        final int _BLK_SIZE_LG  = BLOCK_SIZE_LG;

        final int blkW = (from >> _BLK_SIZE_LG);
        final int blkE = (to   >> _BLK_SIZE_LG) + 1;
        blkFlags[blkE] = 0;

        int val = 0;
        int cx0 = from;
        int runLen;

        final int _MAX_VALUE = Integer.MAX_VALUE;
        int last_t0 = _MAX_VALUE;

        int skip = 0;

        for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
            if (blkFlags[t] != 0) {
                blkFlags[t] = 0;

                if (last_t0 == _MAX_VALUE) {
                    last_t0 = t;
                }
                continue;
            }
            if (last_t0 != _MAX_VALUE) {
                blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                last_t0 = _MAX_VALUE;

                blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, to);

                for (cx = blk_x0; cx < blk_x1; cx++) {
                    if ((delta = alphaRow[cx]) != 0) {
                        alphaRow[cx] = 0;

                        if (cx != cx0) {
                            runLen = cx - cx0;


                            if (DO_CHECK_UNSAFE) {
                                if ((addr_off & 3) != 0) {
                                    MarlinUtils.logInfo("Misaligned Unsafe address: " + addr_off);
                                }
                            }

                            if (val == 0) {
                                _unsafe.putInt(addr_off,
                                    ((_bboxX0 + cx) << 8)
                                );
                            } else {
                                _unsafe.putInt(addr_off,
                                    ((_bboxX0 + cx) << 8)
                                    | (((int) _unsafe.getByte(addr_alpha + val)) & 0xFF) 
                                );

                                if (runLen == 1) {
                                    _touchedTile[cx0 >> _TILE_SIZE_LG] += val;
                                } else {
                                    touchTile(cx0, val, cx, runLen, _touchedTile);
                                }
                            }
                            addr_off += SIZE_INT;

                            if (DO_STATS) {
                                rdrStats.hist_tile_generator_encoding_runLen
                                    .add(runLen);
                            }
                            cx0 = cx;
                        }

                        val += delta;

                        if (DO_AA_RANGE_CHECK) {
                            if (val < 0) {
                                MarlinUtils.logInfo("Invalid coverage = " + val);
                                val = 0;
                            }
                            if (val > MAX_AA_ALPHA) {
                                MarlinUtils.logInfo("Invalid coverage = " + val);
                                val = MAX_AA_ALPHA;
                            }
                        }
                    }
                }
            } else if (DO_STATS) {
                skip++;
            }
        }

        runLen = to - cx0;


        if (DO_CHECK_UNSAFE) {
            if ((addr_off & 3) != 0) {
                MarlinUtils.logInfo("Misaligned Unsafe address: " + addr_off);
            }
        }

        if (val == 0) {
            _unsafe.putInt(addr_off,
                ((_bboxX0 + to) << 8)
            );
        } else {
            _unsafe.putInt(addr_off,
                ((_bboxX0 + to) << 8)
                | (((int) _unsafe.getByte(addr_alpha + val)) & 0xFF) 
            );

            if (runLen == 1) {
                _touchedTile[cx0 >> _TILE_SIZE_LG] += val;
            } else {
                touchTile(cx0, val, to, runLen, _touchedTile);
            }
        }
        addr_off += SIZE_INT;

        if (DO_STATS) {
            rdrStats.hist_tile_generator_encoding_runLen.add(runLen);
        }

        long len = (addr_off - _rowAAChunk.address);

        rowAALen[row] = (len - initialPos);

        rowAAChunkPos = len;

        if (DO_STATS) {
            rdrStats.stat_cache_rowAA.add(rowAALen[row]);
            rdrStats.hist_tile_generator_encoding_ratio.add(
                (100 * skip) / (blkE - blkW)
            );
        }

        int tx = from >> _TILE_SIZE_LG; 
        if (tx < tileMin) {
            tileMin = tx;
        }

        tx = ((to - 1) >> _TILE_SIZE_LG) + 1; 
        if (tx > tileMax) {
            tileMax = tx;
        }

        alphaRow[to] = 0;
        if (DO_CHECKS) {
            ArrayCacheInt.check(blkFlags, blkW, blkE, 0);
            ArrayCacheInt.check(alphaRow, from, px1 + 1 - bboxX0, 0);
        }
    }

    long startRLERow(final int row, final int x0, final int x1) {
        rowAAx0[row]  = x0; 
        rowAAx1[row]  = x1; 
        rowAAEnc[row] = 1; 
        rowAAPos[row] = 0L; 

        return (rowAAChunkIndex[row] = rowAAChunkPos);
    }

    private void expandRowAAChunk(final long needSize) {
        if (DO_STATS) {
            rdrStats.stat_array_marlincache_rowAAChunk.add(needSize);
        }

        final long newSize = ArrayCacheConst.getNewLargeSize(rowAAChunk.length,
                                                             needSize);

        rowAAChunk.resize(newSize);
    }

    private void touchTile(final int x0, final int val, final int x1,
                           final int runLen,
                           final int[] _touchedTile)
    {
        final int _TILE_SIZE_LG = TILE_W_LG;

        int tx = (x0 >> _TILE_SIZE_LG);

        if (tx == (x1 >> _TILE_SIZE_LG)) {
            _touchedTile[tx] += val * runLen;
            return;
        }

        final int tx1 = (x1 - 1) >> _TILE_SIZE_LG;

        if (tx <= tx1) {
            final int nextTileXCoord = (tx + 1) << _TILE_SIZE_LG;
            _touchedTile[tx++] += val * (nextTileXCoord - x0);
        }
        if (tx < tx1) {
            final int tileVal = (val << _TILE_SIZE_LG);
            for (; tx < tx1; tx++) {
                _touchedTile[tx] += tileVal;
            }
        }
        if (tx == tx1) {
            final int txXCoord       =  tx      << _TILE_SIZE_LG;
            final int nextTileXCoord = (tx + 1) << _TILE_SIZE_LG;

            final int lastXCoord = (nextTileXCoord <= x1) ? nextTileXCoord : x1;
            _touchedTile[tx] += val * (lastXCoord - txXCoord);
        }
    }

    int alphaSumInTile(final int x) {
        return touchedTile[(x - bboxX0) >> TILE_W_LG];
    }

    @Override
    public String toString() {
        return "bbox = ["
            + bboxX0 + ", " + bboxY0 + " => "
            + bboxX1 + ", " + bboxY1 + "]\n";
    }

    private static byte[] buildAlphaMap(final int maxalpha) {
        final byte[] alMap = new byte[maxalpha << 1];
        final int halfmaxalpha = maxalpha >> 2;
        for (int i = 0; i <= maxalpha; i++) {
            alMap[i] = (byte) ((i * 255 + halfmaxalpha) / maxalpha);
        }
        return alMap;
    }
}
