/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @test
 * @bug 8027607
 * @summary Test whether illegal UTF-8 sequences are handled correctly.
 * @run main/othervm -Djava.util.PropertyResourceBundle.encoding=UTF-8 IllegalSequenceTest
 */

import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class IllegalSequenceTest {
    static final byte[][] illegalSequences = {
        {(byte)0xc0, (byte)0xaf}, 
        {(byte)0xc2, (byte)0xe0}, 
        {(byte)0xc2, (byte)0x80, (byte)0x80}, 
        {(byte)0xe0, (byte)0x80}, 
        {(byte)0xf4, (byte)0x90, (byte)0x80, (byte)0x80}, 
    };

    public static void main(String[] args) throws IOException {
        for (byte[] illegalSec: illegalSequences) {
            try (InputStream is = new ByteArrayInputStream(illegalSec)) {
                ResourceBundle rb = new PropertyResourceBundle(is);
                rb.getString("key");
            } catch (MalformedInputException |
                    UnmappableCharacterException e) {
                continue;
            }
            throw new RuntimeException("Excepted exception was not thrown.");
        }
    }
}
