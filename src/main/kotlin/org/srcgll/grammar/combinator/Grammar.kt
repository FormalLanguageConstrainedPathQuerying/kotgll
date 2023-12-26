package org.srcgll.grammar.combinator

import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.rsm.RsmState

open class Grammar {
    val nonTerms = ArrayList<Nt>()

    private var startState: RsmState? = null
    private lateinit var startNt: Nt

    fun setStart(expr: Regexp) {
        if (expr is Nt) {
            startNt = expr
        } else throw IllegalArgumentException("Only NT object can be start state for Grammar")
    }

    /**
     * Builds or returns a Rsm built earlier for the grammar
     */
    fun getRsm(): RsmState {
        if (startState == null) {
            buildRsm()
        }
        return startState as RsmState
    }

    /**
     * Builds a new Rsm for the grammar
     */

    private fun buildRsm(): RsmState {
        nonTerms.forEach { it.buildRsmBox() }
        startState = startNt.getNonterminal()?.startState
        return startState as RsmState
    }
}
