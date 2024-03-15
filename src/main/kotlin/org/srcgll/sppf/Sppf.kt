package org.srcgll.sppf

import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.*

/**
 * Incremental by default
 */
open class Sppf<VertexType> {
    private val createdSppfNodes: HashMap<SppfNode<VertexType>, SppfNode<VertexType>> = HashMap()
    private val createdTerminalNodes: HashMap<VertexType, HashSet<TerminalSppfNode<VertexType>>> = HashMap()
<<<<<<< HEAD
=======
    private val minDistanceRecognisedBySymbol: HashMap<SymbolSppfNode<VertexType>, Int> = HashMap()

    // TODO: Resolve problem with HeapOverflow on large inputs
    fun minDistance(root: ISppfNode): Int {
        val cycle = HashSet<ISppfNode>()
        val visited = HashSet<ISppfNode>()
        val stack = ArrayDeque(listOf(root))
        var curSPPFNode: ISppfNode
        var minDistance = 0

        while (stack.isNotEmpty()) {
            curSPPFNode = stack.last()
            visited.add(curSPPFNode)

            if (!cycle.contains(curSPPFNode)) {
                cycle.add(curSPPFNode)

                when (curSPPFNode) {
                    is TerminalSppfNode<*> -> {
                        minDistance++
                    }

                    is PackedSppfNode<*> -> {
                        if (curSPPFNode.rightSppfNode != null) stack.add(curSPPFNode.rightSppfNode!!)
                        if (curSPPFNode.leftSppfNode != null) stack.add(curSPPFNode.leftSppfNode!!)
                    }

                    is ItemSppfNode<*> -> {
                        if (curSPPFNode.kids.isNotEmpty()) {
                            curSPPFNode.kids.findLast {
                                it.rightSppfNode != curSPPFNode && it.leftSppfNode != curSPPFNode && !visited.contains(
                                    it
                                )
                            }?.let { stack.add(it) }
                            curSPPFNode.kids.forEach { visited.add(it) }
                        }
                    }

                    is SymbolSppfNode<*> -> {
                        if (minDistanceRecognisedBySymbol.containsKey(curSPPFNode)) {
                            minDistance += minDistanceRecognisedBySymbol[curSPPFNode]!!
                        } else {
                            if (curSPPFNode.kids.isNotEmpty()) {
                                curSPPFNode.kids.findLast {
                                    it.rightSppfNode != curSPPFNode && it.leftSppfNode != curSPPFNode && !visited.contains(
                                        it
                                    )
                                }?.let { stack.add(it) }
                                curSPPFNode.kids.forEach { visited.add(it) }
                            }
                        }
                    }
                }
            }
            if (curSPPFNode == stack.last()) {
                stack.removeLast()
                cycle.remove(curSPPFNode)
            }
        }

        minDistanceRecognisedBySymbol[root as SymbolSppfNode<VertexType>] = minDistance

        return minDistance
    }
>>>>>>> work

    fun removeNode(sppfNode: SppfNode<VertexType>) {
        createdSppfNodes.remove(sppfNode)
        if (sppfNode is TerminalSppfNode<*>) {
            createdTerminalNodes.remove(sppfNode.leftExtent)
        }
    }

    /**
     * Receives two subtrees of SPPF and connects them via PackedNode.
     *
     * Get or create ParentNode. If given subtrees parse Nonterminal and state is final, then getOrCreate SymbolNode.
     * Otherwise, getOrCreate IntermediateNode.
     *
     * PackedNode then attached to ParentNode as a child
     *
     * @return ParentNode
     */
    open fun getParentNode(
        state: RsmState,
        sppfNode: SppfNode<VertexType>?,
        nextSppfNode: SppfNode<VertexType>,
    ): SppfNode<VertexType> {
        val leftExtent = sppfNode?.leftExtent ?: nextSppfNode.leftExtent
        val rightExtent = nextSppfNode.rightExtent

        val packedNode = PackedSppfNode(nextSppfNode.leftExtent, state, sppfNode, nextSppfNode)

        val parent: NonterminalSppfNode<VertexType> =
            if (state.isFinal) getOrCreateSymbolSppfNode(state.nonterminal, leftExtent, rightExtent, packedNode.weight)
            else getOrCreateIntermediateSppfNode(state, leftExtent, rightExtent, packedNode.weight)

        //  Restrict SPPF from creating loops PARENT -> PACKED -> PARENT
        if (sppfNode != null || parent != nextSppfNode) {
            sppfNode?.parents?.add(packedNode)
            nextSppfNode.parents.add(packedNode)
            packedNode.parents.add(parent)

            parent.children.add(packedNode)
        }

        return parent
    }

    fun getOrCreateTerminalSppfNode(
        terminal: Terminal<*>?,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int = 0,
    ): SppfNode<VertexType> {
        val node = TerminalSppfNode(terminal, leftExtent, rightExtent, weight)

        if (!createdSppfNodes.containsKey(node)) {
            createdSppfNodes[node] = node
        }
        if (!createdTerminalNodes.containsKey(leftExtent)) {
            createdTerminalNodes[leftExtent] = HashSet()
        }
        createdTerminalNodes[leftExtent]!!.add(createdSppfNodes[node] as TerminalSppfNode<VertexType>)

        return createdSppfNodes[node]!!
    }

    fun getOrCreateIntermediateSppfNode(
        state: RsmState,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int = Int.MAX_VALUE,
    ): NonterminalSppfNode<VertexType> {
        val node = IntermediateSppfNode(state, leftExtent, rightExtent)
        //needed to set 0 when intermediateNode accept epsilon
        //in other case Int.MAX_VALUE like all other parentNodes
        node.weight = weight

        if (!createdSppfNodes.containsKey(node)) {
            createdSppfNodes[node] = node
        }

        return createdSppfNodes[node]!! as IntermediateSppfNode
    }

    fun getOrCreateSymbolSppfNode(
        nonterminal: Nonterminal,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int = Int.MAX_VALUE,
    ): SymbolSppfNode<VertexType> {
        val node = SymbolSppfNode(nonterminal, leftExtent, rightExtent)
        //??
        node.weight = weight

        if (!createdSppfNodes.containsKey(node)) {
            createdSppfNodes[node] = node
        }

        return createdSppfNodes[node]!! as SymbolSppfNode
    }

    /**
     * ??
     */
    fun invalidate(vertex: VertexType, parseResult: ISppfNode) {
        val queue = ArrayDeque<ISppfNode>()
        val added = HashSet<ISppfNode>()
        var curSPPFNode: ISppfNode?

        createdTerminalNodes[vertex]!!.forEach { node ->
            queue.add(node)
            added.add(node)
        }

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.removeFirst()

            when (curSPPFNode) {
                is NonterminalSppfNode<*> -> {
                    if (curSPPFNode.children.isEmpty()) {
                        curSPPFNode.parents.forEach { packed ->
                            if (!added.contains(packed)) {
                                queue.addLast(packed)
                                added.add(packed)
                            }
                            if (packed is PackedSppfNode<*>) {
                                (packed as PackedSppfNode<VertexType>).rightSppfNode = null
                                (packed as PackedSppfNode<VertexType>).leftSppfNode = null
                            }
                        }
                        removeNode(curSPPFNode as SppfNode<VertexType>)
                    }
                }

                is PackedSppfNode<*> -> {
                    curSPPFNode.parents.forEach { parent ->
                        if ((parent as NonterminalSppfNode<*>).children.contains(curSPPFNode)) {
                            if (!added.contains(parent)) {
                                queue.addLast(parent)
                                added.add(parent)
                            }
                            parent.children.remove(curSPPFNode)
                        }
                    }
                }

                is TerminalSppfNode<*> -> {
                    curSPPFNode.parents.forEach { packed ->
                        if (!added.contains(packed)) {
                            queue.addLast(packed)
                            added.add(packed)
                        }
                        (packed as PackedSppfNode<VertexType>).rightSppfNode = null
                        (packed as PackedSppfNode<VertexType>).leftSppfNode = null
                    }
                    removeNode(curSPPFNode as SppfNode<VertexType>)
                }
            }

            if (curSPPFNode != parseResult) {
                curSPPFNode.parents.clear()
            }
        }
    }


    /**
     * Uses for reachability problem
     */
    fun minDistance(root: ISppfNode): Int {
        val minDistanceRecognisedBySymbol: HashMap<SymbolSppfNode<VertexType>, Int> = HashMap()

        val cycle = HashSet<ISppfNode>()
        val visited = HashSet<ISppfNode>()
        val stack = ArrayDeque(listOf(root))
        var curSPPFNode: ISppfNode
        var minDistance = 0

        while (stack.isNotEmpty()) {
            curSPPFNode = stack.last()
            visited.add(curSPPFNode)

            if (!cycle.contains(curSPPFNode)) {
                cycle.add(curSPPFNode)

                when (curSPPFNode) {
                    is TerminalSppfNode<*> -> {
                        minDistance++
                    }

                    is PackedSppfNode<*> -> {
                        if (curSPPFNode.rightSppfNode != null) stack.add(curSPPFNode.rightSppfNode!!)
                        if (curSPPFNode.leftSppfNode != null) stack.add(curSPPFNode.leftSppfNode!!)
                    }

                    is IntermediateSppfNode<*> -> {
                        if (curSPPFNode.children.isNotEmpty()) {
                            curSPPFNode.children.findLast {
                                it.rightSppfNode != curSPPFNode && it.leftSppfNode != curSPPFNode && !visited.contains(
                                    it
                                )
                            }?.let { stack.add(it) }
                            curSPPFNode.children.forEach { visited.add(it) }
                        }
                    }

                    is SymbolSppfNode<*> -> {
                        if (minDistanceRecognisedBySymbol.containsKey(curSPPFNode)) {
                            minDistance += minDistanceRecognisedBySymbol[curSPPFNode]!!
                        } else {
                            if (curSPPFNode.children.isNotEmpty()) {
                                curSPPFNode.children.findLast {
                                    it.rightSppfNode != curSPPFNode && it.leftSppfNode != curSPPFNode && !visited.contains(
                                        it
                                    )
                                }?.let { stack.add(it) }
                                curSPPFNode.children.forEach { visited.add(it) }
                            }
                        }
                    }
                }
            }
            if (curSPPFNode == stack.last()) {
                stack.removeLast()
                cycle.remove(curSPPFNode)
            }
        }

        minDistanceRecognisedBySymbol[root as SymbolSppfNode<VertexType>] = minDistance

        return minDistance
    }
}