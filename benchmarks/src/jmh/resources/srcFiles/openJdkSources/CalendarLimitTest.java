/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4033662
 * @summary test for limit on Calendar
 * @library /java/text/testlib
 * @run junit CalendarLimitTest
 */

import java.util.*;
import java.text.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test verifies the behavior of Calendar around the very earliest limits
 * which it can handle.  It also verifies the behavior for large values of millis.
 *
 * Note: There used to be a limit, due to a bug, for early times.  There is
 * currently no limit.
 *
 * March 17, 1998: Added code to make sure big + dates are big + AD years, and
 * big - dates are big + BC years.
 */
public class CalendarLimitTest
{
    static final long EARLIEST_SUPPORTED_MILLIS = -210993120000000L;

    static final int EPOCH_JULIAN_DAY   = 2440588; 
    static final int JAN_1_1_JULIAN_DAY = 1721426; 

    static final int  ONE_SECOND = 1000;
    static final int  ONE_MINUTE = 60*ONE_SECOND;
    static final int  ONE_HOUR   = 60*ONE_MINUTE;
    static final int  ONE_DAY    = 24*ONE_HOUR;
    static final int  ONE_WEEK   = 7*ONE_DAY;
    static final long ONE_YEAR   = (long)(365.2425 * ONE_DAY);

    static long ORIGIN; 

    /**
     * Converts Julian day to time as milliseconds.
     * @param julian the given Julian day number.
     * @return time as milliseconds.
     */
    private static final long julianDayToMillis(long julian) {
        return (julian - EPOCH_JULIAN_DAY) * ONE_DAY;
    }

    /**
     * Verify that the given time is processed without problem.
     * @return the adjust year, with 0 = 1 BC, -1 = 2 BC, etc.
     */
    int test(long millis, Calendar cal, DateFormat fmt)
    {
        Exception exception = null;
        String theDate = "";
        try {
            Date d= new Date(millis);
            cal.setTime(d);
            theDate = fmt.format(d);
        }
        catch (IllegalArgumentException e) {
            exception = e;
        }
        String s = "0x" + Long.toHexString(millis) + " " + theDate;

        int era=cal.get(Calendar.ERA), year=cal.get(Calendar.YEAR),
            dom=cal.get(Calendar.DATE), mon=cal.get(Calendar.MONTH);

        cal.clear();
        cal.set(year, mon, dom);
        cal.set(Calendar.ERA, era);
        Date rt = cal.getTime();

        boolean ok = true;
        if (exception != null) {
            fail("FAIL: Exception " + s);
            ok = false;
        }
        if (((millis >= ORIGIN) && (era != GregorianCalendar.AD)) ||
                 ((millis < ORIGIN) && (era != GregorianCalendar.BC)) ||
                 (year < 1)) {
            fail("FAIL: Bad year/era " + s);
            ok = false;
        }
        if (dom<1 || dom>31) {
            fail("FAIL: Bad DOM " + s);
            ok = false;
        }
        if (Math.abs(millis - rt.getTime()) > ONE_DAY) {
            fail("FAIL: RT fail " + s + " -> 0x" +
                  Long.toHexString(rt.getTime()) + " " +
                  fmt.format(rt));
            ok = false;
        }
        if (ok) System.out.println(s);
        if (era==GregorianCalendar.BC) year = 1-year;
        return year;
    }

    @Test
    public void TestCalendarLimit()
    {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            System.out.println("Skipping this test because locale is " + locale);
            return;
        }
        ORIGIN = julianDayToMillis(JAN_1_1_JULIAN_DAY);

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Africa/Casablanca"));

        DateFormat dateFormat = DateFormat.getDateInstance();
        dateFormat.setCalendar(cal); 
        ((SimpleDateFormat)dateFormat).applyPattern("MMM d, yyyy G");

        int lastYear=0;
        boolean first=true;
        for (long m = Long.MAX_VALUE; m > 0; m >>= 1)
        {
            int y = test(m, cal, dateFormat);
            if (!first && y > lastYear)
                fail("FAIL: Years should be decreasing " + lastYear + " " + y);
            first = false;
            lastYear = y;
        }

        first = true;
        for (long m = Long.MIN_VALUE; m < 0; m /= 2) 
        {
            int y = test(m, cal, dateFormat);
            if (!first && y < lastYear)
                fail("FAIL: Years should be increasing " + lastYear + " " + y);
            first = false;
            lastYear = y;
        }

        test(EARLIEST_SUPPORTED_MILLIS,   cal, dateFormat);
        test(EARLIEST_SUPPORTED_MILLIS-1, cal, dateFormat);

        test(Long.MIN_VALUE + ONE_DAY,    cal, dateFormat);

        if (false) {
            ((SimpleDateFormat)dateFormat).applyPattern("H:mm MMM d, yyyy G");
            for (int dom=2; dom<=3; ++dom) {
                for (int h=0; h<24; ++h) {
                    cal.clear();
                    cal.set(Calendar.ERA, GregorianCalendar.BC);
                    cal.set(292269055, Calendar.DECEMBER, dom, h, 0);
                    Date d = cal.getTime();
                    cal.setTime(d);
                    System.out.println("" + h + ":00 Dec "+dom+", 292269055 BC -> " +
                          Long.toHexString(d.getTime()) + " -> " +
                          dateFormat.format(cal.getTime()));
                }
            }
            long t = 0x80000000018c5c00L; 
            while (t<0) {
                cal.setTime(new Date(t));
                System.out.println("0x" + Long.toHexString(t) + " -> " +
                      dateFormat.format(cal.getTime()));
                t -= ONE_HOUR;
            }
        }
    }
}

