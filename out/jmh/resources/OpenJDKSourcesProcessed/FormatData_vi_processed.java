/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2003 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 *
 * This locale data is based on the ICU's Vietnamese locale data (rev. 1.38)
 * found at:
 *
 * http:
 */

/*
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright (C) 1991-2012 Unicode, Inc. All rights reserved. Distributed under
 * the Terms of Use in http:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of the Unicode data files and any associated documentation (the "Data
 * Files") or Unicode software and any associated documentation (the
 * "Software") to deal in the Data Files or Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, and/or sell copies of the Data Files or Software, and
 * to permit persons to whom the Data Files or Software are furnished to do so,
 * provided that (a) the above copyright notice(s) and this permission notice
 * appear with all copies of the Data Files or Software, (b) both the above
 * copyright notice(s) and this permission notice appear in associated
 * documentation, and (c) there is clear notice in each modified Data File or
 * in the Software as well as in the documentation associated with the Data
 * File(s) or Software that the data or software has been modified.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
 * INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THE DATA FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall not
 * be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written authorization
 * of the copyright holder.
 */

package sun.text.resources.ext;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_vi extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "th\u00e1ng m\u1ed9t", 
                    "th\u00e1ng hai", 
                    "th\u00e1ng ba", 
                    "th\u00e1ng t\u01b0", 
                    "th\u00e1ng n\u0103m", 
                    "th\u00e1ng s\u00e1u", 
                    "th\u00e1ng b\u1ea3y", 
                    "th\u00e1ng t\u00e1m", 
                    "th\u00e1ng ch\u00edn", 
                    "th\u00e1ng m\u01b0\u1eddi", 
                    "th\u00e1ng m\u01b0\u1eddi m\u1ed9t", 
                    "th\u00e1ng m\u01b0\u1eddi hai", 
                    "" 
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "thg 1", 
                    "thg 2", 
                    "thg 3", 
                    "thg 4", 
                    "thg 5", 
                    "thg 6", 
                    "thg 7", 
                    "thg 8", 
                    "thg 9", 
                    "thg 10", 
                    "thg 11", 
                    "thg 12", 
                    "" 
                }
            },
            { "MonthNarrows",
                new String[] {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "10",
                    "11",
                    "12",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "Ch\u1ee7 nh\u1eadt", 
                    "Th\u1ee9 hai", 
                    "Th\u1ee9 ba",  
                    "Th\u1ee9 t\u01b0", 
                    "Th\u1ee9 n\u0103m", 
                    "Th\u1ee9 s\u00e1u", 
                    "Th\u1ee9 b\u1ea3y" 
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "CN", 
                    "Th 2", 
                    "Th 3", 
                    "Th 4", 
                    "Th 5", 
                    "Th 6", 
                    "Th 7" 
                }
            },
            { "DayNarrows",
                new String[] {
                    "CN",
                    "T2",
                    "T3",
                    "T4",
                    "T5",
                    "T6",
                    "T7",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "CN",
                    "T2",
                    "T3",
                    "T4",
                    "T5",
                    "T6",
                    "T7",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "SA", 
                    "CH" 
                }
            },
            { "Eras",
                new String[] { 
                    "tr. CN",
                    "sau CN"
                }
            },
            { "NumberElements",
                new String[] {
                    ",", 
                    ".", 
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
                    "HH:mm:ss z", 
                    "HH:mm:ss z", 
                    "HH:mm:ss", 
                    "HH:mm", 
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE, 'ng\u00E0y' dd MMMM 'n\u0103m' yyyy", 
                    "'Ng\u00E0y' dd 'th\u00E1ng' M 'n\u0103m' yyyy", 
                    "dd-MM-yyyy", 
                    "dd/MM/yyyy", 
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{0} {1}" 
                }
            },
        };
    }
}
