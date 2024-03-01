/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.bucket.terms;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.xcontent.XContentType;
import org.hamcrest.Matchers;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailuresAndResponse;

/**
 * Test that index enough data to trigger the creation of Cuckoo filters.
 */

public class RareTermsIT extends ESSingleNodeTestCase {

    private static final String index = "idx";

    private void indexDocs(int numDocs) {
        final BulkRequestBuilder bulk = client().prepareBulk();
        for (int i = 0; i < numDocs; ++i) {
            bulk.add(new IndexRequest(index).source("{\"str_value\" : \"s" + i + "\"}", XContentType.JSON));
        }
        assertNoFailures(bulk.get());
    }

    public void testSingleValuedString() {
        final Settings.Builder settings = Settings.builder()
            .put(IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.getKey(), 2)
            .put(IndexMetadata.INDEX_NUMBER_OF_REPLICAS_SETTING.getKey(), 0);
        createIndex(index, settings.build());
        final int numDocs = randomIntBetween(12000, 17000);
        for (int i = 0; i < 3; i++) {
            indexDocs(numDocs);
            assertNoFailures(client().admin().indices().prepareRefresh(index).get());
        }
        assertNumRareTerms(1, 0);
        assertNumRareTerms(10, numDocs);
    }

    private void assertNumRareTerms(int maxDocs, int rareTerms) {
        assertNoFailuresAndResponse(
            client().prepareSearch(index)
                .addAggregation(new RareTermsAggregationBuilder("rareTerms").field("str_value.keyword").maxDocCount(maxDocs)),
            response -> {
                final RareTerms terms = response.getAggregations().get("rareTerms");
                assertThat(terms.getBuckets().size(), Matchers.equalTo(rareTerms));
            }
        );
    }
}
