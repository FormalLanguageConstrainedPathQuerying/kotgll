package org.ucfs.parser

import org.ucfs.descriptors.Descriptor
import org.ucfs.input.Edge
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.parser.context.IContext
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode

/**
 * If overriding field uses -- 1.2 % longer parser operation (due to hashset initialisation)
 */
abstract class GeneratedParser<VertexType, LabelType : ILabel> :
    IGll<VertexType, LabelType> {
    override lateinit var ctx: IContext<VertexType, LabelType>

    /**
     * Processes faster than map from nonterminal to method (proved experimentally)
     */
    protected abstract fun callNtFuncs(
        nt: Nonterminal,
        descriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ): Unit

    override fun handleDescriptor(descriptor: Descriptor<VertexType>) {
        val state = descriptor.rsmState
        val nt = state.nonterminal

        val pos = descriptor.inputPosition
        val curSppfNode = descriptor.sppfNode
        val epsilonSppfNode = ctx.sppf.getEpsilonSppfNode(descriptor)

        if (state.isFinal) {
            pop(descriptor.gssNode, curSppfNode ?: epsilonSppfNode, pos)
        }

        ctx.descriptors.addToHandled(descriptor)

        callNtFuncs(nt, descriptor, curSppfNode)
    }

    protected fun handleTerminal(
        terminal: ITerminal,
        state: RsmState,
        inputEdge: Edge<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ) {
        for (target in state.terminalEdges[terminal] ?: emptyList()) {
            handleTerminalOrEpsilonEdge(
                descriptor,
                curSppfNode,
                terminal,
                target,
                inputEdge.head,
                0
            )
        }
    }

    abstract fun setInput(input: IInputGraph<VertexType, LabelType>)
}