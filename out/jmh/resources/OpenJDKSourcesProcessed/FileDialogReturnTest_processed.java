/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
  test
  @bug 6260676
  @summary FileDialog.setDirectory() does not work properly, XToolkit
  @author Dmitry.Cherepanov area=awt.filedialog
  @run applet/manual=yesno FileDialogReturnTest.html
*/

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

/*
 * Current implementation of the FileDialog class doesn't provide
 * any explicit method to get the return value after the user closes
 * the dialog. The only way to detect whether the user cancels the
 * dialog or the user selects any file is to use the getFile() method.
 * The getFile() method should return null value if the user cancels
 * the dialog or non-null value if the user selects any file.
 */
public class FileDialogReturnTest extends Applet
{

    public static void main(String[] args) {
        Applet a = new FileDialogReturnTest();
        a.init();
        a.start();
    }

    public void init()
    {
        this.setLayout (new BorderLayout ());

        String[] instructions =
        {
            " 1. The test shows the 'FileDialogReturnTest' applet which contains two text fields and one button, ",
            " 2. Input something into the 'File:' text field or just keep the field empty, ",
            " 3. Input something into the 'Dir:' text field or just keep the field empty, ",
            " 4. Press the 'Show' button and a file dialog will appear, ",
            " 5-1. Cancel the file dialog, e.g. by selecting the 'close' menu item, ",
            "      If the output window shows that 'file'/'dir' values is null then the test passes, otherwise the test fails, ",
            " 5-2. Select any file, e.g. by pressing the 'OK' button, ",
            "      If the output window shows that 'file'/'dir' values is not-null then the test passes, otherwise the test fails. "
        };
        Sysout.createDialogWithInstructions( instructions );

    }

    final TextField fileField = new TextField("", 20);
    final TextField dirField = new TextField("", 20);
    final Button button = new Button("Show");

    public void start ()
    {
        setLayout(new FlowLayout());

        add(new Label("File:"));
        add(fileField);
        add(new Label("Dir:"));
        add(dirField);
        add(button);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDialog();
            }
        });

        setSize (200,200);
        setVisible(true);
        validate();
    }

    void showDialog()
    {
        FileDialog fd = new FileDialog(new Frame());
        fd.setFile(fileField.getText());
        fd.setDirectory(dirField.getText());
        fd.setVisible(true);

        Sysout.println("[file=" + fd.getFile()+"]");
        Sysout.println("[dir=" + fd.getDirectory()+"]");
    }

}


/****************************************************
 Standard Test Machinery
 DO NOT modify anything below -- it's a standard
  chunk of code whose purpose is to make user
  interaction uniform, and thereby make it simpler
  to read and understand someone else's test.
 ****************************************************/

/**
 This is part of the standard test machinery.
 It creates a dialog (with the instructions), and is the interface
  for sending text messages to the user.
 To print the instructions, send an array of strings to Sysout.createDialog
  WithInstructions method.  Put one line of instructions per array entry.
 To display a message for the tester to see, simply call Sysout.println
  with the string to be displayed.
 This mimics System.out.println but works within the test harness as well
  as standalone.
 */

class Sysout
{
    private static TestDialog dialog;

    public static void createDialogWithInstructions( String[] instructions )
    {
        dialog = new TestDialog( new Frame(), "Instructions" );
        dialog.printInstructions( instructions );
        dialog.setVisible(true);
        println( "Any messages for the tester will display here." );
    }

    public static void createDialog( )
    {
        dialog = new TestDialog( new Frame(), "Instructions" );
        String[] defInstr = { "Instructions will appear here. ", "" } ;
        dialog.printInstructions( defInstr );
        dialog.setVisible(true);
        println( "Any messages for the tester will display here." );
    }


    public static void printInstructions( String[] instructions )
    {
        dialog.printInstructions( instructions );
    }


    public static void println( String messageIn )
    {
        dialog.displayMessage( messageIn );
    }

}

/**
  This is part of the standard test machinery.  It provides a place for the
   test instructions to be displayed, and a place for interactive messages
   to the user to be displayed.
  To have the test instructions displayed, see Sysout.
  To have a message to the user be displayed, see Sysout.
  Do not call anything in this dialog directly.
  */
class TestDialog extends Dialog
{

    TextArea instructionsText;
    TextArea messageText;
    int maxStringLength = 100;

    public TestDialog( Frame frame, String name )
    {
        super( frame, name );
        int scrollBoth = TextArea.SCROLLBARS_BOTH;
        instructionsText = new TextArea( "", 15, maxStringLength, scrollBoth );
        add( "North", instructionsText );

        messageText = new TextArea( "", 5, maxStringLength, scrollBoth );
        add("Center", messageText);

        pack();

        setVisible(true);
    }

    public void printInstructions( String[] instructions )
    {
        instructionsText.setText( "" );


        String printStr, remainingStr;
        for( int i=0; i < instructions.length; i++ )
        {
            remainingStr = instructions[ i ];
            while( remainingStr.length() > 0 )
            {
                if( remainingStr.length() >= maxStringLength )
                {
                    int posOfSpace = remainingStr.
                        lastIndexOf( ' ', maxStringLength - 1 );

                    if( posOfSpace <= 0 ) posOfSpace = maxStringLength - 1;

                    printStr = remainingStr.substring( 0, posOfSpace + 1 );
                    remainingStr = remainingStr.substring( posOfSpace + 1 );
                }
                else
                {
                    printStr = remainingStr;
                    remainingStr = "";
                }

                instructionsText.append( printStr + "\n" );

            }

        }

    }

    public void displayMessage( String messageIn )
    {
        messageText.append( messageIn + "\n" );
        System.out.println(messageIn);
    }

}
