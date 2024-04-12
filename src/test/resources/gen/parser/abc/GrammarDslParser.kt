@file:Suppress("RedundantVisibilityModifier")

package gen.parser.abc

import grammars.abc.GrammarDsl
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

  private val S: Nonterminal = grammar.S.getNonterminal()!!

  private val A: Nonterminal = grammar.A.getNonterminal()!!

  private val B: Nonterminal = grammar.B.getNonterminal()!!

  private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  S to ::parseS,
  A to ::parseA,
  B to ::parseB,
  )


  private fun parseS(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, A, state.nonterminalEdges[A]!!, curSppfNode)
      }
      "S_3" -> 
       {
      }
      "S_1" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, B, state.nonterminalEdges[B]!!, curSppfNode)
      }
      "S_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[2], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }

  private fun parseA(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "A_1" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "A_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "A_2" -> 
       {
      }
    }
  }

  private fun parseB(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "B_1" -> 
       {
      }
      "B_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }
}
