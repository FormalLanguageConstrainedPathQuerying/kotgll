/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.job.process.autodetect.output;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.RetryableAction;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xpack.core.ml.action.PutJobAction;
import org.elasticsearch.xpack.core.ml.action.UpdateJobAction;
import org.elasticsearch.xpack.ml.MachineLearning;

import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

/**
 * Class to retry updates to the model snapshot ID on the job config after a new model snapshot result
 * is seen. Prior to the introduction of this functionality we saw cases where this particular job config
 * update would fail, so that the job would have persisted a perfectly valid model snapshot and yet it
 * would not be used if the job failed over to another node, leading to wasted work rerunning from an
 * older snapshot.
 */
public class RetryableUpdateModelSnapshotAction extends RetryableAction<PutJobAction.Response> {

    private static final Logger logger = LogManager.getLogger(RetryableUpdateModelSnapshotAction.class);

    private final Client client;
    private final UpdateJobAction.Request updateRequest;
    private volatile boolean hasFailedAtLeastOnce;

    public RetryableUpdateModelSnapshotAction(
        Client client,
        UpdateJobAction.Request updateRequest,
        ActionListener<PutJobAction.Response> listener
    ) {
        super(
            logger,
            client.threadPool(),
            TimeValue.timeValueSeconds(15),
            TimeValue.timeValueMinutes(2),
            TimeValue.timeValueMinutes(5),
            listener,
            client.threadPool().executor(MachineLearning.UTILITY_THREAD_POOL_NAME)
        );
        this.client = client;
        this.updateRequest = updateRequest;
    }

    @Override
    public void tryAction(ActionListener<PutJobAction.Response> listener) {
        executeAsyncWithOrigin(client, ML_ORIGIN, UpdateJobAction.INSTANCE, updateRequest, listener);
    }

    @Override
    public boolean shouldRetry(Exception e) {
        if (hasFailedAtLeastOnce == false) {
            hasFailedAtLeastOnce = true;
            logger.warn(() -> "[" + updateRequest.getJobId() + "] Failed to update job with new model snapshot id; attempting retry", e);
        }
        return true;
    }
}
