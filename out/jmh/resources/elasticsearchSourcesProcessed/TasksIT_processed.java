/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.action.admin.cluster.node.tasks;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.TaskOperationFailure;
import org.elasticsearch.action.admin.cluster.health.TransportClusterHealthAction;
import org.elasticsearch.action.admin.cluster.node.tasks.get.GetTaskRequest;
import org.elasticsearch.action.admin.cluster.node.tasks.get.GetTaskResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.admin.cluster.node.tasks.list.TransportListTasksAction;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeAction;
import org.elasticsearch.action.admin.indices.refresh.RefreshAction;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryAction;
import org.elasticsearch.action.bulk.BulkAction;
import org.elasticsearch.action.index.TransportIndexAction;
import org.elasticsearch.action.search.SearchTransportService;
import org.elasticsearch.action.search.TransportSearchAction;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.support.replication.TransportReplicationActionTests;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.health.node.selection.HealthNode;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskCancelledException;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.tasks.TaskInfo;
import org.elasticsearch.tasks.TaskResult;
import org.elasticsearch.tasks.TaskResultsService;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.tasks.MockTaskManager;
import org.elasticsearch.test.tasks.MockTaskManagerListener;
import org.elasticsearch.test.transport.MockTransportService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.ReceiveTimeoutTransportException;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.elasticsearch.action.admin.cluster.node.tasks.TestTaskPlugin.TEST_TASK_ACTION;
import static org.elasticsearch.action.admin.cluster.node.tasks.TestTaskPlugin.UNBLOCK_TASK_ACTION;
import static org.elasticsearch.core.TimeValue.timeValueMillis;
import static org.elasticsearch.core.TimeValue.timeValueSeconds;
import static org.elasticsearch.http.HttpTransportSettings.SETTING_HTTP_MAX_HEADER_SIZE;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertFutureThrows;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Integration tests for task management API
 * <p>
 * We need at least 2 nodes so we have a master node a non-master node
 */
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.SUITE, minNumDataNodes = 2)
public class TasksIT extends ESIntegTestCase {

    private final Map<Tuple<String, String>, RecordingTaskManagerListener> listeners = new HashMap<>();

    @Override
    protected Collection<Class<? extends Plugin>> getMockPlugins() {
        Collection<Class<? extends Plugin>> mockPlugins = new ArrayList<>(super.getMockPlugins());
        mockPlugins.remove(MockTransportService.TestPlugin.class);
        return mockPlugins;
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(MockTransportService.TestPlugin.class, TestTaskPlugin.class);
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        return Settings.builder()
            .put(super.nodeSettings(nodeOrdinal, otherSettings))
            .put(MockTaskManager.USE_MOCK_TASK_MANAGER_SETTING.getKey(), true)
            .build();
    }

    public void testTaskCounts() {
        ListTasksResponse response = clusterAdmin().prepareListTasks("data:true")
            .setActions(TransportListTasksAction.TYPE.name() + "[n]")
            .get();
        assertThat(response.getTasks().size(), greaterThanOrEqualTo(cluster().numDataNodes()));
    }

    public void testMasterNodeOperationTasks() throws Exception {
        registerTaskManagerListeners(TransportClusterHealthAction.NAME);

        internalCluster().masterClient().admin().cluster().prepareHealth().get();
        assertEquals(1, numberOfEvents(TransportClusterHealthAction.NAME, Tuple::v1)); 
        assertBusy(() -> assertEquals(1, numberOfEvents(TransportClusterHealthAction.NAME, event -> event.v1() == false)));

        resetTaskManagerListeners(TransportClusterHealthAction.NAME);

        internalCluster().nonMasterClient().admin().cluster().prepareHealth().get();
        assertEquals(2, numberOfEvents(TransportClusterHealthAction.NAME, Tuple::v1)); 
        assertBusy(() -> assertEquals(2, numberOfEvents(TransportClusterHealthAction.NAME, event -> event.v1() == false)));
        List<TaskInfo> tasks = findEvents(TransportClusterHealthAction.NAME, Tuple::v1);

        if (tasks.get(0).parentTaskId().isSet()) {
            assertParentTask(Collections.singletonList(tasks.get(0)), tasks.get(1));
        } else {
            assertParentTask(Collections.singletonList(tasks.get(1)), tasks.get(0));
        }
    }

    public void testTransportReplicationAllShardsTasks() {
        registerTaskManagerListeners(ValidateQueryAction.NAME); 
        registerTaskManagerListeners(ValidateQueryAction.NAME + "[s]"); 
        createIndex("test");
        ensureGreen("test"); 
        indicesAdmin().prepareValidateQuery("test").setAllShards(true).get();

        NumShards numberOfShards = getNumShards("test");
        assertEquals(1, numberOfEvents(ValidateQueryAction.NAME, Tuple::v1));
        assertEquals(numberOfShards.numPrimaries, numberOfEvents(ValidateQueryAction.NAME + "[s]", Tuple::v1));

        assertParentTask(findEvents(ValidateQueryAction.NAME + "[s]", Tuple::v1), findEvents(ValidateQueryAction.NAME, Tuple::v1).get(0));
    }

    public void testTransportBroadcastByNodeTasks() {
        registerTaskManagerListeners(ForceMergeAction.NAME);  
        registerTaskManagerListeners(ForceMergeAction.NAME + "[n]"); 
        createIndex("test");
        ensureGreen("test"); 
        indicesAdmin().prepareForceMerge("test").get();

        assertEquals(1, numberOfEvents(ForceMergeAction.NAME, Tuple::v1));
        assertEquals(internalCluster().nodesInclude("test").size(), numberOfEvents(ForceMergeAction.NAME + "[n]", Tuple::v1));

        assertParentTask(findEvents(ForceMergeAction.NAME + "[n]", Tuple::v1), findEvents(ForceMergeAction.NAME, Tuple::v1).get(0));
    }

    public void testTransportReplicationSingleShardTasks() {
        registerTaskManagerListeners(ValidateQueryAction.NAME);  
        registerTaskManagerListeners(ValidateQueryAction.NAME + "[s]"); 
        createIndex("test");
        ensureGreen("test"); 
        indicesAdmin().prepareValidateQuery("test").get();

        assertEquals(1, numberOfEvents(ValidateQueryAction.NAME, Tuple::v1));
        assertEquals(1, numberOfEvents(ValidateQueryAction.NAME + "[s]", Tuple::v1));
        assertParentTask(findEvents(ValidateQueryAction.NAME + "[s]", Tuple::v1), findEvents(ValidateQueryAction.NAME, Tuple::v1).get(0));
    }

    public void testTransportBroadcastReplicationTasks() {
        registerTaskManagerListeners(RefreshAction.NAME);  
        registerTaskManagerListeners(RefreshAction.NAME + "[s]"); 
        registerTaskManagerListeners(RefreshAction.NAME + "[s][*]"); 
        createIndex("test");
        ensureGreen("test"); 
        indicesAdmin().prepareRefresh("test").get();

        NumShards numberOfShards = getNumShards("test");

        logger.debug("number of shards, total: [{}], primaries: [{}] ", numberOfShards.totalNumShards, numberOfShards.numPrimaries);
        logger.debug("main events {}", numberOfEvents(RefreshAction.NAME, Tuple::v1));
        logger.debug("main event node {}", findEvents(RefreshAction.NAME, Tuple::v1).get(0).taskId().getNodeId());
        logger.debug("[s] events {}", numberOfEvents(RefreshAction.NAME + "[s]", Tuple::v1));
        logger.debug("[s][*] events {}", numberOfEvents(RefreshAction.NAME + "[s][*]", Tuple::v1));
        logger.debug("nodes with the index {}", internalCluster().nodesInclude("test"));

        assertEquals(1, numberOfEvents(RefreshAction.NAME, Tuple::v1));
        assertThat(numberOfEvents(RefreshAction.NAME + "[s]", Tuple::v1), greaterThanOrEqualTo(numberOfShards.numPrimaries));
        assertThat(numberOfEvents(RefreshAction.NAME + "[s]", Tuple::v1), lessThanOrEqualTo(numberOfShards.numPrimaries * 2));

        TaskInfo mainTask = findEvents(RefreshAction.NAME, Tuple::v1).get(0);
        List<TaskInfo> sTasks = findEvents(RefreshAction.NAME + "[s]", Tuple::v1);
        for (TaskInfo taskInfo : sTasks) {
            if (mainTask.taskId().getNodeId().equals(taskInfo.taskId().getNodeId())) {
                assertParentTask(Collections.singletonList(taskInfo), mainTask);
            } else {
                String description = taskInfo.description();
                List<TaskInfo> sTasksOnRequestingNode = findEvents(
                    RefreshAction.NAME + "[s]",
                    event -> event.v1()
                        && mainTask.taskId().getNodeId().equals(event.v2().taskId().getNodeId())
                        && description.equals(event.v2().description())
                );
                assertEquals(1, sTasksOnRequestingNode.size());
                assertParentTask(Collections.singletonList(taskInfo), sTasksOnRequestingNode.get(0));
            }
        }

        assertEquals(numberOfShards.totalNumShards, numberOfEvents(RefreshAction.NAME + "[s][*]", Tuple::v1));

        List<TaskInfo> spEvents = findEvents(RefreshAction.NAME + "[s][*]", Tuple::v1);
        for (TaskInfo taskInfo : spEvents) {
            List<TaskInfo> sTask;
            if (taskInfo.action().endsWith("[s][p]")) {
                sTask = findEvents(
                    RefreshAction.NAME + "[s]",
                    event -> event.v1() 
                        && event.v2().taskId().equals(taskInfo.parentTaskId())
                        && event.v2().taskId().getNodeId().equals(taskInfo.taskId().getNodeId())
                );
            } else {
                sTask = findEvents(
                    RefreshAction.NAME + "[s]",
                    event -> event.v1() 
                        && event.v2().taskId().equals(taskInfo.parentTaskId())
                        && event.v2().taskId().getNodeId().equals(taskInfo.taskId().getNodeId()) == false
                );
            }
            assertEquals(1, sTask.size());
            assertParentTask(Collections.singletonList(taskInfo), sTask.get(0));
        }
    }

    public void testTransportBulkTasks() {
        registerTaskManagerListeners(BulkAction.NAME);  
        registerTaskManagerListeners(BulkAction.NAME + "[s]");  
        registerTaskManagerListeners(BulkAction.NAME + "[s][p]");  
        registerTaskManagerListeners(BulkAction.NAME + "[s][r]");  
        createIndex("test");
        ensureGreen("test"); 
        indicesAdmin().preparePutMapping("test").setSource("foo", "type=keyword").get();
        client().prepareBulk().add(prepareIndex("test").setId("test_id").setSource("{\"foo\": \"bar\"}", XContentType.JSON)).get();

        List<TaskInfo> topTask = findEvents(BulkAction.NAME, Tuple::v1);
        assertEquals(1, topTask.size());
        assertEquals("requests[1], indices[test]", topTask.get(0).description());

        List<TaskInfo> shardTasks = findEvents(BulkAction.NAME + "[s]", Tuple::v1);
        assertThat(shardTasks.size(), allOf(lessThanOrEqualTo(2), greaterThanOrEqualTo(1)));

        TaskInfo shardTask;
        if (shardTasks.size() == 1) {
            shardTask = shardTasks.get(0);
            assertParentTask(shardTask, findEvents(BulkAction.NAME, Tuple::v1).get(0));
        } else {
            if (shardTasks.get(0).parentTaskId().equals(shardTasks.get(1).taskId())) {
                shardTask = shardTasks.get(0);
                assertParentTask(shardTasks.get(1), findEvents(BulkAction.NAME, Tuple::v1).get(0));
            } else {
                shardTask = shardTasks.get(1);
                assertParentTask(shardTasks.get(0), findEvents(BulkAction.NAME, Tuple::v1).get(0));
            }
        }
        assertThat(shardTask.description(), startsWith("requests[1], index[test]["));

        assertEquals(1, numberOfEvents(BulkAction.NAME + "[s][p]", Tuple::v1));
        assertParentTask(findEvents(BulkAction.NAME + "[s][p]", Tuple::v1), shardTask);

        assertEquals(getNumShards("test").numReplicas, numberOfEvents(BulkAction.NAME + "[s][r]", Tuple::v1));
        assertParentTask(findEvents(BulkAction.NAME + "[s][r]", Tuple::v1), shardTask);
    }

    public void testSearchTaskDescriptions() {
        registerTaskManagerListeners(TransportSearchAction.TYPE.name());  
        registerTaskManagerListeners(TransportSearchAction.TYPE.name() + "[*]");  
        createIndex("test");
        ensureGreen("test"); 
        prepareIndex("test").setId("test_id")
            .setSource("{\"foo\": \"bar\"}", XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .get();

        Map<String, String> headers = new HashMap<>();
        headers.put(Task.X_OPAQUE_ID_HTTP_HEADER, "my_id");
        headers.put("Foo-Header", "bar");
        headers.put("Custom-Task-Header", "my_value");
        assertNoFailures(client().filterWithHeader(headers).prepareSearch("test").setQuery(QueryBuilders.matchAllQuery()));

        List<TaskInfo> mainTask = findEvents(TransportSearchAction.TYPE.name(), Tuple::v1);
        assertEquals(1, mainTask.size());
        assertThat(mainTask.get(0).description(), startsWith("indices[test], search_type["));
        assertThat(mainTask.get(0).description(), containsString("\"query\":{\"match_all\""));
        assertTaskHeaders(mainTask.get(0));

        List<TaskInfo> shardTasks = findEvents(TransportSearchAction.TYPE.name() + "[*]", Tuple::v1);
        for (TaskInfo taskInfo : shardTasks) {
            assertThat(taskInfo.parentTaskId(), notNullValue());
            assertEquals(mainTask.get(0).taskId(), taskInfo.parentTaskId());
            assertTaskHeaders(taskInfo);
            switch (taskInfo.action()) {
                case SearchTransportService.QUERY_ACTION_NAME, SearchTransportService.DFS_ACTION_NAME -> assertTrue(
                    taskInfo.description(),
                    Regex.simpleMatch("shardId[[test][*]]", taskInfo.description())
                );
                case SearchTransportService.QUERY_ID_ACTION_NAME -> assertTrue(
                    taskInfo.description(),
                    Regex.simpleMatch("id[*], indices[test]", taskInfo.description())
                );
                case SearchTransportService.FETCH_ID_ACTION_NAME -> assertTrue(
                    taskInfo.description(),
                    Regex.simpleMatch("id[*], size[1], lastEmittedDoc[null]", taskInfo.description())
                );
                default -> fail("Unexpected action [" + taskInfo.action() + "] with description [" + taskInfo.description() + "]");
            }
            assertThat(taskInfo.description().length(), greaterThan(0));
        }

    }

    public void testSearchTaskHeaderLimit() {
        int maxSize = Math.toIntExact(SETTING_HTTP_MAX_HEADER_SIZE.getDefault(Settings.EMPTY).getBytes() / 2 + 1);

        Map<String, String> headers = new HashMap<>();
        headers.put(Task.X_OPAQUE_ID_HTTP_HEADER, "my_id");
        headers.put("Custom-Task-Header", randomAlphaOfLengthBetween(maxSize, maxSize + 100));
        IllegalArgumentException ex = expectThrows(
            IllegalArgumentException.class,
            client().filterWithHeader(headers).admin().cluster().prepareListTasks()
        );
        assertThat(ex.getMessage(), startsWith("Request exceeded the maximum size of task headers "));
    }

    private void assertTaskHeaders(TaskInfo taskInfo) {
        assertThat(taskInfo.headers().keySet(), hasSize(2));
        assertEquals("my_id", taskInfo.headers().get(Task.X_OPAQUE_ID_HTTP_HEADER));
        assertEquals("my_value", taskInfo.headers().get("Custom-Task-Header"));
    }

    /**
     * Very basic "is it plugged in" style test that indexes a document and makes sure that you can fetch the status of the process. The
     * goal here is to verify that the large moving parts that make fetching task status work fit together rather than to verify any
     * particular status results from indexing. For that, look at {@link TransportReplicationActionTests}. We intentionally don't use the
     * task recording mechanism used in other places in this test so we can make sure that the status fetching works properly over the wire.
     */
    public void testCanFetchIndexStatus() throws Exception {
        CountDownLatch taskRegistered = new CountDownLatch(1);
        CountDownLatch letTaskFinish = new CountDownLatch(1);
        Thread index = null;
        try {
            for (TransportService transportService : internalCluster().getInstances(TransportService.class)) {
                ((MockTaskManager) transportService.getTaskManager()).addListener(new MockTaskManagerListener() {
                    @Override
                    public void onTaskRegistered(Task task) {
                        if (task.getAction().startsWith(TransportIndexAction.NAME)) {
                            taskRegistered.countDown();
                            logger.debug("Blocking [{}] starting", task);
                            try {
                                assertTrue(letTaskFinish.await(10, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
            }
            index = new Thread(() -> {
                DocWriteResponse indexResponse = prepareIndex("test").setSource("test", "test").get();
                assertArrayEquals(ReplicationResponse.NO_FAILURES, indexResponse.getShardInfo().getFailures());
            });
            index.start();
            assertTrue(taskRegistered.await(10, TimeUnit.SECONDS)); 

            ListTasksResponse listResponse = clusterAdmin().prepareListTasks()
                .setActions("indices:data/write/index*")
                .setDetailed(true)
                .get();
            assertThat(listResponse.getTasks(), not(empty()));
            for (TaskInfo task : listResponse.getTasks()) {
                assertNotNull(task.status());
                GetTaskResponse getResponse = clusterAdmin().prepareGetTask(task.taskId()).get();
                assertFalse("task should still be running", getResponse.getTask().isCompleted());
                TaskInfo fetchedWithGet = getResponse.getTask().getTask();
                assertEquals(task.id(), fetchedWithGet.id());
                assertEquals(task.type(), fetchedWithGet.type());
                assertEquals(task.action(), fetchedWithGet.action());
                assertEquals(task.description(), fetchedWithGet.description());
                assertEquals(task.status(), fetchedWithGet.status());
                assertEquals(task.startTime(), fetchedWithGet.startTime());
                assertThat(fetchedWithGet.runningTimeNanos(), greaterThanOrEqualTo(task.runningTimeNanos()));
                assertEquals(task.cancellable(), fetchedWithGet.cancellable());
                assertEquals(task.parentTaskId(), fetchedWithGet.parentTaskId());
            }
        } finally {
            letTaskFinish.countDown();
            if (index != null) {
                index.join();
            }
            assertBusy(
                () -> {
                    assertEquals(emptyList(), clusterAdmin().prepareListTasks().setActions("indices:data/write/index*").get().getTasks());
                }
            );
        }
    }

    public void testTasksCancellation() throws Exception {
        TestTaskPlugin.NodesRequest request = new TestTaskPlugin.NodesRequest("test");
        ActionFuture<TestTaskPlugin.NodesResponse> future = client().execute(TEST_TASK_ACTION, request);

        logger.info("--> started test tasks");

        assertBusy(
            () -> assertEquals(
                internalCluster().size(),
                clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name() + "[n]").get().getTasks().size()
            )
        );

        logger.info("--> cancelling the main test task");
        ListTasksResponse cancelTasksResponse = clusterAdmin().prepareCancelTasks().setActions(TEST_TASK_ACTION.name()).get();
        assertEquals(1, cancelTasksResponse.getTasks().size());

        expectThrows(TaskCancelledException.class, future);

        logger.info("--> checking that test tasks are not running");
        assertEquals(0, clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name() + "*").get().getTasks().size());
    }

    public void testTasksUnblocking() throws Exception {
        TestTaskPlugin.NodesRequest request = new TestTaskPlugin.NodesRequest("test");
        ActionFuture<TestTaskPlugin.NodesResponse> future = client().execute(TEST_TASK_ACTION, request);
        assertBusy(
            () -> assertEquals(
                internalCluster().size(),
                clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name() + "[n]").get().getTasks().size()
            )
        );

        client().execute(UNBLOCK_TASK_ACTION, new TestTaskPlugin.UnblockTestTasksRequest()).get();

        future.get();
        assertBusy(
            () -> assertEquals(0, clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name() + "[n]").get().getTasks().size())
        );
    }

    public void testListTasksWaitForCompletion() throws Exception {
        waitForCompletionTestCase(randomBoolean(), id -> {
            var future = ensureStartedOnAllNodes(
                "cluster:monitor/tasks/lists[n]",
                () -> clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name()).setWaitForCompletion(true).execute()
            );

            for (var threadPool : internalCluster().getInstances(ThreadPool.class)) {
                var max = threadPool.info(ThreadPool.Names.MANAGEMENT).getMax();
                var executor = threadPool.executor(ThreadPool.Names.MANAGEMENT);
                var waitForManagementToCompleteAllTasks = new CyclicBarrier(max + 1);
                for (int i = 0; i < max; i++) {
                    executor.submit(() -> safeAwait(waitForManagementToCompleteAllTasks));
                }
                safeAwait(waitForManagementToCompleteAllTasks);
            }

            return future;
        }, response -> {
            assertThat(response.getNodeFailures(), empty());
            assertThat(response.getTaskFailures(), empty());
            assertThat(response.getTasks(), hasSize(1));
            TaskInfo task = response.getTasks().get(0);
            assertEquals(TEST_TASK_ACTION.name(), task.action());
        });
    }

    public void testGetTaskWaitForCompletionWithoutStoringResult() throws Exception {
        waitForCompletionTestCase(false, id -> clusterAdmin().prepareGetTask(id).setWaitForCompletion(true).execute(), response -> {
            assertTrue(response.getTask().isCompleted());
            assertNull(response.getTask().getResponse());
            assertNotNull(response.getTask().getTask());
            assertEquals(TEST_TASK_ACTION.name(), response.getTask().getTask().action());
        });
    }

    public void testGetTaskWaitForCompletionWithStoringResult() throws Exception {
        waitForCompletionTestCase(true, id -> clusterAdmin().prepareGetTask(id).setWaitForCompletion(true).execute(), response -> {
            assertTrue(response.getTask().isCompleted());
            assertEquals(0, response.getTask().getResponseAsMap().get("failure_count"));
            assertNotNull(response.getTask().getTask());
            assertEquals(TEST_TASK_ACTION.name(), response.getTask().getTask().action());
        });
    }

    /**
     * Test wait for completion.
     * @param storeResult should the task store its results
     * @param wait start waiting for a task. Accepts that id of the task to wait for and returns a future waiting for it.
     * @param validator validate the response and return the task ids that were found
     */
    private <T> void waitForCompletionTestCase(boolean storeResult, Function<TaskId, ActionFuture<T>> wait, Consumer<T> validator)
        throws Exception {
        TestTaskPlugin.NodesRequest request = new TestTaskPlugin.NodesRequest("test");
        request.setShouldStoreResult(storeResult);

        ActionFuture<TestTaskPlugin.NodesResponse> future = ensureStartedOnAllNodes(
            TEST_TASK_ACTION.name() + "[n]",
            () -> client().execute(TEST_TASK_ACTION, request)
        );
        ActionFuture<T> waitResponseFuture;
        try {
            var tasks = clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name()).get().getTasks();
            assertThat(tasks, hasSize(1));
            var taskId = tasks.get(0).taskId();
            clusterAdmin().prepareGetTask(taskId).get();

            waitResponseFuture = wait.apply(taskId);
        } finally {
            client().execute(UNBLOCK_TASK_ACTION, new TestTaskPlugin.UnblockTestTasksRequest()).get();
        }

        T waitResponse = waitResponseFuture.get();
        validator.accept(waitResponse);

        TestTaskPlugin.NodesResponse response = future.get();
        assertEquals(emptyList(), response.failures());
    }

    public void testListTasksWaitForTimeout() throws Exception {
        waitForTimeoutTestCase(id -> {
            ListTasksResponse response = clusterAdmin().prepareListTasks()
                .setActions(TEST_TASK_ACTION.name())
                .setWaitForCompletion(true)
                .setTimeout(timeValueMillis(100))
                .get();
            assertThat(response.getNodeFailures(), not(empty()));
            return response.getNodeFailures();
        });
    }

    public void testGetTaskWaitForTimeout() throws Exception {
        waitForTimeoutTestCase(id -> {
            Exception e = expectThrows(
                Exception.class,
                clusterAdmin().prepareGetTask(id).setWaitForCompletion(true).setTimeout(timeValueMillis(100))
            );
            return singleton(e);
        });
    }

    /**
     * Test waiting for a task that times out.
     * @param wait wait for the running task and return all the failures you accumulated waiting for it
     */
    private void waitForTimeoutTestCase(Function<TaskId, ? extends Iterable<? extends Throwable>> wait) throws Exception {
        ActionFuture<TestTaskPlugin.NodesResponse> future = ensureStartedOnAllNodes(
            TEST_TASK_ACTION.name() + "[n]",
            () -> client().execute(TEST_TASK_ACTION, new TestTaskPlugin.NodesRequest("test"))
        );
        try {
            var tasks = clusterAdmin().prepareListTasks().setActions(TEST_TASK_ACTION.name()).get().getTasks();
            assertThat(tasks, hasSize(1));
            var taskId = tasks.get(0).taskId();
            clusterAdmin().prepareGetTask(taskId).get();
            Iterable<? extends Throwable> failures = wait.apply(taskId);

            for (Throwable failure : failures) {
                assertNotNull(
                    ExceptionsHelper.unwrap(failure, ElasticsearchTimeoutException.class, ReceiveTimeoutTransportException.class)
                );
            }
        } finally {
            client().execute(UNBLOCK_TASK_ACTION, new TestTaskPlugin.UnblockTestTasksRequest()).get();
        }
        future.get();
    }

    private <T> ActionFuture<T> ensureStartedOnAllNodes(String nodeTaskName, Supplier<ActionFuture<T>> taskStarter) {
        var startedOnAllNodes = new CountDownLatch(internalCluster().size());
        for (TransportService transportService : internalCluster().getInstances(TransportService.class)) {
            ((MockTaskManager) transportService.getTaskManager()).addListener(new MockTaskManagerListener() {
                @Override
                public void onTaskRegistered(Task task) {
                    if (Objects.equals(task.getAction(), nodeTaskName)) {
                        startedOnAllNodes.countDown();
                    }
                }
            });
        }
        var future = taskStarter.get();
        safeAwait(startedOnAllNodes);
        return future;
    }

    public void testTasksListWaitForNoTask() throws Exception {
        ActionFuture<ListTasksResponse> waitResponseFuture = clusterAdmin().prepareListTasks()
            .setActions(TEST_TASK_ACTION.name() + "[n]")
            .setWaitForCompletion(true)
            .setTimeout(timeValueMillis(10))
            .execute();

        assertThat(waitResponseFuture.get().getTasks(), empty());
    }

    public void testTasksGetWaitForNoTask() throws Exception {
        ActionFuture<GetTaskResponse> waitResponseFuture = clusterAdmin().prepareGetTask("notfound:1")
            .setWaitForCompletion(true)
            .setTimeout(timeValueMillis(10))
            .execute();

        expectNotFound(waitResponseFuture::get);
    }

    public void testTasksWaitForAllTask() throws Exception {
        List<PersistentTasksCustomMetadata.PersistentTask<?>> alwaysRunningTasks = findTasks(
            clusterService().state(),
            HealthNode.TASK_NAME
        );
        Set<String> nodesRunningTasks = alwaysRunningTasks.stream()
            .map(PersistentTasksCustomMetadata.PersistentTask::getExecutorNode)
            .collect(Collectors.toSet());
        ListTasksResponse response = clusterAdmin().prepareListTasks().setWaitForCompletion(true).setTimeout(timeValueSeconds(1)).get();

        assertThat(response.getNodeFailures().size(), equalTo(nodesRunningTasks.size()));
        assertThat(
            response.getNodeFailures().stream().map(f -> ((FailedNodeException) f).nodeId()).collect(Collectors.toSet()),
            equalTo(nodesRunningTasks)
        );
        assertThat(response.getTaskFailures(), emptyCollectionOf(TaskOperationFailure.class));
        assertThat(response.getTasks().size(), greaterThanOrEqualTo(1));
    }

    public void testTaskStoringSuccessfulResult() throws Exception {
        registerTaskManagerListeners(TEST_TASK_ACTION.name());  

        TestTaskPlugin.NodesRequest request = new TestTaskPlugin.NodesRequest("test");
        request.setShouldStoreResult(true);
        request.setShouldBlock(false);
        TaskId parentTaskId = new TaskId("parent_node", randomLong());
        request.setParentTask(parentTaskId);

        client().execute(TEST_TASK_ACTION, request).get();

        List<TaskInfo> events = findEvents(TEST_TASK_ACTION.name(), Tuple::v1);

        assertEquals(1, events.size());
        TaskInfo taskInfo = events.get(0);
        TaskId taskId = taskInfo.taskId();

        TaskResult taskResult = clusterAdmin().getTask(new GetTaskRequest().setTaskId(taskId)).get().getTask();
        assertTrue(taskResult.isCompleted());
        assertNull(taskResult.getError());

        assertEquals(taskInfo.taskId(), taskResult.getTask().taskId());
        assertEquals(taskInfo.parentTaskId(), taskResult.getTask().parentTaskId());
        assertEquals(taskInfo.type(), taskResult.getTask().type());
        assertEquals(taskInfo.action(), taskResult.getTask().action());
        assertEquals(taskInfo.description(), taskResult.getTask().description());
        assertEquals(taskInfo.startTime(), taskResult.getTask().startTime());
        assertEquals(taskInfo.headers(), taskResult.getTask().headers());
        Map<?, ?> result = taskResult.getResponseAsMap();
        assertEquals("0", result.get("failure_count").toString());

        assertNoFailures(indicesAdmin().prepareRefresh(TaskResultsService.TASK_INDEX).get());

        assertHitCount(
            prepareSearch(TaskResultsService.TASK_INDEX).setSource(
                SearchSourceBuilder.searchSource().query(QueryBuilders.termQuery("task.action", taskInfo.action()))
            ),
            1L
        );

        assertHitCount(
            prepareSearch(TaskResultsService.TASK_INDEX).setSource(
                SearchSourceBuilder.searchSource().query(QueryBuilders.termQuery("task.node", taskInfo.taskId().getNodeId()))
            ),
            1L
        );

        GetTaskResponse getResponse = expectFinishedTask(taskId);
        assertEquals(result, getResponse.getTask().getResponseAsMap());
        assertNull(getResponse.getTask().getError());

        client().execute(TEST_TASK_ACTION, request).get();

        events = findEvents(TEST_TASK_ACTION.name(), Tuple::v1);

        assertEquals(2, events.size());
    }

    public void testTaskStoringFailureResult() throws Exception {
        registerTaskManagerListeners(TEST_TASK_ACTION.name());  

        TestTaskPlugin.NodesRequest request = new TestTaskPlugin.NodesRequest("test");
        request.setShouldFail(true);
        request.setShouldStoreResult(true);
        request.setShouldBlock(false);

        assertFutureThrows(client().execute(TEST_TASK_ACTION, request), IllegalStateException.class);

        List<TaskInfo> events = findEvents(TEST_TASK_ACTION.name(), Tuple::v1);
        assertEquals(1, events.size());
        TaskInfo failedTaskInfo = events.get(0);
        TaskId failedTaskId = failedTaskInfo.taskId();

        TaskResult taskResult = clusterAdmin().getTask(new GetTaskRequest().setTaskId(failedTaskId)).get().getTask();
        assertTrue(taskResult.isCompleted());
        assertNull(taskResult.getResponse());

        assertEquals(failedTaskInfo.taskId(), taskResult.getTask().taskId());
        assertEquals(failedTaskInfo.type(), taskResult.getTask().type());
        assertEquals(failedTaskInfo.action(), taskResult.getTask().action());
        assertEquals(failedTaskInfo.description(), taskResult.getTask().description());
        assertEquals(failedTaskInfo.startTime(), taskResult.getTask().startTime());
        assertEquals(failedTaskInfo.headers(), taskResult.getTask().headers());
        Map<?, ?> error = (Map<?, ?>) taskResult.getErrorAsMap();
        assertEquals("Simulating operation failure", error.get("reason"));
        assertEquals("illegal_state_exception", error.get("type"));

        GetTaskResponse getResponse = expectFinishedTask(failedTaskId);
        assertNull(getResponse.getTask().getResponse());
        assertEquals(error, getResponse.getTask().getErrorAsMap());
    }

    public void testGetTaskNotFound() throws Exception {
        expectNotFound(() -> clusterAdmin().prepareGetTask("not_a_node:1").get());

        expectNotFound(() -> clusterAdmin().prepareGetTask(new TaskId(internalCluster().getNodeNames()[0], 1)).get());
    }

    public void testNodeNotFoundButTaskFound() throws Exception {
        CyclicBarrier b = new CyclicBarrier(2);
        TaskResultsService resultsService = internalCluster().getInstance(TaskResultsService.class);
        resultsService.storeResult(
            new TaskResult(
                new TaskInfo(
                    new TaskId("fake", 1),
                    "test",
                    "fake",
                    "test",
                    "",
                    null,
                    0,
                    0,
                    false,
                    false,
                    TaskId.EMPTY_TASK_ID,
                    Collections.emptyMap()
                ),
                new RuntimeException("test")
            ),
            new ActionListener<Void>() {
                @Override
                public void onResponse(Void response) {
                    try {
                        b.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        onFailure(e);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        b.await();

        GetTaskResponse response = expectFinishedTask(new TaskId("fake:1"));
        assertEquals("test", response.getTask().getTask().action());
        assertNotNull(response.getTask().getError());
        assertNull(response.getTask().getResponse());
    }

    @Override
    public void tearDown() throws Exception {
        for (Map.Entry<Tuple<String, String>, RecordingTaskManagerListener> entry : listeners.entrySet()) {
            ((MockTaskManager) internalCluster().getInstance(TransportService.class, entry.getKey().v1()).getTaskManager()).removeListener(
                entry.getValue()
            );
        }
        listeners.clear();
        super.tearDown();
    }

    /**
     * Registers recording task event listeners with the given action mask on all nodes
     */
    private void registerTaskManagerListeners(String actionMasks) {
        for (String nodeName : internalCluster().getNodeNames()) {
            DiscoveryNode node = internalCluster().getInstance(ClusterService.class, nodeName).localNode();
            RecordingTaskManagerListener listener = new RecordingTaskManagerListener(node.getId(), actionMasks.split(","));
            ((MockTaskManager) internalCluster().getInstance(TransportService.class, nodeName).getTaskManager()).addListener(listener);
            RecordingTaskManagerListener oldListener = listeners.put(new Tuple<>(node.getName(), actionMasks), listener);
            assertNull(oldListener);
        }
    }

    /**
     * Resets all recording task event listeners with the given action mask on all nodes
     */
    private void resetTaskManagerListeners(String actionMasks) {
        for (Map.Entry<Tuple<String, String>, RecordingTaskManagerListener> entry : listeners.entrySet()) {
            if (actionMasks == null || entry.getKey().v2().equals(actionMasks)) {
                entry.getValue().reset();
            }
        }
    }

    /**
     * Returns the number of events that satisfy the criteria across all nodes
     *
     * @param actionMasks action masks to match
     * @return number of events that satisfy the criteria
     */
    private int numberOfEvents(String actionMasks, Function<Tuple<Boolean, TaskInfo>, Boolean> criteria) {
        return findEvents(actionMasks, criteria).size();
    }

    /**
     * Returns all events that satisfy the criteria across all nodes
     *
     * @param actionMasks action masks to match
     * @return number of events that satisfy the criteria
     */
    private List<TaskInfo> findEvents(String actionMasks, Function<Tuple<Boolean, TaskInfo>, Boolean> criteria) {
        List<TaskInfo> events = new ArrayList<>();
        for (Map.Entry<Tuple<String, String>, RecordingTaskManagerListener> entry : listeners.entrySet()) {
            if (actionMasks == null || entry.getKey().v2().equals(actionMasks)) {
                for (Tuple<Boolean, TaskInfo> taskEvent : entry.getValue().getEvents()) {
                    if (criteria.apply(taskEvent)) {
                        events.add(taskEvent.v2());
                    }
                }
            }
        }
        return events;
    }

    /**
     * Asserts that all tasks in the tasks list have the same parentTask
     */
    private void assertParentTask(List<TaskInfo> tasks, TaskInfo parentTask) {
        for (TaskInfo task : tasks) {
            assertParentTask(task, parentTask);
        }
    }

    private void assertParentTask(TaskInfo task, TaskInfo parentTask) {
        assertTrue(task.parentTaskId().isSet());
        assertEquals(parentTask.taskId().getNodeId(), task.parentTaskId().getNodeId());
        assertTrue(Strings.hasLength(task.parentTaskId().getNodeId()));
        assertEquals(parentTask.id(), task.parentTaskId().getId());
    }

    private void expectNotFound(ThrowingRunnable r) {
        Exception e = expectThrows(Exception.class, r);
        ResourceNotFoundException notFound = (ResourceNotFoundException) ExceptionsHelper.unwrap(e, ResourceNotFoundException.class);
        if (notFound == null) {
            throw new AssertionError("Expected " + ResourceNotFoundException.class.getSimpleName(), e);
        }
    }

    /**
     * Fetch the task status from the list tasks API using it's "fallback to get from the task index" behavior. Asserts some obvious stuff
     * about the fetched task and returns a map of it's status.
     */
    private GetTaskResponse expectFinishedTask(TaskId taskId) throws IOException {
        GetTaskResponse response = clusterAdmin().prepareGetTask(taskId).get();
        assertTrue("the task should have been completed before fetching", response.getTask().isCompleted());
        TaskInfo info = response.getTask().getTask();
        assertEquals(taskId, info.taskId());
        assertNull(info.status()); 
        return response;
    }
}
