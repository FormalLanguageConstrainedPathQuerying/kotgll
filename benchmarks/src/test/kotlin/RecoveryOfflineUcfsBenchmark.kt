import org.ucfs.input.LinearInputLabel

class RecoveryOfflineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "RecUcfsOff"
    override fun parse(text: String) {
        val parser = org.ucfs.Java8ParserRecovery<Int, LinearInputLabel>()
        parser.setInput(getTokenStream(text))
        parser.parse()
    }
}