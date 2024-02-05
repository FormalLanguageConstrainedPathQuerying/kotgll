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
public class StoreMaskedBenchmark {
    @Param({"1024"})
    private int size;

    private byte[] byteIn;
    private byte[] byteOut;
    private short[] shortIn;
    private short[] shortOut;
    private int[] intIn;
    private int[] intOut;
    private long[] longIn;
    private long[] longOut;
    private float[] floatIn;
    private float[] floatOut;
    private double[] doubleIn;
    private double[] doubleOut;

    private boolean[] m;

    private static final VectorSpecies<Byte> bspecies = VectorSpecies.ofLargestShape(byte.class);
    private static final VectorSpecies<Short> sspecies = VectorSpecies.ofLargestShape(short.class);
    private static final VectorSpecies<Integer> ispecies = VectorSpecies.ofLargestShape(int.class);
    private static final VectorSpecies<Long> lspecies = VectorSpecies.ofLargestShape(long.class);
    private static final VectorSpecies<Float> fspecies = VectorSpecies.ofLargestShape(float.class);
    private static final VectorSpecies<Double> dspecies = VectorSpecies.ofLargestShape(double.class);

    @Setup(Level.Trial)
    public void Setup() {
        byteIn = new byte[size];
        byteOut = new byte[size];
        shortIn = new short[size];
        shortOut = new short[size];
        intIn = new int[size];
        intOut = new int[size];
        longIn = new long[size];
        longOut = new long[size];
        floatIn = new float[size];
        floatOut = new float[size];
        doubleIn = new double[size];
        doubleOut = new double[size];
        m = new boolean[size];

        for (int i = 0; i < size; i++) {
            byteIn[i] = (byte) i;
            shortIn[i] = (short) i;
            intIn[i] = i;
            longIn[i] = i;
            floatIn[i] = (float) i;
            doubleIn[i] = (double) i;
        }
        for (int i = 0; i < size; i++) {
            m[i] = i % 2 == 0;
        }
    }

    @Benchmark
    public void byteStoreArrayMask() {
        for (int i = 0; i < size; i += bspecies.length()) {
            VectorMask<Byte> mask = VectorMask.fromArray(bspecies, m, i);
            ByteVector.fromArray(bspecies, byteIn, i).intoArray(byteOut, i, mask);
        }
    }

    @Benchmark
    public void shortStoreArrayMask() {
        for (int i = 0; i < size; i += sspecies.length()) {
            VectorMask<Short> mask = VectorMask.fromArray(sspecies, m, i);
            ShortVector.fromArray(sspecies, shortIn, i).intoArray(shortOut, i, mask);
        }
    }

    @Benchmark
    public void intStoreArrayMask() {
        for (int i = 0; i < size; i += ispecies.length()) {
            VectorMask<Integer> mask = VectorMask.fromArray(ispecies, m, i);
            IntVector.fromArray(ispecies, intIn, i).intoArray(intOut, i, mask);
        }
    }

    @Benchmark
    public void longStoreArrayMask() {
        for (int i = 0; i < size; i += lspecies.length()) {
            VectorMask<Long> mask = VectorMask.fromArray(lspecies, m, i);
            LongVector.fromArray(lspecies, longIn, i).intoArray(longOut, i, mask);
        }
    }

    @Benchmark
    public void floatStoreArrayMask() {
        for (int i = 0; i < size; i += fspecies.length()) {
            VectorMask<Float> mask = VectorMask.fromArray(fspecies, m, i);
            FloatVector.fromArray(fspecies, floatIn, i).intoArray(floatOut, i, mask);
        }
    }

    @Benchmark
    public void doubleStoreArrayMask() {
        for (int i = 0; i < size; i += dspecies.length()) {
            VectorMask<Double> mask = VectorMask.fromArray(dspecies, m, i);
            DoubleVector.fromArray(dspecies, doubleIn, i).intoArray(doubleOut, i, mask);
        }
    }
}
