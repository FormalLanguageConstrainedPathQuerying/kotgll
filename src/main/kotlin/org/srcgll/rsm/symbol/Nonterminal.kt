package org.srcgll.rsm.symbol

import org.srcgll.rsm.RsmState

class Nonterminal(val name: String?) : Symbol {
    lateinit var startState: RsmState
    override fun toString() = "Nonterminal(${name ?: this.hashCode()})"
}
