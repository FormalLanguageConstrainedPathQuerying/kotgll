package org.ucfs.rsm

import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.rsm.symbol.Symbol
import org.ucfs.rsm.symbol.Term
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private fun getAllStates(startState: RsmState): HashSet<RsmState> {
    val states: HashSet<RsmState> = HashSet()
    val queue = ArrayDeque(listOf(startState))
    while (!queue.isEmpty()) {
        val state = queue.removeFirst()
        if (!states.contains(state)) {
            states.add(state)

            for ((symbol, destStates) in state.outgoingEdges) {
                if (symbol is Nonterminal) {
                    queue.addLast(symbol.startState)
                }
                for (destState in destStates) {
                    queue.addLast(destState)
                    queue.addLast(destState.nonterminal.startState)
                }
            }
        }
    }
    return states
}

fun getView(symbol: Symbol): String {
    return when (symbol) {
        is Nonterminal -> symbol.name ?: "unnamed nonterminal ${symbol.hashCode()}"
        is Term<*> -> symbol.value.toString()
        else -> symbol.toString()
    }
}

fun writeRsmToTxt(startState: RsmState, pathToTXT: String) {
    val states = getAllStates(startState)
    File(pathToTXT).printWriter().use { out ->
        out.println(
            """StartState(
            |id=${startState.id},
            |nonterminal=Nonterminal("${startState.nonterminal.name}"),
            |isStart=${startState.isStart},
            |isFinal=${startState.isFinal}
            |)"""
                .trimMargin()
                .replace("\n", "")
        )

        states.forEach { state ->
            out.println(
                """State(
                |id=${state.id},
                |nonterminal=Nonterminal("${state.nonterminal.name}"),
                |isStart=${state.isStart},
                |isFinal=${state.isFinal}
                |)"""
                    .trimMargin()
                    .replace("\n", "")
            )
        }

        fun getSymbolView(symbol: Symbol): Triple<String, String, String> {
            return when (symbol) {
                is Term<*> -> Triple("Terminal", symbol.value.toString(), "terminal")
                is Nonterminal -> Triple("Nonterminal", symbol.name ?: "NON_TERM", "nonterminal")
                else -> throw Exception("Unsupported implementation of Symbol instance: ${symbol.javaClass}")
            }
        }

        for (state in states) {
            for ((symbol, destStates) in state.outgoingEdges) {
                val (typeView, symbolView, typeLabel) = getSymbolView(symbol)
                for (destState in destStates) {
                    out.println(
                        """${typeView}Edge(
                        |tail=${state.id},
                        |head=${destState.id},
                        |$typeLabel=$typeView("$symbolView")
                        |)""".trimMargin().replace("\n", "")
                    )
                }
            }
        }
    }
}

fun writeRsmToDot(startState: RsmState, filePath: String) {
    val states = getAllStates(startState)
    val boxes: HashMap<Nonterminal, HashSet<RsmState>> = HashMap()

    for (state in states) {
        if (!boxes.containsKey(state.nonterminal)) {
            boxes[state.nonterminal] = HashSet()
        }
        boxes.getValue(state.nonterminal).add(state)
    }

    Files.createDirectories(Paths.get("gen"))
    val file = File(Path.of("gen", filePath).toUri())

    file.printWriter().use { out ->
        out.println("digraph g {")

        states.forEach { state ->
            val shape = if (state.isFinal) "doublecircle" else "circle"
            val color =
                if (state == startState) "purple" else if (state.isStart) "green" else if (state.isFinal) "red" else "black"
            val id = state.id
            val name = state.nonterminal.name
            out.println("$id [label = \"$name,$id\", shape = $shape, color = $color]")
        }

        states.forEach { state ->
            state.outgoingEdges.forEach { (symbol, destStates) ->
                destStates.forEach { destState ->
                    out.println("${state.id} -> ${destState.id} [label = \"${getView(symbol)}\"]")
                }
            }
        }

        boxes.forEach { box ->
            out.println("subgraph cluster_${box.key.name} {")

            box.value.forEach { state ->
                out.println(state.id)
            }
            out.println("label = \"${box.key.name}\"")
            out.println("}")
        }
        out.println("}")
    }
}
