package org.srcgll

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.srcgll.rsm.readRSMFromTXT
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRSMToDOT
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import java.io.*
import org.srcgll.lexer.GeneratedLexer
import org.srcgll.lexer.SymbolCode
import org.srcgll.sppf.writeSPPFToDOT
import org.srcgll.sppf.buildStringFromSPPF

enum class RecoveryMode
{
    ON,
    OFF,
}

fun main(args : Array<String>)
{
    val parser = ArgParser("srcgll")

    val recoveryMode by
    parser
        .option(ArgType.Choice<RecoveryMode>(), fullName = "recovery", description = "Recovery mode")
        .default(RecoveryMode.ON)

    val pathToInput by
    parser
        .option(ArgType.String, fullName = "inputPath", description = "Path to input txt file")
        .required()

    val pathToGrammar by
    parser
        .option(ArgType.String, fullName = "grammarPath", description = "Path to grammar txt file")
        .required()

    val pathToOutputString by
    parser
        .option(ArgType.String, fullName = "outputStringPath", description = "Path to output txt file")
        .required()

    val pathToOutputSPPF by
    parser
        .option(ArgType.String, fullName = "outputSPPFPath", description = "Path to output dot file")
        .required()

    parser.parse(args)


    val input    = File(pathToInput).readText().replace("\n","").trim()
    val grammar  = readRSMFromTXT(pathToGrammar)
    val inputGraph = LinearInput<Int, LinearInputLabel>()
    val gll = GLL(grammar, inputGraph, RecoveryMode.ON)
    var vertexId = 0

    inputGraph.addStartVertex(vertexId)
    inputGraph.addVertex(vertexId)

    for (x in input) {
        inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(x.toString())), ++vertexId)
        inputGraph.addVertex(vertexId)
    }

    // Parse result for initial input
    var result = gll.parse()

    writeSPPFToDOT(result.first!!, pathToOutputSPPF + "before.dot")

    inputGraph.addEdge(vertexId, LinearInputLabel(Terminal("a")), ++vertexId)
    inputGraph.addVertex(vertexId)

    // If new edge was added to graph - we need to recover corresponding descriptors and add them to
    // descriptors stack and proceed with parsing them
    result = gll.parse(vertexId - 1)

    writeSPPFToDOT(result.first!!, pathToOutputSPPF + "after.dot")
}
