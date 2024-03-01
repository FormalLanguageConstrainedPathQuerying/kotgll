/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test %W% %E%
  @key headful
  @bug 6768332
  @summary Tests whether internal frames are always considered opaque
  @author anthony.petrov@...: area=awt.mixing
  @library ../regtesthelpers
  @build Util
  @run main NonOpaqueInternalFrame
*/


/**
 * NonOpaqueInternalFrame.java
 *
 * summary:  Tests whether internal frames are always considered opaque
 */

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import javax.swing.*;
import java.util.Vector;
import test.java.awt.regtesthelpers.Util;



public class NonOpaqueInternalFrame
{
    static volatile boolean failed = false;

    private static final class MyButton extends Button
            implements ActionListener
        {
            public MyButton() {
                setPreferredSize(new Dimension(100, 100));
                addActionListener(this);
            }

            public void actionPerformed(ActionEvent e) {
                failed = true;
            }
        }

    private static void init()
    {
        JFrame jframe = new JFrame("mixing test");
        JDesktopPane desktop = new JDesktopPane();
        jframe.setContentPane(desktop);
        JInternalFrame iframe1 = new JInternalFrame("iframe 1");
        iframe1.setIconifiable(true);
        iframe1.add(new MyButton());
        iframe1.setBounds(10, 10, 100, 100);
        iframe1.setOpaque(false);
        iframe1.setVisible(true);
        desktop.add(iframe1);
        JInternalFrame iframe2 = new JInternalFrame("iframe 2");
        iframe2.setIconifiable(true);
        iframe2.add(new MyButton());
        iframe2.setBounds(50, 50, 100, 100);
        iframe2.setOpaque(false);
        iframe2.setVisible(true);
        desktop.add(iframe2);
        jframe.setSize(300, 300);
        jframe.setVisible(true);

        Robot robot = Util.createRobot();
        robot.setAutoDelay(20);

        Util.waitForIdle(robot);

        try {
            iframe2.setSelected(true);
        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }

        Point lLoc = iframe2.getLocationOnScreen();
        System.err.println("lLoc: " + lLoc);
        robot.mouseMove(lLoc.x + 10, lLoc.y + 10);
        Util.waitForIdle(robot);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);


        if (failed) {
            fail("The JInternalFrame is considered non-opaque.");
        } else {
            pass();
        }
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
