package rsm.builder

import org.srcgll.grammar.combinator.regexp.StandAloneNt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import rsm.RsmTest
import kotlin.test.Test

class StandAloneBuilderTest : RsmTest {
    @Test
    fun testDyckDelta() {
        fun getExpected(nonTerm: Nonterminal): RSMState {
            val deltaStart = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm)
            val st4 = RSMState(nonTerm, isFinal = true)
            deltaStart.addEdge(Terminal("["), st1)
            st1.addEdge(nonTerm, st2)
            st2.addEdge(Terminal("]"), st3)
            st3.addEdge(nonTerm, st4)
            return deltaStart
        }

        fun getActual(nonTerm: Nonterminal): RSMState {
            val s = StandAloneNt(nonTerm)
            s.setDescription(Term("[") * s * Term("]") * s)
            return s.buildRsmBox()
        }

        val nonTerm = Nonterminal("S")
        equalsByNtName(getExpected(nonTerm), getActual(nonTerm))
    }

}