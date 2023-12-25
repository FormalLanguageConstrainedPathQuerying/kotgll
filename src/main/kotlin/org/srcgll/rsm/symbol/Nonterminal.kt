package org.srcgll.rsm.symbol

import org.srcgll.rsm.RSMState

class Nonterminal(val name: String?) : Symbol {
    lateinit var startState: RSMState
    override fun toString() = "Nonterminal(${name ?: this.hashCode()})"
}
