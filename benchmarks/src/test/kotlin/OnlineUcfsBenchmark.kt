import java8.Java8
import org.ucfs.parser.Gll

class OnlineUcfsBenchmark : TimeParsingBenchmark() {
    override fun getShortName(): String = "UcfsOn"

    override fun parse(text: String) {
        val startState = Java8().rsm
        val tokens = getTokenStream(text)
        val gll = Gll.gll(startState, tokens)
        gll.parse()
    }
}