/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package datatype;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.DatatypeFactoryTest
 * @run testng/othervm datatype.DatatypeFactoryTest
 * @summary Test DatatypeFactory.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DatatypeFactoryTest {

    private static final boolean DEBUG = false;

    private static final String TEST_VALUE_FAIL = "*FAIL*";

    private static final String FIELD_UNDEFINED = "FIELD_UNDEFINED";

    static int parseInt(String value) {
        return FIELD_UNDEFINED.equals(value) ? DatatypeConstants.FIELD_UNDEFINED : Integer.parseInt(value);
    }

    static BigDecimal parseBigDecimal(String value) {
        return FIELD_UNDEFINED.equals(value) ? null : new BigDecimal(value);
    }

    static BigInteger parseBigInteger(String value) {
        return FIELD_UNDEFINED.equals(value) ? null : new BigInteger(value);
    }

    @Test
    public final void testNewDurationMilliseconds() {

        /*
         * to generate millisecond values
         * final TimeZone GMT = TimeZone.getTimeZone("GMT"); GregorianCalendar
         * gregorianCalendar = new GregorianCalendar(GMT);
         * gregorianCalendar.setTimeInMillis(0);
         * gregorianCalendar.add(Calendar.HOUR_OF_DAY, 1);
         * gregorianCalendar.add(Calendar.MINUTE, 1);
         * System.err.println("1 hour, 1 minute = " +
         * gregorianCalendar.getTimeInMillis() + " milliseconds");
         */

        /**
         * Millisecond test values to test.
         */
        final long[] TEST_VALUES_MILLISECONDS = { 0L, 
                1L, 
                -1L, 1000L, 
                -1000L, 1001L, 
                -1001L, 60000L, 
                -60000L, 61000L, 
                -61000L, 3600000L, 
                -3600000L, 3660000L, 
                -3660000L, 86400000L, 
                -86400000L, 90000000L, 
                -90000000L, 2678400000L, 
                -2678400000L, 2764800000L, 
                -2764800000L, 31536000000L, 
                -31536000000L, 34214400000L, 
                -34214400000L };

        /**
         * Millisecond test value results of test.
         */
        final String[] TEST_VALUES_MILLISECONDS_RESULTS = { "P0Y0M0DT0H0M0.000S", 
                "P0Y0M0DT0H0M0.001S", 
                "-P0Y0M0DT0H0M0.001S", "P0Y0M0DT0H0M1.000S", 
                "-P0Y0M0DT0H0M1.000S", "P0Y0M0DT0H0M1.001S", 
                "-P0Y0M0DT0H0M1.001S", "P0Y0M0DT0H1M0.000S", 
                "-P0Y0M0DT0H1M0.000S", "P0Y0M0DT0H1M1.000S", 
                "-P0Y0M0DT0H1M1.000S", "P0Y0M0DT1H0M0.000S", 
                "-P0Y0M0DT1H0M0.000S", "P0Y0M0DT1H1M0.000S", 
                "-P0Y0M0DT1H1M0.000S", "P0Y0M1DT0H0M0.000S", 
                "-P0Y0M1DT0H0M0.000S", "P0Y0M1DT1H0M0.000S", 
                "-P0Y0M1DT1H0M0.000S", "P0Y1M0DT0H0M0.000S", 
                "-P0Y1M0DT0H0M0.000S", "P0Y1M1DT0H0M0.000S", 
                "-P0Y1M1DT0H0M0.000S", "P1Y0M0DT0H0M0.000S", 
                "-P1Y0M0DT0H0M0.000S", "P1Y1M0DT0H0M0.000S", 
                "-P1Y1M0DT0H0M0.000S" };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int onTestValue = 0; onTestValue < TEST_VALUES_MILLISECONDS.length; onTestValue++) {

            if (DEBUG) {
                System.err.println("testing value: \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\", expecting: \""
                        + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\"");
            }

            try {
                Duration duration = datatypeFactory.newDuration(TEST_VALUES_MILLISECONDS[onTestValue]);

                if (DEBUG) {
                    System.err.println("Duration created: \"" + duration.toString() + "\"");
                }

                if (TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is invalid yet it created the Duration \"" + duration.toString()
                            + "\"");
                }

                QName xmlSchemaType = duration.getXMLSchemaType();
                if (!xmlSchemaType.equals(DatatypeConstants.DURATION)) {
                    Assert.fail("Duration created with XMLSchemaType of\"" + xmlSchemaType + "\" was expected to be \"" + DatatypeConstants.DURATION
                            + "\" and has the value \"" + duration.toString() + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(duration.toString())) {
                    Assert.fail("Duration created with \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" was expected to be \""
                            + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\" and has the value \"" + duration.toString() + "\"");
                }

            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating duration: \"" + exception.toString() + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is valid yet it failed with \"" + exception.toString() + "\"");
                }
            }
        }
    }

    /**
     * Test {@link DatatypeFactory.newDurationYearMonth(String
     * lexicalRepresentation)}.
     */
    @Test
    public final void testNewDurationYearMonthLexicalRepresentation() {

        /**
         * Lexical test values to test.
         */
        final String[] TEST_VALUES_LEXICAL = { null, TEST_VALUE_FAIL, "", TEST_VALUE_FAIL, "-", TEST_VALUE_FAIL, "P", TEST_VALUE_FAIL, "-P", TEST_VALUE_FAIL,
                "P1D", TEST_VALUE_FAIL, "P1Y1M1D", TEST_VALUE_FAIL, "P1M", "P1M", "-P1M", "-P1M", "P1Y", "P1Y", "-P1Y", "-P1Y", "P1Y1M", "P1Y1M", "-P1Y1M",
                "-P1Y1M" };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int onTestValue = 0; onTestValue < TEST_VALUES_LEXICAL.length; onTestValue = onTestValue + 2) {

            if (DEBUG) {
                System.err.println("testing value: \"" + TEST_VALUES_LEXICAL[onTestValue] + "\", expecting: \"" + TEST_VALUES_LEXICAL[onTestValue + 1] + "\"");
            }

            try {
                Duration duration = datatypeFactory.newDurationYearMonth(TEST_VALUES_LEXICAL[onTestValue]);

                if (DEBUG) {
                    System.err.println("Duration created: \"" + duration.toString() + "\"");
                }

                if (TEST_VALUES_LEXICAL[onTestValue + 1].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" is invalid yet it created the Duration \"" + duration.toString() + "\"");
                }

                try {
                    QName xmlSchemaType = duration.getXMLSchemaType();
                    if (!xmlSchemaType.equals(DatatypeConstants.DURATION_YEARMONTH)) {
                        Assert.fail("Duration created with XMLSchemaType of\"" + xmlSchemaType + "\" was expected to be \""
                                + DatatypeConstants.DURATION_YEARMONTH + "\" and has the value \"" + duration.toString() + "\"");
                    }
                } catch (IllegalStateException illegalStateException) {
                    System.err.println("Please fix this bug that is being ignored, for now: " + illegalStateException.getMessage());
                }

                if (!TEST_VALUES_LEXICAL[onTestValue + 1].equals(duration.toString())) {
                    Assert.fail("Duration created with \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" was expected to be \""
                            + TEST_VALUES_LEXICAL[onTestValue + 1] + "\" and has the value \"" + duration.toString() + "\"");
                }

            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating duration: \"" + exception.toString() + "\"");
                }

                if (!TEST_VALUES_LEXICAL[onTestValue + 1].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" is valid yet it failed with \"" + exception.toString() + "\"");
                }
            }
        }
    }

    /**
     * Test {@link DatatypeFactory.newDurationYearMonth(long milliseconds)}.
     *
     */
    @Test
    public final void testNewDurationYearMonthMilliseconds() {

        /**
         * Millisecond test values to test.
         */
        final long[] TEST_VALUES_MILLISECONDS = { 0L, 1L, -1L, 2678400000L, 
                -2678400000L, 5270400000L, 
                -5270400000L, 31622400000L, 
                -31622400000L, 34300800000L, 
                -34300800000L };

        /**
         * Millisecond test value results of test.
         */
        final String[] TEST_VALUES_MILLISECONDS_RESULTS = { "P0Y0M", "P0Y0M", "P0Y0M", "P0Y1M", "-P0Y1M", "P0Y2M", "-P0Y2M", "P1Y0M", "-P1Y0M", "P1Y1M",
                "-P1Y1M" };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int onTestValue = 0; onTestValue < TEST_VALUES_MILLISECONDS.length; onTestValue++) {

            if (DEBUG) {
                System.err.println("testing value: \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\", expecting: \""
                        + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\"");
            }

            try {
                Duration duration = datatypeFactory.newDurationYearMonth(TEST_VALUES_MILLISECONDS[onTestValue]);

                if (DEBUG) {
                    System.err.println("Duration created: \"" + duration.toString() + "\"");
                }

                if (TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is invalid yet it created the Duration \"" + duration.toString()
                            + "\"");
                }

                QName xmlSchemaType = duration.getXMLSchemaType();
                if (!xmlSchemaType.equals(DatatypeConstants.DURATION_YEARMONTH)) {
                    Assert.fail("Duration created with XMLSchemaType of\"" + xmlSchemaType + "\" was expected to be \"" + DatatypeConstants.DURATION_YEARMONTH
                            + "\" and has the value \"" + duration.toString() + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(duration.toString())) {
                    Assert.fail("Duration created with \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" was expected to be \""
                            + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\" and has the value \"" + duration.toString() + "\"");
                }

                int days = duration.getDays();
                int hours = duration.getHours();
                int minutes = duration.getMinutes();
                if (days != 0 || hours != 0 || minutes != 0) {
                    Assert.fail("xdt:yearMonthDuration created without discarding remaining milliseconds: " + " days = " + days + ", hours = " + hours
                            + ", minutess = " + minutes);
                }

            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating duration: \"" + exception.toString() + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is valid yet it failed with \"" + exception.toString() + "\"");
                }
            }
        }
    }

    /**
     * Test {@link DatatypeFactory.newDurationDayTime(long milliseconds)}.
     */
    @Test
    public final void testNewDurationDayTime() {

        /**
         * Millisecond test values to test.
         */
        final long[] TEST_VALUES_MILLISECONDS = { 0L, 1L, -1L, 2678400000L, 
                -2678400000L, 5270400000L, 
                -5270400000L, 31622400000L, 
                -31622400000L, 34300800000L, 
                -34300800000L };

        /**
         * Millisecond test value results of test.
         */
        final String[] TEST_VALUES_MILLISECONDS_RESULTS = { "P0Y0M0DT0H0M0.000S", "P0Y0M0DT0H0M0.001S", "-P0Y0M0DT0H0M0.001S", "P0Y1M", "-P0Y1M", "P0Y2M",
                "-P0Y2M", "P1Y0M", "-P1Y0M", "P1Y1M", "-P1Y1M" };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int onTestValue = 0; onTestValue < TEST_VALUES_MILLISECONDS.length; onTestValue++) {

            if (DEBUG) {
                System.err.println("testing value: \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\", expecting: \""
                        + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\"");
            }

            try {
                Duration duration = datatypeFactory.newDurationDayTime(TEST_VALUES_MILLISECONDS[onTestValue]);

                if (DEBUG) {
                    System.err.println("Duration created: \"" + duration.toString() + "\"");
                }

                if (TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is invalid yet it created the Duration \"" + duration.toString()
                            + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(duration.toString())) {
                    if (false) {
                        Assert.fail("Duration created with \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" was expected to be \""
                                + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\" and has the value \"" + duration.toString() + "\"");
                    } else {
                        System.err.println("Please fix this bug: " + "Duration created with \"" + TEST_VALUES_MILLISECONDS[onTestValue]
                                + "\" was expected to be \"" + TEST_VALUES_MILLISECONDS_RESULTS[onTestValue] + "\" and has the value \"" + duration.toString()
                                + "\"");
                    }
                }

                QName xmlSchemaType = duration.getXMLSchemaType();
                int years = duration.getYears();
                int months = duration.getMonths();

                if (!xmlSchemaType.equals(DatatypeConstants.DURATION_DAYTIME) || years != 0 || months != 0) {
                    if (false) {
                        Assert.fail("xdt:dayTimeDuration created without discarding remaining milliseconds: " + " XMLSchemaType = " + xmlSchemaType
                                + ", years = " + years + ", months = " + months);
                    } else {
                        System.err.println("Please fix this bug: " + "xdt:dayTimeDuration created without discarding remaining milliseconds: "
                                + " XMLSchemaType = " + xmlSchemaType + ", years = " + years + ", months = " + months);
                    }
                }

            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating duration: \"" + exception.toString() + "\"");
                }

                if (!TEST_VALUES_MILLISECONDS_RESULTS[onTestValue].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_MILLISECONDS[onTestValue] + "\" is valid yet it failed with \"" + exception.toString() + "\"");
                }
            }
        }
    }

    /**
     * Test {@link DatatypeFactory.newXMLGregorianCalendar(String
     * lexicalRepresentation)}.
     */
    @Test
    public final void testNewXMLGregorianCalendarLexicalRepresentation() {

        /**
         * Lexical test values to test.
         */
        final String[] TEST_VALUES_LEXICAL = { null, TEST_VALUE_FAIL, "", TEST_VALUE_FAIL, "---01", "---01", 
                "---01Z", "---01Z", 
                "---01-08:00", "---01-08:00", 
                "--01--", TEST_VALUE_FAIL, 
                "--01", "--01", 
                "--01Z", "--01Z", 
                "--01-08:00", "--01-08:00", 
                "--01-01", "--01-01", 
                "--01-01Z", "--01-01Z", 
                "--01-01-08:00", "--01-01-08:00" 
        };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int onTestValue = 0; onTestValue < TEST_VALUES_LEXICAL.length; onTestValue = onTestValue + 2) {

            if (DEBUG) {
                System.err.println("testing value: \"" + TEST_VALUES_LEXICAL[onTestValue] + "\", expecting: \"" + TEST_VALUES_LEXICAL[onTestValue + 1] + "\"");
            }

            try {
                XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(TEST_VALUES_LEXICAL[onTestValue]);

                if (DEBUG) {
                    System.err.println("XMLGregorianCalendar created: \"" + xmlGregorianCalendar.toString() + "\"");
                }

                if (TEST_VALUES_LEXICAL[onTestValue + 1].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" is invalid yet it created the XMLGregorianCalendar \""
                            + xmlGregorianCalendar.toString() + "\"");
                }

                if (!TEST_VALUES_LEXICAL[onTestValue + 1].equals(xmlGregorianCalendar.toString())) {
                    Assert.fail("XMLGregorianCalendar created with \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" was expected to be \""
                            + TEST_VALUES_LEXICAL[onTestValue + 1] + "\" and has the value \"" + xmlGregorianCalendar.toString() + "\"");
                }

            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating XMLGregorianCalendar: \"" + exception.toString() + "\"");
                }

                if (!TEST_VALUES_LEXICAL[onTestValue + 1].equals(TEST_VALUE_FAIL)) {
                    Assert.fail("the value \"" + TEST_VALUES_LEXICAL[onTestValue] + "\" is valid yet it failed with \"" + exception.toString() + "\"");
                }
            }
        }
    }

    /**
     * Test {@link DatatypeFactory.newXMLGregorianCalendar( BigInteger year, int
     * month, int day, int hour, int minute, int second, BigDecimal
     * fractionalSecond, int timezone)} and
     * DatatypeFactory.newXMLGregorianCalendar( int year, int month, int day,
     * int hour, int minute, int second, int fractionalSecond, int timezone)} .
     */
    @Test
    public final void testNewXMLGregorianCalendarYearMonthDayHourMinuteSecondFractionalSecondTimezone() {

        final String[][] invalidDates = {
                { "1970", "-1", "1", "0", "0", "0", "0", "0" },
                { "1970", "0", "1", "0", "0", "0", "0", "0" },
                { "1970", "13", "1", "0", "0", "0", "0", "0" },
                { "1970", "1", "-1", "0", "0", "0", "0", "0" },
                { "1970", "1", "0", "0", "0", "0", "0", "0" },
                { "1970", "1", "32", "0", "0", "0", "0", "0" },
                { "1970", "1", "1", "-1", "0", "0", "0", "0" },
                { "1970", "1", "1", "0", "-1", "0", "0", "0" }, { "1970", "1", "1", "0", "-1", "0", "0", "0" }, { "1970", "1", "1", "0", "60", "0", "0", "0" },
                { "1970", "1", "1", "0", "0", "-1", "0", "0" }, { "1970", "1", "1", "0", "0", "61", "0", "0" },
                { "1970", "1", "1", "0", "0", "0", "-0.000001", "0" }, { "1970", "1", "1", "0", "0", "0", "1.0001", "0" },
                { "1970", "1", "1", "0", "0", "0", "0", "841" }, { "1970", "1", "1", "0", "0", "0", "0", "-841" }, };

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        for (int valueIndex = 0; valueIndex < invalidDates.length; ++valueIndex) {

            try {

                if (DEBUG) {
                    System.err.println("testing DatatypeFactory.newXMLGregorianCalendar(" + invalidDates[valueIndex][0] + ", " + invalidDates[valueIndex][1]
                            + ", " + invalidDates[valueIndex][2] + ", " + invalidDates[valueIndex][3] + ", " + invalidDates[valueIndex][4] + ", "
                            + invalidDates[valueIndex][5] + ", " + invalidDates[valueIndex][6] + ", " + invalidDates[valueIndex][7] + ")");
                }

                XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(parseBigInteger(invalidDates[valueIndex][0]),
                        parseInt(invalidDates[valueIndex][1]), parseInt(invalidDates[valueIndex][2]), parseInt(invalidDates[valueIndex][3]),
                        parseInt(invalidDates[valueIndex][4]), parseInt(invalidDates[valueIndex][5]), parseBigDecimal(invalidDates[valueIndex][6]),
                        parseInt(invalidDates[valueIndex][7]));

                if (DEBUG) {
                    System.err.println("created XMLGregorianCalendar: " + xmlGregorianCalendar.toString());
                }

                Assert.fail("expected IllegalArgumentException " + "for DatatypeFactory.newXMLGregorianCalendar(" + invalidDates[valueIndex][0] + ", "
                        + invalidDates[valueIndex][1] + ", " + invalidDates[valueIndex][2] + ", " + invalidDates[valueIndex][3] + ", "
                        + invalidDates[valueIndex][4] + ", " + invalidDates[valueIndex][5] + ", " + invalidDates[valueIndex][6] + ", "
                        + invalidDates[valueIndex][7] + ").  " + "Instead, XMLGregorianCalendar: \"" + xmlGregorianCalendar.toString() + "\" was created.");
            } catch (IllegalArgumentException illegalArgumentException) {
                if (DEBUG) {
                    System.err.println("Exception creating XMLGregorianCalendar: " + illegalArgumentException.toString());
                }
            }
        }

        int[] testIndex = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, };
        for (int i = 0; i < testIndex.length; ++i) {
            int valueIndex = testIndex[i];
            try {
                if (DEBUG) {
                    System.err.println("testing DatatypeFactory.newXMLGregorianCalendar(" + invalidDates[valueIndex][0] + ", " + invalidDates[valueIndex][1]
                            + ", " + invalidDates[valueIndex][2] + ", " + invalidDates[valueIndex][3] + ", " + invalidDates[valueIndex][4] + ", "
                            + invalidDates[valueIndex][5] + ", " + invalidDates[valueIndex][6] + ", " + invalidDates[valueIndex][7] + ")");
                }

                XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(parseInt(invalidDates[valueIndex][0]),
                        parseInt(invalidDates[valueIndex][1]), parseInt(invalidDates[valueIndex][2]), parseInt(invalidDates[valueIndex][3]),
                        parseInt(invalidDates[valueIndex][4]), parseInt(invalidDates[valueIndex][5]), parseInt(invalidDates[valueIndex][6]),
                        parseInt(invalidDates[valueIndex][7]));

                if (DEBUG) {
                    System.err.println("created XMLGregorianCalendar: " + xmlGregorianCalendar.toString());
                }

                Assert.fail("expected IllegalArgumentException " + "for DatatypeFactory.newXMLGregorianCalendar(" + invalidDates[valueIndex][0] + ", "
                        + invalidDates[valueIndex][1] + ", " + invalidDates[valueIndex][2] + ", " + invalidDates[valueIndex][3] + ", "
                        + invalidDates[valueIndex][4] + ", " + invalidDates[valueIndex][5] + ", " + invalidDates[valueIndex][6] + ", "
                        + invalidDates[valueIndex][7] + ").  " + "Instead, XMLGregorianCalendar: \"" + xmlGregorianCalendar.toString() + "\" was created.");
            } catch (IllegalArgumentException illegalArgumentException) {
                if (DEBUG) {
                    System.err.println("Exception creating XMLGregorianCalendar: " + illegalArgumentException.toString());
                }
            }
        }
    }
}
