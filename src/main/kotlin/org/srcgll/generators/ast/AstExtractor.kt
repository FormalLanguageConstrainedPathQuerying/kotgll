package org.srcgll.generators.ast

import org.srcgll.ast.Node
import org.srcgll.sppf.node.ISppfNode
import org.srcgll.sppf.node.IntermediateSppfNode
import org.srcgll.sppf.node.PackedSppfNode
import org.srcgll.sppf.node.SymbolSppfNode

object AstExtractor {
    fun extract(sppf: ISppfNode): Node {

        when (sppf) {
            is PackedSppfNode<*> -> {

            }
            is IntermediateSppfNode<*> -> TODO()
            is SymbolSppfNode<*> -> {
                val className = AstClassesGenerator.getClassName(sppf.symbol)

            }
        }
        TODO()
    }
}