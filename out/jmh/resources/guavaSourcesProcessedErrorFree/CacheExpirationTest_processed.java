/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.cache.TestingCacheLoaders.IdentityLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.cache.TestingRemovalListeners.QueuingRemovalListener;
import com.google.common.collect.Iterators;
import com.google.common.testing.FakeTicker;
import com.google.common.util.concurrent.Callables;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Tests relating to cache expiration: make sure entries expire at the right times, make sure
 * expired entries don't show up, etc.
 *
 * @author mike nonemacher
 */
@SuppressWarnings("deprecation") 
public class CacheExpirationTest extends TestCase {

  private static final long EXPIRING_TIME = 1000;
  private static final int VALUE_PREFIX = 12345;
  private static final String KEY_PREFIX = "key prefix:";

  public void testExpiration_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    checkExpiration(cache, loader, ticker, removalListener);
  }

  public void testExpiration_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    checkExpiration(cache, loader, ticker, removalListener);
  }

  private void checkExpiration(
      LoadingCache<String, Integer> cache,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      CountingRemovalListener<String, Integer> removalListener) {

    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
    }

    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertFalse("Creator should not have been called @#" + i, loader.wasCalled());
    }

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);

    assertEquals("Map must be empty by now", 0, cache.size());
    assertEquals("Eviction notifications must be received", 10, removalListener.getCount());

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    assertEquals("Eviction notifications must be received", 10, removalListener.getCount());
  }

  public void testExpiringGet_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runExpirationTest(cache, loader, ticker, removalListener);
  }

  public void testExpiringGet_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runExpirationTest(cache, loader, ticker, removalListener);
  }

  private void runExpirationTest(
      LoadingCache<String, Integer> cache,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      CountingRemovalListener<String, Integer> removalListener) {

    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
    }

    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertFalse("Loader should NOT have been called @#" + i, loader.wasCalled());
    }

    ticker.advance(EXPIRING_TIME * 10, MILLISECONDS);

    cache.getUnchecked(KEY_PREFIX + 11);

    assertEquals(1, Iterators.size(cache.asMap().entrySet().iterator()));
    assertEquals(1, Iterators.size(cache.asMap().keySet().iterator()));
    assertEquals(1, Iterators.size(cache.asMap().values().iterator()));

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);

    for (int i = 0; i < 11; i++) {
      assertFalse(cache.asMap().containsKey(KEY_PREFIX + i));
    }
    assertEquals(11, removalListener.getCount());

    for (int i = 0; i < 10; i++) {
      assertFalse(cache.asMap().containsKey(KEY_PREFIX + i));
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertTrue("Creator should have been called @#" + i, loader.wasCalled());
    }

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    assertEquals("Eviction notifications must be received", 21, removalListener.getCount());

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    assertEquals("Eviction notifications must be received", 21, removalListener.getCount());
  }

  public void testRemovalListener_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    final AtomicInteger evictionCount = new AtomicInteger();
    final AtomicInteger applyCount = new AtomicInteger();
    final AtomicInteger totalSum = new AtomicInteger();

    RemovalListener<Integer, AtomicInteger> removalListener =
        new RemovalListener<Integer, AtomicInteger>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, AtomicInteger> notification) {
            if (notification.wasEvicted()) {
              evictionCount.incrementAndGet();
              totalSum.addAndGet(notification.getValue().get());
            }
          }
        };

    CacheLoader<Integer, AtomicInteger> loader =
        new CacheLoader<Integer, AtomicInteger>() {
          @Override
          public AtomicInteger load(Integer key) {
            applyCount.incrementAndGet();
            return new AtomicInteger();
          }
        };

    LoadingCache<Integer, AtomicInteger> cache =
        CacheBuilder.newBuilder()
            .removalListener(removalListener)
            .expireAfterWrite(10, MILLISECONDS)
            .ticker(ticker)
            .build(loader);

    for (int i = 0; i < 100; ++i) {
      cache.getUnchecked(10).incrementAndGet();
      ticker.advance(1, MILLISECONDS);
    }

    assertEquals(evictionCount.get() + 1, applyCount.get());
    int remaining = cache.getUnchecked(10).get();
    assertEquals(100, totalSum.get() + remaining);
  }

  public void testRemovalScheduler_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testRemovalScheduler_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testRemovalScheduler_expireAfterBoth() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testExpirationOrder_access() {
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(11, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 10; i++) {
      cache.getUnchecked(i);
      ticker.advance(1, MILLISECONDS);
    }
    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(2, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(4, 5, 6, 7, 8, 9, 0, 1, 2);

    getAll(cache, asList(5, 7, 9));
    CacheTesting.drainRecencyQueues(cache);
    assertThat(keySet).containsExactly(4, 6, 8, 0, 1, 2, 5, 7, 9);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 0, 1, 2, 5, 7, 9);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 0, 1, 2, 5, 7, 9);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(8, 0, 1, 2, 5, 7, 9);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(8, 0, 1, 2, 5, 7, 9);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(0, 1, 2, 5, 7, 9);
  }

  public void testExpirationOrder_write() throws ExecutionException {
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(11, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 10; i++) {
      cache.getUnchecked(i);
      ticker.advance(1, MILLISECONDS);
    }
    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(2, 3, 4, 5, 6, 7, 8, 9, 0);

    Integer unused = cache.get(2, Callables.returning(-2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 4, 5, 6, 7, 8, 9, 0);

    cache.asMap().put(3, -3);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(4, 5, 6, 7, 8, 9, 0, 3);

    cache.asMap().replace(4, -4);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(5, 6, 7, 8, 9, 0, 3, 4);

    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 7, 8, 9, 0, 3, 4);
  }

  public void testExpirationOrder_writeAccess() throws ExecutionException {
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(5, MILLISECONDS)
            .expireAfterAccess(3, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 5; i++) {
      cache.getUnchecked(i);
    }
    ticker.advance(1, MILLISECONDS);
    for (int i = 5; i < 10; i++) {
      cache.getUnchecked(i);
    }
    ticker.advance(1, MILLISECONDS);

    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    getAll(cache, asList(1, 3));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(5, 6, 7, 8, 9, 1, 3);

    getAll(cache, asList(6, 8));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 3, 6, 8);

    cache.asMap().put(3, -3);
    getAll(cache, asList(1));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 3);

    cache.asMap().replace(6, -6);
    Integer unused = cache.get(8, Callables.returning(-8));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 6);
  }

  public void testExpiration_invalidateAll() {
    FakeTicker ticker = new FakeTicker();
    QueuingRemovalListener<Integer, Integer> listener =
        TestingRemovalListeners.queuingRemovalListener();
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener(listener)
            .ticker(ticker)
            .build();
    cache.put(1, 1);
    ticker.advance(10, TimeUnit.MINUTES);
    cache.invalidateAll();

    assertThat(listener.poll().getCause()).isEqualTo(RemovalCause.EXPIRED);
  }

  private void runRemovalScheduler(
      LoadingCache<String, Integer> cache,
      CountingRemovalListener<String, Integer> removalListener,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      String keyPrefix,
      long ttl) {

    int shift1 = 10 + VALUE_PREFIX;
    loader.setValuePrefix(shift1);
    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(i + shift1), cache.getUnchecked(keyPrefix + i));
    }
    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(0, removalListener.getCount());

    ticker.advance(ttl * 2 / 3, MILLISECONDS);

    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(0, removalListener.getCount());

    int shift2 = shift1 + 10;
    loader.setValuePrefix(shift2);
    for (int i = 0; i < 10; i++) {
      cache.invalidate(keyPrefix + i);
      assertEquals(
          "key: " + keyPrefix + i, Integer.valueOf(i + shift2), cache.getUnchecked(keyPrefix + i));
    }
    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(10, removalListener.getCount()); 

    ticker.advance(ttl * 2 / 3, MILLISECONDS);

    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(10, removalListener.getCount());

    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(i + shift2), cache.getUnchecked(keyPrefix + i));
      assertFalse("Creator should NOT have been called @#" + i, loader.wasCalled());
    }
    assertEquals(10, removalListener.getCount());
  }

  private static void getAll(LoadingCache<Integer, Integer> cache, List<Integer> keys) {
    for (int i : keys) {
      cache.getUnchecked(i);
    }
  }

  private static class WatchedCreatorLoader extends CacheLoader<String, Integer> {
    boolean wasCalled = false; 
    String keyPrefix = KEY_PREFIX;
    int valuePrefix = VALUE_PREFIX;

    public WatchedCreatorLoader() {}

    public void reset() {
      wasCalled = false;
    }

    public boolean wasCalled() {
      return wasCalled;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public void setValuePrefix(int valuePrefix) {
      this.valuePrefix = valuePrefix;
    }

    @Override
    public Integer load(String key) {
      wasCalled = true;
      return valuePrefix + Integer.parseInt(key.substring(keyPrefix.length()));
    }
  }
}
