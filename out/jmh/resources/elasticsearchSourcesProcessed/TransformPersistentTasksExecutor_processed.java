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
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.ParentTaskAssigningClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ValidationException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.persistent.AllocatedPersistentTask;
import org.elasticsearch.persistent.PersistentTaskState;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.persistent.PersistentTasksExecutor;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.core.indexing.IndexerState;
import org.elasticsearch.xpack.core.transform.TransformDeprecations;
import org.elasticsearch.xpack.core.transform.TransformField;
import org.elasticsearch.xpack.core.transform.TransformMessages;
import org.elasticsearch.xpack.core.transform.TransformMetadata;
import org.elasticsearch.xpack.core.transform.action.StartTransformAction;
import org.elasticsearch.xpack.core.transform.transforms.AuthorizationState;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformState;
import org.elasticsearch.xpack.core.transform.transforms.TransformStoredDoc;
import org.elasticsearch.xpack.core.transform.transforms.TransformTaskParams;
import org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants;
import org.elasticsearch.xpack.transform.Transform;
import org.elasticsearch.xpack.transform.TransformServices;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.persistence.SeqNoPrimaryTermAndIndex;
import org.elasticsearch.xpack.transform.persistence.TransformInternalIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.transform.transforms.TransformNodes.nodeCanRunThisTransform;

public class TransformPersistentTasksExecutor extends PersistentTasksExecutor<TransformTaskParams> {

    private static final Logger logger = LogManager.getLogger(TransformPersistentTasksExecutor.class);

    private static final int MARK_AS_FAILED_TIMEOUT_SEC = 90;
    private final Client client;
    private final TransformServices transformServices;
    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private final IndexNameExpressionResolver resolver;
    private final TransformAuditor auditor;
    private final Settings transformInternalIndexAdditionalSettings;
    private volatile int numFailureRetries;

    public TransformPersistentTasksExecutor(
        Client client,
        TransformServices transformServices,
        ThreadPool threadPool,
        ClusterService clusterService,
        Settings settings,
        Settings transformInternalIndexAdditionalSettings,
        IndexNameExpressionResolver resolver
    ) {
        super(TransformField.TASK_NAME, ThreadPool.Names.GENERIC);
        this.client = client;
        this.transformServices = transformServices;
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        this.resolver = resolver;
        this.auditor = transformServices.getAuditor();
        this.numFailureRetries = Transform.NUM_FAILURE_RETRIES_SETTING.get(settings);
        clusterService.getClusterSettings().addSettingsUpdateConsumer(Transform.NUM_FAILURE_RETRIES_SETTING, this::setNumFailureRetries);
        this.transformInternalIndexAdditionalSettings = transformInternalIndexAdditionalSettings;
    }

    @Override
    public PersistentTasksCustomMetadata.Assignment getAssignment(
        TransformTaskParams params,
        Collection<DiscoveryNode> candidateNodes,
        ClusterState clusterState
    ) {
        /* Note:
         *
         * This method is executed on the _master_ node. The master and transform node might be on a different version.
         * Therefore certain checks must happen on the corresponding node, e.g. the existence of the internal index.
         *
         * Operations on the transform node happen in {@link #nodeOperation()}
         */
        if (TransformMetadata.getTransformMetadata(clusterState).isResetMode()) {
            return new PersistentTasksCustomMetadata.Assignment(
                null,
                "Transform task will not be assigned as a feature reset is in progress."
            );
        }
        List<String> unavailableIndices = verifyIndicesPrimaryShardsAreActive(clusterState, resolver);
        if (unavailableIndices.size() != 0) {
            String reason = "Not starting transform ["
                + params.getId()
                + "], "
                + "because not all primary shards are active for the following indices ["
                + String.join(",", unavailableIndices)
                + "]";
            logger.debug(reason);
            return new PersistentTasksCustomMetadata.Assignment(null, reason);
        }
        DiscoveryNode discoveryNode = selectLeastLoadedNode(
            clusterState,
            candidateNodes,
            node -> nodeCanRunThisTransform(node, params.getVersion(), params.requiresRemote(), null)
        );

        if (discoveryNode == null) {
            Map<String, String> explainWhyAssignmentFailed = new TreeMap<>();
            for (DiscoveryNode node : clusterState.getNodes()) {
                nodeCanRunThisTransform(node, params.getVersion(), params.requiresRemote(), explainWhyAssignmentFailed);
            }
            String reason = "Not starting transform ["
                + params.getId()
                + "], reasons ["
                + explainWhyAssignmentFailed.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining("|"))
                + "]";

            logger.debug(reason);
            return new PersistentTasksCustomMetadata.Assignment(null, reason);
        }

        return new PersistentTasksCustomMetadata.Assignment(discoveryNode.getId(), "");
    }

    static List<String> verifyIndicesPrimaryShardsAreActive(ClusterState clusterState, IndexNameExpressionResolver resolver) {
        String[] indices = resolver.concreteIndexNames(
            clusterState,
            IndicesOptions.lenientExpandOpen(),
            TransformInternalIndexConstants.INDEX_NAME_PATTERN,
            TransformInternalIndexConstants.INDEX_NAME_PATTERN_DEPRECATED
        );
        List<String> unavailableIndices = new ArrayList<>(indices.length);
        for (String index : indices) {
            IndexRoutingTable routingTable = clusterState.getRoutingTable().index(index);
            if (routingTable == null
                || routingTable.allPrimaryShardsActive() == false
                || routingTable.readyForSearch(clusterState) == false) {
                unavailableIndices.add(index);
            }
        }
        return unavailableIndices;
    }

    @Override
    protected void nodeOperation(AllocatedPersistentTask task, @Nullable TransformTaskParams params, PersistentTaskState state) {
        /* Note:
         *
         * This method is executed on the _transform_ node. The master and transform node might be on a different version.
         * Operations on master happen in {@link #getAssignment()}
         */

        final String transformId = params.getId();
        final TransformTask buildTask = (TransformTask) task;
        final ParentTaskAssigningClient parentTaskClient = new ParentTaskAssigningClient(client, buildTask.getParentTaskId());
        final ClientTransformIndexerBuilder indexerBuilder = new ClientTransformIndexerBuilder().setClient(parentTaskClient)
            .setTransformServices(transformServices);

        final SetOnce<TransformState> stateHolder = new SetOnce<>();

        ActionListener<StartTransformAction.Response> startTaskListener = ActionListener.wrap(
            response -> logger.info("[{}] successfully completed and scheduled task in node operation", transformId),
            failure -> {
                auditor.error(
                    transformId,
                    "Failed to start transform. " + "Please stop and attempt to start again. Failure: " + failure.getMessage()
                );
                logger.error("Failed to start task [" + transformId + "] in node operation", failure);
            }
        );

        ActionListener<TransformCheckpoint> getTransformNextCheckpointListener = ActionListener.wrap(nextCheckpoint -> {

            if (nextCheckpoint.isEmpty()) {
                indexerBuilder.setInitialPosition(null);
                indexerBuilder.setProgress(null);
            } else {
                logger.trace("[{}] Loaded next checkpoint [{}] found, starting the task", transformId, nextCheckpoint.getCheckpoint());
                indexerBuilder.setNextCheckpoint(nextCheckpoint);
            }

            final long lastCheckpoint = stateHolder.get().getCheckpoint();
            final AuthorizationState authState = stateHolder.get().getAuthState();

            startTask(buildTask, indexerBuilder, authState, lastCheckpoint, startTaskListener);
        }, error -> {
            String msg = TransformMessages.getMessage(TransformMessages.FAILED_TO_LOAD_TRANSFORM_CHECKPOINT, transformId);
            logger.error(msg, error);
            markAsFailed(buildTask, error, msg);
        });

        ActionListener<TransformCheckpoint> getTransformLastCheckpointListener = ActionListener.wrap(lastCheckpoint -> {

            indexerBuilder.setLastCheckpoint(lastCheckpoint);
            logger.trace("[{}] Loaded last checkpoint [{}], looking for next checkpoint", transformId, lastCheckpoint.getCheckpoint());
            transformServices.getConfigManager()
                .getTransformCheckpoint(transformId, lastCheckpoint.getCheckpoint() + 1, getTransformNextCheckpointListener);
        }, error -> {
            String msg = TransformMessages.getMessage(TransformMessages.FAILED_TO_LOAD_TRANSFORM_CHECKPOINT, transformId);
            logger.error(msg, error);
            markAsFailed(buildTask, error, msg);
        });

        ActionListener<Tuple<TransformStoredDoc, SeqNoPrimaryTermAndIndex>> transformStatsActionListener = ActionListener.wrap(
            stateAndStatsAndSeqNoPrimaryTermAndIndex -> {

                TransformStoredDoc stateAndStats = stateAndStatsAndSeqNoPrimaryTermAndIndex.v1();
                SeqNoPrimaryTermAndIndex seqNoPrimaryTermAndIndex = stateAndStatsAndSeqNoPrimaryTermAndIndex.v2();
                logger.trace("[{}] initializing state and stats: [{}]", transformId, stateAndStats.toString());
                TransformState transformState = stateAndStats.getTransformState();
                indexerBuilder.setInitialStats(stateAndStats.getTransformStats())
                    .setInitialPosition(stateAndStats.getTransformState().getPosition())
                    .setProgress(stateAndStats.getTransformState().getProgress())
                    .setIndexerState(currentIndexerState(transformState))
                    .setSeqNoPrimaryTermAndIndex(seqNoPrimaryTermAndIndex)
                    .setShouldStopAtCheckpoint(transformState.shouldStopAtNextCheckpoint());
                logger.debug(
                    "[{}] Loading existing state: [{}], position [{}]",
                    transformId,
                    stateAndStats.getTransformState(),
                    stateAndStats.getTransformState().getPosition()
                );

                stateHolder.set(transformState);
                final long lastCheckpoint = stateHolder.get().getCheckpoint();

                if (lastCheckpoint == 0) {
                    logger.trace("[{}] No last checkpoint found, looking for next checkpoint", transformId);
                    transformServices.getConfigManager()
                        .getTransformCheckpoint(transformId, lastCheckpoint + 1, getTransformNextCheckpointListener);
                } else {
                    logger.trace("[{}] Restore last checkpoint: [{}]", transformId, lastCheckpoint);
                    transformServices.getConfigManager()
                        .getTransformCheckpoint(transformId, lastCheckpoint, getTransformLastCheckpointListener);
                }
            },
            error -> {
                if (error instanceof ResourceNotFoundException == false) {
                    String msg = TransformMessages.getMessage(TransformMessages.FAILED_TO_LOAD_TRANSFORM_STATE, transformId);
                    logger.error(msg, error);
                    markAsFailed(buildTask, error, msg);
                } else {
                    logger.trace("[{}] No stats found (new transform), starting the task", transformId);
                    startTask(buildTask, indexerBuilder, null, null, startTaskListener);
                }
            }
        );

        ActionListener<TransformConfig> getTransformConfigListener = ActionListener.wrap(config -> {

            if (config.getVersion() == null || config.getVersion().before(TransformDeprecations.MIN_TRANSFORM_VERSION)) {
                String transformTooOldError = format(
                    "Transform configuration is too old [%s], use the upgrade API to fix your transform. "
                        + "Minimum required version is [%s]",
                    config.getVersion(),
                    TransformDeprecations.MIN_TRANSFORM_VERSION
                );
                auditor.error(transformId, transformTooOldError);
                markAsFailed(buildTask, null, transformTooOldError);
                return;
            }

            ValidationException validationException = config.validate(null);
            if (validationException == null) {
                indexerBuilder.setTransformConfig(config);
                transformServices.getConfigManager().getTransformStoredDoc(transformId, false, transformStatsActionListener);
            } else {
                auditor.error(transformId, validationException.getMessage());
                markAsFailed(
                    buildTask,
                    validationException,
                    TransformMessages.getMessage(
                        TransformMessages.TRANSFORM_CONFIGURATION_INVALID,
                        transformId,
                        validationException.getMessage()
                    )
                );
            }
        }, error -> {
            String msg = TransformMessages.getMessage(TransformMessages.FAILED_TO_LOAD_TRANSFORM_CONFIGURATION, transformId);
            markAsFailed(buildTask, error, msg);
        });

        ActionListener<Void> templateCheckListener = ActionListener.wrap(
            aVoid -> transformServices.getConfigManager().getTransformConfiguration(transformId, getTransformConfigListener),
            error -> {
                Throwable cause = ExceptionsHelper.unwrapCause(error);
                String msg = "Failed to create internal index mappings";
                markAsFailed(buildTask, error, msg + "[" + cause + "]");
            }
        );

        TransformInternalIndex.createLatestVersionedIndexIfRequired(
            clusterService,
            parentTaskClient,
            transformInternalIndexAdditionalSettings,
            templateCheckListener
        );
    }

    private static IndexerState currentIndexerState(TransformState previousState) {
        if (previousState == null) {
            return IndexerState.STOPPED;
        }
        return switch (previousState.getIndexerState()) {
            case STARTED, INDEXING -> IndexerState.STARTED;
            case STOPPED, STOPPING, ABORTING -> IndexerState.STOPPED;
        };
    }

    private static void markAsFailed(TransformTask task, Throwable exception, String reason) {
        CountDownLatch latch = new CountDownLatch(1);

        task.fail(
            exception,
            reason,
            new LatchedActionListener<>(
                ActionListener.wrap(
                    nil -> {},
                    failure -> logger.error("Failed to set task [" + task.getTransformId() + "] to failed", failure)
                ),
                latch
            )
        );
        try {
            latch.await(MARK_AS_FAILED_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Timeout waiting for task [" + task.getTransformId() + "] to be marked as failed in cluster state", e);
        }
    }

    private void startTask(
        TransformTask buildTask,
        ClientTransformIndexerBuilder indexerBuilder,
        AuthorizationState authState,
        Long previousCheckpoint,
        ActionListener<StartTransformAction.Response> listener
    ) {
        threadPool.generic().execute(() -> {
            buildTask.initializeIndexer(indexerBuilder);
            buildTask.setAuthState(authState);
            buildTask.setNumFailureRetries(numFailureRetries).start(previousCheckpoint, listener);
        });
    }

    private void setNumFailureRetries(int numFailureRetries) {
        this.numFailureRetries = numFailureRetries;
    }

    @Override
    protected AllocatedPersistentTask createTask(
        long id,
        String type,
        String action,
        TaskId parentTaskId,
        PersistentTasksCustomMetadata.PersistentTask<TransformTaskParams> persistentTask,
        Map<String, String> headers
    ) {
        return new TransformTask(
            id,
            type,
            action,
            parentTaskId,
            persistentTask.getParams(),
            (TransformState) persistentTask.getState(),
            transformServices.getScheduler(),
            auditor,
            threadPool,
            headers
        );
    }
}
