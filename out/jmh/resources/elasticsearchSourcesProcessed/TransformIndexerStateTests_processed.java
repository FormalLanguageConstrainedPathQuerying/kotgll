/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.transforms;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.ActionTestUtils;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.BulkByScrollTask;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.profile.SearchProfileResults;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.client.NoOpClient;
import org.elasticsearch.test.junit.annotations.TestIssueLogging;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.core.indexing.IndexerState;
import org.elasticsearch.xpack.core.indexing.IterationResult;
import org.elasticsearch.xpack.core.transform.action.ValidateTransformAction;
import org.elasticsearch.xpack.core.transform.transforms.SettingsConfig;
import org.elasticsearch.xpack.core.transform.transforms.TimeSyncConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerPosition;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerStats;
import org.elasticsearch.xpack.core.transform.transforms.TransformState;
import org.elasticsearch.xpack.core.transform.transforms.TransformTaskState;
import org.elasticsearch.xpack.transform.TransformServices;
import org.elasticsearch.xpack.transform.checkpoint.CheckpointProvider;
import org.elasticsearch.xpack.transform.checkpoint.MockTimebasedCheckpointProvider;
import org.elasticsearch.xpack.transform.checkpoint.TransformCheckpointService;
import org.elasticsearch.xpack.transform.notifications.MockTransformAuditor;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.persistence.InMemoryTransformConfigManager;
import org.elasticsearch.xpack.transform.persistence.TransformConfigManager;
import org.elasticsearch.xpack.transform.transforms.scheduling.TransformScheduler;
import org.junit.After;
import org.junit.Before;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.elasticsearch.xpack.core.transform.transforms.DestConfigTests.randomDestConfig;
import static org.elasticsearch.xpack.core.transform.transforms.SourceConfigTests.randomSourceConfig;
import static org.elasticsearch.xpack.core.transform.transforms.pivot.PivotConfigTests.randomPivotConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.mock;

public class TransformIndexerStateTests extends ESTestCase {

    private static final SearchResponse ONE_HIT_SEARCH_RESPONSE = new SearchResponse(
        new SearchHits(new SearchHit[] { new SearchHit(1) }, new TotalHits(1L, TotalHits.Relation.EQUAL_TO), 1.0f),
        null,
        new Suggest(Collections.emptyList()),
        false,
        false,
        new SearchProfileResults(Collections.emptyMap()),
        1,
        "",
        1,
        1,
        0,
        0,
        ShardSearchFailure.EMPTY_ARRAY,
        SearchResponse.Clusters.EMPTY
    );

    private Client client;
    private ThreadPool threadPool;
    private TransformAuditor auditor;
    private TransformConfigManager transformConfigManager;

    class MockedTransformIndexer extends TransformIndexer {

        private final ThreadPool threadPool;

        private TransformState persistedState;
        private int saveStateListenerCallCount = 0;
        private CountDownLatch searchLatch;
        private CountDownLatch doProcessLatch;

        MockedTransformIndexer(
            ThreadPool threadPool,
            TransformServices transformServices,
            CheckpointProvider checkpointProvider,
            TransformConfig transformConfig,
            AtomicReference<IndexerState> initialState,
            TransformIndexerPosition initialPosition,
            TransformIndexerStats jobStats,
            TransformContext context
        ) {
            super(
                threadPool,
                transformServices,
                checkpointProvider,
                transformConfig,
                initialState,
                initialPosition,
                jobStats,
                /* TransformProgress */ null,
                TransformCheckpoint.EMPTY,
                TransformCheckpoint.EMPTY,
                context
            );
            this.threadPool = threadPool;

            persistedState = new TransformState(
                context.getTaskState(),
                initialState.get(),
                initialPosition,
                context.getCheckpoint(),
                context.getStateReason(),
                getProgress(),
                null,
                context.shouldStopAtCheckpoint(),
                null
            );
        }

        public void initialize() {
            this.initializeFunction();
        }

        public CountDownLatch createAwaitForSearchLatch(int count) {
            return searchLatch = new CountDownLatch(count);
        }

        public CountDownLatch createCountDownOnResponseLatch(int count) {
            return doProcessLatch = new CountDownLatch(count);
        }

        @Override
        void doGetInitialProgress(SearchRequest request, ActionListener<SearchResponse> responseListener) {
            responseListener.onResponse(ONE_HIT_SEARCH_RESPONSE);
        }

        @Override
        void doDeleteByQuery(DeleteByQueryRequest deleteByQueryRequest, ActionListener<BulkByScrollResponse> responseListener) {
            responseListener.onResponse(
                new BulkByScrollResponse(
                    TimeValue.ZERO,
                    new BulkByScrollTask.Status(Collections.emptyList(), null),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false
                )
            );
        }

        @Override
        void refreshDestinationIndex(ActionListener<Void> responseListener) {
            responseListener.onResponse(null);
        }

        @Override
        protected void doNextSearch(long waitTimeInNanos, ActionListener<SearchResponse> nextPhase) {
            if (searchLatch != null) {
                try {
                    searchLatch.await();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
            threadPool.generic().execute(() -> nextPhase.onResponse(ONE_HIT_SEARCH_RESPONSE));
        }

        @Override
        protected void doNextBulk(BulkRequest request, ActionListener<BulkResponse> nextPhase) {
            if (doProcessLatch != null) {
                doProcessLatch.countDown();
            }
            threadPool.generic().execute(() -> nextPhase.onResponse(new BulkResponse(new BulkItemResponse[0], 100)));
        }

        @Override
        protected void doSaveState(IndexerState state, TransformIndexerPosition position, Runnable next) {
            Collection<ActionListener<Void>> saveStateListenersAtTheMomentOfCalling = saveStateListeners.get();
            saveStateListenerCallCount += (saveStateListenersAtTheMomentOfCalling != null)
                ? saveStateListenersAtTheMomentOfCalling.size()
                : 0;
            super.doSaveState(state, position, next);
        }

        @Override
        protected IterationResult<TransformIndexerPosition> doProcess(SearchResponse searchResponse) {
            getStats().incrementNumDocuments(10_000);
            return new IterationResult<>(Stream.of(new IndexRequest()), new TransformIndexerPosition(null, null), false);
        }

        public boolean waitingForNextSearch() {
            return super.getScheduledNextSearch() != null;
        }

        public int getSaveStateListenerCallCount() {
            return saveStateListenerCallCount;
        }

        public int getSaveStateListenerCount() {
            Collection<ActionListener<Void>> saveStateListenersAtTheMomentOfCalling = saveStateListeners.get();
            return (saveStateListenersAtTheMomentOfCalling != null) ? saveStateListenersAtTheMomentOfCalling.size() : 0;
        }

        public TransformState getPersistedState() {
            return persistedState;
        }

        @Override
        void doGetFieldMappings(ActionListener<Map<String, String>> fieldMappingsListener) {
            fieldMappingsListener.onResponse(Collections.emptyMap());
        }

        @Override
        void persistState(TransformState state, ActionListener<Void> listener) {
            persistedState = state;
            listener.onResponse(null);
        }

        @Override
        void validate(ActionListener<ValidateTransformAction.Response> listener) {
            listener.onResponse(null);
        }
    }

    class MockedTransformIndexerForStatePersistenceTesting extends TransformIndexer {

        private long timeNanos = 0;

        MockedTransformIndexerForStatePersistenceTesting(
            ThreadPool threadPool,
            TransformServices transformServices,
            CheckpointProvider checkpointProvider,
            TransformConfig transformConfig,
            AtomicReference<IndexerState> initialState,
            TransformIndexerPosition initialPosition,
            TransformIndexerStats jobStats,
            TransformContext context
        ) {
            super(
                threadPool,
                transformServices,
                checkpointProvider,
                transformConfig,
                initialState,
                initialPosition,
                jobStats,
                /* TransformProgress */ null,
                TransformCheckpoint.EMPTY,
                TransformCheckpoint.EMPTY,
                context
            );
        }

        public void setTimeMillis(long millis) {
            this.timeNanos = TimeUnit.MILLISECONDS.toNanos(millis);
        }

        @Override
        protected long getTimeNanos() {
            return timeNanos;
        }

        @Override
        protected void doNextSearch(long waitTimeInNanos, ActionListener<SearchResponse> nextPhase) {
            threadPool.generic().execute(() -> nextPhase.onResponse(ONE_HIT_SEARCH_RESPONSE));
        }

        @Override
        protected void doNextBulk(BulkRequest request, ActionListener<BulkResponse> nextPhase) {
            threadPool.generic().execute(() -> nextPhase.onResponse(new BulkResponse(new BulkItemResponse[0], 100)));
        }

        @Override
        void doGetInitialProgress(SearchRequest request, ActionListener<SearchResponse> responseListener) {
            responseListener.onResponse(ONE_HIT_SEARCH_RESPONSE);
        }

        @Override
        void doGetFieldMappings(ActionListener<Map<String, String>> fieldMappingsListener) {
            fieldMappingsListener.onResponse(Collections.emptyMap());
        }

        @Override
        void doDeleteByQuery(DeleteByQueryRequest deleteByQueryRequest, ActionListener<BulkByScrollResponse> responseListener) {
            responseListener.onResponse(
                new BulkByScrollResponse(
                    TimeValue.ZERO,
                    new BulkByScrollTask.Status(Collections.emptyList(), null),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false
                )
            );
        }

        @Override
        void refreshDestinationIndex(ActionListener<Void> responseListener) {
            responseListener.onResponse(null);
        }

        @Override
        void persistState(TransformState state, ActionListener<Void> listener) {
            listener.onResponse(null);
        }

        @Override
        void validate(ActionListener<ValidateTransformAction.Response> listener) {
            listener.onResponse(null);
        }

        public void initialize() {
            this.initializeFunction();
        }
    }

    @Before
    public void setUpMocks() {
        auditor = MockTransformAuditor.createMockAuditor();
        transformConfigManager = new InMemoryTransformConfigManager();
        threadPool = new TestThreadPool(ThreadPool.Names.GENERIC);
        client = new NoOpClient(threadPool);
    }

    @After
    public void tearDownClient() {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    public void testTriggerStatePersistence() {
        TransformConfig config = new TransformConfig(
            randomAlphaOfLength(10),
            randomSourceConfig(),
            randomDestConfig(),
            null,
            new TimeSyncConfig("timestamp", TimeValue.timeValueSeconds(1)),
            null,
            randomPivotConfig(),
            null,
            randomBoolean() ? null : randomAlphaOfLengthBetween(1, 1000),
            null,
            null,
            null,
            null,
            null
        );
        AtomicReference<IndexerState> state = new AtomicReference<>(IndexerState.INDEXING);

        TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
        final MockedTransformIndexerForStatePersistenceTesting indexer = createMockIndexerForStatePersistenceTesting(
            config,
            state,
            null,
            threadPool,
            auditor,
            null,
            new TransformIndexerStats(),
            context
        );

        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(80_000);
        assertTrue(indexer.triggerSaveState());
        assertTrue(indexer.triggerSaveState());
        indexer.doSaveState(IndexerState.INDEXING, null, () -> {});
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(81_000);
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(140_000);
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(140_001);
        assertTrue(indexer.triggerSaveState());
        indexer.doSaveState(IndexerState.INDEXING, null, () -> {});
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(200_001);
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(240_000);
        indexer.doSaveState(IndexerState.INDEXING, null, () -> {});
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(270_000);
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(300_001);
        assertTrue(indexer.triggerSaveState());
        indexer.doSaveState(IndexerState.INDEXING, null, () -> {});
        assertFalse(indexer.triggerSaveState());
        indexer.setTimeMillis(310_000);
        assertFalse(indexer.triggerSaveState());

        setStopAtCheckpoint(indexer, true, ActionListener.noop());
        assertTrue(indexer.triggerSaveState());
        indexer.setTimeMillis(311_000);
        indexer.doSaveState(IndexerState.INDEXING, null, () -> {});
        indexer.setTimeMillis(310_200);
        assertFalse(indexer.triggerSaveState());

        indexer.setTimeMillis(371_001);
    }

    public void testStopAtCheckpoint() throws Exception {
        TransformConfig config = new TransformConfig(
            randomAlphaOfLength(10),
            randomSourceConfig(),
            randomDestConfig(),
            null,
            new TimeSyncConfig("timestamp", TimeValue.timeValueSeconds(1)),
            null,
            randomPivotConfig(),
            null,
            randomBoolean() ? null : randomAlphaOfLengthBetween(1, 1000),
            null,
            null,
            null,
            null,
            null
        );

        for (IndexerState state : IndexerState.values()) {
            if (IndexerState.INDEXING.equals(state)) {
                continue;
            }
            AtomicReference<IndexerState> stateRef = new AtomicReference<>(state);
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                stateRef,
                null,
                threadPool,
                auditor,
                new TransformIndexerPosition(Collections.singletonMap("afterkey", "value"), Collections.emptyMap()),
                new TransformIndexerStats(),
                context
            );
            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
            assertEquals(0, indexer.getSaveStateListenerCallCount());
            if (IndexerState.STARTED.equals(state)) {
                assertTrue(context.shouldStopAtCheckpoint());
                assertTrue(indexer.getPersistedState().shouldStopAtNextCheckpoint());
            } else {
                assertFalse(context.shouldStopAtCheckpoint());
                assertFalse(indexer.getPersistedState().shouldStopAtNextCheckpoint());
            }
        }

        {
            AtomicReference<IndexerState> stateRef = new AtomicReference<>(IndexerState.STARTED);
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                stateRef,
                null,
                threadPool,
                auditor,
                null,
                new TransformIndexerStats(),
                context
            );
            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
            assertEquals(0, indexer.getSaveStateListenerCallCount());
            assertFalse(context.shouldStopAtCheckpoint());
            assertFalse(indexer.getPersistedState().shouldStopAtNextCheckpoint());
        }

        AtomicReference<IndexerState> state = new AtomicReference<>(IndexerState.STARTED);
        {
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                state,
                null,
                threadPool,
                auditor,
                null,
                new TransformIndexerStats(),
                context
            );
            indexer.start();
            assertTrue(indexer.maybeTriggerAsyncJob(System.currentTimeMillis()));
            assertEquals(indexer.getState(), IndexerState.INDEXING);

            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));

            indexer.stop();
            assertBusy(() -> assertThat(indexer.getState(), equalTo(IndexerState.STOPPED)), 5, TimeUnit.SECONDS);

            assertEquals(1, indexer.getSaveStateListenerCallCount());

            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
            assertEquals(1, indexer.getSaveStateListenerCallCount());
        }

        {
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                state,
                null,
                threadPool,
                auditor,
                null,
                new TransformIndexerStats(),
                context
            );

            indexer.start();
            assertTrue(indexer.maybeTriggerAsyncJob(System.currentTimeMillis()));
            assertEquals(indexer.getState(), IndexerState.INDEXING);

            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
            assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));

            indexer.stop();
            assertBusy(() -> assertThat(indexer.getState(), equalTo(IndexerState.STOPPED)), 5, TimeUnit.SECONDS);

            assertThat(indexer.getSaveStateListenerCallCount(), greaterThanOrEqualTo(1));
            assertThat(indexer.getSaveStateListenerCallCount(), lessThanOrEqualTo(3));
        }

        {
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                state,
                null,
                threadPool,
                auditor,
                null,
                new TransformIndexerStats(),
                context
            );
            indexer.start();
            assertTrue(indexer.maybeTriggerAsyncJob(System.currentTimeMillis()));
            assertEquals(indexer.getState(), IndexerState.INDEXING);

            CountDownLatch searchLatch = indexer.createAwaitForSearchLatch(1);

            List<CountDownLatch> responseLatches = new ArrayList<>();
            for (int i = 0; i < 5; ++i) {
                CountDownLatch latch = new CountDownLatch(1);
                boolean stopAtCheckpoint = i % 2 == 0;
                countResponse(listener -> setStopAtCheckpoint(indexer, stopAtCheckpoint, listener), latch);
                responseLatches.add(latch);
            }

            searchLatch.countDown();

            indexer.stop();
            assertBusy(() -> assertThat(indexer.getState(), equalTo(IndexerState.STOPPED)), 5, TimeUnit.SECONDS);

            for (CountDownLatch l : responseLatches) {
                assertTrue("timed out after 5s", l.await(5, TimeUnit.SECONDS));
            }

            assertThat(indexer.getSaveStateListenerCallCount(), equalTo(5));
        }

        {
            TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
            final MockedTransformIndexer indexer = createMockIndexer(
                config,
                state,
                null,
                threadPool,
                auditor,
                null,
                new TransformIndexerStats(),
                context
            );
            indexer.start();
            assertTrue(indexer.maybeTriggerAsyncJob(System.currentTimeMillis()));
            assertEquals(indexer.getState(), IndexerState.INDEXING);

            CountDownLatch searchLatch = indexer.createAwaitForSearchLatch(1);

            List<CountDownLatch> responseLatches = new ArrayList<>();
            boolean previousStopAtCheckpoint = false;

            for (int i = 0; i < 3; ++i) {
                CountDownLatch latch = new CountDownLatch(1);
                boolean stopAtCheckpoint = randomBoolean();
                previousStopAtCheckpoint = stopAtCheckpoint;
                countResponse(listener -> setStopAtCheckpoint(indexer, stopAtCheckpoint, listener), latch);
                responseLatches.add(latch);
            }

            searchLatch.countDown();

            for (int i = 0; i < 3; ++i) {
                boolean stopAtCheckpoint = randomBoolean();
                previousStopAtCheckpoint = stopAtCheckpoint;
                assertResponse(listener -> setStopAtCheckpoint(indexer, stopAtCheckpoint, listener));
            }

            indexer.stop();
            assertBusy(() -> assertThat(indexer.getState(), equalTo(IndexerState.STOPPED)), 5, TimeUnit.SECONDS);

            for (CountDownLatch l : responseLatches) {
                assertTrue("timed out after 5s", l.await(5, TimeUnit.SECONDS));
            }

            assertEquals(0, indexer.getSaveStateListenerCount());

            assertThat(indexer.getSaveStateListenerCallCount(), lessThanOrEqualTo(6));
        }
    }

    @TestIssueLogging(
        value = "org.elasticsearch.xpack.transform.transforms:DEBUG",
        issueUrl = "https:
    )
    public void testStopAtCheckpointForThrottledTransform() throws Exception {
        TransformConfig config = new TransformConfig(
            randomAlphaOfLength(10),
            randomSourceConfig(),
            randomDestConfig(),
            null,
            new TimeSyncConfig("timestamp", TimeValue.timeValueSeconds(1)),
            null,
            randomPivotConfig(),
            null,
            randomBoolean() ? null : randomAlphaOfLengthBetween(1, 1000),
            new SettingsConfig.Builder().setRequestsPerSecond(1.0f).build(),
            null,
            null,
            null,
            null
        );
        AtomicReference<IndexerState> state = new AtomicReference<>(IndexerState.STARTED);

        TransformContext context = new TransformContext(TransformTaskState.STARTED, "", 0, mock(TransformContext.Listener.class));
        final MockedTransformIndexer indexer = createMockIndexer(
            config,
            state,
            null,
            threadPool,
            auditor,
            null,
            new TransformIndexerStats(),
            context
        );

        CountDownLatch onResponseLatch = indexer.createCountDownOnResponseLatch(1);

        indexer.start();
        assertTrue(indexer.maybeTriggerAsyncJob(System.currentTimeMillis()));
        assertEquals(indexer.getState(), IndexerState.INDEXING);

        onResponseLatch.await();
        onResponseLatch = indexer.createCountDownOnResponseLatch(1);

        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
        assertTrue(indexer.getPersistedState().shouldStopAtNextCheckpoint());
        assertResponse(listener -> setStopAtCheckpoint(indexer, false, listener));
        assertFalse(indexer.getPersistedState().shouldStopAtNextCheckpoint());
        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
        assertTrue(indexer.getPersistedState().shouldStopAtNextCheckpoint());
        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
        assertResponse(listener -> setStopAtCheckpoint(indexer, false, listener));
        assertFalse(indexer.getPersistedState().shouldStopAtNextCheckpoint());

        onResponseLatch.await();
        onResponseLatch = indexer.createCountDownOnResponseLatch(1);

        assertBusy(() -> assertTrue(indexer.waitingForNextSearch()), 5, TimeUnit.SECONDS);
        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
        assertTrue(indexer.getPersistedState().shouldStopAtNextCheckpoint());

        onResponseLatch.await();
        onResponseLatch = indexer.createCountDownOnResponseLatch(1);

        assertBusy(() -> assertTrue(indexer.waitingForNextSearch()), 5, TimeUnit.SECONDS);
        assertResponse(listener -> setStopAtCheckpoint(indexer, false, listener));
        assertFalse(indexer.getPersistedState().shouldStopAtNextCheckpoint());

        onResponseLatch.await();
        assertBusy(() -> assertTrue(indexer.waitingForNextSearch()), 5, TimeUnit.SECONDS);
        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));
        assertTrue(indexer.getPersistedState().shouldStopAtNextCheckpoint());
        assertResponse(listener -> setStopAtCheckpoint(indexer, true, listener));

        indexer.stop();
        assertBusy(() -> assertThat(indexer.getState(), equalTo(IndexerState.STOPPED)), 5, TimeUnit.SECONDS);
    }

    private void setStopAtCheckpoint(
        TransformIndexer indexer,
        boolean shouldStopAtCheckpoint,
        ActionListener<Void> shouldStopAtCheckpointListener
    ) {
        CountDownLatch latch = new CountDownLatch(1);
        threadPool.generic().execute(() -> {
            indexer.setStopAtCheckpoint(shouldStopAtCheckpoint, shouldStopAtCheckpointListener);
            latch.countDown();
        });
        try {
            assertTrue("timed out after 5s", latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("timed out after 5s");
        }
    }

    private void assertResponse(Consumer<ActionListener<Void>> function) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        countResponse(function, latch);
        assertTrue("timed out after 5s", latch.await(5, TimeUnit.SECONDS));
    }

    private void countResponse(Consumer<ActionListener<Void>> function, CountDownLatch latch) throws InterruptedException {
        LatchedActionListener<Void> listener = new LatchedActionListener<>(
            ActionTestUtils.assertNoFailureListener(r -> assertEquals("listener called more than once", 1, latch.getCount())),
            latch
        );
        function.accept(listener);
    }

    private MockedTransformIndexer createMockIndexer(
        TransformConfig config,
        AtomicReference<IndexerState> state,
        Consumer<String> failureConsumer,
        ThreadPool threadPool,
        TransformAuditor transformAuditor,
        TransformIndexerPosition initialPosition,
        TransformIndexerStats jobStats,
        TransformContext context
    ) {
        CheckpointProvider checkpointProvider = new MockTimebasedCheckpointProvider(config);
        transformConfigManager.putTransformConfiguration(config, ActionListener.noop());
        TransformServices transformServices = new TransformServices(
            transformConfigManager,
            mock(TransformCheckpointService.class),
            transformAuditor,
            new TransformScheduler(Clock.systemUTC(), threadPool, Settings.EMPTY, TimeValue.ZERO)
        );

        MockedTransformIndexer indexer = new MockedTransformIndexer(
            threadPool,
            transformServices,
            checkpointProvider,
            config,
            state,
            initialPosition,
            jobStats,
            context
        );

        indexer.initialize();
        return indexer;
    }

    private MockedTransformIndexerForStatePersistenceTesting createMockIndexerForStatePersistenceTesting(
        TransformConfig config,
        AtomicReference<IndexerState> state,
        Consumer<String> failureConsumer,
        ThreadPool threadPool,
        TransformAuditor transformAuditor,
        TransformIndexerPosition initialPosition,
        TransformIndexerStats jobStats,
        TransformContext context
    ) {
        CheckpointProvider checkpointProvider = new MockTimebasedCheckpointProvider(config);
        transformConfigManager.putTransformConfiguration(config, ActionListener.noop());
        TransformServices transformServices = new TransformServices(
            transformConfigManager,
            mock(TransformCheckpointService.class),
            transformAuditor,
            new TransformScheduler(Clock.systemUTC(), threadPool, Settings.EMPTY, TimeValue.ZERO)
        );

        MockedTransformIndexerForStatePersistenceTesting indexer = new MockedTransformIndexerForStatePersistenceTesting(
            threadPool,
            transformServices,
            checkpointProvider,
            config,
            state,
            initialPosition,
            jobStats,
            context
        );

        indexer.initialize();
        return indexer;
    }
}
