/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.sql.execution.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregation;
import org.elasticsearch.xpack.ql.execution.search.extractor.BucketExtractor;
import org.elasticsearch.xpack.ql.type.Schema;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static java.util.Collections.emptyList;

class PivotRowSet extends SchemaCompositeAggRowSet {

    private final List<Object[]> data;
    private final Map<String, Object> lastAfterKey;

    PivotRowSet(
        Schema schema,
        List<BucketExtractor> exts,
        BitSet mask,
        SearchResponse response,
        int sizeRequested,
        int limit,
        Map<String, Object> previousLastKey,
        boolean mightProducePartialPages
    ) {
        super(schema, exts, mask, response, sizeRequested, limit, mightProducePartialPages);

        data = buckets.isEmpty() ? emptyList() : new ArrayList<>();

        if (buckets.isEmpty()) {
            lastAfterKey = null;
            return;
        }


        Map<String, Object> currentRowGroupKey = null;
        Map<String, Object> lastCompletedGroupKey = null;
        Object[] currentRow = new Object[columnCount()];

        for (int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++) {
            CompositeAggregation.Bucket bucket = buckets.get(bucketIndex);
            Map<String, Object> key = bucket.getKey();

            if (currentRowGroupKey == null || sameCompositeKey(currentRowGroupKey, key)) {
                currentRowGroupKey = key;
            }
            else {
                lastCompletedGroupKey = currentRowGroupKey;
                currentRowGroupKey = key;
                data.add(currentRow);

                if (limit > 0 && data.size() == limit) {
                    break;
                }
                currentRow = new Object[columnCount()];
            }

            for (int columnIndex = 0; columnIndex < currentRow.length; columnIndex++) {
                BucketExtractor extractor = userExtractor(columnIndex);
                Object value = extractor.extract(bucket);

                if (currentRow[columnIndex] == null && value != null) {
                    currentRow[columnIndex] = value;
                }
            }
        }

        if (limit > 0 && data.size() == limit) {
            afterKey = null;
        }
        else if ((previousLastKey != null && sameCompositeKey(previousLastKey, currentRowGroupKey))) {
            data.add(currentRow);
            afterKey = null;
        }
        else if (hasNull(currentRow) == false || data.isEmpty()) {
            data.add(currentRow);
            afterKey = currentRowGroupKey;
        }
        else {
            afterKey = lastCompletedGroupKey;
        }

        size = data.size();
        remainingData = remainingData(afterKey != null, size, limit);
        lastAfterKey = currentRowGroupKey;
    }

    private static boolean hasNull(Object[] currentRow) {
        for (Object object : currentRow) {
            if (object == null) {
                return true;
            }
        }
        return false;
    }

    static boolean sameCompositeKey(Map<String, Object> previous, Map<String, Object> current) {
        int keys = current.size() - 1;
        int keyIndex = 0;
        for (Entry<String, Object> entry : current.entrySet()) {
            if (keyIndex++ >= keys) {
                return true;
            }
            if (Objects.equals(entry.getValue(), previous.get(entry.getKey())) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Object getColumn(int column) {
        return data.get(row)[column];
    }

    Map<String, Object> lastAfterKey() {
        return lastAfterKey;
    }
}
