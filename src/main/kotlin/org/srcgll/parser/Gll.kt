package org.srcgll.parser

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.input.IRecoveryInputGraph
import org.srcgll.parser.context.Context
import org.srcgll.parser.context.IContext
import org.srcgll.parser.context.RecoveryContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode


class Gll<VertexType, LabelType : ILabel> private constructor(
    override val ctx: IContext<VertexType, LabelType>,
) : GllParser<VertexType, LabelType> {

    companion object {
        fun <VertexType, LabelType : ILabel> gll(
            startState: RsmState,
            inputGraph: IInputGraph<VertexType, LabelType>
        ): Gll<VertexType, LabelType> {
            return Gll(Context(startState, inputGraph))
        }

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

        while (!ctx.descriptors.defaultDescriptorsStorageIsEmpty()) {
            val curDefaultDescriptor = ctx.descriptors.next()

            parse(curDefaultDescriptor)
        }

        while (ctx.parseResult == null && ctx.recovery == RecoveryMode.ON) {
            val curRecoveryDescriptor = ctx.descriptors.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(ctx.parseResult, ctx.reachabilityPairs)
    }


    override fun parse(curDescriptor: Descriptor<VertexType>) {
        val state = curDescriptor.rsmState
        val pos = curDescriptor.inputPosition
        val gssNode = curDescriptor.gssNode

        ctx.descriptors.addToHandled(curDescriptor)

        val curSppfNode = if (state.isStart && state.isFinal) {
            // if nonterminal accept epsilon
            ctx.sppf.getParentNode(
                state, curDescriptor.sppfNode, ctx.sppf.getOrCreateIntermediateSppfNode(state, pos, pos, weight = 0)
            )
        } else {
            curDescriptor.sppfNode
        }

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

        if (state.isFinal) pop(gssNode, curSppfNode, pos)
    }


    private fun handleNonterminalEdge(
        descriptor: Descriptor<VertexType>,
        nonterminal: Nonterminal,
        targetStates: HashSet<RsmState>,
        curSppfNode: SppfNode<VertexType>?
    ) {
        for (target in targetStates) {
            val newDescriptor = Descriptor(
                nonterminal.startState,
                createGssNode(nonterminal, target, descriptor.gssNode, curSppfNode, descriptor.inputPosition),
                sppfNode = null,
                descriptor.inputPosition
            )
            ctx.addDescriptor(newDescriptor)
        }
    }


    /**
     * Check that parsed nonterminal accepts whole input
     * Update result of parsing
     */
    private fun checkAcceptance(
        sppfNode: SppfNode<VertexType>?,
        leftExtent: VertexType?,
        rightExtent: VertexType?,
        nonterminal: Nonterminal
    ) {
        if (sppfNode is SymbolSppfNode<VertexType> && nonterminal == ctx.startState.nonterminal
            && ctx.input.isStart(leftExtent!!) && ctx.input.isFinal(rightExtent!!)
        ) {
            if (ctx.parseResult == null || ctx.parseResult!!.weight > sppfNode.weight) {
                ctx.parseResult = sppfNode
            }

            //update reachability
            val pair = Pair(leftExtent, rightExtent)
            val distance = ctx.sppf.minDistance(sppfNode)

            ctx.reachabilityPairs[pair] = if (ctx.reachabilityPairs.containsKey(pair)) {
                minOf(distance, ctx.reachabilityPairs[pair]!!)
            } else {
                distance
            }
        }
    }


    private fun handleTerminalOrEpsilonEdge(
        curDescriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?,
        terminal: Terminal<*>?,
        targetState: RsmState,
        targetVertex: VertexType,
        targetWeight: Int = 0,
    ) {
        val descriptor = Descriptor(
            targetState, curDescriptor.gssNode, ctx.sppf.getParentNode(
                targetState, curSppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                    terminal, curDescriptor.inputPosition, targetVertex, targetWeight
                )
            ), targetVertex
        )
        addDescriptor(descriptor)
    }


}

