/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ListResourceBundle;

public class CollationData_da extends ListResourceBundle {

        protected final Object[][] getContents() {
                return new Object[][] {
                        { "Rule",
                                "& A;\u00C1;\u00C0;\u00C2,a;\u00E1;\u00E0;\u00E2" 
                                        + "<B,b"
                                        + "<C;\u00c7,c;\u00e7" 
                                        + "<D;\u00D0;\u0110,d;\u00F0;\u0111" 
                                        + "<E;\u00C9;\u00C8;\u00CA;\u00CB,e;\u00E9;\u00E8;\u00EA;\u00EB" 
                                        + "<F,f <G,g <H,h"
                                        + "<I;\u00CD,i;\u00ED" 
                                        + "<J,j <K,k <L,l <M,m <N,n"
                                        + "<O;\u00D3;\u00d4,o;\u00F3;\u00f4" 
                                        + "<P,p <Q,q <R,r <S,s <T,t"
                                        + "<U,u <V,v <W,w <X,x"
                                        + "<Y;\u00DD;U\u0308,y;\u00FD;u\u0308" 
                                        + "<Z,z"
                                        + "<\u00c6,\u00e6" 
                                        + ";\u00c6\u0301,\u00e6\u0301" 
                                        + ";A\u0308,a\u0308 "       
                                        + "<\u00d8,\u00f8 " 
                                        + ";\u00d8\u0301,\u00f8\u0301" 
                                        + ";O\u0308,o\u0308 "  
                                        + ";O\u030b,o\u030b"        
                                        + "< \u00c5 , \u00e5"       
                                        + ";\u00c5\u0301,\u00e5\u0301" 
                                        + ", AA , Aa , aA , aa "      
                                        + "& ss;\u00DF"             
                                        + "& th, \u00FE & th, \u00DE "     
                                        + "& oe, \u0153 & oe, \u0152 " 
                        }
                };
        }
}
