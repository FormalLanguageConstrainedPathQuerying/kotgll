package org.ucfs.ast

import org.ucfs.GeneratorException
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.*

class AstExtractor(val pkg: String) {
    val nonterminalToClass = HashMap<Nonterminal, Class<*>>()

    /**
     * need to handle "many" in rules (many can make cycles in sppf)
     */
    val used = HashSet<PackedSppfNode<*>>()
    fun extract(sppf: ISppfNode?): Node {
        val root = Node(null, 0)
        extract(sppf, root, null)
        return root.children.firstOrNull() ?: root
    }

    private fun getOffset(left: Node?, parent: Node): Int {
        return if (left == null) {
            parent.offset
        } else {
            left.offset + left.length
        }
    }

    /**
     * return rightest node of subtree
     */
    private fun extract(sppf: ISppfNode?, parent: Node, left: Node?): Node? {
        when (sppf) {
            is PackedSppfNode<*> -> {
                val newLeft = extract(sppf.leftSppfNode, parent, left)
                return extract(sppf.rightSppfNode, parent, newLeft)
            }

            is IntermediateSppfNode<*> -> {
                return extract(sppf.children.firstOrNull(), parent, left)
            }

            is SymbolSppfNode<*> -> {
                val nodeClass = getNodeClass(sppf.symbol)
                val ctor = nodeClass.getConstructor(Node::class.java, Int::class.java)

                val node: Node = ctor.newInstance(parent, getOffset(left, parent)) as Node
                node.left = left
                parent.children.add(node)

                val packedNode: PackedSppfNode<*> = sppf.children.first { pn -> !used.contains(pn) }
                used.add(packedNode)

                extract(packedNode, node, null)
                parent.length += node.length
                return node
            }

            is TerminalSppfNode<*> -> {
                val node = TerminalNode(parent, getOffset(left, parent), sppf.terminal, left)
                parent.children.add(node)
                parent.length += sppf.terminal.toString().length
                return node
            }

            null -> return null
            else -> throw GeneratorException("Unknown sppf node type : $sppf")
        }
    }

    private fun getNodeClass(nt: Nonterminal): Class<*> {
        return nonterminalToClass.getOrPut(nt)
        { Class.forName("$pkg.${NodeClassesGenerator.getClassName(nt)}") }
    }
}