package org.srcgll.parser.context

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.RecoveringDescriptorsStorage
import org.srcgll.gss.GssNode
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode

class RecoveryContext<VertexType, LabelType : ILabel>(
    override val startState: RsmState,
    override val input: IInputGraph<VertexType, LabelType>
) : IContext<VertexType, LabelType> {
    override val recovery: RecoveryMode = RecoveryMode.ON
    override val descriptors: RecoveringDescriptorsStorage<VertexType> = RecoveringDescriptorsStorage()
    override val sppf: Sppf<VertexType> = Sppf()
    override val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null
}


//
//class RecoveryContext<VertexType, LabelType : ILabel>(
//    startState: RsmState, input: IGraph<VertexType, LabelType>, recovery: RecoveryMode = RecoveryMode.OFF
//) : Context<VertexType, LabelType>(startState, input, recovery) {
//    override val descriptors = ErrorRecoveringDescriptorsStorage<VertexType>()
//}