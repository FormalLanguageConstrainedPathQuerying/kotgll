package org.ucfs.intersection

import org.ucfs.descriptors.Descriptor
import org.ucfs.input.ILabel
import org.ucfs.parser.IGll
import org.ucfs.sppf.node.SppfNode

object IntersectionEngine : IIntersectionEngine {

    /**
     * Process outgoing edges from input position in given descriptor, according to processing logic, represented as
     * separate functions for both outgoing terminal and nonterminal edges from rsmState in descriptor
     * @param gll - Gll parser instance
     * @param descriptor - descriptor, represents current parsing stage
     * @param sppfNode - root node of derivation tree, corresponds to already parsed portion of input
     */
    override fun <VertexType, LabelType : ILabel> handleEdges(
        gll: IGll<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?
    ) {
        val rsmState = descriptor.rsmState
        val inputPosition = descriptor.inputPosition
        val terminalEdges = rsmState.terminalEdges
        val nonterminalEdges = rsmState.nonterminalEdges

        for (inputEdge in gll.ctx.input.getEdges(inputPosition)) {
            val terminal = inputEdge.label.terminal
            if (terminal == null) {
                gll.handleTerminalOrEpsilonEdge(descriptor, sppfNode, null, descriptor.rsmState, inputEdge.head, 0)
                continue
            }
            val targetStates = terminalEdges[inputEdge.label.terminal]
            if (targetStates != null) {
                for (targetState in targetStates) {
                    gll.handleTerminalOrEpsilonEdge(descriptor, sppfNode, terminal, targetState, inputEdge.head, 0)
                }
            }
        }

        for ((edgeNonterminal, targetStates) in nonterminalEdges) {
            gll.handleNonterminalEdge(descriptor, edgeNonterminal, targetStates, sppfNode)
        }
    }
}