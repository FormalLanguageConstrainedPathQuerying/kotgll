package org.ucfs.parser

import org.ucfs.descriptors.Descriptor
import org.ucfs.gss.GssNode
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.parser.context.IContext
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode
import org.ucfs.sppf.node.SymbolSppfNode

/**
 * Interface for Gll parser
 * @param VertexType - type of vertex in input graph
 * @param LabelType - type of label on edges in input graph
 */
interface IGll<VertexType, LabelType : ILabel> {
    /**
     * Parser configuration
     */
    var ctx: IContext<VertexType, LabelType>

    /**
     * Main parsing loop. Iterates over available descriptors and processes them
     * @return Pair of derivation tree root and collection of reachability pairs
     */
    fun parse(): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        initDescriptors(ctx.input)

        // Continue parsing until all default descriptors processed
        var curDescriptor = ctx.nextDescriptorToHandle()
        while (curDescriptor != null) {
            parse(curDescriptor)
            curDescriptor = ctx.nextDescriptorToHandle()
        }

        return Pair(ctx.parseResult, ctx.reachabilityPairs)
    }

    /**
     * Processes descriptor
     * @param descriptor - descriptor to process
     */
    fun parse(descriptor: Descriptor<VertexType>)

    /**
     * Creates descriptors for all starting vertices in input graph
     * @param input - input graph
     */
    fun initDescriptors(input: IInputGraph<VertexType, LabelType>) {
        for (startVertex in input.getInputStartVertices()) {
            val descriptor = Descriptor(
                ctx.startState,
                getOrCreateGssNode(ctx.startState.nonterminal, startVertex, weight = 0),
                sppfNode = null,
                startVertex
            )
            addDescriptor(descriptor)
        }
    }

    /**
     * Creates or retrieves gssNode with given parameters
     * @param nonterminal - nonterminal, corresponding to grammar slot aA·b
     * @param inputPosition - vertex in input graph
     * @param weight - weight of minimal left part, i.e. derivation tree with minimum weight
     * @return gssNode
     */
    fun getOrCreateGssNode(
        nonterminal: Nonterminal,
        inputPosition: VertexType,
        weight: Int,
    ): GssNode<VertexType> {
        val gssNode = GssNode(nonterminal, inputPosition, weight)
        val storedNode = ctx.createdGssNodes.computeIfAbsent(gssNode) { gssNode }
        if (storedNode.minWeightOfLeftPart > weight) {
            storedNode.minWeightOfLeftPart = weight
        }
        return storedNode
    }

    /**
     * Creates gssNode with given parameters, along with corresponding edge
     * @param nonterminal - nonterminal, corresponding to grammar slot aA·b
     * @param rsmState - rsmState
     * @param gssNode - tail of the edge in Gss
     * @param sppfNode - derivation tree to store on edge in Gss
     * @param inputPosition - vertex in input graph
     */
    fun createGssNode(
        nonterminal: Nonterminal,
        rsmState: RsmState,
        gssNode: GssNode<VertexType>,
        sppfNode: SppfNode<VertexType>?,
        inputPosition: VertexType,
    ): GssNode<VertexType> {
        val newNode =
            getOrCreateGssNode(
                nonterminal,
                inputPosition,
                weight = gssNode.minWeightOfLeftPart + (sppfNode?.weight ?: 0)
            )

        if (newNode.addEdge(rsmState, sppfNode, gssNode)) {
            if (ctx.poppedGssNodes.containsKey(newNode)) {
                for (popped in ctx.poppedGssNodes[newNode]!!) {
                    val descriptor = Descriptor(
                        rsmState, gssNode, ctx.sppf.getParentNode(rsmState, sppfNode, popped!!), popped.rightExtent
                    )
                    addDescriptor(descriptor)
                }
            }
        }

        return newNode
    }

    /**
     * Adds descriptor to processing
     * @param descriptor - descriptor to add
     */
    fun addDescriptor(descriptor: Descriptor<VertexType>) {
        ctx.addDescriptor(descriptor)
    }

    /**
     * Iterates over all outgoing edges from current gssNode, collects derivation trees, stored on edges and
     * combines them with current derivation tree, given as sppfNode
     * @param gssNode - current node in top layer of Graph Structured Stack
     * @param sppfNode - derivation tree, corresponding to parsed portion of input
     * @param inputPosition - vertex in input graph
     */
    fun pop(gssNode: GssNode<VertexType>, sppfNode: SppfNode<VertexType>?, inputPosition: VertexType) {
        if (!ctx.poppedGssNodes.containsKey(gssNode)) ctx.poppedGssNodes[gssNode] = HashSet()
        ctx.poppedGssNodes.getValue(gssNode).add(sppfNode)

        for ((label, target) in gssNode.edges) {
            for (node in target) {
                val descriptor = Descriptor(
                    label.first, node, ctx.sppf.getParentNode(label.first, label.second, sppfNode!!), inputPosition
                )
                addDescriptor(descriptor)
            }
        }
    }

    /**
     * Check whether range leftExtent - rightExtent belongs to the language, defined by given nonterminal. And if so -
     * updates parseResult field in context with given sppfNode, i.e. derivation tree of corresponding range
     * @param sppfNode - derivation tree of the range leftExtent - rightExtent
     * @param leftExtent - left limit of the range
     * @param rightExtent - right limit of the range
     * @param nonterminal - nonterminal, which defines language we check belonging to
     */
    fun checkAcceptance(
        sppfNode: SppfNode<VertexType>?,
        leftExtent: VertexType?,
        rightExtent: VertexType?,
        nonterminal: Nonterminal
    ) {
        if (sppfNode is SymbolSppfNode<VertexType>
            && nonterminal == ctx.startState.nonterminal
            && ctx.input.isStart(leftExtent!!)
            && ctx.input.isFinal(rightExtent!!)
        ) {
            if (ctx.parseResult == null || ctx.parseResult!!.weight > sppfNode.weight) {
                ctx.parseResult = sppfNode
            }

            //update reachability
            val pair = Pair(leftExtent, rightExtent)
            val distance = ctx.sppf.minDistance(sppfNode)

            ctx.reachabilityPairs[pair] =
                if (ctx.reachabilityPairs.containsKey(pair)) {
                    minOf(distance, ctx.reachabilityPairs[pair]!!)
                } else {
                    distance
                }
        }
    }

    /**
     * Function for processing nonterminal edges in RSM
     * @param descriptor - current parsing stage
     * @param nonterminal - nonterminal, defining box in RSM
     * @param targetStates - set of all adjacent nodes in RSM
     * @param sppfNode - derivation tree of already parsed portion of input
     */
    fun handleNonterminalEdge(
        descriptor: Descriptor<VertexType>,
        nonterminal: Nonterminal,
        targetStates: HashSet<RsmState>,
        sppfNode: SppfNode<VertexType>?
    ) {
        for (target in targetStates) {
            val newDescriptor = Descriptor(
                nonterminal.startState,
                createGssNode(nonterminal, target, descriptor.gssNode, sppfNode, descriptor.inputPosition),
                sppfNode = null,
                descriptor.inputPosition
            )
            ctx.addDescriptor(newDescriptor)
        }
    }

    /**
     * Function for processing terminal edges in RSM, with respect to terminal or epsilon value on label
     * in input graph edges
     * @param descriptor - current parsing stage
     * @param sppfNode - derivation tree of already parsed portion of input
     * @param terminal - value on label in input graph
     * @param targetState - head of edge in RSM
     * @param targetVertex - head of edge in input graph
     * @param targetWeight - weight that should be assigned to newly created nodes in sppf
     */
    fun handleTerminalOrEpsilonEdge(
        descriptor: Descriptor<VertexType>,
        sppfNode: SppfNode<VertexType>?,
        terminal: ITerminal?,
        targetState: RsmState,
        targetVertex: VertexType,
        targetWeight: Int,
    ) {
        val newDescriptor = Descriptor(
            targetState, descriptor.gssNode, ctx.sppf.getParentNode(
                targetState, sppfNode, ctx.sppf.getOrCreateTerminalSppfNode(
                    terminal, descriptor.inputPosition, targetVertex, targetWeight
                )
            ), targetVertex
        )
        addDescriptor(newDescriptor)
    }
}
