package org.openjdk.bench.jdk.incubator.vector;

import java.util.concurrent.TimeUnit;
import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class IndexInRangeBenchmark {
    @Param({"7", "256", "259", "512"})
    private int size;

    private boolean[] mask;

    private static final VectorSpecies<Byte> bspecies = VectorSpecies.ofLargestShape(byte.class);
    private static final VectorSpecies<Short> sspecies = VectorSpecies.ofLargestShape(short.class);
    private static final VectorSpecies<Integer> ispecies = VectorSpecies.ofLargestShape(int.class);
    private static final VectorSpecies<Long> lspecies = VectorSpecies.ofLargestShape(long.class);
    private static final VectorSpecies<Float> fspecies = VectorSpecies.ofLargestShape(float.class);
    private static final VectorSpecies<Double> dspecies = VectorSpecies.ofLargestShape(double.class);

    @Setup(Level.Trial)
    public void Setup() {
        mask = new boolean[512];
    }

    @Benchmark
    public void byteIndexInRange() {
        for (int i = 0; i < size; i += bspecies.length()) {
            var m = bspecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }

    @Benchmark
    public void shortIndexInRange() {
        for (int i = 0; i < size; i += sspecies.length()) {
            var m = sspecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }

    @Benchmark
    public void intIndexInRange() {
        for (int i = 0; i < size; i += ispecies.length()) {
            var m = ispecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }

    @Benchmark
    public void longIndexInRange() {
        for (int i = 0; i < size; i += lspecies.length()) {
            var m = lspecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }

    @Benchmark
    public void floatIndexInRange() {
        for (int i = 0; i < size; i += fspecies.length()) {
            var m = fspecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }

    @Benchmark
    public void doubleIndexInRange() {
        for (int i = 0; i < size; i += dspecies.length()) {
            var m = dspecies.indexInRange(i, size);
            m.intoArray(mask, i);
        }
    }
}
