@file:Suppress("RedundantVisibilityModifier")

package gen.parser.multi_dyck

import grammars.multi_dyck.GrammarDsl
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

  private val S1: Nonterminal = grammar.S1.getNonterminal()!!

  private val S2: Nonterminal = grammar.S2.getNonterminal()!!

  private val S3: Nonterminal = grammar.S3.getNonterminal()!!

  private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  S to ::parseS,
  S1 to ::parseS1,
  S2 to ::parseS2,
  S3 to ::parseS3,
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
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S3, state.nonterminalEdges[S3]!!, curSppfNode)
        handleNonterminalEdge(descriptor, S1, state.nonterminalEdges[S1]!!, curSppfNode)
        handleNonterminalEdge(descriptor, S2, state.nonterminalEdges[S2]!!, curSppfNode)
      }
    }
  }

  private fun parseS1(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S1_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S1_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S1_4" -> 
       {
      }
      "S1_1" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S1_3" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
    }
  }

  private fun parseS2(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S2_1" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S2_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[5], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S2_3" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S2_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[3], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S2_4" -> 
       {
      }
    }
  }

  private fun parseS3(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "S3_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[4], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "S3_3" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S3_1" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, S, state.nonterminalEdges[S]!!, curSppfNode)
      }
      "S3_4" -> 
       {
      }
      "S3_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[2], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }
}
