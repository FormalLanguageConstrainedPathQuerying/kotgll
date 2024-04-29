/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8318761 8329118
 * @summary Test MessageFormatPattern ability to recognize and produce
 *          appropriate FormatType and FormatStyle for CompactNumberFormat.
 * @run junit CompactSubFormats
 */

import java.text.CompactNumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompactSubFormats {

    @Test
    public void applyPatternTest() {
        var mFmt = new MessageFormat(
                "{0,number,compact_short}{1,number,compact_long}", Locale.US);
        var compactShort = NumberFormat.getCompactNumberInstance(
                mFmt.getLocale(), NumberFormat.Style.SHORT);
        var compactLong = NumberFormat.getCompactNumberInstance(
                mFmt.getLocale(), NumberFormat.Style.LONG);
        assertEquals(mFmt.getFormatsByArgumentIndex()[0], compactShort);
        assertEquals(mFmt.getFormatsByArgumentIndex()[1], compactLong);
    }

    @Test
    public void recognizedCompactStylesTest() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        assertEquals(new DecimalFormat("compact_regular", symbols),
                new MessageFormat("{0,number,compact_regular}", Locale.US).getFormatsByArgumentIndex()[0]);
    }

    @Test
    public void toPatternTest() {
        var mFmt = new MessageFormat("{0}{1}", Locale.US);
        mFmt.setFormatByArgumentIndex(0, NumberFormat.getCompactNumberInstance(
                mFmt.getLocale(), NumberFormat.Style.SHORT));
        mFmt.setFormatByArgumentIndex(1, NumberFormat.getCompactNumberInstance(
                mFmt.getLocale(), NumberFormat.Style.LONG));
        assertEquals("{0,number,compact_short}{1,number,compact_long}", mFmt.toPattern());
    }

    @Test
    public void badToPatternTest() {
        var mFmt = new MessageFormat("{0}", Locale.US);
        mFmt.setFormatByArgumentIndex(0, new CompactNumberFormat("",
                        DecimalFormatSymbols.getInstance(Locale.US), new String[]{""}));
        assertEquals("{0}", mFmt.toPattern());
    }
}
