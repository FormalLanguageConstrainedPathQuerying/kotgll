package org.srcgll.sppf

import org.srcgll.rsm.RsmState
import org.srcgll.sppf.node.*

/**
 * Part of error recovery mechanism.
 * Sppf with additional support for updating weights, when necessary
 * @param VertexType - type of vertex in input graph
 */
class RecoverySppf<VertexType> : Sppf<VertexType>() {
    /**
     * Part of error recovery mechanism.
     * Receives two subtrees of SPPF and connects them via PackedNode. Additionally, for newly created/retrieved
     * parent sppfNode traverses upwards, updating weights on the path, when necessary
     * @param rsmState - current rsmState
     * @param sppfNode - left subtree
     * @param nextSppfNode - right subtree
     * @return ParentNode, which has combined subtree as alternative derivation
     */
    override fun getParentNode(
        rsmState: RsmState,
        sppfNode: SppfNode<VertexType>?,
        nextSppfNode: SppfNode<VertexType>,
    ): SppfNode<VertexType> {
        val parent = super.getParentNode(rsmState, sppfNode, nextSppfNode)
        updateWeights(parent)
        return parent
    }

    /**
     * Part of error recovery mechanism.
     * Traverses from given node all the way up to the root, updating weights on the path when necessary
     * @param sppfNode - given sppfNode to start traverse from
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