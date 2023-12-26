package rsm

import org.junit.jupiter.api.Test
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface RsmTest {
    /**
     * Compare two RSM, two state are equal if they have same name
     *
     */
    fun equalsByNtName(expected: RsmState, actual: RsmState): Boolean {
        if (actual.nonterminal.name == null) {
            throw IllegalArgumentException("For comparing by name non terminal must have unique not null name")
        }
        if (expected.nonterminal.name != actual.nonterminal.name
            || expected.isStart != actual.isStart || expected.isFinal != actual.isFinal
        ) {
            return false
        }
        if (actual.outgoingEdges.size != expected.outgoingEdges.size) {
            return false
        }
        for ((expectedSymbol, originDestStates) in expected.outgoingEdges) {
            val actualDestStates: HashSet<RsmState> = when (expectedSymbol) {
                is Terminal<*> -> actual.outgoingEdges[expectedSymbol]
                is Nonterminal -> {
                    actual.outgoingEdges.entries.firstOrNull { (actualSymbol, _) ->
                        actualSymbol is Nonterminal && actualSymbol.name == expectedSymbol.name
                    }?.value
                }

                else -> throw Exception("Unsupported instance of Symbol: ${expectedSymbol.javaClass}")
            } ?: return false
            if (!equalsAsSetByName(originDestStates, actualDestStates)) {
                return false
            }
        }
        return true
    }

    private fun equalsAsSetByName(expected: HashSet<RsmState>, actual: HashSet<RsmState>): Boolean {
        if (expected.size != actual.size) {
            return false
        }
        for (expectedState in expected) {
            val actualState =
                actual.firstOrNull { actualState -> actualState.nonterminal.name == expectedState.nonterminal.name }
            if (actualState == null || !equalsByNtName(expectedState, actualState)) {
                return false
            }
        }
        return true
    }

    fun getAStarRsm(stateName: String): RsmState {
        val s = Nonterminal(stateName)
        val a = Terminal("a")
        val st0 = RsmState(s, isStart = true)
        s.startState = st0
        val st1 = RsmState(s, isFinal = true)
        val st2 = RsmState(s)
        val st3 = RsmState(s, isFinal = true)
        st0.addEdge(a, st1)
        st1.addEdge(s, st3)
        st0.addEdge(s, st2)
        st2.addEdge(s, st3)
        return s.startState
    }

    @Test
    fun testEquals() {
        assertTrue { equalsByNtName(getAStarRsm("S"), getAStarRsm("S")) }
        assertFalse { equalsByNtName(getAStarRsm("S"), getAStarRsm("K")) }
    }
}