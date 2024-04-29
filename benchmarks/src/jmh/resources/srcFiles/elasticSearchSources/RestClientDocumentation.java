/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client.documentation;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLContext;

/**
 * This class is used to generate the Java low-level REST client documentation.
 * You need to wrap your code between two tags like:
 * 
 * 
 *
 * Where example is your tag name.
 *
 * Then in the documentation, you can extract what is between tag and end tags with
 * ["source","java",subs="attributes,callouts,macros"]
 * --------------------------------------------------
 * include-tagged::{doc-tests}/RestClientDocumentation.java[example]
 * --------------------------------------------------
 *
 * Note that this is not a test class as we are only interested in testing that docs snippets compile. We don't want
 * to send requests to a node and we don't even have the tools to do it.
 */
@SuppressWarnings("unused")
public class RestClientDocumentation {
    private static final String TOKEN = "DUMMY";

    private static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("Authorization", "Bearer " + TOKEN); 
        builder.setHttpAsyncResponseConsumerFactory(           
            new HttpAsyncResponseConsumerFactory
                .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    @SuppressWarnings("unused")
    public void usage() throws IOException, InterruptedException {

        RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http"),
            new HttpHost("localhost", 9201, "http")).build();

        restClient.close();

        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
            Header[] defaultHeaders = new Header[]{new BasicHeader("header", "value")};
            builder.setDefaultHeaders(defaultHeaders); 
        }
        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
            builder.setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS); 
        }
        {
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost("localhost", 9200, "http"));
            builder.setNodeSelector(new NodeSelector() { 
                @Override
                public void select(Iterable<Node> nodes) {
                    /*
                     * Prefer any node that belongs to rack_one. If none is around
                     * we will go to another rack till it's time to try and revive
                     * some of the nodes that belong to rack_one.
                     */
                    boolean foundOne = false;
                    for (Node node : nodes) {
                        String rackId = node.getAttributes().get("rack_id").get(0);
                        if ("rack_one".equals(rackId)) {
                            foundOne = true;
                            break;
                        }
                    }
                    if (foundOne) {
                        Iterator<Node> nodesIt = nodes.iterator();
                        while (nodesIt.hasNext()) {
                            Node node = nodesIt.next();
                            String rackId = node.getAttributes().get("rack_id").get(0);
                            if ("rack_one".equals(rackId) == false) {
                                nodesIt.remove();
                            }
                        }
                    }
                }
            });
        }
        {
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost("localhost", 9200, "http"));
            builder.setFailureListener(new RestClient.FailureListener() {
                @Override
                public void onFailure(Node node) {
                }
            });
        }
        {
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost("localhost", 9200, "http"));
            builder.setRequestConfigCallback(
                new RestClientBuilder.RequestConfigCallback() {
                    @Override
                    public RequestConfig.Builder customizeRequestConfig(
                            RequestConfig.Builder requestConfigBuilder) {
                        return requestConfigBuilder.setSocketTimeout(10000); 
                    }
                });
        }
        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
            builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setProxy(
                            new HttpHost("proxy", 9000, "http"));  
                    }
                });
        }

        {
            Request request = new Request(
                "GET",  
                "/");   
            Response response = restClient.performRequest(request);
        }
        {
            Request request = new Request(
                "GET",  
                "/");   
            Cancellable cancellable = restClient.performRequestAsync(request,
                new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                    }

                    @Override
                    public void onFailure(Exception exception) {
                    }
            });
        }
        {
            Request request = new Request("GET", "/");
            request.addParameter("pretty", "true");
            request.setEntity(new NStringEntity(
                    "{\"json\":\"text\"}",
                    ContentType.APPLICATION_JSON));
            request.setJsonEntity("{\"json\":\"text\"}");
            request.setOptions(COMMON_OPTIONS);
            {
                RequestOptions.Builder options = COMMON_OPTIONS.toBuilder();
                options.addHeader("cats", "knock things off of other things");
                request.setOptions(options);
            }
        }
        {
            HttpEntity[] documents = new HttpEntity[10];
            final CountDownLatch latch = new CountDownLatch(documents.length);
            for (int i = 0; i < documents.length; i++) {
                Request request = new Request("PUT", "/posts/doc/" + i);
                request.setEntity(documents[i]);
                restClient.performRequestAsync(
                        request,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(Response response) {
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                latch.countDown();
                            }
                        }
                );
            }
            latch.await();
        }
        {
            Request request = new Request("GET", "/posts/_search");
            Cancellable cancellable = restClient.performRequestAsync(
                request,
                new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                    }

                    @Override
                    public void onFailure(Exception exception) {
                    }
                }
            );
            cancellable.cancel();
        }
        {
            Response response = restClient.performRequest(new Request("GET", "/"));
            RequestLine requestLine = response.getRequestLine(); 
            HttpHost host = response.getHost(); 
            int statusCode = response.getStatusLine().getStatusCode(); 
            Header[] headers = response.getHeaders(); 
            String responseBody = EntityUtils.toString(response.getEntity()); 
        }
    }

    @SuppressWarnings("unused")
    public void commonConfiguration() throws Exception {
        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200))
                .setRequestConfigCallback(
                    new RestClientBuilder.RequestConfigCallback() {
                        @Override
                        public RequestConfig.Builder customizeRequestConfig(
                                RequestConfig.Builder requestConfigBuilder) {
                            return requestConfigBuilder
                                .setConnectTimeout(5000)
                                .setSocketTimeout(60000);
                        }
                    });
        }
        {
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(60000)
                .build();
            RequestOptions options = RequestOptions.DEFAULT.toBuilder()
                .setRequestConfig(requestConfig)
                .build();
        }
        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultIOReactorConfig(
                            IOReactorConfig.custom()
                                .setIoThreadCount(1)
                                .build());
                    }
                });
        }
        {
            final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("user", "test-user-password"));

            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        }
        {
            final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("user", "test-user-password"));

            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.disableAuthCaching(); 
                        return httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        }
        {
            String keyStorePass = "";
            Path trustStorePath = Paths.get("/path/to/truststore.p12");
            KeyStore truststore = KeyStore.getInstance("pkcs12");
            try (InputStream is = Files.newInputStream(trustStorePath)) {
                truststore.load(is, keyStorePass.toCharArray());
            }
            SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(truststore, null);
            final SSLContext sslContext = sslBuilder.build();
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext);
                    }
                });
        }
        {
            Path caCertificatePath = Paths.get("/path/to/ca.crt");
            CertificateFactory factory =
                CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);
            final SSLContext sslContext = sslContextBuilder.build();
            RestClient.builder(
                new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                        HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext);
                    }
                });
        }
        {
            String trustStorePass = "";
            String keyStorePass = "";
            Path trustStorePath = Paths.get("/path/to/your/truststore.p12");
            Path keyStorePath = Paths.get("/path/to/your/keystore.p12");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            try (InputStream is = Files.newInputStream(trustStorePath)) {
                trustStore.load(is, trustStorePass.toCharArray());
            }
            try (InputStream is = Files.newInputStream(keyStorePath)) {
                keyStore.load(is, keyStorePass.toCharArray());
            }
            SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null)
                .loadKeyMaterial(keyStore, keyStorePass.toCharArray());
            final SSLContext sslContext = sslBuilder.build();
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                        HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext);
                    }
                });
        }
        {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
            Header[] defaultHeaders =
                new Header[]{new BasicHeader("Authorization",
                    "Bearer u6iuAxZ0RG1Kcm5jVFI4eU4tZU9aVFEwT2F3")};
            builder.setDefaultHeaders(defaultHeaders);
        }
        {
            String apiKeyId = "uqlEyn8B_gQ_jlvwDIvM";
            String apiKeySecret = "HxHWk2m4RN-V_qg9cDpuX";
            String apiKeyAuth =
                Base64.getEncoder().encodeToString(
                    (apiKeyId + ":" + apiKeySecret)
                        .getBytes(StandardCharsets.UTF_8));
            RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
            Header[] defaultHeaders =
                new Header[]{new BasicHeader("Authorization",
                    "ApiKey " + apiKeyAuth)};
            builder.setDefaultHeaders(defaultHeaders);
        }

    }
}
