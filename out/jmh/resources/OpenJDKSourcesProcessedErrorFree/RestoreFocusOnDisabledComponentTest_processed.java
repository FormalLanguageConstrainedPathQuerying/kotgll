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
  @test
  @key headful
  @bug       6598089
  @summary   Tests restoring focus on a single disabled coponent
  @library   ../../regtesthelpers
  @build     Util
  @run       main RestoreFocusOnDisabledComponentTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

/*
 * The bug is not reproducible on Windows.
 */
public class RestoreFocusOnDisabledComponentTest {
    Frame frame = new Frame("Frame") {public String toString() {return "FRAME";}};
    Button b0 = new Button("button0") {public String toString() {return "B-0";}};
    Button b1 = new Button("button1") {public String toString() {return "B-1";}};
    volatile int nFocused;
    Robot robot;

    public static void main(String[] args) {
        RestoreFocusOnDisabledComponentTest app = new RestoreFocusOnDisabledComponentTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
    }

    public void start() {
        frame.add(b0);
        frame.add(b1);
        frame.setLayout(new FlowLayout());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Util.waitForIdle(robot);
        KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultKeyboardFocusManager() {
            public boolean dispatchEvent(AWTEvent e) {
                if (e.getID() == FocusEvent.FOCUS_GAINED) {
                    if (e.getSource() == b1) {
                        b1.setEnabled(false);

                    } else if (e.getSource() == b0) {
                        if (++nFocused > 10) {
                            nFocused = -1;
                            throw new TestFailedException("Focus went into busy loop!");
                        }
                    }
                }
                return super.dispatchEvent(e);
            }
        });
        b0.setEnabled(false);

        Util.waitForIdle(robot);
        if (nFocused != -1) {
            System.out.println("Test passed.");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
