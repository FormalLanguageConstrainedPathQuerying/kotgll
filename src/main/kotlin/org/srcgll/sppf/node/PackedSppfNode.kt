package org.ucfs.sppf.node

import org.ucfs.rsm.RsmState
import java.util.*

/**
 * Packed sppfNode. Every nonterminal sppfNode has packed nodes as children. Each packed node represents ambiguity in
 * derivation. Furthermore, each packed node has at most two children
 * @param VertexType - type of vertex in input graph
 */
open class PackedSppfNode<VertexType>(
    /**
     * Divides subrange leftExtent - rightExtent into two subranges leftExtent - pivot, pivot - rightExtent
     */
    val pivot: VertexType,
    /**
     * rsmState, corresponding to grammar slot in CFS
     */
    val rsmState: RsmState,
    /**
     * Left child
     */
    var leftSppfNode: SppfNode<VertexType>? = null,
    /**
     * Right child
     */
    var rightSppfNode: SppfNode<VertexType>? = null,
    override var id: Int = SppfNodeId.getFirstFreeSppfNodeId(),
) : ISppfNode {
    /**
     * Set of all nodes that have current one as child
     */
    override val parents: HashSet<ISppfNode> = HashSet()

    /**
     * Part of error recovery mechanism.
     * Represents minimum number of insertions/deletions that are needed for the subrange leftExtent - rightExtent
     * to be recognized
     */
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
