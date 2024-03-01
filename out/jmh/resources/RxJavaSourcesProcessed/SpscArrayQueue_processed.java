/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

/*
 * The code was inspired by the similarly named JCTools class:
 * https:
 */

package io.reactivex.rxjava3.operators;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.internal.util.Pow2;

/**
 * A Single-Producer-Single-Consumer queue backed by a pre-allocated buffer.
 * <p>
 * This implementation is a mashup of the <a href="http:
 * algorithm with an optimization of the offer method taken from the <a
 * href="http:
 * Flow), and adjusted to comply with Queue.offer semantics with regards to capacity.<br>
 * For convenience the relevant papers are available in the resources folder:<br>
 * <i>2010 - Pisa - SPSC Queues on Shared Cache Multi-Core Systems.pdf<br>
 * 2012 - Junchang- BQueue- Efficient and Practical Queuing.pdf <br>
 * </i> This implementation is wait free.
 *
 * @param <E> the element type of the queue
 * @since 3.1.1
 */
public final class SpscArrayQueue<E> extends AtomicReferenceArray<E> implements SimplePlainQueue<E> {
    private static final long serialVersionUID = -1296597691183856449L;
    private static final Integer MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
    final int mask;
    final AtomicLong producerIndex;
    long producerLookAhead;
    final AtomicLong consumerIndex;
    final int lookAheadStep;

    /**
     * Constructs an array-backed queue with the given capacity rounded
     * up to the next power of 2 size.
     * @param capacity the maximum number of elements the queue would hold,
     *                 rounded up to the next power of 2
     */
    public SpscArrayQueue(int capacity) {
        super(Pow2.roundToPowerOfTwo(capacity));
        this.mask = length() - 1;
        this.producerIndex = new AtomicLong();
        this.consumerIndex = new AtomicLong();
        lookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
    }

    @Override
    public boolean offer(E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final int mask = this.mask;
        final long index = producerIndex.get();
        final int offset = calcElementOffset(index, mask);
        if (index >= producerLookAhead) {
            int step = lookAheadStep;
            if (null == lvElement(calcElementOffset(index + step, mask))) { 
                producerLookAhead = index + step;
            } else if (null != lvElement(offset)) {
                return false;
            }
        }
        soElement(offset, e); 
        soProducerIndex(index + 1); 
        return true;
    }

    @Override
    public boolean offer(E v1, E v2) {
        return offer(v1) && offer(v2);
    }

    @Nullable
    @Override
    public E poll() {
        final long index = consumerIndex.get();
        final int offset = calcElementOffset(index);
        final E e = lvElement(offset); 
        if (null == e) {
            return null;
        }
        soConsumerIndex(index + 1); 
        soElement(offset, null); 
        return e;
    }

    @Override
    public boolean isEmpty() {
        return producerIndex.get() == consumerIndex.get();
    }

    void soProducerIndex(long newIndex) {
        producerIndex.lazySet(newIndex);
    }

    void soConsumerIndex(long newIndex) {
        consumerIndex.lazySet(newIndex);
    }

    @Override
    public void clear() {
        while (poll() != null || !isEmpty()) { } 
    }

    int calcElementOffset(long index, int mask) {
        return (int)index & mask;
    }

    int calcElementOffset(long index) {
        return (int)index & mask;
    }

    void soElement(int offset, E value) {
        lazySet(offset, value);
    }

    E lvElement(int offset) {
        return get(offset);
    }
}

