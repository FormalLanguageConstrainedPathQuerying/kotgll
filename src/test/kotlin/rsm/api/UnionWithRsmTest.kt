package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.add
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRSMToDOT
import rsm.RsmTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnionWithRsmTest : RsmTest {

    @Test
    fun testSimpleAddition() {
        class DyckLanguage : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = Term("(") * S * Term(")")
            }
        }

        val grammar = DyckLanguage()
        val startOrigin = grammar.getRsm()
        val deltaStart = RSMState(grammar.S.getNonterminal()!!, isStart = true, isFinal = false)
        val st1 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = false)
        val st2 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = false)
        val st3 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = true)
        deltaStart.addEdge(Terminal("["), st1)
        st1.addEdge(grammar.S.getNonterminal()!!, st2)
        st2.addEdge(Terminal("]"), st3)
        writeRSMToDOT(deltaStart, "delta.dot")
        writeRSMToDOT(startOrigin, "source.dot")
        startOrigin.add(deltaStart)
        writeRSMToDOT(startOrigin, "result.dot")
        assertEquals(startOrigin, startOrigin)
    }

    @Test
    fun `test union {(ba)+, bar} with {bra}`() {
        fun getExpected(nonTerm: Nonterminal): RSMState {
            //construct expected RSM: Minimal automaton accepting the set (ba)+ ∪ {bar} ∪ {bra}.
            val st0 = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm, isFinal = true)
            val st3 = RSMState(nonTerm)
            val st4 = RSMState(nonTerm)
            val st5 = RSMState(nonTerm, isFinal = true)
            val st6 = RSMState(nonTerm, isFinal = true)
            st0.addEdge(Terminal("b"), st1)
            st1.addEdge(Terminal("a"), st2)
            st1.addEdge(Terminal("r"), st3)
            st2.addEdge(Terminal("b"), st4)
            st2.addEdge(Terminal("r"), st5)
            st3.addEdge(Terminal("a"), st5)
            st4.addEdge(Terminal("a"), st6)
            st6.addEdge(Terminal("b"), st4)
            return st0
        }

        fun getDelta(nonTerm: Nonterminal): RSMState {
            //single-string automaton accepting string bra
            val st0 = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm, isFinal = true)
            st0.addEdge(Terminal("b"), st1)
            st1.addEdge(Terminal("r"), st2)
            st2.addEdge(Terminal("a"), st3)
            return st0
        }

        class BaPlusBar : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = some(Term("b") * Term("a")) or (
                        Term("b") * Term("a") * Term("r")
                        )
            }
//            init {
//                setStart(S)
//                S = some(Term("b") * Term("a")) or(
//                        Term("b") * Term("a") * Term("b")
//                        )
//            }
        }

        val s = Nonterminal("S")
        val grammar = BaPlusBar()
        grammar.getRsm().add(getDelta(s))
        val expected = getExpected(s)
        val actual = grammar.getRsm()
        writeRSMToDOT(actual, "inc/actual.dot")
        writeRSMToDOT(expected, "inc/expected.dot")

        assertTrue { equalsByNtName(expected, actual) }
    }
}