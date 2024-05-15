package org

import kotlinx.benchmark.*
import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File


@State(Scope.Benchmark)
class AntlrBench {

    @Param("Throwables.java")
    var fileName: String = ""

    lateinit var fileContents: String

    @Setup
    fun prepare() {
        fileContents = File(fileName).readText()
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
