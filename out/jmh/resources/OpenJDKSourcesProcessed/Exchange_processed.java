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
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLPermission;
import java.security.AccessControlContext;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.common.Log;

import static jdk.internal.net.http.common.Utils.permissionForProxy;

/**
 * One request/response exchange (handles 100/101 intermediate response also).
 * depth field used to track number of times a new request is being sent
 * for a given API request. If limit exceeded exception is thrown.
 *
 * Security check is performed here:
 * - uses AccessControlContext captured at API level
 * - checks for appropriate URLPermission for request
 * - if permission allowed, grants equivalent SocketPermission to call
 * - in case of direct HTTP proxy, checks additionally for access to proxy
 *    (CONNECT proxying uses its own Exchange, so check done there)
 *
 */
final class Exchange<T> {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    final HttpRequestImpl request;
    final HttpClientImpl client;
    volatile ExchangeImpl<T> exchImpl;
    volatile CompletableFuture<? extends ExchangeImpl<T>> exchangeCF;
    volatile CompletableFuture<Void> bodyIgnored;

    private volatile IOException failed;
    @SuppressWarnings("removal")
    final AccessControlContext acc;
    final MultiExchange<T> multi;
    final Executor parentExecutor;
    volatile boolean upgrading; 
    volatile boolean upgraded;  
    final PushGroup<T> pushGroup;
    final String dbgTag;

    final ConnectionAborter connectionAborter = new ConnectionAborter();

    Exchange(HttpRequestImpl request, MultiExchange<T> multi) {
        this.request = request;
        this.upgrading = false;
        this.client = multi.client();
        this.multi = multi;
        this.acc = multi.acc;
        this.parentExecutor = multi.executor;
        this.pushGroup = multi.pushGroup;
        this.dbgTag = "Exchange";
    }

    /* If different AccessControlContext to be used  */
    Exchange(HttpRequestImpl request,
             MultiExchange<T> multi,
             @SuppressWarnings("removal") AccessControlContext acc)
    {
        this.request = request;
        this.acc = acc;
        this.upgrading = false;
        this.client = multi.client();
        this.multi = multi;
        this.parentExecutor = multi.executor;
        this.pushGroup = multi.pushGroup;
        this.dbgTag = "Exchange";
    }

    PushGroup<T> getPushGroup() {
        return pushGroup;
    }

    Executor executor() {
        return parentExecutor;
    }

    public HttpRequestImpl request() {
        return request;
    }

    public Optional<Duration> remainingConnectTimeout() {
        return multi.remainingConnectTimeout();
    }

    HttpClientImpl client() {
        return client;
    }

    static final class ConnectionAborter {
        private volatile HttpConnection connection;
        private volatile boolean closeRequested;
        private volatile Throwable cause;

        void connection(HttpConnection connection) {
            boolean closeRequested;
            synchronized (this) {
                closeRequested = this.closeRequested;
                if (!closeRequested) {
                    this.connection = connection;
                } else {
                    this.closeRequested = false;
                }
            }
            if (closeRequested) closeConnection(connection, cause);
        }

        void closeConnection(Throwable error) {
            HttpConnection connection;
            Throwable cause;
            synchronized (this) {
                cause = this.cause;
                if (cause == null) {
                    cause = error;
                }
                connection = this.connection;
                if (connection == null) {
                    closeRequested = true;
                    this.cause = cause;
                } else {
                    this.connection = null;
                    this.cause = null;
                }
            }
            closeConnection(connection, cause);
        }

        HttpConnection disable() {
            HttpConnection connection;
            synchronized (this) {
                connection = this.connection;
                this.connection = null;
                this.closeRequested = false;
                this.cause = null;
            }
            return connection;
        }

        private static void closeConnection(HttpConnection connection, Throwable cause) {
            if (connection != null) {
                try {
                    connection.close(cause);
                } catch (Throwable t) {
                }
            }
        }
    }

    void nullBody(HttpResponse<T> resp, Throwable t) {
        exchImpl.nullBody(resp, t);
    }

    public CompletableFuture<T> readBodyAsync(HttpResponse.BodyHandler<T> handler) {
        if (bodyIgnored != null) return MinimalFuture.completedFuture(null);

        return exchImpl.readBodyAsync(handler, !request.isWebSocket(), parentExecutor)
                .whenComplete((r,t) -> exchImpl.completed());
    }

    /**
     * Called after a redirect or similar kind of retry where a body might
     * be sent but we don't want it. Should send a RESET in h2. For http/1.1
     * we can consume small quantity of data, or close the connection in
     * other cases.
     */
    public CompletableFuture<Void> ignoreBody() {
        if (bodyIgnored != null) return bodyIgnored;
        return exchImpl.ignoreBody();
    }

    /**
     * Called when a new exchange is created to replace this exchange.
     * At this point it is guaranteed that readBody/readBodyAsync will
     * not be called.
     */
    public void released() {
        ExchangeImpl<?> impl = exchImpl;
        if (impl != null) impl.released();
    }

    public void cancel() {
        if (exchImpl != null) {
            exchImpl.cancel();
        } else {
            cancel(new IOException("Request cancelled"));
        }
    }

    public void cancel(IOException cause) {
        if (debug.on()) debug.log("cancel exchImpl: %s, with \"%s\"", exchImpl, cause);
        ExchangeImpl<?> impl = exchImpl;
        if (impl != null) {
            if (debug.on()) debug.log("Cancelling exchImpl: %s", exchImpl);
            impl.cancel(cause);
        } else {
            IOException failed = this.failed;
            if (failed == null) {
                synchronized (this) {
                    failed = this.failed;
                    if (failed == null) {
                        failed = this.failed = cause;
                    }
                }
            }

            connectionAborter.closeConnection(failed);

            checkCancelled();
        }
    }

    private void checkCancelled() {
        ExchangeImpl<?> impl = null;
        IOException cause = null;
        CompletableFuture<? extends ExchangeImpl<T>> cf = null;
        if (failed != null) {
            synchronized (this) {
                cause = failed;
                impl = exchImpl;
                cf = exchangeCF;
            }
        }
        if (cause == null) return;
        if (impl != null) {
            if (debug.on()) debug.log("Cancelling exchImpl: %s", impl);
            impl.cancel(cause);
            failed = null;
        } else {
            Log.logTrace("Exchange: request [{0}/timeout={1}ms] no impl is set."
                         + "\n\tCan''t cancel yet with {2}",
                         request.uri(),
                         request.timeout().isPresent() ?
                         (request.timeout().get().getSeconds() * 1000
                          + request.timeout().get().getNano() / 1000000) : -1,
                         cause);
            if (cf != null) cf.completeExceptionally(cause);
        }
    }

    <T> CompletableFuture<T> checkCancelled(CompletableFuture<T> cf, HttpConnection connection) {
        return cf.handle((r,t) -> {
            if (t == null) {
                if (multi.requestCancelled()) {
                    if (!upgraded) {
                        t = getCancelCause();
                        if (t == null) t = new IOException("Request cancelled");
                        if (debug.on()) debug.log("exchange cancelled during connect: " + t);
                        try {
                            connection.close();
                        } catch (Throwable x) {
                            if (debug.on()) debug.log("Failed to close connection", x);
                        }
                        return MinimalFuture.<T>failedFuture(t);
                    }
                }
            }
            return cf;
        }).thenCompose(Function.identity());
    }

    public void h2Upgrade() {
        upgrading = true;
        request.setH2Upgrade(client.client2());
    }

    synchronized IOException getCancelCause() {
        return failed;
    }

    private CompletableFuture<? extends ExchangeImpl<T>>
    establishExchange(HttpConnection connection) {
        if (debug.on()) {
            debug.log("establishing exchange for %s,%n\t proxy=%s",
                      request, request.proxy());
        }
        Throwable t = getCancelCause();
        checkCancelled();
        if (t != null) {
            if (debug.on()) {
                debug.log("exchange was cancelled: returned failed cf (%s)", String.valueOf(t));
            }
            return exchangeCF = MinimalFuture.failedFuture(t);
        }

        CompletableFuture<? extends ExchangeImpl<T>> cf, res;
        cf = ExchangeImpl.get(this, connection);
        synchronized (this) { exchangeCF = cf; };
        res = cf.whenComplete((r,x) -> {
            synchronized (Exchange.this) {
                if (exchangeCF == cf) exchangeCF = null;
            }
        });
        checkCancelled();
        return res.thenCompose((eimpl) -> {
                    exchImpl = eimpl;
                    IOException tt = getCancelCause();
                    checkCancelled();
                    if (tt != null) {
                        return MinimalFuture.failedFuture(tt);
                    } else {
                        return MinimalFuture.completedFuture(eimpl);
                    } });
    }


    public CompletableFuture<Response> responseAsync() {
        return responseAsyncImpl(null);
    }

    CompletableFuture<Response> responseAsyncImpl(HttpConnection connection) {
        SecurityException e = checkPermissions();
        if (e != null) {
            return MinimalFuture.failedFuture(e);
        } else {
            return responseAsyncImpl0(connection);
        }
    }

    private CompletableFuture<Response> checkFor407(ExchangeImpl<T> ex, Throwable t,
                                                    Function<ExchangeImpl<T>,CompletableFuture<Response>> andThen) {
        t = Utils.getCompletionCause(t);
        if (t instanceof ProxyAuthenticationRequired) {
            if (debug.on()) debug.log("checkFor407: ProxyAuthenticationRequired: building synthetic response");
            bodyIgnored = MinimalFuture.completedFuture(null);
            Response proxyResponse = ((ProxyAuthenticationRequired)t).proxyResponse;
            HttpConnection c = ex == null ? null : ex.connection();
            Response syntheticResponse = new Response(request, this,
                    proxyResponse.headers, c, proxyResponse.statusCode,
                    proxyResponse.version, true);
            return MinimalFuture.completedFuture(syntheticResponse);
        } else if (t != null) {
            if (debug.on()) debug.log("checkFor407: no response - %s", (Object)t);
            return MinimalFuture.failedFuture(t);
        } else {
            if (debug.on()) debug.log("checkFor407: all clear");
            return andThen.apply(ex);
        }
    }

    private CompletableFuture<Response> expectContinue(ExchangeImpl<T> ex) {
        assert request.expectContinue();
        return ex.getResponseAsync(parentExecutor)
                .thenCompose((Response r1) -> {
            Log.logResponse(r1::toString);
            int rcode = r1.statusCode();
            if (rcode == 100) {
                Log.logTrace("Received 100-Continue: sending body");
                if (debug.on()) debug.log("Received 100-Continue for %s", r1);
                CompletableFuture<Response> cf =
                        exchImpl.sendBodyAsync()
                                .thenCompose(exIm -> exIm.getResponseAsync(parentExecutor));
                cf = wrapForUpgrade(cf);
                cf = wrapForLog(cf);
                return cf;
            } else {
                Log.logTrace("Expectation failed: Received {0}",
                        rcode);
                if (debug.on()) debug.log("Expect-Continue failed (%d) for: %s", rcode, r1);
                if (upgrading && rcode == 101) {
                    IOException failed = new IOException(
                            "Unable to handle 101 while waiting for 100");
                    return MinimalFuture.failedFuture(failed);
                }
                exchImpl.expectContinueFailed(rcode);
                return MinimalFuture.completedFuture(r1);
            }
        });
    }

    private CompletableFuture<Response> sendRequestBody(ExchangeImpl<T> ex) {
        assert !request.expectContinue();
        if (debug.on()) debug.log("sendRequestBody");
        CompletableFuture<Response> cf = ex.sendBodyAsync()
                .thenCompose(exIm -> exIm.getResponseAsync(parentExecutor));
        cf = wrapForUpgrade(cf);
        cf = cf.thenCompose(this::ignore1xxResponse);
        cf = wrapForLog(cf);
        return cf;
    }

    /**
     * Checks whether the passed Response has a status code between 102 and 199 (both inclusive).
     * If so, then that {@code Response} is considered intermediate informational response and is
     * ignored by the client. This method then creates a new {@link CompletableFuture} which
     * completes when a subsequent response is sent by the server. Such newly constructed
     * {@link CompletableFuture} will not complete till a "final" response (one which doesn't have
     * a response code between 102 and 199 inclusive) is sent by the server. The returned
     * {@link CompletableFuture} is thus capable of handling multiple subsequent intermediate
     * informational responses from the server.
     * <p>
     * If the passed Response doesn't have a status code between 102 and 199 (both inclusive) then
     * this method immediately returns back a completed {@link CompletableFuture} with the passed
     * {@code Response}.
     * </p>
     *
     * @param rsp The response
     * @return A {@code CompletableFuture} with the final response from the server
     */
    private CompletableFuture<Response> ignore1xxResponse(final Response rsp) {
        final int statusCode = rsp.statusCode();
        if ((statusCode >= 102 && statusCode <= 199)
                || (statusCode == 100 && !request.expectContinue)) {
            Log.logTrace("Ignoring (1xx informational) response code {0}", rsp.statusCode());
            if (debug.on()) {
                debug.log("Ignoring (1xx informational) response code "
                        + rsp.statusCode());
            }
            assert exchImpl != null : "Illegal state - current exchange isn't set";
            final CompletableFuture<Response> cf = exchImpl.getResponseAsync(parentExecutor);
            return cf.thenCompose(this::ignore1xxResponse);
        } else {
            return MinimalFuture.completedFuture(rsp);
        }
    }

    CompletableFuture<Response> responseAsyncImpl0(HttpConnection connection) {
        Function<ExchangeImpl<T>, CompletableFuture<Response>> after407Check;
        bodyIgnored = null;
        if (request.expectContinue()) {
            request.setSystemHeader("Expect", "100-Continue");
            Log.logTrace("Sending Expect: 100-Continue");
            after407Check = this::expectContinue;
        } else {
            after407Check = this::sendRequestBody;
        }
        Function<ExchangeImpl<T>, CompletableFuture<Response>> afterExch407Check =
                (ex) -> ex.sendHeadersAsync()
                        .handle((r,t) -> this.checkFor407(r, t, after407Check))
                        .thenCompose(Function.identity());
        return establishExchange(connection)
                .handle((r,t) -> this.checkFor407(r,t, afterExch407Check))
                .thenCompose(Function.identity());
    }

    private CompletableFuture<Response> wrapForUpgrade(CompletableFuture<Response> cf) {
        if (upgrading) {
            return cf.thenCompose(r -> checkForUpgradeAsync(r, exchImpl));
        }
        if (request.isWebSocket()) {
            return cf;
        }
        return cf.thenCompose(r -> {
            if (r.statusCode == 101) {
                final ProtocolException protoEx = new ProtocolException("Unexpected 101 " +
                        "response, when not upgrading");
                assert exchImpl != null : "Illegal state - current exchange isn't set";
                try {
                    exchImpl.onProtocolError(protoEx);
                } catch (Throwable ignore){
                }
                return MinimalFuture.failedFuture(protoEx);
            }
            return MinimalFuture.completedFuture(r);
        });
    }

    private CompletableFuture<Response> wrapForLog(CompletableFuture<Response> cf) {
        if (Log.requests()) {
            return cf.thenApply(response -> {
                Log.logResponse(response::toString);
                return response;
            });
        }
        return cf;
    }

    HttpResponse.BodySubscriber<T> ignoreBody(HttpResponse.ResponseInfo hdrs) {
        return HttpResponse.BodySubscribers.replacing(null);
    }


    private CompletableFuture<Response>
    checkForUpgradeAsync(Response resp,
                         ExchangeImpl<T> ex) {

        int rcode = resp.statusCode();
        if (upgrading && (rcode == 101)) {
            Http1Exchange<T> e = (Http1Exchange<T>)ex;
            if (debug.on()) debug.log("Upgrading async %s", e.connection());
            return e.readBodyAsync(this::ignoreBody, false, parentExecutor)
                .thenCompose((T v) -> {
                    debug.log("Ignored body");
                    ex.upgraded();
                    upgraded = true;
                    return Http2Connection.createAsync(e.connection(),
                                                 client.client2(),
                                                 this, e::drainLeftOverBytes)
                        .thenCompose((Http2Connection c) -> {
                            HttpConnection connection = connectionAborter.disable();
                            boolean cached = c.offerConnection();
                            if (!cached && connection != null) {
                                connectionAborter.connection(connection);
                            }
                            Stream<T> s = c.getInitialStream();

                            if (s == null) {
                                Throwable t = c.getRecordedCause();
                                IOException ioe;
                                if (t != null) {
                                    if (!cached)
                                        c.close();
                                    ioe = new IOException("Can't get stream 1: " + t, t);
                                } else {
                                    ioe = new IOException("Can't get stream 1");
                                }
                                return MinimalFuture.failedFuture(ioe);
                            }
                            exchImpl.released();
                            Throwable t;
                            synchronized (this) {
                                exchImpl = s;
                                t = failed;
                            }
                            if (t == null) t = e.getCancelCause();
                            if (t instanceof HttpTimeoutException || multi.requestCancelled()) {
                                if (t == null) t = new IOException("Request cancelled");
                                s.cancelImpl(t);
                                return MinimalFuture.failedFuture(t);
                            }
                            if (debug.on())
                                debug.log("Getting response async %s", s);
                            return s.getResponseAsync(null);
                        });}
                );
        }
        return MinimalFuture.completedFuture(resp);
    }

    private URI getURIForSecurityCheck() {
        URI u;
        String method = request.method();
        InetSocketAddress authority = request.authority();
        URI uri = request.uri();

        if (method.equalsIgnoreCase("CONNECT")) {
            try {
                u = new URI("socket",
                             null,
                             authority.getHostString(),
                             authority.getPort(),
                             null,
                             null,
                             null);
            } catch (URISyntaxException e) {
                throw new InternalError(e); 
            }
        } else {
            u = uri;
        }
        return u;
    }

    /**
     * Returns the security permission required for the given details.
     * If method is CONNECT, then uri must be of form "scheme:
     */
    private static URLPermission permissionForServer(URI uri,
                                                     String method,
                                                     Map<String, List<String>> headers) {
        if (method.equals("CONNECT")) {
            return new URLPermission(uri.toString(), "CONNECT");
        } else {
            return Utils.permissionForServer(uri, method, headers.keySet().stream());
        }
    }

    /**
     * Performs the necessary security permission checks required to retrieve
     * the response. Returns a security exception representing the denied
     * permission, or null if all checks pass or there is no security manager.
     */
    private SecurityException checkPermissions() {
        String method = request.method();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || method.equals("CONNECT")) {
            return null;
        }

        HttpHeaders userHeaders = request.getUserHeaders();
        URI u = getURIForSecurityCheck();
        URLPermission p = permissionForServer(u, method, userHeaders.map());

        try {
            assert acc != null;
            sm.checkPermission(p, acc);
        } catch (SecurityException e) {
            return e;
        }
        String hostHeader = userHeaders.firstValue("Host").orElse(null);
        if (hostHeader != null && !hostHeader.equalsIgnoreCase(u.getHost())) {
            URI u1 = replaceHostInURI(u, hostHeader);
            URLPermission p1 = permissionForServer(u1, method, userHeaders.map());
            try {
                assert acc != null;
                sm.checkPermission(p1, acc);
            } catch (SecurityException e) {
                return e;
            }
        }
        ProxySelector ps = client.proxySelector();
        if (ps != null) {
            if (!method.equals("CONNECT")) {
                URLPermission proxyPerm = permissionForProxy(request.proxy());
                if (proxyPerm != null) {
                    try {
                        sm.checkPermission(proxyPerm, acc);
                    } catch (SecurityException e) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    private static URI replaceHostInURI(URI u, String hostPort) {
        StringBuilder sb = new StringBuilder();
        sb.append(u.getScheme())
                .append(":
                .append(hostPort)
                .append(u.getRawPath());
        return URI.create(sb.toString());
    }

    HttpClient.Version version() {
        return multi.version();
    }

    String dbgString() {
        return dbgTag;
    }
}
