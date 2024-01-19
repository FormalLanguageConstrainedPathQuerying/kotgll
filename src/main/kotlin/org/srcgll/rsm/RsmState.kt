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
    //All path in rsm from start state to current
    val pathLabels: HashSet<String> = HashSet()

    init {
        if (isStart) {
            pathLabels.add("")
        }
    }

    override fun toString() = "RsmState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    fun addEdge(symbol: Symbol, destinationState: RsmState) {
        if (symbol is Terminal<*>) {
            addRecoveryInfo(symbol, destinationState)
        }
        val destinationStates = outgoingEdges.getOrPut(symbol) { hashSetOf() }
        destinationStates.add(destinationState)

        val view = getGeneratorView(symbol)
        for (path in pathLabels) {
            if (!destinationState.isStart) {
                destinationState.pathLabels.add(path + view)
            }
        }
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


fun <T> getGeneratorView(t: T): String {
    return if (t is Terminal<*>) {
        getGeneratorView(t.value)
    } else {
        t.hashCode().toString()
    }
}

fun getGeneratorView(t: Terminal<out String>) = t.value