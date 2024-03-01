/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.util.concurrent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.core.Strings;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractThrottledTaskRunner} runs the enqueued tasks using the given executor, limiting the number of tasks that are submitted to
 * the executor at once.
 */
public class AbstractThrottledTaskRunner<T extends ActionListener<Releasable>> {
    private static final Logger logger = LogManager.getLogger(AbstractThrottledTaskRunner.class);

    private final String taskRunnerName;
    private final int maxRunningTasks;
    private final AtomicInteger runningTasks = new AtomicInteger();
    private final Queue<T> tasks;
    private final Executor executor;

    public AbstractThrottledTaskRunner(final String name, final int maxRunningTasks, final Executor executor, final Queue<T> taskQueue) {
        assert maxRunningTasks > 0;
        this.taskRunnerName = name;
        this.maxRunningTasks = maxRunningTasks;
        this.executor = executor;
        this.tasks = taskQueue;
    }

    /**
     * Submits a task for execution. If there are fewer than {@code maxRunningTasks} tasks currently running then this task is immediately
     * submitted to the executor. Otherwise this task is enqueued and will be submitted to the executor in turn on completion of some other
     * task.
     *
     * Tasks are executed via their {@link ActionListener#onResponse} method, receiving a {@link Releasable} which must be closed on
     * completion of the task. Task which are rejected from their executor are notified via their {@link ActionListener#onFailure} method.
     * Neither of these methods may themselves throw exceptions.
     */
    public void enqueueTask(final T task) {
        logger.trace("[{}] enqueuing task {}", taskRunnerName, task);
        tasks.add(task);
        pollAndSpawn();
    }

    /**
     * Allows certain tasks to force their execution, bypassing the queue-length limit on the executor. See also {@link
     * AbstractRunnable#isForceExecution()}.
     */
    protected boolean isForceExecution(@SuppressWarnings("unused") /* TODO test this */ T task) {
        return false;
    }

    private void pollAndSpawn() {
        while (incrementRunningTasks()) {
            T task = tasks.poll();
            if (task == null) {
                logger.trace("[{}] task queue is empty", taskRunnerName);
                int decremented = runningTasks.decrementAndGet();
                assert decremented >= 0;
                if (tasks.peek() == null) break;
            } else {
                final boolean isForceExecution = isForceExecution(task);
                executor.execute(new AbstractRunnable() {
                    private boolean rejected; 

                    private final Releasable releasable = Releasables.releaseOnce(() -> {
                        int decremented = runningTasks.decrementAndGet();
                        assert decremented >= 0;

                        if (rejected == false) {
                            pollAndSpawn();
                        }
                    });

                    @Override
                    public boolean isForceExecution() {
                        return isForceExecution;
                    }

                    @Override
                    public void onRejection(Exception e) {
                        logger.trace("[{}] task {} rejected", taskRunnerName, task);
                        rejected = true;
                        try {
                            task.onFailure(e);
                        } finally {
                            releasable.close();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.error(() -> Strings.format("[%s] task %s failed", taskRunnerName, task), e);
                        assert false : e;
                        task.onFailure(e);
                    }

                    @Override
                    protected void doRun() {
                        logger.trace("[{}] running task {}", taskRunnerName, task);
                        task.onResponse(releasable);
                    }

                    @Override
                    public String toString() {
                        return task.toString();
                    }
                });
            }
        }
    }

    private boolean incrementRunningTasks() {
        int preUpdateValue = runningTasks.getAndAccumulate(maxRunningTasks, (v, maxRunning) -> v < maxRunning ? v + 1 : v);
        assert preUpdateValue <= maxRunningTasks;
        return preUpdateValue < maxRunningTasks;
    }

    int runningTasks() {
        return runningTasks.get();
    }

    /**
     * Run a single task on the given executor which eagerly pulls tasks from the queue and executes them. This must only be used if the
     * tasks in the queue are all synchronous, i.e. they release their ref before returning from {@code onResponse()}.
     */
    public void runSyncTasksEagerly(Executor executor) {
        executor.execute(new AbstractRunnable() {
            @Override
            protected void doRun() {
                final AtomicBoolean isDone = new AtomicBoolean(true);
                final Releasable ref = () -> isDone.set(true);
                ActionListener<Releasable> task;
                while ((task = tasks.poll()) != null) {
                    isDone.set(false);
                    try {
                        logger.trace("[{}] eagerly running task {}", taskRunnerName, task);
                        task.onResponse(ref);
                    } catch (Exception e) {
                        logger.error(Strings.format("[%s] task %s failed", taskRunnerName, task), e);
                        assert false : e;
                        task.onFailure(e);
                        return;
                    }
                    if (isDone.get() == false) {
                        logger.error(
                            "runSyncTasksEagerly() was called on a queue [{}] containing an async task: [{}]",
                            taskRunnerName,
                            task
                        );
                        assert false;
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                logger.error("unexpected failure in runSyncTasksEagerly", e);
                assert false : e;
            }

            @Override
            public void onRejection(Exception e) {
                if (e instanceof EsRejectedExecutionException) {
                    logger.debug("runSyncTasksEagerly was rejected", e);
                } else {
                    onFailure(e);
                }
            }
        });
    }
}
