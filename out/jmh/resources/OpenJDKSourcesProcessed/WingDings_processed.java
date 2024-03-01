/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

public final class WingDings extends Charset {
    public WingDings () {
        super("WingDings", null);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    /* Seems like supporting a decoder is required, but we aren't going
     * to be publicly exposing this class, so no need to waste work
     */
    @Override
    public CharsetDecoder newDecoder() {
        throw new Error("Decoder isn't implemented for WingDings Charset");
    }

    @Override
    public boolean contains(Charset cs) {
        return cs instanceof WingDings;
    }

    private static class Encoder extends CharsetEncoder {
        public Encoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        @Override
        public boolean canEncode(char c) {
            if(c >= 0x2701 && c <= 0x27be){
                if (table[c - 0x2700] != 0x00)
                    return true;
                else
                    return false;
            }
            return false;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            assert (sp <= sl);
            sp = (sp <= sl ? sp : sl);
            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();
            assert (dp <= dl);
            dp = (dp <= dl ? dp : dl);

            try {
                while (sp < sl) {
                    char c = sa[sp];
                    if (dl - dp < 1)
                        return CoderResult.OVERFLOW;
                    if (!canEncode(c))
                        return CoderResult.unmappableForLength(1);
                    sp++;
                    da[dp++] = table[c - 0x2700];
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private static byte[] table = {
            (byte)0x00, (byte)0x23, (byte)0x22, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x29, (byte)0x3e,  
            (byte)0x51, (byte)0x2a, (byte)0x00, (byte)0x00,  
            (byte)0x41, (byte)0x3f, (byte)0x00, (byte)0x00,  

            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfc,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfb,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x56, (byte)0x00,  

            (byte)0x58, (byte)0x59, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0xb5, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  

            (byte)0xb6, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0xad, (byte)0xaf, (byte)0xac, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x7c,  

            (byte)0x7b, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x54, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0xa6, (byte)0x00, (byte)0x00,  

            (byte)0x00, (byte)0x71, (byte)0x72, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x75, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x7d, (byte)0x7e, (byte)0x00,  

            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  

            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x8c, (byte)0x8d,  
            (byte)0x8e, (byte)0x8f, (byte)0x90, (byte)0x91,  
            (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95,  

            (byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84,  
            (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88,  
            (byte)0x89, (byte)0x8a, (byte)0x8c, (byte)0x8d,  
            (byte)0x8e, (byte)0x8f, (byte)0x90, (byte)0x91,  

            (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95,  
            (byte)0xe8, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  

            (byte)0x00, (byte)0xe8, (byte)0xd8, (byte)0x00,  
            (byte)0x00, (byte)0xc4, (byte)0xc6, (byte)0x00,  
            (byte)0x00, (byte)0xf0, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  

            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xdc,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  
            (byte)0x00, (byte)0x00, (byte)0x00               
        };

        /* The default implementation creates a decoder and we don't have one */
        @Override
        public boolean isLegalReplacement(byte[] repl) {
            return true;
        }
    }
}
