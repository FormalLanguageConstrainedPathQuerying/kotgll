package org

import java8.Java8
import kotlinx.benchmark.*
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll


@State(Scope.Benchmark)
class SimpleOnlineGll : BaseBench() {

    val startState = Java8().rsm
    lateinit var tokens: LinearInput<Int, LinearInputLabel>

    @Setup
    override fun prepare() {
        super.prepare()
        tokens = getTokenStream(fileContents)
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val gll = Gll.gll(startState, getTokenStream(fileContents))
        blackhole.consume(gll.parse())
    }
}
