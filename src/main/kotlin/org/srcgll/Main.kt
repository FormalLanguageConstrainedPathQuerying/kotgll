package org.srcgll

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.antlr.Java8Lexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.lexer.*
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.writeSppfToDot
import org.srcgll.rsm.writeRsmToDot
import org.srcgll.sppf.node.SppfNode
import java.io.File
import java.io.IOException
import java.io.StringReader

enum class RecoveryMode {
    ON, OFF,
}

enum class ReachabilityMode {
    REACHABILITY, ALLPAIRS,
}

fun main(args: Array<String>) {
    val parser = ArgParser("srcgll")

    val recoveryMode by parser.option(
        ArgType.Choice<RecoveryMode>(), fullName = "recovery", description = "Recovery mode"
    ).default(RecoveryMode.ON)

    val pathToInput by parser.option(ArgType.String, fullName = "inputPath", description = "Path to input txt file")
        .required()

    val pathToGrammar by parser.option(
        ArgType.String, fullName = "grammarPath", description = "Path to grammar txt file"
    ).required()

    val pathToOutputString by parser.option(
        ArgType.String, fullName = "outputStringPath", description = "Path to output txt file"
    ).required()

    val pathToOutputSPPF by parser.option(
        ArgType.String, fullName = "outputSPPFPath", description = "Path to output dot file"
    ).required()

    parser.parse(args)

//    val input = File(pathToInput).readText()
//    val grammar = JavaGrammar().getRsm()
//    val inputGraph = LinearInput<Int, LinearInputLabel>()
//    val gll = Gll(grammar, inputGraph, RecoveryMode.ON, ReachabilityMode.REACHABILITY)
//    val lexer = Scanner(StringReader(input))
//    var token: JavaSymbol
//    var vertexId = 0
//
//    inputGraph.addStartVertex(vertexId)
//    inputGraph.addVertex(vertexId)
//
//    while (true) {
//        token = lexer.yylex()
//        if (token.type == JavaToken.EOF) break
//        println(token.type.toString() + " " + token.value)
//        inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token.type)), ++vertexId)
//        inputGraph.addVertex(vertexId)
//    }
//
//    val result = gll.parse()
//    writeSppfToDot(result.first!!, "./result.dot")

//    File("/home/hollowcoder/Programming/SRC/UCFS/src/jmh/resources/OpenJDKSourcesProcessed/").walk().filter {it.isFile}.forEach {inputPath ->
//        val file = File(inputPath.path)
//        val newFile = File("/home/hollowcoder/Programming/SRC/UCFS/src/jmh/resources/OpenJDKSourcesProcessedErrorFree/${file.name}")
//        newFile.delete()
//        val input = file.readText()
//        val grammar = JavaGrammar().getRsm()
//        val inputGraph = LinearInput<Int, LinearInputLabel>()
//        val gll = Gll(grammar, inputGraph, RecoveryMode.OFF, ReachabilityMode.REACHABILITY)
//        val lexer = JavaLexer(StringReader(input))
//        var token: JavaToken
//        var vertexId = 0
//
//        inputGraph.addStartVertex(vertexId)
//        inputGraph.addVertex(vertexId)
//
//        while (true) {
//            try {
//                token = lexer.yylex()
//            } catch (e: java.lang.Error) {
//                return@forEach
//            }
//            if (token == JavaToken.EOF) break
//            inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token)), ++vertexId)
//            inputGraph.addVertex(vertexId)
//        }
//        val result: Pair<SppfNode<Int>?, HashMap<Pair<Int, Int>, Int>>
//        try {
//            result = gll.parse()
//        } catch (e : java.lang.Error) {
//            return@forEach
//        }
//        if (result.first != null) {
//            file.copyTo(newFile)
//        } else {
////            println(file.name)
//        }
//    }

}