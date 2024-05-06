package org

import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import kotlinx.benchmark.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.openjdk.jmh.annotations.Threads


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)

@Warmup(iterations = 1, time = 50, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Threads(Threads.MAX)
@State(Scope.Benchmark)
class AntlrBench {

    @Param("Throwables.java")
    var fileName: String = ""

    lateinit var fileContents: String

    @Setup
    fun prepare() {
        fileContents = AntlrBench::class.java.classLoader
            .getResource(fileName)?.readText()
            ?: throw Exception("file $fileName does not exists")
    }

    @Benchmark
    fun measureAntlr(blackhole: Blackhole) {
        val antlrParser =
            Java8Parser(
                CommonTokenStream(
                    Java8Lexer(
                        CharStreams.fromString(fileContents)
                    )
                )
            )
        blackhole.consume(antlrParser.compilationUnit())
    }
}
