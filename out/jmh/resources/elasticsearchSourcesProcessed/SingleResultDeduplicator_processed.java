/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action;

import org.elasticsearch.action.support.ContextPreservingActionListener;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * Wraps an async action that consumes an {@link ActionListener} such that multiple invocations of {@link #execute(ActionListener)} can
 * share the result from a single call to the wrapped action. This implementation is similar to {@link ResultDeduplicator} but offers
 * stronger guarantees of not seeing a stale result ever. Concretely, every invocation of {@link #execute(ActionListener)} is guaranteed to
 * be resolved with a response that has been computed at a time after the call to {@code execute} has been made. This allows this class to
 * be used to deduplicate results from actions that produce results that change over time transparently.
 *
 * @param <T> Result type
 */
public final class SingleResultDeduplicator<T> {

    private final ThreadContext threadContext;

    /**
     * List of listeners waiting for the execution after the current in-progress execution. If {@code null} then no execution is in
     * progress currently, otherwise an execution is in progress and will trigger another execution that will resolve any listeners queued
     * up here once done.
     */
    private List<ActionListener<T>> waitingListeners;
    /**
     * The threadContext associated with the first listener in the waitingListeners. This context will be restored right before
     * we perform the {@code executeAction}.
     */
    private ThreadContext.StoredContext waitingStoredContext;

    private final Consumer<ActionListener<T>> executeAction;

    public SingleResultDeduplicator(ThreadContext threadContext, Consumer<ActionListener<T>> executeAction) {
        this.threadContext = threadContext;
        this.executeAction = executeAction;
    }

    /**
     * Execute the action for the given {@code listener}.
     * @param listener listener to resolve with execution result. The listener always has its threadContext preserved, i.e.
     *                 when the listener is invoked, it will see its original threadContext plus any response headers generated
     *                 by performing the {@code executeAction}.
     */
    public void execute(ActionListener<T> listener) {
        synchronized (this) {
            if (waitingListeners == null) {
                waitingListeners = new ArrayList<>();
                waitingStoredContext = null;
            } else {
                if (waitingListeners.isEmpty()) {
                    assert waitingStoredContext == null;
                    waitingStoredContext = threadContext.newStoredContext();
                }
                waitingListeners.add(ContextPreservingActionListener.wrapPreservingContext(listener, threadContext));
                return;
            }
        }
        doExecute(ContextPreservingActionListener.wrapPreservingContext(listener, threadContext), null);
    }

    private void doExecute(ActionListener<T> listener, @Nullable ThreadContext.StoredContext storedContext) {
        final ActionListener<T> wrappedListener = ActionListener.runBefore(listener, () -> {
            final List<ActionListener<T>> listeners;
            final ThreadContext.StoredContext thisStoredContext;
            synchronized (this) {
                if (waitingListeners.isEmpty()) {
                    waitingListeners = null;
                    waitingStoredContext = null;
                    return;
                } else {
                    listeners = waitingListeners;
                    thisStoredContext = waitingStoredContext;
                    assert thisStoredContext != null : "stored context must not be null for the first listener in a batch";
                    waitingListeners = new ArrayList<>();
                    waitingStoredContext = null;
                }
            }

            try (var ignore = threadContext.newStoredContext()) {
                doExecute(new ActionListener<>() {
                    @Override
                    public void onResponse(T response) {
                        ActionListener.onResponse(listeners, response);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        ActionListener.onFailure(listeners, e);
                    }
                }, thisStoredContext);
            }
        });
        if (storedContext != null) {
            storedContext.restore();
        }
        ActionListener.run(wrappedListener, executeAction::accept);
    }
}
