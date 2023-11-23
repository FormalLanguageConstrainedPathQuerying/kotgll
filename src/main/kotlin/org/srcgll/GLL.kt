package org.srcgll

import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.descriptors.*
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.gss.*
import org.srcgll.input.ILabel
import org.srcgll.input.IGraph
import org.srcgll.sppf.node.*
import org.srcgll.sppf.*
import java.util.IntSummaryStatistics

class GLL <VertexType, LabelType : ILabel>
(
    private val startState : RSMState,
    private val input      : IGraph<VertexType, LabelType>,
    private val recovery   : RecoveryMode,
)
{
    private val stack             : IDescriptorsStack<VertexType> = ErrorRecoveringDescriptorsStack()
    private val createdSPPFNodes  : HashMap<SPPFNode<VertexType>, SPPFNode<VertexType>> = HashMap()
//    private val sppf              : SPPF<VertexType> = SPPF()
    private val poppedGSSNodes    : HashMap<GSSNode<VertexType>, HashSet<SPPFNode<VertexType>?>> = HashMap()
    private val createdGSSNodes   : HashMap<GSSNode<VertexType>, GSSNode<VertexType>> = HashMap()
    private var parseResult       : SPPFNode<VertexType>? = null
    private val reachableVertices : HashSet<VertexType> = HashSet()

    fun addDescriptor(descriptor : Descriptor<VertexType>)
    {
        val sppfNode    = descriptor.sppfNode
        val state       = descriptor.rsmState
        val leftExtent  = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

        if (sppfNode is SymbolSPPFNode<*> &&
            state.nonterminal == startState.nonterminal && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)) {
            stack.removeFromHandled(descriptor)
        }

        stack.add(descriptor)
    }

    fun parse() : Pair<SPPFNode<VertexType>?, HashSet<VertexType>>
    {
        for (startVertex in input.getInputStartVertices()) {
            val descriptor =
                    Descriptor(
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

        return Pair(parseResult, reachableVertices)
    }

    fun parse(vertex : VertexType) : Pair<SPPFNode<VertexType>?, HashSet<VertexType>>
    {
        stack.recoverDescriptors(vertex)

        val queue = ArrayDeque<ISPPFNode>()
        val cycle = HashSet<ISPPFNode>()
        val added = HashSet<ISPPFNode>()
        var curSPPFNode : ISPPFNode? = parseResult as ISPPFNode

        queue.add(curSPPFNode!!)
        added.add(curSPPFNode!!)

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.last()

            when (curSPPFNode) {
                is SymbolSPPFNode<*> -> {
                    curSPPFNode.kids.forEach { kid ->
                        if (!added.contains(kid)) {
                            queue.addLast(kid)
                            added.add(kid)
                        }
                    }
                }
                is ItemSPPFNode<*> -> {
                    if (!cycle.contains(curSPPFNode)) {
                        cycle.add(curSPPFNode)

                        curSPPFNode.kids.forEach { kid ->
                            if (!added.contains(kid)) {
                                queue.addLast(kid)
                                added.add(kid)
                            }
                        }

                        if (queue.last() == curSPPFNode) {
                            cycle.remove(curSPPFNode)
                        }
                    }
                }
                is PackedSPPFNode<*> -> {
                    if (curSPPFNode.rightSPPFNode != null) {
                        if (!added.contains(curSPPFNode.rightSPPFNode!!)) {
                            queue.addLast(curSPPFNode.rightSPPFNode!!)
                            added.add(curSPPFNode.rightSPPFNode!!)
                        }
                    }
                    if (curSPPFNode.leftSPPFNode != null) {
                        if (!added.contains(curSPPFNode.leftSPPFNode!!)) {
                            queue.addLast(curSPPFNode.leftSPPFNode!!)
                            added.add(curSPPFNode.leftSPPFNode!!)
                        }
                    }
                }
                is TerminalSPPFNode<*> -> {
                    if (curSPPFNode.leftExtent == vertex) {
                        break
                    }
                }
            }

            if (curSPPFNode == queue.last()) queue.removeLast()
        }

        queue.clear()
        cycle.clear()

        if (curSPPFNode != null && curSPPFNode is TerminalSPPFNode<*>) {
            curSPPFNode.parents.forEach { packed ->
                queue.addLast(packed)
            }
            curSPPFNode.parents.clear()
        }

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.last()

            when (curSPPFNode) {
                is SymbolSPPFNode<*> -> {
                    if (curSPPFNode.kids.isEmpty()) {
                        curSPPFNode.parents.forEach { parent ->
                            queue.addLast(parent)
                        }
                        curSPPFNode.parents.clear()
                    }
                }
                is ItemSPPFNode<*> -> {
                    if (!cycle.contains(curSPPFNode)) {
                        cycle.add(curSPPFNode)

                        if (curSPPFNode.kids.isEmpty()) {
                            curSPPFNode.parents.forEach { parent ->
                                queue.addLast(parent)
                            }
                            curSPPFNode.parents.clear()
                        }

                        if (queue.last() == curSPPFNode) {
                            cycle.remove(curSPPFNode)
                        }
                    }
                }
                is PackedSPPFNode<*> -> {
                    curSPPFNode.parents.forEach { parent ->
                        if ((parent as ParentSPPFNode<*>).kids.contains(curSPPFNode)) {
                            queue.addLast(parent)
                            parent.kids.remove(curSPPFNode)
                        }
                    }
                }
                else -> {
                    throw  Error("Terminal node can not be parent")
                }
            }

            if (curSPPFNode == queue.last()) queue.removeLast()
        }

        parseResult = null

        while (parseResult == null && !stack.defaultDescriptorsStackIsEmpty()) {
            val curDefaultDescriptor = stack.next()

            parse(curDefaultDescriptor)
        }

        while (parseResult == null && recovery == RecoveryMode.ON) {
            val curRecoveryDescriptor = stack.next()

            parse(curRecoveryDescriptor)
        }

        return Pair(parseResult, reachableVertices)
    }

    private fun parse(curDescriptor : Descriptor<VertexType>)
    {
        val state       = curDescriptor.rsmState
        val pos         = curDescriptor.inputPosition
        val gssNode     = curDescriptor.gssNode
        var curSPPFNode = curDescriptor.sppfNode
        var leftExtent  = curSPPFNode?.leftExtent
        var rightExtent = curSPPFNode?.rightExtent

        stack.addToHandled(curDescriptor)

        if (state.isStart && state.isFinal) {
            curSPPFNode = getNodeP(
                              state,
                              curSPPFNode,
                              getOrCreateItemSPPFNode(
                                  state,
                                  pos,
                                  pos,
                                  weight = 0
                                  )
                              )
            leftExtent = curSPPFNode.leftExtent
            rightExtent = curSPPFNode.rightExtent
        }

        if (curSPPFNode is SymbolSPPFNode<VertexType>
            && state.nonterminal == startState.nonterminal && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)) {

            if (parseResult == null || parseResult!!.weight > curSPPFNode.weight) {
                parseResult = curSPPFNode
            }
            reachableVertices.add(rightExtent)
        }

        for (inputEdge in input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                val descriptor =
                        Descriptor(
                            state,
                            gssNode,
                            getNodeP(
                                state,
                                curSPPFNode,
                                getOrCreateTerminalSPPFNode(
                                    terminal = null,
                                    pos,
                                    inputEdge.head,
                                    weight = 0
                                )
                            ),
                            inputEdge.head
                        )
                addDescriptor(descriptor)
                continue
            }
            for (kvp in state.outgoingTerminalEdges) {
                if (inputEdge.label.terminal == kvp.key) {
                    for (target in kvp.value) {
                        val rsmEdge = RSMTerminalEdge(kvp.key, target)

                        val descriptor =
                                Descriptor(
                                    rsmEdge.head,
                                    gssNode,
                                    getNodeP(
                                        rsmEdge.head,
                                        curSPPFNode,
                                        getOrCreateTerminalSPPFNode(
                                            rsmEdge.terminal,
                                            pos,
                                            inputEdge.head,
                                            weight = 0
                                        )
                                    ),
                                    inputEdge.head
                                )
                        addDescriptor(descriptor)
                    }
                }
            }
        }

        for (kvp in state.outgoingNonterminalEdges) {
            for (target in kvp.value) {
                val rsmEdge = RSMNonterminalEdge(kvp.key, target)

                val descriptor =
                        Descriptor(
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
            val currentEdges       = input.getEdges(pos)

            if (currentEdges.isNotEmpty()) {
                for (currentEdge in currentEdges) {
                    if (currentEdge.label.terminal == null) continue

                    val currentTerminal = currentEdge.label.terminal!!

                    val coveredByCurrentTerminal : HashSet<RSMState> =
                        if (state.outgoingTerminalEdges.containsKey(currentTerminal)) {
                            state.outgoingTerminalEdges.getValue(currentTerminal)
                        } else {
                            HashSet()
                        }
                    for (terminal in state.errorRecoveryLabels) {
                        val coveredByTerminal = HashSet(state.outgoingTerminalEdges[terminal] as HashSet<RSMState>)

                        coveredByCurrentTerminal.forEach { coveredByTerminal.remove(it) }

                        if (terminal != currentTerminal && coveredByTerminal.isNotEmpty()) {
                            errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                        }
                    }
                    errorRecoveryEdges[null] = TerminalRecoveryEdge(currentEdge.head, weight = 1)
                }
            } else {
                for (terminal in state.errorRecoveryLabels) {
                    val coveredByTerminal = HashSet(state.outgoingTerminalEdges[terminal] as HashSet<RSMState>)

                    if (coveredByTerminal.isNotEmpty()) {
                        errorRecoveryEdges[terminal] = TerminalRecoveryEdge(pos, weight = 1)
                    }
                }
            }

            for (kvp in errorRecoveryEdges) {
                val errorRecoveryEdge = kvp.value
                val terminal          = kvp.key

                if (terminal == null) {
                    handleTerminalOrEpsilonEdge(curDescriptor, terminal = null, errorRecoveryEdge, curDescriptor.rsmState)
                } else {
                    if (state.outgoingTerminalEdges.containsKey(terminal)) {
                        for (targetState in state.outgoingTerminalEdges.getValue(terminal)) {
                            handleTerminalOrEpsilonEdge(curDescriptor, terminal,  errorRecoveryEdge, targetState)
                        }
                    }
                }
            }
        }

        if (state.isFinal) pop(gssNode, curSPPFNode, pos)
    }

    private fun handleTerminalOrEpsilonEdge
    (
        curDescriptor : Descriptor<VertexType>,
        terminal      : Terminal<*>?,
        targetEdge    : TerminalRecoveryEdge<VertexType>,
        targetState   : RSMState
    )
    {
        val descriptor =
                Descriptor(
                    targetState,
                    curDescriptor.gssNode,
                    getNodeP(
                        targetState,
                        curDescriptor.sppfNode,
                        getOrCreateTerminalSPPFNode(
                            terminal,
                            curDescriptor.inputPosition,
                            targetEdge.head,
                            targetEdge.weight
                        )
                    ),
                    targetEdge.head
                )
        addDescriptor(descriptor)
    }

    private fun getOrCreateGSSNode(nonterminal : Nonterminal, inputPosition : VertexType, weight : Int)
        : GSSNode<VertexType>
    {
        val gssNode = GSSNode(nonterminal, inputPosition, weight)

        if (createdGSSNodes.containsKey(gssNode)) {
            if (createdGSSNodes.getValue(gssNode).minWeightOfLeftPart > weight)
                createdGSSNodes.getValue(gssNode).minWeightOfLeftPart = weight
        } else
            createdGSSNodes[gssNode] = gssNode

        return createdGSSNodes.getValue(gssNode)
    }


    private fun createGSSNode
    (
        nonterminal  : Nonterminal,
        state        : RSMState,
        gssNode      : GSSNode<VertexType>,
        sppfNode     : SPPFNode<VertexType>?,
        pos          : VertexType,
    )
        : GSSNode<VertexType>
    {
        val newNode= getOrCreateGSSNode(nonterminal, pos, weight = gssNode.minWeightOfLeftPart + (sppfNode?.weight ?: 0))

        if (newNode.addEdge(state, sppfNode, gssNode)) {
            if (poppedGSSNodes.containsKey(newNode)) {
                for (popped in poppedGSSNodes[newNode]!!) {
                    val descriptor =
                            Descriptor(
                                state,
                                gssNode,
                                getNodeP(state, sppfNode, popped!!),
                                popped.rightExtent
                            )
                    addDescriptor(descriptor)
                }
            }
        }

        return newNode
    }

    private fun pop(gssNode : GSSNode<VertexType>, sppfNode : SPPFNode<VertexType>?, pos : VertexType)
    {
        if (!poppedGSSNodes.containsKey(gssNode)) poppedGSSNodes[gssNode] = HashSet()

        poppedGSSNodes.getValue(gssNode).add(sppfNode)

        for (edge in gssNode.edges) {
            for (node in edge.value) {
                val descriptor =
                        Descriptor(
                            edge.key.first,
                            node,
                            getNodeP(edge.key.first, edge.key.second, sppfNode!!),
                            pos
                        )
                addDescriptor(descriptor)
            }
        }
    }

    fun getNodeP(state : RSMState, sppfNode : SPPFNode<VertexType>?, nextSPPFNode : SPPFNode<VertexType>) : SPPFNode<VertexType>
    {
        val leftExtent  = sppfNode?.leftExtent ?: nextSPPFNode.leftExtent
        val rightExtent = nextSPPFNode.rightExtent

        val packedNode = PackedSPPFNode(nextSPPFNode.leftExtent, state, sppfNode, nextSPPFNode)

        val parent : ParentSPPFNode<VertexType> =
            if (state.isFinal) getOrCreateSymbolSPPFNode(state.nonterminal, leftExtent, rightExtent, packedNode.weight)
            else               getOrCreateItemSPPFNode(state, leftExtent, rightExtent, packedNode.weight)


        //  Restrict SPPF from creating loops PARENT -> PACKED -> PARENT
        if (sppfNode != null || parent != nextSPPFNode) {
            sppfNode?.parents?.add(packedNode)
            nextSPPFNode.parents.add(packedNode)
            packedNode.parents.add(parent)

            parent.kids.add(packedNode)
        }

        updateWeights(parent)

//        if (parent is SymbolSPPFNode<*> &&
//            state.nonterminal == startState.nonterminal && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)) {
//            if ((parseResult == null || parseResult!!.weight > parent!!.weight)) {
//                parseResult = parent
//            }
//            reachableVertices.add(rightExtent)
//        }

        return parent
    }

    fun getOrCreateTerminalSPPFNode
    (
        terminal    : Terminal<*>?,
        leftExtent  : VertexType,
        rightExtent : VertexType,
        weight      : Int
    )
        : SPPFNode<VertexType>
    {
        val node = TerminalSPPFNode(terminal, leftExtent, rightExtent, weight)

        if (!createdSPPFNodes.containsKey(node)) {
            createdSPPFNodes[node] = node
        }

        return createdSPPFNodes[node]!!
    }

    fun getOrCreateItemSPPFNode
    (
        state       : RSMState,
        leftExtent  : VertexType,
        rightExtent : VertexType,
        weight      : Int
    )
        : ParentSPPFNode<VertexType>
    {
        val node = ItemSPPFNode(state, leftExtent, rightExtent)
        node.weight = weight

        if (!createdSPPFNodes.containsKey(node)) {
            createdSPPFNodes[node] = node
        }

        return createdSPPFNodes[node]!! as ItemSPPFNode
    }

    fun getOrCreateSymbolSPPFNode
    (
        nonterminal : Nonterminal,
        leftExtent  : VertexType,
        rightExtent : VertexType,
        weight      : Int
    )
        : SymbolSPPFNode<VertexType>
    {
        val node = SymbolSPPFNode(nonterminal, leftExtent, rightExtent)
        node.weight = weight

        if (!createdSPPFNodes.containsKey(node)) createdSPPFNodes[node] = node

//        if (nonterminal == startState.nonterminal && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)) {
//            if (parseResult == null || parseResult!!.weight > createdSPPFNodes[node]!!.weight) {
//                parseResult = createdSPPFNodes[node]
//            }
//            reachableVertices.add(rightExtent)
//        }

        return createdSPPFNodes[node]!! as SymbolSPPFNode
    }

    fun updateWeights(sppfNode : ISPPFNode)
    {
        val cycle = HashSet<ISPPFNode>()
        val deque = ArrayDeque(listOf(sppfNode))
        var curNode : ISPPFNode

        while (deque.isNotEmpty()) {
            curNode = deque.last()

            when (curNode) {
                is SymbolSPPFNode<*> -> {
                    val oldWeight = curNode.weight
                    var newWeight = Int.MAX_VALUE

                    curNode.kids.forEach { newWeight = minOf(newWeight, it.weight) }

                    if (oldWeight > newWeight) {
                        curNode.weight = newWeight

                        curNode.kids.forEach { if (it.weight > newWeight) it.parents.remove(curNode) }
                        curNode.kids.removeIf { it.weight > newWeight }

                        curNode.parents.forEach { deque.addLast(it) }
                    }
                }
                is ItemSPPFNode<*> -> {
                    if (!cycle.contains(curNode)) {
                        cycle.add(curNode)

                        val oldWeight = curNode.weight
                        var newWeight = Int.MAX_VALUE

                        curNode.kids.forEach { newWeight = minOf(newWeight, it.weight) }

                        if (oldWeight > newWeight) {
                            curNode.weight = newWeight

                            curNode.kids.forEach { if (it.weight > newWeight) it.parents.remove(curNode) }
                            curNode.kids.removeIf { it.weight > newWeight }

                            curNode.parents.forEach { deque.addLast(it) }
                        }
                        if (deque.last() == curNode) {
                            cycle.remove(curNode)
                        }
                    }
                }
                is PackedSPPFNode<*> -> {
                    val oldWeight = curNode.weight
                    val newWeight = (curNode.leftSPPFNode?.weight ?: 0) + (curNode.rightSPPFNode?.weight ?: 0)

                    if (oldWeight > newWeight) {
                        curNode.weight = newWeight

                        curNode.parents.forEach { deque.addLast(it) }
                    }
                }
                else -> {
                    throw  Error("Terminal node can not be parent")
                }
            }

            if (curNode == deque.last()) deque.removeLast()
        }
    }
}
