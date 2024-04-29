package org.openjdk.bench.jdk.incubator.vector;

import jdk.incubator.vector.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class MaskFromLongBenchmark {
    private static final int ITERATION = 20000;

    @Benchmark
    public long microMaskFromLong_Byte64() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ByteVector.SPECIES_64, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Byte128() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ByteVector.SPECIES_128, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Byte256() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ByteVector.SPECIES_256, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Byte512() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ByteVector.SPECIES_512, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Short64() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ShortVector.SPECIES_64, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Short128() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ShortVector.SPECIES_128, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Short256() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ShortVector.SPECIES_256, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Short512() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(ShortVector.SPECIES_512, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Integer64() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(IntVector.SPECIES_64, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Integer128() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(IntVector.SPECIES_128, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Integer256() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(IntVector.SPECIES_256, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Integer512() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(IntVector.SPECIES_512, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Long64() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(LongVector.SPECIES_64, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Long128() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(LongVector.SPECIES_128, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Long256() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(LongVector.SPECIES_256, res);
            res += mask.trueCount();
        }

        return res;
    }

    @Benchmark
    public long microMaskFromLong_Long512() {
        long res = 0;
        for (int i = 0; i < ITERATION; i++) {
            VectorMask mask = VectorMask.fromLong(LongVector.SPECIES_512, res);
            res += mask.trueCount();
        }

        return res;
    }

}
