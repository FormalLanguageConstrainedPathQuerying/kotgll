package org.srcgll.parser.context

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.RecoveringDescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.ILabel
import org.srcgll.input.IRecoveryInputGraph
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode

class RecoveryContext<VertexType, LabelType : ILabel>(
    override val startState: RsmState,
    override val input: IRecoveryInputGraph<VertexType, LabelType>
) : IContext<VertexType, LabelType> {
    override val recovery: RecoveryMode = RecoveryMode.ON
    override val descriptors: RecoveringDescriptorsStorage<VertexType> = RecoveringDescriptorsStorage()
    override val sppf: Sppf<VertexType> = Sppf()
    override val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null

    override fun nextDescriptorToHandle(): Descriptor<VertexType>? {
        // Continue parsing until all default descriptors processed
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

