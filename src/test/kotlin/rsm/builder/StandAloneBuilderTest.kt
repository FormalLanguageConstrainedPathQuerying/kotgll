package rsm.builder

import org.srcgll.grammar.combinator.regexp.StandAloneNt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.makeConcat
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

    @Test
    fun testBabaDelta() {
        fun getExpectedBaba(nonTerm: Nonterminal): RSMState {
            val st0 = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm)
            val st4 = RSMState(nonTerm, isFinal = true)
            st0.addEdge(Terminal("b"), st1)
            st1.addEdge(Terminal("a"), st2)
            st2.addEdge(Terminal("b"), st3)
            st3.addEdge(Terminal("a"), st4)
            return st0
        }

        fun getActualBaba(nonTerm: Nonterminal): RSMState {
            val s = StandAloneNt(nonTerm)
            return s.buildRsm(makeConcat("b", "a", "b", "a"))
        }

        val nonTerm = Nonterminal("S")
        equalsByNtName(getExpectedBaba(nonTerm), getActualBaba(nonTerm))
    }

    @Test
    fun testBra(){
        fun getExpectedBra(nonTerm: Nonterminal): RSMState {
            val st0 = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm, isFinal = true)
            st0.addEdge(Terminal("b"), st1)
            st1.addEdge(Terminal("r"), st2)
            st2.addEdge(Terminal("a"), st3)
            return st0
        }

        fun getActualBra(nonTerm: Nonterminal): RSMState {
            val s = StandAloneNt(nonTerm)
            return s.buildRsm(makeConcat("b", "r", "a"))
        }
        val nonTerm = Nonterminal("S")
        equalsByNtName(getExpectedBra(nonTerm), getActualBra(nonTerm))
    }
}

