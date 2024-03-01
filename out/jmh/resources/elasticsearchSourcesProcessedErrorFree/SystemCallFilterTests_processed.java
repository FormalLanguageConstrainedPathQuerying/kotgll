/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.bootstrap;

import org.apache.lucene.util.Constants;
import org.elasticsearch.test.ESTestCase;

/** Simple tests system call filter is working. */
public class SystemCallFilterTests extends ESTestCase {

    /** command to try to run in tests */
    static final String EXECUTABLE = Constants.WINDOWS ? "calc" : "ls";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        assumeTrue("requires system call filter installation", Natives.isSystemCallFilterInstalled());
        assumeTrue("cannot test with security manager enabled", System.getSecurityManager() == null);
        if (JNANatives.LOCAL_SYSTEM_CALL_FILTER_ALL == false) {
            try {
                SystemCallFilter.init(createTempDir());
            } catch (Exception e) {
                throw new RuntimeException("unable to forcefully apply system call filter to test thread", e);
            }
        }
    }

    public void testNoExecution() throws Exception {
        try {
            Runtime.getRuntime().exec(EXECUTABLE);
            fail("should not have been able to execute!");
        } catch (Exception expected) {
            /*
            java.io.IOException: Cannot run program "ls": error=13, Permission denied
                    at __randomizedtesting.SeedInfo.seed([65E6C4BED11899E:FC6E1CA6AA2DB634]:0)
                    at java.lang.ProcessBuilder.start(ProcessBuilder.java:1048)
                    at java.lang.Runtime.exec(Runtime.java:620)
                    ...
                  Caused by: java.io.IOException: error=13, Permission denied
                    at java.lang.UNIXProcess.forkAndExec(Native Method)
                    at java.lang.UNIXProcess.<init>(UNIXProcess.java:248)
                    at java.lang.ProcessImpl.start(ProcessImpl.java:134)
                    at java.lang.ProcessBuilder.start(ProcessBuilder.java:1029)
                    ...
            */
        }
    }

    public void testNoExecutionFromThread() throws Exception {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec(EXECUTABLE);
                    fail("should not have been able to execute!");
                } catch (Exception expected) {
                }
            }
        };
        t.start();
        t.join();
    }
}
