package org

import kotlinx.benchmark.*
import org.ucfs.input.LinearInputLabel


@State(Scope.Benchmark)
class RecoveryOfflineGll : BaseBench() {

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val parser = org.ucfs.Java7RecoveryParser<Int, LinearInputLabel>()
        parser.input = getTokenStream(fileContents)
        blackhole.consume(parser.parse())
    }
}
