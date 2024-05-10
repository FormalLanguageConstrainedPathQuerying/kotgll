package org.ucfs.grammar.combinator

import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.Regexp
import org.ucfs.incrementalDfs
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal


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

    fun Nt.asStart(): Nt {
        if (this@Grammar::startNt.isInitialized) {
            throw Exception("Nonterminal ${nonterm.name} is already initialized")
        }
        startNt = this
        return this
    }


    /**
     * Builds a Rsm for the grammar
     */
    private fun buildRsm(): RsmState {
        nonTerms.forEach { it.buildRsmBox() }
        //if nonterminal not initialized -- it will be checked in buildRsmBox()
        return startNt.nonterm.startState
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
