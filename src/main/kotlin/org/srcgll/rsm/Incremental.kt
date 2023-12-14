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
    private lateinit var commonStart: RSMState
    private var isRemoving = false

    /**
     * Only linear input
     */
    fun constructIncremental(delta: RSMState, isRemoving: Boolean) {
        this.isRemoving = isRemoving
        registerOriginStates()
        commonStart = clone(origin, delta)
        addDeltaStates()
        val unreachable = unregisterUnreachable()
        restoreUnreachableOrigins(unreachable)
        mergeOrRegister()
        removeDeadlocks()
    }


    /**
     * Check queue and cloned states one by one from the end
     */
    private fun mergeOrRegister() {
        val used = hashSetOf<RSMState>()

        /**
         * Redirect into newState those transitions coming into oldState
         * States must be equivalent on output
         */
        fun merge(oldState: RSMState, newState: RSMState) {
            val incomingEdges = calculateIncomingEdges(commonStart)
            val edges = incomingEdges[oldState] ?: emptySet()
            for (edge in edges) {
                edge.state.removeEdge(Edge(oldState, edge.symbol))
                edge.state.addEdge(edge.symbol, newState)
            }
        }

        fun mergeRecursive(state: RSMState) {
            if (!used.contains(state)) {
                used.add(state)
                for (outEdge in state.getOutgoingEdges()) {
                    mergeRecursive(outEdge.state)
                }
                val equivState = register.find {
                    state.equivalent(it)
                }
                if (equivState != null) {
                    merge(state, equivState)
                } else (register.add(state))
            }
        }
        mergeRecursive(commonStart)
    }

    private enum class State { Deadlock, Final, Known }

    /**
     * Remove deadlocks -- states with no outgoing edges that are not finite
     */
    private fun removeDeadlocks() {
        val canGoToFinal = HashMap<RSMState, State>()
        fun removeRecursive(state: RSMState) {
            if (!canGoToFinal.contains(state)) {
                if (state.isFinal) {
                    canGoToFinal[state] = State.Final
                } else {
                    canGoToFinal[state] = State.Known
                }
                if (state.getOutgoingEdges().isEmpty()) {
                    canGoToFinal[state] = if (state.isFinal) State.Final else State.Deadlock
                }
                for (outEdge in state.getOutgoingEdges()) {
                    removeRecursive(outEdge.state)
                    when (canGoToFinal[outEdge.state]) {
                        State.Deadlock -> state.removeEdge(outEdge)
                        //cycle
                        State.Known -> {}
                        State.Final -> canGoToFinal[state] = State.Final
                        else -> throw IllegalArgumentException()
                    }
                }
                if (canGoToFinal[state] != State.Final) {
                    canGoToFinal[state] = State.Deadlock
                }
            }
        }
        removeRecursive(commonStart)
    }


    /**
     *  Find unreachable origin states and remove them from Register
     */
    private fun unregisterUnreachable(): HashSet<RSMState> {
        val incomingEdges = calculateIncomingEdges(commonStart)
        val unreachable = origin.getAllStates().filter {
            incomingEdges[it].isNullOrEmpty()
        }.toHashSet()
        register.removeAll(unreachable)
        return unreachable
    }

    /**
     * Replace unreachable states from original Rsm as kotlin objects
     * in equivalent place in result rsm
     */
    private fun restoreUnreachableOrigins(unreachable: HashSet<RSMState>) {
        /**
         * Replace all incoming and outgoing transition from oldState to newState
         * Remove all transition of oldState
         */
        fun replace(oldState: RSMState, newState: RSMState) {
            newState.getOutgoingEdges().forEach { edge ->
                newState.removeEdge(edge)
            }
            calculateIncomingEdges(commonStart)[oldState]?.forEach { (state, symbol) ->
                state.removeEdge(oldState, symbol)
                state.addEdge(symbol, newState)
            }
            oldState.getOutgoingEdges().forEach { (state, symbol) ->
                newState.addEdge(symbol, state)
            }
        }

        val used = mutableSetOf<RSMState>()
        val queue = ArrayDeque<RSMState>()
        var updatedStart: RSMState? = null
        queue.add(commonStart)

        while (queue.isNotEmpty()) {
            var state = queue.removeFirst()
            if (!used.contains(state)) {
                used.add(state)
                val originState = cloneStatesToParents[state]!!.origin
                if (unreachable.contains(originState)) {
                    unreachable.remove(originState)
                    replace(state, originState)
                    if (state == commonStart) {
                        updatedStart = originState
                        commonStart = originState
                    }
                    state = originState
                }
                for (edge in state.getOutgoingEdges()) {
                    queue.add(edge.state)
                }
            }
        }
        if (updatedStart == null) {
            throw Exception("Start state should be updated!!")
        }
    }

    private fun clone(origin: RSMState, delta: RSMState): RSMState {
        /**
         * All outgoing transitions point to the corresponding intact states in ,
         * except for the transition with symbol a : xa ∈ Pr(w),
         * which will points to the corresponding cloned state
         */
        fun cloneOutgoingEdges(srcState: RSMState, destState: RSMState) {
            val srcOutgoingEdges = srcState.getOutgoingEdges()
            for (srcEdge in srcOutgoingEdges) {
                destState.addEdge(srcEdge.symbol, srcEdge.state)
            }
        }

        fun isFinal(): Boolean {
            if (isRemoving && delta.isFinal) {
                return false
            }
            return origin.isFinal || delta.isFinal
        }

        fun isStart(): Boolean = origin.isStart || delta.isStart

        val newState = RSMState(origin.nonterminal, isStart(), isFinal())
        cloneStatesToParents[newState] = CloneState(origin, delta)
        cloneOutgoingEdges(origin, newState)
        return newState
    }

    private fun registerOriginStates() {
        for (state in origin.getAllStates()) {
            //modification: common states are states of original rsm
            register.add(state)
            cloneStatesToParents[state] = CloneState(state, botDelta)
        }
    }

    private fun addDeltaStates() {
        /**
         * If source rsm contains edge with deltaSymbol -- returns clone state
         * in form (<stateFromOrigin>, <stateFromDelta>)
         * Else (<originAbsorption>, <stateFromDelta>)
         */
        fun cloneStep(
            qLast: RSMState,
            deltaSymbol: Symbol,
            newDelta: RSMState,
            origins: HashMap<out Symbol, HashSet<RSMState>>
        ): RSMState {
            val newOrigin = origins[deltaSymbol]?.first() ?: botOrigin
            val newState = clone(newOrigin, newDelta)
            val destEdge = qLast.getOutgoingEdges().find { it.symbol == deltaSymbol }
            if (destEdge != null) {
                qLast.removeEdge(destEdge)
            }
            qLast.addEdge(deltaSymbol, newState)
            return newState
        }

        var qLast = commonStart
        do {
            val (originState, deltaState) = cloneStatesToParents[qLast]!!
            val termEdges = deltaState.outgoingTerminalEdges.entries
            val nonTermEdges = deltaState.outgoingNonterminalEdges.entries
            for (t in termEdges) {
                qLast = cloneStep(qLast, t.key, t.value.first(), originState.outgoingTerminalEdges)
            }
            for (nt in nonTermEdges) {
                qLast = cloneStep(qLast, nt.key, nt.value.first(), originState.outgoingNonterminalEdges)
            }
        } while (termEdges.isNotEmpty() || nonTermEdges.isNotEmpty())
    }

    /**
     * For each state get set of state which contains output edge to it
     * and Symbol on this edge
     */
    private fun calculateIncomingEdges(state: RSMState): HashMap<RSMState, HashSet<Edge>> {
        val used = hashSetOf<RSMState>()
        val queue = ArrayDeque<RSMState>()
        queue.add(state)
        val incomingEdges = hashMapOf<RSMState, HashSet<Edge>>()
        while (queue.isNotEmpty()) {
            val nextState = queue.removeFirst()
            if (!used.contains(nextState)) {
                used.add(nextState)
                for (edge in nextState.getOutgoingEdges()) {
                    incomingEdges.getOrPut(edge.state) { hashSetOf() }.add(Edge(nextState, edge.symbol))
                    queue.add(edge.state)
                }
            }
        }
        return incomingEdges
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

fun RSMState.removeEdge(state: RSMState, symbol: Symbol) {
    when (symbol) {
        is Terminal<*> -> {
            outgoingTerminalEdges[symbol]!!.remove(state)
            if (outgoingTerminalEdges[symbol]!!.isEmpty()) {
                outgoingTerminalEdges.remove(symbol)
            }
        }

        is Nonterminal -> {
            outgoingNonterminalEdges[symbol]!!.remove(state)
            if (outgoingNonterminalEdges[symbol]!!.isEmpty()) {
                outgoingNonterminalEdges.remove(symbol)
            }
        }
    }
}

fun RSMState.removeEdge(edge: Edge) = this.removeEdge(edge.state, edge.symbol)

/**
 * Get all states of RSM reachable from startState
 */
fun RSMState.getAllStates(): HashSet<RSMState> {
    val states = hashSetOf<RSMState>()
    val queue = ArrayDeque<RSMState>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        if (!states.contains(state)) {
            states.add(state)
            for (edge in state.getOutgoingEdges()) {
                queue.add(edge.state)
            }
        }
    }
    return states
}

fun RSMState.getOutgoingEdges(): HashSet<Edge> {
    val states = hashSetOf<Edge>()
    outgoingNonterminalEdges.map { entry -> states.addAll(entry.value.map { Edge(it, entry.key) }) }
    outgoingTerminalEdges.map { entry -> states.addAll(entry.value.map { Edge(it, entry.key) }) }
    return states
}
