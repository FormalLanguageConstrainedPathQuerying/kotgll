/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
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

public class FormatData_ja extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    @Override
    protected final Object[][] getContents() {
        final String[] japaneseEras = {
            "\u897f\u66a6", 
            "\u660e\u6cbb", 
            "\u5927\u6b63", 
            "\u662d\u548c", 
            "\u5e73\u6210", 
            "\u4ee4\u548c", 
        };
        final String[] rocEras = {
            "\u6c11\u56fd\u524d",
            "\u6c11\u56fd",
        };
        final String[] gregoryEras = {
            "\u7d00\u5143\u524d",
            "\u897f\u66a6",
        };
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "1\u6708", 
                    "2\u6708", 
                    "3\u6708", 
                    "4\u6708", 
                    "5\u6708", 
                    "6\u6708", 
                    "7\u6708", 
                    "8\u6708", 
                    "9\u6708", 
                    "10\u6708", 
                    "11\u6708", 
                    "12\u6708", 
                    ""          
                }
            },
            { "MonthAbbreviations",
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
                    ""    
                }
            },
            { "DayNames",
                new String[] {
                    "\u65e5\u66dc\u65e5", 
                    "\u6708\u66dc\u65e5", 
                    "\u706b\u66dc\u65e5", 
                    "\u6c34\u66dc\u65e5", 
                    "\u6728\u66dc\u65e5", 
                    "\u91d1\u66dc\u65e5", 
                    "\u571f\u66dc\u65e5"  
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u65e5", 
                    "\u6708", 
                    "\u706b", 
                    "\u6c34", 
                    "\u6728", 
                    "\u91d1", 
                    "\u571f"  
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u65e5",
                    "\u6708",
                    "\u706b",
                    "\u6c34",
                    "\u6728",
                    "\u91d1",
                    "\u571f",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "\u5348\u524d", 
                    "\u5348\u5f8c" 
                }
            },
            { "Eras", gregoryEras },
            { "short.Eras", gregoryEras },
            { "buddhist.Eras",
                new String[] { 
                    "\u7d00\u5143\u524d", 
                    "\u4ecf\u66a6",       
                }
            },
            { "japanese.Eras", japaneseEras },
            { "japanese.FirstYear",
                new String[] {  
                    "\u5143",   
                }
            },
            { "NumberElements",
                new String[] {
                    ".",        
                    ",",        
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
                    "H'\u6642'mm'\u5206'ss'\u79d2' z", 
                    "H:mm:ss z",                       
                    "H:mm:ss",                         
                    "H:mm",                            
                }
            },
            { "DatePatterns",
                new String[] {
                    "yyyy'\u5e74'M'\u6708'd'\u65e5'",  
                    "yyyy/MM/dd",                      
                    "yyyy/MM/dd",                      
                    "yy/MM/dd",                        
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}"                          
                }
            },
            { "japanese.DatePatterns",
                new String[] {
                    "GGGGyyyy'\u5e74'M'\u6708'd'\u65e5'", 
                    "Gy.MM.dd",  
                    "Gy.MM.dd",  
                    "Gy.MM.dd",  
                }
            },
            { "japanese.TimePatterns",
                new String[] {
                    "H'\u6642'mm'\u5206'ss'\u79d2' z", 
                    "H:mm:ss z", 
                    "H:mm:ss",   
                    "H:mm",      
                }
            },
            { "japanese.DateTimePatterns",
                new String[] {
                    "{1} {0}"    
                }
            },
            { "DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ" },
        };
    }
}
