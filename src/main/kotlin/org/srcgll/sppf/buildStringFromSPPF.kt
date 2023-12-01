package org.srcgll.sppf

import org.srcgll.sppf.node.ISPPFNode
import org.srcgll.sppf.node.PackedSPPFNode
import org.srcgll.sppf.node.ParentSPPFNode
import org.srcgll.sppf.node.TerminalSPPFNode

fun buildStringFromSPPF(sppfNode: ISPPFNode): MutableList<String> {
    val visited: HashSet<ISPPFNode> = HashSet()
    val stack: ArrayDeque<ISPPFNode> = ArrayDeque(listOf(sppfNode))
    val result: MutableList<String> = ArrayList()
    var curNode: ISPPFNode

    while (stack.isNotEmpty()) {
        curNode = stack.removeLast()
        visited.add(curNode)

        when (curNode) {
            is TerminalSPPFNode<*> -> {
                if (curNode.terminal != null) result.add(curNode.terminal!!.value.toString())
            }

            is PackedSPPFNode<*> -> {
                if (curNode.rightSPPFNode != null) stack.add(curNode.rightSPPFNode!!)
                if (curNode.leftSPPFNode != null) stack.add(curNode.leftSPPFNode!!)
            }

            is ParentSPPFNode<*> -> {
                if (curNode.kids.isNotEmpty()) {
                    curNode.kids.findLast {
                        it.rightSPPFNode != curNode && it.leftSPPFNode != curNode && !visited.contains(
                            it
                        )
                    }?.let { stack.add(it) }
                    curNode.kids.forEach { visited.add(it) }
                }
            }
        }

    }
    return result
}