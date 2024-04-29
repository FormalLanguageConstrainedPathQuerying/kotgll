/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.downsample;

import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.NumberFieldMapper;
import org.elasticsearch.xpack.aggregatemetric.mapper.AggregateDoubleMetricFieldMapper;
import org.elasticsearch.xpack.aggregatemetric.mapper.AggregateDoubleMetricFieldMapper.AggregateDoubleMetricFieldType;

public final class AggregateMetricFieldValueFetcher extends FieldValueFetcher {

    private final AggregateDoubleMetricFieldType aggMetricFieldType;

    private final AbstractDownsampleFieldProducer fieldProducer;

    AggregateMetricFieldValueFetcher(
        MappedFieldType fieldType,
        AggregateDoubleMetricFieldType aggMetricFieldType,
        IndexFieldData<?> fieldData
    ) {
        super(fieldType.name(), fieldType, fieldData);
        this.aggMetricFieldType = aggMetricFieldType;
        this.fieldProducer = createFieldProducer();
    }

    public AbstractDownsampleFieldProducer fieldProducer() {
        return fieldProducer;
    }

    private AbstractDownsampleFieldProducer createFieldProducer() {
        AggregateDoubleMetricFieldMapper.Metric metric = null;
        for (var e : aggMetricFieldType.getMetricFields().entrySet()) {
            NumberFieldMapper.NumberFieldType metricSubField = e.getValue();
            if (metricSubField.name().equals(name())) {
                metric = e.getKey();
                break;
            }
        }
        assert metric != null : "Cannot resolve metric type for field " + name();

        if (aggMetricFieldType.getMetricType() != null) {
            MetricFieldProducer.Metric metricOperation = switch (metric) {
                case max -> new MetricFieldProducer.Max();
                case min -> new MetricFieldProducer.Min();
                case sum -> new MetricFieldProducer.Sum();
                case value_count -> new MetricFieldProducer.Sum(AggregateDoubleMetricFieldMapper.Metric.value_count.name());
            };
            return new MetricFieldProducer.GaugeMetricFieldProducer(aggMetricFieldType.name(), metricOperation);
        } else {
            return new LabelFieldProducer.AggregateMetricFieldProducer.AggregateMetricFieldProducer(aggMetricFieldType.name(), metric);
        }
    }
}
