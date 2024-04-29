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

import java.io.EOFException;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import jdk.internal.net.http.ResponseContent.BodyParser;
import jdk.internal.net.http.ResponseContent.UnknownLengthBodyParser;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpResponse.BodySubscribers.discarding;
import static jdk.internal.net.http.common.Utils.wrapWithExtraDetail;
import static jdk.internal.net.http.RedirectFilter.HTTP_NOT_MODIFIED;

/**
 * Handles a HTTP/1.1 response (headers + body).
 * There can be more than one of these per Http exchange.
 */
class Http1Response<T> {

    private volatile ResponseContent content;
    private final HttpRequestImpl request;
    private Response response;
    private final HttpConnection connection;
    private HttpHeaders headers;
    private int responseCode;
    private final Http1Exchange<T> exchange;
    private boolean return2Cache; 
    private final HeadersReader headersReader; 
    private final BodyReader bodyReader; 
    private final Http1AsyncReceiver asyncReceiver;
    private volatile EOFException eof;
    private volatile BodyParser bodyParser;
    private volatile boolean closeWhenFinished;
    private static final int MAX_IGNORE = 1024;

    enum State {INITIAL, READING_HEADERS, READING_BODY, DONE}
    private volatile State readProgress = State.INITIAL;

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    static final AtomicLong responseCount = new AtomicLong();
    final long id = responseCount.incrementAndGet();
    private Http1HeaderParser hd;

    Http1Response(HttpConnection conn,
                  Http1Exchange<T> exchange,
                  Http1AsyncReceiver asyncReceiver) {
        this.readProgress = State.INITIAL;
        this.request = exchange.request();
        this.exchange = exchange;
        this.connection = conn;
        this.asyncReceiver = asyncReceiver;
        headersReader = new HeadersReader(this::advance);
        bodyReader = new BodyReader(this::advance);

        hd = new Http1HeaderParser();
        readProgress = State.READING_HEADERS;
        headersReader.start(hd);
        asyncReceiver.subscribe(headersReader);
    }

    String dbgTag;
    private String dbgString() {
        String dbg = dbgTag;
        if (dbg == null) {
            String cdbg = connection.dbgTag;
            if (cdbg != null) {
                dbgTag = dbg = "Http1Response(id=" + id + ", " + cdbg + ")";
            } else {
                dbg = "Http1Response(id=" + id + ")";
            }
        }
        return dbg;
    }

    private static final class ClientRefCountTracker {
        final HttpClientImpl client;
        final Logger debug;
        volatile byte state;

        ClientRefCountTracker(HttpClientImpl client, Logger logger) {
            this.client = client;
            this.debug = logger;
        }

        public boolean acquire() {
            if (STATE.compareAndSet(this, (byte) 0, (byte) 0x01)) {
                if (debug.on())
                    debug.log("Operation started: incrementing ref count for %s", client);
                client.reference();
                return true;
            } else {
                if (debug.on())
                    debug.log("Operation ref count for %s is already %s",
                              client, ((state & 0x2) == 0x2) ? "released." : "incremented!" );
                assert (state & 0x01) == 0 : "reference count already incremented";
                return false;
            }
        }

        public void tryRelease() {
            if (STATE.compareAndSet(this, (byte) 0x01, (byte) 0x03)) {
                if (debug.on())
                    debug.log("Operation finished: decrementing ref count for %s", client);
                client.unreference();
            } else if (state == 0) {
                if (debug.on())
                    debug.log("Operation not started: releasing ref count for %s", client);
            } else if ((state & 0x02) == 0x02) {
                if (debug.on())
                    debug.log("ref count for %s already released", client);
            }
        }

        private static final VarHandle STATE;
        static {
            try {
                STATE = MethodHandles.lookup().findVarHandle(
                        ClientRefCountTracker.class, "state", byte.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    private volatile boolean firstTimeAround = true;

    public CompletableFuture<Response> readHeadersAsync(Executor executor) {
        if (debug.on())
            debug.log("Reading Headers: (remaining: "
                      + asyncReceiver.remaining() +") "  + readProgress);

        if (firstTimeAround) {
            if (debug.on()) debug.log("First time around");
            firstTimeAround = false;
        } else {
            asyncReceiver.unsubscribe(bodyReader);
            bodyReader.reset();

            hd = new Http1HeaderParser();
            readProgress = State.READING_HEADERS;
            headersReader.reset();
            headersReader.start(hd);
            asyncReceiver.subscribe(headersReader);
        }

        CompletableFuture<State> cf = headersReader.completion();
        assert cf != null : "parsing not started";
        if (debug.on()) {
            debug.log("headersReader is %s",
                    cf == null ? "not yet started"
                            : cf.isDone() ? "already completed"
                            : "not yet completed");
        }

        Function<State, Response> lambda = (State completed) -> {
                assert completed == State.READING_HEADERS;
                if (debug.on())
                    debug.log("Reading Headers: creating Response object;"
                              + " state is now " + readProgress);
                asyncReceiver.unsubscribe(headersReader);
                responseCode = hd.responseCode();
                headers = hd.headers();

                response = new Response(request,
                                        exchange.getExchange(),
                                        headers,
                                        connection,
                                        responseCode,
                                        HTTP_1_1);

                if (Log.headers()) {
                    StringBuilder sb = new StringBuilder("RESPONSE HEADERS:\n");
                    Log.dumpHeaders(sb, "    ", headers);
                    Log.logHeaders(sb.toString());
                }

                return response;
            };

        if (executor != null) {
            return cf.thenApplyAsync(lambda, executor);
        } else {
            return cf.thenApply(lambda);
        }
    }

    private boolean finished;

    synchronized void completed() {
        finished = true;
    }

    synchronized boolean finished() {
        return finished;
    }

    /**
     * Return known fixed content length or -1 if chunked, or -2 if no content-length
     * information in which case, connection termination delimits the response body
     */
    long fixupContentLen(long clen) {
        if (request.method().equalsIgnoreCase("HEAD") || responseCode == HTTP_NOT_MODIFIED) {
            return 0L;
        }
        if (clen == -1L) {
            if (headers.firstValue("Transfer-encoding").orElse("")
                       .equalsIgnoreCase("chunked")) {
                return -1L;
            }
            if (responseCode == 101) {
                return 0L;
            }
            return -2L;
        }
        return clen;
    }

    /**
     * Read up to MAX_IGNORE bytes discarding
     */
    public CompletableFuture<Void> ignoreBody(Executor executor) {
        int clen = (int)headers.firstValueAsLong("Content-Length").orElse(-1);
        if (clen == -1 || clen > MAX_IGNORE) {
            connection.close();
            return MinimalFuture.completedFuture(null); 
        } else {
            return readBody(discarding(), !request.isWebSocket(), executor);
        }
    }

    public void nullBody(HttpResponse<T> resp, Throwable t) {
        if (t != null) connection.close();
        else {
            return2Cache = !request.isWebSocket();
            onFinished();
        }
    }


    public <U> CompletableFuture<U> readBody(HttpResponse.BodySubscriber<U> p,
                                         boolean return2Cache,
                                         Executor executor) {
        if (debug.on()) {
            debug.log("readBody: return2Cache: " + return2Cache);
            if (request.isWebSocket() && return2Cache && connection != null) {
                debug.log("websocket connection will be returned to cache: "
                        + connection.getClass() + "/" + connection);
            }
        }
        assert !return2Cache || !request.isWebSocket();
        this.return2Cache = return2Cache;
        final BodySubscriber<U> subscriber = p;


        final CompletableFuture<U> cf = new MinimalFuture<>();

        long clen0 = headers.firstValueAsLong("Content-Length").orElse(-1L);
        final long clen = fixupContentLen(clen0);

        asyncReceiver.unsubscribe(headersReader);
        headersReader.reset();
        ClientRefCountTracker refCountTracker = new ClientRefCountTracker(connection.client(), debug);

        connection.client().reference();
        executor.execute(() -> {
            boolean acquired = false;
            try {
                content = new ResponseContent(
                        connection, clen, headers, subscriber,
                        this::onFinished
                );
                if (cf.isCompletedExceptionally()) {
                    connection.close();
                    return;
                }
                acquired = refCountTracker.acquire();
                assert acquired == true;
                bodyParser = content.getBodyParser(
                    (t) -> {
                        try {
                            if (t != null) {
                                try {
                                    subscriber.onError(t);
                                } finally {
                                    cf.completeExceptionally(t);
                                }
                            }
                        } finally {
                            bodyReader.onComplete(t);
                            if (t != null) {
                                connection.close();
                            }
                        }
                    });
                bodyReader.start(bodyParser);
                CompletableFuture<State> bodyReaderCF = bodyReader.completion();
                asyncReceiver.subscribe(bodyReader);
                assert bodyReaderCF != null : "parsing not started";
                CompletableFuture<?> trailingOp = bodyReaderCF.whenComplete((s, t) -> {
                    t = Utils.getCompletionCause(t);
                    try {
                        if (t == null) {
                            if (debug.on()) debug.log("Finished reading body: " + s);
                            assert s == State.READING_BODY;
                        }
                        if (t != null) {
                            subscriber.onError(t);
                            cf.completeExceptionally(t);
                        }
                    } catch (Throwable x) {
                        asyncReceiver.onReadError(x);
                    } finally {
                        refCountTracker.tryRelease();
                    }
                });
                connection.addTrailingOperation(trailingOp);
            } catch (Throwable t) {
                if (debug.on()) debug.log("Failed reading body: " + t);
                try {
                    subscriber.onError(t);
                    cf.completeExceptionally(t);
                } finally {
                    if (acquired) refCountTracker.tryRelease();
                    asyncReceiver.onReadError(t);
                }
            } finally {
                connection.client().unreference();
            }
        });

        ResponseSubscribers.getBodyAsync(executor, p, cf, (t) -> {
            subscriber.onError(t);
            cf.completeExceptionally(t);
            asyncReceiver.setRetryOnError(false);
            asyncReceiver.onReadError(t);
        });

        return cf.whenComplete((s,t) -> {
            if (t != null) {
                refCountTracker.tryRelease();
            }
        });
    }


    private void onFinished() {
        asyncReceiver.clear();
        if (closeWhenFinished) {
            if (debug.on())
                debug.log("Closing Connection when finished");
            connection.close();
        } else if (return2Cache) {
            Log.logTrace("Attempting to return connection to the pool: {0}", connection);

            if (debug.on())
                debug.log(connection.getConnectionFlow() + ": return to HTTP/1.1 pool");
            connection.closeOrReturnToCache(eof == null ? headers : null);
        }
    }

    void closeWhenFinished() {
        closeWhenFinished = true;
    }

    HttpHeaders responseHeaders() {
        return headers;
    }

    int responseCode() {
        return responseCode;
    }


    void onReadError(Throwable t) {
        Log.logError(t);
        Receiver<?> receiver = receiver(readProgress);
        if (t instanceof EOFException) {
            debug.log(Level.DEBUG, "onReadError: received EOF");
            eof = (EOFException) t;
        }
        CompletableFuture<?> cf = receiver == null ? null : receiver.completion();
        debug.log(Level.DEBUG, () -> "onReadError: cf is "
                + (cf == null  ? "null"
                : (cf.isDone() ? "already completed"
                               : "not yet completed")));
        if (cf != null) {
            cf.completeExceptionally(t);
        } else {
            debug.log(Level.DEBUG, "onReadError", t);
        }
        debug.log(Level.DEBUG, () -> "closing connection: cause is " + t);
        connection.close();
    }


    private State advance(State previous) {
        assert readProgress == previous;
        switch(previous) {
            case READING_HEADERS:
                asyncReceiver.unsubscribe(headersReader);
                return readProgress = State.READING_BODY;
            case READING_BODY:
                asyncReceiver.unsubscribe(bodyReader);
                return readProgress = State.DONE;
            default:
                throw new InternalError("can't advance from " + previous);
        }
    }

    Receiver<?> receiver(State state) {
        return switch (state) {
            case READING_HEADERS    -> headersReader;
            case READING_BODY       -> bodyReader;

            default -> null;
        };

    }

    abstract static class Receiver<T>
            implements Http1AsyncReceiver.Http1AsyncDelegate {
        abstract void start(T parser);
        abstract CompletableFuture<State> completion();
        public abstract boolean tryAsyncReceive(ByteBuffer buffer);
        public abstract void onReadError(Throwable t);
        abstract void handle(ByteBuffer buf, T parser,
                             CompletableFuture<State> cf);
        abstract void reset();

        final boolean accept(ByteBuffer buf, T parser,
                CompletableFuture<State> cf) {
            if (cf == null || parser == null || cf.isDone()) return false;
            handle(buf, parser, cf);
            return !cf.isDone();
        }
        public abstract void onSubscribe(AbstractSubscription s);
        public abstract AbstractSubscription subscription();

    }

    final class HeadersReader extends Receiver<Http1HeaderParser> {
        final Consumer<State> onComplete;
        volatile Http1HeaderParser parser;
        volatile CompletableFuture<State> cf;
        volatile long count; 
        volatile AbstractSubscription subscription;

        HeadersReader(Consumer<State> onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        public AbstractSubscription subscription() {
            return subscription;
        }

        @Override
        public void onSubscribe(AbstractSubscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        void reset() {
            cf = null;
            parser = null;
            count = 0;
            subscription = null;
        }

        @Override
        final void start(Http1HeaderParser hp) {
            count = 0;
            cf = new MinimalFuture<>();
            parser = hp;
        }

        @Override
        CompletableFuture<State> completion() {
            return cf;
        }

        @Override
        public final boolean tryAsyncReceive(ByteBuffer ref) {
            boolean hasDemand = subscription.demand().tryDecrement();
            assert hasDemand;
            boolean needsMore = accept(ref, parser, cf);
            if (needsMore) subscription.request(1);
            return needsMore;
        }

        @Override
        public final void onReadError(Throwable t) {
            t = wrapWithExtraDetail(t, parser::currentStateMessage);
            Http1Response.this.onReadError(t);
        }

        @Override
        final void handle(ByteBuffer b,
                          Http1HeaderParser parser,
                          CompletableFuture<State> cf) {
            assert cf != null : "parsing not started";
            assert parser != null : "no parser";
            try {
                count += b.remaining();
                if (debug.on())
                    debug.log("Sending " + b.remaining() + "/" + b.capacity()
                              + " bytes to header parser");
                if (parser.parse(b)) {
                    count -= b.remaining();
                    if (debug.on())
                        debug.log("Parsing headers completed. bytes=" + count);
                    onComplete.accept(State.READING_HEADERS);
                    cf.complete(State.READING_HEADERS);
                }
            } catch (Throwable t) {
                if (debug.on())
                    debug.log("Header parser failed to handle buffer: " + t);
                cf.completeExceptionally(t);
            }
        }

        @Override
        public void close(Throwable error) {
            if (error != null) {
                CompletableFuture<State> cf = this.cf;
                if (cf != null) {
                    if (debug.on())
                        debug.log("close: completing header parser CF with " + error);
                    cf.completeExceptionally(error);
                }
            }
        }
    }

    final class BodyReader extends Receiver<BodyParser> {
        final Consumer<State> onComplete;
        volatile BodyParser parser;
        volatile CompletableFuture<State> cf;
        volatile AbstractSubscription subscription;
        BodyReader(Consumer<State> onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        void reset() {
            parser = null;
            cf = null;
            subscription = null;
        }

        @Override
        final void start(BodyParser parser) {
            cf = new MinimalFuture<>();
            this.parser = parser;
        }

        @Override
        CompletableFuture<State> completion() {
            return cf;
        }

        @Override
        public final boolean tryAsyncReceive(ByteBuffer b) {
            return accept(b, parser, cf);
        }

        @Override
        public final void onReadError(Throwable t) {
            BodyParser parser = bodyParser;
            if (t instanceof EOFException && parser != null &&
                    parser instanceof UnknownLengthBodyParser ulBodyParser) {
                ulBodyParser.complete();
                return;
            }
            t = wrapWithExtraDetail(t, parser::currentStateMessage);
            parser.onError(t);
            Http1Response.this.onReadError(t);
        }

        @Override
        public AbstractSubscription subscription() {
            return subscription;
        }

        @Override
        public void onSubscribe(AbstractSubscription s) {
            this.subscription = s;
            try {
                parser.onSubscribe(s);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
                throw t;
            }
        }

        @Override
        final void handle(ByteBuffer b,
                          BodyParser parser,
                          CompletableFuture<State> cf) {
            assert cf != null : "parsing not started";
            assert parser != null : "no parser";
            try {
                if (debug.on())
                    debug.log("Sending " + b.remaining() + "/" + b.capacity()
                              + " bytes to body parser");
                parser.accept(b);
            } catch (Throwable t) {
                if (debug.on())
                    debug.log("Body parser failed to handle buffer: " + t);
                if (!cf.isDone()) {
                    cf.completeExceptionally(t);
                }
            }
        }

        final void onComplete(Throwable closedExceptionally) {
            if (cf.isDone()) return;
            if (closedExceptionally != null) {
                cf.completeExceptionally(closedExceptionally);
            } else {
                onComplete.accept(State.READING_BODY);
                cf.complete(State.READING_BODY);
            }
        }

        @Override
        public final void close(Throwable error) {
            CompletableFuture<State> cf = this.cf;
            if (cf != null && !cf.isDone()) {
                if (error != null) {
                    if (debug.on())
                        debug.log("close: completing body parser CF with " + error);
                    cf.completeExceptionally(error);
                } else {
                    if (debug.on())
                        debug.log("close: completing body parser CF");
                    cf.complete(State.READING_BODY);
                }
            }
            if (error != null) {
                BodyParser parser = this.parser;
                if (parser != null) {
                    if (debug.on()) {
                        debug.log("propagating error to parser: " + error);
                    }
                    parser.onError(error);
                } else {
                    if (debug.on()) {
                        debug.log("no parser - error not propagated: " + error);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + "/parser=" + parser;
        }
    }
}
