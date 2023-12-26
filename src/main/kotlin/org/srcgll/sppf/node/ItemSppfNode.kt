package org.srcgll.sppf.node

import org.srcgll.rsm.RsmState
import java.util.*

class ItemSppfNode<VertexType>(
    val rsmState: RsmState,
    leftExtent: VertexType,
    rightExtent: VertexType,
) : ParentSppfNode<VertexType>(leftExtent, rightExtent) {
    override fun toString() = "ItemSppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent, rsmState=$rsmState)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemSppfNode<*>) return false
        if (!super.equals(other)) return false
        if (rsmState != other.rsmState) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent, rsmState)
    override fun hashCode() = hashCode
}
