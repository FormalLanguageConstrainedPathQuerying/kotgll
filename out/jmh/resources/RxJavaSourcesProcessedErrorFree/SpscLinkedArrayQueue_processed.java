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
 * A single-producer single-consumer array-backed queue which can allocate new arrays in case the consumer is slower
 * than the producer.
 * @param <T> the contained value type
 * @since 3.1.1
 */
public final class SpscLinkedArrayQueue<T> implements SimplePlainQueue<T> {
    static final int MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
    final AtomicLong producerIndex = new AtomicLong();

    int producerLookAheadStep;
    long producerLookAhead;

    final int producerMask;

    AtomicReferenceArray<Object> producerBuffer;
    final int consumerMask;
    AtomicReferenceArray<Object> consumerBuffer;
    final AtomicLong consumerIndex = new AtomicLong();

    private static final Object HAS_NEXT = new Object();

    /**
     * Constructs a linked array-based queue instance with the given
     * island size rounded up to the next power of 2.
     * @param bufferSize the maximum number of elements per island
     */
    public SpscLinkedArrayQueue(final int bufferSize) {
        int p2capacity = Pow2.roundToPowerOfTwo(Math.max(8, bufferSize));
        int mask = p2capacity - 1;
        AtomicReferenceArray<Object> buffer = new AtomicReferenceArray<>(p2capacity + 1);
        producerBuffer = buffer;
        producerMask = mask;
        adjustLookAheadStep(p2capacity);
        consumerBuffer = buffer;
        consumerMask = mask;
        producerLookAhead = mask - 1; 
        soProducerIndex(0L);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    @Override
    public boolean offer(final T e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final AtomicReferenceArray<Object> buffer = producerBuffer;
        final long index = lpProducerIndex();
        final int mask = producerMask;
        final int offset = calcWrappedOffset(index, mask);
        if (index < producerLookAhead) {
            return writeToQueue(buffer, e, index, offset);
        } else {
            final int lookAheadStep = producerLookAheadStep;
            int lookAheadElementOffset = calcWrappedOffset(index + lookAheadStep, mask);
            if (null == lvElement(buffer, lookAheadElementOffset)) { 
                producerLookAhead = index + lookAheadStep - 1; 
                return writeToQueue(buffer, e, index, offset);
            } else if (null == lvElement(buffer, calcWrappedOffset(index + 1, mask))) { 
                return writeToQueue(buffer, e, index, offset);
            } else {
                resize(buffer, index, offset, e, mask); 
                return true;
            }
        }
    }

    private boolean writeToQueue(final AtomicReferenceArray<Object> buffer, final T e, final long index, final int offset) {
        soElement(buffer, offset, e); 
        soProducerIndex(index + 1); 
        return true;
    }

    private void resize(final AtomicReferenceArray<Object> oldBuffer, final long currIndex, final int offset, final T e,
            final long mask) {
        final int capacity = oldBuffer.length();
        final AtomicReferenceArray<Object> newBuffer = new AtomicReferenceArray<>(capacity);
        producerBuffer = newBuffer;
        producerLookAhead = currIndex + mask - 1;
        soElement(newBuffer, offset, e); 
        soNext(oldBuffer, newBuffer);
        soElement(oldBuffer, offset, HAS_NEXT); 
        soProducerIndex(currIndex + 1); 
    }

    private void soNext(AtomicReferenceArray<Object> curr, AtomicReferenceArray<Object> next) {
        soElement(curr, calcDirectOffset(curr.length() - 1), next);
    }

    @SuppressWarnings("unchecked")
    private AtomicReferenceArray<Object> lvNextBufferAndUnlink(AtomicReferenceArray<Object> curr, int nextIndex) {
        int nextOffset = calcDirectOffset(nextIndex);
        AtomicReferenceArray<Object> nextBuffer = (AtomicReferenceArray<Object>)lvElement(curr, nextOffset);
        soElement(curr, nextOffset, null); 
        return nextBuffer;
    }
    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public T poll() {
        final AtomicReferenceArray<Object> buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final int mask = consumerMask;
        final int offset = calcWrappedOffset(index, mask);
        final Object e = lvElement(buffer, offset); 
        boolean isNextBuffer = e == HAS_NEXT;
        if (null != e && !isNextBuffer) {
            soElement(buffer, offset, null); 
            soConsumerIndex(index + 1); 
            return (T) e;
        } else if (isNextBuffer) {
            return newBufferPoll(lvNextBufferAndUnlink(buffer, mask + 1), index, mask);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private T newBufferPoll(AtomicReferenceArray<Object> nextBuffer, final long index, final int mask) {
        consumerBuffer = nextBuffer;
        final int offsetInNew = calcWrappedOffset(index, mask);
        final T n = (T) lvElement(nextBuffer, offsetInNew); 
        if (null != n) {
            soElement(nextBuffer, offsetInNew, null); 
            soConsumerIndex(index + 1); 
        }
        return n;
    }

    /**
     * Returns the next element in this queue without removing it or {@code null}
     * if this queue is empty
     * @return the next element or {@code null}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T peek() {
        final AtomicReferenceArray<Object> buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final int mask = consumerMask;
        final int offset = calcWrappedOffset(index, mask);
        final Object e = lvElement(buffer, offset); 
        if (e == HAS_NEXT) {
            return newBufferPeek(lvNextBufferAndUnlink(buffer, mask + 1), index, mask);
        }

        return (T) e;
    }

    @SuppressWarnings("unchecked")
    private T newBufferPeek(AtomicReferenceArray<Object> nextBuffer, final long index, final int mask) {
        consumerBuffer = nextBuffer;
        final int offsetInNew = calcWrappedOffset(index, mask);
        return (T) lvElement(nextBuffer, offsetInNew); 
    }

    @Override
    public void clear() {
        while (poll() != null || !isEmpty()) { } 
    }

    /**
     * Returns the number of elements in the queue.
     * @return the number of elements in the queue
     */
    public int size() {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and
         * consumer indices, therefore protection is required to ensure size is within valid range. In the
         * event of concurrent polls/offers to this method the size is OVER estimated as we read consumer
         * index BEFORE the producer index.
         */
        long after = lvConsumerIndex();
        while (true) {
            final long before = after;
            final long currentProducerIndex = lvProducerIndex();
            after = lvConsumerIndex();
            if (before == after) {
                return (int) (currentProducerIndex - after);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return lvProducerIndex() == lvConsumerIndex();
    }

    private void adjustLookAheadStep(int capacity) {
        producerLookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
    }

    private long lvProducerIndex() {
        return producerIndex.get();
    }

    private long lvConsumerIndex() {
        return consumerIndex.get();
    }

    private long lpProducerIndex() {
        return producerIndex.get();
    }

    private long lpConsumerIndex() {
        return consumerIndex.get();
    }

    private void soProducerIndex(long v) {
        producerIndex.lazySet(v);
    }

    private void soConsumerIndex(long v) {
        consumerIndex.lazySet(v);
    }

    private static int calcWrappedOffset(long index, int mask) {
        return calcDirectOffset((int)index & mask);
    }
    private static int calcDirectOffset(int index) {
        return index;
    }
    private static void soElement(AtomicReferenceArray<Object> buffer, int offset, Object e) {
        buffer.lazySet(offset, e);
    }

    private static Object lvElement(AtomicReferenceArray<Object> buffer, int offset) {
        return buffer.get(offset);
    }

    /**
     * Offer two elements at the same time.
     * <p>Don't use the regular offer() with this at all!
     * @param first the first value, not null
     * @param second the second value, not null
     * @return true if the queue accepted the two new values
     */
    @Override
    public boolean offer(T first, T second) {
        final AtomicReferenceArray<Object> buffer = producerBuffer;
        final long p = lvProducerIndex();
        final int m = producerMask;

        int pi = calcWrappedOffset(p + 2, m);

        if (null == lvElement(buffer, pi)) {
            pi = calcWrappedOffset(p, m);
            soElement(buffer, pi + 1, second);
            soElement(buffer, pi, first);
            soProducerIndex(p + 2);
        } else {
            final int capacity = buffer.length();
            final AtomicReferenceArray<Object> newBuffer = new AtomicReferenceArray<>(capacity);
            producerBuffer = newBuffer;

            pi = calcWrappedOffset(p, m);
            soElement(newBuffer, pi + 1, second); 
            soElement(newBuffer, pi, first);
            soNext(buffer, newBuffer);

            soElement(buffer, pi, HAS_NEXT); 

            soProducerIndex(p + 2); 
        }

        return true;
    }
}
