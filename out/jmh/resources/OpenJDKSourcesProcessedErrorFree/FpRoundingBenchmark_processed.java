package org.openjdk.bench.java.math;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3)
public class FpRoundingBenchmark {

  @Param({"2048"})
  public int TESTSIZE;

  public double[] DargV1;
  public double[] ResD;
  public long[] ResL;
  public float[] FargV1;
  public float[] ResF;
  public int[] ResI;

  public final double[] DspecialVals = {
      0.0, -0.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
      Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE,
      Double.MIN_NORMAL
  };

  public final float[] FspecialVals = {
      0.0f, -0.0f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
      Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, -Float.MIN_VALUE,
      Float.MIN_NORMAL
  };

  @Setup(Level.Trial)
  public void BmSetup() {
      int i = 0;
      Random r = new Random(1024);

      DargV1 = new double[TESTSIZE];
      ResD = new double[TESTSIZE];

      for (; i < DspecialVals.length; i++) {
          DargV1[i] = DspecialVals[i];
      }

      for (; i < TESTSIZE; i++) {
          DargV1[i] = Double.longBitsToDouble(r.nextLong());;
      }

      FargV1 = new float[TESTSIZE];
      ResF = new float[TESTSIZE];

      i = 0;
      for (; i < FspecialVals.length; i++) {
          FargV1[i] = FspecialVals[i];
      }

      for (; i < TESTSIZE; i++) {
          FargV1[i] = Float.intBitsToFloat(r.nextInt());
      }

      ResI = new int[TESTSIZE];
      ResL = new long[TESTSIZE];
  }

  @Benchmark
  public void test_ceil() {
      for (int i = 0; i < TESTSIZE; i++) {
          ResD[i] = Math.ceil(DargV1[i]);
      }
  }

  @Benchmark
  public void test_floor() {
      for (int i = 0; i < TESTSIZE; i++) {
          ResD[i] = Math.floor(DargV1[i]);
      }
  }

  @Benchmark
  public void test_rint() {
      for (int i = 0; i < TESTSIZE; i++) {
          ResD[i] = Math.rint(DargV1[i]);
      }
  }

  @Benchmark
  public void test_round_double() {
      for (int i = 0; i < TESTSIZE; i++) {
          ResL[i] = Math.round(DargV1[i]);
      }
  }

  @Benchmark
  public void test_round_float() {
      for (int i = 0; i < TESTSIZE; i++) {
          ResI[i] = Math.round(FargV1[i]);
      }
  }
}
