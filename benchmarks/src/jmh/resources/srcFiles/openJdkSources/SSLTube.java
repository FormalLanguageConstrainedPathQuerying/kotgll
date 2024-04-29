/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import jdk.internal.net.http.common.SubscriberWrapper.SchedulingAction;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;

/**
 * An implementation of FlowTube that wraps another FlowTube in an
 * SSL flow.
 * <p>
 * The following diagram shows a typical usage of the SSLTube, where
 * the SSLTube wraps a SocketTube on the right hand side, and is connected
 * to an HttpConnection on the left hand side.
 *
 * <preformatted>{@code
 *                  +----------  SSLTube -------------------------+
 *                  |                                             |
 *                  |                    +---SSLFlowDelegate---+  |
 *  HttpConnection  |                    |                     |  |   SocketTube
 *    read sink  <- SSLSubscriberW.   <- Reader <- upstreamR.() <---- read source
 *  (a subscriber)  |                    |    \         /      |  |  (a publisher)
 *                  |                    |     SSLEngine       |  |
 *  HttpConnection  |                    |    /         \      |  |   SocketTube
 *  write source -> SSLSubscriptionW. -> upstreamW.() -> Writer ----> write sink
 *  (a publisher)   |                    |                     |  |  (a subscriber)
 *                  |                    +---------------------+  |
 *                  |                                             |
 *                  +---------------------------------------------+
 * }</preformatted>
 */
public class SSLTube implements FlowTube {

    final Logger debug =
            Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    private final FlowTube tube;
    private final SSLSubscriberWrapper readSubscriber;
    private final SSLSubscriptionWrapper writeSubscription;
    private final SSLFlowDelegate sslDelegate;
    private final SSLEngine engine;
    private volatile boolean finished;

    public SSLTube(SSLEngine engine, Executor executor, FlowTube tube) {
        this(engine, executor, null, tube);
    }

    public SSLTube(SSLEngine engine,
                   Executor executor,
                   Consumer<ByteBuffer> recycler,
                   FlowTube tube) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(executor);
        this.tube = Objects.requireNonNull(tube);
        writeSubscription = new SSLSubscriptionWrapper();
        readSubscriber = new SSLSubscriberWrapper();
        this.engine = engine;
        sslDelegate = new SSLTubeFlowDelegate(engine,
                                              executor,
                                              recycler,
                                              readSubscriber,
                                              tube);
    }

    final class SSLTubeFlowDelegate extends SSLFlowDelegate {
        SSLTubeFlowDelegate(SSLEngine engine, Executor executor,
                            Consumer<ByteBuffer> recycler,
                            SSLSubscriberWrapper readSubscriber,
                            FlowTube tube) {
            super(engine, executor, recycler, readSubscriber, tube);
        }
        protected SchedulingAction enterReadScheduling() {
            readSubscriber.processPendingSubscriber();
            return SchedulingAction.CONTINUE;
        }
        void connect(Flow.Subscriber<? super List<ByteBuffer>> downReader,
                     Flow.Subscriber<? super List<ByteBuffer>> downWriter) {
            assert downWriter == tube;
            assert downReader == readSubscriber;

            reader.subscribe(downReader);

            tube.connectFlows(FlowTube.asTubePublisher(writer),
                              FlowTube.asTubeSubscriber(upstreamReader()));

            upstreamWriter().onSubscribe(writeSubscription);
        }

        @Override
        protected Throwable checkForHandshake(Throwable t) {
            return SSLTube.this.checkForHandshake(t);
        }
    }

    public CompletableFuture<String> getALPN() {
        return sslDelegate.alpn();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> s) {
        readSubscriber.dropSubscription();
        readSubscriber.setDelegate(s);
        s.onSubscribe(readSubscription);
    }

    /**
     * Tells whether, or not, this FlowTube has finished receiving data.
     *
     * @return true when one of this FlowTube Subscriber's OnError or onComplete
     * methods have been invoked
     */
    @Override
    public boolean isFinished() {
        return finished;
    }

    private volatile Flow.Subscription readSubscription;

    static final class DelegateWrapper implements FlowTube.TubeSubscriber {
        private final FlowTube.TubeSubscriber delegate;
        private final Logger debug;
        volatile boolean subscribedCalled;
        volatile boolean subscribedDone;
        volatile boolean completed;
        volatile Throwable error;
        DelegateWrapper(Flow.Subscriber<? super List<ByteBuffer>> delegate,
                        Logger debug) {
            this.delegate = FlowTube.asTubeSubscriber(delegate);
            this.debug = debug;
        }

        @Override
        public void dropSubscription() {
            if (subscribedCalled && !completed) {
                delegate.dropSubscription();
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            assert subscribedCalled;
            delegate.onNext(item);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            onSubscribe(delegate::onSubscribe, subscription);
        }

        private void onSubscribe(Consumer<Flow.Subscription> method,
                                 Flow.Subscription subscription) {
            subscribedCalled = true;
            method.accept(subscription);
            Throwable x;
            boolean finished;
            synchronized (this) {
                subscribedDone = true;
                x = error;
                finished = completed;
            }
            if (x != null) {
                if (debug.on())
                    debug.log("Subscriber completed before subscribe: forwarding %s",
                              (Object)x);
                delegate.onError(x);
            } else if (finished) {
                if (debug.on())
                    debug.log("Subscriber completed before subscribe: calling onComplete()");
                delegate.onComplete();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (completed) {
                if (debug.on())
                    debug.log("Subscriber already completed: ignoring %s",
                              (Object)t);
                return;
            }
            boolean subscribed;
            synchronized (this) {
                if (completed) return;
                error = t;
                completed = true;
                subscribed = subscribedDone;
            }
            if (subscribed) {
                delegate.onError(t);
            } else {
                if (debug.on())
                    debug.log("Subscriber not yet subscribed: stored %s",
                              (Object)t);
            }
        }

        @Override
        public void onComplete() {
            if (completed) return;
            boolean subscribed;
            synchronized (this) {
                if (completed) return;
                completed = true;
                subscribed = subscribedDone;
            }
            if (subscribed) {
                if (debug.on()) debug.log("DelegateWrapper: completing subscriber");
                delegate.onComplete();
            } else {
                if (debug.on())
                    debug.log("Subscriber not yet subscribed: stored completed=true");
            }
        }

        @Override
        public String toString() {
            return "DelegateWrapper[subscribedCalled: " + subscribedCalled
                    +", subscribedDone: " + subscribedDone
                    +", completed: " + completed
                    +", error: " + error
                    +"]: " + delegate;
        }

    }

    final class SSLSubscriberWrapper implements FlowTube.TubeSubscriber {
        private AtomicReference<DelegateWrapper> pendingDelegate =
                new AtomicReference<>();
        private volatile DelegateWrapper subscribed;
        private volatile boolean onCompleteReceived;
        private final AtomicReference<Throwable> errorRef
                = new AtomicReference<>();

        @Override
        public String toString() {
            DelegateWrapper sub = subscribed;
            DelegateWrapper pend = pendingDelegate.get();
            SSLFlowDelegate sslFD = sslDelegate;
            return "SSLSubscriberWrapper[" + SSLTube.this
                    + ", delegate: " + (sub == null ? pend  :sub)
                    + ", getALPN: " + (sslFD == null ? null : sslFD.alpn())
                    + ", onCompleteReceived: " + onCompleteReceived
                    + ", onError: " + errorRef.get() + "]";
        }

        void setDelegate(Flow.Subscriber<? super List<ByteBuffer>> delegate) {
            if (debug.on())
                debug.log("SSLSubscriberWrapper (reader) got delegate: %s",
                          delegate);
            assert delegate != null;
            DelegateWrapper delegateWrapper = new DelegateWrapper(delegate, debug);
            DelegateWrapper previous;
            Flow.Subscription subscription;
            boolean handleNow;
            synchronized (this) {
                previous = pendingDelegate.getAndSet(delegateWrapper);
                subscription = readSubscription;
                handleNow = this.errorRef.get() != null || onCompleteReceived;
            }
            if (previous != null) {
                previous.dropSubscription();
            }
            if (subscription == null) {
                if (debug.on())
                    debug.log("SSLSubscriberWrapper (reader) no subscription yet");
                return;
            }
            if (handleNow || !sslDelegate.resumeReader()) {
                processPendingSubscriber();
            }
        }

        void processPendingSubscriber() {
            Flow.Subscription subscription;
            DelegateWrapper delegateWrapper, previous;
            synchronized (this) {
                delegateWrapper = pendingDelegate.get();
                if (delegateWrapper == null) return;
                subscription = readSubscription;
                previous = subscribed;
            }
            if (subscription == null) {
                if (debug.on())
                    debug.log("SSLSubscriberWrapper (reader) " +
                              "processPendingSubscriber: no subscription yet");
                return;
            }
            delegateWrapper = pendingDelegate.getAndSet(null);
            if (delegateWrapper == null) return;
            if (previous != null) {
                previous.dropSubscription();
            }
            onNewSubscription(delegateWrapper, subscription);
        }

        @Override
        public void dropSubscription() {
            DelegateWrapper subscriberImpl = subscribed;
            if (subscriberImpl != null) {
                subscriberImpl.dropSubscription();
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (debug.on())
                debug.log("SSLSubscriberWrapper (reader) onSubscribe(%s)",
                          subscription);
            onSubscribeImpl(subscription);
        }

        private void onSubscribeImpl(Flow.Subscription subscription) {
            assert subscription != null;
            DelegateWrapper subscriberImpl, pending;
            synchronized (this) {
                readSubscription = subscription;
                subscriberImpl = subscribed;
                pending = pendingDelegate.get();
            }

            if (subscriberImpl == null && pending == null) {
                if (debug.on())
                    debug.log("SSLSubscriberWrapper (reader) onSubscribeImpl: "
                              + "no delegate yet");
                return;
            }

            if (pending == null) {
                if (debug.on())
                    debug.log("SSLSubscriberWrapper (reader) onSubscribeImpl: "
                              + "resubscribing");
                onNewSubscription(subscriberImpl, subscription);
            } else {
                if (debug.on())
                    debug.log("SSLSubscriberWrapper (reader) onSubscribeImpl: "
                              + "subscribing pending");
                processPendingSubscriber();
            }
        }

        private void complete(DelegateWrapper subscriberImpl, Throwable t) {
            try {
                if (t == null) subscriberImpl.onComplete();
                else subscriberImpl.onError(t = checkForHandshake(t));
                if (debug.on()) {
                    debug.log("subscriber completed %s",
                            ((t == null) ? "normally" : ("with error: " + t)));
                }
            } finally {
                writeSubscription.cancel();
            }
        }

        private void onNewSubscription(DelegateWrapper subscriberImpl,
                                       Flow.Subscription subscription) {
            assert subscriberImpl != null;
            assert subscription != null;

            Throwable failed;
            boolean completed;
            sslDelegate.resetReaderDemand();
            subscriberImpl.onSubscribe(subscription);

            synchronized (this) {
                failed = this.errorRef.get();
                completed = onCompleteReceived;
                subscribed = subscriberImpl;
            }

            if (failed != null) {
                if (debug.on())
                    debug.log("onNewSubscription: subscriberImpl:%s, invoking onError:%s",
                              subscriberImpl, failed);
                complete(subscriberImpl, failed);
            } else if (completed) {
                if (debug.on())
                    debug.log("onNewSubscription: subscriberImpl:%s, invoking onCompleted",
                              subscriberImpl);
                finished = true;
                complete(subscriberImpl, null);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            subscribed.onNext(item);
        }

        public void onErrorImpl(Throwable throwable) {
            throwable = checkForHandshake(throwable);
            errorRef.compareAndSet(null, throwable);
            Throwable failed = errorRef.get();
            finished = true;
            if (debug.on())
                debug.log("%s: onErrorImpl: %s", this, throwable);
            DelegateWrapper subscriberImpl;
            synchronized (this) {
                subscriberImpl = subscribed;
            }
            if (subscriberImpl != null) {
                complete(subscriberImpl, failed);
            } else {
                if (debug.on())
                    debug.log("%s: delegate null, stored %s", this, failed);
            }
            processPendingSubscriber();
        }

        @Override
        public void onError(Throwable throwable) {
            assert !finished && !onCompleteReceived;
            onErrorImpl(throwable);
        }

        @Override
        public void onComplete() {
            assert !finished && !onCompleteReceived;
            DelegateWrapper subscriberImpl;
            synchronized (this) {
                subscriberImpl = subscribed;
            }

            Throwable handshakeFailed = checkForHandshake(null);
            if (handshakeFailed != null) {
                onErrorImpl(handshakeFailed);
            } else if (subscriberImpl != null) {
                onCompleteReceived = finished = true;
                complete(subscriberImpl, null);
            } else {
                onCompleteReceived = true;
            }
            processPendingSubscriber();
        }
    }

    private boolean handshaking() {
        HandshakeStatus hs = engine.getHandshakeStatus();
        return !(hs == NOT_HANDSHAKING || hs == FINISHED);
    }

    private String handshakeFailed() {
        if (handshaking()
                && (sslDelegate == null
                || !sslDelegate.closeNotifyReceived())) {
            return "Remote host terminated the handshake";
        }
        if ("SSL_NULL_WITH_NULL_NULL".equals(engine.getSession().getCipherSuite()))
            return "Remote host closed the channel";
        return null;
    }

    /**
     * If the stream is completed before the handshake is finished, we want
     * to forward an SSLHandshakeException downstream.
     * If t is not null an exception will always be returned. If t is null an
     * exception will be returned if the engine is handshaking.
     * @param t an exception from upstream, or null.
     * @return t or an SSLHandshakeException wrapping t, or null.
     */
    Throwable checkForHandshake(Throwable t) {
        if (t instanceof SSLException) {
            return t;
        }
        String handshakeFailed = handshakeFailed();
        if (handshakeFailed == null) return t;
        if (debug.on())
            debug.log("handshake: %s, inbound done: %s, outbound done: %s: %s",
                    engine.getHandshakeStatus(),
                    engine.isInboundDone(),
                    engine.isOutboundDone(),
                    handshakeFailed);

        return new SSLHandshakeException(handshakeFailed, t);
    }

    @Override
    public void connectFlows(TubePublisher writePub,
                             TubeSubscriber readSub) {
        if (debug.on()) debug.log("connecting flows");
        readSubscriber.setDelegate(readSub);
        writePub.subscribe(this);
    }

    /** Outstanding write demand from the SSL Flow Delegate. */
    private final Demand writeDemand = new Demand();

    final class SSLSubscriptionWrapper implements Flow.Subscription {

        volatile Flow.Subscription delegate;
        private volatile boolean cancelled;

        void setSubscription(Flow.Subscription sub) {
            long demand = writeDemand.get(); 
            delegate = sub;
            if (debug.on())
                debug.log("setSubscription: demand=%d, cancelled:%s", demand, cancelled);

            if (cancelled)
                delegate.cancel();
            else if (demand > 0)
                sub.request(demand);
        }

        @Override
        public void request(long n) {
            writeDemand.increase(n);
            if (debug.on()) debug.log("request: n=%d", n);
            Flow.Subscription sub = delegate;
            if (sub != null && n > 0) {
                sub.request(n);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (delegate != null)
                delegate.cancel();
        }
    }

    /* Subscriber - writing side */
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        Objects.requireNonNull(subscription);
        Flow.Subscription x = writeSubscription.delegate;
        if (x != null)
            x.cancel();

        writeSubscription.setSubscription(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        Objects.requireNonNull(item);
        boolean decremented = writeDemand.tryDecrement();
        assert decremented : "Unexpected writeDemand: ";
        if (debug.on())
            debug.log("sending %d  buffers to SSL flow delegate", item.size());
        sslDelegate.upstreamWriter().onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        Objects.requireNonNull(throwable);
        sslDelegate.upstreamWriter().onError(throwable);
    }

    @Override
    public void onComplete() {
        sslDelegate.upstreamWriter().onComplete();
    }

    @Override
    public String toString() {
        return dbgString();
    }

    final String dbgString() {
        return "SSLTube(" + tube + ")";
    }

}
