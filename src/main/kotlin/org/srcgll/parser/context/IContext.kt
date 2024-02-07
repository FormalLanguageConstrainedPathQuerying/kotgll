package org.srcgll.parser.context

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.DescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode

interface IContext<VertexType, LabelType : ILabel> {
    val startState: RsmState
    val input: IInputGraph<VertexType, LabelType>
    val recovery: RecoveryMode
    val descriptors: DescriptorsStorage<VertexType>
    val sppf: Sppf<VertexType>
    val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>>
    val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>>
    val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int>
    var parseResult: SppfNode<VertexType>?

    /**
     * An attempt to support incrementality.
     * If the previous attempt failed -- remove the descriptor from the processed ones and try again.
     * TODO get only the descriptors you need at the right time.
     * TODO remove unnecessary descriptor processing. It's broke GLL invariant
     */
    fun addDescriptor(descriptor: Descriptor<VertexType>) {
        val sppfNode = descriptor.sppfNode
        val state = descriptor.rsmState
        val leftExtent = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

        if (parseResult == null && sppfNode is SymbolSppfNode<*> && state.nonterminal == startState.nonterminal && input.isStart(
                leftExtent!!
            ) && input.isFinal(rightExtent!!)
        ) {
            descriptors.removeFromHandled(descriptor)
        }
        descriptors.addToHandling(descriptor)
    }

    fun nextDescriptorToHandle(): Descriptor<VertexType>?{
        // Continue parsing until all default descriptors processed
        if (!descriptors.defaultDescriptorsStorageIsEmpty()) {
            return descriptors.next()
        }
        return null
    }
}