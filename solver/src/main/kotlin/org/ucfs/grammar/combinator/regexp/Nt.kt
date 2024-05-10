package org.ucfs.grammar.combinator.regexp

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.Nonterminal
import kotlin.reflect.KProperty

open class Nt() : DerivedSymbol {
    private lateinit var name : String
    constructor(lhs: Regexp) : this() {
        rsmDescription = lhs
    }

    lateinit var nonterm: Nonterminal
        private set

    private lateinit var rsmDescription: Regexp

    fun isInitialized(): Boolean {
        return ::rsmDescription.isInitialized
    }

    fun buildRsmBox() {
        nonterm.startState = RsmState(nonterm, isStart = true, rsmDescription.acceptEpsilon())
        nonterm.startState.buildRsmBox(rsmDescription)
    }

    operator fun getValue(grammar: Grammar, property: KProperty<*>): Nt = this

    operator fun divAssign(lhs: Regexp) {
        if (isInitialized()) {
            throw Exception("Nonterminal '${nonterm.name}' is already initialized")
        }
        rsmDescription = lhs
    }

    operator fun provideDelegate(
        grammar: Grammar, property: KProperty<*>
    ): Nt {
        name = property.name
        nonterm = Nonterminal(property.name)
        grammar.nonTerms.add(this)
        return this
    }
}
