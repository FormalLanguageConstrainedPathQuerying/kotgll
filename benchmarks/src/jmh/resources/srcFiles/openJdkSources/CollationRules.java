/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996,1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996, 1997 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.util.locale.provider;
/**
 * CollationRules contains the default en_US collation rules as a base
 * for building other collation tables.
 * <p>Note that decompositions are done before these rules are used,
 * so they do not have to contain accented characters, such as A-grave.
 * @see                RuleBasedCollator
 * @see                LocaleElements
 * @author             Helena Shih, Mark Davis
 */
final class CollationRules {
    static final String DEFAULTRULES =
        "" 
        + "='\u200B'=\u200C=\u200D=\u200E=\u200F"
        + "=\u0000 =\u0001 =\u0002 =\u0003 =\u0004" 
        + "=\u0005 =\u0006 =\u0007 =\u0008 ='\u0009'" 
        + "='\u000b' =\u000e" 
        + "=\u000f ='\u0010' =\u0011 =\u0012 =\u0013" 
        + "=\u0014 =\u0015 =\u0016 =\u0017 =\u0018" 
        + "=\u0019 =\u001a =\u001b =\u001c =\u001d" 
        + "=\u001e =\u001f =\u007f"                   
        + "=\u0080 =\u0081 =\u0082 =\u0083 =\u0084 =\u0085"
        + "=\u0086 =\u0087 =\u0088 =\u0089 =\u008a =\u008b"
        + "=\u008c =\u008d =\u008e =\u008f =\u0090 =\u0091"
        + "=\u0092 =\u0093 =\u0094 =\u0095 =\u0096 =\u0097"
        + "=\u0098 =\u0099 =\u009a =\u009b =\u009c =\u009d"
        + "=\u009e =\u009f"
        + ";'\u0020';'\u00A0'"                  
        + ";'\u2000';'\u2001';'\u2002';'\u2003';'\u2004'"  
        + ";'\u2005';'\u2006';'\u2007';'\u2008';'\u2009'"  
        + ";'\u200A';'\u3000';'\uFEFF'"                
        + ";'\r' ;'\t' ;'\n';'\f';'\u000b'"  


        + ";\u0301"          
        + ";\u0300"          
        + ";\u0306"          
        + ";\u0302"          
        + ";\u030c"          
        + ";\u030a"          
        + ";\u030d"          
        + ";\u0308"          
        + ";\u030b"          
        + ";\u0303"          
        + ";\u0307"          
        + ";\u0304"          
        + ";\u0337"          
        + ";\u0327"          
        + ";\u0328"          
        + ";\u0323"          
        + ";\u0332"          
        + ";\u0305"          
        + ";\u0309"          
        + ";\u030e"          
        + ";\u030f"          
        + ";\u0310"          
        + ";\u0311"          
        + ";\u0312"          
        + ";\u0313"          
        + ";\u0314"          
        + ";\u0315"          
        + ";\u0316"          
        + ";\u0317"          
        + ";\u0318"          
        + ";\u0319"          
        + ";\u031a"          
        + ";\u031b"          
        + ";\u031c"          
        + ";\u031d"          
        + ";\u031e"          
        + ";\u031f"          
        + ";\u0320"          
        + ";\u0321"          
        + ";\u0322"          
        + ";\u0324"          
        + ";\u0325"          
        + ";\u0326"          
        + ";\u0329"          
        + ";\u032a"          
        + ";\u032b"          
        + ";\u032c"          
        + ";\u032d"          
        + ";\u032e"          
        + ";\u032f"          
        + ";\u0330"          
        + ";\u0331"          
        + ";\u0333"          
        + ";\u0334"          
        + ";\u0335"          
        + ";\u0336"          
        + ";\u0338"          
        + ";\u0339"          
        + ";\u033a"          
        + ";\u033b"          
        + ";\u033c"          
        + ";\u033d"          
        + ";\u033e"          
        + ";\u033f"          
        + ";\u0342;"
        + "\u0344;\u0345;\u0360;\u0361"    
        + ";\u0483;\u0484;\u0485;\u0486"    

        + ";\u20D0;\u20D1;\u20D2"           
        + ";\u20D3;\u20D4;\u20D5"           
        + ";\u20D6;\u20D7;\u20D8"           
        + ";\u20D9;\u20DA;\u20DB"           
        + ";\u20DC;\u20DD;\u20DE"           
        + ";\u20DF;\u20E0;\u20E1"           

        + ",'\u002D';\u00AD"                
        + ";\u2010;\u2011;\u2012"           
        + ";\u2013;\u2014;\u2015"           
        + ";\u2212"                         


        + "<'\u005f'"        
        + "<\u00af"          
        + "<'\u002c'"        
        + "<'\u003b'"        
        + "<'\u003a'"        
        + "<'\u0021'"        
        + "<\u00a1"          
        + "<'\u003f'"        
        + "<\u00bf"          
        + "<'\u002f'"        
        + "<'\u002e'"        
        + "<\u00b4"          
        + "<'\u0060'"        
        + "<'\u005e'"        
        + "<\u00a8"          
        + "<'\u007e'"        
        + "<\u00b7"          
        + "<\u00b8"          
        + "<'\u0027'"        
        + "<'\"'"            
        + "<\u00ab"          
        + "<\u00bb"          
        + "<'\u0028'"        
        + "<'\u0029'"        
        + "<'\u005b'"        
        + "<'\u005d'"        
        + "<'\u007b'"        
        + "<'\u007d'"        
        + "<\u00a7"          
        + "<\u00b6"          
        + "<\u00a9"          
        + "<\u00ae"          
        + "<'\u0040'"          
        + "<\u00a4"          
        + "<\u0e3f"          
        + "<\u00a2"          
        + "<\u20a1"          
        + "<\u20a2"          
        + "<'\u0024'"        
        + "<\u20ab"          
        + "<\u20ac"          
        + "<\u20a3"          
        + "<\u20a4"          
        + "<\u20a5"          
        + "<\u20a6"          
        + "<\u20a7"          
        + "<\u00a3"          
        + "<\u20a8"          
        + "<\u20aa"          
        + "<\u20a9"          
        + "<\u00a5"          
        + "<'\u002a'"        
        + "<'\\'"            
        + "<'\u0026'"        
        + "<'\u0023'"        
        + "<'\u0025'"        
        + "<'\u002b'"        
        + "<\u00b1"          
        + "<\u00f7"          
        + "<\u00d7"          
        + "<'\u003c'"        
        + "<'\u003d'"        
        + "<'\u003e'"        
        + "<\u00ac"          
        + "<'\u007c'"          
        + "<\u00a6"          
        + "<\u00b0"          
        + "<\u00b5"          


        + "<0<1<2<3<4<5<6<7<8<9"
        + "<\u00bc<\u00bd<\u00be"   

        + "<a,A"
        + "<b,B"
        + "<c,C"
        + "<d,D"
        + "<\u00F0,\u00D0"                  
        + "<e,E"
        + "<f,F"
        + "<g,G"
        + "<h,H"
        + "<i,I"
        + "<j,J"
        + "<k,K"
        + "<l,L"
        + "<m,M"
        + "<n,N"
        + "<o,O"
        + "<p,P"
        + "<q,Q"
        + "<r,R"
        + "<s, S & SS,\u00DF"             
        + "<t,T"
        + "& TH, \u00DE &TH, \u00FE "     
        + "<u,U"
        + "<v,V"
        + "<w,W"
        + "<x,X"
        + "<y,Y"
        + "<z,Z"
        + "&AE,\u00C6"                    
        + "&AE,\u00E6"
        + "&OE,\u0152"                    
        + "&OE,\u0153";

    private CollationRules() {
    }
}
