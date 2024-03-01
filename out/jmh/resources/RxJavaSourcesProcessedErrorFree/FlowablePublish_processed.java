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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.operators.SimpleQueue;
import io.reactivex.rxjava3.operators.SpscArrayQueue;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Shares a single underlying connection to the upstream Publisher
 * and multicasts events to all subscribed subscribers until the upstream
 * completes or the connection is disposed.
 * <p>
 * The difference to FlowablePublish is that when the upstream terminates,
 * late subscribers will receive that terminal event until the connection is
 * disposed and the ConnectableFlowable is reset to its fresh state.
 *
 * @param <T> the element type
 * @since 2.2.10
 */
public final class FlowablePublish<T> extends ConnectableFlowable<T>
implements HasUpstreamPublisher<T> {

    final Publisher<T> source;

    final int bufferSize;

    final AtomicReference<PublishConnection<T>> current;

    public FlowablePublish(Publisher<T> source, int bufferSize) {
        this.source = source;
        this.bufferSize = bufferSize;
        this.current = new AtomicReference<>();
    }

    @Override
    public Publisher<T> source() {
        return source;
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        PublishConnection<T> conn;
        boolean doConnect = false;

        for (;;) {
            conn = current.get();

            if (conn == null || conn.isDisposed()) {
                PublishConnection<T> fresh = new PublishConnection<>(current, bufferSize);
                if (!current.compareAndSet(conn, fresh)) {
                    continue;
                }
                conn = fresh;
            }

            doConnect = !conn.connect.get() && conn.connect.compareAndSet(false, true);
            break;
        }

        try {
            connection.accept(conn);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        if (doConnect) {
            source.subscribe(conn);
        }
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        PublishConnection<T> conn;

        for (;;) {
            conn = current.get();

            if (conn == null) {
                PublishConnection<T> fresh = new PublishConnection<>(current, bufferSize);
                if (!current.compareAndSet(conn, fresh)) {
                    continue;
                }
                conn = fresh;
            }

            break;
        }

        InnerSubscription<T> inner = new InnerSubscription<>(s, conn);
        s.onSubscribe(inner);

        if (conn.add(inner)) {
            if (inner.isCancelled()) {
                conn.remove(inner);
            } else {
                conn.drain();
            }
            return;
        }

        Throwable ex = conn.error;
        if (ex != null) {
            inner.downstream.onError(ex);
        } else {
            inner.downstream.onComplete();
        }
    }

    @Override
    public void reset() {
        PublishConnection<T> conn = current.get();
        if (conn != null && conn.isDisposed()) {
            current.compareAndSet(conn, null);
        }
    }

    static final class PublishConnection<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Disposable {

        private static final long serialVersionUID = -1672047311619175801L;

        final AtomicReference<PublishConnection<T>> current;

        final AtomicReference<Subscription> upstream;

        final AtomicBoolean connect;

        final AtomicReference<InnerSubscription<T>[]> subscribers;

        final int bufferSize;

        volatile SimpleQueue<T> queue;

        int sourceMode;

        volatile boolean done;
        Throwable error;

        int consumed;

        @SuppressWarnings("rawtypes")
        static final InnerSubscription[] EMPTY = new InnerSubscription[0];
        @SuppressWarnings("rawtypes")
        static final InnerSubscription[] TERMINATED = new InnerSubscription[0];

        @SuppressWarnings("unchecked")
        PublishConnection(AtomicReference<PublishConnection<T>> current, int bufferSize) {
            this.current = current;
            this.upstream = new AtomicReference<>();
            this.connect = new AtomicBoolean();
            this.bufferSize = bufferSize;
            this.subscribers = new AtomicReference<>(EMPTY);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispose() {
            subscribers.getAndSet(TERMINATED);
            current.compareAndSet(this, null);
            SubscriptionHelper.cancel(upstream);
        }

        @Override
        public boolean isDisposed() {
            return subscribers.get() == TERMINATED;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.upstream, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);
                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;
                        drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;
                        s.request(bufferSize);
                        return;
                    }
                }

                queue = new SpscArrayQueue<>(bufferSize);

                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == QueueSubscription.NONE && !queue.offer(t)) {
                onError(new QueueOverflowException());
                return;
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                error = t;
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            SimpleQueue<T> queue = this.queue;
            int consumed = this.consumed;
            int limit = this.bufferSize - (this.bufferSize >> 2);
            boolean async = this.sourceMode != QueueSubscription.SYNC;

            outer:
            for (;;) {
                if (queue != null) {
                    long minDemand = Long.MAX_VALUE;
                    boolean hasDemand = false;

                    InnerSubscription<T>[] consumers = subscribers.get();

                    for (InnerSubscription<T> inner : consumers) {
                        long request = inner.get();
                        if (request != Long.MIN_VALUE) {
                            hasDemand = true;
                            minDemand = Math.min(request - inner.emitted, minDemand);
                        }
                    }

                    if (!hasDemand) {
                        minDemand = 0L;
                    }

                    while (minDemand != 0L) {
                        boolean d = done;
                        T v;

                        try {
                            v = queue.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.get().cancel();
                            queue.clear();
                            done = true;
                            signalError(ex);
                            return;
                        }

                        boolean empty = v == null;

                        if (checkTerminated(d, empty)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        for (InnerSubscription<T> inner : consumers) {
                            if (!inner.isCancelled()) {
                                inner.downstream.onNext(v);
                                inner.emitted++;
                            }
                        }

                        if (async && ++consumed == limit) {
                            consumed = 0;
                            upstream.get().request(limit);
                        }
                        minDemand--;

                        if (consumers != subscribers.get()) {
                            continue outer;
                        }
                    }

                    if (checkTerminated(done, queue.isEmpty())) {
                        return;
                    }
                }

                this.consumed = consumed;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (queue == null) {
                    queue = this.queue;
                }
            }
        }

        @SuppressWarnings("unchecked")
        boolean checkTerminated(boolean isDone, boolean isEmpty) {
            if (isDone && isEmpty) {
                Throwable ex = error;

                if (ex != null) {
                    signalError(ex);
                } else {
                    for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                        if (!inner.isCancelled()) {
                            inner.downstream.onComplete();
                        }
                    }
                }
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        void signalError(Throwable ex) {
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                if (!inner.isCancelled()) {
                    inner.downstream.onError(ex);
                }
            }
        }

        boolean add(InnerSubscription<T> inner) {
            for (;;) {
                InnerSubscription<T>[] c = subscribers.get();
                if (c == TERMINATED) {
                    return false;
                }
                int len = c.length;
                @SuppressWarnings("unchecked")
                InnerSubscription<T>[] u = new InnerSubscription[len + 1];
                System.arraycopy(c, 0, u, 0, len);
                u[len] = inner;
                if (subscribers.compareAndSet(c, u)) {
                    return true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void remove(InnerSubscription<T> inner) {
            for (;;) {
                InnerSubscription<T>[] c = subscribers.get();
                int len = c.length;
                if (len == 0) {
                    break;
                }
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (c[i] == inner) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                InnerSubscription<T>[] u;
                if (len == 1) {
                    u = EMPTY;
                } else {
                    u = new InnerSubscription[len - 1];
                    System.arraycopy(c, 0, u, 0, j);
                    System.arraycopy(c, j + 1, u, j, len - j - 1);
                }
                if (subscribers.compareAndSet(c, u)) {
                    break;
                }
            }
        }
    }

    static final class InnerSubscription<T> extends AtomicLong
    implements Subscription {

        private static final long serialVersionUID = 2845000326761540265L;

        final Subscriber<? super T> downstream;

        final PublishConnection<T> parent;

        long emitted;

        InnerSubscription(Subscriber<? super T> downstream, PublishConnection<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(this, n);
                parent.drain();
            }
        }

        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
                parent.drain();
            }
        }

        public boolean isCancelled() {
            return get() == Long.MIN_VALUE;
        }
    }
}
