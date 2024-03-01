/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4804273
 * @modules jdk.localedata
 * @summary updating collation tables for swedish
 */

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

public class Bug4804273 {

  /********************************************************
  *********************************************************/
  public static void main (String[] args) {
      Locale reservedLocale = Locale.getDefault();
      try {
        int errors=0;

        Locale loc = new Locale ("sv", "se");   

        Locale.setDefault (loc);
        Collator col = Collator.getInstance ();

        String[] data = {"A",
                         "Aa",
                         "Ae",
                         "B",
                         "Y",
                         "U\u0308", 
                         "Z",
                         "A\u030a", 
                         "A\u0308", 
                         "\u00c6", 
                         "O\u0308", 
                         "a\u030b", 
                         "\u00d8", 
                         "a",
                         "aa",
                         "ae",
                         "b",
                         "y",
                         "u\u0308", 
                         "z",
                         "A\u030b", 
                         "a\u030a", 
                         "a\u0308", 
                         "\u00e6", 
                         "o\u0308", 
                         "\u00f8", 
        };


        String[] sortedData = {"a",
                               "A",
                               "aa",
                               "Aa",
                               "ae",
                               "Ae",
                               "b",
                               "B",
                               "y",
                               "Y",
                               "u\u0308", 
                               "U\u0308", 
                               "z",
                               "Z",
                               "a\u030a", 
                               "A\u030a", 
                               "a\u0308", 
                               "A\u0308", 
                               "a\u030b", 
                               "A\u030b", 
                               "\u00e6", 
                               "\u00c6", 
                               "o\u0308", 
                               "O\u0308", 
                               "\u00f8", 
                               "\u00d8", 
        };

        Arrays.sort (data, col);

        System.out.println ("Using " + loc.getDisplayName());
        for (int i = 0;  i < data.length;  i++) {
            System.out.println(data[i] + "  :  " + sortedData[i]);
            if (sortedData[i].compareTo(data[i]) != 0) {
                errors++;
            }
        }

        if (errors > 0)
            throw new RuntimeException("There are " + errors +
                        " words sorted incorrectly!");
      } finally {
          Locale.setDefault(reservedLocale);
      }
  }

}
