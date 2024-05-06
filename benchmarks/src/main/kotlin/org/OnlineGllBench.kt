package org

import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.Threads
import org.ucfs.Java8
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

    @Setup
    fun prepare() {
        val srcText: String = OnlineGllBench::class.java.classLoader
            .getResource(fileName)?.readText() ?: throw Exception("File $fileName does not exists")
        val gll = Gll.gll(
            startState,
            getTokenStream(srcText)
        )
        val parseResult = gll.parse().first ?: throw Exception("File $fileName cant be parsed by online gll")
        fileContents = buildStringFromSppf(parseResult)
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val inputGraph = getTokenStream(fileContents)
        val gll = Gll.gll(startState, inputGraph)
        blackhole.consume(gll.parse())
    }
}
