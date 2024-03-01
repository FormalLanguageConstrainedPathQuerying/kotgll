/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search;

import org.elasticsearch.monitor.jvm.JvmStats;
import org.elasticsearch.threadpool.ThreadPool;

public final class SearchUtils {

    public static final int DEFAULT_MAX_CLAUSE_COUNT = 1024;

    public static int calculateMaxClauseValue(ThreadPool threadPool) {
        int searchThreadPoolSize = threadPool.info(ThreadPool.Names.SEARCH).getMax();
        long heapSize = JvmStats.jvmStats().getMem().getHeapMax().getMb();
        return calculateMaxClauseValue(searchThreadPoolSize, heapSize);
    }

    static int calculateMaxClauseValue(long threadPoolSize, long heapInMb) {
        if (threadPoolSize <= 0 || heapInMb <= 0) {
            return DEFAULT_MAX_CLAUSE_COUNT;
        }
        long maxClauseCount = (heapInMb * 64 / threadPoolSize);
        return Math.max(DEFAULT_MAX_CLAUSE_COUNT, (int) maxClauseCount);
    }

    private SearchUtils() {}
}
