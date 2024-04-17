@file:Suppress("RedundantVisibilityModifier")

package gen.parser.simple_golang

import grammars.simple_golang.ScanerlessGrammarDsl
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

  private val Program: Nonterminal = grammar.Program.getNonterminal()!!

  private val Block: Nonterminal = grammar.Block.getNonterminal()!!

  private val Statement: Nonterminal = grammar.Statement.getNonterminal()!!

  private val IntExpr: Nonterminal = grammar.IntExpr.getNonterminal()!!

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf(
  Program to ::parseProgram,
  Block to ::parseBlock,
  Statement to ::parseStatement,
  IntExpr to ::parseIntExpr,
  )


  private val terminals: List<ITerminal> = grammar.getTerminals().toList()

  private fun parseProgram(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.id) {
      "Program_0" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      "Program_1" -> 
       {
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
      "Statement_3" -> 
       {
      }
      "Statement_2" -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, IntExpr, state.nonterminalEdges[IntExpr]!!, curSppfNode)
      }
      "Statement_1" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[2], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "Statement_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[3], state, inputEdge, descriptor, curSppfNode)
        }
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
          handleTerminal(terminals[0], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "IntExpr_0" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "IntExpr_2" -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          handleTerminal(terminals[1], state, inputEdge, descriptor, curSppfNode)
        }
      }
      "IntExpr_3" -> 
       {
      }
    }
  }
}
