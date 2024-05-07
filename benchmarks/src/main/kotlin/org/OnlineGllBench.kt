package org

import kotlinx.benchmark.*
import org.junit.Before
import org.junit.Test
import org.openjdk.jmh.annotations.Threads
import org.ucfs.Java8
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll
import org.ucfs.sppf.buildStringFromSppf


@State(Scope.Benchmark)
class OnlineGllBench {

    @Param("Throwables.java")
    var fileName: String = "BaseTestRunner.java"

    lateinit var fileContents: String

    val startState = Java8().rsm
    lateinit var tokens: LinearInput<Int, LinearInputLabel>

    @Setup
    @Before
    fun prepare() {
        fileContents = OnlineGllBench::class.java.classLoader
            .getResource(fileName)?.readText() ?: throw Exception("File $fileName does not exists")
        tokens = getTokenStream(fileContents)
//        val gll = Gll.gll(
//            startState,
//            tokens
//        )
//        gll.parse().first ?: throw Exception("File $fileName cant be parsed by online gll")
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val gll = Gll.gll(startState, getTokenStream(fileContents))
        blackhole.consume(gll.parse())
    }

    @Test
    fun testGll() {
        val gll = Gll.gll(startState, getTokenStream(fileContents))
        val res = gll.parse().first
        println(buildStringFromSppf(res!!))
    }
}
