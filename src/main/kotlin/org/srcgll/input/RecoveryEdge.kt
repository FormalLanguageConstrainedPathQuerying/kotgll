package org.srcgll.input

import org.srcgll.rsm.symbol.ITerminal

data class RecoveryEdge<VertexType>(
    val label: ITerminal?,
    val head: VertexType,
    val weight: Int,
)