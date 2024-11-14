
import org.junit.jupiter.api.Test
import org.ucfs.Java8
import org.ucfs.parser.Gll

class OnlineUcfsBenchmark : ParsingBenchmarks() {
    override fun getShortName(): String = "UcfsOn"

    override fun parse(text: String) {
        val startState = Java8().rsm
        val tokens = getTokenStream(text)
        val gll = Gll.gll(startState, tokens)
        assert(gll.parse().first != null)
    }

    @Test
    fun parseOne(){
        parse(sourceCode)
    }

    val sourceCode: String = """
      final class OpenHashSet {
        double i = 12.1;
      }
    """.trimIndent()
}