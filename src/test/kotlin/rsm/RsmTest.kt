package rsm

import org.junit.jupiter.api.Test
import org.srcgll.rsm.*
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface RsmTest {
    fun isDebug() = false
    fun writeDotInDebug(startState: RSMState, rsmName: String) {
        if (isDebug()) {
            writeRSMToDOT(startState, "inc/$rsmName.dot")
            writeRSMToTXT(startState, "inc/$rsmName.txt")
        }
    }

    /**
     * Compare two RSM, two state are equal if they have same name
     */
    fun equalsByNtName(expected: RSMState, actual: RSMState): Boolean {
        return equalsByNtName(expected, actual, hashMapOf())
    }

    private fun equalsByNtName(expected: RSMState, actual: RSMState, equals: HashMap<RSMState, RSMState>): Boolean {
        if (equals[expected] != null) {
            return equals[expected] === actual
        }
        if (actual.nonterminal.name == null) {
            throw IllegalArgumentException("For comparing by name non terminal must have unique not null name")
        }
        if (expected.nonterminal.name != actual.nonterminal.name || expected.isStart != actual.isStart || expected.isFinal != actual.isFinal) {
            return false
        }
        equals[expected] = actual
        if (actual.outgoingTerminalEdges.size != expected.outgoingTerminalEdges.size || actual.outgoingNonterminalEdges.size != expected.outgoingNonterminalEdges.size) {
            return false
        }
        for (tEdge in expected.outgoingTerminalEdges) {
            val states = actual.outgoingTerminalEdges[tEdge.key]
                if(states == null) {
                    return false
                }
            if (!equalsAsSetByName(tEdge.value, states, equals)) {
                return false
            }
        }
        for (ntEdge in expected.outgoingNonterminalEdges) {
            val states =
                actual.outgoingNonterminalEdges.entries.firstOrNull { it.key.name == ntEdge.key.name } ?: return false
            if (!equalsAsSetByName(ntEdge.value, states.value, equals)) {
                return false
            }
        }
        equals[expected] = actual
        return true
    }

    private fun equalsAsSetByName(
        expected: HashSet<RSMState>, actual: HashSet<RSMState>, equals: HashMap<RSMState, RSMState>
    ): Boolean {
        if (expected.size != actual.size) {
            return false
        }
        for (state in expected) {
            val curState = actual.firstOrNull { it.nonterminal.name == state.nonterminal.name }
            if (curState == null || !equalsByNtName(state, curState, equals)) {
                return false
            }
        }
        return true
    }

    fun testIncremental(
        origin: RSMState,
        delta: RSMState,
        expected: RSMState,
        expectedCommonStates: Int,
        isRemoving: Boolean = false
    ) {
        writeDotInDebug(delta, "delta")
        writeDotInDebug(expected, "expected")
        writeDotInDebug(origin, "origin")
        val originStates = origin.getAllStates()
        if (isRemoving) {
            origin.remove(delta)
        } else {
            origin.add(delta)
        }
        writeDotInDebug(origin, "actual")
        assertTrue { equalsByNtName(expected, origin) }
        assertEquals(expectedCommonStates, originStates.intersect(origin.getAllStates()).size)
    }


    @Test
    fun testEquals() {
        assertTrue { equalsByNtName(getAStarRSM("S"), getAStarRSM("S")) }
        assertFalse { equalsByNtName(getAStarRSM("S"), getAStarRSM("K")) }
    }

    @Test
    fun debugTest(){
        assertFalse(isDebug(), "\"Debug\" flag must be set to false before committing.")
    }

    fun getAStarRSM(stateName: String): RSMState {
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

}