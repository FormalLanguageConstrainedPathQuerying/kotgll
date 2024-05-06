package org

import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.Threads
import org.ucfs.Java8
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll
import org.ucfs.sppf.buildStringFromSppf


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)

@Warmup(iterations = 1, time = 50, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Threads(Threads.MAX)
@State(Scope.Benchmark)
class OnlineGllBench {

    @Param("Throwables.java")
    var fileName: String = ""

    lateinit var fileContents: String

    val startState = Java8().rsm
    lateinit var tokens: LinearInput<Int, LinearInputLabel>

    @Setup
    fun prepare() {
        fileContents = OnlineGllBench::class.java.classLoader
            .getResource(fileName)?.readText() ?: throw Exception("File $fileName does not exists")
        tokens = getTokenStream(fileContents)
        val gll = Gll.gll(
            startState,
            tokens
        )
        gll.parse().first ?: throw Exception("File $fileName cant be parsed by online gll")
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val gll = Gll.gll(startState, getTokenStream(fileContents))
        blackhole.consume(gll.parse())
    }
}
