/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import javax.annotation.CheckForNull;

/**
 * A support class for {@code ListenableFuture} implementations to manage their listeners. An
 * instance contains a list of listeners, each with an associated {@code Executor}, and guarantees
 * that every {@code Runnable} that is {@linkplain #add added} will be executed after {@link
 * #execute()} is called. Any {@code Runnable} added after the call to {@code execute} is still
 * guaranteed to execute. There is no guarantee, however, that listeners will be executed in the
 * order that they are added.
 *
 * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown
 * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception
 * thrown by {@linkplain MoreExecutors#directExecutor direct execution}) will be caught and logged.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class ExecutionList {
  /** Logger to log exceptions caught when running runnables. */
  private static final LazyLogger log = new LazyLogger(ExecutionList.class);

  /**
   * The runnable, executor pairs to execute. This acts as a stack threaded through the {@link
   * RunnableExecutorPair#next} field.
   */
  @GuardedBy("this")
  @CheckForNull
  private RunnableExecutorPair runnables;

  @GuardedBy("this")
  private boolean executed;

  /** Creates a new, empty {@link ExecutionList}. */
  public ExecutionList() {}

  /**
   * Adds the {@code Runnable} and accompanying {@code Executor} to the list of listeners to
   * execute. If execution has already begun, the listener is executed immediately.
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation.
   */
  public void add(Runnable runnable, Executor executor) {
    checkNotNull(runnable, "Runnable was null.");
    checkNotNull(executor, "Executor was null.");

    synchronized (this) {
      if (!executed) {
        runnables = new RunnableExecutorPair(runnable, executor, runnables);
        return;
      }
    }
    executeListener(runnable, executor);
  }

  /**
   * Runs this execution list, executing all existing pairs in the order they were added. However,
   * note that listeners added after this point may be executed before those previously added, and
   * note that the execution order of all listeners is ultimately chosen by the implementations of
   * the supplied executors.
   *
   * <p>This method is idempotent. Calling it several times in parallel is semantically equivalent
   * to calling it exactly once.
   *
   * @since 10.0 (present in 1.0 as {@code run})
   */
  public void execute() {
    RunnableExecutorPair list;
    synchronized (this) {
      if (executed) {
        return;
      }
      executed = true;
      list = runnables;
      runnables = null; 
    }

    RunnableExecutorPair reversedList = null;
    while (list != null) {
      RunnableExecutorPair tmp = list;
      list = list.next;
      tmp.next = reversedList;
      reversedList = tmp;
    }
    while (reversedList != null) {
      executeListener(reversedList.runnable, reversedList.executor);
      reversedList = reversedList.next;
    }
  }

  /**
   * Submits the given runnable to the given {@link Executor} catching and logging all {@linkplain
   * RuntimeException runtime exceptions} thrown by the executor.
   */
  @SuppressWarnings("CatchingUnchecked") 
  private static void executeListener(Runnable runnable, Executor executor) {
    try {
      executor.execute(runnable);
    } catch (Exception e) { 
      log.get()
          .log(
              Level.SEVERE,
              "RuntimeException while executing runnable "
                  + runnable
                  + " with executor "
                  + executor,
              e);
    }
  }

  private static final class RunnableExecutorPair {
    final Runnable runnable;
    final Executor executor;
    @CheckForNull RunnableExecutorPair next;

    RunnableExecutorPair(
        Runnable runnable, Executor executor, @CheckForNull RunnableExecutorPair next) {
      this.runnable = runnable;
      this.executor = executor;
      this.next = next;
    }
  }
}
