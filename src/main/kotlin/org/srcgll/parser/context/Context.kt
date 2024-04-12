package org.ucfs.parser.context

import org.ucfs.descriptors.DescriptorsStorage
import org.ucfs.gss.GssNode
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.rsm.RsmState
import org.ucfs.sppf.Sppf
import org.ucfs.sppf.node.SppfNode

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
