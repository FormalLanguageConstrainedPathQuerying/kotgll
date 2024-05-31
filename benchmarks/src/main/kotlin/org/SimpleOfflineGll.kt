package org

import kotlinx.benchmark.*
import org.ucfs.input.LinearInputLabel


@State(Scope.Benchmark)
class SimpleOfflineGll : BaseBench() {

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.setInput(getTokenStream(fileContents))
        blackhole.consume(parser.parse())
    }
}
