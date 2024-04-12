package org.ucfs.input

import org.ucfs.rsm.symbol.Term

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

    override fun toString(): String {
        if(startVertices.isEmpty()){
            return "${this.javaClass}: empty"
        }
        var v: VertexType = startVertices.first()
        val sb = StringBuilder()
        while(v != null){
            val e = edges[v]?.first() ?: break
            sb.append("\n")
            sb.append(e.label)
            v = e.head
        }
        return sb.toString()
    }

    companion object {
        /**
         * Split CharSequence into stream of strings, separated by space symbol
         */
        fun buildFromString(input: String): IInputGraph<Int, LinearInputLabel> {
            val inputGraph = LinearInput<Int, LinearInputLabel>()
            var curVertexId = 0

            inputGraph.addStartVertex(curVertexId)
            inputGraph.addVertex(curVertexId)

            for (x in input.trim().split(SPACE).filter { it.isNotEmpty() }) {
                if (x.isNotEmpty()) {
                    inputGraph.addEdge(curVertexId, LinearInputLabel(Term(x)), ++curVertexId)
                    inputGraph.addVertex(curVertexId)
                }
            }
            return inputGraph
        }
        const val SPACE = " "
    }
}