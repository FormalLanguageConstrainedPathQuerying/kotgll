package org.ucfs.input

/**
 * Input graph interface
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
interface IInputGraph<VertexType, LabelType : ILabel> {
    /**
     * @return collection of all starting vertices
     */
    fun getInputStartVertices(): MutableSet<VertexType>

    /**
     * Adds passed vertex both to collection of starting vertices and collection of all vertices
     * @param vertex - vertex to add
     */
    fun addStartVertex(vertex: VertexType)

    /**
     * Adds passed vertex to collection of all vertices, but not collection of starting vertices
     * @param vertex - vertex to add
     */
    fun addVertex(vertex: VertexType)


    /**
     * Returns all outgoing edges from given vertex
     * @param from - vertex to retrieve outgoing edges from
     * @return Collection of outgoing edges
     */
    fun getEdges(from: VertexType): MutableList<Edge<VertexType, LabelType>>

    /**
     * Adds edge to graph
     * @param from - tail of the edge
     * @param label - value to store on the edge
     * @param to - head of the edge
     */
    fun addEdge(from: VertexType, label: LabelType, to: VertexType)

    /**
     * Removes edge from graph
     * @param from - tail of the edge
     * @param label - value, stored on the edge
     * @param to - head of the edge
     */
    fun removeEdge(from: VertexType, label: LabelType, to: VertexType)

    /**
     * @param vertex - vertex to check
     * @return true if given vertex is starting, false otherwise
     */
    fun isStart(vertex: VertexType): Boolean

    /**
     * @param vertex - vertex to check
     * @return true if given vertex is final, false otherwise
     */
    fun isFinal(vertex: VertexType): Boolean

    fun removeVertex(vertex: VertexType)
}