package org.srcgll.rsm

import org.srcgll.grammar.combinator.regexp.Regexp
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class PrintableRsmState(
    nonterminal: Nonterminal,
    isStart: Boolean = false,
    isFinal: Boolean = false,
) : RsmState(nonterminal, isStart, isFinal) {
    constructor(state: RsmState) : this(state.nonterminal, state.isStart, state.isFinal) {}

    //All path in rsm from start state to current
    val pathLabels: HashSet<String> = HashSet()

    init {
        if (isStart) {
            pathLabels.add("")
        }
    }

    override fun getNewState(regex: Regexp): RsmState {
        return PrintableRsmState(this.nonterminal, isStart = false, regex.acceptEpsilon())
    }

    override fun addEdge(symbol: Symbol, destinationState: RsmState) {
        val view = getGeneratorView(symbol)
        for (path in pathLabels) {
            if (!destinationState.isStart) {
                if (destinationState is PrintableRsmState) {
                    destinationState.pathLabels.add(path + view)
                } else {
                    throw Exception("Only PrintableRsmState can be used in generated Parser")
                }
            }
        }
        super.addEdge(symbol, destinationState)
    }
}

fun <T> getGeneratorView(t: T): String {
    return when (t) {
        is Terminal<*> -> getGeneratorView(t.value)
        is Nonterminal -> t.name!!
        else -> t.toString()
    }
}

fun getGeneratorView(t: Terminal<out String>) = t.value