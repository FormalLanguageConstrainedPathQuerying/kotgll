package org.ucfs.rsm

import org.ucfs.grammar.combinator.regexp.Empty
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.Regexp
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.rsm.symbol.Symbol
import java.util.*

open class RsmState(
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val numId: Int = getId(nonterminal)
    val id: String = "${nonterminal.name}_${(numId)}"

    companion object {
        private val counters = HashMap<Nonterminal, Int>()
        private fun getId(nt: Nonterminal): Int {
            val id = counters.getOrPut(nt) { 0 }
            counters[nt] = id + 1
            return id
        }
    }

    val outgoingEdges
        get() = terminalEdges.plus(nonterminalEdges)

    /**
     * Map from terminal to edges set
     */
    val terminalEdges = HashMap<ITerminal, HashSet<RsmState>>()

    /**
     * Map from nonterminal to edges set
     */
    val nonterminalEdges = HashMap<Nonterminal, HashSet<RsmState>>()

    /**
     * Keep a list of all available RsmStates
     */
    val targetStates: HashSet<RsmState> = HashSet()

    /**
     * Part of error recovery mechanism.
     * A set of terminals that can be used to move from a given state to other states.
     * Moreover, if there are several different edges that can be used to move to one state,
     * then only 1 is chosen non-deterministically.
     * TODO Maybe you can get rid of it or find a better optimization (?)
     */
    val errorRecoveryLabels: HashSet<ITerminal> = HashSet()

    override fun toString() = "RsmState(nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    /**
     * Adds edge from current rsmState to given destinationState via given symbol, terminal or nonterminal
     * @param symbol - symbol to store on edge
     * @param destinationState - head of edge
     */
    open fun addEdge(symbol: Symbol, destinationState: RsmState) {
        val destinationStates: HashSet<RsmState>
        when (symbol) {
            is ITerminal -> {
                destinationStates = terminalEdges.getOrPut(symbol) { hashSetOf() }
                addRecoveryInfo(symbol, destinationState)
            }

            is Nonterminal -> {
                destinationStates = nonterminalEdges.getOrPut(symbol) { hashSetOf() }
            }

            else -> throw RsmException("Unsupported type of symbol")
        }
        destinationStates.add(destinationState)
    }

    /**
     * Part of error recovery mechanism.
     * Adds given rsmState to set of available rsmStates from current one, via given terminal
     * @param terminal - terminal on edge
     * @param head - destination state
     */
    private fun addRecoveryInfo(terminal: ITerminal, head: RsmState) {
        if (!targetStates.contains(head)) {
            errorRecoveryLabels.add(terminal)
            targetStates.add(head)
        }
    }

    protected open fun getNewState(regex: Regexp): RsmState {
        return RsmState(this.nonterminal, isStart = false, regex.acceptEpsilon())
    }

    /**
     * Builds RSM from current state
     * @param rsmDescription - right hand side of the rule in GrammarDsl in the form of regular expression
     */
    fun buildRsmBox(rsmDescription: Regexp) {
        val regexpToProcess = Stack<Regexp>()
        val regexpToRsmState = HashMap<Regexp, RsmState>()
        regexpToRsmState[rsmDescription] = this

        val alphabet = rsmDescription.getAlphabet()

        regexpToProcess.add(rsmDescription)

        while (!regexpToProcess.empty()) {
            val regexp = regexpToProcess.pop()
            val state = regexpToRsmState[regexp]

            for (symbol in alphabet) {
                val newState = regexp.derive(symbol)
                if (newState !is Empty) {
                    if (!regexpToRsmState.containsKey(newState)) {
                        regexpToProcess.add(newState)
                    }
                    val destinationState = regexpToRsmState.getOrPut(newState) { getNewState(newState) }

                    when (symbol) {
                        is ITerminal -> {
                            state?.addEdge(symbol, destinationState)
                        }

                        is Nt -> {
                            if (!symbol.isInitialized()) {
                                throw IllegalArgumentException("Not initialized Nt used in description of \"${symbol.nonterm.name}\"")
                            }
                            state?.addEdge(symbol.nonterm, destinationState)
                        }
                    }
                }
            }
        }
    }
}