package org.srcgll

import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.ErrorRecoveringDescriptorsStack
import org.srcgll.descriptors.IDescriptorsStack
import org.srcgll.gss.GSSNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.SPPF
import org.srcgll.sppf.TerminalRecoveryEdge
import org.srcgll.sppf.node.ISPPFNode
import org.srcgll.sppf.node.SPPFNode
import org.srcgll.sppf.node.SymbolSPPFNode

class GLL<VertexType, LabelType : ILabel>(
    private val startState: RSMState,
    private val input: IGraph<VertexType, LabelType>,
    private val recovery: RecoveryMode,
) {
    private val stack: IDescriptorsStack<VertexType> = ErrorRecoveringDescriptorsStack()
    private val sppf: SPPF<VertexType> = SPPF()
    private val poppedGSSNodes: HashMap<GSSNode<VertexType>, HashSet<SPPFNode<VertexType>?>> = HashMap()
    private val createdGSSNodes: HashMap<GSSNode<VertexType>, GSSNode<VertexType>> = HashMap()
    private var parseResult: SPPFNode<VertexType>? = null
    private val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()

    fun parse(): Pair<SPPFNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        for (startVertex in input.getInputStartVertices()) {
            val descriptor = Descriptor(
                startState,
                getOrCreateGSSNode(startState.nonterminal, startVertex, weight = 0),
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

    fun parse(vertex: VertexType): Pair<SPPFNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        stack.recoverDescriptors(vertex)
        sppf.invalidate(vertex, parseResult as ISPPFNode)

        parseResult = null

//        while (parseResult == null && !stack.defaultDescriptorsStackIsEmpty()) {
//            val curDefaultDescriptor = stack.next()
//
//            parse(curDefaultDescriptor)
//        }

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
        var curSPPFNode = curDescriptor.sppfNode
        var leftExtent = curSPPFNode?.leftExtent
        var rightExtent = curSPPFNode?.rightExtent
        val terminalEdges = state.getTerminalEdges()
        val nonTerminalEdges = state.getNonTerminalEdges()

        stack.addToHandled(curDescriptor)

        if (state.isStart && state.isFinal) {
            curSPPFNode = sppf.getNodeP(
                state, curSPPFNode, sppf.getOrCreateItemSPPFNode(
                    state, pos, pos, weight = 0
                )
            )
            leftExtent = curSPPFNode.leftExtent
            rightExtent = curSPPFNode.rightExtent
        }

        if (curSPPFNode is SymbolSPPFNode<VertexType> && state.nonterminal == startState.nonterminal && input.isStart(
                leftExtent!!
            ) && input.isFinal(rightExtent!!)
        ) {
            if (parseResult == null || parseResult!!.weight > curSPPFNode.weight) {
                parseResult = curSPPFNode
            }

            val pair = Pair(leftExtent, rightExtent)
            val distance = sppf.minDistance(curSPPFNode)

            if (reachabilityPairs.containsKey(pair)) {
                reachabilityPairs[pair] = minOf(distance, reachabilityPairs[pair]!!)
            } else {
                reachabilityPairs[pair] = distance
            }
        }

        for (inputEdge in input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                val descriptor = Descriptor(
                    state, gssNode, sppf.getNodeP(
                        state, curSPPFNode, sppf.getOrCreateTerminalSPPFNode(
                            terminal = null, pos, inputEdge.head, weight = 0
                        )
                    ), inputEdge.head
                )
                addDescriptor(descriptor)
                continue
            }
            for (kvp in terminalEdges) {
                if (inputEdge.label.terminal == kvp.key) {
                    for (target in kvp.value) {
                        val rsmEdge = RSMTerminalEdge(kvp.key, target)

                        val descriptor = Descriptor(
                            rsmEdge.head, gssNode, sppf.getNodeP(
                                rsmEdge.head, curSPPFNode, sppf.getOrCreateTerminalSPPFNode(
                                    rsmEdge.terminal, pos, inputEdge.head, weight = 0
                                )
                            ), inputEdge.head
                        )
                        addDescriptor(descriptor)
                    }
                }
            }
        }

        for (kvp in nonTerminalEdges) {
            for (target in kvp.value) {
                val rsmEdge = RSMNonterminalEdge(kvp.key, target)

                val descriptor = Descriptor(
                    rsmEdge.nonterminal.startState,
                    createGSSNode(rsmEdge.nonterminal, rsmEdge.head, gssNode, curSPPFNode, pos),
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

                    val coveredByCurrentTerminal: HashSet<RSMState> = terminalEdges[currentTerminal] ?: hashSetOf()

                    for (terminal in state.errorRecoveryLabels) {
                        val coveredByTerminal = HashSet(terminalEdges[terminal] as HashSet<RSMState>)

                        coveredByCurrentTerminal.forEach { coveredByTerminal.remove(it) }

                        if (terminal != currentTerminal && coveredByTerminal.isNotEmpty()) {
                            errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                        }
                    }
                    errorRecoveryEdges[null] = TerminalRecoveryEdge(currentEdge.head, weight = 1)
                }
            } else {
                for (terminal in state.errorRecoveryLabels) {
                    val coveredByTerminal = HashSet(terminalEdges[terminal] as HashSet<RSMState>)

                    if (coveredByTerminal.isNotEmpty()) {
                        errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                    }
                }
            }

            for (kvp in errorRecoveryEdges) {
                val errorRecoveryEdge = kvp.value
                val terminal = kvp.key

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

        if (state.isFinal) pop(gssNode, curSPPFNode, pos)
    }

    private fun handleTerminalOrEpsilonEdge(
        curDescriptor: Descriptor<VertexType>,
        terminal: Terminal<*>?,
        targetEdge: TerminalRecoveryEdge<VertexType>,
        targetState: RSMState,
    ) {
        val descriptor = Descriptor(
            targetState, curDescriptor.gssNode, sppf.getNodeP(
                targetState, curDescriptor.sppfNode, sppf.getOrCreateTerminalSPPFNode(
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

        if (parseResult == null && sppfNode is SymbolSPPFNode<*> && state.nonterminal == startState.nonterminal && input.isStart(
                leftExtent!!
            ) && input.isFinal(rightExtent!!)
        ) {
            stack.removeFromHandled(descriptor)
        }

        stack.add(descriptor)
    }

    private fun getOrCreateGSSNode(
        nonterminal: Nonterminal,
        inputPosition: VertexType,
        weight: Int,
    ): GSSNode<VertexType> {
        val gssNode = GSSNode(nonterminal, inputPosition, weight)

        if (createdGSSNodes.containsKey(gssNode)) {
            if (createdGSSNodes.getValue(gssNode).minWeightOfLeftPart > weight) {
                createdGSSNodes.getValue(gssNode).minWeightOfLeftPart = weight
            }
        } else createdGSSNodes[gssNode] = gssNode

        return createdGSSNodes.getValue(gssNode)
    }


    private fun createGSSNode(
        nonterminal: Nonterminal,
        state: RSMState,
        gssNode: GSSNode<VertexType>,
        sppfNode: SPPFNode<VertexType>?,
        pos: VertexType,
    ): GSSNode<VertexType> {
        val newNode =
            getOrCreateGSSNode(nonterminal, pos, weight = gssNode.minWeightOfLeftPart + (sppfNode?.weight ?: 0))

        if (newNode.addEdge(state, sppfNode, gssNode)) {
            if (poppedGSSNodes.containsKey(newNode)) {
                for (popped in poppedGSSNodes[newNode]!!) {
                    val descriptor = Descriptor(
                        state, gssNode, sppf.getNodeP(state, sppfNode, popped!!), popped.rightExtent
                    )
                    addDescriptor(descriptor)
                }
            }
        }

        return newNode
    }

    private fun pop(gssNode: GSSNode<VertexType>, sppfNode: SPPFNode<VertexType>?, pos: VertexType) {
        if (!poppedGSSNodes.containsKey(gssNode)) poppedGSSNodes[gssNode] = HashSet()
        poppedGSSNodes.getValue(gssNode).add(sppfNode)

        for (edge in gssNode.edges) {
            for (node in edge.value) {
                val descriptor = Descriptor(
                    edge.key.first, node, sppf.getNodeP(edge.key.first, edge.key.second, sppfNode!!), pos
                )
                addDescriptor(descriptor)
            }
        }
    }
}
