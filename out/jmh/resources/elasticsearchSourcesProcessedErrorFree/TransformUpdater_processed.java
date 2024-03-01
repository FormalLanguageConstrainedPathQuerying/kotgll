/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.transform.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.xpack.core.ClientHelper;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.security.SecurityContext;
import org.elasticsearch.xpack.core.transform.action.ValidateTransformAction;
import org.elasticsearch.xpack.core.transform.transforms.AuthorizationState;
import org.elasticsearch.xpack.core.transform.transforms.TransformCheckpoint;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfig;
import org.elasticsearch.xpack.core.transform.transforms.TransformConfigUpdate;
import org.elasticsearch.xpack.core.transform.transforms.TransformStoredDoc;
import org.elasticsearch.xpack.core.transform.transforms.persistence.TransformInternalIndexConstants;
import org.elasticsearch.xpack.transform.notifications.TransformAuditor;
import org.elasticsearch.xpack.transform.persistence.SeqNoPrimaryTermAndIndex;
import org.elasticsearch.xpack.transform.persistence.TransformConfigManager;
import org.elasticsearch.xpack.transform.persistence.TransformIndex;

import java.util.Map;

/**
 * With {@link TransformUpdater} transforms can be updated or upgraded to the latest version
 *
 * This implementation is shared between _update and _upgrade
 */
public class TransformUpdater {

    private static final Logger logger = LogManager.getLogger(TransformUpdater.class);

    public static final class UpdateResult {

        public enum Status {
            NONE, 
            UPDATED, 
            NEEDS_UPDATE, 
            DELETED 
        }

        @Nullable
        private final TransformConfig config;

        @Nullable
        private final AuthorizationState authState;

        private final Status status;

        UpdateResult(final TransformConfig config, final AuthorizationState authState, final Status status) {
            this.config = config;
            this.authState = authState;
            this.status = status;
        }

        @Nullable
        public TransformConfig getConfig() {
            return config;
        }

        @Nullable
        public AuthorizationState getAuthState() {
            return authState;
        }

        public Status getStatus() {
            return status;
        }
    }

    /**
     * Update a single transform given a config and update
     *
     * In addition to applying update to the config, old versions of {@link TransformConfig}, {@link TransformStoredDoc} and
     * {@link TransformCheckpoint} are rewritten into the latest format and written back using {@link TransformConfigManager}
     *
     * @param securityContext the security context
     * @param indexNameExpressionResolver index name expression resolver
     * @param clusterState the current cluster state
     * @param settings settings
     * @param client a client
     * @param transformConfigManager the transform configuration manager
     * @param config the old configuration to update
     * @param update the update to apply to the configuration
     * @param seqNoPrimaryTermAndIndex sequence id and primary term of the configuration
     * @param deferValidation whether to defer some validation checks
     * @param dryRun whether to actually write the configuration back or whether to just check for updates
     * @param checkAccess whether to run access checks
     * @param listener the listener called containing the result of the update
     */

    public static void updateTransform(
        SecurityContext securityContext,
        IndexNameExpressionResolver indexNameExpressionResolver,
        ClusterState clusterState,
        Settings settings,
        Client client,
        TransformConfigManager transformConfigManager,
        TransformAuditor auditor,
        final TransformConfig config,
        final TransformConfigUpdate update,
        final SeqNoPrimaryTermAndIndex seqNoPrimaryTermAndIndex,
        final boolean deferValidation,
        final boolean dryRun,
        final boolean checkAccess,
        final TimeValue timeout,
        final Settings destIndexSettings,
        ActionListener<UpdateResult> listener
    ) {
        final TransformConfig rewrittenConfig = TransformConfig.rewriteForUpdate(config);
        final TransformConfig updatedConfig = update != null ? update.apply(rewrittenConfig) : rewrittenConfig;
        final SetOnce<AuthorizationState> authStateHolder = new SetOnce<>();

        ActionListener<Long> updateStateListener = ActionListener.wrap(lastCheckpoint -> {
            if (lastCheckpoint == null || lastCheckpoint == -1) {
                listener.onResponse(new UpdateResult(updatedConfig, authStateHolder.get(), UpdateResult.Status.UPDATED));
                return;
            }

            updateTransformCheckpoint(
                config.getId(),
                lastCheckpoint,
                transformConfigManager,
                ActionListener.wrap(
                    r -> listener.onResponse(new UpdateResult(updatedConfig, authStateHolder.get(), UpdateResult.Status.UPDATED)),
                    listener::onFailure
                )
            );
        }, listener::onFailure);

        ActionListener<Void> updateTransformListener = ActionListener.wrap(
            r -> updateTransformStateAndGetLastCheckpoint(config.getId(), transformConfigManager, updateStateListener),
            listener::onFailure
        );

        ActionListener<Map<String, String>> validateTransformListener = ActionListener.wrap(destIndexMappings -> {
            if (config.getVersion() != null
                && config.getVersion().onOrAfter(TransformInternalIndexConstants.INDEX_VERSION_LAST_CHANGED)
                && updatedConfig.equals(config)) {
                listener.onResponse(new UpdateResult(updatedConfig, authStateHolder.get(), UpdateResult.Status.NONE));
                return;
            }

            if (dryRun) {
                listener.onResponse(new UpdateResult(updatedConfig, authStateHolder.get(), UpdateResult.Status.NEEDS_UPDATE));
                return;
            }

            updateTransformConfiguration(
                client,
                transformConfigManager,
                auditor,
                indexNameExpressionResolver,
                updatedConfig,
                destIndexMappings,
                seqNoPrimaryTermAndIndex,
                clusterState,
                destIndexSettings,
                ActionListener.wrap(r -> updateTransformListener.onResponse(null), listener::onFailure)
            );
        }, listener::onFailure);

        ActionListener<AuthorizationState> checkPrivilegesListener = ActionListener.wrap(authState -> {
            authStateHolder.set(authState);
            validateTransform(updatedConfig, client, deferValidation, timeout, validateTransformListener);
        }, listener::onFailure);

        if (checkAccess && XPackSettings.SECURITY_ENABLED.get(settings)) {
            TransformPrivilegeChecker.checkPrivileges(
                "update",
                settings,
                securityContext,
                indexNameExpressionResolver,
                clusterState,
                client,
                updatedConfig,
                true,
                ActionListener.wrap(aVoid -> checkPrivilegesListener.onResponse(AuthorizationState.green()), e -> {
                    if (deferValidation) {
                        checkPrivilegesListener.onResponse(AuthorizationState.red(e));
                    } else {
                        checkPrivilegesListener.onFailure(e);
                    }
                })
            );
        } else { 
            checkPrivilegesListener.onResponse(null);
        }
    }

    private static void validateTransform(
        TransformConfig config,
        Client client,
        boolean deferValidation,
        TimeValue timeout,
        ActionListener<Map<String, String>> listener
    ) {
        ClientHelper.executeAsyncWithOrigin(
            client,
            ClientHelper.TRANSFORM_ORIGIN,
            ValidateTransformAction.INSTANCE,
            new ValidateTransformAction.Request(config, deferValidation, timeout),
            ActionListener.wrap(response -> listener.onResponse(response.getDestIndexMappings()), listener::onFailure)
        );
    }

    private static void updateTransformStateAndGetLastCheckpoint(
        String transformId,
        TransformConfigManager transformConfigManager,
        ActionListener<Long> listener
    ) {
        transformConfigManager.getTransformStoredDoc(transformId, true, ActionListener.wrap(currentState -> {
            if (currentState == null) {
                listener.onResponse(-1L);
                return;
            }

            long lastCheckpoint = currentState.v1().getTransformState().getCheckpoint();

            if (currentState.v2().getIndex().equals(TransformInternalIndexConstants.LATEST_INDEX_VERSIONED_NAME)) {
                listener.onResponse(lastCheckpoint);
                return;
            }

            transformConfigManager.putOrUpdateTransformStoredDoc(
                currentState.v1(),
                null, 
                ActionListener.wrap(r -> listener.onResponse(lastCheckpoint), e -> {
                    if (org.elasticsearch.ExceptionsHelper.unwrapCause(e) instanceof VersionConflictEngineException) {
                        logger.trace("[{}] could not update transform state during update due to running transform", transformId);
                        listener.onResponse(lastCheckpoint);
                    } else {
                        logger.warn("[{}] failed to persist transform state during update.", transformId);
                        listener.onFailure(e);
                    }
                })
            );
        }, listener::onFailure));
    }

    private static void updateTransformCheckpoint(
        String transformId,
        long lastCheckpoint,
        TransformConfigManager transformConfigManager,
        ActionListener<Boolean> listener
    ) {
        transformConfigManager.getTransformCheckpointForUpdate(transformId, lastCheckpoint, ActionListener.wrap(checkpointAndVersion -> {
            if (checkpointAndVersion == null
                || checkpointAndVersion.v2().getIndex().equals(TransformInternalIndexConstants.LATEST_INDEX_VERSIONED_NAME)) {
                listener.onResponse(true);
                return;
            }

            transformConfigManager.putTransformCheckpoint(checkpointAndVersion.v1(), listener);
        }, listener::onFailure));
    }

    private static void updateTransformConfiguration(
        Client client,
        TransformConfigManager transformConfigManager,
        TransformAuditor auditor,
        IndexNameExpressionResolver indexNameExpressionResolver,
        TransformConfig config,
        Map<String, String> destIndexMappings,
        SeqNoPrimaryTermAndIndex seqNoPrimaryTermAndIndex,
        ClusterState clusterState,
        Settings destIndexSettings,
        ActionListener<Void> listener
    ) {
        ActionListener<Boolean> putTransformConfigurationListener = ActionListener.wrap(
            putTransformConfigurationResult -> transformConfigManager.deleteOldTransformConfigurations(
                config.getId(),
                ActionListener.wrap(r -> {
                    logger.trace("[{}] successfully deleted old transform configurations", config.getId());
                    listener.onResponse(null);
                }, e -> {
                    logger.warn(LoggerMessageFormat.format("[{}] failed deleting old transform configurations.", config.getId()), e);
                    listener.onResponse(null);
                })
            ),
            listener::onFailure
        );

        ActionListener<Boolean> createDestinationListener = ActionListener.wrap(
            createDestResponse -> transformConfigManager.updateTransformConfiguration(
                config,
                seqNoPrimaryTermAndIndex,
                putTransformConfigurationListener
            ),
            listener::onFailure
        );

        final String destinationIndex = config.getDestination().getIndex();
        String[] dest = indexNameExpressionResolver.concreteIndexNames(clusterState, IndicesOptions.lenientExpandOpen(), destinationIndex);

        String[] src = indexNameExpressionResolver.concreteIndexNames(
            clusterState,
            IndicesOptions.lenientExpandOpen(),
            true,
            config.getSource().getIndex()
        );
        if (PersistentTasksCustomMetadata.getTaskWithId(clusterState, config.getId()) != null && dest.length == 0
            && src.length > 0) {
            TransformIndex.createDestinationIndex(
                client,
                auditor,
                indexNameExpressionResolver,
                clusterState,
                config,
                destIndexSettings,
                destIndexMappings,
                createDestinationListener
            );
        } else {
            createDestinationListener.onResponse(null);
        }
    }

    private TransformUpdater() {}
}
