/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import jdk.internal.net.http.common.HttpHeadersBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;

import java.lang.ref.Reference;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.net.http.HttpClient.Version;
import java.util.function.BiPredicate;

import static java.lang.String.format;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.joining;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static org.testng.Assert.*;

public class AuthenticationFilterTest {

    @DataProvider(name = "uris")
    public Object[][] responses() {
        return new Object[][] {
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "http:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
                { "https:
        };
    }

    static final ConcurrentMap<String,Throwable> FAILED = new ConcurrentHashMap<>();

    static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    @Test(dataProvider = "uris")
    public void testAuthentication(String uri, Version v, String proxy) throws Exception {
        String test = format("testAuthentication: {\"%s\", %s, \"%s\"}", uri, v, proxy);
        try {
            doTestAuthentication(uri, v, proxy);
        } catch(Exception | Error x) {
            FAILED.putIfAbsent(test, x);
            throw x;
        }
    }

    @AfterClass
    public void printDiagnostic() {
        if (FAILED.isEmpty()) {
            out.println("All tests passed");
            return;
        }
        out.println("Failed tests: ");
        FAILED.keySet().forEach(s ->
                out.println("\t " + s.substring(s.indexOf(':')+1) + ","));
        out.println();
        FAILED.entrySet().forEach(e -> {
                System.err.println("\n" + e.getKey()
                        + " FAILED: " + e.getValue());
                e.getValue().printStackTrace();
        });
    }

    static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

    private void doTestAuthentication(String uri, Version v, String proxy) throws Exception {
        int colon = proxy == null ? -1 : proxy.lastIndexOf(":");
        ProxySelector ps = proxy == null ? NO_PROXY
                : ProxySelector.of(InetSocketAddress.createUnresolved(
                        proxy.substring(0, colon),
                        Integer.parseInt(proxy.substring(colon+1))));
        int unauthorized = proxy == null ? 401 : 407;

        TestAuthenticator authenticator = new TestAuthenticator();

        HttpClientBuilderImpl clientBuilder = new HttpClientBuilderImpl()
                .authenticator(authenticator).proxy(ps);
        HttpClientFacade facade = HttpClientImpl.create(clientBuilder);
        HttpClientImpl client = facade.impl;
        AuthenticationFilter filter = new AuthenticationFilter();

        assertEquals(authenticator.COUNTER.get(), 0);

        URI reqURI = URI.create(uri);
        HttpRequestBuilderImpl reqBuilder =
                new HttpRequestBuilderImpl(reqURI);
        HttpRequestImpl origReq = new HttpRequestImpl(reqBuilder);
        HttpRequestImpl req = new HttpRequestImpl(origReq, ps);
        MultiExchange<?> multi = new MultiExchange<Void>(origReq, req, client,
                BodyHandlers.replacing(null),
                null, AccessController.getContext());
        Exchange<?> exchange = new Exchange<>(req, multi);
        out.println("\nSimulating unauthenticated request to " + uri);
        filter.request(req, multi);
        HttpHeaders hdrs = req.getSystemHeadersBuilder().build();
        assertFalse(hdrs.firstValue(authorization(true)).isPresent());
        assertFalse(hdrs.firstValue(authorization(false)).isPresent());
        assertEquals(authenticator.COUNTER.get(), 0);

        HttpHeadersBuilder headersBuilder = new HttpHeadersBuilder();
        headersBuilder.addHeader(authenticate(proxy!=null),
                                "Basic realm=\"earth\"");
        HttpHeaders headers = headersBuilder.build();
        Response response = new Response(req, exchange, headers, null, unauthorized, v);
        out.println("Simulating " + unauthorized
                + " response from " + uri);
        HttpRequestImpl next = filter.response(response);

        out.println("Checking filter's response to "
                + unauthorized + " from " + uri);
        assertTrue(next != null, "next should not be null");
        String[] up = check(reqURI, next.getSystemHeadersBuilder().build(), proxy);
        assertEquals(authenticator.COUNTER.get(), 1);

        out.println("Simulating next request with credentials to " + uri);
        exchange = new Exchange<>(next, multi);
        filter.request(next, multi);
        out.println("Checking credentials in request header after filter for " + uri);
        hdrs = next.getSystemHeadersBuilder().build();
        check(reqURI, hdrs, proxy);
        check(next.uri(), hdrs, proxy);
        out.println("Simulating  successful response 200 from " + uri);
        HttpHeaders h = HttpHeaders.of(Collections.emptyMap(), ACCEPT_ALL);
        response = new Response(next, exchange,h, null, 200, v);
        next = filter.response(response);
        assertTrue(next == null, "next should be null");
        assertEquals(authenticator.COUNTER.get(), 1);

        int fragmentIndex = uri.indexOf('#');
        String subpath = "/bar";
        String prefix = uri;
        String fragment =  "";
        if (fragmentIndex > -1) {
            prefix = uri.substring(0, fragmentIndex);
            fragment = uri.substring(fragmentIndex);
        }
        URI reqURI2 = URI.create(prefix + subpath + fragment);
        out.println("Simulating new request to " + reqURI2);
        HttpRequestBuilderImpl reqBuilder2 =
                new HttpRequestBuilderImpl(reqURI2);
        HttpRequestImpl origReq2 = new HttpRequestImpl(reqBuilder2);
        HttpRequestImpl req2 = new HttpRequestImpl(origReq2, ps);
        MultiExchange<?> multi2 = new MultiExchange<Void>(origReq2, req2, client,
                HttpResponse.BodyHandlers.replacing(null),
                null, AccessController.getContext());
        filter.request(req2, multi2);
        out.println("Check that filter has added credentials from cache for " + reqURI2
                + " with proxy " + req2.proxy());
        String[] up2 = check(reqURI, req2.getSystemHeadersBuilder().build(), proxy);
        assertTrue(Arrays.deepEquals(up, up2), format("%s:%s != %s:%s", up2[0], up2[1], up[0], up[1]));
        assertEquals(authenticator.COUNTER.get(), 1);

        URI reqURI3;
        if (isNullOrEmpty(reqURI.getPath())
                && isNullOrEmpty(reqURI.getFragment())
                && reqURI.getPort() == -1) {
            reqURI3 = URI.create(uri + ".bar");
        } else {
            reqURI3 = new URI(reqURI.getScheme(), reqURI.getUserInfo(),
                         reqURI.getHost() + ".bar", reqURI.getPort(),
                              reqURI.getPath(), reqURI.getQuery(),
                              reqURI.getFragment());
        }
        out.println("Simulating new request to " + reqURI3);
        HttpRequestBuilderImpl reqBuilder3 =
                new HttpRequestBuilderImpl(reqURI3);
        HttpRequestImpl origReq3 = new HttpRequestImpl(reqBuilder3);
        HttpRequestImpl req3 = new HttpRequestImpl(origReq3, ps);
        MultiExchange<?> multi3 = new MultiExchange<Void>(origReq3, req3, client,
                HttpResponse.BodyHandlers.replacing(null),
                null, AccessController.getContext());
        filter.request(req3, multi3);
        HttpHeaders h3 = req3.getSystemHeadersBuilder().build();
        if (proxy == null) {
            out.println("Check that filter has not added proxy credentials from cache for " + reqURI3);
            assert !h3.firstValue(authorization(true)).isPresent()
                    : format("Unexpected proxy credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(req3.getSystemHeadersBuilder().build(), true))
                            .collect(joining(":")));
            assertFalse(h3.firstValue(authorization(true)).isPresent());
        } else {
            out.println("Check that filter has added proxy credentials from cache for " + reqURI3);
            String[] up3 = check(reqURI, h3, proxy);
            assertTrue(Arrays.deepEquals(up, up3), format("%s:%s != %s:%s", up3[0], up3[1], up[0], up[1]));
        }
        out.println("Check that filter has not added server credentials from cache for " + reqURI3);
        assert !h3.firstValue(authorization(false)).isPresent()
                : format("Unexpected server credentials found: %s",
                java.util.stream.Stream.of(getAuthorization(h3, false))
                        .collect(joining(":")));
        assertFalse(h3.firstValue(authorization(false)).isPresent());
        assertEquals(authenticator.COUNTER.get(), 1);

        int port = reqURI.getPort();
        port = port == -1 ? defaultPort(reqURI.getScheme()) : port;
        ProxySelector fakeProxy = proxy == null
                ? ProxySelector.of(InetSocketAddress.createUnresolved(
                reqURI.getHost(), port))
                : NO_PROXY;
        URI reqURI4 = proxy == null ? reqURI : new URI("http", null, req.proxy().getHostName(),
                    req.proxy().getPort(), "/", null, null);
        HttpRequestBuilderImpl reqBuilder4 = new HttpRequestBuilderImpl(reqURI4);
        HttpRequestImpl origReq4 = new HttpRequestImpl(reqBuilder4);
        HttpRequestImpl req4 = new HttpRequestImpl(origReq4, fakeProxy);
        MultiExchange<?> multi4 = new MultiExchange<Void>(origReq4, req4, client,
                HttpResponse.BodyHandlers.replacing(null), null,
                AccessController.getContext());
        out.println("Simulating new request to " + reqURI4 + " with a proxy " + req4.proxy());
        assertTrue((req4.proxy() == null) == (proxy != null),
                "(req4.proxy() == null) == (proxy != null) should be true");
        filter.request(req4, multi4);
        out.println("Check that filter has not added proxy credentials from cache for "
                + reqURI4 + " (proxy: " + req4.proxy()  + ")");
        HttpHeaders h4 = req4.getSystemHeadersBuilder().build();
        assert !h4.firstValue(authorization(true)).isPresent()
                : format("Unexpected proxy credentials found: %s",
                java.util.stream.Stream.of(getAuthorization(h4, true))
                        .collect(joining(":")));
        assertFalse(h4.firstValue(authorization(true)).isPresent());
        if (proxy != null) {
            out.println("Check that filter has not added server credentials from cache for "
                    + reqURI4 + " (proxy: " + req4.proxy()  + ")");
            assert !h4.firstValue(authorization(false)).isPresent()
                    : format("Unexpected server credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(h4, false))
                            .collect(joining(":")));
            assertFalse(h4.firstValue(authorization(false)).isPresent());
        } else {
            out.println("Check that filter has added server credentials from cache for "
                    + reqURI4 + " (proxy: " + req4.proxy()  + ")");
            String[] up4 = check(reqURI, h4, proxy);
            assertTrue(Arrays.deepEquals(up, up4),  format("%s:%s != %s:%s", up4[0], up4[1], up[0], up[1]));
        }
        assertEquals(authenticator.COUNTER.get(), 1);

        if (proxy != null) {
            HttpRequestBuilderImpl reqBuilder5 = new HttpRequestBuilderImpl(reqURI);
            HttpRequestImpl origReq5 = new HttpRequestImpl(reqBuilder5);
            HttpRequestImpl req5 = new HttpRequestImpl(origReq5, NO_PROXY);
            MultiExchange<?> multi5 = new MultiExchange<Void>(origReq5, req5, client,
                    HttpResponse.BodyHandlers.replacing(null), null,
                    AccessController.getContext());
            out.println("Simulating new request to " + reqURI + " with a proxy " + req5.proxy());
            assertTrue(req5.proxy() == null, "req5.proxy() should be null");
            Exchange<?> exchange5 = new Exchange<>(req5, multi5);
            filter.request(req5, multi5);
            out.println("Check that filter has not added server credentials from cache for "
                    + reqURI + " (proxy: " + req5.proxy()  + ")");
            HttpHeaders h5 = req5.getSystemHeadersBuilder().build();
            assert !h5.firstValue(authorization(false)).isPresent()
                    : format("Unexpected server credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(h5, false))
                            .collect(joining(":")));
            assertFalse(h5.firstValue(authorization(false)).isPresent());
            out.println("Check that filter has not added proxy credentials from cache for "
                    + reqURI + " (proxy: " + req5.proxy()  + ")");
            assert !h5.firstValue(authorization(true)).isPresent()
                    : format("Unexpected proxy credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(h5, true))
                            .collect(joining(":")));
            assertFalse(h5.firstValue(authorization(true)).isPresent());
            assertEquals(authenticator.COUNTER.get(), 1);

            HttpHeadersBuilder headers5Builder = new HttpHeadersBuilder();
            headers5Builder.addHeader(authenticate(false),
                               "Basic realm=\"earth\"");
            HttpHeaders headers5 = headers5Builder.build();
            unauthorized = 401;
            Response response5 = new Response(req5, exchange5, headers5, null, unauthorized, v);
            out.println("Simulating " + unauthorized
                    + " response from " + uri);
            HttpRequestImpl next5 = filter.response(response5);
            assertEquals(authenticator.COUNTER.get(), 2);

            out.println("Checking filter's response to "
                    + unauthorized + " from " + uri);
            assertTrue(next5 != null, "next5 should not be null");
            String[] up5 = check(reqURI, next5.getSystemHeadersBuilder().build(), null);

            exchange5 = new Exchange<>(next5, multi5);
            filter.request(next5, multi5);
            h = HttpHeaders.of(Map.of(), ACCEPT_ALL);
            response5 = new Response(next5, exchange5, h, null, 200, v);
            filter.response(response5);
            assertEquals(authenticator.COUNTER.get(), 2);

            HttpRequestBuilderImpl reqBuilder6 = new HttpRequestBuilderImpl(reqURI);
            HttpRequestImpl origReq6 = new HttpRequestImpl(reqBuilder6);
            HttpRequestImpl req6 = new HttpRequestImpl(origReq6, ps);
            MultiExchange<?> multi6 = new MultiExchange<Void>(origReq6, req6, client,
                    HttpResponse.BodyHandlers.replacing(null), null,
                    AccessController.getContext());
            out.println("Simulating new request to " + reqURI + " with a proxy " + req6.proxy());
            assertTrue(req6.proxy() != null, "req6.proxy() should not be null");
            Exchange<?> exchange6 = new Exchange<>(req6, multi6);
            filter.request(req6, multi6);
            out.println("Check that filter has added server credentials from cache for "
                    + reqURI + " (proxy: " + req6.proxy()  + ")");
            HttpHeaders h6 = req6.getSystemHeadersBuilder().build();
            check(reqURI, h6, null);
            out.println("Check that filter has added proxy credentials from cache for "
                    + reqURI + " (proxy: " + req6.proxy()  + ")");
            String[] up6 = check(reqURI, h6, proxy);
            assertTrue(Arrays.deepEquals(up, up6), format("%s:%s != %s:%s", up6[0], up6[1], up[0], up[1]));
            assertEquals(authenticator.COUNTER.get(), 2);
        }

        if (proxy == null && uri.contains("x/y/z")) {
            URI reqURI7 = URI.create(prefix + "/../../w/z" + fragment);
            assertTrue(reqURI7.getPath().contains("../../"));
            HttpRequestBuilderImpl reqBuilder7 = new HttpRequestBuilderImpl(reqURI7);
            HttpRequestImpl origReq7 = new HttpRequestImpl(reqBuilder7);
            HttpRequestImpl req7 = new HttpRequestImpl(origReq7, ps);
            MultiExchange<?> multi7 = new MultiExchange<Void>(origReq7, req7, client,
                    HttpResponse.BodyHandlers.replacing(null), null,
                    AccessController.getContext());
            out.println("Simulating new request to " + reqURI7 + " with a proxy " + req7.proxy());
            assertTrue(req7.proxy() == null, "req7.proxy() should be null");
            Exchange<?> exchange7 = new Exchange<>(req7, multi7);
            filter.request(req7, multi7);
            out.println("Check that filter has not added server credentials from cache for "
                    + reqURI7 + " (proxy: " + req7.proxy()  + ") [resolved uri: "
                    + reqURI7.resolve(".") + " should not match " + reqURI.resolve(".") + "]");
            HttpHeaders h7 = req7.getSystemHeadersBuilder().build();
            assert !h7.firstValue(authorization(false)).isPresent()
                    : format("Unexpected server credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(h7, false))
                            .collect(joining(":")));
            assertFalse(h7.firstValue(authorization(false)).isPresent());
            out.println("Check that filter has not added proxy credentials from cache for "
                    + reqURI7 + " (proxy: " + req7.proxy()  + ")");
            assert !h7.firstValue(authorization(true)).isPresent()
                    : format("Unexpected proxy credentials found: %s",
                    java.util.stream.Stream.of(getAuthorization(h7, true))
                            .collect(joining(":")));
            assertFalse(h7.firstValue(authorization(true)).isPresent());
            assertEquals(authenticator.COUNTER.get(), 1);

        }

        Reference.reachabilityFence(facade);
    }

    static int defaultPort(String protocol) {
        if ("http".equalsIgnoreCase(protocol)) return 80;
        if ("https".equalsIgnoreCase(protocol)) return 443;
        return -1;
    }

    static String authenticate(boolean proxy) {
        return proxy ? "Proxy-Authenticate" : "WWW-Authenticate";
    }
    static String authorization(boolean proxy) {
        return proxy ? "Proxy-Authorization" : "Authorization";
    }

    static String[] getAuthorization(HttpHeaders headers, boolean proxy) {
        String auth = headers.firstValue(authorization(proxy)).get().substring(6);
        String pw = new String(Base64.getDecoder().decode(auth), US_ASCII);
        String[] up = pw.split(":");
        up[1] = new String(Base64.getDecoder().decode(up[1]), US_ASCII);
        return up;
    }

    static Authenticator.RequestorType requestorType(boolean proxy) {
        return proxy ? Authenticator.RequestorType.PROXY
                     : Authenticator.RequestorType.SERVER;
    }

    static String[] check(URI reqURI, HttpHeaders headers, String proxy) throws Exception {
        out.println("Next request headers: " + headers.map());
        String[] up = getAuthorization(headers, proxy != null);
        String u = up[0];
        String p = up[1];
        out.println("user:password: " + u + ":" + p);
        String protocol = proxy != null ? "http" : reqURI.getScheme();
        String expectedUser = "u." + protocol;
        assertEquals(u, expectedUser);
        String host = proxy == null ? reqURI.getHost() :
                proxy.substring(0, proxy.lastIndexOf(':'));
        int port = proxy == null ? reqURI.getPort()
                : Integer.parseInt(proxy.substring(proxy.lastIndexOf(':')+1));
        String expectedPw = concat(requestorType(proxy!=null),
                "basic", protocol, host,
                port, "earth", reqURI.toURL());
        assertEquals(p, expectedPw);
        return new String[] {u, p};
    }

    static String concat(Authenticator.RequestorType reqType,
                           String authScheme,
                           String requestingProtocol,
                           String requestingHost,
                           int requestingPort,
                           String realm,
                           URL requestingURL) {
        return new StringBuilder()
                .append(reqType).append(":")
                .append(authScheme).append(":")
                .append(String.valueOf(realm))
                .append("[")
                .append(requestingProtocol).append(':')
                .append(requestingHost).append(':')
                .append(requestingPort).append("]")
                .append("/").append(String.valueOf(requestingURL))
                .toString();
    }

    static class TestAuthenticator extends Authenticator {
        final AtomicLong COUNTER = new AtomicLong();
        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            COUNTER.incrementAndGet();
            return new PasswordAuthentication("u."+getRequestingProtocol(),
                    Base64.getEncoder().encodeToString(concat().getBytes(US_ASCII))
                            .toCharArray());
        }

        String concat() {
            return AuthenticationFilterTest.concat(
                    getRequestorType(),
                    getRequestingScheme(),
                    getRequestingProtocol(),
                    getRequestingHost(),
                    getRequestingPort(),
                    getRequestingPrompt(),
                    getRequestingURL());
        }

    }
}
