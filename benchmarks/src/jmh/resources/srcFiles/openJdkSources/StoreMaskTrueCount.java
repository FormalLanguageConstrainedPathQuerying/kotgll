package org.openjdk.bench.jdk.incubator.vector;

import java.util.concurrent.TimeUnit;
import java.util.Random;
import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class StoreMaskTrueCount {
    private static final VectorSpecies<Short> S_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> I_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> L_SPECIES = LongVector.SPECIES_PREFERRED;
    private static final int LENGTH = 128;
    private static final Random RD = new Random();
    private static boolean[] ba;

    static {
        ba = new boolean[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            ba[i] = RD.nextBoolean();
        }
    }

    @Benchmark
    public int testShort() {
        int res = 0;
        for (int i = 0; i < LENGTH; i += S_SPECIES.length()) {
            VectorMask<Short> m = VectorMask.fromArray(S_SPECIES, ba, i);
            res += m.not().trueCount();
        }

        return res;
    }

    @Benchmark
    public int testInt() {
        int res = 0;
        for (int i = 0; i < LENGTH; i += I_SPECIES.length()) {
            VectorMask<Integer> m = VectorMask.fromArray(I_SPECIES, ba, i);
            res += m.not().trueCount();
        }

        return res;
    }

    @Benchmark
    public int testLong() {
        int res = 0;
        for (int i = 0; i < LENGTH; i += L_SPECIES.length()) {
            VectorMask<Long> m = VectorMask.fromArray(L_SPECIES, ba, i);
            res += m.not().trueCount();
        }

        return res;
    }
}
