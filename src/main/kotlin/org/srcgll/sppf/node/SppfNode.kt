package org.srcgll.sppf.node

import java.util.*

class SppfNodeId private constructor() {
    companion object {
        private var curSPPFNodeId: Int = 0

        fun getFirstFreeSppfNodeId() = curSPPFNodeId++
    }
}

open class SppfNode<VertexType>(
    val leftExtent: VertexType,
    val rightExtent: VertexType,
    override var weight: Int,
    override var id: Int = SppfNodeId.getFirstFreeSppfNodeId(),
) : ISppfNode {
    override val parents: HashSet<ISppfNode> = HashSet()

    override fun toString() = "SppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SppfNode<*>) return false
        if (leftExtent != other.leftExtent) return false
        if (rightExtent != other.rightExtent) return false
        if (weight != other.weight) return false

        return true
    }

    // TODO: Think about redefining hash := (Prime * leftHash + rightHash)
    open val hashCode: Int = Objects.hash(leftExtent, rightExtent)
    override fun hashCode() = hashCode
}
