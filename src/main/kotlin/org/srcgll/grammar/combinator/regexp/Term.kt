package org.srcgll.grammar.combinator.regexp

import org.srcgll.rsm.symbol.Terminal

open class Term <TerminalType>
(
    value : TerminalType,
)
    : DerivedSymbol
{
    val terminal : Terminal<TerminalType> = Terminal(value)
}