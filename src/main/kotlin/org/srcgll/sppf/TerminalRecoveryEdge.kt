package org.srcgll.sppf

data class TerminalRecoveryEdge<VertexType>(
    val head: VertexType,
    val weight: Int,
)