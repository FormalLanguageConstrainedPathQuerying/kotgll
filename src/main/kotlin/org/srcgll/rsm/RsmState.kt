package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class RsmState(
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val outgoingEdges: HashMap<Symbol, HashSet<RsmState>> = HashMap()
    private val coveredTargetStates: HashSet<RsmState> = HashSet()
    val errorRecoveryLabels: HashSet<Terminal<*>> = HashSet()

    override fun toString() = "RsmState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    fun addEdge(symbol: Symbol, head: RsmState) {
        if (symbol is Terminal<*>) {
            addRecoveryInfo(symbol, head)
        }
        val destinationStates = outgoingEdges.getOrPut(symbol) { hashSetOf() }
        destinationStates.add(head)
    }

    private fun addRecoveryInfo(symbol: Terminal<*>, head: RsmState) {
        if (!coveredTargetStates.contains(head)) {
            errorRecoveryLabels.add(symbol)
            coveredTargetStates.add(head)
        }
    }

    fun getTerminalEdges(): HashMap<Terminal<*>, HashSet<RsmState>> {
        val terminalEdges = HashMap<Terminal<*>, HashSet<RsmState>>()
        for ((symbol, edges) in outgoingEdges) {
            if (symbol is Terminal<*>) {
                terminalEdges[symbol] = edges
            }
        }
        return terminalEdges
    }

    fun getNonterminalEdges(): HashMap<Nonterminal, HashSet<RsmState>> {
        val nonTerminalEdges = HashMap<Nonterminal, HashSet<RsmState>>()
        for ((symbol, edges) in outgoingEdges) {
            if (symbol is Nonterminal) {
                nonTerminalEdges[symbol] = edges
            }
        }
        return nonTerminalEdges
    }
}