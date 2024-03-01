
package org.openjdk.bench.vm.gc;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MINUTES)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {"-Xmx256m", "-XX:+UseLargePages", "-XX:LargePageSizeInBytes=1g", "-Xlog:pagesize"}, value = 5)

public class MicroLargePages {

    @Param({"2097152"})
    public int ARRAYSIZE;

    @Param({"1", "2", "4"})
    public int NUM;

    public long[][] INP;
    public long[][] OUT;

    @Setup(Level.Trial)
    public void BmSetup() {
        INP = new long[NUM][ARRAYSIZE];
        OUT = new long[NUM][ARRAYSIZE];
        for (int i = 0; i < NUM; i++) {
            Arrays.fill(INP[i], 10);
        }
    }

    @Benchmark
    public void micro_HOP_DIST_4KB() {
        for (int i = 0; i < NUM; i += 1) {
             for (int j = 0; j < ARRAYSIZE; j += 512) {
                 OUT[i][j] = INP[i][j];
             }
        }
    }
}
