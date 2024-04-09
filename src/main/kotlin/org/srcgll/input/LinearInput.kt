package org.srcgll.input

import org.srcgll.rsm.symbol.Terminal

open class LinearInput<VertexType, LabelType : ILabel> : IInputGraph<VertexType, LabelType> {
    override val vertices: MutableSet<VertexType> = HashSet()
    override val edges: MutableMap<VertexType, MutableList<Edge<VertexType, LabelType>>> = HashMap()
    override val startVertices: MutableSet<VertexType> = HashSet()

    override fun getInputStartVertices(): MutableSet<VertexType> {
        return startVertices
    }

    override fun getAllVertices(): MutableSet<VertexType> = vertices

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

    companion object {
        /**
         * Split CharSequence into stream of strings, separated by space symbol
         */
        fun buildFromString(input: String): IInputGraph<Int, LinearInputLabel> {
            val inputGraph = LinearInput<Int, LinearInputLabel>()
            var curVertexId = 0

            inputGraph.addStartVertex(curVertexId)
            inputGraph.addVertex(curVertexId)

            for (x in input.trim().split(' ')) {
                if (x.isNotEmpty()) {
                    inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x)), ++curVertexId)
                    inputGraph.addVertex(curVertexId)
                }
            }
            return inputGraph
        }
    }
}