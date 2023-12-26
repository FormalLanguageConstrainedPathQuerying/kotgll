package org.srcgll.sppf

import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.PackedSppfNode
import org.srcgll.sppf.node.ParentSppfNode
import org.srcgll.sppf.node.TerminalSppfNode

fun buildStringFromSppf(sppfNode: ISppfNode): MutableList<String> {
    val visited: HashSet<ISppfNode> = HashSet()
    val stack: ArrayDeque<ISppfNode> = ArrayDeque(listOf(sppfNode))
    val result: MutableList<String> = ArrayList()
    var curNode: ISppfNode

    while (stack.isNotEmpty()) {
        curNode = stack.removeLast()
        visited.add(curNode)

        when (curNode) {
            is TerminalSppfNode<*> -> {
                if (curNode.terminal != null) result.add(curNode.terminal!!.value.toString())
            }

            is PackedSppfNode<*> -> {
                if (curNode.rightSppfNode != null) stack.add(curNode.rightSppfNode!!)
                if (curNode.leftSppfNode != null) stack.add(curNode.leftSppfNode!!)
            }

            is ParentSppfNode<*> -> {
                if (curNode.kids.isNotEmpty()) {
                    curNode.kids.findLast {
                        it.rightSppfNode != curNode && it.leftSppfNode != curNode && !visited.contains(
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