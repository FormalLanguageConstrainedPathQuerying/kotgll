@file:Suppress("RedundantVisibilityModifier")

package gen.parser.bracket_star_x

import grammars.bracket_star_x.GrammarDsl
import java.util.HashMap
import kotlin.Unit
import kotlin.collections.List
import org.srcgll.descriptors.Descriptor
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode

public class GrammarDslParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
  override lateinit var ctx: IContext<VertexType, LabelType>

  override val grammar: GrammarDsl = GrammarDsl()

  private val List: Nonterminal = grammar.List.getNonterminal()!!

  private val Elem: Nonterminal = grammar.Elem.getNonterminal()!!

  private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  List to ::parseList,
  Elem to ::parseElem,
  )


  private fun parseList(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "List_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "List_1" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Elem, state.nonterminalEdges[Elem]!!, curSppfNode)
      }
      "List_2" -> 
       {
      }
    }
  }

  private fun parseElem(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "Elem_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, List, state.nonterminalEdges[List]!!, curSppfNode)
      }
      "Elem_1" -> 
       {
      }
    }
  }
}
