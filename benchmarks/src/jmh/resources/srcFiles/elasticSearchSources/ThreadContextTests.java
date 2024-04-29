/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.common.util.concurrent;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.logging.HeaderWarning;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomAsciiLettersOfLengthBetween;
import static org.elasticsearch.tasks.Task.HEADERS_TO_COPY;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class ThreadContextTests extends ESTestCase {

    public void testStashContext() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            assertNull(threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertEquals("1", threadContext.getHeader("default"));
        }

        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
    }

    public void testStashContextPreservesDefaultHeadersToCopy() {
        for (String header : HEADERS_TO_COPY) {
            ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
            threadContext.putHeader(header, "bar");
            try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
                assertEquals("bar", threadContext.getHeader(header));
            }
        }
    }

    public void testStashContextPreservingRequestHeaders() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putHeader("bar", "foo");
        threadContext.putTransient("ctx.foo", 1);
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        try (ThreadContext.StoredContext ignored = threadContext.stashContextPreservingRequestHeaders("foo", "ctx.foo", "missing")) {
            assertEquals("bar", threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertNull(threadContext.getHeader("bar"));
            assertEquals("1", threadContext.getHeader("default"));
            assertNull(threadContext.getHeader("missing"));
        }

        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals("foo", threadContext.getHeader("bar"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
    }

    public void testStashContextPreservingHeadersWithDefaultHeadersToCopy() {
        for (String header : HEADERS_TO_COPY) {
            ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
            threadContext.putHeader(header, "bar");
            try (ThreadContext.StoredContext ignored = threadContext.stashContextPreservingRequestHeaders()) {
                assertEquals("bar", threadContext.getHeader(header));
            }
            try (ThreadContext.StoredContext ignored = threadContext.stashContextPreservingRequestHeaders(header)) {
                assertEquals("bar", threadContext.getHeader(header));
            }
        }
    }

    public void testNewContextWithClearedTransients() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        threadContext.putTransient("foo", "bar");
        threadContext.putTransient("bar", "baz");
        threadContext.putHeader("foo", "bar");
        threadContext.putHeader("baz", "bar");
        threadContext.addResponseHeader("foo", "bar");
        threadContext.addResponseHeader("bar", "qux");

        if (randomBoolean()) {
            threadContext.putTransient("acme", null);
        }

        try (
            ThreadContext.StoredContext stashed = threadContext.newStoredContext(
                randomFrom(List.of("foo", "foo"), List.of("foo"), List.of("foo", "acme")),
                List.of()
            )
        ) {
            assertNull(threadContext.getTransient("foo"));
            assertNull(threadContext.getTransient("acme"));
            assertEquals("baz", threadContext.getTransient("bar"));
            assertEquals("bar", threadContext.getHeader("foo"));
            assertEquals("bar", threadContext.getHeader("baz"));
            assertEquals("bar", threadContext.getResponseHeaders().get("foo").get(0));
            assertEquals("qux", threadContext.getResponseHeaders().get("bar").get(0));

            threadContext.putTransient("foo", "acme");
            assertEquals("acme", threadContext.getTransient("foo"));
            threadContext.putTransient("baz", "bar");
            threadContext.putHeader("bar", "baz");
            threadContext.addResponseHeader("baz", "bar");
            threadContext.addResponseHeader("foo", "baz");
        }

        assertEquals("bar", threadContext.getTransient("foo"));
        assertNull(threadContext.getTransient("baz"));
        assertNull(threadContext.getHeader("bar"));
        assertNull(threadContext.getResponseHeaders().get("baz"));
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals("bar", threadContext.getHeader("baz"));
        assertEquals("bar", threadContext.getResponseHeaders().get("foo").get(0));
        assertEquals(1, threadContext.getResponseHeaders().get("foo").size());
        assertEquals("qux", threadContext.getResponseHeaders().get("bar").get(0));

        try (
            ThreadContext.StoredContext stashed = threadContext.newStoredContext(
                randomFrom(Arrays.asList("acme", "acme"), Arrays.asList("acme")),
                List.of()
            )
        ) {
            assertNull(threadContext.getTransient("acme"));
            threadContext.putTransient("acme", "foo");
        }
        assertNull(threadContext.getTransient("acme"));
    }

    public void testNewContextWithClearedRequestHeaders() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        final Map<String, String> requestHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry(Task.X_OPAQUE_ID_HTTP_HEADER, randomAlphaOfLength(10)),
            Map.entry(Task.TRACE_ID, randomAlphaOfLength(20)),
            Map.entry("_username", "elastic-admin")
        );
        threadContext.putHeader(requestHeaders);

        final Map<String, Object> transientHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_random", randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_map", Map.of("key", new Object())),
            Map.entry("_address", "125.124.123.122"),
            Map.entry("_object", new Object()),
            Map.entry("_number", 42)
        );
        transientHeaders.forEach((k, v) -> threadContext.putTransient(k, v));

        final Map<String, String> responseHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 6), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_response_message", "All good."),
            Map.entry("Warning", "Some warning!")
        );
        responseHeaders.forEach((k, v) -> threadContext.addResponseHeader(k, v));

        if (randomBoolean()) {
            threadContext.putHeader("_missing_or_null", null);
        }

        boolean setSystemContext = randomBoolean();
        if (setSystemContext) {
            threadContext.markAsSystemContext();
        }

        threadContext.putHeader("_password", "elastic-password");

        try (
            ThreadContext.StoredContext stashed = threadContext.newStoredContext(
                List.of(),
                randomFrom(List.of("_password", "_password"), List.of("_password"), List.of("_password", "_missing_or_null"))
            )
        ) {
            assertThat(threadContext.getHeader("_password"), nullValue());
            assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));
            assertThat(threadContext.getHeader("_missing_or_null"), nullValue());
            requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
            transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
            responseHeaders.forEach((k, v) -> assertThat(threadContext.getResponseHeaders().get(k).get(0), equalTo(v)));
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(1));

            threadContext.putHeader("_password", "new-password");
            assertThat(threadContext.getHeader("_password"), equalTo("new-password"));
            threadContext.addResponseHeader("_new_response_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.putTransient("_new_transient_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.putHeader("_new_request_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.addResponseHeader("Warning", randomAlphaOfLengthBetween(3, 8));
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(2));
        }

        assertThat(threadContext.getHeader("_password"), equalTo("elastic-password"));
        assertThat(threadContext.getResponseHeaders().get("_new_response_header"), nullValue());
        assertThat(threadContext.getTransient("_new_transient_header"), nullValue());
        assertThat(threadContext.getHeader("_new_request_header"), nullValue());
        assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(1));
        assertThat(threadContext.getResponseHeaders().get("Warning").get(0), equalTo("Some warning!"));
        requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
        transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
        responseHeaders.forEach((k, v) -> assertThat(threadContext.getResponseHeaders().get(k).get(0), equalTo(v)));
        assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));

        try (
            ThreadContext.StoredContext stashed = threadContext.newStoredContext(
                randomFrom(Arrays.asList("_missing_or_null", "_missing_or_null"), Arrays.asList("_missing_or_null")),
                List.of()
            )
        ) {
            assertThat(threadContext.getHeader("_missing_or_null"), nullValue());
            threadContext.putHeader("_missing_or_null", "not_null");
        }
        assertThat(threadContext.getHeader("_missing_or_null"), nullValue());
    }

    public void testNewContextWithoutClearingTransientAndRequestHeaders() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        final Map<String, String> requestHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry(Task.X_OPAQUE_ID_HTTP_HEADER, randomAlphaOfLength(10)),
            Map.entry(Task.TRACE_ID, randomAlphaOfLength(20)),
            Map.entry("_username", "elastic-admin")
        );
        threadContext.putHeader(requestHeaders);

        final Map<String, Object> transientHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_random", randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_map", Map.of("key", new Object())),
            Map.entry("_address", "125.124.123.122"),
            Map.entry("_object", new Object()),
            Map.entry("_number", 42)
        );
        transientHeaders.forEach((k, v) -> threadContext.putTransient(k, v));

        final Map<String, String> responseHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 6), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_response_message", "All good."),
            Map.entry("Warning", "Some warning!")
        );
        responseHeaders.forEach((k, v) -> threadContext.addResponseHeader(k, v));

        boolean setSystemContext = randomBoolean();
        if (setSystemContext) {
            threadContext.markAsSystemContext();
        }

        try (ThreadContext.StoredContext stashed = threadContext.newStoredContext(List.of(), List.of())) {
            assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));
            assertThat(threadContext.getHeaders().size(), equalTo(requestHeaders.size()));
            assertThat(threadContext.getResponseHeaders().size(), equalTo(responseHeaders.size()));
            assertThat(threadContext.getTransientHeaders().size(), equalTo(transientHeaders.size()));
            requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
            transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
            responseHeaders.forEach((k, v) -> assertThat(threadContext.getResponseHeaders().get(k).get(0), equalTo(v)));
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(1));
            threadContext.addResponseHeader("_new_response_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.putTransient("_new_transient_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.putHeader("_new_request_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.addResponseHeader("Warning", randomAlphaOfLengthBetween(3, 8));
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(2));
        }

        assertThat(threadContext.getResponseHeaders().get("_new_response_header"), nullValue());
        assertThat(threadContext.getTransient("_new_transient_header"), nullValue());
        assertThat(threadContext.getHeader("_new_request_header"), nullValue());
        assertThat(threadContext.getHeaders().size(), equalTo(requestHeaders.size()));
        assertThat(threadContext.getResponseHeaders().size(), equalTo(responseHeaders.size()));
        assertThat(threadContext.getTransientHeaders().size(), equalTo(transientHeaders.size()));
        requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
        transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
        responseHeaders.forEach((k, v) -> assertThat(threadContext.getResponseHeaders().get(k).get(0), equalTo(v)));
        assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));
        assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(1));
        assertThat(threadContext.getResponseHeaders().get("Warning").get(0), equalTo("Some warning!"));
    }

    public void testNewContextPreservingResponseHeadersWithClearedTransientAndRequestHeaders() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        final Map<String, String> requestHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry(Task.X_OPAQUE_ID_HTTP_HEADER, randomAlphaOfLength(10)),
            Map.entry(Task.TRACE_ID, randomAlphaOfLength(20)),
            Map.entry("_username", "elastic-admin")
        );
        threadContext.putHeader(requestHeaders);

        final Map<String, Object> transientHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_random", randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_map", Map.of("key", new Object())),
            Map.entry("_address", "125.124.123.122"),
            Map.entry("_object", new Object()),
            Map.entry("_number", 42)
        );
        transientHeaders.forEach((k, v) -> threadContext.putTransient(k, v));

        final Map<String, String> responseHeaders = Map.ofEntries(
            Map.entry(randomAlphaOfLengthBetween(3, 6), randomAlphaOfLengthBetween(3, 8)),
            Map.entry("_response_message", "All good."),
            Map.entry("Warning", "Some warning!")
        );
        responseHeaders.forEach((k, v) -> threadContext.addResponseHeader(k, v));

        if (randomBoolean()) {
            threadContext.putHeader("_missing_or_null", null);
            threadContext.putTransient("_missing_or_null", null);
        }

        boolean setSystemContext = randomBoolean();
        if (setSystemContext) {
            threadContext.markAsSystemContext();
        }

        threadContext.putHeader("_password", "elastic-password");
        threadContext.putTransient("_transient_to_be_cleared", "original-transient-value");

        try (
            ThreadContext.StoredContext stashed = threadContext.newStoredContextPreservingResponseHeaders(
                randomFrom(
                    List.of("_transient_to_be_cleared"),
                    List.of("_transient_to_be_cleared", "_transient_to_be_cleared"),
                    List.of("_transient_to_be_cleared", "_missing_or_null")
                ),
                randomFrom(List.of("_password", "_password"), List.of("_password"), List.of("_password", "_missing_or_null"))
            )
        ) {
            assertThat(threadContext.getHeader("_password"), nullValue());
            assertThat(threadContext.getTransient("_transient_to_be_cleared"), nullValue());
            assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));
            assertThat(threadContext.getHeader("_missing_or_null"), nullValue());
            assertThat(threadContext.getTransient("_missing_or_null"), nullValue());
            requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
            transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
            responseHeaders.forEach((k, v) -> assertThat(threadContext.getResponseHeaders().get(k).get(0), equalTo(v)));
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(1));

            threadContext.putHeader("_password", "new-password");
            threadContext.putTransient("_transient_to_be_cleared", "new-transient-value");
            assertThat(threadContext.getHeader("_password"), equalTo("new-password"));
            assertThat(threadContext.getTransient("_transient_to_be_cleared"), equalTo("new-transient-value"));
            threadContext.addResponseHeader("_new_response_header", "value-which-should-be-preserved");
            threadContext.putTransient("_new_transient_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.putHeader("_new_request_header", randomAlphaOfLengthBetween(3, 8));
            threadContext.addResponseHeader("Warning", "Another warning!");
            assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(2));
        }

        assertThat(threadContext.getHeader("_password"), equalTo("elastic-password"));
        assertThat(threadContext.getTransient("_transient_to_be_cleared"), equalTo("original-transient-value"));
        requestHeaders.forEach((k, v) -> assertThat(threadContext.getHeader(k), equalTo(v)));
        transientHeaders.forEach((k, v) -> assertThat(threadContext.getTransient(k), equalTo(v)));
        assertThat(threadContext.getTransient("_new_transient_header"), nullValue());
        assertThat(threadContext.getHeader("_new_request_header"), nullValue());
        assertThat(threadContext.getResponseHeaders().get("_new_response_header").get(0), equalTo("value-which-should-be-preserved"));
        assertThat(threadContext.getResponseHeaders().get("Warning").size(), equalTo(2));
        assertThat(threadContext.getResponseHeaders().get("Warning").get(0), equalTo("Some warning!"));
        assertThat(threadContext.getResponseHeaders().get("Warning").get(1), equalTo("Another warning!"));
        assertThat(threadContext.isSystemContext(), equalTo(setSystemContext));
    }

    public void testStashWithOrigin() {
        final String origin = randomAlphaOfLengthBetween(4, 16);
        final ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        final boolean setOtherValues = randomBoolean();
        if (setOtherValues) {
            threadContext.putTransient("foo", "bar");
            threadContext.putHeader("foo", "bar");
        }

        assertNull(threadContext.getTransient(ThreadContext.ACTION_ORIGIN_TRANSIENT_NAME));
        try (ThreadContext.StoredContext storedContext = threadContext.stashWithOrigin(origin)) {
            assertEquals(origin, threadContext.getTransient(ThreadContext.ACTION_ORIGIN_TRANSIENT_NAME));
            assertNull(threadContext.getTransient("foo"));
            assertNull(threadContext.getTransient("bar"));
        }

        assertNull(threadContext.getTransient(ThreadContext.ACTION_ORIGIN_TRANSIENT_NAME));

        if (setOtherValues) {
            assertEquals("bar", threadContext.getTransient("foo"));
            assertEquals("bar", threadContext.getHeader("foo"));
        }
    }

    public void testStashAndMerge() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        HashMap<String, String> toMerge = new HashMap<>();
        toMerge.put("foo", "baz");
        toMerge.put("simon", "says");
        try (ThreadContext.StoredContext ctx = threadContext.stashAndMergeHeaders(toMerge)) {
            assertEquals("bar", threadContext.getHeader("foo"));
            assertEquals("says", threadContext.getHeader("simon"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertEquals("1", threadContext.getHeader("default"));
        }

        assertNull(threadContext.getHeader("simon"));
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
    }

    public void testStoreContext() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        ThreadContext.StoredContext storedContext = threadContext.newStoredContext();
        threadContext.putHeader("foo.bar", "baz");
        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            assertNull(threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertEquals("1", threadContext.getHeader("default"));
        }

        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        assertEquals("baz", threadContext.getHeader("foo.bar"));
        if (randomBoolean()) {
            storedContext.restore();
        } else {
            storedContext.close();
        }
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        assertNull(threadContext.getHeader("foo.bar"));
    }

    public void testRestorableContext() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);
        threadContext.addResponseHeader("resp.header", "baaaam");
        Supplier<ThreadContext.StoredContext> contextSupplier = threadContext.newRestorableContext(true);

        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            assertNull(threadContext.getHeader("foo"));
            assertEquals("1", threadContext.getHeader("default"));
            threadContext.addResponseHeader("resp.header", "boom");
            try (ThreadContext.StoredContext tmp = contextSupplier.get()) {
                assertEquals("bar", threadContext.getHeader("foo"));
                assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
                assertEquals("1", threadContext.getHeader("default"));
                assertEquals(2, threadContext.getResponseHeaders().get("resp.header").size());
                assertEquals("boom", threadContext.getResponseHeaders().get("resp.header").get(0));
                assertEquals("baaaam", threadContext.getResponseHeaders().get("resp.header").get(1));
            }
            assertNull(threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertEquals(1, threadContext.getResponseHeaders().get("resp.header").size());
            assertEquals("boom", threadContext.getResponseHeaders().get("resp.header").get(0));
        }
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        assertEquals(1, threadContext.getResponseHeaders().get("resp.header").size());
        assertEquals("baaaam", threadContext.getResponseHeaders().get("resp.header").get(0));

        contextSupplier = threadContext.newRestorableContext(false);

        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            assertNull(threadContext.getHeader("foo"));
            assertEquals("1", threadContext.getHeader("default"));
            threadContext.addResponseHeader("resp.header", "boom");
            try (ThreadContext.StoredContext tmp = contextSupplier.get()) {
                assertEquals("bar", threadContext.getHeader("foo"));
                assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
                assertEquals("1", threadContext.getHeader("default"));
                assertEquals(1, threadContext.getResponseHeaders().get("resp.header").size());
                assertEquals("baaaam", threadContext.getResponseHeaders().get("resp.header").get(0));
            }
            assertNull(threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertEquals(1, threadContext.getResponseHeaders().get("resp.header").size());
            assertEquals("boom", threadContext.getResponseHeaders().get("resp.header").get(0));
        }

        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
        assertEquals(1, threadContext.getResponseHeaders().get("resp.header").size());
        assertEquals("baaaam", threadContext.getResponseHeaders().get("resp.header").get(0));
    }

    public void testResponseHeaders() {
        final boolean expectThird = randomBoolean();

        final ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        threadContext.addResponseHeader("foo", "bar");
        if (randomBoolean()) {
            threadContext.addResponseHeader("foo", "bar");
        }

        final String value = HeaderWarning.formatWarning("qux");
        threadContext.addResponseHeader("baz", value, s -> HeaderWarning.extractWarningValueFromWarningHeader(s, false));
        if (randomBoolean()) {
            final String duplicateValue = HeaderWarning.formatWarning("qux");
            threadContext.addResponseHeader("baz", duplicateValue, s -> HeaderWarning.extractWarningValueFromWarningHeader(s, false));
        }

        threadContext.addResponseHeader("Warning", "One is the loneliest number");
        threadContext.addResponseHeader("Warning", "Two can be as bad as one");
        if (expectThird) {
            threadContext.addResponseHeader("Warning", "No is the saddest experience");
        }

        final Map<String, List<String>> responseHeaders = threadContext.getResponseHeaders();
        final List<String> foo = responseHeaders.get("foo");
        final List<String> baz = responseHeaders.get("baz");
        final List<String> warnings = responseHeaders.get("Warning");
        final int expectedWarnings = expectThird ? 3 : 2;

        assertThat(foo, hasSize(1));
        assertThat(baz, hasSize(1));
        assertEquals("bar", foo.get(0));
        assertEquals(value, baz.get(0));
        assertThat(warnings, hasSize(expectedWarnings));
        assertThat(warnings, hasItem(equalTo("One is the loneliest number")));
        assertThat(warnings, hasItem(equalTo("Two can be as bad as one")));

        if (expectThird) {
            assertThat(warnings, hasItem(equalTo("No is the saddest experience")));
        }
    }

    public void testCopyHeaders() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.copyHeaders(Collections.<String, String>emptyMap().entrySet());
        threadContext.copyHeaders(Collections.<String, String>singletonMap("foo", "bar").entrySet());
        assertEquals("bar", threadContext.getHeader("foo"));
    }

    public void testSerialize() throws IOException {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);
        threadContext.addResponseHeader("Warning", "123456");
        if (rarely()) {
            threadContext.addResponseHeader("Warning", "123456");
        }
        threadContext.addResponseHeader("Warning", "234567");

        BytesStreamOutput out = new BytesStreamOutput();
        threadContext.writeTo(out);
        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            assertNull(threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));
            assertTrue(threadContext.getResponseHeaders().isEmpty());
            assertEquals("1", threadContext.getHeader("default"));

            threadContext.readHeaders(out.bytes().streamInput());
            assertEquals("bar", threadContext.getHeader("foo"));
            assertNull(threadContext.getTransient("ctx.foo"));

            final Map<String, List<String>> responseHeaders = threadContext.getResponseHeaders();
            final List<String> warnings = responseHeaders.get("Warning");

            assertThat(responseHeaders.keySet(), hasSize(1));
            assertThat(warnings, hasSize(2));
            assertThat(warnings, hasItem(equalTo("123456")));
            assertThat(warnings, hasItem(equalTo("234567")));
        }
        assertEquals("bar", threadContext.getHeader("foo"));
        assertEquals(Integer.valueOf(1), threadContext.getTransient("ctx.foo"));
        assertEquals("1", threadContext.getHeader("default"));
    }

    public void testSerializeInDifferentContext() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        {
            Settings build = Settings.builder().put("request.headers.default", "1").build();
            ThreadContext threadContext = new ThreadContext(build);
            threadContext.putHeader("foo", "bar");
            threadContext.putTransient("ctx.foo", 1);
            threadContext.addResponseHeader("Warning", "123456");
            if (rarely()) {
                threadContext.addResponseHeader("Warning", "123456");
            }
            threadContext.addResponseHeader("Warning", "234567");

            assertEquals("bar", threadContext.getHeader("foo"));
            assertNotNull(threadContext.getTransient("ctx.foo"));
            assertEquals("1", threadContext.getHeader("default"));
            assertThat(threadContext.getResponseHeaders().keySet(), hasSize(1));
            threadContext.writeTo(out);
        }
        {
            Settings otherSettings = Settings.builder().put("request.headers.default", "5").build();
            ThreadContext otherThreadContext = new ThreadContext(otherSettings);
            otherThreadContext.readHeaders(out.bytes().streamInput());

            assertEquals("bar", otherThreadContext.getHeader("foo"));
            assertNull(otherThreadContext.getTransient("ctx.foo"));
            assertEquals("1", otherThreadContext.getHeader("default"));

            final Map<String, List<String>> responseHeaders = otherThreadContext.getResponseHeaders();
            final List<String> warnings = responseHeaders.get("Warning");

            assertThat(responseHeaders.keySet(), hasSize(1));
            assertThat(warnings, hasSize(2));
            assertThat(warnings, hasItem(equalTo("123456")));
            assertThat(warnings, hasItem(equalTo("234567")));
        }
    }

    public void testSerializeInDifferentContextNoDefaults() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        {
            ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
            threadContext.putHeader("foo", "bar");
            threadContext.putTransient("ctx.foo", 1);

            assertEquals("bar", threadContext.getHeader("foo"));
            assertNotNull(threadContext.getTransient("ctx.foo"));
            assertNull(threadContext.getHeader("default"));
            threadContext.writeTo(out);
        }
        {
            Settings otherSettings = Settings.builder().put("request.headers.default", "5").build();
            ThreadContext otherhreadContext = new ThreadContext(otherSettings);
            otherhreadContext.readHeaders(out.bytes().streamInput());

            assertEquals("bar", otherhreadContext.getHeader("foo"));
            assertNull(otherhreadContext.getTransient("ctx.foo"));
            assertEquals("5", otherhreadContext.getHeader("default"));
        }
    }

    public void testCanResetDefault() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("default", "2");
        assertEquals("2", threadContext.getHeader("default"));
    }

    public void testStashAndMergeWithModifiedDefaults() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        HashMap<String, String> toMerge = new HashMap<>();
        toMerge.put("default", "2");
        try (ThreadContext.StoredContext ctx = threadContext.stashAndMergeHeaders(toMerge)) {
            assertEquals("2", threadContext.getHeader("default"));
        }

        build = Settings.builder().put("request.headers.default", "1").build();
        threadContext = new ThreadContext(build);
        threadContext.putHeader("default", "4");
        toMerge = new HashMap<>();
        toMerge.put("default", "2");
        try (ThreadContext.StoredContext ctx = threadContext.stashAndMergeHeaders(toMerge)) {
            assertEquals("4", threadContext.getHeader("default"));
        }
    }

    public void testPreserveContext() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        Runnable withContext;

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            withContext = threadContext.preserveContext(
                sometimesAbstractRunnable(() -> { assertEquals("bar", threadContext.getHeader("foo")); })
            );
        }

        assertNull(threadContext.getHeader("foo"));

        withContext.run();

        assertNull(threadContext.getHeader("foo"));
    }

    public void testPreserveContextKeepsOriginalContextWhenCalledTwice() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        Runnable originalWithContext;
        Runnable withContext;

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            withContext = threadContext.preserveContext(
                sometimesAbstractRunnable(() -> { assertEquals("bar", threadContext.getHeader("foo")); })
            );
        }

        originalWithContext = withContext;
        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "zot");
            withContext = threadContext.preserveContext(withContext);
        }

        withContext.run();

        assertThat(withContext, sameInstance(originalWithContext));
    }

    public void testPreservesThreadsOriginalContextOnRunException() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        Runnable withContext;

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            boolean systemContext = randomBoolean();
            if (systemContext) {
                threadContext.markAsSystemContext();
            }
            threadContext.putTransient("foo", "bar_transient");
            withContext = threadContext.preserveContext(new AbstractRunnable() {

                @Override
                public void onAfter() {
                    assertEquals(systemContext, threadContext.isSystemContext());
                    assertEquals("bar", threadContext.getHeader("foo"));
                    assertEquals("bar_transient", threadContext.getTransient("foo"));
                    assertNotNull(threadContext.getTransient("failure"));
                    assertEquals("exception from doRun", ((RuntimeException) threadContext.getTransient("failure")).getMessage());
                    assertFalse(threadContext.isDefaultContext());
                    threadContext.putTransient("after", "after");
                }

                @Override
                public void onFailure(Exception e) {
                    assertEquals(systemContext, threadContext.isSystemContext());
                    assertEquals("exception from doRun", e.getMessage());
                    assertEquals("bar", threadContext.getHeader("foo"));
                    assertEquals("bar_transient", threadContext.getTransient("foo"));
                    assertFalse(threadContext.isDefaultContext());
                    threadContext.putTransient("failure", e);
                }

                @Override
                protected void doRun() throws Exception {
                    assertEquals(systemContext, threadContext.isSystemContext());
                    assertEquals("bar", threadContext.getHeader("foo"));
                    assertEquals("bar_transient", threadContext.getTransient("foo"));
                    assertFalse(threadContext.isDefaultContext());
                    throw new RuntimeException("exception from doRun");
                }
            });
        }

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertNull(threadContext.getTransient("failure"));
        assertNull(threadContext.getTransient("after"));
        assertTrue(threadContext.isDefaultContext());

        withContext.run();

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertNull(threadContext.getTransient("failure"));
        assertNull(threadContext.getTransient("after"));
        assertTrue(threadContext.isDefaultContext());

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            threadContext.putTransient("foo", "bar_transient");
            withContext = threadContext.preserveContext(() -> {
                assertEquals("bar", threadContext.getHeader("foo"));
                assertEquals("bar_transient", threadContext.getTransient("foo"));
                assertFalse(threadContext.isDefaultContext());
                threadContext.putTransient("run", true);
                throw new RuntimeException("exception from run");
            });
        }

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertNull(threadContext.getTransient("run"));
        assertTrue(threadContext.isDefaultContext());

        final Runnable runnable = withContext;
        RuntimeException e = expectThrows(RuntimeException.class, runnable::run);
        assertEquals("exception from run", e.getMessage());
        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertNull(threadContext.getTransient("run"));
        assertTrue(threadContext.isDefaultContext());
    }

    public void testPreservesThreadsOriginalContextOnFailureException() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        Runnable withContext;

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            threadContext.putTransient("foo", "bar_transient");
            withContext = threadContext.preserveContext(new AbstractRunnable() {
                @Override
                public void onFailure(Exception e) {
                    throw new RuntimeException("from onFailure", e);
                }

                @Override
                protected void doRun() throws Exception {
                    assertEquals("bar", threadContext.getHeader("foo"));
                    assertEquals("bar_transient", threadContext.getTransient("foo"));
                    assertFalse(threadContext.isDefaultContext());
                    throw new RuntimeException("from doRun");
                }
            });
        }

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertTrue(threadContext.isDefaultContext());

        RuntimeException e = expectThrows(RuntimeException.class, withContext::run);
        assertEquals("from onFailure", e.getMessage());
        assertEquals("from doRun", e.getCause().getMessage());

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertTrue(threadContext.isDefaultContext());
    }

    public void testPreservesThreadsOriginalContextOnAfterException() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        Runnable withContext;

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader("foo", "bar");
            threadContext.putTransient("foo", "bar_transient");
            withContext = threadContext.preserveContext(new AbstractRunnable() {

                @Override
                public void onAfter() {
                    throw new RuntimeException("from onAfter");
                }

                @Override
                public void onFailure(Exception e) {
                    throw new RuntimeException("from onFailure", e);
                }

                @Override
                protected void doRun() throws Exception {
                    assertEquals("bar", threadContext.getHeader("foo"));
                    assertEquals("bar_transient", threadContext.getTransient("foo"));
                    assertFalse(threadContext.isDefaultContext());
                }
            });
        }

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertTrue(threadContext.isDefaultContext());

        RuntimeException e = expectThrows(RuntimeException.class, withContext::run);
        assertEquals("from onAfter", e.getMessage());
        assertNull(e.getCause());

        assertNull(threadContext.getHeader("foo"));
        assertNull(threadContext.getTransient("foo"));
        assertTrue(threadContext.isDefaultContext());
    }

    public void testMarkAsSystemContext() throws IOException {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        assertFalse(threadContext.isSystemContext());
        try (ThreadContext.StoredContext context = threadContext.stashContext()) {
            assertFalse(threadContext.isSystemContext());
            threadContext.markAsSystemContext();
            assertTrue(threadContext.isSystemContext());
        }
        assertFalse(threadContext.isSystemContext());
    }

    public void testPutHeaders() {
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader(Collections.<String, String>emptyMap());
        threadContext.putHeader(Collections.<String, String>singletonMap("foo", "bar"));
        assertEquals("bar", threadContext.getHeader("foo"));
        IllegalArgumentException e = expectThrows(
            IllegalArgumentException.class,
            () -> threadContext.putHeader(Collections.<String, String>singletonMap("foo", "boom"))
        );
        assertEquals("value for key [foo] already present", e.getMessage());
    }

    public void testHeadersCopiedOnStash() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
            threadContext.putHeader(Task.X_OPAQUE_ID_HTTP_HEADER, "x-opaque-id");
            threadContext.putHeader(Task.TRACE_ID, "0af7651916cd43dd8448eb211c80319c");
            threadContext.putHeader(Task.X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER, "kibana");

            try (ThreadContext.StoredContext ignored2 = threadContext.stashContext()) {
                assertEquals("x-opaque-id", threadContext.getHeader(Task.X_OPAQUE_ID_HTTP_HEADER));
                assertEquals("0af7651916cd43dd8448eb211c80319c", threadContext.getHeader(Task.TRACE_ID));
                assertEquals("kibana", threadContext.getHeader(Task.X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER));
            }

            assertEquals("x-opaque-id", threadContext.getHeader(Task.X_OPAQUE_ID_HTTP_HEADER));
            assertEquals("0af7651916cd43dd8448eb211c80319c", threadContext.getHeader(Task.TRACE_ID));
            assertEquals("kibana", threadContext.getHeader(Task.X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER));
        }
    }

    public void testSanitizeHeaders() {
        for (boolean randomizeHeaders : List.of(true, false)) {
            final ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
            final String authorizationHeader = randomCase("authorization");
            final String authorizationHeader2 = randomCase("es-secondary-authorization");
            final String authorizationHeader3 = randomCase("ES-Client-Authentication");
            Set<String> possibleHeaders = Set.of(authorizationHeader, authorizationHeader2, authorizationHeader3);
            Set<String> headers = randomizeHeaders
                ? randomSet(0, possibleHeaders.size(), () -> randomFrom(possibleHeaders))
                : possibleHeaders;

            headers.forEach(header -> threadContext.putHeader(header, randomAsciiLettersOfLengthBetween(1, 10)));

            Set<Tuple<String, String>> additionalHeaders = IntStream.range(0, randomInt(10))
                .mapToObj(i -> new Tuple<>(randomAsciiLettersOfLengthBetween(1, 10) + i, randomAsciiLettersOfLengthBetween(1, 10)))
                .collect(Collectors.toSet());
            additionalHeaders.forEach(t -> threadContext.putHeader(t.v1(), t.v2()));
            Set<String> requestHeaders = threadContext.getHeaders().keySet();
            threadContext.addResponseHeader("authorization", randomAsciiLettersOfLengthBetween(1, 10));
            threadContext.putTransient("authorization", randomAsciiLettersOfLengthBetween(1, 10));
            assertThat(
                requestHeaders,
                containsInAnyOrder(Stream.concat(additionalHeaders.stream().map(Tuple::v1), headers.stream()).toArray())
            );

            threadContext.sanitizeHeaders();

            requestHeaders = threadContext.getHeaders().keySet();
            assertThat(requestHeaders, not(hasItem(authorizationHeader)));
            assertThat(requestHeaders, not(hasItem(authorizationHeader2)));
            assertThat(requestHeaders, not(hasItem(authorizationHeader3)));
            assertThat(requestHeaders, containsInAnyOrder(additionalHeaders.stream().map(Tuple::v1).toArray()));
            assertThat(threadContext.getTransient("authorization"), instanceOf(String.class)); 
            assertThat(threadContext.getResponseHeaders().keySet(), containsInAnyOrder("authorization")); 
        }
    }

    private String randomCase(String original) {
        int i = randomInt(original.length() - 1);
        StringBuilder sb = new StringBuilder(original);
        sb.setCharAt(i, Character.toUpperCase(original.toCharArray()[i]));
        return sb.toString();
    }

    /**
     * Sometimes wraps a Runnable in an AbstractRunnable.
     */
    private Runnable sometimesAbstractRunnable(Runnable r) {
        if (random().nextBoolean()) {
            return r;
        }
        return new AbstractRunnable() {
            @Override
            public void onFailure(Exception e) {
                throw new RuntimeException(e);
            }

            @Override
            protected void doRun() throws Exception {
                r.run();
            }
        };
    }
}
