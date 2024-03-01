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
 * @bug 4245280
 * @summary PrinterJob not cancelled when PrinterJob.cancel() is used
 * @key printer
 * @run main/manual PrinterJobCancel
 */

import java.awt.* ;
import java.awt.print.* ;

public class PrinterJobCancel extends Thread implements Printable {

  PrinterJob pj ;
  boolean okayed;

  public static void main ( String args[] ) {

     String[] instructions =
        {
         "Test that print job cancellation works.",
         "You must have a printer available to perform this test.",
         "This test silently starts a print job and while the job is",
         "still being printed, cancels the print job",
         "You should see a message on System.out that the job",
         "was properly cancelled.",
         "You will need to kill the application manually since regression",
         "tests apparently aren't supposed to call System.exit()"
       };

      Sysout.createDialog( );
      Sysout.printInstructions( instructions );

      PrinterJobCancel pjc = new PrinterJobCancel() ;

      if (pjc.okayed) {
          pjc.start();
          try {
               Thread.sleep(5000);
               pjc.pj.cancel();
          } catch ( InterruptedException e ) {
          }
      }
  }

  public PrinterJobCancel() {

    pj = PrinterJob.getPrinterJob() ;
    pj.setPrintable(this);
    okayed = pj.printDialog();
  }

  public void run() {
    boolean cancelWorked = false;
    try {
        pj.print() ;
    }
    catch ( PrinterAbortException paex ) {
      cancelWorked = true;
      System.out.println("Job was properly cancelled and we");
      System.out.println("got the expected PrintAbortException");
    }
    catch ( PrinterException prex ) {
      System.out.println("This is wrong .. we shouldn't be here");
      System.out.println("Looks like a test failure");
      prex.printStackTrace() ;
    }
    finally {
       System.out.println("DONE PRINTING");
       if (!cancelWorked) {
           System.out.println("Looks like the test failed - we didn't get");
           System.out.println("the expected PrintAbortException ");
       }
    }
  }

  public int print(Graphics g, PageFormat pagef, int pidx) {

     if (pidx > 5) {
        return( Printable.NO_SUCH_PAGE ) ;
     }

     Graphics2D g2d = (Graphics2D)g;
     g2d.translate(pagef.getImageableX(), pagef.getImageableY());
     g2d.setColor(Color.black);

     g2d.drawString(("This is page"+(pidx+1)), 60 , 80);
     try {
          Thread.sleep(2000);
     } catch (InterruptedException e) {
     }

     return ( Printable.PAGE_EXISTS );
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
