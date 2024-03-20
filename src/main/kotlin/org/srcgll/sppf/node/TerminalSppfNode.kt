package org.srcgll.sppf.node

import org.srcgll.rsm.symbol.ITerminal
import java.util.*

class TerminalSppfNode<VertexType>(
    val terminal: ITerminal?,
    leftExtent: VertexType,
    rightExtent: VertexType,
    weight: Int = 0,
) : SppfNode<VertexType>(leftExtent, rightExtent, weight) {
    override fun toString() = "TerminalSppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent, terminal=$terminal)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalSppfNode<*>) return false
        if (!super.equals(other)) return false
        if (terminal != other.terminal) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent, terminal)
    override fun hashCode() = hashCode
}
