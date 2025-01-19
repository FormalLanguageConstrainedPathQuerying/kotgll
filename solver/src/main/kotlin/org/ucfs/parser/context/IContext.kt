package org.ucfs.parser.context

import org.ucfs.descriptors.Descriptor
import org.ucfs.descriptors.DescriptorsStorage
import org.ucfs.gss.GssNode
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.rsm.RsmState
import org.ucfs.sppf.Sppf
import org.ucfs.sppf.node.SppfNode
import org.ucfs.sppf.node.SymbolSppfNode

/**
 * Context interface. Represents configuration of Gll parser instance
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
interface IContext<VertexType, LabelType : ILabel> {
    /**
     * Starting state of accepting Nonterminal in RSM
     */
    val startState: RsmState

    /**
     * Input graph
     */
    val input: IInputGraph<VertexType, LabelType>

    /**
     * Collection of descriptors
     */
    val descriptors: DescriptorsStorage<VertexType>

    /**
     * Derivation tree
     */
    val sppf: Sppf<VertexType>

    /**
     * Map gssNode -> Set of derivation trees that were obtained in gssNode, i.e. when processing descriptors,
     * which contained gssNode as value in their field.
     */
    val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>>

    /**
     * Collection of created gssNodes, with O(1)-time access and search
     */
    val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>>

    /**
     * Part of incrementality mechanism.
     * Map (vertex, vertex) -> weight. Essentially, for every pair (first, second) stores weight of
     * minimal path between first and second, such that corresponding path is accepted
     */
    val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int>

    /**
     * Parsing result. Either root of derivation tree of null
     */
    var parseResult: SppfNode<VertexType>?

    /**
     * Part of incrementality mechanism
     * Adds descriptors to process. If the descriptor is "final", i.e. can be used to make a derivation, then
     * remove from already handled and process again
     * TODO get only the descriptors you need at the right time.
     * TODO remove unnecessary descriptor processing. It's broke GLL invariant
     * @param descriptor - descriptor to add
     */
    fun addDescriptor(descriptor: Descriptor<VertexType>) {
        val sppfNode = descriptor.sppfNode
        val state = descriptor.rsmState
        val leftExtent = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

//        if (parseResult == null
//            && sppfNode is SymbolSppfNode<*>
//            && state.nonterminal == startState.nonterminal
//            && input.isStart(leftExtent!!)
//            && input.isFinal(rightExtent!!)
//        ) {
//            descriptors.removeFromHandled(descriptor)
//        }
        descriptors.addToHandling(descriptor)
    }

    /**
     * Gets next descriptor to handle
     * @return default descriptor if there is available one, null otherwise
     */
    fun nextDescriptorToHandle(): Descriptor<VertexType>? {
        if (!descriptors.defaultDescriptorsStorageIsEmpty()) {
            return descriptors.next()
        }
        return null
    }
}