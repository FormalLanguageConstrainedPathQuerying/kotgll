/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serialize;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import com.sun.org.apache.xerces.internal.util.EncodingMap;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * This class represents an encoding.
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 */
@Deprecated
public class EncodingInfo {

    String ianaName;
    String javaName;
    int lastPrintable;

    CharsetEncoder fCharsetEncoder = null;

    boolean fHaveTriedCharsetEncoder = false;

    /**
     * Creates new <code>EncodingInfo</code> instance.
     */
    public EncodingInfo(String ianaName, String javaName, int lastPrintable) {
        this.ianaName = ianaName;
        this.javaName = EncodingMap.getIANA2JavaMapping(ianaName);
        this.lastPrintable = lastPrintable;
    }

    /**
     * Returns a MIME charset name of this encoding.
     */
    public String getIANAName() {
        return this.ianaName;
    }

    /**
     * Returns a writer for this encoding based on
     * an output stream.
     *
     * @return A suitable writer
     * @exception UnsupportedEncodingException There is no convertor
     *  to support this encoding
     */
    public Writer getWriter(OutputStream output)
        throws UnsupportedEncodingException {
        if (javaName != null)
            return new OutputStreamWriter(output, javaName);
        javaName = EncodingMap.getIANA2JavaMapping(ianaName);
        if(javaName == null)
            return new OutputStreamWriter(output, "UTF8");
        return new OutputStreamWriter(output, javaName);
    }

    /**
     * Checks whether the specified character is printable or not in this encoding.
     *
     * @param ch a code point (0-0x10ffff)
     */
    public boolean isPrintable(char ch) {
        if (ch <= this.lastPrintable) {
            return true;
        }
        return isPrintable0(ch);
    }

    /**
     * Checks whether the specified character is printable or not in this encoding.
     * This method accomplishes this using a java.nio.CharsetEncoder. If NIO isn't
     * available it will attempt use a sun.io.CharToByteConverter.
     *
     * @param ch a code point (0-0x10ffff)
     */
    private boolean isPrintable0(char ch) {

        if (fCharsetEncoder == null && !fHaveTriedCharsetEncoder) {
            try {
                Charset charset = java.nio.charset.Charset.forName(javaName);
                if (charset.canEncode()) {
                    fCharsetEncoder = charset.newEncoder();
                }
                else {
                    fHaveTriedCharsetEncoder = true;
                }
            }
            catch (Exception e) {
                fHaveTriedCharsetEncoder = true;
            }
        }
        if (fCharsetEncoder != null) {
            try {
                return fCharsetEncoder.canEncode(ch);
            }
            catch (Exception e) {
                fCharsetEncoder = null;
                fHaveTriedCharsetEncoder = false;
            }
        }

        return false;
    }

    public static void testJavaEncodingName(String name)  throws UnsupportedEncodingException {
        final byte [] bTest = {(byte)'v', (byte)'a', (byte)'l', (byte)'i', (byte)'d'};
        String s = new String(bTest, name);
    }

}
