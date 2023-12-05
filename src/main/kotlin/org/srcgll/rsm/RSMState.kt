package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class RSMState
    (
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val outgoingTerminalEdges: HashMap<Terminal<*>, HashSet<RSMState>> = HashMap()
    val outgoingNonterminalEdges: HashMap<Nonterminal, HashSet<RSMState>> = HashMap()
    val coveredTargetStates: HashSet<RSMState> = HashSet()
    val errorRecoveryLabels: HashSet<Terminal<*>> = HashSet()

    override fun toString() =
        "RSMState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    fun addTerminalEdge(edge: RSMTerminalEdge) {
        if (!coveredTargetStates.contains(edge.head)) {
            errorRecoveryLabels.add(edge.terminal)
            coveredTargetStates.add(edge.head)
        }

        if (outgoingTerminalEdges.containsKey(edge.terminal)) {
            val targetStates = outgoingTerminalEdges.getValue(edge.terminal)

            targetStates.add(edge.head)
        } else {
            outgoingTerminalEdges[edge.terminal] = hashSetOf(edge.head)
        }
    }

    fun addNonterminalEdge(edge: RSMNonterminalEdge) {
        if (outgoingNonterminalEdges.containsKey(edge.nonterminal)) {
            val targetStates = outgoingNonterminalEdges.getValue(edge.nonterminal)

            targetStates.add(edge.head)
        } else {
            outgoingNonterminalEdges[edge.nonterminal] = hashSetOf(edge.head)
        }
    }

    fun addEdge(label: Symbol, head: RSMState){
        when (label){
            is Terminal<*> -> addTerminalEdge(RSMTerminalEdge(label, head))
            is Nonterminal -> addNonterminalEdge(RSMNonterminalEdge(label, head))
        }
    }
}
