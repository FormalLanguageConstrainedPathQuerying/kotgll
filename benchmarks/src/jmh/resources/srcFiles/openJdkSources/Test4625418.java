/*
 * Copyright (c) 2006, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4625418 8239965 8290488
 * @summary Tests XML <a href="http:
 * @author Sergey Malenkov
 * @run main/timeout=360 Test4625418
 */

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

public final class Test4625418 implements ExceptionListener {

    private static final String[] encodings = {
        "ASCII",
        "Big5",
        "Big5_Solaris",
        "Cp1006",
        "Cp1046",
        "Cp1047",
        "Cp1097",
        "Cp1098",
        "Cp1124",
        "Cp1250",
        "Cp1251",
        "Cp1252",
        "Cp1253",
        "Cp1254",
        "Cp1255",
        "Cp1256",
        "Cp1257",
        "Cp1258",
        "Cp437",
        "Cp737",
        "Cp775",
        "Cp850",
        "Cp852",
        "Cp855",
        "Cp856",
        "Cp857",
        "Cp858",
        "Cp860",
        "Cp861",
        "Cp862",
        "Cp863",
        "Cp865",
        "Cp866",
        "Cp868",
        "Cp869",
        "Cp874",
        "Cp921",
        "Cp922",
        "Cp933",
        "Cp948",
        "Cp949",
        "Cp950",
        "Cp964",
        "EUC-KR",
        "EUC_CN",
        "EUC_KR",
        "GB18030",
        "GB2312",
        "GBK",
        "IBM00858",
        "IBM1047",
        "IBM437",
        "IBM775",
        "IBM850",
        "IBM852",
        "IBM855",
        "IBM857",
        "IBM860",
        "IBM861",
        "IBM862",
        "IBM863",
        "IBM865",
        "IBM866",
        "IBM868",
        "IBM869",
        "ISO-2022-JP",
        "ISO-2022-KR",
        "ISO-8859-1",
        "ISO-8859-13",
        "ISO-8859-15",
        "ISO-8859-2",
        "ISO-8859-3",
        "ISO-8859-4",
        "ISO-8859-5",
        "ISO-8859-6",
        "ISO-8859-7",
        "ISO-8859-8",
        "ISO-8859-9",
        "ISO2022JP",
        "ISO2022KR",
        "ISO8859_1",
        "ISO8859_13",
        "ISO8859_15",
        "ISO8859_2",
        "ISO8859_3",
        "ISO8859_4",
        "ISO8859_5",
        "ISO8859_6",
        "ISO8859_7",
        "ISO8859_8",
        "ISO8859_9",
        "KOI8-R",
        "KOI8-U",
        "KOI8_R",
        "KOI8_U",
        "MS874",
        "MS949",
        "MS950",
        "MacArabic",
        "MacCentralEurope",
        "MacCroatian",
        "MacCyrillic",
        "MacGreek",
        "MacHebrew",
        "MacIceland",
        "MacRoman",
        "MacRomania",
        "MacThai",
        "MacTurkish",
        "MacUkraine",
        "TIS-620",
        "TIS620",
        "US-ASCII",
        "UTF-16",
        "UTF-16BE",
        "UTF-16LE",
        "UTF-32",
        "UTF-32BE",
        "UTF-32LE",
        "UTF-8",
        "UTF8",
        "UTF_32",
        "UTF_32BE",
        "UTF_32LE",
        "UnicodeBig",
        "UnicodeBigUnmarked",
        "UnicodeLittle",
        "UnicodeLittleUnmarked",
        "windows-1250",
        "windows-1251",
        "windows-1252",
        "windows-1253",
        "windows-1254",
        "windows-1255",
        "windows-1256",
        "windows-1257",
        "windows-1258",
        "x-IBM1006",
        "x-IBM1046",
        "x-IBM1097",
        "x-IBM1098",
        "x-IBM1124",
        "x-IBM737",
        "x-IBM856",
        "x-IBM874",
        "x-IBM921",
        "x-IBM922",
        "x-IBM933",
        "x-IBM948",
        "x-IBM949",
        "x-IBM950",
        "x-IBM964",
        "x-Johab",
        "x-MacArabic",
        "x-MacCentralEurope",
        "x-MacCroatian",
        "x-MacCyrillic",
        "x-MacGreek",
        "x-MacHebrew",
        "x-MacIceland",
        "x-MacRoman",
        "x-MacRomania",
        "x-MacThai",
        "x-MacTurkish",
        "x-MacUkraine",
        "x-UTF-16LE-BOM",
        "x-iso-8859-11",
        "x-mswin-936",
        "x-windows-874",
        "x-windows-949",
        "x-windows-950",
    };

    public static void main(final String[] args) {
        final String string = createString(0x10000);
        for (String encoding : encodings) {
            System.out.println("Test encoding: " + encoding);
            new Test4625418(encoding).test(string);
        }
    }

    private static String createString(int length) {
        StringBuilder sb = new StringBuilder(length);
        while (0 < length--)
            sb.append((char) length);

        return sb.toString();
    }

    private final String encoding;

    private Test4625418(final String encoding) {
        this.encoding = encoding;
    }

    private void test(String string) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(output, this.encoding, true, 0);
            encoder.setExceptionListener(this);
            encoder.writeObject(string);
            encoder.close();

            InputStream input = new ByteArrayInputStream(output.toByteArray());
            XMLDecoder decoder = new XMLDecoder(input);
            decoder.setExceptionListener(this);
            Object object = decoder.readObject();
            decoder.close();

            if (!string.equals(object)) {
                throw new Error(this.encoding + " - can't read properly");
            }
        }
        catch (IllegalCharsetNameException exception) {
            throw new Error(this.encoding + " - illegal charset name", exception);
        }
        catch (UnsupportedCharsetException exception) {
            throw new Error(this.encoding + " - unsupported charset", exception);
        }
        catch (UnsupportedOperationException exception) {
            throw new Error(this.encoding + " - unsupported encoder", exception);
        }
    }

    public void exceptionThrown(Exception exception) {
        throw new Error(this.encoding + " - internal", exception);
    }
}
