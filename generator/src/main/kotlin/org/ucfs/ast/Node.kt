package org.ucfs.ast

/**
 * TODO add methods below
 * - sppfNode (internalNode)
 * - constructor (parent, sppfNode, offset)
 */
open class Node(
    var parent: Node?,
    var offset: Int,
) {
    var length: Int = 0
    open var left: Node? = null
    var right: Node? = null
    var children: ArrayList<Node> = ArrayList()
    var isRecovered: Boolean = false
}