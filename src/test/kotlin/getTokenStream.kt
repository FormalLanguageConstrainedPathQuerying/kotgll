import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.symbol.Terminal

fun getTokenStream(input: String): LinearInput<Int, LinearInputLabel> {
    var curVertexId = 0
    val inputGraph = LinearInput<Int, LinearInputLabel>()

    inputGraph.addVertex(curVertexId)

    for (x in input) {
        inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
        inputGraph.addVertex(curVertexId)
    }
    inputGraph.addStartVertex(0)

    return inputGraph
}

fun getTokenStream(input: MutableList<String>): LinearInput<Int, LinearInputLabel> {
    var curVertexId = 0
    val inputGraph = LinearInput<Int, LinearInputLabel>()

    inputGraph.addVertex(curVertexId)

    for (x in input) {
        inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x)), ++curVertexId)
        inputGraph.addVertex(curVertexId)
    }
    inputGraph.addStartVertex(0)

    return inputGraph
}