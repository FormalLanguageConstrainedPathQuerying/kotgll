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
import org.srcgll.sppf.node.*


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
            curSPPFNode = sppf.getNodeP(state, curSPPFNode, sppf.getOrCreateItemSPPFNode(state, pos, pos, weight = 0))
            leftExtent = curSPPFNode.leftExtent
            rightExtent = curSPPFNode.rightExtent
        }

        if (curSPPFNode is SymbolSPPFNode<VertexType> && state.nonterminal == startState.nonterminal
            && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)
        ) {
            if (parseResult == null || parseResult!!.weight > curSPPFNode.weight) {
                parseResult = curSPPFNode
            }

            val pair = Pair(leftExtent, rightExtent)
            val distance = sppf.minDistance(curSPPFNode)

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
                        state, curSPPFNode, sppf.getOrCreateTerminalSPPFNode(
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
                        val rsmEdge = RSMTerminalEdge(edgeTerminal, target)

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

        for ((edgeNonterminal, targetStates) in nonTerminalEdges) {
            for (target in targetStates) {
                val rsmEdge = RSMNonterminalEdge(edgeNonterminal, target)

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
                        coveredByCurrentTerminal.forEach { terminalEdges[terminal]?.remove(it) }

                        if (terminal != currentTerminal && !terminalEdges[terminal].isNullOrEmpty()) {
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

        if (parseResult == null && sppfNode is SymbolSPPFNode<*> && state.nonterminal == startState.nonterminal
            && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)
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
