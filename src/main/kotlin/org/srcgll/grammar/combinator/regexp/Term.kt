package org.srcgll.grammar.combinator.regexp

import org.srcgll.rsm.symbol.Terminal

open class Term<TerminalType>(
    value: TerminalType,
) : DerivedSymbol {
    val terminal: Terminal<TerminalType> = Terminal(value)

    override fun equals(other: Any?): Boolean {
        if (other !is Term<*>) return false
        return terminal == other.terminal
    }

    override fun hashCode(): Int = terminal.hashCode()
}