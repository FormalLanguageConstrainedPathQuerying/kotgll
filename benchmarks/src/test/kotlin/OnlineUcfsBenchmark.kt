import org.ucfs.Java8
import org.ucfs.parser.Gll
import org.ucfs.sppf.writeSppfToDot
import java.io.File

class OnlineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "UcfsOn"

    override fun parse(text: String) {
        val startState = Java8().rsm
        val tokens = getTokenStream(text)
        val gll = Gll.gll(startState, tokens)
        assert(gll.parse().first != null) { "can't build sppf" }
    }


    fun parseOne(sourceCode: String) {
        val startState = Java8().rsm
        val tokens = getTokenStream(sourceCode)
        val gll = Gll.gll(startState, tokens)
        val sppf = gll.parse().first
        assert(sppf != null) { "can't build sppf" }
        writeSppfToDot(sppf!!, "sppf.dot")
    }

    fun getSourceCode(path : String): String =
        File(path).readText()
}