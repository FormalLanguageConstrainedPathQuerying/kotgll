package org.srcgll.input

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.TerminalRecoveryEdge
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

        if (ctx.recovery == RecoveryMode.ON) {
            val errorRecoveryEdges = createRecoveryEdges(curDescriptor)
            handleRecoveryEdges(errorRecoveryEdges, handleTerminalOrEpsilonEdge, curDescriptor, terminalEdges)
        }
    }

    private fun createRecoveryEdges(curDescriptor: Descriptor<VertexType>): HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>> {
        val pos = curDescriptor.inputPosition
        val state = curDescriptor.rsmState
        val terminalEdges = state.getTerminalEdges()


        val errorRecoveryEdges = HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>()
        val currentEdges = getEdges(pos)
        if (currentEdges.isNotEmpty()) {
            for (currentEdge in currentEdges) {
                if (currentEdge.label.terminal == null) continue

                val currentTerminal = currentEdge.label.terminal!!

                val coveredByCurrentTerminal: HashSet<RsmState> = terminalEdges[currentTerminal] ?: hashSetOf()

                for (terminal in state.errorRecoveryLabels) {
                    val coveredByTerminal = HashSet(terminalEdges[terminal] as HashSet<RsmState>)

                    coveredByCurrentTerminal.forEach { coveredByTerminal.remove(it) }

                    if (terminal != currentTerminal && coveredByTerminal.isNotEmpty()) {
                        errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                    }
                }

                errorRecoveryEdges[null] = TerminalRecoveryEdge(currentEdge.head, weight = 1)
            }
        } else {
            for (terminal in state.errorRecoveryLabels) {
                if (!terminalEdges[terminal].isNullOrEmpty()) {
                    errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                }
            }
        }
        return errorRecoveryEdges
    }

    private fun handleRecoveryEdges(
        errorRecoveryEdges: HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>,
        handleTerminalOrEpsilonEdge: (
            curDescriptor: Descriptor<VertexType>,
            curSppfNode: SppfNode<VertexType>?,
            terminal: Terminal<*>?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        curDescriptor: Descriptor<VertexType>,
        terminalEdges: HashMap<Terminal<*>, HashSet<RsmState>>
    ) {
        for ((terminal, errorRecoveryEdge) in errorRecoveryEdges) {
            if (terminal == null) {
                handleTerminalOrEpsilonEdge(
                    curDescriptor,
                    curDescriptor.sppfNode,
                    null,
                    curDescriptor.rsmState,
                    errorRecoveryEdge.head,
                    errorRecoveryEdge.weight
                )
            } else {

                if (terminalEdges.containsKey(terminal)) {
                    for (targetState in terminalEdges.getValue(terminal)) {
                        handleTerminalOrEpsilonEdge(
                            curDescriptor,
                            curDescriptor.sppfNode,
                            terminal,
                            targetState,
                            errorRecoveryEdge.head,
                            errorRecoveryEdge.weight
                        )
                    }
                }
            }
        }

    }

}