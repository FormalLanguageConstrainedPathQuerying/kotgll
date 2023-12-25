package org.srcgll

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.lexer.JavaGrammar
import org.srcgll.rsm.readRSMFromTXT
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.writeSPPFToDOT
import org.srcgll.lexer.JavaLexer
import org.srcgll.lexer.JavaToken
import org.srcgll.lexer.SymbolCode
import org.srcgll.rsm.writeRSMToDOT
import java.io.File
import java.io.StringReader

enum class RecoveryMode {
    ON, OFF,
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


    val input = File(pathToInput).readText().replace("\n", "").trim()
    val grammar = JavaGrammar().getRsm()
    val inputGraph = LinearInput<Int, LinearInputLabel>()
    val lexer = JavaLexer(StringReader(input))
    val gll = GLL(grammar, inputGraph, RecoveryMode.ON)
    var vertexId = 0
    var token : JavaToken

    writeRSMToDOT(grammar, "./rsm.dot")

    inputGraph.addStartVertex(vertexId)
    inputGraph.addVertex(vertexId)

    while (true) {
        token = lexer.yylex() as JavaToken
        if (token == JavaToken.EOF) break
        println(token.name)
        inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token)), ++vertexId)
        inputGraph.addVertex(vertexId)
    }

    val result = gll.parse()
    writeSPPFToDOT(result.first!!, "./result.dot")
}
