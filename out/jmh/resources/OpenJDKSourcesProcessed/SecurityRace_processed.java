/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6302839
 * @summary SecurityRace System field accesses in two threads
 * @author Pete Soper
 * @build SecurityRace
 * @run main/othervm/policy=System.policy SecurityRace
 */

/*
 * By default the test runs for a very short time.  Use main arg "stress"
 * to have a real chance of exposing races. Use main arg "time" to report
 * the average nanoseconds per invocation of System.setSecurityManager and
 * System.getSecurityManager. No testing for access races is performed with
 * argument "time."
 *
 * Requires security permissions "setSecurityManager" and
 * "createSecurityManager."
 */



public class SecurityRace implements Runnable {

    static final int WARMUP_LOOPS = 100000;

    static final int TIMING_TRIALS = 10;

    static final int STRESS_MILLISECONDS = 300000;
    static final int SET_TIMING_LOOPS    = 10000;

    static int MAX_MILLISECONDS = 100;

    static final int GET_TIMING_LOOPS = 10000000;

    static volatile boolean stopthreads = false;

    static final int       GETPROPERTY_LOOPS = 30000;

    static SecurityManager sm = new SecurityManager();

    public static void main(String[] argv) throws Exception {
        String s;

        if (argv.length > 0) {
            if (argv[0].equals("time")) {

                for (int i = 0; i < WARMUP_LOOPS; i++) {
                    timeit(1, 1, 1);
                }

                System.out.println("boo");

                timeit(TIMING_TRIALS, GET_TIMING_LOOPS, SET_TIMING_LOOPS);
            } else if (argv[0].equals("stress")) {

                MAX_MILLISECONDS = STRESS_MILLISECONDS;
            } else {
                throw new RuntimeException(
                    "SecurityRace: " + argv[0]
                    + " argument to main not recognized");
            }    
        }        

        long start = System.currentTimeMillis(),
             end   = start + MAX_MILLISECONDS;

        (new Thread(new SecurityRace())).start();

        try {
            do {
                if (stopthreads) {

                    throw new RuntimeException("SecurityRace failed with NPE");
                }

                for (int i = 0; i < GETPROPERTY_LOOPS; i++) {
                    s = System.getProperty("java.version");
                }
            } while (System.currentTimeMillis() < end);
        } catch (NullPointerException e) {
            throw new RuntimeException("SecurityRace failed with NPE");
        } finally {

            stopthreads = true;
        }
    }    

    public void run() {
        try {
            while (true) {
                if (stopthreads) {
                    return;
                }

                System.setSecurityManager(sm);

                System.setSecurityManager(null);
            }
        } catch (NullPointerException e) {
            stopthreads = true;

            return;
        }
    }

    public static void timeit(int timing_trials, int get_timing_loops,
                              int set_timing_loops) {
        try {
            long start;

            for (int j = 0; j < timing_trials; j++) {
                start = System.nanoTime();

                for (int i = 0; i < get_timing_loops; i++) {
                    sm = System.getSecurityManager();
                }

                if (timing_trials > 1) {
                    System.out.println((float) (System.nanoTime() - start)
                                       / (float) get_timing_loops);
                }
            }

            for (int j = 0; j < timing_trials; j++) {
                start = System.nanoTime();

                for (int i = 0; i < set_timing_loops; i++) {
                    System.setSecurityManager(sm);
                }

                if (timing_trials > 1) {
                    System.out.println((float) (System.nanoTime() - start)
                                       / (float) set_timing_loops);
                }
            }

            return;
        } catch (Exception e) {
            throw new RuntimeException("SecurityRace got unexpected: " + e);
        }
    }    
}    
