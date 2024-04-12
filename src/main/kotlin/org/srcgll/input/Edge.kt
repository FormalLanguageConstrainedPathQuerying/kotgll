package org.ucfs.input

data class Edge<VertexType, LabelType : ILabel>(
    val label: LabelType,
    val head: VertexType,
)