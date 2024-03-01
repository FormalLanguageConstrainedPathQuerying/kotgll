package jmh.kotlin

import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.srcgll.Gll
import org.srcgll.ReachabilityMode
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.lexer.*
import org.srcgll.rsm.readRsmFromTxt
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.writeSppfToDot
import java.io.File
import java.io.StringReader
import kotlin.system.measureNanoTime
import kotlin.time.measureTime


fun getResultPath(
    pathToOutput: String,
    inputName: String,
    grammarMode: String,
    grammarName: String,
    sppfMode: String,
): String {
    return pathToOutput + (if (pathToOutput.endsWith("/")) "" else "/") + "${inputName}_${grammarMode}_${grammarName}_${sppfMode}.csv"
}

fun getTokenStream(input: String): LinearInput<Int, LinearInputLabel> {
    val inputGraph = LinearInput<Int, LinearInputLabel>()
    val lexer = JavaLexer(StringReader(input))
    var token: JavaToken
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    while (true) {
        token = lexer.yylex() as JavaToken
        if (token == JavaToken.EOF) break
        inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token)), ++vertexId)
    }

    return inputGraph
}

fun getCharStream(input: String): LinearInput<Int, LinearInputLabel> {
    val inputGraph = LinearInput<Int, LinearInputLabel>()
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    for (ch in input) {
        inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(ch.toString())), ++vertexId)
        inputGraph.addVertex(vertexId)
    }

    return inputGraph
}

fun main(args: Array<String>) {
    val parser = ArgParser("srcgll.benchmarks")

    val pathToInput by parser.option(
            ArgType.String, fullName = "inputPath", description = "Path to folder with inputs"
        ).required()
    val pathToGrammar by parser.option(
            ArgType.String, fullName = "grammarPath", description = "Path to grammar txt file"
        )
    val pathToOutput by parser.option(
            ArgType.String, fullName = "outputPath", description = "Path to folder with results"
        ).required()
    val warmUpRounds by parser.option(ArgType.Int, fullName = "warmUpRounds", description = "Number of warm-up rounds")
        .default(3)
    val benchmarksRounds by parser.option(
            ArgType.Int, fullName = "benchmarkRounds", description = "Number of benchmark rounds"
        ).default(10)

    parser.parse(args)
}

fun runRsmWithSppf(
    pathToInput: String,
    pathToRSM: String,
    pathToOutput: String,
    warmUpRounds: Int,
    benchmarkRounds: Int,
) {
    val rsm = readRsmFromTxt(pathToRSM)
    val rsmName = File(pathToRSM).nameWithoutExtension

    File(pathToInput).walk().filter { it.isFile }.forEach { inputPath ->
            val inputName = inputPath.nameWithoutExtension
            println("start:: $inputName")
            val input = File(inputPath.path).readText()

            val resultPath = getResultPath(pathToOutput, inputName, "rsm", rsmName, "with_sppf")
            File(resultPath).writeText("")

            val inputGraph = LinearInput<Int, LinearInputLabel>()
            val lexer = GeneratedLexer(StringReader(input))
            val gll = Gll(rsm, inputGraph, recovery = RecoveryMode.ON, reachability = ReachabilityMode.REACHABILITY)
            var token: SymbolCode
            var vertexId = 1

            inputGraph.addVertex(vertexId)
            inputGraph.addStartVertex(vertexId)

            while (true) {
                token = lexer.yylex() as SymbolCode
                if (token == SymbolCode.EOF) break
                inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token)), ++vertexId)
            }


            var result = gll.parse()

            writeSppfToDot(result.first!!, "./outputFiles/${inputName}_sppf.dot")

            for (warmUp in 1 .. warmUpRounds) {
                var result: Pair<SppfNode<Int>?, HashMap<Pair<Int, Int>, Int>>

                val elapsedRecovery = measureNanoTime {
                    result = gll.parse()
                }

                val elapsedRecoverySeconds = elapsedRecovery.toDouble() / 1_000_000_000.0

                println("warmup:: $inputName $rsmName $elapsedRecoverySeconds")
            }

            var totalRecoveryTime = 0.0

            for (benchmarkAttempt in 1 .. benchmarkRounds) {
                var result: Pair<SppfNode<Int>?, HashMap<Pair<Int, Int>, Int>>

                val elapsedRecovery = measureNanoTime {
                    result = Gll(rsm, inputGraph, recovery = RecoveryMode.ON, reachability = ReachabilityMode.REACHABILITY).parse()
                }

                val elapsedRecoverySeconds = elapsedRecovery.toDouble() / 1_000_000_000.0

                totalRecoveryTime += elapsedRecoverySeconds

                println("benchmark:: $inputName $elapsedRecoverySeconds")
                File(resultPath).appendText("${input.length} ::: $elapsedRecoverySeconds\n")
            }
            val averageRecoveryTime = totalRecoveryTime / benchmarkRounds

            File(resultPath).appendText("totalRecoveryTime: $totalRecoveryTime seconds\n")
            File(resultPath).appendText("averageRecoveryTime : $averageRecoveryTime seconds\n")
        }
}

fun compareAntlrIguana(
    pathtoInput: String,
    pathToOutput: String,
    warmUpRounds: Int,
    benchmarkRounds: Int,
) {
    val rsmJava = JavaGrammar().getRsm()
    val rsmJavaScannerless = JavaGrammarScannerless().getRsm()
    val resultPath = pathToOutput + (if (pathToOutput.endsWith("/")) "" else "/")
    val outputAntlrFile = File(resultPath + "antlr.csv")
    val outputGllFile = File(resultPath + "gll.csv")
    val badPerformanceFiles = File(resultPath + "tooLongToParse.csv")

    outputAntlrFile.writeText("")
    outputGllFile.writeText("")
    badPerformanceFiles.writeText("")

    var currentFileNumber = 1

    File(pathtoInput).walk().filter() { it.isFile }.forEach { inputPath ->
        val inputName = inputPath.nameWithoutExtension
        val input = File(inputPath.path).readText()

        val antlrLexer = Java8Lexer(CharStreams.fromString(input))
        val antlrTokens = CommonTokenStream(antlrLexer)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val lexer = JavaLexer(StringReader(input))
        var token: JavaToken
        var vertexId = 1

        inputGraph.addVertex(vertexId)
        inputGraph.addStartVertex(vertexId)

        try {
            while (true) {
                token = lexer.yylex() as JavaToken
                if (token == JavaToken.EOF) break
                inputGraph.addEdge(vertexId, LinearInputLabel(Terminal(token)), ++vertexId)
            }
        } catch (e : Error) {
            return@forEach
        }

        println("Starting: ${inputName}, current progress: ${currentFileNumber++}/40584")

        for (warmUp in 1 .. warmUpRounds) {
            val gll = Gll(rsmJava, inputGraph, recovery = RecoveryMode.OFF, reachability = ReachabilityMode.REACHABILITY)
            val antlrParser = Java8Parser(antlrTokens)

            val elapsedAntlr = measureTime {
                antlrParser.compilationUnit()
            }

            val elapsedGll = measureTime {
                gll.parse()
            }

            val elapsedAntlrSeconds = elapsedAntlr.inWholeNanoseconds.toDouble() / 1_000_000_000.0
            val elapsedGllSeconds = elapsedGll.inWholeNanoseconds.toDouble() / 1_000_000_000.0
        }

        var totalAntlrTime = 0.0
        var totalGllTime = 0.0

        for (benchmarkAttempt in 1 .. benchmarkRounds) {
            val gll = Gll(rsmJava, inputGraph, recovery = RecoveryMode.OFF, reachability = ReachabilityMode.REACHABILITY)
            val antlrParser = Java8Parser(antlrTokens)
            var gllParseResult : Pair<SppfNode<Int>?, HashMap<Pair<Int, Int>, Int>>
            var antlrParseResult : Java8Parser.CompilationUnitContext

            val elapsedAntlr = measureTime {
                antlrParseResult = antlrParser.compilationUnit()
            }

            val elapsedGll = measureTime {
                gllParseResult = gll.parse()
            }

            if (gllParseResult.first == null) {
                badPerformanceFiles.appendText("${inputName},Parse tree empty\n")
                return@forEach
            }

            val elapsedAntlrSeconds = elapsedAntlr.inWholeNanoseconds.toDouble() / 1_000_000_000.0
            val elapsedGllSeconds = elapsedGll.inWholeNanoseconds.toDouble() / 1_000_000_000.0

            totalAntlrTime += elapsedAntlrSeconds
            totalGllTime += elapsedGllSeconds
        }
        val averageAntlrTime = totalAntlrTime / benchmarkRounds
        val averageGllTime = totalGllTime / benchmarkRounds

        if (averageGllTime >= 1) {
            badPerformanceFiles.appendText("${inputName},${averageGllTime}\n")
        }

        outputAntlrFile.appendText("${input.length},${averageAntlrTime}\n")
        outputGllFile.appendText("${input.length},${averageGllTime}\n")
    }

//    val iguanaGrammar = IggyParserUtils.fromIggyGrammarPath("./src/main/kotlin/org/iguana/Java.iggy")
//    val iguanaInput = Input.fromString(input)
//    val iguanaParser = IguanaParser(iguanaGrammar)
}
