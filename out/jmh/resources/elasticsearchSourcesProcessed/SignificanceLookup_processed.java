/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.bucket.terms;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.lucene.index.FilterableTermsEnum;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.BytesRefHash;
import org.elasticsearch.common.util.LongArray;
import org.elasticsearch.common.util.LongHash;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.CardinalityUpperBound;
import org.elasticsearch.search.aggregations.bucket.terms.heuristic.SignificanceHeuristic;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.SamplingContext;

import java.io.IOException;

/**
 * Looks up values used for {@link SignificanceHeuristic}s.
 */
class SignificanceLookup {
    /**
     * Lookup frequencies for {@link BytesRef} terms.
     */
    interface BackgroundFrequencyForBytes extends Releasable {
        long freq(BytesRef term) throws IOException;
    }

    /**
     * Lookup frequencies for {@code long} terms.
     */
    interface BackgroundFrequencyForLong extends Releasable {
        long freq(long term) throws IOException;
    }

    private final AggregationContext context;
    private final MappedFieldType fieldType;
    private final DocValueFormat format;
    private final Query backgroundFilter;
    private final int supersetNumDocs;
    private TermsEnum termsEnum;

    SignificanceLookup(
        AggregationContext context,
        SamplingContext samplingContext,
        MappedFieldType fieldType,
        DocValueFormat format,
        QueryBuilder backgroundFilter
    ) throws IOException {
        this.context = context;
        this.fieldType = fieldType;
        this.format = format;
        Query backgroundQuery = backgroundFilter == null
            ? samplingContext.buildSamplingQueryIfNecessary(context).orElse(null)
            : samplingContext.buildQueryWithSampler(backgroundFilter, context);
        if (backgroundQuery == null) {
            Query matchAllDocsQuery = new MatchAllDocsQuery();
            Query contextFiltered = context.filterQuery(matchAllDocsQuery);
            if (contextFiltered != matchAllDocsQuery) {
                this.backgroundFilter = contextFiltered;
            } else {
                this.backgroundFilter = null;
            }
        } else {
            Query contextFiltered = context.filterQuery(backgroundQuery);
            this.backgroundFilter = contextFiltered;
        }
        /*
         * We need to use a superset size that includes deleted docs or we
         * could end up blowing up with bad statistics that cause us to blow
         * up later on.
         */
        IndexSearcher searcher = context.searcher();
        supersetNumDocs = this.backgroundFilter == null ? searcher.getIndexReader().maxDoc() : searcher.count(this.backgroundFilter);
    }

    /**
     * Get the number of docs in the superset.
     */
    long supersetSize() {
        return supersetNumDocs;
    }

    /**
     * Get the background frequency of a {@link BytesRef} term.
     */
    BackgroundFrequencyForBytes bytesLookup(BigArrays bigArrays, CardinalityUpperBound cardinality) {
        if (cardinality == CardinalityUpperBound.ONE) {
            return new BackgroundFrequencyForBytes() {
                @Override
                public long freq(BytesRef term) throws IOException {
                    return getBackgroundFrequency(term);
                }

                @Override
                public void close() {}
            };
        }
        final BytesRefHash termToPosition = new BytesRefHash(1, bigArrays);
        boolean success = false;
        try {
            BackgroundFrequencyForBytes b = new BackgroundFrequencyForBytes() {
                private LongArray positionToFreq = bigArrays.newLongArray(1, false);

                @Override
                public long freq(BytesRef term) throws IOException {
                    long position = termToPosition.add(term);
                    if (position < 0) {
                        return positionToFreq.get(-1 - position);
                    }
                    long freq = getBackgroundFrequency(term);
                    positionToFreq = bigArrays.grow(positionToFreq, position + 1);
                    positionToFreq.set(position, freq);
                    return freq;
                }

                @Override
                public void close() {
                    Releasables.close(termToPosition, positionToFreq);
                }
            };
            success = true;
            return b;
        } finally {
            if (success == false) {
                termToPosition.close();
            }
        }

    }

    /**
     * Get the background frequency of a {@link BytesRef} term.
     */
    private long getBackgroundFrequency(BytesRef term) throws IOException {
        return getBackgroundFrequency(context.buildQuery(new TermQueryBuilder(fieldType.name(), format.format(term).toString())));
    }

    /**
     * Get the background frequency of a {@code long} term.
     */
    BackgroundFrequencyForLong longLookup(BigArrays bigArrays, CardinalityUpperBound cardinality) {
        if (cardinality == CardinalityUpperBound.ONE) {
            return new BackgroundFrequencyForLong() {
                @Override
                public long freq(long term) throws IOException {
                    return getBackgroundFrequency(term);
                }

                @Override
                public void close() {}
            };
        }
        final LongHash termToPosition = new LongHash(1, bigArrays);
        boolean success = false;
        try {
            BackgroundFrequencyForLong b = new BackgroundFrequencyForLong() {

                private LongArray positionToFreq = bigArrays.newLongArray(1, false);

                @Override
                public long freq(long term) throws IOException {
                    long position = termToPosition.add(term);
                    if (position < 0) {
                        return positionToFreq.get(-1 - position);
                    }
                    long freq = getBackgroundFrequency(term);
                    positionToFreq = bigArrays.grow(positionToFreq, position + 1);
                    positionToFreq.set(position, freq);
                    return freq;
                }

                @Override
                public void close() {
                    Releasables.close(termToPosition, positionToFreq);
                }
            };
            success = true;
            return b;
        } finally {
            if (success == false) {
                termToPosition.close();
            }
        }
    }

    /**
     * Get the background frequency of a {@code long} term.
     */
    private long getBackgroundFrequency(long term) throws IOException {
        return getBackgroundFrequency(context.buildQuery(new TermQueryBuilder(fieldType.name(), format.format(term).toString())));
    }

    private long getBackgroundFrequency(Query query) throws IOException {
        if (query instanceof TermQuery) {
            Term term = ((TermQuery) query).getTerm();
            TermsEnum termsEnum = getTermsEnum();
            if (termsEnum.seekExact(term.bytes())) {
                return termsEnum.docFreq();
            }
            return 0;
        }
        if (backgroundFilter != null) {
            query = new BooleanQuery.Builder().add(query, Occur.FILTER).add(backgroundFilter, Occur.FILTER).build();
        }
        return new IndexSearcher(context.searcher().getIndexReader()).count(query);
    }

    private TermsEnum getTermsEnum() throws IOException {
        if (termsEnum != null) {
            return termsEnum;
        }
        IndexReader reader = context.searcher().getIndexReader();
        termsEnum = new FilterableTermsEnum(reader, fieldType.name(), PostingsEnum.NONE, backgroundFilter);
        return termsEnum;
    }

}
