package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import java.io.File

fun writeRSMToTXT(startState: RSMState, pathToTXT: String) {
    val states : ArrayList<RSMState>  = ArrayList()
    val queue  : ArrayDeque<RSMState> = ArrayDeque(listOf(startState))

    while (!queue.isEmpty()) {
        val state = queue.removeFirst()

        if (!states.contains(state)) states.add(state)

        for (kvp in state.outgoingTerminalEdges) {
            for (head in kvp.value) {
                if (!states.contains(head))
                    queue.addLast(head)
            }
        }

        for (kvp in state.outgoingNonterminalEdges) {
            for (head in kvp.value) {
                if (!states.contains(head))
                    queue.addLast(head)
                if (!states.contains(kvp.key.startState))
                    queue.addLast(kvp.key.startState)
                if (!states.contains(head.nonterminal.startState))
                    queue.addLast(head.nonterminal.startState)
            }
        }
    }

    File(pathToTXT).printWriter().use { out ->
        out.println(
            """StartState(
            |id=${startState.id},
            |nonterminal=Nonterminal("${startState.nonterminal.value}"),
            |isStart=${startState.isStart},
            |isFinal=${startState.isFinal}
            |)"""
            .trimMargin()
            .replace("\n", ""))

        states.forEach { state ->
            out.println(
                """State(
                |id=${state.id},
                |nonterminal=Nonterminal("${state.nonterminal.value}"),
                |isStart=${state.isStart},
                |isFinal=${state.isFinal}
                |)"""
                .trimMargin()
                .replace("\n", ""))
        }

        states.forEach { state ->
            state.outgoingTerminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println(
                        """TerminalEdge(
                        |tail=${state.id},
                        |head=${head.id},
                        |terminal=Terminal("${edge.key.value}")
                        |)"""
                        .trimMargin()
                        .replace("\n", ""))
                }
            }
            state.outgoingNonterminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println(
                        """NonterminalEdge(
                        |tail=${state.id},
                        |head=${head.id},
                        |nonterminal=Nonterminal("${head.nonterminal.value}")
                        |)"""
                        .trimMargin()
                        .replace("\n", ""))
                }
            }
        }
    }

}
fun writeRSMToDOT(startState: RSMState, pathToTXT: String) {
    val states : HashSet<RSMState> = HashSet()
    val queue  : ArrayDeque<RSMState> = ArrayDeque(listOf(startState))
    var state  : RSMState
    val boxes  : HashMap<Nonterminal, HashSet<RSMState>> = HashMap()

    while (!queue.isEmpty()) {
        state = queue.removeFirst()

        if (!states.contains(state)) states.add(state)

        for (kvp in state.outgoingTerminalEdges) {
            for (head in kvp.value) {
                if (!states.contains(head))
                    queue.addLast(head)
            }
        }

        for (kvp in state.outgoingNonterminalEdges) {
            for (head in kvp.value) {
                if (!states.contains(head))
                    queue.addLast(head)
                if (!states.contains(kvp.key.startState))
                    queue.addLast(kvp.key.startState)
                if (!states.contains(head.nonterminal.startState))
                    queue.addLast(head.nonterminal.startState)
            }
        }
    }

    for (state in states) {
        if (!boxes.containsKey(state.nonterminal)) {
            boxes[state.nonterminal] = HashSet()
        }
        boxes.getValue(state.nonterminal).add(state)
    }

    File(pathToTXT).printWriter().use { out ->
        out.println("digraph g {")

        states.forEach { state ->
            if (state.isStart)
                out.println("${state.id} [label = \"${state.nonterminal.value},${state.id}\", shape = doublecircle, color = green]")
            else if (state.isFinal)
                out.println("${state.id} [label = \"${state.nonterminal.value},${state.id}\", shape = doublecircle, color = red]")
            else
                out.println("${state.id} [label = \"${state.nonterminal.value},${state.id}\", shape = circle]")
        }

        states.forEach { state ->
            state.outgoingTerminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println("${state.id} -> ${head.id} [label = \"${edge.key.value}\"]")
                }
            }
            state.outgoingNonterminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println("${state.id} -> ${head.id} [label = ${edge.key.value}]")
                }
            }
        }

        boxes.forEach { box ->
            out.println("subgraph cluster_${box.key.value} {")

            box.value.forEach { state ->
                out.println("${state.id}")
            }
            out.println("label = \"${box.key.value}\"")
            out.println("}")
        }
        out.println("}")
    }
}
