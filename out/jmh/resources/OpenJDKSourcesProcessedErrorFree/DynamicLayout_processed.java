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
  @key headful
  @bug 6500477
  @summary Tests whether DynamicLayout is really off
  @author anthony.petrov@...: area=awt.toplevel
  @library ../../regtesthelpers
  @build Util
  @run main DynamicLayout
*/


/**
 * DynamicLayout.java
 *
 * summary:  tests whether DynamicLayout is really off
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import test.java.awt.regtesthelpers.Util;


public class DynamicLayout
{



    private static void init() {


        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        System.out.println("isDynamicLayoutActive(): " + Toolkit.getDefaultToolkit().isDynamicLayoutActive());


        final Frame frame = new Frame("Test Frame");

        JPanel panel = new JPanel();
        panel.setBackground(Color.RED);

        JTextField jf = new JTextField (10);
        JTextField jf1 = new JTextField (10);
        JButton jb = new JButton("Test");

        panel.add(jf);
        panel.add(jf1);
        panel.add(jb);
        frame.add(panel);

        frame.setSize(400, 400);
        frame.setVisible(true);

        Robot robot = Util.createRobot();
        robot.setAutoDelay(20);

        Util.waitForIdle(robot);

        Point loc1 = jf1.getLocation();

        System.out.println("The initial position of the JTextField is: " + loc1);

        Insets insets = frame.getInsets();
        if (insets.right == 0 || insets.bottom == 0) {
            System.out.println("The test environment must have non-zero right & bottom insets! The current insets are: " + insets);
            pass();
            return;
        }

        Rectangle bounds = frame.getBounds();

        robot.mouseMove(bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);

        robot.mousePress( InputEvent.BUTTON1_MASK );
        robot.mouseMove(bounds.x + bounds.width + 20, bounds.y + bounds.height + 15);
        Util.waitForIdle(robot);

        Point loc2 = jf1.getLocation();
        System.out.println("Position of the JTextField while resizing is: " + loc2);

        robot.mouseRelease( InputEvent.BUTTON1_MASK );

        if (!loc2.equals(loc1)) {
            fail("Location of a component has been changed.");
            return;
        }

        DynamicLayout.pass();

    }



    /*****************************************************
     * Standard Test Machinery Section
     * DO NOT modify anything in this section -- it's a
     * standard chunk of code which has all of the
     * synchronisation necessary for the test harness.
     * By keeping it the same in all tests, it is easier
     * to read and understand someone else's test, as
     * well as insuring that all tests behave correctly
     * with the test harness.
     * There is a section following this for test-
     * classes
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
        System.out.println( "The test is over, hit  Ctl-C to stop Java VM" );
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
        System.out.println( "The test is over, hit  Ctl-C to stop Java VM" );
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
