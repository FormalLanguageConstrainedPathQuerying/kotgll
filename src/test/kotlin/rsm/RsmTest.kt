package rsm

import org.junit.jupiter.api.Test
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface RsmTest {
    /**
     * Compare two RSM, two state are equal if they have same name
     *
     */
    fun equalsByNtName(expected: RSMState, actual: RSMState): Boolean {
        if (actual.nonterminal.name == null) {
            throw IllegalArgumentException("For comparing by name non terminal must have unique not null name")
        }
        if (expected.nonterminal.name != actual.nonterminal.name
            || expected.isStart != actual.isStart || expected.isFinal != actual.isFinal) {
            return false
        }
        if (actual.outgoingTerminalEdges.size != expected.outgoingTerminalEdges.size
            || actual.outgoingNonterminalEdges.size != expected.outgoingNonterminalEdges.size) {
            return false
        }
        for (tEdge in expected.outgoingTerminalEdges) {
            val states = actual.outgoingTerminalEdges[tEdge.key] ?: return false
            if (!equalsAsSetByName(tEdge.value, states)) {
                return false
            }
        }
        for (ntEdge in expected.outgoingNonterminalEdges) {
            val states =
                actual.outgoingNonterminalEdges.entries.firstOrNull { it.key.name == ntEdge.key.name } ?: return false
            if (!equalsAsSetByName(ntEdge.value, states.value)) {
                return false
            }
        }
        return true
    }

    private fun equalsAsSetByName(expected: HashSet<RSMState>, actual: HashSet<RSMState>): Boolean {
        if (expected.size != actual.size) {
            return false
        }
        for (state in expected) {
            val curState = actual.firstOrNull { it.nonterminal.name == state.nonterminal.name }
            if (curState == null || !equalsByNtName(state, curState)) {
                return false
            }
        }
        return true
    }

    fun getAStar(stateName: String): RSMState {
        val s = Nonterminal(stateName)
        val a = Terminal("a")
        val st0 = RSMState(s, isStart = true)
        s.startState = st0
        val st1 = RSMState(s, isFinal = true)
        val st2 = RSMState(s)
        val st3 = RSMState(s, isFinal = true)
        st0.addTerminalEdge(RSMTerminalEdge(a, st1))
        st1.addNonterminalEdge(RSMNonterminalEdge(s, st3))
        st0.addNonterminalEdge(RSMNonterminalEdge(s, st2))
        st2.addNonterminalEdge(RSMNonterminalEdge(s, st3))
        return s.startState
    }

    @Test
    fun testEquals() {
        assertTrue { equalsByNtName(getAStar("S"), getAStar("S")) }
        assertFalse { equalsByNtName(getAStar("S"), getAStar("K")) }
    }
}