package org.srcgll.sppf

import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.*

class SPPF<VertexType> {
    private val createdSPPFNodes: HashMap<SPPFNode<VertexType>, SPPFNode<VertexType>> = HashMap()
    private val createdTerminalNodes: HashMap<VertexType, HashSet<TerminalSPPFNode<VertexType>>> = HashMap()

    fun removeNode(sppfNode: SPPFNode<VertexType>) {
        createdSPPFNodes.remove(sppfNode)
        if (sppfNode is TerminalSPPFNode<*>) {
            createdTerminalNodes.remove(sppfNode.leftExtent)
        }
    }

    fun getNodeP(
        state: RSMState,
        sppfNode: SPPFNode<VertexType>?,
        nextSPPFNode: SPPFNode<VertexType>,
    ): SPPFNode<VertexType> {
        val leftExtent = sppfNode?.leftExtent ?: nextSPPFNode.leftExtent
        val rightExtent = nextSPPFNode.rightExtent

        val packedNode = PackedSPPFNode(nextSPPFNode.leftExtent, state, sppfNode, nextSPPFNode)

        val parent: ParentSPPFNode<VertexType> =
            if (state.isFinal) getOrCreateSymbolSPPFNode(state.nonterminal, leftExtent, rightExtent, packedNode.weight)
            else getOrCreateItemSPPFNode(state, leftExtent, rightExtent, packedNode.weight)

        //  Restrict SPPF from creating loops PARENT -> PACKED -> PARENT
        if (sppfNode != null || parent != nextSPPFNode) {
            sppfNode?.parents?.add(packedNode)
            nextSPPFNode.parents.add(packedNode)
            packedNode.parents.add(parent)

            parent.kids.add(packedNode)
        }

        updateWeights(parent)

        return parent
    }

    fun getOrCreateTerminalSPPFNode(
        terminal: Terminal<*>?,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int,
    ): SPPFNode<VertexType> {
        val node = TerminalSPPFNode(terminal, leftExtent, rightExtent, weight)

        if (!createdSPPFNodes.containsKey(node)) {
            createdSPPFNodes[node] = node
        }
        if (!createdTerminalNodes.containsKey(leftExtent)) {
            createdTerminalNodes[leftExtent] = HashSet()
        }
        createdTerminalNodes[leftExtent]!!.add(createdSPPFNodes[node] as TerminalSPPFNode<VertexType>)

        return createdSPPFNodes[node]!!
    }

    fun getOrCreateItemSPPFNode(
        state: RSMState,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int,
    ): ParentSPPFNode<VertexType> {
        val node = ItemSPPFNode(state, leftExtent, rightExtent)
        node.weight = weight

        if (!createdSPPFNodes.containsKey(node)) {
            createdSPPFNodes[node] = node
        }

        return createdSPPFNodes[node]!! as ItemSPPFNode
    }

    fun getOrCreateSymbolSPPFNode(
        nonterminal: Nonterminal,
        leftExtent: VertexType,
        rightExtent: VertexType,
        weight: Int,
    ): SymbolSPPFNode<VertexType> {
        val node = SymbolSPPFNode(nonterminal, leftExtent, rightExtent)
        node.weight = weight

        if (!createdSPPFNodes.containsKey(node)) {
            createdSPPFNodes[node] = node
        }

        return createdSPPFNodes[node]!! as SymbolSPPFNode
    }

    fun invalidate(vertex: VertexType, parseResult: ISPPFNode) {
        val queue = ArrayDeque<ISPPFNode>()
        val added = HashSet<ISPPFNode>()
        var curSPPFNode: ISPPFNode? = parseResult

        createdTerminalNodes[vertex]!!.forEach { node ->
            queue.add(node)
            added.add(node)
        }

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.removeFirst()

            when (curSPPFNode) {
                is ParentSPPFNode<*> -> {
                    if (curSPPFNode.kids.isEmpty()) {
                        curSPPFNode.parents.forEach { packed ->
                            if (!added.contains(packed)) {
                                queue.addLast(packed)
                                added.add(packed)
                            }
                            (packed as PackedSPPFNode<VertexType>).rightSPPFNode = null
                            (packed as PackedSPPFNode<VertexType>).leftSPPFNode = null
                        }
                        removeNode(curSPPFNode as SPPFNode<VertexType>)
                    }
                }

                is PackedSPPFNode<*> -> {
                    curSPPFNode.parents.forEach { parent ->
                        if ((parent as ParentSPPFNode<*>).kids.contains(curSPPFNode)) {
                            if (!added.contains(parent)) {
                                queue.addLast(parent)
                                added.add(parent)
                            }
                            parent.kids.remove(curSPPFNode)
                        }
                    }
                }

                is TerminalSPPFNode<*> -> {
                    curSPPFNode.parents.forEach { packed ->
                        if (!added.contains(packed)) {
                            queue.addLast(packed)
                            added.add(packed)
                        }
                        (packed as PackedSPPFNode<VertexType>).rightSPPFNode = null
                        (packed as PackedSPPFNode<VertexType>).leftSPPFNode = null
                    }
                    removeNode(curSPPFNode as SPPFNode<VertexType>)
                }
            }

            if (curSPPFNode != parseResult) {
                curSPPFNode.parents.clear()
            }
        }
    }

    fun updateWeights(sppfNode: ISPPFNode) {
        val added = HashSet<ISPPFNode>(listOf(sppfNode))
        val queue = ArrayDeque(listOf(sppfNode))
        var curSPPFNode: ISPPFNode

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.removeFirst()

            when (curSPPFNode) {
                is ParentSPPFNode<*> -> {
                    val oldWeight = curSPPFNode.weight
                    var newWeight = Int.MAX_VALUE

                    curSPPFNode.kids.forEach { newWeight = minOf(newWeight, it.weight) }

                    if (oldWeight > newWeight) {
                        curSPPFNode.weight = newWeight

                        curSPPFNode.kids.forEach { if (it.weight > newWeight) it.parents.remove(curSPPFNode) }
                        curSPPFNode.kids.removeIf { it.weight > newWeight }

                        curSPPFNode.parents.forEach {
                            queue.addLast(it)
                            added.add(it)
                        }
                    }
                }

                is PackedSPPFNode<*> -> {
                    val oldWeight = curSPPFNode.weight
                    val newWeight = (curSPPFNode.leftSPPFNode?.weight ?: 0) + (curSPPFNode.rightSPPFNode?.weight ?: 0)

                    if (oldWeight > newWeight) {
                        curSPPFNode.weight = newWeight

                        curSPPFNode.parents.forEach {
                            queue.addLast(it)
                            added.add(it)
                        }
                    }
                }

                else -> {
                    throw Error("Terminal node can not be parent")
                }
            }
        }
    }
}