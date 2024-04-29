/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.ingest.common;

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.TestIngestDocument;
import org.elasticsearch.test.ESTestCase;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

public class UriPartsProcessorTests extends ESTestCase {

    public void testUriParts() throws Exception {

        testUriParsing("http:

        testUriParsing("http:

        testUriParsing(
            "http:
            Map.of("scheme", "http", "domain", "www.google.com", "extension", "png", "path", "/google.png", "port", 88)
        );

        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "fragment", "bar", "path", "/foo", "port", 88)
        );

        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "path", "/foo.jpg", "extension", "jpg", "port", 88)
        );

        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "path", "/foo", "query", "key=val", "port", 88)
        );

        testUriParsing(
            "https:
            Map.of(
                "scheme",
                "https",
                "domain",
                "www.google.com",
                "path",
                "/foo",
                "port",
                88,
                "user_info",
                "user:pw",
                "username",
                "user",
                "password",
                "pw"
            )
        );

        testUriParsing(
            "https:
            Map.of(
                "scheme",
                "https",
                "domain",
                "www.google.com",
                "path",
                "/foo",
                "port",
                88,
                "user_info",
                "user:",
                "username",
                "user",
                "password",
                ""
            )
        );

        testUriParsing(
            "https:
            Map.of(
                "scheme",
                "https",
                "domain",
                "testing.google.com",
                "fragment",
                "anchorVal",
                "path",
                "/foo/bar",
                "port",
                8080,
                "username",
                "user",
                "password",
                "pw",
                "user_info",
                "user:pw",
                "query",
                "foo1=bar1&foo2=bar2"
            )
        );

        testUriParsing(
            "ftp:
            Map.of("scheme", "ftp", "path", "/rfc/rfc1808.txt", "extension", "txt", "domain", "ftp.is.co.za")
        );

        testUriParsing("telnet:

        testUriParsing(
            "ldap:
            Map.of("scheme", "ldap", "path", "/c=GB", "query", "objectClass?one", "domain", "[2001:db8::7]")
        );

        testUriParsing(
            true,
            false,
            "http:
            Map.of("scheme", "http", "domain", "www.google.com", "fragment", "bar", "path", "/foo", "port", 88)
        );

        testUriParsing(
            false,
            true,
            "http:
            Map.of("scheme", "http", "domain", "www.google.com", "fragment", "bar", "path", "/foo", "port", 88)
        );
    }

    public void testUrlWithCharactersNotToleratedByUri() throws Exception {
        testUriParsing(
            "http:
            Map.of("scheme", "http", "domain", "www.google.com", "path", "/path with spaces")
        );

        testUriParsing(
            "https:
            Map.of(
                "scheme",
                "https",
                "domain",
                "testing.google.com",
                "fragment",
                "anchorVal",
                "path",
                "/foo with space/bar",
                "port",
                8080,
                "username",
                "user",
                "password",
                "pw",
                "user_info",
                "user:pw",
                "query",
                "foo1=bar1&foo2=bar2"
            )
        );
    }

    public void testDotPathWithoutExtension() throws Exception {
        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "path", "/path.withdot/filenamewithoutextension")
        );
    }

    public void testDotPathWithExtension() throws Exception {
        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "path", "/path.withdot/filenamewithextension.txt", "extension", "txt")
        );
    }

    /**
     * This test verifies that we return an empty extension instead of <code>null</code> if the URI ends with a period. This is probably
     * not behaviour we necessarily want to keep forever, but this test ensures that we're conscious about changing that behaviour.
     */
    public void testEmptyExtension() throws Exception {
        testUriParsing(
            "https:
            Map.of("scheme", "https", "domain", "www.google.com", "path", "/foo/bar.", "extension", "")
        );
    }

    public void testRemoveIfSuccessfulDoesNotRemoveTargetField() throws Exception {
        String field = "field";
        UriPartsProcessor processor = new UriPartsProcessor(null, null, field, field, true, false, false);

        Map<String, Object> source = new HashMap<>();
        source.put(field, "http:
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);
        IngestDocument output = processor.execute(input);

        Map<String, Object> expectedSourceAndMetadata = new HashMap<>();
        expectedSourceAndMetadata.put(field, Map.of("scheme", "http", "domain", "www.google.com", "path", ""));
        for (Map.Entry<String, Object> entry : expectedSourceAndMetadata.entrySet()) {
            assertThat(output.getSourceAndMetadata(), hasEntry(entry.getKey(), entry.getValue()));
        }
    }

    public void testInvalidUri() {
        String uri = "not:\\/_a_valid_uri";
        UriPartsProcessor processor = new UriPartsProcessor(null, null, "field", "url", true, false, false);

        Map<String, Object> source = new HashMap<>();
        source.put("field", uri);
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> processor.execute(input));
        assertThat(e.getMessage(), containsString("unable to parse URI [" + uri + "]"));
    }

    public void testNullValue() {
        Map<String, Object> source = new HashMap<>();
        source.put("field", null);
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);

        UriPartsProcessor processor = new UriPartsProcessor(null, null, "field", "url", true, false, false);

        expectThrows(NullPointerException.class, () -> processor.execute(input));
    }

    public void testMissingField() {
        Map<String, Object> source = new HashMap<>();
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);

        UriPartsProcessor processor = new UriPartsProcessor(null, null, "field", "url", true, false, false);

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> processor.execute(input));
        assertThat(e.getMessage(), containsString("field [field] not present as part of path [field]"));
    }

    public void testIgnoreMissingField() throws Exception {
        Map<String, Object> source = new HashMap<>();
        source.put(randomIdentifier(), randomIdentifier());
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);
        Map<String, Object> expectedSourceAndMetadata = Map.copyOf(input.getSourceAndMetadata());

        UriPartsProcessor processor = new UriPartsProcessor(null, null, "field", "url", true, false, true);
        IngestDocument output = processor.execute(input);

        assertThat(output.getSourceAndMetadata().entrySet(), hasSize(expectedSourceAndMetadata.size()));

        for (Map.Entry<String, Object> entry : expectedSourceAndMetadata.entrySet()) {
            assertThat(output.getSourceAndMetadata(), hasEntry(entry.getKey(), entry.getValue()));
        }
    }

    private void testUriParsing(String uri, Map<String, Object> expectedValues) throws Exception {
        testUriParsing(false, false, uri, expectedValues);
    }

    private void testUriParsing(boolean keepOriginal, boolean removeIfSuccessful, String uri, Map<String, Object> expectedValues)
        throws Exception {
        UriPartsProcessor processor = new UriPartsProcessor(null, null, "field", "url", removeIfSuccessful, keepOriginal, false);

        Map<String, Object> source = new HashMap<>();
        source.put("field", uri);
        IngestDocument input = TestIngestDocument.withDefaultVersion(source);
        IngestDocument output = processor.execute(input);

        Map<String, Object> expectedSourceAndMetadata = new HashMap<>();

        if (removeIfSuccessful == false) {
            expectedSourceAndMetadata.put("field", uri);
        }

        Map<String, Object> values;
        if (keepOriginal) {
            values = new HashMap<>(expectedValues);
            values.put("original", uri);
        } else {
            values = expectedValues;
        }
        expectedSourceAndMetadata.put("url", values);

        for (Map.Entry<String, Object> entry : expectedSourceAndMetadata.entrySet()) {
            assertThat(output.getSourceAndMetadata(), hasEntry(entry.getKey(), entry.getValue()));
        }
    }

}
