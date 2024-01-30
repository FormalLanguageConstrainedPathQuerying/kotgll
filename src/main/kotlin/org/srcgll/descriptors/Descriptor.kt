package org.srcgll.descriptors

import org.srcgll.gss.GssNode
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.node.SppfNode

class Descriptor<VertexType>(
    val rsmState: RsmState,
    val gssNode: GssNode<VertexType>,
    val sppfNode: SppfNode<VertexType>?,
    val inputPosition: VertexType,
) {
    val hashCode = 23 * (23 * (23 * 17 + rsmState.hashCode()) + inputPosition.hashCode()) + gssNode.hashCode()

    fun weight(): Int = (sppfNode?.weight ?: 0) + gssNode.minWeightOfLeftPart

    override fun hashCode() = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Descriptor<*>) return false
        if (other.rsmState != rsmState) return false
        if (other.gssNode != gssNode) return false
        if (other.sppfNode != sppfNode) return false
        if (other.inputPosition != inputPosition) return false

        return true
    }
}


