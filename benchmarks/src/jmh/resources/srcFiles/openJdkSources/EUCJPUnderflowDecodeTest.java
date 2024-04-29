/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
   @bug 4867457
   @summary Check for correct byte buffer underflow handling in EUC-JP
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class EUCJPUnderflowDecodeTest {
    public static void main(String[] args) throws Exception{

        ByteBuffer bb = ByteBuffer.allocateDirect(255);
        CharBuffer cc = CharBuffer.allocate(255);



        String[] charsetNames = { "EUC_JP", "EUC-JP-LINUX" };

        for (int i = 0 ; i < charsetNames.length; i++) {
            Charset cs = Charset.forName(charsetNames[i]);
            CharsetDecoder decoder = cs.newDecoder();
            bb.clear();
            cc.clear();

            bb.put((byte)0x8f);
            bb.put((byte)0xa2);
            bb.flip();

            CoderResult result = decoder.decode(bb, cc, false);


            if (result != CoderResult.UNDERFLOW) {
                throw new Exception("test failed - UNDERFLOW not returned");
            }

            decoder.reset();
            bb.clear();
            cc.clear();
            bb.put((byte)0xa1);
            bb.flip();
            result = decoder.decode(bb, cc, false);
            if (result != CoderResult.UNDERFLOW) {
                throw new Exception("test failed");
            }


            decoder.reset();
            bb.clear();
            cc.clear();
            bb.put((byte)0xa1);
            bb.put((byte)0xc0);
            bb.flip();

            result = decoder.decode(bb, cc, false);

            cc.flip();

            if (result != CoderResult.UNDERFLOW && cc.get() != '\uFF3c') {
                throw new Exception("test failed to decode EUC-JP (0xA1C0)");
            }
        }
    }
}
