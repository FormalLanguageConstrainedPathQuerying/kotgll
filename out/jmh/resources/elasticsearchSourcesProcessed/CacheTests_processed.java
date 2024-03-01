/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.cache;

import org.elasticsearch.core.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class CacheTests extends ESTestCase {
    private int numberOfEntries;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        numberOfEntries = randomIntBetween(1000, 10000);
        logger.debug("numberOfEntries: {}", numberOfEntries);
    }

    public void testCacheStats() {
        AtomicLong evictions = new AtomicLong();
        Set<Integer> keys = new HashSet<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder()
            .setMaximumWeight(numberOfEntries / 2)
            .removalListener(notification -> {
                keys.remove(notification.getKey());
                evictions.incrementAndGet();
            })
            .build();

        for (int i = 0; i < numberOfEntries; i++) {
            keys.add(i);
            cache.put(i, Integer.toString(i));
        }
        long hits = 0;
        long misses = 0;
        Integer missingKey = 0;
        for (Integer key : keys) {
            --missingKey;
            if (rarely()) {
                misses++;
                cache.get(missingKey);
            } else {
                hits++;
                cache.get(key);
            }
        }
        assertEquals(hits, cache.stats().getHits());
        assertEquals(misses, cache.stats().getMisses());
        assertEquals((long) Math.ceil(numberOfEntries / 2.0), evictions.get());
        assertEquals(evictions.get(), cache.stats().getEvictions());
    }

    public void testCacheEvictions() {
        int maximumWeight = randomIntBetween(1, numberOfEntries);
        AtomicLong evictions = new AtomicLong();
        List<Integer> evictedKeys = new ArrayList<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder()
            .setMaximumWeight(maximumWeight)
            .removalListener(notification -> {
                evictions.incrementAndGet();
                evictedKeys.add(notification.getKey());
            })
            .build();
        List<Integer> expectedEvictions = new ArrayList<>();
        int iterations = (int) Math.ceil((numberOfEntries - maximumWeight) / (1.0 * maximumWeight));
        for (int i = 0; i < iterations; i++) {
            for (int j = i * maximumWeight; j < (i + 1) * maximumWeight && j < numberOfEntries - maximumWeight; j++) {
                cache.put(j, Integer.toString(j));
                if (j % 2 == 1) {
                    expectedEvictions.add(j);
                }
            }
            for (int j = i * maximumWeight; j < (i + 1) * maximumWeight && j < numberOfEntries - maximumWeight; j++) {
                if (j % 2 == 0) {
                    cache.get(j);
                    expectedEvictions.add(j);
                }
            }
        }
        for (int i = numberOfEntries - maximumWeight; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        assertEquals(numberOfEntries - maximumWeight, evictions.get());
        assertEquals(evictions.get(), cache.stats().getEvictions());

        Set<Integer> keys = new HashSet<>();
        List<Integer> remainingKeys = new ArrayList<>();
        for (Integer key : cache.keys()) {
            keys.add(key);
            remainingKeys.add(key);
        }
        assertEquals(expectedEvictions.size(), evictedKeys.size());
        for (int i = 0; i < expectedEvictions.size(); i++) {
            assertFalse(keys.contains(expectedEvictions.get(i)));
            assertEquals(expectedEvictions.get(i), evictedKeys.get(i));
        }
        for (int i = numberOfEntries - maximumWeight; i < numberOfEntries; i++) {
            assertTrue(keys.contains(i));
            assertEquals(
                numberOfEntries - i + (numberOfEntries - maximumWeight) - 1,
                (int) remainingKeys.get(i - (numberOfEntries - maximumWeight))
            );
        }
    }

    public void testWeigher() {
        int maximumWeight = 2 * numberOfEntries;
        int weight = randomIntBetween(2, 10);
        AtomicLong evictions = new AtomicLong();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder()
            .setMaximumWeight(maximumWeight)
            .weigher((k, v) -> weight)
            .removalListener(notification -> evictions.incrementAndGet())
            .build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        assertEquals(weight * (maximumWeight / weight), cache.weight());

        assertEquals((int) Math.ceil((weight - 2) * numberOfEntries / (1.0 * weight)), evictions.get());

        assertEquals(evictions.get(), cache.stats().getEvictions());
    }

    public void testWeight() {
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().weigher((k, v) -> k).build();
        int weight = 0;
        for (int i = 0; i < numberOfEntries; i++) {
            weight += i;
            cache.put(i, Integer.toString(i));
        }
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                weight -= i;
                cache.invalidate(i);
            }
        }
        assertEquals(weight, cache.weight());
    }

    public void testCount() {
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        int count = 0;
        for (int i = 0; i < numberOfEntries; i++) {
            count++;
            cache.put(i, Integer.toString(i));
        }
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                count--;
                cache.invalidate(i);
            }
        }
        assertEquals(count, cache.count());
    }

    public void testExpirationAfterAccess() {
        AtomicLong now = new AtomicLong();
        Cache<Integer, String> cache = new Cache<Integer, String>() {
            @Override
            protected long now() {
                return now.get();
            }
        };
        cache.setExpireAfterAccessNanos(1);
        List<Integer> evictedKeys = new ArrayList<>();
        cache.setRemovalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.EVICTED, notification.getRemovalReason());
            evictedKeys.add(notification.getKey());
        });
        now.set(0);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        now.set(1);
        for (int i = numberOfEntries; i < 2 * numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        now.set(2);
        cache.refresh();
        assertEquals(numberOfEntries, cache.count());
        for (int i = 0; i < evictedKeys.size(); i++) {
            assertEquals(i, (int) evictedKeys.get(i));
        }
        Set<Integer> remainingKeys = new HashSet<>();
        for (Integer key : cache.keys()) {
            remainingKeys.add(key);
        }
        for (int i = numberOfEntries; i < 2 * numberOfEntries; i++) {
            assertTrue(remainingKeys.contains(i));
        }
    }

    public void testSimpleExpireAfterAccess() {
        AtomicLong now = new AtomicLong();
        Cache<Integer, String> cache = new Cache<Integer, String>() {
            @Override
            protected long now() {
                return now.get();
            }
        };
        cache.setExpireAfterAccessNanos(1);
        now.set(0);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        for (int i = 0; i < numberOfEntries; i++) {
            assertEquals(cache.get(i), Integer.toString(i));
        }
        now.set(2);
        for (int i = 0; i < numberOfEntries; i++) {
            assertNull(cache.get(i));
        }
    }

    public void testExpirationAfterWrite() {
        AtomicLong now = new AtomicLong();
        Cache<Integer, String> cache = new Cache<Integer, String>() {
            @Override
            protected long now() {
                return now.get();
            }
        };
        cache.setExpireAfterWriteNanos(1);
        List<Integer> evictedKeys = new ArrayList<>();
        cache.setRemovalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.EVICTED, notification.getRemovalReason());
            evictedKeys.add(notification.getKey());
        });
        now.set(0);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        now.set(1);
        for (int i = numberOfEntries; i < 2 * numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        now.set(2);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.get(i);
        }
        cache.refresh();
        assertEquals(numberOfEntries, cache.count());
        for (int i = 0; i < evictedKeys.size(); i++) {
            assertEquals(i, (int) evictedKeys.get(i));
        }
        Set<Integer> remainingKeys = new HashSet<>();
        for (Integer key : cache.keys()) {
            remainingKeys.add(key);
        }
        for (int i = numberOfEntries; i < 2 * numberOfEntries; i++) {
            assertTrue(remainingKeys.contains(i));
        }
    }

    public void testComputeIfAbsentAfterExpiration() throws ExecutionException {
        AtomicLong now = new AtomicLong();
        Cache<Integer, String> cache = new Cache<Integer, String>() {
            @Override
            protected long now() {
                return now.get();
            }
        };
        cache.setExpireAfterAccessNanos(1);
        now.set(0);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i) + "-first");
        }
        now.set(2);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.computeIfAbsent(i, k -> Integer.toString(k) + "-second");
        }
        for (int i = 0; i < numberOfEntries; i++) {
            assertEquals(i + "-second", cache.get(i));
        }
        assertEquals(numberOfEntries, cache.stats().getEvictions());
    }

    public void testComputeIfAbsentDeadlock() {
        final int numberOfThreads = randomIntBetween(2, 32);
        final Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder()
            .setExpireAfterAccess(TimeValue.timeValueNanos(1))
            .build();

        final CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            final Thread thread = new Thread(() -> {
                safeAwait(barrier);
                for (int j = 0; j < numberOfEntries; j++) {
                    try {
                        cache.computeIfAbsent(0, k -> Integer.toString(k));
                    } catch (final ExecutionException e) {
                        throw new AssertionError(e);
                    }
                }
                safeAwait(barrier);
            });
            thread.start();
        }

        safeAwait(barrier);
        safeAwait(barrier);
    }

    public void testPromotion() {
        AtomicLong now = new AtomicLong();
        Cache<Integer, String> cache = new Cache<Integer, String>() {
            @Override
            protected long now() {
                return now.get();
            }
        };
        cache.setExpireAfterAccessNanos(1);
        now.set(0);
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        now.set(1);
        Set<Integer> promotedKeys = new HashSet<>();
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                cache.get(i);
                promotedKeys.add(i);
            }
        }
        now.set(2);
        cache.refresh();
        assertEquals(promotedKeys.size(), cache.count());
        for (int i = 0; i < numberOfEntries; i++) {
            if (promotedKeys.contains(i)) {
                assertNotNull(cache.get(i));
            } else {
                assertNull(cache.get(i));
            }
        }
    }

    public void testInvalidate() {
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        Set<Integer> keys = new HashSet<>();
        for (Integer key : cache.keys()) {
            if (rarely()) {
                cache.invalidate(key);
                keys.add(key);
            }
        }
        for (int i = 0; i < numberOfEntries; i++) {
            if (keys.contains(i)) {
                assertNull(cache.get(i));
            } else {
                assertNotNull(cache.get(i));
            }
        }
    }

    public void testNotificationOnInvalidate() {
        Set<Integer> notifications = new HashSet<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().removalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.INVALIDATED, notification.getRemovalReason());
            notifications.add(notification.getKey());
        }).build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        Set<Integer> invalidated = new HashSet<>();
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                cache.invalidate(i);
                invalidated.add(i);
            }
        }
        assertEquals(notifications, invalidated);
    }

    public void testInvalidateWithValue() {
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        Set<Integer> keys = new HashSet<>();
        for (Integer key : cache.keys()) {
            if (rarely()) {
                if (randomBoolean()) {
                    cache.invalidate(key, key.toString());
                    keys.add(key);
                } else {
                    cache.invalidate(key, Integer.toString(key + randomIntBetween(2, 10)));
                }
            }
        }
        for (int i = 0; i < numberOfEntries; i++) {
            if (keys.contains(i)) {
                assertNull(cache.get(i));
            } else {
                assertNotNull(cache.get(i));
            }
        }
    }

    public void testNotificationOnInvalidateWithValue() {
        Set<Integer> notifications = new HashSet<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().removalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.INVALIDATED, notification.getRemovalReason());
            notifications.add(notification.getKey());
        }).build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        Set<Integer> invalidated = new HashSet<>();
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                if (randomBoolean()) {
                    cache.invalidate(i, Integer.toString(i));
                    invalidated.add(i);
                } else {
                    cache.invalidate(i, Integer.toString(i + randomIntBetween(2, 10)));
                }
            }
        }
        assertEquals(notifications, invalidated);
    }

    public void testInvalidateAll() {
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        cache.invalidateAll();
        assertEquals(0, cache.count());
        assertEquals(0, cache.weight());
    }

    public void testNotificationOnInvalidateAll() {
        Set<Integer> notifications = new HashSet<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().removalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.INVALIDATED, notification.getRemovalReason());
            notifications.add(notification.getKey());
        }).build();
        Set<Integer> invalidated = new HashSet<>();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
            invalidated.add(i);
        }
        cache.invalidateAll();
        assertEquals(invalidated, notifications);
    }

    public void testReplaceRecomputesSize() {
        class Value {
            private String value;
            private long weight;

            Value(String value, long weight) {
                this.value = value;
                this.weight = weight;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Value that = (Value) o;

                return value.equals(that.value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }
        Cache<Integer, Value> cache = CacheBuilder.<Integer, Value>builder().weigher((k, s) -> s.weight).build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, new Value(Integer.toString(i), 1));
        }
        assertEquals(numberOfEntries, cache.count());
        assertEquals(numberOfEntries, cache.weight());
        int replaced = 0;
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                replaced++;
                cache.put(i, new Value(Integer.toString(i), 2));
            }
        }
        assertEquals(numberOfEntries, cache.count());
        assertEquals(numberOfEntries + replaced, cache.weight());
    }

    public void testNotificationOnReplace() {
        Set<Integer> notifications = new HashSet<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().removalListener(notification -> {
            assertEquals(RemovalNotification.RemovalReason.REPLACED, notification.getRemovalReason());
            notifications.add(notification.getKey());
        }).build();
        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }
        Set<Integer> replacements = new HashSet<>();
        for (int i = 0; i < numberOfEntries; i++) {
            if (rarely()) {
                cache.put(i, Integer.toString(i) + Integer.toString(i));
                replacements.add(i);
            }
        }
        assertEquals(replacements, notifications);
    }

    public void testComputeIfAbsentLoadsSuccessfully() {
        Map<Integer, Integer> map = new HashMap<>();
        Cache<Integer, Integer> cache = CacheBuilder.<Integer, Integer>builder().build();
        for (int i = 0; i < numberOfEntries; i++) {
            try {
                cache.computeIfAbsent(i, k -> {
                    int value = randomInt();
                    map.put(k, value);
                    return value;
                });
            } catch (ExecutionException e) {
                throw new AssertionError(e);
            }
        }
        for (int i = 0; i < numberOfEntries; i++) {
            assertEquals(map.get(i), cache.get(i));
        }
    }

    public void testComputeIfAbsentCallsOnce() {
        int numberOfThreads = randomIntBetween(2, 32);
        final Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        AtomicReferenceArray<Object> flags = new AtomicReferenceArray<>(numberOfEntries);
        for (int j = 0; j < numberOfEntries; j++) {
            flags.set(j, false);
        }

        CopyOnWriteArrayList<ExecutionException> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                safeAwait(barrier);
                for (int j = 0; j < numberOfEntries; j++) {
                    try {
                        cache.computeIfAbsent(j, key -> {
                            assertTrue(flags.compareAndSet(key, false, true));
                            return Integer.toString(key);
                        });
                    } catch (ExecutionException e) {
                        failures.add(e);
                        break;
                    }
                }
                safeAwait(barrier);
            });
            thread.start();
        }

        safeAwait(barrier);
        safeAwait(barrier);

        assertThat(failures, is(empty()));
    }

    public void testComputeIfAbsentThrowsExceptionIfLoaderReturnsANullValue() {
        final Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();
        try {
            cache.computeIfAbsent(1, k -> null);
            fail("expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(NullPointerException.class));
        }
    }

    public void testDependentKeyDeadlock() {
        class Key {
            private final int key;

            Key(int key) {
                this.key = key;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Key key1 = (Key) o;

                return key == key1.key;

            }

            @Override
            public int hashCode() {
                return key % 2;
            }
        }

        int numberOfThreads = randomIntBetween(2, 32);
        final Cache<Key, Integer> cache = CacheBuilder.<Key, Integer>builder().build();

        CopyOnWriteArrayList<ExecutionException> failures = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);
        CountDownLatch deadlockLatch = new CountDownLatch(numberOfThreads);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    safeAwait(barrier);
                    Random random = new Random(random().nextLong());
                    for (int j = 0; j < numberOfEntries; j++) {
                        Key key = new Key(random.nextInt(numberOfEntries));
                        try {
                            cache.computeIfAbsent(key, k -> {
                                if (k.key == 0) {
                                    return 0;
                                } else {
                                    Integer value = cache.get(new Key(k.key / 2));
                                    return value != null ? value : 0;
                                }
                            });
                        } catch (ExecutionException e) {
                            failures.add(e);
                            break;
                        }
                    }
                } finally {
                    deadlockLatch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        AtomicBoolean deadlock = new AtomicBoolean();
        assert deadlock.get() == false;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            Set<Long> ids = threads.stream().map(t -> t.getId()).collect(Collectors.toSet());
            ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
            long[] deadlockedThreads = mxBean.findDeadlockedThreads();
            if (deadlock.get() == false && deadlockedThreads != null) {
                for (long deadlockedThread : deadlockedThreads) {
                    if (ids.contains(deadlockedThread)) {
                        deadlock.set(true);
                        for (int i = 0; i < numberOfThreads; i++) {
                            deadlockLatch.countDown();
                        }
                        break;
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        safeAwait(barrier);

        safeAwait(deadlockLatch);

        scheduler.shutdown();

        assertThat(failures, is(empty()));

        assertFalse("deadlock", deadlock.get());
    }

    public void testCachePollution() {
        int numberOfThreads = randomIntBetween(2, 32);
        final Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().build();

        CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                safeAwait(barrier);
                Random random = new Random(random().nextLong());
                for (int j = 0; j < numberOfEntries; j++) {
                    Integer key = random.nextInt(numberOfEntries);
                    boolean first;
                    boolean second;
                    do {
                        first = random.nextBoolean();
                        second = random.nextBoolean();
                    } while (first && second);
                    if (first) {
                        try {
                            cache.computeIfAbsent(key, k -> {
                                if (random.nextBoolean()) {
                                    return Integer.toString(k);
                                } else {
                                    throw new Exception("testCachePollution");
                                }
                            });
                        } catch (ExecutionException e) {
                            assertNotNull(e.getCause());
                            assertThat(e.getCause(), instanceOf(Exception.class));
                            assertEquals(e.getCause().getMessage(), "testCachePollution");
                        }
                    } else if (second) {
                        cache.invalidate(key);
                    } else {
                        cache.get(key);
                    }
                }
                safeAwait(barrier);
            });
            thread.start();
        }

        safeAwait(barrier);
        safeAwait(barrier);
    }

    public void testExceptionThrownDuringConcurrentComputeIfAbsent() {
        int numberOfThreads = randomIntBetween(2, 32);
        final Cache<String, String> cache = CacheBuilder.<String, String>builder().build();

        CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);

        final String key = randomAlphaOfLengthBetween(2, 32);
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                safeAwait(barrier);
                for (int j = 0; j < numberOfEntries; j++) {
                    try {
                        String value = cache.computeIfAbsent(key, k -> { throw new RuntimeException("failed to load"); });
                        fail("expected exception but got: " + value);
                    } catch (ExecutionException e) {
                        assertNotNull(e.getCause());
                        assertThat(e.getCause(), instanceOf(RuntimeException.class));
                        assertEquals(e.getCause().getMessage(), "failed to load");
                    }
                }
                safeAwait(barrier);
            });
            thread.start();
        }

        safeAwait(barrier);
        safeAwait(barrier);
    }

    public void testTorture() {
        int numberOfThreads = randomIntBetween(2, 32);
        final Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder().setMaximumWeight(1000).weigher((k, v) -> 2).build();

        CyclicBarrier barrier = new CyclicBarrier(1 + numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                safeAwait(barrier);
                Random random = new Random(random().nextLong());
                for (int j = 0; j < numberOfEntries; j++) {
                    Integer key = random.nextInt(numberOfEntries);
                    cache.put(key, Integer.toString(j));
                }
                safeAwait(barrier);
            });
            thread.start();
        }

        safeAwait(barrier);
        safeAwait(barrier);

        cache.refresh();
        assertEquals(500, cache.count());
    }

    public void testRemoveUsingValuesIterator() {
        final List<RemovalNotification<Integer, String>> removalNotifications = new ArrayList<>();
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>builder()
            .setMaximumWeight(numberOfEntries)
            .removalListener(removalNotifications::add)
            .build();

        for (int i = 0; i < numberOfEntries; i++) {
            cache.put(i, Integer.toString(i));
        }

        assertThat(removalNotifications.size(), is(0));
        final List<String> expectedRemovals = new ArrayList<>();
        Iterator<String> valueIterator = cache.values().iterator();
        while (valueIterator.hasNext()) {
            String value = valueIterator.next();
            if (randomBoolean()) {
                valueIterator.remove();
                expectedRemovals.add(value);
            }
        }

        assertEquals(expectedRemovals.size(), removalNotifications.size());
        for (int i = 0; i < expectedRemovals.size(); i++) {
            assertEquals(expectedRemovals.get(i), removalNotifications.get(i).getValue());
            assertEquals(RemovalNotification.RemovalReason.INVALIDATED, removalNotifications.get(i).getRemovalReason());
        }
    }
}
