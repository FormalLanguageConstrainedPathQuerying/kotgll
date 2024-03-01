/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047072
 * @summary javap OOM on fuzzed classfile
 * @modules jdk.jdeps/com.sun.tools.javap
 * @run main BadAttributeLength
 */


import java.io.*;

public class BadAttributeLength {

    public static String source = "public class Test {\n" +
                                  "    public static void main(String[] args) {}\n" +
                                  "}";

    public static void main(String[] args) throws Exception {
        final File sourceFile = new File("Test.java");
        if (sourceFile.exists()) {
            if (!sourceFile.delete()) {
                throw new IOException("Can't override the Test.java file. " +
                        "Check permissions.");
            }
        }
        try (FileWriter fw = new FileWriter(sourceFile)) {
            fw.write(source);
        }

        final String[] javacOpts = {"Test.java"};

        if (com.sun.tools.javac.Main.compile(javacOpts) != 0) {
            throw new Exception("Can't compile embedded test.");
        }

        RandomAccessFile raf = new RandomAccessFile("Test.class", "rw");
        long attPos = getFirstAttributePos(raf);
        if (attPos < 0) {
            throw new Exception("The class file contains no attributes at all.");
        }
        raf.seek(attPos + 2); 
        raf.writeInt(Integer.MAX_VALUE - 1);
        raf.close();

        String[] opts = { "-v", "Test.class" };
        StringWriter sw = new StringWriter();
        PrintWriter pout = new PrintWriter(sw);

        com.sun.tools.javap.Main.run(opts, pout);
        pout.flush();

        if (sw.getBuffer().indexOf("OutOfMemoryError") != -1) {
            throw new Exception("javap exited with OutOfMemoryError " +
                    "instead of giving the proper error message.");
        }
    }

    private static long getFirstAttributePos(RandomAccessFile cfile) throws Exception {
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
                    v2 = cfile.readUnsignedShort(); 
                    cfile.skipBytes(v2); 
                    break;
                default :
                    throw new Exception("Unexpected tag in CPool: [" + tag + "] at "
                            + Long.toHexString(cfile.getFilePointer()));
            }
        }

        cfile.skipBytes(6); 
        v1 = cfile.readUnsignedShort(); 
        cfile.skipBytes(3 * v1); 
        v1 = cfile.readUnsignedShort(); 
        for (; v1 > 0; v1--) {
            cfile.skipBytes(6); 
            v2 = cfile.readUnsignedShort(); 
            if (v2 > 0) {
                return cfile.getFilePointer();
            }
        }
        v1 = cfile.readUnsignedShort(); 
        for (; v1 > 0; v1--) {
            cfile.skipBytes(6); 
            v2 = cfile.readUnsignedShort(); 
            if (v2 > 0) {
                return cfile.getFilePointer();
            }
        }
        v1 = cfile.readUnsignedShort(); 
        if (v1 > 0) {
            return cfile.getFilePointer();
        }
        return -1L;
    }
}
