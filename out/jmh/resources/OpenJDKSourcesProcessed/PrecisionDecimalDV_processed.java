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

/**
 * Validator for <precisionDecimal> datatype (W3C Schema 1.1)
 *
 * @xerces.experimental
 *
 * @author Ankit Pasricha, IBM
 *
 */
class PrecisionDecimalDV extends TypeValidator {

    static final class XPrecisionDecimal {

        int sign = 1;
        int totalDigits = 0;
        int intDigits = 0;
        int fracDigits = 0;
        String ivalue = "";
        String fvalue = "";

        int pvalue = 0;


        XPrecisionDecimal(String content) throws NumberFormatException {
            if(content.equals("NaN")) {
                ivalue = content;
                sign = 0;
            }
            if(content.equals("+INF") || content.equals("INF") || content.equals("-INF")) {
                ivalue = content.charAt(0) == '+' ? content.substring(1) : content;
                return;
            }
            initD(content);
        }

        void initD(String content) throws NumberFormatException {
            int len = content.length();
            if (len == 0)
                throw new NumberFormatException();

            int intStart = 0, intEnd = 0, fracStart = 0, fracEnd = 0;

            if (content.charAt(0) == '+') {
                intStart = 1;
            }
            else if (content.charAt(0) == '-') {
                intStart = 1;
                sign = -1;
            }

            int actualIntStart = intStart;
            while (actualIntStart < len && content.charAt(actualIntStart) == '0') {
                actualIntStart++;
            }

            for (intEnd = actualIntStart; intEnd < len && TypeValidator.isDigit(content.charAt(intEnd)); intEnd++);

            if (intEnd < len) {
                if (content.charAt(intEnd) != '.' && content.charAt(intEnd) != 'E' && content.charAt(intEnd) != 'e')
                    throw new NumberFormatException();

                if(content.charAt(intEnd) == '.') {
                    fracStart = intEnd + 1;

                    for (fracEnd = fracStart;
                    fracEnd < len && TypeValidator.isDigit(content.charAt(fracEnd));
                    fracEnd++);
                }
                else {
                    pvalue = Integer.parseInt(content.substring(intEnd + 1, len));
                }
            }

            if (intStart == intEnd && fracStart == fracEnd)
                throw new NumberFormatException();

            /*while (fracEnd > fracStart && content.charAt(fracEnd-1) == '0') {
             fracEnd--;
             }*/

            for (int fracPos = fracStart; fracPos < fracEnd; fracPos++) {
                if (!TypeValidator.isDigit(content.charAt(fracPos)))
                    throw new NumberFormatException();
            }

            intDigits = intEnd - actualIntStart;
            fracDigits = fracEnd - fracStart;

            if (intDigits > 0) {
                ivalue = content.substring(actualIntStart, intEnd);
            }

            if (fracDigits > 0) {
                fvalue = content.substring(fracStart, fracEnd);
                if(fracEnd < len) {
                    pvalue = Integer.parseInt(content.substring(fracEnd + 1, len));
                }
            }
            totalDigits = intDigits + fracDigits;
        }

        private static String canonicalToStringForHashCode(String ivalue, String fvalue, int sign, int pvalue) {
            if ("NaN".equals(ivalue)) {
                return "NaN";
            }
            if ("INF".equals(ivalue)) {
                return sign < 0 ? "-INF" : "INF";
            }
            final StringBuilder builder = new StringBuilder();
            final int ilen = ivalue.length();
            final int flen0 = fvalue.length();
            int lastNonZero;
            for (lastNonZero = flen0; lastNonZero > 0 ; lastNonZero--) {
                if (fvalue.charAt(lastNonZero -1 ) != '0') break;
            }
            final int flen = lastNonZero;
            int iStart;
            int exponent = pvalue;
            for (iStart = 0; iStart < ilen; iStart++) {
                if (ivalue.charAt(iStart) != '0') break;
            }
            int fStart = 0;
            if (iStart < ivalue.length()) {
                builder.append(sign == -1 ? "-" : "");
                builder.append(ivalue.charAt(iStart));
                iStart++;
            } else {
                if (flen > 0) {
                    for (fStart = 0; fStart < flen; fStart++) {
                        if (fvalue.charAt(fStart) != '0') break;
                    }
                    if (fStart < flen) {
                        builder.append(sign == -1 ? "-" : "");
                        builder.append(fvalue.charAt(fStart));
                        exponent -= ++fStart;
                    } else {
                        return "0";
                    }
                } else {
                    return "0";
                }
            }

            if (iStart < ilen || fStart < flen) {
                builder.append('.');
            }
            while (iStart < ilen) {
                builder.append(ivalue.charAt(iStart++));
                exponent++;
            }
            while (fStart < flen) {
                builder.append(fvalue.charAt(fStart++));
            }
            if (exponent != 0) {
                builder.append("E").append(exponent);
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object val) {
            if (val == this)
                return true;

            if (!(val instanceof XPrecisionDecimal))
                return false;
            XPrecisionDecimal oval = (XPrecisionDecimal)val;

            return this.compareTo(oval) == EQUAL;
        }

        @Override
        public int hashCode() {
            return canonicalToStringForHashCode(ivalue, fvalue, sign, pvalue).hashCode();
        }

        /**
         * @return
         */
        private int compareFractionalPart(XPrecisionDecimal oval) {
            if(fvalue.equals(oval.fvalue))
                return EQUAL;

            StringBuffer temp1 = new StringBuffer(fvalue);
            StringBuffer temp2 = new StringBuffer(oval.fvalue);

            truncateTrailingZeros(temp1, temp2);
            return temp1.toString().compareTo(temp2.toString());
        }

        private void truncateTrailingZeros(StringBuffer fValue, StringBuffer otherFValue) {
            for(int i = fValue.length() - 1;i >= 0; i--)
                if(fValue.charAt(i) == '0')
                    fValue.deleteCharAt(i);
                else
                    break;

            for(int i = otherFValue.length() - 1;i >= 0; i--)
                if(otherFValue.charAt(i) == '0')
                    otherFValue.deleteCharAt(i);
                else
                    break;
        }

        public int compareTo(XPrecisionDecimal val) {

            if(sign == 0)
                return INDETERMINATE;

            if(ivalue.equals("INF") || val.ivalue.equals("INF")) {
                if(ivalue.equals(val.ivalue))
                    return EQUAL;
                else if(ivalue.equals("INF"))
                    return GREATER_THAN;
                return LESS_THAN;
            }

            if(ivalue.equals("-INF") || val.ivalue.equals("-INF")) {
                if(ivalue.equals(val.ivalue))
                    return EQUAL;
                else if(ivalue.equals("-INF"))
                    return LESS_THAN;
                return GREATER_THAN;
            }

            if (sign != val.sign)
                return sign > val.sign ? GREATER_THAN : LESS_THAN;

            return sign * compare(val);
        }

        private int compare(XPrecisionDecimal val) {

            if(pvalue != 0 || val.pvalue != 0) {
                if(pvalue == val.pvalue)
                    return intComp(val);
                else {

                    if(intDigits + pvalue != val.intDigits + val.pvalue)
                        return intDigits + pvalue > val.intDigits + val.pvalue ? GREATER_THAN : LESS_THAN;

                    if(pvalue > val.pvalue) {
                        int expDiff = pvalue - val.pvalue;
                        StringBuffer buffer = new StringBuffer(ivalue);
                        StringBuffer fbuffer = new StringBuffer(fvalue);
                        for(int i = 0;i < expDiff; i++) {
                            if(i < fracDigits) {
                                buffer.append(fvalue.charAt(i));
                                fbuffer.deleteCharAt(i);
                            }
                            else
                                buffer.append('0');
                        }
                        return compareDecimal(buffer.toString(), val.ivalue, fbuffer.toString(), val.fvalue);
                    }
                    else {
                        int expDiff = val.pvalue - pvalue;
                        StringBuffer buffer = new StringBuffer(val.ivalue);
                        StringBuffer fbuffer = new StringBuffer(val.fvalue);
                        for(int i = 0;i < expDiff; i++) {
                            if(i < val.fracDigits) {
                                buffer.append(val.fvalue.charAt(i));
                                fbuffer.deleteCharAt(i);
                            }
                            else
                                buffer.append('0');
                        }
                        return compareDecimal(ivalue, buffer.toString(), fvalue, fbuffer.toString());
                    }
                }
            }
            else {
                return intComp(val);
            }
        }

        /**
         * @param val
         * @return
         */
        private int intComp(XPrecisionDecimal val) {
            if (intDigits != val.intDigits)
                return intDigits > val.intDigits ? GREATER_THAN : LESS_THAN;

            return compareDecimal(ivalue, val.ivalue, fvalue, val.fvalue);
        }

        /**
         * @param val
         * @return
         */
        private int compareDecimal(String iValue, String fValue, String otherIValue, String otherFValue) {
            int ret = iValue.compareTo(otherIValue);
            if (ret != 0)
                return ret > 0 ? GREATER_THAN : LESS_THAN;

            if(fValue.equals(otherFValue))
                return EQUAL;

            StringBuffer temp1=new StringBuffer(fValue);
            StringBuffer temp2=new StringBuffer(otherFValue);

            truncateTrailingZeros(temp1, temp2);
            ret = temp1.toString().compareTo(temp2.toString());
            return ret == 0 ? EQUAL : (ret > 0 ? GREATER_THAN : LESS_THAN);
        }

        private String canonical;

        @Override
        public synchronized String toString() {
            if (canonical == null) {
                makeCanonical();
            }
            return canonical;
        }

        private void makeCanonical() {
            canonical = "TBD by Working Group";
        }

        /**
         * @param decimal
         * @return
         */
        public boolean isIdentical(XPrecisionDecimal decimal) {
            if(ivalue.equals(decimal.ivalue) && (ivalue.equals("INF") || ivalue.equals("-INF") || ivalue.equals("NaN")))
                return true;

            if(sign == decimal.sign && intDigits == decimal.intDigits && fracDigits == decimal.fracDigits && pvalue == decimal.pvalue
                    && ivalue.equals(decimal.ivalue) && fvalue.equals(decimal.fvalue))
                return true;
            return false;
        }

    }
    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.impl.dv.xs.TypeValidator#getAllowedFacets()
     */
    @Override
    public short getAllowedFacets() {
        return ( XSSimpleTypeDecl.FACET_PATTERN | XSSimpleTypeDecl.FACET_WHITESPACE | XSSimpleTypeDecl.FACET_ENUMERATION |XSSimpleTypeDecl.FACET_MAXINCLUSIVE |XSSimpleTypeDecl.FACET_MININCLUSIVE | XSSimpleTypeDecl.FACET_MAXEXCLUSIVE  | XSSimpleTypeDecl.FACET_MINEXCLUSIVE | XSSimpleTypeDecl.FACET_TOTALDIGITS | XSSimpleTypeDecl.FACET_FRACTIONDIGITS);
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.impl.dv.xs.TypeValidator#getActualValue(java.lang.String, com.sun.org.apache.xerces.internal.impl.dv.ValidationContext)
     */
    @Override
    public Object getActualValue(String content, ValidationContext context)
    throws InvalidDatatypeValueException {
        try {
            return new XPrecisionDecimal(content);
        } catch (NumberFormatException nfe) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "precisionDecimal"});
        }
    }

    @Override
    public int compare(Object value1, Object value2) {
        return ((XPrecisionDecimal)value1).compareTo((XPrecisionDecimal)value2);
    }

    @Override
    public int getFractionDigits(Object value) {
        return ((XPrecisionDecimal)value).fracDigits;
    }

    @Override
    public int getTotalDigits(Object value) {
        return ((XPrecisionDecimal)value).totalDigits;
    }

    @Override
    public boolean isIdentical(Object value1, Object value2) {
        if(!(value2 instanceof XPrecisionDecimal) || !(value1 instanceof XPrecisionDecimal))
            return false;
        return ((XPrecisionDecimal)value1).isIdentical((XPrecisionDecimal)value2);
    }
}
