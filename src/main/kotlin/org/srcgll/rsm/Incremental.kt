package org.srcgll.rsm

import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Symbol
import org.srcgll.rsm.symbol.Terminal

class Incremental(private val origin: RSMState) {
    data class CloneState(val origin: RSMState, val delta: RSMState)

    private val botDelta = RSMState(origin.nonterminal)
    private val botOrigin = RSMState(origin.nonterminal)
    private val cloneStatesToParents = hashMapOf<RSMState, CloneState>()
    private val register = mutableSetOf<RSMState>()
    private val rsmProxy = RsmProxyInfo(origin)

    /**
     * Only linear input
     */
    fun constructIncremental(delta: RSMState, isRemoving: Boolean) {
        writeRSMToTXT(origin, "inc/1_origin.txt")
        // 1. Add all states from original RSM
        for (state in rsmProxy.originAllStates) {
            //optimization: common states are states of original rsm
            register.add(state)
            cloneStatesToParents[state] = CloneState(state, botDelta)
        }
        var commonStart = clone(origin, delta)
        cloneStatesToParents[commonStart] = CloneState(origin, delta)
        writeRSMToTXT(commonStart, "inc/2_common.txt")
        addDeltaStates(commonStart)
        writeRSMToTXT(commonStart, "inc/3_common.txt")

        rsmProxy.incomingEdges.putAll(rsmProxy.calculateIncomingEdges(commonStart))
        // 2. Find unreachable states and remove them from Register
        val unreachable = rsmProxy.originAllStates
            .filter { isUnreachable(it) }
            .toHashSet()
        register.removeAll(unreachable)
        writeRSMToTXT(commonStart, "inc/4_restore.txt")

        // 2.5. Optimization: "save" unreachable states from original RSM
        commonStart = restoreUnreachable(commonStart, unreachable)
        writeRSMToTXT(commonStart, "inc/5_restore.txt")

        // 3. Find and merge equivalent states between register and new states
        replaceOrRegister(commonStart)
        writeRSMToTXT(commonStart, "inc/6_replace.txt")
    }

    private fun isUnreachable(state: RSMState): Boolean {
        val edges = rsmProxy.incomingEdges[state]
        return edges == null || edges.size == 0
    }

    /**
     * Check queue and cloned states one by one from the end
     */
    private fun replaceOrRegister(startState: RSMState) {
        val used = hashSetOf<RSMState>()
        fun replaceByDfs(state: RSMState) {
            if (!used.contains(state)) {
                used.add(state)
                for (outEdge in rsmProxy.getOutgoingEdges(state)) {
                    replaceByDfs(outEdge.state)
                }
                val equivState = register.find {
                    state.equivalent(it)
                }
                if (equivState != null) {
                    replace(state, equivState)
                }
            }
        }
        replaceByDfs(startState)
    }

    private fun restoreUnreachable(
        newStart: RSMState,
        unreachable: HashSet<RSMState>,
    ): RSMState {
        val used = mutableSetOf<RSMState>()
        val queue = ArrayDeque<RSMState>()
        var updatedStart: RSMState? = null
        queue.add(newStart)
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            if (!used.contains(state)) {
                used.add(state)
                val originState = cloneStatesToParents[state]!!.origin
                if (unreachable.contains(originState)) {
                    unreachable.remove(originState)
                    replace(state, originState)
                    if (state == newStart) {
                        updatedStart = originState
                    }
                }
                for (edge in rsmProxy.getOutgoingEdges(state)) {
                    queue.add(edge.state)
                }
            }
        }
        return updatedStart ?: throw Exception("Start state should be updated!!")
    }

    private fun clone(origin: RSMState, delta: RSMState): RSMState {
        val newState = RSMState(origin.nonterminal, origin.isStart || delta.isStart, origin.isFinal || delta.isFinal)
        cloneStatesToParents[newState] = CloneState(origin, delta)
        rsmProxy.cloneOutgoingEdges(origin, newState)
        return newState
    }

    private fun addDeltaStates(commonStart: RSMState) {
        fun cloneStep(
            qLast: RSMState,
            s: Symbol,
            newDelta: RSMState,
            origins: HashMap<out Symbol, HashSet<RSMState>>
        ): RSMState {
            val newOrigin = origins[s]?.first() ?: botOrigin
            val newState = clone(newOrigin, newDelta)
            qLast.addEdge(s, newState)
            return newState
        }

        var qLast = commonStart
        do {
            val parents = cloneStatesToParents[qLast]!!
            val termEdges = parents.delta.outgoingTerminalEdges.entries
            val nonTermEdges = parents.delta.outgoingNonterminalEdges.entries
            for (t in termEdges) {
                qLast = cloneStep(qLast, t.key, t.value.first(), parents.origin.outgoingTerminalEdges)
            }
            for (nt in nonTermEdges) {
                qLast = cloneStep(qLast, nt.key, nt.value.first(), parents.origin.outgoingNonterminalEdges)
            }
        } while (termEdges.isNotEmpty() || nonTermEdges.isNotEmpty())
    }

    private fun replace(oldState: RSMState, newState: RSMState) {
        val edges = rsmProxy.incomingEdges[oldState] ?: emptySet()
        for (edge in edges) {
            edge.state.removeEdge(Edge(oldState, edge.symbol))
            edge.state.addEdge(edge.symbol, newState)
        }
        for (edge in rsmProxy.getOutgoingEdges(oldState)) {
            newState.addEdge(edge.symbol, edge.state)
        }
    }

    data class RsmProxyInfo(val startState: RSMState) {
        private var outgoingEdges = hashMapOf<RSMState, HashSet<Edge>>()
        var originAllStates: HashSet<RSMState>
        var incomingEdges: HashMap<RSMState, HashSet<Edge>>

        init {
            originAllStates = getAllStates()
            incomingEdges = calculateIncomingEdges(startState)
        }

        fun cloneOutgoingEdges(srcState: RSMState, destState: RSMState) {
            val srcOutgoingEdges = getOutgoingEdges(srcState)
            val destOutgoingEdges = getOutgoingEdges(destState)

            for (edge in srcOutgoingEdges) {
                if (destOutgoingEdges.find { it.symbol == edge.symbol } == null) {
                    destState.addEdge(edge.symbol, edge.state)
                    incomingEdges.getOrPut(edge.state) { hashSetOf() }.add(Edge(destState, edge.symbol))
                }
            }
        }

        fun getOutgoingEdges(state: RSMState): HashSet<Edge> {
            var states = outgoingEdges[state]
            return if (states == null) {
                states = hashSetOf()
                state.outgoingNonterminalEdges.map { entry -> states.addAll(entry.value.map { Edge(it, entry.key) }) }
                state.outgoingTerminalEdges.map { entry -> states.addAll(entry.value.map { Edge(it, entry.key) }) }
                states
            } else {
                states
            }
        }

        private fun getAllStates(): HashSet<RSMState> {
            val states = hashSetOf<RSMState>()
            val queue = ArrayDeque<RSMState>()
            queue.add(startState)
            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()
                if (!states.contains(state)) {
                    states.add(state)
                    for (edge in getOutgoingEdges(state)) {
                        queue.add(edge.state)
                    }
                }
            }
            return states
        }

        /**
         * For each state get set of state which contains output edge to it
         * and Symbol on this edge
         */
        fun calculateIncomingEdges(state: RSMState): HashMap<RSMState, HashSet<Edge>> {
            val used = hashSetOf<RSMState>()
            val queue = ArrayDeque<RSMState>()
            queue.add(state)
            val incomingEdges = hashMapOf<RSMState, HashSet<Edge>>()
            while (queue.isNotEmpty()) {
                val nextState = queue.removeFirst()
                if (!used.contains(nextState)) {
                    used.add(nextState)
                    for (edge in getOutgoingEdges(nextState)) {
                        incomingEdges.getOrPut(edge.state) { hashSetOf() }.add(Edge(nextState, edge.symbol))
                        queue.add(edge.state)
                    }
                }
            }
            return incomingEdges
        }
    }
}

data class Edge(val state: RSMState, val symbol: Symbol)

fun RSMState.equivalent(other: RSMState): Boolean {
    if (nonterminal != other.nonterminal) {
        return false
    }
    if (isFinal != other.isFinal || isStart != other.isStart) {
        return false
    }
    if (outgoingTerminalEdges != other.outgoingTerminalEdges) {
        return false
    }
    return outgoingNonterminalEdges == other.outgoingNonterminalEdges
}

fun RSMState.add(delta: RSMState) {
    Incremental(this).constructIncremental(delta, false)
}

fun RSMState.remove(delta: RSMState) {
    Incremental(this).constructIncremental(delta, true)
}

fun RSMState.removeEdge(edge: Edge){
    when(edge.symbol){
        is Terminal<*> -> outgoingTerminalEdges[edge.symbol]!!.remove(edge.state)
        is Nonterminal -> outgoingNonterminalEdges[edge.symbol]!!.remove(edge.state)
    }
}





