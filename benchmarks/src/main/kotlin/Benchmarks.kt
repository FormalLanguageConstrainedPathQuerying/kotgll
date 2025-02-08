import org.ucfs.JavaToken
import org.ucfs.Scanner
import org.ucfs.input.*
import org.ucfs.rsm.symbol.Term
import java.io.StringReader

fun getResultPath(
    pathToOutput: String,
    inputName: String,
    grammarMode: String,
    grammarName: String,
    sppfMode: String,
): String {
    return pathToOutput + (if (pathToOutput.endsWith("/")) "" else "/") + "${inputName}_${grammarMode}_${grammarName}_${sppfMode}.csv"
}


fun getTokenStream(input: String): LinearInput<Int, TerminalInputLabel> {
    val graph = LinearInput<Int, TerminalInputLabel>()
    getTokenStream(input, graph)
    return graph
}



fun <G : IInputGraph<Int, TerminalInputLabel>> getTokenStream(input: String, inputGraph: G): G {
    val lexer = Scanner(StringReader(input))
    var token: JavaToken
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    while (true) {
        token = lexer.yylex() as JavaToken
        if (token == JavaToken.EOF) break
        inputGraph.addEdge(vertexId, TerminalInputLabel(token), ++vertexId)
    }

    return inputGraph
}

fun getCharStream(input: String): LinearInput<Int, TerminalInputLabel> {
    val inputGraph = LinearInput<Int, TerminalInputLabel>()
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    for (ch in input) {
        inputGraph.addEdge(vertexId, TerminalInputLabel(Term(ch.toString())), ++vertexId)
        inputGraph.addVertex(vertexId)
    }

    return inputGraph
}
