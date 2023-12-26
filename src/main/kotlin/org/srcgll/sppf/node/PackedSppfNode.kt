package org.srcgll.sppf.node

import org.srcgll.rsm.RsmState
import java.util.*

open class PackedSppfNode<VertexType>(
    val pivot: VertexType,
    val rsmState: RsmState,
    var leftSppfNode: SppfNode<VertexType>? = null,
    var rightSppfNode: SppfNode<VertexType>? = null,
    override var id: Int = SppfNodeId.getFirstFreeSppfNodeId(),
) : ISppfNode {
    override val parents: HashSet<ISppfNode> = HashSet()

    override var weight: Int = (leftSppfNode?.weight ?: 0) + (rightSppfNode?.weight ?: 0)

    override fun toString() =
        "PackedSppfNode(pivot=$pivot, rsmState=$rsmState, leftSppfNode=$leftSppfNode, rightSppfNode=$rightSppfNode)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackedSppfNode<*>) return false
        if (pivot != other.pivot) return false
        if (rsmState != other.rsmState) return false
        if (leftSppfNode != other.leftSppfNode) return false
        if (rightSppfNode != other.rightSppfNode) return false

        return true
    }

    val hashCode: Int = Objects.hash(pivot, rsmState, leftSppfNode, rightSppfNode)
    override fun hashCode() = hashCode
}
