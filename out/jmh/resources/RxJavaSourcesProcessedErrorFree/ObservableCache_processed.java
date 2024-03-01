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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * An observable which auto-connects to another observable, caches the elements
 * from that observable but allows terminating the connection and completing the cache.
 *
 * @param <T> the source element type
 */
public final class ObservableCache<T> extends AbstractObservableWithUpstream<T, T>
implements Observer<T> {

    /**
     * The subscription to the source should happen at most once.
     */
    final AtomicBoolean once;

    /**
     * The number of items per cached nodes.
     */
    final int capacityHint;

    /**
     * The current known array of observer state to notify.
     */
    final AtomicReference<CacheDisposable<T>[]> observers;

    /**
     * A shared instance of an empty array of observers to avoid creating
     * a new empty array when all observers dispose.
     */
    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] EMPTY = new CacheDisposable[0];
    /**
     * A shared instance indicating the source has no more events and there
     * is no need to remember observers anymore.
     */
    @SuppressWarnings("rawtypes")
    static final CacheDisposable[] TERMINATED = new CacheDisposable[0];

    /**
     * The total number of elements in the list available for reads.
     */
    volatile long size;

    /**
     * The starting point of the cached items.
     */
    final Node<T> head;

    /**
     * The current tail of the linked structure holding the items.
     */
    Node<T> tail;

    /**
     * How many items have been put into the tail node so far.
     */
    int tailOffset;

    /**
     * If {@link #observers} is {@link #TERMINATED}, this holds the terminal error if not null.
     */
    Throwable error;

    /**
     * True if the source has terminated.
     */
    volatile boolean done;

    /**
     * Constructs an empty, non-connected cache.
     * @param source the source to subscribe to for the first incoming observer
     * @param capacityHint the number of items expected (reduce allocation frequency)
     */
    @SuppressWarnings("unchecked")
    public ObservableCache(Observable<T> source, int capacityHint) {
        super(source);
        this.capacityHint = capacityHint;
        this.once = new AtomicBoolean();
        Node<T> n = new Node<>(capacityHint);
        this.head = n;
        this.tail = n;
        this.observers = new AtomicReference<>(EMPTY);
    }

    @Override
    protected void subscribeActual(Observer<? super T> t) {
        CacheDisposable<T> consumer = new CacheDisposable<>(t, this);
        t.onSubscribe(consumer);
        add(consumer);

        if (!once.get() && once.compareAndSet(false, true)) {
            source.subscribe(this);
        } else {
            replay(consumer);
        }
    }

    /**
     * Check if this cached observable is connected to its source.
     * @return true if already connected
     */
    /* public */boolean isConnected() {
        return once.get();
    }

    /**
     * Returns true if there are observers subscribed to this observable.
     * @return true if the cache has observers
     */
    /* public */ boolean hasObservers() {
        return observers.get().length != 0;
    }

    /**
     * Returns the number of events currently cached.
     * @return the number of currently cached event count
     */
    /* public */ long cachedEventCount() {
        return size;
    }

    /**
     * Atomically adds the consumer to the {@link #observers} copy-on-write array
     * if the source has not yet terminated.
     * @param consumer the consumer to add
     */
    void add(CacheDisposable<T> consumer) {
        for (;;) {
            CacheDisposable<T>[] current = observers.get();
            if (current == TERMINATED) {
                return;
            }
            int n = current.length;

            @SuppressWarnings("unchecked")
            CacheDisposable<T>[] next = new CacheDisposable[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = consumer;

            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }

    /**
     * Atomically removes the consumer from the {@link #observers} copy-on-write array.
     * @param consumer the consumer to remove
     */
    @SuppressWarnings("unchecked")
    void remove(CacheDisposable<T> consumer) {
        for (;;) {
            CacheDisposable<T>[] current = observers.get();
            int n = current.length;
            if (n == 0) {
                return;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (current[i] == consumer) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }
            CacheDisposable<T>[] next;

            if (n == 1) {
                next = EMPTY;
            } else {
                next = new CacheDisposable[n - 1];
                System.arraycopy(current, 0, next, 0, j);
                System.arraycopy(current, j + 1, next, j, n - j - 1);
            }

            if (observers.compareAndSet(current, next)) {
                return;
            }
        }
    }

    /**
     * Replays the contents of this cache to the given consumer based on its
     * current state and number of items requested by it.
     * @param consumer the consumer to continue replaying items to
     */
    void replay(CacheDisposable<T> consumer) {
        if (consumer.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;
        long index = consumer.index;
        int offset = consumer.offset;
        Node<T> node = consumer.node;
        Observer<? super T> downstream = consumer.downstream;
        int capacity = capacityHint;

        for (;;) {
            if (consumer.disposed) {
                consumer.node = null;
                return;
            }

            boolean sourceDone = done;
            boolean empty = size == index;

            if (sourceDone && empty) {
                consumer.node = null;
                Throwable ex = error;
                if (ex != null) {
                    downstream.onError(ex);
                } else {
                    downstream.onComplete();
                }
                return;
            }

            if (!empty) {
                if (offset == capacity) {
                    node = node.next;
                    offset = 0;
                }

                downstream.onNext(node.values[offset]);

                offset++;
                index++;

                continue;
            }

            consumer.index = index;
            consumer.offset = offset;
            consumer.node = node;
            missed = consumer.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
    }

    @Override
    public void onNext(T t) {
        int tailOffset = this.tailOffset;
        if (tailOffset == capacityHint) {
            Node<T> n = new Node<>(tailOffset);
            n.values[0] = t;
            this.tailOffset = 1;
            tail.next = n;
            tail = n;
        } else {
            tail.values[tailOffset] = t;
            this.tailOffset = tailOffset + 1;
        }
        size++;
        for (CacheDisposable<T> consumer : observers.get()) {
            replay(consumer);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        error = t;
        done = true;
        for (CacheDisposable<T> consumer : observers.getAndSet(TERMINATED)) {
            replay(consumer);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        done = true;
        for (CacheDisposable<T> consumer : observers.getAndSet(TERMINATED)) {
            replay(consumer);
        }
    }

    /**
     * Hosts the downstream consumer and its current requested and replay states.
     * {@code this} holds the work-in-progress counter for the serialized replay.
     * @param <T> the value type
     */
    static final class CacheDisposable<T> extends AtomicInteger
    implements Disposable {

        private static final long serialVersionUID = 6770240836423125754L;

        final Observer<? super T> downstream;

        final ObservableCache<T> parent;

        Node<T> node;

        int offset;

        long index;

        volatile boolean disposed;

        /**
         * Constructs a new instance with the actual downstream consumer and
         * the parent cache object.
         * @param downstream the actual consumer
         * @param parent the parent that holds onto the cached items
         */
        CacheDisposable(Observer<? super T> downstream, ObservableCache<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
            this.node = parent.head;
        }

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                parent.remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }

    /**
     * Represents a segment of the cached item list as
     * part of a linked-node-list structure.
     * @param <T> the element type
     */
    static final class Node<T> {

        /**
         * The array of values held by this node.
         */
        final T[] values;

        /**
         * The next node if not null.
         */
        volatile Node<T> next;

        @SuppressWarnings("unchecked")
        Node(int capacityHint) {
            this.values = (T[])new Object[capacityHint];
        }
    }
}
