/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.MockUtils;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.xpack.transform.TransformInfoTransportAction.PROVIDED_STATS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformInfoTransportActionTests extends ESTestCase {

    public void testAvailable() {
        TransportService transportService = MockUtils.setupTransportServiceWithThreadpoolExecutor();
        TransformInfoTransportAction featureSet = new TransformInfoTransportAction(transportService, mock(ActionFilters.class));
        assertThat(featureSet.available(), is(true));
    }

    public void testEnabledDefault() {
        TransportService transportService = MockUtils.setupTransportServiceWithThreadpoolExecutor();
        TransformInfoTransportAction featureSet = new TransformInfoTransportAction(transportService, mock(ActionFilters.class));
        assertTrue(featureSet.enabled());
    }

    public void testParseSearchAggs() {
        InternalAggregations emptyAggs = InternalAggregations.from(Collections.emptyList());
        SearchResponse withEmptyAggs = mock(SearchResponse.class);
        when(withEmptyAggs.getAggregations()).thenReturn(emptyAggs);

        assertThat(TransformInfoTransportAction.parseSearchAggs(withEmptyAggs), equalTo(new TransformIndexerStats()));

        TransformIndexerStats expectedStats = new TransformIndexerStats(
            1,  
            2,  
            3,  
            4,  
            5,  
            6,  
            7,  
            8,  
            9,  
            10,  
            11,  
            12, 
            13, 
            14, 
            15.0,  
            16.0,  
            17.0   
        );

        int currentStat = 1;
        List<InternalAggregation> aggs = new ArrayList<>(PROVIDED_STATS.length);
        for (String statName : PROVIDED_STATS) {
            aggs.add(buildAgg(statName, currentStat++));
        }
        InternalAggregations aggregations = InternalAggregations.from(aggs);
        SearchResponse withAggs = mock(SearchResponse.class);
        when(withAggs.getAggregations()).thenReturn(aggregations);

        assertThat(TransformInfoTransportAction.parseSearchAggs(withAggs), equalTo(expectedStats));
    }

    private static InternalAggregation buildAgg(String name, double value) {
        InternalNumericMetricsAggregation.SingleValue agg = mock(InternalNumericMetricsAggregation.SingleValue.class);
        when(agg.getName()).thenReturn(name);
        when(agg.value()).thenReturn(value);
        return agg;
    }
}
