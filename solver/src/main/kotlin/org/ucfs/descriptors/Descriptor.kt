package org.ucfs.descriptors

import org.ucfs.gss.GssNode
import org.ucfs.input.ILabel
import org.ucfs.parser.context.IContext
import org.ucfs.rsm.RsmState
import org.ucfs.sppf.node.SppfNode

/**
 * Descriptor represents current parsing stage
 * @param VertexType - type of vertex in input graph
 */
open class Descriptor<VertexType>(
    /**
     * State in RSM, corresponds to slot in CF grammar
     */
    val rsmState: RsmState,
    /**
     * Pointer to node in top layer of graph structured stack
     */
    val gssNode: GssNode<VertexType>,
    /**
     * Pointer to already parsed portion of input, represented as derivation tree, which shall be connected afterwards
     * to derivation trees, stored on edges of GSS, it corresponds to return from recursive function
     */
    val sppfNode: SppfNode<VertexType>?,
    /**
     * Pointer to vertex in input graph
     */
    val inputPosition: VertexType,
) {
    val hashCode = 23 * (23 * (23 * 17 + rsmState.hashCode()) + inputPosition.hashCode()) + gssNode.hashCode()

    val weight: Int
        get() = (sppfNode?.weight ?: 0) + gssNode.minWeightOfLeftPart

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


