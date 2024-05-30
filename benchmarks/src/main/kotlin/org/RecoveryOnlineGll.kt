package org

import java7.Java7
import kotlinx.benchmark.*
import org.junit.Before
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll


@State(Scope.Benchmark)
class RecoveryOnlineGll : BaseBench() {

    val startState = Java7().rsm
    lateinit var tokens: LinearInput<Int, LinearInputLabel>

    @Setup
    @Before
    override fun prepare() {
        super.prepare()
        tokens = getTokenStream(fileContents)
    }

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val gll = Gll.recoveryGll(startState, getTokenStream(fileContents))
        blackhole.consume(gll.parse())
    }
}
