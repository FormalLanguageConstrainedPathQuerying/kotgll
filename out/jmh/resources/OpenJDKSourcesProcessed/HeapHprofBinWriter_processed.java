/*
 * Copyright (c) 2004, 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.utilities;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.classfile.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * This class writes Java heap in hprof binary format. This format is
 * used by Heap Analysis Tool (HAT). The class is heavily influenced
 * by 'hprof_io.c' of 1.5 new hprof implementation.
 */

/* hprof binary format: (result either written to a file or sent over
 * the network).
 *
 * WARNING: This format is still under development, and is subject to
 * change without notice.
 *
 * header     "JAVA PROFILE 1.0.2" (0-terminated)
 * u4         size of identifiers. Identifiers are used to represent
 *            UTF8 strings, objects, stack traces, etc. They usually
 *            have the same size as host pointers. For example, on
 *            Solaris and Win32, the size is 4.
 * u4         high word
 * u4         low word    number of milliseconds since 0:00 GMT, 1/1/70
 * [record]*  a sequence of records.
 *
 */

/*
 *
 * Record format:
 *
 * u1         a TAG denoting the type of the record
 * u4         number of *microseconds* since the time stamp in the
 *            header. (wraps around in a little more than an hour)
 * u4         number of bytes *remaining* in the record. Note that
 *            this number excludes the tag and the length field itself.
 * [u1]*      BODY of the record (a sequence of bytes)
 */

/*
 * The following TAGs are supported:
 *
 * TAG           BODY       notes
 *----------------------------------------------------------
 * HPROF_UTF8               a UTF8-encoded name
 *
 *               id         name ID
 *               [u1]*      UTF8 characters (no trailing zero)
 *
 * HPROF_LOAD_CLASS         a newly loaded class
 *
 *                u4        class serial number (> 0)
 *                id        class object ID
 *                u4        stack trace serial number
 *                id        class name ID
 *
 * HPROF_UNLOAD_CLASS       an unloading class
 *
 *                u4        class serial_number
 *
 * HPROF_FRAME              a Java stack frame
 *
 *                id        stack frame ID
 *                id        method name ID
 *                id        method signature ID
 *                id        source file name ID
 *                u4        class serial number
 *                i4        line number. >0: normal
 *                                       -1: unknown
 *                                       -2: compiled method
 *                                       -3: native method
 *
 * HPROF_TRACE              a Java stack trace
 *
 *               u4         stack trace serial number
 *               u4         thread serial number
 *               u4         number of frames
 *               [id]*      stack frame IDs
 *
 *
 * HPROF_ALLOC_SITES        a set of heap allocation sites, obtained after GC
 *
 *               u2         flags 0x0001: incremental vs. complete
 *                                0x0002: sorted by allocation vs. live
 *                                0x0004: whether to force a GC
 *               u4         cutoff ratio
 *               u4         total live bytes
 *               u4         total live instances
 *               u8         total bytes allocated
 *               u8         total instances allocated
 *               u4         number of sites that follow
 *               [u1        is_array: 0:  normal object
 *                                    2:  object array
 *                                    4:  boolean array
 *                                    5:  char array
 *                                    6:  float array
 *                                    7:  double array
 *                                    8:  byte array
 *                                    9:  short array
 *                                    10: int array
 *                                    11: long array
 *                u4        class serial number (may be zero during startup)
 *                u4        stack trace serial number
 *                u4        number of bytes alive
 *                u4        number of instances alive
 *                u4        number of bytes allocated
 *                u4]*      number of instance allocated
 *
 * HPROF_START_THREAD       a newly started thread.
 *
 *               u4         thread serial number (> 0)
 *               id         thread object ID
 *               u4         stack trace serial number
 *               id         thread name ID
 *               id         thread group name ID
 *               id         thread group parent name ID
 *
 * HPROF_END_THREAD         a terminating thread.
 *
 *               u4         thread serial number
 *
 * HPROF_HEAP_SUMMARY       heap summary
 *
 *               u4         total live bytes
 *               u4         total live instances
 *               u8         total bytes allocated
 *               u8         total instances allocated
 *
 * HPROF_HEAP_DUMP          denote a heap dump
 *
 *               [heap dump sub-records]*
 *
 *                          There are four kinds of heap dump sub-records:
 *
 *               u1         sub-record type
 *
 *               HPROF_GC_ROOT_UNKNOWN         unknown root
 *
 *                          id         object ID
 *
 *               HPROF_GC_ROOT_THREAD_OBJ      thread object
 *
 *                          id         thread object ID  (may be 0 for a
 *                                     thread newly attached through JNI)
 *                          u4         thread sequence number
 *                          u4         stack trace sequence number
 *
 *               HPROF_GC_ROOT_JNI_GLOBAL      JNI global ref root
 *
 *                          id         object ID
 *                          id         JNI global ref ID
 *
 *               HPROF_GC_ROOT_JNI_LOCAL       JNI local ref
 *
 *                          id         object ID
 *                          u4         thread serial number
 *                          u4         frame # in stack trace (-1 for empty)
 *
 *               HPROF_GC_ROOT_JAVA_FRAME      Java stack frame
 *
 *                          id         object ID
 *                          u4         thread serial number
 *                          u4         frame # in stack trace (-1 for empty)
 *
 *               HPROF_GC_ROOT_NATIVE_STACK    Native stack
 *
 *                          id         object ID
 *                          u4         thread serial number
 *
 *               HPROF_GC_ROOT_STICKY_CLASS    System class
 *
 *                          id         object ID
 *
 *               HPROF_GC_ROOT_THREAD_BLOCK    Reference from thread block
 *
 *                          id         object ID
 *                          u4         thread serial number
 *
 *               HPROF_GC_ROOT_MONITOR_USED    Busy monitor
 *
 *                          id         object ID
 *
 *               HPROF_GC_CLASS_DUMP           dump of a class object
 *
 *                          id         class object ID
 *                          u4         stack trace serial number
 *                          id         super class object ID
 *                          id         class loader object ID
 *                          id         signers object ID
 *                          id         protection domain object ID
 *                          id         reserved
 *                          id         reserved
 *
 *                          u4         instance size (in bytes)
 *
 *                          u2         size of constant pool
 *                          [u2,       constant pool index,
 *                           ty,       type
 *                                     2:  object
 *                                     4:  boolean
 *                                     5:  char
 *                                     6:  float
 *                                     7:  double
 *                                     8:  byte
 *                                     9:  short
 *                                     10: int
 *                                     11: long
 *                           vl]*      and value
 *
 *                          u2         number of static fields
 *                          [id,       static field name,
 *                           ty,       type,
 *                           vl]*      and value
 *
 *                          u2         number of inst. fields (not inc. super)
 *                          [id,       instance field name,
 *                           ty]*      type
 *
 *               HPROF_GC_INSTANCE_DUMP        dump of a normal object
 *
 *                          id         object ID
 *                          u4         stack trace serial number
 *                          id         class object ID
 *                          u4         number of bytes that follow
 *                          [vl]*      instance field values (class, followed
 *                                     by super, super's super ...)
 *
 *               HPROF_GC_OBJ_ARRAY_DUMP       dump of an object array
 *
 *                          id         array object ID
 *                          u4         stack trace serial number
 *                          u4         number of elements
 *                          id         array class ID
 *                          [id]*      elements
 *
 *               HPROF_GC_PRIM_ARRAY_DUMP      dump of a primitive array
 *
 *                          id         array object ID
 *                          u4         stack trace serial number
 *                          u4         number of elements
 *                          u1         element type
 *                                     4:  boolean array
 *                                     5:  char array
 *                                     6:  float array
 *                                     7:  double array
 *                                     8:  byte array
 *                                     9:  short array
 *                                     10: int array
 *                                     11: long array
 *                          [u1]*      elements
 *
 * HPROF_CPU_SAMPLES        a set of sample traces of running threads
 *
 *                u4        total number of samples
 *                u4        # of traces
 *               [u4        # of samples
 *                u4]*      stack trace serial number
 *
 * HPROF_CONTROL_SETTINGS   the settings of on/off switches
 *
 *                u4        0x00000001: alloc traces on/off
 *                          0x00000002: cpu sampling on/off
 *                u2        stack trace depth
 *
 *
 * A heap dump can optionally be generated as a sequence of heap dump
 * segments. This sequence is terminated by an end record. The additional
 * tags allowed by format "JAVA PROFILE 1.0.2" are:
 *
 * HPROF_HEAP_DUMP_SEGMENT  denote a heap dump segment
 *
 *               [heap dump sub-records]*
 *               The same sub-record types allowed by HPROF_HEAP_DUMP
 *
 * HPROF_HEAP_DUMP_END      denotes the end of a heap dump
 *
 */

public class HeapHprofBinWriter extends AbstractHeapGraphWriter {

    private HashSet<Symbol> names;

    private static final long HPROF_SEGMENTED_HEAP_DUMP_THRESHOLD = 2L * 0x40000000;

    private static final long HPROF_SEGMENTED_HEAP_DUMP_SEGMENT_SIZE = 1L * 0x40000000;

    private static final String HPROF_HEADER_1_0_2 = "JAVA PROFILE 1.0.2";

    private static final int HPROF_UTF8             = 0x01;
    private static final int HPROF_LOAD_CLASS       = 0x02;
    private static final int HPROF_UNLOAD_CLASS     = 0x03;
    private static final int HPROF_FRAME            = 0x04;
    private static final int HPROF_TRACE            = 0x05;
    private static final int HPROF_ALLOC_SITES      = 0x06;
    private static final int HPROF_HEAP_SUMMARY     = 0x07;
    private static final int HPROF_START_THREAD     = 0x0A;
    private static final int HPROF_END_THREAD       = 0x0B;
    private static final int HPROF_HEAP_DUMP        = 0x0C;
    private static final int HPROF_CPU_SAMPLES      = 0x0D;
    private static final int HPROF_CONTROL_SETTINGS = 0x0E;

    private static final int HPROF_HEAP_DUMP_SEGMENT = 0x1C;
    private static final int HPROF_HEAP_DUMP_END     = 0x2C;

    private static final int HPROF_GC_ROOT_UNKNOWN       = 0xFF;
    private static final int HPROF_GC_ROOT_JNI_GLOBAL    = 0x01;
    private static final int HPROF_GC_ROOT_JNI_LOCAL     = 0x02;
    private static final int HPROF_GC_ROOT_JAVA_FRAME    = 0x03;
    private static final int HPROF_GC_ROOT_NATIVE_STACK  = 0x04;
    private static final int HPROF_GC_ROOT_STICKY_CLASS  = 0x05;
    private static final int HPROF_GC_ROOT_THREAD_BLOCK  = 0x06;
    private static final int HPROF_GC_ROOT_MONITOR_USED  = 0x07;
    private static final int HPROF_GC_ROOT_THREAD_OBJ    = 0x08;
    private static final int HPROF_GC_CLASS_DUMP         = 0x20;
    private static final int HPROF_GC_INSTANCE_DUMP      = 0x21;
    private static final int HPROF_GC_OBJ_ARRAY_DUMP     = 0x22;
    private static final int HPROF_GC_PRIM_ARRAY_DUMP    = 0x23;

    private static final int HPROF_ARRAY_OBJECT  = 1;
    private static final int HPROF_NORMAL_OBJECT = 2;
    private static final int HPROF_BOOLEAN       = 4;
    private static final int HPROF_CHAR          = 5;
    private static final int HPROF_FLOAT         = 6;
    private static final int HPROF_DOUBLE        = 7;
    private static final int HPROF_BYTE          = 8;
    private static final int HPROF_SHORT         = 9;
    private static final int HPROF_INT           = 10;
    private static final int HPROF_LONG          = 11;

    private static final int JVM_SIGNATURE_BOOLEAN = 'Z';
    private static final int JVM_SIGNATURE_CHAR    = 'C';
    private static final int JVM_SIGNATURE_BYTE    = 'B';
    private static final int JVM_SIGNATURE_SHORT   = 'S';
    private static final int JVM_SIGNATURE_INT     = 'I';
    private static final int JVM_SIGNATURE_LONG    = 'J';
    private static final int JVM_SIGNATURE_FLOAT   = 'F';
    private static final int JVM_SIGNATURE_DOUBLE  = 'D';
    private static final int JVM_SIGNATURE_ARRAY   = '[';
    private static final int JVM_SIGNATURE_CLASS   = 'L';

    private static final long MAX_U4_VALUE = 0xFFFFFFFFL;
    int serialNum = 1;

    public HeapHprofBinWriter() {
        this.KlassMap = new ArrayList<Klass>();
        this.names = new HashSet<Symbol>();
        this.gzLevel = 0;
    }

    public HeapHprofBinWriter(int gzLevel) {
        this.KlassMap = new ArrayList<Klass>();
        this.names = new HashSet<Symbol>();
        this.gzLevel = gzLevel;
    }

    public synchronized void write(String fileName) throws IOException {
        VM vm = VM.getVM();

        useSegmentedHeapDump = isCompression() ||
                (vm.getUniverse().heap().used() > HPROF_SEGMENTED_HEAP_DUMP_THRESHOLD);

        fos = new FileOutputStream(fileName);
        hprofBufferedOut = new BufferedOutputStream(fos);
        if (useSegmentedHeapDump) {
            if (isCompression()) {
                hprofBufferedOut = new GZIPOutputStream(hprofBufferedOut) {
                    {
                        this.def.setLevel(gzLevel);
                    }
                };
            }
        }
        out = new DataOutputStream(hprofBufferedOut);
        dbg = vm.getDebugger();
        objectHeap = vm.getObjectHeap();

        OBJ_ID_SIZE = (int) vm.getOopSize();

        BOOLEAN_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_BOOLEAN);
        BYTE_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_BYTE);
        CHAR_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_CHAR);
        SHORT_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_SHORT);
        INT_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_INT);
        LONG_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_LONG);
        FLOAT_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_FLOAT);
        DOUBLE_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_DOUBLE);
        OBJECT_BASE_OFFSET = TypeArray.baseOffsetInBytes(BasicType.T_OBJECT);

        BOOLEAN_SIZE = objectHeap.getBooleanSize();
        BYTE_SIZE = objectHeap.getByteSize();
        CHAR_SIZE = objectHeap.getCharSize();
        SHORT_SIZE = objectHeap.getShortSize();
        INT_SIZE = objectHeap.getIntSize();
        LONG_SIZE = objectHeap.getLongSize();
        FLOAT_SIZE = objectHeap.getFloatSize();
        DOUBLE_SIZE = objectHeap.getDoubleSize();

        writeFileHeader();

        writeDummyTrace();

        writeSymbols();

        writeClasses();

        dumpStackTraces();

        writeClassDumpRecords();

        super.write();

        out.flush();
        if (!useSegmentedHeapDump) {
            fillInHeapRecordLength();
        } else {
            out.writeByte((byte) HPROF_HEAP_DUMP_END);
            out.writeInt(0);
            out.writeInt(0);
        }
        out.flush();
        out.close();
        out = null;
        hprofBufferedOut = null;
        currentSegmentStart = 0;
    }

    protected int calculateOopDumpRecordSize(Oop oop) throws IOException {
        if (oop instanceof TypeArray taOop) {
            return calculatePrimitiveArrayDumpRecordSize(taOop);
        } else if (oop instanceof ObjArray oaOop) {
            Klass klass = oop.getKlass();
            ObjArrayKlass oak = (ObjArrayKlass) klass;
            Klass bottomType = oak.getBottomKlass();
            if (bottomType instanceof InstanceKlass ||
                bottomType instanceof TypeArrayKlass) {
                return calculateObjectArrayDumpRecordSize(oaOop);
            } else {
                return 0;
            }
        } else if (oop instanceof Instance instance) {
            Klass klass = instance.getKlass();
            Symbol name = klass.getName();
            if (name.equals(javaLangClass)) {
                return calculateClassInstanceDumpRecordSize(instance);
            }
            return calculateInstanceDumpRecordSize(instance);
        } else {
            return 0;
        }
    }

    private int calculateInstanceDumpRecordSize(Instance instance) {
        Klass klass = instance.getKlass();
        if (klass.getClassLoaderData() == null) {
            return 0;
        }

        ClassData cd = classDataCache.get(klass);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(cd != null, "can not get class data for " + klass.getName().asString() + klass.getAddress());
        }
        List<Field> fields = cd.fields;
        return BYTE_SIZE + OBJ_ID_SIZE * 2 + INT_SIZE * 2 + getSizeForFields(fields);
    }

    private int calculateClassDumpRecordSize(Klass k) {
        int size = BYTE_SIZE + INT_SIZE + OBJ_ID_SIZE * 2;
        if (k instanceof InstanceKlass ik) {
            List<Field> fields = getInstanceFields(ik);
            List<Field> declaredFields = ik.getImmediateFields();
            List<Field> staticFields = new ArrayList<>();
            List<Field> instanceFields = new ArrayList<>();
            Iterator<Field> itr = null;
            size += OBJ_ID_SIZE * 5 + INT_SIZE + SHORT_SIZE;
            for (itr = declaredFields.iterator(); itr.hasNext();) {
                Field field = itr.next();
                if (field.isStatic()) {
                    staticFields.add(field);
                } else {
                    instanceFields.add(field);
                }
            }
            size += calculateFieldDescriptorsDumpRecordSize(staticFields, ik);
            size += calculateFieldDescriptorsDumpRecordSize(instanceFields, null);
        } else {
            size += OBJ_ID_SIZE * 5  + INT_SIZE + SHORT_SIZE * 3;
        }
        return size;
    }

    private int calculateFieldDescriptorsDumpRecordSize(List<Field> fields, InstanceKlass ik) {
        int size = 0;
        size += SHORT_SIZE;
        for (Field field : fields) {
            size += OBJ_ID_SIZE + BYTE_SIZE;
            if (ik != null) {
                size += getSizeForField(field);
            }
        }
        return size;
    }

    private int calculateClassInstanceDumpRecordSize(Instance instance) {
        Klass reflectedKlass = java_lang_Class.asKlass(instance);
        if (reflectedKlass == null) {
            return calculateInstanceDumpRecordSize(instance);
        }
        return 0;
    }

    private int calculateObjectArrayDumpRecordSize(ObjArray array) {
        int headerSize = getArrayHeaderSize(true);
        final int length = calculateArrayMaxLength(array.getLength(),
                headerSize,
                OBJ_ID_SIZE,
                "Object");
        return headerSize + length * OBJ_ID_SIZE;
    }

    private int calculatePrimitiveArrayDumpRecordSize(TypeArray array) throws IOException {
        int headerSize = getArrayHeaderSize(false);
        TypeArrayKlass tak = (TypeArrayKlass) array.getKlass();
        final int type = tak.getElementType();
        final String typeName = tak.getElementTypeName();
        final long typeSize = getSizeForType(type);
        final int length = calculateArrayMaxLength(array.getLength(),
                                                   headerSize,
                                                   typeSize,
                                                   typeName);
        return headerSize + (int)typeSize * length;
    }

    @Override
    protected void writeHeapRecordPrologue(int size) throws IOException {
        if (size == 0 || currentSegmentStart > 0) {
            return;
        }
        if (useSegmentedHeapDump) {
            out.writeByte((byte)HPROF_HEAP_DUMP_SEGMENT);
            out.writeInt(0);
            out.writeInt(size);
        } else {
            out.writeByte((byte)HPROF_HEAP_DUMP);
            out.writeInt(0);
            out.flush();
            currentSegmentStart = fos.getChannel().position();
            out.writeInt(0);
        }
    }

    private void fillInHeapRecordLength() throws IOException {
        assert !useSegmentedHeapDump : "fillInHeapRecordLength is not supported for segmented heap dump";

        long dumpEnd = fos.getChannel().position();

        long dumpLenLong = (dumpEnd - currentSegmentStart - 4L);

        if (dumpLenLong >= (4L * 0x40000000)) {
            throw new RuntimeException("Heap segment size overflow.");
        }

        long currentPosition = fos.getChannel().position();

        fos.getChannel().position(currentSegmentStart);

        int dumpLen = (int) dumpLenLong;
        byte[] lenBytes = genByteArrayFromInt(dumpLen);
        fos.write(lenBytes);

        fos.getChannel().position(currentPosition);
    }

    private int getSizeForType(int type) throws IOException {
        switch (type) {
            case TypeArrayKlass.T_BOOLEAN:
                return BOOLEAN_SIZE;
            case TypeArrayKlass.T_INT:
                return INT_SIZE;
            case TypeArrayKlass.T_CHAR:
                return CHAR_SIZE;
            case TypeArrayKlass.T_SHORT:
                return SHORT_SIZE;
            case TypeArrayKlass.T_BYTE:
                return BYTE_SIZE;
            case TypeArrayKlass.T_LONG:
                return LONG_SIZE;
            case TypeArrayKlass.T_FLOAT:
                return FLOAT_SIZE;
            case TypeArrayKlass.T_DOUBLE:
                return DOUBLE_SIZE;
            default:
                throw new RuntimeException(
                    "Should not reach here: Unknown type: " + type);
         }
    }

    private int getArrayHeaderSize(boolean isObjectAarray) {
        return isObjectAarray?
            (BYTE_SIZE + 2 * INT_SIZE + 2 * OBJ_ID_SIZE):
            (2 * BYTE_SIZE + 2 * INT_SIZE + OBJ_ID_SIZE);
    }

    private int calculateArrayMaxLength(long originalArrayLength,
                                        int headerSize,
                                        long typeSize,
                                        String typeName) {

        long length = originalArrayLength;

        long originalLengthInBytes = originalArrayLength * typeSize;

        long maxBytes = MAX_U4_VALUE - headerSize;

        if (originalLengthInBytes > maxBytes) {
            length = maxBytes/typeSize;
            System.err.println("WARNING: Cannot dump array of type " + typeName
                               + " with length " + originalArrayLength
                               + "; truncating to length " + length);
        }
        return (int) length;
    }

    private void writeClassDumpRecords() throws IOException {
        ClassLoaderDataGraph cldGraph = VM.getVM().getClassLoaderDataGraph();
        try {
             cldGraph.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                            public void visit(Klass k) {
                                try {
                                    writeHeapRecordPrologue(calculateClassDumpRecordSize(k));
                                    writeClassDumpRecord(k);
                                    writeHeapRecordEpilogue();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    protected void writeClass(Instance instance) throws IOException {
        Klass reflectedKlass = java_lang_Class.asKlass(instance);
        if (reflectedKlass == null) {
            writeInstance(instance);
        }
    }

    private void writeClassDumpRecord(Klass k) throws IOException {
        out.writeByte((byte)HPROF_GC_CLASS_DUMP);
        writeObjectID(k.getJavaMirror());
        out.writeInt(DUMMY_STACK_TRACE_ID);
        Klass superKlass = k.getJavaSuper();
        if (superKlass != null) {
            writeObjectID(superKlass.getJavaMirror());
        } else {
            writeObjectID(null);
        }

        if (k instanceof InstanceKlass) {
            InstanceKlass ik = (InstanceKlass) k;
            writeObjectID(ik.getClassLoader());
            writeObjectID(null);  
            writeObjectID(null);  
            writeObjectID(null);
            writeObjectID(null);
            List<Field> fields = getInstanceFields(ik);
            int instSize = getSizeForFields(fields);
            classDataCache.put(ik, new ClassData(instSize, fields));
            out.writeInt(instSize);

            out.writeShort((short) 0);

            List<Field> declaredFields = ik.getImmediateFields();
            List<Field> staticFields = new ArrayList<>();
            List<Field> instanceFields = new ArrayList<>();
            Iterator<Field> itr = null;
            for (itr = declaredFields.iterator(); itr.hasNext();) {
                Field field = itr.next();
                if (field.isStatic()) {
                    staticFields.add(field);
                } else {
                    instanceFields.add(field);
                }
            }

            writeFieldDescriptors(staticFields, ik);

            writeFieldDescriptors(instanceFields, null);
        } else {
            if (k instanceof ObjArrayKlass) {
                ObjArrayKlass oak = (ObjArrayKlass) k;
                Klass bottomKlass = oak.getBottomKlass();
                if (bottomKlass instanceof InstanceKlass) {
                    InstanceKlass ik = (InstanceKlass) bottomKlass;
                    writeObjectID(ik.getClassLoader());
                    writeObjectID(null); 
                    writeObjectID(null); 
                } else {
                    writeObjectID(null);
                    writeObjectID(null);
                    writeObjectID(null);
                }
            } else {
                writeObjectID(null);
                writeObjectID(null);
                writeObjectID(null);
            }
            writeObjectID(null);
            writeObjectID(null);
            out.writeInt(0);
            out.writeShort((short) 0);
            out.writeShort((short) 0);
            out.writeShort((short) 0);
        }
    }

    private void dumpStackTraces() throws IOException {
        writeHeader(HPROF_TRACE, 3 * INT_SIZE );
        out.writeInt(DUMMY_STACK_TRACE_ID);
        out.writeInt(0);                    
        out.writeInt(0);                    

        int frameSerialNum = 0;
        int numThreads = 0;
        Threads threads = VM.getVM().getThreads();
        for (int i = 0; i < threads.getNumberOfThreads(); i++) {
            JavaThread thread = threads.getJavaThreadAt(i);
            Oop threadObj = thread.getThreadObj();
            if (threadObj != null && !thread.isExiting() && !thread.isHiddenFromExternalView()) {

                ThreadStackTrace st = new ThreadStackTrace(thread);
                st.dumpStack(-1);
                numThreads++;

                int depth = st.getStackDepth();
                int threadFrameStart = frameSerialNum;
                for (int j=0; j < depth; j++) {
                    StackFrameInfo frame = st.stackFrameAt(j);
                    Method m = frame.getMethod();
                    int classSerialNum = KlassMap.indexOf(m.getMethodHolder()) + 1;
                    assert classSerialNum > 0:"class not found";
                    dumpStackFrame(++frameSerialNum, classSerialNum, m, frame.getBCI());
                }

                writeHeader(HPROF_TRACE, 3 * INT_SIZE + depth * OBJ_ID_SIZE);
                int stackSerialNum = numThreads + DUMMY_STACK_TRACE_ID;
                out.writeInt(stackSerialNum);      
                out.writeInt(numThreads);          
                out.writeInt(depth);               
                for (int j=1; j <= depth; j++) {
                    writeObjectID(threadFrameStart + j);
                }
            }
        }
    }

    private void dumpStackFrame(int frameSN, int classSN, Method m, int bci) throws IOException {
        int lineNumber;
        if (m.isNative()) {
            lineNumber = -3; 
        } else {
            lineNumber = m.getLineNumberFromBCI(bci);
        }
        writeSymbol(m.getName());                              
        writeSymbol(m.getSignature());                         
        writeSymbol(m.getMethodHolder().getSourceFileName());  
        writeHeader(HPROF_FRAME, 4 * OBJ_ID_SIZE + 2 * INT_SIZE);
        writeObjectID(frameSN);                                  
        writeSymbolID(m.getName());                              
        writeSymbolID(m.getSignature());                         
        writeSymbolID(m.getMethodHolder().getSourceFileName());  
        out.writeInt(classSN);                                   
        out.writeInt(lineNumber);                                
    }

    protected void writeJavaThread(JavaThread jt, int index) throws IOException {
        int size = BYTE_SIZE + OBJ_ID_SIZE + INT_SIZE * 2;
        writeHeapRecordPrologue(size);
        out.writeByte((byte) HPROF_GC_ROOT_THREAD_OBJ);
        writeObjectID(jt.getThreadObj());
        out.writeInt(index);
        out.writeInt(DUMMY_STACK_TRACE_ID);
        writeLocalJNIHandles(jt, index);
    }

    protected void writeLocalJNIHandles(JavaThread jt, int index) throws IOException {
        final int threadIndex = index;
        JNIHandleBlock blk = jt.activeHandles();
        if (blk != null) {
            try {
                blk.oopsDo(new AddressVisitor() {
                           public void visitAddress(Address handleAddr) {
                               try {
                                   if (handleAddr != null) {
                                       OopHandle oopHandle = handleAddr.getOopHandleAt(0);
                                       Oop oop = objectHeap.newOop(oopHandle);
                                       if (oop != null && isJavaVisible(oop)) {
                                           int size = BYTE_SIZE + OBJ_ID_SIZE + INT_SIZE * 2;
                                           writeHeapRecordPrologue(size);
                                           out.writeByte((byte) HPROF_GC_ROOT_JNI_LOCAL);
                                           writeObjectID(oop);
                                           out.writeInt(threadIndex);
                                           out.writeInt(EMPTY_FRAME_DEPTH);
                                       }
                                   }
                               } catch (IOException exp) {
                                   throw new RuntimeException(exp);
                               }
                           }
                           public void visitCompOopAddress(Address handleAddr) {
                             throw new RuntimeException(
                                   " Should not reach here. JNIHandles are not compressed \n");
                           }
                       });
            } catch (RuntimeException re) {
                handleRuntimeException(re);
            }
        }
    }

    protected void writeGlobalJNIHandle(Address handleAddr) throws IOException {
        OopHandle oopHandle = handleAddr.getOopHandleAt(0);
        Oop oop = objectHeap.newOop(oopHandle);
        if (oop != null && isJavaVisible(oop)) {
            int size = BYTE_SIZE + OBJ_ID_SIZE * 2;
            writeHeapRecordPrologue(size);
            out.writeByte((byte) HPROF_GC_ROOT_JNI_GLOBAL);
            writeObjectID(oop);
            writeObjectID(getAddressValue(handleAddr));
        }
    }

    protected void writeObjectArray(ObjArray array) throws IOException {
        int headerSize = getArrayHeaderSize(true);
        final int length = calculateArrayMaxLength(array.getLength(),
                                                   headerSize,
                                                   OBJ_ID_SIZE,
                                                   "Object");
        out.writeByte((byte) HPROF_GC_OBJ_ARRAY_DUMP);
        writeObjectID(array);
        out.writeInt(DUMMY_STACK_TRACE_ID);
        out.writeInt(length);
        writeObjectID(array.getKlass().getJavaMirror());
        for (int index = 0; index < length; index++) {
            OopHandle handle = array.getOopHandleAt(index);
            writeObjectID(getAddressValue(handle));
        }
    }

    protected void writePrimitiveArray(TypeArray array) throws IOException {
        int headerSize = getArrayHeaderSize(false);
        TypeArrayKlass tak = (TypeArrayKlass) array.getKlass();
        final int type = tak.getElementType();
        final String typeName = tak.getElementTypeName();
        final long typeSize = getSizeForType(type);
        final int length = calculateArrayMaxLength(array.getLength(),
                                                   headerSize,
                                                   typeSize,
                                                   typeName);
        out.writeByte((byte) HPROF_GC_PRIM_ARRAY_DUMP);
        writeObjectID(array);
        out.writeInt(DUMMY_STACK_TRACE_ID);
        out.writeInt(length);
        out.writeByte((byte) type);
        switch (type) {
            case TypeArrayKlass.T_BOOLEAN:
                writeBooleanArray(array, length);
                break;
            case TypeArrayKlass.T_CHAR:
                writeCharArray(array, length);
                break;
            case TypeArrayKlass.T_FLOAT:
                writeFloatArray(array, length);
                break;
            case TypeArrayKlass.T_DOUBLE:
                writeDoubleArray(array, length);
                break;
            case TypeArrayKlass.T_BYTE:
                writeByteArray(array, length);
                break;
            case TypeArrayKlass.T_SHORT:
                writeShortArray(array, length);
                break;
            case TypeArrayKlass.T_INT:
                writeIntArray(array, length);
                break;
            case TypeArrayKlass.T_LONG:
                writeLongArray(array, length);
                break;
            default:
                throw new RuntimeException(
                    "Should not reach here: Unknown type: " + type);
        }
    }

    private void writeBooleanArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = BOOLEAN_BASE_OFFSET + index * BOOLEAN_SIZE;
             out.writeBoolean(array.getHandle().getJBooleanAt(offset));
        }
    }

    private void writeByteArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = BYTE_BASE_OFFSET + index * BYTE_SIZE;
             out.writeByte(array.getHandle().getJByteAt(offset));
        }
    }

    private void writeShortArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = SHORT_BASE_OFFSET + index * SHORT_SIZE;
             out.writeShort(array.getHandle().getJShortAt(offset));
        }
    }

    private void writeIntArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = INT_BASE_OFFSET + index * INT_SIZE;
             out.writeInt(array.getHandle().getJIntAt(offset));
        }
    }

    private void writeLongArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = LONG_BASE_OFFSET + index * LONG_SIZE;
             out.writeLong(array.getHandle().getJLongAt(offset));
        }
    }

    private void writeCharArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = CHAR_BASE_OFFSET + index * CHAR_SIZE;
             out.writeChar(array.getHandle().getJCharAt(offset));
        }
    }

    private void writeFloatArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = FLOAT_BASE_OFFSET + index * FLOAT_SIZE;
             out.writeFloat(array.getHandle().getJFloatAt(offset));
        }
    }

    private void writeDoubleArray(TypeArray array, int length) throws IOException {
        for (int index = 0; index < length; index++) {
             long offset = DOUBLE_BASE_OFFSET + index * DOUBLE_SIZE;
             out.writeDouble(array.getHandle().getJDoubleAt(offset));
        }
    }

    protected void writeInstance(Instance instance) throws IOException {
        Klass klass = instance.getKlass();
        if (klass.getClassLoaderData() == null) {
            return;
        }

        out.writeByte((byte) HPROF_GC_INSTANCE_DUMP);
        writeObjectID(instance);
        out.writeInt(DUMMY_STACK_TRACE_ID);
        writeObjectID(klass.getJavaMirror());

        ClassData cd = classDataCache.get(klass);

        if (Assert.ASSERTS_ENABLED) {
            Assert.that(cd != null, "can not get class data for " + klass.getName().asString() + klass.getAddress());
        }
        List<Field> fields = cd.fields;
        int size = cd.instSize;
        out.writeInt(size);
        for (Iterator<Field> itr = fields.iterator(); itr.hasNext();) {
            writeField(itr.next(), instance);
        }
    }


    private void writeFieldDescriptors(List<Field> fields, InstanceKlass ik)
        throws IOException {
        out.writeShort((short) fields.size());
        for (Iterator<Field> itr = fields.iterator(); itr.hasNext();) {
            Field field = itr.next();
            Symbol name = field.getName();
            writeSymbolID(name);
            char typeCode = (char) field.getSignature().getByteAt(0);
            int kind = signatureToHprofKind(typeCode);
            out.writeByte((byte)kind);
            if (ik != null) {
                writeField(field, ik.getJavaMirror());
            }
        }
    }

    public static int signatureToHprofKind(char ch) {
        switch (ch) {
        case JVM_SIGNATURE_CLASS:
        case JVM_SIGNATURE_ARRAY:
            return HPROF_NORMAL_OBJECT;
        case JVM_SIGNATURE_BOOLEAN:
            return HPROF_BOOLEAN;
        case JVM_SIGNATURE_CHAR:
            return HPROF_CHAR;
        case JVM_SIGNATURE_FLOAT:
            return HPROF_FLOAT;
        case JVM_SIGNATURE_DOUBLE:
            return HPROF_DOUBLE;
        case JVM_SIGNATURE_BYTE:
            return HPROF_BYTE;
        case JVM_SIGNATURE_SHORT:
            return HPROF_SHORT;
        case JVM_SIGNATURE_INT:
            return HPROF_INT;
        case JVM_SIGNATURE_LONG:
            return HPROF_LONG;
        default:
            throw new RuntimeException("should not reach here");
        }
    }

    private void writeField(Field field, Oop oop) throws IOException {
        char typeCode = (char) field.getSignature().getByteAt(0);
        switch (typeCode) {
        case JVM_SIGNATURE_BOOLEAN:
            out.writeBoolean(((BooleanField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_CHAR:
            out.writeChar(((CharField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_BYTE:
            out.writeByte(((ByteField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_SHORT:
            out.writeShort(((ShortField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_INT:
            out.writeInt(((IntField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_LONG:
            out.writeLong(((LongField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_FLOAT:
            out.writeFloat(((FloatField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_DOUBLE:
            out.writeDouble(((DoubleField)field).getValue(oop));
            break;
        case JVM_SIGNATURE_CLASS:
        case JVM_SIGNATURE_ARRAY: {
            if (VM.getVM().isCompressedOopsEnabled()) {
              OopHandle handle = ((NarrowOopField)field).getValueAsOopHandle(oop);
              writeObjectID(getAddressValue(handle));
            } else {
              OopHandle handle = ((OopField)field).getValueAsOopHandle(oop);
              writeObjectID(getAddressValue(handle));
            }
            break;
        }
        default:
            throw new RuntimeException("should not reach here");
        }
    }

    private void writeHeader(int tag, int len) throws IOException {
        out.writeByte((byte)tag);
        out.writeInt(0); 
        out.writeInt(len);
    }

    private void writeDummyTrace() throws IOException {
        writeHeader(HPROF_TRACE, 3 * 4);
        out.writeInt(DUMMY_STACK_TRACE_ID);
        out.writeInt(0);
        out.writeInt(0);
    }

    private void writeClassSymbols(Klass k) throws IOException {
        writeSymbol(k.getName());
        if (k instanceof InstanceKlass) {
            InstanceKlass ik = (InstanceKlass) k;
            List<Field> declaredFields = ik.getImmediateFields();
            for (Iterator<Field> itr = declaredFields.iterator(); itr.hasNext();) {
                Field field = itr.next();
                writeSymbol(field.getName());
            }
        }
    }

    private void writeSymbols() throws IOException {
        ClassLoaderDataGraph cldGraph = VM.getVM().getClassLoaderDataGraph();
        try {
             cldGraph.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                            public void visit(Klass k) {
                                try {
                                    writeClassSymbols(k);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    private void writeSymbol(Symbol sym) throws IOException {
        if (names.add(sym)) {
            if(sym != null) {
              byte[] buf = sym.asString().getBytes(UTF_8);
              writeHeader(HPROF_UTF8, buf.length + OBJ_ID_SIZE);
              writeSymbolID(sym);
              out.write(buf);
           } else {
              writeHeader(HPROF_UTF8, 0 + OBJ_ID_SIZE);
              writeSymbolID(null);
           }
        }
    }

    private void writeClasses() throws IOException {
        ClassLoaderDataGraph cldGraph = VM.getVM().getClassLoaderDataGraph();
        try {
            cldGraph.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                public void visit(Klass k) {
                    try {
                        Instance clazz = k.getJavaMirror();
                        writeHeader(HPROF_LOAD_CLASS, 2 * (OBJ_ID_SIZE + 4));
                        out.writeInt(serialNum);
                        writeObjectID(clazz);
                        KlassMap.add(serialNum - 1, k);
                        out.writeInt(DUMMY_STACK_TRACE_ID);
                        writeSymbolID(k.getName());
                        serialNum++;
                    } catch (IOException exp) {
                        throw new RuntimeException(exp);
                    }
                }
            });
        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    private void writeFileHeader() throws IOException {
        out.writeBytes(HPROF_HEADER_1_0_2);
        out.writeByte((byte)'\0');

        out.writeInt(OBJ_ID_SIZE);

        out.writeLong(System.currentTimeMillis());
    }

    private void writeObjectID(Oop oop) throws IOException {
        OopHandle handle = (oop != null)? oop.getHandle() : null;
        long address = getAddressValue(handle);
        writeObjectID(address);
    }

    private void writeSymbolID(Symbol sym) throws IOException {
        assert names.contains(sym);
        long address = (sym != null) ? getAddressValue(sym.getAddress()) : getAddressValue(null);
        writeObjectID(address);
    }

    private void writeObjectID(long address) throws IOException {
        if (OBJ_ID_SIZE == 4) {
            out.writeInt((int) address);
        } else {
            out.writeLong(address);
        }
    }

    private long getAddressValue(Address addr) {
        return (addr == null)? 0L : dbg.getAddressValue(addr);
    }

    private static List<Field> getInstanceFields(InstanceKlass ik) {
        InstanceKlass klass = ik;
        List<Field> res = new ArrayList<>();
        while (klass != null) {
            List<Field> curFields = klass.getImmediateFields();
            for (Iterator<Field> itr = curFields.iterator(); itr.hasNext();) {
                Field f = itr.next();
                if (! f.isStatic()) {
                    res.add(f);
                }
            }
            klass = (InstanceKlass) klass.getSuper();
        }
        return res;
    }

    private int getSizeForField(Field field) {
        char typeCode = (char)field.getSignature().getByteAt(0);
        switch (typeCode) {
        case JVM_SIGNATURE_BOOLEAN:
        case JVM_SIGNATURE_BYTE:
            return 1;
        case JVM_SIGNATURE_CHAR:
        case JVM_SIGNATURE_SHORT:
            return 2;
        case JVM_SIGNATURE_INT:
        case JVM_SIGNATURE_FLOAT:
            return 4;
        case JVM_SIGNATURE_CLASS:
        case JVM_SIGNATURE_ARRAY:
            return OBJ_ID_SIZE;
        case JVM_SIGNATURE_LONG:
        case JVM_SIGNATURE_DOUBLE:
            return 8;
        default:
            throw new RuntimeException("should not reach here");
        }
    }

    private int getSizeForFields(List<Field> fields) {
        int size = 0;
        for (Field field : fields) {
            size += getSizeForField(field);
        }
        return size;
    }

    private boolean isCompression() {
        return (gzLevel >= 1 && gzLevel <= 9);
    }

    private static byte[] genByteArrayFromInt(int value) {
        ByteBuffer intBuffer = ByteBuffer.allocate(4);
        intBuffer.order(ByteOrder.BIG_ENDIAN);
        intBuffer.putInt(value);
        return intBuffer.array();
    }

    private static final int DUMMY_STACK_TRACE_ID = 1;
    private static final int EMPTY_FRAME_DEPTH = -1;

    private DataOutputStream out;
    private FileOutputStream fos;
    private OutputStream hprofBufferedOut;
    private Debugger dbg;
    private ObjectHeap objectHeap;
    private ArrayList<Klass> KlassMap;
    private int gzLevel;

    private int OBJ_ID_SIZE;

    private boolean useSegmentedHeapDump;
    private long currentSegmentStart;

    private long BOOLEAN_BASE_OFFSET;
    private long BYTE_BASE_OFFSET;
    private long CHAR_BASE_OFFSET;
    private long SHORT_BASE_OFFSET;
    private long INT_BASE_OFFSET;
    private long LONG_BASE_OFFSET;
    private long FLOAT_BASE_OFFSET;
    private long DOUBLE_BASE_OFFSET;
    private long OBJECT_BASE_OFFSET;

    private int BOOLEAN_SIZE;
    private int BYTE_SIZE;
    private int CHAR_SIZE;
    private int SHORT_SIZE;
    private int INT_SIZE;
    private int LONG_SIZE;
    private int FLOAT_SIZE;
    private int DOUBLE_SIZE;

    private static class ClassData {
        int instSize;
        List<Field> fields;

        ClassData(int instSize, List<Field> fields) {
            this.instSize = instSize;
            this.fields = fields;
        }
    }

    private Map<InstanceKlass, ClassData> classDataCache = new HashMap<>();
}
