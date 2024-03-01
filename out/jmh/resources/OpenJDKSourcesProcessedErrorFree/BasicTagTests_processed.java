/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Unit tests for JVMTI SetTag and GetTag functions
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;

public class BasicTagTests {

    final static int JCK_STATUS_BASE = 95;
    final static int PASSED = 0;
    final static int FAILED = 2;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {


        Object[] o = new Object[50*1000];
        for (int i=0; i<o.length; i++) {
            o[i] = new Object();
            Heap.setTag(o[i], i+99);
        }


        Object not_me = new Object();

        for (int i=0; i<5; i++) {
            System.gc();
        }


        for (int i=0; i<o.length; i++) {
            long tag = Heap.getTag(o[i]);
            if (tag != i+99) {
                if (tag == 0) {
                    throw new RuntimeException("Test failed - object not tagged");
                } else {
                    throw new RuntimeException("Test failed - object incorrectly tagged");
                }
            }
        }


        if (Heap.getTag(not_me) != 0) {
            throw new RuntimeException("Test failed - object unexpectately tagged");
        }


        for (int i=0; i<o.length; i++) {
            Heap.setTag(o[i], 0);
        }

        for (int i=0; i<o.length; i++) {
            long tag = Heap.getTag(o[i]);
            if (tag != 0)  {
                throw new RuntimeException("Test failed - object unexpectately tagged");
            }
        }

        return PASSED;
    }

}
