/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196869
 * @summary Make sure we deal with internal Key data being cleared properly
 * @ignore This test aims to provoke NPEs, but due to the need to constrain
 *         memory usage it fails intermittently with OOME on various systems
 *         with no way to ignore such failures.
 * @run main/othervm -Xms16m -Xmx16m -esa SoftKeys
 */
import java.util.*;

public class SoftKeys {

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 512*1024; j++) {
                    Locale.of(HexFormat.of().toHexDigits((short)j));
                }
            }
        } catch (OutOfMemoryError e) {

            System.gc();
        }
    }
}

