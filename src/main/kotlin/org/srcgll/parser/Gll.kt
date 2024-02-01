package org.srcgll.parser

import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.gss.GssNode
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
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
        val terminalEdges = state.getTerminalEdges()
        val nonterminalEdges = state.getNonterminalEdges()

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

        if (curSppfNode is SymbolSppfNode<VertexType> && state.nonterminal == ctx.startState.nonterminal
            && ctx.input.isStart(leftExtent!!) && ctx.input.isFinal(rightExtent!!)
        ) {
            if (ctx.parseResult == null || ctx.parseResult!!.weight > curSppfNode.weight) {
                ctx.parseResult = curSppfNode
            }

            val pair = Pair(leftExtent, rightExtent)
            val distance = ctx.sppf.minDistance(curSppfNode)

            ctx.reachabilityPairs[pair] = if (ctx.reachabilityPairs.containsKey(pair)) {
                minOf(distance, ctx.reachabilityPairs[pair]!!)
            } else {
                distance
            }
        }

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
                        val descriptor = Descriptor(
                            target, gssNode, ctx.sppf.getParentNode(
                                target, curSppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                                    edgeTerminal, pos, inputEdge.head
                                )
                            ), inputEdge.head
                        )
                        addDescriptor(descriptor)
                    }
                }
            }
        }

        for ((edgeNonterminal, targetStates) in nonterminalEdges) {
            for (target in targetStates) {
                val descriptor = Descriptor(
                    edgeNonterminal.startState,
                    createGssNode(edgeNonterminal, target, gssNode, curSppfNode, pos),
                    sppfNode = null,
                    pos
                )
                addDescriptor(descriptor)
            }
        }

        if (ctx.recovery == RecoveryMode.ON) {
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

            for ((terminal, errorRecoveryEdge) in errorRecoveryEdges) {
                if (terminal == null) {
                    handleTerminalOrEpsilonEdge(
                        curDescriptor, terminal = null, errorRecoveryEdge, curDescriptor.rsmState
                    )
                } else {

                    if (terminalEdges.containsKey(terminal)) {
                        for (targetState in terminalEdges.getValue(terminal)) {
                            handleTerminalOrEpsilonEdge(curDescriptor, terminal, errorRecoveryEdge, targetState)
                        }
                    }
                }
            }
        }

        if (state.isFinal) pop(gssNode, curSppfNode, pos)
    }

    private fun handleTerminalOrEpsilonEdge(
        curDescriptor: Descriptor<VertexType>,
        terminal: Terminal<*>?,
        targetEdge: TerminalRecoveryEdge<VertexType>,
        targetState: RsmState,
    ) {
        val descriptor = Descriptor(
            targetState, curDescriptor.gssNode, ctx.sppf.getParentNode(
                targetState, curDescriptor.sppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                    terminal, curDescriptor.inputPosition, targetEdge.head, targetEdge.weight
                )
            ), targetEdge.head
        )
        addDescriptor(descriptor)
    }


    private fun pop(gssNode: GssNode<VertexType>, sppfNode: SppfNode<VertexType>?, pos: VertexType) {
        if (!ctx.poppedGssNodes.containsKey(gssNode)) ctx.poppedGssNodes[gssNode] = HashSet()
        ctx.poppedGssNodes.getValue(gssNode).add(sppfNode)

        for ((label, target) in gssNode.edges) {
            for (node in target) {
                val descriptor = Descriptor(
                    label.first, node, ctx.sppf.getParentNode(label.first, label.second, sppfNode!!), pos
                )
                addDescriptor(descriptor)
            }
        }
    }
}

