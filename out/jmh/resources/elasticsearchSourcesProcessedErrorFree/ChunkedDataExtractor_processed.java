/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.datafeed.extractor.chunked;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.datafeed.SearchInterval;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractor;
import org.elasticsearch.xpack.ml.datafeed.extractor.DataExtractorFactory;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A wrapper {@link DataExtractor} that can be used with other extractors in order to perform
 * searches in smaller chunks of the time range.
 *
 * <p> The chunk span can be either specified or not. When not specified,
 * a heuristic is employed (see {@link #setUpChunkedSearch()}) to automatically determine the chunk span.
 * The search is set up by querying a data summary for the given time range
 * that includes the number of total hits and the earliest/latest times. Those are then used to determine the chunk span,
 * when necessary, and to jump the search forward to the time where the earliest data can be found.
 * If a search for a chunk returns empty, the set up is performed again for the remaining time.
 *
 * <p> Cancellation's behaviour depends on the delegate extractor.
 *
 * <p> Note that this class is NOT thread-safe.
 */
public class ChunkedDataExtractor implements DataExtractor {

    private static final Logger LOGGER = LogManager.getLogger(ChunkedDataExtractor.class);

    /** Let us set a minimum chunk span of 1 minute */
    private static final long MIN_CHUNK_SPAN = 60000L;

    private final DataExtractorFactory dataExtractorFactory;
    private final ChunkedDataExtractorContext context;
    private long currentStart;
    private long currentEnd;
    private long chunkSpan;
    private boolean isCancelled;
    private DataExtractor currentExtractor;

    public ChunkedDataExtractor(DataExtractorFactory dataExtractorFactory, ChunkedDataExtractorContext context) {
        this.dataExtractorFactory = Objects.requireNonNull(dataExtractorFactory);
        this.context = Objects.requireNonNull(context);
        this.currentStart = context.start();
        this.currentEnd = context.start();
        this.isCancelled = false;
    }

    @Override
    public DataSummary getSummary() {
        return null;
    }

    @Override
    public boolean hasNext() {
        boolean currentHasNext = currentExtractor != null && currentExtractor.hasNext();
        if (isCancelled()) {
            return currentHasNext;
        }
        return currentHasNext || currentEnd < context.end();
    }

    @Override
    public Result next() throws IOException {
        if (hasNext() == false) {
            throw new NoSuchElementException();
        }

        if (currentExtractor == null) {
            setUpChunkedSearch();
        }

        return getNextStream();
    }

    private void setUpChunkedSearch() {
        DataSummary dataSummary = dataExtractorFactory.newExtractor(currentStart, context.end()).getSummary();
        if (dataSummary.hasData()) {
            currentStart = context.timeAligner().alignToFloor(dataSummary.earliestTime());
            currentEnd = currentStart;

            if (context.chunkSpan() != null) {
                chunkSpan = context.chunkSpan().getMillis();
            } else if (context.hasAggregations()) {
                chunkSpan = DatafeedConfig.DEFAULT_AGGREGATION_CHUNKING_BUCKETS * context.histogramInterval();
            } else {
                long timeSpread = dataSummary.latestTime() - dataSummary.earliestTime();
                if (timeSpread <= 0) {
                    chunkSpan = context.end() - currentEnd;
                } else {
                    chunkSpan = Math.max(MIN_CHUNK_SPAN, 10 * (context.scrollSize() * timeSpread) / dataSummary.totalHits());
                }
            }

            chunkSpan = context.timeAligner().alignToCeil(chunkSpan);
            LOGGER.debug("[{}] Chunked search configured: chunk span = {} ms", context.jobId(), chunkSpan);
        } else {
            currentEnd = context.end();
            LOGGER.debug("[{}] Chunked search configured: no data found", context.jobId());
        }
    }

    private Result getNextStream() throws IOException {
        SearchInterval lastSearchInterval = new SearchInterval(context.start(), context.end());
        while (hasNext()) {
            boolean isNewSearch = false;

            if (currentExtractor == null || currentExtractor.hasNext() == false) {
                advanceTime();
                isNewSearch = true;
            }

            Result result = currentExtractor.next();
            lastSearchInterval = result.searchInterval();
            if (result.data().isPresent()) {
                return result;
            }

            if (isNewSearch && hasNext()) {
                currentStart += chunkSpan;
                setUpChunkedSearch();
            }
        }
        return new Result(lastSearchInterval, Optional.empty());
    }

    private void advanceTime() {
        currentStart = currentEnd;
        currentEnd = Math.min(currentStart + chunkSpan, context.end());
        currentExtractor = dataExtractorFactory.newExtractor(currentStart, currentEnd);
        LOGGER.debug("[{}] advances time to [{}, {})", context.jobId(), currentStart, currentEnd);
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void cancel() {
        if (currentExtractor != null) {
            currentExtractor.cancel();
        }
        isCancelled = true;
    }

    @Override
    public void destroy() {
        cancel();
        if (currentExtractor != null) {
            currentExtractor.destroy();
        }
    }

    @Override
    public long getEndTime() {
        return context.end();
    }

    ChunkedDataExtractorContext getContext() {
        return context;
    }
}
