package org.ucfs.sppf.node

import org.ucfs.rsm.symbol.Nonterminal
import java.util.*

/**
 * Symbol sppfNode. The corresponding tree with Symbol sppfNode as root represents derivation subtree for
 * subrange, recognized by nonterminal
 */
class SymbolSppfNode<VertexType>(
    /**
     * Nonterminal, which defines language recognized by it
     */
    val symbol: Nonterminal,
    /**
     * Left limit of the subrange
     */
    leftExtent: VertexType,
    /**
     * Right limit of the subrange
     */
    rightExtent: VertexType,
) : NonterminalSppfNode<VertexType>(leftExtent, rightExtent) {
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
