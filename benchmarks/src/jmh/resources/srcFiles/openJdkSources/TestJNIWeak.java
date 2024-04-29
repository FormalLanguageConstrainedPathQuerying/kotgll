/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package gc.TestJNIWeak;

/* @test
 * @bug 8166188 8178813
 * @summary Test return of JNI weak global refs during concurrent
 * marking, verifying the use of the load barrier to keep the
 * referent alive.
 * @library /test/lib
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/native
 *    -Xbootclasspath/a:.
 *    -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    -Xint
 *    gc.TestJNIWeak.TestJNIWeak
 * @run main/othervm/native
 *    -Xbootclasspath/a:.
 *    -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    -Xcomp
 *    gc.TestJNIWeak.TestJNIWeak
 */

import jdk.test.whitebox.gc.GC;
import jdk.test.whitebox.WhiteBox;
import jtreg.SkippedException;
import java.lang.ref.Reference;

public final class TestJNIWeak {

    static {
        System.loadLibrary("TestJNIWeak");
    }

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static final class TestObject {
        public final int value;

        public TestObject(int value) {
            this.value = value;
        }
    }

    private volatile TestObject testObject = null;

    private static native void registerObject(Object o);
    private static native void unregisterObject();
    private static native Object getReturnedWeak();
    private static native Object getResolvedWeak();

    private boolean resolve = true;

    TestJNIWeak(boolean resolve) {
        this.resolve = resolve;
    }

    private Object getObject() {
        if (resolve) {
            return getResolvedWeak();
        } else {
            return getReturnedWeak();
        }
    }

    private void remember(int value) {
        TestObject o = new TestObject(value);
        registerObject(o);
        testObject = o;
    }

    private void forget() {
        unregisterObject();
        testObject = null;
    }

    private void gcUntilOld(Object o) {
        while (!WB.isObjectInOldGen(o)) {
            WB.fullGC();
        }
    }

    private void checkValue(int value) throws Exception {
        Object o = getObject();
        if (o == null) {
            throw new RuntimeException("Weak reference unexpectedly null");
        }
        TestObject t = (TestObject)o;
        if (t.value != value) {
            throw new RuntimeException("Incorrect value");
        }
    }

    private void checkSanity() throws Exception {
        System.out.println("running checkSanity");
        try {
            WB.concurrentGCAcquireControl();

            int value = 5;
            try {
                remember(value);
                checkValue(value);
            } finally {
                forget();
            }

        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    private void checkSurvival() throws Exception {
        System.out.println("running checkSurvival");
        try {
            int value = 10;
            try {
                remember(value);
                checkValue(value);
                gcUntilOld(testObject);
                WB.concurrentGCAcquireControl();
                WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
                WB.concurrentGCRunToIdle();
                checkValue(value);
            } finally {
                forget();
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    private void checkClear() throws Exception {
        System.out.println("running checkClear");
        try {
            int value = 15;
            try {
                remember(value);
                checkValue(value);
                gcUntilOld(testObject);
                WB.concurrentGCAcquireControl();
                WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
                WB.concurrentGCRunToIdle();
                checkValue(value);
                testObject = null;
                WB.concurrentGCRunTo(WB.AFTER_MARKING_STARTED);
                WB.concurrentGCRunToIdle();
                Object recorded = getObject();
                if (recorded != null) {
                    throw new RuntimeException("expected clear");
                }
            } finally {
                forget();
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    private void checkShouldNotClear() throws Exception {
        System.out.println("running checkShouldNotClear");
        try {
            int value = 20;
            try {
                remember(value);
                checkValue(value);
                gcUntilOld(testObject);
                WB.concurrentGCAcquireControl();
                checkValue(value);
                testObject = null; 
                WB.concurrentGCRunTo(WB.BEFORE_MARKING_COMPLETED);
                Object recovered = getObject();
                if (recovered == null) {
                    throw new RuntimeException("unexpected clear during mark");
                }
                WB.concurrentGCRunToIdle();
                if (getObject() == null) {
                    throw new RuntimeException("cleared jweak for live object");
                }
                Reference.reachabilityFence(recovered);
            } finally {
                forget();
            }
        } finally {
            WB.concurrentGCReleaseControl();
        }
    }

    private void check() throws Exception {
        checkSanity();
        checkSurvival();
        checkClear();
        checkShouldNotClear();
        System.out.println("Check passed");
    }

    public static void main(String[] args) throws Exception {
        if (!WB.supportsConcurrentGCBreakpoints()) {
            throw new SkippedException(
                GC.selected().name() + " doesn't support concurrent GC breakpoints");
        }

        System.out.println("Check with jweak resolved");
        new TestJNIWeak(true).check();

        System.out.println("Check with jweak returned");
        new TestJNIWeak(false).check();
    }
}
