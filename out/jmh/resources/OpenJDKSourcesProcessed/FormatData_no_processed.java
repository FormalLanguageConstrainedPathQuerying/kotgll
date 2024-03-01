/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text.resources.ext;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_no extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "januar", 
                    "februar", 
                    "mars", 
                    "april", 
                    "mai", 
                    "juni", 
                    "juli", 
                    "august", 
                    "september", 
                    "oktober", 
                    "november", 
                    "desember", 
                    "" 
                }
            },
            { "standalone.MonthNames",
                new String[] {
                    "januar",
                    "februar",
                    "mars",
                    "april",
                    "mai",
                    "juni",
                    "juli",
                    "august",
                    "september",
                    "oktober",
                    "november",
                    "desember",
                    "",
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "jan", 
                    "feb", 
                    "mar", 
                    "apr", 
                    "mai", 
                    "jun", 
                    "jul", 
                    "aug", 
                    "sep", 
                    "okt", 
                    "nov", 
                    "des", 
                    "" 
                }
            },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "jan",
                    "feb",
                    "mar",
                    "apr",
                    "mai",
                    "jun",
                    "jul",
                    "aug",
                    "sep",
                    "okt",
                    "nov",
                    "des",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "J",
                    "F",
                    "M",
                    "A",
                    "M",
                    "J",
                    "J",
                    "A",
                    "S",
                    "O",
                    "N",
                    "D",
                    "",
                }
            },
            { "standalone.MonthNarrows",
                new String[] {
                    "J",
                    "F",
                    "M",
                    "A",
                    "M",
                    "J",
                    "J",
                    "A",
                    "S",
                    "O",
                    "N",
                    "D",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "s\u00f8ndag", 
                    "mandag", 
                    "tirsdag", 
                    "onsdag", 
                    "torsdag", 
                    "fredag", 
                    "l\u00f8rdag" 
                }
            },
            { "standalone.DayNames",
                new String[] {
                    "s\u00f8ndag",
                    "mandag",
                    "tirsdag",
                    "onsdag",
                    "torsdag",
                    "fredag",
                    "l\u00f8rdag",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "s\u00f8", 
                    "ma", 
                    "ti", 
                    "on", 
                    "to", 
                    "fr", 
                    "l\u00f8" 
                }
            },
            { "standalone.DayAbbreviations",
                new String[] {
                    "s\u00f8.",
                    "ma.",
                    "ti.",
                    "on.",
                    "to.",
                    "fr.",
                    "l\u00f8.",
                }
            },
            { "DayNarrows",
                new String[] {
                    "S",
                    "M",
                    "T",
                    "O",
                    "T",
                    "F",
                    "L",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "S",
                    "M",
                    "T",
                    "O",
                    "T",
                    "F",
                    "L",
                }
            },
            { "NumberElements",
                new String[] {
                    ",", 
                    "\u00a0", 
                    ";", 
                    "%", 
                    "0", 
                    "#", 
                    "-", 
                    "E", 
                    "\u2030", 
                    "\u221e", 
                    "\ufffd" 
                }
            },
            { "TimePatterns",
                new String[] {
                    "'kl 'HH.mm z", 
                    "HH:mm:ss z", 
                    "HH:mm:ss", 
                    "HH:mm", 
                }
            },
            { "DatePatterns",
                new String[] {
                    "d. MMMM yyyy", 
                    "d. MMMM yyyy", 
                    "dd.MMM.yyyy", 
                    "dd.MM.yy", 
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" 
                }
            },
            { "DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ" },
        };
    }
}
