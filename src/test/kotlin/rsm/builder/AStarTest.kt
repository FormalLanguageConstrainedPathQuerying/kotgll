package rsm.builder

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.GlobalState
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertTrue

class AStarTest {
    class AStar : Grammar() {
        var S by NT()
        val A = Term("a")

        init {
            setStart(S)
            S = A or A * S or S * S
        }
    }

    @Test
    fun testRsm() {
        val s = Nonterminal("S")
        val a = Terminal("a")
        val st0 = RSMState(0, s, isStart = true)
        s.startState = st0
        val st1 = RSMState(1, s, isFinal = true)
        val st2 = RSMState(2, s)
        val st3 = RSMState(3, s, isFinal = true)
        st0.addTerminalEdge(RSMTerminalEdge(a, st1))
        st1.addNonterminalEdge(RSMNonterminalEdge(s, st3))
        st0.addNonterminalEdge(RSMNonterminalEdge(s, st2))
        st2.addNonterminalEdge(RSMNonterminalEdge(s, st3))

        GlobalState.resetCounter()
        val actual = AStar().buildRsm()

        assertTrue { st0.rsmEquals(actual) }
    }
}