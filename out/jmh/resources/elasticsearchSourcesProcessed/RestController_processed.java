/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.ReleasableBytesReference;
import org.elasticsearch.common.io.stream.BytesStream;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.path.PathTrie;
import org.elasticsearch.common.recycler.Recycler;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.core.RestApiVersion;
import org.elasticsearch.core.Streams;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.http.HttpHeadersValidationException;
import org.elasticsearch.http.HttpRouteStats;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.rest.RestHandler.Route;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.telemetry.tracing.Tracer;
import org.elasticsearch.transport.Transports;
import org.elasticsearch.usage.SearchUsageHolder;
import org.elasticsearch.usage.UsageService;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.elasticsearch.indices.SystemIndices.EXTERNAL_SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY;
import static org.elasticsearch.indices.SystemIndices.SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY;
import static org.elasticsearch.rest.RestResponse.TEXT_CONTENT_TYPE;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.elasticsearch.rest.RestStatus.METHOD_NOT_ALLOWED;
import static org.elasticsearch.rest.RestStatus.NOT_ACCEPTABLE;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestController implements HttpServerTransport.Dispatcher {

    private static final Logger logger = LogManager.getLogger(RestController.class);
    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestController.class);
    /**
     * list of browser safelisted media types - not allowed on Content-Type header
     * https:
     */
    static final Set<String> SAFELISTED_MEDIA_TYPES = Set.of("application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

    static final String ELASTIC_PRODUCT_HTTP_HEADER = "X-elastic-product";
    static final String ELASTIC_PRODUCT_HTTP_HEADER_VALUE = "Elasticsearch";
    static final Set<String> RESERVED_PATHS = Set.of("/__elb_health__", "/__elb_health__/zk", "/_health", "/_health/zk");
    private static final BytesReference FAVICON_RESPONSE;

    static {
        try (InputStream stream = RestController.class.getResourceAsStream("/config/favicon.ico")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Streams.copy(stream, out);
            FAVICON_RESPONSE = new BytesArray(out.toByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private final PathTrie<MethodHandlers> handlers = new PathTrie<>(RestUtils.REST_DECODER);

    private final RestInterceptor interceptor;

    private final NodeClient client;

    private final CircuitBreakerService circuitBreakerService;

    private final UsageService usageService;
    private final Tracer tracer;
    private final ServerlessApiProtections apiProtections;

    public RestController(
        RestInterceptor restInterceptor,
        NodeClient client,
        CircuitBreakerService circuitBreakerService,
        UsageService usageService,
        Tracer tracer
    ) {
        this.usageService = usageService;
        this.tracer = tracer;
        if (restInterceptor == null) {
            restInterceptor = (request, channel, targetHandler, listener) -> listener.onResponse(Boolean.TRUE);
        }
        this.interceptor = restInterceptor;
        this.client = client;
        this.circuitBreakerService = circuitBreakerService;
        registerHandlerNoWrap(RestRequest.Method.GET, "/favicon.ico", RestApiVersion.current(), new RestFavIconHandler());
        this.apiProtections = new ServerlessApiProtections(false);
    }

    public ServerlessApiProtections getApiProtections() {
        return apiProtections;
    }

    /**
     * Registers a REST handler to be executed when the provided {@code method} and {@code path} match the request.
     *
     * @param method GET, POST, etc.
     * @param path Path to handle (e.g. "/{index}/{type}/_bulk")
     * @param version API version to handle (e.g. RestApiVersion.V_8)
     * @param handler The handler to actually execute
     * @param deprecationMessage The message to log and send as a header in the response
     */
    protected void registerAsDeprecatedHandler(
        RestRequest.Method method,
        String path,
        RestApiVersion version,
        RestHandler handler,
        String deprecationMessage
    ) {
        registerAsDeprecatedHandler(method, path, version, handler, deprecationMessage, null);
    }

    /**
     * Registers a REST handler to be executed when the provided {@code method} and {@code path} match the request.
     *
     * @param method GET, POST, etc.
     * @param path Path to handle (e.g. "/{index}/{type}/_bulk")
     * @param version API version to handle (e.g. RestApiVersion.V_8)
     * @param handler The handler to actually execute
     * @param deprecationMessage The message to log and send as a header in the response
     * @param deprecationLevel The deprecation log level to use for the deprecation warning, either WARN or CRITICAL
     */
    protected void registerAsDeprecatedHandler(
        RestRequest.Method method,
        String path,
        RestApiVersion version,
        RestHandler handler,
        String deprecationMessage,
        @Nullable Level deprecationLevel
    ) {
        assert (handler instanceof DeprecationRestHandler) == false;
        if (version == RestApiVersion.current()) {
            registerHandler(
                method,
                path,
                version,
                new DeprecationRestHandler(handler, method, path, deprecationLevel, deprecationMessage, deprecationLogger, false)
            );
        } else if (version == RestApiVersion.minimumSupported()) {
            registerHandler(
                method,
                path,
                version,
                new DeprecationRestHandler(handler, method, path, deprecationLevel, deprecationMessage, deprecationLogger, true)
            );
        } else {
            logger.debug(
                "Deprecated route ["
                    + method
                    + " "
                    + path
                    + "] for handler ["
                    + handler.getClass()
                    + "] "
                    + "with version ["
                    + version
                    + "], which is less than the minimum supported version ["
                    + RestApiVersion.minimumSupported()
                    + "]"
            );
        }
    }

    /**
     * Registers a REST handler to be executed when the provided {@code method} and {@code path} match the request, or when provided
     * with {@code replacedMethod} and {@code replacedPath}. Expected usage:
     * <pre><code>
     * 
     * controller.registerAsDeprecatedHandler(POST, "/_forcemerge", RestApiVersion.V_8, someHandler,
     *                                        POST, "/_optimize", RestApiVersion.V_7);
     * controller.registerAsDeprecatedHandler(POST, "/{index}/_forcemerge", RestApiVersion.V_8, someHandler,
     *                                        POST, "/{index}/_optimize", RestApiVersion.V_7);
     * </code></pre>
     * <p>
     * The registered REST handler ({@code method} with {@code path}) is a normal REST handler that is not deprecated and it is
     * replacing the deprecated REST handler ({@code replacedMethod} with {@code replacedPath}) that is using the <em>same</em>
     * {@code handler}.
     * <p>
     * Deprecated REST handlers without a direct replacement should be deprecated directly using {@link #registerAsDeprecatedHandler}
     * and a specific message.
     *
     * @param method GET, POST, etc.
     * @param path Path to handle (e.g. "/_forcemerge")
     * @param version API version to handle (e.g. RestApiVersion.V_8)
     * @param handler The handler to actually execute
     * @param replacedMethod GET, POST, etc.
     * @param replacedPath <em>Replaced</em> path to handle (e.g. "/_optimize")
     * @param replacedVersion <em>Replaced</em> API version to handle (e.g. RestApiVersion.V_7)
     */
    protected void registerAsReplacedHandler(
        RestRequest.Method method,
        String path,
        RestApiVersion version,
        RestHandler handler,
        RestRequest.Method replacedMethod,
        String replacedPath,
        RestApiVersion replacedVersion
    ) {
        final String replacedMessage = "["
            + replacedMethod.name()
            + " "
            + replacedPath
            + "] is deprecated! Use ["
            + method.name()
            + " "
            + path
            + "] instead.";

        registerHandler(method, path, version, handler);
        registerAsDeprecatedHandler(replacedMethod, replacedPath, replacedVersion, handler, replacedMessage);
    }

    /**
     * Registers a REST handler to be executed when one of the provided methods and path match the request.
     *
     * @param method GET, POST, etc.
     * @param path Path to handle (e.g. "/{index}/{type}/_bulk")
     * @param version API version to handle (e.g. RestApiVersion.V_8)
     * @param handler The handler to actually execute
     */
    protected void registerHandler(RestRequest.Method method, String path, RestApiVersion version, RestHandler handler) {
        if (handler instanceof BaseRestHandler) {
            usageService.addRestHandler((BaseRestHandler) handler);
        }
        registerHandlerNoWrap(method, path, version, handler);
    }

    private void registerHandlerNoWrap(RestRequest.Method method, String path, RestApiVersion version, RestHandler handler) {
        assert RestApiVersion.minimumSupported() == version || RestApiVersion.current() == version
            : "REST API compatibility is only supported for version " + RestApiVersion.minimumSupported().major;

        if (RESERVED_PATHS.contains(path)) {
            throw new IllegalArgumentException("path [" + path + "] is a reserved path and may not be registered");
        }
        assert method != RestRequest.Method.OPTIONS : "There should be no handlers registered for the OPTIONS HTTP method";
        handlers.insertOrUpdate(
            path,
            new MethodHandlers(path).addMethod(method, version, handler),
            (handlers, ignoredHandler) -> handlers.addMethod(method, version, handler)
        );
    }

    public void registerHandler(final Route route, final RestHandler handler) {
        if (route.isReplacement()) {
            Route replaced = route.getReplacedRoute();
            registerAsReplacedHandler(
                route.getMethod(),
                route.getPath(),
                route.getRestApiVersion(),
                handler,
                replaced.getMethod(),
                replaced.getPath(),
                replaced.getRestApiVersion()
            );
        } else if (route.isDeprecated()) {
            registerAsDeprecatedHandler(
                route.getMethod(),
                route.getPath(),
                route.getRestApiVersion(),
                handler,
                route.getDeprecationMessage(),
                route.getDeprecationLevel()
            );
        } else {
            registerHandler(route.getMethod(), route.getPath(), route.getRestApiVersion(), handler);
        }
    }

    /**
     * Registers a REST handler with the controller. The REST handler declares the {@code method}
     * and {@code path} combinations.
     */
    public void registerHandler(final RestHandler handler) {
        handler.routes().forEach(route -> registerHandler(route, handler));
    }

    @Override
    public void dispatchRequest(RestRequest request, RestChannel channel, ThreadContext threadContext) {
        threadContext.addResponseHeader(ELASTIC_PRODUCT_HTTP_HEADER, ELASTIC_PRODUCT_HTTP_HEADER_VALUE);
        try {
            tryAllHandlers(request, channel, threadContext);
        } catch (Exception e) {
            try {
                sendFailure(channel, e);
            } catch (Exception inner) {
                inner.addSuppressed(e);
                logger.error(() -> "failed to send failure response for uri [" + request.uri() + "]", inner);
            }
        }
    }

    @Override
    public void dispatchBadRequest(final RestChannel channel, final ThreadContext threadContext, final Throwable cause) {
        threadContext.addResponseHeader(ELASTIC_PRODUCT_HTTP_HEADER, ELASTIC_PRODUCT_HTTP_HEADER_VALUE);
        try {
            final Exception e;
            if (cause == null) {
                e = new ElasticsearchException("unknown cause");
            } else if (cause instanceof Exception) {
                e = (Exception) cause;
            } else {
                e = new ElasticsearchException(cause);
            }
            if (e instanceof HttpHeadersValidationException) {
                sendFailure(channel, (Exception) e.getCause());
            } else {
                channel.sendResponse(new RestResponse(channel, BAD_REQUEST, e));
            }
        } catch (final IOException e) {
            if (cause != null) {
                e.addSuppressed(cause);
            }
            logger.warn("failed to send bad request response", e);
            channel.sendResponse(new RestResponse(INTERNAL_SERVER_ERROR, RestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
        }
    }

    @Override
    public Map<String, HttpRouteStats> getStats() {
        final Iterator<MethodHandlers> methodHandlersIterator = handlers.allNodeValues();
        final SortedMap<String, HttpRouteStats> allStats = new TreeMap<>();
        while (methodHandlersIterator.hasNext()) {
            final MethodHandlers mh = methodHandlersIterator.next();
            final HttpRouteStats stats = mh.getStats();
            if (stats.requestCount() > 0 || stats.responseCount() > 0) {
                allStats.put(mh.getPath(), stats);
            }
        }
        return Collections.unmodifiableSortedMap(allStats);
    }

    private void dispatchRequest(
        RestRequest request,
        RestChannel channel,
        RestHandler handler,
        MethodHandlers methodHandlers,
        ThreadContext threadContext
    ) throws Exception {
        final int contentLength = request.contentLength();
        if (contentLength > 0) {
            if (isContentTypeDisallowed(request) || handler.mediaTypesValid(request) == false) {
                sendContentTypeErrorMessage(request.getAllHeaderValues("Content-Type"), channel);
                return;
            }
            final XContentType xContentType = request.getXContentType();
            if (handler.supportsContentStream()
                && XContentType.JSON != xContentType.canonical()
                && XContentType.SMILE != xContentType.canonical()) {
                channel.sendResponse(
                    RestResponse.createSimpleErrorResponse(
                        channel,
                        RestStatus.NOT_ACCEPTABLE,
                        "Content-Type [" + xContentType + "] does not support stream parsing. Use JSON or SMILE instead"
                    )
                );
                return;
            }
        }
        RestChannel responseChannel = channel;
        if (apiProtections.isEnabled()) {
            Scope scope = handler.getServerlessScope();
            if (scope == null) {
                handleServerlessRequestToProtectedResource(request.uri(), request.method(), responseChannel);
                return;
            }
        }
        try {
            if (handler.canTripCircuitBreaker()) {
                inFlightRequestsBreaker(circuitBreakerService).addEstimateBytesAndMaybeBreak(contentLength, "<http_request>");
            } else {
                inFlightRequestsBreaker(circuitBreakerService).addWithoutBreaking(contentLength);
            }
            responseChannel = new ResourceHandlingHttpChannel(channel, circuitBreakerService, contentLength, methodHandlers);
            if (handler.allowsUnsafeBuffers() == false) {
                request.ensureSafeBuffers();
            }

            if (handler.allowSystemIndexAccessByDefault() == false) {
                final String prodOriginValue = request.header(Task.X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER);
                if (prodOriginValue != null) {
                    threadContext.putHeader(SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY, Boolean.TRUE.toString());
                    threadContext.putHeader(EXTERNAL_SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY, prodOriginValue);
                } else {
                    threadContext.putHeader(SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY, Boolean.FALSE.toString());
                }
            } else {
                threadContext.putHeader(SYSTEM_INDEX_ACCESS_CONTROL_HEADER_KEY, Boolean.TRUE.toString());
            }
            final var finalChannel = responseChannel;
            this.interceptor.intercept(request, responseChannel, handler.getConcreteRestHandler(), new ActionListener<>() {
                @Override
                public void onResponse(Boolean processRequest) {
                    if (processRequest) {
                        try {
                            validateRequest(request, handler, client);
                            handler.handleRequest(request, finalChannel, client);
                        } catch (Exception e) {
                            onFailure(e);
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    try {
                        sendFailure(finalChannel, e);
                    } catch (IOException ex) {
                        logger.info("Failed to send error [{}] to HTTP client", ex.toString());
                    }
                }
            });
        } catch (Exception e) {
            sendFailure(responseChannel, e);
        }
    }

    /**
     * Validates that the request should be allowed. Throws an exception if the request should be rejected.
     */
    @SuppressWarnings("unused")
    protected void validateRequest(RestRequest request, RestHandler handler, NodeClient client) throws ElasticsearchStatusException {}

    private static void sendFailure(RestChannel responseChannel, Exception e) throws IOException {
        responseChannel.sendResponse(new RestResponse(responseChannel, e));
    }

    /**
     * in order to prevent CSRF we have to reject all media types that are from a browser safelist
     * see https:
     * see https:
     * @param request
     */
    private static boolean isContentTypeDisallowed(RestRequest request) {
        return request.getParsedContentType() != null
            && SAFELISTED_MEDIA_TYPES.contains(request.getParsedContentType().mediaTypeWithoutParameters());
    }

    private boolean handleNoHandlerFound(
        ThreadContext threadContext,
        String rawPath,
        RestRequest.Method method,
        String uri,
        RestChannel channel
    ) {
        final Set<RestRequest.Method> validMethodSet = getValidHandlerMethodSet(rawPath);
        if (validMethodSet.contains(method) == false) {
            if (method == RestRequest.Method.OPTIONS) {
                startTrace(threadContext, channel);
                handleOptionsRequest(channel, validMethodSet);
                return true;
            }
            if (validMethodSet.isEmpty() == false) {
                startTrace(threadContext, channel);
                handleUnsupportedHttpMethod(uri, method, channel, validMethodSet, null);
                return true;
            }
        }
        return false;
    }

    private void startTrace(ThreadContext threadContext, RestChannel channel) {
        startTrace(threadContext, channel, null);
    }

    private void startTrace(ThreadContext threadContext, RestChannel channel, String restPath) {
        final RestRequest req = channel.request();
        if (restPath == null) {
            restPath = req.path();
        }
        String method = null;
        try {
            method = req.method().name();
        } catch (IllegalArgumentException e) {
        }
        String name;
        if (method != null) {
            name = method + " " + restPath;
        } else {
            name = restPath;
        }

        final Map<String, Object> attributes = Maps.newMapWithExpectedSize(req.getHeaders().size() + 3);
        req.getHeaders().forEach((key, values) -> {
            final String lowerKey = key.toLowerCase(Locale.ROOT).replace('-', '_');
            attributes.put("http.request.headers." + lowerKey, values.size() == 1 ? values.get(0) : String.join("; ", values));
        });
        attributes.put("http.method", method);
        attributes.put("http.url", req.uri());
        switch (req.getHttpRequest().protocolVersion()) {
            case HTTP_1_0 -> attributes.put("http.flavour", "1.0");
            case HTTP_1_1 -> attributes.put("http.flavour", "1.1");
        }

        tracer.startTrace(threadContext, channel.request(), name, attributes);
    }

    private void traceException(RestChannel channel, Throwable e) {
        this.tracer.addError(channel.request(), e);
    }

    private static void sendContentTypeErrorMessage(@Nullable List<String> contentTypeHeader, RestChannel channel) throws IOException {
        final String errorMessage;
        if (contentTypeHeader == null) {
            errorMessage = "Content-Type header is missing";
        } else {
            errorMessage = "Content-Type header [" + Strings.collectionToCommaDelimitedString(contentTypeHeader) + "] is not supported";
        }

        channel.sendResponse(RestResponse.createSimpleErrorResponse(channel, NOT_ACCEPTABLE, errorMessage));
    }

    private void tryAllHandlers(final RestRequest request, final RestChannel channel, final ThreadContext threadContext) throws Exception {
        try {
            validateErrorTrace(request, channel);
        } catch (IllegalArgumentException e) {
            startTrace(threadContext, channel);
            channel.sendResponse(RestResponse.createSimpleErrorResponse(channel, BAD_REQUEST, e.getMessage()));
            return;
        }

        final String rawPath = request.rawPath();
        final String uri = request.uri();
        final RestRequest.Method requestMethod;

        RestApiVersion restApiVersion = request.getRestApiVersion();
        try {
            requestMethod = request.method();
            Iterator<MethodHandlers> allHandlers = getAllHandlers(request.params(), rawPath);
            while (allHandlers.hasNext()) {
                final RestHandler handler;
                final MethodHandlers handlers = allHandlers.next();
                if (handlers == null) {
                    handler = null;
                } else {
                    handler = handlers.getHandler(requestMethod, restApiVersion);
                }
                if (handler == null) {
                    if (handleNoHandlerFound(threadContext, rawPath, requestMethod, uri, channel)) {
                        return;
                    }
                } else {
                    startTrace(threadContext, channel, handlers.getPath());
                    dispatchRequest(request, channel, handler, handlers, threadContext);
                    return;
                }
            }
        } catch (final IllegalArgumentException e) {
            startTrace(threadContext, channel);
            traceException(channel, e);
            handleUnsupportedHttpMethod(uri, null, channel, getValidHandlerMethodSet(rawPath), e);
            return;
        }
        startTrace(threadContext, channel);
        handleBadRequest(uri, requestMethod, channel);
    }

    private static void validateErrorTrace(RestRequest request, RestChannel channel) {
        if (request.paramAsBoolean("error_trace", false) && channel.detailedErrorsEnabled() == false) {
            throw new IllegalArgumentException("error traces in responses are disabled.");
        }
    }

    Iterator<MethodHandlers> getAllHandlers(@Nullable Map<String, String> requestParamsRef, String rawPath) {
        final Supplier<Map<String, String>> paramsSupplier;
        if (requestParamsRef == null) {
            paramsSupplier = () -> null;
        } else {
            final Map<String, String> originalParams = Map.copyOf(requestParamsRef);
            paramsSupplier = () -> {
                requestParamsRef.clear();
                requestParamsRef.putAll(originalParams);
                return requestParamsRef;
            };
        }
        return handlers.retrieveAll(rawPath, paramsSupplier);
    }

    /**
     * Returns the holder for search usage statistics, to be used to track search usage when parsing
     * incoming search requests from the relevant REST endpoints. This is exposed for plugins that
     * expose search functionalities which need to contribute to the search usage statistics.
     */
    public SearchUsageHolder getSearchUsageHolder() {
        return usageService.getSearchUsageHolder();
    }

    /**
     * Handle requests to a valid REST endpoint using an unsupported HTTP
     * method. A 405 HTTP response code is returned, and the response 'Allow'
     * header includes a list of valid HTTP methods for the endpoint (see
     * <a href="https:
     * 10.4.6 - 405 Method Not Allowed</a>).
     */
    private static void handleUnsupportedHttpMethod(
        String uri,
        @Nullable RestRequest.Method method,
        final RestChannel channel,
        final Set<RestRequest.Method> validMethodSet,
        @Nullable final IllegalArgumentException exception
    ) {
        try {
            final StringBuilder msg = new StringBuilder();
            if (exception == null) {
                msg.append("Incorrect HTTP method for uri [").append(uri);
                msg.append("] and method [").append(method).append("]");
            } else {
                msg.append(exception.getMessage());
            }
            if (validMethodSet.isEmpty() == false) {
                msg.append(", allowed: ").append(validMethodSet);
            }
            RestResponse restResponse = RestResponse.createSimpleErrorResponse(channel, METHOD_NOT_ALLOWED, msg.toString());
            if (validMethodSet.isEmpty() == false) {
                restResponse.addHeader("Allow", Strings.collectionToDelimitedString(validMethodSet, ","));
            }
            channel.sendResponse(restResponse);
        } catch (final IOException e) {
            logger.warn("failed to send bad request response", e);
            channel.sendResponse(new RestResponse(INTERNAL_SERVER_ERROR, RestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
        }
    }

    /**
     * Handle HTTP OPTIONS requests to a valid REST endpoint. A 200 HTTP
     * response code is returned, and the response 'Allow' header includes a
     * list of valid HTTP methods for the endpoint (see
     * <a href="https:
     * - Options</a>).
     */
    private static void handleOptionsRequest(RestChannel channel, Set<RestRequest.Method> validMethodSet) {
        RestResponse restResponse = new RestResponse(OK, TEXT_CONTENT_TYPE, BytesArray.EMPTY);
        if (validMethodSet.isEmpty() == false) {
            restResponse.addHeader("Allow", Strings.collectionToDelimitedString(validMethodSet, ","));
        }
        channel.sendResponse(restResponse);
    }

    /**
     * Handle a requests with no candidate handlers (return a 400 Bad Request
     * error).
     */
    public static void handleBadRequest(String uri, RestRequest.Method method, RestChannel channel) throws IOException {
        try (XContentBuilder builder = channel.newErrorBuilder()) {
            builder.startObject();
            {
                builder.field("error", "no handler found for uri [" + uri + "] and method [" + method + "]");
            }
            builder.endObject();
            channel.sendResponse(new RestResponse(BAD_REQUEST, builder));
        }
    }

    public static void handleServerlessRequestToProtectedResource(String uri, RestRequest.Method method, RestChannel channel)
        throws IOException {
        String msg = "uri [" + uri + "] with method [" + method + "] exists but is not available when running in serverless mode";
        sendFailure(channel, new ApiNotAvailableException(msg));
    }

    /**
     * Get the valid set of HTTP methods for a REST request.
     */
    private Set<RestRequest.Method> getValidHandlerMethodSet(String rawPath) {
        Set<RestRequest.Method> validMethods = new HashSet<>();
        Iterator<MethodHandlers> allHandlers = getAllHandlers(null, rawPath);
        while (allHandlers.hasNext()) {
            final MethodHandlers methodHandlers = allHandlers.next();
            if (methodHandlers != null) {
                validMethods.addAll(methodHandlers.getValidMethods());
            }
        }
        return validMethods;
    }

    private static final class ResourceHandlingHttpChannel implements RestChannel {
        private final RestChannel delegate;
        private final CircuitBreakerService circuitBreakerService;
        private final int contentLength;
        private final MethodHandlers methodHandlers;
        private final long startTime;
        private final AtomicBoolean closed = new AtomicBoolean();

        ResourceHandlingHttpChannel(
            RestChannel delegate,
            CircuitBreakerService circuitBreakerService,
            int contentLength,
            MethodHandlers methodHandlers
        ) {
            this.delegate = delegate;
            this.circuitBreakerService = circuitBreakerService;
            this.contentLength = contentLength;
            this.methodHandlers = methodHandlers;
            this.startTime = rawRelativeTimeInMillis();
        }

        @Override
        public XContentBuilder newBuilder() throws IOException {
            return delegate.newBuilder();
        }

        @Override
        public XContentBuilder newErrorBuilder() throws IOException {
            return delegate.newErrorBuilder();
        }

        @Override
        public XContentBuilder newBuilder(@Nullable XContentType xContentType, boolean useFiltering) throws IOException {
            return delegate.newBuilder(xContentType, useFiltering);
        }

        @Override
        public XContentBuilder newBuilder(XContentType xContentType, XContentType responseContentType, boolean useFiltering)
            throws IOException {
            return delegate.newBuilder(xContentType, responseContentType, useFiltering);
        }

        @Override
        public XContentBuilder newBuilder(
            XContentType xContentType,
            XContentType responseContentType,
            boolean useFiltering,
            OutputStream out
        ) throws IOException {
            return delegate.newBuilder(xContentType, responseContentType, useFiltering, out);
        }

        @Override
        public BytesStream bytesOutput() {
            return delegate.bytesOutput();
        }

        @Override
        public void releaseOutputBuffer() {
            delegate.releaseOutputBuffer();
        }

        @Override
        public RestRequest request() {
            return delegate.request();
        }

        @Override
        public boolean detailedErrorsEnabled() {
            return delegate.detailedErrorsEnabled();
        }

        @Override
        public void sendResponse(RestResponse response) {
            boolean success = false;
            try {
                close();
                methodHandlers.addRequestStats(contentLength);
                methodHandlers.addResponseTime(rawRelativeTimeInMillis() - startTime);
                if (response.isChunked() == false) {
                    methodHandlers.addResponseStats(response.content().length());
                } else {
                    final var responseLengthRecorder = new ResponseLengthRecorder(methodHandlers);
                    final var headers = response.getHeaders();
                    response = RestResponse.chunked(
                        response.status(),
                        new EncodedLengthTrackingChunkedRestResponseBody(response.chunkedContent(), responseLengthRecorder),
                        Releasables.wrap(responseLengthRecorder, response)
                    );
                    for (final var header : headers.entrySet()) {
                        for (final var value : header.getValue()) {
                            response.addHeader(header.getKey(), value);
                        }
                    }
                }
                delegate.sendResponse(response);
                success = true;
            } finally {
                if (success == false) {
                    releaseOutputBuffer();
                }
            }
        }

        private static long rawRelativeTimeInMillis() {
            return TimeValue.nsecToMSec(System.nanoTime());
        }

        private void close() {
            if (closed.compareAndSet(false, true) == false) {
                throw new IllegalStateException("Channel is already closed");
            }
            inFlightRequestsBreaker(circuitBreakerService).addWithoutBreaking(-contentLength);
        }
    }

    private static class ResponseLengthRecorder extends AtomicReference<MethodHandlers> implements Releasable {
        private long responseLength;

        private ResponseLengthRecorder(MethodHandlers methodHandlers) {
            super(methodHandlers);
        }

        @Override
        public void close() {
            final var methodHandlers = getAndSet(null);
            if (methodHandlers != null) {
                assert responseLength == 0L || Transports.assertTransportThread();
                methodHandlers.addResponseStats(responseLength);
            }
        }

        void addChunkLength(long chunkLength) {
            assert chunkLength >= 0L : chunkLength;
            assert Transports.assertTransportThread(); 
            responseLength += chunkLength;
        }
    }

    private static class EncodedLengthTrackingChunkedRestResponseBody implements ChunkedRestResponseBody {

        private final ChunkedRestResponseBody delegate;
        private final ResponseLengthRecorder responseLengthRecorder;

        private EncodedLengthTrackingChunkedRestResponseBody(
            ChunkedRestResponseBody delegate,
            ResponseLengthRecorder responseLengthRecorder
        ) {
            this.delegate = delegate;
            this.responseLengthRecorder = responseLengthRecorder;
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public ReleasableBytesReference encodeChunk(int sizeHint, Recycler<BytesRef> recycler) throws IOException {
            final ReleasableBytesReference bytesReference = delegate.encodeChunk(sizeHint, recycler);
            responseLengthRecorder.addChunkLength(bytesReference.length());
            if (isDone()) {
                responseLengthRecorder.close();
            }
            return bytesReference;
        }

        @Override
        public String getResponseContentTypeString() {
            return delegate.getResponseContentTypeString();
        }
    }

    private static CircuitBreaker inFlightRequestsBreaker(CircuitBreakerService circuitBreakerService) {
        return circuitBreakerService.getBreaker(CircuitBreaker.IN_FLIGHT_REQUESTS);
    }

    @ServerlessScope(Scope.PUBLIC)
    private static final class RestFavIconHandler implements RestHandler {
        @Override
        public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
            channel.sendResponse(new RestResponse(RestStatus.OK, "image/x-icon", FAVICON_RESPONSE));
        }
    }
}
