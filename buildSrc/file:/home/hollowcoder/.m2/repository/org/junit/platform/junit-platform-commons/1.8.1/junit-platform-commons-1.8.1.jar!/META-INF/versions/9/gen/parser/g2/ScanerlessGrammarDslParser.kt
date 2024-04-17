@file:Suppress("RedundantVisibilityModifier")

package gen.parser.g2

import grammars.g2.ScanerlessGrammarDsl
import java.util.HashMap
import kotlin.Unit
import kotlin.collections.List
import org.ucfs.descriptors.Descriptor
import org.ucfs.input.ILabel
import org.ucfs.parser.context.IContext
import org.ucfs.parser.generator.GeneratedParser
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode

public class ScanerlessGrammarDslParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
  override lateinit var ctx: IContext<VertexType, LabelType>

  override val grammar: ScanerlessGrammarDsl = ScanerlessGrammarDsl()

  private val S: Nonterminal = grammar.S.getNonterminal()!!

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  S to ::parseS,
  )


  private val terminals: List<ITerminal> = grammar.getTerminals().toList()

  private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S_2" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S_1" -> 
       {
      }
      "S_3" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }
}
