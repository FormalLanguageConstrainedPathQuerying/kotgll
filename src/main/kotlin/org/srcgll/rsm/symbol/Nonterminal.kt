package org.srcgll.rsm.symbol

import org.srcgll.rsm.RSMState

class Nonterminal
    (
    val value: String
) : Symbol {
    lateinit var startState: RSMState
    override fun toString() = "Nonterminal($value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Nonterminal) return false
        return value == other.value
    }

    val hashCode: Int = value.hashCode()
    override fun hashCode() = hashCode
}
