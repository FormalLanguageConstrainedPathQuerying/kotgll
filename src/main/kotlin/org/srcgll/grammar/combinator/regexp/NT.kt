package org.srcgll.grammar.combinator.regexp

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import java.util.*
import kotlin.reflect.KProperty

open class NT : DerivedSymbol {
    protected open lateinit var nonTerm: Nonterminal
    protected lateinit var rsmDescription: Regexp

    protected fun getNewState(regex: Regexp, isStart: Boolean = false): RSMState {
        return RSMState(nonTerm, isStart, regex.acceptEpsilon())
    }

    open fun buildRsmBox(): RSMState = buildRsmBox(nonTerm.startState)

    protected fun buildRsmBox(startState: RSMState): RSMState {
        val regexpToProcess = Stack<Regexp>()
        val regexpToRsmState = HashMap<Regexp, RSMState>()
        regexpToRsmState[rsmDescription] = startState

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
                    val toState = regexpToRsmState.getOrPut(newState) { getNewState(newState) }

                    when (symbol) {
                        is Term<*> -> {
                            state?.addTerminalEdge(RSMTerminalEdge(symbol.terminal as Terminal<*>, toState))
                        }

                        is NT -> {
                            if (!symbol::nonTerm.isInitialized) {
                                throw IllegalArgumentException("Not initialized NT used in description of \"${nonTerm.name}\"")
                            }
                            state?.addNonterminalEdge(RSMNonterminalEdge(symbol.nonTerm, toState))
                        }
                    }
                }
            }
        }
        return startState
    }

    override fun getNonterminal(): Nonterminal? {
        return nonTerm
    }

    operator fun setValue(grammar: Grammar, property: KProperty<*>, lrh: Regexp) {
        if (!this::nonTerm.isInitialized) {
            nonTerm = Nonterminal(property.name)
            grammar.nonTerms.add(this)
            rsmDescription = lrh
            nonTerm.startState = RSMState(nonTerm, isStart = true, rsmDescription.acceptEpsilon())
        } else {
            throw Exception("NonTerminal ${property.name} is already initialized")
        }

    }

    operator fun getValue(grammar: Grammar, property: KProperty<*>): Regexp = this

}

/**
 * Helper class for building rsm delta when deleting/adding rules to the grammar.
 * Uses existing grammar nonterminal
 */
class StandAloneNt(nonterminal: Nonterminal) : NT() {
    init {
        nonTerm = nonterminal
    }

    /**
     * Set description of Rsm, may be recursive
     */
    fun setDescription(description: Regexp){
        rsmDescription = description
    }

    /**
     * Create new start state for RsmBox
     * Otherwise the origin of the Rsm will be ruined.
     */
    override fun buildRsmBox(): RSMState = buildRsmBox(getNewState(rsmDescription, true))

    /**
     * Build rsm from given description in regexp
     */
    fun buildRsm(description: Regexp): RSMState{
        rsmDescription = description
        return buildRsmBox()
    }
}