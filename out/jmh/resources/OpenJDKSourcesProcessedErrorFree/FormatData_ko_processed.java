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

public class FormatData_ko extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    @Override
    protected final Object[][] getContents() {
        final String[] rocEras = {
            "\uc911\ud654\ubbfc\uad6d\uc804",
            "\uc911\ud654\ubbfc\uad6d",
        };
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "1\uc6d4", 
                    "2\uc6d4", 
                    "3\uc6d4", 
                    "4\uc6d4", 
                    "5\uc6d4", 
                    "6\uc6d4", 
                    "7\uc6d4", 
                    "8\uc6d4", 
                    "9\uc6d4", 
                    "10\uc6d4", 
                    "11\uc6d4", 
                    "12\uc6d4", 
                    "" 
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "1\uc6d4", 
                    "2\uc6d4", 
                    "3\uc6d4", 
                    "4\uc6d4", 
                    "5\uc6d4", 
                    "6\uc6d4", 
                    "7\uc6d4", 
                    "8\uc6d4", 
                    "9\uc6d4", 
                    "10\uc6d4", 
                    "11\uc6d4", 
                    "12\uc6d4", 
                    "" 
                }
            },
            { "MonthNarrows",
                new String[] {
                    "1\uc6d4",
                    "2\uc6d4",
                    "3\uc6d4",
                    "4\uc6d4",
                    "5\uc6d4",
                    "6\uc6d4",
                    "7\uc6d4",
                    "8\uc6d4",
                    "9\uc6d4",
                    "10\uc6d4",
                    "11\uc6d4",
                    "12\uc6d4",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\uc77c\uc694\uc77c", 
                    "\uc6d4\uc694\uc77c", 
                    "\ud654\uc694\uc77c", 
                    "\uc218\uc694\uc77c", 
                    "\ubaa9\uc694\uc77c", 
                    "\uae08\uc694\uc77c", 
                    "\ud1a0\uc694\uc77c" 
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\uc77c", 
                    "\uc6d4", 
                    "\ud654", 
                    "\uc218", 
                    "\ubaa9", 
                    "\uae08", 
                    "\ud1a0" 
                }
            },
            { "DayNarrows",
                new String[] {
                    "\uc77c",
                    "\uc6d4",
                    "\ud654",
                    "\uc218",
                    "\ubaa9",
                    "\uae08",
                    "\ud1a0",
                }
            },
            { "Eras",
                new String[] {
                    "\uae30\uc6d0\uc804",
                    "\uc11c\uae30",
                }
            },
            { "buddhist.Eras",
                new String[] {
                    "BC",
                    "\ubd88\uae30",
                }
            },
            { "japanese.Eras",
                new String[] {
                    "\uc11c\uae30",
                    "\uba54\uc774\uc9c0",
                    "\ub2e4\uc774\uc1fc",
                    "\uc1fc\uc640",
                    "\ud5e4\uc774\uc138\uc774",
                    "\ub808\uc774\uc640",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "\uc624\uc804", 
                    "\uc624\ud6c4" 
                }
            },
            { "TimePatterns",
                new String[] {
                    "a h'\uc2dc' mm'\ubd84' ss'\ucd08' z", 
                    "a h'\uc2dc' mm'\ubd84' ss'\ucd08'", 
                    "a h:mm:ss", 
                    "a h:mm", 
                }
            },
            { "DatePatterns",
                new String[] {
                    "yyyy'\ub144' M'\uc6d4' d'\uc77c' EEEE", 
                    "yyyy'\ub144' M'\uc6d4' d'\uc77c' '('EE')'", 
                    "yyyy. M. d", 
                    "yy. M. d", 
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" 
                }
            },
            { "buddhist.DatePatterns",
                new String[] {
                    "GGGG y\ub144 M\uc6d4 d\uc77c EEEE",
                    "GGGG y\ub144 M\uc6d4 d\uc77c",
                    "GGGG y. M. d",
                    "GGGG y. M. d",
                }
            },
            { "japanese.DatePatterns",
                new String[] {
                    "GGGG y\ub144 M\uc6d4 d\uc77c EEEE",
                    "GGGG y\ub144 M\uc6d4 d\uc77c",
                    "GGGG y. M. d",
                    "GGGG y. M. d",
                }
            },
            { "DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ" },
        };
    }
}
