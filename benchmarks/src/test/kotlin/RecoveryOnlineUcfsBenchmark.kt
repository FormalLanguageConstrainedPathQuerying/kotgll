import org.ucfs.Java8
import org.ucfs.parser.Gll
import kotlin.test.Ignore

@Ignore
class RecoveryOnlineUcfsBenchmark : ParsingBenchmarks() {

    override fun getShortName(): String = "RecUcfsOn"


    override fun parse(text: String) {
        val startState = Java8().rsm
        val tokens = getTokenStream(text)
        val gll = Gll.gll(startState, tokens)
        gll.parse()
    }
}