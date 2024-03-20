package org.srcgll.rsm.symbol

import org.srcgll.grammar.combinator.regexp.DerivedSymbol

class Term<TerminalType>(val value: TerminalType) : ITerminal, DerivedSymbol {
    override fun toString() = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Term<*>) return false
        return value == other.value
    }

    val hashCode: Int = value.hashCode()
    override fun hashCode() = hashCode
}