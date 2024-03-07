package parser.generator.handwrite

import org.srcgll.descriptors.Descriptor
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRsmToDot
import org.srcgll.sppf.node.SppfNode


/**
 * hand-write parser for @ManyAbX
 *
 */
class ManyAbHandWriteParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
    override lateinit var ctx: IContext<VertexType, LabelType>
    override val grammar = ManyAbX()

    private val s: Nonterminal = grammar.S.getNonterminal()!!
    private val a: Nonterminal = grammar.A.getNonterminal()!!


    override val ntFuncs = hashMapOf<Nonterminal, (Descriptor<VertexType>, SppfNode<VertexType>?) -> Unit>(
        s to ::parseS,
        a to ::parseA
    )

    private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition

        when (state.id) {
            "S_0" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(Terminal("x"), state, inputEdge, descriptor, curSppfNode)
                }
                //handle nonterminal edges
                handleNonterminalEdge(descriptor, a, curSppfNode)
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
                    handleTerminal(Terminal("a"), state, inputEdge, descriptor, curSppfNode)
                }
            }

            "A_1" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(Terminal("b"), state, inputEdge, descriptor, curSppfNode)
                }
            }
        }
    }
}

/**
Grammar for
 *  S = (a b)* | x
 */

class ManyAbX : Grammar() {
    var S by Nt()
    var A by Nt()

    init {
        setStart(S)
        S = A or Term("x")
        A = Many(Term("a") * Term("b"))
        rsm
    }
}

fun main() {
    writeRsmToDot(SomeAbX().rsm, "gen/many_abx.dot")
}