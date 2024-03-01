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
import org.elasticsearch.xpack.ql.type.DataType;
import org.elasticsearch.xpack.ql.type.DataTypes;
import org.elasticsearch.xpack.versionfield.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ToVersionTests extends AbstractFunctionTestCase {
    public ToVersionTests(@Name("TestCase") Supplier<TestCaseSupplier.TestCase> testCaseSupplier) {
        this.testCase = testCaseSupplier.get();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
        String read = "Attribute[channel=0]";
        String stringEvaluator = "ToVersionFromStringEvaluator[field=" + read + "]";
        List<TestCaseSupplier> suppliers = new ArrayList<>();

        TestCaseSupplier.forUnaryVersion(suppliers, read, DataTypes.VERSION, Version::toBytesRef, List.of());

        TestCaseSupplier.forUnaryStrings(
            suppliers,
            stringEvaluator,
            DataTypes.VERSION,
            bytesRef -> new Version(bytesRef.utf8ToString()).toBytesRef(),
            List.of()
        );

        for (DataType inputType : AbstractConvertFunction.STRING_TYPES) {
            TestCaseSupplier.unary(
                suppliers,
                read,
                TestCaseSupplier.versionCases(inputType.typeName() + " "),
                DataTypes.VERSION,
                bytesRef -> new Version((BytesRef) bytesRef).toBytesRef(),
                List.of()
            );
        }

        return parameterSuppliersFromTypedData(errorsForCasesWithoutExamples(anyNullIsNull(true, suppliers)));
    }

    @Override
    protected Expression build(Source source, List<Expression> args) {
        return new ToVersion(source, args.get(0));
    }
}
