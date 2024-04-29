/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.HttpHost;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests {@link HttpHostBuilder}.
 */
public class HttpHostBuilderTests extends ESTestCase {

    private final Scheme scheme = randomFrom(Scheme.values());
    private final String hostname = randomAlphaOfLengthBetween(1, 20);
    private final int port = randomIntBetween(1, 65535);

    public void testBuilder() {
        assertHttpHost(HttpHostBuilder.builder(hostname), Scheme.HTTP, hostname, 9200);
        assertHttpHost(HttpHostBuilder.builder(scheme.toString() + ":
        assertHttpHost(HttpHostBuilder.builder(scheme.toString() + ":
        assertHttpHost(HttpHostBuilder.builder(scheme.toString() + ":
        assertHttpHost(HttpHostBuilder.builder(hostname + ":" + port), Scheme.HTTP, hostname, port);

        assertHttpHost(HttpHostBuilder.builder("localhost"), Scheme.HTTP, "localhost", 9200);
        assertHttpHost(HttpHostBuilder.builder("localhost:9200"), Scheme.HTTP, "localhost", 9200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("server-dash:19200"), Scheme.HTTP, "server-dash", 19200);
        assertHttpHost(HttpHostBuilder.builder("server-dash"), Scheme.HTTP, "server-dash", 9200);
        assertHttpHost(HttpHostBuilder.builder("sub.domain"), Scheme.HTTP, "sub.domain", 9200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:

        assertHttpHost(HttpHostBuilder.builder("127.0.0.1"), Scheme.HTTP, "127.0.0.1", 9200);
        assertHttpHost(HttpHostBuilder.builder("127.0.0.1:19200"), Scheme.HTTP, "127.0.0.1", 19200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:

        assertHttpHost(HttpHostBuilder.builder("[::1]"), Scheme.HTTP, "[::1]", 9200);
        assertHttpHost(HttpHostBuilder.builder("[::1]:19200"), Scheme.HTTP, "[::1]", 19200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("[fdda:5cc1:23:4::1f]"), Scheme.HTTP, "[fdda:5cc1:23:4::1f]", 9200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:

        assertHttpHost(HttpHostBuilder.builder("server_with_underscore"), Scheme.HTTP, "server_with_underscore", 9200);
        assertHttpHost(HttpHostBuilder.builder("server_with_underscore:19200"), Scheme.HTTP, "server_with_underscore", 19200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("_prefix.domain"), Scheme.HTTP, "_prefix.domain", 9200);
        assertHttpHost(HttpHostBuilder.builder("_prefix.domain:19200"), Scheme.HTTP, "_prefix.domain", 19200);
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("http:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
        assertHttpHost(HttpHostBuilder.builder("https:
    }

    public void testManualBuilder() {
        assertHttpHost(HttpHostBuilder.builder().host(hostname), Scheme.HTTP, hostname, 9200);
        assertHttpHost(HttpHostBuilder.builder().scheme(scheme).host(hostname), scheme, hostname, 9200);
        assertHttpHost(HttpHostBuilder.builder().scheme(scheme).host(hostname).port(port), scheme, hostname, port);
        assertHttpHost(HttpHostBuilder.builder().scheme(scheme).host(hostname).port(port).port(-1), scheme, hostname, 9200);
        assertHttpHost(HttpHostBuilder.builder().host(hostname).port(port), Scheme.HTTP, hostname, port);
    }

    public void testBuilderNullUri() {
        final NullPointerException e = expectThrows(NullPointerException.class, () -> HttpHostBuilder.builder(null));

        assertThat(e.getMessage(), equalTo("uri must not be null"));
    }

    public void testUnknownScheme() {
        assertBuilderBadSchemeThrows("htp:
        assertBuilderBadSchemeThrows("htttp:
        assertBuilderBadSchemeThrows("httpd:
        assertBuilderBadSchemeThrows("ws:
        assertBuilderBadSchemeThrows("wss:
        assertBuilderBadSchemeThrows("ftp:
        assertBuilderBadSchemeThrows("gopher:
        assertBuilderBadSchemeThrows("localhost:
    }

    public void testPathIsBlocked() {
        assertBuilderPathThrows("http:
        assertBuilderPathThrows("http:
        assertBuilderPathThrows("http:
    }

    public void testBuildWithoutHost() {
        final IllegalStateException e = expectThrows(IllegalStateException.class, () -> HttpHostBuilder.builder().build());

        assertThat(e.getMessage(), equalTo("host must be set"));
    }

    public void testNullScheme() {
        expectThrows(NullPointerException.class, () -> HttpHostBuilder.builder().scheme(null));
    }

    public void testNullHost() {
        expectThrows(NullPointerException.class, () -> HttpHostBuilder.builder().host(null));
    }

    public void testBadPort() {
        assertPortThrows(0);
        assertPortThrows(65536);

        assertPortThrows(randomIntBetween(Integer.MIN_VALUE, -2));
        assertPortThrows(randomIntBetween(65537, Integer.MAX_VALUE));
    }

    @SuppressWarnings("HiddenField")
    private void assertHttpHost(final HttpHostBuilder host, final Scheme scheme, final String hostname, final int port) {
        assertHttpHost(host.build(), scheme, hostname, port);
    }

    @SuppressWarnings("HiddenField")
    private void assertHttpHost(final HttpHost host, final Scheme scheme, final String hostname, final int port) {
        assertThat(host.getSchemeName(), equalTo(scheme.toString()));
        assertThat(host.getHostName(), equalTo(hostname));
        assertThat(host.getPort(), equalTo(port));
    }

    @SuppressWarnings("HiddenField")
    private void assertBuilderPathThrows(final String uri, final String path) {
        final IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> HttpHostBuilder.builder(uri));

        assertThat(e.getMessage(), containsString("[" + path + "]"));
    }

    @SuppressWarnings("HiddenField")
    private void assertBuilderBadSchemeThrows(final String uri, final String scheme) {
        final IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> HttpHostBuilder.builder(uri));

        assertThat(e.getMessage(), containsString(scheme));
    }

    @SuppressWarnings("HiddenField")
    private void assertPortThrows(final int port) {
        final IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> HttpHostBuilder.builder().port(port));

        assertThat(e.getMessage(), containsString(Integer.toString(port)));
    }

}
