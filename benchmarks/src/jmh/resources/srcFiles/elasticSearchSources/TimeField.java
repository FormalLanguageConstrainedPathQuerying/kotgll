/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.extractor;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.xpack.core.common.time.TimeUtils;

import java.util.Objects;
import java.util.Set;

public class TimeField extends AbstractField {

    static final Set<String> TYPES = Set.of("date", "date_nanos");

    private static final String EPOCH_MILLIS_FORMAT = "epoch_millis";

    private final Method method;

    public TimeField(String name, Method method) {
        super(name, TYPES);
        if (method == Method.SOURCE) {
            throw new IllegalArgumentException("time field [" + name + "] cannot be extracted from source");
        }
        this.method = Objects.requireNonNull(method);
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] value(SearchHit hit) {
        Object[] value = getFieldValue(hit);
        if (value.length != 1) {
            return value;
        }
        if (value[0] instanceof String stringValue) { 
            value[0] = TimeUtils.parseToEpochMs(stringValue);
        } else if (value[0] instanceof Long == false) { 
            throw new IllegalStateException("Unexpected value for a time field: " + value[0].getClass());
        }
        return value;
    }

    @Override
    public String getDocValueFormat() {
        if (method != Method.DOC_VALUE) {
            throw new UnsupportedOperationException();
        }
        return EPOCH_MILLIS_FORMAT;
    }

    @Override
    public boolean supportsFromSource() {
        return false;
    }

    @Override
    public ExtractedField newFromSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultiField() {
        return false;
    }
}
