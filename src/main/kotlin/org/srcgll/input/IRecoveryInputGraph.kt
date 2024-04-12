package org.srcgll.input

import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.ITerminal
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode

/**
 * Part of error recovery mechanism.
 * Input graph interface with additional methods to support error recovery logic
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
interface IRecoveryInputGraph<VertexType, LabelType : ILabel> : IInputGraph<VertexType, LabelType> {
    /**
     * Process outgoing edges from input position in given descriptor, according to processing logic, represented as
     * separate functions for processing both  outgoing terminal and nonterminal edges from rsmState in descriptor.
     * Additionally, considers error recovering edges in input graph. Those are the edges that were not present in
     * initial graph, but could be useful later to successfully parse input
     * @param handleTerminalOrEpsilonEdge - function for processing terminal and epsilon edges in RSM
     * @param handleNonterminalEdge - function for processing nonterminal edges in RSM
     * @param ctx - configuration of Gll parser instance
     * @param descriptor - descriptor, represents current parsing stage
     * @param sppfNode - root node of derivation tree, corresponds to already parsed portion of input
     */
    override fun handleEdges(
        handleTerminalOrEpsilonEdge: (
            curDescriptor: Descriptor<VertexType>,
            curSppfNode: SppfNode<VertexType>?,
            terminal: ITerminal?,
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
            descriptor
        )
    }

    /**
     * Collects all possible edges, via which we can traverse in RSM
     * @param descriptor - current parsing stage
     * @return Map terminal -> (destination, weight)
     */
    private fun createRecoveryEdges(descriptor: Descriptor<VertexType>): HashSet<RecoveryEdge<VertexType>> {
        val inputPosition = descriptor.inputPosition
        val rsmState = descriptor.rsmState
        val terminalEdges = rsmState.terminalEdges

        val errorRecoveryEdges = HashSet<RecoveryEdge<VertexType>>()
        val currentEdges = getEdges(inputPosition)

        if (currentEdges.isNotEmpty()) {
            addTerminalRecoveryEdges(terminalEdges, errorRecoveryEdges, inputPosition, rsmState, currentEdges)
        } else {
            addEpsilonRecoveryEdges(terminalEdges, errorRecoveryEdges, inputPosition, rsmState)
        }

        return errorRecoveryEdges
    }

    /**
     * Collects edges with epsilon on label. This corresponds to deletion in input
     * @param terminalEdges - outgoing terminal edges from current rsmState
     * @param errorRecoveryEdges - reference to map of all error recovery edges
     * @param inputPosition - current vertex in input graph
     * @param rsmState - current rsmState
     */
    private fun addEpsilonRecoveryEdges(
        terminalEdges: HashMap<ITerminal, HashSet<RsmState>>,
        errorRecoveryEdges: HashSet<RecoveryEdge<VertexType>>,
        inputPosition: VertexType,
        rsmState: RsmState
    ) {
        for (terminal in rsmState.errorRecoveryLabels) {
            if (!terminalEdges[terminal].isNullOrEmpty()) {
                errorRecoveryEdges.add(RecoveryEdge(terminal, inputPosition, weight = 1))
            }
        }
    }

    /**
     * Collects edges with terminal on label. This corresponds to insertion in input
     * @param terminalEdges - outgoing terminal edges from current rsmState
     * @param errorRecoveryEdges - reference to map of all error recovery edges
     * @param inputPosition - current vertex in input graph
     * @param rsmState - current rsmState
     * @param currentEdges - outgoing edges from current vertex in input graph
     */
    private fun addTerminalRecoveryEdges(
        terminalEdges: HashMap<ITerminal, HashSet<RsmState>>,
        errorRecoveryEdges: HashSet<RecoveryEdge<VertexType>>,
        inputPosition: VertexType,
        rsmState: RsmState,
        currentEdges: MutableList<Edge<VertexType, LabelType>>
    ) {
        for (currentEdge in currentEdges) {
            if (currentEdge.label.terminal == null) continue
            val currentTerminal = currentEdge.label.terminal!!

            val coveredByCurrentTerminal: HashSet<RsmState> = terminalEdges[currentTerminal] ?: HashSet()

            for (terminal in rsmState.errorRecoveryLabels) {
                //accessible states
                val coveredByTerminal = HashSet(terminalEdges[terminal] as HashSet<RsmState>)

                coveredByCurrentTerminal.forEach { coveredByTerminal.remove(it) }

                if (terminal != currentTerminal && coveredByTerminal.isNotEmpty()) {
                    errorRecoveryEdges.add(RecoveryEdge(terminal, inputPosition, weight = 1))
                }
            }

            errorRecoveryEdges.add(RecoveryEdge(null, currentEdge.head, weight = 1))
        }
    }

    /**
     * Processes created error recovery edges and adds corresponding error recovery descriptors to handling
     * @param errorRecoveryEdges - collection of created error recovery edges
     * @param handleTerminalOrEpsilonEdge - function to process error recovery edges
     * @param descriptor - current parsing stage
     */
    private fun handleRecoveryEdges(
        errorRecoveryEdges: HashSet<RecoveryEdge<VertexType>>,
        handleTerminalOrEpsilonEdge: (
            descriptor: Descriptor<VertexType>,
            sppfNode: SppfNode<VertexType>?,
            terminal: ITerminal?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        descriptor: Descriptor<VertexType>
    ) {
        val terminalEdges = descriptor.rsmState.terminalEdges

        for (errorRecoveryEdge in errorRecoveryEdges) {
            val terminal = errorRecoveryEdge.label
            val head = errorRecoveryEdge.head
            val weight = errorRecoveryEdge.weight

            if (terminal == null) {
                handleTerminalOrEpsilonEdge(
                    descriptor,
                    descriptor.sppfNode,
                    null,
                    descriptor.rsmState,
                    head,
                    weight
                )
            } else if (terminalEdges.containsKey(errorRecoveryEdge.label)) {
                for (targetState in terminalEdges.getValue(terminal)) {
                    handleTerminalOrEpsilonEdge(
                        descriptor,
                        descriptor.sppfNode,
                        terminal,
                        targetState,
                        head,
                        weight
                    )
                }
            }
        }

    }


}