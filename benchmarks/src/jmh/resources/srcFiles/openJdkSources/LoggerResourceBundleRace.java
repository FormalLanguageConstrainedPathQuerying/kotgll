/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     7045594 8002070
 * @summary ResourceBundle setting race in Logger.getLogger(name, rbName)
 * @author  Daniel D. Daugherty
 * @build RacingThreadsTest LoggerResourceBundleRace
 * @run main/othervm LoggerResourceBundleRace
 *
 * (In samevm mode, the bundle classes don't end up in the classpath.)
 */
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public class LoggerResourceBundleRace extends RacingThreadsTest {
    private static final int N_LOOPS   = 500000;   
    private static final int N_SECS    = 15;       
    private static final int N_THREADS = 3;

    private static final String LOGGER_PREFIX = "myLogger-";
    private static final String RESOURCE_PREFIX
        = "LoggerResourceBundleRace$MyResources";
    private static final AtomicInteger iaeCnt = new AtomicInteger();
    private static final AtomicInteger worksCnt = new AtomicInteger();

    Logger dummy;   

    LoggerResourceBundleRace(String name, int n_threads, int n_loops,
        int n_secs) {
        super(name, n_threads, n_loops, n_secs);
    }


    public static void main(String[] args) {
        LoggerResourceBundleRace test
            = new LoggerResourceBundleRace("LoggerResourceBundleRace",
                                           N_THREADS, N_LOOPS, N_SECS);
        test.setVerbose(
            Boolean.getBoolean("LoggerResourceBundleRace.verbose"));

        DriverThread driver = new DriverThread(test);
        MyWorkerThread[] workers = new MyWorkerThread[N_THREADS];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new MyWorkerThread(i, test);
        }
        test.runTest(driver, workers);
    }

    public void oneTimeDriverInit(DriverThread dt) {
        super.oneTimeDriverInit(dt);
        dummy = null;
    }

    public void perRaceDriverInit(DriverThread dt) {
        super.perRaceDriverInit(dt);

        dummy = Logger.getLogger(LOGGER_PREFIX + getLoopCnt());
        iaeCnt.set(0);
        worksCnt.set(0);
    }

    public void executeRace(WorkerThread wt) {
        super.executeRace(wt);

        Logger myLogger = null;
        try {
            MyWorkerThread mwt = (MyWorkerThread) wt;  

            myLogger = Logger.getLogger(LOGGER_PREFIX + getLoopCnt(),
                                        mwt.rbName);
            if (myLogger.getResourceBundleName().equals(mwt.rbName)) {
                worksCnt.incrementAndGet();  
            } else {
                System.err.println(wt.getName()
                    + ": ERROR: expected ResourceBundleName '"
                    + mwt.rbName + "' does not match actual '"
                    + myLogger.getResourceBundleName() + "'");
                incAndGetFailCnt();  
            }
        } catch (IllegalArgumentException iae) {
            iaeCnt.incrementAndGet();  
        } catch (MissingResourceException mre) {
            unexpectedException(wt, mre);
            System.exit(2);
        }
    }

    public void checkRaceResults(DriverThread dt) {
        super.checkRaceResults(dt);

        if (worksCnt.get() != 1) {
            System.err.println(dt.getName() + ": ERROR: worksCnt should be 1"
                + ": loopCnt=" + getLoopCnt() + ", worksCnt=" + worksCnt.get());
            incAndGetFailCnt();  
        } else if (iaeCnt.get() != N_THREADS - 1) {
            System.err.println(dt.getName() + ": ERROR: iaeCnt should be "
                + (N_THREADS - 1) + ": loopCnt=" + getLoopCnt()
                + ", iaeCnt=" + iaeCnt.get());
            incAndGetFailCnt();  
        }
    }

    public void oneTimeDriverEpilog(DriverThread dt) {
        super.oneTimeDriverEpilog(dt);

        dummy.info("This is a test message.");
    }

    public static class MyResources0 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #1 for sample1"},
            {"sample2", "translation #1 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }

    public static class MyResources1 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #2 for sample1"},
            {"sample2", "translation #2 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }

    public static class MyResources2 extends ListResourceBundle {
        static final Object[][] contents = {
            {"sample1", "translation #3 for sample1"},
            {"sample2", "translation #3 for sample2"},
        };

        public Object[][] getContents() {
            return contents;
        }
    }


    public static class MyWorkerThread extends WorkerThread {
        public final String rbName;  

        MyWorkerThread(int workerNum, RacingThreadsTest test) {
            super(workerNum, test);

            rbName = RESOURCE_PREFIX + workerNum;
        }
    }
}
