/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ilm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.LifecycleExecutionState;
import org.elasticsearch.common.Strings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;

/**
 * Deletes the index identified by the shrink index name stored in the lifecycle state of the managed index (if any was generated)
 */
public class CleanupShrinkIndexStep extends AsyncRetryDuringSnapshotActionStep {
    public static final String NAME = "cleanup-shrink-index";
    private static final Logger logger = LogManager.getLogger(CleanupShrinkIndexStep.class);

    public CleanupShrinkIndexStep(StepKey key, StepKey nextStepKey, Client client) {
        super(key, nextStepKey, client);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    @Override
    void performDuringNoSnapshot(IndexMetadata indexMetadata, ClusterState currentClusterState, ActionListener<Void> listener) {
        final String shrunkenIndexSource = IndexMetadata.INDEX_RESIZE_SOURCE_NAME.get(indexMetadata.getSettings());
        if (Strings.isNullOrEmpty(shrunkenIndexSource) == false) {
            if (currentClusterState.metadata().index(shrunkenIndexSource) == null) {
                String policyName = indexMetadata.getLifecyclePolicyName();
                logger.warn(
                    "managed index [{}] as part of policy [{}] is a shrunk index and the source index [{}] does not exist "
                        + "anymore. will skip the [{}] step",
                    indexMetadata.getIndex().getName(),
                    policyName,
                    shrunkenIndexSource,
                    NAME
                );
                listener.onResponse(null);
                return;
            }
        }

        LifecycleExecutionState lifecycleState = indexMetadata.getLifecycleExecutionState();
        final String shrinkIndexName = lifecycleState.shrinkIndexName();
        if (Strings.hasText(shrinkIndexName) == false) {
            listener.onResponse(null);
            return;
        }
        getClient().admin()
            .indices()
            .delete(
                new DeleteIndexRequest(shrinkIndexName).masterNodeTimeout(TimeValue.MAX_VALUE),
                new ActionListener<AcknowledgedResponse>() {
                    @Override
                    public void onResponse(AcknowledgedResponse acknowledgedResponse) {
                        listener.onResponse(null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (e instanceof IndexNotFoundException) {
                            listener.onResponse(null);
                        } else {
                            listener.onFailure(e);
                        }
                    }
                }
            );
    }

}
