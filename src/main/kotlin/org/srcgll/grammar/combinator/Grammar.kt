package org.srcgll.grammar.combinator

import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Terminal
import java.util.HashSet


open class Grammar {
    val nonTerms = ArrayList<Nt>()

    private lateinit var startNt: Nt

    private var _rsm: RsmState? = null
    val rsm: RsmState
        get() {
            if (_rsm == null) {
                _rsm = buildRsm()
            }
            return _rsm!!
        }

    fun setStart(expr: Regexp) {
        if (expr is Nt) {
            startNt = expr
        } else throw IllegalArgumentException("Only NT object can be start state for Grammar")
    }


    /**
     * Builds a new Rsm for grammar
     */
    private fun buildRsm(): RsmState {
        nonTerms.forEach { it.buildRsmBox() }
        val startState = startNt.getNonterminal()?.startState
        //if nonterminal not initialized -- it will be checked in buildRsmBox()
        return startState!!
    }

    /**
     * Get all terminals used in RSM from current state (recursive)
     */
    fun getTerminals(): HashSet<Terminal<*>> {
        return incrementalDfs(
            rsm,
            { state: RsmState -> state.outgoingEdges.values.flatten() +
            state.nonterminalEdges.keys.map{it.startState}},
            hashSetOf(),
            { state, set -> set.addAll(state.terminalEdges.keys) }
        )
    }

}
