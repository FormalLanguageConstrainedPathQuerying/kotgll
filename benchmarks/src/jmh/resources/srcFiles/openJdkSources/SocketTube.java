/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.internal.net.http.common.BufferSupplier;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.SequentialScheduler.DeferredCompleter;
import jdk.internal.net.http.common.SequentialScheduler.RestartableTask;
import jdk.internal.net.http.common.Utils;

/**
 * A SocketTube is a terminal tube plugged directly into the socket.
 * The read subscriber should call {@code subscribe} on the SocketTube before
 * the SocketTube is subscribed to the write publisher.
 */
final class SocketTube implements FlowTube {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    static final AtomicLong IDS = new AtomicLong();

    private final HttpClientImpl client;
    private final SocketChannel channel;
    private final SliceBufferSource sliceBuffersSource;
    private final Object lock = new Object();
    private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    private final InternalReadPublisher readPublisher;
    private final InternalWriteSubscriber writeSubscriber;
    private final long id = IDS.incrementAndGet();

    public SocketTube(HttpClientImpl client, SocketChannel channel,
                      Supplier<ByteBuffer> buffersFactory) {
        this.client = client;
        this.channel = channel;
        this.sliceBuffersSource = new SliceBufferSource(buffersFactory);

        this.readPublisher = new InternalReadPublisher();
        this.writeSubscriber = new InternalWriteSubscriber();
    }

    /**
     * Returns {@code true} if this flow is finished.
     * This happens when this flow internal read subscription is completed,
     * either normally (EOF reading) or exceptionally  (EOF writing, or
     * underlying socket closed, or some exception occurred while reading or
     * writing to the socket).
     *
     * @return {@code true} if this flow is finished.
     */
    public boolean isFinished() {
        InternalReadPublisher.InternalReadSubscription subscription =
                readPublisher.subscriptionImpl;
        return subscription != null && subscription.completed
                || subscription == null && errorRef.get() != null;
    }


    /**
     * {@inheritDoc }
     * @apiNote This method should be called first. In particular, the caller
     *          must ensure that this method must be called by the read
     *          subscriber before the write publisher can call {@code onSubscribe}.
     *          Failure to adhere to this contract may result in assertion errors.
     */
    @Override
    public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> s) {
        Objects.requireNonNull(s);
        assert s instanceof TubeSubscriber : "Expected TubeSubscriber, got:" + s;
        readPublisher.subscribe(s);
    }



    /**
     * {@inheritDoc }
     * @apiNote The caller must ensure that {@code subscribe} is called by
     *          the read subscriber before {@code onSubscribe} is called by
     *          the write publisher.
     *          Failure to adhere to this contract may result in assertion errors.
     */
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        writeSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        writeSubscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        writeSubscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        writeSubscriber.onComplete();
    }


    void signalClosed(Throwable cause) {
        if (Log.channel()) {
            Log.logChannel("Connection close signalled: connection closed locally ({0})",
                    channelDescr());
        }
        readPublisher.subscriptionImpl.signalError(
                new IOException("connection closed locally", cause));
    }

    /**
     * A restartable task used to process tasks in sequence.
     */
    private static class SocketFlowTask implements RestartableTask {
        final Runnable task;
        private final Lock lock = new ReentrantLock();
        SocketFlowTask(Runnable task) {
            this.task = task;
        }
        @Override
        public final void run(DeferredCompleter taskCompleter) {
            try {
                boolean locked = lock.tryLock();
                assert locked : "contention detected in SequentialScheduler";
                try {
                    task.run();
                } finally {
                    if (locked) lock.unlock();
                }
            } finally {
                taskCompleter.complete();
            }
        }
    }

    void debugState(String when) {
        if (debug.on()) {
            StringBuilder state = new StringBuilder();

            InternalReadPublisher.InternalReadSubscription sub =
                    readPublisher.subscriptionImpl;
            InternalReadPublisher.ReadEvent readEvent =
                    sub == null ? null : sub.readEvent;
            Demand rdemand = sub == null ? null : sub.demand;
            InternalWriteSubscriber.WriteEvent writeEvent =
                    writeSubscriber.writeEvent;
            Demand wdemand = writeSubscriber.writeDemand;
            int rops = readEvent == null ? 0 : readEvent.interestOps();
            long rd = rdemand == null ? 0 : rdemand.get();
            int wops = writeEvent == null ? 0 : writeEvent.interestOps();
            long wd = wdemand == null ? 0 : wdemand.get();

            state.append(when).append(" Reading: [ops=")
                    .append(Utils.describeOps(rops)).append(", demand=").append(rd)
                    .append(", stopped=")
                    .append((sub == null ? false : sub.readScheduler.isStopped()))
                    .append("], Writing: [ops=").append(Utils.describeOps(wops))
                    .append(", demand=").append(wd)
                    .append("]");
            debug.log(state.toString());
        }
    }

    /**
     * A repeatable event that can be paused or resumed by changing its
     * interestOps. When the event is fired, it is first paused before being
     * signaled. It is the responsibility of the code triggered by
     * {@code signalEvent} to resume the event if required.
     */
    private abstract static class SocketFlowEvent extends AsyncEvent {
        final SocketChannel channel;
        final int defaultInterest;
        volatile int interestOps;
        volatile boolean registered;
        SocketFlowEvent(int defaultInterest, SocketChannel channel) {
            super(AsyncEvent.REPEATING);
            this.defaultInterest = defaultInterest;
            this.channel = channel;
        }
        final boolean registered() {return registered;}
        final void resume() {
            interestOps = defaultInterest;
            registered = true;
        }
        final void pause() {interestOps = 0;}
        @Override
        public final SelectableChannel channel() {return channel;}
        @Override
        public final int interestOps() {return interestOps;}

        @Override
        public final void handle() {
            pause();       
            signalEvent(); 
        }
        @Override
        public final void abort(IOException error) {
            debug().log(() -> this.getClass().getSimpleName() + " abort: " + error);
            pause();              
            signalError(error);   
        }

        protected abstract void signalEvent();
        protected abstract void signalError(Throwable error);
        abstract Logger debug();
    }


    private final class InternalWriteSubscriber
            implements Flow.Subscriber<List<ByteBuffer>> {

        volatile WriteSubscription subscription;
        volatile List<ByteBuffer> current;
        volatile boolean completed;
        final AsyncTriggerEvent startSubscription =
                new AsyncTriggerEvent(this::signalError, this::startSubscription);
        final WriteEvent writeEvent = new WriteEvent(channel, this);
        final Demand writeDemand = new Demand();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            WriteSubscription previous = this.subscription;
            if (debug.on()) debug.log("subscribed for writing");
            try {
                boolean needEvent = current == null;
                if (needEvent) {
                    if (previous != null && previous.upstreamSubscription != subscription) {
                        previous.dropSubscription();
                    }
                }
                this.subscription = new WriteSubscription(subscription);
                if (needEvent) {
                    if (debug.on())
                        debug.log("write: registering startSubscription event");
                    client.registerEvent(startSubscription);
                }
            } catch (Throwable t) {
                signalError(t);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> bufs) {
            assert current == null : dbgString() 
                    + "w.onNext current: " + current;
            assert subscription != null : dbgString()
                    + "w.onNext: subscription is null";
            current = bufs;
            tryFlushCurrent(client.isSelectorThread()); 
            debugState("leaving w.onNext");
        }

        void tryFlushCurrent(boolean inSelectorThread) {
            List<ByteBuffer> bufs = current;
            if (bufs == null) return;
            if (client.isSelectorClosed()) {
                signalError(client.selectorClosedException());
                return;
            }
            try {
                assert inSelectorThread == client.isSelectorThread() :
                       "should " + (inSelectorThread ? "" : "not ")
                        + " be in the selector thread";
                long remaining = Utils.remaining(bufs);
                if (debug.on()) debug.log("trying to write: %d", remaining);
                long written = writeAvailable(bufs);
                if (debug.on()) debug.log("wrote: %d", written);
                assert written >= 0 : "negative number of bytes written:" + written;
                assert written <= remaining;
                if (remaining - written == 0) {
                    current = null;
                    if (writeDemand.tryDecrement()) {
                        if (client.isSelectorClosed()) {
                            signalError(client.selectorClosedException());
                            return;
                        }
                        Runnable requestMore = this::requestMore;
                        if (inSelectorThread) {
                            assert client.isSelectorThread();
                            client.theExecutor().execute(requestMore);
                        } else {
                            assert !client.isSelectorThread();
                            requestMore.run();
                        }
                    }
                } else {
                    resumeWriteEvent(inSelectorThread);
                }
            } catch (Throwable t) {
                signalError(t);
            }
        }

        void startSubscription() {
            try {
                if (debug.on()) debug.log("write: starting subscription");
                if (Log.channel()) {
                    Log.logChannel("Start requesting bytes for writing to channel: {0}",
                            channelDescr());
                }
                assert client.isSelectorThread();
                readPublisher.subscriptionImpl.handlePending();
                if (debug.on()) debug.log("write: offloading requestMore");
                client.theExecutor().execute(this::requestMore);
            } catch(Throwable t) {
                signalError(t);
            }
        }

        void requestMore() {
           WriteSubscription subscription = this.subscription;
           subscription.requestMore();
        }

        @Override
        public void onError(Throwable throwable) {
            signalError(throwable);
        }

        @Override
        public void onComplete() {
            completed = true;
            List<ByteBuffer> bufs = current;
            long remaining = bufs == null ? 0 : Utils.remaining(bufs);
            if (debug.on())
                debug.log( "write completed, %d yet to send", remaining);
            debugState("InternalWriteSubscriber::onComplete");
        }

        void resumeWriteEvent(boolean inSelectorThread) {
            if (debug.on()) debug.log("scheduling write event");
            resumeEvent(writeEvent, this::signalError);
        }

        void signalWritable() {
            if (debug.on()) debug.log("channel is writable");
            tryFlushCurrent(true);
        }

        void signalError(Throwable error) {
            debug.log(() -> "write error: " + error);
            if (Log.channel()) {
                Log.logChannel("Failed to write to channel ({0}: {1})",
                        channelDescr(), error);
            }
            completed = true;
            readPublisher.signalError(error);
            Flow.Subscription subscription = this.subscription;
            if (subscription != null) subscription.cancel();
        }

        final class WriteEvent extends SocketFlowEvent {
            final InternalWriteSubscriber sub;
            WriteEvent(SocketChannel channel, InternalWriteSubscriber sub) {
                super(SelectionKey.OP_WRITE, channel);
                this.sub = sub;
            }
            @Override
            protected final void signalEvent() {
                try {
                    client.eventUpdated(this);
                    sub.signalWritable();
                } catch(Throwable t) {
                    sub.signalError(t);
                }
            }

            @Override
            protected void signalError(Throwable error) {
                sub.signalError(error);
            }

            @Override
            Logger debug() { return debug; }
        }

        final class WriteSubscription implements Flow.Subscription {
            final Flow.Subscription upstreamSubscription;
            volatile boolean cancelled;
            WriteSubscription(Flow.Subscription subscription) {
                this.upstreamSubscription = subscription;
            }

            @Override
            public void request(long n) {
                if (cancelled) return;
                upstreamSubscription.request(n);
            }

            @Override
            public void cancel() {
                if (cancelled) return;
                if (debug.on()) debug.log("write: cancel");
                if (Log.channel()) {
                    Log.logChannel("Cancelling write subscription");
                }
                dropSubscription();
                upstreamSubscription.cancel();
            }

            void dropSubscription() {
                if (debug.on()) debug.log("write: resetting demand to 0");
                synchronized (InternalWriteSubscriber.this) {
                    cancelled = true;
                    writeDemand.reset();
                }
            }

            void requestMore() {
                try {
                    if (completed || cancelled) return;
                    boolean requestMore;
                    long d;
                    synchronized (InternalWriteSubscriber.this) {
                        if (cancelled) return;
                        d = writeDemand.get();
                        requestMore = writeDemand.increaseIfFulfilled();
                    }
                    if (requestMore) {
                        if (debug.on()) debug.log("write: requesting more...");
                        upstreamSubscription.request(1);
                    } else {
                        if (debug.on())
                            debug.log("write: no need to request more: %d", d);
                    }
                } catch (Throwable t) {
                    if (debug.on())
                        debug.log("write: error while requesting more: " + t);
                    signalError(t);
                } finally {
                    debugState("leaving requestMore: ");
                }
            }
        }
    }


    private final class InternalReadPublisher
            implements Flow.Publisher<List<ByteBuffer>> {
        private final InternalReadSubscription subscriptionImpl
                = new InternalReadSubscription();
        AtomicReference<ReadSubscription> pendingSubscription = new AtomicReference<>();
        private volatile ReadSubscription subscription;

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> s) {
            Objects.requireNonNull(s);

            TubeSubscriber sub = FlowTube.asTubeSubscriber(s);
            ReadSubscription target = new ReadSubscription(subscriptionImpl, sub);
            ReadSubscription previous = pendingSubscription.getAndSet(target);

            if (previous != null && previous != target) {
                if (debug.on())
                    debug.log("read publisher: dropping pending subscriber: "
                              + previous.subscriber);
                previous.errorRef.compareAndSet(null, errorRef.get());
                previous.signalOnSubscribe();
                if (subscriptionImpl.completed) {
                    previous.signalCompletion();
                } else {
                    previous.subscriber.dropSubscription();
                }
            }

            if (debug.on()) debug.log("read publisher got subscriber");
            subscriptionImpl.signalSubscribe();
            debugState("leaving read.subscribe: ");
        }

        void signalError(Throwable error) {
            if (debug.on()) debug.log("error signalled " + error);
            if (!errorRef.compareAndSet(null, error)) {
                return;
            }
            if (Log.channel()) {
                Log.logChannel("Error signalled on channel {0}: {1}",
                        channelDescr(), error);
            }
            subscriptionImpl.handleError();
        }

        final class ReadSubscription implements Flow.Subscription {
            final InternalReadSubscription impl;
            final TubeSubscriber  subscriber;
            final AtomicReference<Throwable> errorRef = new AtomicReference<>();
            final BufferSource bufferSource;
            volatile boolean subscribed;
            volatile boolean cancelled;
            volatile boolean completed;

            public ReadSubscription(InternalReadSubscription impl,
                                    TubeSubscriber subscriber) {
                this.impl = impl;
                this.bufferSource = subscriber.supportsRecycling()
                        ? new SSLDirectBufferSource(client)
                        : SocketTube.this.sliceBuffersSource;
                this.subscriber = subscriber;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }

            @Override
            public void request(long n) {
                if (!cancelled) {
                    impl.request(n);
                } else {
                    if (debug.on())
                        debug.log("subscription cancelled, ignoring request %d", n);
                }
            }

            void signalCompletion() {
                assert subscribed || cancelled;
                if (completed || cancelled) return;
                synchronized (this) {
                    if (completed) return;
                    completed = true;
                }
                Throwable error = errorRef.get();
                if (error != null) {
                    if (debug.on())
                        debug.log("forwarding error to subscriber: " + error);
                    subscriber.onError(error);
                } else {
                    if (debug.on()) debug.log("completing subscriber");
                    subscriber.onComplete();
                }
            }

            void signalOnSubscribe() {
                if (subscribed || cancelled) return;
                synchronized (this) {
                    if (subscribed || cancelled) return;
                    subscribed = true;
                }
                subscriber.onSubscribe(this);
                if (debug.on()) debug.log("onSubscribe called");
                if (errorRef.get() != null) {
                    signalCompletion();
                }
            }
        }

        final class InternalReadSubscription implements Flow.Subscription {

            private final Demand demand = new Demand();
            final SequentialScheduler readScheduler;
            private volatile boolean completed;
            private final ReadEvent readEvent;
            private final AsyncEvent subscribeEvent;

            InternalReadSubscription() {
                readScheduler = new SequentialScheduler(new SocketFlowTask(this::read));
                subscribeEvent = new AsyncTriggerEvent(this::signalError,
                                                       this::handleSubscribeEvent);
                readEvent = new ReadEvent(channel, this);
            }

            /*
             * This method must be invoked before any other method of this class.
             */
            final void signalSubscribe() {
                if (readScheduler.isStopped() || completed) {
                    if (debug.on())
                        debug.log("handling pending subscription while completed");
                    handlePending();
                } else {
                    try {
                        if (debug.on()) debug.log("registering subscribe event");
                        client.registerEvent(subscribeEvent);
                    } catch (Throwable t) {
                        signalError(t);
                        handlePending();
                    }
                }
            }

            final void handleSubscribeEvent() {
                assert client.isSelectorThread();
                debug.log("subscribe event raised");
                if (Log.channel()) Log.logChannel("Start reading from {0}", channelDescr());
                readScheduler.runOrSchedule();
                if (readScheduler.isStopped() || completed) {
                    if (debug.on())
                        debug.log("handling pending subscription when completed");
                    handlePending();
                }
            }


            /*
             * Although this method is thread-safe, the Reactive-Streams spec seems
             * to not require it to be as such. It's a responsibility of the
             * subscriber to signal demand in a thread-safe manner.
             *
             * See Reactive Streams specification, rules 2.7 and 3.4.
             */
            @Override
            public final void request(long n) {
                if (n > 0L) {
                    boolean wasFulfilled = demand.increase(n);
                    if (wasFulfilled) {
                        if (debug.on()) debug.log("got some demand for reading");
                        resumeReadEvent();
                    }
                } else {
                    signalError(new IllegalArgumentException("non-positive request"));
                }
                debugState("leaving request("+n+"): ");
            }

            @Override
            public final void cancel() {
                pauseReadEvent();
                if (debug.on()) debug.log("Read subscription cancelled");
                if (Log.channel()) {
                    Log.logChannel("Read subscription cancelled for channel {0}",
                            channelDescr());
                }
                if (debug.on()) debug.log("Stopping read scheduler");
                readScheduler.stop();
            }

            private void resumeReadEvent() {
                if (debug.on()) debug.log("resuming read event");
                resumeEvent(readEvent, this::signalError);
            }

            private void pauseReadEvent() {
                if (debug.on()) debug.log("pausing read event");
                pauseEvent(readEvent, this::signalError);
            }


            final void handleError() {
                assert errorRef.get() != null;
                readScheduler.runOrSchedule();
            }

            final void signalError(Throwable error) {
                if (debug.on()) debug.log("signal read error: " + error);
                if (!errorRef.compareAndSet(null, error)) {
                    return;
                }
                if (debug.on()) debug.log("got read error: " + error);
                if (Log.channel()) {
                    Log.logChannel("Read error signalled on channel {0}: {1}",
                            channelDescr(), error);
                }
                readScheduler.runOrSchedule();
            }

            final void signalReadable() {
                readScheduler.runOrSchedule();
            }

            /** The body of the task that runs in SequentialScheduler. */
            final void read() {
                try {
                    while(!readScheduler.isStopped()) {
                        if (completed) return;

                        if (handlePending()) {
                            if (debug.on())
                                debug.log("pending subscriber subscribed");
                            return;
                        }

                        ReadSubscription current = subscription;
                        Throwable error = errorRef.get();
                        if (current == null)  {
                            assert error != null;
                            if (debug.on())
                                debug.log("error raised before subscriber subscribed: %s",
                                          (Object)error);
                            return;
                        }
                        TubeSubscriber subscriber = current.subscriber;
                        if (error != null) {
                            completed = true;
                            pauseReadEvent();
                            if (debug.on())
                                debug.log("Sending error " + error
                                          + " to subscriber " + subscriber);
                            if (Log.channel()) {
                                Log.logChannel("Raising error with subscriber for {0}: {1}",
                                        channelDescr(), error);
                            }
                            current.errorRef.compareAndSet(null, error);
                            current.signalCompletion();
                            if (debug.on()) debug.log("Stopping read scheduler");
                            readScheduler.stop();
                            debugState("leaving read() loop with error: ");
                            return;
                        }

                        assert client.isSelectorThread();
                        if (demand.tryDecrement()) {
                            try {
                                List<ByteBuffer> bytes = readAvailable(current.bufferSource);
                                if (bytes == EOF) {
                                    if (!completed) {
                                        if (debug.on()) debug.log("got read EOF");
                                        if (Log.channel()) {
                                            Log.logChannel("EOF read from channel: {0}",
                                                        channelDescr());
                                        }
                                        completed = true;
                                        pauseReadEvent();
                                        current.signalCompletion();
                                        if (debug.on()) debug.log("Stopping read scheduler");
                                        readScheduler.stop();
                                    }
                                    debugState("leaving read() loop after EOF: ");
                                    return;
                                } else if (Utils.remaining(bytes) > 0) {
                                    if (debug.on())
                                        debug.log("read bytes: " + Utils.remaining(bytes));
                                    assert !current.completed;
                                    subscriber.onNext(bytes);
                                    resumeReadEvent();
                                    if (errorRef.get() != null) continue;
                                    debugState("leaving read() loop after onNext: ");
                                    return;
                                } else {
                                    if (debug.on()) debug.log("no more bytes available");
                                    demand.increase(1);
                                    resumeReadEvent();
                                    if (errorRef.get() != null) continue;
                                    debugState("leaving read() loop with no bytes");
                                    return;
                                }
                            } catch (Throwable x) {
                                signalError(x);
                                continue;
                            }
                        } else {
                            if (debug.on()) debug.log("no more demand for reading");
                            if (errorRef.get() != null) continue;
                            debugState("leaving read() loop with no demand");
                            break;
                        }
                    }
                } catch (Throwable t) {
                    if (debug.on()) debug.log("Unexpected exception in read loop", t);
                    signalError(t);
                } finally {
                    if (readScheduler.isStopped()) {
                        if (debug.on()) debug.log("Read scheduler stopped");
                        if (Log.channel()) {
                            Log.logChannel("Stopped reading from channel {0}", channelDescr());
                        }
                    }
                    handlePending();
                }
            }

            boolean handlePending() {
                ReadSubscription pending = pendingSubscription.getAndSet(null);
                if (pending == null) return false;
                if (debug.on())
                    debug.log("handling pending subscription for %s",
                            pending.subscriber);
                ReadSubscription current = subscription;
                if (current != null && current != pending && !completed) {
                    current.subscriber.dropSubscription();
                }
                if (debug.on()) debug.log("read demand reset to 0");
                subscriptionImpl.demand.reset(); 
                pending.errorRef.compareAndSet(null, errorRef.get());
                if (!readScheduler.isStopped()) {
                    subscription = pending;
                } else {
                    if (debug.on()) debug.log("socket tube is already stopped");
                }
                if (debug.on()) debug.log("calling onSubscribe");
                pending.signalOnSubscribe();
                if (completed) {
                    pending.errorRef.compareAndSet(null, errorRef.get());
                    pending.signalCompletion();
                }
                return true;
            }
        }


        final class ReadEvent extends SocketFlowEvent {
            final InternalReadSubscription sub;
            ReadEvent(SocketChannel channel, InternalReadSubscription sub) {
                super(SelectionKey.OP_READ, channel);
                this.sub = sub;
            }
            @Override
            protected final void signalEvent() {
                try {
                    client.eventUpdated(this);
                    sub.signalReadable();
                } catch(Throwable t) {
                    sub.signalError(t);
                }
            }

            @Override
            protected final void signalError(Throwable error) {
                if (debug.on()) debug.log("signalError to %s (%s)", sub, error);
                sub.signalError(error);
            }

            @Override
            Logger debug() { return debug; }
        }
    }


    public interface BufferSource {
        /**
         * Returns a buffer to read data from the socket.
         *
         * @implNote
         * Different implementation can have different strategies, as to
         * which kind of buffer to return, or whether to return the same
         * buffer. The only constraints are that:
         *   a. the buffer returned must not be null
         *   b. the buffer position indicates where to start reading
         *   c. the buffer limit indicates where to stop reading.
         *   d. the buffer is 'free' - that is - it is not used
         *      or retained by anybody else
         *
         * @return A buffer to read data from the socket.
         */
        ByteBuffer getBuffer();

        /**
         * Appends the read-data in {@code buffer} to the list of buffer to
         * be sent downstream to the subscriber. May return a new
         * list, or append to the given list.
         *
         * @implNote
         * Different implementation can have different strategies, but
         * must obviously be consistent with the implementation of the
         * getBuffer() method. For instance, an implementation could
         * decide to add the buffer to the list and return a new buffer
         * next time getBuffer() is called, or could decide to add a buffer
         * slice to the list and return the same buffer (if remaining
         * space is available) next time getBuffer() is called.
         *
         * @param list    The list before adding the data. Can be null.
         * @param buffer  The buffer containing the data to add to the list.
         * @param start   The start position at which data were read.
         *                The current buffer position indicates the end.
         * @return A possibly new list where a buffer containing the
         *         data read from the socket has been added.
         */
        List<ByteBuffer> append(List<ByteBuffer> list, ByteBuffer buffer, int start);

        /**
         * Returns the given unused {@code buffer}, previously obtained from
         * {@code getBuffer}.
         *
         * @implNote This method can be used, if necessary, to return
         *  the unused buffer to the pull.
         *
         * @param buffer The unused buffer.
         */
        default void returnUnused(ByteBuffer buffer) { }
    }

    private static final class SliceBufferSource implements BufferSource {
        private final Supplier<ByteBuffer> factory;
        private volatile ByteBuffer current;

        public SliceBufferSource() {
            this(Utils::getBuffer);
        }
        public SliceBufferSource(Supplier<ByteBuffer> factory) {
            this.factory = Objects.requireNonNull(factory);
        }

        @Override
        public final ByteBuffer getBuffer() {
            ByteBuffer buf = current;
            buf = (buf == null || !buf.hasRemaining())
                    ? (current = factory.get()) : buf;
            assert buf.hasRemaining();
            return buf;
        }

        @Override
        public final List<ByteBuffer> append(List <ByteBuffer> list, ByteBuffer buf, int start) {
            int limit = buf.limit();
            buf.limit(buf.position());
            buf.position(start);
            ByteBuffer slice = buf.slice();

            buf.position(buf.limit());
            buf.limit(limit);

            return SocketTube.listOf(list, slice.asReadOnlyBuffer());
        }
    }


    private static final class SSLDirectBufferSource implements BufferSource {
        private final BufferSupplier factory;
        private final HttpClientImpl client;
        private ByteBuffer current;

        public SSLDirectBufferSource(HttpClientImpl client) {
            this.client = Objects.requireNonNull(client);
            this.factory = Objects.requireNonNull(client.getSSLBufferSupplier());
        }

        @Override
        public final ByteBuffer getBuffer() {
            assert client.isSelectorThread();
            ByteBuffer buf = current;
            if (buf == null) {
                buf = current = factory.get();
            }
            assert buf.hasRemaining();
            assert buf.position() == 0;
            return buf;
        }

        @Override
        public final List<ByteBuffer> append(List <ByteBuffer> list, ByteBuffer buf, int start) {
            assert client.isSelectorThread();
            assert buf.isDirect();
            assert start == 0;
            assert current == buf;
            current = null;
            buf.limit(buf.position());
            buf.position(start);
            return SocketTube.listOf(list, buf);
        }

        @Override
        public void returnUnused(ByteBuffer buffer) {
            assert buffer == current;
            ByteBuffer buf = current;
            if (buf != null) {
                assert buf.position() == 0;
                current = null;
                buf.limit(buf.position());
                factory.recycle(buf);
            }
        }
    }

    static final int MAX_BUFFERS = 3;
    static final List<ByteBuffer> EOF = List.of();
    static final List<ByteBuffer> NOTHING = List.of(Utils.EMPTY_BYTEBUFFER);

    private List<ByteBuffer> readAvailable(BufferSource buffersSource) throws IOException {
        ByteBuffer buf = buffersSource.getBuffer();
        assert buf.hasRemaining();

        int read;
        int pos = buf.position();
        List<ByteBuffer> list = null;
        while (buf.hasRemaining()) {
            try {
                while ((read = channel.read(buf)) > 0) {
                    if (!buf.hasRemaining())
                        break;
                }
            } catch (IOException x) {
                if (buf.position() == pos && list == null) {
                    buffersSource.returnUnused(buf);
                    throw x;
                } else {
                    errorRef.compareAndSet(null, x);
                    read = 0; 
                }
            }

            if (buf.position() == pos) {
                buffersSource.returnUnused(buf);
                if (list == null) {
                    list = read == -1 ? EOF : NOTHING;
                }
                break;
            }

            list = buffersSource.append(list, buf, pos);

            if (read <= 0 || list.size() == MAX_BUFFERS) {
                break;
            }

            buf = buffersSource.getBuffer();
            pos = buf.position();
            assert buf.hasRemaining();
        }
        return list;
    }

    private static <T> List<T> listOf(List<T> list, T item) {
        int size = list == null ? 0 : list.size();
        switch (size) {
            case 0: return List.of(item);
            case 1: return List.of(list.get(0), item);
            case 2: return List.of(list.get(0), list.get(1), item);
            default: 
                List<T> res = list instanceof ArrayList ? list : new ArrayList<>(list);
                res.add(item);
                return res;
        }
    }

    private long writeAvailable(List<ByteBuffer> bytes) throws IOException {
        ByteBuffer[] srcs = bytes.toArray(Utils.EMPTY_BB_ARRAY);
        final long remaining = Utils.remaining(srcs);
        long written = 0;
        while (remaining > written) {
            try {
                long w = channel.write(srcs);
                assert w >= 0 : "negative number of bytes written:" + w;
                if (w == 0) {
                    break;
                }
                written += w;
            } catch (IOException x) {
                if (written == 0) {
                    throw x;
                } else {
                    break;
                }
            }
        }
        return written;
    }

    private void resumeEvent(SocketFlowEvent event,
                             Consumer<Throwable> errorSignaler) {
        boolean registrationRequired;
        synchronized (lock) {
            registrationRequired = !event.registered();
            event.resume();
        }
        try {
            if (registrationRequired) {
                client.registerEvent(event);
             } else {
                client.eventUpdated(event);
            }
        } catch(Throwable t) {
            errorSignaler.accept(t);
        }
   }

    private void pauseEvent(SocketFlowEvent event,
                            Consumer<Throwable> errorSignaler) {
        synchronized (lock) {
            event.pause();
        }
        try {
            client.eventUpdated(event);
        } catch(Throwable t) {
            errorSignaler.accept(t);
        }
    }

    @Override
    public void connectFlows(TubePublisher writePublisher,
                             TubeSubscriber readSubscriber) {
        if (debug.on()) debug.log("connecting flows");
        this.subscribe(readSubscriber);
        writePublisher.subscribe(this);
    }


    @Override
    public String toString() {
        return dbgString();
    }

    final String dbgString() {
        return "SocketTube("+id+")";
    }

    final String channelDescr() {
        return String.valueOf(channel);
    }
}
