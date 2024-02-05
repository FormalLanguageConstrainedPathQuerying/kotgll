/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.state;

import static jdk.test.lib.Asserts.assertEquals;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;

/**
 * @test
 * @summary Test Recording state with concurrent recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.state.TestStateIdenticalListeners
 */
public class TestStateIdenticalListeners {

    public static void main(String[] args) throws Throwable {
        TestListener testListener = new TestListener();
        FlightRecorder.addListener(testListener);
        FlightRecorder.addListener(testListener);

        Recording recording = new Recording();
        recording.start();
        recording.stop();

        assertEquals(4, testListener.notificationCount, "Expected 2 notifications, got " + testListener.notificationCount);

        System.out.println("Test Passed");
        recording.close();
    }

    private static class TestListener implements FlightRecorderListener {
        private int notificationCount;

        @Override
        public void recordingStateChanged(Recording recording) {
            System.out.println("recordingStateChanged: " + " recording: " + recording + " state: " + recording.getState());
            ++notificationCount;
        }
    }
}
