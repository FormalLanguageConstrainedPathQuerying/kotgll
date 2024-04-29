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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.time.Duration;
import java.security.AccessControlContext;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.HttpTimeoutException;
import jdk.internal.net.http.common.Cancelable;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.ConnectionExpiredException;
import jdk.internal.net.http.common.Utils;
import static jdk.internal.net.http.common.MinimalFuture.completedFuture;
import static jdk.internal.net.http.common.MinimalFuture.failedFuture;

/**
 * Encapsulates multiple Exchanges belonging to one HttpRequestImpl.
 * - manages filters
 * - retries due to filters.
 * - I/O errors and most other exceptions get returned directly to user
 *
 * Creates a new Exchange for each request/response interaction
 */
class MultiExchange<T> implements Cancelable {

    static final Logger debug =
            Utils.getDebugLogger("MultiExchange"::toString, Utils.DEBUG);

    private final HttpRequest userRequest; 
    private final HttpRequestImpl request; 
    private final ConnectTimeoutTracker connectTimeout; 
    @SuppressWarnings("removal")
    final AccessControlContext acc;
    final HttpClientImpl client;
    final HttpResponse.BodyHandler<T> responseHandler;
    final HttpClientImpl.DelegatingExecutor executor;
    final AtomicInteger attempts = new AtomicInteger();
    HttpRequestImpl currentreq; 
    HttpRequestImpl previousreq; 
    Exchange<T> exchange; 
    Exchange<T> previous;
    volatile Throwable retryCause;
    volatile boolean expiredOnce;
    volatile HttpResponse<T> response;

    static final int DEFAULT_MAX_ATTEMPTS = 5;
    static final int max_attempts = Utils.getIntegerNetProperty(
            "jdk.httpclient.redirects.retrylimit", DEFAULT_MAX_ATTEMPTS
    );

    private final List<HeaderFilter> filters;
    volatile ResponseTimerEvent responseTimerEvent;
    volatile boolean cancelled;
    AtomicReference<CancellationException> interrupted = new AtomicReference<>();
    final PushGroup<T> pushGroup;

    /**
     * Filter fields. These are attached as required by filters
     * and only used by the filter implementations. This could be
     * generalised into Objects that are passed explicitly to the filters
     * (one per MultiExchange object, and one per Exchange object possibly)
     */
    volatile AuthenticationFilter.AuthInfo serverauth, proxyauth;
    volatile int numberOfRedirects = 0;

    private static final class ConnectTimeoutTracker {
        final Duration max;
        final AtomicLong startTime = new AtomicLong();
        ConnectTimeoutTracker(Duration connectTimeout) {
            this.max = Objects.requireNonNull(connectTimeout);
        }

        Duration getRemaining() {
            long now = System.nanoTime();
            long previous = startTime.compareAndExchange(0, now);
            if (previous == 0 || max.isZero()) return max;
            Duration remaining = max.minus(Duration.ofNanos(now - previous));
            assert remaining.compareTo(max) <= 0;
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }

        void reset() { startTime.set(0); }
    }

    /**
     * MultiExchange with one final response.
     */
    MultiExchange(HttpRequest userRequest,
                  HttpRequestImpl requestImpl,
                  HttpClientImpl client,
                  HttpResponse.BodyHandler<T> responseHandler,
                  PushPromiseHandler<T> pushPromiseHandler,
                  @SuppressWarnings("removal") AccessControlContext acc) {
        this.previous = null;
        this.userRequest = userRequest;
        this.request = requestImpl;
        this.currentreq = request;
        this.previousreq = null;
        this.client = client;
        this.filters = client.filterChain();
        this.acc = acc;
        this.executor = client.theExecutor();
        this.responseHandler = responseHandler;

        if (pushPromiseHandler != null) {
            Executor ensureExecutedAsync = this.executor::ensureExecutedAsync;
            Executor executor = acc == null
                    ? ensureExecutedAsync
                    : new PrivilegedExecutor(ensureExecutedAsync, acc);
            this.pushGroup = new PushGroup<>(pushPromiseHandler, request, executor);
        } else {
            pushGroup = null;
        }
        this.connectTimeout = client.connectTimeout()
                .map(ConnectTimeoutTracker::new).orElse(null);
        this.exchange = new Exchange<>(request, this);
    }

    static final class CancelableRef implements Cancelable {
        private final WeakReference<Cancelable> cancelableRef;
        CancelableRef(Cancelable cancelable) {
            cancelableRef = new WeakReference<>(cancelable);
        }
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Cancelable cancelable = cancelableRef.get();
            if (cancelable != null) {
                return cancelable.cancel(mayInterruptIfRunning);
            } else return false;
        }
    }

    synchronized Exchange<T> getExchange() {
        return exchange;
    }

    HttpClientImpl client() {
        return client;
    }

    HttpClient.Version version() {
        HttpClient.Version vers = request.version().orElse(client.version());
        if (vers == HttpClient.Version.HTTP_2 && !request.secure() && request.proxy() != null)
            vers = HttpClient.Version.HTTP_1_1;
        return vers;
    }

    private void setExchange(Exchange<T> exchange) {
        Exchange<T> previousExchange;
        synchronized (this) {
            previousExchange = this.exchange;
            this.exchange = exchange;
        }
        if (previousExchange != null && exchange != previousExchange) {
            previousExchange.released();
        }
        if (cancelled) exchange.cancel();
    }

    public Optional<Duration> remainingConnectTimeout() {
        return Optional.ofNullable(connectTimeout)
                .map(ConnectTimeoutTracker::getRemaining);
    }

    private void cancelTimer() {
        if (responseTimerEvent != null) {
            client.cancelTimer(responseTimerEvent);
            responseTimerEvent = null;
        }
    }

    private void requestFilters(HttpRequestImpl r) throws IOException {
        Log.logTrace("Applying request filters");
        for (HeaderFilter filter : filters) {
            Log.logTrace("Applying {0}", filter);
            filter.request(r, this);
        }
        Log.logTrace("All filters applied");
    }

    private HttpRequestImpl responseFilters(Response response) throws IOException
    {
        Log.logTrace("Applying response filters");
        ListIterator<HeaderFilter> reverseItr = filters.listIterator(filters.size());
        while (reverseItr.hasPrevious()) {
            HeaderFilter filter = reverseItr.previous();
            Log.logTrace("Applying {0}", filter);
            HttpRequestImpl newreq = filter.response(response);
            if (newreq != null) {
                Log.logTrace("New request: stopping filters");
                return newreq;
            }
        }
        Log.logTrace("All filters applied");
        return null;
    }

    public void cancel(IOException cause) {
        cancelled = true;
        getExchange().cancel(cause);
    }

    /**
     * Used to relay a call from {@link CompletableFuture#cancel(boolean)}
     * to this multi exchange for the purpose of cancelling the
     * HTTP exchange.
     * @param mayInterruptIfRunning if true, and this exchange is not already
     *        cancelled, this method will attempt to interrupt and cancel the
     *        exchange. Otherwise, the exchange is allowed to proceed and this
     *        method does nothing.
     * @return true if the exchange was cancelled, false otherwise.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = this.cancelled;
        boolean firstCancel = false;
        if (!cancelled && mayInterruptIfRunning) {
            if (interrupted.get() == null) {
                firstCancel = interrupted.compareAndSet(null,
                        new CancellationException("Request cancelled"));
            }
            if (debug.on()) {
                if (firstCancel) {
                    debug.log("multi exchange recording: " + interrupted.get());
                } else {
                    debug.log("multi exchange recorded: " + interrupted.get());
                }
            }
            this.cancelled = true;
            var exchange = getExchange();
            if (exchange != null) {
                exchange.cancel();
            }
            return true;
        } else {
            if (cancelled) {
                debug.log("multi exchange already cancelled: " + interrupted.get());
            } else {
                debug.log("multi exchange mayInterruptIfRunning=" + mayInterruptIfRunning);
            }
        }
        return false;
    }

    public <U> MinimalFuture<U> newMinimalFuture() {
        return new MinimalFuture<>(new CancelableRef(this));
    }

    public CompletableFuture<HttpResponse<T>> responseAsync(Executor executor) {
        CompletableFuture<Void> start = newMinimalFuture();
        CompletableFuture<HttpResponse<T>> cf = responseAsync0(start);
        start.completeAsync( () -> null, executor); 
        return cf;
    }


    private static boolean bodyNotPermitted(Response r) {
        return r.statusCode == 204;
    }

    private boolean bodyIsPresent(Response r) {
        HttpHeaders headers = r.headers();
        if (headers.firstValueAsLong("Content-length").orElse(0L) != 0L)
            return true;
        if (headers.firstValue("Transfer-encoding").isPresent())
            return true;
        return false;
    }


    private CompletableFuture<HttpResponse<T>> handleNoBody(Response r, Exchange<T> exch) {
        BodySubscriber<T> bs = responseHandler.apply(new ResponseInfoImpl(r.statusCode(),
                r.headers(), r.version()));
        bs.onSubscribe(new NullSubscription());
        bs.onComplete();
        CompletionStage<T> cs = ResponseSubscribers.getBodyAsync(executor, bs);
        MinimalFuture<HttpResponse<T>> result = new MinimalFuture<>();
        cs.whenComplete((nullBody, exception) -> {
            if (exception != null)
                result.completeExceptionally(exception);
            else {
                this.response =
                        new HttpResponseImpl<>(r.request(), r, this.response, nullBody, exch);
                result.complete(this.response);
            }
        });
        return result.whenComplete(exch::nullBody);
    }

    private CompletableFuture<HttpResponse<T>>
    responseAsync0(CompletableFuture<Void> start) {
        return start.thenCompose( v -> responseAsyncImpl())
                    .thenCompose((Response r) -> {
                        Exchange<T> exch = getExchange();
                        if (bodyNotPermitted(r)) {
                            if (bodyIsPresent(r)) {
                                IOException ioe = new IOException(
                                    "unexpected content length header with 204 response");
                                exch.cancel(ioe);
                                return MinimalFuture.failedFuture(ioe);
                            } else
                                return handleNoBody(r, exch);
                        }
                        return exch.readBodyAsync(responseHandler)
                            .thenApply((T body) -> {
                                this.response =
                                    new HttpResponseImpl<>(r.request(), r, this.response, body, exch);
                                return this.response;
                            });
                    }).exceptionallyCompose(this::whenCancelled);
    }

    private Throwable wrapIfCancelled(Throwable cause) {
        CancellationException interrupt = interrupted.get();
        if (interrupt == null) return cause;

        var cancel = new CancellationException(interrupt.getMessage());
        cancel.setStackTrace(interrupt.getStackTrace());
        cancel.initCause(Utils.getCancelCause(cause));
        return cancel;
    }

    private CompletableFuture<HttpResponse<T>> whenCancelled(Throwable t) {
        var x = wrapIfCancelled(t);
        if (x instanceof CancellationException) {
            if (debug.on()) {
                debug.log("MultiExchange interrupted with: " + x.getCause());
            }
        }
        return MinimalFuture.failedFuture(x);
    }

    static class NullSubscription implements Flow.Subscription {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }

    private CompletableFuture<Response> responseAsyncImpl() {
        CompletableFuture<Response> cf;
        if (attempts.incrementAndGet() > max_attempts) {
            cf = failedFuture(new IOException("Too many retries", retryCause));
        } else {
            if (currentreq.timeout().isPresent()) {
                responseTimerEvent = ResponseTimerEvent.of(this);
                client.registerTimer(responseTimerEvent);
            }
            try {
                if (currentreq != previousreq) {
                    requestFilters(currentreq);
                }
            } catch (IOException e) {
                return failedFuture(e);
            }
            Exchange<T> exch = getExchange();
            cf = exch.responseAsync()
                     .thenCompose((Response response) -> {
                        HttpRequestImpl newrequest;
                        try {
                            newrequest = responseFilters(response);
                        } catch (IOException e) {
                            return failedFuture(e);
                        }
                        if (newrequest == null) {
                            if (attempts.get() > 1) {
                                Log.logError("Succeeded on attempt: " + attempts);
                            }
                            return completedFuture(response);
                        } else {
                            cancelTimer();
                            this.response =
                                new HttpResponseImpl<>(currentreq, response, this.response, null, exch);
                            Exchange<T> oldExch = exch;
                            if (currentreq.isWebSocket()) {
                                exch.exchImpl.connection().close();
                            }
                            return exch.ignoreBody().handle((r,t) -> {
                                previousreq = currentreq;
                                currentreq = newrequest;
                                expiredOnce = false;
                                setExchange(new Exchange<>(currentreq, this, acc));
                                return responseAsyncImpl();
                            }).thenCompose(Function.identity());
                        } })
                     .handle((response, ex) -> {
                        cancelTimer();
                        if (ex == null) {
                            assert response != null;
                            return completedFuture(response);
                        }
                        CompletableFuture<Response> errorCF = getExceptionalCF(ex);
                        if (errorCF == null) {
                            return responseAsyncImpl();
                        } else {
                            return errorCF;
                        } })
                     .thenCompose(Function.identity());
        }
        return cf;
    }

    private static boolean retryPostValue() {
        String s = Utils.getNetProperty("jdk.httpclient.enableAllMethodRetry");
        if (s == null)
            return false;
        return s.isEmpty() ? true : Boolean.parseBoolean(s);
    }

    private static boolean disableRetryConnect() {
        String s = Utils.getNetProperty("jdk.httpclient.disableRetryConnect");
        if (s == null)
            return false;
        return s.isEmpty() ? true : Boolean.parseBoolean(s);
    }

    /** True if ALL ( even non-idempotent ) requests can be automatic retried. */
    private static final boolean RETRY_ALWAYS = retryPostValue();
    /** True if ConnectException should cause a retry. Enabled by default */
    static final boolean RETRY_CONNECT = !disableRetryConnect();

    /** Returns true is given request has an idempotent method. */
    private static boolean isIdempotentRequest(HttpRequest request) {
        String method = request.method();
        return switch (method) {
            case "GET", "HEAD" -> true;
            default -> false;
        };
    }

    /** Returns true if the given request can be automatically retried. */
    private static boolean canRetryRequest(HttpRequest request) {
        if (RETRY_ALWAYS)
            return true;
        if (isIdempotentRequest(request))
            return true;
        return false;
    }

    boolean requestCancelled() {
        return interrupted.get() != null;
    }

    private boolean retryOnFailure(Throwable t) {
        if (requestCancelled()) return false;
        return t instanceof ConnectionExpiredException
                || (RETRY_CONNECT && (t instanceof ConnectException));
    }

    private Throwable retryCause(Throwable t) {
        Throwable cause = t instanceof ConnectionExpiredException ? t.getCause() : t;
        return cause == null ? t : cause;
    }

    /**
     * Takes a Throwable and returns a suitable CompletableFuture that is
     * completed exceptionally, or null.
     */
    private CompletableFuture<Response> getExceptionalCF(Throwable t) {
        if ((t instanceof CompletionException) || (t instanceof ExecutionException)) {
            if (t.getCause() != null) {
                t = t.getCause();
            }
        }
        if (cancelled && !requestCancelled() && t instanceof IOException) {
            if (!(t instanceof HttpTimeoutException)) {
                t = toTimeoutException((IOException)t);
            }
        } else if (retryOnFailure(t)) {
            Throwable cause = retryCause(t);

            if (!(t instanceof ConnectException)) {
                if (connectTimeout != null) connectTimeout.reset();
                if (!canRetryRequest(currentreq)) {
                    return failedFuture(cause); 
                }
            } 

            retryCause = cause;
            if (!expiredOnce) {
                if (debug.on()) {
                    debug.log(t.getClass().getSimpleName()
                            + " (async): retrying due to: ", t);
                }
                expiredOnce = true;
                previousreq = currentreq;
                return null;
            } else {
                if (debug.on()) {
                    debug.log(t.getClass().getSimpleName()
                            + " (async): already retried once.", t);
                }
                t = cause;
            }
        }
        return failedFuture(t);
    }

    private HttpTimeoutException toTimeoutException(IOException ioe) {
        HttpTimeoutException t = null;

        Exchange<?> exchange = getExchange();
        if (exchange != null) {
            ExchangeImpl<?> exchangeImpl = exchange.exchImpl;
            if (exchangeImpl != null) {
                if (exchangeImpl.connection().connected()) {
                    t = new HttpTimeoutException("request timed out");
                    t.initCause(ioe);
                }
            }
        }
        if (t == null) {
            t = new HttpConnectTimeoutException("HTTP connect timed out");
            t.initCause(new ConnectException("HTTP connect timed out"));
        }
        return t;
    }
}
