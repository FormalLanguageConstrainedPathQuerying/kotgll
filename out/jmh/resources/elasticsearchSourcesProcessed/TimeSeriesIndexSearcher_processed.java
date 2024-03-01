/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.support;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.ThreadInterruptedException;
import org.elasticsearch.cluster.metadata.DataStream;
import org.elasticsearch.common.lucene.search.function.MinScoreScorer;
import org.elasticsearch.index.mapper.DataStreamTimestampFieldMapper;
import org.elasticsearch.index.mapper.TimeSeriesIdFieldMapper;
import org.elasticsearch.search.aggregations.AggregationExecutionContext;
import org.elasticsearch.search.aggregations.BucketCollector;
import org.elasticsearch.search.aggregations.LeafBucketCollector;
import org.elasticsearch.search.internal.ContextIndexSearcher;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.IntSupplier;

import static org.elasticsearch.index.IndexSortConfig.TIME_SERIES_SORT;

/**
 * An IndexSearcher wrapper that executes the searches in time-series indices by traversing them by tsid and timestamp
 * TODO: Convert it to use index sort instead of hard-coded tsid and timestamp values
 */
public class TimeSeriesIndexSearcher {
    private static final int CHECK_CANCELLED_SCORER_INTERVAL = 1 << 11;

    private final ContextIndexSearcher searcher;
    private final List<Runnable> cancellations;
    private final boolean tsidReverse;
    private final boolean timestampReverse;

    private Float minimumScore = null;

    public TimeSeriesIndexSearcher(IndexSearcher searcher, List<Runnable> cancellations) {
        try {
            this.searcher = new ContextIndexSearcher(
                searcher.getIndexReader(),
                searcher.getSimilarity(),
                searcher.getQueryCache(),
                searcher.getQueryCachingPolicy(),
                false,
                searcher.getExecutor(),
                1,
                -1
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.cancellations = cancellations;
        cancellations.forEach(this.searcher::addQueryCancellation);

        assert TIME_SERIES_SORT.length == 2;
        assert TIME_SERIES_SORT[0].getField().equals(TimeSeriesIdFieldMapper.NAME);
        assert TIME_SERIES_SORT[1].getField().equals(DataStreamTimestampFieldMapper.DEFAULT_PATH);
        this.tsidReverse = TIME_SERIES_SORT[0].getOrder() == SortOrder.DESC;
        this.timestampReverse = TIME_SERIES_SORT[1].getOrder() == SortOrder.DESC;
    }

    public void setMinimumScore(Float minimumScore) {
        this.minimumScore = minimumScore;
    }

    public void search(Query query, BucketCollector bucketCollector) throws IOException {
        query = searcher.rewrite(query);
        Weight weight = searcher.createWeight(query, bucketCollector.scoreMode(), 1);
        if (searcher.getExecutor() == null) {
            search(bucketCollector, weight);
            bucketCollector.postCollection();
            return;
        }
        RunnableFuture<Void> task = new FutureTask<>(() -> {
            search(bucketCollector, weight);
            bucketCollector.postCollection();
            return null;
        });
        searcher.getExecutor().execute(task);
        try {
            task.get();
        } catch (InterruptedException e) {
            throw new ThreadInterruptedException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        }
    }

    private void search(BucketCollector bucketCollector, Weight weight) throws IOException {
        int seen = 0;
        int[] tsidOrd = new int[1];

        List<LeafWalker> leafWalkers = new ArrayList<>();
        for (LeafReaderContext leaf : searcher.getIndexReader().leaves()) {
            if (++seen % CHECK_CANCELLED_SCORER_INTERVAL == 0) {
                checkCancelled();
            }
            Scorer scorer = weight.scorer(leaf);
            if (scorer != null) {
                if (minimumScore != null) {
                    scorer = new MinScoreScorer(weight, scorer, minimumScore);
                }
                LeafWalker leafWalker = new LeafWalker(leaf, scorer, bucketCollector, () -> tsidOrd[0]);
                if (leafWalker.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    leafWalkers.add(leafWalker);
                }
            } else {
                bucketCollector.getLeafCollector(new AggregationExecutionContext(leaf, null, null, null));
            }
        }

        PriorityQueue<LeafWalker> queue = new PriorityQueue<>(searcher.getIndexReader().leaves().size()) {
            @Override
            protected boolean lessThan(LeafWalker a, LeafWalker b) {
                if (timestampReverse) {
                    return a.timestamp > b.timestamp;
                } else {
                    return a.timestamp < b.timestamp;
                }
            }
        };

        while (populateQueue(leafWalkers, queue)) {
            do {
                if (++seen % CHECK_CANCELLED_SCORER_INTERVAL == 0) {
                    checkCancelled();
                }
                LeafWalker walker = queue.top();
                walker.collectCurrent();
                if (walker.nextDoc() == DocIdSetIterator.NO_MORE_DOCS || walker.shouldPop()) {
                    queue.pop();
                } else {
                    queue.updateTop();
                }
            } while (queue.size() > 0);
            tsidOrd[0]++;
        }
    }

    public void setProfiler(SearchContext context) {
        if ((context.getProfilers() != null) && (context.getProfilers().getCurrentQueryProfiler() != null)) {
            searcher.setProfiler(context.getProfilers().getCurrentQueryProfiler());
        }
    }

    private boolean populateQueue(List<LeafWalker> leafWalkers, PriorityQueue<LeafWalker> queue) throws IOException {
        BytesRef currentTsid = null;
        assert queue.size() == 0;
        Iterator<LeafWalker> it = leafWalkers.iterator();
        while (it.hasNext()) {
            LeafWalker leafWalker = it.next();
            if (leafWalker.docId == DocIdSetIterator.NO_MORE_DOCS) {
                it.remove();
                continue;
            }
            BytesRef tsid = leafWalker.getTsid();
            if (currentTsid == null) {
                currentTsid = tsid;
            }
            int comp = tsid.compareTo(currentTsid);
            if (comp == 0) {
                queue.add(leafWalker);
            } else if ((tsidReverse && comp > 0) || (false == tsidReverse && comp < 0)) {
                queue.clear();
                queue.add(leafWalker);
                currentTsid = tsid;
            }
        }
        assert queueAllHaveTsid(queue, currentTsid);
        return queue.size() > 0;
    }

    private static boolean queueAllHaveTsid(PriorityQueue<LeafWalker> queue, BytesRef tsid) throws IOException {
        for (LeafWalker leafWalker : queue) {
            BytesRef walkerId = leafWalker.tsids.lookupOrd(leafWalker.tsids.ordValue());
            assert walkerId.equals(tsid) : tsid.utf8ToString() + " != " + walkerId.utf8ToString();
        }
        return true;
    }

    private void checkCancelled() {
        for (Runnable r : cancellations) {
            r.run();
        }
    }

    private static class LeafWalker {
        private final LeafBucketCollector collector;
        private final Bits liveDocs;
        private final DocIdSetIterator iterator;
        private final SortedDocValues tsids;
        private final SortedNumericDocValues timestamps;    
        private final BytesRefBuilder scratch = new BytesRefBuilder();

        int docId = -1;
        int tsidOrd;
        long timestamp;

        LeafWalker(LeafReaderContext context, Scorer scorer, BucketCollector bucketCollector, IntSupplier tsidOrdSupplier)
            throws IOException {
            AggregationExecutionContext aggCtx = new AggregationExecutionContext(context, scratch::get, () -> timestamp, tsidOrdSupplier);
            this.collector = bucketCollector.getLeafCollector(aggCtx);
            liveDocs = context.reader().getLiveDocs();
            this.collector.setScorer(scorer);
            iterator = scorer.iterator();
            tsids = DocValues.getSorted(context.reader(), TimeSeriesIdFieldMapper.NAME);
            timestamps = DocValues.getSortedNumeric(context.reader(), DataStream.TIMESTAMP_FIELD_NAME);
        }

        void collectCurrent() throws IOException {
            assert tsids.docID() == docId;
            assert timestamps.docID() == docId;
            collector.collect(docId);
        }

        int nextDoc() throws IOException {
            if (docId == DocIdSetIterator.NO_MORE_DOCS) {
                return DocIdSetIterator.NO_MORE_DOCS;
            }
            do {
                docId = iterator.nextDoc();
            } while (docId != DocIdSetIterator.NO_MORE_DOCS && isInvalidDoc(docId));
            if (docId != DocIdSetIterator.NO_MORE_DOCS) {
                timestamp = timestamps.nextValue();
            }
            return docId;
        }

        BytesRef getTsid() throws IOException {
            tsidOrd = tsids.ordValue();
            scratch.copyBytes(tsids.lookupOrd(tsidOrd));
            return scratch.get();
        }

        private boolean isInvalidDoc(int docId) throws IOException {
            return (liveDocs != null && liveDocs.get(docId) == false)
                || tsids.advanceExact(docId) == false
                || timestamps.advanceExact(docId) == false;
        }

        boolean shouldPop() throws IOException {
            if (tsidOrd != tsids.ordValue()) {
                return true;
            } else {
                return false;
            }
        }
    }
}
