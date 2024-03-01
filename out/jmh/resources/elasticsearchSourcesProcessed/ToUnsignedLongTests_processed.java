/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.convert;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.xpack.esql.expression.function.AbstractFunctionTestCase;
import org.elasticsearch.xpack.esql.expression.function.TestCaseSupplier;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.tree.Source;
import org.elasticsearch.xpack.ql.type.DataTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.elasticsearch.xpack.ql.type.DataTypeConverter.safeToUnsignedLong;
import static org.elasticsearch.xpack.ql.util.NumericUtils.UNSIGNED_LONG_MAX_AS_DOUBLE;

public class ToUnsignedLongTests extends AbstractFunctionTestCase {
    public ToUnsignedLongTests(@Name("TestCase") Supplier<TestCaseSupplier.TestCase> testCaseSupplier) {
        this.testCase = testCaseSupplier.get();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
        String read = "Attribute[channel=0]";
        Function<String, String> evaluatorName = s -> "ToUnsignedLongFrom" + s + "Evaluator[field=" + read + "]";
        List<TestCaseSupplier> suppliers = new ArrayList<>();

        TestCaseSupplier.forUnaryUnsignedLong(
            suppliers,
            read,
            DataTypes.UNSIGNED_LONG,
            n -> n,
            BigInteger.ZERO,
            UNSIGNED_LONG_MAX,
            List.of()
        );

        TestCaseSupplier.forUnaryBoolean(
            suppliers,
            evaluatorName.apply("Boolean"),
            DataTypes.UNSIGNED_LONG,
            b -> b ? BigInteger.ONE : BigInteger.ZERO,
            List.of()
        );

        TestCaseSupplier.forUnaryDatetime(
            suppliers,
            evaluatorName.apply("Long"),
            DataTypes.UNSIGNED_LONG,
            instant -> BigInteger.valueOf(instant.toEpochMilli()),
            List.of()
        );
        TestCaseSupplier.forUnaryStrings(suppliers, evaluatorName.apply("String"), DataTypes.UNSIGNED_LONG, bytesRef -> null, bytesRef -> {
            Exception e = expectThrows(NumberFormatException.class, () -> new BigDecimal(bytesRef.utf8ToString()));
            return List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: java.lang.NumberFormatException: " + e.getMessage()
            );
        });
        TestCaseSupplier.forUnaryDouble(
            suppliers,
            evaluatorName.apply("Double"),
            DataTypes.UNSIGNED_LONG,
            d -> BigDecimal.valueOf(d).toBigInteger(), 
            0d,
            UNSIGNED_LONG_MAX_AS_DOUBLE,
            List.of()
        );
        TestCaseSupplier.forUnaryDouble(
            suppliers,
            evaluatorName.apply("Double"),
            DataTypes.UNSIGNED_LONG,
            d -> null,
            Double.NEGATIVE_INFINITY,
            -1d,
            d -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: [" + d + "] out of [unsigned_long] range"
            )
        );
        TestCaseSupplier.forUnaryDouble(
            suppliers,
            evaluatorName.apply("Double"),
            DataTypes.UNSIGNED_LONG,
            d -> null,
            UNSIGNED_LONG_MAX_AS_DOUBLE + 10e5,
            Double.POSITIVE_INFINITY,
            d -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: [" + d + "] out of [unsigned_long] range"
            )
        );

        TestCaseSupplier.forUnaryLong(
            suppliers,
            evaluatorName.apply("Long"),
            DataTypes.UNSIGNED_LONG,
            BigInteger::valueOf,
            0L,
            Long.MAX_VALUE,
            List.of()
        );
        TestCaseSupplier.forUnaryLong(
            suppliers,
            evaluatorName.apply("Long"),
            DataTypes.UNSIGNED_LONG,
            unused -> null,
            Long.MIN_VALUE,
            -1L,
            l -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: [" + l + "] out of [unsigned_long] range"
            )
        );

        TestCaseSupplier.forUnaryInt(
            suppliers,
            evaluatorName.apply("Int"),
            DataTypes.UNSIGNED_LONG,
            BigInteger::valueOf,
            0,
            Integer.MAX_VALUE,
            List.of()
        );
        TestCaseSupplier.forUnaryInt(
            suppliers,
            evaluatorName.apply("Int"),
            DataTypes.UNSIGNED_LONG,
            unused -> null,
            Integer.MIN_VALUE,
            -1,
            l -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: [" + l + "] out of [unsigned_long] range"
            )
        );

        TestCaseSupplier.unary(
            suppliers,
            evaluatorName.apply("String"),
            TestCaseSupplier.ulongCases(BigInteger.ZERO, UNSIGNED_LONG_MAX)
                .stream()
                .map(
                    tds -> new TestCaseSupplier.TypedDataSupplier(
                        tds.name() + "as string",
                        () -> new BytesRef(tds.supplier().get().toString()),
                        DataTypes.KEYWORD
                    )
                )
                .toList(),
            DataTypes.UNSIGNED_LONG,
            bytesRef -> safeToUnsignedLong(((BytesRef) bytesRef).utf8ToString()),
            List.of()
        );
        TestCaseSupplier.unary(
            suppliers,
            evaluatorName.apply("String"),
            TestCaseSupplier.doubleCases(0, UNSIGNED_LONG_MAX_AS_DOUBLE)
                .stream()
                .map(
                    tds -> new TestCaseSupplier.TypedDataSupplier(
                        tds.name() + "as string",
                        () -> new BytesRef(tds.supplier().get().toString()),
                        DataTypes.KEYWORD
                    )
                )
                .toList(),
            DataTypes.UNSIGNED_LONG,
            bytesRef -> safeToUnsignedLong(((BytesRef) bytesRef).utf8ToString()),
            List.of()
        );
        TestCaseSupplier.unary(
            suppliers,
            evaluatorName.apply("String"),
            TestCaseSupplier.doubleCases(Double.NEGATIVE_INFINITY, -1d)
                .stream()
                .map(
                    tds -> new TestCaseSupplier.TypedDataSupplier(
                        tds.name() + "as string",
                        () -> new BytesRef(tds.supplier().get().toString()),
                        DataTypes.KEYWORD
                    )
                )
                .toList(),
            DataTypes.UNSIGNED_LONG,
            bytesRef -> null,
            bytesRef -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: ["
                    + ((BytesRef) bytesRef).utf8ToString()
                    + "] out of [unsigned_long] range"
            )
        );
        TestCaseSupplier.unary(
            suppliers,
            evaluatorName.apply("String"),
            TestCaseSupplier.doubleCases(UNSIGNED_LONG_MAX_AS_DOUBLE + 10e5, Double.POSITIVE_INFINITY)
                .stream()
                .map(
                    tds -> new TestCaseSupplier.TypedDataSupplier(
                        tds.name() + "as string",
                        () -> new BytesRef(tds.supplier().get().toString()),
                        DataTypes.KEYWORD
                    )
                )
                .toList(),
            DataTypes.UNSIGNED_LONG,
            bytesRef -> null,
            bytesRef -> List.of(
                "Line -1:-1: evaluation of [] failed, treating result as null. Only first 20 failures recorded.",
                "Line -1:-1: org.elasticsearch.xpack.ql.InvalidArgumentException: ["
                    + ((BytesRef) bytesRef).utf8ToString()
                    + "] out of [unsigned_long] range"
            )
        );

        return parameterSuppliersFromTypedData(errorsForCasesWithoutExamples(anyNullIsNull(true, suppliers)));
    }

    @Override
    protected Expression build(Source source, List<Expression> args) {
        return new ToUnsignedLong(source, args.get(0));
    }
}
