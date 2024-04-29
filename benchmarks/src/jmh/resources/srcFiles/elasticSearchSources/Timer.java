/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.profile;

/** Helps measure how much time is spent running some methods.
 *  The {@link #start()} and {@link #stop()} methods should typically be called
 *  in a try/finally clause with {@link #start()} being called right before the
 *  try block and {@link #stop()} being called at the beginning of the finally
 *  block:
 *  <pre>
 *  timer.start();
 *  try {
 *    
 *  } finally {
 *    timer.stop();
 *  }
 *  </pre>
 */
public class Timer {

    private boolean doTiming;
    private long timing, count, lastCount, start;

    /** pkg-private for testing */
    long nanoTime() {
        return System.nanoTime();
    }

    /** Start the timer. */
    public final void start() {
        assert start == 0 : "#start call misses a matching #stop call";
        doTiming = (count - lastCount) >= Math.min(lastCount >>> 8, 1024);
        if (doTiming) {
            start = nanoTime();
        }
        count++;
    }

    /** Stop the timer. */
    public final void stop() {
        if (doTiming) {
            timing += (count - lastCount) * Math.max(nanoTime() - start, 1L);
            lastCount = count;
            start = 0;
        }
    }

    /** Return the number of times that {@link #start()} has been called. */
    public final long getCount() {
        if (start != 0) {
            throw new IllegalStateException("#start call misses a matching #stop call");
        }
        return count;
    }

    /** Return an approximation of the total time spent between consecutive calls of #start and #stop. */
    public final long getApproximateTiming() {
        if (start != 0) {
            throw new IllegalStateException("#start call misses a matching #stop call");
        }
        long timing = this.timing;
        if (count > lastCount) {
            assert lastCount > 0;
            timing += (count - lastCount) * timing / lastCount;
        }
        return timing;
    }
}
