package org.srcgll.sppf.node

import java.util.*

/**
 * Abstract nonterminal sppfNode, generalization of both Symbol and Intermediate sppfNodes
 */
abstract class NonterminalSppfNode<VertexType>(
    /**
     * Left limit of the subrange
     */
    leftExtent: VertexType,
    /**
     * Right limit of the subrange
     */
    rightExtent: VertexType,
) : SppfNode<VertexType>(leftExtent, rightExtent, Int.MAX_VALUE) {
    /**
     * Set of all children nodes
     */
    val children: HashSet<PackedSppfNode<VertexType>> = HashSet()
}
