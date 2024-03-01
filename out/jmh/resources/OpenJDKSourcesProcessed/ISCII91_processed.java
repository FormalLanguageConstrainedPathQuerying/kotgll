/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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


package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;
import sun.nio.cs.HistoricallyNamedCharset;

public class ISCII91 extends Charset implements HistoricallyNamedCharset
{
    private static final char NUKTA_CHAR = '\u093c';
    private static final char HALANT_CHAR = '\u094d';
    private static final byte NO_CHAR = (byte)255;

    public ISCII91() {
        super("x-ISCII91", ExtendedCharsets.aliasesFor("x-ISCII91"));
    }

    public String historicalName() {
        return "ISCII91";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof ISCII91));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static final char[] directMapTable = {
        '\u0000', 
        '\u0001', 
        '\u0002', 
        '\u0003', 
        '\u0004', 
        '\u0005', 
        '\u0006', 
        '\u0007', 
        '\u0008', 
        '\u0009', 
        '\012', 
        '\u000b', 
        '\u000c', 
        '\015', 
        '\u000e', 
        '\u000f', 
        '\u0010', 
        '\u0011', 
        '\u0012', 
        '\u0013', 
        '\u0014', 
        '\u0015', 
        '\u0016', 
        '\u0017', 
        '\u0018', 
        '\u0019', 
        '\u001a', 
        '\u001b', 
        '\u001c', 
        '\u001d', 
        '\u001e', 
        '\u001f', 
        '\u0020', 
        '\u0021', 
        '\u0022', 
        '\u0023', 
        '\u0024', 
        '\u0025', 
        '\u0026', 
        (char)0x0027, 
        '\u0028', 
        '\u0029', 
        '\u002a', 
        '\u002b', 
        '\u002c', 
        '\u002d', 
        '\u002e', 
        '\u002f', 
        '\u0030', 
        '\u0031', 
        '\u0032', 
        '\u0033', 
        '\u0034', 
        '\u0035', 
        '\u0036', 
        '\u0037', 
        '\u0038', 
        '\u0039', 
        '\u003a', 
        '\u003b', 
        '\u003c', 
        '\u003d', 
        '\u003e', 
        '\u003f', 
        '\u0040', 
        '\u0041', 
        '\u0042', 
        '\u0043', 
        '\u0044', 
        '\u0045', 
        '\u0046', 
        '\u0047', 
        '\u0048', 
        '\u0049', 
        '\u004a', 
        '\u004b', 
        '\u004c', 
        '\u004d', 
        '\u004e', 
        '\u004f', 
        '\u0050', 
        '\u0051', 
        '\u0052', 
        '\u0053', 
        '\u0054', 
        '\u0055', 
        '\u0056', 
        '\u0057', 
        '\u0058', 
        '\u0059', 
        '\u005a', 
        '\u005b', 
        '\\',
        '\u005d', 
        '\u005e', 
        '\u005f', 
        '\u0060', 
        '\u0061', 
        '\u0062', 
        '\u0063', 
        '\u0064', 
        '\u0065', 
        '\u0066', 
        '\u0067', 
        '\u0068', 
        '\u0069', 
        '\u006a', 
        '\u006b', 
        '\u006c', 
        '\u006d', 
        '\u006e', 
        '\u006f', 
        '\u0070', 
        '\u0071', 
        '\u0072', 
        '\u0073', 
        '\u0074', 
        '\u0075', 
        '\u0076', 
        '\u0077', 
        '\u0078', 
        '\u0079', 
        '\u007a', 
        '\u007b', 
        '\u007c', 
        '\u007d', 
        '\u007e', 
        '\u007f', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\u0901', 
        '\u0902', 
        '\u0903', 

        '\u0905', 
        '\u0906', 
        '\u0907', 
        '\u0908', 
        '\u0909', 
        '\u090a', 
        '\u090b', 
        '\u090e', 
        '\u090f', 
        '\u0910', 
        '\u090d', 
        '\u0912', 
        '\u0913', 
        '\u0914', 
        '\u0911', 
        '\u0915', 
        '\u0916', 
        '\u0917', 
        '\u0918', 
        '\u0919', 
        '\u091a', 
        '\u091b', 
        '\u091c', 
        '\u091d', 
        '\u091e', 
        '\u091f', 
        '\u0920', 
        '\u0921', 
        '\u0922', 
        '\u0923', 
        '\u0924', 
        '\u0925', 
        '\u0926', 
        '\u0927', 
        '\u0928', 
        '\u0929', 
        '\u092a', 
        '\u092b', 
        '\u092c', 
        '\u092d', 
        '\u092e', 
        '\u092f', 
        '\u095f', 
        '\u0930', 
        '\u0931', 
        '\u0932', 
        '\u0933', 
        '\u0934', 
        '\u0935', 
        '\u0936', 
        '\u0937', 
        '\u0938', 
        '\u0939', 

        '\u200d', 
        '\u093e', 

        '\u093f', 
        '\u0940', 
        '\u0941', 
        '\u0942', 
        '\u0943', 
        '\u0946', 
        '\u0947', 
        '\u0948', 
        '\u0945', 
        '\u094a', 
        '\u094b', 
        '\u094c', 
        '\u0949', 

        '\u094d', 
        '\u093c', 
        '\u0964', 

        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 

        '\ufffd', 
        '\ufffd', 

        '\u0966', 
        '\u0967', 
        '\u0968', 
        '\u0969', 
        '\u096a', 
        '\u096b', 
        '\u096c', 
        '\u096d', 
        '\u096e', 
        '\u096f', 

        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff', 
        '\uffff'  
    }; 

    private static final byte[] encoderMappingTable = {
    NO_CHAR,NO_CHAR, 
    (byte)161,NO_CHAR, 
    (byte)162,NO_CHAR, 
    (byte)163,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    (byte)164,NO_CHAR, 
    (byte)165,NO_CHAR, 
    (byte)166,NO_CHAR, 
    (byte)167,NO_CHAR, 
    (byte)168,NO_CHAR, 
    (byte)169,NO_CHAR, 
    (byte)170,NO_CHAR, 
    (byte)166,(byte)233, 
    (byte)174,NO_CHAR, 
    (byte)171,NO_CHAR, 
    (byte)172,NO_CHAR, 
    (byte)173,NO_CHAR, 
    (byte)178,NO_CHAR, 
    (byte)175,NO_CHAR, 
    (byte)176,NO_CHAR, 
    (byte)177,NO_CHAR, 
    (byte)179,NO_CHAR, 
    (byte)180,NO_CHAR, 
    (byte)181,NO_CHAR, 
    (byte)182,NO_CHAR, 
    (byte)183,NO_CHAR, 
    (byte)184,NO_CHAR, 
    (byte)185,NO_CHAR, 
    (byte)186,NO_CHAR, 
    (byte)187,NO_CHAR, 
    (byte)188,NO_CHAR, 
    (byte)189,NO_CHAR, 
    (byte)190,NO_CHAR, 
    (byte)191,NO_CHAR, 
    (byte)192,NO_CHAR, 
    (byte)193,NO_CHAR, 
    (byte)194,NO_CHAR, 
    (byte)195,NO_CHAR, 
    (byte)196,NO_CHAR, 
    (byte)197,NO_CHAR, 
    (byte)198,NO_CHAR, 
    (byte)199,NO_CHAR, 
    (byte)200,NO_CHAR, 
    (byte)201,NO_CHAR, 
    (byte)202,NO_CHAR, 
    (byte)203,NO_CHAR, 
    (byte)204,NO_CHAR, 
    (byte)205,NO_CHAR, 
    (byte)207,NO_CHAR, 
    (byte)208,NO_CHAR, 
    (byte)209,NO_CHAR, 
    (byte)210,NO_CHAR, 
    (byte)211,NO_CHAR, 
    (byte)212,NO_CHAR, 
    (byte)213,NO_CHAR, 
    (byte)214,NO_CHAR, 
    (byte)215,NO_CHAR, 
    (byte)216,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    (byte)233,NO_CHAR, 
    (byte)234,(byte)233, 
    (byte)218,NO_CHAR, 
    (byte)219,NO_CHAR, 
    (byte)220,NO_CHAR, 
    (byte)221,NO_CHAR, 
    (byte)222,NO_CHAR, 
    (byte)223,NO_CHAR, 
    (byte)223,(byte)233, 
    (byte)227,NO_CHAR, 
    (byte)224,NO_CHAR, 
    (byte)225,NO_CHAR, 
    (byte)226,NO_CHAR, 
    (byte)231,NO_CHAR, 
    (byte)228,NO_CHAR, 
    (byte)229,NO_CHAR, 
    (byte)230,NO_CHAR, 
    (byte)232,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    (byte)161,(byte)233, 
    (byte)240,(byte)181, 
    (byte)240,(byte)184, 
    (byte)254,NO_CHAR, 
    (byte)254,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    (byte)179,(byte)233, 
    (byte)180,(byte)233, 
    (byte)181,(byte)233, 
    (byte)186,(byte)233, 
    (byte)191,(byte)233, 
    (byte)192,(byte)233, 
    (byte)201,(byte)233, 
    (byte)206,NO_CHAR, 
    (byte)170,(byte)233, 
    (byte)167,(byte)233, 
    (byte)219,(byte)233, 
    (byte)220,(byte)233, 
    (byte)234,NO_CHAR, 
    (byte)234,(byte)234, 
    (byte)241,NO_CHAR, 
    (byte)242,NO_CHAR, 
    (byte)243,NO_CHAR, 
    (byte)244,NO_CHAR, 
    (byte)245,NO_CHAR, 
    (byte)246,NO_CHAR, 
    (byte)247,NO_CHAR, 
    (byte)248,NO_CHAR, 
    (byte)249,NO_CHAR, 
    (byte)250,NO_CHAR, 
    (byte)240,(byte)191,  
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR, 
    NO_CHAR,NO_CHAR  
    }; 

    private static class Decoder extends CharsetDecoder {

        private static final char ZWNJ_CHAR = '\u200c';
        private static final char ZWJ_CHAR = '\u200d';
        private static final char INVALID_CHAR = '\uffff';

        private char contextChar = INVALID_CHAR;
        private boolean needFlushing = false;


        private Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        protected CoderResult implFlush(CharBuffer out) {
            if(needFlushing) {
                if (out.remaining() < 1) {
                    return CoderResult.OVERFLOW;
                } else {
                    out.put(contextChar);
                }
            }
            contextChar = INVALID_CHAR;
            needFlushing = false;
            return CoderResult.UNDERFLOW;
        }

        /* Rules:
         * 1) ATR,EXT,following character to be replaced with '\ufffd'
         * 2) Halant + Halant => '\u094d' (Virama) + '\u200c'(ZWNJ)
         * 3) Halant + Nukta => '\u094d' (Virama) + '\u200d'(ZWJ)
         */
        private CoderResult decodeArrayLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    int index = sa[sp];
                    index = ( index < 0 )? ( index + 255 ):index;
                    char currentChar = directMapTable[index];

                    if(contextChar == '\ufffd') {
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = '\ufffd';
                        contextChar = INVALID_CHAR;
                        needFlushing = false;
                        sp++;
                        continue;
                    }

                    switch(currentChar) {
                    case '\u0901':
                    case '\u0907':
                    case '\u0908':
                    case '\u090b':
                    case '\u093f':
                    case '\u0940':
                    case '\u0943':
                    case '\u0964':
                        if(needFlushing) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        contextChar = currentChar;
                        needFlushing = true;
                        sp++;
                        continue;
                    case NUKTA_CHAR:
                        if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                        switch(contextChar) {
                        case '\u0901':
                            da[dp++] = '\u0950';
                            break;
                        case '\u0907':
                            da[dp++] = '\u090c';
                            break;
                        case '\u0908':
                            da[dp++] = '\u0961';
                            break;
                        case '\u090b':
                            da[dp++] = '\u0960';
                            break;
                        case '\u093f':
                            da[dp++] = '\u0962';
                            break;
                        case '\u0940':
                            da[dp++] = '\u0963';
                            break;
                        case '\u0943':
                            da[dp++] = '\u0944';
                            break;
                        case '\u0964':
                            da[dp++] = '\u093d';
                            break;
                        case HALANT_CHAR:
                            if(needFlushing) {
                                da[dp++] = contextChar;
                                contextChar = currentChar;
                                sp++;
                                continue;
                            }
                            da[dp++] = ZWJ_CHAR;
                            break;
                        default:
                            if(needFlushing) {
                                da[dp++] = contextChar;
                                contextChar = currentChar;
                                sp++;
                                continue;
                            }
                            da[dp++] = NUKTA_CHAR;
                        }
                        break;
                    case HALANT_CHAR:
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        if(contextChar == HALANT_CHAR) {
                            da[dp++] = ZWNJ_CHAR;
                            break;
                        }
                        da[dp++] = HALANT_CHAR;
                        break;
                    case INVALID_CHAR:
                        if(needFlushing) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        return CoderResult.unmappableForLength(1);
                    default:
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        da[dp++] = currentChar;
                        break;
                    }

                contextChar = currentChar;
                needFlushing = false;
                sp++;
            }
            return CoderResult.UNDERFLOW;
           } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
           }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();

            try {
                while (src.hasRemaining()) {
                    int index = src.get();
                    index = ( index < 0 )? ( index + 255 ):index;
                    char currentChar = directMapTable[index];

                    if(contextChar == '\ufffd') {
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put('\ufffd');
                        contextChar = INVALID_CHAR;
                        needFlushing = false;
                        mark++;
                        continue;
                    }

                    switch(currentChar) {
                    case '\u0901':
                    case '\u0907':
                    case '\u0908':
                    case '\u090b':
                    case '\u093f':
                    case '\u0940':
                    case '\u0943':
                    case '\u0964':
                        if(needFlushing) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        contextChar = currentChar;
                        needFlushing = true;
                        mark++;
                        continue;
                    case NUKTA_CHAR:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        switch(contextChar) {
                        case '\u0901':
                            dst.put('\u0950');
                            break;
                        case '\u0907':
                            dst.put('\u090c');
                            break;
                        case '\u0908':
                            dst.put('\u0961');
                            break;
                        case '\u090b':
                            dst.put('\u0960');
                            break;
                        case '\u093f':
                            dst.put('\u0962');
                            break;
                        case '\u0940':
                            dst.put('\u0963');
                            break;
                        case '\u0943':
                            dst.put('\u0944');
                            break;
                        case '\u0964':
                            dst.put('\u093d');
                            break;
                        case HALANT_CHAR:
                            if(needFlushing) {
                                dst.put(contextChar);
                                contextChar = currentChar;
                                mark++;
                                continue;
                            }
                            dst.put(ZWJ_CHAR);
                            break;
                        default:
                            if(needFlushing) {
                                dst.put(contextChar);
                                contextChar = currentChar;
                                mark++;
                                continue;
                            }
                            dst.put(NUKTA_CHAR);
                        }
                        break;
                    case HALANT_CHAR:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        if(contextChar == HALANT_CHAR) {
                            dst.put(ZWNJ_CHAR);
                            break;
                        }
                        dst.put(HALANT_CHAR);
                        break;
                    case INVALID_CHAR:
                        if(needFlushing) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        return CoderResult.unmappableForLength(1);
                    default:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        dst.put(currentChar);
                        break;
                    }
                contextChar = currentChar;
                needFlushing = false;
                mark++;
                }
            return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
           }
        }

        protected CoderResult decodeLoop(ByteBuffer src,
                                         CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }

    private static class Encoder extends CharsetEncoder {

        private static final byte NO_CHAR = (byte)255;


        private final Surrogate.Parser sgp = new Surrogate.Parser();

        private Encoder(Charset cs) {
            super(cs, 2.0f, 2.0f);
        }

        public boolean canEncode(char ch) {
            return ((ch >= '\u0900' && ch <= '\u097f' &&
                     encoderMappingTable[2*(ch-'\u0900')] != NO_CHAR) ||
                    (ch == '\u200d') ||
                    (ch == '\u200c') ||
                    (ch <= '\u007f'));
        }


        private CoderResult encodeArrayLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                char inputChar;
                while (sp < sl) {
                    int index = Integer.MIN_VALUE;
                    inputChar = sa[sp];

                    if (inputChar >= 0x0000 && inputChar <= 0x007f) {
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte) inputChar;
                        sp++;
                        continue;
                    }


                    if (inputChar == 0x200c) {
                        inputChar = HALANT_CHAR;
                    }
                    else if (inputChar == 0x200d) {
                        inputChar = NUKTA_CHAR;
                    }

                    if (inputChar >= 0x0900 && inputChar <= 0x097f) {
                        index = ((int)(inputChar) - 0x0900)*2;
                    }

                    if (Character.isSurrogate(inputChar)) {
                        if (sgp.parse(inputChar, sa, sp, sl) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    if (index == Integer.MIN_VALUE ||
                        encoderMappingTable[index] == NO_CHAR) {
                        return CoderResult.unmappableForLength(1);
                    } else {
                        if(encoderMappingTable[index + 1] == NO_CHAR) {
                            if(dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = encoderMappingTable[index];
                        } else {
                            if(dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = encoderMappingTable[index];
                            da[dp++] = encoderMappingTable[index + 1];
                        }
                        sp++;
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int mark = src.position();

            try {
                char inputChar;
                while (src.hasRemaining()) {
                    int index = Integer.MIN_VALUE;
                    inputChar = src.get();

                    if (inputChar >= 0x0000 && inputChar <= 0x007f) {
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte) inputChar);
                        mark++;
                        continue;
                    }


                    if (inputChar == 0x200c) {
                        inputChar = HALANT_CHAR;
                    }
                    else if (inputChar == 0x200d) {
                        inputChar = NUKTA_CHAR;
                    }

                    if (inputChar >= 0x0900 && inputChar <= 0x097f) {
                        index = ((int)(inputChar) - 0x0900)*2;
                    }

                    if (Character.isSurrogate(inputChar)) {
                        if (sgp.parse(inputChar, src) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    if (index == Integer.MIN_VALUE ||
                        encoderMappingTable[index] == NO_CHAR) {
                        return CoderResult.unmappableForLength(1);
                    } else {
                        if(encoderMappingTable[index + 1] == NO_CHAR) {
                            if(dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(encoderMappingTable[index]);
                        } else {
                            if(dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put(encoderMappingTable[index]);
                            dst.put(encoderMappingTable[index + 1]);
                        }
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src,
                                         ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
