package org.srcgll.parser.context

import org.srcgll.descriptors.DescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode

/**
 * Default context for parsing without error recovery
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
class Context<VertexType, LabelType : ILabel>(
    override val startState: RsmState,
    override val input: IInputGraph<VertexType, LabelType>
) : IContext<VertexType, LabelType> {
    override val descriptors: DescriptorsStorage<VertexType> = DescriptorsStorage()
    override val sppf: Sppf<VertexType> = Sppf()
    override val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null
}
