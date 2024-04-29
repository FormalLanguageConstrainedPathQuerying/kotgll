/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.common.util.concurrent;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.core.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This async IO processor allows to batch IO operations and have a single writer processing the write operations.
 * This can be used to ensure that threads can continue with other work while the actual IO operation is still processed
 * by a single worker. A worker in this context can be any caller of the {@link #put(Object, Consumer)} method since it will
 * hijack a worker if nobody else is currently processing queued items. If the internal queue has reached it's capacity incoming threads
 * might be blocked until other items are processed
 */
public abstract class AsyncIOProcessor<Item> {
    private final Logger logger;
    private final ArrayBlockingQueue<Tuple<Item, Consumer<Exception>>> queue;
    private final ThreadContext threadContext;
    private final Semaphore promiseSemaphore = new Semaphore(1);

    protected AsyncIOProcessor(Logger logger, int queueSize, ThreadContext threadContext) {
        this.logger = logger;
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.threadContext = threadContext;
    }

    /**
     * Adds the given item to the queue. The listener is notified once the item is processed
     */
    public final void put(Item item, Consumer<Exception> listener) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        final boolean promised = promiseSemaphore.tryAcquire();
        if (promised == false) {
            try {
                queue.put(new Tuple<>(item, preserveContext(listener)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                listener.accept(e);
            }
        }

        if (promised || promiseSemaphore.tryAcquire()) {
            final List<Tuple<Item, Consumer<Exception>>> candidates = new ArrayList<>();
            if (promised) {
                candidates.add(new Tuple<>(item, listener));
            }
            drainAndProcessAndRelease(candidates);
            while (queue.isEmpty() == false && promiseSemaphore.tryAcquire()) {
                drainAndProcessAndRelease(candidates);
            }
        }
    }

    private void drainAndProcessAndRelease(List<Tuple<Item, Consumer<Exception>>> candidates) {
        Exception exception;
        try {
            queue.drainTo(candidates);
            exception = processList(candidates);
        } finally {
            promiseSemaphore.release();
        }
        notifyList(candidates, exception);
        candidates.clear();
    }

    private Exception processList(List<Tuple<Item, Consumer<Exception>>> candidates) {
        Exception exception = null;
        if (candidates.isEmpty() == false) {
            try {
                write(candidates);
            } catch (Exception ex) { 
                logger.debug("failed to write candidates", ex);
                exception = ex;
            }
        }
        return exception;
    }

    private void notifyList(List<Tuple<Item, Consumer<Exception>>> candidates, Exception exception) {
        for (Tuple<Item, Consumer<Exception>> tuple : candidates) {
            Consumer<Exception> consumer = tuple.v2();
            try {
                consumer.accept(exception);
            } catch (Exception ex) {
                logger.warn("failed to notify callback", ex);
            }
        }
    }

    private Consumer<Exception> preserveContext(Consumer<Exception> consumer) {
        Supplier<ThreadContext.StoredContext> restorableContext = threadContext.newRestorableContext(false);
        return e -> {
            try (ThreadContext.StoredContext ignore = restorableContext.get()) {
                consumer.accept(e);
            }
        };
    }

    /**
     * Writes or processes the items out or to disk.
     */
    protected abstract void write(List<Tuple<Item, Consumer<Exception>>> candidates) throws IOException;
}
