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
            curSppfNode: SppfNode<VertexType>?,
            terminal: Terminal<*>?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        handleNonterminalEdge: (
            descriptor: Descriptor<VertexType>,
            nonterminal: Nonterminal,
            targetStates: HashSet<RsmState>,
            curSppfNode: SppfNode<VertexType>?
        ) -> Unit,
        ctx: IContext<VertexType, LabelType>,
        curDescriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ) {
        val state = curDescriptor.rsmState
        val pos = curDescriptor.inputPosition
        val gssNode = curDescriptor.gssNode
        val terminalEdges = state.getTerminalEdges()
        val nonterminalEdges = state.getNonterminalEdges()
        for (inputEdge in ctx.input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                val descriptor = Descriptor(
                    state, gssNode, ctx.sppf.getParentNode(
                        state, curSppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                            terminal = null, pos, inputEdge.head
                        )
                    ), inputEdge.head
                )
                ctx.addDescriptor(descriptor)
                continue
            }
            for ((edgeTerminal, targetStates) in terminalEdges) {
                if (inputEdge.label.terminal == edgeTerminal) {
                    for (target in targetStates) {
                        handleTerminalOrEpsilonEdge(curDescriptor, curSppfNode, edgeTerminal, target, inputEdge.head, 0)
                    }
                }
            }
        }

        for ((edgeNonterminal, targetStates) in nonterminalEdges) {
            handleNonterminalEdge(curDescriptor, edgeNonterminal, targetStates, curSppfNode)
        }
    }


}