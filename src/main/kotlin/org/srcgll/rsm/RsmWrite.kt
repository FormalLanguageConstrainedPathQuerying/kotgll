package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal
import java.io.File


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

fun writeRsmToDot(startState: RsmState, pathToTXT: String) {
    var lastId = 0
    val stateToId: HashMap<RsmState, Int> = HashMap()

    fun getId(state: RsmState): Int {
        return stateToId.getOrPut(state) { lastId++ }
    }

    val states = getAllStates(startState)
    val boxes: HashMap<Nonterminal, HashSet<RsmState>> = HashMap()

    for (state in states) {
        if (!boxes.containsKey(state.nonterminal)) {
            boxes[state.nonterminal] = HashSet()
        }
        boxes.getValue(state.nonterminal).add(state)
    }

    File(pathToTXT).printWriter().use { out ->
        out.println("digraph g {")

        states.forEach { state ->
            val shape = if (state.isFinal) "doublecircle" else "circle"
            val color = if (state.isStart) "green" else if (state.isFinal) "red" else "black"
            val id = getId(state)
            val name = state.nonterminal.name
            out.println("$id [label = \"$name,$id\", shape = $shape, color = $color]")
        }

        fun getView(symbol: Symbol) {
            when (symbol) {
                is Nonterminal -> symbol.name
                is Terminal<*> -> symbol.value
                else -> symbol.toString()
            }
        }
        states.forEach { state ->
            state.outgoingEdges.forEach { (symbol, destStates) ->
                destStates.forEach { destState ->
                    out.println("${getId(state)} -> ${getId(destState)} [label = \"${getView(symbol)}\"]")
                }
            }
        }

        boxes.forEach { box ->
            out.println("subgraph cluster_${box.key.name} {")

            box.value.forEach { state ->
                out.println("${getId(state)}")
            }
            out.println("label = \"${box.key.name}\"")
            out.println("}")
        }
        out.println("}")
    }
}
