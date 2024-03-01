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
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.ParentTaskAssigningClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.persistent.AllocatedPersistentTask;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata.PersistentTask;
import org.elasticsearch.persistent.PersistentTasksService;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.tasks.TaskManager;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.core.indexing.IndexerState;
import org.elasticsearch.xpack.core.transform.TransformField;
import org.elasticsearch.xpack.core.transform.TransformMessages;
import org.elasticsearch.xpack.core.transform.action.StartTransformAction;
import org.elasticsearch.xpack.core.transform.transforms.AuthorizationState;
import org.elasticsearch.xpack.core.transform.transforms.SettingsConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpointingInfo;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpointingInfo.TransformCheckpointingInfoBuilder;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerPosition;
import org.elasticsearch.xpack.core.transform.transforms.TransformIndexerStats;
import org.elasticsearch.xpack.core.transform.transforms.TransformState;
import org.elasticsearch.xpack.core.transform.transforms.TransformTaskParams;
import org.elasticsearch.xpack.core.transform.transforms.TransformTaskState;
import org.elasticsearch.xpack.transform.checkpoint.TransformCheckpointService;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.transforms.scheduling.TransformScheduler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.core.transform.TransformMessages.CANNOT_START_FAILED_TRANSFORM;
import static org.elasticsearch.xpack.core.transform.TransformMessages.CANNOT_STOP_FAILED_TRANSFORM;

public class TransformTask extends AllocatedPersistentTask implements TransformScheduler.Listener, TransformContext.Listener {

    private static final Logger logger = LogManager.getLogger(TransformTask.class);
    private static final IndexerState[] RUNNING_STATES = new IndexerState[] { IndexerState.STARTED, IndexerState.INDEXING };

    private final TransformTaskParams transform;
    private final TransformScheduler transformScheduler;
    private final ThreadPool threadPool;
    private final TransformAuditor auditor;
    private final TransformIndexerPosition initialPosition;
    private final IndexerState initialIndexerState;
    private final TransformContext context;
    private final SetOnce<ClientTransformIndexer> indexer = new SetOnce<>();

    @SuppressWarnings("this-escape")
    public TransformTask(
        long id,
        String type,
        String action,
        TaskId parentTask,
        TransformTaskParams transform,
        TransformState state,
        TransformScheduler transformScheduler,
        TransformAuditor auditor,
        ThreadPool threadPool,
        Map<String, String> headers
    ) {
        super(id, type, action, TransformField.PERSISTENT_TASK_DESCRIPTION_PREFIX + transform.getId(), parentTask, headers);
        this.transform = transform;
        this.transformScheduler = transformScheduler;
        this.threadPool = threadPool;
        this.auditor = auditor;
        IndexerState initialState = IndexerState.STOPPED;
        TransformTaskState initialTaskState = TransformTaskState.STOPPED;
        String initialReason = null;
        long initialCheckpoint = 0;
        TransformIndexerPosition initialPosition = null;

        if (state != null) {
            initialTaskState = state.getTaskState();
            initialReason = state.getReason();
            final IndexerState existingState = state.getIndexerState();
            if (existingState.equals(IndexerState.INDEXING)) {
                initialState = IndexerState.STARTED;
            } else if (existingState.equals(IndexerState.ABORTING) || existingState.equals(IndexerState.STOPPING)) {
                initialState = IndexerState.STOPPED;
            } else {
                initialState = existingState;
            }
            initialPosition = state.getPosition();
            initialCheckpoint = state.getCheckpoint();
        }

        this.initialIndexerState = initialState;
        this.initialPosition = initialPosition;

        this.context = new TransformContext(initialTaskState, initialReason, initialCheckpoint, transform.from(), this);
        if (state != null) {
            this.context.setAuthState(state.getAuthState());
        }
    }

    public String getTransformId() {
        return transform.getId();
    }

    /**
     * Enable Task API to return detailed status information
     */
    @Override
    public Status getStatus() {
        return getState();
    }

    private ClientTransformIndexer getIndexer() {
        return indexer.get();
    }

    public TransformState getState() {
        if (getIndexer() == null) {
            return new TransformState(
                context.getTaskState(),
                initialIndexerState,
                initialPosition,
                context.getCheckpoint(),
                context.getStateReason(),
                null,
                null,
                false,
                context.getAuthState()
            );
        } else {
            return new TransformState(
                context.getTaskState(),
                indexer.get().getState(),
                indexer.get().getPosition(),
                context.getCheckpoint(),
                context.getStateReason(),
                getIndexer().getProgress(),
                null,
                context.shouldStopAtCheckpoint(),
                context.getAuthState()
            );
        }
    }

    public TransformIndexerStats getStats() {
        if (getIndexer() == null) {
            return new TransformIndexerStats();
        } else {
            return getIndexer().getStats();
        }
    }

    public void getCheckpointingInfo(
        TransformCheckpointService transformsCheckpointService,
        ParentTaskAssigningClient parentTaskClient,
        ActionListener<TransformCheckpointingInfo> listener,
        TimeValue timeout
    ) {
        ActionListener<TransformCheckpointingInfoBuilder> checkPointInfoListener = ActionListener.wrap(infoBuilder -> {
            if (context.getChangesLastDetectedAt() != null) {
                infoBuilder.setChangesLastDetectedAt(context.getChangesLastDetectedAt());
            }
            if (context.getLastSearchTime() != null) {
                infoBuilder.setLastSearchTime(context.getLastSearchTime());
            }
            listener.onResponse(infoBuilder.build());
        }, listener::onFailure);

        ClientTransformIndexer transformIndexer = getIndexer();
        if (transformIndexer == null) {
            transformsCheckpointService.getCheckpointingInfo(
                parentTaskClient,
                timeout,
                transform.getId(),
                context.getCheckpoint(),
                initialPosition,
                null,
                checkPointInfoListener
            );
            return;
        }
        transformsCheckpointService.getCheckpointProvider(parentTaskClient, transformIndexer.getConfig())
            .getCheckpointingInfo(
                transformIndexer.getLastCheckpoint(),
                transformIndexer.getNextCheckpoint(),
                transformIndexer.getPosition(),
                transformIndexer.getProgress(),
                timeout,
                checkPointInfoListener
            );
    }

    /**
     * Derives basic checkpointing stats.  This does not make a call to obtain any additional information.
     * This will only read checkpointing information from this TransformTask.
     *
     * @return basic checkpointing info, including id, position, and progress of the Next Checkpoint and the id of the Last Checkpoint.
     */
    public TransformCheckpointingInfo deriveBasicCheckpointingInfo() {
        var transformIndexer = getIndexer();
        if (transformIndexer == null) {
            return TransformCheckpointingInfo.EMPTY;
        }
        return new TransformCheckpointingInfo.TransformCheckpointingInfoBuilder().setLastCheckpoint(transformIndexer.getLastCheckpoint())
            .setNextCheckpoint(transformIndexer.getNextCheckpoint())
            .setNextCheckpointPosition(transformIndexer.getPosition())
            .setNextCheckpointProgress(transformIndexer.getProgress())
            .build();
    }

    /**
     * Starts the transform and schedules it to be triggered in the future.
     *
     * @param startingCheckpoint The starting checkpoint, could null. Null indicates that there is no starting checkpoint
     * @param listener The listener to alert once started
     */
    void start(Long startingCheckpoint, ActionListener<StartTransformAction.Response> listener) {
        logger.debug("[{}] start called with state [{}].", getTransformId(), getState());
        if (context.getTaskState() == TransformTaskState.FAILED) {
            listener.onFailure(
                new ElasticsearchStatusException(
                    TransformMessages.getMessage(CANNOT_START_FAILED_TRANSFORM, getTransformId(), context.getStateReason()),
                    RestStatus.CONFLICT
                )
            );
            return;
        }

        synchronized (context) {
            if (getIndexer() == null) {
                String msg = context.getTaskState() == TransformTaskState.FAILED
                    ? "It failed during the initialization process; force stop to allow reinitialization."
                    : "Try again later.";
                listener.onFailure(
                    new ElasticsearchStatusException(
                        "Task for transform [{}] not fully initialized. {}",
                        RestStatus.CONFLICT,
                        getTransformId(),
                        msg
                    )
                );
                return;
            }
            final IndexerState newState = getIndexer().start();
            if (Arrays.stream(RUNNING_STATES).noneMatch(newState::equals)) {
                listener.onFailure(
                    new ElasticsearchException("Cannot start task for transform [{}], because state was [{}]", transform.getId(), newState)
                );
                return;
            }
            context.resetTaskState();

            if (startingCheckpoint != null) {
                context.setCheckpoint(startingCheckpoint);
            }

            final TransformState state = new TransformState(
                TransformTaskState.STARTED,
                IndexerState.STOPPED,
                getIndexer().getPosition(),
                context.getCheckpoint(),
                null,
                getIndexer().getProgress(),
                null,
                context.shouldStopAtCheckpoint(),
                context.getAuthState()
            );

            logger.info("[{}] updating state for transform to [{}].", transform.getId(), state.toString());
            persistStateToClusterState(state, ActionListener.wrap(task -> {
                auditor.info(transform.getId(), "Updated transform state to [" + state.getTaskState() + "].");
                transformScheduler.registerTransform(transform, this);
                listener.onResponse(new StartTransformAction.Response(true));
            }, exc -> {
                auditor.warning(
                    transform.getId(),
                    "Failed to persist to cluster state while marking task as started. Failure: " + exc.getMessage()
                );
                logger.error(() -> format("[%s] failed updating state to [%s].", getTransformId(), state), exc);
                getIndexer().stop();
                listener.onFailure(
                    new ElasticsearchException(
                        "Error while updating state for transform [" + transform.getId() + "] to [" + state.getIndexerState() + "].",
                        exc
                    )
                );
            }));
        }
    }

    /**
     * This sets the flag for the task to stop at the next checkpoint.
     *
     * @param shouldStopAtCheckpoint whether or not we should stop at the next checkpoint or not
     * @param shouldStopAtCheckpointListener the listener to return to when we have persisted the updated value to the state index.
     */
    public void setShouldStopAtCheckpoint(boolean shouldStopAtCheckpoint, ActionListener<Void> shouldStopAtCheckpointListener) {
        assert ThreadPool.assertCurrentThreadPool(ThreadPool.Names.GENERIC);
        logger.debug(
            "[{}] attempted to set task to stop at checkpoint [{}] with state [{}]",
            getTransformId(),
            shouldStopAtCheckpoint,
            getState()
        );
        synchronized (context) {
            if (context.getTaskState() != TransformTaskState.STARTED || getIndexer() == null) {
                shouldStopAtCheckpointListener.onResponse(null);
                return;
            }

            if (context.shouldStopAtCheckpoint() == shouldStopAtCheckpoint) {
                shouldStopAtCheckpointListener.onResponse(null);
                return;
            }

            getIndexer().setStopAtCheckpoint(shouldStopAtCheckpoint, shouldStopAtCheckpointListener);
        }
    }

    public void stop(boolean force, boolean shouldStopAtCheckpoint) {
        logger.debug(
            "[{}] stop called with force [{}], shouldStopAtCheckpoint [{}], state [{}], indexerstate[{}]",
            getTransformId(),
            force,
            shouldStopAtCheckpoint,
            getState(),
            getIndexer() != null ? getIndexer().getState() : null
        );

        synchronized (context) {
            if (context.getTaskState() == TransformTaskState.FAILED && force == false) {
                throw new ElasticsearchStatusException(
                    TransformMessages.getMessage(CANNOT_STOP_FAILED_TRANSFORM, getTransformId(), context.getStateReason()),
                    RestStatus.CONFLICT
                );
            }

            boolean wasFailed = context.setTaskState(TransformTaskState.FAILED, TransformTaskState.STARTED);
            context.resetReasonAndFailureCounter();

            if (getIndexer() == null) {
                shutdown();
                return;
            }

            if (wasFailed) {
                getIndexer().stopAndMaybeSaveState();
                return;
            }

            IndexerState indexerState = getIndexer().getState();

            if (indexerState == IndexerState.STOPPED || indexerState == IndexerState.STOPPING) {
                return;
            }

            if (shouldStopAtCheckpoint == false ||
                (indexerState == IndexerState.STARTED && getIndexer().initialRun())) {
                getIndexer().stopAndMaybeSaveState();
            }
        }
    }

    public void applyNewSettings(SettingsConfig newSettings) {
        synchronized (context) {
            getIndexer().applyNewSettings(newSettings);
        }
    }

    public void applyNewAuthState(AuthorizationState authState) {
        synchronized (context) {
            context.setAuthState(authState);
        }
    }

    @Override
    protected void init(
        PersistentTasksService persistentTasksService,
        TaskManager taskManager,
        String persistentTaskId,
        long allocationId
    ) {
        super.init(persistentTasksService, taskManager, persistentTaskId, allocationId);
    }

    @Override
    public void triggered(TransformScheduler.Event event) {
        logger.trace(() -> format("[%s] triggered(event=%s) ", getTransformId(), event));
        if (event.transformId().equals(getTransformId()) == false) {
            return;
        }

        synchronized (context) {
            if (getIndexer() == null) {
                logger.warn("[{}] transform task triggered with an unintialized indexer.", getTransformId());
                return;
            }

            if (context.getTaskState() == TransformTaskState.FAILED || context.getTaskState() == TransformTaskState.STOPPED) {
                logger.debug(
                    "[{}] schedule was triggered for transform but task is [{}]. Ignoring trigger.",
                    getTransformId(),
                    context.getTaskState()
                );
                return;
            }

            IndexerState indexerState = getIndexer().getState();
            if (IndexerState.INDEXING.equals(indexerState)
                || IndexerState.STOPPING.equals(indexerState)
                || IndexerState.STOPPED.equals(indexerState)) {
                logger.debug("[{}] indexer for transform has state [{}]. Ignoring trigger.", getTransformId(), indexerState);
                return;
            }

            logger.debug("[{}] transform indexer schedule has triggered, state: [{}].", getTransformId(), indexerState);

            if (context.getCheckpoint() == 0) {
                logger.debug("[{}] trigger initial run.", getTransformId());
                getIndexer().maybeTriggerAsyncJob(System.currentTimeMillis());
            } else if (getIndexer().isContinuous()) {
                getIndexer().maybeTriggerAsyncJob(System.currentTimeMillis());
            }
        }
    }

    @Override
    public boolean shouldCancelChildrenOnCancellation() {
        return false;
    }

    /**
     * Attempt to gracefully cleanup the transform so it can be terminated.
     * This tries to remove the job from the scheduler and completes the persistent task
     */
    @Override
    public void shutdown() {
        logger.debug("[{}] shutdown of transform requested", transform.getId());
        transformScheduler.deregisterTransform(getTransformId());
        markAsCompleted();
    }

    void persistStateToClusterState(TransformState state, ActionListener<PersistentTask<?>> listener) {
        updatePersistentTaskState(state, ActionListener.wrap(success -> {
            logger.debug("[{}] successfully updated state for transform to [{}].", transform.getId(), state.toString());
            listener.onResponse(success);
        }, failure -> {
            logger.error(() -> "[" + transform.getId() + "] failed to update cluster state for transform.", failure);
            listener.onFailure(failure);
        }));
    }

    @Override
    public void failureCountChanged() {
        transformScheduler.handleTransformFailureCountChanged(transform.getId(), context.getFailureCount());
    }

    @Override
    public void fail(Throwable exception, String reason, ActionListener<Void> listener) {
        synchronized (context) {
            if (context.getTaskState() == TransformTaskState.FAILED) {
                logger.warn("[{}] is already failed but encountered new failure; reason [{}].", getTransformId(), reason);
                listener.onResponse(null);
                return;
            }
            if (getIndexer() != null && getIndexer().getState() == IndexerState.STOPPING) {
                logger.info("[{}] attempt to fail transform with reason [{}] while it was stopping.", getTransformId(), reason);
                listener.onResponse(null);
                return;
            }
            if (getIndexer() != null && getIndexer().getState() == IndexerState.STOPPED) {
                logger.info("[{}] encountered a failure but indexer is STOPPED; reason [{}].", getTransformId(), reason);
                listener.onResponse(null);
                return;
            }

            logger.atError().withThrowable(exception).log("[{}] transform has failed; experienced: [{}].", transform.getId(), reason);
            auditor.error(transform.getId(), reason);
            transformScheduler.deregisterTransform(getTransformId());
            context.setShouldStopAtCheckpoint(false);

            context.setTaskStateToFailed(reason);
            TransformState newState = getState();
            persistStateToClusterState(newState, ActionListener.wrap(r -> listener.onResponse(null), e -> {
                String msg = "Failed to persist to cluster state while marking task as failed with reason [" + reason + "].";
                auditor.warning(transform.getId(), msg + " Failure: " + e.getMessage());
                logger.error(() -> format("[%s] %s", getTransformId(), msg), e);
                listener.onFailure(e);
            }));
        }
    }

    /**
     * This is called when the persistent task signals that the allocated task should be terminated.
     * Termination in the task framework is essentially voluntary, as the allocated task can only be
     * shut down from the inside.
     */
    @Override
    public void onCancelled() {
        logger.info("[{}] received cancellation request for transform, state: [{}].", getTransformId(), context.getTaskState());
        ClientTransformIndexer theIndexer = getIndexer();
        if (theIndexer != null && theIndexer.abort()) {
            shutdown();
        }
    }

    TransformTask setNumFailureRetries(int numFailureRetries) {
        context.setNumFailureRetries(numFailureRetries);
        return this;
    }

    TransformTask setAuthState(AuthorizationState authState) {
        context.setAuthState(authState);
        return this;
    }

    void initializeIndexer(ClientTransformIndexerBuilder indexerBuilder) {
        indexer.set(indexerBuilder.build(getThreadPool(), context));
    }

    ThreadPool getThreadPool() {
        return threadPool;
    }

    public static PersistentTask<?> getTransformTask(String transformId, ClusterState clusterState) {
        Collection<PersistentTask<?>> transformTasks = findTransformTasks(t -> t.getId().equals(transformId), clusterState);
        if (transformTasks.isEmpty()) {
            return null;
        }
        assert (transformTasks.size() == 1) : "There were 2 or more transform tasks with the same id";
        PersistentTask<?> pTask = transformTasks.iterator().next();
        if (pTask.getParams() instanceof TransformTaskParams) {
            return pTask;
        }
        throw new ElasticsearchStatusException(
            "Found transform persistent task [{}] with incorrect params",
            RestStatus.INTERNAL_SERVER_ERROR,
            transformId
        );
    }

    public static Collection<PersistentTask<?>> findAllTransformTasks(ClusterState clusterState) {
        return findTransformTasks(task -> true, clusterState);
    }

    public static Collection<PersistentTask<?>> findTransformTasks(Set<String> transformIds, ClusterState clusterState) {
        return findTransformTasks(task -> transformIds.contains(task.getId()), clusterState);
    }

    public static Collection<PersistentTask<?>> findTransformTasks(String transformIdPattern, ClusterState clusterState) {
        Predicate<PersistentTasksCustomMetadata.PersistentTask<?>> taskMatcher = transformIdPattern == null
            || Strings.isAllOrWildcard(transformIdPattern) ? t -> true : t -> {
                TransformTaskParams transformParams = (TransformTaskParams) t.getParams();
                return Regex.simpleMatch(transformIdPattern, transformParams.getId());
            };
        return findTransformTasks(taskMatcher, clusterState);
    }

    public TransformContext getContext() {
        return context;
    }

    private static Collection<PersistentTask<?>> findTransformTasks(Predicate<PersistentTask<?>> predicate, ClusterState clusterState) {
        PersistentTasksCustomMetadata pTasksMeta = PersistentTasksCustomMetadata.getPersistentTasksCustomMetadata(clusterState);
        if (pTasksMeta == null) {
            return Collections.emptyList();
        }
        return pTasksMeta.findTasks(TransformTaskParams.NAME, predicate);
    }
}
