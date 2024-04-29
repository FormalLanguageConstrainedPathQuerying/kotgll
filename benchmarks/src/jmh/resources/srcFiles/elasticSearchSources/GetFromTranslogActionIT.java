/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.get;

import org.elasticsearch.action.ActionListenerResponseHandler;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.get.TransportGetFromTranslogAction;
import org.elasticsearch.action.get.TransportGetFromTranslogAction.Response;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

public class GetFromTranslogActionIT extends ESIntegTestCase {

    private static final String INDEX = "test";
    private static final String ALIAS = "alias";

    public void testGetFromTranslog() throws Exception {
        assertAcked(
            prepareCreate(INDEX).setMapping("field1", "type=keyword,store=true")
                .setSettings(
                    Settings.builder()
                        .put("index.refresh_interval", -1)
                        .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                        .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                )
                .addAlias(new Alias(ALIAS).writeIndex(randomFrom(true, false, null)))
        );
        ensureGreen();

        var shardRouting = randomFrom(clusterService().state().routingTable().allShards(INDEX));
        var indicesService = internalCluster().getInstance(
            IndicesService.class,
            clusterService().state().nodes().get(shardRouting.currentNodeId()).getName()
        );
        var initialGeneration = indicesService.indexServiceSafe(shardRouting.index())
            .getShard(shardRouting.id())
            .getEngineOrNull()
            .getLastCommittedSegmentInfos()
            .getGeneration();

        var response = getFromTranslog(shardRouting, "1");
        assertNull(response.getResult());
        assertThat(response.segmentGeneration(), equalTo(initialGeneration));

        var indexResponse = prepareIndex("test").setId("1").setSource("field1", "value1").setRefreshPolicy(RefreshPolicy.NONE).get();
        response = getFromTranslog(shardRouting, "1");
        assertNotNull(response.getResult());
        assertThat(response.getResult().isExists(), equalTo(true));
        assertThat(response.getResult().getVersion(), equalTo(indexResponse.getVersion()));
        assertThat(response.segmentGeneration(), equalTo(-1L));
        client().prepareDelete("test", "1").get();
        response = getFromTranslog(shardRouting, "1");
        assertNotNull("get followed by a delete should still return a result", response.getResult());
        assertThat(response.getResult().isExists(), equalTo(false));
        assertThat(response.segmentGeneration(), equalTo(-1L));

        indexResponse = prepareIndex("test").setSource("field1", "value2").get();
        response = getFromTranslog(shardRouting, indexResponse.getId());
        assertNotNull(response.getResult());
        assertThat(response.getResult().isExists(), equalTo(true));
        assertThat(response.getResult().getVersion(), equalTo(indexResponse.getVersion()));
        assertThat(response.segmentGeneration(), equalTo(-1L));
        refresh("test");
        response = getFromTranslog(shardRouting, indexResponse.getId());
        assertNull("after a refresh we should not be able to get from translog", response.getResult());
        assertThat(response.segmentGeneration(), equalTo(initialGeneration));
        prepareIndex("test").setSource("field1", "value3").get();
        refresh("test");
        refresh("test");
        prepareIndex("test").setSource("field1", "value4").get();
        response = getFromTranslog(shardRouting, "non-existent");
        assertNull(response.getResult());
        assertThat(response.segmentGeneration(), equalTo(initialGeneration));
    }

    private Response getFromTranslog(ShardRouting shardRouting, String id) throws Exception {
        var getRequest = client().prepareGet(indexOrAlias(), id).request();
        var node = clusterService().state().nodes().get(shardRouting.currentNodeId());
        assertNotNull(node);
        TransportGetFromTranslogAction.Request request = new TransportGetFromTranslogAction.Request(getRequest, shardRouting.shardId());
        var transportService = internalCluster().getInstance(TransportService.class);
        PlainActionFuture<Response> response = new PlainActionFuture<>();
        transportService.sendRequest(
            node,
            TransportGetFromTranslogAction.NAME,
            request,
            new ActionListenerResponseHandler<>(response, Response::new, transportService.getThreadPool().executor(ThreadPool.Names.GET))
        );
        return response.get();
    }

    private String indexOrAlias() {
        return randomBoolean() ? INDEX : ALIAS;
    }
}
