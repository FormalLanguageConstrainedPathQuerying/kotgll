/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.util;

import org.apache.lucene.util.hppc.BitMixer;
import org.elasticsearch.core.Releasable;

/**
 * Base implementation for a hash table that is paged, recycles arrays and grows in-place.
 */
abstract class AbstractPagedHashMap implements Releasable {

    static final float DEFAULT_MAX_LOAD_FACTOR = 0.6f;

    static long hash(long value) {
        return BitMixer.mix64(value);
    }

    final BigArrays bigArrays;
    final float maxLoadFactor;
    long size, maxSize;
    long mask;

    AbstractPagedHashMap(long capacity, float maxLoadFactor, BigArrays bigArrays) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        if (maxLoadFactor <= 0 || maxLoadFactor >= 1) {
            throw new IllegalArgumentException("maxLoadFactor must be > 0 and < 1");
        }
        this.bigArrays = bigArrays;
        this.maxLoadFactor = maxLoadFactor;
        long buckets = 1L + (long) (capacity / maxLoadFactor);
        buckets = Math.max(1, Long.highestOneBit(buckets - 1) << 1); 
        assert buckets == Long.highestOneBit(buckets);
        maxSize = (long) (buckets * maxLoadFactor);
        assert maxSize >= capacity;
        size = 0;
        mask = buckets - 1;
    }

    /**
     * Return the number of allocated slots to store this hash table.
     */
    public long capacity() {
        return mask + 1;
    }

    /**
     * Return the number of longs in this hash table.
     */
    public long size() {
        return size;
    }

    static long slot(long hash, long mask) {
        return hash & mask;
    }

    static long nextSlot(long curSlot, long mask) {
        return (curSlot + 1) & mask; 
    }

    /** Resize to the given capacity. */
    protected abstract void resize(long capacity);

    protected abstract boolean used(long bucket);

    /** Remove the entry at the given index and add it back */
    protected abstract void removeAndAdd(long index);

    protected final void grow() {
        assert size == maxSize;
        final long prevSize = size;
        final long buckets = capacity();
        final long newBuckets = buckets << 1;
        assert newBuckets == Long.highestOneBit(newBuckets) : newBuckets; 
        resize(newBuckets);
        mask = newBuckets - 1;
        for (long i = 0; i < buckets; ++i) {
            if (used(i)) {
                removeAndAdd(i);
            }
        }
        for (long i = buckets; i < newBuckets; ++i) {
            if (used(i)) {
                removeAndAdd(i); 
            } else {
                break;
            }
        }
        assert size == prevSize;
        maxSize = (long) (newBuckets * maxLoadFactor);
        assert size < maxSize;
    }

}
