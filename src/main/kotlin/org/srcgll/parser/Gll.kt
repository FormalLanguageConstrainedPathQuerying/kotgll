package org.srcgll.parser

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.TerminalRecoveryEdge
import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode


class Gll<VertexType, LabelType : ILabel>(
    override val ctx: IContext<VertexType, LabelType>,
) : GllParser<VertexType, LabelType> {

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
            ctx.sppf.getParentNode(
                state, curDescriptor.sppfNode, ctx.sppf.getOrCreateIntermediateSppfNode(state, pos, pos, weight = 0)
            )
        } else {
            curDescriptor.sppfNode
        }

        val leftExtent = curSppfNode?.leftExtent
        val rightExtent = curSppfNode?.rightExtent

        checkAcceptance(curSppfNode, leftExtent, rightExtent, state.nonterminal)

        handleEdges(this::handleTerminalOrEpsilonEdge, this::handleNonterminalEdge, ctx, curDescriptor, curSppfNode)

        if (state.isFinal) pop(gssNode, curSppfNode, pos)
    }

    private fun handleEdges(
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
                addDescriptor(descriptor)
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
            handleRecoveryEdges(errorRecoveryEdges, this::handleTerminalOrEpsilonEdge, curDescriptor, terminalEdges)
        }
    }

    private fun createRecoveryEdges(curDescriptor: Descriptor<VertexType>): HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>> {
        val pos = curDescriptor.inputPosition
        val state = curDescriptor.rsmState
        val terminalEdges = state.getTerminalEdges()


        val errorRecoveryEdges = HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>()
        val currentEdges = ctx.input.getEdges(pos)
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

