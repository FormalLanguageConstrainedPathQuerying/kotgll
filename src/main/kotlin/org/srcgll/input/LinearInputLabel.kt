package org.srcgll.input

import org.srcgll.rsm.symbol.Term

class LinearInputLabel(
    override val terminal: Term<*>,
) : ILabel {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearInputLabel) return false
        if (this.terminal != other.terminal) return false

        return true
    }

    val hashCode: Int = terminal.hashCode()
    override fun hashCode() = hashCode
}
