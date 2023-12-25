package org.srcgll.sppf.node

import java.util.*

open class ParentSPPFNode<VertexType>(
    leftExtent: VertexType,
    rightExtent: VertexType,
) : SPPFNode<VertexType>(leftExtent, rightExtent, Int.MAX_VALUE) {
    val kids: HashSet<PackedSPPFNode<VertexType>> = HashSet()

    override fun toString() = "ParentSPPFNode(leftExtent=$leftExtent, rightExtent=$rightExtent)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParentSPPFNode<*>) return false
        if (!super.equals(other)) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent)
    override fun hashCode() = hashCode
}
