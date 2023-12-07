package rsm.builder

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.opt
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRSMToTXT
import rsm.RsmTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OptionalTest : RsmTest {
    class AStar : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = opt(Term("a")) * S
        }
    }
    override fun getAStar(stateName: String): RSMState {
        val s = Nonterminal(stateName)
        val a = Terminal("a")
        val st0 = RSMState(s, isStart = true)
        s.startState = st0
        val st1 = RSMState(s)
        val st2 = RSMState(s, isFinal = true)
        st0.addTerminalEdge(RSMTerminalEdge(a, st1))
        st1.addNonterminalEdge(RSMNonterminalEdge(s, st2))
        st0.addNonterminalEdge(RSMNonterminalEdge(s, st2))
        return s.startState
    }

    @Test
    fun testRsm() {
        val aStar = AStar()
        assertNotNull(aStar.S.getNonterminal())
        writeRSMToTXT(aStar.getRsm(), "actual.txt")
        writeRSMToTXT(getAStar("S"), "expected.txt")
        assertTrue { equalsByNtName(getAStar("S"), aStar.getRsm()) }
    }
}