package org

import kotlinx.benchmark.*
import org.junit.Before
import org.ucfs.Java8
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll


@State(Scope.Benchmark)
class OnlineGll : BaseBench(){

    val startState = Java8().rsm
    lateinit var tokens: LinearInput<Int, LinearInputLabel>

    @Setup
    @Before
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
