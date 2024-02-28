package parser.generator.handwrite

import org.srcgll.descriptors.Descriptor
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode


/**
 * hand-write parser for @SomeAbX
 *
 */
class SomeAbHandWriteParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
    override lateinit var ctx: IContext<VertexType, LabelType>
    override val grammar: SomeAbX = SomeAbX()

    override val ntFuncs = hashMapOf<Nonterminal, (Descriptor<VertexType>, SppfNode<VertexType>?) -> Unit>(
        grammar.S.getNonterminal()!! to ::parseS,
        grammar.A.getNonterminal()!! to ::parseA
    )


    private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition

        when (state.id) {
            "S_0" -> {
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

    private fun parseA(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition

        when (state.id) {
            "A_0" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(grammar.a.terminal, state, inputEdge, descriptor, curSppfNode)
                }
            }

            "A_1" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(grammar.b.terminal, state, inputEdge, descriptor, curSppfNode)
                }
            }

            "A_2" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(grammar.a.terminal, state, inputEdge, descriptor, curSppfNode)
                }
            }
        }
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
