package org.ucfs.sppf

import org.ucfs.sppf.node.ISppfNode
import org.ucfs.sppf.node.PackedSppfNode
import org.ucfs.sppf.node.NonterminalSppfNode
import org.ucfs.sppf.node.TerminalSppfNode

/**
 * Collects leaves of the derivation tree in order from left to right.
 * @return Ordered collection of terminals
 */
fun buildTokenStreamFromSppf(sppfNode: ISppfNode): MutableList<String> {
    val visited: HashSet<ISppfNode> = HashSet()
    val stack: ArrayDeque<ISppfNode> = ArrayDeque(listOf(sppfNode))
    val result: MutableList<String> = ArrayList()
    var curNode: ISppfNode

    while (stack.isNotEmpty()) {
        curNode = stack.removeLast()
        visited.add(curNode)

        when (curNode) {
            is TerminalSppfNode<*> -> {
                if (curNode.terminal != null) result.add(curNode.terminal!!.toString())
            }

            is PackedSppfNode<*> -> {
                if (curNode.rightSppfNode != null) stack.add(curNode.rightSppfNode!!)
                if (curNode.leftSppfNode != null) stack.add(curNode.leftSppfNode!!)
            }

            is NonterminalSppfNode<*> -> {
                if (curNode.children.isNotEmpty()) {
                    curNode.children.findLast {
                        it.rightSppfNode != curNode && it.leftSppfNode != curNode && !visited.contains(
                            it
                        )
                    }?.let { stack.add(it) }
                    curNode.children.forEach { visited.add(it) }
                }
            }
        }

    }
    return result
}

/**
 * Collects leaves of the derivation tree in order from left to right and joins them into one string
 * @return String value of recognized subrange
 */
fun buildStringFromSppf(sppfNode: ISppfNode): String {
    return buildTokenStreamFromSppf(sppfNode).joinToString(separator = "")
}