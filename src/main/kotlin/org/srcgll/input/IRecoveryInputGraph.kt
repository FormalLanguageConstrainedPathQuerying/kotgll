package org.srcgll.input

import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.TerminalRecoveryEdge
import org.srcgll.sppf.node.SppfNode

interface IRecoveryInputGraph<VertexType, LabelType : ILabel> : IInputGraph<VertexType, LabelType> {
    override fun handleEdges(
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
        super.handleEdges(handleTerminalOrEpsilonEdge, handleNonterminalEdge, ctx, curDescriptor, curSppfNode)
        val errorRecoveryEdges = createRecoveryEdges(curDescriptor)
        handleRecoveryEdges(
            errorRecoveryEdges,
            handleTerminalOrEpsilonEdge,
            curDescriptor,
            curDescriptor.rsmState.getTerminalEdges()
        )
    }

    private fun createRecoveryEdges(curDescriptor: Descriptor<VertexType>): HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>> {
        val pos = curDescriptor.inputPosition
        val state = curDescriptor.rsmState
        val terminalEdges = state.getTerminalEdges()

        val errorRecoveryEdges = HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>()
        val currentEdges = getEdges(pos)

        if (currentEdges.isNotEmpty()) {
            addTerminalRecoveryEdges(terminalEdges, errorRecoveryEdges, pos, state, currentEdges)
        } else {
            addEpsilonRecoveryEdges(terminalEdges, errorRecoveryEdges, pos, state)
        }

        return errorRecoveryEdges
    }

    private fun addEpsilonRecoveryEdges(
        terminalEdges: HashMap<Terminal<*>, HashSet<RsmState>>,
        errorRecoveryEdges: HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>,
        pos: VertexType,
        state: RsmState
    ) {
        for (terminal in state.errorRecoveryLabels) {
            if (!terminalEdges[terminal].isNullOrEmpty()) {
                errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
            }
        }
    }

    /**
     * Trying to reach states that were previously inaccessible using recovery terminal
     */
    private fun addTerminalRecoveryEdges(
        terminalEdges: HashMap<Terminal<*>, HashSet<RsmState>>,
        errorRecoveryEdges: HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>,
        pos: VertexType,
        state: RsmState,
        currentEdges: MutableList<Edge<VertexType, LabelType>>
    ) {
        for (currentEdge in currentEdges) {
            if (currentEdge.label.terminal == null) continue
            val currentTerminal = currentEdge.label.terminal!!

            val coveredByCurrentTerminal: HashSet<RsmState> = terminalEdges[currentTerminal] ?: hashSetOf()

            for (terminal in state.errorRecoveryLabels) {
                //accessible states
                val coveredByTerminal = HashSet(terminalEdges[terminal] as HashSet<RsmState>)

                coveredByCurrentTerminal.forEach { coveredByTerminal.remove(it) }

                if (terminal != currentTerminal && coveredByTerminal.isNotEmpty()) {
                    errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                }
            }

            errorRecoveryEdges[null] = TerminalRecoveryEdge(currentEdge.head, weight = 1)
        }
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