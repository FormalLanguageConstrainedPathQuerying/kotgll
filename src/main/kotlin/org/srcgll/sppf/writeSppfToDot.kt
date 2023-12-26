package org.srcgll.sppf

import org.srcgll.sppf.node.*
import java.io.File


fun writeSppfToDot(sppfNode: ISppfNode, filePath: String) {
    val queue: ArrayDeque<ISppfNode> = ArrayDeque(listOf(sppfNode))
    val edges: HashMap<Int, HashSet<Int>> = HashMap()
    val visited: HashSet<Int> = HashSet()
    var node: ISppfNode

    val file = File(filePath)

    file.printWriter().use { out ->
        out.println("digraph g {")

        while (queue.isNotEmpty()) {
            node = queue.removeFirst()
            if (!visited.add(node.id)) continue

            out.println(printNode(node.id, node))

            (node as? ParentSppfNode<*>)?.kids?.forEach {
                queue.addLast(it)
                if (!edges.containsKey(node.id)) {
                    edges[node.id] = HashSet()
                }
                edges.getValue(node.id).add(it.id)
            }

            val leftChild = (node as? PackedSppfNode<*>)?.leftSppfNode
            val rightChild = (node as? PackedSppfNode<*>)?.rightSppfNode

            if (leftChild != null) {
                queue.addLast(leftChild)
                if (!edges.containsKey(node.id)) {
                    edges[node.id] = HashSet()
                }
                edges.getValue(node.id).add(leftChild.id)
            }
            if (rightChild != null) {
                queue.addLast(rightChild)
                if (!edges.containsKey(node.id)) {
                    edges[node.id] = HashSet()
                }
                edges.getValue(node.id).add(rightChild.id)
            }
        }
        for (kvp in edges) {
            val head = kvp.key
            for (tail in kvp.value) out.println(printEdge(head, tail))
        }
        out.println("}")
    }
}

fun getColor(weight: Int): String = if (weight == 0) "black" else "red"

fun printEdge(x: Int, y: Int): String {
    return "${x}->${y}"
}

fun printNode(nodeId: Int, node: ISppfNode): String {
    return when (node) {
        is TerminalSppfNode<*> -> {
            "${nodeId} [label = \"${nodeId} ; ${node.terminal ?: "eps"}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = ellipse, color = ${getColor(node.weight)}]"
        }

        is SymbolSppfNode<*> -> {
            "${nodeId} [label = \"${nodeId} ; ${node.symbol.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = octagon, color = ${getColor(node.weight)}]"
        }

        is ItemSppfNode<*> -> {
            "${nodeId} [label = \"${nodeId} ; RSM: ${node.rsmState.nonterminal.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = rectangle, color = ${getColor(node.weight)}]"
        }

        is PackedSppfNode<*> -> {
            "${nodeId} [label = \"${nodeId} ; Weight: ${node.weight}\", shape = point, width = 0.5, color = ${getColor(node.weight)}]"
        }

        else -> ""
    }
}