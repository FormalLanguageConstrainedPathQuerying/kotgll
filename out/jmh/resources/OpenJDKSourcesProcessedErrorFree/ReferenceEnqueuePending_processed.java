/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4243978
 * @summary Test if Reference.enqueue() works properly with pending references
 */
import java.lang.ref.*;

public class ReferenceEnqueuePending {
    static class NumberedWeakReference extends WeakReference<Integer> {
        int number;

        NumberedWeakReference(Integer referent, ReferenceQueue<Integer> q, int i) {
            super(referent, q);
            number = i;
        }
    }

    static final boolean debug = System.getProperty("test.debug") != null;
    static final int iterations = 1000;
    static final int gc_trigger = 99;
    static int[] a = new int[2 * iterations];
    static NumberedWeakReference[] b = new NumberedWeakReference[iterations];

    public static void main(String[] argv) throws Exception {
        if (debug) {
            System.out.println("Starting the test.");
        }
        raisePriority();

        ReferenceQueue<Integer> refQueue = new ReferenceQueue<>();


        Integer obj = new Integer(0);
        NumberedWeakReference weaky = new NumberedWeakReference(obj, refQueue, 0);
        for (int i = 1; i < iterations; i++) {
            obj = new Integer(i);
            if ((i % gc_trigger) == 0) {
                forceGc(0);
            }
            if ((i % 2) == 0) {
                weaky.enqueue();
            }
            b[i - 1] = weaky;
            weaky = new NumberedWeakReference(obj, refQueue, i);
        }

        forceGc(100);
        forceGc(100);

        checkResult(refQueue, iterations-1);

        Reference.reachabilityFence(weaky);
        Reference.reachabilityFence(obj);

        System.out.println("Test passed.");
    }

    private static NumberedWeakReference waitForReference(ReferenceQueue<Integer> queue) {
        try {
            return (NumberedWeakReference) queue.remove(30000); 
        } catch (InterruptedException ie) {
            return null;
        }
    }

    private static void checkResult(ReferenceQueue<Integer> queue,
                                    int expected) {
        if (debug) {
            System.out.println("Reading the queue");
        }

        NumberedWeakReference weakRead = waitForReference(queue);
        int length = 0;
        while (weakRead != null) {
            a[length++] = weakRead.number;
            if (length < expected) {
                weakRead = waitForReference(queue);
            } else {            
                weakRead = (NumberedWeakReference) queue.poll();
            }
        }
        if (debug) {
            System.out.println("Reference Queue had " + length + " elements");
        }


        if (debug) {
            System.out.println("Start of final check");
        }

        sort(length);

        boolean fail = (length != expected);
        for (int i = 0; i < length; i++) {
            if (a[i] != i) {
                if (debug) {
                    System.out.println("a[" + i + "] is not " + i + " but " + a[i]);
                }
                fail = true;
            }
        }
        if (fail) {
             printMissingElements(length, expected);
             throw new RuntimeException("TEST FAILED: only " + length
                    + " reference objects have been queued out of "
                    + expected);
        }
    }

    private static void printMissingElements(int length, int expected) {
        System.out.println("The following numbers were not found in the reference queue: ");
        int missing = 0;
        int element = 0;
        for (int i = 0; i < length; i++) {
            while ((a[i] != element) & (element < expected)) {
                System.out.print(element + " ");
                if (missing % 20 == 19) {
                    System.out.println(" ");
                }
                missing++;
                element++;
            }
            element++;
        }
        System.out.print("\n");
    }

    private static void forceGc(long millis) throws InterruptedException {
        Runtime.getRuntime().gc();
        Thread.sleep(millis);
    }

    private static void sort(int length) {
        int hold;
        if (debug) {
            System.out.println("Sorting. Length=" + length);
        }
        for (int pass = 1; pass < length; pass++) {    
            for (int i = 0; i < length - pass; i++) {  
                if (a[i] > a[i + 1]) {  
                    hold = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = hold;
                }
            }  
        } 
    }

    static void raisePriority() {
        Thread tr = Thread.currentThread();
        tr.setPriority(Thread.MAX_PRIORITY);
    }
}   
