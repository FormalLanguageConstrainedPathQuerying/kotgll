package org.srcgll.ast

/**
 * TODO add methods below
 * - sppfNode (internalNode)
 * - constructor (parent, sppfNode, offset)
 */
abstract class Node(
    var children: List<Node>,
    var parent: Node?,
    var offset: Int,
    var length: Int
)