/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234687
 * @summary change javap reporting on unknown attributes
 * @modules jdk.jdeps/com.sun.tools.javap
 * @run main BadAttributeName
 */


import java.io.*;
import java.nio.file.*;

public class BadAttributeName {

    public static String source = "public class Test {\n" +
                                  "    public static void main(String[] args) {}\n" +
                                  "}";

    public static void main(String[] args) throws Exception {
        final File srcFile = new File("Test.java");
        Files.writeString(Path.of("Test.java"), source);

        final String[] javacOpts = {"Test.java"};

        if (com.sun.tools.javac.Main.compile(javacOpts) != 0) {
            throw new Exception("Can't compile embedded test.");
        }

        RandomAccessFile raf = new RandomAccessFile("Test.class", "rw");
        String sourceFile = "SourceFile";
        long namePos = getConstantPoolUTF8Pos(raf, sourceFile);
        if (namePos < 0) {
            throw new Exception("The class file contains no SourceFile attribute.");
        }

        raf.seek(namePos); 
        String customAttr = "CustomAttribute".substring(0, sourceFile.length());
        raf.writeUTF(customAttr);
        raf.close();

        String[] opts = { "-v", "Test.class" };
        StringWriter sw = new StringWriter();
        PrintWriter pout = new PrintWriter(sw);

        com.sun.tools.javap.Main.run(opts, pout);
        pout.flush();

        String expect = customAttr + ": length = 0x2 (unknown attribute)";
        if (sw.toString().indexOf(expect) == -1) {
            sw.toString().lines().forEach(System.out::println);
            throw new Exception("expected text not found: " + expect);
        }
    }

    private static long getConstantPoolUTF8Pos(RandomAccessFile cfile, String name) throws Exception {
        cfile.seek(0);
        int v1, v2;
        v1 = cfile.readInt();

        v1 = cfile.readUnsignedShort();
        v2 = cfile.readUnsignedShort();

        v1 = cfile.readUnsignedShort();
        for (; v1 > 1; v1--) {
            byte tag = cfile.readByte();
            switch (tag) {
                case 7  : 
                case 8  : 
                    cfile.skipBytes(2);
                    break;
                case 3  : 
                case 4  : 
                case 9  : 
                case 10 : 
                case 11 : 
                case 12 : 
                    cfile.skipBytes(4);
                    break;
                case 5  : 
                case 6  : 
                    cfile.skipBytes(8);
                    break;
                case 1  : 
                    long fp = cfile.getFilePointer();
                    String s = cfile.readUTF();
                    if (s.equals(name)) {
                        return fp;
                    }
                    break;
                default :
                    throw new Exception("Unexpected tag in CPool: [" + tag + "] at "
                            + Long.toHexString(cfile.getFilePointer()));
            }
        }

        return -1L;
    }
}
