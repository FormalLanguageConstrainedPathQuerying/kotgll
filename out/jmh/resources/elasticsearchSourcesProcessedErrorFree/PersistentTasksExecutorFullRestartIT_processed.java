/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.persistent;

import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata.PersistentTask;
import org.elasticsearch.persistent.TestPersistentTasksPlugin.TestParams;
import org.elasticsearch.persistent.TestPersistentTasksPlugin.TestPersistentTasksExecutor;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, minNumDataNodes = 1)
public class PersistentTasksExecutorFullRestartIT extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(TestPersistentTasksPlugin.class);
    }

    public void testFullClusterRestart() throws Exception {
        PersistentTasksService service = internalCluster().getInstance(PersistentTasksService.class);
        int numberOfTasks = randomIntBetween(1, 10);
        String[] taskIds = new String[numberOfTasks];
        List<PlainActionFuture<PersistentTask<TestParams>>> futures = new ArrayList<>(numberOfTasks);

        for (int i = 0; i < numberOfTasks; i++) {
            PlainActionFuture<PersistentTask<TestParams>> future = new PlainActionFuture<>();
            futures.add(future);
            taskIds[i] = UUIDs.base64UUID();
            service.sendStartRequest(taskIds[i], TestPersistentTasksExecutor.NAME, new TestParams("Blah"), future);
        }

        for (int i = 0; i < numberOfTasks; i++) {
            assertThat(futures.get(i).get().getId(), equalTo(taskIds[i]));
        }

        List<PersistentTask<?>> tasksInProgress = findTasks(internalCluster().clusterService().state(), TestPersistentTasksExecutor.NAME);
        assertThat(tasksInProgress.size(), equalTo(numberOfTasks));

        assertBusy(() -> {
            assertThat(
                clusterAdmin().prepareListTasks().setActions(TestPersistentTasksExecutor.NAME + "[c]").get().getTasks().size(),
                greaterThan(0)
            );
        });

        internalCluster().fullRestart();
        ensureYellow();

        tasksInProgress = findTasks(internalCluster().clusterService().state(), TestPersistentTasksExecutor.NAME);
        assertThat(tasksInProgress.size(), equalTo(numberOfTasks));
        Set<String> taskInProgressIds = tasksInProgress.stream().map(PersistentTask::getId).collect(Collectors.toSet());
        for (int i = 0; i < numberOfTasks; i++) {
            assertTrue(taskInProgressIds.contains(taskIds[i]));
        }

        logger.info("Waiting for {} tasks to start", numberOfTasks);
        assertBusy(() -> {
            assertThat(
                clusterAdmin().prepareListTasks().setActions(TestPersistentTasksExecutor.NAME + "[c]").get().getTasks().size(),
                equalTo(numberOfTasks)
            );
        });

        logger.info("Complete all tasks");
        assertThat(
            new TestPersistentTasksPlugin.TestTasksRequestBuilder(client()).setOperation("finish").get().getTasks().size(),
            equalTo(numberOfTasks)
        );

        assertBusy(() -> {
            assertThat(findTasks(internalCluster().clusterService().state(), TestPersistentTasksExecutor.NAME), empty());
        });

    }
}
