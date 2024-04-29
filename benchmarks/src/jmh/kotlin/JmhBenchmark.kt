import antlr4.Java8Lexer
import antlr4.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import grammars.JavaGrammar
import lexers.JavaLexer
import lexers.JavaToken
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.input.RecoveryLinearInput
import org.ucfs.parser.Gll
import org.ucfs.rsm.symbol.Term
import org.ucfs.sppf.buildTokenStreamFromSppf
import java.io.File
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

val logger: Logger = Logger.getLogger("Logger")
val fh: FileHandler = FileHandler("Benchmarks.log", true)
val pathToInput = "./../../src/jmh/src_files_processed/"

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
        inputGraph.addEdge(vertexId, LinearInputLabel(Term(token)), ++vertexId)
    }

    return inputGraph
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
open class AntlrBenchmark {

    @Param()
    var filename: String = ""

    lateinit var fileContents: String

    @Setup(Level.Trial)
    fun prepare() {
        if (logger.handlers.isEmpty()) {
            logger.addHandler(fh)
            fh.setFormatter(SimpleFormatter())
        }
        logger.info("Benchmarking ANTLR, Processing file: ${filename}")
        fileContents = File(pathToInput + filename).readText()
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun measureAntlr(blackhole: Blackhole) {
        val antlrParser = Java8Parser(CommonTokenStream(Java8Lexer(CharStreams.fromString(fileContents))))
        blackhole.consume(antlrParser.compilationUnit())
    }
}

@State(Scope.Benchmark)
open class GllRecoveryBenchmark {
    @Param()
    var filename: String = ""
    val startState = JavaGrammar().rsm

    lateinit var fileContents: String

    @Setup(Level.Trial)
    fun prepare() {
        if (logger.handlers.isEmpty()) {
            logger.addHandler(fh)
            fh.setFormatter(SimpleFormatter())
        }
        logger.info("Benchmarking Recovery GLL, Processing file: ${filename}")
        fileContents = File(pathToInput + filename).readText()
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun measureGll(blackhole: Blackhole) {
        val inputGraph = getTokenStream(fileContents)
        val gll = Gll.recoveryGll(startState, inputGraph as RecoveryLinearInput)

        blackhole.consume(gll.parse())
    }
}

@State(Scope.Benchmark)
open class GllDefaultBenchmark {
    @Param()
    var filename: String = ""
    val startState = JavaGrammar().rsm

    lateinit var fileContents: String

    @Setup(Level.Trial)
    fun prepare() {
        if (logger.handlers.isEmpty()) {
            logger.addHandler(fh)
            fh.setFormatter(SimpleFormatter())
        }
        logger.info("Benchmarking Default GLL, Processing file: ${filename}")
        val gll = Gll.gll(startState, getTokenStream(File(pathToInput + filename).readText()))
        val parseResult = gll.parse()

        fileContents = buildTokenStreamFromSppf(parseResult.first!!).joinToString(" ")
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    fun measureGll(blackhole: Blackhole) {
        val inputGraph = getTokenStream(fileContents)
        val gll = Gll.gll(startState, inputGraph)

        blackhole.consume(gll.parse())
    }
}