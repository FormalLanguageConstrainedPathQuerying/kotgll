package org

import kotlinx.benchmark.*
import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream


@State(Scope.Benchmark)
class Antlr : BaseBench(){

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
