/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.lucene.queries;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Objects;

/** A {@link Query} that only matches documents that are greater than or equal
 *  to a configured doc ID. */
public final class MinDocQuery extends Query {

    private final Object readerId;
    private final int minDoc;

    /** Sole constructor. */
    public MinDocQuery(int minDoc) {
        this(minDoc, null);
    }

    MinDocQuery(int minDoc, Object readerId) {
        this.minDoc = minDoc;
        this.readerId = readerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classHash(), minDoc, readerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (sameClassAs(obj) == false) {
            return false;
        }
        MinDocQuery that = (MinDocQuery) obj;
        return minDoc == that.minDoc && Objects.equals(readerId, that.readerId);
    }

    @Override
    public Query rewrite(IndexSearcher searcher) throws IOException {
        IndexReader reader = searcher.getIndexReader();
        if (Objects.equals(reader.getContext().id(), readerId) == false) {
            return new MinDocQuery(minDoc, reader.getContext().id());
        }
        return this;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        if (readerId == null) {
            throw new IllegalStateException("Rewrite first");
        } else if (Objects.equals(searcher.getIndexReader().getContext().id(), readerId) == false) {
            throw new IllegalStateException("Executing against a different reader than the query has been rewritten against");
        }
        return new ConstantScoreWeight(this, boost) {
            @Override
            public Scorer scorer(LeafReaderContext context) throws IOException {
                final int maxDoc = context.reader().maxDoc();
                if (context.docBase + maxDoc <= minDoc) {
                    return null;
                }
                final int segmentMinDoc = Math.max(0, minDoc - context.docBase);
                final DocIdSetIterator disi = new MinDocIterator(segmentMinDoc, maxDoc);
                return new ConstantScoreScorer(this, score(), scoreMode, disi);
            }

            @Override
            public boolean isCacheable(LeafReaderContext ctx) {
                return false;
            }
        };
    }

    static class MinDocIterator extends DocIdSetIterator {
        final int segmentMinDoc;
        final int maxDoc;
        int doc = -1;

        MinDocIterator(int segmentMinDoc, int maxDoc) {
            this.segmentMinDoc = segmentMinDoc;
            this.maxDoc = maxDoc;
        }

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
            assert target > doc;
            if (doc == -1) {
                doc = Math.max(target, segmentMinDoc);
            } else {
                doc = target;
            }
            if (doc >= maxDoc) {
                doc = NO_MORE_DOCS;
            }
            return doc;
        }

        @Override
        public long cost() {
            return maxDoc - segmentMinDoc;
        }
    }

    @Override
    public void visit(QueryVisitor visitor) {
        visitor.visitLeaf(this);
    }

    @Override
    public String toString(String field) {
        return "MinDocQuery(minDoc=" + minDoc + ")";
    }
}
