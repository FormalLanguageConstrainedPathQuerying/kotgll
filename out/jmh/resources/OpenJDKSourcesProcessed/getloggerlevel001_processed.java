/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.LoggingMXBean.getLoggerLevel;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;
import java.util.logging.*;

public class getloggerlevel001 {

    private static boolean testFailed = false;

    public static void main(String[] args) {

        System.exit(Consts.JCK_STATUS_BASE + run(args, System.out));
    }

    private static Log log;

    private static final Level[] LogLevels = new Level[] {

        Level.ALL, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST,
        Level.INFO, Level.OFF, Level.SEVERE, Level.WARNING
    };

    static int run(String[] args, PrintStream out) {

        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        LoggingMonitor loggingMonitor = Monitor.getLoggingMonitor(log,
            argumentHandler);

        String testLog1Name = getloggerlevel001.class.getName();
        Logger testLog1 = Logger.getLogger(testLog1Name);

        for (int i=0; i<LogLevels.length; i++) {

            testLog1.setLevel(LogLevels[i]);
            String mxbeanLevel = loggingMonitor.getLoggerLevel(testLog1Name);
            if (! LogLevels[i].toString().equals(mxbeanLevel)) {

                testFailed = true;
                log.complain("Failure 1.");
                log.complain("LogLevels[i] = "+LogLevels[i].toString());
                log.complain("loggingMXBean.getLoggerLevel() method returns "
                    + "unexpected value");
            }
        }


        if (loggingMonitor.getLoggerLevel("no such logger") != null) {

            testFailed = true;
            log.complain("Failure 2.");
            log.complain("loggingMXBean.getLoggerLevel(\"no such logger\") does "
                + "not return null");
        }


        synchronized (testLog1) {
            testLog1.setLevel(null);
            String returnedLevel = loggingMonitor.getLoggerLevel(testLog1Name);
            if (returnedLevel == null || !returnedLevel.equals("")) {

                testFailed = true;
                log.complain("Failure 3.");
                log.complain("Level of the specified logger is null, but returned "
                    + "string was not empty: "+returnedLevel);
            }
        }

        if (testFailed)
            log.complain("TEST FAILED");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

}
