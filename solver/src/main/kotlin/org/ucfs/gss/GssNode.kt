package org.ucfs.gss

import org.ucfs.descriptors.Descriptor
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode
import java.util.*

/**
 * Node in Graph Structured Stack
 * @param VertexType - type of vertex in input graph
 */
class GssNode<VertexType>(
    /**
     * Nonterminal A, corresponding to grammar slot of the form aA·b
     */
    val nonterminal: Nonterminal,
    /**
     * Pointer to vertex in input graph
     */
    val inputPosition: VertexType,
    /**
     * Part of error recovery mechanism.
     * Stores minimally possible weight of already parsed portion of input
     */
    var minWeightOfLeftPart: Int,
) {
    /**
     * Maps edge label (rsmState, sppfNode) to destination gssNode
     */
    val edges: HashMap<Pair<RsmState, SppfNode<VertexType>?>, HashSet<GssNode<VertexType>>> = HashMap()

    /**
     * Stores handled descriptors, which contained current gssNode as value of corresponding field
     */
    val handledDescriptors: HashSet<Descriptor<VertexType>> = HashSet()

    /**
     * Add new edge from current gssNode
     * @param rsmState - rsmState to store on the edge
     * @param sppfNode - sppfNode to store on the edge
     * @param gssNode - destination gssNode
     * @return true if creation was successful, false otherwise
     */
    fun addEdge(rsmState: RsmState, sppfNode: SppfNode<VertexType>?, gssNode: GssNode<VertexType>): Boolean {
        val label = Pair(rsmState, sppfNode)

        return edges.computeIfAbsent(label) { HashSet() }.add(gssNode)
    }

    override fun toString() = "GSSNode(nonterminal=$nonterminal, inputPosition=$inputPosition)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GssNode<*>) return false
        if (nonterminal != other.nonterminal) return false
        if (inputPosition != other.inputPosition) return false

        return true
    }

    val hashCode = Objects.hash(nonterminal, inputPosition)
    override fun hashCode() = hashCode
}
