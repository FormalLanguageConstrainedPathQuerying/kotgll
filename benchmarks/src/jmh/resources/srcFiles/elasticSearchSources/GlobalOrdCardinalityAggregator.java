/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.metrics;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.common.hash.MurmurHash3;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.BitArray;
import org.elasticsearch.common.util.LongArray;
import org.elasticsearch.common.util.ObjectArray;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.search.aggregations.AggregationExecutionContext;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.LeafBucketCollector;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.ValuesSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * An aggregator that computes approximate counts of unique values
 * using global ords.
 */
public class GlobalOrdCardinalityAggregator extends NumericMetricsAggregator.SingleValue {

    private static final int MAX_FIELD_CARDINALITY_FOR_DYNAMIC_PRUNING = 1024;

    private static final int MAX_TERMS_FOR_DYNAMIC_PRUNING = 128;

    private final ValuesSource.Bytes.WithOrdinals valuesSource;
    private final String field;
    private final BigArrays bigArrays;
    private final int maxOrd;
    private final int precision;
    private int dynamicPruningAttempts;
    private int dynamicPruningSuccess;
    private int bruteForce;
    private int noData;

    @Nullable
    private HyperLogLogPlusPlus counts;
    private ObjectArray<BitArray> visitedOrds;
    private SortedSetDocValues values;

    public GlobalOrdCardinalityAggregator(
        String name,
        ValuesSource.Bytes.WithOrdinals valuesSource,
        String field,
        int precision,
        int maxOrd,
        AggregationContext context,
        Aggregator parent,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, context, parent, metadata);
        this.valuesSource = valuesSource;
        this.field = field;
        this.precision = precision;
        this.maxOrd = maxOrd;
        this.bigArrays = context.bigArrays();
        this.visitedOrds = bigArrays.newObjectArray(1);
    }

    @Override
    public ScoreMode scoreMode() {
        if (field != null && valuesSource.needsScores() == false && maxOrd <= MAX_FIELD_CARDINALITY_FOR_DYNAMIC_PRUNING) {
            return ScoreMode.TOP_DOCS;
        } else if (valuesSource.needsScores()) {
            return ScoreMode.COMPLETE;
        } else {
            return ScoreMode.COMPLETE_NO_SCORES;
        }
    }

    /**
     * A competitive iterator that helps only collect values that have not been collected so far.
     */
    private class CompetitiveIterator extends DocIdSetIterator {

        private final BitArray visitedOrds;
        private long numNonVisitedOrds;
        private final TermsEnum indexTerms;
        private final DocIdSetIterator docsWithField;

        CompetitiveIterator(int numNonVisitedOrds, BitArray visitedOrds, Terms indexTerms, DocIdSetIterator docsWithField)
            throws IOException {
            this.visitedOrds = visitedOrds;
            this.numNonVisitedOrds = numNonVisitedOrds;
            this.indexTerms = Objects.requireNonNull(indexTerms).iterator();
            this.docsWithField = docsWithField;
        }

        private Map<Long, PostingsEnum> nonVisitedOrds;
        private PriorityQueue<PostingsEnum> nonVisitedPostings;

        private int doc = -1;

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() throws IOException {
            return advance(doc + 1);
        }

        @Override
        public int advance(int target) throws IOException {
            if (nonVisitedPostings == null) {
                return doc = docsWithField.advance(target);
            } else if (nonVisitedPostings.size() == 0) {
                return doc = DocIdSetIterator.NO_MORE_DOCS;
            } else {
                PostingsEnum top = nonVisitedPostings.top();
                while (top.docID() < target) {
                    top.advance(target);
                    top = nonVisitedPostings.updateTop();
                }
                return doc = top.docID();
            }
        }

        @Override
        public long cost() {
            return docsWithField.cost();
        }

        void startPruning() throws IOException {
            dynamicPruningSuccess++;
            nonVisitedOrds = new HashMap<>();
            for (long ord = 0; ord < maxOrd; ++ord) {
                if (visitedOrds.get(ord)) {
                    continue;
                }
                BytesRef term = values.lookupOrd(ord);
                if (indexTerms.seekExact(term) == false) {
                    continue;
                }
                nonVisitedOrds.put(ord, indexTerms.postings(null, PostingsEnum.NONE));
            }
            nonVisitedPostings = new PriorityQueue<>(nonVisitedOrds.size()) {
                @Override
                protected boolean lessThan(PostingsEnum a, PostingsEnum b) {
                    return a.docID() < b.docID();
                }
            };
            for (PostingsEnum pe : nonVisitedOrds.values()) {
                nonVisitedPostings.add(pe);
            }
        }

        void onVisitedOrdinal(long ordinal) throws IOException {
            numNonVisitedOrds--;
            if (nonVisitedOrds == null) {
                if (numNonVisitedOrds <= MAX_TERMS_FOR_DYNAMIC_PRUNING) {
                    startPruning();
                }
            } else {
                if (nonVisitedOrds.remove(ordinal) != null) {
                    nonVisitedPostings.clear();
                    for (PostingsEnum pe : nonVisitedOrds.values()) {
                        nonVisitedPostings.add(pe);
                    }
                }
            }
        }
    }

    @Override
    public LeafBucketCollector getLeafCollector(AggregationExecutionContext aggCtx, final LeafBucketCollector sub) throws IOException {
        values = valuesSource.globalOrdinalsValues(aggCtx.getLeafReaderContext());

        if (parent == null && field != null) {
            final Terms indexTerms = aggCtx.getLeafReaderContext().reader().terms(field);
            if (indexTerms != null) {
                visitedOrds = bigArrays.grow(visitedOrds, 1);
                BitArray bits = visitedOrds.get(0);
                final int numNonVisitedOrds = maxOrd - (bits == null ? 0 : (int) bits.cardinality());
                if (maxOrd <= MAX_FIELD_CARDINALITY_FOR_DYNAMIC_PRUNING || numNonVisitedOrds <= MAX_TERMS_FOR_DYNAMIC_PRUNING) {
                    dynamicPruningAttempts++;
                    return new LeafBucketCollector() {
                        final SortedSetDocValues docValues = values;

                        final BitArray bits;
                        final CompetitiveIterator competitiveIterator;

                        {
                            visitedOrds = bigArrays.grow(visitedOrds, 1);
                            BitArray bits = visitedOrds.get(0);
                            if (bits == null) {
                                bits = new BitArray(maxOrd, bigArrays);
                                visitedOrds.set(0, bits);
                            }
                            this.bits = bits;
                            final DocIdSetIterator docsWithField = valuesSource.ordinalsValues(aggCtx.getLeafReaderContext());
                            competitiveIterator = new CompetitiveIterator(numNonVisitedOrds, bits, indexTerms, docsWithField);
                            if (numNonVisitedOrds <= MAX_TERMS_FOR_DYNAMIC_PRUNING) {
                                competitiveIterator.startPruning();
                            }
                        }

                        @Override
                        public void collect(int doc, long bucketOrd) throws IOException {
                            if (docValues.advanceExact(doc)) {
                                for (long ord = docValues.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = docValues.nextOrd()) {
                                    if (bits.getAndSet(ord) == false) {
                                        competitiveIterator.onVisitedOrdinal(ord);
                                    }
                                }
                            }
                        }

                        @Override
                        public CompetitiveIterator competitiveIterator() {
                            return competitiveIterator;
                        }
                    };
                }
            } else {
                final FieldInfo fi = aggCtx.getLeafReaderContext().reader().getFieldInfos().fieldInfo(field);
                if (fi != null && fi.getIndexOptions() != IndexOptions.NONE) {
                    noData++;
                    return LeafBucketCollector.NO_OP_COLLECTOR;
                }
            }
        }

        bruteForce++;
        return new LeafBucketCollector() {
            final SortedSetDocValues docValues = values;

            @Override
            public void collect(int doc, long bucketOrd) throws IOException {
                if (docValues.advanceExact(doc)) {
                    visitedOrds = bigArrays.grow(visitedOrds, bucketOrd + 1);
                    BitArray bits = visitedOrds.get(bucketOrd);
                    if (bits == null) {
                        bits = new BitArray(maxOrd, bigArrays);
                        visitedOrds.set(bucketOrd, bits);
                    }
                    for (long ord = docValues.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = docValues.nextOrd()) {
                        bits.set((int) ord);
                    }
                }
            }
        };
    }

    protected void doPostCollection() throws IOException {
        counts = new HyperLogLogPlusPlus(precision, bigArrays, visitedOrds.size());
        try (LongArray hashes = bigArrays.newLongArray(maxOrd, false)) {
            try (BitArray allVisitedOrds = new BitArray(maxOrd, bigArrays)) {
                for (long bucket = visitedOrds.size() - 1; bucket >= 0; --bucket) {
                    final BitArray bits = visitedOrds.get(bucket);
                    if (bits != null) {
                        allVisitedOrds.or(bits);
                    }
                }

                final MurmurHash3.Hash128 hash = new MurmurHash3.Hash128();
                for (long ord = allVisitedOrds.nextSetBit(0); ord < Long.MAX_VALUE; ord = ord + 1 < maxOrd
                    ? allVisitedOrds.nextSetBit(ord + 1)
                    : Long.MAX_VALUE) {
                    final BytesRef value = values.lookupOrd(ord);
                    MurmurHash3.hash128(value.bytes, value.offset, value.length, 0, hash);
                    hashes.set(ord, hash.h1);
                }
            }
            for (long bucket = visitedOrds.size() - 1; bucket >= 0; --bucket) {
                try (BitArray bits = visitedOrds.get(bucket)) {
                    if (bits != null) {
                        visitedOrds.set(bucket, null); 
                        for (long ord = bits.nextSetBit(0); ord < Long.MAX_VALUE; ord = ord + 1 < maxOrd
                            ? bits.nextSetBit(ord + 1)
                            : Long.MAX_VALUE) {
                            counts.collect(bucket, hashes.get(ord));
                        }
                    }
                }
            }
            Releasables.close(visitedOrds);
            visitedOrds = null;
        }
    }

    @Override
    public double metric(long owningBucketOrd) {
        return counts.cardinality(owningBucketOrd);
    }

    @Override
    public InternalAggregation buildAggregation(long owningBucketOrdinal) {
        if (counts == null || owningBucketOrdinal >= counts.maxOrd() || counts.cardinality(owningBucketOrdinal) == 0) {
            return buildEmptyAggregation();
        }
        AbstractHyperLogLogPlusPlus copy = counts.clone(owningBucketOrdinal, BigArrays.NON_RECYCLING_INSTANCE);
        return new InternalCardinality(name, copy, metadata());
    }

    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalCardinality(name, null, metadata());
    }

    @Override
    protected void doClose() {
        if (visitedOrds != null) {
            for (int i = 0; i < visitedOrds.size(); i++) {
                Releasables.close(visitedOrds.get(i));
            }
        }
        Releasables.close(visitedOrds, counts);
    }

    @Override
    public void collectDebugInfo(BiConsumer<String, Object> add) {
        super.collectDebugInfo(add);
        add.accept("dynamic_pruning_attempted", dynamicPruningAttempts);
        add.accept("dynamic_pruning_used", dynamicPruningSuccess);
        add.accept("brute_force_used", bruteForce);
        add.accept("skipped_due_to_no_data", noData);
    }
}
