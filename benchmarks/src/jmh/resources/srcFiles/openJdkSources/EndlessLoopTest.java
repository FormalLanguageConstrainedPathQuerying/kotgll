/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6382144
  @summary REGRESSION: InputVerifier and JOptionPane
  @run main EndlessLoopTest
*/

/**
 * EndlessLoopTest.java
 *
 * summary: REGRESSION: InputVerifier and JOptionPane
 */

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class EndlessLoopTest
{

    static volatile int n_iv_calls;
    static JFrame frame;
    static JTextField t1;
    static JButton button;

    private static void init() throws Exception
    {

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame();
                final JDialog dialog = new JDialog(frame, true);
                button = new JButton("press me");
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        dialog.dispose();
                    }
                });
                dialog.getContentPane().add(button);
                dialog.setLocationRelativeTo(null);
                dialog.pack();

                t1 = new JTextField();
                t1.setInputVerifier(new InputVerifier() {
                    public boolean verify(JComponent input) {
                        n_iv_calls++;
                        if (n_iv_calls == 1) {
                            dialog.setVisible(true);
                        }
                        return true;
                    }
                });
                JTextField t2 = new JTextField();

                frame.getContentPane().add(t1, BorderLayout.NORTH);
                frame.getContentPane().add(t2, BorderLayout.SOUTH);
                frame.setLocationRelativeTo(null);
                frame.setSize(200, 200);
                frame.setVisible(true);
            });

            Robot r = null;
            try {
                r = new Robot();
            } catch (AWTException e) {
                EndlessLoopTest.fail(e);
            }

            try {
                r.setAutoDelay(100);
                r.waitForIdle();
                r.delay(1000);

                mouseClickOnComp(r, t1);
                r.waitForIdle();

                if (!t1.isFocusOwner()) {
                    throw new RuntimeException("t1 is not a focus owner");
                }
                n_iv_calls = 0;
                r.keyPress(KeyEvent.VK_TAB);
                r.keyRelease(KeyEvent.VK_TAB);
                r.waitForIdle();

                mouseClickOnComp(r, button);
                r.waitForIdle();
            } catch (Exception e) {
                EndlessLoopTest.fail(e);
            }

            if (n_iv_calls != 1) {
                EndlessLoopTest.fail(new RuntimeException("InputVerifier was called " + n_iv_calls + " times"));
            }

            EndlessLoopTest.pass();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }


    static void mouseClickOnComp(Robot r, Component comp) {
        Point loc = comp.getLocationOnScreen();
        loc.x += comp.getWidth() / 2;
        loc.y += comp.getHeight() / 2;
        r.mouseMove(loc.x, loc.y);
        r.waitForIdle();
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
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

    public static void main( String args[] ) throws Exception
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

    public static synchronized void fail( Exception whyFailed )
    {
        System.out.println( "The test failed: " + whyFailed );
        System.out.println( "The test is over, hit  Ctl-C to stop Java VM" );
        if ( mainThread == Thread.currentThread() )
        {
            throw new RuntimeException( whyFailed );
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed.toString();
        mainThread.interrupt();
    }

}

class TestPassedException extends RuntimeException
{
}
