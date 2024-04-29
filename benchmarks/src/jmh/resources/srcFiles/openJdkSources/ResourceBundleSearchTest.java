/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8002070 8013382
 * @summary Remove the stack search for a resource bundle Logger to use
 * @author  Jim Gish
 * @build  ResourceBundleSearchTest IndirectlyLoadABundle LoadItUp1 LoadItUp2 TwiceIndirectlyLoadABundle LoadItUp2Invoker
 * @run main/othervm ResourceBundleSearchTest
 */
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This class tests various scenarios of loading resource bundles from
 * java.util.logging.  Since jtreg uses the logging system, it is necessary to
 * run these tests using othervm mode to ensure no interference from logging
 * initialization by jtreg
 */
public class ResourceBundleSearchTest {

    private static final boolean DEBUG = false;
    private static final String LOGGER_PREFIX = "myLogger.";
    private static int loggerNum = 0;
    private static final String PROP_RB_NAME = "ClassPathTestBundle";
    private static final String TCCL_TEST_BUNDLE = "ContextClassLoaderTestBundle";

    private static int numPass = 0;
    private static int numFail = 0;
    private static List<String> msgs = new ArrayList<>();

    static final long stamp = System.currentTimeMillis();
    private static String getTimeStamp() {
        long time = System.currentTimeMillis();
        long delta = time - stamp;
        long min = delta/60000;
        long sec = (delta - min * 60000) / 10000;
        long msec = delta - min * 60000 - sec * 1000;
        return (min == 0 ? "" : (min + " min. ")) +
               (sec == 0 ? "" : (sec + " sec. ")) +
               (msec == 0 ? "" : (msec + "ms."));
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("ResourceBundleSearchTest starting: "+getTimeStamp());
        ResourceBundleSearchTest test = new ResourceBundleSearchTest();
        try {
            test.runTests();
        } finally {
            System.out.println("ResourceBundleSearchTest terminated: "+getTimeStamp());
        }
    }

    private void runTests() throws Throwable {
        Locale.setDefault(Locale.ENGLISH);

        ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();

        String userDir = System.getProperty("user.dir");
        String testDir = System.getProperty("test.src", userDir);

        URL[] urls = new URL[1];

        urls[0] = Paths.get(testDir, "resources").toUri().toURL();
        URLClassLoader rbClassLoader = new URLClassLoader(urls);

        int testnb = 1;
        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        assertFalse(testGetBundleFromStackSearch(), "1-testGetBundleFromStackSearch");

        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        assertTrue(testGetBundleFromTCCL(TCCL_TEST_BUNDLE, rbClassLoader),
                   "2-testGetBundleFromTCCL");

        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        if (isOnClassPath(PROP_RB_NAME, myClassLoader)) {
            debug("We should be able to see " + PROP_RB_NAME + " on the classpath");
            assertTrue(testGetBundleFromSystemClassLoader(PROP_RB_NAME),
                       "3-testGetBundleFromSystemClassLoader");
        } else {
            throw new Exception("TEST SETUP FAILURE: Cannot see " + PROP_RB_NAME
                                 + " on the classpath");
        }

        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        assertTrue(testGetBundleFromCallersClassLoader(),
                   "4-testGetBundleFromCallersClassLoader");

        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        assertTrue(testGetAnonymousLogger(), "5-testGetAnonymousLogger");

        System.out.println("ResourceBundleSearchTest starting test #"+(testnb++)+": "+getTimeStamp());
        assertTrue(testGetBundleFromSecondCallersClassLoader(),
                   "6-testGetBundleFromSecondCallersClassLoader");

        System.out.println("ResourceBundleSearchTest generating report: "+getTimeStamp());
        report();
    }

    private void report() throws Exception {
        System.out.println("Num passed = " + numPass + " Num failed = " + numFail);
        if (numFail > 0) {
            for (String msg : msgs) {
                System.out.println(msg);
            }
            throw new Exception(numFail + " out of " + (numPass + numFail)
                                 + " tests failed.");
        }
    }

    public void assertTrue(boolean testResult, String testName) {
        if (testResult) {
            numPass++;
            System.out.println("PASSED: " + testName);
        } else {
            numFail++;
            System.out.println("FAILED: " + testName
                               + " was supposed to return true but did NOT!");
        }
    }

    public void assertFalse(boolean testResult, String testName) {
        if (!testResult) {
            numPass++;
            System.out.println("PASSED: " + testName);
        } else {
            numFail++;
            System.out.println("FAILED: " + testName
                               + " was supposed to return false but did NOT!");
        }
    }

    public boolean testGetBundleFromStackSearch() throws Throwable {
        TwiceIndirectlyLoadABundle indirectLoader = new TwiceIndirectlyLoadABundle();
        return indirectLoader.loadAndTest();
    }

    public boolean testGetBundleFromCallersClassLoader() throws Throwable {
        IndirectlyLoadABundle indirectLoader = new IndirectlyLoadABundle();
        return indirectLoader.loadAndTest();
    }

    public boolean testGetBundleFromTCCL(String bundleName,
            ClassLoader setOnTCCL) throws InterruptedException {
        debug("Looking for " + bundleName + " using TCCL");
        LoggingThread lr = new LoggingThread(bundleName, setOnTCCL);
        lr.start();
        try {
            lr.join();
        } catch (InterruptedException ex) {
            throw ex;
        }
        msgs.add(lr.msg);
        return lr.foundBundle;
    }

    /*
     * @param String bundleClass
     * @param ClassLoader to use for search
     * @return true iff bundleClass is on system classpath
     */
    public static boolean isOnClassPath(String baseName, ClassLoader cl) {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(baseName, Locale.getDefault(), cl);
            System.out.println("INFO: Found bundle " + baseName + " on " + cl);
        } catch (MissingResourceException e) {
            System.out.println("INFO: Could not find bundle " + baseName + " on " + cl);
            return false;
        }
        return (rb != null);
    }

    private static String newLoggerName() {
        loggerNum++;
        return LOGGER_PREFIX + loggerNum;
    }

    public boolean testGetBundleFromSystemClassLoader(String bundleName) {
        try {
            Logger aLogger = Logger.getLogger(ResourceBundleSearchTest.newLoggerName(),
                    bundleName);
        } catch (MissingResourceException re) {
            msgs.add("INFO: testGetBundleFromSystemClassLoader() did not find bundle "
                     + bundleName);
            return false;
        }
        msgs.add("INFO: testGetBundleFromSystemClassLoader() found the bundle "
                 + bundleName);
        return true;
    }

    private boolean testGetAnonymousLogger() throws Throwable {
        IndirectlyLoadABundle indirectLoader = new IndirectlyLoadABundle();
        return indirectLoader.testGetAnonymousLogger();
    }

    private boolean testGetBundleFromSecondCallersClassLoader() throws Throwable {
        IndirectlyLoadABundle indirectLoader = new IndirectlyLoadABundle();
        return indirectLoader.testGetLoggerGetLoggerWithBundle();
    }

    public static class LoggingThread extends Thread {

        boolean foundBundle = false;
        String msg = null;
        ClassLoader clToSetOnTCCL = null;
        String bundleName = null;

        public LoggingThread(String bundleName) {
            this.bundleName = bundleName;
        }

        public LoggingThread(String bundleName, ClassLoader setOnTCCL) {
            this.clToSetOnTCCL = setOnTCCL;
            this.bundleName = bundleName;
        }

        public void run() {
            boolean setTCCL = false;
            try {
                if (clToSetOnTCCL != null) {
                    Thread.currentThread().setContextClassLoader(clToSetOnTCCL);
                    setTCCL = true;
                }
                try {
                    Logger aLogger = Logger.getLogger(ResourceBundleSearchTest.newLoggerName(),
                                                      bundleName);
                    msg = "INFO: LoggingThread.run() found the bundle " + bundleName
                          + (setTCCL ? " with " : " without ") + "setting the TCCL";
                    foundBundle = true;
                } catch (MissingResourceException re) {
                    msg = "INFO: LoggingThread.run() did not find the bundle " + bundleName
                          + (setTCCL ? " with " : " without ") + "setting the TCCL";
                    foundBundle = false;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private void debug(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
}
