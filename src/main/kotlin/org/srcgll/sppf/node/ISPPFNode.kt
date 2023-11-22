package org.srcgll.sppf.node

interface ISPPFNode
{
    var id      : Int
    var weight  : Int
    val parents : HashSet<ISPPFNode>
}