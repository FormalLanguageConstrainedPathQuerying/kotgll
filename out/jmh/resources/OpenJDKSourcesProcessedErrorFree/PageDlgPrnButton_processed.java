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

/**
 * @test
 * @bug 4956397
 * @key printer
 * @run main/manual PageDlgPrnButton
 */

import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.* ;

public class PageDlgPrnButton implements Printable
{
    public static void main ( String args[] ) {

        String[] instructions =
           {"For non-windows OS, this test PASSes.",
            "You must have at least 2 printers available to perform this test.",
            "This test brings up a native Windows page dialog.",
            "Click on the Printer... button and change the selected printer. ",
            "Test passes if the printout comes from the new selected printer.",
         };

         Sysout.createDialog( );
         Sysout.printInstructions( instructions );

        PageDlgPrnButton pdpb = new PageDlgPrnButton() ;
    }

    public PageDlgPrnButton()
    {
        try
        {
            pageDialogExample();
        }
        catch(Exception e)
        {e.printStackTrace(System.err);}
    }


    public void pageDialogExample() throws PrinterException
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat originalPageFormat = job.defaultPage();
        PageFormat pageFormat = job.pageDialog(originalPageFormat);

        if(originalPageFormat == pageFormat) return;

        job.setPrintable(this,pageFormat);
        job.print();
    }



    public int print(Graphics g, PageFormat pageFormat, int pageIndex)
    {
        final int boxWidth = 100;
        final int boxHeight = 100;
        final Rectangle rect = new Rectangle(0,0,boxWidth,boxHeight);
        final double pageH = pageFormat.getImageableHeight();
        final double pageW = pageFormat.getImageableWidth();

        if (pageIndex > 0) return (NO_SUCH_PAGE);

        final Graphics2D g2d = (Graphics2D)g;

        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        g2d.drawRect(0,0,(int)pageW,(int)pageH);

        final double scale = Math.min( (pageW/boxWidth), (pageH/boxHeight) );

        if(scale < 1.0) g2d.scale(scale, scale);

        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);

        return(PAGE_EXISTS);
    }
}

class Sysout {
   private static TestDialog dialog;

   public static void createDialogWithInstructions( String[] instructions )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      dialog.printInstructions( instructions );
      dialog.show();
      println( "Any messages for the tester will display here." );
    }

   public static void createDialog( )
    {
      dialog = new TestDialog( new Frame(), "Instructions" );
      String[] defInstr = { "Instructions will appear here. ", "" } ;
      dialog.printInstructions( defInstr );
      dialog.show();
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
class TestDialog extends Dialog {

   TextArea instructionsText;
   TextArea messageText;
   int maxStringLength = 80;

   public TestDialog( Frame frame, String name )
    {
      super( frame, name );
      int scrollBoth = TextArea.SCROLLBARS_BOTH;
      instructionsText = new TextArea( "", 15, maxStringLength, scrollBoth );
      add( "North", instructionsText );

      messageText = new TextArea( "", 5, maxStringLength, scrollBoth );
      add("Center", messageText);

      pack();

      show();
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
    }

 }
