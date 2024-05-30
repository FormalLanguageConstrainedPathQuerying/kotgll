package org.ucfs.ast

import java.nio.file.Files
import java.nio.file.Path

class DotWriter {
    private var lastId = 0
    var showSiblings = true
    val ids: HashMap<Node, Int> = HashMap()
    fun getId(node: Node): Int {
        return ids.getOrPut(node) { lastId++ }
    }

    fun getDotView(root: Node, label: String = "AST"): String {
        val view = StringBuilder("digraph g {")
        view.append("label=\"$label\"")
        view.append(handleNode(root))
        view.append("}")
        return view.toString()
    }

    private fun getNodeLabel(node: Node): String {
        val view = StringBuilder("label = \"")
        when (node) {
            is TerminalNode<*> -> {
                view.append(node.terminal.toString())
            }

            else -> {
                view.append(node.javaClass.simpleName)
            }
        }
        view.append("\noffset = ${node.offset}")
        view.append("\nlength = ${node.length}")
        view.append("\"")
        return view.toString()
    }

    private fun getNodeView(node: Node): StringBuilder {
        val view = StringBuilder("\n${getId(node)}  [ ${getNodeLabel(node)}")
        if (node is TerminalNode<*>) {
            if(node.isRecovered) {
                view.append(", color = red")
            }
            else{
                view.append(", color = green")
            }
        }
        view.append("]")
        return view
    }

    fun handleNode(node: Node): String {
        val id = getId(node)
        val view = getNodeView(node)
        val left = node.left

        if (showSiblings && left != null) {
            view.append("\n$id -> ${getId(left)} [color=blue]")
        }

        for (child in node.children) {
            view.append("\n$id -> ${getId(child)}")
            view.append(handleNode(child))
        }
        return view.toString()
    }

    fun writeToFile(view: String, filePath: Path) {
        val genPath = Path.of("gen", "ast")
        Files.createDirectories(genPath)
        val file = genPath.resolve(filePath).toFile()
        file.writeText(view)
    }

    fun writeToFile(root: Node, fileName: String, label: String = "AST", showSiblings: Boolean) {
        this.showSiblings = showSiblings
        writeToFile(getDotView(root, label), Path.of("$fileName.dot"))
    }

}