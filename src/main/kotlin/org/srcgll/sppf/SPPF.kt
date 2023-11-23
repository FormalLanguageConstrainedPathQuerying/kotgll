//package org.srcgll.sppf
//
//import org.srcgll.rsm.RSMState
//import org.srcgll.rsm.symbol.Nonterminal
//import org.srcgll.rsm.symbol.Terminal
//import org.srcgll.sppf.node.*
//
//class SPPF <VertexType>
//{
//    private val createdSPPFNodes : HashMap<SPPFNode<VertexType>, SPPFNode<VertexType>> = HashMap()
//
//    fun getNodeP(state : RSMState, sppfNode : SPPFNode<VertexType>?, nextSPPFNode : SPPFNode<VertexType>) : SPPFNode<VertexType>
//    {
//        val leftExtent  = sppfNode?.leftExtent ?: nextSPPFNode.leftExtent
//        val rightExtent = nextSPPFNode.rightExtent
//
//        val packedNode = PackedSPPFNode(nextSPPFNode.leftExtent, state, sppfNode, nextSPPFNode)
//
//        val parent : ParentSPPFNode<VertexType> =
//            if (state.isFinal) getOrCreateSymbolSPPFNode(state.nonterminal, leftExtent, rightExtent, packedNode.weight)
//            else               getOrCreateItemSPPFNode(state, leftExtent, rightExtent, packedNode.weight)
//
//
//        //  Restrict SPPF from creating loops PARENT -> PACKED -> PARENT
//        if (sppfNode != null || parent != nextSPPFNode) {
//            sppfNode?.parents?.add(packedNode)
//            nextSPPFNode.parents.add(packedNode)
//            packedNode.parents.add(parent)
//
//            parent.kids.add(packedNode)
//        }
//
//        updateWeights(parent)
//
//        return parent
//    }
//
//    fun getOrCreateTerminalSPPFNode
//    (
//        terminal    : Terminal<*>?,
//        leftExtent  : VertexType,
//        rightExtent : VertexType,
//        weight      : Int
//    )
//        : SPPFNode<VertexType>
//    {
//        val node = TerminalSPPFNode(terminal, leftExtent, rightExtent, weight)
//
//        if (!createdSPPFNodes.containsKey(node)) {
//            createdSPPFNodes[node] = node
//        }
//
//        return createdSPPFNodes[node]!!
//    }
//
//    fun getOrCreateItemSPPFNode
//    (
//        state       : RSMState,
//        leftExtent  : VertexType,
//        rightExtent : VertexType,
//        weight      : Int
//    )
//        : ParentSPPFNode<VertexType>
//    {
//        val node = ItemSPPFNode(state, leftExtent, rightExtent)
//        node.weight = weight
//
//        if (!createdSPPFNodes.containsKey(node)) {
//            createdSPPFNodes[node] = node
//        }
//
//        return createdSPPFNodes[node]!! as ItemSPPFNode
//    }
//
//    fun getOrCreateSymbolSPPFNode
//    (
//        nonterminal : Nonterminal,
//        leftExtent  : VertexType,
//        rightExtent : VertexType,
//        weight      : Int
//    )
//        : SymbolSPPFNode<VertexType>
//    {
//        val node = SymbolSPPFNode(nonterminal, leftExtent, rightExtent)
//        node.weight = weight
//
//        if (!createdSPPFNodes.containsKey(node)) createdSPPFNodes[node] = node
//
//        return createdSPPFNodes[node]!! as SymbolSPPFNode
//    }
//
//    fun updateWeights(sppfNode : ISPPFNode)
//    {
//        val cycle = HashSet<ISPPFNode>()
//        val deque = ArrayDeque(listOf(sppfNode))
//        var curNode : ISPPFNode
//
//        while (deque.isNotEmpty()) {
//            curNode = deque.last()
//
//            when (curNode) {
//                is SymbolSPPFNode<*> -> {
//                    val oldWeight = curNode.weight
//                    var newWeight = Int.MAX_VALUE
//
//                    curNode.kids.forEach { newWeight = minOf(newWeight, it.weight) }
//
//                    if (oldWeight > newWeight) {
//                        curNode.weight = newWeight
//
//                        curNode.kids.forEach { if (it.weight > newWeight) it.parents.remove(curNode) }
//                        curNode.kids.removeIf { it.weight > newWeight }
//
//                        curNode.parents.forEach { deque.addLast(it) }
//                    }
//                }
//                is ItemSPPFNode<*> -> {
//                    if (!cycle.contains(curNode)) {
//                        cycle.add(curNode)
//
//                        val oldWeight = curNode.weight
//                        var newWeight = Int.MAX_VALUE
//
//                        curNode.kids.forEach { newWeight = minOf(newWeight, it.weight) }
//
//                        if (oldWeight > newWeight) {
//                            curNode.weight = newWeight
//
//                            curNode.kids.forEach { if (it.weight > newWeight) it.parents.remove(curNode) }
//                            curNode.kids.removeIf { it.weight > newWeight }
//
//                            curNode.parents.forEach { deque.addLast(it) }
//                        }
//                        if (deque.last() == curNode) {
//                            cycle.remove(curNode)
//                        }
//                    }
//                }
//                is PackedSPPFNode<*> -> {
//                    val oldWeight = curNode.weight
//                    val newWeight = (curNode.leftSPPFNode?.weight ?: 0) + (curNode.rightSPPFNode?.weight ?: 0)
//
//                    if (oldWeight > newWeight) {
//                        curNode.weight = newWeight
//
//                        curNode.parents.forEach { deque.addLast(it) }
//                    }
//                }
//                else -> {
//                    throw  Error("Terminal node can not be parent")
//                }
//            }
//
//            if (curNode == deque.last()) deque.removeLast()
//        }
//    }
//}
//
