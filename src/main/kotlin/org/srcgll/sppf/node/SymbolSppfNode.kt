package org.srcgll.sppf.node

import org.srcgll.rsm.symbol.Nonterminal
import java.util.*

class SymbolSppfNode<VertexType>(
    val symbol: Nonterminal,
    leftExtent: VertexType,
    rightExtent: VertexType,
) : ParentSppfNode<VertexType>(leftExtent, rightExtent) {
    override fun toString() = "SymbolSppfNode(leftExtent=$leftExtent, rightExtent=$rightExtent, symbol=$symbol)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolSppfNode<*>) return false
        if (!super.equals(other)) return false
        if (symbol != other.symbol) return false

        return true
    }

    override val hashCode: Int = Objects.hash(leftExtent, rightExtent, symbol)
    override fun hashCode() = hashCode
}
