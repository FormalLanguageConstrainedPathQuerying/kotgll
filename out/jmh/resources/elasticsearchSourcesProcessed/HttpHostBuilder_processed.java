/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * {@code HttpHostBuilder} creates an {@link HttpHost} meant to be used with an Elasticsearch cluster. The {@code HttpHostBuilder} uses
 * defaults that are most common for Elasticsearch, including an unspecified port defaulting to <code>9200</code> and the default scheme
 * being <code>http</code> (as opposed to <code>https</code>).
 * <p>
 * The only <em>required</em> detail is the host to connect too, either via hostname or IP address.
 * <p>
 * This enables you to create an {@code HttpHost} directly via a builder mechanism, or indirectly by parsing a URI-like string. For example:
 * <pre><code>
 * HttpHost host1 = HttpHostBuilder.builder("localhost").build();               
 * HttpHost host2 = HttpHostBuilder.builder("localhost:9200").build();          
 * HttpHost host4 = HttpHostBuilder.builder("http:
 * HttpHost host5 = HttpHostBuilder.builder("https:
 * HttpHost host6 = HttpHostBuilder.builder("https:
 * HttpHost host7 = HttpHostBuilder.builder("http:
 * HttpHost host8 = HttpHostBuilder.builder("https:
 * HttpHost host9 = HttpHostBuilder.builder("https:
 * HttpHost host10= HttpHostBuilder.builder("https:
 * </code></pre>
 * Note: {@code HttpHost}s are the mechanism that the {@link RestClient} uses to build the base request. If you need to specify proxy
 * settings, then use the {@link RestClientBuilder.RequestConfigCallback} to configure the {@code Proxy} settings.
 *
 * @see #builder(String)
 * @see #builder()
 */
public class HttpHostBuilder {

    /**
     * The scheme used to connect to Elasticsearch.
     */
    private Scheme scheme = Scheme.HTTP;
    /**
     * The host is the only required portion of the supplied URI when building it. The rest can be defaulted.
     */
    private String host = null;
    /**
     * The port used to connect to Elasticsearch.
     * <p>
     * The default port is 9200 when unset.
     */
    private int port = -1;

    /**
     * Create an empty {@link HttpHostBuilder}.
     * <p>
     * The expectation is that you then explicitly build the {@link HttpHost} piece-by-piece.
     * <p>
     * For example:
     * <pre><code>
     * HttpHost localhost = HttpHostBuilder.builder().host("localhost").build();                            
     * HttpHost explicitLocalhost = HttpHostBuilder.builder.().scheme(Scheme.HTTP).host("localhost").port(9200).build();
     *                                                                                                      
     * HttpHost secureLocalhost = HttpHostBuilder.builder().scheme(Scheme.HTTPS).host("localhost").build(); 
     * HttpHost differentPort = HttpHostBuilder.builder().host("my_host").port(19200).build();              
     * HttpHost ipBased = HttpHostBuilder.builder().host("192.168.0.11").port(80).build();                  
     * </code></pre>
     *
     * @return Never {@code null}.
     */
    public static HttpHostBuilder builder() {
        return new HttpHostBuilder();
    }

    /**
     * Create an empty {@link HttpHostBuilder}.
     * <p>
     * The expectation is that you then explicitly build the {@link HttpHost} piece-by-piece.
     * <p>
     * For example:
     * <pre><code>
     * HttpHost localhost = HttpHostBuilder.builder("localhost").build();                     
     * HttpHost explicitLocalhost = HttpHostBuilder.builder("http:
     * HttpHost secureLocalhost = HttpHostBuilder.builder("https:
     * HttpHost differentPort = HttpHostBuilder.builder("my_host:19200").build();             
     * HttpHost ipBased = HttpHostBuilder.builder("192.168.0.11:80").build();                 
     * </code></pre>
     *
     * @return Never {@code null}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if any issue occurs while parsing the {@code uri}.
     */
    public static HttpHostBuilder builder(final String uri) {
        return new HttpHostBuilder(uri);
    }

    /**
     * Create a new {@link HttpHost} from scratch.
     */
    HttpHostBuilder() {
    }

    /**
     * Create a new {@link HttpHost} based on the supplied host.
     *
     * @param uri The [partial] URI used to build.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if any issue occurs while parsing the {@code uri}.
     */
    HttpHostBuilder(final String uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        try {
            String cleanedUri = uri;

            if (uri.contains(":
                cleanedUri = "http:
            }

            final URI parsedUri = new URI(cleanedUri);

            if (parsedUri.getScheme() != null) {
                scheme(Scheme.fromString(parsedUri.getScheme()));
            }

            if (parsedUri.getHost() != null) {
                host(parsedUri.getHost());
            } else {
                final String host = parsedUri.getRawAuthority();

                if (host.contains(":")) {
                    final String[] hostPort = host.split(":", 2);

                    host(hostPort[0]);
                    port(Integer.parseInt(hostPort[1]));
                } else {
                    host(host);
                }
            }

            if (parsedUri.getPort() != -1) {
                port(parsedUri.getPort());
            }

            if (parsedUri.getRawPath() != null && parsedUri.getRawPath().isEmpty() == false) {
                throw new IllegalArgumentException(
                    "HttpHosts do not use paths ["
                        + parsedUri.getRawPath()
                        + "]. see setRequestConfigCallback for proxies. value: ["
                        + uri
                        + "]"
                );
            }
        } catch (URISyntaxException | IndexOutOfBoundsException | NullPointerException e) {
            throw new IllegalArgumentException("error parsing host: [" + uri + "]", e);
        }
    }

    /**
     * Set the scheme (aka protocol) for the {@link HttpHost}.
     *
     * @param scheme The scheme to use.
     * @return Always {@code this}.
     * @throws NullPointerException if {@code scheme} is {@code null}.
     */
    public HttpHostBuilder scheme(final Scheme scheme) {
        this.scheme = Objects.requireNonNull(scheme);

        return this;
    }

    /**
     * Set the host for the {@link HttpHost}.
     * <p>
     * This does not attempt to parse the {@code host} in any way.
     *
     * @param host The host to use.
     * @return Always {@code this}.
     * @throws NullPointerException if {@code host} is {@code null}.
     */
    public HttpHostBuilder host(final String host) {
        this.host = Objects.requireNonNull(host);

        return this;
    }

    /**
     * Set the port for the {@link HttpHost}.
     * <p>
     * Specifying the {@code port} as -1 will cause it to be defaulted to 9200 when the {@code HttpHost} is built.
     *
     * @param port The port to use.
     * @return Always {@code this}.
     * @throws IllegalArgumentException if the {@code port} is not -1 or [1, 65535].
     */
    public HttpHostBuilder port(final int port) {
        if (port != -1 && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("port must be -1 for the default or [1, 65535]. was: " + port);
        }

        this.port = port;

        return this;
    }

    /**
     * Create a new {@link HttpHost} from the current {@code scheme}, {@code host}, and {@code port}.
     *
     * @return Never {@code null}.
     * @throws IllegalStateException if {@code host} is unset.
     */
    public HttpHost build() {
        if (host == null) {
            throw new IllegalStateException("host must be set");
        }

        return new HttpHost(host, port == -1 ? 9200 : port, scheme.toString());
    }

}
