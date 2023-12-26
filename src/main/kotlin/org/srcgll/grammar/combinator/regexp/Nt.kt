package org.srcgll.grammar.combinator.regexp

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import java.util.*
import kotlin.reflect.KProperty

open class Nt : DerivedSymbol {
    private lateinit var nonterm: Nonterminal
    private lateinit var rsmDescription: Regexp

    private fun getNewState(regex: Regexp): RsmState {
        return RsmState(nonterm, isStart = false, regex.acceptEpsilon())
    }

    fun buildRsmBox(): RsmState {
        val regexpToProcess = Stack<Regexp>()
        val regexpToRsmState = HashMap<Regexp, RsmState>()
        regexpToRsmState[rsmDescription] = nonterm.startState

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
                            state?.addEdge(symbol.terminal as Terminal<*>, toState)
                        }

                        is Nt -> {
                            if (!symbol::nonterm.isInitialized) {
                                throw IllegalArgumentException("Not initialized Nt used in description of \"${nonterm.name}\"")
                            }
                            state?.addEdge(symbol.nonterm, toState)
                        }
                    }
                }
            }
        }
        return nonterm.startState
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