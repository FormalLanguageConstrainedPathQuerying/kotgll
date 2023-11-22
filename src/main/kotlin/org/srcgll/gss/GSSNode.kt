package org.srcgll.gss

import org.srcgll.descriptors.Descriptor
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SPPFNode
import java.util.*

class GSSNode<VertexType>
(
    val nonterminal         : Nonterminal,
    val inputPosition       : VertexType,
    var minWeightOfLeftPart : Int,
)
{
    val edges : HashMap<Pair<RSMState, SPPFNode<VertexType>?>, HashSet<GSSNode<VertexType>>> = HashMap()
    val handledDescriptors : HashSet<Descriptor<VertexType>> = HashSet()
    
    fun addEdge(rsmState : RSMState, sppfNode : SPPFNode<VertexType>?, gssNode : GSSNode<VertexType>) : Boolean
    {
        val label = Pair(rsmState, sppfNode)
        
        if (!edges.containsKey(label)) edges[label] = HashSet()
        
        return edges.getValue(label).add(gssNode)
    }
    
    override fun toString() = "GSSNode(nonterminal=$nonterminal, inputPosition=$inputPosition)"
    
    override fun equals(other : Any?) : Boolean
    {
        if (this === other)                       return true
        if (other !is GSSNode<*>)                 return false
        if (nonterminal != other.nonterminal)     return false
        if (inputPosition != other.inputPosition) return false

        return true
    }
    
    val hashCode = Objects.hash(nonterminal, inputPosition)
    override fun hashCode() = hashCode
}
