package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal

class RSMState
(
    val id          : Int,
    val nonterminal : Nonterminal,
    val isStart     : Boolean = false,
    val isFinal     : Boolean = false,
)
{
    val outgoingTerminalEdges    : HashMap<Terminal<*>, HashSet<RSMState>> = HashMap()
    val outgoingNonterminalEdges : HashMap<Nonterminal, HashSet<RSMState>> = HashMap()
    val coveredTargetStates      : HashSet<RSMState>                       = HashSet()
    val errorRecoveryLabels      : HashSet<Terminal<*>>                    = HashSet()

    override fun toString() =
        "RSMState(id=$id, nonterminal=$nonterminal, isStart=$isStart, isFinal=$isFinal)"

    override fun equals(other : Any?) : Boolean
    {
        if (this === other)     return true
        if (other !is RSMState) return false
        if (id != other.id)     return false

        return true
    }
    
    val hashCode : Int = id
    override fun hashCode() = hashCode

    fun addTerminalEdge(edge: RSMTerminalEdge) {
        if (!coveredTargetStates.contains(edge.head)) {
            errorRecoveryLabels.add(edge.terminal)
            coveredTargetStates.add(edge.head)
        }
        
        if (outgoingTerminalEdges.containsKey(edge.terminal)) {
            val targetStates = outgoingTerminalEdges.getValue(edge.terminal)

            targetStates.add(edge.head)
        } else {
            outgoingTerminalEdges[edge.terminal] = hashSetOf(edge.head)
        }
    }

    fun addNonterminalEdge(edge : RSMNonterminalEdge)
    {
        if (outgoingNonterminalEdges.containsKey(edge.nonterminal)) {
            val targetStates = outgoingNonterminalEdges.getValue(edge.nonterminal)

            targetStates.add(edge.head)
        } else {
            outgoingNonterminalEdges[edge.nonterminal] = hashSetOf(edge.head)
        }
    }
 
    fun rsmEquals(other : RSMState) : Boolean 
  {
        if (this != other) {
            return false
        }
        if (outgoingTerminalEdges != other.outgoingTerminalEdges) {
            return false
        }
        return outgoingNonterminalEdges == other.outgoingNonterminalEdges
    }
}
