package org.srcgll.rsm

import org.srcgll.grammar.combinator.regexp.Empty
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.incrementalDfs
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal
import java.util.*
import kotlin.collections.HashMap

open class RsmState(
    val nonterminal: Nonterminal,
    val isStart: Boolean = false,
    val isFinal: Boolean = false,
) {
    val id:String = getId(nonterminal)

    companion object{
        private val counters = HashMap<Nonterminal, Int>()
        private fun getId(nt: Nonterminal): String{
            val id = counters.getOrPut(nt) { 0 }
            counters[nt] = id + 1
            return "${nt.name}_${(id)}"
        }
    }

    val outgoingEdges get() = terminalEdges.plus(nonterminalEdges)

    /**
     * map from terminal to edges set
     */
    val terminalEdges = HashMap<Terminal<*>, HashSet<RsmState>>()

    /**
     * map from nonterminal to edges set
     */
    val nonterminalEdges = HashMap<Nonterminal, HashSet<RsmState>>()

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
        val destinationStates: HashSet<RsmState>
        when (symbol) {
            is Terminal<*> -> {
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

    private fun addRecoveryInfo(symbol: Terminal<*>, head: RsmState) {
        if (!targetStates.contains(head)) {
            errorRecoveryLabels.add(symbol)
            targetStates.add(head)
        }
    }

    protected open fun getNewState(regex: Regexp): RsmState {
        return RsmState(this.nonterminal, isStart = false, regex.acceptEpsilon())
    }

    /**
     * Build RSM from current state
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
                        is Term<*> -> {

                            state?.addEdge(symbol.terminal as Terminal<*>, destinationState)
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