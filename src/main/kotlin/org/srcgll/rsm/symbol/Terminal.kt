package org.srcgll.rsm.symbol

class Terminal<TerminalType>(
    val value: TerminalType,
) : Symbol {
    override fun toString() = "Literal($value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Terminal<*>) return false
        if (value != other.value) return false

        return true
    }

    val hashCode: Int = value.hashCode()
    override fun hashCode() = hashCode
}
