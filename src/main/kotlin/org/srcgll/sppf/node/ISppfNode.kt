package org.srcgll.sppf.node

interface ISppfNode {
    var id: Int
    var weight: Int
    val parents: HashSet<ISppfNode>
}