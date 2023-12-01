package org.srcgll.input

import org.srcgll.rsm.symbol.Terminal

class LinearInputLabel(
    override val terminal: Terminal<*>,
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
