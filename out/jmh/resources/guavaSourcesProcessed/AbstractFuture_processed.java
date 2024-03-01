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
import static com.google.common.util.concurrent.NullnessCasts.uncheckedNull;
import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import com.google.common.util.concurrent.internal.InternalFutures;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.ForOverride;
import com.google.j2objc.annotations.ReflectionSupport;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract implementation of {@link ListenableFuture}, intended for advanced users only. More
 * common ways to create a {@code ListenableFuture} include instantiating a {@link SettableFuture},
 * submitting a task to a {@link ListeningExecutorService}, and deriving a {@code Future} from an
 * existing one, typically using methods like {@link Futures#transform(ListenableFuture,
 * com.google.common.base.Function, java.util.concurrent.Executor) Futures.transform} and {@link
 * Futures#catching(ListenableFuture, Class, com.google.common.base.Function,
 * java.util.concurrent.Executor) Futures.catching}.
 *
 * <p>This class implements all methods in {@code ListenableFuture}. Subclasses should provide a way
 * to set the result of the computation through the protected methods {@link #set(Object)}, {@link
 * #setFuture(ListenableFuture)} and {@link #setException(Throwable)}. Subclasses may also override
 * {@link #afterDone()}, which will be invoked automatically when the future completes. Subclasses
 * should rarely override other methods.
 *
 * @author Sven Mawson
 * @author Luke Sandberg
 * @since 1.0
 */
@SuppressWarnings({
  "ShortCircuitBoolean", 
  "nullness", 
})
@GwtCompatible(emulated = true)
@ReflectionSupport(value = ReflectionSupport.Level.FULL)
@ElementTypesAreNonnullByDefault
public abstract class AbstractFuture<V extends @Nullable Object> extends InternalFutureFailureAccess
    implements ListenableFuture<V> {

  static final boolean GENERATE_CANCELLATION_CAUSES;

  static {
    boolean generateCancellationCauses;
    try {
      generateCancellationCauses =
          Boolean.parseBoolean(
              System.getProperty("guava.concurrent.generate_cancellation_cause", "false"));
    } catch (SecurityException e) {
      generateCancellationCauses = false;
    }
    GENERATE_CANCELLATION_CAUSES = generateCancellationCauses;
  }

  /**
   * Tag interface marking trusted subclasses. This enables some optimizations. The implementation
   * of this interface must also be an AbstractFuture and must not override or expose for overriding
   * any of the public methods of ListenableFuture.
   */
  interface Trusted<V extends @Nullable Object> extends ListenableFuture<V> {}

  /**
   * A less abstract subclass of AbstractFuture. This can be used to optimize setFuture by ensuring
   * that {@link #get} calls exactly the implementation of {@link AbstractFuture#get}.
   */
  abstract static class TrustedFuture<V extends @Nullable Object> extends AbstractFuture<V>
      implements Trusted<V> {
    @CanIgnoreReturnValue
    @Override
    @ParametricNullness
    public final V get() throws InterruptedException, ExecutionException {
      return super.get();
    }

    @CanIgnoreReturnValue
    @Override
    @ParametricNullness
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return super.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
      return super.isDone();
    }

    @Override
    public final boolean isCancelled() {
      return super.isCancelled();
    }

    @Override
    public final void addListener(Runnable listener, Executor executor) {
      super.addListener(listener, executor);
    }

    @CanIgnoreReturnValue
    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(mayInterruptIfRunning);
    }
  }

  static final LazyLogger log = new LazyLogger(AbstractFuture.class);

  private static final long SPIN_THRESHOLD_NANOS = 1000L;

  private static final AtomicHelper ATOMIC_HELPER;

  static {
    AtomicHelper helper;
    Throwable thrownUnsafeFailure = null;
    Throwable thrownAtomicReferenceFieldUpdaterFailure = null;

    try {
      helper = new UnsafeAtomicHelper();
    } catch (Exception | Error unsafeFailure) { 
      thrownUnsafeFailure = unsafeFailure;
      try {
        helper =
            new SafeAtomicHelper(
                newUpdater(Waiter.class, Thread.class, "thread"),
                newUpdater(Waiter.class, Waiter.class, "next"),
                newUpdater(AbstractFuture.class, Waiter.class, "waiters"),
                newUpdater(AbstractFuture.class, Listener.class, "listeners"),
                newUpdater(AbstractFuture.class, Object.class, "value"));
      } catch (Exception 
          | Error atomicReferenceFieldUpdaterFailure) {
        thrownAtomicReferenceFieldUpdaterFailure = atomicReferenceFieldUpdaterFailure;
        helper = new SynchronizedHelper();
      }
    }
    ATOMIC_HELPER = helper;

    @SuppressWarnings("unused")
    Class<?> ensureLoaded = LockSupport.class;

    if (thrownAtomicReferenceFieldUpdaterFailure != null) {
      log.get().log(Level.SEVERE, "UnsafeAtomicHelper is broken!", thrownUnsafeFailure);
      log.get()
          .log(
              Level.SEVERE,
              "SafeAtomicHelper is broken!",
              thrownAtomicReferenceFieldUpdaterFailure);
    }
  }

  /** Waiter links form a Treiber stack, in the {@link #waiters} field. */
  private static final class Waiter {
    static final Waiter TOMBSTONE = new Waiter(false /* ignored param */);

    @CheckForNull volatile Thread thread;
    @CheckForNull volatile Waiter next;

    /**
     * Constructor for the TOMBSTONE, avoids use of ATOMIC_HELPER in case this class is loaded
     * before the ATOMIC_HELPER. Apparently this is possible on some android platforms.
     */
    Waiter(boolean unused) {}

    Waiter() {
      ATOMIC_HELPER.putThread(this, Thread.currentThread());
    }

    void setNext(@CheckForNull Waiter next) {
      ATOMIC_HELPER.putNext(this, next);
    }

    void unpark() {
      Thread w = thread;
      if (w != null) {
        thread = null;
        LockSupport.unpark(w);
      }
    }
  }

  /**
   * Marks the given node as 'deleted' (null waiter) and then scans the list to unlink all deleted
   * nodes. This is an O(n) operation in the common case (and O(n^2) in the worst), but we are saved
   * by two things.
   *
   * <ul>
   *   <li>This is only called when a waiting thread times out or is interrupted. Both of which
   *       should be rare.
   *   <li>The waiters list should be very short.
   * </ul>
   */
  private void removeWaiter(Waiter node) {
    node.thread = null; 
    restart:
    while (true) {
      Waiter pred = null;
      Waiter curr = waiters;
      if (curr == Waiter.TOMBSTONE) {
        return; 
      }
      Waiter succ;
      while (curr != null) {
        succ = curr.next;
        if (curr.thread != null) { 
          pred = curr;
        } else if (pred != null) { 
          pred.next = succ;
          if (pred.thread == null) { 
            continue restart;
          }
        } else if (!ATOMIC_HELPER.casWaiters(this, curr, succ)) { 
          continue restart; 
        }
        curr = succ;
      }
      break;
    }
  }

  /** Listeners also form a stack through the {@link #listeners} field. */
  private static final class Listener {
    static final Listener TOMBSTONE = new Listener();
    @CheckForNull 
    final Runnable task;
    @CheckForNull 
    final Executor executor;

    @CheckForNull Listener next;

    Listener(Runnable task, Executor executor) {
      this.task = task;
      this.executor = executor;
    }

    Listener() {
      this.task = null;
      this.executor = null;
    }
  }

  /** A special value to represent {@code null}. */
  private static final Object NULL = new Object();

  /** A special value to represent failure, when {@link #setException} is called successfully. */
  private static final class Failure {
    static final Failure FALLBACK_INSTANCE =
        new Failure(
            new Throwable("Failure occurred while trying to finish a future.") {
              @Override
              public synchronized Throwable fillInStackTrace() {
                return this; 
              }
            });
    final Throwable exception;

    Failure(Throwable exception) {
      this.exception = checkNotNull(exception);
    }
  }

  /** A special value to represent cancellation and the 'wasInterrupted' bit. */
  private static final class Cancellation {
    @CheckForNull static final Cancellation CAUSELESS_INTERRUPTED;
    @CheckForNull static final Cancellation CAUSELESS_CANCELLED;

    static {
      if (GENERATE_CANCELLATION_CAUSES) {
        CAUSELESS_CANCELLED = null;
        CAUSELESS_INTERRUPTED = null;
      } else {
        CAUSELESS_CANCELLED = new Cancellation(false, null);
        CAUSELESS_INTERRUPTED = new Cancellation(true, null);
      }
    }

    final boolean wasInterrupted;
    @CheckForNull final Throwable cause;

    Cancellation(boolean wasInterrupted, @CheckForNull Throwable cause) {
      this.wasInterrupted = wasInterrupted;
      this.cause = cause;
    }
  }

  /** A special value that encodes the 'setFuture' state. */
  private static final class SetFuture<V extends @Nullable Object> implements Runnable {
    final AbstractFuture<V> owner;
    final ListenableFuture<? extends V> future;

    SetFuture(AbstractFuture<V> owner, ListenableFuture<? extends V> future) {
      this.owner = owner;
      this.future = future;
    }

    @Override
    public void run() {
      if (owner.value != this) {
        return;
      }
      Object valueToSet = getFutureValue(future);
      if (ATOMIC_HELPER.casValue(owner, this, valueToSet)) {
        complete(
            owner,
            /*
             * Interruption doesn't propagate through a SetFuture chain (see getFutureValue), so
             * don't invoke interruptTask.
             */
            false);
      }
    }
  }

  /**
   * This field encodes the current state of the future.
   *
   * <p>The valid values are:
   *
   * <ul>
   *   <li>{@code null} initial state, nothing has happened.
   *   <li>{@link Cancellation} terminal state, {@code cancel} was called.
   *   <li>{@link Failure} terminal state, {@code setException} was called.
   *   <li>{@link SetFuture} intermediate state, {@code setFuture} was called.
   *   <li>{@link #NULL} terminal state, {@code set(null)} was called.
   *   <li>Any other non-null value, terminal state, {@code set} was called with a non-null
   *       argument.
   * </ul>
   */
  @CheckForNull private volatile Object value;

  /** All listeners. */
  @CheckForNull private volatile Listener listeners;

  /** All waiting threads. */
  @CheckForNull private volatile Waiter waiters;

  /** Constructor for use by subclasses. */
  protected AbstractFuture() {}



  /**
   * {@inheritDoc}
   *
   * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if the
   * current thread is interrupted during the call, even if the value is already available.
   *
   * @throws CancellationException {@inheritDoc}
   */
  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, TimeoutException, ExecutionException {
    final long timeoutNanos = unit.toNanos(timeout); 
    long remainingNanos = timeoutNanos;
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Object localValue = value;
    if (localValue != null & !(localValue instanceof SetFuture)) {
      return getDoneValue(localValue);
    }
    final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0;
    long_wait_loop:
    if (remainingNanos >= SPIN_THRESHOLD_NANOS) {
      Waiter oldHead = waiters;
      if (oldHead != Waiter.TOMBSTONE) {
        Waiter node = new Waiter();
        do {
          node.setNext(oldHead);
          if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
            while (true) {
              OverflowAvoidingLockSupport.parkNanos(this, remainingNanos);
              if (Thread.interrupted()) {
                removeWaiter(node);
                throw new InterruptedException();
              }

              localValue = value;
              if (localValue != null & !(localValue instanceof SetFuture)) {
                return getDoneValue(localValue);
              }

              remainingNanos = endNanos - System.nanoTime();
              if (remainingNanos < SPIN_THRESHOLD_NANOS) {
                removeWaiter(node);
                break long_wait_loop; 
              }
            }
          }
          oldHead = waiters; 
        } while (oldHead != Waiter.TOMBSTONE);
      }
      return getDoneValue(requireNonNull(value));
    }
    while (remainingNanos > 0) {
      localValue = value;
      if (localValue != null & !(localValue instanceof SetFuture)) {
        return getDoneValue(localValue);
      }
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      remainingNanos = endNanos - System.nanoTime();
    }

    String futureToString = toString();
    final String unitString = unit.toString().toLowerCase(Locale.ROOT);
    String message = "Waited " + timeout + " " + unit.toString().toLowerCase(Locale.ROOT);
    if (remainingNanos + SPIN_THRESHOLD_NANOS < 0) {
      message += " (plus ";
      long overWaitNanos = -remainingNanos;
      long overWaitUnits = unit.convert(overWaitNanos, TimeUnit.NANOSECONDS);
      long overWaitLeftoverNanos = overWaitNanos - unit.toNanos(overWaitUnits);
      boolean shouldShowExtraNanos =
          overWaitUnits == 0 || overWaitLeftoverNanos > SPIN_THRESHOLD_NANOS;
      if (overWaitUnits > 0) {
        message += overWaitUnits + " " + unitString;
        if (shouldShowExtraNanos) {
          message += ",";
        }
        message += " ";
      }
      if (shouldShowExtraNanos) {
        message += overWaitLeftoverNanos + " nanoseconds ";
      }

      message += "delay)";
    }
    if (isDone()) {
      throw new TimeoutException(message + " but future completed as timeout expired");
    }
    throw new TimeoutException(message + " for " + futureToString);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if the
   * current thread is interrupted during the call, even if the value is already available.
   *
   * @throws CancellationException {@inheritDoc}
   */
  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public V get() throws InterruptedException, ExecutionException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Object localValue = value;
    if (localValue != null & !(localValue instanceof SetFuture)) {
      return getDoneValue(localValue);
    }
    Waiter oldHead = waiters;
    if (oldHead != Waiter.TOMBSTONE) {
      Waiter node = new Waiter();
      do {
        node.setNext(oldHead);
        if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
          while (true) {
            LockSupport.park(this);
            if (Thread.interrupted()) {
              removeWaiter(node);
              throw new InterruptedException();
            }
            localValue = value;
            if (localValue != null & !(localValue instanceof SetFuture)) {
              return getDoneValue(localValue);
            }
          }
        }
        oldHead = waiters; 
      } while (oldHead != Waiter.TOMBSTONE);
    }
    return getDoneValue(requireNonNull(value));
  }

  /** Unboxes {@code obj}. Assumes that obj is not {@code null} or a {@link SetFuture}. */
  @ParametricNullness
  private V getDoneValue(Object obj) throws ExecutionException {
    if (obj instanceof Cancellation) {
      throw cancellationExceptionWithCause("Task was cancelled.", ((Cancellation) obj).cause);
    } else if (obj instanceof Failure) {
      throw new ExecutionException(((Failure) obj).exception);
    } else if (obj == NULL) {
      /*
       * It's safe to return null because we would only have stored it in the first place if it were
       * a valid value for V.
       */
      return uncheckedNull();
    } else {
      @SuppressWarnings("unchecked") 
      V asV = (V) obj;
      return asV;
    }
  }

  @Override
  public boolean isDone() {
    final Object localValue = value;
    return localValue != null & !(localValue instanceof SetFuture);
  }

  @Override
  public boolean isCancelled() {
    final Object localValue = value;
    return localValue instanceof Cancellation;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a cancellation attempt succeeds on a {@code Future} that had previously been {@linkplain
   * #setFuture set asynchronously}, then the cancellation will also be propagated to the delegate
   * {@code Future} that was supplied in the {@code setFuture} call.
   *
   * <p>Rather than override this method to perform additional cancellation work or cleanup,
   * subclasses should override {@link #afterDone}, consulting {@link #isCancelled} and {@link
   * #wasInterrupted} as necessary. This ensures that the work is done even if the future is
   * cancelled without a call to {@code cancel}, such as by calling {@code
   * setFuture(cancelledFuture)}.
   *
   * <p>Beware of completing a future while holding a lock. Its listeners may do slow work or
   * acquire other locks, risking deadlocks.
   */
  @CanIgnoreReturnValue
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    Object localValue = value;
    boolean rValue = false;
    if (localValue == null | localValue instanceof SetFuture) {
      Object valueToSet =
          GENERATE_CANCELLATION_CAUSES
              ? new Cancellation(
                  mayInterruptIfRunning, new CancellationException("Future.cancel() was called."))
              /*
               * requireNonNull is safe because we've initialized these if
               * !GENERATE_CANCELLATION_CAUSES.
               *
               * TODO(cpovirk): Maybe it would be cleaner to define a CancellationSupplier interface
               * with two implementations, one that contains causeless Cancellation instances and
               * the other of which creates new Cancellation instances each time it's called? Yet
               * another alternative is to fill in a non-null value for each of the fields no matter
               * what and to just not use it if !GENERATE_CANCELLATION_CAUSES.
               */
              : requireNonNull(
                  mayInterruptIfRunning
                      ? Cancellation.CAUSELESS_INTERRUPTED
                      : Cancellation.CAUSELESS_CANCELLED);
      AbstractFuture<?> abstractFuture = this;
      while (true) {
        if (ATOMIC_HELPER.casValue(abstractFuture, localValue, valueToSet)) {
          rValue = true;
          complete(abstractFuture, mayInterruptIfRunning);
          if (localValue instanceof SetFuture) {
            ListenableFuture<?> futureToPropagateTo = ((SetFuture) localValue).future;
            if (futureToPropagateTo instanceof Trusted) {
              AbstractFuture<?> trusted = (AbstractFuture<?>) futureToPropagateTo;
              localValue = trusted.value;
              if (localValue == null | localValue instanceof SetFuture) {
                abstractFuture = trusted;
                continue; 
              }
            } else {
              futureToPropagateTo.cancel(mayInterruptIfRunning);
            }
          }
          break;
        }
        localValue = abstractFuture.value;
        if (!(localValue instanceof SetFuture)) {
          break;
        }
      }
    }
    return rValue;
  }

  /**
   * Subclasses can override this method to implement interruption of the future's computation. The
   * method is invoked automatically by a successful call to {@link #cancel(boolean) cancel(true)}.
   *
   * <p>The default implementation does nothing.
   *
   * <p>This method is likely to be deprecated. Prefer to override {@link #afterDone}, checking
   * {@link #wasInterrupted} to decide whether to interrupt your task.
   *
   * @since 10.0
   */
  protected void interruptTask() {}

  /**
   * Returns true if this future was cancelled with {@code mayInterruptIfRunning} set to {@code
   * true}.
   *
   * @since 14.0
   */
  protected final boolean wasInterrupted() {
    final Object localValue = value;
    return (localValue instanceof Cancellation) && ((Cancellation) localValue).wasInterrupted;
  }

  /**
   * {@inheritDoc}
   *
   * @since 10.0
   */
  @Override
  public void addListener(Runnable listener, Executor executor) {
    checkNotNull(listener, "Runnable was null.");
    checkNotNull(executor, "Executor was null.");
    if (!isDone()) {
      Listener oldHead = listeners;
      if (oldHead != Listener.TOMBSTONE) {
        Listener newNode = new Listener(listener, executor);
        do {
          newNode.next = oldHead;
          if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {
            return;
          }
          oldHead = listeners; 
        } while (oldHead != Listener.TOMBSTONE);
      }
    }
    executeListener(listener, executor);
  }

  /**
   * Sets the result of this {@code Future} unless this {@code Future} has already been cancelled or
   * set (including {@linkplain #setFuture set asynchronously}). When a call to this method returns,
   * the {@code Future} is guaranteed to be {@linkplain #isDone done} <b>only if</b> the call was
   * accepted (in which case it returns {@code true}). If it returns {@code false}, the {@code
   * Future} may have previously been set asynchronously, in which case its result may not be known
   * yet. That result, though not yet known, cannot be overridden by a call to a {@code set*}
   * method, only by a call to {@link #cancel}.
   *
   * <p>Beware of completing a future while holding a lock. Its listeners may do slow work or
   * acquire other locks, risking deadlocks.
   *
   * @param value the value to be used as the result
   * @return true if the attempt was accepted, completing the {@code Future}
   */
  @CanIgnoreReturnValue
  protected boolean set(@ParametricNullness V value) {
    Object valueToSet = value == null ? NULL : value;
    if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
      complete(this, /*callInterruptTask=*/ false);
      return true;
    }
    return false;
  }

  /**
   * Sets the failed result of this {@code Future} unless this {@code Future} has already been
   * cancelled or set (including {@linkplain #setFuture set asynchronously}). When a call to this
   * method returns, the {@code Future} is guaranteed to be {@linkplain #isDone done} <b>only if</b>
   * the call was accepted (in which case it returns {@code true}). If it returns {@code false}, the
   * {@code Future} may have previously been set asynchronously, in which case its result may not be
   * known yet. That result, though not yet known, cannot be overridden by a call to a {@code set*}
   * method, only by a call to {@link #cancel}.
   *
   * <p>Beware of completing a future while holding a lock. Its listeners may do slow work or
   * acquire other locks, risking deadlocks.
   *
   * @param throwable the exception to be used as the failed result
   * @return true if the attempt was accepted, completing the {@code Future}
   */
  @CanIgnoreReturnValue
  protected boolean setException(Throwable throwable) {
    Object valueToSet = new Failure(checkNotNull(throwable));
    if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
      complete(this, /*callInterruptTask=*/ false);
      return true;
    }
    return false;
  }

  /**
   * Sets the result of this {@code Future} to match the supplied input {@code Future} once the
   * supplied {@code Future} is done, unless this {@code Future} has already been cancelled or set
   * (including "set asynchronously," defined below).
   *
   * <p>If the supplied future is {@linkplain #isDone done} when this method is called and the call
   * is accepted, then this future is guaranteed to have been completed with the supplied future by
   * the time this method returns. If the supplied future is not done and the call is accepted, then
   * the future will be <i>set asynchronously</i>. Note that such a result, though not yet known,
   * cannot be overridden by a call to a {@code set*} method, only by a call to {@link #cancel}.
   *
   * <p>If the call {@code setFuture(delegate)} is accepted and this {@code Future} is later
   * cancelled, cancellation will be propagated to {@code delegate}. Additionally, any call to
   * {@code setFuture} after any cancellation will propagate cancellation to the supplied {@code
   * Future}.
   *
   * <p>Note that, even if the supplied future is cancelled and it causes this future to complete,
   * it will never trigger interruption behavior. In particular, it will not cause this future to
   * invoke the {@link #interruptTask} method, and the {@link #wasInterrupted} method will not
   * return {@code true}.
   *
   * <p>Beware of completing a future while holding a lock. Its listeners may do slow work or
   * acquire other locks, risking deadlocks.
   *
   * @param future the future to delegate to
   * @return true if the attempt was accepted, indicating that the {@code Future} was not previously
   *     cancelled or set.
   * @since 19.0
   */
  @CanIgnoreReturnValue
  protected boolean setFuture(ListenableFuture<? extends V> future) {
    checkNotNull(future);
    Object localValue = value;
    if (localValue == null) {
      if (future.isDone()) {
        Object value = getFutureValue(future);
        if (ATOMIC_HELPER.casValue(this, null, value)) {
          complete(
              this,
              /*
               * Interruption doesn't propagate through a SetFuture chain (see getFutureValue), so
               * don't invoke interruptTask.
               */
              false);
          return true;
        }
        return false;
      }
      SetFuture<V> valueToSet = new SetFuture<V>(this, future);
      if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
        try {
          future.addListener(valueToSet, DirectExecutor.INSTANCE);
        } catch (Throwable t) {
          Failure failure;
          try {
            failure = new Failure(t);
          } catch (Exception | Error oomMostLikely) { 
            failure = Failure.FALLBACK_INSTANCE;
          }
          boolean unused = ATOMIC_HELPER.casValue(this, valueToSet, failure);
        }
        return true;
      }
      localValue = value; 
    }
    if (localValue instanceof Cancellation) {
      future.cancel(((Cancellation) localValue).wasInterrupted);
    }
    return false;
  }

  /**
   * Returns a value that satisfies the contract of the {@link #value} field based on the state of
   * given future.
   *
   * <p>This is approximately the inverse of {@link #getDoneValue(Object)}
   */
  private static Object getFutureValue(ListenableFuture<?> future) {
    if (future instanceof Trusted) {
      Object v = ((AbstractFuture<?>) future).value;
      if (v instanceof Cancellation) {
        Cancellation c = (Cancellation) v;
        if (c.wasInterrupted) {
          v =
              c.cause != null
                  ? new Cancellation(/* wasInterrupted= */ false, c.cause)
                  : Cancellation.CAUSELESS_CANCELLED;
        }
      }
      return requireNonNull(v);
    }
    if (future instanceof InternalFutureFailureAccess) {
      Throwable throwable =
          InternalFutures.tryInternalFastPathGetFailure((InternalFutureFailureAccess) future);
      if (throwable != null) {
        return new Failure(throwable);
      }
    }
    boolean wasCancelled = future.isCancelled();
    if (!GENERATE_CANCELLATION_CAUSES & wasCancelled) {
      /*
       * requireNonNull is safe because we've initialized CAUSELESS_CANCELLED if
       * !GENERATE_CANCELLATION_CAUSES.
       */
      return requireNonNull(Cancellation.CAUSELESS_CANCELLED);
    }
    try {
      Object v = getUninterruptibly(future);
      if (wasCancelled) {
        return new Cancellation(
            false,
            new IllegalArgumentException(
                "get() did not throw CancellationException, despite reporting "
                    + "isCancelled() == true: "
                    + future));
      }
      return v == null ? NULL : v;
    } catch (ExecutionException exception) {
      if (wasCancelled) {
        return new Cancellation(
            false,
            new IllegalArgumentException(
                "get() did not throw CancellationException, despite reporting "
                    + "isCancelled() == true: "
                    + future,
                exception));
      }
      return new Failure(exception.getCause());
    } catch (CancellationException cancellation) {
      if (!wasCancelled) {
        return new Failure(
            new IllegalArgumentException(
                "get() threw CancellationException, despite reporting isCancelled() == false: "
                    + future,
                cancellation));
      }
      return new Cancellation(false, cancellation);
    } catch (Exception | Error t) { 
      return new Failure(t);
    }
  }

  /**
   * An inlined private copy of {@link Uninterruptibles#getUninterruptibly} used to break an
   * internal dependency on other /util/concurrent classes.
   */
  @ParametricNullness
  private static <V extends @Nullable Object> V getUninterruptibly(Future<V> future)
      throws ExecutionException {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return future.get();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Unblocks all threads and runs all listeners. */
  private static void complete(AbstractFuture<?> param, boolean callInterruptTask) {
    AbstractFuture<?> future = param;

    Listener next = null;
    outer:
    while (true) {
      future.releaseWaiters();
      /*
       * We call interruptTask() immediately before afterDone() so that migrating between the two
       * can be a no-op.
       */
      if (callInterruptTask) {
        future.interruptTask();
        /*
         * Interruption doesn't propagate through a SetFuture chain (see getFutureValue), so don't
         * invoke interruptTask on any subsequent futures.
         */
        callInterruptTask = false;
      }
      future.afterDone();
      next = future.clearListeners(next);
      future = null;
      while (next != null) {
        Listener curr = next;
        next = next.next;
        /*
         * requireNonNull is safe because the listener stack never contains TOMBSTONE until after
         * clearListeners.
         */
        Runnable task = requireNonNull(curr.task);
        if (task instanceof SetFuture) {
          SetFuture<?> setFuture = (SetFuture<?>) task;
          future = setFuture.owner;
          if (future.value == setFuture) {
            Object valueToSet = getFutureValue(setFuture.future);
            if (ATOMIC_HELPER.casValue(future, setFuture, valueToSet)) {
              continue outer;
            }
          }
        } else {
          /*
           * requireNonNull is safe because the listener stack never contains TOMBSTONE until after
           * clearListeners.
           */
          executeListener(task, requireNonNull(curr.executor));
        }
      }
      break;
    }
  }

  /**
   * Callback method that is called exactly once after the future is completed.
   *
   * <p>If {@link #interruptTask} is also run during completion, {@link #afterDone} runs after it.
   *
   * <p>The default implementation of this method in {@code AbstractFuture} does nothing. This is
   * intended for very lightweight cleanup work, for example, timing statistics or clearing fields.
   * If your task does anything heavier consider, just using a listener with an executor.
   *
   * @since 20.0
   */
  @ForOverride
  protected void afterDone() {}

  /**
   * Usually returns {@code null} but, if this {@code Future} has failed, may <i>optionally</i>
   * return the cause of the failure. "Failure" means specifically "completed with an exception"; it
   * does not include "was cancelled." To be explicit: If this method returns a non-null value,
   * then:
   *
   * <ul>
   *   <li>{@code isDone()} must return {@code true}
   *   <li>{@code isCancelled()} must return {@code false}
   *   <li>{@code get()} must not block, and it must throw an {@code ExecutionException} with the
   *       return value of this method as its cause
   * </ul>
   *
   * <p>This method is {@code protected} so that classes like {@code
   * com.google.common.util.concurrent.SettableFuture} do not expose it to their users as an
   * instance method. In the unlikely event that you need to call this method, call {@link
   * InternalFutures#tryInternalFastPathGetFailure(InternalFutureFailureAccess)}.
   *
   * @since 27.0
   */
  @Override
  /*
   * We should annotate the superclass, InternalFutureFailureAccess, to say that its copy of this
   * method returns @Nullable, too. However, we're not sure if we want to make any changes to that
   * class, since it's in a separate artifact that we planned to release only a single version of.
   */
  @CheckForNull
  protected final Throwable tryInternalFastPathGetFailure() {
    if (this instanceof Trusted) {
      Object obj = value;
      if (obj instanceof Failure) {
        return ((Failure) obj).exception;
      }
    }
    return null;
  }

  /**
   * If this future has been cancelled (and possibly interrupted), cancels (and possibly interrupts)
   * the given future (if available).
   */
  final void maybePropagateCancellationTo(@CheckForNull Future<?> related) {
    if (related != null & isCancelled()) {
      related.cancel(wasInterrupted());
    }
  }

  /** Releases all threads in the {@link #waiters} list, and clears the list. */
  private void releaseWaiters() {
    Waiter head = ATOMIC_HELPER.gasWaiters(this, Waiter.TOMBSTONE);
    for (Waiter currentWaiter = head; currentWaiter != null; currentWaiter = currentWaiter.next) {
      currentWaiter.unpark();
    }
  }

  /**
   * Clears the {@link #listeners} list and prepends its contents to {@code onto}, least recently
   * added first.
   */
  @CheckForNull
  private Listener clearListeners(@CheckForNull Listener onto) {
    Listener head = ATOMIC_HELPER.gasListeners(this, Listener.TOMBSTONE);
    Listener reversedList = onto;
    while (head != null) {
      Listener tmp = head;
      head = head.next;
      tmp.next = reversedList;
      reversedList = tmp;
    }
    return reversedList;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (getClass().getName().startsWith("com.google.common.util.concurrent.")) {
      builder.append(getClass().getSimpleName());
    } else {
      builder.append(getClass().getName());
    }
    builder.append('@').append(toHexString(identityHashCode(this))).append("[status=");
    if (isCancelled()) {
      builder.append("CANCELLED");
    } else if (isDone()) {
      addDoneString(builder);
    } else {
      addPendingString(builder); 
    }
    return builder.append("]").toString();
  }

  /**
   * Provide a human-readable explanation of why this future has not yet completed.
   *
   * @return null if an explanation cannot be provided (e.g. because the future is done).
   * @since 23.0
   */
  @CheckForNull
  protected String pendingToString() {
    if (this instanceof ScheduledFuture) {
      return "remaining delay=["
          + ((ScheduledFuture) this).getDelay(TimeUnit.MILLISECONDS)
          + " ms]";
    }
    return null;
  }

  @SuppressWarnings("CatchingUnchecked") 
  private void addPendingString(StringBuilder builder) {
    int truncateLength = builder.length();

    builder.append("PENDING");

    Object localValue = value;
    if (localValue instanceof SetFuture) {
      builder.append(", setFuture=[");
      appendUserObject(builder, ((SetFuture) localValue).future);
      builder.append("]");
    } else {
      String pendingDescription;
      try {
        pendingDescription = Strings.emptyToNull(pendingToString());
      } catch (Exception | StackOverflowError e) {
        pendingDescription = "Exception thrown from implementation: " + e.getClass();
      }
      if (pendingDescription != null) {
        builder.append(", info=[").append(pendingDescription).append("]");
      }
    }

    if (isDone()) {
      builder.delete(truncateLength, builder.length());
      addDoneString(builder);
    }
  }

  @SuppressWarnings("CatchingUnchecked") 
  private void addDoneString(StringBuilder builder) {
    try {
      V value = getUninterruptibly(this);
      builder.append("SUCCESS, result=[");
      appendResultObject(builder, value);
      builder.append("]");
    } catch (ExecutionException e) {
      builder.append("FAILURE, cause=[").append(e.getCause()).append("]");
    } catch (CancellationException e) {
      builder.append("CANCELLED"); 
    } catch (Exception e) { 
      builder.append("UNKNOWN, cause=[").append(e.getClass()).append(" thrown from get()]");
    }
  }

  /**
   * Any object can be the result of a Future, and not every object has a reasonable toString()
   * implementation. Using a reconstruction of the default Object.toString() prevents OOMs and stack
   * overflows, and helps avoid sensitive data inadvertently ending up in exception messages.
   */
  private void appendResultObject(StringBuilder builder, @CheckForNull Object o) {
    if (o == null) {
      builder.append("null");
    } else if (o == this) {
      builder.append("this future");
    } else {
      builder
          .append(o.getClass().getName())
          .append("@")
          .append(Integer.toHexString(System.identityHashCode(o)));
    }
  }

  /** Helper for printing user supplied objects into our toString method. */
  @SuppressWarnings("CatchingUnchecked") 
  private void appendUserObject(StringBuilder builder, @CheckForNull Object o) {
    try {
      if (o == this) {
        builder.append("this future");
      } else {
        builder.append(o);
      }
    } catch (Exception | StackOverflowError e) {
      builder.append("Exception thrown from implementation: ").append(e.getClass());
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

  private abstract static class AtomicHelper {
    /** Non-volatile write of the thread to the {@link Waiter#thread} field. */
    abstract void putThread(Waiter waiter, Thread newValue);

    /** Non-volatile write of the waiter to the {@link Waiter#next} field. */
    abstract void putNext(Waiter waiter, @CheckForNull Waiter newValue);

    /** Performs a CAS operation on the {@link #waiters} field. */
    abstract boolean casWaiters(
        AbstractFuture<?> future, @CheckForNull Waiter expect, @CheckForNull Waiter update);

    /** Performs a CAS operation on the {@link #listeners} field. */
    abstract boolean casListeners(
        AbstractFuture<?> future, @CheckForNull Listener expect, Listener update);

    /** Performs a GAS operation on the {@link #waiters} field. */
    abstract Waiter gasWaiters(AbstractFuture<?> future, Waiter update);

    /** Performs a GAS operation on the {@link #listeners} field. */
    abstract Listener gasListeners(AbstractFuture<?> future, Listener update);

    /** Performs a CAS operation on the {@link #value} field. */
    abstract boolean casValue(AbstractFuture<?> future, @CheckForNull Object expect, Object update);
  }

  /**
   * {@link AtomicHelper} based on {@link sun.misc.Unsafe}.
   *
   * <p>Static initialization of this class will fail if the {@link sun.misc.Unsafe} object cannot
   * be accessed.
   */
  @SuppressWarnings("sunapi")
  private static final class UnsafeAtomicHelper extends AtomicHelper {
    static final sun.misc.Unsafe UNSAFE;
    static final long LISTENERS_OFFSET;
    static final long WAITERS_OFFSET;
    static final long VALUE_OFFSET;
    static final long WAITER_THREAD_OFFSET;
    static final long WAITER_NEXT_OFFSET;

    static {
      sun.misc.Unsafe unsafe = null;
      try {
        unsafe = sun.misc.Unsafe.getUnsafe();
      } catch (SecurityException tryReflectionInstead) {
        try {
          unsafe =
              AccessController.doPrivileged(
                  new PrivilegedExceptionAction<sun.misc.Unsafe>() {
                    @Override
                    public sun.misc.Unsafe run() throws Exception {
                      Class<sun.misc.Unsafe> k = sun.misc.Unsafe.class;
                      for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object x = f.get(null);
                        if (k.isInstance(x)) {
                          return k.cast(x);
                        }
                      }
                      throw new NoSuchFieldError("the Unsafe");
                    }
                  });
        } catch (PrivilegedActionException e) {
          throw new RuntimeException("Could not initialize intrinsics", e.getCause());
        }
      }
      try {
        Class<?> abstractFuture = AbstractFuture.class;
        WAITERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("waiters"));
        LISTENERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("listeners"));
        VALUE_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("value"));
        WAITER_THREAD_OFFSET = unsafe.objectFieldOffset(Waiter.class.getDeclaredField("thread"));
        WAITER_NEXT_OFFSET = unsafe.objectFieldOffset(Waiter.class.getDeclaredField("next"));
        UNSAFE = unsafe;
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    void putThread(Waiter waiter, Thread newValue) {
      UNSAFE.putObject(waiter, WAITER_THREAD_OFFSET, newValue);
    }

    @Override
    void putNext(Waiter waiter, @CheckForNull Waiter newValue) {
      UNSAFE.putObject(waiter, WAITER_NEXT_OFFSET, newValue);
    }

    /** Performs a CAS operation on the {@link #waiters} field. */
    @Override
    boolean casWaiters(
        AbstractFuture<?> future, @CheckForNull Waiter expect, @CheckForNull Waiter update) {
      return UNSAFE.compareAndSwapObject(future, WAITERS_OFFSET, expect, update);
    }

    /** Performs a CAS operation on the {@link #listeners} field. */
    @Override
    boolean casListeners(AbstractFuture<?> future, @CheckForNull Listener expect, Listener update) {
      return UNSAFE.compareAndSwapObject(future, LISTENERS_OFFSET, expect, update);
    }

    /** Performs a GAS operation on the {@link #listeners} field. */
    @Override
    Listener gasListeners(AbstractFuture<?> future, Listener update) {
      return (Listener) UNSAFE.getAndSetObject(future, LISTENERS_OFFSET, update);
    }

    /** Performs a GAS operation on the {@link #waiters} field. */
    @Override
    Waiter gasWaiters(AbstractFuture<?> future, Waiter update) {
      return (Waiter) UNSAFE.getAndSetObject(future, WAITERS_OFFSET, update);
    }

    /** Performs a CAS operation on the {@link #value} field. */
    @Override
    boolean casValue(AbstractFuture<?> future, @CheckForNull Object expect, Object update) {
      return UNSAFE.compareAndSwapObject(future, VALUE_OFFSET, expect, update);
    }
  }

  /** {@link AtomicHelper} based on {@link AtomicReferenceFieldUpdater}. */
  @SuppressWarnings("rawtypes")
  private static final class SafeAtomicHelper extends AtomicHelper {
    final AtomicReferenceFieldUpdater<Waiter, Thread> waiterThreadUpdater;
    final AtomicReferenceFieldUpdater<Waiter, Waiter> waiterNextUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Waiter> waitersUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Listener> listenersUpdater;
    final AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater;

    SafeAtomicHelper(
        AtomicReferenceFieldUpdater<Waiter, Thread> waiterThreadUpdater,
        AtomicReferenceFieldUpdater<Waiter, Waiter> waiterNextUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Waiter> waitersUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Listener> listenersUpdater,
        AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater) {
      this.waiterThreadUpdater = waiterThreadUpdater;
      this.waiterNextUpdater = waiterNextUpdater;
      this.waitersUpdater = waitersUpdater;
      this.listenersUpdater = listenersUpdater;
      this.valueUpdater = valueUpdater;
    }

    @Override
    void putThread(Waiter waiter, Thread newValue) {
      waiterThreadUpdater.lazySet(waiter, newValue);
    }

    @Override
    void putNext(Waiter waiter, @CheckForNull Waiter newValue) {
      waiterNextUpdater.lazySet(waiter, newValue);
    }

    @Override
    boolean casWaiters(
        AbstractFuture<?> future, @CheckForNull Waiter expect, @CheckForNull Waiter update) {
      return waitersUpdater.compareAndSet(future, expect, update);
    }

    @Override
    boolean casListeners(AbstractFuture<?> future, @CheckForNull Listener expect, Listener update) {
      return listenersUpdater.compareAndSet(future, expect, update);
    }

    /** Performs a GAS operation on the {@link #listeners} field. */
    @Override
    Listener gasListeners(AbstractFuture<?> future, Listener update) {
      return listenersUpdater.getAndSet(future, update);
    }

    /** Performs a GAS operation on the {@link #waiters} field. */
    @Override
    Waiter gasWaiters(AbstractFuture<?> future, Waiter update) {
      return waitersUpdater.getAndSet(future, update);
    }

    @Override
    boolean casValue(AbstractFuture<?> future, @CheckForNull Object expect, Object update) {
      return valueUpdater.compareAndSet(future, expect, update);
    }
  }

  /**
   * {@link AtomicHelper} based on {@code synchronized} and volatile writes.
   *
   * <p>This is an implementation of last resort for when certain basic VM features are broken (like
   * AtomicReferenceFieldUpdater).
   */
  private static final class SynchronizedHelper extends AtomicHelper {
    @Override
    void putThread(Waiter waiter, Thread newValue) {
      waiter.thread = newValue;
    }

    @Override
    void putNext(Waiter waiter, @CheckForNull Waiter newValue) {
      waiter.next = newValue;
    }

    @Override
    boolean casWaiters(
        AbstractFuture<?> future, @CheckForNull Waiter expect, @CheckForNull Waiter update) {
      synchronized (future) {
        if (future.waiters == expect) {
          future.waiters = update;
          return true;
        }
        return false;
      }
    }

    @Override
    boolean casListeners(AbstractFuture<?> future, @CheckForNull Listener expect, Listener update) {
      synchronized (future) {
        if (future.listeners == expect) {
          future.listeners = update;
          return true;
        }
        return false;
      }
    }

    /** Performs a GAS operation on the {@link #listeners} field. */
    @Override
    Listener gasListeners(AbstractFuture<?> future, Listener update) {
      synchronized (future) {
        Listener old = future.listeners;
        if (old != update) {
          future.listeners = update;
        }
        return old;
      }
    }

    /** Performs a GAS operation on the {@link #waiters} field. */
    @Override
    Waiter gasWaiters(AbstractFuture<?> future, Waiter update) {
      synchronized (future) {
        Waiter old = future.waiters;
        if (old != update) {
          future.waiters = update;
        }
        return old;
      }
    }

    @Override
    boolean casValue(AbstractFuture<?> future, @CheckForNull Object expect, Object update) {
      synchronized (future) {
        if (future.value == expect) {
          future.value = update;
          return true;
        }
        return false;
      }
    }
  }

  private static CancellationException cancellationExceptionWithCause(
      String message, @CheckForNull Throwable cause) {
    CancellationException exception = new CancellationException(message);
    exception.initCause(cause);
    return exception;
  }
}
