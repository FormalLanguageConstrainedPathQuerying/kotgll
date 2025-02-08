package org.ucfs.sppf

import org.ucfs.sppf.node.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun writeSppfToDot(sppfNode: ISppfNode, filePath: String, label: String = "") {
    val genPath = Path.of("gen", "sppf")
    Files.createDirectories(genPath)
    val file = genPath.resolve(filePath).toFile()

    file.printWriter().use { out ->
        out.println(getSppfDot(sppfNode, label))
    }
}

fun getSppfDot(sppfNode: ISppfNode, label: String = ""): String {
    val queue: ArrayDeque<ISppfNode> = ArrayDeque(listOf(sppfNode))
    val edges: HashMap<Int, HashSet<Int>> = HashMap()
    val visited: HashSet<Int> = HashSet()
    var node: ISppfNode
    val sb = StringBuilder()
    sb.append("digraph g {")
    sb.append("labelloc=\"t\"")
    sb.append("label=\"$label\"")

    while (queue.isNotEmpty()) {
        node = queue.removeFirst()
        if (!visited.add(node.id)) continue

        sb.append(printNode(node.id, node))

        (node as? NonterminalSppfNode<*>)?.children?.forEach {
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
    for ((head, tails) in edges) {
        for (tail in tails) {
            sb.append(printEdge(head, tail))
        }
    }
    sb.append("}")
    return sb.toString()

}

fun getColor(weight: Int): String = if (weight == 0) "black" else "red"

fun printEdge(x: Int, y: Int): String {
    return "${x}->${y}"
}

fun printNode(nodeId: Int, node: ISppfNode): String {
    return when (node) {
        is TerminalSppfNode<*> -> {
            "${nodeId} [label = \"Terminal ${nodeId} ; ${node.terminal ?: "eps"}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = ellipse, color = ${
                getColor(
                    node.weight
                )
            }]"
        }

        is SymbolSppfNode<*> -> {
            "${nodeId} [label = \"Symbol ${nodeId} ; ${node.symbol.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = octagon, color = ${
                getColor(
                    node.weight
                )
            }]"
        }

        is IntermediateSppfNode<*> -> {
            "${nodeId} [label = \"Intermediate ${nodeId} ; RSM: ${node.rsmState.nonterminal.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = rectangle, color = ${
                getColor(
                    node.weight
                )
            }]"
        }

        is PackedSppfNode<*> -> {
            "${nodeId} [label = \"Packed ${nodeId} ; Weight: ${node.weight}\", shape = point, width = 0.5, color = ${
                getColor(
                    node.weight
                )
            }]"
        }

        else -> ""
    }
}