/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

import javax.management.Query;
import javax.management.QueryExp;
import javax.management.ValueExp;

/**
 * Class used for building QueryExp instances of all every possible type
 * in terms of JMX API members; note that several JMX classes are private
 * and appears in the JDK API only by their serial form.
 * Comments in each case of the big switch in method getQuery() details which
 * API member we cover with a given query.
 */
public class QueryFactory extends QueryData {

    private String mbeanClassName = "";
    private String primitiveIntAttName = "IntAtt";
    private String primitiveLongAttName = "LongAtt";
    private String integerAttName = "IntegerAtt";
    private String primitiveBooleanAttName = "BooleanAtt";
    private String primitiveDoubleAttName = "DoubleAtt";
    private String primitiveFloatAttName = "FloatAtt";
    private String stringAttName = "StringAtt";
    private ArrayList<QueryExp> queries = new ArrayList<QueryExp>();

    /**
     * Creates a new instance of QueryFactory.
     * The name is the fully qualified class name of an MBean.
     * There is severe constraints on that MBean that must:
     * <ul>
     * <li>extend QueryData in order to inherit attribute values.
     * <li>define a RW attribute IntAtt of type int
     * initialized to QueryData.longValue
     * <li>define a RW attribute LongAtt of type long
     * initialized to QueryData.intValue
     * <li>define a RW attribute IntegerAtt of type Integer
     * initialized to QueryData.integerValue
     * <li>define a RW attribute BooleanAtt of type boolean
     * initialized to QueryData.booleanValue
     * <li>define a RW attribute DoubleAtt of type double
     * initialized to QueryData.doubleValue
     * <li>define a RW attribute FloatAtt of type float
     * initialized to QueryData.floatValue
     * <li>define a RW attribute StringAtt of type String
     * initialized to QueryData.stringValue
     * </ul>
     */
    public QueryFactory(String name) {
        this.mbeanClassName = name;
    }

    /**
     * Returns the highest index value the method getQuery supports.
     * WARNING : returns 0 if buildQueries haven't been called first !
     */
    public int getSize() {
        return queries.size();
    }

    /**
     * Populates an ArrayList of QueryExp.
     * Lowest index is 1.
     * Highest index is returned by getSize().
     * <br>The queries numbered 1 to 23 allow to cover all the underlying
     * Java classes of the JMX API used to build queries.
     */
    public void buildQueries() {
        if ( queries.size() == 0 ) {
            int smallerIntValue = intValue - 1;
            int biggerIntValue = intValue + 1;

            queries.add(Query.isInstanceOf(Query.value(mbeanClassName)));

            queries.add(Query.match(Query.classattr(),
                    Query.value(mbeanClassName)));

            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)));

            queries.add(Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue)));

            queries.add(Query.eq(Query.attr(primitiveDoubleAttName),
                    Query.value(doubleValue)));

            queries.add(Query.eq(Query.attr(primitiveFloatAttName),
                    Query.value(floatValue)));

            queries.add(Query.eq(Query.attr(mbeanClassName, primitiveIntAttName),
                    Query.value(intValue)));

            queries.add(Query.eq(Query.attr(stringAttName),
                    Query.value(stringValue)));

            queries.add(Query.eq(Query.attr(integerAttName),
                    Query.value(integerValue)));

            queries.add(Query.eq(Query.attr(primitiveBooleanAttName),
                    Query.value(booleanValue)));

            queries.add(Query.not(Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue))));

            queries.add(Query.or(
                    Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)),
                    Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue))));

            queries.add(Query.and(
                    Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)),
                    Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue))));

            ValueExp[] inArray = {Query.value(intValue)};
            queries.add(Query.in(Query.attr(primitiveIntAttName), inArray));

            queries.add(Query.between(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue),
                    Query.value(biggerIntValue)));

            queries.add(Query.gt(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue)));

            queries.add(Query.geq(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue)));

            queries.add(Query.lt(Query.attr(primitiveIntAttName),
                    Query.value(biggerIntValue)));

            queries.add(Query.leq(Query.attr(primitiveIntAttName),
                    Query.value(biggerIntValue)));

            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.minus(Query.value(intValue), Query.value(0))));

            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.plus(Query.value(intValue), Query.value(0))));

            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.div(Query.value(intValue), Query.value(1))));

            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.times(Query.value(intValue), Query.value(1))));

            QueryExp q2_3 = Query.and(queries.get(2-1), queries.get(3-1));
            QueryExp q4_5 = Query.and(queries.get(4-1), queries.get(5-1));
            QueryExp q6_7 = Query.and(queries.get(6-1), queries.get(7-1));
            QueryExp q8_9 = Query.and(queries.get(8-1), queries.get(9-1));
            QueryExp q10_11 = Query.and(queries.get(10-1), queries.get(11-1));
            QueryExp q12_13 = Query.and(queries.get(12-1), queries.get(13-1));
            QueryExp q14_15 = Query.and(queries.get(14-1), queries.get(15-1));
            QueryExp q16_17 = Query.and(queries.get(16-1), queries.get(17-1));
            QueryExp q18_19 = Query.and(queries.get(18-1), queries.get(19-1));
            QueryExp q20_21 = Query.and(queries.get(20-1), queries.get(21-1));
            QueryExp q22_23 = Query.and(queries.get(22-1), queries.get(23-1));
            QueryExp q2_5 = Query.and(q2_3, q4_5);
            QueryExp q6_9 = Query.and(q6_7, q8_9);
            QueryExp q10_13 = Query.and(q10_11, q12_13);
            QueryExp q14_17 = Query.and(q14_15, q16_17);
            QueryExp q18_21 = Query.and(q18_19, q20_21);
            QueryExp q2_9 = Query.and(q2_5, q6_9);
            QueryExp q10_17 = Query.and(q10_13, q14_17);
            QueryExp q18_23 = Query.and(q18_21, q22_23);
            QueryExp q2_17 = Query.and(q2_9, q10_17);
            queries.add(Query.and(q2_17, q18_23));

            queries.add(Query.or(q6_9, q18_23));
        }
    }

    /**
     * Returns a QueryExp taken is the ArrayList populated by buildQueries().
     * Lowest index is 1.
     * Highest index is returned by getSize().
     * <br>The queries numbered 1 to 23 allow to cover all the underlying
     * Java classes of the JMX API used to build queries.
     */
    public QueryExp getQuery(int index) {
        return queries.get(index - 1);
    }
}
