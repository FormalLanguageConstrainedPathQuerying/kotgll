/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.dv.xs;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;

/**
 * Validator for &lt;duration&gt; datatype (W3C Schema Datatypes)
 *
 * @xerces.internal
 *
 * @author Elena Litani
 * @author Gopal Sharma, SUN Microsystem Inc.
 */
public class DurationDV extends AbstractDateTimeDV {

        public static final int DURATION_TYPE = 0;
        public static final int YEARMONTHDURATION_TYPE = 1;
        public static final int DAYTIMEDURATION_TYPE = 2;
    private final static DateTimeData[] DATETIMES= {
        new DateTimeData(1696, 9, 1, 0, 0, 0, 'Z', null, true, null),
        new DateTimeData(1697, 2, 1, 0, 0, 0, 'Z', null, true, null),
        new DateTimeData(1903, 3, 1, 0, 0, 0, 'Z', null, true, null),
        new DateTimeData(1903, 7, 1, 0, 0, 0, 'Z', null, true, null)};

    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException{
        try{
            return parse(content, DURATION_TYPE);
        } catch (Exception ex) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "duration"});
        }
    }

    /**
     * Parses, validates and computes normalized version of duration object
     *
     * @param str    The lexical representation of duration object PnYn MnDTnH nMnS
     * @param durationType
     * @return normalized date representation
     * @exception SchemaDateTimeException Invalid lexical representation
     */
    protected DateTimeData parse(String str, int durationType) throws SchemaDateTimeException{
        int len = str.length();
        DateTimeData date= new DateTimeData(str, this);

        int start = 0;
        char c=str.charAt(start++);
        if ( c!='P' && c!='-' ) {
            throw new SchemaDateTimeException();
        }
        else {
            date.utc=(c=='-')?'-':0;
            if ( c=='-' && str.charAt(start++)!='P' ) {
                throw new SchemaDateTimeException();
            }
        }

        int negate = 1;
        if ( date.utc=='-' ) {
            negate = -1;

        }
        boolean designator = false;

        int endDate = indexOf (str, start, len, 'T');
        if ( endDate == -1 ) {
            endDate = len;
        }
        else if (durationType == YEARMONTHDURATION_TYPE) {
            throw new SchemaDateTimeException();
        }

        int end = indexOf (str, start, endDate, 'Y');
        if ( end!=-1 ) {

            if (durationType == DAYTIMEDURATION_TYPE) {
                throw new SchemaDateTimeException();
            }

            date.year=negate * parseInt(str,start,end);
            start = end+1;
            designator = true;
        }

        end = indexOf (str, start, endDate, 'M');
        if ( end!=-1 ) {

            if (durationType == DAYTIMEDURATION_TYPE) {
                throw new SchemaDateTimeException();
            }

            date.month=negate * parseInt(str,start,end);
            start = end+1;
            designator = true;
        }

        end = indexOf (str, start, endDate, 'D');
        if ( end!=-1 ) {

            if(durationType == YEARMONTHDURATION_TYPE) {
                throw new SchemaDateTimeException();
            }

            date.day=negate * parseInt(str,start,end);
            start = end+1;
            designator = true;
        }

        if ( len == endDate && start!=len ) {
            throw new SchemaDateTimeException();
        }
        if ( len !=endDate ) {


            end = indexOf (str, ++start, len, 'H');
            if ( end!=-1 ) {
                date.hour=negate * parseInt(str,start,end);
                start=end+1;
                designator = true;
            }

            end = indexOf (str, start, len, 'M');
            if ( end!=-1 ) {
                date.minute=negate * parseInt(str,start,end);
                start=end+1;
                designator = true;
            }

            end = indexOf (str, start, len, 'S');
            if ( end!=-1 ) {
                date.second = negate * parseSecond(str, start, end);
                start=end+1;
                designator = true;
            }
            if ( start != len || str.charAt(--start)=='T' ) {
                throw new SchemaDateTimeException();
            }
        }

        if ( !designator ) {
            throw new SchemaDateTimeException();
        }

        return date;
    }

    /**
     * Compares 2 given durations. (refer to W3C Schema Datatypes "3.2.6 duration")
     *
     * @param date1  Unnormalized duration
     * @param date2  Unnormalized duration
     * @param strict (min/max)Exclusive strict == true ( LESS_THAN ) or ( GREATER_THAN )
     *               (min/max)Inclusive strict == false (LESS_EQUAL) or (GREATER_EQUAL)
     * @return INDETERMINATE if the order relationship between date1 and date2 is indeterminate.
     * EQUAL if the order relation between date1 and date2 is EQUAL.
     * If the strict parameter is true, return LESS_THAN if date1 is less than date2 and
     * return GREATER_THAN if date1 is greater than date2.
     * If the strict parameter is false, return LESS_THAN if date1 is less than OR equal to date2 and
     * return GREATER_THAN if date1 is greater than OR equal to date2
     */
    protected  short compareDates(DateTimeData date1, DateTimeData date2, boolean strict) {


        short resultA, resultB= INDETERMINATE;
        resultA = compareOrder (date1, date2);
        if ( resultA == 0 ) {
            return 0;
        }

        DateTimeData[] result = new DateTimeData[2];
        result[0] = new DateTimeData(null, this);
        result[1] = new DateTimeData(null, this);

        DateTimeData tempA = addDuration (date1, DATETIMES[0], result[0]);
        DateTimeData tempB = addDuration (date2, DATETIMES[0], result[1]);
        resultA =  compareOrder(tempA, tempB);
        if ( resultA == INDETERMINATE ) {
            return INDETERMINATE;
        }

        tempA = addDuration(date1, DATETIMES[1], result[0]);
        tempB = addDuration(date2, DATETIMES[1], result[1]);
        resultB = compareOrder(tempA, tempB);
        resultA = compareResults(resultA, resultB, strict);
        if (resultA == INDETERMINATE) {
            return INDETERMINATE;
        }

        tempA = addDuration(date1, DATETIMES[2], result[0]);
        tempB = addDuration(date2, DATETIMES[2], result[1]);
        resultB = compareOrder(tempA, tempB);
        resultA = compareResults(resultA, resultB, strict);
        if (resultA == INDETERMINATE) {
            return INDETERMINATE;
        }

        tempA = addDuration(date1, DATETIMES[3], result[0]);
        tempB = addDuration(date2, DATETIMES[3], result[1]);
        resultB = compareOrder(tempA, tempB);
        resultA = compareResults(resultA, resultB, strict);

        return resultA;
    }

    private short compareResults(short resultA, short resultB, boolean strict){

      if ( resultB == INDETERMINATE ) {
            return INDETERMINATE;
        }
        else if ( resultA!=resultB && strict ) {
            return INDETERMINATE;
        }
        else if ( resultA!=resultB && !strict ) {
            if ( resultA!=0 && resultB!=0 ) {
                return INDETERMINATE;
            }
            else {
                return (resultA!=0)?resultA:resultB;
            }
        }
        return resultA;
    }

    private DateTimeData addDuration(DateTimeData date, DateTimeData addto, DateTimeData duration) {


        resetDateObj(duration);
        int temp = addto.month + date.month;
        duration.month = modulo (temp, 1, 13);
        int carry = fQuotient (temp, 1, 13);

        duration.year=addto.year + date.year + carry;

        double dtemp = addto.second + date.second;
        carry = (int)Math.floor(dtemp/60);
        duration.second = dtemp - carry*60;

        temp = addto.minute +date.minute + carry;
        carry = fQuotient (temp, 60);
        duration.minute= mod(temp, 60, carry);

        temp = addto.hour + date.hour + carry;
        carry = fQuotient(temp, 24);
        duration.hour = mod(temp, 24, carry);


        duration.day=addto.day + date.day + carry;

        while ( true ) {

            temp=maxDayInMonthFor(duration.year, duration.month);
            if ( duration.day < 1 ) { 
                duration.day = duration.day + maxDayInMonthFor(duration.year, duration.month-1);
                carry=-1;
            }
            else if ( duration.day > temp ) {
                duration.day = duration.day - temp;
                carry=1;
            }
            else {
                break;
            }
            temp = duration.month+carry;
            duration.month = modulo(temp, 1, 13);
            duration.year = duration.year+fQuotient(temp, 1, 13);
        }

        duration.utc='Z';
        return duration;
    }

    protected double parseSecond(String buffer, int start, int end)
        throws NumberFormatException {
        int dot = -1;
        for (int i = start; i < end; i++) {
            char ch = buffer.charAt(i);
            if (ch == '.')
                dot = i;
            else if (ch > '9' || ch < '0')
                throw new NumberFormatException("'" + buffer + "' has wrong format");
        }
        if (dot+1 == end) {
            throw new NumberFormatException("'" + buffer + "' has wrong format");
        }
        double value = Double.parseDouble(buffer.substring(start, end));
        if (value == Double.POSITIVE_INFINITY) {
            throw new NumberFormatException("'" + buffer + "' has wrong format");
        }
        return value;
    }

    protected String dateToString(DateTimeData date) {
        StringBuffer message = new StringBuffer(30);
        if ( date.year<0 || date.month<0 || date.day<0
                || date.hour<0 || date.minute<0 || date.second<0) {
            message.append('-');
        }
        message.append('P');
        message.append((date.year < 0?-1:1) * date.year);
        message.append('Y');
        message.append((date.month < 0?-1:1) * date.month);
        message.append('M');
        message.append((date.day < 0?-1:1) * date.day);
        message.append('D');
        message.append('T');
        message.append((date.hour < 0?-1:1) * date.hour);
        message.append('H');
        message.append((date.minute < 0?-1:1) * date.minute);
        message.append('M');
        append2(message, (date.second < 0?-1:1) * date.second);
        message.append('S');

        return message.toString();
    }

    protected Duration getDuration(DateTimeData date) {
        int sign = 1;
        if ( date.year<0 || date.month<0 || date.day<0
                || date.hour<0 || date.minute<0 || date.second<0) {
            sign = -1;
        }
        return datatypeFactory.newDuration(sign == 1,
                date.year != DatatypeConstants.FIELD_UNDEFINED?BigInteger.valueOf(sign*date.year):null,
                date.month != DatatypeConstants.FIELD_UNDEFINED?BigInteger.valueOf(sign*date.month):null,
                date.day != DatatypeConstants.FIELD_UNDEFINED?BigInteger.valueOf(sign*date.day):null,
                date.hour != DatatypeConstants.FIELD_UNDEFINED?BigInteger.valueOf(sign*date.hour):null,
                date.minute != DatatypeConstants.FIELD_UNDEFINED?BigInteger.valueOf(sign*date.minute):null,
                date.second != DatatypeConstants.FIELD_UNDEFINED?new BigDecimal(String.valueOf(sign*date.second)):null);
    }
}
