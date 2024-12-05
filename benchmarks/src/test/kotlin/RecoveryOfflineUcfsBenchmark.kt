import org.ucfs.input.LinearInputLabel
import kotlin.test.Ignore

@Ignore
class RecoveryOfflineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "RecUcfsOff"
    override fun parse(text: String) {
        val parser = org.ucfs.Java8ParserRecovery<Int, LinearInputLabel>()
        parser.setInput(getTokenStream(text))
        assert(parser.parse().first!= null){"can't build sppf"}
    }
}