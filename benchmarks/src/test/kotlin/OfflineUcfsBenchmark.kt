import org.ucfs.input.TerminalInputLabel
import java.io.File

fun main(args: Array<String>) {
    try {
        OfflineUcfsBenchmark().parse(File(args[0]).readText())
    } catch (e: Throwable) {
        println(e)
        System.exit(1)
    }
}

class OfflineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "UcfsOff"

    fun main(args: Array<String>) {
        parse(File(args[0]).readText())
    }

    override fun parse(text: String) {
        val parser = org.ucfs.Java8Parser<Int, TerminalInputLabel>()
        parser.setInput(getTokenStream(text))
        parser.parse()
        assert(parser.parse().first != null)
    }
}