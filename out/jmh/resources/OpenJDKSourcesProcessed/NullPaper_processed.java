/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4199506
  @summary  java.awt.print.PageFormat.setpaper(Paper paper)
                 assertion test fails by not throwing
                 NullPointerException when a null paper instance is
                 passed as argument and this is specified in the doc.
  @run main NullPaper
*/



/**
 * NullPaper.java
 *
 * summary: java.awt.print.PageFormat.setpaper(Paper paper)
                 assertion test fails by not throwing
                 NullPointerException when a null paper instance is
                 passed as argument and this is specified in the doc.

 */

import java.awt.print.*;


public class NullPaper {

   private static void init()
    {
    boolean settingNullWorked = false;

    try {
        /* Setting the paper to null should throw an exception.
         * The bug was the exception was not being thrown.
         */
        new PageFormat().setPaper(null);
        settingNullWorked = true;

    /* If the test succeeds we'll end up here, so write
     * to standard out.
     */
    } catch (NullPointerException e) {
        pass();

    /* The test failed if we end up here because an exception
     * other than the one we were expecting was thrown.
     */
    } catch (Exception e) {
        fail("Instead of the expected NullPointerException, '" + e + "' was thrown.");
    }

    if (settingNullWorked) {
        fail("The expected NullPointerException was not thrown");
    }

    }


   /*****************************************************
     Standard Test Machinery Section
      DO NOT modify anything in this section -- it's a
      standard chunk of code which has all of the
      synchronisation necessary for the test harness.
      By keeping it the same in all tests, it is easier
      to read and understand someone else's test, as
      well as insuring that all tests behave correctly
      with the test harness.
     There is a section following this for test-defined
      classes
   ******************************************************/
   private static boolean theTestPassed = false;
   private static boolean testGeneratedInterrupt = false;
   private static String failureMessage = "";

   private static Thread mainThread = null;

   private static int sleepTime = 300000;

   public static void main( String args[] ) throws InterruptedException
    {
      mainThread = Thread.currentThread();
      try
       {
         init();
       }
      catch( TestPassedException e )
       {
         return;
       }

      try
       {
         Thread.sleep( sleepTime );
         throw new RuntimeException( "Timed out after " + sleepTime/1000 + " seconds" );
       }
      catch (InterruptedException e)
       {
         if( ! testGeneratedInterrupt ) throw e;

         testGeneratedInterrupt = false;
         if ( theTestPassed == false )
          {
            throw new RuntimeException( failureMessage );
          }
       }

    }

   public static synchronized void setTimeoutTo( int seconds )
    {
      sleepTime = seconds * 1000;
    }

   public static synchronized void pass()
    {
      System.out.println( "The test passed." );
      if ( mainThread == Thread.currentThread() )
       {
         theTestPassed = true;
         throw new TestPassedException();
       }
      theTestPassed = true;
      testGeneratedInterrupt = true;
      mainThread.interrupt();
    }

   public static synchronized void fail()
    {
      fail( "it just plain failed! :-)" );
    }

   public static synchronized void fail( String whyFailed )
    {
      System.out.println( "The test failed: " + whyFailed );
      if ( mainThread == Thread.currentThread() )
       {
         throw new RuntimeException( whyFailed );
       }
      theTestPassed = false;
      testGeneratedInterrupt = true;
      failureMessage = whyFailed;
      mainThread.interrupt();
    }

 }

class TestPassedException extends RuntimeException
 {
 }
