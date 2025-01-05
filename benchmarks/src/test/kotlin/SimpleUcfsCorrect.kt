import org.junit.jupiter.api.Test
import org.ucfs.input.IInputGraph
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll
import org.ucfs.rsm.symbol.Term
import org.ucfs.rsm.writeRsmToDot
import org.ucfs.simple.SimpleGrammar
import org.ucfs.sppf.writeSppfToDot
import kotlin.test.Ignore

@Ignore
class SimpleUcfsCorrect {
    val grammar = SimpleGrammar()

    @Test
    fun parseOne() {
        val startState = grammar.rsm
        val tokens = getTokenStream(sourceCode)
        val gll = Gll.gll(startState, tokens)
        val sppf = gll.parse().first
        writeRsmToDot(grammar.rsm, "simple_grammar.dot")
        assert(sppf != null)
        writeSppfToDot(sppf!!, "simple_beeb.dot")
    }


    fun getTokenStream(input: List<Term<String>>): IInputGraph<Int, LinearInputLabel> {
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var vertexId = 1

        inputGraph.addVertex(vertexId)
        inputGraph.addStartVertex(vertexId)
        for (term in input) {
            inputGraph.addEdge(vertexId, LinearInputLabel(term), ++vertexId)
        }

        return inputGraph
    }

    val sourceCode: List<Term<String>>
        get() = listOf(grammar.c, grammar.c)
}