/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.metrics;

/**
 * An aggregation that approximates the median absolute deviation of a numeric field
 *
 * @see <a href="https:
 */
public interface MedianAbsoluteDeviation extends NumericMetricsAggregation.SingleValue {

    /**
     * Returns the median absolute deviation statistic computed for this aggregation
     */
    double getMedianAbsoluteDeviation();
}
