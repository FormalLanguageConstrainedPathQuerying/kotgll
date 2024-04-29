/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Stopwatch;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link Queues}.
 *
 * @author Dimitris Andreou
 */
public class QueuesTest extends TestCase {
  /*
   * All the following tests relate to BlockingQueue methods in Queues.
   */

  public static List<BlockingQueue<Object>> blockingQueues() {
    return ImmutableList.<BlockingQueue<Object>>of(
        new LinkedBlockingQueue<Object>(),
        new LinkedBlockingQueue<Object>(10),
        new SynchronousQueue<Object>(),
        new ArrayBlockingQueue<Object>(10),
        new LinkedBlockingDeque<Object>(),
        new LinkedBlockingDeque<Object>(10),
        new PriorityBlockingQueue<Object>(10, Ordering.arbitrary()));
  }

  /*
   * We need to perform operations in a thread pool, even for simple cases, because the queue might
   * be a SynchronousQueue.
   */
  private ExecutorService threadPool;

  @Override
  public void setUp() {
    threadPool = newCachedThreadPool();
  }

  @Override
  public void tearDown() throws InterruptedException {
    threadPool.shutdown();
    assertTrue("Some worker didn't finish in time", threadPool.awaitTermination(10, SECONDS));
  }

  private static <T> int drain(
      BlockingQueue<T> q,
      Collection<? super T> buffer,
      int maxElements,
      long timeout,
      TimeUnit unit,
      boolean interruptibly)
      throws InterruptedException {
    return interruptibly
        ? Queues.drain(q, buffer, maxElements, timeout, unit)
        : Queues.drainUninterruptibly(q, buffer, maxElements, timeout, unit);
  }

  public void testMultipleProducers() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testMultipleProducers(q);
    }
  }

  private void testMultipleProducers(BlockingQueue<Object> q) throws InterruptedException {
    for (boolean interruptibly : new boolean[] {true, false}) {
      @SuppressWarnings("unused") 
      Future<?> possiblyIgnoredError = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") 
      Future<?> possiblyIgnoredError1 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") 
      Future<?> possiblyIgnoredError2 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") 
      Future<?> possiblyIgnoredError3 = threadPool.submit(new Producer(q, 20));
      @SuppressWarnings("unused") 
      Future<?> possiblyIgnoredError4 = threadPool.submit(new Producer(q, 20));

      List<Object> buf = newArrayList();
      int elements = drain(q, buf, 100, MAX_VALUE, NANOSECONDS, interruptibly);
      assertEquals(100, elements);
      assertEquals(100, buf.size());
      assertDrained(q);
    }
  }

  public void testDrainTimesOut() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrainTimesOut(q);
    }
  }

  private void testDrainTimesOut(BlockingQueue<Object> q) throws Exception {
    for (boolean interruptibly : new boolean[] {true, false}) {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 1, 10, MILLISECONDS));

      Producer producer = new Producer(q, 1);
      Future<?> producerThread = threadPool.submit(producer);
      producer.beganProducing.await();

      Stopwatch timer = Stopwatch.createStarted();

      int drained = drain(q, newArrayList(), 2, 10, MILLISECONDS, interruptibly);
      assertThat(drained).isAtMost(1);

      assertThat(timer.elapsed(MILLISECONDS)).isAtLeast(10L);

      producerThread.cancel(true);
      producer.doneProducing.await();
      if (drained == 0) {
        q.poll(); 
      }
    }
  }

  public void testZeroElements() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testZeroElements(q);
    }
  }

  private void testZeroElements(BlockingQueue<Object> q) throws InterruptedException {
    for (boolean interruptibly : new boolean[] {true, false}) {
      assertEquals(0, drain(q, ImmutableList.of(), 0, 10, MILLISECONDS, interruptibly));
    }
  }

  public void testEmpty() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testEmpty(q);
    }
  }

  private void testEmpty(BlockingQueue<Object> q) {
    assertDrained(q);
  }

  public void testNegativeMaxElements() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testNegativeMaxElements(q);
    }
  }

  private void testNegativeMaxElements(BlockingQueue<Object> q) throws InterruptedException {
    @SuppressWarnings("unused") 
    Future<?> possiblyIgnoredError = threadPool.submit(new Producer(q, 1));

    List<Object> buf = newArrayList();
    int elements = Queues.drain(q, buf, -1, MAX_VALUE, NANOSECONDS);
    assertEquals(0, elements);
    assertThat(buf).isEmpty();

    q.take();
  }

  public void testDrain_throws() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrain_throws(q);
    }
  }

  private void testDrain_throws(BlockingQueue<Object> q) {
    @SuppressWarnings("unused") 
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));
    try {
      Queues.drain(q, ImmutableList.of(), 100, MAX_VALUE, NANOSECONDS);
      fail();
    } catch (InterruptedException expected) {
    }
  }

  public void testDrainUninterruptibly_doesNotThrow() throws Exception {
    for (BlockingQueue<Object> q : blockingQueues()) {
      testDrainUninterruptibly_doesNotThrow(q);
    }
  }

  private void testDrainUninterruptibly_doesNotThrow(final BlockingQueue<Object> q) {
    final Thread mainThread = currentThread();
    @SuppressWarnings("unused") 
    Future<?> possiblyIgnoredError =
        threadPool.submit(
            new Callable<@Nullable Void>() {
              @Override
              public @Nullable Void call() throws InterruptedException {
                new Producer(q, 50).call();
                new Interrupter(mainThread).run();
                new Producer(q, 50).call();
                return null;
              }
            });
    List<Object> buf = newArrayList();
    int elements = Queues.drainUninterruptibly(q, buf, 100, MAX_VALUE, NANOSECONDS);
    assertTrue(Thread.interrupted());
    assertEquals(100, elements);
    assertEquals(100, buf.size());
  }

  public void testNewLinkedBlockingDequeCapacity() {
    assertThrows(IllegalArgumentException.class, () -> Queues.newLinkedBlockingDeque(0));
    assertEquals(1, Queues.newLinkedBlockingDeque(1).remainingCapacity());
    assertEquals(11, Queues.newLinkedBlockingDeque(11).remainingCapacity());
  }

  public void testNewLinkedBlockingQueueCapacity() {
    assertThrows(IllegalArgumentException.class, () -> Queues.newLinkedBlockingQueue(0));
    assertEquals(1, Queues.newLinkedBlockingQueue(1).remainingCapacity());
    assertEquals(11, Queues.newLinkedBlockingQueue(11).remainingCapacity());
  }

  /** Checks that #drain() invocations behave correctly for a drained (empty) queue. */
  private void assertDrained(BlockingQueue<Object> q) {
    assertNull(q.peek());
    assertInterruptibleDrained(q);
    assertUninterruptibleDrained(q);
  }

  private void assertInterruptibleDrained(BlockingQueue<Object> q) {
    try {
      assertEquals(0, Queues.drain(q, ImmutableList.of(), 0, 10, MILLISECONDS));
    } catch (InterruptedException e) {
      throw new AssertionError();
    }

    @SuppressWarnings("unused") 
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));
    try {
      Queues.drain(q, newArrayList(), 1, MAX_VALUE, NANOSECONDS);
      fail();
    } catch (InterruptedException expected) {
    }
  }

  private void assertUninterruptibleDrained(BlockingQueue<Object> q) {
    assertEquals(0, Queues.drainUninterruptibly(q, ImmutableList.of(), 0, 10, MILLISECONDS));

    @SuppressWarnings("unused") 
    Future<?> possiblyIgnoredError = threadPool.submit(new Interrupter(currentThread()));

    Stopwatch timer = Stopwatch.createStarted();
    Queues.drainUninterruptibly(q, newArrayList(), 1, 10, MILLISECONDS);
    assertThat(timer.elapsed(MILLISECONDS)).isAtLeast(10L);
    while (!Thread.interrupted()) {
      Thread.yield();
    }
  }

  private static class Producer implements Callable<@Nullable Void> {
    final BlockingQueue<Object> q;
    final int elements;
    final CountDownLatch beganProducing = new CountDownLatch(1);
    final CountDownLatch doneProducing = new CountDownLatch(1);

    Producer(BlockingQueue<Object> q, int elements) {
      this.q = q;
      this.elements = elements;
    }

    @Override
    public @Nullable Void call() throws InterruptedException {
      try {
        beganProducing.countDown();
        for (int i = 0; i < elements; i++) {
          q.put(new Object());
        }
        return null;
      } finally {
        doneProducing.countDown();
      }
    }
  }

  private static class Interrupter implements Runnable {
    final Thread threadToInterrupt;

    Interrupter(Thread threadToInterrupt) {
      this.threadToInterrupt = threadToInterrupt;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new AssertionError();
      } finally {
        threadToInterrupt.interrupt();
      }
    }
  }
}
