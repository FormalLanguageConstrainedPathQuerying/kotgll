package org.ucfs.input

open class InputGraph<VertexType, LabelType : ILabel> : IInputGraph<VertexType, LabelType> {

    var name = "G"

    val vertices: MutableSet<VertexType> = HashSet()

    val edges: MutableMap<VertexType, MutableList<Edge<VertexType, LabelType>>> = HashMap()

    val startVertices: MutableSet<VertexType> = HashSet()

    override fun getInputStartVertices(): MutableSet<VertexType> {
        return startVertices
    }

    override fun addStartVertex(vertex: VertexType) {
        startVertices.add(vertex)
        vertices.add(vertex)
    }

    override fun addVertex(vertex: VertexType) {
        vertices.add(vertex)
    }

    override fun removeVertex(vertex: VertexType) {
        startVertices.remove(vertex)
        vertices.remove(vertex)
    }

    override fun getEdges(from: VertexType): MutableList<Edge<VertexType, LabelType>> {
        return edges.getOrDefault(from, ArrayList())
    }

    override fun addEdge(from: VertexType, label: LabelType, to: VertexType) {
        val edge = Edge(label, to)

        if (!edges.containsKey(from)) edges[from] = ArrayList()

        edges.getValue(from).add(edge)
    }


    override fun removeEdge(from: VertexType, label: LabelType, to: VertexType) {
        val edge = Edge(label, to)
        edges.getValue(from).remove(edge)
    }

    override fun isStart(vertex: VertexType) = startVertices.contains(vertex)
    override fun isFinal(vertex: VertexType) = getEdges(vertex).isEmpty()
}