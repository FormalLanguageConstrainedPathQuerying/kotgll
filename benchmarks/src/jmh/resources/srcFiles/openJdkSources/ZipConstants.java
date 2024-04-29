/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.zipfs;

/**
 * @author Xueming Shen
 */
class ZipConstants {
    /*
     * Compression methods
     */
    static final int METHOD_STORED     = 0;
    static final int METHOD_DEFLATED   = 8;
    static final int METHOD_DEFLATED64 = 9;
    static final int METHOD_BZIP2      = 12;
    static final int METHOD_LZMA       = 14;
    static final int METHOD_LZ77       = 19;
    static final int METHOD_AES        = 99;

    /*
     * General purpose bit flag
     */
    static final int FLAG_ENCRYPTED  = 0x01;
    static final int FLAG_DATADESCR  = 0x08;    
    static final int FLAG_USE_UTF8   = 0x800;   
    /*
     * Header signatures
     */
    static long LOCSIG = 0x04034b50L;   
    static long EXTSIG = 0x08074b50L;   
    static long CENSIG = 0x02014b50L;   
    static long ENDSIG = 0x06054b50L;   

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;       
    static final int EXTHDR = 16;       
    static final int CENHDR = 46;       
    static final int ENDHDR = 22;       

    /*
     * File attribute compatibility types of CEN field "version made by"
     */
    static final int FILE_ATTRIBUTES_UNIX = 3; 

    /*
     * Base values for CEN field "version made by"
     */
    static final int VERSION_MADE_BY_BASE_UNIX = FILE_ATTRIBUTES_UNIX << 8; 

    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;        
    static final int LOCFLG = 6;        
    static final int LOCHOW = 8;        
    static final int LOCTIM = 10;       
    static final int LOCCRC = 14;       
    static final int LOCSIZ = 18;       
    static final int LOCLEN = 22;       
    static final int LOCNAM = 26;       
    static final int LOCEXT = 28;       

    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;        
    static final int EXTSIZ = 8;        
    static final int EXTLEN = 12;       

    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;        
    static final int CENVER = 6;        
    static final int CENFLG = 8;        
    static final int CENHOW = 10;       
    static final int CENTIM = 12;       
    static final int CENCRC = 16;       
    static final int CENSIZ = 20;       
    static final int CENLEN = 24;       
    static final int CENNAM = 28;       
    static final int CENEXT = 30;       
    static final int CENCOM = 32;       
    static final int CENDSK = 34;       
    static final int CENATT = 36;       
    static final int CENATX = 38;       
    static final int CENOFF = 42;       

    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;        
    static final int ENDTOT = 10;       
    static final int ENDSIZ = 12;       
    static final int ENDOFF = 16;       
    static final int ENDCOM = 20;       

    /*
     * ZIP64 constants
     */
    static final long ZIP64_ENDSIG = 0x06064b50L;  
    static final long ZIP64_LOCSIG = 0x07064b50L;  
    static final int  ZIP64_ENDHDR = 56;           
    static final int  ZIP64_LOCHDR = 20;           
    static final int  ZIP64_EXTHDR = 24;           
    static final int  ZIP64_EXTID  = 0x0001;       

    static final int  ZIP64_MINVAL32 = 0xFFFF;
    static final long ZIP64_MINVAL = 0xFFFFFFFFL;

    /*
     * Zip64 End of central directory (END) header field offsets
     */
    static final int  ZIP64_ENDLEN = 4;       
    static final int  ZIP64_ENDVEM = 12;      
    static final int  ZIP64_ENDVER = 14;      
    static final int  ZIP64_ENDNMD = 16;      
    static final int  ZIP64_ENDDSK = 20;      
    static final int  ZIP64_ENDTOD = 24;      
    static final int  ZIP64_ENDTOT = 32;      
    static final int  ZIP64_ENDSIZ = 40;      
    static final int  ZIP64_ENDOFF = 48;      
    static final int  ZIP64_ENDEXT = 56;      

    /*
     * Zip64 End of central directory locator field offsets
     */
    static final int  ZIP64_LOCDSK = 4;       
    static final int  ZIP64_LOCOFF = 8;       
    static final int  ZIP64_LOCTOT = 16;      

    /*
     * Zip64 Extra local (EXT) header field offsets
     */
    static final int  ZIP64_EXTCRC = 4;       
    static final int  ZIP64_EXTSIZ = 8;       
    static final int  ZIP64_EXTLEN = 16;      

    /*
     * Extra field header ID
     */
    static final int  EXTID_ZIP64 = 0x0001;      
    static final int  EXTID_NTFS  = 0x000a;      
    static final int  EXTID_UNIX  = 0x000d;      
    static final int  EXTID_EFS   = 0x0017;      
    static final int  EXTID_EXTT  = 0x5455;      

    /*
     * fields access methods
     */
    static final int CH(byte[] b, int n) {
        return Byte.toUnsignedInt(b[n]);
    }

    static final int SH(byte[] b, int n) {
        return Byte.toUnsignedInt(b[n]) | (Byte.toUnsignedInt(b[n + 1]) << 8);
    }

    static final long LG(byte[] b, int n) {
        return ((SH(b, n)) | (SH(b, n + 2) << 16)) & 0xffffffffL;
    }

    static final long LL(byte[] b, int n) {
        return (LG(b, n)) | (LG(b, n + 4) << 32);
    }

    static long getSig(byte[] b, int n) { return LG(b, n); }

    private static boolean pkSigAt(byte[] b, int n, int b1, int b2) {
        return b[n] == 'P' & b[n + 1] == 'K' & b[n + 2] == b1 & b[n + 3] == b2;
    }

    static boolean cenSigAt(byte[] b, int n) { return pkSigAt(b, n, 1, 2); }
    static boolean locSigAt(byte[] b, int n) { return pkSigAt(b, n, 3, 4); }
    static boolean endSigAt(byte[] b, int n) { return pkSigAt(b, n, 5, 6); }
    static boolean extSigAt(byte[] b, int n) { return pkSigAt(b, n, 7, 8); }
    static boolean end64SigAt(byte[] b, int n) { return pkSigAt(b, n, 6, 6); }
    static boolean locator64SigAt(byte[] b, int n) { return pkSigAt(b, n, 6, 7); }

    static final long LOCSIG(byte[] b) { return LG(b, 0); } 
    static final int  LOCVER(byte[] b) { return SH(b, 4); } 
    static final int  LOCFLG(byte[] b) { return SH(b, 6); } 
    static final int  LOCHOW(byte[] b) { return SH(b, 8); } 
    static final long LOCTIM(byte[] b) { return LG(b, 10);} 
    static final long LOCCRC(byte[] b) { return LG(b, 14);} 
    static final long LOCSIZ(byte[] b) { return LG(b, 18);} 
    static final long LOCLEN(byte[] b) { return LG(b, 22);} 
    static final int  LOCNAM(byte[] b) { return SH(b, 26);} 
    static final int  LOCEXT(byte[] b) { return SH(b, 28);} 

    static final long EXTCRC(byte[] b) { return LG(b, 4);}  
    static final long EXTSIZ(byte[] b) { return LG(b, 8);}  
    static final long EXTLEN(byte[] b) { return LG(b, 12);} 

    static final int  ENDSUB(byte[] b) { return SH(b, 8); }  
    static final int  ENDTOT(byte[] b) { return SH(b, 10);}  
    static final long ENDSIZ(byte[] b) { return LG(b, 12);}  
    static final long ENDOFF(byte[] b) { return LG(b, 16);}  
    static final int  ENDCOM(byte[] b) { return SH(b, 20);}  
    static final int  ENDCOM(byte[] b, int off) { return SH(b, off + 20);}

    static final long ZIP64_ENDTOD(byte[] b) { return LL(b, 24);}  
    static final long ZIP64_ENDTOT(byte[] b) { return LL(b, 32);}  
    static final long ZIP64_ENDSIZ(byte[] b) { return LL(b, 40);}  
    static final long ZIP64_ENDOFF(byte[] b) { return LL(b, 48);}  
    static final long ZIP64_LOCOFF(byte[] b) { return LL(b, 8);}   

    static final long CENSIG(byte[] b, int pos) { return LG(b, pos + 0); } 
    static final int  CENVEM(byte[] b, int pos) { return SH(b, pos + 4); } 
    static final int  CENVEM_FA(byte[] b, int pos) { return CH(b, pos + 5); } 
    static final int  CENVER(byte[] b, int pos) { return SH(b, pos + 6); } 
    static final int  CENFLG(byte[] b, int pos) { return SH(b, pos + 8); } 
    static final int  CENHOW(byte[] b, int pos) { return SH(b, pos + 10);} 
    static final long CENTIM(byte[] b, int pos) { return LG(b, pos + 12);} 
    static final long CENCRC(byte[] b, int pos) { return LG(b, pos + 16);} 
    static final long CENSIZ(byte[] b, int pos) { return LG(b, pos + 20);} 
    static final long CENLEN(byte[] b, int pos) { return LG(b, pos + 24);} 
    static final int  CENNAM(byte[] b, int pos) { return SH(b, pos + 28);} 
    static final int  CENEXT(byte[] b, int pos) { return SH(b, pos + 30);} 
    static final int  CENCOM(byte[] b, int pos) { return SH(b, pos + 32);} 
    static final int  CENDSK(byte[] b, int pos) { return SH(b, pos + 34);} 
    static final int  CENATT(byte[] b, int pos) { return SH(b, pos + 36);} 
    static final long CENATX(byte[] b, int pos) { return LG(b, pos + 38);} 
    static final int  CENATX_PERMS(byte[] b, int pos) { return SH(b, pos + 40);} 
    static final long CENOFF(byte[] b, int pos) { return LG(b, pos + 42);} 

    /* The END header is followed by a variable length comment of size < 64k. */
    static final long END_MAXLEN = 0xFFFF + ENDHDR;
    static final int READBLOCKSZ = 128;
}
