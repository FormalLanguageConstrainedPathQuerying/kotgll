package org.srcgll.grammar.combinator.regexp

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import kotlin.reflect.KProperty

open class Nt : DerivedSymbol {
    lateinit var nonterm: Nonterminal
        private set

    private lateinit var rsmDescription: Regexp

    fun isInitialized(): Boolean {
        return ::nonterm.isInitialized
    }

    fun buildRsmBox() {
        nonterm.startState.buildRsmBox(rsmDescription)
    }

    override fun getNonterminal(): Nonterminal? {
        return nonterm
    }

    operator fun setValue(grammar: Grammar, property: KProperty<*>, lrh: Regexp) {
        if (!this::nonterm.isInitialized) {
            nonterm = Nonterminal(property.name)
            grammar.nonTerms.add(this)
            rsmDescription = lrh
            nonterm.startState = RsmState(nonterm, isStart = true, rsmDescription.acceptEpsilon())
        } else {
            throw Exception("Nonterminal ${property.name} is already initialized")
        }

    }

    operator fun getValue(grammar: Grammar, property: KProperty<*>): Regexp = this
}