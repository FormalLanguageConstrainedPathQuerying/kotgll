/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4930708 4174436 5008498
 * @library /java/text/testlib
 * @summary test Danish Collation
 * @modules jdk.localedata
 * @run junit DanishTest
 */
/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

import java.util.Locale;
import java.text.Collator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class DanishTest {

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "Lvi",
        "L\u00E4vi",
        "L\u00FCbeck",
        "ANDR\u00C9",
        "ANDRE",
        "ANNONCERER"
    };

    private static final String[] primaryTargetData = {
            "Lwi",
            "L\u00F6wi",
            "Lybeck",
            "ANDR\u00E9",
            "ANDR\u00C9",
            "ANN\u00D3NCERER"
    };

    private static final int[] primaryResults = {
            -1, -1, 0, 0, 0, 0
    };

    /*
     * Data for TestTertiary()
     */
    private static final String[] tertiarySourceData = {
            "Luc",
            "luck",
            "L\u00FCbeck",
            "L\u00E4vi",
            "L\u00F6ww"
    };

    private static final String[] tertiaryTargetData = {
            "luck",
            "L\u00FCbeck",
            "lybeck",
            "L\u00F6we",
            "mast"
    };

    private static final int[] tertiaryResults = {
            -1, -1,  1, -1, -1
    };

    /*
     * Data for TestExtra()
     */
    private static final String[] testData = {
            "A/S",
            "ANDRE",
            "ANDR\u00C9", 
            "ANDR\u00C8", 
            "ANDR\u00E9", 
            "ANDR\u00EA", 
            "Andre",
            "Andr\u00E9", 
            "\u00C1NDRE", 
            "\u00C0NDRE", 
            "andre",
            "\u00E1ndre", 
            "\u00E0ndre", 
            "ANDREAS",
            "ANNONCERER",
            "ANN\u00D3NCERER", 
            "annoncerer",
            "ann\u00F3ncerer", 
            "AS",
            "A\u00e6RO", 
            "CA",
            "\u00C7A", 
            "CB",
            "\u00C7C", 
            "D.S.B.",
            "DA",
            "DB",
            "\u00D0ORA", 
            "DSB",
            "\u00D0SB", 
            "DSC",
            "EKSTRA_ARBEJDE",
            "EKSTRABUD",
            "H\u00D8ST",  
            "HAAG",
            "H\u00C5NDBOG", 
            "HAANDV\u00C6RKSBANKEN", 
            "INTERNETFORBINDELSE",
            "Internetforbindelse",
            "\u00CDNTERNETFORBINDELSE", 
            "internetforbindelse",
            "\u00EDnternetforbindelse", 
            "Karl",
            "karl",
            "NIELSEN",
            "NIELS J\u00D8RGEN", 
            "NIELS-J\u00D8RGEN", 
            "OERVAL",
            "\u0152RVAL", 
            "\u0153RVAL", 
            "R\u00C9E, A", 
            "REE, B",
            "R\u00C9E, L", 
            "REE, V",
            "SCHYTT, B",
            "SCHYTT, H",
            "SCH\u00DCTT, H", 
            "SCHYTT, L",
            "SCH\u00DCTT, M", 
            "SS",
            "ss",
            "\u00DF", 
            "SSA",
            "\u00DFA", 
            "STOREK\u00C6R", 
            "STORE VILDMOSE",
            "STORMLY",
            "STORM PETERSEN",
            "THORVALD",
            "THORVARDUR",
            "\u00DEORVAR\u0110UR", 
            "THYGESEN",
            "VESTERG\u00C5RD, A",
            "VESTERGAARD, A",
            "VESTERG\u00C5RD, B",                
            "Westmalle",
            "YALLE",
            "Yderligere",
            "\u00DDderligere", 
            "\u00DCderligere", 
            "\u00FDderligere", 
            "\u00FCderligere", 
            "U\u0308ruk-hai",
            "ZORO",
            "\u00C6BLE",  
            "\u00E6BLE",  
            "\u00C4BLE",  
            "\u00E4BLE",  
            "\u00D8BERG", 
            "\u00F8BERG", 
            "\u00D6BERG", 
            "\u00F6BERG"  
    };

    @Test
    public void TestPrimary() {
        TestUtils.doCollatorTest(myCollation, Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
    }

    @Test
    public void TestTertiary() {
        TestUtils.doCollatorTest(myCollation, Collator.TERTIARY,
               tertiarySourceData, tertiaryTargetData, tertiaryResults);

        for (int i = 0; i < testData.length-1; i++) {
            for (int j = i+1; j < testData.length; j++) {
                TestUtils.doCollatorTest(myCollation, testData[i], testData[j], -1);
            }
        }
    }

    private final Collator myCollation = Collator.getInstance(Locale.of("da"));
}
