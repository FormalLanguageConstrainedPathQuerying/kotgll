package org.ucfs.input

import org.ucfs.rsm.symbol.ITerminal

data class RecoveryEdge<VertexType>(
    val label: ITerminal?,
    val head: VertexType,
    val weight: Int,
)