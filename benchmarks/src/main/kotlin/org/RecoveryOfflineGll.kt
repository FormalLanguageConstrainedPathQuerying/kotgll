package org

import kotlinx.benchmark.*
import org.ucfs.Java8
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll


@State(Scope.Benchmark)
class RecoveryOfflineGll : BaseBench() {

    @Benchmark
    fun measureGll(blackhole: Blackhole) {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.input = getRecoveryTokenStream(fileContents)
        blackhole.consume(parser.parse())
    }
}
//fun main(){
//    val inp = getRecoveryTokenStream(
//        """            public clas HellowWorldCTY{
//              }
//
//        """.trimIndent()
//    )
//    val p = Gll.recoveryGll(Java8().rsm, inp)
//    assert(p.parse().first != null)
//    val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
//    parser.input = inp
//    assert(parser.parse().first != null)
//}
