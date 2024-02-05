/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.*;

/*
 * @test
 * @summary To test the basic Clipboard functions
 * @author Kanishk Jethi (kanishk.jethi@sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main BasicClipboardTest
 */

public class BasicClipboardTest implements ClipboardOwner {

    StringSelection strSelect = new StringSelection("Transferable String Selection");
    StringSelection strCheck;
    String clipName = "Test Clipboard";
    Clipboard clip = new Clipboard(clipName);
    DataFlavor dataFlavor, testDataFlavor ;
    DataFlavor dataFlavorArray[];
    Object testObject;
    String strTest = null;

    public static void main (String[] args) throws Exception {
        new BasicClipboardTest().doTest();
    }

    public void doTest() throws Exception {
        dataFlavor = new DataFlavor(DataFlavor.javaRemoteObjectMimeType, null, this.getClass().getClassLoader());
        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        dataFlavorArray = new DataFlavor[0];

        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        dataFlavorArray = new DataFlavor[1];
        dataFlavorArray[0] = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + ";class=java.io.Serializable");

        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        if (clip.getName() != clipName)
            throw new RuntimeException("\n*** Error in Clipboard.getName()");

        clip.setContents(null, null);

        clip.setContents(null, new BasicClipboardTest());

        clip.setContents(null, this);

        clip.setContents(strSelect, this);

        strCheck = (StringSelection)clip.getContents(this);
        if (!strCheck.equals(strSelect))
            throw new RuntimeException("\n***The contents of the clipboard are "
            + "not the same as those that were set");

        dataFlavor = DataFlavor.stringFlavor;
        strSelect = new StringSelection(null);
        try {
            testObject = dataFlavor.getReaderForText(strSelect);
            throw new RuntimeException("\n***Error in getReaderForText. An IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }

        dataFlavor.setHumanPresentableName("String Flavor");
        if (!(dataFlavor.getParameter("humanPresentableName")).equals("String Flavor"))
            throw new RuntimeException("\n***Error in getParameter");

        try {
            if (dataFlavor.isMimeTypeEqual(strTest))
                throw new RuntimeException("\n***Error in DataFlavor.equals(String s)");
        } catch (NullPointerException e) {
        }

        if (!(dataFlavor.isMimeTypeEqual(dataFlavor.getMimeType())))
            throw new RuntimeException("\n***Error in DataFlavor.equals(String s)");

        if (!dataFlavorArray[0].isMimeTypeSerializedObject())
            throw new RuntimeException("\n***Error in isMimeTypeSerializedObject()");
        System.out.println(dataFlavorArray[0].getDefaultRepresentationClass());
        System.out.println(dataFlavorArray[0].getDefaultRepresentationClassAsString());
        if (dataFlavor.isFlavorRemoteObjectType())
            System.out.println("The DataFlavor is a remote object type");

        testDataFlavor = (DataFlavor)dataFlavor.clone();
    }

    public void lostOwnership (Clipboard clipboard, Transferable contents) { }
}
