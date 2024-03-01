/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.authc.jwt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.network.NetworkAddress;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.mocksocket.MockHttpServer;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.ssl.CertParsingUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

/**
 * HTTPS server for JWT issuer to host a public PKC JWKSet.
 */
public class JwtIssuerHttpsServer implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(JwtIssuerHttpsServer.class);

    private static final String ADDRESS = NetworkAddress.format(InetAddress.getLoopbackAddress()); 
    private static final int PORT = 0; 
    private static final int BACKLOG = 0; 
    private static final int STOP_DELAY_SECONDS = 0; 
    private static final String PATH = "/valid/"; 

    static final Path CERT_PATH = JwtTestCase.resolvePath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.crt");
    private static final Path KEY_PATH = JwtTestCase.resolvePath(
        "/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.pem"
    );
    private static final char[] PASSWORD = "testnode".toCharArray();

    private final HttpsServer httpsServer;
    final String url; 

    /**
     * HTTPS server for JWT issuer to host a public PKC JWKSet.
     * @param encodedJwkSetPkcPublicBytes UTF-8 bytes of the encoded PKC JWKSet.
     * @throws Exception Error for configuration or start error.
     */
    @SuppressForbidden(reason = "MockHttpServer.createHttps requires InetSocketAddress, PORT=0 resolves to an available ephemeral port.")
    public JwtIssuerHttpsServer(final byte[] encodedJwkSetPkcPublicBytes) throws Exception {
        this.httpsServer = MockHttpServer.createHttps(new InetSocketAddress(ADDRESS, PORT), BACKLOG);
        this.url = "https:
        this.httpsServer.setHttpsConfigurator(new HttpsConfigurator(this.createSslContext()));
        this.httpsServer.createContext(PATH, new JwtIssuerHttpHandler(encodedJwkSetPkcPublicBytes));
        LOGGER.trace("Starting [{}]", this.url);
        this.httpsServer.start();
        LOGGER.debug("Started [{}]", this.url);
    }

    public void updateJwkSetPkcContents(final byte[] encodedJwkSetPkcPublicBytes) {
        httpsServer.removeContext(PATH);
        httpsServer.createContext(PATH, new JwtIssuerHttpHandler(encodedJwkSetPkcPublicBytes));
    }

    @Override
    public void close() throws IOException {
        if (httpsServer != null) {
            LOGGER.trace("Stopping [{}]", url);
            httpsServer.stop(STOP_DELAY_SECONDS);
            LOGGER.debug("Stopped [{}]", url);
        }
    }

    private SSLContext createSslContext() throws Exception {
        final String tlsProtocol = ESTestCase.inFipsJvm() ? "TLSv1.2" : ESTestCase.randomFrom("TLSv1.2", "TLSv1.3");
        final SSLContext sslContext = SSLContext.getInstance(tlsProtocol);
        final KeyManager keyManager = CertParsingUtils.getKeyManagerFromPEM(CERT_PATH, KEY_PATH, PASSWORD);
        sslContext.init(new KeyManager[] { keyManager }, null, null);
        return sslContext;
    }

    private record JwtIssuerHttpHandler(byte[] encodedJwkSetPkcPublicBytes) implements HttpHandler {
        @Override
        public void handle(final HttpExchange httpExchange) throws IOException {
            try {
                final String path = httpExchange.getRequestURI().getPath(); 
                LOGGER.trace("Request: [{}]", path);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    if (encodedJwkSetPkcPublicBytes == null) {
                        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0);
                    } else {
                        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, encodedJwkSetPkcPublicBytes.length);
                        os.write(encodedJwkSetPkcPublicBytes);
                    }
                }
                LOGGER.trace("Response: [{}]", path); 
            } catch (Throwable t) {
                LOGGER.warn("Exception: ", t); 
                throw t;
            }
        }
    }
}
