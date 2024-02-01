package org.srcgll.parser.context

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.DescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode

interface IContext<VertexType, LabelType : ILabel> {
    val startState: RsmState
    val input: IGraph<VertexType, LabelType>
    val recovery: RecoveryMode
    val descriptors: DescriptorsStorage<VertexType>
    val sppf: Sppf<VertexType>
    val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>>
    val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>>
    val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int>
    var parseResult: SppfNode<VertexType>?
}