package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class RSMState(
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val outgoingEdges: HashMap<Symbol, HashSet<RSMState>> = HashMap()
    private val coveredTargetStates: HashSet<RSMState> = HashSet()
    val errorRecoveryLabels: HashSet<Terminal<*>> = HashSet()

    override fun toString() = "RSMState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    fun addEdge(symbol: Symbol, destState: RSMState) {
        if (symbol is Terminal<*>) {
            addRecoveryInfo(RSMTerminalEdge(symbol, destState))
        }
        val destinationStates = outgoingEdges.getOrPut(symbol) { hashSetOf() }
        destinationStates.add(destState)
    }

    private fun addRecoveryInfo(edge: RSMTerminalEdge) {
        if (!coveredTargetStates.contains(edge.head)) {
            errorRecoveryLabels.add(edge.terminal)
            coveredTargetStates.add(edge.head)
        }
    }

    fun getTerminalEdges(): HashMap<Terminal<*>, HashSet<RSMState>> {
        val terminalEdges = HashMap<Terminal<*>, HashSet<RSMState>>()
        for ((symbol, edges) in outgoingEdges) {
            if (symbol is Terminal<*>) {
                terminalEdges[symbol] = edges
            }
        }
        return terminalEdges
    }

    fun getNonTerminalEdges(): HashMap<Nonterminal, HashSet<RSMState>> {
        val nonTerminalEdges = HashMap<Nonterminal, HashSet<RSMState>>()
        for ((symbol, edges) in outgoingEdges) {
            if (symbol is Nonterminal) {
                nonTerminalEdges[symbol] = edges
            }
        }
        return nonTerminalEdges
    }
}