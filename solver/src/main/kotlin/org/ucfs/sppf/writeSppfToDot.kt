package org.ucfs.sppf

import org.ucfs.sppf.node.*
import java.nio.file.Files
import java.nio.file.Path

fun writeSppfToDot(sppfNode: ISppfNode, filePath: String, label: String = "") {
    val queue: ArrayDeque<ISppfNode> = ArrayDeque(listOf(sppfNode))
    val edges: HashMap<Int, HashSet<Int>> = HashMap()
    val visited: HashSet<Int> = HashSet()
    var node: ISppfNode
    val genPath = Path.of("gen", "sppf")
    Files.createDirectories(genPath)
    val file = genPath.resolve(filePath).toFile()


    file.printWriter().use { out ->
        out.println("digraph g {")
        out.println("labelloc=\"t\"")
        out.println("label=\"$label\"")

        while (queue.isNotEmpty()) {
            node = queue.removeFirst()
            if (!visited.add(node.id)) continue

            out.println(printNode(node.id, node))

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
            "${nodeId} [label = \"Terminal ${nodeId} ; ${node.terminal ?: "eps"}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = ellipse, color = ${getColor(node.weight)}]"
        }

        is SymbolSppfNode<*> -> {
            "${nodeId} [label = \"Symbol ${nodeId} ; ${node.symbol.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = octagon, color = ${getColor(node.weight)}]"
        }

        is IntermediateSppfNode<*> -> {
            "${nodeId} [label = \"Intermediate ${nodeId} ; RSM: ${node.rsmState.nonterminal.name}, ${node.leftExtent}, ${node.rightExtent}, Weight: ${node.weight}\", shape = rectangle, color = ${getColor(node.weight)}]"
        }

        is PackedSppfNode<*> -> {
            "${nodeId} [label = \"Packed ${nodeId} ; Weight: ${node.weight}\", shape = point, width = 0.5, color = ${getColor(node.weight)}]"
        }

        else -> ""
    }
}