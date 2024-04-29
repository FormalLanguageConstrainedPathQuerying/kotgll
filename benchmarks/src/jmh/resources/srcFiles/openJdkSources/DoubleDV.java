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

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.xs.datatypes.XSDouble;

/**
 * Represent the schema type "double"
 *
 * @xerces.internal
 *
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 * @author Sandy Gao, IBM
 *
 */
public class DoubleDV extends TypeValidator {

    public short getAllowedFacets(){
        return ( XSSimpleTypeDecl.FACET_PATTERN | XSSimpleTypeDecl.FACET_WHITESPACE | XSSimpleTypeDecl.FACET_ENUMERATION |XSSimpleTypeDecl.FACET_MAXINCLUSIVE |XSSimpleTypeDecl.FACET_MININCLUSIVE | XSSimpleTypeDecl.FACET_MAXEXCLUSIVE  | XSSimpleTypeDecl.FACET_MINEXCLUSIVE  );
    }

    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try{
            return new XDouble(content);
        } catch (NumberFormatException ex){
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "double"});
        }
    }

    public int compare(Object value1, Object value2) {
        return ((XDouble)value1).compareTo((XDouble)value2);
    }

    public boolean isIdentical (Object value1, Object value2) {
        if (value2 instanceof XDouble) {
            return ((XDouble)value1).isIdentical((XDouble)value2);
        }
        return false;
    }

    /**
     * Returns true if it's possible that the given
     * string represents a valid floating point value
     * (excluding NaN, INF and -INF).
     */
    static boolean isPossibleFP(String val) {
        final int length = val.length();
        for (int i = 0; i < length; ++i) {
            char c = val.charAt(i);
            if (!(c >= '0' && c <= '9' || c == '.' ||
                c == '-' || c == '+' || c == 'E' || c == 'e')) {
                return false;
            }
        }
        return true;
    }

    private static final class XDouble implements XSDouble {
        private final double value;
        public XDouble(String s) throws NumberFormatException {
            if (isPossibleFP(s)) {
                value = Double.parseDouble(s);
            }
            else if ( s.equals("INF") ) {
                value = Double.POSITIVE_INFINITY;
            }
            else if ( s.equals("-INF") ) {
                value = Double.NEGATIVE_INFINITY;
            }
            else if ( s.equals("NaN" ) ) {
                value = Double.NaN;
            }
            else {
                throw new NumberFormatException(s);
            }
        }

        public boolean equals(Object val) {
            if (val == this)
                return true;

            if (!(val instanceof XDouble))
                return false;
            XDouble oval = (XDouble)val;

            if (value == oval.value)
                return true;

            if (value != value && oval.value != oval.value)
                return true;

            return false;
        }

        public int hashCode() {
            if (value == 0d) {
                return 0;
            }
            return Double.hashCode(value);
        }

        public boolean isIdentical (XDouble val) {
            if (val == this) {
                return true;
            }

            if (value == val.value) {
                return (value != 0.0d ||
                    (Double.doubleToLongBits(value) == Double.doubleToLongBits(val.value)));
            }

            if (value != value && val.value != val.value)
                return true;

            return false;
        }

        private int compareTo(XDouble val) {
            double oval = val.value;

            if (value < oval)
                return -1;
            if (value > oval)
                return 1;
            if (value == oval)
                return 0;


            if (value != value) {
                if (oval != oval)
                    return 0;
                return INDETERMINATE;
            }

            return INDETERMINATE;
        }

        private String canonical;
        public synchronized String toString() {
            if (canonical == null) {
                if (value == Double.POSITIVE_INFINITY)
                    canonical = "INF";
                else if (value == Double.NEGATIVE_INFINITY)
                    canonical = "-INF";
                else if (value != value)
                    canonical = "NaN";
                else if (value == 0)
                    canonical = "0.0E1";
                else {
                    canonical = Double.toString(value);
                    if (canonical.indexOf('E') == -1) {
                        int len = canonical.length();
                        char[] chars = new char[len+3];
                        canonical.getChars(0, len, chars, 0);
                        int edp = chars[0] == '-' ? 2 : 1;
                        if (value >= 1 || value <= -1) {
                            int dp = canonical.indexOf('.');
                            for (int i = dp; i > edp; i--) {
                                chars[i] = chars[i-1];
                            }
                            chars[edp] = '.';
                            while (chars[len-1] == '0')
                                len--;
                            if (chars[len-1] == '.')
                                len++;
                            chars[len++] = 'E';
                            int shift = dp - edp;
                            chars[len++] = (char)(shift + '0');
                        }
                        else {
                            int nzp = edp + 1;
                            while (chars[nzp] == '0')
                                nzp++;
                            chars[edp-1] = chars[nzp];
                            chars[edp] = '.';
                            for (int i = nzp+1, j = edp+1; i < len; i++, j++)
                                chars[j] = chars[i];
                            len -= nzp - edp;
                            if (len == edp + 1)
                                chars[len++] = '0';
                            chars[len++] = 'E';
                            chars[len++] = '-';
                            int shift = nzp - edp;
                            chars[len++] = (char)(shift + '0');
                        }
                        canonical = new String(chars, 0, len);
                    }
                }
            }
            return canonical;
        }
        public double getValue() {
            return value;
        }
    }
} 
