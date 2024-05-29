package org.ucfs.parser

import org.ucfs.descriptors.Descriptor
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.input.Edge
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode


abstract class GeneratedParser<VertexType, LabelType : ILabel> :
    IGll<VertexType, LabelType> {
    abstract val grammar: Grammar

    abstract var input: IInputGraph<VertexType, LabelType>

    /**
     * Processes faster than map from nonterminal to method (proved experimentally)
     */
    protected abstract fun callNtFuncs(
        nt: Nonterminal,
        descriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ): Unit

    override fun parse(descriptor: Descriptor<VertexType>) {
        val state = descriptor.rsmState
        val nt = state.nonterminal

        val pos = descriptor.inputPosition
        val curSppfNode = descriptor.sppfNode
        val epsilonSppfNode = ctx.sppf.getEpsilonSppfNode(descriptor)
        val leftExtent = curSppfNode?.leftExtent
        val rightExtent = curSppfNode?.rightExtent

        if (state.isFinal) {
            pop(descriptor.gssNode, curSppfNode ?: epsilonSppfNode, pos)
        }

        ctx.descriptors.addToHandled(descriptor)

        if (state.isStart && state.isFinal) {
            checkAcceptance(
                epsilonSppfNode,
                epsilonSppfNode!!.leftExtent,
                epsilonSppfNode!!.rightExtent,
                nt
            )
        }
        checkAcceptance(curSppfNode, leftExtent, rightExtent, nt)

        callNtFuncs(nt, descriptor, curSppfNode)
    }

    protected fun handleTerminal(
        terminal: ITerminal,
        state: RsmState,
        inputEdge: Edge<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ) {


        if (inputEdge.label.terminal == terminal) {
            val newStates = state.terminalEdges[terminal] ?: throw ParsingException(
                "State $state does not contains edges " +
                        "\nby terminal $terminal" +
                        "\naccessible edges: ${state.terminalEdges}\n"
            )
            for (target in newStates) {
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
    }
}