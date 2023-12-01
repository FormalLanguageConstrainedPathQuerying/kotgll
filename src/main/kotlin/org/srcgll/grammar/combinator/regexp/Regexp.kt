package org.srcgll.grammar.combinator.regexp

import org.srcgll.rsm.symbol.Nonterminal


sealed interface Regexp {
    /*
    Based on Brzozowski derivative
     */
    fun derive(symbol: DerivedSymbol): Regexp
    fun getNonterminal(): Nonterminal? = null

    /*
     Does the expression accept an epsilon
     */
    fun acceptEpsilon(): Boolean {
        return when (this) {
            is Empty -> false
            is Epsilon -> true
            is DerivedSymbol -> false
            is Concat -> head.acceptEpsilon() && tail.acceptEpsilon()
            is Alternative -> left.acceptEpsilon() || right.acceptEpsilon()
            is Many -> true
        }
    }

    fun getAlphabet(): Set<DerivedSymbol> {
        return when (this) {
            is Empty -> emptySet()
            is Epsilon -> emptySet()
            is DerivedSymbol -> setOf(this)
            is Concat -> head.getAlphabet() + tail.getAlphabet()
            is Alternative -> left.getAlphabet() + right.getAlphabet()
            is Many -> exp.getAlphabet()
        }
    }
}

data object Epsilon : Regexp {
    override fun derive(symbol: DerivedSymbol): Regexp = Empty
}

/*
    Regular expression that does not accept any input string.
 */
data object Empty : Regexp {
    override fun derive(symbol: DerivedSymbol): Regexp = this
}

