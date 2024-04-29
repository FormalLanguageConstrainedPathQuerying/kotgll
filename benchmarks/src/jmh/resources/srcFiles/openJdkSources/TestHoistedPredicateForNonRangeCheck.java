/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8307683
 * @library /test/lib /
 * @requires vm.compiler2.enabled
 * @summary Tests that IfNode is not wrongly chosen as range check by Loop Predication leading to crashes and wrong executions.
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.predicates.TestHoistedPredicateForNonRangeCheck::test*
 *                   compiler.predicates.TestHoistedPredicateForNonRangeCheck
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.predicates.TestHoistedPredicateForNonRangeCheck::test*
 *                   -XX:LoopMaxUnroll=0 compiler.predicates.TestHoistedPredicateForNonRangeCheck
 */

/*
 * @test
 * @bug 8307683
 * @library /test/lib /
 * @summary Tests that IfNode is not wrongly chosen as range check by Loop Predication leading to crashes and wrong executions.
 * @run main/othervm -Xbatch compiler.predicates.TestHoistedPredicateForNonRangeCheck calendar
 */

package compiler.predicates;

import jdk.test.lib.Asserts;

import java.util.Calendar;
import java.util.Date;


public class TestHoistedPredicateForNonRangeCheck {
    static int iFld, iFld2;
    static int[] iArr = new int[100];

    public static void main(String[] args) {
        if (args.length == 0) {
            Integer.compareUnsigned(34, 34); 

            for (int i = 0; i < 2; i++) {
                iFld = 0;
                iFld2 = 0;
                test();
                Asserts.assertEQ(iFld, 3604, "wrong value");
                Asserts.assertEQ(iFld2, 400, "wrong value");
            }

            for (int i = 0; i < 2000; i++) {
                iFld = -100;
                testRangeCheckNode();
            }
            iFld = -1;
            iFld2 = 0;
            testRangeCheckNode();
            Asserts.assertEQ(iFld2, 36, "wrong value");
        } else {
            boolean flag = false;
            for (int i = 0; i < 10000; i++) {
                testCalendar1();
                testCalendar2(flag);
            }
        }
    }

    public static void test() {
        for (int i = -1; i < 1000; i++) {
            if (Integer.compareUnsigned(i, 100) < 0) {
                iFld2++;
                Float.isNaN(34); 
            } else {
                iFld++;
            }

            if (Integer.compareUnsigned(i, 100) >= 0) { 
                iFld++;
            } else {
                iFld2++;
                Float.isNaN(34); 
            }

            if (Integer.compareUnsigned(i, iArr.length) >= 0) { 
                iFld++;
            } else {
                iFld2++;
                Float.isNaN(34); 
            }

            if (Integer.compareUnsigned(i, iArr.length) >= 0) { 
                iFld++;
            } else {
                iFld2++;
                Float.isNaN(34); 
            }
        }
    }

    static void testRangeCheckNode() {
        int array[] = new int[34];
        for (int i = 0; i < 37; i++) {
            try {
                array[iFld] = 34; 
                iFld2++;
                Math.ceil(34); 
            } catch (Exception e) {
                iFld++;
            }
        }
    }

    static void testCalendar1() {
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.getTime();
    }

    static void testCalendar2(boolean flag) {
        flag = !flag;
        Calendar timespan = removeTime(new Date(), flag);
        timespan.getTime();
    }

    static Calendar removeTime(Date date, boolean flag) {
        Calendar calendar = Calendar.getInstance();
        if (flag) {
            calendar.setLenient(false);
        }
        calendar.setTime(date);
        calendar = removeTime(calendar);
        return calendar;
    }

    static Calendar removeTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
