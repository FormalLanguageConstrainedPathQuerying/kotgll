package org.srcgll.sppf.node

import java.util.*

/**
 * Node that defines parse tree of a subrange from leftExtent to rightExtent
 * Weight is assigned to Int.MAX_VALUE to ensure that after creation weight of Node
 * would be equal to minimum weight of it's children
 */
abstract class ParentSppfNode<VertexType>(
    leftExtent: VertexType,
    rightExtent: VertexType,
) : SppfNode<VertexType>(leftExtent, rightExtent, Int.MAX_VALUE) {
    val children: HashSet<PackedSppfNode<VertexType>> = HashSet()
}
