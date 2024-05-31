package org

import java8.JavaLexer
import java8.JavaToken
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


fun getTokenStream(input: String): LinearInput<Int, LinearInputLabel> {
    val graph = LinearInput<Int, LinearInputLabel>()
    getTokenStream(input, graph)
    return graph
}



fun <G : IInputGraph<Int, LinearInputLabel>> getTokenStream(input: String, inputGraph: G): G {
    val lexer = JavaLexer(StringReader(input))
    var token: JavaToken
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    while (true) {
        token = lexer.yylex() as JavaToken
        if (token == JavaToken.EOF) break
        inputGraph.addEdge(vertexId, LinearInputLabel(token), ++vertexId)
    }

    return inputGraph
}

fun getCharStream(input: String): LinearInput<Int, LinearInputLabel> {
    val inputGraph = LinearInput<Int, LinearInputLabel>()
    var vertexId = 1

    inputGraph.addVertex(vertexId)
    inputGraph.addStartVertex(vertexId)

    for (ch in input) {
        inputGraph.addEdge(vertexId, LinearInputLabel(Term(ch.toString())), ++vertexId)
        inputGraph.addVertex(vertexId)
    }

    return inputGraph
}
