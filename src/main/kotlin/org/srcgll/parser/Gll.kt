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


class Gll<VertexType, LabelType : ILabel> private constructor(
    override var ctx: IContext<VertexType, LabelType>,
) : IGll<VertexType, LabelType> {

    companion object {
        /**
         * Create instance of incremental Gll
         */
        fun <VertexType, LabelType : ILabel> gll(
            startState: RsmState,
            inputGraph: IInputGraph<VertexType, LabelType>
        ): Gll<VertexType, LabelType> {
            return Gll(Context(startState, inputGraph))
        }

        /**
         * Create instance of incremental Gll with error recovery
         */
        fun <VertexType, LabelType : ILabel> recoveryGll(
            startState: RsmState,
            inputGraph: IRecoveryInputGraph<VertexType, LabelType>
        ): Gll<VertexType, LabelType> {
            return Gll(RecoveryContext(startState, inputGraph))
        }
    }

    fun parse(vertex: VertexType): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        ctx.descriptors.restoreDescriptors(vertex)
        ctx.sppf.invalidate(vertex, ctx.parseResult as ISppfNode)
        ctx.parseResult = null
        return parse()
    }


    override fun parse(curDescriptor: Descriptor<VertexType>) {
        val state = curDescriptor.rsmState
        val pos = curDescriptor.inputPosition
        val curSppfNode = curDescriptor.getCurSppfNode(ctx)

        ctx.descriptors.addToHandled(curDescriptor)

        val leftExtent = curSppfNode?.leftExtent
        val rightExtent = curSppfNode?.rightExtent

        checkAcceptance(curSppfNode, leftExtent, rightExtent, state.nonterminal)

        ctx.input.handleEdges(
            this::handleTerminalOrEpsilonEdge,
            this::handleNonterminalEdge,
            ctx,
            curDescriptor,
            curSppfNode
        )

        if (state.isFinal) pop(curDescriptor.gssNode, curSppfNode, pos)
    }

}

