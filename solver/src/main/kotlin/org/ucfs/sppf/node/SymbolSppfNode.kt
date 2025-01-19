package org.ucfs.sppf.node

import org.ucfs.rsm.symbol.Nonterminal
import java.util.*

/**
 * Symbol sppfNode to represent the root of the subtree which
 * corresponds to paths can be derived from the respective
 * nonterminal.
 */
class SymbolSppfNode<VertexType>(
    /**
     * Nonterminal, which defines language recognized by it
     */
    val symbol: Nonterminal,
    /**
     * Left limit of the subrange
     */
    private val intermediateNode: IntermediateSppfNode<VertexType>
) : NonterminalSppfNode<VertexType>(intermediateNode.leftExtent, intermediateNode.rightExtent) {

    override val children: HashSet<PackedSppfNode<VertexType>>
        get() = intermediateNode.children

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
