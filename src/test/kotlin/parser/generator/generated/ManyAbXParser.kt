@file:Suppress("RedundantVisibilityModifier")

package parser.generator.generated

import org.srcgll.descriptors.Descriptor
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode
import parser.generator.handwrite.ManyAbX

public class ManyAbXParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
    override lateinit var ctx: IContext<VertexType, LabelType>

    override val grammar: ManyAbX = ManyAbX()

    private val S: Nonterminal = grammar.S.getNonterminal()!!

    private val A: Nonterminal = grammar.A.getNonterminal()!!

    private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

    override val ntFuncs: HashMap<Nonterminal, (
        descriptor: Descriptor<VertexType>,
        sppf: SppfNode<VertexType>?
    ) -> Unit> = hashMapOf(
        S to ::parseS,
        A to ::parseA,
    )


    private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
        val state = descriptor.rsmState
        val pos = descriptor.inputPosition
        when (state.id) {
            "S_1" -> {
            }

            "S_0" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(terminals[2], state, inputEdge, descriptor, curSppfNode)
                }
                // handle nonterminal edges
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
                    handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
                }
            }

            "A_1" -> {
                // handle terminal edges
                for (inputEdge in ctx.input.getEdges(pos)) {
                    handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
                }
            }
        }
    }
}
