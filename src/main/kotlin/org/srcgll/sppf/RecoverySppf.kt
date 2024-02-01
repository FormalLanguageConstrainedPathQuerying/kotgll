package org.srcgll.sppf

import org.srcgll.rsm.RsmState
import org.srcgll.sppf.node.*

class RecoverySppf<VertexType> : Sppf<VertexType>() {


    override fun getParentNode(
        state: RsmState,
        sppfNode: SppfNode<VertexType>?,
        nextSppfNode: SppfNode<VertexType>,
    ): SppfNode<VertexType> {
        val parent = super.getParentNode(state, sppfNode, nextSppfNode)
        updateWeights(parent)
        return parent
    }

    /**
     * ??
     */
    fun updateWeights(sppfNode: ISppfNode) {
        val added = HashSet<ISppfNode>(listOf(sppfNode))
        val queue = ArrayDeque(listOf(sppfNode))
        var curSPPFNode: ISppfNode

        while (queue.isNotEmpty()) {
            curSPPFNode = queue.removeFirst()
            val oldWeight = curSPPFNode.weight

            when (curSPPFNode) {
                is NonterminalSppfNode<*> -> {
                    var newWeight = Int.MAX_VALUE

                    curSPPFNode.children.forEach { newWeight = minOf(newWeight, it.weight) }

                    if (oldWeight > newWeight) {
                        curSPPFNode.weight = newWeight

                        curSPPFNode.children.forEach { if (it.weight > newWeight) it.parents.remove(curSPPFNode) }
                        curSPPFNode.children.removeIf { it.weight > newWeight }

                        curSPPFNode.parents.forEach {
                            queue.addLast(it)
                            added.add(it)
                        }
                    }
                }

                is PackedSppfNode<*> -> {
                    val newWeight = (curSPPFNode.leftSppfNode?.weight ?: 0) + (curSPPFNode.rightSppfNode?.weight ?: 0)

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