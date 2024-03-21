package org.srcgll.parser.generator

import org.srcgll.descriptors.Descriptor
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.input.Edge
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.parser.IGll
import org.srcgll.parser.ParsingException
import org.srcgll.parser.context.Context
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.ITerminal
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode

abstract class GeneratedParser<VertexType, LabelType : ILabel> :
    IGll<VertexType, LabelType> {
    abstract val grammar: Grammar

    var input: IInputGraph<VertexType, LabelType>
        get() {
            return ctx.input
        }
        set(value) {
            ctx = Context(grammar.rsm, value)
        }

    protected abstract val ntFuncs: HashMap<Nonterminal, (Descriptor<VertexType>, SppfNode<VertexType>?) -> Unit>

    override fun parse(descriptor: Descriptor<VertexType>) {
        val state = descriptor.rsmState
        val nt = state.nonterminal

        val handleEdges = ntFuncs[nt] ?: throw ParsingException("Nonterminal ${nt.name} is absent from the grammar!")

        val pos = descriptor.inputPosition

        ctx.descriptors.addToHandled(descriptor)
        val curSppfNode = descriptor.getCurSppfNode(ctx)

        val leftExtent = curSppfNode?.leftExtent
        val rightExtent = curSppfNode?.rightExtent

        checkAcceptance(curSppfNode, leftExtent, rightExtent, state.nonterminal)

        for (inputEdge in ctx.input.getEdges(pos)) {
            if (inputEdge.label.terminal == null) {
                input.handleNullLabel(descriptor, curSppfNode, inputEdge, ctx)
                continue
            }
        }
        handleEdges(descriptor, curSppfNode)

        if (state.isFinal) pop(descriptor.gssNode, curSppfNode, pos)

    }

    protected fun handleTerminal(
        terminal: ITerminal,
        state: RsmState,
        inputEdge: Edge<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>,
        curSppfNode: SppfNode<VertexType>?
    ) {

        val newStates = state.terminalEdges[terminal] ?:
            throw ParsingException("State $state does not contains edges by terminal $terminal\n" +
                    "accessible edges: ${state.terminalEdges}")


        if (inputEdge.label.terminal == terminal) {
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