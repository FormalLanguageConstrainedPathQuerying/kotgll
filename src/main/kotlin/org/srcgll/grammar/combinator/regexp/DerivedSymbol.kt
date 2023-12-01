package org.srcgll.grammar.combinator.regexp

interface DerivedSymbol : Regexp {
    override fun derive(symbol: DerivedSymbol): Regexp {
        return if (this == symbol) Epsilon else Empty
    }
}