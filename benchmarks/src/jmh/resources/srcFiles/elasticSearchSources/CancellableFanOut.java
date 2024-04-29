/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.Strings;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.Task;

import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows an action to fan-out to several sub-actions and accumulate their results, but which reacts to a cancellation by releasing all
 * references to itself, and hence the partially-accumulated results, allowing them to be garbage-collected. This is a useful protection for
 * cases where the results may consume a lot of heap (e.g. stats) but the final response may be delayed by a single slow node for long
 * enough that the client gives up.
 * <p>
 * Note that it's easy to accidentally capture another reference to this class when implementing it, and this will prevent the early release
 * of any accumulated results. Beware of lambdas and method references. You must test your implementation carefully (using e.g.
 * {@code ReachabilityChecker}) to make sure it doesn't do this.
 */
public abstract class CancellableFanOut<Item, ItemResponse, FinalResponse> {

    private static final Logger logger = LogManager.getLogger(CancellableFanOut.class);

    /**
     * Run the fan-out action.
     *
     * @param task          The task to watch for cancellations. If {@code null} or not a {@link CancellableTask} then the fan-out still
     *                      works, just without any cancellation handling.
     * @param itemsIterator The items over which to fan out. Iterated on the calling thread.
     * @param listener      A listener for the final response, which is completed after all the fanned-out actions have completed. It is not
     *                      completed promptly on cancellation. Completed on the thread that handles the final per-item response (or
     *                      the calling thread if there are no items).
     */
    public final void run(@Nullable Task task, Iterator<Item> itemsIterator, ActionListener<FinalResponse> listener) {

        final var cancellableTask = task instanceof CancellableTask ct ? ct : null;

        final var resultListener = new SubscribableListener<FinalResponse>();

        final var resultListenerCompleter = new AtomicReference<Runnable>(() -> {
            if (cancellableTask != null && cancellableTask.notifyIfCancelled(resultListener)) {
                return;
            }
            ActionListener.completeWith(resultListener, this::onCompletion);
        });

        final var itemCancellationListener = new SubscribableListener<ItemResponse>();
        if (cancellableTask != null) {
            cancellableTask.addListener(() -> {
                assert cancellableTask.isCancelled();
                final var semaphore = new Semaphore(0);
                resultListenerCompleter.getAndSet(semaphore::acquireUninterruptibly).run();
                semaphore.release();
                cancellableTask.notifyIfCancelled(itemCancellationListener);
            });
        }

        try (var refs = new RefCountingRunnable(new SubtasksCompletionHandler<>(resultListenerCompleter, resultListener, listener))) {
            while (itemsIterator.hasNext()) {
                final var item = itemsIterator.next();

                final ActionListener<ItemResponse> itemResponseListener = ActionListener.notifyOnce(new ActionListener<>() {
                    @Override
                    public void onResponse(ItemResponse itemResponse) {
                        try {
                            onItemResponse(item, itemResponse);
                        } catch (Exception e) {
                            logger.error(
                                () -> Strings.format(
                                    "unexpected exception handling [%s] for item [%s] in [%s]",
                                    itemResponse,
                                    item,
                                    CancellableFanOut.this
                                ),
                                e
                            );
                            assert false : e;
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (cancellableTask != null && cancellableTask.isCancelled()) {
                            return;
                        }
                        onItemFailure(item, e); 
                    }

                    @Override
                    public String toString() {
                        return "[" + CancellableFanOut.this + "][" + listener + "][" + item + "]";
                    }
                });

                if (cancellableTask != null) {
                    if (cancellableTask.isCancelled()) {
                        return;
                    }

                    itemCancellationListener.addListener(itemResponseListener);
                }

                ActionListener.run(ActionListener.releaseAfter(itemResponseListener, refs.acquire()), l -> sendItemRequest(item, l));
            }
        } catch (Exception e) {
            logger.error("unexpected failure in [" + this + "][" + listener + "]", e);
            assert false : e;
            throw e;
        }
    }

    /**
     * Run the action (typically by sending a transport request) for an individual item. Called in sequence on the thread that invoked
     * {@link #run}. May not be called for every item if the task is cancelled during the iteration.
     * <p>
     * Note that it's easy to accidentally capture another reference to this class when implementing this method, and that will prevent the
     * early release of any accumulated results. Beware of lambdas, and test carefully.
     */
    protected abstract void sendItemRequest(Item item, ActionListener<ItemResponse> listener);

    /**
     * Handle a successful response for an item. May be called concurrently for multiple items. Not called if the task is cancelled. Must
     * not throw any exceptions.
     * <p>
     * Note that it's easy to accidentally capture another reference to this class when implementing this method, and that will prevent the
     * early release of any accumulated results. Beware of lambdas, and test carefully.
     */
    protected abstract void onItemResponse(Item item, ItemResponse itemResponse);

    /**
     * Handle a failure for an item. May be called concurrently for multiple items. Not called if the task is cancelled. Must not throw any
     * exceptions.
     * <p>
     * Note that it's easy to accidentally capture another reference to this class when implementing this method, and that will prevent the
     * early release of any accumulated results. Beware of lambdas, and test carefully.
     */
    protected abstract void onItemFailure(Item item, Exception e);

    /**
     * Called when responses for all items have been processed, on the thread that processed the last per-item response or possibly the
     * thread which called {@link #run} if all items were processed before {@link #run} returns. Not called if the task is cancelled.
     * <p>
     * Note that it's easy to accidentally capture another reference to this class when implementing this method, and that will prevent the
     * early release of any accumulated results. Beware of lambdas, and test carefully.
     */
    protected abstract FinalResponse onCompletion() throws Exception;

    private static class SubtasksCompletionHandler<FinalResponse> implements Runnable {
        private final AtomicReference<Runnable> resultListenerCompleter;
        private final SubscribableListener<FinalResponse> resultListener;
        private final ActionListener<FinalResponse> listener;

        private SubtasksCompletionHandler(
            AtomicReference<Runnable> resultListenerCompleter,
            SubscribableListener<FinalResponse> resultListener,
            ActionListener<FinalResponse> listener
        ) {
            this.resultListenerCompleter = resultListenerCompleter;
            this.resultListener = resultListener;
            this.listener = listener;
        }

        @Override
        public void run() {
            resultListenerCompleter.getAndSet(() -> {}).run();
            assert resultListener.isDone();
            resultListener.addListener(listener);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + listener.toString() + "]";
        }
    }
}
