/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.*;

/*
 * @test
 * @summary To test SystemFlavorMap method:
 *          addUnencodedNativeForFlavor(DataFlavor flav, String nat)
 *          with valid natives and DataFlavors. This stress test will
 *          define numerous mappings of valid String natives and
 *          DataFlavors.  The mappings will be verified by examining
 *          that all entries are present.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main AddNativeTest
 */

public class AddNativeTest {

    SystemFlavorMap flavorMap;
    Hashtable hashVerify;

    Map mapFlavors;
    Map mapNatives;

    Hashtable hashFlavors;
    Hashtable hashNatives;

    public static void main(String[] args) {
        new AddNativeTest().doTest();
    }

    public void doTest() {
        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        mapFlavors = flavorMap.getNativesForFlavors(null);
        mapNatives = flavorMap.getFlavorsForNatives(null);

        hashFlavors = new Hashtable(mapFlavors);
        hashNatives = new Hashtable(mapNatives);

        DataFlavor key;
        hashVerify = new Hashtable();

        for (Enumeration e = hashFlavors.keys() ; e.hasMoreElements() ;) {
            key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorNatives = new Vector(listNatives);


            StringBuffer mimeType = new StringBuffer(key.getMimeType());
            mimeType.insert(mimeType.indexOf(";"),"-TEST");

            DataFlavor testFlavor = new DataFlavor(mimeType.toString(), "Test DataFlavor");

            for (ListIterator i = vectorNatives.listIterator() ; i.hasNext() ;) {
                String element = (String)i.next();
                flavorMap.addUnencodedNativeForFlavor(testFlavor, element);
            }

            Vector existingNatives = new Vector(flavorMap.getNativesForFlavor(testFlavor));
            existingNatives.addAll(vectorNatives);
            vectorNatives = existingNatives;
            hashVerify.put(testFlavor, vectorNatives);
        }

        verifyNewMappings();
    }

    public boolean verifyNewMappings() {
        boolean result = true;

        for (Enumeration e = hashVerify.keys() ; e.hasMoreElements() ;) {
            DataFlavor key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorNatives = new Vector(listNatives);

            if ( !(vectorNatives.containsAll((Vector)hashVerify.get(key)) && ((Vector)hashVerify.get(key)).containsAll(vectorNatives))) {
                throw new RuntimeException("\n*** Error in verifyNewMappings()" +
                    "\nmethod1: addUnencodedNativeForFlavor(DataFlavor flav, String nat)"  +
                    "\nmethod2: List getNativesForFlavor(DataFlavor flav)" +
                    "\nDataFlavor: " + key.getMimeType() +
                    "\nAfter adding several mappings with addUnencodedNativeForFlavor," +
                    "\nthe returned list did not match the mappings that were added." +
                    "\nThe mapping was not included in the list.");
            }
        }
        System.out.println("*** DataFlavor size = " + hashVerify.size());
        System.out.println("*** verifyNewMappings result: " + result + "\n");
        return result;
    }
}
