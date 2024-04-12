package org.srcgll.input

import org.srcgll.descriptors.Descriptor
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.ITerminal
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode

/**
 * Input graph interface
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
interface IInputGraph<VertexType, LabelType : ILabel> {
    /**
     * Collection of all vertices in graph
     */
    val vertices: MutableSet<VertexType>

    /**
     * Maps vertex to edges, outgoing from it
     */
    val edges: MutableMap<VertexType, MutableList<Edge<VertexType, LabelType>>>

    /**
     * Collection of all starting vertices, used to create initial descriptors to begin parsing
     */
    val startVertices: MutableSet<VertexType>

    /**
     * @return collection of all starting vertices
     */
    fun getInputStartVertices(): MutableSet<VertexType>

    /**
     * @return Collection of all vertices
     */
    fun getAllVertices(): MutableSet<VertexType>

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
     * Removes vertex both from collection of starting vertices and collection of all vertices
     * @param vertex - vertex to remove
     */
    fun removeVertex(vertex: VertexType)

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

    /**
     * Process outgoing edges from input position in given descriptor, according to processing logic, represented as
     * separate functions for both outgoing terminal and nonterminal edges from rsmState in descriptor
     * @param handleTerminalOrEpsilonEdge - function for processing terminal and epsilon edges in RSM
     * @param handleNonterminalEdge - function for processing nonterminal edges in RSM
     * @param ctx - configuration of Gll parser instance
     * @param descriptor - descriptor, represents current parsing stage
     * @param sppfNode - root node of derivation tree, corresponds to already parsed portion of input
     */
    fun handleEdges(
        handleTerminalOrEpsilonEdge: (
            descriptor: Descriptor<VertexType>,
            sppfNode: SppfNode<VertexType>?,
            terminal: ITerminal?,
            targetState: RsmState,
            targetVertex: VertexType,
            targetWeight: Int,
        ) -> Unit,
        handleNonterminalEdge: (
            descriptor: Descriptor<VertexType>,
            nonterminal: Nonterminal,
            targetStates: HashSet<RsmState>,
            sppfNode: SppfNode<VertexType>?
        ) -> Unit,
        ctx: IContext<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?
    ) {
        val rsmState = descriptor.rsmState
        val inputPosition = descriptor.inputPosition
        val terminalEdges = rsmState.terminalEdges
        val nonterminalEdges = rsmState.nonterminalEdges

        for (inputEdge in ctx.input.getEdges(inputPosition)) {
            if (inputEdge.label.terminal == null) {
                handleTerminalOrEpsilonEdge(descriptor, sppfNode, null, descriptor.rsmState, inputEdge.head, 0)
                continue
            }
            for ((edgeTerminal, targetStates) in terminalEdges) {
                if (inputEdge.label.terminal == edgeTerminal) {
                    for (target in targetStates) {
                        handleTerminalOrEpsilonEdge(descriptor, sppfNode, edgeTerminal, target, inputEdge.head, 0)
                    }
                }
            }
        }

        for ((edgeNonterminal, targetStates) in nonterminalEdges) {
            handleNonterminalEdge(descriptor, edgeNonterminal, targetStates, sppfNode)
        }
    }
}