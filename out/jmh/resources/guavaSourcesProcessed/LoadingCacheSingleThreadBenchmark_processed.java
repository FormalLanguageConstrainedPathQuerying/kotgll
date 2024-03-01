/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.cache;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.primitives.Ints;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-threaded benchmark for {@link LoadingCache}.
 *
 * @author Charles Fry
 */
public class LoadingCacheSingleThreadBenchmark {
  @Param({"1000", "2000"})
  int maximumSize;

  @Param("5000")
  int distinctKeys;

  @Param("4")
  int segments;

  @Param("2.5")
  double concentration;

  Random random = new Random();

  LoadingCache<Integer, Integer> cache;

  int max;

  static AtomicLong requests = new AtomicLong(0);
  static AtomicLong misses = new AtomicLong(0);

  @BeforeExperiment
  void setUp() {
    max = Ints.checkedCast((long) Math.pow(distinctKeys, concentration));

    cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(segments)
            .maximumSize(maximumSize)
            .build(
                new CacheLoader<Integer, Integer>() {
                  @Override
                  public Integer load(Integer from) {
                    return (int) misses.incrementAndGet();
                  }
                });

    while (cache.getUnchecked(nextRandomKey()) < maximumSize) {}

    requests.set(0);
    misses.set(0);
  }

  @Benchmark
  int time(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += cache.getUnchecked(nextRandomKey());
    }
    requests.addAndGet(reps);
    return dummy;
  }

  private int nextRandomKey() {
    int a = random.nextInt(max);

    /*
     * For example, if concentration=2.0, the following takes the square root of
     * the uniformly-distributed random integer, then truncates any fractional
     * part, so higher integers would appear (in this case linearly) more often
     * than lower ones.
     */
    return (int) Math.pow(a, 1.0 / concentration);
  }

  @AfterExperiment
  void tearDown() {
    double req = requests.get();
    double hit = req - misses.get();

    System.out.println("hit rate: " + hit / req);
  }

}
