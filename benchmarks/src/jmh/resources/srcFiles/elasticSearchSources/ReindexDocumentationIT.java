/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.documentation;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.admin.cluster.node.tasks.get.GetTaskResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.AbstractBulkByScrollRequestBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.BulkByScrollTask;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexRequestBuilder;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.index.shard.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.reindex.CancelTests;
import org.elasticsearch.reindex.ReindexPlugin;
import org.elasticsearch.reindex.RethrottleRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.tasks.TaskInfo;
import org.elasticsearch.test.ESIntegTestCase;
import org.hamcrest.Matcher;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;

public class ReindexDocumentationIT extends ESIntegTestCase {

    private static final Semaphore ALLOWED_OPERATIONS = new Semaphore(0);
    private static final String INDEX_NAME = "source_index";

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(ReindexPlugin.class, ReindexCancellationPlugin.class);
    }

    @Before
    public void setup() {
        indicesAdmin().prepareCreate(INDEX_NAME).get();
    }

    @SuppressWarnings("unused")
    public void testReindex() {
        Client client = client();
        BulkByScrollResponse response =
          new ReindexRequestBuilder(client)
            .source("source_index")
            .destination("target_index")
            .filter(QueryBuilders.matchQuery("category", "xzy")) 
            .get();
    }

    @SuppressWarnings("unused")
    public void testUpdateByQuery() {
        Client client = client();
        client.admin().indices().prepareCreate("foo").get();
        client.admin().indices().prepareCreate("bar").get();
        client.admin().indices().preparePutMapping(INDEX_NAME).setSource("cat", "type=keyword").get();
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("source_index").abortOnVersionConflict(false);
            BulkByScrollResponse response = updateByQuery.get();
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("source_index")
                .filter(QueryBuilders.termQuery("level", "awesome"))
                .maxDocs(1000)
                .script(new Script(ScriptType.INLINE,
                    "painless",
                    "ctx._source.awesome = 'absolutely'",
                    Collections.emptyMap()));
            BulkByScrollResponse response = updateByQuery.get();

            assertEquals(updateByQuery.request().getScript().getLang(), "painless");
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("source_index")
                .source()
                .setSize(500);
            BulkByScrollResponse response = updateByQuery.get();
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
               new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("source_index")
                .maxDocs(100)
                .source()
                .addSort("cat", SortOrder.DESC);
            BulkByScrollResponse response = updateByQuery.get();
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("source_index")
                .script(new Script(
                    ScriptType.INLINE,
                    "painless",
                    "if (ctx._source.awesome == 'absolutely') {"
                        + "  ctx.op='noop'"
                        + "} else if (ctx._source.awesome == 'lame') {"
                        + "  ctx.op='delete'"
                        + "} else {"
                        + "ctx._source.awesome = 'absolutely'}",
                    Collections.emptyMap()));
            BulkByScrollResponse response = updateByQuery.get();

            assertEquals(updateByQuery.request().getScript().getLang(), "painless");
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source("foo", "bar");
            BulkByScrollResponse response = updateByQuery.get();
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.source().setRouting("cat");
            BulkByScrollResponse response = updateByQuery.get();
        }
        {
            UpdateByQueryRequestBuilder updateByQuery =
              new UpdateByQueryRequestBuilder(client);
            updateByQuery.setPipeline("hurray");
            BulkByScrollResponse response = updateByQuery.get();
        }
    }

    public void testTasks() throws Exception {
        final Client client = client();
        final ReindexRequestBuilder builder = reindexAndPartiallyBlock();

        {
            ListTasksResponse tasksList = client.admin().cluster().prepareListTasks()
                .setActions(UpdateByQueryAction.NAME).setDetailed(true).get();
            for (TaskInfo info: tasksList.getTasks()) {
                TaskId taskId = info.taskId();
                BulkByScrollTask.Status status =
                    (BulkByScrollTask.Status) info.status();
            }
        }

        TaskInfo mainTask = CancelTests.findTaskToCancel(ReindexAction.NAME, builder.request().getSlices());
        BulkByScrollTask.Status status = (BulkByScrollTask.Status) mainTask.status();
        assertNull(status.getReasonCancelled());
        TaskId taskId = mainTask.taskId();
        {
            GetTaskResponse get = client.admin().cluster().prepareGetTask(taskId).get();
        }
        {
            client.admin().cluster().prepareCancelTasks()
                .setActions(UpdateByQueryAction.NAME).get().getTasks();
            client.admin().cluster().prepareCancelTasks()
                .setTargetTaskId(taskId).get().getTasks();
        }
        {
            new RethrottleRequestBuilder(client)
                .setTargetTaskId(taskId)
                .setRequestsPerSecond(2.0f)
                .get();
        }

        ALLOWED_OPERATIONS.release(builder.request().getSlices());
    }

    @SuppressWarnings("unused")
    public void testDeleteByQuery() {
        Client client = client();
        client.admin().indices().prepareCreate("persons").get();

        BulkByScrollResponse response =
          new DeleteByQueryRequestBuilder(client)
            .filter(QueryBuilders.matchQuery("gender", "male")) 
            .source("persons")                                  
            .get();                                             
        long deleted = response.getDeleted();                   

        new DeleteByQueryRequestBuilder(client)
            .filter(QueryBuilders.matchQuery("gender", "male"))     
            .source("persons")                                      
            .execute(new ActionListener<BulkByScrollResponse>() {   
                @Override
                public void onResponse(BulkByScrollResponse response) {
                    long deleted = response.getDeleted();           
                }
                @Override
                public void onFailure(Exception e) {
                }
            });
    }

    /**
     * Similar to what CancelTests does: blocks some operations to be able to catch some tasks in running state
     * @see CancelTests#testCancel(ActionType, AbstractBulkByScrollRequestBuilder, CancelTests.CancelAssertion, Matcher)
     */
    private ReindexRequestBuilder reindexAndPartiallyBlock() throws Exception {
        final Client client = client();
        final int numDocs = randomIntBetween(10, 100);
        ALLOWED_OPERATIONS.release(numDocs);

        indexRandom(
            true,
            false,
            true,
            IntStream.range(0, numDocs)
                .mapToObj(i -> prepareIndex(INDEX_NAME).setId(Integer.toString(i)).setSource("n", Integer.toString(i)))
                .collect(Collectors.toList())
        );

        assertHitCount(prepareSearch(INDEX_NAME).setSize(0), numDocs);
        assertThat(ALLOWED_OPERATIONS.drainPermits(), equalTo(0));

        ReindexRequestBuilder builder = new ReindexRequestBuilder(client).source(INDEX_NAME).destination("target_index");
        builder.source().setSize(1);

        int numModifiedDocs = randomIntBetween(builder.request().getSlices() * 2, numDocs);
        ALLOWED_OPERATIONS.release(numModifiedDocs - builder.request().getSlices());

        builder.execute();

        assertBusy(() -> {
            assertTrue("Expected some queued threads", ALLOWED_OPERATIONS.hasQueuedThreads());
            assertEquals("Expected that no permits are available", 0, ALLOWED_OPERATIONS.availablePermits());
        }, 1, TimeUnit.MINUTES);
        return builder;
    }

    public static class ReindexCancellationPlugin extends Plugin {

        @Override
        public void onIndexModule(IndexModule indexModule) {
            indexModule.addIndexOperationListener(new BlockingOperationListener());
        }
    }

    public static class BlockingOperationListener implements IndexingOperationListener {

        @Override
        public Engine.Index preIndex(ShardId shardId, Engine.Index index) {
            return preCheck(index);
        }

        @Override
        public Engine.Delete preDelete(ShardId shardId, Engine.Delete delete) {
            return preCheck(delete);
        }

        private <T extends Engine.Operation> T preCheck(T operation) {
            if (operation.origin() != Engine.Operation.Origin.PRIMARY) {
                return operation;
            }

            try {
                if (ALLOWED_OPERATIONS.tryAcquire(30, TimeUnit.SECONDS)) {
                    return operation;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            throw new IllegalStateException("Something went wrong");
        }
    }

}
