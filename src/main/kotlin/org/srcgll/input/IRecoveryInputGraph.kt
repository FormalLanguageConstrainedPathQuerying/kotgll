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
        super.handleEdges(handleTerminalOrEpsilonEdge, handleNonterminalEdge, ctx, descriptor, sppfNode)
        val errorRecoveryEdges = createRecoveryEdges(descriptor)
        handleRecoveryEdges(
            errorRecoveryEdges,
            handleTerminalOrEpsilonEdge,
            descriptor,
            descriptor.rsmState.terminalEdges
        )
    }

    private fun createRecoveryEdges(descriptor: Descriptor<VertexType>): HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>> {
        val pos = descriptor.inputPosition
        val state = descriptor.rsmState
        val terminalEdges = state.terminalEdges

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
            descriptor: Descriptor<VertexType>,
            sppfNode: SppfNode<VertexType>?,
            terminal: Terminal<*>?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        descriptor: Descriptor<VertexType>,
        terminalEdges: HashMap<Terminal<*>, HashSet<RsmState>>
    ) {
        for ((terminal, errorRecoveryEdge) in errorRecoveryEdges) {
            if (terminal == null) {
                handleTerminalOrEpsilonEdge(
                    descriptor,
                    descriptor.sppfNode,
                    null,
                    descriptor.rsmState,
                    errorRecoveryEdge.head,
                    errorRecoveryEdge.weight
                )
            } else if (terminalEdges.containsKey(terminal)) {
                for (targetState in terminalEdges.getValue(terminal)) {
                    handleTerminalOrEpsilonEdge(
                        descriptor,
                        descriptor.sppfNode,
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