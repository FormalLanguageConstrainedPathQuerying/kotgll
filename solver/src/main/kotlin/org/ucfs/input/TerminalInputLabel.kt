package org.ucfs.input

import org.ucfs.rsm.symbol.ITerminal

class TerminalInputLabel(
    override val terminal: ITerminal,
) : ILabel {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalInputLabel) return false
        if (this.terminal != other.terminal) return false

        return true
    }

    val hashCode: Int = terminal.hashCode()
    override fun hashCode() = hashCode

    override fun toString(): String {
        return terminal.toString()
    }
}
