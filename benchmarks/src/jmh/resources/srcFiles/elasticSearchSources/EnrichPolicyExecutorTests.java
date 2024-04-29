/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.enrich;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.admin.cluster.node.tasks.get.TransportGetTaskAction;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.indices.TestIndexNameExpressionResolver;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.client.NoOpClient;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.enrich.EnrichMetadata;
import org.elasticsearch.xpack.core.enrich.EnrichPolicy;
import org.elasticsearch.xpack.core.enrich.action.ExecuteEnrichPolicyAction;
import org.elasticsearch.xpack.enrich.action.InternalExecutePolicyAction;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnrichPolicyExecutorTests extends ESTestCase {

    private static ThreadPool testThreadPool;

    @BeforeClass
    public static void beforeCLass() {
        testThreadPool = new TestThreadPool("EnrichPolicyExecutorTests");
    }

    @AfterClass
    public static void afterClass() {
        ThreadPool.terminate(testThreadPool, 30, TimeUnit.SECONDS);
    }

    public void testNonConcurrentPolicyCoordination() throws InterruptedException {
        String testPolicyName = "test_policy";
        CountDownLatch latch = new CountDownLatch(1);
        Client client = getClient(latch);
        final EnrichPolicyExecutor testExecutor = new EnrichPolicyExecutor(
            Settings.EMPTY,
            null,
            null,
            client,
            testThreadPool,
            TestIndexNameExpressionResolver.newInstance(testThreadPool.getThreadContext()),
            new EnrichPolicyLocks(),
            ESTestCase::randomNonNegativeLong
        );

        final CountDownLatch firstTaskComplete = new CountDownLatch(1);
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyName),
            new LatchedActionListener<>(ActionListener.noop(), firstTaskComplete)
        );

        EsRejectedExecutionException expected = expectThrows(
            EsRejectedExecutionException.class,
            "Expected exception but nothing was thrown",
            () -> {
                testExecutor.coordinatePolicyExecution(new ExecuteEnrichPolicyAction.Request(testPolicyName), ActionListener.noop());
                latch.countDown();
                firstTaskComplete.await();
            }
        );

        latch.countDown();
        firstTaskComplete.await();

        assertThat(
            expected.getMessage(),
            containsString("Could not obtain lock because policy execution for [" + testPolicyName + "] is already in progress.")
        );

        CountDownLatch secondTaskComplete = new CountDownLatch(1);
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyName),
            new LatchedActionListener<>(ActionListener.noop(), secondTaskComplete)
        );
        secondTaskComplete.await();
    }

    public void testMaximumPolicyExecutionLimit() throws InterruptedException {
        String testPolicyBaseName = "test_policy_";
        Settings testSettings = Settings.builder().put(EnrichPlugin.ENRICH_MAX_CONCURRENT_POLICY_EXECUTIONS.getKey(), 2).build();
        CountDownLatch latch = new CountDownLatch(1);
        Client client = getClient(latch);
        EnrichPolicyLocks locks = new EnrichPolicyLocks();
        final EnrichPolicyExecutor testExecutor = new EnrichPolicyExecutor(
            testSettings,
            null,
            null,
            client,
            testThreadPool,
            TestIndexNameExpressionResolver.newInstance(testThreadPool.getThreadContext()),
            locks,
            ESTestCase::randomNonNegativeLong
        );

        final CountDownLatch firstTaskComplete = new CountDownLatch(1);
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyBaseName + "1"),
            new LatchedActionListener<>(ActionListener.noop(), firstTaskComplete)
        );

        final CountDownLatch secondTaskComplete = new CountDownLatch(1);
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyBaseName + "2"),
            new LatchedActionListener<>(ActionListener.noop(), secondTaskComplete)
        );

        EsRejectedExecutionException expected = expectThrows(
            EsRejectedExecutionException.class,
            "Expected exception but nothing was thrown",
            () -> {
                testExecutor.coordinatePolicyExecution(
                    new ExecuteEnrichPolicyAction.Request(testPolicyBaseName + "3"),
                    ActionListener.noop()
                );
                latch.countDown();
                firstTaskComplete.await();
                secondTaskComplete.await();
            }
        );

        latch.countDown();
        firstTaskComplete.await();
        secondTaskComplete.await();

        assertThat(
            expected.getMessage(),
            containsString(
                "Policy execution failed. Policy execution for [test_policy_3] would exceed " + "maximum concurrent policy executions [2]"
            )
        );

        assertThat(locks.lockedPolices(), is(empty()));
        CountDownLatch finalTaskComplete = new CountDownLatch(1);
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyBaseName + "1"),
            new LatchedActionListener<>(ActionListener.noop(), finalTaskComplete)
        );
        finalTaskComplete.await();
    }

    public void testWaitForCompletionConditionRemainsLocked() throws Exception {
        String testPolicyName = "test_policy";
        String testTaskId = randomAlphaOfLength(10) + ":" + randomIntBetween(100, 300);
        boolean completeWithResourceNotFound = randomBoolean();

        CountDownLatch clientBlockingLatch = new CountDownLatch(1);
        CountDownLatch secondGetTaskWasCalled = new CountDownLatch(1);
        CyclicBarrier getTaskActionBlockingBarrier = new CyclicBarrier(2);
        AtomicBoolean shouldGetTaskApiReturnTimeout = new AtomicBoolean(true);

        Client client = new NoOpClient(testThreadPool) {
            @Override
            protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
                ActionType<Response> action,
                Request request,
                ActionListener<Response> listener
            ) {
                if (request instanceof InternalExecutePolicyAction.Request) {
                    assertFalse(((InternalExecutePolicyAction.Request) request).isWaitForCompletion());
                }
                testThreadPool.generic().execute(() -> {
                    try {
                        clientBlockingLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    if (TransportGetTaskAction.TYPE.equals(action)) {
                        if (shouldGetTaskApiReturnTimeout.get() == false) {
                            secondGetTaskWasCalled.countDown();
                        }
                        try {
                            getTaskActionBlockingBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                        if (shouldGetTaskApiReturnTimeout.getAndSet(false)) {
                            listener.onFailure(new ElasticsearchTimeoutException("Test call has timed out"));
                        } else if (completeWithResourceNotFound) {
                            listener.onFailure(new ElasticsearchException("Test wrapping", new ResourceNotFoundException("test")));
                        } else {
                            listener.onResponse(null);
                        }
                    } else if (InternalExecutePolicyAction.INSTANCE.equals(action)) {
                        @SuppressWarnings("unchecked")
                        Response response = (Response) new ExecuteEnrichPolicyAction.Response(new TaskId(testTaskId));
                        listener.onResponse(response);
                    } else {
                        listener.onResponse(null);
                    }
                });
            }
        };

        final EnrichPolicyLocks enrichPolicyLocks = new EnrichPolicyLocks();
        final EnrichPolicyExecutor testExecutor = new EnrichPolicyExecutor(
            Settings.EMPTY,
            null,
            null,
            client,
            testThreadPool,
            TestIndexNameExpressionResolver.newInstance(testThreadPool.getThreadContext()),
            enrichPolicyLocks,
            ESTestCase::randomNonNegativeLong
        );

        PlainActionFuture<ExecuteEnrichPolicyAction.Response> firstTaskResult = new PlainActionFuture<>();
        testExecutor.coordinatePolicyExecution(
            new ExecuteEnrichPolicyAction.Request(testPolicyName).setWaitForCompletion(false),
            firstTaskResult
        );

        if (enrichPolicyLocks.lockedPolices().contains(testPolicyName) == false) {
            clientBlockingLatch.countDown();
            try {
                firstTaskResult.get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("Encountered ignorable exception during test cleanup");
            }
            try {
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                logger.error("Encountered ignorable barrier wait exception during test cleanup");
            }
            fail("Enrich policy was not locked during task submission when it should have been");
        }

        clientBlockingLatch.countDown();

        try {
            ExecuteEnrichPolicyAction.Response response = firstTaskResult.actionGet();
            assertThat(response.getStatus(), is(nullValue()));
            assertThat(response.getTaskId(), is(notNullValue()));
        } catch (AssertionError e) {
            try {
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException be) {
                logger.error("Encountered ignorable barrier wait exception during test cleanup");
            }
            throw e;
        }

        if (enrichPolicyLocks.lockedPolices().contains(testPolicyName) == false) {
            try {
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                logger.error("Encountered ignorable barrier wait exception during test cleanup");
            }
            fail("Enrich policy was not locked after task response when it should have been");
        }

        try {
            getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException("Unexpected broken barrier exception", e);
        }

        try {
            assertTrue(
                "Expected task API to be called a second time by the executor after first call timed out",
                secondGetTaskWasCalled.await(3, TimeUnit.SECONDS)
            );
        } catch (InterruptedException e) {
            Assert.fail("Thread interrupted while waiting for background executor to call task API");
        }

        if (enrichPolicyLocks.lockedPolices().contains(testPolicyName) == false) {
            try {
                getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                logger.error("Encountered ignorable barrier wait exception during test cleanup");
            }
            fail("Enrich policy was not locked after timeout when it should have been");
        }

        try {
            getTaskActionBlockingBarrier.await(3, TimeUnit.SECONDS);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException("Unexpected broken barrier exception", e);
        }

        assertBusy(() -> assertFalse(enrichPolicyLocks.lockedPolices().contains(testPolicyName)), 3, TimeUnit.SECONDS);
    }

    public void testRunPolicyLocallyMissingPolicy() {
        EnrichPolicy enrichPolicy = EnrichPolicyTests.randomEnrichPolicy(XContentType.JSON);
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name"))
            .metadata(Metadata.builder().putCustom(EnrichMetadata.TYPE, new EnrichMetadata(Map.of("id", enrichPolicy))).build())
            .build();
        ClusterService clusterService = mock(ClusterService.class);
        when(clusterService.state()).thenReturn(clusterState);

        final EnrichPolicyExecutor testExecutor = new EnrichPolicyExecutor(
            Settings.EMPTY,
            clusterService,
            null,
            null,
            testThreadPool,
            TestIndexNameExpressionResolver.newInstance(testThreadPool.getThreadContext()),
            new EnrichPolicyLocks(),
            ESTestCase::randomNonNegativeLong
        );

        ExecuteEnrichPolicyTask task = mock(ExecuteEnrichPolicyTask.class);
        Exception e = expectThrows(
            ResourceNotFoundException.class,
            () -> testExecutor.runPolicyLocally(task, "my-policy", ".enrich-my-policy-123456789", null)
        );
        assertThat(e.getMessage(), equalTo("policy [my-policy] does not exist"));
    }

    private Client getClient(CountDownLatch latch) {
        return new NoOpClient(testThreadPool) {
            @Override
            protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
                ActionType<Response> action,
                Request request,
                ActionListener<Response> listener
            ) {
                testThreadPool.generic().execute(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    super.doExecute(action, request, listener);
                });
            }
        };
    }
}
