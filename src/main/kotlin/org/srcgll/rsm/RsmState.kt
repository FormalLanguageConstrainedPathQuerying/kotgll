package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

open class RsmState(
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val outgoingEdges: HashMap<Symbol, HashSet<RsmState>> = HashMap()
    /**
     * Keep a list of all available RsmStates
     */
    private val targetStates: HashSet<RsmState> = HashSet()
    /**
     * A set of terminals that can be used to move from a given state to other states.
     * Moreover, if there are several different edges that can be used to move to one state,
     * then only 1 is chosen non-deterministically.
     * Uses for error-recovery
     * TODO Maybe you can get rid of it or find a better optimization (?)
     */
    val errorRecoveryLabels: HashSet<Terminal<*>> = HashSet()

    override fun toString() = "RsmState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    open fun addEdge(symbol: Symbol, destinationState: RsmState) {
        if (symbol is Terminal<*>) {
            addRecoveryInfo(symbol, destinationState)
        }
        val destinationStates = outgoingEdges.getOrPut(symbol) { hashSetOf() }
        destinationStates.add(destinationState)
    }

    protected fun addRecoveryInfo(symbol: Terminal<*>, head: RsmState) {
        if (!targetStates.contains(head)) {
            errorRecoveryLabels.add(symbol)
            targetStates.add(head)
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