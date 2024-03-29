package org.srcgll.grammar.combinator

import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.incrementalDfs
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.ITerminal


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
     * Builds a Rsm for the grammar
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
    fun getTerminals(): Iterable<ITerminal> {
        val terms : HashSet<ITerminal> = incrementalDfs(
            rsm,
            { state: RsmState ->
                state.outgoingEdges.values.flatten() +
                        state.nonterminalEdges.keys.map { it.startState }
            },
            hashSetOf(),
            { state, set -> set.addAll(state.terminalEdges.keys) }
        )
        val comparator = terms.firstOrNull()?.getComparator() ?: return emptyList()
        return terms.toSortedSet(comparator).toList()
    }

}
