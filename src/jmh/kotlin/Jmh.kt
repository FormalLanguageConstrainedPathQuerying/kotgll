//package jmh.kotlin
//
//import org.antlr.Java8Lexer
//import org.antlr.Java8Parser
//import org.antlr.v4.runtime.CharStreams
//import org.antlr.v4.runtime.CommonTokenStream
//import org.openjdk.jmh.annotations.*
//import org.openjdk.jmh.infra.Blackhole
//import org.openjdk.jmh.infra.Control
//import org.openjdk.jmh.results.format.ResultFormatType
//import org.openjdk.jmh.runner.Runner
//import org.openjdk.jmh.runner.options.OptionsBuilder
//import org.srcgll.Gll
//import org.srcgll.ReachabilityMode
//import org.srcgll.RecoveryMode
//import org.srcgll.lexer.JavaGrammar
//import org.srcgll.rsm.RsmState
//import java.io.File
//import java.util.concurrent.TimeUnit
//
////val pathToInput = "/home/hollowcoder/Programming/SRC/UCFS/src/jmh/resources/junit4SourcesProcessedErrorFree/"
//
//@State(Scope.Benchmark)
//open class JmhBenchmark {
//
//    @State(Scope.Thread)
//    open class AntlrState{
//        lateinit var file: File
//
//        companion object {
//            lateinit var sources: Iterator<File>
//        }
//
//        @Setup(Level.Trial)
//        fun prepare() {
//            sources = File(pathToInput).walk().filter { it.isFile }.iterator()
//            file = sources.next()
//        }
//    }
//
//    @State(Scope.Thread)
//    open class GllState{
//        lateinit var file: File
//
//        lateinit var startStateJavaTokenized: RsmState
//
//        companion object {
//            lateinit var sources: Iterator<File>
//        }
//
//        @Setup(Level.Trial)
//        fun prepare() {
//            startStateJavaTokenized = JavaGrammar().getRsm()
//            sources = File(pathToInput).walk().filter { it.isFile }.iterator()
//            file = sources.next()
//        }
//    }
//
//    @Benchmark
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    fun measureAntlr(stateObject: AntlrState, blackhole: Blackhole) {
//        val antlrParser = Java8Parser(CommonTokenStream(Java8Lexer(CharStreams.fromString(stateObject.file.readText()))))
//        blackhole.consume(antlrParser.compilationUnit())
//    }
//
//    @Benchmark
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    fun measureGll(stateObject: GllState, blackhole: Blackhole) {
//        val inputGraph = getTokenStream(stateObject.file.readText())
//        val gll = Gll(stateObject.startStateJavaTokenized, inputGraph, recovery = RecoveryMode.ON, reachability = ReachabilityMode.REACHABILITY)
//
//        blackhole.consume(gll.parse())
//    }
//}
