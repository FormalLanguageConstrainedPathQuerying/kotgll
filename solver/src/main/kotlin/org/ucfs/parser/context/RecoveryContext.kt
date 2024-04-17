package org.ucfs.parser.context

import org.ucfs.descriptors.Descriptor
import org.ucfs.descriptors.RecoveringDescriptorsStorage
import org.ucfs.gss.GssNode
import org.ucfs.input.ILabel
import org.ucfs.input.IRecoveryInputGraph
import org.ucfs.rsm.RsmState
import org.ucfs.sppf.RecoverySppf
import org.ucfs.sppf.node.SppfNode

/**
 * Recovery context for parsing with enabled error recovery
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
class RecoveryContext<VertexType, LabelType : ILabel>(
    override val startState: RsmState,
    override val input: IRecoveryInputGraph<VertexType, LabelType>
) : IContext<VertexType, LabelType> {
    override val descriptors: RecoveringDescriptorsStorage<VertexType> = RecoveringDescriptorsStorage()
    override val sppf: RecoverySppf<VertexType> = RecoverySppf()
    override val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null

    /**
     * Part of error recovery mechanism.
     * Gets next descriptor to handle. First tries to get default descriptor. If there is non and derivation tree
     * was not obtained, then tries to get recovery descriptor
     * @return descriptor if available one, null otherwise
     */
    override fun nextDescriptorToHandle(): Descriptor<VertexType>? {
        if (!descriptors.defaultDescriptorsStorageIsEmpty()) {
            return descriptors.next()
        }

        // If string was not parsed - process recovery descriptors until first valid parse tree is found
        // Due to the Error Recovery algorithm used it will be parse tree of the string with min editing cost
        if (parseResult == null) {
            //return recovery descriptor
            return descriptors.next()
        }

        return null
    }
}

