/*
 * @notice
 * Copyright 2001-2014 Stephen Colebourne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.common.time;

/**
 * This class has been copied from different locations within the joda time package, as
 * these methods fast when used for rounding, as they do not require conversion to java
 * time objects
 *
 * This code has been copied from jodatime 2.10.1
 * The source can be found at https:
 *
 * See following methods have been copied (along with required helper variables)
 *
 * - org.joda.time.chrono.GregorianChronology.calculateFirstDayOfYearMillis(int year)
 * - org.joda.time.chrono.BasicChronology.getYear(int year)
 * - org.joda.time.chrono.BasicGJChronology.getMonthOfYear(long utcMillis, int year)
 * - org.joda.time.chrono.BasicGJChronology.getTotalMillisByYearMonth(int year, int month)
 */
class DateUtilsRounding {

    private static final int DAYS_0000_TO_1970 = 719527;
    private static final int MILLIS_PER_DAY = 86_400_000;
    private static final long MILLIS_PER_YEAR = 31556952000L;

    private static final long[] MIN_TOTAL_MILLIS_BY_MONTH_ARRAY;
    private static final long[] MAX_TOTAL_MILLIS_BY_MONTH_ARRAY;
    private static final int[] MIN_DAYS_PER_MONTH_ARRAY = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private static final int[] MAX_DAYS_PER_MONTH_ARRAY = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    static {
        MIN_TOTAL_MILLIS_BY_MONTH_ARRAY = new long[12];
        MAX_TOTAL_MILLIS_BY_MONTH_ARRAY = new long[12];

        long minSum = 0;
        long maxSum = 0;
        for (int i = 0; i < 11; i++) {
            long millis = MIN_DAYS_PER_MONTH_ARRAY[i] * (long) MILLIS_PER_DAY;
            minSum += millis;
            MIN_TOTAL_MILLIS_BY_MONTH_ARRAY[i + 1] = minSum;

            millis = MAX_DAYS_PER_MONTH_ARRAY[i] * (long) MILLIS_PER_DAY;
            maxSum += millis;
            MAX_TOTAL_MILLIS_BY_MONTH_ARRAY[i + 1] = maxSum;
        }
    }

    /**
     * calculates the first day of a year in milliseconds since the epoch (assuming UTC)
     *
     * @param year the year
     * @return the milliseconds since the epoch of the first of january at midnight of the specified year
     */
    static long utcMillisAtStartOfYear(final int year) {
        int leapYears = year / 100;
        if (year < 0) {
            leapYears = ((year + 3) >> 2) - leapYears + ((leapYears + 3) >> 2) - 1;
        } else {
            leapYears = (year >> 2) - leapYears + (leapYears >> 2);
            if (isLeapYear(year)) {
                leapYears--;
            }
        }

        return (year * 365L + (leapYears - DAYS_0000_TO_1970)) * MILLIS_PER_DAY; 
    }

    static boolean isLeapYear(final int year) {
        if ((year & 3) != 0) {
            return false;
        }
        if (year % 100 != 0) {
            return true;
        }
        return ((year / 100) & 3) == 0;
        /*
         * It is a little faster because it saves a division. We don't have good
         * measurements for this method on its own, but this change speeds up
         * rounding the nearest month by about 8%.
         *
         * Note: If you decompile this method to x86 assembly you won't see the
         * division you'd expect from % 100 and / 100. Instead you'll see a funny
         * sequence of bit twiddling operations which the jvm thinks is faster.
         * Division is slow so it almost certainly is.
         */
    }

    private static final long AVERAGE_MILLIS_PER_YEAR_DIVIDED_BY_TWO = MILLIS_PER_YEAR / 2;
    private static final long APPROX_MILLIS_AT_EPOCH_DIVIDED_BY_TWO = (1970L * MILLIS_PER_YEAR) / 2;

    static int getYear(final long utcMillis) {

        long unitMillis = AVERAGE_MILLIS_PER_YEAR_DIVIDED_BY_TWO;
        long i2 = (utcMillis >> 1) + APPROX_MILLIS_AT_EPOCH_DIVIDED_BY_TWO;
        if (i2 < 0) {
            i2 = i2 - unitMillis + 1;
        }
        int year = (int) (i2 / unitMillis);

        long yearStart = utcMillisAtStartOfYear(year);
        long diff = utcMillis - yearStart;

        if (diff < 0) {
            year--;
        } else if (diff >= MILLIS_PER_DAY * 365L) {
            long oneYear;
            if (isLeapYear(year)) {
                oneYear = MILLIS_PER_DAY * 366L;
            } else {
                oneYear = MILLIS_PER_DAY * 365L;
            }

            yearStart += oneYear;

            if (yearStart <= utcMillis) {
                year++;
            }
        }

        return year;
    }

    static int getMonthOfYear(final long utcMillis, final int year) {

        int i = (int) ((utcMillis - utcMillisAtStartOfYear(year)) >> 10);


        return (isLeapYear(year))
            ? ((i < 182 * 84375)
                ? ((i < 91 * 84375)
                    ? ((i < 31 * 84375) ? 1 : (i < 60 * 84375) ? 2 : 3)
                    : ((i < 121 * 84375) ? 4 : (i < 152 * 84375) ? 5 : 6))
                : ((i < 274 * 84375)
                    ? ((i < 213 * 84375) ? 7 : (i < 244 * 84375) ? 8 : 9)
                    : ((i < 305 * 84375) ? 10 : (i < 335 * 84375) ? 11 : 12)))
            : ((i < 181 * 84375)
                ? ((i < 90 * 84375)
                    ? ((i < 31 * 84375) ? 1 : (i < 59 * 84375) ? 2 : 3)
                    : ((i < 120 * 84375) ? 4 : (i < 151 * 84375) ? 5 : 6))
                : ((i < 273 * 84375)
                    ? ((i < 212 * 84375) ? 7 : (i < 243 * 84375) ? 8 : 9)
                    : ((i < 304 * 84375) ? 10 : (i < 334 * 84375) ? 11 : 12)));
    }

    static long getTotalMillisByYearMonth(final int year, final int month) {
        if (isLeapYear(year)) {
            return MAX_TOTAL_MILLIS_BY_MONTH_ARRAY[month - 1];
        } else {
            return MIN_TOTAL_MILLIS_BY_MONTH_ARRAY[month - 1];
        }
    }
}
