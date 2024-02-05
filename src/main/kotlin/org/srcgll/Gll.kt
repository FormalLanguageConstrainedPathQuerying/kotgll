package org.srcgll

import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.ErrorRecoveringDescriptorsStack
import org.srcgll.descriptors.IDescriptorsStack
import org.srcgll.gss.GssNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.TerminalRecoveryEdge
import org.srcgll.sppf.node.*


class Gll<VertexType, LabelType : ILabel>(
    private val startState: RsmState,
    private val input: IGraph<VertexType, LabelType>,
    private val recovery: RecoveryMode,
    private val reachability: ReachabilityMode = ReachabilityMode.REACHABILITY,
) {
    private val stack: IDescriptorsStack<VertexType> = ErrorRecoveringDescriptorsStack()
    private val sppf: Sppf<VertexType> = Sppf()
    private val poppedGssNodes: HashMap<GssNode<VertexType>, HashSet<SppfNode<VertexType>?>> = HashMap()
    private val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    private var parseResult: SppfNode<VertexType>? = null
    private val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()

    fun parse(): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        for (startVertex in input.getInputStartVertices()) {
            val descriptor = Descriptor(
                startState,
                getOrCreateGssNode(startState.nonterminal, startVertex, weight = 0),
                sppfNode = null,
                startVertex
            )
            addDescriptor(descriptor)
        }

        // Continue parsing until all default descriptors processed
        while (!stack.defaultDescriptorsStackIsEmpty()) {
            val curDefaultDescriptor = stack.next()

            parse(curDefaultDescriptor)
        }

        // If string was not parsed - process recovery descriptors until first valid parse tree is found
        // Due to the Error Recovery algorithm used it will be parse tree of the string with min editing cost
        while (recovery == RecoveryMode.ON && parseResult == null) {
            val curRecoveryDescriptor = stack.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(parseResult, reachabilityPairs)
    }

    fun parse(vertex: VertexType): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        stack.recoverDescriptors(vertex)
        sppf.invalidate(vertex, parseResult as ISppfNode)

        parseResult = null

        while (!stack.defaultDescriptorsStackIsEmpty()) {
            val curDefaultDescriptor = stack.next()

            parse(curDefaultDescriptor)
        }

        while (parseResult == null && recovery == RecoveryMode.ON) {
            val curRecoveryDescriptor = stack.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(parseResult, reachabilityPairs)
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

        stack.addToHandled(curDescriptor)

        if (state.isStart && state.isFinal) {
            curSppfNode = sppf.getNodeP(state, curSppfNode, sppf.getOrCreateItemSppfNode(state, pos, pos, weight = 0))
            leftExtent = curSppfNode.leftExtent
            rightExtent = curSppfNode.rightExtent
        }

        if (curSppfNode is SymbolSppfNode<VertexType> && state.nonterminal == startState.nonterminal
            && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)
        ) {
            if (parseResult == null || parseResult!!.weight > curSppfNode.weight) {
                parseResult = curSppfNode
            }

            val pair = Pair(leftExtent, rightExtent)
            var distance = 0

            if (reachability == ReachabilityMode.ALLPAIRS) {
                distance = sppf.minDistance(curSppfNode)
            }

            reachabilityPairs[pair] =
                if (reachabilityPairs.containsKey(pair)) {
                    minOf(distance, reachabilityPairs[pair]!!)
                } else {
                    distance
                }
        }

        for (inputEdge in input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                val descriptor = Descriptor(
                    state, gssNode, sppf.getNodeP(
                        state, curSppfNode, sppf.getOrCreateTerminalSppfNode(
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
                            target, gssNode, sppf.getNodeP(
                                target, curSppfNode, sppf.getOrCreateTerminalSppfNode(
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

        if (recovery == RecoveryMode.ON) {
            val errorRecoveryEdges = HashMap<Terminal<*>?, TerminalRecoveryEdge<VertexType>>()
            val currentEdges = input.getEdges(pos)

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
            targetState, curDescriptor.gssNode, sppf.getNodeP(
                targetState, curDescriptor.sppfNode, sppf.getOrCreateTerminalSppfNode(
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

        if (parseResult == null && sppfNode is SymbolSppfNode<*> && state.nonterminal == startState.nonterminal
            && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)
        ) {
            stack.removeFromHandled(descriptor)
        }

        stack.add(descriptor)
    }

    private fun getOrCreateGssNode(
        nonterminal: Nonterminal,
        inputPosition: VertexType,
        weight: Int,
    ): GssNode<VertexType> {
        val gssNode = GssNode(nonterminal, inputPosition, weight)

        if (createdGssNodes.containsKey(gssNode)) {
            if (createdGssNodes.getValue(gssNode).minWeightOfLeftPart > weight) {
                createdGssNodes.getValue(gssNode).minWeightOfLeftPart = weight
            }
        } else createdGssNodes[gssNode] = gssNode

        return createdGssNodes.getValue(gssNode)
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
            if (poppedGssNodes.containsKey(newNode)) {
                for (popped in poppedGssNodes[newNode]!!) {
                    val descriptor = Descriptor(
                        state, gssNode, sppf.getNodeP(state, sppfNode, popped!!), popped.rightExtent
                    )
                    addDescriptor(descriptor)
                }
            }
        }

        return newNode
    }

    private fun pop(gssNode: GssNode<VertexType>, sppfNode: SppfNode<VertexType>?, pos: VertexType) {
        if (!poppedGssNodes.containsKey(gssNode)) poppedGssNodes[gssNode] = HashSet()
        poppedGssNodes.getValue(gssNode).add(sppfNode)

        for ((label, target) in gssNode.edges) {
            for (node in target) {
                val descriptor = Descriptor(
                    label.first, node, sppf.getNodeP(label.first, label.second, sppfNode!!), pos
                )
                addDescriptor(descriptor)
            }
        }
    }
}
