package org.ucfs.sppf.node

import org.ucfs.rsm.RsmState
import java.util.*

/**
 * An Intermediate node which corresponds to the intermediate
 * point used in the path index. This node has two children,
 * both are range nodes.
 * <p>
 *     Ensures that the resulting derivation tree has at most cubic complexity
 * @param VertexType - type of vertex in input graph
 */
class IntermediateSppfNode<VertexType>(
    /**
     * rsmState, corresponding to grammar slot in CFG
     */
    val rsmState: RsmState,
    /**
     * Left limit of the subrange
     */
    leftExtent: VertexType,
    /**
     * Right limit of the subrange
     */
    rightExtent: VertexType,
) : NonterminalSppfNode<VertexType>(leftExtent, rightExtent) {
    override fun toString() = "IntermediateSppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent, rsmState=$rsmState)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntermediateSppfNode<*>) return false
        if (!super.equals(other)) return false
        if (rsmState != other.rsmState) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent, rsmState)
    override fun hashCode() = hashCode
}
