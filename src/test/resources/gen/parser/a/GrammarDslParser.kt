@file:Suppress("RedundantVisibilityModifier")

package gen.parser.a

import grammars.a.GrammarDsl
import java.util.HashMap
import kotlin.Unit
import kotlin.collections.List
import org.ucfs.descriptors.Descriptor
import org.ucfs.input.ILabel
import org.ucfs.parser.context.IContext
import org.ucfs.parser.generator.GeneratedParser
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.rsm.symbol.Terminal
import org.ucfs.sppf.node.SppfNode

public class GrammarDslParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
  override lateinit var ctx: IContext<VertexType, LabelType>

  override val grammar: GrammarDsl = GrammarDsl()

  private val S: Nonterminal = grammar.S.getNonterminal()!!

  private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  S to ::parseS,
  )


  private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S_1" -> 
       {
      }
      "S_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }
}
