/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.transforms.latest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.transform.transforms.Function;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * {@link Function.ChangeCollector} implementation for the latest function.
 */
class LatestChangeCollector implements Function.ChangeCollector {

    private final String synchronizationField;

    LatestChangeCollector(String synchronizationField) {
        this.synchronizationField = Objects.requireNonNull(synchronizationField);
    }

    @Override
    public SearchSourceBuilder buildChangesQuery(SearchSourceBuilder searchSourceBuilder, Map<String, Object> position, int pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> processSearchResponse(SearchResponse searchResponse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryBuilder buildFilterQuery(TransformCheckpoint lastCheckpoint, TransformCheckpoint nextCheckpoint) {
        return QueryBuilders.rangeQuery(synchronizationField)
            .gte(lastCheckpoint.getTimeUpperBound())
            .lt(nextCheckpoint.getTimeUpperBound())
            .format("epoch_millis");
    }

    @Override
    public Collection<String> getIndicesToQuery(TransformCheckpoint lastCheckpoint, TransformCheckpoint nextCheckpoint) {
        return TransformCheckpoint.getChangedIndices(TransformCheckpoint.EMPTY, nextCheckpoint);
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean isOptimized() {
        return true;
    }

    @Override
    public boolean queryForChanges() {
        return false;
    }
}
