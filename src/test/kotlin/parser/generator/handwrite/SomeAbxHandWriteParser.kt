package parser.generator.handwrite

import org.srcgll.descriptors.Descriptor
import org.srcgll.exceptions.ParsingException
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.input.Edge
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.PrintableRsmState
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode


/**
 * hand-write parser for @Ab
 *
 */
class SomeAbHandWriteParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
    override lateinit var ctx: IContext<VertexType, LabelType>
    override val grammar = SomeAbX()
    val startState: PrintableRsmState = grammar.buildPrintableRsm()

    private fun handleTerminal(
        terminal: Terminal<String>, state: RsmState,
        inputEdge: Edge<VertexType, LabelType>,
        descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?
    ) {
        val newStates = state.terminalEdges[terminal]!!

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

    private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition

        if (state is PrintableRsmState) {
            for (path in state.pathLabels) {
                when (path) {
                    "" -> {
                        // handle terminal edges
                        for (inputEdge in ctx.input.getEdges(pos)) {
                            if (inputEdge.label.terminal == null) {
                                input.handleNullLabel(descriptor, curSppfNode, inputEdge, ctx)
                                continue
                            }
                            handleTerminal(grammar.x.terminal, state, inputEdge, descriptor, curSppfNode)
                        }
                        //handle nonterminal edges
                        val A = grammar.A.getNonterminal()!!
                        handleNonterminalEdge(descriptor, A, state.nonterminalEdges[A]!!, curSppfNode)
                    }
                }
            }
        }
    }

    private fun parseA(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition

        if (state is PrintableRsmState) {
            for (path in state.pathLabels) {
                when (path) {
                    "" -> {
                        // handle terminal edges
                        for (inputEdge in ctx.input.getEdges(pos)) {
                            if (inputEdge.label.terminal == null) {
                                input.handleNullLabel(descriptor, curSppfNode, inputEdge, ctx)
                                continue
                            }
                            handleTerminal(grammar.a.terminal, state, inputEdge, descriptor, curSppfNode)
                        }
                    }

                    "a" -> {
                        // handle terminal edges
                        for (inputEdge in ctx.input.getEdges(pos)) {
                            if (inputEdge.label.terminal == null) {
                                input.handleNullLabel(descriptor, curSppfNode, inputEdge, ctx)
                                continue
                            }
                            handleTerminal(grammar.b.terminal, state, inputEdge, descriptor, curSppfNode)
                        }
                    }

                    "ab" -> {
                        // handle terminal edges
                        for (inputEdge in ctx.input.getEdges(pos)) {
                            if (inputEdge.label.terminal == null) {
                                input.handleNullLabel(descriptor, curSppfNode, inputEdge, ctx)
                                continue
                            }
                            handleTerminal(grammar.a.terminal, state, inputEdge, descriptor, curSppfNode)
                        }
                    }
                }
            }
        }
    }


    private val NtFuncs = hashMapOf<Nonterminal, (Descriptor<VertexType>, SppfNode<VertexType>?) -> Unit>(
        grammar.S.getNonterminal()!! to ::parseS,
        grammar.A.getNonterminal()!! to ::parseA
    )

    override fun parse(curDescriptor: Descriptor<VertexType>) {
        val state = curDescriptor.rsmState
        val nt = state.nonterminal

        val handleEdges = NtFuncs[nt] ?: throw ParsingException("Nonterminal ${nt.name} is absent from the grammar!")

        val pos = curDescriptor.inputPosition

        ctx.descriptors.addToHandled(curDescriptor)
        val curSppfNode = curDescriptor.getCurSppfNode(ctx)

        val leftExtent = curSppfNode?.leftExtent
        val rightExtent = curSppfNode?.rightExtent

        checkAcceptance(curSppfNode, leftExtent, rightExtent, state.nonterminal)

        handleEdges(curDescriptor, curSppfNode)

        if (state.isFinal) pop(curDescriptor.gssNode, curSppfNode, pos)

    }
}


/**
Grammar for
 *  S = (a b)* | x
 */
class SomeAbX : Grammar() {
    var S by Nt()
    var A by Nt()
    val a = Term("a")
    val b = Term("b")
    val x = Term("x")

    init {
        setStart(S)
        S = A or x
        A = Some(a * b)
    }
}
