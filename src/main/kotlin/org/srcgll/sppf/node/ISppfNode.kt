package org.srcgll.sppf.node

interface ISppfNode {
    var id: Int
    /**
     * Used only with error recovery
     * Weight of Node defines how many insertions/deletions are needed
     * for the subrange to be recognised by corresponding Nonterminal
     */
    var weight: Int
    val parents: HashSet<ISppfNode>
}