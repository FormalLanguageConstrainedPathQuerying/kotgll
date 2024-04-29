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

import java.util.Arrays;
import sun.java2d.pipe.AATileGenerator;
import jdk.internal.misc.Unsafe;

final class MarlinTileGenerator implements AATileGenerator, MarlinConst {

    private static final boolean DISABLE_BLEND = false;

    private static final int MAX_TILE_ALPHA_SUM = TILE_W * TILE_H * MAX_AA_ALPHA;

    private static final int TH_AA_ALPHA_FILL_EMPTY = ((MAX_AA_ALPHA + 1) / 3); 
    private static final int TH_AA_ALPHA_FILL_FULL  = ((MAX_AA_ALPHA + 1) * 2 / 3); 

    private static final int FILL_TILE_W = TILE_W >> 1; 

    static {
        if (MAX_TILE_ALPHA_SUM <= 0) {
            throw new IllegalStateException("Invalid MAX_TILE_ALPHA_SUM: " + MAX_TILE_ALPHA_SUM);
        }
        if (DO_TRACE) {
            MarlinUtils.logInfo("MAX_AA_ALPHA           : " + MAX_AA_ALPHA);
            MarlinUtils.logInfo("TH_AA_ALPHA_FILL_EMPTY : " + TH_AA_ALPHA_FILL_EMPTY);
            MarlinUtils.logInfo("TH_AA_ALPHA_FILL_FULL  : " + TH_AA_ALPHA_FILL_FULL);
            MarlinUtils.logInfo("FILL_TILE_W            : " + FILL_TILE_W);
        }
    }

    private final Renderer renderer;
    private final MarlinCache cache;
    private int x, y;

    final RendererStats rdrStats;

    MarlinTileGenerator(final RendererStats stats, final Renderer r,
                        final MarlinCache cache)
    {
        this.rdrStats = stats;
        this.renderer = r;
        this.cache = cache;
    }

    MarlinTileGenerator init() {
        this.x = cache.bboxX0;
        this.y = cache.bboxY0;

        return this; 
    }

    /**
     * Disposes this tile generator:
     * clean up before reusing this instance
     */
    @Override
    public void dispose() {
        if (DO_MONITORS) {
            rdrStats.mon_pipe_renderTiles.stop();
        }
        cache.dispose();
        renderer.dispose();
    }

    void getBbox(int[] bbox) {
        bbox[0] = cache.bboxX0;
        bbox[1] = cache.bboxY0;
        bbox[2] = cache.bboxX1;
        bbox[3] = cache.bboxY1;
    }

    /**
     * Gets the width of the tiles that the generator batches output into.
     * @return the width of the standard alpha tile
     */
    @Override
    public int getTileWidth() {
        if (DO_MONITORS) {
            rdrStats.mon_pipe_renderTiles.start();
        }
        return TILE_W;
    }

    /**
     * Gets the height of the tiles that the generator batches output into.
     * @return the height of the standard alpha tile
     */
    @Override
    public int getTileHeight() {
        return TILE_H;
    }

    /**
     * Gets the typical alpha value that will characterize the current
     * tile.
     * The answer may be 0x00 to indicate that the current tile has
     * no coverage in any of its pixels, or it may be 0xff to indicate
     * that the current tile is completely covered by the path, or any
     * other value to indicate non-trivial coverage cases.
     * @return 0x00 for no coverage, 0xff for total coverage, or any other
     *         value for partial coverage of the tile
     */
    @Override
    public int getTypicalAlpha() {
        if (DISABLE_BLEND) {
            return 0x00;
        }
        int al = cache.alphaSumInTile(x);
        final int alpha = (al == 0x00 ? 0x00
                              : (al == MAX_TILE_ALPHA_SUM ? 0xff : 0x80));
        if (DO_STATS) {
            rdrStats.hist_tile_generator_alpha.add(alpha);
        }
        return alpha;
    }

    /**
     * Skips the current tile and moves on to the next tile.
     * Either this method, or the getAlpha() method should be called
     * once per tile, but not both.
     */
    @Override
    public void nextTile() {
        if ((x += TILE_W) >= cache.bboxX1) {
            x = cache.bboxX0;
            y += TILE_H;

            if (y < cache.bboxY1) {
                renderer.endRendering(y);
            }
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    @Override
    public void getAlpha(final byte[] tile, final int offset,
                                            final int rowstride)
    {
        if (cache.useRLE) {
            getAlphaRLE(tile, offset, rowstride);
        } else {
            getAlphaNoRLE(tile, offset, rowstride);
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    private void getAlphaNoRLE(final byte[] tile, final int offset,
                               final int rowstride)
    {
        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.start();
        }

        final MarlinCache _cache = this.cache;
        final long[] rowAAChunkIndex = _cache.rowAAChunkIndex;
        final int[] rowAAx0 = _cache.rowAAx0;
        final int[] rowAAx1 = _cache.rowAAx1;

        final int x0 = this.x;
        final int x1 = FloatMath.min(x0 + TILE_W, _cache.bboxX1);

        final int y0 = 0;
        final int y1 = FloatMath.min(this.y + TILE_H, _cache.bboxY1) - this.y;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("getAlpha = [" + x0 + " ... " + x1
                                + "[ [" + y0 + " ... " + y1 + "[");
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE = 1L;
        final long addr_rowAA = _cache.rowAAChunk.address;
        long addr;

        final int skipRowPixels = (rowstride - (x1 - x0));

        int aax0, aax1, end;
        int idx = offset;

        for (int cy = y0, cx; cy < y1; cy++) {
            cx = x0;

            aax1 = rowAAx1[cy]; 

            if (aax1 > x0) {
                aax0 = rowAAx0[cy]; 

                if (aax0 < x1) {
                    cx = aax0;

                    if (cx <= x0) {
                        cx = x0;
                    } else {
                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }
                    }


                    addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                    for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                        tile[idx++] = _unsafe.getByte(addr); 
                        addr += SIZE;
                    }
                }
            }

            while (cx < x1) {
                tile[idx++] = 0;
                cx++;
            }

            if (DO_TRACE) {
                for (int i = idx - (x1 - x0); i < idx; i++) {
                    System.out.print(hex(tile[i], 2));
                }
                System.out.println();
            }

            idx += skipRowPixels;
        }

        nextTile();

        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.stop();
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    private void getAlphaRLE(final byte[] tile, final int offset,
                             final int rowstride)
    {
        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.start();
        }


        final MarlinCache _cache = this.cache;
        final long[] rowAAChunkIndex = _cache.rowAAChunkIndex;
        final int[] rowAAx0 = _cache.rowAAx0;
        final int[] rowAAx1 = _cache.rowAAx1;
        final int[] rowAAEnc = _cache.rowAAEnc;
        final long[] rowAALen = _cache.rowAALen;
        final long[] rowAAPos = _cache.rowAAPos;

        final int x0 = this.x;
        final int x1 = FloatMath.min(x0 + TILE_W, _cache.bboxX1);
        final int w  = x1 - x0;

        final int y0 = 0;
        final int y1 = FloatMath.min(this.y + TILE_H, _cache.bboxY1) - this.y;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("getAlpha = [" + x0 + " ... " + x1
                                + "[ [" + y0 + " ... " + y1 + "[");
        }

        final int clearTile;
        final byte refVal;
        final int area;

        if ((w >= FILL_TILE_W) && (area = w * y1) > 64) { 
            final int alphaSum = cache.alphaSumInTile(x0);

            if (alphaSum < area * TH_AA_ALPHA_FILL_EMPTY) {
                clearTile = 1;
                refVal = 0;
            } else if (alphaSum > area * TH_AA_ALPHA_FILL_FULL) {
                clearTile = 2;
                refVal = (byte)0xff;
            } else {
                clearTile = 0;
                refVal = 0;
            }
        } else {
            clearTile = 0;
            refVal = 0;
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_BYTE = 1L;
        final long SIZE_INT = 4L;
        final long addr_rowAA = _cache.rowAAChunk.address;
        long addr, addr_row, last_addr, addr_end;

        final int skipRowPixels = (rowstride - w);

        int cx, cy, cx1;
        int rx0, rx1, runLen, end;
        int packed;
        byte val;
        int idx = offset;

        switch (clearTile) {
        case 1: 
            Arrays.fill(tile, offset, offset + (y1 * rowstride), refVal);

            for (cy = y0; cy < y1; cy++) {
                cx = x0;

                if (rowAAEnc[cy] == 0) {

                    final int aax1 = rowAAx1[cy]; 

                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; 

                        if (aax0 < x1) {
                            cx = aax0;

                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                idx += (cx - x0); 
                            }


                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); 
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {

                    if (rowAAx1[cy] > x0) { 

                        cx = rowAAx0[cy]; 
                        if (cx > x1) {
                            cx = x1;
                        }

                        if (cx > x0) {
                            idx += (cx - x0); 
                        }

                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        addr_end = addr_row + rowAALen[cy]; 

                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            last_addr = addr;

                            packed = _unsafe.getInt(addr);

                            cx1 = (packed >> 8);
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; 
                            }
                            runLen = rx1 - rx0;

                            if (runLen > 0) {
                                packed &= 0xFF; 

                                if (packed == 0)
                                {
                                    idx += runLen;
                                    continue;
                                }
                                val = (byte) packed; 
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        if (last_addr != 0L) {
                            rowAAx0[cy]  = cx; 
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                if (cx < x1) {
                    idx += (x1 - cx); 
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        break;

        case 0:
        default:
            for (cy = y0; cy < y1; cy++) {
                cx = x0;

                if (rowAAEnc[cy] == 0) {

                    final int aax1 = rowAAx1[cy]; 

                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; 

                        if (aax0 < x1) {
                            cx = aax0;

                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                for (end = x0; end < cx; end++) {
                                    tile[idx++] = 0;
                                }
                            }


                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); 
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {

                    if (rowAAx1[cy] > x0) { 

                        cx = rowAAx0[cy]; 
                        if (cx > x1) {
                            cx = x1;
                        }

                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }

                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        addr_end = addr_row + rowAALen[cy]; 

                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            last_addr = addr;

                            packed = _unsafe.getInt(addr);

                            cx1 = (packed >> 8);
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; 
                            }
                            runLen = rx1 - rx0;

                            if (runLen > 0) {
                                packed &= 0xFF; 

                                val = (byte) packed; 
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        if (last_addr != 0L) {
                            rowAAx0[cy]  = cx; 
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                while (cx < x1) {
                    tile[idx++] = 0;
                    cx++;
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        break;

        case 2: 
            Arrays.fill(tile, offset, offset + (y1 * rowstride), refVal);

            for (cy = y0; cy < y1; cy++) {
                cx = x0;

                if (rowAAEnc[cy] == 0) {

                    final int aax1 = rowAAx1[cy]; 

                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; 

                        if (aax0 < x1) {
                            cx = aax0;

                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                for (end = x0; end < cx; end++) {
                                    tile[idx++] = 0;
                                }
                            }


                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); 
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {

                    if (rowAAx1[cy] > x0) { 

                        cx = rowAAx0[cy]; 
                        if (cx > x1) {
                            cx = x1;
                        }

                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }

                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        addr_end = addr_row + rowAALen[cy]; 

                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            last_addr = addr;

                            packed = _unsafe.getInt(addr);

                            cx1 = (packed >> 8);
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; 
                            }
                            runLen = rx1 - rx0;

                            if (runLen > 0) {
                                packed &= 0xFF; 

                                if (packed == 0xFF)
                                {
                                    idx += runLen;
                                    continue;
                                }
                                val = (byte) packed; 
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        if (last_addr != 0L) {
                            rowAAx0[cy]  = cx; 
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                while (cx < x1) {
                    tile[idx++] = 0;
                    cx++;
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        }

        nextTile();

        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.stop();
        }
    }

    static String hex(int v, int d) {
        StringBuilder s = new StringBuilder(Integer.toHexString(v));
        while (s.length() < d) {
            s.insert(0, "0");
        }
        return s.substring(0, d);
    }
}
