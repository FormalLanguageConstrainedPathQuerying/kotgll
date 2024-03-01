/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.ProxySelector;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.WebSocket;

import jdk.internal.net.http.common.BufferSupplier;
import jdk.internal.net.http.common.Deadline;
import jdk.internal.net.http.common.HttpBodySubscriberWrapper;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Pair;
import jdk.internal.net.http.common.TimeSource;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.common.OperationTrackers.Trackable;
import jdk.internal.net.http.common.OperationTrackers.Tracker;
import jdk.internal.net.http.websocket.BuilderImpl;
import jdk.internal.misc.InnocuousThread;

/**
 * Client implementation. Contains all configuration information and also
 * the selector manager thread which allows async events to be registered
 * and delivered when they occur. See AsyncEvent.
 */
final class HttpClientImpl extends HttpClient implements Trackable {

    static final boolean DEBUGELAPSED = Utils.TESTING || Utils.DEBUG;  
    static final boolean DEBUGTIMEOUT = false; 
    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    final Logger debugelapsed = Utils.getDebugLogger(this::dbgString, DEBUGELAPSED);
    final Logger debugtimeout = Utils.getDebugLogger(this::dbgString, DEBUGTIMEOUT);
    static final AtomicLong CLIENT_IDS = new AtomicLong();
    private final AtomicLong CONNECTION_IDS = new AtomicLong();
    static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 30;
    static final long KEEP_ALIVE_TIMEOUT = getTimeoutProp("jdk.httpclient.keepalive.timeout", DEFAULT_KEEP_ALIVE_TIMEOUT);
    static final long IDLE_CONNECTION_TIMEOUT = getTimeoutProp("jdk.httpclient.keepalive.timeout.h2", KEEP_ALIVE_TIMEOUT);

    private static final class DefaultThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger nextId = new AtomicInteger();

        DefaultThreadFactory(long clientID) {
            namePrefix = "HttpClient-" + clientID + "-Worker-";
        }

        @SuppressWarnings("removal")
        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + nextId.getAndIncrement();
            Thread t;
            if (System.getSecurityManager() == null) {
                t = new Thread(null, r, name, 0, false);
            } else {
                t = InnocuousThread.newThread(name, r);
            }
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * A DelegatingExecutor is an executor that delegates tasks to
     * a wrapped executor when it detects that the current thread
     * is the SelectorManager thread. If the current thread is not
     * the selector manager thread the given task is executed inline.
     */
    static final class DelegatingExecutor implements Executor {
        private final BooleanSupplier isInSelectorThread;
        private final Executor delegate;
        private final BiConsumer<Runnable, Throwable> errorHandler;
        DelegatingExecutor(BooleanSupplier isInSelectorThread,
                           Executor delegate,
                           BiConsumer<Runnable, Throwable> errorHandler) {
            this.isInSelectorThread = isInSelectorThread;
            this.delegate = delegate;
            this.errorHandler = errorHandler;
        }

        Executor delegate() {
            return delegate;
        }



        @Override
        public void execute(Runnable command) {
            if (isInSelectorThread.getAsBoolean()) {
                ensureExecutedAsync(command);
            } else {
                command.run();
            }
        }

        public void ensureExecutedAsync(Runnable command) {
            try {
                delegate.execute(command);
            } catch (Throwable t) {
                errorHandler.accept(command, t);
                ASYNC_POOL.execute(command);
            }
        }

        @SuppressWarnings("removal")
        private void shutdown() {
            if (delegate instanceof ExecutorService service) {
                PrivilegedAction<?> action = () -> {
                    service.shutdown();
                    return null;
                };
                AccessController.doPrivileged(action, null,
                        new RuntimePermission("modifyThread"));
            }
        }
    }

    private final Set<PendingRequest> pendingRequests;
    private final AtomicLong pendingRequestId = new AtomicLong();
    private static final class PendingRequest implements Comparable<PendingRequest> {
        final long id;
        final HttpRequest request;
        final CompletableFuture<?> cf;
        final HttpClientImpl client;
        final MultiExchange<?> mex;
        Object ref;
        private PendingRequest(long id,
                               HttpRequest request,
                               CompletableFuture<?> cf,
                               MultiExchange<?> mex,
                               HttpClientImpl client) {
            this.id = id;
            this.request = request;
            this.cf = cf;
            this.mex = mex;
            this.client = client;
        }

        public void abort(Throwable t) {
            try {
                if (client.isSelectorThread()) {
                    var done = cf.exceptionally((e) -> null);
                    ASYNC_POOL.execute(() -> completeExceptionally(t));
                    done.join();
                } else {
                    cf.completeExceptionally(t);
                }
            } finally {
                mex.cancel(Utils.getIOException(t));
            }
        }

        private void completeExceptionally(Throwable t) {
            if (client.debug.on()) {
                client.debug.log("aborting %s with %s", this, t);
            }
            try { cf.completeExceptionally(t); } catch (Throwable e) {
                client.debug.log("Failed to complete cf for [%s]: %s", this, e);
            }
        }


        @Override
        public int compareTo(PendingRequest o) {
            if (o == null) return 1;
            return Long.compare(id, o.id);
        }

        public String toString() {
            return id + ": " + request.toString();
        }
    }

    static <T> CompletableFuture<T> registerPending(PendingRequest pending, CompletableFuture<T> res) {
        if (pending.cf.isDone()) return res;

        var client = pending.client;
        var cf = pending.cf;
        var id = pending.id;
        boolean added = client.pendingRequests.add(pending);
        var ref = res.whenComplete((r,t) -> client.pendingRequests.remove(pending));
        pending.ref = ref;
        assert added : "request %d was already added".formatted(id);
        if (client.selmgr.isClosed()) {
            pending.abort(client.selmgr.selectorClosedException());
        }
        return ref;
    }

    static void abortPendingRequests(HttpClientImpl client, Throwable reason) {
        reason = Utils.getCompletionCause(reason);
        if (client.debug.on()) {
            var msg = reason instanceof RejectedExecutionException
                    ? reason.getClass() : reason;
            client.debug.log("aborting pending requests due to: %s", msg);
        }
        closeSubscribers(client, reason);
        var pendingRequests = client.pendingRequests;
        while (!pendingRequests.isEmpty()) {
            var pendings = pendingRequests.iterator();
            while (pendings.hasNext()) {
                var pending = pendings.next();
                try {
                    pending.abort(reason);
                } finally {
                    pendings.remove();
                }
            }
        }
    }

    private final CookieHandler cookieHandler;
    private final Duration connectTimeout;
    private final Redirect followRedirects;
    private final ProxySelector userProxySelector;
    private final ProxySelector proxySelector;
    private final Authenticator authenticator;
    private final Version version;
    private final ConnectionPool connections;
    private final DelegatingExecutor delegatingExecutor;
    private final boolean isDefaultExecutor;
    private final SSLContext sslContext;
    private final SSLParameters sslParams;
    private final SelectorManager selmgr;
    private final FilterFactory filters;
    private final Http2ClientImpl client2;
    private final long id;
    private final String dbgTag;
    private final InetAddress localAddr;

    private final SSLDirectBufferSupplier sslBufferSupplier
            = new SSLDirectBufferSupplier(this);

    private final WeakReference<HttpClientFacade> facadeRef;
    private final WeakReference<HttpClientImpl> implRef;

    private final ConcurrentSkipListSet<PlainHttpConnection> openedConnections
            = new ConcurrentSkipListSet<>(HttpConnection.COMPARE_BY_ID);
    private final ConcurrentSkipListSet<HttpBodySubscriberWrapper<?>> subscribers
            = new ConcurrentSkipListSet<>(HttpBodySubscriberWrapper.COMPARE_BY_ID);

    private final AtomicLong pendingOperationCount = new AtomicLong();
    private final AtomicLong pendingWebSocketCount = new AtomicLong();
    private final AtomicLong pendingHttpOperationsCount = new AtomicLong();
    private final AtomicLong pendingHttpRequestCount = new AtomicLong();
    private final AtomicLong pendingHttp2StreamCount = new AtomicLong();
    private final AtomicLong pendingTCPConnectionCount = new AtomicLong();
    private final AtomicLong pendingSubscribersCount = new AtomicLong();
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private final AtomicBoolean isStarted = new AtomicBoolean();
    private volatile boolean shutdownRequested;

    /** A Set of, deadline first, ordered timeout events. */
    private final TreeSet<TimeoutEvent> timeouts;

    /**
     * This is a bit tricky:
     * 1. an HttpClientFacade has a final HttpClientImpl field.
     * 2. an HttpClientImpl has a final WeakReference<HttpClientFacade> field,
     *    where the referent is the facade created for that instance.
     * 3. We cannot just create the HttpClientFacade in the HttpClientImpl
     *    constructor, because it would be only weakly referenced and could
     *    be GC'ed before we can return it.
     * The solution is to use an instance of SingleFacadeFactory which will
     * allow the caller of new HttpClientImpl(...) to retrieve the facade
     * after the HttpClientImpl has been created.
     */
    private static final class SingleFacadeFactory {
        HttpClientFacade facade;
        HttpClientFacade createFacade(HttpClientImpl impl) {
            assert facade == null;
            return (facade = new HttpClientFacade(impl));
        }
    }

    static HttpClientFacade create(HttpClientBuilderImpl builder) {
        SingleFacadeFactory facadeFactory = new SingleFacadeFactory();
        HttpClientImpl impl = new HttpClientImpl(builder, facadeFactory);
        impl.start();
        assert facadeFactory.facade != null;
        assert impl.facadeRef.get() == facadeFactory.facade;
        return facadeFactory.facade;
    }

    private HttpClientImpl(HttpClientBuilderImpl builder,
                           SingleFacadeFactory facadeFactory) {
        id = CLIENT_IDS.incrementAndGet();
        dbgTag = "HttpClientImpl(" + id +")";
        @SuppressWarnings("removal")
        var sm = System.getSecurityManager();
        if (sm != null && builder.localAddr != null) {
            sm.checkListen(0);
        }
        localAddr = builder.localAddr;
        if (builder.sslContext == null) {
            try {
                sslContext = SSLContext.getDefault();
            } catch (NoSuchAlgorithmException ex) {
                throw new UncheckedIOException(new IOException(ex));
            }
        } else {
            sslContext = builder.sslContext;
        }
        Executor ex = builder.executor;
        if (ex == null) {
            ex = Executors.newCachedThreadPool(new DefaultThreadFactory(id));
            isDefaultExecutor = true;
        } else {
            isDefaultExecutor = false;
        }
        pendingRequests = new ConcurrentSkipListSet<>();
        delegatingExecutor = new DelegatingExecutor(this::isSelectorThread, ex,
                this::onSubmitFailure);
        facadeRef = new WeakReference<>(facadeFactory.createFacade(this));
        implRef = new WeakReference<>(this);
        client2 = new Http2ClientImpl(this);
        cookieHandler = builder.cookieHandler;
        connectTimeout = builder.connectTimeout;
        followRedirects = builder.followRedirects == null ?
                Redirect.NEVER : builder.followRedirects;
        this.userProxySelector = builder.proxy;
        this.proxySelector = Optional.ofNullable(userProxySelector)
                .orElseGet(HttpClientImpl::getDefaultProxySelector);
        if (debug.on())
            debug.log("proxySelector is %s (user-supplied=%s)",
                      this.proxySelector, userProxySelector != null);
        authenticator = builder.authenticator;
        if (builder.version == null) {
            version = HttpClient.Version.HTTP_2;
        } else {
            version = builder.version;
        }
        if (builder.sslParams == null) {
            sslParams = getDefaultParams(sslContext);
        } else {
            sslParams = builder.sslParams;
        }
        connections = new ConnectionPool(id);
        connections.start();
        timeouts = new TreeSet<>();
        try {
            selmgr = new SelectorManager(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        selmgr.setDaemon(true);
        filters = new FilterFactory();
        initFilters();
        assert facadeRef.get() != null;
    }

    void facadeCleanup() {
        SelectorManager selmgr = this.selmgr;
        if (selmgr != null) selmgr.wakeupSelector();
    }

    void onSubmitFailure(Runnable command, Throwable failure) {
        selmgr.abort(failure);
    }

    private void start() {
        try {
            selmgr.start();
        } catch (Throwable t) {
            isStarted.set(true);
            throw t;
        }
    }

    private void stop() {
        connections.stop();
        client2.stop();
        closeSubscribers();
        openedConnections.forEach(this::closeConnection);
        if (isDefaultExecutor) delegatingExecutor.shutdown();
    }

    private void closeSubscribers() {
        if (subscribers.isEmpty()) return;
        IOException io = selmgr.selectorClosedException();
        closeSubscribers(this, io);
    }

    private static void closeSubscribers(HttpClientImpl client, Throwable t) {
        client.subscribers.forEach(s -> s.onError(t));
    }

    /**
     * Adds the given subscriber to the subscribers list, or call
     * its {@linkplain HttpBodySubscriberWrapper#onError onError}
     * method if the client is shutting down.
     * @param subscriber the subscriber
     * @return true if the subscriber was added to the list.
     */
    public boolean registerSubscriber(HttpBodySubscriberWrapper<?> subscriber) {
        if (!selmgr.isClosed()) {
            selmgr.lock();
            try {
                if (!selmgr.isClosed()) {
                    if (subscribers.add(subscriber)) {
                        long count = pendingSubscribersCount.incrementAndGet();
                        if (debug.on()) {
                            debug.log("body subscriber registered: " + count);
                        }
                        return true;
                    }
                    return false;
                }
            } finally {
                selmgr.unlock();
            }
        }
        subscriber.onError(selmgr.selectorClosedException());
        return false;
    }

    /**
     * Remove the given subscriber from the subscribers list.
     * @param subscriber the subscriber
     * @return true if the subscriber was found and removed from the list.
     */
    public boolean unregisterSubscriber(HttpBodySubscriberWrapper<?> subscriber) {
        if (subscribers.remove(subscriber)) {
            long count = pendingSubscribersCount.decrementAndGet();
            if (debug.on()) {
                debug.log("body subscriber unregistered: " + count);
            }
            return true;
        }
        return false;
    }

    private void closeConnection(HttpConnection conn) {
        try { conn.close(); } catch (Throwable e) {
            if (Log.channel()) {
                Log.logChannel("Failed to close connection: " + e);
            }
        }
    }

    @Override
    public void shutdown() {
        shutdownRequested = true;
        selmgr.wakeupSelector();
    }

    @Override
    public void shutdownNow() {
        shutdown();
        selmgr.abort(new IOException("shutdownNow"));
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        return selmgr.join(duration);
    }

    @Override
    public boolean isTerminated() {
        return isStarted.get() && !isAlive.get();
    }

    private static SSLParameters getDefaultParams(SSLContext ctx) {
        SSLParameters params = ctx.getDefaultSSLParameters();
        return params;
    }

    @SuppressWarnings("removal")
    private static ProxySelector getDefaultProxySelector() {
        PrivilegedAction<ProxySelector> action = ProxySelector::getDefault;
        return AccessController.doPrivileged(action);
    }

    final HttpClientFacade facade() {
        return facadeRef.get();
    }

    public long newConnectionId() {
        return CONNECTION_IDS.incrementAndGet();
    }

    public void connectionOpened(PlainHttpConnection plainHttpConnection) {
        if (!finished()) {
            if (openedConnections.add(plainHttpConnection)) {
                pendingTCPConnectionCount.incrementAndGet();
            }
        } else closeConnection(plainHttpConnection);
    }

    public void connectionClosed(PlainHttpConnection plainHttpConnection) {
        if (openedConnections.remove(plainHttpConnection)) {
            pendingTCPConnectionCount.decrementAndGet();
        }
    }

    final long requestReference() {
        pendingHttpRequestCount.incrementAndGet();
        return reference();
    }

    final long requestUnreference() {
        pendingHttpRequestCount.decrementAndGet();
        return unreference();
    }

    final long reference() {
        pendingHttpOperationsCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    final long unreference() {
        final long count = pendingOperationCount.decrementAndGet();
        final long httpCount = pendingHttpOperationsCount.decrementAndGet();
        final long http2Count = pendingHttp2StreamCount.get();
        final long webSocketCount = pendingWebSocketCount.get();
        if (count == 0 && (facadeRef.refersTo(null) || shutdownRequested)) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    final long streamReference() {
        pendingHttp2StreamCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    final long streamUnreference() {
        final long count = pendingOperationCount.decrementAndGet();
        final long http2Count = pendingHttp2StreamCount.decrementAndGet();
        final long httpCount = pendingHttpOperationsCount.get();
        final long webSocketCount = pendingWebSocketCount.get();
        if (count == 0 && (facadeRef.refersTo(null) || shutdownRequested)) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    final long webSocketOpen() {
        pendingWebSocketCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    final long webSocketClose() {
        final long count = pendingOperationCount.decrementAndGet();
        final long webSocketCount = pendingWebSocketCount.decrementAndGet();
        final long httpCount = pendingHttpOperationsCount.get();
        final long http2Count = pendingHttp2StreamCount.get();
        if (count == 0 && (facadeRef.refersTo(null) || shutdownRequested)) {
            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    final long referenceCount() {
        return pendingOperationCount.get();
    }

    static final class HttpClientTracker implements Tracker {
        final AtomicLong requestCount;
        final AtomicLong httpCount;
        final AtomicLong http2Count;
        final AtomicLong websocketCount;
        final AtomicLong operationsCount;
        final AtomicLong connnectionsCount;
        final AtomicLong subscribersCount;
        final Reference<?> reference;
        final Reference<?> implRef;
        final AtomicBoolean isAlive;
        final AtomicBoolean isStarted;
        final String name;
        HttpClientTracker(AtomicLong request,
                          AtomicLong http,
                          AtomicLong http2,
                          AtomicLong ws,
                          AtomicLong ops,
                          AtomicLong conns,
                          AtomicLong subscribers,
                          Reference<?> ref,
                          Reference<?> implRef,
                          AtomicBoolean isAlive,
                          AtomicBoolean isStarted,
                          String name) {
            this.requestCount = request;
            this.httpCount = http;
            this.http2Count = http2;
            this.websocketCount = ws;
            this.operationsCount = ops;
            this.connnectionsCount = conns;
            this.subscribersCount = subscribers;
            this.reference = ref;
            this.implRef = implRef;
            this.isAlive = isAlive;
            this.isStarted = isStarted;
            this.name = name;
        }
        @Override
        public long getOutstandingSubscribers() {
            return subscribersCount.get();
        }
        @Override
        public long getOutstandingOperations() {
            return operationsCount.get();
        }
        @Override
        public long getOutstandingHttpRequests() {
            return requestCount.get();
        }
        @Override
        public long getOutstandingTcpConnections() { return connnectionsCount.get();}
        @Override
        public long getOutstandingHttpOperations() {
            return httpCount.get();
        }
        @Override
        public long getOutstandingHttp2Streams() { return http2Count.get(); }
        @Override
        public long getOutstandingWebSocketOperations() {
            return websocketCount.get();
        }
        @Override
        public boolean isFacadeReferenced() {
            return !reference.refersTo(null);
        }
        public boolean isImplementationReferenced() {
            return !implRef.refersTo(null);
        }
        @Override
        public boolean isSelectorAlive() { return isAlive.get() || !isStarted.get(); }
        @Override
        public String getName() {
            return name;
        }
    }

    public Tracker getOperationsTracker() {
        return new HttpClientTracker(
                pendingHttpRequestCount,
                pendingHttpOperationsCount,
                pendingHttp2StreamCount,
                pendingWebSocketCount,
                pendingOperationCount,
                pendingTCPConnectionCount,
                pendingSubscribersCount,
                facadeRef,
                implRef,
                isAlive,
                isStarted,
                dbgTag);
    }

    boolean finished() {
        if (referenceCount() > 0) return false;
        if (shutdownRequested) {
            synchronized (this) {
                if (referenceCount() == 0) return true;
            }
        }
        return !isReferenced();
    }

    private boolean isReferenced() {
        return !facadeRef.refersTo(null) || referenceCount() > 0;
    }

    /**
     * Wait for activity on given exchange.
     * The following occurs in the SelectorManager thread.
     *
     *  1) add to selector
     *  2) If selector fires for this exchange then
     *     call AsyncEvent.handle()
     *
     * If exchange needs to change interest ops, then call registerEvent() again.
     */
    void registerEvent(AsyncEvent exchange) throws IOException {
        selmgr.register(exchange);
    }

    /**
     * Allows an AsyncEvent to modify its interestOps.
     * @param event The modified event.
     */
    void eventUpdated(AsyncEvent event) throws ClosedChannelException {
        assert !(event instanceof AsyncTriggerEvent);
        selmgr.eventUpdated(event);
    }

    boolean isSelectorThread() {
        return Thread.currentThread() == selmgr;
    }

    boolean isSelectorClosed() {
        return selmgr.isClosed();
    }

    IOException selectorClosedException() {
        return selmgr.selectorClosedException();
    }

    Http2ClientImpl client2() {
        return client2;
    }

    private void debugCompleted(String tag, long startNanos, HttpRequest req) {
        if (debugelapsed.on()) {
            debugelapsed.log(tag + " elapsed "
                    + (System.nanoTime() - startNanos)/1000_000L
                    + " millis for " + req.method()
                    + " to " + req.uri());
        }
    }

    @Override
    public <T> HttpResponse<T>
    send(HttpRequest req, BodyHandler<T> responseHandler)
        throws IOException, InterruptedException
    {
        CompletableFuture<HttpResponse<T>> cf = null;

        if (Thread.interrupted()) throw new InterruptedException();
        try {
            cf = sendAsync(req, responseHandler, null, null);
            return cf.get();
        } catch (InterruptedException ie) {
            if (cf != null) {
                cf.cancel(true);
            }
            throw ie;
        } catch (ExecutionException e) {
            final Throwable throwable = e.getCause();
            final String msg = throwable.getMessage();

            if (throwable instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(msg, throwable);
            } else if (throwable instanceof SecurityException) {
                throw new SecurityException(msg, throwable);
            } else if (throwable instanceof HttpConnectTimeoutException) {
                HttpConnectTimeoutException hcte = new HttpConnectTimeoutException(msg);
                hcte.initCause(throwable);
                throw hcte;
            } else if (throwable instanceof HttpTimeoutException) {
                throw new HttpTimeoutException(msg);
            } else if (throwable instanceof ConnectException) {
                ConnectException ce = new ConnectException(msg);
                ce.initCause(throwable);
                throw ce;
            } else if (throwable instanceof SSLHandshakeException) {
                throw new SSLHandshakeException(msg, throwable);
            } else if (throwable instanceof SSLException) {
                throw new SSLException(msg, throwable);
            } else if (throwable instanceof ProtocolException) {
                throw new ProtocolException(msg);
            } else if (throwable instanceof IOException) {
                throw new IOException(msg, throwable);
            } else {
                throw new IOException(msg, throwable);
            }
        }
    }

    private static final Executor ASYNC_POOL = new CompletableFuture<Void>().defaultExecutor();

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest, BodyHandler<T> responseHandler)
    {
        return sendAsync(userRequest, responseHandler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest,
              BodyHandler<T> responseHandler,
              PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(userRequest, responseHandler, pushPromiseHandler, delegatingExecutor.delegate);
    }

    @SuppressWarnings("removal")
    private <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest,
              BodyHandler<T> responseHandler,
              PushPromiseHandler<T> pushPromiseHandler,
              Executor exchangeExecutor)    {

        Objects.requireNonNull(userRequest);
        Objects.requireNonNull(responseHandler);

        if (shutdownRequested) {
            return MinimalFuture.failedFuture(new IOException("closed"));
        }

        if (selmgr.isClosed()) {
            return MinimalFuture.failedFuture(selmgr.selectorClosedException());
        }

        AccessControlContext acc = null;
        if (System.getSecurityManager() != null)
            acc = AccessController.getContext();

        HttpRequestImpl requestImpl = new HttpRequestImpl(userRequest, proxySelector);
        if (requestImpl.method().equals("CONNECT"))
            throw new IllegalArgumentException("Unsupported method CONNECT");

        long id = pendingRequestId.incrementAndGet();
        long start = DEBUGELAPSED ? System.nanoTime() : 0;
        synchronized (this) {
            if (shutdownRequested) {
                return MinimalFuture.failedFuture(new IOException("closed"));
            }
            requestReference();
        }
        try {
            if (debugelapsed.on())
                debugelapsed.log("ClientImpl (async) send %s", userRequest);

            Executor executor = exchangeExecutor == null
                    ? this.delegatingExecutor : exchangeExecutor;

            MultiExchange<T> mex = new MultiExchange<>(userRequest,
                                                            requestImpl,
                                                            this,
                                                            responseHandler,
                                                            pushPromiseHandler,
                                                            acc);
            CompletableFuture<HttpResponse<T>> mexCf = mex.responseAsync(executor);
            CompletableFuture<HttpResponse<T>> res = mexCf.whenComplete((b,t) -> requestUnreference());
            if (DEBUGELAPSED) {
                res = res.whenComplete(
                        (b,t) -> debugCompleted("ClientImpl (async)", start, userRequest));
            }

            PendingRequest pending = new PendingRequest(id, requestImpl, mexCf, mex, this);
            res = registerPending(pending, res);

            if (exchangeExecutor != null) {
                return res.isDone() ? res
                        : res.whenCompleteAsync((r, t) -> { /* do nothing */}, ASYNC_POOL);
            } else {
                return res.isDone() ? res : res.copy();
            }
        } catch (Throwable t) {
            requestUnreference();
            debugCompleted("ClientImpl (async)", start, userRequest);
            throw t;
        }
    }

    private static final class SelectorManager extends Thread {

        private static final int MIN_NODEADLINE = 1000; 
        private static final int MAX_NODEADLINE = 1000 * 1200; 
        private static final int DEF_NODEADLINE = 3000; 
        private static final long NODEADLINE; 
        static {
            long deadline =  Utils.getIntegerProperty(
                "jdk.internal.httpclient.selectorTimeout",
                DEF_NODEADLINE); 
            if (deadline <= 0) deadline = DEF_NODEADLINE;
            deadline = Math.max(deadline, MIN_NODEADLINE);
            NODEADLINE = Math.min(deadline, MAX_NODEADLINE);
        }

        private final Selector selector;
        private volatile boolean closed;
        private final List<AsyncEvent> registrations;
        private final List<AsyncTriggerEvent> deregistrations;
        private final Logger debug;
        private final Logger debugtimeout;
        private final HttpClientImpl owner;
        private final ConnectionPool pool;
        private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        private final ReentrantLock lock = new ReentrantLock();

        SelectorManager(HttpClientImpl ref) throws IOException {
            super(null, null,
                  "HttpClient-" + ref.id + "-SelectorManager",
                  0, false);
            owner = ref;
            debug = ref.debug;
            debugtimeout = ref.debugtimeout;
            pool = ref.connectionPool();
            registrations = new ArrayList<>();
            deregistrations = new ArrayList<>();
            selector = Selector.open();
        }

        IOException selectorClosedException() {
            var io = new IOException("selector manager closed");
            var cause = errorRef.get();
            if (cause != null) {
                io.initCause(cause);
            }
            return io;
        }

        void eventUpdated(AsyncEvent e) throws ClosedChannelException {
            if (Thread.currentThread() == this) {
                SelectionKey key = e.channel().keyFor(selector);
                if (key != null && key.isValid()) {
                    SelectorAttachment sa = (SelectorAttachment) key.attachment();
                    sa.register(e);
                } else if (e.interestOps() != 0) {
                    if (debug.on()) debug.log("No key for channel");
                    e.abort(new IOException("No key for channel"));
                }
            } else {
                register(e);
            }
        }

        void register(AsyncEvent e) {
            var closed = this.closed;
            if (!closed) {
                lock.lock();
                try {
                    closed = this.closed;
                    if (!closed) {
                        registrations.add(e);
                    }
                } finally {
                    lock.unlock();
                }
            }
            if (closed) {
                e.abort(selectorClosedException());
            } else {
                selector.wakeup();
            }
        }

        void wakeupSelector() {
            selector.wakeup();
        }

        void abort(Throwable t) {
            boolean closed = this.closed;
            errorRef.compareAndSet(null, t);
            if (debug.on()) {
                debug.log("aborting selector manager(closed=%s): " + t, closed);
            }
            t = errorRef.get();
            boolean inSelectorThread = owner.isSelectorThread();
            if (!inSelectorThread) {
                abortPendingRequests(owner, t);
            }
            Set<SelectionKey> keys = new HashSet<>();
            Set<AsyncEvent> toAbort = new HashSet<>();
            lock.lock();
            try {
                if (closed = this.closed) return;
                this.closed = true;
                try {
                    keys.addAll(selector.keys());
                } catch (ClosedSelectorException ce) {
                }
                toAbort.addAll(this.registrations);
                toAbort.addAll(this.deregistrations);
                this.registrations.clear();
                this.deregistrations.clear();
            } finally {
                lock.unlock();
            }
            abortPendingRequests(owner, t);

            IOException io = toAbort.isEmpty()
                    ? null : selectorClosedException();
            for (AsyncEvent e : toAbort) {
                try {
                    e.abort(io);
                } catch (Throwable x) {
                    debug.log("Failed to abort event: " + x);
                }
            }
            if (!inSelectorThread) selector.wakeup();
        }

        private void shutdown() {
            try {
                lock.lock();
                try {
                    Log.logTrace("{0}: shutting down", getName());
                    if (debug.on()) debug.log("SelectorManager shutting down");
                    closed = true;
                    selector.close();
                } finally {
                    lock.unlock();
                }
            } catch (IOException ignored) {
            } finally {
                owner.stop();
            }
        }

        boolean isClosed() {
            return closed;
        }

        @Override
        public void run() {
            List<Pair<AsyncEvent,IOException>> errorList = new ArrayList<>();
            List<AsyncEvent> readyList = new ArrayList<>();
            List<Runnable> resetList = new ArrayList<>();
            owner.isAlive.set(true);   
            owner.isStarted.set(true); 
            try {
                if (Log.channel()) Log.logChannel(getName() + ": starting");
                while (!Thread.currentThread().isInterrupted() && !closed) {
                    lock.lock();
                    try {
                        assert errorList.isEmpty();
                        assert readyList.isEmpty();
                        assert resetList.isEmpty();
                        for (AsyncTriggerEvent event : deregistrations) {
                            event.handle();
                        }
                        deregistrations.clear();
                        for (AsyncEvent event : registrations) {
                            if (event instanceof AsyncTriggerEvent) {
                                readyList.add(event);
                                continue;
                            }
                            SelectableChannel chan = event.channel();
                            SelectionKey key = null;
                            try {
                                key = chan.keyFor(selector);
                                SelectorAttachment sa;
                                if (key == null || !key.isValid()) {
                                    if (key != null) {
                                        selector.selectNow();
                                    }
                                    sa = new SelectorAttachment(chan, selector);
                                } else {
                                    sa = (SelectorAttachment) key.attachment();
                                }
                                sa.register(event);
                                if (!chan.isOpen()) {
                                    throw new ClosedChannelException();
                                }
                            } catch (IOException e) {
                                Log.logTrace("{0}: {1}", getName(), e);
                                if (debug.on())
                                    debug.log("Got " + e.getClass().getName()
                                              + " while handling registration events");
                                chan.close();
                                errorList.add(new Pair<>(event, e));
                                if (key != null) {
                                    key.cancel();
                                    selector.selectNow();
                                }
                            }
                        }
                        registrations.clear();
                        selector.selectedKeys().clear();
                    } finally {
                        lock.unlock();
                    }

                    for (AsyncEvent event : readyList) {
                        assert event instanceof AsyncTriggerEvent;
                        event.handle();
                    }
                    readyList.clear();

                    for (Pair<AsyncEvent,IOException> error : errorList) {
                        handleEvent(error.first, error.second);
                    }
                    errorList.clear();

                    if (owner.finished()) {
                        Log.logTrace("{0}: {1}",
                                getName(),
                                "HttpClient finished. Exiting...");
                        return;
                    }

                    long nextTimeout = owner.purgeTimeoutsAndReturnNextDeadline();
                    if (debugtimeout.on())
                        debugtimeout.log("next timeout: %d", nextTimeout);

                    long nextExpiry = pool.purgeExpiredConnectionsAndReturnNextDeadline();
                    if (debugtimeout.on())
                        debugtimeout.log("next expired: %d", nextExpiry);

                    assert nextTimeout >= 0;
                    assert nextExpiry >= 0;

                    if (nextTimeout <= 0) nextTimeout = NODEADLINE;

                    if (nextExpiry <= 0) nextExpiry = NODEADLINE;
                    else nextExpiry = Math.min(NODEADLINE, nextExpiry);

                    long millis = Math.min(nextExpiry, nextTimeout);

                    if (debugtimeout.on())
                        debugtimeout.log("Next deadline is %d",
                                         (millis == 0 ? NODEADLINE : millis));
                    int n = selector.select(millis == 0 ? NODEADLINE : millis);
                    if (n == 0) {
                        if (owner.finished()) {
                            Log.logTrace("{0}: {1}",
                                    getName(),
                                    "HttpClient finished. Exiting...");
                            return;
                        }
                        owner.purgeTimeoutsAndReturnNextDeadline();
                        continue;
                    }

                    Set<SelectionKey> keys = selector.selectedKeys();
                    assert errorList.isEmpty();

                    for (SelectionKey key : keys) {
                        SelectorAttachment sa = (SelectorAttachment) key.attachment();
                        if (!key.isValid()) {
                            IOException ex = sa.chan.isOpen()
                                    ? new IOException("Invalid key")
                                    : new ClosedChannelException();
                            sa.pending.forEach(e -> errorList.add(new Pair<>(e,ex)));
                            sa.pending.clear();
                            continue;
                        }

                        int eventsOccurred;
                        try {
                            eventsOccurred = key.readyOps();
                        } catch (CancelledKeyException ex) {
                            IOException io = Utils.getIOException(ex);
                            sa.pending.forEach(e -> errorList.add(new Pair<>(e,io)));
                            sa.pending.clear();
                            continue;
                        }
                        sa.events(eventsOccurred).forEach(readyList::add);
                        resetList.add(() -> sa.resetInterestOps(eventsOccurred));
                    }

                    selector.selectNow(); 
                    selector.selectedKeys().clear();

                    IOException ioe = closed ? selectorClosedException() : null;
                    readyList.forEach((e) -> handleEvent(e, ioe));
                    readyList.clear();

                    errorList.forEach((p) -> handleEvent(p.first, p.second));
                    errorList.clear();

                    resetList.forEach(r -> r.run());
                    resetList.clear();

                }
            } catch (Throwable e) {
                errorRef.compareAndSet(null, e);
                if (!closed) {
                    closed = true; 
                    String err = Utils.stackTrace(e);
                    Log.logError("{0}: {1}: {2}", getName(),
                            "HttpClientImpl shutting down due to fatal error", err);
                }
                abortPendingRequests(owner, selectorClosedException());
                if (debug.on()) debug.log("shutting down", e);
                if (Utils.ASSERTIONSENABLED && !debug.on()) {
                    e.printStackTrace(System.err); 
                }
            } finally {
                if (Log.channel()) Log.logChannel(getName() + ": stopping");
                try {
                    shutdown();
                } finally {
                    owner.isAlive.set(false);
                }
            }
        }


        /** Handles the given event. The given ioe may be null. */
        void handleEvent(AsyncEvent event, IOException ioe) {
            if (ioe == null && closed) {
                ioe = selectorClosedException();
            }
            if (ioe != null) {
                event.abort(ioe);
            } else {
                event.handle();
            }
        }

        void lock() {
            lock.lock();
        }

        void unlock() {
            lock.unlock();
        }
    }

    final String debugInterestOps(SelectableChannel channel) {
        try {
            SelectionKey key = channel.keyFor(selmgr.selector);
            if (key == null) return "channel not registered with selector";
            String keyInterestOps = key.isValid()
                    ? "key.interestOps=" + Utils.interestOps(key) : "invalid key";
            return String.format("channel registered with selector, %s, sa.interestOps=%s",
                    keyInterestOps,
                    Utils.describeOps(((SelectorAttachment)key.attachment()).interestOps));
        } catch (Throwable t) {
            return String.valueOf(t);
        }
    }

    /**
     * Tracks multiple user level registrations associated with one NIO
     * registration (SelectionKey). In this implementation, registrations
     * are one-off and when an event is posted the registration is cancelled
     * until explicitly registered again.
     *
     * <p> No external synchronization required as this class is only used
     * by the SelectorManager thread. One of these objects required per
     * connection.
     */
    private static class SelectorAttachment {
        private final SelectableChannel chan;
        private final Selector selector;
        private final Set<AsyncEvent> pending;
        private static final Logger debug =
                Utils.getDebugLogger("SelectorAttachment"::toString, Utils.DEBUG);
        private int interestOps;

        SelectorAttachment(SelectableChannel chan, Selector selector) {
            this.pending = new HashSet<>();
            this.chan = chan;
            this.selector = selector;
        }

        void register(AsyncEvent e) throws ClosedChannelException {
            int newOps = e.interestOps();
            boolean reRegister = (interestOps & newOps) != newOps;
            interestOps |= newOps;
            pending.add(e);
            if (debug.on())
                debug.log("Registering %s for %s (%s)",
                        e, Utils.describeOps(newOps), reRegister);
            if (reRegister) {
                try {
                    chan.register(selector, interestOps, this);
                } catch (Throwable x) {
                    abortPending(x);
                }
            } else if (!chan.isOpen()) {
                abortPending(new ClosedChannelException());
            }
        }

        /**
         * Returns a Stream<AsyncEvents> containing only events that are
         * registered with the given {@code interestOps}.
         */
        Stream<AsyncEvent> events(int interestOps) {
            return pending.stream()
                    .filter(ev -> (ev.interestOps() & interestOps) != 0);
        }

        /**
         * Removes any events with the given {@code interestOps}, and if no
         * events remaining, cancels the associated SelectionKey.
         */
        void resetInterestOps(int interestOps) {
            int newOps = 0;

            Iterator<AsyncEvent> itr = pending.iterator();
            while (itr.hasNext()) {
                AsyncEvent event = itr.next();
                int evops = event.interestOps();
                if (event.repeating()) {
                    newOps |= evops;
                    continue;
                }
                if ((evops & interestOps) != 0) {
                    itr.remove();
                } else {
                    newOps |= evops;
                }
            }

            this.interestOps = newOps;
            SelectionKey key = chan.keyFor(selector);
            if (newOps == 0 && key != null && pending.isEmpty()) {
                key.cancel();
            } else {
                try {
                    if (key == null || !key.isValid()) {
                        throw new CancelledKeyException();
                    }
                    key.interestOps(newOps);
                    if (!chan.isOpen()) {
                        abortPending(new ClosedChannelException());
                        return;
                    }
                    assert key.interestOps() == newOps;
                } catch (CancelledKeyException x) {
                    if (debug.on()) debug.log("key cancelled for " + chan);
                    abortPending(x);
                }
            }
        }

        void abortPending(Throwable x) {
            if (!pending.isEmpty()) {
                AsyncEvent[] evts = pending.toArray(new AsyncEvent[0]);
                pending.clear();
                IOException io = Utils.getIOException(x);
                for (AsyncEvent event : evts) {
                    event.abort(io);
                }
            }
        }
    }

    /*package-private*/ SSLContext theSSLContext() {
        return sslContext;
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public SSLParameters sslParameters() {
        return Utils.copySSLParameters(sslParams);
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.ofNullable(authenticator);
    }

    /*package-private*/ final DelegatingExecutor theExecutor() {
        return delegatingExecutor;
    }

    @Override
    public final Optional<Executor> executor() {
        return isDefaultExecutor
                ? Optional.empty()
                : Optional.of(delegatingExecutor.delegate());
    }

    ConnectionPool connectionPool() {
        return connections;
    }

    @Override
    public Redirect followRedirects() {
        return followRedirects;
    }


    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.ofNullable(cookieHandler);
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    Optional<Duration> idleConnectionTimeout() {
        return Optional.ofNullable(getIdleConnectionTimeout());
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.ofNullable(userProxySelector);
    }

    ProxySelector proxySelector() {
        return proxySelector;
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return new BuilderImpl(this.facade(), proxySelector);
    }

    @Override
    public Version version() {
        return version;
    }

    InetAddress localAddress() {
        return localAddr;
    }

    String dbgString() {
        return dbgTag;
    }

    @Override
    public String toString() {
        return super.toString() + ("(" + id + ")");
    }

    private void initFilters() {
        addFilter(AuthenticationFilter.class);
        addFilter(RedirectFilter.class);
        if (this.cookieHandler != null) {
            addFilter(CookieFilter.class);
        }
    }

    private void addFilter(Class<? extends HeaderFilter> f) {
        filters.addFilter(f);
    }

    final List<HeaderFilter> filterChain() {
        return filters.getFilterChain();
    }


    void registerTimer(TimeoutEvent event) {
        Log.logTrace("Registering timer {0}", event);
        synchronized (this) {
            timeouts.add(event);
            selmgr.wakeupSelector();
        }
    }

    void cancelTimer(TimeoutEvent event) {
        Log.logTrace("Canceling timer {0}", event);
        synchronized (this) {
            timeouts.remove(event);
        }
    }

    /**
     * Purges ( handles ) timer events that have passed their deadline, and
     * returns the amount of time, in milliseconds, until the next earliest
     * event. A return value of 0 means that there are no events.
     */
    private long purgeTimeoutsAndReturnNextDeadline() {
        long diff = 0L;
        List<TimeoutEvent> toHandle = null;
        int remaining = 0;
        synchronized (this) {
            if (timeouts.isEmpty()) return 0L;

            Deadline now = TimeSource.now();
            Iterator<TimeoutEvent> itr = timeouts.iterator();
            while (itr.hasNext()) {
                TimeoutEvent event = itr.next();
                diff = now.until(event.deadline(), ChronoUnit.MILLIS);
                if (diff <= 0) {
                    itr.remove();
                    toHandle = (toHandle == null) ? new ArrayList<>() : toHandle;
                    toHandle.add(event);
                } else {
                    break;
                }
            }
            remaining = timeouts.size();
        }

        if (toHandle != null && Log.trace()) {
            Log.logTrace("purgeTimeoutsAndReturnNextDeadline: handling "
                    +  toHandle.size() + " events, "
                    + "remaining " + remaining
                    + ", next deadline: " + (diff < 0 ? 0L : diff));
        }

        if (toHandle != null) {
            Throwable failed = null;
            for (TimeoutEvent event : toHandle) {
                try {
                   Log.logTrace("Firing timer {0}", event);
                   event.handle();
                } catch (Error | RuntimeException e) {
                    if (failed == null) failed = e;
                    else failed.addSuppressed(e);
                    Log.logTrace("Failed to handle event {0}: {1}", event, e);
                }
            }
            if (failed instanceof Error) throw (Error) failed;
            if (failed instanceof RuntimeException) throw (RuntimeException) failed;
        }

        return diff < 0 ? 0L : diff;
    }

    int getReceiveBufferSize() {
        return Utils.getIntegerNetProperty(
                "jdk.httpclient.receiveBufferSize",
                0 
        );
    }

    int getSendBufferSize() {
        return Utils.getIntegerNetProperty(
                "jdk.httpclient.sendBufferSize",
                0 
        );
    }


    BufferSupplier getSSLBufferSupplier() {
        return sslBufferSupplier;
    }

    private Duration getIdleConnectionTimeout() {
        if (IDLE_CONNECTION_TIMEOUT >= 0)
            return Duration.ofSeconds(IDLE_CONNECTION_TIMEOUT);
        return null;
    }

    private static long getTimeoutProp(String prop, long def) {
        String s = Utils.getNetProperty(prop);
        try {
            if (s != null) {
                long timeoutVal = Long.parseLong(s);
                if (timeoutVal >= 0) return timeoutVal;
            }
        } catch (NumberFormatException ignored) {
            Log.logTrace("Invalid value set for " + prop + " property: " + ignored);
        }
        return def;
    }

    private static final class SSLDirectBufferSupplier implements BufferSupplier {
        private static final int POOL_SIZE = SocketTube.MAX_BUFFERS;
        private final ByteBuffer[] pool = new ByteBuffer[POOL_SIZE];
        private final HttpClientImpl client;
        private final Logger debug;
        private int tail, count; 

        SSLDirectBufferSupplier(HttpClientImpl client) {
            this.client = Objects.requireNonNull(client);
            this.debug = client.debug;
        }

        @Override
        public ByteBuffer get() {
            assert client.isSelectorThread();
            assert tail <= POOL_SIZE : "allocate tail is " + tail;
            ByteBuffer buf;
            if (tail == 0) {
                if (debug.on()) {
                    debug.log("ByteBuffer.allocateDirect(%d)", Utils.BUFSIZE);
                }
                assert count++ < POOL_SIZE : "trying to allocate more than "
                            + POOL_SIZE + " buffers";
                buf = ByteBuffer.allocateDirect(Utils.BUFSIZE);
            } else {
                assert tail > 0 : "non positive tail value: " + tail;
                tail--;
                buf = pool[tail];
                pool[tail] = null;
            }
            assert buf.isDirect();
            assert buf.position() == 0;
            assert buf.hasRemaining();
            assert buf.limit() == Utils.BUFSIZE;
            assert tail < POOL_SIZE;
            assert tail >= 0;
            return buf;
        }

        @Override
        public void recycle(ByteBuffer buffer) {
            assert client.isSelectorThread();
            assert buffer.isDirect();
            assert !buffer.hasRemaining();
            assert tail < POOL_SIZE : "recycle tail is " + tail;
            assert tail >= 0;
            buffer.position(0);
            buffer.limit(buffer.capacity());
            if (tail < POOL_SIZE) {
                pool[tail] = buffer;
                tail++;
            }
            assert tail <= POOL_SIZE;
            assert tail > 0;
        }
    }

}
