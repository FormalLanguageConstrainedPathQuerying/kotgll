import org.ucfs.input.LinearInputLabel

class OfflineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "UcfsOff"

    override fun parse(text: String) {
        val parser = org.ucfs.Java8Parser<Int, LinearInputLabel>()
        parser.setInput(getTokenStream(text))
        parser.parse()
    }
}