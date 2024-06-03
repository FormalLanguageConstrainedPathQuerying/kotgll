package org.ucfs

import java8.Java8
import java8.JavaLexer
import java8.JavaToken
import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.ucfs.ast.AstExtractor
import org.ucfs.input.IInputGraph
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path

fun <G : IInputGraph<Int, LinearInputLabel>> getTokenStream(input: String, inputGraph: G): G {
    val lexer = JavaLexer(StringReader(input))
    var token: JavaToken
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    while (true) {
        token = lexer.yylex() as JavaToken
        if (token == JavaToken.EOF) break
        inputGraph.addEdge(vertexId, LinearInputLabel(token), ++vertexId)
    }

    return inputGraph
}


fun getTokenStream(input: String): LinearInput<Int, LinearInputLabel> {
    val graph = LinearInput<Int, LinearInputLabel>()
    getTokenStream(input, graph)
    return graph
}

fun runAntlr(fileContents: String) {
    val antlrParser =
        Java8Parser(
            CommonTokenStream(
                Java8Lexer(
                    CharStreams.fromString(fileContents)
                )
            )
        )
    antlrParser.compilationUnit()
}

fun runSimpleOnline(fileContents: String) {
    val gll = Gll.gll(Java8().rsm, getTokenStream(fileContents))
    gll.parse()
}

fun runSimpleOffline(fileContents: String) {
    val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
    parser.setInput(getTokenStream(fileContents))
    parser.parse()
}

fun runRecoveryOnline(fileContents: String) {
    val gll = Gll.recoveryGll(Java8().rsm, getTokenStream(fileContents))
    gll.parse()
}

fun runRecoveryOffline(fileContents: String) {
    val parser = org.ucfs.Java8ParserRecovery<Int, LinearInputLabel>()
    parser.setInput(getTokenStream(fileContents))
    val res = parser.parse()
}

fun main(args: Array<String>){
    val tool = args[0]
    val file = args[1]
    val fileContents = Files.readString(Path.of(file))
    try {

    when(tool) {
        "Antlr" -> runAntlr(fileContents)
        "SimpleOn" -> runSimpleOnline(fileContents)
        "SimpleOff" -> runSimpleOffline(fileContents)
        "RecoveryOn" -> runRecoveryOnline(fileContents)
        "RecoveryOff" -> runRecoveryOffline(fileContents)
        else -> System.exit(42)
    }
    } catch(e: OutOfMemoryError){
        System.exit(2)
    }

}