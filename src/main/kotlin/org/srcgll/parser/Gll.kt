package org.srcgll.parser

import org.srcgll.descriptors.Descriptor
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.input.IRecoveryInputGraph
import org.srcgll.parser.context.Context
import org.srcgll.parser.context.IContext
import org.srcgll.parser.context.RecoveryContext
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.SppfNode

/**
 * Gll Factory
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
class Gll<VertexType, LabelType : ILabel> private constructor(
    override var ctx: IContext<VertexType, LabelType>,
) : IGll<VertexType, LabelType> {

    companion object {
        /**
         * Creates instance of incremental Gll
         * @param startState - starting state of accepting nonterminal in RSM
         * @param inputGraph - input graph
         * @return default instance of gll parser
         */
        fun <VertexType, LabelType : ILabel> gll(
            startState: RsmState,
            inputGraph: IInputGraph<VertexType, LabelType>
        ): Gll<VertexType, LabelType> {
            return Gll(Context(startState, inputGraph))
        }

        /**
         * Part of error recovery mechanism
         * Creates instance of incremental Gll with error recovery
         * @param startState - starting state of accepting nonterminal in RSM
         * @param inputGraph - input graph
         * @return recovery instance of gll parser
         */
        fun <VertexType, LabelType : ILabel> recoveryGll(
            startState: RsmState,
            inputGraph: IRecoveryInputGraph<VertexType, LabelType>
        ): Gll<VertexType, LabelType> {
            return Gll(RecoveryContext(startState, inputGraph))
        }
    }

    /**
     * Part of incrementality mechanism.
     * Perform incremental parsing, via restoring and adding to handling already processed descriptors,
     * which contain given vertex as their field
     * @param vertex - vertex in input graph, which outgoing edges were modified
     */
    fun parse(vertex: VertexType): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        ctx.descriptors.restoreDescriptors(vertex)
        ctx.sppf.invalidate(vertex, ctx.parseResult as ISppfNode)
        ctx.parseResult = null
        return parse()
    }

    /**
     * Processes descriptor
     * @param descriptor - descriptor to process
     */
    override fun parse(descriptor: Descriptor<VertexType>) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition
        val sppfNode = descriptor.sppfNode
        val epsilonSppfNode = ctx.sppf.getEpsilonSppfNode(descriptor)

        if (state.isFinal) {
            pop(descriptor.gssNode, sppfNode ?: epsilonSppfNode, pos)
        }

        val leftExtent = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

        ctx.descriptors.addToHandled(descriptor)

        if (state.isStart && state.isFinal) {
            checkAcceptance(epsilonSppfNode, epsilonSppfNode!!.leftExtent, epsilonSppfNode!!.rightExtent, state.nonterminal)
        }
        checkAcceptance(sppfNode, leftExtent, rightExtent, state.nonterminal)

        ctx.input.handleEdges(
            this::handleTerminalOrEpsilonEdge,
            this::handleNonterminalEdge,
            ctx,
            descriptor,
            sppfNode
        )
    }
}

