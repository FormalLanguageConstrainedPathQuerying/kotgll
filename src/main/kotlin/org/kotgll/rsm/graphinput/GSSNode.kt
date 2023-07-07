package org.kotgll.rsm.graphinput

import org.kotgll.graph.GraphNode
import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import org.kotgll.rsm.graphinput.sppf.SPPFNode
import java.util.*

class GSSNode(val nonterminal : Nonterminal, val pos : GraphNode)
{
  val edges : HashMap<Pair<RSMState, SPPFNode?>, HashSet<GSSNode>> = HashMap()

  fun addEdge(rsmState : RSMState, sppfNode : SPPFNode?, gssNode : GSSNode) : Boolean
  {
    val label = Pair(rsmState, sppfNode)

    if (!edges.containsKey(label)) edges[label] = HashSet()

    return edges[label]!!.add(gssNode)
  }

  override fun toString() = "GSSNode(nonterminal=$nonterminal, pos=$pos)"

  override fun equals(other : Any?) : Boolean
  {
    if (other !is GSSNode)                return false

    if (nonterminal != other.nonterminal) return false

    return pos == other.pos
  }

  val hashCode = Objects.hash(nonterminal, pos)
  override fun hashCode() = hashCode
}
