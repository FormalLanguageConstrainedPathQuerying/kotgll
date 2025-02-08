package org.ucfs.input.utils


import org.ucfs.input.ILabel
import org.ucfs.input.InputGraph
import org.ucfs.input.TerminalInputLabel
import java.lang.StringBuilder

class DotWriter {

    fun getDotView(graph: InputGraph<Int, TerminalInputLabel>, isDirected: Boolean = true): String {
        val builder = StringBuilder()
        val graphType = if (isDirected) "digraph" else "graph"
        val connector = if (isDirected) "->" else "--"

        builder.append("$graphType ${graph.name} {\n")

        for (vertex in graph.startVertices) {
            builder.append("    start $connector $vertex;\n")
        }

        for ((from, edges) in graph.edges) {
            for (edge in edges) {
                val to = edge.head
                val label = edge.label.terminal.toString()
                builder.append("    $from $connector $to [label = $label];\n")
            }
        }

        builder.append("}")
        return builder.toString()
    }
}
