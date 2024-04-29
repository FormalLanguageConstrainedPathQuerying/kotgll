/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.graph.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DelegatingActionListener;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.protocol.xpack.graph.Connection;
import org.elasticsearch.protocol.xpack.graph.Connection.ConnectionId;
import org.elasticsearch.protocol.xpack.graph.GraphExploreRequest;
import org.elasticsearch.protocol.xpack.graph.GraphExploreRequest.TermBoost;
import org.elasticsearch.protocol.xpack.graph.GraphExploreResponse;
import org.elasticsearch.protocol.xpack.graph.Hop;
import org.elasticsearch.protocol.xpack.graph.Vertex;
import org.elasticsearch.protocol.xpack.graph.Vertex.VertexId;
import org.elasticsearch.protocol.xpack.graph.VertexRequest;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.sampler.DiversifiedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.sampler.Sampler;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantTerms;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantTerms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.XPackField;
import org.elasticsearch.xpack.core.graph.action.GraphExploreAction;
import org.elasticsearch.xpack.graph.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Performs a series of elasticsearch queries and aggregations to explore
 * connected terms in a single index.
 */
public class TransportGraphExploreAction extends HandledTransportAction<GraphExploreRequest, GraphExploreResponse> {
    private static final Logger logger = LogManager.getLogger(TransportGraphExploreAction.class);

    private final ThreadPool threadPool;
    private final NodeClient client;
    protected final XPackLicenseState licenseState;

    static class VertexPriorityQueue extends PriorityQueue<Vertex> {

        VertexPriorityQueue(int maxSize) {
            super(maxSize);
        }

        @Override
        protected boolean lessThan(Vertex a, Vertex b) {
            return a.getWeight() < b.getWeight();
        }

    }

    @Inject
    public TransportGraphExploreAction(
        ThreadPool threadPool,
        NodeClient client,
        TransportService transportService,
        ActionFilters actionFilters,
        XPackLicenseState licenseState
    ) {
        super(GraphExploreAction.NAME, transportService, actionFilters, GraphExploreRequest::new, EsExecutors.DIRECT_EXECUTOR_SERVICE);
        this.threadPool = threadPool;
        this.client = client;
        this.licenseState = licenseState;
    }

    @Override
    protected void doExecute(Task task, GraphExploreRequest request, ActionListener<GraphExploreResponse> listener) {
        if (Graph.GRAPH_FEATURE.check(licenseState)) {
            new AsyncGraphAction(request, listener).start();
        } else {
            listener.onFailure(LicenseUtils.newComplianceException(XPackField.GRAPH));
        }
    }

    class AsyncGraphAction {

        private final GraphExploreRequest request;
        private final ActionListener<GraphExploreResponse> listener;

        private final long startTime;
        private volatile ShardOperationFailedException[] shardFailures;
        private Map<VertexId, Vertex> vertices = new HashMap<>();
        private Map<ConnectionId, Connection> connections = new HashMap<>();

        private Map<Integer, Map<String, Set<Vertex>>> hopFindings = new HashMap<>();
        private int currentHopNumber = 0;

        AsyncGraphAction(GraphExploreRequest request, ActionListener<GraphExploreResponse> listener) {
            this.request = request;
            this.listener = listener;
            this.startTime = threadPool.relativeTimeInMillis();
            this.shardFailures = ShardSearchFailure.EMPTY_ARRAY;
        }

        private Vertex getVertex(String field, String term) {
            return vertices.get(Vertex.createId(field, term));
        }

        private Connection addConnection(Vertex from, Vertex to, double weight, long docCount) {
            Connection connection = new Connection(from, to, weight, docCount);
            connections.put(connection.getId(), connection);
            return connection;
        }

        private Vertex addVertex(String field, String term, double score, int depth, long bg, long fg) {
            VertexId key = Vertex.createId(field, term);
            Vertex vertex = vertices.get(key);
            if (vertex == null) {
                vertex = new Vertex(field, term, score, depth, bg, fg);
                vertices.put(key, vertex);
                Map<String, Set<Vertex>> currentWave = hopFindings.get(currentHopNumber);
                if (currentWave == null) {
                    currentWave = new HashMap<>();
                    hopFindings.put(currentHopNumber, currentWave);
                }
                Set<Vertex> verticesForField = currentWave.get(field);
                if (verticesForField == null) {
                    verticesForField = new HashSet<>();
                    currentWave.put(field, verticesForField);
                }
                verticesForField.add(vertex);
            }
            return vertex;
        }

        private void removeVertex(Vertex vertex) {
            vertices.remove(vertex.getId());
            hopFindings.get(currentHopNumber).get(vertex.getField()).remove(vertex);
        }

        /**
         * Step out from some existing vertex terms looking for useful
         * connections
         *
         * @param timedOut the value of timedOut field in the search response
         */
        synchronized void expand(boolean timedOut) {
            Map<String, Set<Vertex>> lastHopFindings = hopFindings.get(currentHopNumber);
            if ((currentHopNumber >= (request.getHopNumbers() - 1)) || (lastHopFindings == null) || (lastHopFindings.size() == 0)) {
                listener.onResponse(buildResponse(timedOut));
                return;
            }
            Hop lastHop = request.getHop(currentHopNumber);
            currentHopNumber++;
            Hop currentHop = request.getHop(currentHopNumber);

            final SearchRequest searchRequest = new SearchRequest(request.indices()).indicesOptions(request.indicesOptions());
            if (request.routing() != null) {
                searchRequest.routing(request.routing());
            }

            BoolQueryBuilder rootBool = QueryBuilders.boolQuery();

            AggregationBuilder sampleAgg = null;
            if (request.sampleDiversityField() != null) {
                DiversifiedAggregationBuilder diversifiedSampleAgg = AggregationBuilders.diversifiedSampler("sample")
                    .shardSize(request.sampleSize());
                diversifiedSampleAgg.field(request.sampleDiversityField());
                diversifiedSampleAgg.maxDocsPerValue(request.maxDocsPerDiversityValue());
                sampleAgg = diversifiedSampleAgg;
            } else {
                sampleAgg = AggregationBuilders.sampler("sample").shardSize(request.sampleSize());
            }

            rootBool.must(currentHop.guidingQuery());

            BoolQueryBuilder sourceTermsOrClause = QueryBuilders.boolQuery();
            addUserDefinedIncludesToQuery(currentHop, sourceTermsOrClause);
            addBigOrClause(lastHopFindings, sourceTermsOrClause);

            rootBool.must(sourceTermsOrClause);

            for (int fieldNum = 0; fieldNum < lastHop.getNumberVertexRequests(); fieldNum++) {
                VertexRequest lastVr = lastHop.getVertexRequest(fieldNum);
                Set<Vertex> lastWaveVerticesForField = lastHopFindings.get(lastVr.fieldName());
                if (lastWaveVerticesForField == null) {
                    continue;
                }
                SortedSet<BytesRef> terms = new TreeSet<>();
                for (Vertex v : lastWaveVerticesForField) {
                    terms.add(new BytesRef(v.getTerm()));
                }
                TermsAggregationBuilder lastWaveTermsAgg = AggregationBuilders.terms("field" + fieldNum)
                    .includeExclude(new IncludeExclude(null, null, terms, null))
                    .shardMinDocCount(1)
                    .field(lastVr.fieldName())
                    .minDocCount(1)
                    .executionHint("map")
                    .size(terms.size());
                sampleAgg.subAggregation(lastWaveTermsAgg);
                for (int f = 0; f < currentHop.getNumberVertexRequests(); f++) {
                    VertexRequest vr = currentHop.getVertexRequest(f);
                    int size = vr.size();
                    if (vr.fieldName().equals(lastVr.fieldName())) {
                        size++;
                    }
                    if (request.useSignificance()) {
                        SignificantTermsAggregationBuilder nextWaveSigTerms = AggregationBuilders.significantTerms("field" + f)
                            .field(vr.fieldName())
                            .minDocCount(vr.minDocCount())
                            .shardMinDocCount(vr.shardMinDocCount())
                            .executionHint("map")
                            .size(size);
                        if (size < 10) {
                            nextWaveSigTerms.shardSize(10);
                        }

                        if (vr.hasIncludeClauses()) {
                            SortedSet<BytesRef> includes = vr.includeValuesAsSortedSet();
                            nextWaveSigTerms.includeExclude(new IncludeExclude(null, null, includes, null));


                        } else if (vr.hasExcludeClauses()) {
                            nextWaveSigTerms.includeExclude(new IncludeExclude(null, null, null, vr.excludesAsSortedSet()));
                        }
                        lastWaveTermsAgg.subAggregation(nextWaveSigTerms);
                    } else {
                        TermsAggregationBuilder nextWavePopularTerms = AggregationBuilders.terms("field" + f)
                            .field(vr.fieldName())
                            .minDocCount(vr.minDocCount())
                            .shardMinDocCount(vr.shardMinDocCount())
                            .executionHint("map")
                            .size(size);
                        if (vr.hasIncludeClauses()) {
                            SortedSet<BytesRef> includes = vr.includeValuesAsSortedSet();
                            nextWavePopularTerms.includeExclude(new IncludeExclude(null, null, includes, null));
                        } else if (vr.hasExcludeClauses()) {
                            nextWavePopularTerms.includeExclude(new IncludeExclude(null, null, null, vr.excludesAsSortedSet()));
                        }
                        lastWaveTermsAgg.subAggregation(nextWavePopularTerms);
                    }
                }
            }

            SearchSourceBuilder source = new SearchSourceBuilder().query(rootBool).aggregation(sampleAgg).size(0);
            if (request.timeout() != null) {
                long timeRemainingMillis = startTime + request.timeout().millis() - threadPool.relativeTimeInMillis();
                if (timeRemainingMillis <= 0) {
                    listener.onResponse(buildResponse(true));
                    return;
                }

                source.timeout(TimeValue.timeValueMillis(timeRemainingMillis));
            }
            searchRequest.source(source);

            logger.trace("executing expansion graph search request");
            client.search(searchRequest, new DelegatingActionListener<>(listener) {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    addShardFailures(searchResponse.getShardFailures());

                    ArrayList<Connection> newConnections = new ArrayList<Connection>();
                    ArrayList<Vertex> newVertices = new ArrayList<Vertex>();
                    Sampler sample = searchResponse.getAggregations().get("sample");

                    double totalSignalOutput = getExpandTotalSignalStrength(lastHop, currentHop, sample);

                    if (totalSignalOutput > 0) {
                        addAndScoreNewVertices(lastHop, currentHop, sample, totalSignalOutput, newConnections, newVertices);

                        trimNewAdditions(currentHop, newConnections, newVertices);
                    }

                    expand(searchResponse.isTimedOut());

                }

                private void addAndScoreNewVertices(
                    Hop lastHop,
                    Hop currentHop,
                    Sampler sample,
                    double totalSignalOutput,
                    ArrayList<Connection> newConnections,
                    ArrayList<Vertex> newVertices
                ) {
                    for (int j = 0; j < lastHop.getNumberVertexRequests(); j++) {
                        VertexRequest lastVr = lastHop.getVertexRequest(j);
                        Terms lastWaveTerms = sample.getAggregations().get("field" + j);
                        if (lastWaveTerms == null) {
                            continue;
                        }
                        List<? extends Terms.Bucket> buckets = lastWaveTerms.getBuckets();
                        for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket lastWaveTerm : buckets) {
                            Vertex fromVertex = getVertex(lastVr.fieldName(), lastWaveTerm.getKeyAsString());
                            for (int k = 0; k < currentHop.getNumberVertexRequests(); k++) {
                                VertexRequest vr = currentHop.getVertexRequest(k);
                                double decay = 0.95d;
                                if (request.useSignificance()) {
                                    SignificantTerms significantTerms = lastWaveTerm.getAggregations().get("field" + k);
                                    if (significantTerms != null) {
                                        for (Bucket bucket : significantTerms.getBuckets()) {
                                            if ((vr.fieldName().equals(fromVertex.getField()))
                                                && (bucket.getKeyAsString().equals(fromVertex.getTerm()))) {
                                                continue;
                                            }
                                            double signalStrength = bucket.getSignificanceScore() / totalSignalOutput;

                                            signalStrength = signalStrength * Math.min(decay, fromVertex.getWeight());

                                            Vertex toVertex = getVertex(vr.fieldName(), bucket.getKeyAsString());
                                            if (toVertex == null) {
                                                toVertex = addVertex(
                                                    vr.fieldName(),
                                                    bucket.getKeyAsString(),
                                                    signalStrength,
                                                    currentHopNumber,
                                                    bucket.getSupersetDf(),
                                                    bucket.getSubsetDf()
                                                );
                                                newVertices.add(toVertex);
                                            } else {
                                                toVertex.setWeight(toVertex.getWeight() + signalStrength);
                                                toVertex.setFg(Math.max(toVertex.getFg(), bucket.getSubsetDf()));
                                            }
                                            newConnections.add(addConnection(fromVertex, toVertex, signalStrength, bucket.getDocCount()));
                                        }
                                    }
                                } else {
                                    Terms terms = lastWaveTerm.getAggregations().get("field" + k);
                                    if (terms != null) {
                                        for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : terms.getBuckets()) {
                                            double signalStrength = bucket.getDocCount() / totalSignalOutput;
                                            signalStrength = signalStrength * Math.min(decay, fromVertex.getWeight());

                                            Vertex toVertex = getVertex(vr.fieldName(), bucket.getKeyAsString());
                                            if (toVertex == null) {
                                                toVertex = addVertex(
                                                    vr.fieldName(),
                                                    bucket.getKeyAsString(),
                                                    signalStrength,
                                                    currentHopNumber,
                                                    0,
                                                    0
                                                );
                                                newVertices.add(toVertex);
                                            } else {
                                                toVertex.setWeight(toVertex.getWeight() + signalStrength);
                                            }
                                            newConnections.add(addConnection(fromVertex, toVertex, signalStrength, bucket.getDocCount()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                private void trimNewAdditions(Hop currentHop, ArrayList<Connection> newConnections, ArrayList<Vertex> newVertices) {
                    Set<Vertex> evictions = new HashSet<>();

                    for (int k = 0; k < currentHop.getNumberVertexRequests(); k++) {
                        VertexRequest vr = currentHop.getVertexRequest(k);
                        if (newVertices.size() <= vr.size()) {
                            continue;
                        }
                        VertexPriorityQueue pq = new VertexPriorityQueue(vr.size());
                        for (Vertex vertex : newVertices) {
                            if (vertex.getField().equals(vr.fieldName())) {
                                Vertex eviction = pq.insertWithOverflow(vertex);
                                if (eviction != null) {
                                    evictions.add(eviction);
                                }
                            }
                        }
                    }
                    if (evictions.size() > 0) {
                        for (Connection connection : newConnections) {
                            if (evictions.contains(connection.getTo())) {
                                connections.remove(connection.getId());
                                removeVertex(connection.getTo());
                            }
                        }
                    }
                }

                private double getExpandTotalSignalStrength(Hop lastHop, Hop currentHop, Sampler sample) {
                    double totalSignalOutput = 0;
                    for (int j = 0; j < lastHop.getNumberVertexRequests(); j++) {
                        VertexRequest lastVr = lastHop.getVertexRequest(j);
                        Terms lastWaveTerms = sample.getAggregations().get("field" + j);
                        if (lastWaveTerms == null) {
                            continue;
                        }
                        List<? extends Terms.Bucket> buckets = lastWaveTerms.getBuckets();
                        for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket lastWaveTerm : buckets) {
                            for (int k = 0; k < currentHop.getNumberVertexRequests(); k++) {
                                VertexRequest vr = currentHop.getVertexRequest(k);
                                if (request.useSignificance()) {
                                    SignificantTerms significantTerms = lastWaveTerm.getAggregations().get("field" + k);
                                    if (significantTerms != null) {
                                        for (Bucket bucket : significantTerms.getBuckets()) {
                                            if ((vr.fieldName().equals(lastVr.fieldName()))
                                                && (bucket.getKeyAsString().equals(lastWaveTerm.getKeyAsString()))) {
                                                continue;
                                            } else {
                                                totalSignalOutput += bucket.getSignificanceScore();
                                            }
                                        }
                                    }
                                } else {
                                    Terms terms = lastWaveTerm.getAggregations().get("field" + k);
                                    if (terms != null) {
                                        for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : terms.getBuckets()) {
                                            if ((vr.fieldName().equals(lastVr.fieldName()))
                                                && (bucket.getKeyAsString().equals(lastWaveTerm.getKeyAsString()))) {
                                                continue;
                                            } else {
                                                totalSignalOutput += bucket.getDocCount();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return totalSignalOutput;
                }
            });
        }

        private static void addUserDefinedIncludesToQuery(Hop hop, BoolQueryBuilder sourceTermsOrClause) {
            for (int i = 0; i < hop.getNumberVertexRequests(); i++) {
                VertexRequest vr = hop.getVertexRequest(i);
                if (vr.hasIncludeClauses()) {
                    addNormalizedBoosts(sourceTermsOrClause, vr);
                }
            }
        }

        private static void addBigOrClause(Map<String, Set<Vertex>> lastHopFindings, BoolQueryBuilder sourceTermsOrClause) {
            int numClauses = sourceTermsOrClause.should().size();
            for (Entry<String, Set<Vertex>> entry : lastHopFindings.entrySet()) {
                numClauses += entry.getValue().size();
            }
            if (numClauses < BooleanQuery.getMaxClauseCount()) {
                for (Entry<String, Set<Vertex>> entry : lastHopFindings.entrySet()) {
                    for (Vertex vertex : entry.getValue()) {
                        sourceTermsOrClause.should(
                            QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(vertex.getField(), vertex.getTerm()))
                                .boost((float) vertex.getWeight())
                        );
                    }
                }

            } else {
                for (Entry<String, Set<Vertex>> entry : lastHopFindings.entrySet()) {
                    List<String> perFieldTerms = new ArrayList<>();
                    for (Vertex vertex : entry.getValue()) {
                        perFieldTerms.add(vertex.getTerm());
                    }
                    sourceTermsOrClause.should(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery(entry.getKey(), perFieldTerms)));
                }
            }
        }

        /**
         * For a given root query (or a set of "includes" root constraints) find
         * the related terms. These will be our start points in the graph
         * navigation.
         */
        public synchronized void start() {
            try {

                final SearchRequest searchRequest = new SearchRequest(request.indices()).indicesOptions(request.indicesOptions());
                if (request.routing() != null) {
                    searchRequest.routing(request.routing());
                }

                BoolQueryBuilder rootBool = QueryBuilders.boolQuery();

                AggregationBuilder rootSampleAgg = null;
                if (request.sampleDiversityField() != null) {
                    DiversifiedAggregationBuilder diversifiedRootSampleAgg = AggregationBuilders.diversifiedSampler("sample")
                        .shardSize(request.sampleSize());
                    diversifiedRootSampleAgg.field(request.sampleDiversityField());
                    diversifiedRootSampleAgg.maxDocsPerValue(request.maxDocsPerDiversityValue());
                    rootSampleAgg = diversifiedRootSampleAgg;
                } else {
                    rootSampleAgg = AggregationBuilders.sampler("sample").shardSize(request.sampleSize());
                }

                Hop rootHop = request.getHop(0);

                rootBool.must(rootHop.guidingQuery());

                BoolQueryBuilder includesContainer = QueryBuilders.boolQuery();
                addUserDefinedIncludesToQuery(rootHop, includesContainer);
                if (includesContainer.should().size() > 0) {
                    rootBool.must(includesContainer);
                }

                for (int i = 0; i < rootHop.getNumberVertexRequests(); i++) {
                    VertexRequest vr = rootHop.getVertexRequest(i);
                    if (request.useSignificance()) {
                        SignificantTermsAggregationBuilder sigBuilder = AggregationBuilders.significantTerms("field" + i);
                        sigBuilder.field(vr.fieldName())
                            .shardMinDocCount(vr.shardMinDocCount())
                            .minDocCount(vr.minDocCount())
                            .executionHint("map")
                            .size(vr.size());

                        if (vr.hasIncludeClauses()) {
                            SortedSet<BytesRef> includes = vr.includeValuesAsSortedSet();
                            sigBuilder.includeExclude(new IncludeExclude(null, null, includes, null));
                            sigBuilder.size(includes.size());
                        }
                        if (vr.hasExcludeClauses()) {
                            sigBuilder.includeExclude(new IncludeExclude(null, null, null, vr.excludesAsSortedSet()));
                        }
                        rootSampleAgg.subAggregation(sigBuilder);
                    } else {
                        TermsAggregationBuilder termsBuilder = AggregationBuilders.terms("field" + i);
                        termsBuilder.field(vr.fieldName()).executionHint("map").size(vr.size());
                        if (vr.hasIncludeClauses()) {
                            SortedSet<BytesRef> includes = vr.includeValuesAsSortedSet();
                            termsBuilder.includeExclude(new IncludeExclude(null, null, includes, null));
                            termsBuilder.size(includes.size());
                        }
                        if (vr.hasExcludeClauses()) {
                            termsBuilder.includeExclude(new IncludeExclude(null, null, null, vr.excludesAsSortedSet()));
                        }
                        rootSampleAgg.subAggregation(termsBuilder);
                    }
                }

                SearchSourceBuilder source = new SearchSourceBuilder().query(rootBool).aggregation(rootSampleAgg).size(0);
                if (request.timeout() != null) {
                    source.timeout(request.timeout());
                }
                searchRequest.source(source);
                logger.trace("executing initial graph search request");
                client.search(searchRequest, new DelegatingActionListener<>(listener) {
                    @Override
                    public void onResponse(SearchResponse searchResponse) {
                        addShardFailures(searchResponse.getShardFailures());
                        Sampler sample = searchResponse.getAggregations().get("sample");

                        double totalSignalStrength = getInitialTotalSignalStrength(rootHop, sample);

                        for (int j = 0; j < rootHop.getNumberVertexRequests(); j++) {
                            VertexRequest vr = rootHop.getVertexRequest(j);
                            if (request.useSignificance()) {
                                SignificantTerms significantTerms = sample.getAggregations().get("field" + j);
                                List<? extends Bucket> buckets = significantTerms.getBuckets();
                                for (Bucket bucket : buckets) {
                                    double signalWeight = bucket.getSignificanceScore() / totalSignalStrength;
                                    addVertex(
                                        vr.fieldName(),
                                        bucket.getKeyAsString(),
                                        signalWeight,
                                        currentHopNumber,
                                        bucket.getSupersetDf(),
                                        bucket.getSubsetDf()
                                    );
                                }
                            } else {
                                Terms terms = sample.getAggregations().get("field" + j);
                                List<? extends Terms.Bucket> buckets = terms.getBuckets();
                                for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : buckets) {
                                    double signalWeight = bucket.getDocCount() / totalSignalStrength;
                                    addVertex(vr.fieldName(), bucket.getKeyAsString(), signalWeight, currentHopNumber, 0, 0);
                                }
                            }
                        }
                        expand(searchResponse.isTimedOut());

                    }

                    private double getInitialTotalSignalStrength(Hop rootHop, Sampler sample) {
                        double totalSignalStrength = 0;
                        for (int i = 0; i < rootHop.getNumberVertexRequests(); i++) {
                            if (request.useSignificance()) {
                                SignificantTerms significantTerms = sample.getAggregations().get("field" + i);
                                List<? extends Bucket> buckets = significantTerms.getBuckets();
                                for (Bucket bucket : buckets) {
                                    totalSignalStrength += bucket.getSignificanceScore();
                                }
                            } else {
                                Terms terms = sample.getAggregations().get("field" + i);
                                List<? extends Terms.Bucket> buckets = terms.getBuckets();
                                for (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket : buckets) {
                                    totalSignalStrength += bucket.getDocCount();
                                }
                            }
                        }
                        return totalSignalStrength;
                    }
                });
            } catch (Exception e) {
                logger.error("unable to execute the graph query", e);
                listener.onFailure(e);
            }
        }

        private static void addNormalizedBoosts(BoolQueryBuilder includesContainer, VertexRequest vr) {
            TermBoost[] termBoosts = vr.includeValues();

            if ((includesContainer.should().size() + termBoosts.length) > BooleanQuery.getMaxClauseCount()) {
                List<String> termValues = new ArrayList<>();
                for (TermBoost tb : termBoosts) {
                    termValues.add(tb.getTerm());
                }
                includesContainer.should(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery(vr.fieldName(), termValues)));
                return;

            }
            float minBoost = Float.MAX_VALUE;
            for (TermBoost tb : termBoosts) {
                minBoost = Math.min(minBoost, tb.getBoost());
            }
            for (TermBoost tb : termBoosts) {
                float normalizedBoost = tb.getBoost() / minBoost;
                includesContainer.should(QueryBuilders.termQuery(vr.fieldName(), tb.getTerm()).boost(normalizedBoost));
            }
        }

        void addShardFailures(ShardOperationFailedException[] failures) {
            if (CollectionUtils.isEmpty(failures) == false) {
                ShardOperationFailedException[] duplicates = new ShardOperationFailedException[shardFailures.length + failures.length];
                System.arraycopy(shardFailures, 0, duplicates, 0, shardFailures.length);
                System.arraycopy(failures, 0, duplicates, shardFailures.length, failures.length);
                shardFailures = ExceptionsHelper.groupBy(duplicates);
            }
        }

        protected GraphExploreResponse buildResponse(boolean timedOut) {
            long took = threadPool.relativeTimeInMillis() - startTime;
            return new GraphExploreResponse(took, timedOut, shardFailures, vertices, connections, request.returnDetailedInfo());
        }

    }
}
