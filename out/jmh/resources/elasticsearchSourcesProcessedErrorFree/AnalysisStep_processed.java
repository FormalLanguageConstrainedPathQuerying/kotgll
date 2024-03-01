/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.dataframe.steps;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.client.internal.ParentTaskAssigningClient;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xpack.core.ml.dataframe.DataFrameAnalyticsConfig;
import org.elasticsearch.xpack.ml.dataframe.DataFrameAnalyticsTask;
import org.elasticsearch.xpack.ml.dataframe.extractor.DataFrameDataExtractorFactory;
import org.elasticsearch.xpack.ml.dataframe.process.AnalyticsProcessManager;
import org.elasticsearch.xpack.ml.notifications.DataFrameAnalyticsAuditor;

import java.util.Objects;

public class AnalysisStep extends AbstractDataFrameAnalyticsStep {

    private final AnalyticsProcessManager processManager;

    public AnalysisStep(
        NodeClient client,
        DataFrameAnalyticsTask task,
        DataFrameAnalyticsAuditor auditor,
        DataFrameAnalyticsConfig config,
        AnalyticsProcessManager processManager
    ) {
        super(client, task, auditor, config);
        this.processManager = Objects.requireNonNull(processManager);
    }

    @Override
    public Name name() {
        return Name.ANALYSIS;
    }

    @Override
    public void cancel(String reason, TimeValue timeout) {
        processManager.stop(task);
    }

    @Override
    public void updateProgress(ActionListener<Void> listener) {
        listener.onResponse(null);
    }

    @Override
    protected void doExecute(ActionListener<StepResponse> listener) {
        task.getStatsHolder().getDataCountsTracker().reset();

        final ParentTaskAssigningClient parentTaskClient = parentTaskClient();
        ActionListener<DataFrameDataExtractorFactory> dataExtractorFactoryListener = ActionListener.wrap(
            dataExtractorFactory -> processManager.runJob(task, config, dataExtractorFactory, listener),
            listener::onFailure
        );

        ActionListener<BroadcastResponse> refreshListener = ActionListener.wrap(refreshResponse -> {
            DataFrameDataExtractorFactory.createForDestinationIndex(parentTaskClient, config, dataExtractorFactoryListener);
        }, dataExtractorFactoryListener::onFailure);

        refreshDestAsync(refreshListener);
    }
}
