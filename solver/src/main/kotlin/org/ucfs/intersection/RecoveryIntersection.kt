package org.ucfs.intersection

import org.ucfs.descriptors.Descriptor
import org.ucfs.input.Edge
import org.ucfs.input.ILabel
import org.ucfs.input.RecoveryEdge
import org.ucfs.parser.IGll
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.sppf.node.SppfNode

object RecoveryIntersection : IIntersectionEngine{
    /**
     * Process outgoing edges from input position in given descriptor, according to processing logic, represented as
     * separate functions for processing both  outgoing terminal and nonterminal edges from rsmState in descriptor.
     * Additionally, considers error recovering edges in input graph. Those are the edges that were not present in
     * initial graph, but could be useful later to successfully parse input
     * @param gll - Gll parser instance
     * @param descriptor - descriptor, represents current parsing stage
     * @param sppfNode - root node of derivation tree, corresponds to already parsed portion of input
     */
    override fun <VertexType, LabelType : ILabel> handleEdges(
        gll: IGll<VertexType, LabelType>, descriptor: Descriptor<VertexType>, sppfNode: SppfNode<VertexType>?
    ) {
        IntersectionEngine.handleEdges(gll, descriptor, sppfNode)
        handleRecoveryEdges(gll, descriptor)
    }

    /**
     * Collects all possible edges, via which we can traverse in RSM
     * @param descriptor - current parsing stage
     * @return Map terminal -> (destination, weight)
     */
    private fun <VertexType, LabelType : ILabel> createRecoveryEdges(
        gll: IGll<VertexType, LabelType>, descriptor: Descriptor<VertexType>
    ): HashSet<RecoveryEdge<VertexType>> {
        val inputPosition = descriptor.inputPosition
        val rsmState = descriptor.rsmState
        val terminalEdges = rsmState.terminalEdges

        val errorRecoveryEdges = HashSet<RecoveryEdge<VertexType>>()
        val currentEdges = gll.ctx.input.getEdges(inputPosition)

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
    private fun <VertexType> addEpsilonRecoveryEdges(
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
    private fun <VertexType, LabelType : ILabel> addTerminalRecoveryEdges(
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
     * Creates and processes error recovery edges and adds corresponding error recovery descriptors to handling
     * @param gll - GLL parser instance
     * @param descriptor - current parsing stage
     */
    fun <VertexType, LabelType : ILabel> handleRecoveryEdges(
        gll: IGll<VertexType, LabelType>, descriptor: Descriptor<VertexType>
    ) {
        val errorRecoveryEdges: HashSet<RecoveryEdge<VertexType>> = createRecoveryEdges(gll, descriptor)
        val terminalEdges = descriptor.rsmState.terminalEdges

        for (errorRecoveryEdge in errorRecoveryEdges) {
            val terminal = errorRecoveryEdge.label
            val head = errorRecoveryEdge.head
            val weight = errorRecoveryEdge.weight

            if (terminal == null) {
                gll.handleTerminalOrEpsilonEdge(
                    descriptor, descriptor.sppfNode, null, descriptor.rsmState, head, weight
                )
            } else if (terminalEdges.containsKey(errorRecoveryEdge.label)) {
                for (targetState in terminalEdges.getValue(terminal)) {
                    gll.handleTerminalOrEpsilonEdge(
                        descriptor, descriptor.sppfNode, terminal, targetState, head, weight
                    )
                }
            }
        }

    }

}