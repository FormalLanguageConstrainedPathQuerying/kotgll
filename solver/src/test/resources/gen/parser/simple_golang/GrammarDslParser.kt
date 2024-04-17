@file:Suppress("RedundantVisibilityModifier")

package gen.parser.simple_golang

import grammars.simple_golang.GrammarDsl
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

  private val Program: Nonterminal = grammar.Program.getNonterminal()!!

  private val Block: Nonterminal = grammar.Block.getNonterminal()!!

  private val Statement: Nonterminal = grammar.Statement.getNonterminal()!!

  private val IntExpr: Nonterminal = grammar.IntExpr.getNonterminal()!!

  private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  Program to ::parseProgram,
  Block to ::parseBlock,
  Statement to ::parseStatement,
  IntExpr to ::parseIntExpr,
  )


  private fun parseProgram(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "Program_1" -> 
       {
      }
      "Program_0" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
    }
  }

  private fun parseBlock(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "Block_0" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
    }
  }

  private fun parseStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "Statement_1" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[2], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "Statement_3" -> 
       {
      }
      "Statement_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, IntExpr, state.nonterminalEdges[IntExpr]!!, curSppfNode)
      }
      "Statement_2" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, IntExpr, state.nonterminalEdges[IntExpr]!!, curSppfNode)
      }
    }
  }

  private fun parseIntExpr(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "IntExpr_1" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[3], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "IntExpr_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "IntExpr_3" -> 
       {
      }
      "IntExpr_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
    }
  }
}
