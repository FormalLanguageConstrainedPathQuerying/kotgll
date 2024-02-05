package org.srcgll.parser.context

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.DescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode

class Context<VertexType, LabelType : ILabel>(
    override val startState: RsmState,
    override val input: IInputGraph<VertexType, LabelType>
) : IContext<VertexType, LabelType> {
    override val recovery: RecoveryMode = RecoveryMode.OFF
    override val descriptors: DescriptorsStorage<VertexType> = DescriptorsStorage()
    override val sppf: Sppf<VertexType> = Sppf()
    override val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null
}
