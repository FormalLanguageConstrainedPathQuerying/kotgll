/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.util.concurrent;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.util.concurrent.RateLimiter.SleepingStopwatch;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.mockito.Mockito;

/**
 * Tests for RateLimiter.
 *
 * @author Dimitris Andreou
 */
public class RateLimiterTest extends TestCase {
  private static final double EPSILON = 1e-8;

  private final FakeStopwatch stopwatch = new FakeStopwatch();

  public void testSimple() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    limiter.acquire(); 
    limiter.acquire(); 
    limiter.acquire(); 
    assertEvents("R0.00", "R0.20", "R0.20");
  }

  public void testImmediateTryAcquire() {
    RateLimiter r = RateLimiter.create(1);
    assertTrue("Unable to acquire initial permit", r.tryAcquire());
    assertFalse("Capable of acquiring secondary permit", r.tryAcquire());
  }

  public void testDoubleMinValueCanAcquireExactlyOnce() {
    RateLimiter r = RateLimiter.create(Double.MIN_VALUE, stopwatch);
    assertTrue("Unable to acquire initial permit", r.tryAcquire());
    assertFalse("Capable of acquiring an additional permit", r.tryAcquire());
    stopwatch.sleepMillis(Integer.MAX_VALUE);
    assertFalse("Capable of acquiring an additional permit after sleeping", r.tryAcquire());
  }

  public void testSimpleRateUpdate() {
    RateLimiter limiter = RateLimiter.create(5.0, 5, SECONDS);
    assertEquals(5.0, limiter.getRate());
    limiter.setRate(10.0);
    assertEquals(10.0, limiter.getRate());

    assertThrows(IllegalArgumentException.class, () -> limiter.setRate(0.0));
    assertThrows(IllegalArgumentException.class, () -> limiter.setRate(-10.0));
  }

  public void testAcquireParameterValidation() {
    RateLimiter limiter = RateLimiter.create(999);
    assertThrows(IllegalArgumentException.class, () -> limiter.acquire(0));
    assertThrows(IllegalArgumentException.class, () -> limiter.acquire(-1));
    assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0));
    assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1));
    assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0, 1, SECONDS));
    assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1, 1, SECONDS));
  }

  public void testSimpleWithWait() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    limiter.acquire(); 
    stopwatch.sleepMillis(200); 
    limiter.acquire(); 
    limiter.acquire(); 
    assertEvents("R0.00", "U0.20", "R0.00", "R0.20");
  }

  public void testSimpleAcquireReturnValues() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertEquals(0.0, limiter.acquire(), EPSILON); 
    stopwatch.sleepMillis(200); 
    assertEquals(0.0, limiter.acquire(), EPSILON); 
    assertEquals(0.2, limiter.acquire(), EPSILON); 
    assertEvents("R0.00", "U0.20", "R0.00", "R0.20");
  }

  public void testSimpleAcquireEarliestAvailableIsInPast() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertEquals(0.0, limiter.acquire(), EPSILON);
    stopwatch.sleepMillis(400);
    assertEquals(0.0, limiter.acquire(), EPSILON);
    assertEquals(0.0, limiter.acquire(), EPSILON);
    assertEquals(0.2, limiter.acquire(), EPSILON);
  }

  public void testOneSecondBurst() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    stopwatch.sleepMillis(1000); 
    stopwatch.sleepMillis(1000); 
    limiter.acquire(1); 

    limiter.acquire(1); 
    limiter.acquire(3); 
    limiter.acquire(1); 

    limiter.acquire(); 
    assertEvents(
        "U1.00", "U1.00", "R0.00", "R0.00", "R0.00", "R0.00", 
        "R0.20");
  }

  public void testCreateWarmupParameterValidation() {
    RateLimiter unused;
    unused = RateLimiter.create(1.0, 1, NANOSECONDS);
    unused = RateLimiter.create(1.0, 0, NANOSECONDS);

    assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0, 1, NANOSECONDS));

    assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(1.0, -1, NANOSECONDS));
  }

  @AndroidIncompatible 
  public void testWarmUp() {
    RateLimiter limiter = RateLimiter.create(2.0, 4000, MILLISECONDS, 3.0, stopwatch);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(500); 
    stopwatch.sleepMillis(4000); 
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(500); 
    stopwatch.sleepMillis(2000); 
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    assertEvents(
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", 
        "U0.50", 
        "U4.00", 
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", 
        "U0.50", 
        "U2.00", 
        "R0.00, R0.50, R0.50, R0.50, R0.50, R0.50, R0.50, R0.50"); 
  }

  public void testWarmUpWithColdFactor() {
    RateLimiter limiter = RateLimiter.create(5.0, 4000, MILLISECONDS, 10.0, stopwatch);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(200); 
    stopwatch.sleepMillis(4000); 
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(200); 
    stopwatch.sleepMillis(1000); 
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    assertEvents(
        "R0.00, R1.75, R1.26, R0.76, R0.30, R0.20, R0.20, R0.20", 
        "U0.20", 
        "U4.00", 
        "R0.00, R1.75, R1.26, R0.76, R0.30, R0.20, R0.20, R0.20", 
        "U0.20", 
        "U1.00", 
        "R0.00, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20"); 
  }

  public void testWarmUpWithColdFactor1() {
    RateLimiter limiter = RateLimiter.create(5.0, 4000, MILLISECONDS, 1.0, stopwatch);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(340); 
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    assertEvents(
        "R0.00, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20", 
        "U0.34", 
        "R0.00, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20, R0.20"); 
  }

  @AndroidIncompatible 
  public void testWarmUpAndUpdate() {
    RateLimiter limiter = RateLimiter.create(2.0, 4000, MILLISECONDS, 3.0, stopwatch);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(4500); 
    for (int i = 0; i < 3; i++) { 
      limiter.acquire(); 
    }

    limiter.setRate(4.0); 
    limiter.acquire(); 
    for (int i = 0; i < 4; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(4250); 
    for (int i = 0; i < 11; i++) {
      limiter.acquire(); 
    }

    assertEvents(
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", 
        "U4.50", 
        "R0.00, R1.38, R1.13", 
        "R0.88", 
        "R0.34, R0.28, R0.25, R0.25", 
        "U4.25", 
        "R0.00, R0.72, R0.66, R0.59, R0.53, R0.47, R0.41", 
        "R0.34, R0.28, R0.25, R0.25"); 
  }

  public void testWarmUpAndUpdateWithColdFactor() {
    RateLimiter limiter = RateLimiter.create(5.0, 4000, MILLISECONDS, 10.0, stopwatch);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(4200); 
    for (int i = 0; i < 3; i++) { 
      limiter.acquire(); 
    }

    limiter.setRate(10.0); 
    limiter.acquire(); 
    for (int i = 0; i < 4; i++) {
      limiter.acquire(); 
    }
    stopwatch.sleepMillis(4100); 
    for (int i = 0; i < 11; i++) {
      limiter.acquire(); 
    }

    assertEvents(
        "R0.00, R1.75, R1.26, R0.76, R0.30, R0.20, R0.20, R0.20", 
        "U4.20", 
        "R0.00, R1.75, R1.26", 
        "R0.76", 
        "R0.20, R0.10, R0.10, R0.10", 
        "U4.10", 
        "R0.00, R0.94, R0.81, R0.69, R0.57, R0.44, R0.32", 
        "R0.20, R0.10, R0.10, R0.10"); 
  }

  public void testBurstyAndUpdate() {
    RateLimiter rateLimiter = RateLimiter.create(1.0, stopwatch);
    rateLimiter.acquire(1); 
    rateLimiter.acquire(1); 

    rateLimiter.setRate(2.0); 

    rateLimiter.acquire(1); 
    rateLimiter.acquire(2); 
    rateLimiter.acquire(4); 
    rateLimiter.acquire(1); 
    assertEvents("R0.00", "R1.00", "R1.00", "R0.50", "R1.00", "R2.00");
  }

  public void testTryAcquire_noWaitAllowed() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertTrue(limiter.tryAcquire(0, SECONDS));
    assertFalse(limiter.tryAcquire(0, SECONDS));
    assertFalse(limiter.tryAcquire(0, SECONDS));
    stopwatch.sleepMillis(100);
    assertFalse(limiter.tryAcquire(0, SECONDS));
  }

  public void testTryAcquire_someWaitAllowed() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertTrue(limiter.tryAcquire(0, SECONDS));
    assertTrue(limiter.tryAcquire(200, MILLISECONDS));
    assertFalse(limiter.tryAcquire(100, MILLISECONDS));
    stopwatch.sleepMillis(100);
    assertTrue(limiter.tryAcquire(100, MILLISECONDS));
  }

  public void testTryAcquire_overflow() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertTrue(limiter.tryAcquire(0, MICROSECONDS));
    stopwatch.sleepMillis(100);
    assertTrue(limiter.tryAcquire(Long.MAX_VALUE, MICROSECONDS));
  }

  public void testTryAcquire_negative() {
    RateLimiter limiter = RateLimiter.create(5.0, stopwatch);
    assertTrue(limiter.tryAcquire(5, 0, SECONDS));
    stopwatch.sleepMillis(900);
    assertFalse(limiter.tryAcquire(1, Long.MIN_VALUE, SECONDS));
    stopwatch.sleepMillis(100);
    assertTrue(limiter.tryAcquire(1, -1, SECONDS));
  }

  public void testSimpleWeights() {
    RateLimiter rateLimiter = RateLimiter.create(1.0, stopwatch);
    rateLimiter.acquire(1); 
    rateLimiter.acquire(1); 
    rateLimiter.acquire(2); 
    rateLimiter.acquire(4); 
    rateLimiter.acquire(8); 
    rateLimiter.acquire(1); 
    assertEvents("R0.00", "R1.00", "R1.00", "R2.00", "R4.00", "R8.00");
  }

  public void testInfinity_Bursty() {
    RateLimiter limiter = RateLimiter.create(Double.POSITIVE_INFINITY, stopwatch);
    limiter.acquire(Integer.MAX_VALUE / 4);
    limiter.acquire(Integer.MAX_VALUE / 2);
    limiter.acquire(Integer.MAX_VALUE);
    assertEvents("R0.00", "R0.00", "R0.00"); 

    limiter.setRate(2.0);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents(
        "R0.00", 
        "R0.00", "R0.00", 
        "R0.50", 
        "R0.50");

    limiter.setRate(Double.POSITIVE_INFINITY);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R0.50", "R0.00", "R0.00"); 
  }

  /** https:
  public void testInfinity_BustyTimeElapsed() {
    RateLimiter limiter = RateLimiter.create(Double.POSITIVE_INFINITY, stopwatch);
    stopwatch.instant += 1000000;
    limiter.setRate(2.0);
    for (int i = 0; i < 5; i++) {
      limiter.acquire();
    }
    assertEvents(
        "R0.00", 
        "R0.00", "R0.00", 
        "R0.50", 
        "R0.50");
  }

  public void testInfinity_WarmUp() {
    RateLimiter limiter = RateLimiter.create(Double.POSITIVE_INFINITY, 10, SECONDS, 3.0, stopwatch);
    limiter.acquire(Integer.MAX_VALUE / 4);
    limiter.acquire(Integer.MAX_VALUE / 2);
    limiter.acquire(Integer.MAX_VALUE);
    assertEvents("R0.00", "R0.00", "R0.00");

    limiter.setRate(1.0);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R0.00", "R1.00", "R1.00");

    limiter.setRate(Double.POSITIVE_INFINITY);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R1.00", "R0.00", "R0.00");
  }

  public void testInfinity_WarmUpTimeElapsed() {
    RateLimiter limiter = RateLimiter.create(Double.POSITIVE_INFINITY, 10, SECONDS, 3.0, stopwatch);
    stopwatch.instant += 1000000;
    limiter.setRate(1.0);
    for (int i = 0; i < 5; i++) {
      limiter.acquire();
    }
    assertEvents("R0.00", "R1.00", "R1.00", "R1.00", "R1.00");
  }

  /**
   * Make sure that bursts can never go above 1-second-worth-of-work for the current rate, even when
   * we change the rate.
   */
  public void testWeNeverGetABurstMoreThanOneSec() {
    RateLimiter limiter = RateLimiter.create(1.0, stopwatch);
    int[] rates = {1000, 1, 10, 1000000, 10, 1};
    for (int rate : rates) {
      int oneSecWorthOfWork = rate;
      stopwatch.sleepMillis(rate * 1000);
      limiter.setRate(rate);
      long burst = measureTotalTimeMillis(limiter, oneSecWorthOfWork, new Random());
      assertTrue(burst <= 1000);
      long afterBurst = measureTotalTimeMillis(limiter, oneSecWorthOfWork, new Random());
      assertTrue(afterBurst >= 1000);
    }
  }

  /**
   * This neat test shows that no matter what weights we use in our requests, if we push X amount of
   * permits in a cool state, where X = rate * timeToCoolDown, and we have specified a
   * timeToWarmUp() period, it will cost as the prescribed amount of time. E.g., calling
   * [acquire(5), acquire(1)] takes exactly the same time as [acquire(2), acquire(3), acquire(1)].
   */
  public void testTimeToWarmUpIsHonouredEvenWithWeights() {
    Random random = new Random();
    int warmupPermits = 10;
    double[] coldFactorsToTest = {2.0, 3.0, 10.0};
    double[] qpsToTest = {4.0, 2.0, 1.0, 0.5, 0.1};
    for (int trial = 0; trial < 100; trial++) {
      for (double coldFactor : coldFactorsToTest) {
        for (double qps : qpsToTest) {
          long warmupMillis = (long) ((1 + coldFactor) * warmupPermits / (2.0 * qps) * 1000.0);
          RateLimiter rateLimiter =
              RateLimiter.create(qps, warmupMillis, MILLISECONDS, coldFactor, stopwatch);
          assertEquals(warmupMillis, measureTotalTimeMillis(rateLimiter, warmupPermits, random));
        }
      }
    }
  }

  public void testNulls() {
    NullPointerTester tester =
        new NullPointerTester()
            .setDefault(SleepingStopwatch.class, stopwatch)
            .setDefault(int.class, 1)
            .setDefault(double.class, 1.0d);
    tester.testStaticMethods(RateLimiter.class, Visibility.PACKAGE);
    tester.testInstanceMethods(RateLimiter.create(5.0, stopwatch), Visibility.PACKAGE);
  }

  public void testVerySmallDoubleValues() throws Exception {
    RateLimiter rateLimiter = RateLimiter.create(Double.MIN_VALUE, stopwatch);
    assertTrue("Should acquire initial permit", rateLimiter.tryAcquire());
    assertFalse("Should not acquire additional permit", rateLimiter.tryAcquire());
    stopwatch.sleepMillis(5000);
    assertFalse(
        "Should not acquire additional permit even after sleeping", rateLimiter.tryAcquire());
  }

  private long measureTotalTimeMillis(RateLimiter rateLimiter, int permits, Random random) {
    long startTime = stopwatch.instant;
    while (permits > 0) {
      int nextPermitsToAcquire = Math.max(1, random.nextInt(permits));
      permits -= nextPermitsToAcquire;
      rateLimiter.acquire(nextPermitsToAcquire);
    }
    rateLimiter.acquire(1); 
    return NANOSECONDS.toMillis(stopwatch.instant - startTime);
  }

  private void assertEvents(String... events) {
    assertEquals(Arrays.toString(events), stopwatch.readEventsAndClear());
  }

  /**
   * The stopwatch gathers events and presents them as strings. R0.6 means a delay of 0.6 seconds
   * caused by the (R)ateLimiter U1.0 means the (U)ser caused the stopwatch to sleep for a second.
   */
  static class FakeStopwatch extends SleepingStopwatch {
    long instant = 0L;
    final List<String> events = Lists.newArrayList();

    @Override
    public long readMicros() {
      return NANOSECONDS.toMicros(instant);
    }

    void sleepMillis(int millis) {
      sleepMicros("U", MILLISECONDS.toMicros(millis));
    }

    void sleepMicros(String caption, long micros) {
      instant += MICROSECONDS.toNanos(micros);
      events.add(caption + String.format(Locale.ROOT, "%3.2f", (micros / 1000000.0)));
    }

    @Override
    protected void sleepMicrosUninterruptibly(long micros) {
      sleepMicros("R", micros);
    }

    String readEventsAndClear() {
      try {
        return events.toString();
      } finally {
        events.clear();
      }
    }

    @Override
    public String toString() {
      return events.toString();
    }
  }

  @AndroidIncompatible 
  public void testMockingMockito() throws Exception {
    RateLimiter mock = Mockito.mock(RateLimiter.class);
    for (Method method : RateLimiter.class.getMethods()) {
      if (!isStatic(method.getModifiers())
          && !NOT_WORKING_ON_MOCKS.contains(method.getName())
          && !method.getDeclaringClass().equals(Object.class)) {
        method.invoke(mock, arbitraryParameters(method));
      }
    }
  }

  private static Object[] arbitraryParameters(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    Object[] params = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      params[i] = PARAMETER_VALUES.get(parameterTypes[i]);
    }
    return params;
  }

  private static final ImmutableSet<String> NOT_WORKING_ON_MOCKS =
      ImmutableSet.of("latestPermitAgeSec", "latestPermitAge", "setRate", "getAvailablePermits");

  private static final ImmutableClassToInstanceMap<Object> PARAMETER_VALUES =
      ImmutableClassToInstanceMap.builder()
          .put(int.class, 1)
          .put(long.class, 1L)
          .put(double.class, 1.0)
          .put(TimeUnit.class, SECONDS)
          .build();
}
