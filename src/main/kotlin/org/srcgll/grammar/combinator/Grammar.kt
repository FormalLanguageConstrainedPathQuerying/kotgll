package org.srcgll.grammar.combinator

import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.rsm.RsmState


open class Grammar {
    val nonTerms = ArrayList<Nt>()

    private lateinit var startNt: Nt

    fun setStart(expr: Regexp) {
        if (expr is Nt) {
            startNt = expr
        } else throw IllegalArgumentException("Only NT object can be start state for Grammar")
    }


    /**
     * Builds a new Rsm for grammar
     */
    fun buildRsm(): RsmState {
        nonTerms.forEach { it.buildRsmBox() }
        val startState = startNt.getNonterminal()?.startState
        //if nonterminal not initialized -- it will be checked in buildRsmBox()
        return startState!!
    }
}
