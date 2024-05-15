package org

import org.antlr.Java8Lexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.ucfs.Java8
import org.ucfs.JavaToken
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.ParserGenerator
import java.io.File
import java.nio.file.Path

fun generate() {
    ParserGenerator(
        Java8::class.java,
        JavaToken::class.java
    ).generate(Path.of("benchmarks/src/main/kotlin/"), "org.ucfs")

}

fun runGenerated() {
    val fileContents = """
        /**
         * Provides JUnit v3.x test runners.
         */
        package junit.runner;
    """.trimIndent()
    var cnt = 0
    var sleepCnt = 2000
    while (true) {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.input = getTokenStream(fileContents)
        val res = parser.parse().first!!
        cnt += 1
    }
}
class Main(){}
fun main() {
   calculateTokens()
}

fun getAnrltTokens(file: String): Int{
    val stream = CommonTokenStream(
        Java8Lexer(
            CharStreams.fromString(file)
        )
    )
    return stream.numberOfOnChannelTokens
}
fun calculateTokens(){
    val pth = Main::class.java.getResource("/junit")
    val resourcesDir = File(pth.toURI())
    val stat = File("junit-tokens.csv").printWriter()
    stat.println("Param: fileName,Line Count")
    for (file in resourcesDir.listFiles()!!){
        // stat.println("${file.name},${getTokenStream(file.readText()).edges.size}")
          stat.println("${file.name},${getAnrltTokens(file.readText())}")
    }
    stat.close()
}
fun run() {
    class L : Grammar() {
        val List by Nt().asStart()
        val Elem by Nt("x" or List)

        init {
            List /= "[" * Elem
        }
    }
    val l = L()
}

