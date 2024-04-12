package org.srcgll.sppf.node

/**
 * SppfNode interface
 */
interface ISppfNode {
    var id: Int
    /**
     * Part of the error recovery mechanism
     * weight of the node defines how many insertions/deletions are needed
     * for the subrange to be recognised by corresponding Nonterminal
     */
    var weight: Int

    /**
     * Set of all nodes, that have current one as a child
     */
    val parents: HashSet<ISppfNode>
}