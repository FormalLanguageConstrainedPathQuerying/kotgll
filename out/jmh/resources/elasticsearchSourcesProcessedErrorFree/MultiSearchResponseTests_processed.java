/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.action.search;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.core.RefCounted;
import org.elasticsearch.search.SearchResponseUtils;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.function.Predicate;

import static org.elasticsearch.test.AbstractXContentTestCase.chunkedXContentTester;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class MultiSearchResponseTests extends ESTestCase {


    private MultiSearchResponse createTestInstance() {
        int numItems = randomIntBetween(0, 128);
        MultiSearchResponse.Item[] items = new MultiSearchResponse.Item[numItems];
        for (int i = 0; i < numItems; i++) {
            long tookInMillis = randomNonNegativeLong();
            int totalShards = randomIntBetween(1, Integer.MAX_VALUE);
            int successfulShards = randomIntBetween(0, totalShards);
            int skippedShards = totalShards - successfulShards;
            SearchResponse.Clusters clusters = SearchResponseTests.randomSimpleClusters();
            SearchResponse searchResponse = SearchResponseUtils.emptyWithTotalHits(
                null,
                totalShards,
                successfulShards,
                skippedShards,
                tookInMillis,
                ShardSearchFailure.EMPTY_ARRAY,
                clusters
            );
            items[i] = new MultiSearchResponse.Item(searchResponse, null);
        }
        return new MultiSearchResponse(items, randomNonNegativeLong());
    }

    private static MultiSearchResponse createTestInstanceWithFailures() {
        int numItems = randomIntBetween(0, 128);
        MultiSearchResponse.Item[] items = new MultiSearchResponse.Item[numItems];
        for (int i = 0; i < numItems; i++) {
            if (randomBoolean()) {
                long tookInMillis = randomNonNegativeLong();
                int totalShards = randomIntBetween(1, Integer.MAX_VALUE);
                int successfulShards = randomIntBetween(0, totalShards);
                int skippedShards = totalShards - successfulShards;
                SearchResponse.Clusters clusters = SearchResponseTests.randomSimpleClusters();
                SearchResponse searchResponse = SearchResponseUtils.emptyWithTotalHits(
                    null,
                    totalShards,
                    successfulShards,
                    skippedShards,
                    tookInMillis,
                    ShardSearchFailure.EMPTY_ARRAY,
                    clusters
                );
                items[i] = new MultiSearchResponse.Item(searchResponse, null);
            } else {
                items[i] = new MultiSearchResponse.Item(null, new ElasticsearchException("an error"));
            }
        }
        return new MultiSearchResponse(items, randomNonNegativeLong());
    }

    private MultiSearchResponse doParseInstance(XContentParser parser) throws IOException {
        return MultiSearchResponse.fromXContext(parser);
    }

    private void assertEqualInstances(MultiSearchResponse expected, MultiSearchResponse actual) {
        assertThat(actual.getTook(), equalTo(expected.getTook()));
        assertThat(actual.getResponses().length, equalTo(expected.getResponses().length));
        for (int i = 0; i < expected.getResponses().length; i++) {
            MultiSearchResponse.Item expectedItem = expected.getResponses()[i];
            MultiSearchResponse.Item actualItem = actual.getResponses()[i];
            if (expectedItem.isFailure()) {
                assertThat(actualItem.getResponse(), nullValue());
                assertThat(actualItem.getFailureMessage(), containsString(expectedItem.getFailureMessage()));
            } else {
                assertThat(actualItem.getResponse().toString(), equalTo(expectedItem.getResponse().toString()));
                assertThat(actualItem.getFailure(), nullValue());
            }
        }
    }

    private boolean supportsUnknownFields() {
        return true;
    }

    protected Predicate<String> getRandomFieldsExcludeFilterWhenResultHasErrors() {
        return field -> field.startsWith("responses");
    }

    public final void testFromXContent() throws IOException {
        chunkedXContentTester(this::createParser, t -> createTestInstance(), ToXContent.EMPTY_PARAMS, this::doParseInstance)
            .numberOfTestRuns(20)
            .supportsUnknownFields(supportsUnknownFields())
            .assertEqualsConsumer(this::assertEqualInstances)
            .dispose(RefCounted::decRef)
            .test();
    }

    /**
     * Test parsing {@link MultiSearchResponse} with inner failures as they don't support asserting on xcontent equivalence, given that
     * exceptions are not parsed back as the same original class. We run the usual
     * {@link org.elasticsearch.test.AbstractSerializationTestCase#testFromXContent()}
     * without failures, and this other test with failures where we disable asserting on xcontent equivalence at the end.
     */
    public void testFromXContentWithFailures() throws IOException {
        chunkedXContentTester(this::createParser, t -> createTestInstanceWithFailures(), ToXContent.EMPTY_PARAMS, this::doParseInstance)
            .numberOfTestRuns(20)
            .randomFieldsExcludeFilter(getRandomFieldsExcludeFilterWhenResultHasErrors())
            .supportsUnknownFields(true)
            .assertToXContentEquivalence(false)
            .assertEqualsConsumer(this::assertEqualInstances)
            .dispose(RefCounted::decRef)
            .test();
    }

}
