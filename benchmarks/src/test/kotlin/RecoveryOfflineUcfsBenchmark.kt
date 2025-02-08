import org.ucfs.input.TerminalInputLabel
import kotlin.test.Ignore

@Ignore
class RecoveryOfflineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "RecUcfsOff"
    override fun parse(text: String) {
        val parser = org.ucfs.Java8ParserRecovery<Int, TerminalInputLabel>()
        parser.setInput(getTokenStream(text))
        assert(parser.parse().first!= null){"can't build sppf"}
    }
}