package org.ucfs.sppf.node

import java.util.*

class SppfNodeId private constructor() {
    companion object {
        private var curSPPFNodeId: Int = 0

        fun getFirstFreeSppfNodeId() = curSPPFNodeId++
    }
}

/**
 * Abstract class of sppfNode, generalizes all sppfNodes, except packed ones
 * @param VertexType - type of vertex in input graph
 */
abstract class SppfNode<VertexType>(
    /**
     * Left limit of subrange
     */
    val leftExtent: VertexType,
    /**
     * Right limit of subrange
     */
    val rightExtent: VertexType,
    /**
     * Part of error recovery mechanism.
     * Represents minimum number of insertions/deletions that are needed for the subrange leftExtent - rightExtent
     * to be recognized
     */
    override var weight: Int,
    override var id: Int = SppfNodeId.getFirstFreeSppfNodeId(),
) : ISppfNode {
    override fun toString() = "SppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SppfNode<*>) return false
        if (leftExtent != other.leftExtent) return false
        if (rightExtent != other.rightExtent) return false
        if (weight != other.weight) return false

        return true
    }

    open val hashCode: Int = Objects.hash(leftExtent, rightExtent)
    override fun hashCode() = hashCode
}
