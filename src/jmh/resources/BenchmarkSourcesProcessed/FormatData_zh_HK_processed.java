/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Locale;
import java.util.ResourceBundle;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.ResourceBundleBasedAdapter;

public class FormatData_zh_HK extends ParallelListResourceBundle {

    public FormatData_zh_HK() {
        ResourceBundle bundle = ((ResourceBundleBasedAdapter)LocaleProviderAdapter.forJRE())
            .getLocaleData().getDateFormatData(Locale.TAIWAN);
        setParent(bundle);
    }

    /**
     * Overrides ParallelListResourceBundle
     */
    @Override
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthAbbreviations",
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
            { "DayAbbreviations",
                new String[] {
                    "\u65e5", 
                    "\u4e00", 
                    "\u4e8c", 
                    "\u4e09", 
                    "\u56db", 
                    "\u4e94", 
                    "\u516d" 
                }
            },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###;-#,##0.###", 
                    "\u00A4#,##0.00;(\u00A4#,##0.00)", 
                    "#,##0%" 
                }
            },
            { "TimePatterns",
                new String[] {
                    "ahh'\u6642'mm'\u5206'ss'\u79d2' z", 
                    "ahh'\u6642'mm'\u5206'ss'\u79d2'", 
                    "ahh:mm:ss", 
                    "ah:mm", 
                }
            },
            { "DatePatterns",
                new String[] {
                    "yyyy'\u5e74'MM'\u6708'dd'\u65e5' EEEE", 
                    "yyyy'\u5e74'MM'\u6708'dd'\u65e5' EEEE", 
                    "yyyy'\u5e74'M'\u6708'd'\u65e5'", 
                    "yy'\u5e74'M'\u6708'd'\u65e5'", 
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" 
                }
            },
            { "DateTimePatternChars", "GanjkHmsSEDFwWxhKzZ" },
        };
    }
}
