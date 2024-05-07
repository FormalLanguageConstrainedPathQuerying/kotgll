package org

import kotlinx.benchmark.*
import org.junit.Before
import org.junit.Test
import org.ucfs.input.LinearInputLabel


@State(Scope.Benchmark)
class OfflineGllBench {

    @Param("Throwables.java")
    var fileName: String = "BaseTestRunner.java"

    lateinit var fileContents: String

    @Setup
    @Before
    fun prepare() {
        fileContents = OfflineGllBench::class.java.classLoader
            .getResource(fileName)?.readText() ?: throw Exception("File $fileName does not exists")
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.input = getTokenStream(fileContents)
        blackhole.consume(parser.parse())
    }

    @Test
    fun testGll() {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.input = getTokenStream(fileContents)
        parser.parse().first!!
    }
}
