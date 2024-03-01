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
 * Unit tests for JVMTI IterateOverReachableObjects and
 * IterateOverObjectsReachableFromObject functions.
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;

public class HeapWalkTests {

    final static int JCK_STATUS_BASE = 95;
    final static int PASSED = 0;
    final static int FAILED = 2;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        test1();
        test2();
        test3();
        return PASSED;
    }

    private static void test1() {
        long tag;



        Object o = new Object();
        Heap.newGlobalRef(o);

        Heap.setTag(Thread.currentThread(), 0);

        Heap.setHeapRootCallback();
        Heap.iterateOverReachableObjects();


        tag = Heap.getTag(o);

        if (tag != Heap.JVMTI_HEAP_ROOT_JNI_GLOBAL) {
            throw new RuntimeException("JNI global should have been tagged");
        }


        tag = Heap.getTag(Thread.currentThread());
        if (tag == 0) {
            throw new RuntimeException("Current thread isn't tagged");
        }
    }

    private static void test2() {
        long tag;


        Object o = new Object();

        Heap.setTag(Thread.currentThread(), 888);

        Heap.setStackRefCallback();
        Heap.iterateOverReachableObjects();

        tag = Heap.getTag(o);
        if (tag != 888) {
            throw new RuntimeException("stack local not tagged correctly");
        }
    }


    static class Foo {
        private Object fld;

        Foo() {
            fld = new Object();
        }

        Object field() {
            return fld;
        }

        public static Object static_field = new Object();
    }

    private static int failures = 0;

    private static void check(Object o, long tag) {
        long actual_tag = Heap.getTag(o);
        if (actual_tag != tag) {
            if (actual_tag == 0) {
                System.err.println(o + " is not tagged!");
            } else {
                System.err.println(o + " is incorrectly tagged");
            }
            failures++;
        }
    }

    private static void test3() {
        long tag;


        Foo foo = new Foo();

        Heap.setObjectRefCallback();
        Heap.iterateOverObjectsReachableFromObject(foo);

        check(Foo.class, 777);
        check(Foo.static_field, 777);
        check(foo.field(), 777);

        if (failures > 0) {
            throw new RuntimeException("IterateOverObjectsReachableFromObject test failed");
        }
    }
}
