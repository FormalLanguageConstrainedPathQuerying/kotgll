/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.datafeed.extractor.scroll;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.fieldcaps.FieldCapabilities;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse;
import org.elasticsearch.action.fieldcaps.TransportFieldCapabilitiesAction;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xpack.core.ClientHelper;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.core.ml.utils.MlStrings;
import org.elasticsearch.xpack.ml.datafeed.DatafeedTimingStatsReporter;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractorFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ScrollDataExtractorFactory implements DataExtractorFactory {

    private static final String AGGREGATE_METRIC_DOUBLE = "aggregate_metric_double";

    private final Client client;
    private final DatafeedConfig datafeedConfig;
    private final QueryBuilder extraFilters;
    private final Job job;
    private final TimeBasedExtractedFields extractedFields;
    private final NamedXContentRegistry xContentRegistry;
    private final DatafeedTimingStatsReporter timingStatsReporter;

    private ScrollDataExtractorFactory(
        Client client,
        DatafeedConfig datafeedConfig,
        QueryBuilder extraFilters,
        Job job,
        TimeBasedExtractedFields extractedFields,
        NamedXContentRegistry xContentRegistry,
        DatafeedTimingStatsReporter timingStatsReporter
    ) {
        this.client = Objects.requireNonNull(client);
        this.datafeedConfig = Objects.requireNonNull(datafeedConfig);
        this.extraFilters = extraFilters;
        this.job = Objects.requireNonNull(job);
        this.extractedFields = Objects.requireNonNull(extractedFields);
        this.xContentRegistry = xContentRegistry;
        this.timingStatsReporter = Objects.requireNonNull(timingStatsReporter);
    }

    @Override
    public DataExtractor newExtractor(long start, long end) {
        QueryBuilder queryBuilder = datafeedConfig.getParsedQuery(xContentRegistry);
        if (extraFilters != null) {
            queryBuilder = QueryBuilders.boolQuery().filter(queryBuilder).filter(extraFilters);
        }
        ScrollDataExtractorContext dataExtractorContext = new ScrollDataExtractorContext(
            job.getId(),
            extractedFields,
            datafeedConfig.getIndices(),
            queryBuilder,
            datafeedConfig.getScriptFields(),
            datafeedConfig.getScrollSize(),
            start,
            end,
            datafeedConfig.getHeaders(),
            datafeedConfig.getIndicesOptions(),
            datafeedConfig.getRuntimeMappings()
        );
        return new ScrollDataExtractor(client, dataExtractorContext, timingStatsReporter);
    }

    public static void create(
        Client client,
        DatafeedConfig datafeed,
        QueryBuilder extraFilters,
        Job job,
        NamedXContentRegistry xContentRegistry,
        DatafeedTimingStatsReporter timingStatsReporter,
        ActionListener<DataExtractorFactory> listener
    ) {

        ActionListener<FieldCapabilitiesResponse> fieldCapabilitiesHandler = ActionListener.wrap(fieldCapabilitiesResponse -> {
            if (fieldCapabilitiesResponse.getIndices().length == 0) {
                listener.onFailure(
                    ExceptionsHelper.badRequestException(
                        "datafeed [{}] cannot retrieve data because no index matches datafeed's indices {}",
                        datafeed.getId(),
                        datafeed.getIndices()
                    )
                );
                return;
            }
            Optional<String> optionalAggregatedMetricDouble = findFirstAggregatedMetricDoubleField(fieldCapabilitiesResponse);
            if (optionalAggregatedMetricDouble.isPresent()) {
                listener.onFailure(
                    ExceptionsHelper.badRequestException(
                        "field [{}] is of type [{}] and cannot be used in a datafeed without aggregations",
                        optionalAggregatedMetricDouble.get(),
                        AGGREGATE_METRIC_DOUBLE
                    )
                );
                return;
            }
            TimeBasedExtractedFields fields = TimeBasedExtractedFields.build(job, datafeed, fieldCapabilitiesResponse);
            listener.onResponse(
                new ScrollDataExtractorFactory(client, datafeed, extraFilters, job, fields, xContentRegistry, timingStatsReporter)
            );
        }, e -> {
            Throwable cause = ExceptionsHelper.unwrapCause(e);
            if (cause instanceof IndexNotFoundException notFound) {
                listener.onFailure(
                    new ResourceNotFoundException(
                        "datafeed [" + datafeed.getId() + "] cannot retrieve data because index " + notFound.getIndex() + " does not exist"
                    )
                );
            } else if (e instanceof IllegalArgumentException) {
                listener.onFailure(ExceptionsHelper.badRequestException("[" + datafeed.getId() + "] " + e.getMessage()));
            } else {
                listener.onFailure(e);
            }
        });

        FieldCapabilitiesRequest fieldCapabilitiesRequest = new FieldCapabilitiesRequest();
        fieldCapabilitiesRequest.indices(datafeed.getIndices().toArray(new String[0])).indicesOptions(datafeed.getIndicesOptions());

        Set<String> runtimefields = datafeed.getRuntimeMappings().keySet();

        String[] requestFields = job.allInputFields()
            .stream()
            .map(f -> MlStrings.getParentField(f) + "*")
            .filter(f -> runtimefields.contains(f) == false)
            .toArray(String[]::new);
        fieldCapabilitiesRequest.fields(requestFields);
        ClientHelper.<FieldCapabilitiesResponse>executeWithHeaders(datafeed.getHeaders(), ClientHelper.ML_ORIGIN, client, () -> {
            client.execute(TransportFieldCapabilitiesAction.TYPE, fieldCapabilitiesRequest, fieldCapabilitiesHandler);
            return null;
        });
    }

    private static Optional<String> findFirstAggregatedMetricDoubleField(FieldCapabilitiesResponse fieldCapabilitiesResponse) {
        Map<String, Map<String, FieldCapabilities>> indexTofieldCapsMap = fieldCapabilitiesResponse.get();
        for (Map.Entry<String, Map<String, FieldCapabilities>> indexToFieldCaps : indexTofieldCapsMap.entrySet()) {
            for (Map.Entry<String, FieldCapabilities> typeToFieldCaps : indexToFieldCaps.getValue().entrySet()) {
                if (AGGREGATE_METRIC_DOUBLE.equals(typeToFieldCaps.getKey())) {
                    return Optional.of(typeToFieldCaps.getValue().getName());
                }
            }
        }
        return Optional.empty();
    }
}
