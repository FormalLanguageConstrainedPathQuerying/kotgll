package org.ucfs.sppf.node

import org.ucfs.rsm.symbol.ITerminal
import java.util.*

/**
 * Terminal sppfNode. Only terminal sppfNodes can be leaves of the derivation tree
 */
class TerminalSppfNode<VertexType>(
    /**
     * Terminal, recognized by parser
     */
    val terminal: ITerminal?,
    /**
     * Left limit of the subrange
     */
    leftExtent: VertexType,
    /**
     * Right limit of the subrange
     */
    rightExtent: VertexType,
    /**
     * Part of error recovery mechanism.
     * Represents minimum number of insertions/deletions that are needed for the subrange leftExtent - rightExtent
     * to be recognized
     */
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
