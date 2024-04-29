/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 ******************************************************************************
 *
 *   Copyright (C) 2009-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 ******************************************************************************
 */

package jdk.internal.icu.impl;

import jdk.internal.icu.text.UnicodeSet.SpanCondition;
import jdk.internal.icu.util.OutputInt;

/**
 * Helper class for frozen UnicodeSets, implements contains() and span() optimized for BMP code points.
 *
 * Latin-1: Look up bytes.
 * 2-byte characters: Bits organized vertically.
 * 3-byte characters: Use zero/one/mixed data per 64-block in U+0000..U+FFFF, with mixed for illegal ranges.
 * Supplementary characters: Call contains() on the parent set.
 */
public final class BMPSet {

    /**
     * One boolean ('true' or 'false') per Latin-1 character.
     */
    private boolean[] latin1Contains;

    /**
     * One bit per code point from U+0000..U+07FF. The bits are organized vertically; consecutive code points
     * correspond to the same bit positions in consecutive table words. With code point parts lead=c{10..6}
     * trail=c{5..0} it is set.contains(c)==(table7FF[trail] bit lead)
     *
     * Bits for 0..7F (non-shortest forms) are set to the result of contains(FFFD) for faster validity checking at
     * runtime.
     */
    private int[] table7FF;

    /**
     * One bit per 64 BMP code points. The bits are organized vertically; consecutive 64-code point blocks
     * correspond to the same bit position in consecutive table words. With code point parts lead=c{15..12}
     * t1=c{11..6} test bits (lead+16) and lead in bmpBlockBits[t1]. If the upper bit is 0, then the lower bit
     * indicates if contains(c) for all code points in the 64-block. If the upper bit is 1, then the block is mixed
     * and set.contains(c) must be called.
     *
     * Bits for 0..7FF (non-shortest forms) and D800..DFFF are set to the result of contains(FFFD) for faster
     * validity checking at runtime.
     */
    private int[] bmpBlockBits;

    /**
     * Inversion list indexes for restricted binary searches in findCodePoint(), from findCodePoint(U+0800, U+1000,
     * U+2000, .., U+F000, U+10000). U+0800 is the first 3-byte-UTF-8 code point. Code points below U+0800 are
     * always looked up in the bit tables. The last pair of indexes is for finding supplementary code points.
     */
    private int[] list4kStarts;

    /**
     * The inversion list of the parent set, for the slower contains() implementation for mixed BMP blocks and for
     * supplementary code points. The list is terminated with list[listLength-1]=0x110000.
     */
    private final int[] list;
    private final int listLength; 

    public BMPSet(final int[] parentList, int parentListLength) {
        list = parentList;
        listLength = parentListLength;
        latin1Contains = new boolean[0x100];
        table7FF = new int[64];
        bmpBlockBits = new int[64];
        list4kStarts = new int[18];

        /*
         * Set the list indexes for binary searches for U+0800, U+1000, U+2000, .., U+F000, U+10000. U+0800 is the
         * first 3-byte-UTF-8 code point. Lower code points are looked up in the bit tables. The last pair of
         * indexes is for finding supplementary code points.
         */
        list4kStarts[0] = findCodePoint(0x800, 0, listLength - 1);
        int i;
        for (i = 1; i <= 0x10; ++i) {
            list4kStarts[i] = findCodePoint(i << 12, list4kStarts[i - 1], listLength - 1);
        }
        list4kStarts[0x11] = listLength - 1;

        initBits();
    }

    public boolean contains(int c) {
        if (c <= 0xff) {
            return (latin1Contains[c]);
        } else if (c <= 0x7ff) {
            return ((table7FF[c & 0x3f] & (1 << (c >> 6))) != 0);
        } else if (c < 0xd800 || (c >= 0xe000 && c <= 0xffff)) {
            int lead = c >> 12;
            int twoBits = (bmpBlockBits[(c >> 6) & 0x3f] >> lead) & 0x10001;
            if (twoBits <= 1) {
                return (0 != twoBits);
            } else {
                return containsSlow(c, list4kStarts[lead], list4kStarts[lead + 1]);
            }
        } else if (c <= 0x10ffff) {
            return containsSlow(c, list4kStarts[0xd], list4kStarts[0x11]);
        } else {
            return false;
        }
    }

    /**
     * Span the initial substring for which each character c has spanCondition==contains(c). It must be
     * spanCondition==0 or 1.
     *
     * @param start The start index
     * @param outCount If not null: Receives the number of code points in the span.
     * @return the limit (exclusive end) of the span
     *
     * NOTE: to reduce the overhead of function call to contains(c), it is manually inlined here. Check for
     * sufficient length for trail unit for each surrogate pair. Handle single surrogates as surrogate code points
     * as usual in ICU.
     */
    public final int span(CharSequence s, int start, SpanCondition spanCondition,
            OutputInt outCount) {
        char c, c2;
        int i = start;
        int limit = s.length();
        int numSupplementary = 0;
        if (SpanCondition.NOT_CONTAINED != spanCondition) {
            while (i < limit) {
                c = s.charAt(i);
                if (c <= 0xff) {
                    if (!latin1Contains[c]) {
                        break;
                    }
                } else if (c <= 0x7ff) {
                    if ((table7FF[c & 0x3f] & (1 << (c >> 6))) == 0) {
                        break;
                    }
                } else if (c < 0xd800 ||
                           c >= 0xdc00 || (i + 1) == limit || (c2 = s.charAt(i + 1)) < 0xdc00 || c2 >= 0xe000) {
                    int lead = c >> 12;
                    int twoBits = (bmpBlockBits[(c >> 6) & 0x3f] >> lead) & 0x10001;
                    if (twoBits <= 1) {
                        if (twoBits == 0) {
                            break;
                        }
                    } else {
                        if (!containsSlow(c, list4kStarts[lead], list4kStarts[lead + 1])) {
                            break;
                        }
                    }
                } else {
                    int supplementary = UCharacterProperty.getRawSupplementary(c, c2);
                    if (!containsSlow(supplementary, list4kStarts[0x10], list4kStarts[0x11])) {
                        break;
                    }
                    ++numSupplementary;
                    ++i;
                }
                ++i;
            }
        } else {
            while (i < limit) {
                c = s.charAt(i);
                if (c <= 0xff) {
                    if (latin1Contains[c]) {
                        break;
                    }
                } else if (c <= 0x7ff) {
                    if ((table7FF[c & 0x3f] & (1 << (c >> 6))) != 0) {
                        break;
                    }
                } else if (c < 0xd800 ||
                           c >= 0xdc00 || (i + 1) == limit || (c2 = s.charAt(i + 1)) < 0xdc00 || c2 >= 0xe000) {
                    int lead = c >> 12;
                    int twoBits = (bmpBlockBits[(c >> 6) & 0x3f] >> lead) & 0x10001;
                    if (twoBits <= 1) {
                        if (twoBits != 0) {
                            break;
                        }
                    } else {
                        if (containsSlow(c, list4kStarts[lead], list4kStarts[lead + 1])) {
                            break;
                        }
                    }
                } else {
                    int supplementary = UCharacterProperty.getRawSupplementary(c, c2);
                    if (containsSlow(supplementary, list4kStarts[0x10], list4kStarts[0x11])) {
                        break;
                    }
                    ++numSupplementary;
                    ++i;
                }
                ++i;
            }
        }
        if (outCount != null) {
            int spanLength = i - start;
            outCount.value = spanLength - numSupplementary;  
        }
        return i;
    }

    /**
     * Symmetrical with span().
     * Span the trailing substring for which each character c has spanCondition==contains(c). It must be s.length >=
     * limit and spanCondition==0 or 1.
     *
     * @return The string index which starts the span (i.e. inclusive).
     */
    public final int spanBack(CharSequence s, int limit, SpanCondition spanCondition) {
        char c, c2;

        if (SpanCondition.NOT_CONTAINED != spanCondition) {
            for (;;) {
                c = s.charAt(--limit);
                if (c <= 0xff) {
                    if (!latin1Contains[c]) {
                        break;
                    }
                } else if (c <= 0x7ff) {
                    if ((table7FF[c & 0x3f] & (1 << (c >> 6))) == 0) {
                        break;
                    }
                } else if (c < 0xd800 ||
                           c < 0xdc00 || 0 == limit || (c2 = s.charAt(limit - 1)) < 0xd800 || c2 >= 0xdc00) {
                    int lead = c >> 12;
                    int twoBits = (bmpBlockBits[(c >> 6) & 0x3f] >> lead) & 0x10001;
                    if (twoBits <= 1) {
                        if (twoBits == 0) {
                            break;
                        }
                    } else {
                        if (!containsSlow(c, list4kStarts[lead], list4kStarts[lead + 1])) {
                            break;
                        }
                    }
                } else {
                    int supplementary = UCharacterProperty.getRawSupplementary(c2, c);
                    if (!containsSlow(supplementary, list4kStarts[0x10], list4kStarts[0x11])) {
                        break;
                    }
                    --limit;
                }
                if (0 == limit) {
                    return 0;
                }
            }
        } else {
            for (;;) {
                c = s.charAt(--limit);
                if (c <= 0xff) {
                    if (latin1Contains[c]) {
                        break;
                    }
                } else if (c <= 0x7ff) {
                    if ((table7FF[c & 0x3f] & (1 << (c >> 6))) != 0) {
                        break;
                    }
                } else if (c < 0xd800 ||
                           c < 0xdc00 || 0 == limit || (c2 = s.charAt(limit - 1)) < 0xd800 || c2 >= 0xdc00) {
                    int lead = c >> 12;
                    int twoBits = (bmpBlockBits[(c >> 6) & 0x3f] >> lead) & 0x10001;
                    if (twoBits <= 1) {
                        if (twoBits != 0) {
                            break;
                        }
                    } else {
                        if (containsSlow(c, list4kStarts[lead], list4kStarts[lead + 1])) {
                            break;
                        }
                    }
                } else {
                    int supplementary = UCharacterProperty.getRawSupplementary(c2, c);
                    if (containsSlow(supplementary, list4kStarts[0x10], list4kStarts[0x11])) {
                        break;
                    }
                    --limit;
                }
                if (0 == limit) {
                    return 0;
                }
            }
        }
        return limit + 1;
    }

    /**
     * Set bits in a bit rectangle in "vertical" bit organization. start<limit<=0x800
     */
    private static void set32x64Bits(int[] table, int start, int limit) {
        assert (64 == table.length);
        int lead = start >> 6;  
        int trail = start & 0x3f;  

        int bits = 1 << lead;
        if ((start + 1) == limit) { 
            table[trail] |= bits;
            return;
        }

        int limitLead = limit >> 6;
        int limitTrail = limit & 0x3f;

        if (lead == limitLead) {
            while (trail < limitTrail) {
                table[trail++] |= bits;
            }
        } else {
            if (trail > 0) {
                do {
                    table[trail++] |= bits;
                } while (trail < 64);
                ++lead;
            }
            if (lead < limitLead) {
                bits = ~((1 << lead) - 1);
                if (limitLead < 0x20) {
                    bits &= (1 << limitLead) - 1;
                }
                for (trail = 0; trail < 64; ++trail) {
                    table[trail] |= bits;
                }
            }
            bits = 1 << limitLead;
            for (trail = 0; trail < limitTrail; ++trail) {
                table[trail] |= bits;
            }
        }
    }

    private void initBits() {
        int start, limit;
        int listIndex = 0;

        do {
            start = list[listIndex++];
            if (listIndex < listLength) {
                limit = list[listIndex++];
            } else {
                limit = 0x110000;
            }
            if (start >= 0x100) {
                break;
            }
            do {
                latin1Contains[start++] = true;
            } while (start < limit && start < 0x100);
        } while (limit <= 0x100);

        while (start < 0x800) {
            set32x64Bits(table7FF, start, limit <= 0x800 ? limit : 0x800);
            if (limit > 0x800) {
                start = 0x800;
                break;
            }

            start = list[listIndex++];
            if (listIndex < listLength) {
                limit = list[listIndex++];
            } else {
                limit = 0x110000;
            }
        }

        int minStart = 0x800;
        while (start < 0x10000) {
            if (limit > 0x10000) {
                limit = 0x10000;
            }

            if (start < minStart) {
                start = minStart;
            }
            if (start < limit) { 
                if (0 != (start & 0x3f)) {
                    start >>= 6;
                    bmpBlockBits[start & 0x3f] |= 0x10001 << (start >> 6);
                    start = (start + 1) << 6; 
                    minStart = start; 
                }
                if (start < limit) {
                    if (start < (limit & ~0x3f)) {
                        set32x64Bits(bmpBlockBits, start >> 6, limit >> 6);
                    }

                    if (0 != (limit & 0x3f)) {
                        limit >>= 6;
                        bmpBlockBits[limit & 0x3f] |= 0x10001 << (limit >> 6);
                      limit = (limit + 1) << 6; 
                        minStart = limit; 
                    }
                }
            }

            if (limit == 0x10000) {
                break;
          }

            start = list[listIndex++];
            if (listIndex < listLength) {
                limit = list[listIndex++];
            } else {
                limit = 0x110000;
            }
        }
    }

    /**
     * Same as UnicodeSet.findCodePoint(int c) except that the binary search is restricted for finding code
     * points in a certain range.
     *
     * For restricting the search for finding in the range start..end, pass in lo=findCodePoint(start) and
     * hi=findCodePoint(end) with 0<=lo<=hi<len. findCodePoint(c) defaults to lo=0 and hi=len-1.
     *
     * @param c
     *            a character in a subrange of MIN_VALUE..MAX_VALUE
     * @param lo
     *            The lowest index to be returned.
     * @param hi
     *            The highest index to be returned.
     * @return the smallest integer i in the range lo..hi, inclusive, such that c < list[i]
     */
    private int findCodePoint(int c, int lo, int hi) {
        /* Examples:
                                           findCodePoint(c)
           set              list[]         c=0 1 3 4 7 8
           ===              ==============   ===========
           []               [110000]         0 0 0 0 0 0
           [\u0000-\u0003]  [0, 4, 110000]   1 1 1 2 2 2
           [\u0004-\u0007]  [4, 8, 110000]   0 0 0 1 1 2
           [:Any:]          [0, 110000]      1 1 1 1 1 1
         */

        if (c < list[lo])
            return lo;
        if (lo >= hi || c >= list[hi - 1])
            return hi;
        for (;;) {
            int i = (lo + hi) >>> 1;
            if (i == lo) {
                break; 
            } else if (c < list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
        return hi;
    }

    private final boolean containsSlow(int c, int lo, int hi) {
        return (0 != (findCodePoint(c, lo, hi) & 1));
    }
}

