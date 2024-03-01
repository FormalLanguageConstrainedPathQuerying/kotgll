/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.protocol.xpack.watcher.PutWatchResponse;
import org.elasticsearch.xpack.core.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchRequestBuilder;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.put.PutWatchRequestBuilder;
import org.elasticsearch.xpack.watcher.actions.index.IndexAction;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;

import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;

public class ActionErrorIntegrationTests extends AbstractWatcherIntegrationTestCase {

    /**
     * This test makes sure that when an action encounters an error it should
     * not be subject to throttling. Also, the ack status of the action in the
     * watch should remain awaits_successful_execution as long as the execution
     * fails.
     */
    public void testErrorInAction() throws Exception {
        createIndex("foo");
        updateIndexSettings(Settings.builder().put("index.blocks.write", true), "foo");

        PutWatchResponse putWatchResponse = new PutWatchRequestBuilder(client(), "_id").setSource(
            watchBuilder().trigger(schedule(interval("10m")))

                .addAction("_action", TimeValue.timeValueMinutes(60), IndexAction.builder("foo"))
        ).get();

        assertThat(putWatchResponse.isCreated(), is(true));

        timeWarp().trigger("_id");

        flush();

        assertBusy(() -> {
            long count = watchRecordCount(
                QueryBuilders.boolQuery()
                    .must(termsQuery("result.actions.id", "_action"))
                    .must(termsQuery("result.actions.status", "failure"))
            );
            assertThat(count, is(1L));
        });


        timeWarp().clock().fastForward(TimeValue.timeValueMinutes(randomIntBetween(1, 50)));
        timeWarp().trigger("_id");

        flush();

        assertBusy(() -> {
            long count = watchRecordCount(
                QueryBuilders.boolQuery()
                    .must(termsQuery("result.actions.id", "_action"))
                    .must(termsQuery("result.actions.status", "failure"))
            );
            assertThat(count, is(2L));
        });

        GetWatchResponse getWatchResponse = new GetWatchRequestBuilder(client(), "_id").get();
        XContentSource watch = getWatchResponse.getSource();
        watch.getValue("status.actions._action.ack.awaits_successful_execution");
    }
}
