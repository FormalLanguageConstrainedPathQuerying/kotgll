/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8036823
 * @bug     8046287
 * @summary Creates two threads contending for the same lock and checks
 *      whether jstack reports "locked" by more than one thread.
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/othervm TestThreadDumpMonitorContention
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class TestThreadDumpMonitorContention {
    final static String JSTACK = JDKToolFinder.getTestJDKTool("jstack");
    final static String PID = Long.toString(ProcessHandle.current().pid());

    final static Pattern HEADER_PREFIX_PATTERN = Pattern.compile(
        "^\"ContendingThread-.*");
    final static Pattern HEADER_WAITING_PATTERN1 = Pattern.compile(
        "^\"ContendingThread-.* waiting for monitor entry .*");
    final static Pattern HEADER_WAITING_PATTERN2 = Pattern.compile(
        "^\"ContendingThread-.* waiting on condition .*");
    final static Pattern HEADER_RUNNABLE_PATTERN = Pattern.compile(
        "^\"ContendingThread-.* runnable .*");

    final static Pattern THREAD_STATE_PREFIX_PATTERN = Pattern.compile(
        " *java\\.lang\\.Thread\\.State: .*");
    final static Pattern THREAD_STATE_BLOCKED_PATTERN = Pattern.compile(
        " *java\\.lang\\.Thread\\.State: BLOCKED \\(on object monitor\\)");
    final static Pattern THREAD_STATE_RUNNABLE_PATTERN = Pattern.compile(
        " *java\\.lang\\.Thread\\.State: RUNNABLE");

    final static Pattern LOCK_PATTERN = Pattern.compile(
        ".* locked \\<.*\\(a TestThreadDumpMonitorContention.*");

    final static Pattern WAITING_PATTERN = Pattern.compile(
        ".* waiting to lock \\<.*\\(a TestThreadDumpMonitorContention.*");

    final static Object barrier = new Object();
    volatile static boolean done = false;

    static int barrier_cnt = 0;
    static int blank_line_match_cnt = 0;
    static int error_cnt = 0;
    static boolean have_header_line = false;
    static boolean have_thread_state_line = false;
    static String header_line = null;
    static int header_prefix_match_cnt = 0;
    static int locked_line_match_cnt = 0;
    static String[] locked_match_list = new String[2];
    static int n_samples = 15;
    static int sum_both_running_cnt = 0;
    static int sum_both_waiting_cnt = 0;
    static int sum_contended_cnt = 0;
    static int sum_locked_hdr_runnable_cnt = 0;
    static int sum_locked_hdr_waiting1_cnt = 0;
    static int sum_locked_hdr_waiting2_cnt = 0;
    static int sum_locked_thr_state_blocked_cnt = 0;
    static int sum_locked_thr_state_runnable_cnt = 0;
    static int sum_one_waiting_cnt = 0;
    static int sum_uncontended_cnt = 0;
    static int sum_waiting_hdr_waiting1_cnt = 0;
    static int sum_waiting_thr_state_blocked_cnt = 0;
    static String thread_state_line = null;
    static boolean verbose = false;
    static int waiting_line_match_cnt = 0;

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            int arg_i = 0;
            if (args[arg_i].equals("-v")) {
                verbose = true;
                arg_i++;
            }

            try {
                n_samples = Integer.parseInt(args[arg_i]);
            } catch (NumberFormatException nfe) {
                System.err.println(nfe);
                usage();
            }
        }

        Runnable runnable = new Runnable() {
            public void run() {
                synchronized (barrier) {
                    barrier_cnt++;
                    barrier.notify();
                }
                while (!done) {
                    synchronized (this) { }
                }
            }
        };
        Thread[] thread_list = new Thread[2];
        thread_list[0] = new Thread(runnable, "ContendingThread-1");
        thread_list[1] = new Thread(runnable, "ContendingThread-2");
        synchronized (barrier) {
            thread_list[0].start();
            thread_list[1].start();

            while (barrier_cnt < 2) {
                barrier.wait();
            }
        }

        doSamples();

        done = true;

        thread_list[0].join();
        thread_list[1].join();

        if (error_cnt == 0) {
            System.out.println("Test PASSED.");
        } else {
            System.out.println("Test FAILED.");
            throw new AssertionError("error_cnt=" + error_cnt);
        }
    }

    static boolean checkBlankLine(String line) {
        if (line.length() == 0) {
            blank_line_match_cnt++;
            have_header_line = false;
            have_thread_state_line = false;
            return true;
        }

        return false;
    }

    static boolean checkLockedLine(String line) {
        Matcher matcher = LOCK_PATTERN.matcher(line);
        if (matcher.matches()) {
            if (verbose) {
                System.out.println("locked_line='" + line + "'");
            }
            locked_match_list[locked_line_match_cnt] = new String(line);
            locked_line_match_cnt++;

            matcher = HEADER_RUNNABLE_PATTERN.matcher(header_line);
            if (matcher.matches()) {
                sum_locked_hdr_runnable_cnt++;
            } else {
                matcher = HEADER_WAITING_PATTERN1.matcher(header_line);
                if (matcher.matches()) {
                    sum_locked_hdr_waiting1_cnt++;
                } else {
                    matcher = HEADER_WAITING_PATTERN2.matcher(header_line);
                    if (matcher.matches()) {
                        sum_locked_hdr_waiting2_cnt++;
                    } else {
                        System.err.println();
                        System.err.println("ERROR: header line does " +
                            "not match runnable or waiting patterns.");
                        System.err.println("ERROR: header_line='" +
                            header_line + "'");
                        System.err.println("ERROR: locked_line='" + line +
                            "'");
                        error_cnt++;
                    }
                }
            }

            matcher = THREAD_STATE_RUNNABLE_PATTERN.matcher(thread_state_line);
            if (matcher.matches()) {
                sum_locked_thr_state_runnable_cnt++;
            } else {
                matcher = THREAD_STATE_BLOCKED_PATTERN.matcher(
                              thread_state_line);
                if (matcher.matches()) {
                    sum_locked_thr_state_blocked_cnt++;
                } else {
                    System.err.println();
                    System.err.println("ERROR: thread state line does not " +
                        "match runnable or waiting patterns.");
                    System.err.println("ERROR: " + "thread_state_line='" +
                        thread_state_line + "'");
                    System.err.println("ERROR: locked_line='" + line + "'");
                    error_cnt++;
                }
            }

            have_header_line = false;
            have_thread_state_line = false;
            return true;
        }

        return false;
    }

    static boolean checkWaitingLine(String line) {
        Matcher matcher = WAITING_PATTERN.matcher(line);
        if (matcher.matches()) {
            waiting_line_match_cnt++;
            if (verbose) {
                System.out.println("waiting_line='" + line + "'");
            }

            matcher = HEADER_WAITING_PATTERN1.matcher(header_line);
            if (matcher.matches()) {
                sum_waiting_hdr_waiting1_cnt++;
            } else {
                System.err.println();
                System.err.println("ERROR: header line does " +
                    "not match a waiting pattern.");
                System.err.println("ERROR: header_line='" + header_line + "'");
                System.err.println("ERROR: waiting_line='" + line + "'");
                error_cnt++;
            }

            matcher = THREAD_STATE_BLOCKED_PATTERN.matcher(thread_state_line);
            if (matcher.matches()) {
                sum_waiting_thr_state_blocked_cnt++;
            } else {
                System.err.println();
                System.err.println("ERROR: thread state line " +
                    "does not match a waiting pattern.");
                System.err.println("ERROR: thread_state_line='" +
                    thread_state_line + "'");
                System.err.println("ERROR: waiting_line='" + line + "'");
                error_cnt++;
            }

            have_header_line = false;
            have_thread_state_line = false;
            return true;
        }

        return false;
    }

    static void doSamples() throws Exception {
        for (int count = 0; count < n_samples; count++) {
            blank_line_match_cnt = 0;
            header_prefix_match_cnt = 0;
            locked_line_match_cnt = 0;
            waiting_line_match_cnt = 0;
            if (verbose || error_cnt > 0) System.out.println();
            System.out.println("Sample #" + count);

            Process process = new ProcessBuilder(JSTACK, PID)
                .redirectErrorStream(true).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = null;

                if (!have_header_line) {
                    matcher = HEADER_PREFIX_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        header_prefix_match_cnt++;
                        if (verbose) {
                            System.out.println();
                            System.out.println("header='" + line + "'");
                        }
                        header_line = new String(line);
                        have_header_line = true;
                        continue;
                    }
                    continue;  
                }

                if (!have_thread_state_line) {
                    matcher = THREAD_STATE_PREFIX_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        if (verbose) {
                            System.out.println("thread_state='" + line + "'");
                        }
                        thread_state_line = new String(line);
                        have_thread_state_line = true;
                        continue;
                    }
                    continue;  
                }

                if (checkLockedLine(line)) {
                    continue;
                }

                if (checkWaitingLine(line)) {
                    continue;
                }

                if (checkBlankLine(line)) {
                    continue;
                }
            }
            process.waitFor();

            if (header_prefix_match_cnt != 2) {
                System.err.println();
                System.err.println("ERROR: should match exactly two headers.");
                System.err.println("ERROR: header_prefix_match_cnt=" +
                    header_prefix_match_cnt);
                error_cnt++;
            }

            if (locked_line_match_cnt == 2) {
                if (locked_match_list[0].equals(locked_match_list[1])) {
                    System.err.println();
                    System.err.println("ERROR: matching lock lines:");
                    System.err.println("ERROR: line[0]'" +
                        locked_match_list[0] + "'");
                    System.err.println("ERROR: line[1]'" +
                        locked_match_list[1] + "'");
                    error_cnt++;
                }
            }

            if (locked_line_match_cnt == 1) {
                if (waiting_line_match_cnt == 1) {
                    sum_contended_cnt++;
                } else {
                    sum_uncontended_cnt++;
                }
            } else if (waiting_line_match_cnt == 1) {
                sum_one_waiting_cnt++;
            } else if (waiting_line_match_cnt == 2) {
                sum_both_waiting_cnt++;
            } else {
                sum_both_running_cnt++;
            }

            Thread.sleep(500);
        }

        if (error_cnt != 0) {
            return;
        }

        System.out.println("INFO: Summary for all samples:");
        System.out.println("INFO: both_running_cnt=" + sum_both_running_cnt);
        System.out.println("INFO: both_waiting_cnt=" + sum_both_waiting_cnt);
        System.out.println("INFO: contended_cnt=" + sum_contended_cnt);
        System.out.println("INFO: one_waiting_cnt=" + sum_one_waiting_cnt);
        System.out.println("INFO: uncontended_cnt=" + sum_uncontended_cnt);
        System.out.println("INFO: locked_hdr_runnable_cnt=" +
            sum_locked_hdr_runnable_cnt);
        System.out.println("INFO: locked_hdr_waiting1_cnt=" +
            sum_locked_hdr_waiting1_cnt);
        System.out.println("INFO: locked_hdr_waiting2_cnt=" +
            sum_locked_hdr_waiting2_cnt);
        System.out.println("INFO: locked_thr_state_blocked_cnt=" +
            sum_locked_thr_state_blocked_cnt);
        System.out.println("INFO: locked_thr_state_runnable_cnt=" +
            sum_locked_thr_state_runnable_cnt);
        System.out.println("INFO: waiting_hdr_waiting1_cnt=" +
            sum_waiting_hdr_waiting1_cnt);
        System.out.println("INFO: waiting_thr_state_blocked_cnt=" +
            sum_waiting_thr_state_blocked_cnt);

        if (sum_contended_cnt == 0) {
            System.err.println("WARNING: the primary scenario for 8036823" +
                " has not been exercised by this test run.");
        }
    }

    static void usage() {
        System.err.println("Usage: " +
            "java TestThreadDumpMonitorContention [-v] [n_samples]");
        System.exit(1);
    }
}
