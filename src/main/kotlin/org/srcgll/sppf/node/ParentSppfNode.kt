package org.srcgll.sppf.node

import java.util.*

open class ParentSppfNode<VertexType>(
    leftExtent: VertexType,
    rightExtent: VertexType,
) : SppfNode<VertexType>(leftExtent, rightExtent, Int.MAX_VALUE) {
    val kids: HashSet<PackedSppfNode<VertexType>> = HashSet()

    override fun toString() = "ParentSppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParentSppfNode<*>) return false
        if (!super.equals(other)) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent)
    override fun hashCode() = hashCode
}
