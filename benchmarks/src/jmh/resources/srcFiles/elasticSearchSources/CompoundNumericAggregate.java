/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.expression.function.aggregate;

import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.expression.function.aggregate.CompoundAggregate;
import org.elasticsearch.xpack.ql.tree.Source;

import java.util.List;

public abstract class CompoundNumericAggregate extends NumericAggregate implements CompoundAggregate {

    CompoundNumericAggregate(Source source, Expression field, List<Expression> arguments) {
        super(source, field, arguments);
    }

    CompoundNumericAggregate(Source source, Expression field) {
        super(source, field);
    }
}
