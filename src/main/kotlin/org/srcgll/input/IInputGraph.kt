package org.srcgll.input

import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode

interface IInputGraph<VertexType, LabelType : ILabel> {
    val vertices: MutableMap<VertexType, VertexType>
    val edges: MutableMap<VertexType, MutableList<Edge<VertexType, LabelType>>>
    val startVertices: MutableSet<VertexType>

    fun getInputStartVertices(): MutableSet<VertexType>
    fun getVertex(vertex: VertexType?): VertexType?
    fun addStartVertex(vertex: VertexType)
    fun addVertex(vertex: VertexType)
    fun removeVertex(vertex: VertexType)

    /**
     * Get all outgoing edges
     */
    fun getEdges(from: VertexType): MutableList<Edge<VertexType, LabelType>>
    fun addEdge(from: VertexType, label: LabelType, to: VertexType)
    fun removeEdge(from: VertexType, label: LabelType, to: VertexType)
    fun isStart(vertex: VertexType): Boolean
    fun isFinal(vertex: VertexType): Boolean

    fun handleEdges(
        handleTerminalOrEpsilonEdge: (
            curDescriptor: Descriptor<VertexType>,
            terminal: Terminal<*>?,
            targetState: RsmState,
            sppfNode: SppfNode<VertexType>?,
            targetVertex: VertexType,
            weight: Int
        ) -> Unit,
        handleNonterminalEdge: (
            descriptor: Descriptor<VertexType>,
            nonterminal: Nonterminal,
            targetStates: HashSet<RsmState>,
            curSppfNode: SppfNode<VertexType>?
        ) -> Unit,
        ctx: IContext<VertexType, LabelType>,
        curDescriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?
    ) {
        val pos = curDescriptor.inputPosition
        val state = curDescriptor.rsmState

        for ((edgeNonterminal, targetStates) in curDescriptor.rsmState.getNonterminalEdges()) {
            handleNonterminalEdge(curDescriptor, edgeNonterminal, targetStates, sppfNode)
        }

        val gssNode = curDescriptor.gssNode
        for (inputEdge in getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                handleTerminalOrEpsilonEdge(
                    curDescriptor, null, state, sppfNode, inputEdge.head, 0
                )
                continue
            }
            for ((edgeTerminal, targetStates) in state.getTerminalEdges()) {
                if (inputEdge.label.terminal == edgeTerminal) {
                    for (target in targetStates) {
                        handleTerminalOrEpsilonEdge(
                            curDescriptor, edgeTerminal, target, sppfNode, inputEdge.head, 0
                        )
                    }
                }
            }
        }
    }
}