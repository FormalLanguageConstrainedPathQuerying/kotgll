package org.openjdk.bench.java.lang;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class RotateBenchmark {

  @Param({"1024"})
  public int TESTSIZE;

  @Param({"20"})
  public int SHIFT;

  static final int CONSHIFT = 20;

  public long [] larr;
  public int  [] iarr;

  public long [] lres;
  public int  [] ires;


  @Setup(Level.Trial)
  public void BmSetup() {
    Random r = new Random(1024);
    larr = new long[TESTSIZE];
    iarr = new int[TESTSIZE];
    lres = new long[TESTSIZE];
    ires = new int[TESTSIZE];

    for (int i = 0; i < TESTSIZE; i++) {
      larr[i] = r.nextLong();
    }

    for (int i = 0; i < TESTSIZE; i++) {
      iarr[i] = r.nextInt();
    }
  }

  @Benchmark
  public void testRotateLeftI() {
    for (int i = 0; i < TESTSIZE; i++)
       ires[i] = Integer.rotateLeft(iarr[i], SHIFT);
  }
  @Benchmark
  public void testRotateRightI() {
    for (int i = 0; i < TESTSIZE; i++)
       ires[i] = Integer.rotateRight(iarr[i], SHIFT);
  }
  @Benchmark
  public void testRotateLeftL() {
    for (int i = 0; i < TESTSIZE; i++)
       lres[i] = Long.rotateLeft(larr[i], SHIFT);
  }
  @Benchmark
  public void testRotateRightL() {
    for (int i = 0; i < TESTSIZE; i++)
       lres[i] = Long.rotateRight(larr[i], SHIFT);
  }
  @Benchmark
  public void testRotateLeftConI() {
    for (int i = 0; i < TESTSIZE; i++)
      ires[i] = Integer.rotateLeft(iarr[i], CONSHIFT);
  }
  @Benchmark
  public void testRotateRightConI() {
    for (int i = 0; i < TESTSIZE; i++)
      ires[i] = Integer.rotateRight(iarr[i], CONSHIFT);
  }
  @Benchmark
  public void testRotateLeftConL() {
    for (int i = 0; i < TESTSIZE; i++)
      lres[i] = Long.rotateLeft(larr[i], CONSHIFT);
  }
  @Benchmark
  public void testRotateRightConL() {
    for (int i = 0; i < TESTSIZE; i++)
      lres[i] = Long.rotateRight(larr[i], CONSHIFT);
  }
}
