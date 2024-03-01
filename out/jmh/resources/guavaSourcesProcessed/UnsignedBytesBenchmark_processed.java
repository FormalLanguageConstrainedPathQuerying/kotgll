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

package com.google.common.primitives;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Microbenchmark for {@link UnsignedBytes}.
 *
 * @author Hiroshi Yamauchi
 */
public class UnsignedBytesBenchmark {

  private byte[] ba1;
  private byte[] ba2;
  private byte[] ba3;
  private byte[] ba4;
  private Comparator<byte[]> javaImpl;
  private Comparator<byte[]> unsafeImpl;

  @Param({"4", "8", "64", "1024"})
  private int length;

  @BeforeExperiment
  void setUp() throws Exception {
    Random r = new Random();
    ba1 = new byte[length];
    r.nextBytes(ba1);
    ba2 = Arrays.copyOf(ba1, ba1.length);
    ba3 = Arrays.copyOf(ba1, ba1.length);
    ba4 = Arrays.copyOf(ba1, ba1.length);
    ba3[ba1.length - 1] = (byte) 43;
    ba4[ba1.length - 1] = (byte) 42;

    javaImpl = UnsignedBytes.lexicographicalComparatorJavaImpl();
    unsafeImpl = UnsignedBytes.LexicographicalComparatorHolder.UnsafeComparator.INSTANCE;
  }

  @Benchmark
  void longEqualJava(int reps) {
    for (int i = 0; i < reps; ++i) {
      if (javaImpl.compare(ba1, ba2) != 0) {
        throw new Error(); 
      }
    }
  }

  @Benchmark
  void longEqualUnsafe(int reps) {
    for (int i = 0; i < reps; ++i) {
      if (unsafeImpl.compare(ba1, ba2) != 0) {
        throw new Error(); 
      }
    }
  }

  @Benchmark
  void diffLastJava(int reps) {
    for (int i = 0; i < reps; ++i) {
      if (javaImpl.compare(ba3, ba4) == 0) {
        throw new Error(); 
      }
    }
  }

  @Benchmark
  void diffLastUnsafe(int reps) {
    for (int i = 0; i < reps; ++i) {
      if (unsafeImpl.compare(ba3, ba4) == 0) {
        throw new Error(); 
      }
    }
  }

  /*
  try {
    UnsignedBytesBenchmark bench = new UnsignedBytesBenchmark();
    bench.length = 1024;
    bench.setUp();
    bench.timeUnsafe(100000);
  } catch (Exception e) {
  }*/
}
