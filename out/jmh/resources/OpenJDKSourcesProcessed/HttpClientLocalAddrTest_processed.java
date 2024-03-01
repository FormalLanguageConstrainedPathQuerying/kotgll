/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import jdk.test.lib.net.IPSupport;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jdk.httpclient.test.lib.common.HttpServerAdapters;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;

/*
 * @test
 * @summary Tests HttpClient usage when configured with a local address to bind
 *          to, when sending requests
 * @bug 8209137 8316031
 * @library /test/lib /test/jdk/java/net/httpclient/lib
 *
 * @build jdk.test.lib.net.SimpleSSLContext jdk.test.lib.net.IPSupport
 *        jdk.httpclient.test.lib.common.HttpServerAdapters
 *
 * @run testng/othervm
 *      -Djdk.httpclient.HttpClient.log=frames,ssl,requests,responses,errors
 *      -Djdk.internal.httpclient.debug=true
 *      -Dsun.net.httpserver.idleInterval=50000
 *      -Djdk.tracePinnedThreads=full
 *      HttpClientLocalAddrTest
 *
 * @run testng/othervm/java.security.policy=httpclient-localaddr-security.policy
 *      -Djdk.httpclient.HttpClient.log=frames,ssl,requests,responses,errors
 *      -Djdk.internal.httpclient.debug=true
 *      -Dsun.net.httpserver.idleInterval=50000
 *      -Djdk.tracePinnedThreads=full
 *      HttpClientLocalAddrTest
 *
 */
public class HttpClientLocalAddrTest implements HttpServerAdapters {

    private static SSLContext sslContext;
    private static HttpServerAdapters.HttpTestServer http1_1_Server;
    private static URI httpURI;
    private static HttpServerAdapters.HttpTestServer https_1_1_Server;
    private static URI httpsURI;
    private static HttpServerAdapters.HttpTestServer http2_Server;
    private static URI http2URI;
    private static HttpServerAdapters.HttpTestServer https2_Server;
    private static URI https2URI;
    private static final AtomicInteger IDS = new AtomicInteger();

    @BeforeClass
    public static void beforeClass() throws Exception {
        sslContext = new SimpleSSLContext().get();
        Assert.assertNotNull(sslContext, "Unexpected null sslContext");

        HttpServerAdapters.HttpTestHandler handler = (exchange) -> {
            var clientAddr = exchange.getRemoteAddress();
            System.out.println("Received a request from client address " + clientAddr);
            var responseContent = clientAddr.getAddress().getAddress();
            exchange.sendResponseHeaders(200, responseContent.length);
            try (var os = exchange.getResponseBody()) {
                os.write(responseContent);
            }
            exchange.close();
        };

        http1_1_Server = HttpServerAdapters.HttpTestServer.create(HTTP_1_1);
        http1_1_Server.addHandler(handler, "/");
        http1_1_Server.start();
        System.out.println("Started HTTP v1.1 server at " + http1_1_Server.serverAuthority());
        httpURI = new URI("http:

        https_1_1_Server = HttpServerAdapters.HttpTestServer.create(HTTP_1_1, sslContext);
        https_1_1_Server.addHandler(handler, "/");
        https_1_1_Server.start();
        System.out.println("Started HTTPS v1.1 server at " + https_1_1_Server.serverAuthority());
        httpsURI = new URI("https:

        http2_Server = HttpServerAdapters.HttpTestServer.create(HTTP_2);
        http2_Server.addHandler(handler, "/");
        http2_Server.start();
        System.out.println("Started HTTP v2 server at " + http2_Server.serverAuthority());
        http2URI = new URI("http:

        https2_Server = HttpServerAdapters.HttpTestServer.create(HTTP_2, sslContext);
        https2_Server.addHandler(handler, "/");
        https2_Server.start();
        System.out.println("Started HTTPS v2 server at " + https2_Server.serverAuthority());
        https2URI = new URI("https:
    }

    @AfterClass
    public static void afterClass() throws Exception {
        var e = safeStop(http1_1_Server, null);
        e = safeStop(https_1_1_Server, e);
        e = safeStop(http2_Server, e);
        e = safeStop(https2_Server, e);
        if (e != null) {
            throw e;
        }
    }

    /**
     * Stops the server and returns (instead of throwing) any exception that might
     * have occurred during stop. If {@code prevException} is not null then any
     * exception during stop of the {@code server} will be added as a suppressed
     * exception to the {@code prevException} and the {@code prevException} will be
     * returned.
     */
    private static Exception safeStop(HttpServerAdapters.HttpTestServer server, Exception prevException) {
        if (server == null) {
            return null;
        }
        var serverAuthority = server.serverAuthority();
        try {
            server.stop();
        } catch (Exception e) {
            System.err.println("Failed to stop server " + serverAuthority);
            if (prevException == null) {
                return e;
            }
            prevException.addSuppressed(e);
            return prevException;
        }
        return prevException;
    }

    @DataProvider(name = "params")
    private Object[][] paramsProvider() throws Exception {
        final List<Object[]> testMethodParams = new ArrayList();
        final URI[] requestURIs = new URI[]{httpURI, httpsURI, http2URI, https2URI};
        final Predicate<URI> requiresSSLContext = (uri) -> uri.getScheme().equals("https");
        for (var requestURI : requestURIs) {
            final var configureClientSSL = requiresSSLContext.test(requestURI);
            testMethodParams.add(new Object[]{
                    newBuilder(configureClientSSL).provider(),
                    requestURI,
                    null
            });
            testMethodParams.add(new Object[]{
                    newBuilder(configureClientSSL).localAddress(null).provider(),
                    requestURI,
                    null
            });
            final var loopbackAddr = InetAddress.getLoopbackAddress();
            testMethodParams.add(new Object[]{
                    newBuilder(configureClientSSL)
                            .localAddress(loopbackAddr)
                            .provider(),
                    requestURI,
                    loopbackAddr
            });
            if (IPSupport.hasIPv6()) {
                final var localAddr = InetAddress.getByName("::");
                testMethodParams.add(new Object[]{
                        newBuilder(configureClientSSL)
                                .localAddress(localAddr)
                                .provider(),
                        requestURI,
                        localAddr
                });
            }
            if (IPSupport.hasIPv4()) {
                final var localAddr = InetAddress.getByName("0.0.0.0");
                testMethodParams.add(new Object[]{
                        newBuilder(configureClientSSL)
                                .localAddress(localAddr)
                                .provider(),
                        requestURI,
                        localAddr
                });
            }
        }
        return testMethodParams.stream().toArray(Object[][]::new);
    }

    sealed interface ClientCloseable extends Closeable {

        HttpClient client();

        @Override
        void close();

        record ReusableClient(HttpClient client) implements ClientCloseable {
            @Override
            public void close() { }
        }

        record ClientWithExecutor(HttpClient client, ExecutorService service)
                implements ClientCloseable {
            @Override
            public void close() {
                client.close();
                service.close();
            }
        }

        static ReusableClient reusable(HttpClient client) {
            return new ReusableClient(client);
        }

        static ClientWithExecutor withExecutor(HttpClient client, ExecutorService service) {
            return new ClientWithExecutor(client, service);
        }
    }

    sealed interface ClientProvider extends Supplier<ClientCloseable> {

        ClientCloseable get();

        record ReusableClientProvider(HttpClient client) implements ClientProvider {
            @Override
            public ClientCloseable get() {
                return ClientCloseable.reusable(client);
            }
        }

        record ClientBuilder(HttpClient.Builder builder) implements ClientProvider {
            ClientCloseable build() {
                int id = IDS.getAndIncrement();
                ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                        .name("HttpClient-" + id + "-Worker", 0).factory());
                builder.executor(virtualExecutor);
                return ClientCloseable.withExecutor(builder.build(), virtualExecutor);
            }

            public ClientBuilder localAddress(InetAddress localAddress) {
                builder.localAddress(localAddress);
                return this;
            }

            public ClientProvider provider() { return this; }

            @Override
            public ClientCloseable get() { return build(); }
        }

        static ReusableClientProvider reusable(HttpClient client) {
            return new ReusableClientProvider(client);
        }

        static ClientBuilder builder(HttpClient.Builder builder) {
            return new ClientBuilder(builder);
        }
    }




    private static ClientProvider.ClientBuilder newBuilder(boolean configureClientSSL) {
        var builder = HttpClient.newBuilder();
        builder.proxy(HttpClient.Builder.NO_PROXY);
        if (configureClientSSL) {
            builder.sslContext(sslContext);
        }
        return ClientProvider.builder(builder);
    }

    /**
     * Sends a GET request using the {@code client} and expects a 200 response.
     * The returned response body is then tested to see if the client address
     * seen by the server side handler is the same one as that is set on the
     * {@code client}
     */
    @Test(dataProvider = "params")
    public void testSend(ClientProvider clientProvider, URI requestURI, InetAddress localAddress) throws Exception {
        try (var c = clientProvider.get()) {
            HttpClient client = c.client();
            System.out.println("Testing using a HTTP client " + client.version() + " with local address " + localAddress
                    + " against request URI " + requestURI);
            var req = HttpRequest.newBuilder(requestURI).build();
            var resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            Assert.assertEquals(resp.statusCode(), 200, "Unexpected status code");
            if (localAddress != null && !localAddress.isAnyLocalAddress()) {
                Assert.assertEquals(resp.body(), localAddress.getAddress(),
                        "Unexpected client address seen by the server handler");
            }
        }
    }

    /**
     * Sends a GET request using the {@code sendAsync} method on the {@code client} and
     * expects a 200 response. The returned response body is then tested to see if the client address
     * seen by the server side handler is the same one as that is set on the
     * {@code client}
     */
    @Test(dataProvider = "params")
    public void testSendAsync(ClientProvider clientProvider, URI requestURI, InetAddress localAddress) throws Exception {
        try (var c = clientProvider.get()) {
            HttpClient client = c.client();
            System.out.println("Testing using a HTTP client " + client.version()
                    + " with local address " + localAddress
                    + " against request URI " + requestURI);
            var req = HttpRequest.newBuilder(requestURI).build();
            var cf = client.sendAsync(req,
                    HttpResponse.BodyHandlers.ofByteArray());
            var resp = cf.get();
            Assert.assertEquals(resp.statusCode(), 200, "Unexpected status code");
            if (localAddress != null && !localAddress.isAnyLocalAddress()) {
                Assert.assertEquals(resp.body(), localAddress.getAddress(),
                        "Unexpected client address seen by the server handler");
            }
        }
    }

    /**
     * Invokes the {@link #testSend} and {@link #testSendAsync}
     * tests, concurrently in multiple threads to verify that the correct local address
     * is used when multiple concurrent threads are involved in sending requests from
     * the {@code client}
     */
    @Test(dataProvider = "params")
    public void testMultiSendRequests(ClientProvider clientProvider,
                                      URI requestURI,
                                      InetAddress localAddress) throws Exception {
        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Void>> taskResults = new ArrayList<>();
        try (var c = clientProvider.get()) {
            ClientProvider client = ClientProvider.reusable(c.client());
            for (int i = 0; i < numThreads; i++) {
                final var currentIdx = i;
                var f = executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if (currentIdx % 2 == 0) {
                            testSend(client, requestURI, localAddress);
                        } else {
                            testSendAsync(client, requestURI, localAddress);
                        }
                        return null;
                    }
                });
                taskResults.add(f);
            }
            for (var r : taskResults) {
                r.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
