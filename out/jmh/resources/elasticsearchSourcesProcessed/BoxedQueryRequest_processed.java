/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.execution.assembler;

import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.eql.execution.search.Ordinal;
import org.elasticsearch.xpack.eql.execution.search.QueryRequest;
import org.elasticsearch.xpack.eql.execution.search.RuntimeUtils;
import org.elasticsearch.xpack.ql.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

/**
 * Ranged or boxed query. Provides a beginning or end to the current query.
 * The query moves between them through search_after.
 *
 * Note that the range is not set at once on purpose since each query tends to have
 * its own number of results separate from the others.
 * As such, each query starts from where it left off to reach the current in-progress window
 * as oppose to always operating with the exact same window.
 */
public class BoxedQueryRequest implements QueryRequest {

    public static final int MAX_TERMS = 128;

    private final RangeQueryBuilder timestampRange;
    private final String timestampField;
    private final SearchSourceBuilder searchSource;

    private final List<String> keys;
    private List<QueryBuilder> keyFilters;
    private final Set<String> optionalKeyNames;

    private Ordinal from, to;
    private Ordinal after;

    public BoxedQueryRequest(QueryRequest original, String timestamp, List<String> keyNames, Set<String> optionalKeyNames) {
        searchSource = original.searchSource();
        timestampRange = timestampRangeQuery(timestamp);
        timestampField = timestamp;
        keys = keyNames;
        this.optionalKeyNames = optionalKeyNames;
        RuntimeUtils.combineFilters(searchSource, timestampRange);
    }

    @Override
    public SearchSourceBuilder searchSource() {
        return searchSource;
    }

    @Override
    public void nextAfter(Ordinal ordinal) {
        after = ordinal;
        searchSource.searchAfter(ordinal.toArray());
    }

    public String timestampField() {
        return timestampField;
    }

    public RangeQueryBuilder timestampRangeQuery() {
        return timestampRangeQuery(timestampField);
    }

    private static RangeQueryBuilder timestampRangeQuery(String timestamp) {
        return rangeQuery(timestamp).timeZone("UTC").format("epoch_millis");
    }

    /**
     * Sets the lower boundary for the query (inclusive).
     * Can be removed (when the query in unbounded) through null.
     */
    public BoxedQueryRequest from(Ordinal begin) {
        from = begin;
        timestampRange.gte(begin != null ? begin.timestamp().toString() : null);
        return this;
    }

    /**
     * Sets the upper boundary for the query (inclusive).
     * Can be removed through null.
     */
    public BoxedQueryRequest to(Ordinal end) {
        to = end;
        timestampRange.lte(end != null ? end.timestamp().toString() : null);
        return this;
    }

    /**
     * Sets keys / terms to filter on.
     * Accepts the unwrapped SequenceKey as a list of values matching an instance of a given
     * event.
     * Can be removed through null.
     */
    public BoxedQueryRequest keys(List<List<Object>> values) {
        List<QueryBuilder> newFilters;

        if (CollectionUtils.isEmpty(values)) {
            if (CollectionUtils.isEmpty(keyFilters)) {
                return this;
            }
            newFilters = emptyList();
        } else {
            newFilters = new ArrayList<>(values.size());
            for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
                String key = keys.get(keyIndex);
                if (key == null) {
                    continue;
                }

                boolean hasNullValue = false;
                Set<Object> keyValues = Sets.newHashSetWithExpectedSize(BoxedQueryRequest.MAX_TERMS);
                for (List<Object> value : values) {
                    Object keyValue = value.get(keyIndex);
                    if (keyValue == null) {
                        hasNullValue = true;
                    } else {
                        keyValues.add(keyValue);
                    }
                }

                if (keyValues.size() > BoxedQueryRequest.MAX_TERMS) {
                    newFilters = emptyList();
                    break;
                }

                QueryBuilder query = null;

                if (keyValues.size() == 1) {
                    query = termQuery(key, keyValues.iterator().next());
                } else if (keyValues.size() > 1) {
                    query = termsQuery(key, keyValues);
                }

                if (hasNullValue && optionalKeyNames.contains(key)) {
                    BoolQueryBuilder isMissing = boolQuery().mustNot(existsQuery(key));
                    if (query != null) {
                        query = boolQuery()
                            .should(query)
                            .should(isMissing);
                    } else {
                        query = isMissing;
                    }
                }
                if (query != null) {
                    newFilters.add(query);
                }
            }
        }

        RuntimeUtils.replaceFilter(searchSource, keyFilters, newFilters);
        keyFilters = newFilters;
        return this;
    }

    public Ordinal after() {
        return after;
    }

    public Ordinal from() {
        return from;
    }

    public Ordinal to() {
        return to;
    }

    @Override
    public String toString() {
        return "( " + string(from) + " >-" + string(after) + "-> " + string(to) + "]";
    }

    private static String string(Ordinal o) {
        return o != null ? o.toString() : "<none>";
    }
}
