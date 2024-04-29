/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionListenerResponseHandler;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.TaskOperationFailure;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.tasks.TransportTasksAction;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.OriginSettingClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.elasticsearch.persistent.PersistentTasksClusterService;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.persistent.PersistentTasksService;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.ml.MlTasks;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.StopDatafeedAction;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.core.ml.job.messages.Messages;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.datafeed.persistence.DatafeedConfigProvider;
import org.elasticsearch.xpack.ml.notifications.AnomalyDetectionAuditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.ml.utils.ExceptionCollectionHandling.exceptionArrayToStatusException;

public class TransportStopDatafeedAction extends TransportTasksAction<
    TransportStartDatafeedAction.DatafeedTask,
    StopDatafeedAction.Request,
    StopDatafeedAction.Response,
    StopDatafeedAction.Response> {

    private static final int MAX_ATTEMPTS = 10;

    private static final Logger logger = LogManager.getLogger(TransportStopDatafeedAction.class);

    private final ThreadPool threadPool;
    private final PersistentTasksService persistentTasksService;
    private final DatafeedConfigProvider datafeedConfigProvider;
    private final AnomalyDetectionAuditor auditor;
    private final OriginSettingClient client;

    @Inject
    public TransportStopDatafeedAction(
        TransportService transportService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        ClusterService clusterService,
        PersistentTasksService persistentTasksService,
        DatafeedConfigProvider datafeedConfigProvider,
        AnomalyDetectionAuditor auditor,
        Client client
    ) {
        super(
            StopDatafeedAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            StopDatafeedAction.Request::new,
            StopDatafeedAction.Response::new,
            threadPool.executor(MachineLearning.UTILITY_THREAD_POOL_NAME)
        );
        this.threadPool = Objects.requireNonNull(threadPool);
        this.persistentTasksService = Objects.requireNonNull(persistentTasksService);
        this.datafeedConfigProvider = Objects.requireNonNull(datafeedConfigProvider);
        this.auditor = Objects.requireNonNull(auditor);
        this.client = new OriginSettingClient(client, ML_ORIGIN);
    }

    /**
     * Sort the datafeed IDs the their task state and add to one
     * of the list arguments depending on the state.
     *
     * @param expandedDatafeedIds The expanded set of IDs
     * @param tasks Persistent task meta data
     * @param startedDatafeedIds Started datafeed ids are added to this list
     * @param stoppingDatafeedIds Stopping datafeed ids are added to this list
     * @param notStoppedDatafeedIds Datafeed ids are added to this list for all datafeeds that are not stopped
     */
    static void sortDatafeedIdsByTaskState(
        Collection<String> expandedDatafeedIds,
        PersistentTasksCustomMetadata tasks,
        List<String> startedDatafeedIds,
        List<String> stoppingDatafeedIds,
        List<String> notStoppedDatafeedIds
    ) {

        for (String expandedDatafeedId : expandedDatafeedIds) {
            addDatafeedTaskIdAccordingToState(
                expandedDatafeedId,
                MlTasks.getDatafeedState(expandedDatafeedId, tasks),
                startedDatafeedIds,
                stoppingDatafeedIds,
                notStoppedDatafeedIds
            );
        }
    }

    private static void addDatafeedTaskIdAccordingToState(
        String datafeedId,
        DatafeedState datafeedState,
        List<String> startedDatafeedIds,
        List<String> stoppingDatafeedIds,
        List<String> notStoppedDatafeedIds
    ) {
        switch (datafeedState) {
            case STARTING:
            case STARTED:
                startedDatafeedIds.add(datafeedId);
                notStoppedDatafeedIds.add(datafeedId);
                break;
            case STOPPED:
                break;
            case STOPPING:
                stoppingDatafeedIds.add(datafeedId);
                notStoppedDatafeedIds.add(datafeedId);
                break;
            default:
                assert false : "Unexpected datafeed state " + datafeedState;
                break;
        }
    }

    @Override
    protected void doExecute(Task task, StopDatafeedAction.Request request, ActionListener<StopDatafeedAction.Response> listener) {
        doExecute(task, request, listener, 1);
    }

    private void doExecute(
        Task task,
        StopDatafeedAction.Request request,
        ActionListener<StopDatafeedAction.Response> listener,
        int attempt
    ) {
        final ClusterState state = clusterService.state();
        final DiscoveryNodes nodes = state.nodes();
        if (nodes.isLocalNodeElectedMaster() == false) {
            if (nodes.getMasterNode() == null) {
                listener.onFailure(new MasterNotDiscoveredException());
            } else {
                transportService.sendRequest(
                    nodes.getMasterNode(),
                    actionName,
                    request,
                    new ActionListenerResponseHandler<>(
                        listener,
                        StopDatafeedAction.Response::new,
                        TransportResponseHandler.TRANSPORT_WORKER
                    )
                );
            }
        } else {
            PersistentTasksCustomMetadata tasks = state.getMetadata().custom(PersistentTasksCustomMetadata.TYPE);
            datafeedConfigProvider.expandDatafeedIds(
                request.getDatafeedId(),
                request.allowNoMatch(),
                tasks,
                request.isForce(),
                null,
                ActionListener.wrap(expandedIds -> {
                    List<String> startedDatafeeds = new ArrayList<>();
                    List<String> stoppingDatafeeds = new ArrayList<>();
                    List<String> notStoppedDatafeeds = new ArrayList<>();
                    sortDatafeedIdsByTaskState(expandedIds, tasks, startedDatafeeds, stoppingDatafeeds, notStoppedDatafeeds);
                    if (startedDatafeeds.isEmpty() && stoppingDatafeeds.isEmpty()) {
                        listener.onResponse(new StopDatafeedAction.Response(true));
                        return;
                    }

                    if (request.isForce()) {
                        forceStopDatafeed(request, listener, tasks, nodes, notStoppedDatafeeds);
                    } else {
                        normalStopDatafeed(task, request, listener, tasks, nodes, startedDatafeeds, stoppingDatafeeds, attempt);
                    }
                }, listener::onFailure)
            );
        }
    }

    private void normalStopDatafeed(
        Task task,
        StopDatafeedAction.Request request,
        ActionListener<StopDatafeedAction.Response> listener,
        PersistentTasksCustomMetadata tasks,
        DiscoveryNodes nodes,
        List<String> startedDatafeeds,
        List<String> stoppingDatafeeds,
        int attempt
    ) {
        final Set<String> executorNodes = new HashSet<>();
        final List<String> startedDatafeedsJobs = new ArrayList<>();
        final List<String> resolvedStartedDatafeeds = new ArrayList<>();
        final List<PersistentTasksCustomMetadata.PersistentTask<?>> allDataFeedsToWaitFor = new ArrayList<>();
        for (String datafeedId : startedDatafeeds) {
            PersistentTasksCustomMetadata.PersistentTask<?> datafeedTask = MlTasks.getDatafeedTask(datafeedId, tasks);
            if (datafeedTask == null) {
                String msg = "Requested datafeed [" + datafeedId + "] be stopped, but datafeed's task could not be found.";
                assert datafeedTask != null : msg;
                logger.error(msg);
            } else if (PersistentTasksClusterService.needsReassignment(datafeedTask.getAssignment(), nodes) == false) {
                startedDatafeedsJobs.add(((StartDatafeedAction.DatafeedParams) datafeedTask.getParams()).getJobId());
                resolvedStartedDatafeeds.add(datafeedId);
                executorNodes.add(datafeedTask.getExecutorNode());
                allDataFeedsToWaitFor.add(datafeedTask);
            } else {
                persistentTasksService.sendRemoveRequest(
                    datafeedTask.getId(),
                    null,
                    ActionListener.wrap(
                        r -> auditDatafeedStopped(datafeedTask),
                        e -> logger.error("[" + datafeedId + "] failed to remove task to stop unassigned datafeed", e)
                    )
                );
                allDataFeedsToWaitFor.add(datafeedTask);
            }
        }

        for (String datafeedId : stoppingDatafeeds) {
            PersistentTasksCustomMetadata.PersistentTask<?> datafeedTask = MlTasks.getDatafeedTask(datafeedId, tasks);
            assert datafeedTask != null : "Requested datafeed [" + datafeedId + "] be stopped, but datafeed's task could not be found.";
            allDataFeedsToWaitFor.add(datafeedTask);
        }

        request.setResolvedStartedDatafeedIds(resolvedStartedDatafeeds.toArray(new String[0]));
        request.setNodes(executorNodes.toArray(new String[0]));

        final Set<String> movedDatafeeds = ConcurrentCollections.newConcurrentSet();

        ActionListener<StopDatafeedAction.Response> finalListener = ActionListener.wrap(
            response -> waitForDatafeedStopped(allDataFeedsToWaitFor, request, response, ActionListener.wrap(finished -> {
                for (String datafeedId : movedDatafeeds) {
                    PersistentTasksCustomMetadata.PersistentTask<?> datafeedTask = MlTasks.getDatafeedTask(datafeedId, tasks);
                    persistentTasksService.sendRemoveRequest(
                        datafeedTask.getId(),
                        null,
                        ActionListener.wrap(r -> auditDatafeedStopped(datafeedTask), e -> {
                            if (ExceptionsHelper.unwrapCause(e) instanceof ResourceNotFoundException) {
                                logger.debug("[{}] relocated datafeed task already removed", datafeedId);
                            } else {
                                logger.error("[" + datafeedId + "] failed to remove task to stop relocated datafeed", e);
                            }
                        })
                    );
                }
                if (startedDatafeedsJobs.isEmpty()) {
                    listener.onResponse(finished);
                    return;
                }
                client.admin()
                    .indices()
                    .prepareRefresh(startedDatafeedsJobs.stream().map(AnomalyDetectorsIndex::jobResultsAliasedName).toArray(String[]::new))
                    .execute(ActionListener.wrap(_unused -> listener.onResponse(finished), ex -> {
                        logger.warn(
                            () -> format(
                                "failed to refresh job [%s] results indices when stopping datafeeds [%s]",
                                startedDatafeedsJobs,
                                startedDatafeeds
                            ),
                            ex
                        );
                        listener.onResponse(finished);
                    }));
            }, listener::onFailure), movedDatafeeds),
            e -> {
                Throwable unwrapped = ExceptionsHelper.unwrapCause(e);
                if (unwrapped instanceof FailedNodeException) {
                    if (attempt <= MAX_ATTEMPTS) {
                        logger.warn(
                            "Node [{}] failed while processing stop datafeed request - retrying",
                            ((FailedNodeException) unwrapped).nodeId()
                        );
                        doExecute(task, request, listener, attempt + 1);
                    } else {
                        listener.onFailure(e);
                    }
                } else if (unwrapped instanceof RetryStopDatafeedException) {
                    if (attempt <= MAX_ATTEMPTS) {
                        logger.info(
                            "Insufficient responses while processing stop datafeed request [{}] - retrying",
                            unwrapped.getMessage()
                        );
                        threadPool.schedule(
                            () -> doExecute(task, request, listener, attempt + 1),
                            TimeValue.timeValueMillis(100L * attempt),
                            EsExecutors.DIRECT_EXECUTOR_SERVICE
                        );
                    } else {
                        listener.onFailure(
                            ExceptionsHelper.serverError(
                                "Failed to stop datafeed ["
                                    + request.getDatafeedId()
                                    + "] after "
                                    + MAX_ATTEMPTS
                                    + " due to inconsistencies between local and persistent tasks within the cluster"
                            )
                        );
                    }
                } else {
                    listener.onFailure(e);
                }
            }
        );

        super.doExecute(task, request, finalListener);
    }

    private void auditDatafeedStopped(PersistentTasksCustomMetadata.PersistentTask<?> datafeedTask) {
        @SuppressWarnings("unchecked")
        String jobId = ((PersistentTasksCustomMetadata.PersistentTask<StartDatafeedAction.DatafeedParams>) datafeedTask).getParams()
            .getJobId();
        auditor.info(jobId, Messages.getMessage(Messages.JOB_AUDIT_DATAFEED_STOPPED));
    }

    private void forceStopDatafeed(
        final StopDatafeedAction.Request request,
        final ActionListener<StopDatafeedAction.Response> listener,
        PersistentTasksCustomMetadata tasks,
        DiscoveryNodes nodes,
        final List<String> notStoppedDatafeeds
    ) {
        final AtomicInteger counter = new AtomicInteger();
        final AtomicArray<Exception> failures = new AtomicArray<>(notStoppedDatafeeds.size());

        for (String datafeedId : notStoppedDatafeeds) {
            PersistentTasksCustomMetadata.PersistentTask<?> datafeedTask = MlTasks.getDatafeedTask(datafeedId, tasks);
            if (datafeedTask != null) {
                persistentTasksService.sendRemoveRequest(datafeedTask.getId(), null, ActionListener.wrap(persistentTask -> {
                    if (PersistentTasksClusterService.needsReassignment(datafeedTask.getAssignment(), nodes)) {
                        auditDatafeedStopped(datafeedTask);
                    }
                    if (counter.incrementAndGet() == notStoppedDatafeeds.size()) {
                        sendResponseOrFailure(request.getDatafeedId(), listener, failures);
                    }
                }, e -> {
                    final int slot = counter.incrementAndGet();
                    if (ExceptionsHelper.unwrapCause(e) instanceof ResourceNotFoundException == false) {
                        failures.set(slot - 1, e);
                    }
                    if (slot == notStoppedDatafeeds.size()) {
                        sendResponseOrFailure(request.getDatafeedId(), listener, failures);
                    }
                }));
            } else {
                String msg = "Requested datafeed [" + datafeedId + "] be force-stopped, but datafeed's task could not be found.";
                assert datafeedTask != null : msg;
                logger.error(msg);
                final int slot = counter.incrementAndGet();
                failures.set(slot - 1, new RuntimeException(msg));
                if (slot == notStoppedDatafeeds.size()) {
                    sendResponseOrFailure(request.getDatafeedId(), listener, failures);
                }
            }
        }
    }

    @Override
    protected void taskOperation(
        CancellableTask actionTask,
        StopDatafeedAction.Request request,
        TransportStartDatafeedAction.DatafeedTask datafeedTask,
        ActionListener<StopDatafeedAction.Response> listener
    ) {
        DatafeedState taskState = DatafeedState.STOPPING;
        datafeedTask.updatePersistentTaskState(taskState, ActionListener.wrap(task -> {
            threadPool.executor(MachineLearning.UTILITY_THREAD_POOL_NAME).execute(new AbstractRunnable() {
                @Override
                public void onFailure(Exception e) {
                    if (ExceptionsHelper.unwrapCause(e) instanceof ResourceNotFoundException) {
                        listener.onResponse(new StopDatafeedAction.Response(true));
                    } else {
                        listener.onFailure(e);
                    }
                }

                @Override
                protected void doRun() {
                    datafeedTask.stop("stop_datafeed (api)", request.getStopTimeout());
                    listener.onResponse(new StopDatafeedAction.Response(true));
                }
            });
        }, e -> {
            if (ExceptionsHelper.unwrapCause(e) instanceof ResourceNotFoundException) {
                listener.onResponse(new StopDatafeedAction.Response(true));
            } else {
                listener.onFailure(e);
            }
        }));
    }

    private static void sendResponseOrFailure(
        String datafeedId,
        ActionListener<StopDatafeedAction.Response> listener,
        AtomicArray<Exception> failures
    ) {
        List<Exception> caughtExceptions = failures.asList();
        if (caughtExceptions.isEmpty()) {
            listener.onResponse(new StopDatafeedAction.Response(true));
            return;
        }

        String msg = "Failed to stop datafeed ["
            + datafeedId
            + "] with ["
            + caughtExceptions.size()
            + "] failures, rethrowing first. All Exceptions: ["
            + caughtExceptions.stream().map(Exception::getMessage).collect(Collectors.joining(", "))
            + "]";

        ElasticsearchStatusException e = exceptionArrayToStatusException(failures, msg);
        listener.onFailure(e);
    }

    /**
     * Wait for datafeed to be marked as stopped in cluster state, which means the datafeed persistent task has been removed.
     * This api returns when task has been cancelled, but that doesn't mean the persistent task has been removed from cluster state,
     * so wait for that to happen here.
     *
     * Since the stop datafeed action consists of a chain of async callbacks, it's possible that datafeeds have moved nodes since we
     * decided what to do with them at the beginning of the chain.  We cannot simply wait for these, as the request to stop them will
     * have been sent to the wrong node and ignored there, so we'll just spin until the timeout expires.
     */
    void waitForDatafeedStopped(
        List<PersistentTasksCustomMetadata.PersistentTask<?>> datafeedPersistentTasks,
        StopDatafeedAction.Request request,
        StopDatafeedAction.Response response,
        ActionListener<StopDatafeedAction.Response> listener,
        Set<String> movedDatafeeds
    ) {
        persistentTasksService.waitForPersistentTasksCondition(persistentTasksCustomMetadata -> {
            for (PersistentTasksCustomMetadata.PersistentTask<?> originalPersistentTask : datafeedPersistentTasks) {
                String originalPersistentTaskId = originalPersistentTask.getId();
                PersistentTasksCustomMetadata.PersistentTask<?> currentPersistentTask = persistentTasksCustomMetadata.getTask(
                    originalPersistentTaskId
                );
                if (currentPersistentTask != null) {
                    if (Objects.equals(originalPersistentTask.getExecutorNode(), currentPersistentTask.getExecutorNode())
                        && originalPersistentTask.getAllocationId() == currentPersistentTask.getAllocationId()) {
                        return false;
                    }
                    StartDatafeedAction.DatafeedParams params = (StartDatafeedAction.DatafeedParams) originalPersistentTask.getParams();
                    if (movedDatafeeds.add(params.getDatafeedId())) {
                        logger.info("Datafeed [{}] changed assignment while waiting for it to be stopped", params.getDatafeedId());
                    }
                }
            }
            return true;
        }, request.getTimeout(), listener.safeMap(result -> response));
    }

    @Override
    protected StopDatafeedAction.Response newResponse(
        StopDatafeedAction.Request request,
        List<StopDatafeedAction.Response> tasks,
        List<TaskOperationFailure> taskOperationFailures,
        List<FailedNodeException> failedNodeExceptions
    ) {
        if (request.getResolvedStartedDatafeedIds().length != tasks.size()) {
            if (taskOperationFailures.isEmpty() == false) {
                throw ExceptionsHelper.taskOperationFailureToStatusException(taskOperationFailures.get(0));
            } else if (failedNodeExceptions.isEmpty() == false) {
                throw failedNodeExceptions.get(0);
            } else {
                throw new RetryStopDatafeedException(request.getResolvedStartedDatafeedIds().length, tasks.size());
            }
        }

        return new StopDatafeedAction.Response(tasks.stream().allMatch(StopDatafeedAction.Response::isStopped));
    }

    /**
     * A special exception to indicate that we should retry stopping the datafeeds.
     * This exception is not transportable, so should only be thrown in situations
     * where it will be caught on the same node.
     */
    static class RetryStopDatafeedException extends RuntimeException {

        RetryStopDatafeedException(int numResponsesExpected, int numResponsesReceived) {
            super("expected " + numResponsesExpected + " responses, got " + numResponsesReceived);
        }
    }
}
