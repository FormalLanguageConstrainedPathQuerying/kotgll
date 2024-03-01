/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.progressbar.ProgressBarDemo;
import static com.sun.swingset3.demos.progressbar.ProgressBarDemo.*;
import java.awt.Component;
import javax.swing.UIManager;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JProgressBarOperator;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 ProgressBarDemo page by pressing start and stop
 *          buttons and checking the progress bar and the buttons state.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.progressbar.ProgressBarDemo
 * @run testng/timeout=1200 ProgressBarDemoTest
 */
@Listeners(GuiTestListener.class)
public class ProgressBarDemoTest {

    private final static long PROGRESS_BAR_TIMEOUT = 180000;

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(ProgressBarDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);

        JButtonOperator startButton = new JButtonOperator(frame, START_BUTTON);
        JButtonOperator stopButton = new JButtonOperator(frame, STOP_BUTTON);
        JProgressBarOperator jpbo = new JProgressBarOperator(frame);

        checkCompleteProgress(frame, startButton, stopButton, jpbo);

        checkStartStop(frame, startButton, stopButton, jpbo);
    }

    public void checkStartStop(JFrameOperator frame, JButtonOperator startButton, JButtonOperator stopButton, JProgressBarOperator progressBar) throws Exception {
        int initialProgress = progressBar.getValue();
        System.out.println("initialProgress = " + initialProgress);
        int maximum = progressBar.getMaximum();

        startButton.pushNoBlock();

        progressBar.waitState(new ComponentChooser() {

            @Override
            public boolean checkComponent(Component comp) {
                int value = progressBar.getValue();
                return value < maximum;
            }

            @Override
            public String getDescription() {
                return "Progress < maximum (" + maximum + ")";
            }
        });

        stopButton.waitComponentEnabled();

        progressBar.waitState(new ComponentChooser() {

            @Override
            public boolean checkComponent(Component comp) {
                int value = progressBar.getValue();
                return value > 0;
            }

            @Override
            public String getDescription() {
                return "Progress > 0";
            }
        });

        int progress = progressBar.getValue();
        System.out.println("progress = " + progress);

        assertTrue("Progress Bar Progressing (progress > 0, actual value: " + progress + ")", progress > 0);
        assertFalse("Start Button Disabled", startButton.isEnabled());
        assertTrue("Stop Button Enabled", stopButton.isEnabled());

        progressBar.waitState(new ComponentChooser() {

            @Override
            public boolean checkComponent(Component comp) {
                return progressBar.getValue() > progress;
            }

            @Override
            public String getDescription() {
                return "Progress > " + progress;
            }
        });

        stopButton.pushNoBlock();

        startButton.waitComponentEnabled();

        int interimProgress = progressBar.getValue();

        assertTrue("Progress Bar Stopped "
                + "(interimProgress, actual value: " + interimProgress + " "
                + "> progress, actual value: " + progress + ")",
                interimProgress > progress);
        assertTrue("Start Button Enabled", startButton.isEnabled());
        assertFalse("Stop Button Disabled", stopButton.isEnabled());
    }

    public void checkCompleteProgress(JFrameOperator frame, JButtonOperator startButton, JButtonOperator stopButton, JProgressBarOperator progressBar) throws Exception {
        Timeouts timeouts = progressBar.getTimeouts();
        long defaultTimeout = timeouts.getTimeout("ComponentOperator.WaitStateTimeout");
        startButton.pushNoBlock();

        timeouts.setTimeout("ComponentOperator.WaitStateTimeout", PROGRESS_BAR_TIMEOUT);
        progressBar.waitValue(progressBar.getMaximum());
        timeouts.setTimeout("ComponentOperator.WaitStateTimeout", defaultTimeout);

        startButton.waitComponentEnabled();

        assertEquals("Complete Progress", progressBar.getMaximum(), progressBar.getValue());
        assertTrue("Start Button Enabled", startButton.isEnabled());
        assertFalse("Stop Button Disabled", stopButton.isEnabled());
    }

}
