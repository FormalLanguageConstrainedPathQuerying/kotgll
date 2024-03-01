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
import java.util.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

/**
 * <p>This class writes Java heap in Graph eXchange Language (GXL)
 * format. GXL is an open standard for serializing arbitrary graphs in
 * XML syntax.</p>
 *
 * <p>A GXL document contains one or more graphs. A graph contains
 * nodes and edges. Both nodes and edges can have attributes. graphs,
 * nodes, edges and attributes are represented by XML elements graph,
 * node, edge and attr respectively. Attributes can be typed. GXL
 * supports locator, bool, int, float, bool, string, enum as well as
 * set, seq, bag, tup types. Nodes must have a XML attribute 'id' that
 * is unique id of the node in the GXL document. Edges must have
 * 'from' and 'to' XML attributes that are ids of from and to nodes.</p>
 *
 * <p>Java heap to GXL document mapping:</p>
 * <ul>
 * <li>Java object - GXL node.
 * <li>Java primitive field - GXL attribute (type mapping below).
 * <li>Java reference field - GXL edge from referee to referent node.
 * <li>Java primitive array - GXL node with seq type attribute.
 * <li>Java char array - GXL node with one attribute of string type.
 * <li>Java object array - GXL node and 'length' edges.
 * </ul>
 *
 * <p>Java primitive to GXL type mapping:</p>
 * <ul>
 * <li>Java byte, int, short, long - GXL int attribute
 * <li>Java float, double - GXL float attribute
 * <li>Java boolean - GXL bool attribute
 * <li>Java char - GXL string attribute
 * </ul>
 *
 * Exact Java primitive type code is written in 'kind' attribute of
 * 'attr' element.  Type code is specified in JVM spec. second edition
 * section 4.3.2 (Field Descriptor).
 *
 * @see <a href="http:
 * @see <a href="http:
 */

public class HeapGXLWriter extends AbstractHeapGraphWriter {
    public void write(String fileName) throws IOException {
        out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        super.write();
        if (out.checkError()) {
            throw new IOException();
        }
        out.flush();
    }

    protected void writeHeapHeader() throws IOException {
        out.print("<?xml version='1.0' encoding='");
        out.print(ENCODING);
        out.println("'?>");

        out.println("<gxl>");
        out.println("<graph id='JavaHeap'>");

        writeAttribute("creation-date", "string", new Date().toString());

        writeVMInfo();

        out.print("<node id='");
        out.print(getID(null));
        out.println("'/>");
    }

    protected void writeObjectHeader(Oop oop) throws IOException  {
        refFields = new ArrayList<>();
        isArray = oop.isArray();

        writeEdge(oop, oop.getKlass().getJavaMirror(), "instanceof");

        out.print("<node id='");
        out.print(getID(oop));
        out.println("'>");
    }

    protected void writeObjectFooter(Oop oop) throws IOException  {
        out.println("</node>");

        for (Iterator itr = refFields.iterator(); itr.hasNext();) {
            OopField field = (OopField) itr.next();
            Oop ref = field.getValue(oop);

            String name = field.getID().getName();
            if (isArray) {
                name = "element" + name;
            } else {
                name = identifierToXMLName(name);
            }
            writeEdge(oop, ref, name);
        }
        refFields = null;
    }

    protected void writeObjectArray(ObjArray array) throws IOException {
        writeObjectHeader(array);
        writeArrayLength(array);
        writeObjectFields(array);
        writeObjectFooter(array);
    }

    protected void writePrimitiveArray(TypeArray array)
        throws IOException  {
        writeObjectHeader(array);
        writeArrayLength(array);
        out.println("\t<attr name='elements'>");
        TypeArrayKlass klass = (TypeArrayKlass) array.getKlass();
        if (klass.getElementType() == TypeArrayKlass.T_CHAR) {
            out.print("\t<string>");
            out.print(escapeXMLChars(OopUtilities.charArrayToString(array)));
            out.println("</string>");
        } else {
            out.println("\t<seq>");
            writeObjectFields(array);
            out.println("\t</seq>");
        }
        out.println("\t</attr>");
        writeObjectFooter(array);
    }

    protected void writeClass(Instance instance) throws IOException  {
        writeObjectHeader(instance);
        Klass reflectedType = java_lang_Class.asKlass(instance);
        boolean isInstanceKlass = (reflectedType instanceof InstanceKlass);
        if (reflectedType != null) {
            Symbol name = reflectedType.getName();
            if (name != null) {
                writeAttribute("class-name", "string", name.asString());
            }
            if (isInstanceKlass) {
                long sizeInBytes = reflectedType.getLayoutHelper();
                writeAttribute("object-size", "int",
                               Long.toString(sizeInBytes));
                writeObjectFields((InstanceKlass)reflectedType);
            }
        }
        out.println("</node>");

        if (reflectedType != null) {
            Klass superType = reflectedType.getSuper();
            Oop superMirror = (superType == null)?
                              null : superType.getJavaMirror();
            writeEdge(instance, superMirror, "extends");
            if (isInstanceKlass) {
                InstanceKlass ik = (InstanceKlass) reflectedType;
                KlassArray interfaces = ik.getLocalInterfaces();
                final int len = interfaces.length();
                for (int i = 0; i < len; i++) {
                    Klass k = interfaces.getAt(i);
                    writeEdge(instance, k.getJavaMirror(), "implements");
                }

                Oop loader = ik.getClassLoader();
                writeEdge(instance, loader, "loaded-by");

                writeEdge(instance, null, "signed-by");

                writeEdge(instance, null, "protection-domain");

                for (Iterator itr = refFields.iterator(); itr.hasNext();) {
                    OopField field = (OopField) itr.next();
                    Oop ref = field.getValue(reflectedType);
                    String name = field.getID().getName();
                    writeEdge(instance, ref, identifierToXMLName(name));
                }
            }
        }
        refFields = null;
    }

    protected void writeReferenceField(Oop oop, OopField field)
        throws IOException {
        refFields.add(field);
    }

    protected void writeByteField(Oop oop, ByteField field)
        throws IOException {
        writeField(field, "int", "B", Byte.toString(field.getValue(oop)));
    }

    protected void writeCharField(Oop oop, CharField field)
        throws IOException {
        writeField(field, "string", "C",
                   escapeXMLChars(Character.toString(field.getValue(oop))));
    }

    protected void writeBooleanField(Oop oop, BooleanField field)
        throws IOException {
        writeField(field, "bool", "Z", Boolean.toString(field.getValue(oop)));
    }

    protected void writeShortField(Oop oop, ShortField field)
        throws IOException {
        writeField(field, "int", "S", Short.toString(field.getValue(oop)));
    }

    protected void writeIntField(Oop oop, IntField field)
        throws IOException {
        writeField(field, "int", "I", Integer.toString(field.getValue(oop)));
    }

    protected void writeLongField(Oop oop, LongField field)
        throws IOException {
        writeField(field, "int", "J", Long.toString(field.getValue(oop)));
    }

    protected void writeFloatField(Oop oop, FloatField field)
        throws IOException {
        writeField(field, "float", "F", Float.toString(field.getValue(oop)));
    }

    protected void writeDoubleField(Oop oop, DoubleField field)
        throws IOException {
        writeField(field, "float", "D", Double.toString(field.getValue(oop)));
    }

    protected void writeHeapFooter() throws IOException  {
        out.println("</graph>");
        out.println("</gxl>");
    }

    @Override
    protected int calculateOopDumpRecordSize(Oop oop) throws IOException {
        return 0;
    }


    private static String identifierToXMLName(String name) {
        return name.replace('$', '_');
    }

    private static String escapeXMLChars(String s) {
        StringBuilder result = null;
        for(int i = 0, max = s.length(), delta = 0; i < max; i++) {
            char c = s.charAt(i);
            String replacement = null;
            if (c == '&') {
                replacement = "&amp;";
            } else if (c == '<') {
                replacement = "&lt;";
            } else if (c == '>') {
                replacement = "&gt;";
            } else if (c == '"') {
                replacement = "&quot;";
            } else if (c == '\'') {
                replacement = "&apos;";
            } else if (c <  '\u0020' || (c > '\ud7ff' && c < '\ue000') ||
                       c == '\ufffe' || c == '\uffff') {
                replacement = "<![CDATA[&#x" +
                    Integer.toHexString((int)c) + ";]]>";
            }

            if (replacement != null) {
                if (result == null) {
                    result = new StringBuilder(s);
                }
                result.replace(i + delta, i + delta + 1, replacement);
                delta += (replacement.length() - 1);
            }
        }
        if (result == null) {
            return s;
        }
        return result.toString();
    }

    private static String getID(Oop oop) {
        if (oop == null) {
            return "ID_NULL";
        } else {
            return "ID_" + oop.getHandle().toString();
        }
    }

    private void writeArrayLength(Array array) throws IOException {
        writeAttribute("length", "int",
                       Integer.toString((int) array.getLength()));
    }

    private void writeAttribute(String name, String type, String value) {
        out.print("\t<attr name='");
        out.print(name);
        out.print("'><");
        out.print(type);
        out.print('>');
        out.print(value);
        out.print("</");
        out.print(type);
        out.println("></attr>");
    }

    private void writeEdge(Oop from, Oop to, String name) throws IOException {
        out.print("<edge from='");
        out.print(getID(from));
        out.print("' to='");
        out.print(getID(to));
        out.println("'>");
        writeAttribute("name", "string", name);
        out.println("</edge>");
    }

    private void writeField(Field field, String type, String kind,
                            String value) throws IOException  {
        if (isArray) {
            out.print('\t');
        } else {
            out.print("\t<attr name='");
            String name = field.getID().getName();
            out.print(identifierToXMLName(name));
            out.print("' kind='");
            out.print(kind);
            out.print("'>");
        }
        out.print('<');
        out.print(type);
        out.print('>');
        out.print(value);
        out.print("</");
        out.print(type);
        out.print('>');
        if (isArray) {
            out.println();
        } else {
            out.println("</attr>");
        }
    }

    private void writeVMInfo() throws IOException {
        VM vm = VM.getVM();
        writeAttribute("vm-version", "string", vm.getVMRelease());
        writeAttribute("vm-type", "string",
                       (vm.isClientCompiler())? "client" :
                       ((vm.isServerCompiler())? "server" : "core"));
        writeAttribute("os", "string", vm.getOS());
        writeAttribute("cpu", "string", vm.getCPU());
        writeAttribute("pointer-size", "string",
                       Integer.toString((int)vm.getOopSize() * 8));
    }

    private static final String ENCODING = "UTF-8";

    private List<OopField> refFields;
    private boolean isArray;
    private PrintWriter out;
}
