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

package com.google.common.collect;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.collect.BenchmarkHelpers.SetImpl;
import com.google.common.collect.CollectionBenchmarkSampleData.Element;
import java.util.Set;

/**
 * A microbenchmark that tests the performance of contains() on various Set implementations.
 *
 * @author Kevin Bourrillion
 */
public class SetContainsBenchmark {
  @Param({"5", "30", "180", "1100", "6900", "43000", "260000"}) 
  private int size;

  @Param({"0.2", "0.8"})
  private double hitRate;

  @Param("true")
  private boolean isUserTypeFast;

  @Param("")
  private SpecialRandom random;

  @Param({"HashSetImpl", "ImmutableSetImpl"})
  private SetImpl impl;

  private Element[] queries;
  private Set<Element> setToTest;

  @BeforeExperiment
  void setUp() {
    CollectionBenchmarkSampleData sampleData =
        new CollectionBenchmarkSampleData(isUserTypeFast, random, hitRate, size);

    this.setToTest = (Set<Element>) impl.create(sampleData.getValuesInSet());
    this.queries = sampleData.getQueries();
  }

  @Benchmark
  boolean contains(int reps) {
    Set<Element> set = setToTest;
    Element[] queries = this.queries;

    int mask = queries.length - 1;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= set.contains(queries[i & mask]);
    }
    return dummy;
  }
}
