/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.cluster;

import org.elasticsearch.cluster.service.MasterService;
import org.elasticsearch.common.Priority;

import java.util.List;

/**
 * Used to execute things on the master service thread on nodes that are not necessarily master
 */
public abstract class LocalMasterServiceTask implements ClusterStateTaskListener {

    private final Priority priority;

    public LocalMasterServiceTask(Priority priority) {
        this.priority = priority;
    }

    protected void execute(ClusterState currentState) {}

    protected void onPublicationComplete() {}

    public void submit(MasterService masterService, String source) {
        masterService.createTaskQueue("local-master-service-task", priority, new ClusterStateTaskExecutor<LocalMasterServiceTask>() {

            @Override
            public boolean runOnlyOnMaster() {
                return false;
            }

            @Override
            public String describeTasks(List<LocalMasterServiceTask> tasks) {
                return ""; 
            }

            @Override
            public ClusterState execute(BatchExecutionContext<LocalMasterServiceTask> batchExecutionContext) {
                final var thisTask = LocalMasterServiceTask.this;
                final var taskContexts = batchExecutionContext.taskContexts();
                assert taskContexts.size() == 1 && taskContexts.get(0).getTask() == thisTask
                    : "expected one-element task list containing current object but was " + taskContexts;
                try (var ignored = taskContexts.get(0).captureResponseHeaders()) {
                    thisTask.execute(batchExecutionContext.initialState());
                }
                taskContexts.get(0).success(() -> onPublicationComplete());
                return batchExecutionContext.initialState();
            }
        }).submitTask(source, this, null);
    }
}
