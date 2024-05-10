package org.ucfs.rsm.symbol

import org.ucfs.rsm.RsmState
import java.util.*

data class Nonterminal(val name: String?) : Symbol {
    lateinit var startState: RsmState
    private var rsmStateLastId = 0
    override fun toString() = "Nonterminal(${name ?: this.hashCode()})"

    fun getNextRsmStateId(): Int {
        val id = rsmStateLastId
        rsmStateLastId++
        return id
    }

    /**
     * Get all states from RSM for current nonterminal
     */
    fun getStates(): Iterable<RsmState> {
        val used = HashSet<RsmState>()
        val queue = LinkedList<RsmState>()
        queue.add(startState)
        while (queue.isNotEmpty()) {
            val state = queue.remove()
            used.add(state)
            for (nextState in state.outgoingEdges.values.flatten()) {
                if (!used.contains(nextState)) {
                    queue.add(nextState)
                }
            }
        }
        return used
    }
}
