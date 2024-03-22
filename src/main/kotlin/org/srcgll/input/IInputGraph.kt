package org.srcgll.input

import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode

interface IInputGraph<VertexType, LabelType : ILabel> {
    val vertices: MutableSet<VertexType>
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
            descriptor: Descriptor<VertexType>,
            sppfNode: SppfNode<VertexType>?,
            terminal: Terminal<*>?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        handleNonterminalEdge: (
            descriptor: Descriptor<VertexType>,
            nonterminal: Nonterminal,
            targetStates: HashSet<RsmState>,
            sppfNode: SppfNode<VertexType>?
        ) -> Unit,
        ctx: IContext<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?
    ) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition
        val terminalEdges = state.terminalEdges
        val nonterminalEdges = state.nonterminalEdges

        for (inputEdge in ctx.input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                handleNullLabel(descriptor, sppfNode, inputEdge, ctx)
                continue
            }
            for ((edgeTerminal, targetStates) in terminalEdges) {
                if (inputEdge.label.terminal == edgeTerminal) {
                    for (target in targetStates) {
                        handleTerminalOrEpsilonEdge(descriptor, sppfNode, edgeTerminal, target, inputEdge.head, 0)
                    }
                }
            }
        }

        for ((edgeNonterminal, targetStates) in nonterminalEdges) {
            handleNonterminalEdge(descriptor, edgeNonterminal, targetStates, sppfNode)
        }
    }

    fun handleNullLabel(
        descriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?,
        inputEdge: Edge<VertexType, LabelType>,
        ctx: IContext<VertexType, LabelType>
    ) {
        val newDescriptor = Descriptor(
            descriptor.rsmState, descriptor.gssNode, ctx.sppf.getParentNode(
                descriptor.rsmState, sppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                    terminal = null, descriptor.inputPosition, inputEdge.head
                )
            ), inputEdge.head
        )
        ctx.addDescriptor(newDescriptor)
    }
}