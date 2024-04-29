/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.transforms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.health.HealthStatus;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.core.indexing.AsyncTwoPhaseIndexer;
import org.elasticsearch.xpack.core.indexing.IndexerState;
import org.elasticsearch.xpack.core.indexing.IterationResult;
import org.elasticsearch.xpack.core.transform.TransformMessages;
import org.elasticsearch.xpack.core.transform.action.ValidateTransformAction;
import org.elasticsearch.xpack.core.transform.transforms.SettingsConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformEffectiveSettings;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerPosition;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerStats;
import org.elasticsearch.xpack.core.transform.transforms.TransformProgress;
import org.elasticsearch.xpack.core.transform.transforms.TransformState;
import org.elasticsearch.xpack.core.transform.transforms.TransformTaskState;
import org.elasticsearch.xpack.core.transform.utils.ExceptionsHelper;
import org.elasticsearch.xpack.transform.TransformServices;
import org.elasticsearch.xpack.transform.checkpoint.CheckpointProvider;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.persistence.TransformConfigManager;
import org.elasticsearch.xpack.transform.transforms.Function.ChangeCollector;
import org.elasticsearch.xpack.transform.transforms.RetentionPolicyToDeleteByQueryRequestConverter.RetentionPolicyException;
import org.elasticsearch.xpack.transform.transforms.scheduling.TransformSchedulingUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.elasticsearch.core.Strings.format;

public abstract class TransformIndexer extends AsyncTwoPhaseIndexer<TransformIndexerPosition, TransformIndexerStats> {

    private static final int PERSIST_STOP_AT_CHECKPOINT_TIMEOUT_SEC = 5;

    /**
     * RunState is an internal (non-persisted) state that controls the internal logic
     * which query filters to run and which index requests to send
     */
    private enum RunState {
        APPLY_RESULTS,

        IDENTIFY_CHANGES,
    }

    public static final int MINIMUM_PAGE_SIZE = 10;

    private static final Logger logger = LogManager.getLogger(TransformIndexer.class);

    private static final long NUMBER_OF_CHECKPOINTS_TO_KEEP = 10;
    private static final long RETENTION_OF_CHECKPOINTS_MS = 864000000L; 
    private static final long CHECKPOINT_CLEANUP_INTERVAL = 100L; 

    public static final TimeValue DEFAULT_TRIGGER_SAVE_STATE_INTERVAL = TimeValue.timeValueSeconds(60);

    protected final TransformConfigManager transformsConfigManager;
    private final CheckpointProvider checkpointProvider;
    protected final TransformFailureHandler failureHandler;
    private volatile float docsPerSecond = -1;

    protected final TransformAuditor auditor;
    protected final TransformContext context;

    protected volatile TransformConfig transformConfig;
    private volatile TransformProgress progress;
    protected volatile boolean hasSourceChanged = true;

    protected final AtomicReference<Collection<ActionListener<Void>>> saveStateListeners = new AtomicReference<>();

    private volatile Map<String, String> fieldMappings;

    private Function function;

    private ChangeCollector changeCollector;

    private Map<String, Object> nextChangeCollectorBucketPosition = null;

    private volatile Integer initialConfiguredPageSize;
    private final AtomicInteger remainingCheckpointsUntilAudit = new AtomicInteger(0);
    private volatile TransformCheckpoint lastCheckpoint;
    private volatile TransformCheckpoint nextCheckpoint;

    private volatile RunState runState;

    private volatile long lastCheckpointCleanup = 0L;
    private volatile long lastSaveStateMilliseconds;

    protected volatile boolean indexerThreadShuttingDown = false;
    protected volatile boolean saveStateRequestedDuringIndexerThreadShutdown = false;

    @SuppressWarnings("this-escape")
    public TransformIndexer(
        ThreadPool threadPool,
        TransformServices transformServices,
        CheckpointProvider checkpointProvider,
        TransformConfig transformConfig,
        AtomicReference<IndexerState> initialState,
        TransformIndexerPosition initialPosition,
        TransformIndexerStats jobStats,
        TransformProgress transformProgress,
        TransformCheckpoint lastCheckpoint,
        TransformCheckpoint nextCheckpoint,
        TransformContext context
    ) {
        super(threadPool, initialState, initialPosition, jobStats, context);
        ExceptionsHelper.requireNonNull(transformServices, "transformServices");
        this.transformsConfigManager = transformServices.getConfigManager();
        this.checkpointProvider = ExceptionsHelper.requireNonNull(checkpointProvider, "checkpointProvider");
        this.auditor = transformServices.getAuditor();
        this.transformConfig = ExceptionsHelper.requireNonNull(transformConfig, "transformConfig");
        this.progress = transformProgress != null ? transformProgress : new TransformProgress();
        this.lastCheckpoint = ExceptionsHelper.requireNonNull(lastCheckpoint, "lastCheckpoint");
        this.nextCheckpoint = ExceptionsHelper.requireNonNull(nextCheckpoint, "nextCheckpoint");
        this.context = ExceptionsHelper.requireNonNull(context, "context");
        this.runState = RunState.APPLY_RESULTS;

        this.failureHandler = new TransformFailureHandler(auditor, context, transformConfig.getId());
        if (transformConfig.getSettings() != null && transformConfig.getSettings().getDocsPerSecond() != null) {
            docsPerSecond = transformConfig.getSettings().getDocsPerSecond();
        }
        this.lastSaveStateMilliseconds = TimeUnit.NANOSECONDS.toMillis(getTimeNanos());
    }

    abstract void doGetInitialProgress(SearchRequest request, ActionListener<SearchResponse> responseListener);

    abstract void doGetFieldMappings(ActionListener<Map<String, String>> fieldMappingsListener);

    abstract void doMaybeCreateDestIndex(Map<String, String> deducedDestIndexMappings, ActionListener<Boolean> listener);

    abstract void doDeleteByQuery(DeleteByQueryRequest deleteByQueryRequest, ActionListener<BulkByScrollResponse> responseListener);

    abstract void refreshDestinationIndex(ActionListener<Void> responseListener);

    abstract void persistState(TransformState state, ActionListener<Void> listener);

    abstract void validate(ActionListener<ValidateTransformAction.Response> listener);

    @Override
    protected String getJobId() {
        return transformConfig.getId();
    }

    @Override
    protected float getMaxDocsPerSecond() {
        return docsPerSecond;
    }

    @Override
    protected boolean triggerSaveState() {
        if (saveStateListeners.get() != null) {
            return true;
        }
        long currentTimeMilliseconds = TimeUnit.NANOSECONDS.toMillis(getTimeNanos());
        long nextSaveStateMilliseconds = TransformSchedulingUtils.calculateNextScheduledTime(
            lastSaveStateMilliseconds,
            DEFAULT_TRIGGER_SAVE_STATE_INTERVAL,
            context.getStatePersistenceFailureCount()
        );
        return currentTimeMilliseconds > nextSaveStateMilliseconds;
    }

    public TransformConfig getConfig() {
        return transformConfig;
    }

    public boolean isContinuous() {
        return getConfig().getSyncConfig() != null;
    }

    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }

    public TransformProgress getProgress() {
        return progress;
    }

    public TransformCheckpoint getLastCheckpoint() {
        return lastCheckpoint;
    }

    public TransformCheckpoint getNextCheckpoint() {
        return nextCheckpoint;
    }

    /**
     * Request a checkpoint
     */
    protected void createCheckpoint(ActionListener<TransformCheckpoint> listener) {
        checkpointProvider.createNextCheckpoint(
            getLastCheckpoint(),
            ActionListener.wrap(
                checkpoint -> transformsConfigManager.putTransformCheckpoint(
                    checkpoint,
                    ActionListener.wrap(putCheckPointResponse -> listener.onResponse(checkpoint), createCheckpointException -> {
                        logger.warn(() -> "[" + getJobId() + "] failed to create checkpoint.", createCheckpointException);
                        listener.onFailure(
                            new RuntimeException(
                                "Failed to create checkpoint due to: " + createCheckpointException.getMessage(),
                                createCheckpointException
                            )
                        );
                    })
                ),
                getCheckPointException -> {
                    logger.warn(() -> "[" + getJobId() + "] failed to retrieve checkpoint.", getCheckPointException);
                    listener.onFailure(
                        new RuntimeException(
                            "Failed to retrieve checkpoint due to: " + getCheckPointException.getMessage(),
                            getCheckPointException
                        )
                    );
                }
            )
        );
    }

    @Override
    protected void onStart(long now, ActionListener<Boolean> listener) {
        if (context.getTaskState() == TransformTaskState.FAILED) {
            logger.debug("[{}] attempted to start while in state [{}].", getJobId(), TransformTaskState.FAILED.value());
            listener.onFailure(new ElasticsearchException("Attempted to start a failed transform [{}].", getJobId()));
            return;
        }

        switch (getState()) {
            case ABORTING, STOPPING, STOPPED -> {
                logger.debug("[{}] attempted to start while in state [{}].", getJobId(), getState().value());
                listener.onResponse(false);
                return;
            }
        }

        if (context.getAuthState() != null && HealthStatus.RED.equals(context.getAuthState().getStatus())) {
            listener.onFailure(
                new ElasticsearchSecurityException(
                    TransformMessages.getMessage(TransformMessages.TRANSFORM_CANNOT_START_WITHOUT_PERMISSIONS, getConfig().getId())
                )
            );
            return;
        }

        ActionListener<Void> finalListener = listener.delegateFailureAndWrap((l, r) -> {
            if (context.getPageSize() == 0) {
                configurePageSize(getConfig().getSettings().getMaxPageSearchSize());
            }

            runState = determineRunStateAtStart();
            l.onResponse(true);
        });

        ActionListener<Boolean> configurationReadyListener = ActionListener.wrap(unused -> {
            initializeFunction();

            if (initialRun()) {
                createCheckpoint(ActionListener.wrap(cp -> {
                    nextCheckpoint = cp;
                    if (nextCheckpoint.getCheckpoint() > 1) {
                        progress = new TransformProgress(null, 0L, 0L);
                        finalListener.onResponse(null);
                        return;
                    }

                    SearchRequest request = new SearchRequest(transformConfig.getSource().getIndex());
                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

                    function.buildSearchQueryForInitialProgress(searchSourceBuilder);
                    searchSourceBuilder.query(QueryBuilders.boolQuery().filter(buildFilterQuery()).filter(searchSourceBuilder.query()));
                    request.allowPartialSearchResults(false).source(searchSourceBuilder);

                    doGetInitialProgress(request, ActionListener.wrap(response -> {
                        function.getInitialProgressFromResponse(response, ActionListener.wrap(newProgress -> {
                            logger.trace("[{}] reset the progress from [{}] to [{}].", getJobId(), progress, newProgress);
                            progress = newProgress != null ? newProgress : new TransformProgress();
                            finalListener.onResponse(null);
                        }, failure -> {
                            progress = new TransformProgress();
                            logger.warn(() -> "[" + getJobId() + "] unable to load progress information for task.", failure);
                            finalListener.onResponse(null);
                        }));
                    }, failure -> {
                        progress = new TransformProgress();
                        logger.warn(() -> "[" + getJobId() + "] unable to load progress information for task.", failure);
                        finalListener.onResponse(null);
                    }));
                }, listener::onFailure));
            } else {
                finalListener.onResponse(null);
            }
        }, listener::onFailure);

        var deducedDestIndexMappings = new SetOnce<Map<String, String>>();
        var shouldMaybeCreateDestIndexForUnattended = context.getCheckpoint() == 0
            && TransformEffectiveSettings.isUnattended(transformConfig.getSettings());

        ActionListener<Map<String, String>> fieldMappingsListener = ActionListener.wrap(destIndexMappings -> {
            if (destIndexMappings.isEmpty() == false) {
                this.fieldMappings = destIndexMappings;
            } else {
                this.fieldMappings = deducedDestIndexMappings.get();
            }
            if (destIndexMappings.isEmpty() && shouldMaybeCreateDestIndexForUnattended) {
                doMaybeCreateDestIndex(deducedDestIndexMappings.get(), configurationReadyListener);
            } else {
                configurationReadyListener.onResponse(null);
            }
        }, listener::onFailure);

        ActionListener<Void> reLoadFieldMappingsListener = ActionListener.wrap(updateConfigResponse -> {
            logger.debug(() -> format("[%s] Retrieve field mappings from the destination index", getJobId()));

            doGetFieldMappings(fieldMappingsListener);
        }, listener::onFailure);

        ActionListener<ValidateTransformAction.Response> changedSourceListener = ActionListener.wrap(validationResponse -> {
            deducedDestIndexMappings.set(validationResponse.getDestIndexMappings());
            if (isContinuous()) {
                transformsConfigManager.getTransformConfiguration(getJobId(), ActionListener.wrap(config -> {
                    if (transformConfig.equals(config) && fieldMappings != null && shouldMaybeCreateDestIndexForUnattended == false) {
                        logger.trace("[{}] transform config has not changed.", getJobId());
                        configurationReadyListener.onResponse(null);
                    } else {
                        transformConfig = config;
                        logger.debug("[{}] successfully refreshed transform config from index.", getJobId());
                        reLoadFieldMappingsListener.onResponse(null);
                    }
                }, failure -> {
                    String msg = TransformMessages.getMessage(TransformMessages.FAILED_TO_RELOAD_TRANSFORM_CONFIGURATION, getJobId());
                    if (failure instanceof ResourceNotFoundException) {
                        logger.error(msg, failure);
                        reLoadFieldMappingsListener.onFailure(new TransformConfigLostOnReloadException(msg, failure));
                    } else {
                        logger.warn(msg, failure);
                        auditor.warning(getJobId(), msg);
                        reLoadFieldMappingsListener.onResponse(null);
                    }
                }));
            } else {
                reLoadFieldMappingsListener.onResponse(null);
            }
        }, listener::onFailure);

        Instant instantOfTrigger = Instant.ofEpochMilli(now);
        if (context.getCheckpoint() > 0 && initialRun()) {
            checkpointProvider.sourceHasChanged(getLastCheckpoint(), ActionListener.wrap(hasChanged -> {
                context.setLastSearchTime(instantOfTrigger);
                hasSourceChanged = hasChanged;
                if (hasChanged) {
                    context.setChangesLastDetectedAt(instantOfTrigger);
                    logger.debug("[{}] source has changed, triggering new indexer run.", getJobId());
                    changedSourceListener.onResponse(new ValidateTransformAction.Response(emptyMap()));
                } else {
                    logger.trace("[{}] source has not changed, finish indexer early.", getJobId());
                    listener.onResponse(false);
                }
            }, failure -> {
                hasSourceChanged = true;
                listener.onFailure(failure);
            }));
        } else if (context.getCheckpoint() == 0 && TransformEffectiveSettings.isUnattended(transformConfig.getSettings())) {
            validate(changedSourceListener);
        } else {
            hasSourceChanged = true;
            context.setLastSearchTime(instantOfTrigger);
            context.setChangesLastDetectedAt(instantOfTrigger);
            changedSourceListener.onResponse(new ValidateTransformAction.Response(emptyMap()));
        }
    }

    protected void initializeFunction() {
        function = FunctionFactory.create(getConfig());
        if (isContinuous()) {
            changeCollector = function.buildChangeCollector(getConfig().getSyncConfig().getField());
        }
    }

    protected boolean initialRun() {
        return getPosition() == null;
    }

    @Override
    protected void onFinish(ActionListener<Void> listener) {
        startIndexerThreadShutdown();

        if (hasSourceChanged == false) {
            if (context.shouldStopAtCheckpoint()) {
                stop();
            }
            listener.onResponse(null);
            return;
        }

        ActionListener<Void> failureHandlingListener = ActionListener.wrap(listener::onResponse, failure -> {
            failureHandler.handleIndexerFailure(failure, getConfig().getSettings());
            listener.onFailure(failure);
        });

        try {
            refreshDestinationIndex(ActionListener.wrap(response -> {
                if (transformConfig.getRetentionPolicyConfig() != null) {
                    executeRetentionPolicy(failureHandlingListener);
                } else {
                    finalizeCheckpoint(failureHandlingListener);
                }
            }, failureHandlingListener::onFailure));
        } catch (Exception e) {
            failureHandlingListener.onFailure(e);
        }
    }

    private void executeRetentionPolicy(ActionListener<Void> listener) {
        DeleteByQueryRequest deleteByQuery = RetentionPolicyToDeleteByQueryRequestConverter.buildDeleteByQueryRequest(
            transformConfig.getRetentionPolicyConfig(),
            transformConfig.getSettings(),
            transformConfig.getDestination(),
            nextCheckpoint
        );

        if (deleteByQuery == null) {
            finalizeCheckpoint(listener);
            return;
        }

        logger.debug(
            () -> format(
                "[%s] Run delete based on retention policy using dbq [%s] with query: [%s]",
                getJobId(),
                deleteByQuery,
                deleteByQuery.getSearchRequest()
            )
        );
        getStats().markStartDelete();

        ActionListener<Void> deleteByQueryAndRefreshDoneListener = ActionListener.wrap(
            unused -> finalizeCheckpoint(listener),
            listener::onFailure
        );

        doDeleteByQuery(deleteByQuery, ActionListener.wrap(bulkByScrollResponse -> {
            logger.trace(() -> format("[%s] dbq response: [%s]", getJobId(), bulkByScrollResponse));

            getStats().markEndDelete();
            getStats().incrementNumDeletedDocuments(bulkByScrollResponse.getDeleted());
            logger.debug("[{}] deleted [{}] documents as part of the retention policy.", getJobId(), bulkByScrollResponse.getDeleted());

            if (bulkByScrollResponse.getVersionConflicts() > 0) {
                listener.onFailure(
                    new RetentionPolicyException(
                        "found [{}] version conflicts when deleting documents as part of the retention policy.",
                        bulkByScrollResponse.getDeleted()
                    )
                );
                return;
            }
            if (bulkByScrollResponse.getBulkFailures().size() > 0 || bulkByScrollResponse.getSearchFailures().size() > 0) {
                assert false : "delete by query failed unexpectedly" + bulkByScrollResponse;
                listener.onFailure(
                    new RetentionPolicyException(
                        "found failures when deleting documents as part of the retention policy. Response: [{}]",
                        bulkByScrollResponse
                    )
                );
                return;
            }

            refreshDestinationIndex(deleteByQueryAndRefreshDoneListener);
        }, listener::onFailure));
    }

    private void finalizeCheckpoint(ActionListener<Void> listener) {
        try {
            if (function != null) {
                context.setPageSize(function.getInitialPageSize());
            }
            if (changeCollector != null) {
                changeCollector.clear();
            }

            long checkpoint = context.incrementAndGetCheckpoint();
            lastCheckpoint = getNextCheckpoint();
            nextCheckpoint = null;
            context.resetReasonAndFailureCounter();

            if (progress.getPercentComplete() != null && progress.getPercentComplete() < 100.0) {
                progress.incrementDocsProcessed(progress.getTotalDocs() - progress.getDocumentsProcessed());
            }

            if (lastCheckpoint != null) {
                long docsIndexed = progress.getDocumentsIndexed();
                long docsProcessed = progress.getDocumentsProcessed();

                long durationMs = System.currentTimeMillis() - lastCheckpoint.getTimestamp();
                getStats().incrementCheckpointExponentialAverages(durationMs < 0 ? 0 : durationMs, docsIndexed, docsProcessed);
            }
            if (shouldAuditOnFinish(checkpoint)) {
                auditor.info(getJobId(), "Finished indexing for transform checkpoint [" + checkpoint + "].");
            }
            logger.debug("[{}] finished indexing for transform checkpoint [{}].", getJobId(), checkpoint);
            if (context.shouldStopAtCheckpoint()) {
                stop();
            }

            if (checkpoint - lastCheckpointCleanup > CHECKPOINT_CLEANUP_INTERVAL) {
                cleanupOldCheckpoints(listener);
            } else {
                listener.onResponse(null);
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected void afterFinishOrFailure() {
        finishIndexerThreadShutdown();
    }

    @Override
    protected IterationResult<TransformIndexerPosition> doProcess(SearchResponse searchResponse) {
        switch (runState) {
            case APPLY_RESULTS:
                return processBuckets(searchResponse);
            case IDENTIFY_CHANGES:
                return processChangedBuckets(searchResponse);

            default:
                logger.warn("[{}] Encountered unexpected run state [{}]", getJobId(), runState);
                throw new IllegalStateException("Transform indexer job encountered an illegal state [" + runState + "]");
        }
    }

    @Override
    public boolean maybeTriggerAsyncJob(long now) {

        if (context.getTaskState() == TransformTaskState.FAILED) {
            logger.debug("[{}] schedule was triggered for transform but task is failed. Ignoring trigger.", getJobId());
            return false;
        }

        synchronized (context) {
            IndexerState indexerState = getState();
            if (IndexerState.INDEXING.equals(indexerState) || IndexerState.STOPPING.equals(indexerState)) {
                logger.debug("[{}] indexer for transform has state [{}]. Ignoring trigger.", getJobId(), indexerState);
                return false;
            }

            /*
             * ignore if indexer thread is shutting down (after finishing a checkpoint)
             * shutting down means:
             *  - indexer has finished a checkpoint and called onFinish
             *  - indexer state has changed from indexing to started
             *  - state persistence has been called but has _not_ returned yet
             *
             *  If we trigger the indexer in this situation the 2nd indexer thread might
             *  try to save state at the same time, causing a version conflict
             *  see gh#67121
             */
            if (indexerThreadShuttingDown) {
                logger.debug("[{}] indexer thread is shutting down. Ignoring trigger.", getJobId());
                return false;
            }

            return super.maybeTriggerAsyncJob(now);
        }
    }

    /**
     * Handle new settings at runtime, this is triggered by a call to _transform/id/_update
     *
     * @param newSettings The new settings that should be applied
     */
    public void applyNewSettings(SettingsConfig newSettings) {
        auditor.info(transformConfig.getId(), "Transform settings have been updated.");
        logger.info("[{}] transform settings have been updated.", transformConfig.getId());

        docsPerSecond = newSettings.getDocsPerSecond() != null ? newSettings.getDocsPerSecond() : -1;
        if (Objects.equals(newSettings.getMaxPageSearchSize(), initialConfiguredPageSize) == false) {
            configurePageSize(newSettings.getMaxPageSearchSize());
        }
        rethrottle();
    }

    @Override
    protected void onFailure(Exception exc) {
        startIndexerThreadShutdown();
        try {
            failureHandler.handleIndexerFailure(exc, getConfig().getSettings());
        } catch (Exception e) {
            logger.error(() -> "[" + getJobId() + "] transform encountered an unexpected internal exception: ", e);
        }
    }

    @Override
    protected void onStop() {
        auditor.info(transformConfig.getId(), "Transform has stopped.");
        logger.info("[{}] transform has stopped.", transformConfig.getId());
    }

    @Override
    protected void onAbort() {
        auditor.info(transformConfig.getId(), "Received abort request, stopping transform.");
        logger.info("[{}] transform received abort request. Stopping indexer.", transformConfig.getId());
        context.shutdown();
    }

    @Override
    protected void doSaveState(IndexerState indexerState, TransformIndexerPosition position, Runnable next) {
        if (context.getTaskState() == TransformTaskState.FAILED) {
            logger.debug("[{}] attempted to save state and stats while failed.", getJobId());
            next.run();
            return;
        }
        if (indexerState.equals(IndexerState.ABORTING)) {
            next.run();
            return;
        }

        Collection<ActionListener<Void>> saveStateListenersAtTheMomentOfCalling = saveStateListeners.getAndSet(null);
        boolean shouldStopAtCheckpoint = context.shouldStopAtCheckpoint();

        if (shouldStopAtCheckpoint && initialRun() && indexerState.equals(IndexerState.STARTED)) {
            indexerState = IndexerState.STOPPED;
            auditor.info(transformConfig.getId(), "Transform is no longer in the middle of a checkpoint, initiating stop.");
            logger.info("[{}] transform is no longer in the middle of a checkpoint, initiating stop.", transformConfig.getId());
        }

        if (hasSourceChanged == false && indexerState.equals(IndexerState.STOPPED) == false) {
            if (saveStateListenersAtTheMomentOfCalling != null) {
                ActionListener.onResponse(saveStateListenersAtTheMomentOfCalling, null);
            }
            next.run();
            return;
        }

        TransformTaskState taskState = context.getTaskState();

        if (indexerState.equals(IndexerState.STARTED) && context.getCheckpoint() == 1 && this.isContinuous() == false) {
            indexerState = IndexerState.STOPPED;

            auditor.info(transformConfig.getId(), "Transform finished indexing all data, initiating stop");
            logger.info("[{}] transform finished indexing all data, initiating stop.", transformConfig.getId());
        }

        if (indexerState.equals(IndexerState.STOPPED)) {
            shouldStopAtCheckpoint = false;

            taskState = TransformTaskState.STOPPED;
        }

        final TransformState state = new TransformState(
            taskState,
            indexerState,
            position,
            context.getCheckpoint(),
            context.getStateReason(),
            getProgress(),
            null,
            shouldStopAtCheckpoint,
            context.getAuthState()
        );
        logger.debug("[{}] updating persistent state of transform to [{}].", transformConfig.getId(), state.toString());

        persistStateWithAutoStop(state, ActionListener.wrap(r -> {
            try {
                if (saveStateListenersAtTheMomentOfCalling != null) {
                    ActionListener.onResponse(saveStateListenersAtTheMomentOfCalling, r);
                }
            } catch (Exception onResponseException) {
                String msg = LoggerMessageFormat.format("[{}] failed notifying saveState listeners, ignoring.", getJobId());
                logger.warn(msg, onResponseException);
            } finally {
                lastSaveStateMilliseconds = TimeUnit.NANOSECONDS.toMillis(getTimeNanos());
                next.run();
            }
        }, e -> {
            try {
                if (saveStateListenersAtTheMomentOfCalling != null) {
                    ActionListener.onFailure(saveStateListenersAtTheMomentOfCalling, e);
                }
            } catch (Exception onFailureException) {
                String msg = LoggerMessageFormat.format("[{}] failed notifying saveState listeners, ignoring.", getJobId());
                logger.warn(msg, onFailureException);
            } finally {
                next.run();
            }
        }));
    }

    private void persistStateWithAutoStop(TransformState state, ActionListener<Void> listener) {
        persistState(state, ActionListener.runBefore(listener, () -> {
            if (state.getTaskState().equals(TransformTaskState.STOPPED)) {
                context.shutdown();
            }
        }));
    }

    /**
     * Let the indexer stop at the next checkpoint and call the listener after the flag has been persisted in state.
     *
     * If the indexer isn't running, persist state if required and call the listener immediately.
     */
    final void setStopAtCheckpoint(boolean shouldStopAtCheckpoint, ActionListener<Void> shouldStopAtCheckpointListener) {
        assert ThreadPool.assertCurrentThreadPool(ThreadPool.Names.GENERIC);

        try {
            if (addSetStopAtCheckpointListener(shouldStopAtCheckpoint, shouldStopAtCheckpointListener) == false) {
                shouldStopAtCheckpointListener.onResponse(null);
            }
        } catch (InterruptedException e) {
            logger.error(
                () -> format(
                    "[%s] Interrupt waiting (%ss) for transform state to be stored.",
                    getJobId(),
                    PERSIST_STOP_AT_CHECKPOINT_TIMEOUT_SEC
                ),
                e
            );

            shouldStopAtCheckpointListener.onFailure(
                new RuntimeException(
                    "Timed out (" + PERSIST_STOP_AT_CHECKPOINT_TIMEOUT_SEC + "s) waiting for transform state to be stored.",
                    e
                )
            );
        } catch (Exception e) {
            logger.error(() -> "[" + getJobId() + "] failed to persist transform state.", e);
            shouldStopAtCheckpointListener.onFailure(e);
        }
    }

    private boolean addSetStopAtCheckpointListener(boolean shouldStopAtCheckpoint, ActionListener<Void> shouldStopAtCheckpointListener)
        throws InterruptedException {

        synchronized (context) {
            if (indexerThreadShuttingDown) {
                context.setShouldStopAtCheckpoint(shouldStopAtCheckpoint);
                saveStateRequestedDuringIndexerThreadShutdown = true;
                return false;
            }

            IndexerState state = getState();

            if (state == IndexerState.STARTED && context.shouldStopAtCheckpoint() != shouldStopAtCheckpoint) {
                IndexerState newIndexerState = IndexerState.STARTED;
                TransformTaskState newtaskState = context.getTaskState();

                if (shouldStopAtCheckpoint && initialRun()) {
                    newIndexerState = IndexerState.STOPPED;
                    newtaskState = TransformTaskState.STOPPED;
                    logger.debug("[{}] transform is at a checkpoint, initiating stop.", transformConfig.getId());
                } else {
                    context.setShouldStopAtCheckpoint(shouldStopAtCheckpoint);
                }

                final TransformState newTransformState = new TransformState(
                    newtaskState,
                    newIndexerState,
                    getPosition(),
                    context.getCheckpoint(),
                    context.getStateReason(),
                    getProgress(),
                    null,
                    newIndexerState == IndexerState.STARTED,
                    context.getAuthState()
                );

                CountDownLatch latch = new CountDownLatch(1);
                logger.debug("[{}] persisting stop at checkpoint", getJobId());

                persistState(newTransformState, ActionListener.running(() -> latch.countDown()));

                if (latch.await(PERSIST_STOP_AT_CHECKPOINT_TIMEOUT_SEC, TimeUnit.SECONDS) == false) {
                    logger.error(
                        () -> format(
                            "[%s] Timed out (%ss) waiting for transform state to be stored.",
                            getJobId(),
                            PERSIST_STOP_AT_CHECKPOINT_TIMEOUT_SEC
                        )
                    );
                }

                if (newtaskState.equals(TransformTaskState.STOPPED)) {
                    context.shutdown();
                }

                return false;
            }

            if (state != IndexerState.INDEXING) {
                return false;
            }

            if (saveStateListeners.updateAndGet(currentListeners -> {
                if (getState() != IndexerState.INDEXING) {
                    return null;
                }

                if (currentListeners == null) {
                    if (context.shouldStopAtCheckpoint() == shouldStopAtCheckpoint) {
                        return null;
                    }

                    return Collections.singletonList(shouldStopAtCheckpointListener);
                }
                return CollectionUtils.appendToCopy(currentListeners, shouldStopAtCheckpointListener);
            }) == null) {
                return false;
            }

            context.setShouldStopAtCheckpoint(shouldStopAtCheckpoint);
        }
        runSearchImmediately();
        return true;
    }

    void stopAndMaybeSaveState() {
        synchronized (context) {
            onStop();
            IndexerState state = stop();

            if (indexerThreadShuttingDown) {
                saveStateRequestedDuringIndexerThreadShutdown = true;
            } else if (state == IndexerState.STOPPED) {
                doSaveState(IndexerState.STOPPED, getPosition(), () -> {});
            }
        }
    }

    /**
     * Checks the given exception and handles the error based on it.
     *
     * In case the error is permanent or the number for failures exceed the number of retries, sets the indexer
     * to `FAILED`.
     *
     * Important: Might call into TransformTask, this should _only_ be called with an acquired indexer lock if and only if
     * the lock for TransformTask has been acquired, too. See gh#75846
     *
     * (Note: originally this method was synchronized, which is not necessary)
     */
    void handleFailure(Exception e) {
        failureHandler.handleIndexerFailure(e, getConfig().getSettings());
    }

    /**
     * Cleanup old checkpoints
     *
     * @param listener listener to call after done
     */
    private void cleanupOldCheckpoints(ActionListener<Void> listener) {
        long now = getTimeNanos() * 1000;
        long checkpointLowerBound = context.getCheckpoint() - NUMBER_OF_CHECKPOINTS_TO_KEEP;
        long lowerBoundEpochMs = now - RETENTION_OF_CHECKPOINTS_MS;

        if (checkpointLowerBound > 0 && lowerBoundEpochMs > 0) {
            transformsConfigManager.deleteOldCheckpoints(
                transformConfig.getId(),
                checkpointLowerBound,
                lowerBoundEpochMs,
                ActionListener.wrap(deletes -> {
                    logger.debug("[{}] deleted [{}] outdated checkpoints", getJobId(), deletes);
                    listener.onResponse(null);
                    lastCheckpointCleanup = context.getCheckpoint();
                }, e -> {
                    logger.warn(() -> "[" + getJobId() + "] failed to cleanup old checkpoints, retrying after next checkpoint", e);
                    auditor.warning(
                        getJobId(),
                        "Failed to cleanup old checkpoints, retrying after next checkpoint. Exception: " + e.getMessage()
                    );

                    listener.onResponse(null);
                })
            );
        } else {
            logger.debug("[{}] checked for outdated checkpoints", getJobId());
            listener.onResponse(null);
        }
    }

    private IterationResult<TransformIndexerPosition> processBuckets(final SearchResponse searchResponse) {
        Tuple<Stream<IndexRequest>, Map<String, Object>> indexRequestStreamAndCursor = function.processSearchResponse(
            searchResponse,
            getConfig().getDestination().getIndex(),
            getConfig().getDestination().getPipeline(),
            getFieldMappings(),
            getStats(),
            progress
        );

        if (indexRequestStreamAndCursor == null || indexRequestStreamAndCursor.v1() == null) {
            if (nextCheckpoint.getCheckpoint() == 1 || isContinuous() == false || changeCollector.queryForChanges() == false) {
                return new IterationResult<>(Stream.empty(), null, true);
            }

            changeCollector.clear();

            runState = RunState.IDENTIFY_CHANGES;

            return new IterationResult<>(Stream.empty(), new TransformIndexerPosition(null, nextChangeCollectorBucketPosition), false);
        }

        Stream<IndexRequest> indexRequestStream = indexRequestStreamAndCursor.v1();
        TransformIndexerPosition oldPosition = getPosition();
        TransformIndexerPosition newPosition = new TransformIndexerPosition(
            indexRequestStreamAndCursor.v2(),
            oldPosition != null ? getPosition().getBucketsPosition() : null
        );

        return new IterationResult<>(indexRequestStream, newPosition, false);
    }

    private IterationResult<TransformIndexerPosition> processChangedBuckets(final SearchResponse searchResponse) {
        nextChangeCollectorBucketPosition = changeCollector.processSearchResponse(searchResponse);

        if (nextChangeCollectorBucketPosition == null) {
            changeCollector.clear();
            return new IterationResult<>(Stream.empty(), null, true);
        }

        runState = RunState.APPLY_RESULTS;

        return new IterationResult<>(Stream.empty(), getPosition(), false);
    }

    protected QueryBuilder buildFilterQuery() {
        assert nextCheckpoint != null;

        QueryBuilder queryBuilder = getConfig().getSource().getQueryConfig().getQuery();

        TransformConfig config = getConfig();
        if (this.isContinuous()) {
            BoolQueryBuilder filteredQuery = new BoolQueryBuilder().filter(queryBuilder);

            if (lastCheckpoint != null) {
                filteredQuery.filter(config.getSyncConfig().getRangeQuery(lastCheckpoint, nextCheckpoint));
            } else {
                filteredQuery.filter(config.getSyncConfig().getRangeQuery(nextCheckpoint));
            }
            return filteredQuery;
        }

        return queryBuilder;
    }

    protected Tuple<String, SearchRequest> buildSearchRequest() {
        assert nextCheckpoint != null;

        switch (runState) {
            case APPLY_RESULTS:
                return new Tuple<>("apply_results", buildQueryToUpdateDestinationIndex());
            case IDENTIFY_CHANGES:
                return new Tuple<>("identify_changes", buildQueryToFindChanges());
            default:
                logger.warn("Encountered unexpected run state [" + runState + "]");
                throw new IllegalStateException("Transform indexer job encountered an illegal state [" + runState + "]");
        }
    }

    private SearchRequest buildQueryToFindChanges() {
        assert isContinuous();

        TransformIndexerPosition position = getPosition();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().runtimeMappings(getConfig().getSource().getRuntimeMappings());

        SearchRequest request = new SearchRequest(
            /*
             * gh#77329 optimization turned off, gh#81252 transform can fail if an index gets deleted during searches
             *
             * Until proper checkpoint searches (seq_id per shard) are possible, we have to query
             *  - all indices
             *  - resolve indices at search
             *
             * TransformCheckpoint.getChangedIndices(TransformCheckpoint.EMPTY, getNextCheckpoint()).toArray(new String[0])
             */
            getConfig().getSource().getIndex()
        );

        request.allowPartialSearchResults(false) 
            .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN); 

        changeCollector.buildChangesQuery(sourceBuilder, position != null ? position.getBucketsPosition() : null, context.getPageSize());

        QueryBuilder queryBuilder = getConfig().getSource().getQueryConfig().getQuery();

        TransformConfig config = getConfig();
        BoolQueryBuilder filteredQuery = new BoolQueryBuilder().filter(queryBuilder)
            .filter(config.getSyncConfig().getRangeQuery(lastCheckpoint, nextCheckpoint));

        sourceBuilder.query(filteredQuery);

        logger.debug("[{}] Querying {} for changes: {}", getJobId(), request.indices(), sourceBuilder);
        return request.source(sourceBuilder);
    }

    private SearchRequest buildQueryToUpdateDestinationIndex() {
        TransformIndexerPosition position = getPosition();

        TransformConfig config = getConfig();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().runtimeMappings(getConfig().getSource().getRuntimeMappings());

        function.buildSearchQuery(sourceBuilder, position != null ? position.getIndexerPosition() : null, context.getPageSize());

        SearchRequest request = new SearchRequest();
        QueryBuilder queryBuilder = config.getSource().getQueryConfig().getQuery();

        if (isContinuous()) {
            BoolQueryBuilder filteredQuery = new BoolQueryBuilder().filter(queryBuilder)
                .filter(config.getSyncConfig().getRangeQuery(nextCheckpoint));

            if (changeCollector != null) {
                QueryBuilder filter = changeCollector.buildFilterQuery(lastCheckpoint, nextCheckpoint);
                if (filter != null) {
                    filteredQuery.filter(filter);
                }
                /*
                 * gh#81252 transform can fail if an index gets deleted during searches
                 *
                 * Until proper checkpoint searches (seq_id per shard) are possible, we have to query
                 *  - all indices
                 *  - resolve indices at search time
                 *
                 * request.indices(changeCollector.getIndicesToQuery(lastCheckpoint, nextCheckpoint).toArray(new String[0]));
                 */
                request.indices(getConfig().getSource().getIndex());
            } else {
                request.indices(getConfig().getSource().getIndex());
            }

            queryBuilder = filteredQuery;

        } else {
            request.indices(getConfig().getSource().getIndex());
        }

        sourceBuilder.query(queryBuilder);
        logger.debug("[{}] Querying {} for data: {}", getJobId(), request.indices(), sourceBuilder);

        return request.source(sourceBuilder)
            .allowPartialSearchResults(false) 
            .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN); 
    }

    /**
     * Indicates if an audit message should be written when onFinish is called for the given checkpoint.
     * We audit every checkpoint for the first 10 checkpoints until completedCheckpoint == 9.
     * Then we audit every 10th checkpoint until completedCheckpoint == 99.
     * Then we audit every 100th checkpoint until completedCheckpoint == 999.
     * Then we always audit every 1_000th checkpoints.
     *
     * @param completedCheckpoint The checkpoint that was just completed
     * @return {@code true} if an audit message should be written
     */
    protected boolean shouldAuditOnFinish(long completedCheckpoint) {
        return remainingCheckpointsUntilAudit.getAndUpdate(count -> {
            if (count > 0) {
                return count - 1;
            }

            if (completedCheckpoint >= 1000) {
                return 999;
            } else if (completedCheckpoint >= 100) {
                return 99;
            } else if (completedCheckpoint >= 10) {
                return 9;
            } else {
                return 0;
            }
        }) == 0;
    }

    private RunState determineRunStateAtStart() {
        if (context.from() != null && changeCollector != null && changeCollector.queryForChanges()) {
            return RunState.IDENTIFY_CHANGES;
        }

        if (nextCheckpoint.getCheckpoint() == 1 || isContinuous() == false) {
            return RunState.APPLY_RESULTS;
        }

        if (changeCollector == null || changeCollector.queryForChanges() == false) {
            return RunState.APPLY_RESULTS;
        }

        return RunState.IDENTIFY_CHANGES;
    }

    private void configurePageSize(Integer newPageSize) {
        initialConfiguredPageSize = newPageSize;

        if (initialConfiguredPageSize != null && initialConfiguredPageSize > 0) {
            context.setPageSize(initialConfiguredPageSize);
        } else {
            context.setPageSize(function.getInitialPageSize());
        }
    }

    private void startIndexerThreadShutdown() {
        synchronized (context) {
            indexerThreadShuttingDown = true;
            saveStateRequestedDuringIndexerThreadShutdown = false;
        }
    }

    private void finishIndexerThreadShutdown() {
        synchronized (context) {
            indexerThreadShuttingDown = false;
            if (saveStateRequestedDuringIndexerThreadShutdown) {
                if (context.shouldStopAtCheckpoint() && nextCheckpoint == null) {
                    stop();
                }
                doSaveState(getState(), getPosition(), () -> {});
            }
        }
    }

    /**
     * Thrown when the transform configuration disappeared permanently.
     * (not if reloading failed due to an intermittent problem)
     */
    static class TransformConfigLostOnReloadException extends ResourceNotFoundException {
        TransformConfigLostOnReloadException(String msg, Throwable cause, Object... args) {
            super(msg, cause, args);
        }
    }
}
