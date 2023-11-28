package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import java.io.File

fun writeRSMToTXT(startState: RSMState, pathToTXT: String) {
    var lastId = 0
    val stateToId: HashMap<RSMState, Int> = HashMap()

    fun getId(state: RSMState) {
        stateToId.getOrPut(state) { lastId++ }
    }

    val states: ArrayList<RSMState> = ArrayList()
    val queue: ArrayDeque<RSMState> = ArrayDeque(listOf(startState))


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
            |id=${getId(startState)},
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
                |id=${getId(state)},
                |nonterminal=Nonterminal("${state.nonterminal.name}"),
                |isStart=${state.isStart},
                |isFinal=${state.isFinal}
                |)"""
                    .trimMargin()
                    .replace("\n", "")
            )
        }

        states.forEach { state ->
            state.outgoingTerminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println(
                        """TerminalEdge(
                        |tail=${getId(state)},
                        |head=${getId(head)},
                        |terminal=Terminal("${edge.key.value}")
                        |)"""
                            .trimMargin()
                            .replace("\n", "")
                    )
                }
            }
            state.outgoingNonterminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println(
                        """NonterminalEdge(
                        |tail=${getId(state)},
                        |head=${getId(head)},
                        |nonterminal=Nonterminal("${head.nonterminal.name}")
                        |)"""
                            .trimMargin()
                            .replace("\n", "")
                    )
                }
            }
        }
    }

}

fun writeRSMToDOT(startState: RSMState, pathToTXT: String) {
    var lastId = 0
    val stateToId: HashMap<RSMState, Int> = HashMap()

    fun getId(state: RSMState) {
        stateToId.getOrPut(state) { lastId++ }
    }

    val states: HashSet<RSMState> = HashSet()
    val queue: ArrayDeque<RSMState> = ArrayDeque(listOf(startState))
    val boxes: HashMap<Nonterminal, HashSet<RSMState>> = HashMap()

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
                out.println("${getId(state)} [label = \"${state.nonterminal.name},${getId(state)}\", shape = circle, color = green]")
            else if (state.isFinal)
                out.println("${getId(state)} [label = \"${state.nonterminal.name},${getId(state)}\", shape = doublecircle, color = red]")
            else
                out.println("${getId(state)} [label = \"${state.nonterminal.name},${getId(state)}\", shape = circle]")
        }

        states.forEach { state ->
            state.outgoingTerminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println("${getId(state)} -> ${getId(head)} [label = \"${edge.key.value}\"]")
                }
            }
            state.outgoingNonterminalEdges.forEach { edge ->
                edge.value.forEach { head ->
                    out.println("${getId(state)} -> ${getId(head)} [label = ${edge.key.name}]")
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
