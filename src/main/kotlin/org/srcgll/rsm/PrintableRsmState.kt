package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class PrintableRsmState(
    nonterminal: Nonterminal,
    isStart: Boolean = false,
    isFinal: Boolean = false,
) : RsmState(nonterminal, isStart, isFinal) {
    //All path in rsm from start state to current
    val pathLabels: HashSet<String> = HashSet()

    init {
        if (isStart) {
            pathLabels.add("")
        }
    }

    override fun addEdge(symbol: Symbol, destinationState: RsmState) {
        if (symbol is Terminal<*>) {
            addRecoveryInfo(symbol, destinationState)
        }
        val destinationStates = outgoingEdges.getOrPut(symbol) { hashSetOf() }
        destinationStates.add(destinationState)

        val view = getGeneratorView(symbol)
        for (path in pathLabels) {
            if (!destinationState.isStart) {
                if (destinationState is PrintableRsmState) {
                    destinationState.pathLabels.add(path + view)
                }
                else{
                    throw Exception("Only PrintableRsmState can be used in generated Parser")
                }
            }
        }
    }
}

fun <T> getGeneratorView(t: T): String {
    return if (t is Terminal<*>) {
        getGeneratorView(t.value)
    } else {
        t.hashCode().toString()
    }
}

fun getGeneratorView(t: Terminal<out String>) = t.value