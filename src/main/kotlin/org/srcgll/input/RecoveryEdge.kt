package org.srcgll.input

import org.srcgll.rsm.symbol.Terminal

data class RecoveryEdge<VertexType>(
    val label: Terminal<*>?,
    val head: VertexType,
    val weight: Int,
)