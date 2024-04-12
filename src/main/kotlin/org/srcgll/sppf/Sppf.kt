package org.srcgll.sppf

import org.srcgll.descriptors.Descriptor
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.*

/**
 * Default sppf
 * @param VertexType - type of vertex in input graph
 */
open class Sppf<VertexType> {
    /**
     * Collection of created sppfNodes with access and search in O(1) time
     */
    private val createdSppfNodes: HashMap<SppfNode<VertexType>, SppfNode<VertexType>> = HashMap()

    /**
     * Part of incrementality mechanism.
     * Map vertex in input graph -> set of all created terminal nodes that have vertex as their leftExtent
     */
    private val createdTerminalNodes: HashMap<VertexType, HashSet<TerminalSppfNode<VertexType>>> = HashMap()

    /**
     * Used when rsmState in descriptor is both starting and final. In such case this function creates special
     * epsilon node which is used to produce correct derivation tree
     * @param descriptor - current parsing stage
     * @return created epsilonNode
     */
    fun getEpsilonSppfNode(descriptor: Descriptor<VertexType>): SppfNode<VertexType>? {
        val rsmState = descriptor.rsmState
        val sppfNode = descriptor.sppfNode
        val inputPosition = descriptor.inputPosition

        return if (rsmState.isStart && rsmState.isFinal) {
            // if nonterminal accepts epsilon
            getParentNode(
                rsmState,
                sppfNode,
                getOrCreateIntermediateSppfNode(rsmState, inputPosition, inputPosition, weight = 0)
            )
        } else {
            sppfNode
        }
    }

    /**
     * Part of incrementality mechanism.
     * Removes given node from collection of created. If the sppfNode is terminal, additionally removes it
     * from set in createdTerminalNodes, corresponding to it's leftExtent
     * @param sppfNode - sppfNode to remove
     */
    fun removeNode(sppfNode: SppfNode<VertexType>) {
        createdSppfNodes.remove(sppfNode)
        if (sppfNode is TerminalSppfNode<*>) {
            createdTerminalNodes.remove(sppfNode.leftExtent)
        }
    }

    /**
     * Receives two subtrees of SPPF and connects them via PackedNode.
     * If given subtrees repesent derivation tree for nonterminal and state is final, then retrieves or creates
     * Symbol sppfNode, otherwise retrieves or creates Intermediate sppfNode
     * @param rsmState - current rsmState
     * @param sppfNode - left subtree
     * @param nextSppfNode - right subtree
     * @return ParentNode, which has combined subtree as alternative derivation
     */
    open fun getParentNode(
        rsmState: RsmState,
        sppfNode: SppfNode<VertexType>?,
        nextSppfNode: SppfNode<VertexType>,
    ): SppfNode<VertexType> {
        val leftExtent = sppfNode?.leftExtent ?: nextSppfNode.leftExtent
        val rightExtent = nextSppfNode.rightExtent

        val packedNode = PackedSppfNode(nextSppfNode.leftExtent, rsmState, sppfNode, nextSppfNode)

        val parent: NonterminalSppfNode<VertexType> =
            if (rsmState.isFinal) getOrCreateSymbolSppfNode(rsmState.nonterminal, leftExtent, rightExtent, packedNode.weight)
            else getOrCreateIntermediateSppfNode(rsmState, leftExtent, rightExtent, packedNode.weight)

        if (sppfNode != null || parent != nextSppfNode) { // Restrict SPPF from creating loops PARENT -> PACKED -> PARENT
            sppfNode?.parents?.add(packedNode)
            nextSppfNode.parents.add(packedNode)
            packedNode.parents.add(parent)

            parent.children.add(packedNode)
        }

        return parent
    }

    /**
     * Creates or retrieves Terminal sppfNode with given parameters
     * @param terminal - terminal value
     * @param leftExtent - left limit subrange
     * @param rightExtent - right limit subrange
     * @param weight - weight of the node, default value is 0
     * @return Terminal sppfNode
     */
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

    /**
     * Creates of retrieves Intermediate sppfNode with given parameters
     * @param state - current rsmState
     * @param leftExtent - left limit of subrange
     * @param rightExtent - right limit of subrange
     * @param weight - weight of the node, default value is Int.MAX_VALUE
     */
    fun getOrCreateIntermediateSppfNode(
        state: RsmState,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int = Int.MAX_VALUE,
    ): NonterminalSppfNode<VertexType> {
        val node = IntermediateSppfNode(state, leftExtent, rightExtent)

        node.weight = weight

        if (!createdSppfNodes.containsKey(node)) {
            createdSppfNodes[node] = node
        }

        return createdSppfNodes[node]!! as IntermediateSppfNode
    }

    /**
     * Creates or retrieves Symbol sppfNode with given parameters
     * @param nonterminal - nonterminal
     * @param leftExtent - left limit of subrange
     * @param rightExtent - right limit of subrange
     * @param weight - weight of the node, default value is Int.MAX_VALUE
     */
    fun getOrCreateSymbolSppfNode(
        nonterminal: Nonterminal,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int = Int.MAX_VALUE,
    ): SymbolSppfNode<VertexType> {
        val node = SymbolSppfNode(nonterminal, leftExtent, rightExtent)

        node.weight = weight

        if (!createdSppfNodes.containsKey(node)) {
            createdSppfNodes[node] = node
        }

        return createdSppfNodes[node]!! as SymbolSppfNode
    }

    /**
     * Part of incrementality mechanism.
     * Traverses all the way up to the root from all Terminal sppfNodes, which have given vertex as their leftExtent,
     * deconstructing the derivation trees on the path.
     * parseResult is given to restrict algorithm from deleting parents of parseResult node
     * @param vertex - position in input graph
     * @param parseResult - current derivation tree
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
     * Part of reachability mechanism.
     * Calculates minimal distance between two vertices in input graph amongst all paths that are recognized by
     * accepting nonterminal of RSM (intersection of language, defined by RSM, and input graph).
     * @param root - root of the derivation tree
     * @return minimal distance in number of edges in the path
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