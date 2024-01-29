package org.srcgll

import Context
import org.srcgll.descriptors.Descriptor
import org.srcgll.gss.GssNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.TerminalRecoveryEdge
import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode


class Gll<VertexType, LabelType : ILabel>(
    startState: RsmState,
    input: IGraph<VertexType, LabelType>,
    recovery: RecoveryMode,
) {
    var ctx: Context<VertexType, LabelType> = Context(startState, input, recovery)
    fun parse(): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        for (startVertex in ctx.input.getInputStartVertices()) {
            val descriptor = Descriptor(
                ctx.startState,
                getOrCreateGssNode(ctx.startState.nonterminal, startVertex, weight = 0),
                sppfNode = null,
                startVertex
            )
            addDescriptor(descriptor)
        }

        // Continue parsing until all default descriptors processed
        while (!ctx.stack.defaultDescriptorsStackIsEmpty()) {
            val curDefaultDescriptor = ctx.stack.next()

            parse(curDefaultDescriptor)
        }

        // If string was not parsed - process recovery descriptors until first valid parse tree is found
        // Due to the Error Recovery algorithm used it will be parse tree of the string with min editing cost
        while (ctx.recovery == RecoveryMode.ON && ctx.parseResult == null) {
            val curRecoveryDescriptor = ctx.stack.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(ctx.parseResult, ctx.reachabilityPairs)
    }

    fun parse(vertex: VertexType): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        ctx.stack.recoverDescriptors(vertex)
        ctx.sppf.invalidate(vertex, ctx.parseResult as ISppfNode)

        ctx.parseResult = null

        while (!ctx.stack.defaultDescriptorsStackIsEmpty()) {
            val curDefaultDescriptor = ctx.stack.next()

            parse(curDefaultDescriptor)
        }

        while (ctx.parseResult == null && ctx.recovery == RecoveryMode.ON) {
            val curRecoveryDescriptor = ctx.stack.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(ctx.parseResult, ctx.reachabilityPairs)
    }

    private fun parse(curDescriptor: Descriptor<VertexType>) {
        val state = curDescriptor.rsmState
        val pos = curDescriptor.inputPosition
        val gssNode = curDescriptor.gssNode
        var curSppfNode = curDescriptor.sppfNode
        var leftExtent = curSppfNode?.leftExtent
        var rightExtent = curSppfNode?.rightExtent
        val terminalEdges = state.getTerminalEdges()
        val nonterminalEdges = state.getNonterminalEdges()

        ctx.stack.addToHandled(curDescriptor)

        if (state.isStart && state.isFinal) {
            curSppfNode =
                ctx.sppf.getNodeP(state, curSppfNode, ctx.sppf.getOrCreateItemSppfNode(state, pos, pos, weight = 0))
            leftExtent = curSppfNode.leftExtent
            rightExtent = curSppfNode.rightExtent
        }

        if (curSppfNode is SymbolSppfNode<VertexType> && state.nonterminal == ctx.startState.nonterminal
            && ctx.input.isStart(leftExtent!!) && ctx.input.isFinal(rightExtent!!)
        ) {
            if (ctx.parseResult == null || ctx.parseResult!!.weight > curSppfNode.weight) {
                ctx.parseResult = curSppfNode
            }

            val pair = Pair(leftExtent, rightExtent)
            val distance = ctx.sppf.minDistance(curSppfNode)

            ctx.reachabilityPairs[pair] =
                if (ctx.reachabilityPairs.containsKey(pair)) {
                    minOf(distance, ctx.reachabilityPairs[pair]!!)
                } else {
                    distance
                }
        }

        for (inputEdge in ctx.input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                val descriptor = Descriptor(
                    state, gssNode, ctx.sppf.getNodeP(
                        state, curSppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                            terminal = null, pos, inputEdge.head, weight = 0
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
                            target, gssNode, ctx.sppf.getNodeP(
                                target, curSppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                                    edgeTerminal, pos, inputEdge.head, weight = 0
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
            targetState, curDescriptor.gssNode, ctx.sppf.getNodeP(
                targetState, curDescriptor.sppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                    terminal, curDescriptor.inputPosition, targetEdge.head, targetEdge.weight
                )
            ), targetEdge.head
        )
        addDescriptor(descriptor)
    }

    private fun addDescriptor(descriptor: Descriptor<VertexType>) {
        val sppfNode = descriptor.sppfNode
        val state = descriptor.rsmState
        val leftExtent = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

        if (ctx.parseResult == null && sppfNode is SymbolSppfNode<*> && state.nonterminal == ctx.startState.nonterminal
            && ctx.input.isStart(leftExtent!!) && ctx.input.isFinal(rightExtent!!)
        ) {
            ctx.stack.removeFromHandled(descriptor)
        }

        ctx.stack.add(descriptor)
    }

    private fun getOrCreateGssNode(
        nonterminal: Nonterminal,
        inputPosition: VertexType,
        weight: Int,
    ): GssNode<VertexType> {
        val gssNode = GssNode(nonterminal, inputPosition, weight)

        if (ctx.createdGssNodes.containsKey(gssNode)) {
            if (ctx.createdGssNodes.getValue(gssNode).minWeightOfLeftPart > weight) {
                ctx.createdGssNodes.getValue(gssNode).minWeightOfLeftPart = weight
            }
        } else ctx.createdGssNodes[gssNode] = gssNode

        return ctx.createdGssNodes.getValue(gssNode)
    }


    private fun createGssNode(
        nonterminal: Nonterminal,
        state: RsmState,
        gssNode: GssNode<VertexType>,
        sppfNode: SppfNode<VertexType>?,
        pos: VertexType,
    ): GssNode<VertexType> {
        val newNode =
            getOrCreateGssNode(nonterminal, pos, weight = gssNode.minWeightOfLeftPart + (sppfNode?.weight ?: 0))

        if (newNode.addEdge(state, sppfNode, gssNode)) {
            if (ctx.poppedGssNodes.containsKey(newNode)) {
                for (popped in ctx.poppedGssNodes[newNode]!!) {
                    val descriptor = Descriptor(
                        state, gssNode, ctx.sppf.getNodeP(state, sppfNode, popped!!), popped.rightExtent
                    )
                    addDescriptor(descriptor)
                }
            }
        }

        return newNode
    }

    private fun pop(gssNode: GssNode<VertexType>, sppfNode: SppfNode<VertexType>?, pos: VertexType) {
        if (!ctx.poppedGssNodes.containsKey(gssNode)) ctx.poppedGssNodes[gssNode] = HashSet()
        ctx.poppedGssNodes.getValue(gssNode).add(sppfNode)

        for ((label, target) in gssNode.edges) {
            for (node in target) {
                val descriptor = Descriptor(
                    label.first, node, ctx.sppf.getNodeP(label.first, label.second, sppfNode!!), pos
                )
                addDescriptor(descriptor)
            }
        }
    }
}
