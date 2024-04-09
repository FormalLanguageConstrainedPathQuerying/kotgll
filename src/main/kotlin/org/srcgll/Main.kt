package org.srcgll

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.srcgll.input.*
import org.srcgll.parser.Gll
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRsmToDot
import org.srcgll.sppf.writeSppfToDot
import org.srcgll.lexer.*
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.readRsmFromDot
import org.srcgll.sppf.node.SppfNode
import java.io.File
import java.io.StringReader

enum class RecoveryMode {
    ON, OFF,
}

fun testDyck(input: String) {
    val rsm = Dyck().rsm
    val inputGraph = RecoveryLinearInput.buildFromString(input)
    val gll = Gll.recoveryGll(rsm, inputGraph)

    val result = gll.parse()

    writeSppfToDot(result.first!!, "./result.dot")
    writeRsmToDot(rsm, "./rsm.dot")
}

fun main(args: Array<String>) {
    val parser = ArgParser("srcgll")

    val recoveryMode by parser.option(
        ArgType.Choice<RecoveryMode>(), fullName = "recovery", description = "Recovery mode"
    ).default(RecoveryMode.ON)

    val pathToInput by parser.option(
        ArgType.String, fullName = "inputPath", description = "Path to input txt file"
    ).required()

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
    val input: String = File(pathToInput).readText()

    testDyck(input)
}