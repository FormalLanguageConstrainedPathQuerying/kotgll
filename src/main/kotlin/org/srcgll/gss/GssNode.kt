package org.srcgll.gss

import org.srcgll.descriptors.Descriptor
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode
import java.util.*

class GssNode<VertexType>(
    val nonterminal: Nonterminal,
    val inputPosition: VertexType,
    var minWeightOfLeftPart: Int,
) {
    val edges: HashMap<Pair<RsmState, SppfNode<VertexType>?>, HashSet<GssNode<VertexType>>> = HashMap()
    val handledDescriptors: HashSet<Descriptor<VertexType>> = HashSet()

    fun addEdge(rsmState: RsmState, sppfNode: SppfNode<VertexType>?, gssNode: GssNode<VertexType>): Boolean {
        val label = Pair(rsmState, sppfNode)

        if (!edges.containsKey(label)) edges[label] = HashSet()

        return edges.getValue(label).add(gssNode)
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
