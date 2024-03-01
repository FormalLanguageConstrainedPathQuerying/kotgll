/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.hprof.parser.PositionDataInputStream;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8281267
 * @summary Verifies heap dump does not contain duplicate array classes
 * @library /test/lib
 * @run driver DuplicateArrayClassesTest
 */

class DuplicateArrayClassesTarg extends LingeredApp {
    public static void main(String[] args) {
        int[][][] intArray = new int[0][][];
        String[][][] strArray = new String[0][][];
        LingeredApp.main(args);
        Reference.reachabilityFence(intArray);
        Reference.reachabilityFence(strArray);
    }
}


public class DuplicateArrayClassesTest {

    public static void main(String[] args) throws Exception {
        File dumpFile = new File("Myheapdump.hprof");
        createDump(dumpFile);
        verifyDump(dumpFile);
    }

    private static void createDump(File dumpFile) throws Exception {
        LingeredApp theApp = null;
        try {
            theApp = new DuplicateArrayClassesTarg();

            LingeredApp.startApp(theApp);

            JDKToolLauncher launcher = JDKToolLauncher
                    .createUsingTestJDK("jcmd")
                    .addToolArg(Long.toString(theApp.getPid()))
                    .addToolArg("GC.heap_dump")
                    .addToolArg(dumpFile.getAbsolutePath());
            Process p = ProcessTools.startProcess("jcmd", new ProcessBuilder(launcher.getCommand()));
            while (!p.waitFor(5, TimeUnit.SECONDS)) {
                if (!theApp.getProcess().isAlive()) {
                    log("ERROR: target VM died, killing jcmd...");
                    p.destroyForcibly();
                    throw new Exception("Target VM died");
                }
            }

            if (p.exitValue() != 0) {
                throw new Exception("Jcmd exited with code " + p.exitValue());
            }
        } finally {
            LingeredApp.stopApp(theApp);
        }
    }

    private static final byte HPROF_UTF8                = 0x01;
    private static final byte HPROF_LOAD_CLASS          = 0x02;
    private static final byte HPROF_HEAP_DUMP           = 0x0c;
    private static final byte HPROF_GC_CLASS_DUMP       = 0x20;
    private static final byte HPROF_HEAP_DUMP_SEGMENT   = 0x1C;
    private static final byte HPROF_HEAP_DUMP_END       = 0x2C;

    private static void verifyDump(File dumpFile) throws IOException {
        Asserts.assertTrue(dumpFile.exists(), "Heap dump file not found.");

        Map<Long, String> names = new HashMap<>();
        Map<Long, String> classId2Name = new Hashtable<>();
        Map<String, Long> className2Id = new Hashtable<>();
        List<Long> duplicateLoadClassIDs = new LinkedList<>();
        Set<Long> dumpClassIDs = new HashSet<>();
        List<Long> duplicateDumpClassIDs = new LinkedList<>();

        try (DumpInputStream in = new DumpInputStream(dumpFile)) {
            while (true) {
                DumpRecord rec;
                try {
                    rec = in.readRecord();
                } catch (EOFException ex) {
                    break;
                }
                long pos = in.position();   

                switch (rec.tag()) {
                    case HPROF_UTF8:
                        long id = in.readID();
                        byte[] chars = new byte[(int) rec.size - in.idSize];
                        in.readFully(chars);
                        names.put(id, new String(chars));
                        break;
                    case HPROF_LOAD_CLASS:
                        long classSerialNo = in.readU4();
                        long classID = in.readID();
                        long stackTraceSerialNo = in.readU4();
                        long classNameID = in.readID();
                        String className = names.get(classNameID);

                        String prevName = classId2Name.putIfAbsent(classID, className);
                        if (prevName != null) { 
                            if (!prevName.equals(className)) {
                                throw new RuntimeException("Found new class with id=" + classID
                                        + " and different name (" + className + ", was " + prevName + ")");
                            }
                            duplicateLoadClassIDs.add(classID);
                        }
                        className2Id.putIfAbsent(className, classID);
                        break;
                    case HPROF_HEAP_DUMP:
                    case HPROF_HEAP_DUMP_SEGMENT:
                        long endOfRecordPos = pos + rec.size();

                        while (in.position() < endOfRecordPos) {
                            byte subTag = in.readU1();
                            if (subTag != HPROF_GC_CLASS_DUMP) {
                                break;
                            }
                            long dumpClassID = readClassDump(in);

                            if (!dumpClassIDs.add(dumpClassID)) {
                                duplicateDumpClassIDs.add(dumpClassID);
                            }
                        }
                        break;
                }

                long bytesRead = in.position() - pos;
                if (bytesRead > rec.size()) {
                    throw new RuntimeException("Bad record,"
                            + " record.size = " + rec.size() + ", read " + bytesRead);
                }
                in.skipNBytes(rec.size() - bytesRead);
            }

            log("HPROF_LOAD_CLASS records: " + (classId2Name.size() + duplicateLoadClassIDs.size()));
            log("HPROF_GC_CLASS_DUMP records: " + (dumpClassIDs.size() + duplicateDumpClassIDs.size()));

            String[] expectedClasses = {"[I", "[[I", "[[[I",
                    "[Ljava/lang/String;", "[[Ljava/lang/String;", "[[[Ljava/lang/String;"};
            for (String className: expectedClasses) {
                Long classId = className2Id.get(className);
                if (classId == null) {
                    throw new RuntimeException("no HPROF_LOAD_CLASS record for class " + className);
                }
                if (!dumpClassIDs.contains(classId)) {
                    throw new RuntimeException("no HPROF_GC_CLASS_DUMP for class " + className);
                }
                log("found " + className);
            }
            if (!duplicateLoadClassIDs.isEmpty() || !duplicateDumpClassIDs.isEmpty()) {
                log("Duplicate(s) detected:");
                log("HPROF_LOAD_CLASS records (" + duplicateLoadClassIDs.size() + "):");
                duplicateLoadClassIDs.forEach(id -> log("  - id = " + id + ": " + classId2Name.get(id)));
                log("HPROF_GC_CLASS_DUMP records (" + duplicateDumpClassIDs.size() + "):");
                duplicateDumpClassIDs.forEach(id -> log("  - id = " + id + ": " + classId2Name.get(id)));
                throw new RuntimeException("duplicates detected");
            }
        }
    }

    private static long readClassDump(DumpInputStream in) throws IOException {
        long classID = in.readID();
        long stackTraceNum = in.readU4();
        long superClassId = in.readID();
        long loaderClassId = in.readID();
        long signerClassId = in.readID();
        long protectionDomainClassId = in.readID();
        long reserved1 = in.readID();
        long reserved2 = in.readID();
        long instanceSize = in.readU4();
        long cpSize = in.readU2();
        for (long i = 0; i < cpSize; i++) {
            long cpIndex = in.readU2();
            byte type = in.readU1();
            in.skipNBytes(in.type2size(type)); 
        }
        long staticNum = in.readU2();
        for (long i = 0; i < staticNum; i++) {
            long nameId = in.readID();
            byte type = in.readU1();
            in.skipNBytes(in.type2size(type)); 
        }
        long instanceNum = in.readU2();
        for (long i = 0; i < instanceNum; i++) {
            long nameId = in.readID();
            byte type = in.readU1();
        }
        return classID;
    }

    private static void log(Object s) {
        System.out.println(s);
    }


    private static record DumpRecord (byte tag, long size) {}

    private static class DumpInputStream extends PositionDataInputStream {
        public final int idSize;

        public DumpInputStream(File f) throws IOException {
            super(new BufferedInputStream(new FileInputStream(f)));

            String header = readStr();
            log("header: \"" + header + "\"");
            Asserts.assertTrue(header.startsWith("JAVA PROFILE "));

            idSize = readInt();
            if (idSize != 4 && idSize != 8) {
                Asserts.fail("id size " + idSize + " is not supported");
            }
            readU4();
            readU4();
        }

        public String readStr() throws IOException {
            StringBuilder sb = new StringBuilder();
            for (char ch = (char)readByte(); ch != '\0'; ch = (char)readByte()) {
                sb.append(ch);
            }
            return sb.toString();
        }

        public byte readU1() throws IOException {
            return readByte();
        }
        public int readU2() throws IOException {
            return readUnsignedShort();
        }
        public long readU4() throws IOException {
            return ((long)readInt() & 0x0FFFFFFFFL);
        }

        public long readID() throws IOException {
            return idSize == 4 ? readU4() : readLong();
        }

        public DumpRecord readRecord() throws IOException {
            byte tag = readU1();
            readU4();   
            long size = readU4();
            return new DumpRecord(tag, size);
        }

        public long type2size(byte type) {
            switch (type) {
                case 1:     
                case 2:     
                    return idSize;
                case 4:     
                case 8:     
                    return 1;
                case 5:     
                case 9:     
                    return 2;
                case 6:     
                case 10:    
                    return 4;
                case 7:     
                case 11:    
                    return 8;
            }
            throw new RuntimeException("unknown type: " + type);
        }

    }

}
