package org.srcgll.rsm.symbol

import org.srcgll.generators.GeneratorException
import org.srcgll.grammar.combinator.regexp.DerivedSymbol

class Term<TerminalType>(val value: TerminalType) : ITerminal, DerivedSymbol {
    override fun toString() = value.toString()
    override fun getComparator(): Comparator<ITerminal> {
        //TODO improve comparable interfaces
        return object : Comparator<ITerminal> {
            override fun compare(a: ITerminal, b: ITerminal): Int {
                if (a !is Term<*> || b !is Term<*>) {
                    throw GeneratorException(
                        "used comparator for $javaClass, " +
                                "but got elements of ${a.javaClass}$ and ${b.javaClass}\$"
                    )
                }
                return a.value.toString().compareTo(b.value.toString())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Term<*>) return false
        return value == other.value
    }

    val hashCode: Int = value.hashCode()
    override fun hashCode() = hashCode
}